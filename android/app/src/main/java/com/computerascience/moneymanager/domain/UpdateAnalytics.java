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
            double delta = deltaInBase(event, asset, settings);
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

    public static Breakdown breakdown(
            List<AssetUpdateEvent> events,
            List<AssetRecord> assets,
            PortfolioSettings settings,
            int days
    ) {
        return breakdown(events, assets, settings, days, true);
    }

    public static Breakdown breakdownKnownAssets(
            List<AssetUpdateEvent> events,
            List<AssetRecord> assets,
            PortfolioSettings settings,
            int days
    ) {
        return breakdown(events, assets, settings, days, false);
    }

    private static Breakdown breakdown(
            List<AssetUpdateEvent> events,
            List<AssetRecord> assets,
            PortfolioSettings settings,
            int days,
            boolean includeUnknownAssets
    ) {
        Breakdown breakdown = new Breakdown(days);
        long cutoff = System.currentTimeMillis() - days * AssetMath.DAY_MS;
        Map<String, AssetRecord> assetById = new HashMap<>();
        for (AssetRecord asset : assets) {
            assetById.put(asset.id, asset);
        }

        for (AssetUpdateEvent event : events) {
            if (event.timestamp < cutoff) {
                continue;
            }

            AssetRecord asset = assetById.get(event.assetId);
            if (asset == null && !includeUnknownAssets) {
                continue;
            }

            double delta = deltaInBase(event, asset, settings);
            String reason = cleanReason(event.reason);
            breakdown.count += 1;
            if (isDebtChange(asset, reason)) {
                breakdown.debtCount += 1;
                breakdown.debt += delta;
            } else if (isExternalFlowReason(reason)) {
                breakdown.externalFlowCount += 1;
                breakdown.externalFlow += delta;
                if (delta > 0) {
                    breakdown.externalInflow += delta;
                } else if (delta < 0) {
                    breakdown.externalOutflow += delta;
                }
            } else if (isPerformanceReason(reason)) {
                breakdown.performanceCount += 1;
                breakdown.performance += delta;
            } else if (isTradeReason(reason)) {
                breakdown.tradeCount += 1;
                breakdown.trade += delta;
            } else {
                breakdown.reconcileCount += 1;
                breakdown.reconcile += delta;
            }
        }
        return breakdown;
    }

    private static double deltaInBase(AssetUpdateEvent event, AssetRecord asset, PortfolioSettings settings) {
        String currency = AssetMath.cleanCurrency(event.currency);
        double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
        double previous = AssetMath.parseAmount(event.previousAmount);
        double current = AssetMath.parseAmount(event.newAmount);
        double rawDelta = (current - previous) * rate;
        return AssetMath.isLiability(asset) ? -rawDelta : rawDelta;
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

    private static boolean isDebtChange(AssetRecord asset, String reason) {
        return AssetMath.isLiability(asset) || reason.contains("负债变化");
    }

    private static boolean isExternalFlowReason(String reason) {
        return reason.contains("入金") || reason.contains("出金") || reason.contains("转账");
    }

    private static boolean isPerformanceReason(String reason) {
        return reason.contains("市场涨跌")
                || reason.contains("利息分红")
                || reason.contains("手续费税费");
    }

    private static boolean isTradeReason(String reason) {
        return reason.contains("买入卖出");
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

    public static final class Breakdown {
        public final int days;
        public int count;
        public int performanceCount;
        public int externalFlowCount;
        public int debtCount;
        public int tradeCount;
        public int reconcileCount;
        public double performance;
        public double externalFlow;
        public double externalInflow;
        public double externalOutflow;
        public double debt;
        public double trade;
        public double reconcile;

        private Breakdown(int days) {
            this.days = days;
        }
    }
}
