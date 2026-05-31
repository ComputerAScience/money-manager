package com.computerascience.moneymanager.domain;

import android.util.Base64;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.InstitutionBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public final class SharePayloadBuilder {
    private SharePayloadBuilder() {
    }

    public static String buildLink(
            String pageUrl,
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            PortfolioSettings settings
    ) throws IOException, JSONException {
        JSONObject payload = payload(assets, snapshots, settings);
        byte[] compressed = gzip(payload.toString().getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.encodeToString(
                compressed,
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
        );
        return pageUrl + "#data=" + encoded;
    }

    private static JSONObject payload(
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            PortfolioSettings settings
    ) throws JSONException {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        boolean includeAmounts = !settings.hideAmounts;

        JSONObject root = new JSONObject();
        root.put("schema", "money-manager-share-v1");
        root.put("generatedAt", System.currentTimeMillis());
        root.put("baseCurrency", portfolio.baseCurrency);
        root.put("includeAmounts", includeAmounts);
        root.put("assetCount", portfolio.assetCount);
        root.put("staleCount", portfolio.staleCount);

        JSONObject totals = new JSONObject();
        if (includeAmounts) {
            totals.put("netWorth", portfolio.netWorth);
            totals.put("grossAssets", portfolio.grossAssets);
            totals.put("liabilities", portfolio.liabilities);
        }
        root.put("totals", totals);
        root.put("categories", categories(portfolio, includeAmounts));
        root.put("institutions", institutions(portfolio, includeAmounts));
        root.put("assets", assets(assets, settings, includeAmounts));
        root.put("snapshots", snapshots(snapshots, includeAmounts));
        return root;
    }

    private static JSONArray categories(PortfolioSummary portfolio, boolean includeAmounts) throws JSONException {
        JSONArray array = new JSONArray();
        for (CategoryBreakdown category : portfolio.categories) {
            JSONObject item = new JSONObject();
            item.put("name", category.category);
            item.put("color", category.color);
            if (includeAmounts) {
                item.put("value", category.value);
            }
            array.put(item);
        }
        return array;
    }

    private static JSONArray institutions(PortfolioSummary portfolio, boolean includeAmounts) throws JSONException {
        JSONArray array = new JSONArray();
        for (InstitutionBreakdown institution : portfolio.institutions) {
            JSONObject item = new JSONObject();
            item.put("name", institution.institution);
            item.put("assetCount", institution.assetCount);
            if (includeAmounts) {
                item.put("value", institution.value);
            }
            array.put(item);
        }
        return array;
    }

    private static JSONArray assets(
            List<AssetRecord> assets,
            PortfolioSettings settings,
            boolean includeAmounts
    ) throws JSONException {
        List<AssetRecord> sortedAssets = new ArrayList<>(assets);
        Collections.sort(sortedAssets, (left, right) -> Double.compare(
                assetMagnitude(right, settings),
                assetMagnitude(left, settings)
        ));

        JSONArray array = new JSONArray();
        for (AssetRecord asset : sortedAssets) {
            JSONObject item = new JSONObject();
            item.put("name", asset.name);
            item.put("category", asset.category);
            item.put("institution", AssetMath.cleanInstitution(asset.institution));
            item.put("currency", AssetMath.cleanCurrency(asset.currency));
            item.put("lastUpdatedAt", asset.lastUpdatedAt);
            item.put("stale", AssetMath.isStale(asset));
            if (includeAmounts) {
                String currency = AssetMath.cleanCurrency(asset.currency);
                double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
                double grossBase = AssetMath.assetGrossAmount(asset) * rate;
                double liabilityBase = AssetMath.assetLiabilityAmount(asset) * rate;
                item.put("grossBase", grossBase);
                item.put("liabilityBase", liabilityBase);
                item.put("netBase", grossBase - liabilityBase);
            }
            array.put(item);
        }
        return array;
    }

    private static double assetMagnitude(AssetRecord asset, PortfolioSettings settings) {
        String currency = AssetMath.cleanCurrency(asset.currency);
        double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
        return (AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset)) * rate;
    }

    private static JSONArray snapshots(List<AssetSnapshot> snapshots, boolean includeAmounts) throws JSONException {
        JSONArray array = new JSONArray();
        if (!includeAmounts) {
            return array;
        }
        int start = Math.max(0, snapshots.size() - 24);
        for (int index = start; index < snapshots.size(); index += 1) {
            AssetSnapshot snapshot = snapshots.get(index);
            JSONObject item = new JSONObject();
            item.put("dayKey", snapshot.dayKey);
            item.put("timestamp", snapshot.timestamp);
            item.put("netWorth", snapshot.netWorth);
            item.put("grossAssets", snapshot.grossAssets);
            item.put("liabilities", snapshot.liabilities);
            array.put(item);
        }
        return array;
    }

    private static byte[] gzip(byte[] source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(source);
        }
        return output.toByteArray();
    }
}
