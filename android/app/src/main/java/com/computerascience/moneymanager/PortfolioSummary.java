package com.computerascience.moneymanager;

import java.util.List;

final class PortfolioSummary {
    final double netWorth;
    final double grossAssets;
    final double liabilities;
    final int assetCount;
    final int staleCount;
    final int missingBindingCount;
    final int missingRateCount;
    final String baseCurrency;
    final boolean hasMixedCurrencies;
    final List<CategoryBreakdown> categories;

    PortfolioSummary(
            double netWorth,
            double grossAssets,
            double liabilities,
            int assetCount,
            int staleCount,
            int missingBindingCount,
            int missingRateCount,
            String baseCurrency,
            boolean hasMixedCurrencies,
            List<CategoryBreakdown> categories
    ) {
        this.netWorth = netWorth;
        this.grossAssets = grossAssets;
        this.liabilities = liabilities;
        this.assetCount = assetCount;
        this.staleCount = staleCount;
        this.missingBindingCount = missingBindingCount;
        this.missingRateCount = missingRateCount;
        this.baseCurrency = baseCurrency;
        this.hasMixedCurrencies = hasMixedCurrencies;
        this.categories = categories;
    }
}
