package com.computerascience.moneymanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AssetMath {
    static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private AssetMath() {
    }

    static PortfolioSummary summarize(List<AssetRecord> assets, PortfolioSettings settings) {
        double grossAssets = 0;
        double liabilities = 0;
        int staleCount = 0;
        int missingBindingCount = 0;
        int missingRateCount = 0;
        String baseCurrency = settings.baseCurrency;
        boolean hasMixedCurrencies = false;
        Map<String, Double> categoryTotals = new HashMap<>();

        for (AssetRecord asset : assets) {
            String currency = cleanCurrency(asset.currency);
            if (!currency.isEmpty() && !currency.equals(baseCurrency)) {
                hasMixedCurrencies = true;
            }
            double rate = settings.rateFor(currency);
            if (!settings.hasRateFor(currency)) {
                missingRateCount += 1;
                rate = 1.0;
            }
            double amount = Math.abs(parseAmount(asset.amount)) * rate;
            boolean liability = isLiability(asset);
            if (liability) {
                liabilities += amount;
            } else {
                grossAssets += amount;
            }
            categoryTotals.put(asset.category, categoryTotals.getOrDefault(asset.category, 0.0) + amount);

            if (isStale(asset)) {
                staleCount += 1;
            }
            if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
                missingBindingCount += 1;
            }
        }

        List<CategoryBreakdown> categories = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            categories.add(new CategoryBreakdown(entry.getKey(), entry.getValue(), colorForCategory(entry.getKey())));
        }
        Collections.sort(categories, (left, right) -> Double.compare(right.value, left.value));

        return new PortfolioSummary(
                grossAssets - liabilities,
                grossAssets,
                liabilities,
                assets.size(),
                staleCount,
                missingBindingCount,
                missingRateCount,
                baseCurrency,
                hasMixedCurrencies,
                categories
        );
    }

    static boolean isStale(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return true;
        }
        long days = (System.currentTimeMillis() - asset.lastUpdatedAt) / DAY_MS;
        return days >= asset.updateEveryDays;
    }

    static boolean isLiability(AssetRecord asset) {
        return "负债".equals(asset.category);
    }

    static double parseAmount(String raw) {
        if (raw == null) {
            return 0;
        }
        String cleaned = raw
                .replace(",", "")
                .replace("¥", "")
                .replace("$", "")
                .replace("€", "")
                .replace("￥", "")
                .trim();
        if (cleaned.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    static String cleanCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }

    static int colorForCategory(String category) {
        if ("银行".equals(category)) return 0xFF126B5F;
        if ("券商".equals(category)) return 0xFF315F9F;
        if ("基金".equals(category)) return 0xFF3B7F91;
        if ("加密资产".equals(category)) return 0xFFB85C2F;
        if ("房产".equals(category)) return 0xFF72518A;
        if ("负债".equals(category)) return 0xFFB74955;
        return 0xFF9A7720;
    }
}
