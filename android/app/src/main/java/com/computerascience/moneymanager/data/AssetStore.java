package com.computerascience.moneymanager.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetBackup;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssetStore {
    private static final String PREFS = "money_manager_assets";
    private static final String KEY_ASSETS = "assets";
    private static final String KEY_SNAPSHOTS = "snapshots";
    private static final String KEY_UPDATE_EVENTS = "updateEvents";
    private static final String KEY_SETTINGS = "settings";

    private final SharedPreferences preferences;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public AssetStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<AssetRecord> load() {
        String raw = preferences.getString(KEY_ASSETS, "");
        if (raw == null || raw.isEmpty()) {
            return defaultAssets();
        }

        try {
            JSONArray array = new JSONArray(raw);
            List<AssetRecord> assets = new ArrayList<>();
            for (int index = 0; index < array.length(); index += 1) {
                assets.add(AssetRecord.fromJson(array.getJSONObject(index)));
            }
            if (migrateLegacyAccountAssets(assets)) {
                save(assets);
            }
            return assets;
        } catch (JSONException error) {
            return defaultAssets();
        }
    }

    public void save(List<AssetRecord> assets) {
        JSONArray array = new JSONArray();
        for (AssetRecord asset : assets) {
            try {
                array.put(asset.toJson());
            } catch (JSONException ignored) {
                // Skip malformed records rather than losing the whole list.
            }
        }
        preferences.edit().putString(KEY_ASSETS, array.toString()).apply();
    }

    public PortfolioSettings loadSettings() {
        String raw = preferences.getString(KEY_SETTINGS, "");
        if (raw == null || raw.isEmpty()) {
            return new PortfolioSettings();
        }
        try {
            return PortfolioSettings.fromJson(new JSONObject(raw));
        } catch (JSONException error) {
            return new PortfolioSettings();
        }
    }

    public void saveSettings(PortfolioSettings settings) {
        settings.ensureBaseRate();
        try {
            preferences.edit().putString(KEY_SETTINGS, settings.toJson().toString()).apply();
        } catch (JSONException ignored) {
            // Keep the previous settings if serialization fails.
        }
    }

    public List<AssetSnapshot> loadSnapshots() {
        String raw = preferences.getString(KEY_SNAPSHOTS, "");
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JSONArray array = new JSONArray(raw);
            List<AssetSnapshot> snapshots = new ArrayList<>();
            for (int index = 0; index < array.length(); index += 1) {
                snapshots.add(AssetSnapshot.fromJson(array.getJSONObject(index)));
            }
            return pruneAndSort(snapshots);
        } catch (JSONException error) {
            return new ArrayList<>();
        }
    }

    public List<AssetUpdateEvent> loadUpdateEvents() {
        String raw = preferences.getString(KEY_UPDATE_EVENTS, "");
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JSONArray array = new JSONArray(raw);
            List<AssetUpdateEvent> events = new ArrayList<>();
            for (int index = 0; index < array.length(); index += 1) {
                events.add(AssetUpdateEvent.fromJson(array.getJSONObject(index)));
            }
            return pruneAndSortUpdateEvents(events);
        } catch (JSONException error) {
            return new ArrayList<>();
        }
    }

    public List<AssetSnapshot> recordSnapshot(List<AssetRecord> assets, PortfolioSettings settings) {
        List<AssetSnapshot> snapshots = loadSnapshots();
        PortfolioSummary summary = AssetMath.summarize(assets, settings);
        long now = System.currentTimeMillis();
        String today = dayFormat.format(new Date(now));
        AssetSnapshot snapshot = new AssetSnapshot(
                today,
                now,
                summary.baseCurrency,
                summary.netWorth,
                summary.grossAssets,
                summary.liabilities,
                categoryValues(summary)
        );

        boolean replaced = false;
        for (int index = 0; index < snapshots.size(); index += 1) {
            AssetSnapshot existing = snapshots.get(index);
            if (today.equals(existing.dayKey) && summary.baseCurrency.equals(existing.baseCurrency)) {
                snapshots.set(index, snapshot);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            snapshots.add(snapshot);
        }
        snapshots = pruneAndSort(snapshots);
        saveSnapshots(snapshots);
        return snapshots;
    }

    public List<AssetSnapshot> upsertSnapshot(AssetSnapshot snapshot) {
        List<AssetSnapshot> snapshots = loadSnapshots();
        upsertSnapshotInto(snapshots, snapshot);
        snapshots = pruneAndSort(snapshots);
        saveSnapshots(snapshots);
        return snapshots;
    }

    public List<AssetSnapshot> upsertSnapshots(List<AssetSnapshot> importedSnapshots) {
        List<AssetSnapshot> snapshots = loadSnapshots();
        for (AssetSnapshot snapshot : importedSnapshots) {
            upsertSnapshotInto(snapshots, snapshot);
        }
        snapshots = pruneAndSort(snapshots);
        saveSnapshots(snapshots);
        return snapshots;
    }

    private void upsertSnapshotInto(List<AssetSnapshot> snapshots, AssetSnapshot snapshot) {
        boolean replaced = false;
        for (int index = 0; index < snapshots.size(); index += 1) {
            AssetSnapshot existing = snapshots.get(index);
            if (snapshot.dayKey.equals(existing.dayKey) && snapshot.baseCurrency.equals(existing.baseCurrency)) {
                snapshots.set(index, snapshot);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            snapshots.add(snapshot);
        }
    }

    public List<AssetSnapshot> deleteSnapshot(String dayKey, String baseCurrency) {
        List<AssetSnapshot> snapshots = loadSnapshots();
        for (int index = snapshots.size() - 1; index >= 0; index -= 1) {
            AssetSnapshot snapshot = snapshots.get(index);
            if (dayKey.equals(snapshot.dayKey) && baseCurrency.equals(snapshot.baseCurrency)) {
                snapshots.remove(index);
            }
        }
        saveSnapshots(snapshots);
        return snapshots;
    }

    public List<AssetUpdateEvent> recordUpdateEvent(AssetUpdateEvent event) {
        List<AssetUpdateEvent> events = loadUpdateEvents();
        events.add(event);
        events = pruneAndSortUpdateEvents(events);
        saveUpdateEvents(events);
        return events;
    }

    public List<AssetUpdateEvent> deleteUpdateEvent(String assetId, long timestamp) {
        List<AssetUpdateEvent> events = loadUpdateEvents();
        for (int index = events.size() - 1; index >= 0; index -= 1) {
            AssetUpdateEvent event = events.get(index);
            if (assetId.equals(event.assetId) && event.timestamp == timestamp) {
                events.remove(index);
            }
        }
        events = pruneAndSortUpdateEvents(events);
        saveUpdateEvents(events);
        return events;
    }

    public String exportJson(
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            List<AssetUpdateEvent> updateEvents,
            PortfolioSettings settings
    ) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", "money-manager-android");
        root.put("version", 6);
        root.put("exportedAt", System.currentTimeMillis());

        JSONArray assetArray = new JSONArray();
        for (AssetRecord asset : assets) {
            assetArray.put(asset.toJson());
        }
        root.put("assets", assetArray);

        JSONArray snapshotArray = new JSONArray();
        for (AssetSnapshot snapshot : snapshots) {
            snapshotArray.put(snapshot.toJson());
        }
        root.put("snapshots", snapshotArray);

        JSONArray updateArray = new JSONArray();
        for (AssetUpdateEvent event : updateEvents) {
            updateArray.put(event.toJson());
        }
        root.put("updateEvents", updateArray);
        root.put("settings", settings.toJson());
        return root.toString(2);
    }

    public AssetBackup parseBackup(String raw) throws JSONException {
        JSONObject root = new JSONObject(raw);
        JSONArray assetArray = root.optJSONArray("assets");
        if (assetArray == null) {
            throw new JSONException("Missing assets");
        }

        List<AssetRecord> importedAssets = new ArrayList<>();
        for (int index = 0; index < assetArray.length(); index += 1) {
            importedAssets.add(AssetRecord.fromJson(assetArray.getJSONObject(index)));
        }
        migrateLegacyAccountAssets(importedAssets);

        List<AssetSnapshot> importedSnapshots = new ArrayList<>();
        JSONArray snapshotArray = root.optJSONArray("snapshots");
        if (snapshotArray != null) {
            for (int index = 0; index < snapshotArray.length(); index += 1) {
                importedSnapshots.add(AssetSnapshot.fromJson(snapshotArray.getJSONObject(index)));
            }
        }

        List<AssetUpdateEvent> importedUpdateEvents = new ArrayList<>();
        JSONArray updateArray = root.optJSONArray("updateEvents");
        if (updateArray != null) {
            for (int index = 0; index < updateArray.length(); index += 1) {
                importedUpdateEvents.add(AssetUpdateEvent.fromJson(updateArray.getJSONObject(index)));
            }
        }
        PortfolioSettings importedSettings = root.has("settings")
                ? PortfolioSettings.fromJson(root.getJSONObject("settings"))
                : new PortfolioSettings();
        return new AssetBackup(
                importedAssets,
                pruneAndSort(importedSnapshots),
                pruneAndSortUpdateEvents(importedUpdateEvents),
                importedSettings
        );
    }

    public void replaceAll(AssetBackup backup) {
        save(backup.assets);
        saveSnapshots(pruneAndSort(backup.snapshots));
        saveUpdateEvents(pruneAndSortUpdateEvents(backup.updateEvents));
        saveSettings(backup.settings);
    }

    private boolean migrateLegacyAccountAssets(List<AssetRecord> assets) {
        boolean changed = false;
        for (AssetRecord asset : assets) {
            changed = migrateLegacyAccountAsset(asset) || changed;
        }
        return changed;
    }

    private boolean migrateLegacyAccountAsset(AssetRecord asset) {
        boolean changed = false;
        String category = asset.category == null ? "" : asset.category;
        if ("银行".equals(category) || AssetCategories.BANK_DEPOSIT.equals(category)) {
            if (!hasText(asset.bankDepositAmount)) {
                asset.bankDepositAmount = asset.amount;
            }
            asset.category = AssetCategories.BANK_ACCOUNT;
            changed = true;
        } else if (AssetCategories.BANK_WEALTH.equals(category)) {
            if (!hasText(asset.bankWealthAmount)) {
                asset.bankWealthAmount = asset.amount;
            }
            asset.category = AssetCategories.BANK_ACCOUNT;
            changed = true;
        } else if ("券商".equals(category)
                || AssetCategories.BROKER_HOLDING.equals(category)
                || AssetCategories.STOCK_HOLDING.equals(category)) {
            if (!hasText(asset.investmentHoldingAmount)) {
                asset.investmentHoldingAmount = asset.amount;
            }
            asset.category = AssetCategories.INVESTMENT_ACCOUNT;
            changed = true;
        } else if (AssetCategories.BROKER_CASH.equals(category)) {
            if (!hasText(asset.investmentCashAmount)) {
                asset.investmentCashAmount = asset.amount;
            }
            asset.category = AssetCategories.INVESTMENT_ACCOUNT;
            changed = true;
        }

        if (AssetCategories.BANK_ACCOUNT.equals(asset.category)
                && !AssetMath.hasBankBreakdown(asset)
                && hasText(asset.amount)) {
            asset.bankDepositAmount = asset.amount;
            changed = true;
        }
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)
                && !AssetMath.hasInvestmentBreakdown(asset)
                && hasText(asset.amount)) {
            asset.investmentHoldingAmount = asset.amount;
            changed = true;
        }
        return changed;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void saveSnapshots(List<AssetSnapshot> snapshots) {
        JSONArray array = new JSONArray();
        for (AssetSnapshot snapshot : snapshots) {
            try {
                array.put(snapshot.toJson());
            } catch (JSONException ignored) {
                // Skip malformed snapshots rather than dropping the full trend.
            }
        }
        preferences.edit().putString(KEY_SNAPSHOTS, array.toString()).apply();
    }

    private void saveUpdateEvents(List<AssetUpdateEvent> events) {
        JSONArray array = new JSONArray();
        for (AssetUpdateEvent event : events) {
            try {
                array.put(event.toJson());
            } catch (JSONException ignored) {
                // Skip malformed events rather than dropping the full history.
            }
        }
        preferences.edit().putString(KEY_UPDATE_EVENTS, array.toString()).apply();
    }

    private Map<String, Double> categoryValues(PortfolioSummary summary) {
        Map<String, Double> values = new HashMap<>();
        for (CategoryBreakdown category : summary.categories) {
            if (category.value > 0) {
                values.put(category.category, category.value);
            }
        }
        return values;
    }

    private List<AssetSnapshot> pruneAndSort(List<AssetSnapshot> snapshots) {
        long cutoff = System.currentTimeMillis() - 370L * AssetMath.DAY_MS;
        List<AssetSnapshot> pruned = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (snapshot.timestamp >= cutoff) {
                pruned.add(snapshot);
            }
        }
        Collections.sort(pruned, (left, right) -> Long.compare(left.timestamp, right.timestamp));
        return pruned;
    }

    private List<AssetUpdateEvent> pruneAndSortUpdateEvents(List<AssetUpdateEvent> events) {
        long cutoff = System.currentTimeMillis() - 370L * AssetMath.DAY_MS;
        List<AssetUpdateEvent> pruned = new ArrayList<>();
        for (AssetUpdateEvent event : events) {
            if (event.timestamp >= cutoff) {
                pruned.add(event);
            }
        }
        Collections.sort(pruned, (left, right) -> Long.compare(right.timestamp, left.timestamp));
        if (pruned.size() > 200) {
            return new ArrayList<>(pruned.subList(0, 200));
        }
        return pruned;
    }

    private List<AssetRecord> defaultAssets() {
        List<AssetRecord> assets = new ArrayList<>();

        AssetRecord bank = new AssetRecord();
        bank.name = "银行账户";
        bank.category = AssetCategories.BANK_ACCOUNT;
        bank.institution = "待绑定银行 App";
        bank.amount = "0";
        bank.currency = "CNY";
        bank.updateEveryDays = 7;
        bank.bankDepositAmount = "0";
        bank.bankWealthAmount = "0";
        bank.bankDebtAmount = "0";
        bank.note = "在一个条目里记录存款、理财和负债，只绑定一次银行 App。";
        assets.add(bank);

        AssetRecord investment = new AssetRecord();
        investment.name = "投资账户";
        investment.category = AssetCategories.INVESTMENT_ACCOUNT;
        investment.institution = "待绑定券商 App";
        investment.amount = "0";
        investment.currency = "CNY";
        investment.updateEveryDays = 1;
        investment.investmentHoldingAmount = "0";
        investment.investmentCashAmount = "0";
        investment.note = "记录持仓市值、可用现金和持股备注，只绑定一次券商 App。";
        assets.add(investment);

        return assets;
    }
}
