package com.computerascience.moneymanager;

final class InstitutionBreakdown {
    final String institution;
    final double value;
    final int assetCount;

    InstitutionBreakdown(String institution, double value, int assetCount) {
        this.institution = institution;
        this.value = value;
        this.assetCount = assetCount;
    }
}
