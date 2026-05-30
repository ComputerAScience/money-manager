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
    final String reason;
    final String note;

    AssetUpdateEvent(
            String assetId,
            String assetName,
            long timestamp,
            String currency,
            String previousAmount,
            String newAmount,
            String reason,
            String note
    ) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.timestamp = timestamp;
        this.currency = currency;
        this.previousAmount = previousAmount;
        this.newAmount = newAmount;
        this.reason = cleanReason(reason);
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
                json.optString("reason", "余额核对"),
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
        json.put("reason", reason);
        json.put("note", note);
        return json;
    }

    private static String cleanReason(String value) {
        return value == null || value.trim().isEmpty() ? "余额核对" : value.trim();
    }
}
