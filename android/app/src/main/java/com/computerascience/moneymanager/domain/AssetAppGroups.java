package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssetAppGroups {
    public static List<Group> groupByApp(
            List<AssetRecord> assets,
            PortfolioSettings settings,
            AppTitleResolver titleResolver
    ) {
        Map<String, Group> groups = new HashMap<>();
        for (AssetRecord asset : assets) {
            String key = groupKey(asset);
            Group group = groups.get(key);
            if (group == null) {
                group = new Group(key, groupTitle(asset, titleResolver));
                groups.put(key, group);
            }
            group.assets.add(asset);
            group.total += totalInBase(asset, settings);
            if (AssetMath.isStale(asset)) {
                group.staleCount += 1;
            }
            if (hasBoundApp(asset)) {
                group.hasBoundApp = true;
            }
            if (group.packageName.isEmpty() && !clean(asset.packageName).isEmpty()) {
                group.packageName = clean(asset.packageName);
            }
            if (group.launchUri.isEmpty() && !clean(asset.launchUri).isEmpty()) {
                group.launchUri = clean(asset.launchUri);
            }
            addKinds(group, asset);
        }

        List<Group> result = new ArrayList<>(groups.values());
        Collections.sort(result, (left, right) -> {
            if ((left.staleCount > 0) != (right.staleCount > 0)) {
                return left.staleCount > 0 ? -1 : 1;
            }
            if (left.hasBoundApp != right.hasBoundApp) {
                return left.hasBoundApp ? 1 : -1;
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
        String packageName = clean(asset.packageName);
        if (!packageName.isEmpty()) {
            return "pkg:" + packageName;
        }
        String launchUri = clean(asset.launchUri);
        if (!launchUri.isEmpty()) {
            return "uri:" + launchUri;
        }
        String institution = clean(asset.institution);
        if (!institution.isEmpty() && !institution.contains("待绑定")) {
            return "inst:" + institution.toLowerCase(Locale.ROOT);
        }
        return "unbound";
    }

    private static String groupTitle(AssetRecord asset, AppTitleResolver titleResolver) {
        if (hasBoundApp(asset)) {
            String title = titleResolver == null ? "" : clean(titleResolver.titleFor(asset));
            if (!title.isEmpty()) {
                return title;
            }
            return "已绑定 App";
        }
        String institution = clean(asset.institution);
        if (!institution.isEmpty() && !institution.contains("待绑定")) {
            return institution;
        }
        return "未绑定 App";
    }

    private static double totalInBase(AssetRecord asset, PortfolioSettings settings) {
        String currency = AssetMath.cleanCurrency(asset.currency);
        double rate = settings == null || !settings.hasRateFor(currency) ? 1.0 : settings.rateFor(currency);
        return (AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset)) * rate;
    }

    private static void addKinds(Group group, AssetRecord asset) {
        if (AssetMath.isBankAccount(asset)) {
            addKind(group, "现金");
            if (!clean(asset.bankWealthAmount).isEmpty()) {
                addKind(group, "理财");
            }
            if (!clean(asset.bankDebtAmount).isEmpty()) {
                addKind(group, "负债");
            }
            return;
        }
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)) {
            addKind(group, "投资");
            if (!clean(asset.investmentCashAmount).isEmpty()) {
                addKind(group, "现金");
            }
            return;
        }
        if (AssetCategories.FUND.equals(asset.category)) {
            addKind(group, "基金");
            return;
        }
        if (AssetCategories.DEBT.equals(asset.category)) {
            addKind(group, "负债");
            return;
        }
        addKind(group, asset.category);
    }

    private static void addKind(Group group, String kind) {
        if (!group.kinds.contains(kind)) {
            group.kinds.add(kind);
        }
    }

    private static boolean hasBoundApp(AssetRecord asset) {
        return !clean(asset.packageName).isEmpty() || !clean(asset.launchUri).isEmpty();
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private AssetAppGroups() {
    }

    public interface AppTitleResolver {
        String titleFor(AssetRecord asset);
    }

    public static final class Group {
        public final String key;
        public final String title;
        public final List<AssetRecord> assets = new ArrayList<>();
        public final List<String> kinds = new ArrayList<>();
        public String packageName = "";
        public String launchUri = "";
        public double total;
        public int staleCount;
        public boolean hasBoundApp;

        private Group(String key, String title) {
            this.key = key;
            this.title = title;
        }

        public String displayKinds() {
            if (kinds.isEmpty()) {
                return "明细待完善";
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
    }
}
