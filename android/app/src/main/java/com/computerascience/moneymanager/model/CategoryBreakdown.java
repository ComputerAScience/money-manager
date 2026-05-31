package com.computerascience.moneymanager.model;

public final class CategoryBreakdown {
    public final String category;
    public final double value;
    public final int color;

    public CategoryBreakdown(String category, double value, int color) {
        this.category = category;
        this.value = value;
        this.color = color;
    }
}
