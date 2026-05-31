package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class AssetFilters {
    private AssetFilters() {
    }

    public static List<AssetRecord> visibleAssets(
            List<AssetRecord> assets,
            PortfolioSettings settings,
            String searchQuery,
            String filterMode,
            AppNameResolver appNameResolver
    ) {
        List<AssetRecord> visible = new ArrayList<>();
        for (AssetRecord asset : assets) {
            if (matchesQuery(asset, searchQuery, appNameResolver) && matchesFilter(asset, settings, filterMode)) {
                visible.add(asset);
            }
        }
        Collections.sort(visible, (left, right) -> {
            int priorityCompare = Integer.compare(priority(left, settings), priority(right, settings));
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            int amountCompare = Double.compare(magnitude(right, settings), magnitude(left, settings));
            if (amountCompare != 0) {
                return amountCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return visible;
    }

    public static String label(String filterMode) {
        if ("stale".equals(filterMode)) {
            return "待更新";
        }
        if ("issues".equals(filterMode)) {
            return "待完善";
        }
        if ("unbound".equals(filterMode)) {
            return "未绑定";
        }
        if ("debt".equals(filterMode)) {
            return "负债";
        }
        return "全部";
    }

    private static boolean matchesQuery(AssetRecord asset, String searchQuery, AppNameResolver appNameResolver) {
        String query = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        return asset.name.toLowerCase(Locale.ROOT).contains(query)
                || asset.category.toLowerCase(Locale.ROOT).contains(query)
                || asset.institution.toLowerCase(Locale.ROOT).contains(query)
                || appNameResolver.appName(asset).toLowerCase(Locale.ROOT).contains(query)
                || asset.currency.toLowerCase(Locale.ROOT).contains(query)
                || asset.note.toLowerCase(Locale.ROOT).contains(query);
    }

    private static boolean matchesFilter(AssetRecord asset, PortfolioSettings settings, String filterMode) {
        if ("stale".equals(filterMode)) {
            return AssetMath.isStale(asset);
        }
        if ("unbound".equals(filterMode)) {
            return asset.packageName.isEmpty() && asset.launchUri.isEmpty();
        }
        if ("debt".equals(filterMode)) {
            return AssetMath.assetLiabilityAmount(asset) > 0;
        }
        if ("issues".equals(filterMode)) {
            return DataHealth.hasIssue(asset, settings);
        }
        return true;
    }

    private static int priority(AssetRecord asset, PortfolioSettings settings) {
        if (AssetMath.isStale(asset)) {
            return 0;
        }
        if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
            return 1;
        }
        return 2;
    }

    private static double magnitude(AssetRecord asset, PortfolioSettings settings) {
        return AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset);
    }

    public interface AppNameResolver {
        String appName(AssetRecord asset);
    }
}
