package com.computerascience.moneymanager;

import org.json.JSONException;
import org.json.JSONObject;

final class AssetSnapshot {
    final String dayKey;
    final long timestamp;
    final double netWorth;
    final double grossAssets;
    final double liabilities;

    AssetSnapshot(String dayKey, long timestamp, double netWorth, double grossAssets, double liabilities) {
        this.dayKey = dayKey;
        this.timestamp = timestamp;
        this.netWorth = netWorth;
        this.grossAssets = grossAssets;
        this.liabilities = liabilities;
    }

    static AssetSnapshot fromJson(JSONObject json) {
        return new AssetSnapshot(
                json.optString("dayKey", ""),
                json.optLong("timestamp", 0L),
                json.optDouble("netWorth", 0),
                json.optDouble("grossAssets", 0),
                json.optDouble("liabilities", 0)
        );
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("dayKey", dayKey);
        json.put("timestamp", timestamp);
        json.put("netWorth", netWorth);
        json.put("grossAssets", grossAssets);
        json.put("liabilities", liabilities);
        return json;
    }
}
