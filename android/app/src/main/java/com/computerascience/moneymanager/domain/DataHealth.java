package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.util.ArrayList;
import java.util.List;

public final class DataHealth {
    private DataHealth() {
    }

    public static List<String> portfolioIssues(
            List<AssetRecord> assets,
            PortfolioSettings settings,
            String baseCurrency
    ) {
        List<String> issues = new ArrayList<>();
        int missingAmount = 0;
        int invalidAmount = 0;
        int missingInstitution = 0;
        int missingBinding = 0;
        int missingRate = 0;

        for (AssetRecord asset : assets) {
            if (missingAmount(asset)) {
                missingAmount += 1;
            } else if (invalidAmount(asset)) {
                invalidAmount += 1;
            }
            String institution = clean(asset.institution);
            if (institution.isEmpty() || institution.contains("待绑定")) {
                missingInstitution += 1;
            }
            if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
                missingBinding += 1;
            }
            String currency = AssetMath.cleanCurrency(asset.currency);
            if (!settings.hasRateFor(currency)) {
                missingRate += 1;
            }
        }

        if (assets.isEmpty()) {
            issues.add("还没有资产，请先新增至少一项资产。");
        }
        if (missingAmount > 0) {
            issues.add(missingAmount + " 项资产缺少金额。");
        }
        if (invalidAmount > 0) {
            issues.add(invalidAmount + " 项资产金额无法识别。");
        }
        if (missingInstitution > 0) {
            issues.add(missingInstitution + " 项资产缺少明确机构。");
        }
        if (missingBinding > 0) {
            issues.add(missingBinding + " 项资产还没有绑定 App。");
        }
        if (missingRate > 0) {
            issues.add(missingRate + " 项资产缺少到 " + baseCurrency + " 的汇率。");
        }
        return issues;
    }

    public static List<AssetInstitutionGroups.Group> issueGroups(
            List<AssetRecord> assets,
            PortfolioSettings settings
    ) {
        List<AssetRecord> issueAssets = new ArrayList<>();
        for (AssetRecord asset : assets) {
            if (hasIssue(asset, settings)) {
                issueAssets.add(asset);
            }
        }
        return AssetInstitutionGroups.groupByInstitution(issueAssets, settings);
    }

    public static boolean hasIssue(AssetRecord asset, PortfolioSettings settings) {
        return !assetIssues(asset, settings).isEmpty();
    }

    public static List<String> assetIssues(AssetRecord asset, PortfolioSettings settings) {
        List<String> issues = new ArrayList<>();
        if (missingAmount(asset)) {
            issues.add("缺金额");
        } else if (invalidAmount(asset)) {
            issues.add("金额无法识别");
        }
        String institution = clean(asset.institution);
        if (institution.isEmpty() || institution.contains("待绑定")) {
            issues.add("缺机构");
        }
        if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
            issues.add("未绑定 App");
        }
        String currency = AssetMath.cleanCurrency(asset.currency);
        if (!settings.hasRateFor(currency)) {
            issues.add("缺汇率");
        }
        if (asset.lastUpdatedAt <= 0) {
            issues.add("从未更新");
        } else if (AssetMath.isStale(asset)) {
            issues.add("待更新");
        }
        return issues;
    }

    private static boolean missingAmount(AssetRecord asset) {
        return clean(asset.amount).isEmpty();
    }

    private static boolean invalidAmount(AssetRecord asset) {
        String amount = clean(asset.amount);
        if (amount.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(amount.replace(",", ""));
            return false;
        } catch (NumberFormatException error) {
            return true;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
