package com.computerascience.moneymanager;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

final class AssetStore {
    private static final String PREFS = "money_manager_assets";
    private static final String KEY_ASSETS = "assets";

    private final SharedPreferences preferences;

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

    private List<AssetRecord> defaultAssets() {
        List<AssetRecord> assets = new ArrayList<>();

        AssetRecord bank = new AssetRecord();
        bank.name = "银行卡余额";
        bank.category = "银行";
        bank.institution = "待绑定银行 App";
        bank.amount = "0";
        bank.currency = "CNY";
        bank.updateEveryDays = 7;
        bank.note = "填入银行 App 包名后，可一键打开更新。";
        assets.add(bank);

        AssetRecord broker = new AssetRecord();
        broker.name = "证券账户";
        broker.category = "券商";
        broker.institution = "待绑定券商 App";
        broker.amount = "0";
        broker.currency = "CNY";
        broker.updateEveryDays = 1;
        broker.note = "每日交易后打开券商 App 核对持仓。";
        assets.add(broker);

        return assets;
    }
}
