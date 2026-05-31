package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AllocationAnalytics {
    private AllocationAnalytics() {
    }

    public static List<Drift> drifts(PortfolioSummary portfolio, PortfolioSettings settings) {
        double total = portfolio.grossAssets + portfolio.liabilities;
        Map<String, Double> currentValues = new HashMap<>();
        Set<String> categories = new HashSet<>();
        for (CategoryBreakdown category : portfolio.categories) {
            currentValues.put(category.category, category.value);
            categories.add(category.category);
        }
        for (Map.Entry<String, Double> target : settings.allocationTargets.entrySet()) {
            if (target.getValue() > 0) {
                categories.add(target.getKey());
            }
        }

        List<Drift> drifts = new ArrayList<>();
        for (String category : categories) {
            double currentValue = doubleValue(currentValues, category);
            double currentPercent = total <= 0 ? 0 : currentValue / total * 100;
            double targetPercent = settings.targetForCategory(category);
            if (currentPercent <= 0 && targetPercent <= 0) {
                continue;
            }
            drifts.add(new Drift(
                    category,
                    currentPercent,
                    targetPercent,
                    total * targetPercent / 100 - currentValue,
                    AssetMath.colorForCategory(category)
            ));
        }

        Collections.sort(drifts, (left, right) -> {
            int driftCompare = Double.compare(
                    Math.abs(right.currentPercent - right.targetPercent),
                    Math.abs(left.currentPercent - left.targetPercent)
            );
            if (driftCompare != 0) {
                return driftCompare;
            }
            return left.category.compareToIgnoreCase(right.category);
        });
        return drifts;
    }

    private static double doubleValue(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? 0.0 : value;
    }

    public static final class Drift {
        public final String category;
        public final double currentPercent;
        public final double targetPercent;
        public final double amountDelta;
        public final int color;

        Drift(
                String category,
                double currentPercent,
                double targetPercent,
                double amountDelta,
                int color
        ) {
            this.category = category;
            this.currentPercent = currentPercent;
            this.targetPercent = targetPercent;
            this.amountDelta = amountDelta;
            this.color = color;
        }
    }
}
