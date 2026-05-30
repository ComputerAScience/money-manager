package com.computerascience.moneymanager;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class AssetStore {
    private static final String PREFS = "money_manager_assets";
    private static final String KEY_ASSETS = "assets";
    private static final String KEY_SNAPSHOTS = "snapshots";

    private final SharedPreferences preferences;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    AssetStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    List<AssetRecord> load() {
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
            return assets;
        } catch (JSONException error) {
            return defaultAssets();
        }
    }

    void save(List<AssetRecord> assets) {
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

    List<AssetSnapshot> loadSnapshots() {
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

    List<AssetSnapshot> recordSnapshot(List<AssetRecord> assets) {
        List<AssetSnapshot> snapshots = loadSnapshots();
        PortfolioSummary summary = AssetMath.summarize(assets);
        long now = System.currentTimeMillis();
        String today = dayFormat.format(new Date(now));
        AssetSnapshot snapshot = new AssetSnapshot(
                today,
                now,
                summary.netWorth,
                summary.grossAssets,
                summary.liabilities
        );

        boolean replaced = false;
        for (int index = 0; index < snapshots.size(); index += 1) {
            if (today.equals(snapshots.get(index).dayKey)) {
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

    private List<AssetRecord> defaultAssets() {
        List<AssetRecord> assets = new ArrayList<>();

        AssetRecord bank = new AssetRecord();
        bank.name = "银行卡余额";
        bank.category = "银行";
        bank.institution = "待绑定银行 App";
        bank.amount = "0";
        bank.currency = "CNY";
        bank.updateEveryDays = 7;
        bank.note = "编辑时选择已安装银行 App，即可一键打开更新。";
        assets.add(bank);

        AssetRecord broker = new AssetRecord();
        broker.name = "证券账户";
        broker.category = "券商";
        broker.institution = "待绑定券商 App";
        broker.amount = "0";
        broker.currency = "CNY";
        broker.updateEveryDays = 1;
        broker.note = "每日交易后打开券商 App 核对持仓并记录快照。";
        assets.add(broker);

        return assets;
    }
}
