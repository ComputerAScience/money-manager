package com.computerascience.moneymanager;

import org.json.JSONException;
import org.json.JSONObject;

final class AssetUpdateEvent {
    final String assetId;
    final String assetName;
    final long timestamp;
    final String currency;
    final String previousAmount;
    final String newAmount;
    final String note;

    AssetUpdateEvent(
            String assetId,
            String assetName,
            long timestamp,
            String currency,
            String previousAmount,
            String newAmount,
            String note
    ) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.timestamp = timestamp;
        this.currency = currency;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.note = note;
    }

    static AssetUpdateEvent fromJson(JSONObject json) {
        return new AssetUpdateEvent(
                json.optString("assetId", ""),
                json.optString("assetName", ""),
                json.optLong("timestamp", 0L),
                json.optString("currency", "CNY"),
                json.optString("previousAmount", ""),
                json.optString("newAmount", ""),
                json.optString("note", "")
        );
    }

    JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("assetId", assetId);
        json.put("assetName", assetName);
        json.put("timestamp", timestamp);
        json.put("currency", currency);
        json.put("previousAmount", previousAmount);
        json.put("newAmount", newAmount);
        json.put("note", note);
        return json;
    }
}
