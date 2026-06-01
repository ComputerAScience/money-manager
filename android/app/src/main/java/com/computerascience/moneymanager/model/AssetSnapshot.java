package com.computerascience.moneymanager.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AssetSnapshot {
    public final String dayKey;
    public final long timestamp;
    public final String baseCurrency;
    public final double netWorth;
    public final double grossAssets;
    public final double liabilities;
    public final Map<String, Double> categoryValues;
    public final Map<String, Double> institutionValues;

    public AssetSnapshot(String dayKey, long timestamp, String baseCurrency, double netWorth, double grossAssets, double liabilities) {
        this(dayKey, timestamp, baseCurrency, netWorth, grossAssets, liabilities, new HashMap<>());
    }

    public AssetSnapshot(
            String dayKey,
            long timestamp,
            String baseCurrency,
            double netWorth,
            double grossAssets,
            double liabilities,
            Map<String, Double> categoryValues
    ) {
        this(dayKey, timestamp, baseCurrency, netWorth, grossAssets, liabilities, categoryValues, new HashMap<>());
    }

    public AssetSnapshot(
            String dayKey,
            long timestamp,
            String baseCurrency,
            double netWorth,
            double grossAssets,
            double liabilities,
            Map<String, Double> categoryValues,
            Map<String, Double> institutionValues
    ) {
        this.dayKey = dayKey;
        this.timestamp = timestamp;
        this.baseCurrency = baseCurrency;
        this.netWorth = netWorth;
        this.grossAssets = grossAssets;
        this.liabilities = liabilities;
        this.categoryValues = cleanValues(categoryValues);
        this.institutionValues = cleanValues(institutionValues);
    }

    public static AssetSnapshot fromJson(JSONObject json) {
        JSONObject categoryJson = json.optJSONObject("categoryValues");
        if (categoryJson == null) {
            categoryJson = json.optJSONObject("categories");
        }
        JSONObject institutionJson = json.optJSONObject("institutionValues");
        if (institutionJson == null) {
            institutionJson = json.optJSONObject("institutions");
        }
        return new AssetSnapshot(
                json.optString("dayKey", ""),
                json.optLong("timestamp", 0L),
                json.optString("baseCurrency", "CNY"),
                json.optDouble("netWorth", 0),
                json.optDouble("grossAssets", 0),
                json.optDouble("liabilities", 0),
                readValues(categoryJson),
                readValues(institutionJson)
        );
    }

    public JSONObject toJson() throws JSONException {
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
        JSONObject institutionJson = new JSONObject();
        for (Map.Entry<String, Double> entry : institutionValues.entrySet()) {
            institutionJson.put(entry.getKey(), entry.getValue());
        }
        json.put("institutionValues", institutionJson);
        return json;
    }

    private static Map<String, Double> readValues(JSONObject json) {
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

    private static Map<String, Double> cleanValues(Map<String, Double> source) {
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
