package com.computerascience.moneymanager;

import android.view.View;

import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.List;

final class TrendPageRenderer {
    private final TrendSnapshotRenderer snapshotRenderer;
    private final TrendReviewRenderer reviewRenderer;
    private final TargetProgressRenderer targetRenderer;
    private final DistributionTrendRenderer distributionRenderer;
    private final FlowAttributionRenderer flowRenderer;
    private final AssetTrendRenderer assetTrendRenderer;
    private final TrendUpdatesRenderer updatesRenderer;

    TrendPageRenderer(MainActivity activity) {
        this.snapshotRenderer = new TrendSnapshotRenderer(activity);
        this.reviewRenderer = new TrendReviewRenderer(activity);
        this.targetRenderer = new TargetProgressRenderer(activity);
        this.distributionRenderer = new DistributionTrendRenderer(activity);
        this.flowRenderer = new FlowAttributionRenderer(activity);
        this.assetTrendRenderer = new AssetTrendRenderer(activity);
        this.updatesRenderer = new TrendUpdatesRenderer(activity);
    }

    View trendCard() {
        return snapshotRenderer.trendCard();
    }

    View monthlyReviewCard() {
        return reviewRenderer.monthlyReviewCard();
    }

    View targetProgressCard() {
        return targetRenderer.targetProgressCard();
    }

    View distributionTrendCard() {
        return distributionRenderer.card();
    }

    View flowAttributionCard() {
        return flowRenderer.card();
    }

    View assetTrendCard() {
        return assetTrendRenderer.card();
    }

    void render(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        snapshotRenderer.render(portfolio, trendSnapshots);
        reviewRenderer.renderMonthlyReview(portfolio, trendSnapshots);
        targetRenderer.render(portfolio, trendSnapshots);
        distributionRenderer.render(trendSnapshots);
        flowRenderer.render();
        assetTrendRenderer.render();
    }

    String summaryText(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        return snapshotRenderer.summaryText(portfolio, trendSnapshots);
    }

    List<String> reviewLines(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        return snapshotRenderer.reviewLines(portfolio, trendSnapshots);
    }

    String recentUpdateSummaryText() {
        return updatesRenderer.recentUpdateSummaryText();
    }

    List<String> updateReasonSummaryLines(boolean includeEmpty) {
        return updatesRenderer.updateReasonSummaryLines(includeEmpty);
    }
}
