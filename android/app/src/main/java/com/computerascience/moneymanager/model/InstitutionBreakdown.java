package com.computerascience.moneymanager.model;

public final class InstitutionBreakdown {
    public final String institution;
    public final double value;
    public final int assetCount;

    public InstitutionBreakdown(String institution, double value, int assetCount) {
        this.institution = institution;
        this.value = value;
        this.assetCount = assetCount;
    }
}
