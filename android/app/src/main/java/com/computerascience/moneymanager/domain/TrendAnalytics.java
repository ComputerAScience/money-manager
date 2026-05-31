package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TrendAnalytics {
    private TrendAnalytics() {
    }

    public static List<AssetSnapshot> snapshotsForBase(List<AssetSnapshot> snapshots, String baseCurrency) {
        List<AssetSnapshot> filtered = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (baseCurrency.equals(snapshot.baseCurrency)) {
                filtered.add(snapshot);
            }
        }
        return filtered;
    }

    public static List<Metric> metrics(PortfolioSummary portfolio, List<AssetSnapshot> snapshots, long now) {
        List<Metric> metrics = new ArrayList<>();
        metrics.add(metric(portfolio, snapshots, "近 30 天", 30, now));
        metrics.add(metric(portfolio, snapshots, "近 90 天", 90, now));
        metrics.add(metric(portfolio, snapshots, "近一年", 365, now));
        return metrics;
    }

    private static Metric metric(
            PortfolioSummary portfolio,
            List<AssetSnapshot> snapshots,
            String label,
            int days,
            long now
    ) {
        long cutoff = now - days * AssetMath.DAY_MS;
        List<AssetSnapshot> window = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (snapshot.timestamp >= cutoff) {
                window.add(snapshot);
            }
        }

        if (window.size() < 2) {
            return new Metric(label, window.size(), portfolio.baseCurrency);
        }

        AssetSnapshot first = window.get(0);
        AssetSnapshot last = window.get(window.size() - 1);
        AssetSnapshot high = first;
        AssetSnapshot low = first;
        for (AssetSnapshot snapshot : window) {
            if (snapshot.netWorth > high.netWorth) {
                high = snapshot;
            }
            if (snapshot.netWorth < low.netWorth) {
                low = snapshot;
            }
        }

        return new Metric(
                label,
                window.size(),
                portfolio.baseCurrency,
                true,
                first,
                last,
                high,
                low,
                last.netWorth - first.netWorth
        );
    }

    public static List<AssetSnapshot> snapshotsWithCategoryValues(List<AssetSnapshot> snapshots) {
        List<AssetSnapshot> available = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (!snapshot.categoryValues.isEmpty()) {
                available.add(snapshot);
            }
        }
        return available;
    }

    public static List<CategoryShift> categoryShifts(AssetSnapshot first, AssetSnapshot last) {
        Set<String> categories = new HashSet<>();
        categories.addAll(first.categoryValues.keySet());
        categories.addAll(last.categoryValues.keySet());

        double firstTotal = categoryTotal(first);
        double lastTotal = categoryTotal(last);
        List<CategoryShift> shifts = new ArrayList<>();
        for (String category : categories) {
            double firstValue = categoryValue(first, category);
            double lastValue = categoryValue(last, category);
            double firstPercent = firstTotal <= 0 ? 0 : firstValue / firstTotal * 100;
            double lastPercent = lastTotal <= 0 ? 0 : lastValue / lastTotal * 100;
            shifts.add(new CategoryShift(
                    category,
                    firstValue,
                    lastValue,
                    lastValue - firstValue,
                    firstPercent,
                    lastPercent,
                    lastPercent - firstPercent
            ));
        }
        Collections.sort(shifts, (left, right) -> Double.compare(
                Math.abs(right.delta) + Math.abs(right.percentDelta),
                Math.abs(left.delta) + Math.abs(left.percentDelta)
        ));
        return shifts;
    }

    private static double categoryTotal(AssetSnapshot snapshot) {
        double total = 0;
        for (double value : snapshot.categoryValues.values()) {
            total += value;
        }
        return total;
    }

    private static double categoryValue(AssetSnapshot snapshot, String category) {
        Double value = snapshot.categoryValues.get(category);
        return value == null ? 0 : value;
    }

    public static final class Metric {
        public final String label;
        public final int count;
        public final String currency;
        public final boolean complete;
        public final AssetSnapshot first;
        public final AssetSnapshot last;
        public final AssetSnapshot high;
        public final AssetSnapshot low;
        public final double change;

        Metric(String label, int count, String currency) {
            this(label, count, currency, false, null, null, null, null, 0);
        }

        Metric(
                String label,
                int count,
                String currency,
                boolean complete,
                AssetSnapshot first,
                AssetSnapshot last,
                AssetSnapshot high,
                AssetSnapshot low,
                double change
        ) {
            this.label = label;
            this.count = count;
            this.currency = currency;
            this.complete = complete;
            this.first = first;
            this.last = last;
            this.high = high;
            this.low = low;
            this.change = change;
        }
    }

    public static final class CategoryShift {
        public final String category;
        public final double firstValue;
        public final double lastValue;
        public final double delta;
        public final double firstPercent;
        public final double lastPercent;
        public final double percentDelta;

        CategoryShift(
                String category,
                double firstValue,
                double lastValue,
                double delta,
                double firstPercent,
                double lastPercent,
                double percentDelta
        ) {
            this.category = category;
            this.firstValue = firstValue;
            this.lastValue = lastValue;
            this.delta = delta;
            this.firstPercent = firstPercent;
            this.lastPercent = lastPercent;
            this.percentDelta = percentDelta;
        }
    }
}
