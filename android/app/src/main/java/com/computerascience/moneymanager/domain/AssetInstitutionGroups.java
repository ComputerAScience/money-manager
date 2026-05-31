package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssetInstitutionGroups {
    public static List<Group> groupByInstitution(List<AssetRecord> assets, PortfolioSettings settings) {
        Map<String, Group> groups = new HashMap<>();
        for (AssetRecord asset : assets) {
            String key = groupKey(asset);
            Group group = groups.get(key);
            if (group == null) {
                group = new Group(key, groupTitle(asset));
                groups.put(key, group);
            }
            group.assets.add(asset);
            group.total += totalInBase(asset, settings);
            if (AssetMath.isStale(asset)) {
                group.staleCount += 1;
            }
            addApp(group, asset);
            addKind(group, asset.category);
        }

        List<Group> result = new ArrayList<>(groups.values());
        Collections.sort(result, (left, right) -> {
            if ((left.staleCount > 0) != (right.staleCount > 0)) {
                return left.staleCount > 0 ? -1 : 1;
            }
            int totalCompare = Double.compare(right.total, left.total);
            if (totalCompare != 0) {
                return totalCompare;
            }
            return left.title.compareToIgnoreCase(right.title);
        });
        return result;
    }

    private static String groupKey(AssetRecord asset) {
        String institution = clean(asset.institution);
        if (institution.isEmpty() || institution.contains("待绑定")) {
            return "institution:unassigned";
        }
        return "institution:" + institution.toLowerCase(Locale.ROOT);
    }

    private static String groupTitle(AssetRecord asset) {
        String institution = clean(asset.institution);
        if (institution.isEmpty() || institution.contains("待绑定")) {
            return "未填写机构";
        }
        return institution;
    }

    private static double totalInBase(AssetRecord asset, PortfolioSettings settings) {
        String currency = AssetMath.cleanCurrency(asset.currency);
        double rate = settings == null || !settings.hasRateFor(currency) ? 1.0 : settings.rateFor(currency);
        return (AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset)) * rate;
    }

    private static void addApp(Group group, AssetRecord asset) {
        String packageName = clean(asset.packageName);
        String launchUri = clean(asset.launchUri);
        String appName = clean(asset.appName);
        if (packageName.isEmpty() && launchUri.isEmpty()) {
            return;
        }
        if (!packageName.isEmpty() && !group.packageNames.contains(packageName)) {
            group.packageNames.add(packageName);
        }
        if (packageName.isEmpty() && !launchUri.isEmpty() && !group.launchUris.contains(launchUri)) {
            group.launchUris.add(launchUri);
        }
        String label = appName.isEmpty() ? "已绑定 App" : appName;
        if (!group.appLabels.contains(label)) {
            group.appLabels.add(label);
        }
    }

    private static void addKind(Group group, String kind) {
        String cleanKind = clean(kind);
        if (!cleanKind.isEmpty() && !group.kinds.contains(cleanKind)) {
            group.kinds.add(cleanKind);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private AssetInstitutionGroups() {
    }

    public static final class Group {
        public final String key;
        public final String title;
        public final List<AssetRecord> assets = new ArrayList<>();
        public final List<String> kinds = new ArrayList<>();
        public final List<String> packageNames = new ArrayList<>();
        public final List<String> launchUris = new ArrayList<>();
        public final List<String> appLabels = new ArrayList<>();
        public double total;
        public int staleCount;

        private Group(String key, String title) {
            this.key = key;
            this.title = title;
        }

        public boolean hasBoundApp() {
            return !packageNames.isEmpty() || !launchUris.isEmpty();
        }

        public String primaryPackageName() {
            return packageNames.isEmpty() ? "" : packageNames.get(0);
        }

        public String displayKinds() {
            if (kinds.isEmpty()) {
                return "类型待完善";
            }
            StringBuilder builder = new StringBuilder();
            for (String kind : kinds) {
                if (builder.length() > 0) {
                    builder.append(" / ");
                }
                builder.append(kind);
            }
            return builder.toString();
        }

        public String displayApps() {
            if (appLabels.isEmpty()) {
                return "未绑定 App";
            }
            if (appLabels.size() == 1) {
                return appLabels.get(0);
            }
            return appLabels.get(0) + " 等 " + appLabels.size() + " 个 App";
        }
    }
}
