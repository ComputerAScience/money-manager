package com.computerascience.moneymanager.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public final class AssetRecord {
    public final String id;
    public String name;
    public String category;
    public String institution;
    public String amount;
    public String currency;
    public int updateEveryDays;
    public long lastUpdatedAt;
    public String appName;
    public String packageName;
    public String launchUri;
    public String note;

    public AssetRecord() {
        id = UUID.randomUUID().toString();
        name = "";
        category = "银行";
        institution = "";
        amount = "";
        currency = "CNY";
        updateEveryDays = 7;
        lastUpdatedAt = 0L;
        appName = "";
        packageName = "";
        launchUri = "";
        note = "";
    }

    public static AssetRecord fromJson(JSONObject json) throws JSONException {
        AssetRecord asset = new AssetRecord();
        asset.name = json.optString("name", asset.name);
        asset.category = json.optString("category", asset.category);
        asset.institution = json.optString("institution", asset.institution);
        asset.amount = json.optString("amount", asset.amount);
        asset.currency = json.optString("currency", asset.currency);
        asset.updateEveryDays = Math.max(1, json.optInt("updateEveryDays", asset.updateEveryDays));
        asset.lastUpdatedAt = json.optLong("lastUpdatedAt", asset.lastUpdatedAt);
        asset.appName = json.optString("appName", asset.appName);
        asset.packageName = json.optString("packageName", asset.packageName);
        asset.launchUri = json.optString("launchUri", asset.launchUri);
        asset.note = json.optString("note", asset.note);

        String savedId = json.optString("id", asset.id);
        return new AssetRecord(
                savedId,
                asset.name,
                asset.category,
                asset.institution,
                asset.amount,
                asset.currency,
                asset.updateEveryDays,
                asset.lastUpdatedAt,
                asset.appName,
                asset.packageName,
                asset.launchUri,
                asset.note
        );
    }

    public static AssetRecord copyOf(AssetRecord source) {
        return new AssetRecord(
                source.id,
                source.name,
                source.category,
                source.institution,
                source.amount,
                source.currency,
                source.updateEveryDays,
                source.lastUpdatedAt,
                source.appName,
                source.packageName,
                source.launchUri,
                source.note
        );
    }

    private AssetRecord(
            String id,
            String name,
            String category,
            String institution,
            String amount,
            String currency,
            int updateEveryDays,
            long lastUpdatedAt,
            String appName,
            String packageName,
            String launchUri,
            String note
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.institution = institution;
        this.amount = amount;
        this.currency = currency;
        this.updateEveryDays = updateEveryDays;
        this.lastUpdatedAt = lastUpdatedAt;
        this.appName = appName;
        this.packageName = packageName;
        this.launchUri = launchUri;
        this.note = note;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("category", category);
        json.put("institution", institution);
        json.put("amount", amount);
        json.put("currency", currency);
        json.put("updateEveryDays", updateEveryDays);
        json.put("lastUpdatedAt", lastUpdatedAt);
        json.put("appName", appName);
        json.put("packageName", packageName);
        json.put("launchUri", launchUri);
        json.put("note", note);
        return json;
    }
}
