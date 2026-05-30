package com.computerascience.moneymanager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

final class AssetSnapshot {
    final String dayKey;
    final long timestamp;
    final String baseCurrency;
    final double netWorth;
    final double grossAssets;
    final double liabilities;
    final Map<String, Double> categoryValues;

    AssetSnapshot(String dayKey, long timestamp, String baseCurrency, double netWorth, double grossAssets, double liabilities) {
        this(dayKey, timestamp, baseCurrency, netWorth, grossAssets, liabilities, new HashMap<>());
    }

    AssetSnapshot(
            String dayKey,
            long timestamp,
            String baseCurrency,
            double netWorth,
            double grossAssets,
            double liabilities,
            Map<String, Double> categoryValues
    ) {
        this.dayKey = dayKey;
        this.timestamp = timestamp;
        this.baseCurrency = baseCurrency;
        this.netWorth = netWorth;
        this.grossAssets = grossAssets;
        this.liabilities = liabilities;
        this.categoryValues = cleanCategoryValues(categoryValues);
    }

    static AssetSnapshot fromJson(JSONObject json) {
        JSONObject categoryJson = json.optJSONObject("categoryValues");
        if (categoryJson == null) {
            categoryJson = json.optJSONObject("categories");
        }
        return new AssetSnapshot(
                json.optString("dayKey", ""),
                json.optLong("timestamp", 0L),
                json.optString("baseCurrency", "CNY"),
                json.optDouble("netWorth", 0),
                json.optDouble("grossAssets", 0),
                json.optDouble("liabilities", 0),
                readCategoryValues(categoryJson)
        );
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("dayKey", dayKey);
        json.put("timestamp", timestamp);
        json.put("baseCurrency", baseCurrency);
        json.put("netWorth", netWorth);
        json.put("grossAssets", grossAssets);
        json.put("liabilities", liabilities);
        JSONObject categoryJson = new JSONObject();
        for (Map.Entry<String, Double> entry : categoryValues.entrySet()) {
            categoryJson.put(entry.getKey(), entry.getValue());
        }
        json.put("categoryValues", categoryJson);
        return json;
    }

    private static Map<String, Double> readCategoryValues(JSONObject json) {
        Map<String, Double> values = new HashMap<>();
        if (json == null) {
            return values;
        }
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            double value = json.optDouble(key, 0);
            if (key != null && !key.trim().isEmpty() && value > 0) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static Map<String, Double> cleanCategoryValues(Map<String, Double> source) {
        Map<String, Double> values = new HashMap<>();
        if (source == null) {
            return values;
        }
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            Double value = entry.getValue();
            if (!key.isEmpty() && value != null && value > 0) {
                values.put(key, value);
            }
        }
        return values;
    }
}
