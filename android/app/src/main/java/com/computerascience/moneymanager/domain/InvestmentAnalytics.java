package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InvestmentAnalytics {
    private InvestmentAnalytics() {
    }

    public static Summary summarize(List<AssetRecord> investments, PortfolioSettings settings) {
        Summary summary = new Summary();
        Map<String, Double> categoryTotals = new HashMap<>();
        long now = System.currentTimeMillis();

        for (AssetRecord asset : investments) {
            double gross = amountInBase(asset, settings, AssetMath.assetGrossAmount(asset));
            summary.total += gross;
            summary.holding += amountInBase(asset, settings, holdingAmount(asset));
            summary.cash += amountInBase(asset, settings, cashAmount(asset));
            categoryTotals.put(asset.category, doubleValue(categoryTotals, asset.category) + gross);
            if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
                summary.unboundAppCount += 1;
            }
            if (asset.institution.isEmpty()) {
                summary.missingInstitutionCount += 1;
            }

            int days = AssetMath.daysUntilDue(asset, now);
            if (days <= 0) {
                summary.dueNow += 1;
            } else if (days <= 3) {
                summary.dueSoon += 1;
            }
        }

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() > 0) {
                summary.categories.add(new CategoryBreakdown(
                        entry.getKey(),
                        entry.getValue(),
                        AssetMath.colorForCategory(entry.getKey())
                ));
            }
        }
        Collections.sort(summary.categories, (left, right) -> Double.compare(right.value, left.value));
        return summary;
    }

    private static double holdingAmount(AssetRecord asset) {
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)) {
            return AssetMath.hasInvestmentBreakdown(asset)
                    ? AssetMath.investmentHoldingAmount(asset)
                    : AssetMath.assetGrossAmount(asset);
        }
        if (AssetCategories.BROKER_CASH.equals(asset.category)) {
            return 0;
        }
        return AssetMath.assetGrossAmount(asset);
    }

    private static double cashAmount(AssetRecord asset) {
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)) {
            return AssetMath.investmentCashAmount(asset);
        }
        if (AssetCategories.BROKER_CASH.equals(asset.category)) {
            return AssetMath.assetGrossAmount(asset);
        }
        return 0;
    }

    private static double amountInBase(AssetRecord asset, PortfolioSettings settings, double amount) {
        String currency = AssetMath.cleanCurrency(asset.currency);
        double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
        return amount * rate;
    }

    private static double doubleValue(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? 0.0 : value;
    }

    public static final class Summary {
        public final List<CategoryBreakdown> categories = new ArrayList<>();
        public double total;
        public double holding;
        public double cash;
        public int dueNow;
        public int dueSoon;
        public int unboundAppCount;
        public int missingInstitutionCount;

        public double cashRatio() {
            return total <= 0 ? 0 : cash / total * 100;
        }

        public double holdingRatio() {
            return total <= 0 ? 0 : holding / total * 100;
        }
    }
}
