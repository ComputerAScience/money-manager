package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UpdateAnalytics {
    private UpdateAnalytics() {
    }

    public static Summary summarize(
            List<AssetUpdateEvent> events,
            List<AssetRecord> assets,
            PortfolioSettings settings,
            int days
    ) {
        return summarize(events, assets, settings, days, true);
    }

    public static Summary summarizeKnownAssets(
            List<AssetUpdateEvent> events,
            List<AssetRecord> assets,
            PortfolioSettings settings,
            int days
    ) {
        return summarize(events, assets, settings, days, false);
    }

    private static Summary summarize(
            List<AssetUpdateEvent> events,
            List<AssetRecord> assets,
            PortfolioSettings settings,
            int days,
            boolean includeUnknownAssets
    ) {
        Summary summary = new Summary(days);
        long cutoff = System.currentTimeMillis() - days * AssetMath.DAY_MS;
        Map<String, AssetRecord> assetById = new HashMap<>();
        for (AssetRecord asset : assets) {
            assetById.put(asset.id, asset);
        }

        Map<String, Bucket> reasons = new HashMap<>();
        Map<String, Bucket> categories = new HashMap<>();
        Map<String, Bucket> institutions = new HashMap<>();
        for (AssetUpdateEvent event : events) {
            if (event.timestamp < cutoff) {
                continue;
            }

            AssetRecord asset = assetById.get(event.assetId);
            if (asset == null && !includeUnknownAssets) {
                continue;
            }
            double delta = deltaInBase(event, settings);
            summary.count += 1;
            summary.delta += delta;
            if (delta > 0.0001) {
                summary.increase += delta;
            } else if (delta < -0.0001) {
                summary.decrease += delta;
            } else {
                summary.flatCount += 1;
            }

            addBucket(reasons, cleanReason(event.reason), delta);
            addBucket(categories, asset == null || asset.category.isEmpty() ? "未知类型" : asset.category, delta);
            addBucket(institutions, asset == null || asset.institution.isEmpty() ? "未填写机构" : asset.institution, delta);
        }

        summary.reasons.addAll(sortedBuckets(reasons));
        summary.categories.addAll(sortedBuckets(categories));
        summary.institutions.addAll(sortedBuckets(institutions));
        return summary;
    }

    private static double deltaInBase(AssetUpdateEvent event, PortfolioSettings settings) {
        String currency = AssetMath.cleanCurrency(event.currency);
        double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
        double previous = AssetMath.parseAmount(event.previousAmount);
        double current = AssetMath.parseAmount(event.newAmount);
        return (current - previous) * rate;
    }

    private static void addBucket(Map<String, Bucket> buckets, String label, double delta) {
        Bucket bucket = buckets.get(label);
        if (bucket == null) {
            bucket = new Bucket(label);
            buckets.put(label, bucket);
        }
        bucket.count += 1;
        bucket.delta += delta;
    }

    private static List<Bucket> sortedBuckets(Map<String, Bucket> buckets) {
        List<Bucket> sorted = new ArrayList<>(buckets.values());
        Collections.sort(sorted, (left, right) -> {
            int amountCompare = Double.compare(Math.abs(right.delta), Math.abs(left.delta));
            if (amountCompare != 0) {
                return amountCompare;
            }
            int countCompare = Integer.compare(right.count, left.count);
            if (countCompare != 0) {
                return countCompare;
            }
            return left.label.compareToIgnoreCase(right.label);
        });
        return sorted;
    }

    private static String cleanReason(String value) {
        return value == null || value.trim().isEmpty() ? "余额核对" : value.trim();
    }

    public static final class Summary {
        public final int days;
        public final List<Bucket> reasons = new ArrayList<>();
        public final List<Bucket> categories = new ArrayList<>();
        public final List<Bucket> institutions = new ArrayList<>();
        public int count;
        public int flatCount;
        public double delta;
        public double increase;
        public double decrease;

        private Summary(int days) {
            this.days = days;
        }
    }

    public static final class Bucket {
        public final String label;
        public int count;
        public double delta;

        private Bucket(String label) {
            this.label = label;
        }
    }
}
