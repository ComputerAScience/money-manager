package com.computerascience.moneymanager;

import java.util.List;

final class PortfolioSummary {
    final double netWorth;
    final double grossAssets;
    final double liabilities;
    final int assetCount;
    final int staleCount;
    final int missingBindingCount;
    final String primaryCurrency;
    final boolean hasMixedCurrencies;
    final List<CategoryBreakdown> categories;

    PortfolioSummary(
            double netWorth,
            double grossAssets,
            double liabilities,
            int assetCount,
            int staleCount,
            int missingBindingCount,
            String primaryCurrency,
            boolean hasMixedCurrencies,
            List<CategoryBreakdown> categories
    ) {
        this.netWorth = netWorth;
        this.grossAssets = grossAssets;
        this.liabilities = liabilities;
        this.assetCount = assetCount;
        this.staleCount = staleCount;
        this.missingBindingCount = missingBindingCount;
        this.primaryCurrency = primaryCurrency;
        this.hasMixedCurrencies = hasMixedCurrencies;
        this.categories = categories;
    }
}
