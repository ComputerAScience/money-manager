package com.computerascience.moneymanager.model;

import java.util.List;

public final class PortfolioSummary {
    public final double netWorth;
    public final double grossAssets;
    public final double liabilities;
    public final int assetCount;
    public final int staleCount;
    public final int missingBindingCount;
    public final int missingRateCount;
    public final String baseCurrency;
    public final boolean hasMixedCurrencies;
    public final List<CategoryBreakdown> categories;
    public final List<InstitutionBreakdown> institutions;

    public PortfolioSummary(
            double netWorth,
            double grossAssets,
            double liabilities,
            int assetCount,
            int staleCount,
            int missingBindingCount,
            int missingRateCount,
            String baseCurrency,
            boolean hasMixedCurrencies,
            List<CategoryBreakdown> categories,
            List<InstitutionBreakdown> institutions
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
        this.institutions = institutions;
    }
}
