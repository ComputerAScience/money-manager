package com.computerascience.moneymanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.computerascience.moneymanager.domain.AllocationAnalytics;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.InstitutionBreakdown;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class OverviewSummarySharer {
    private final MainActivity activity;
    private final OverviewPageRenderer overviewRenderer;
    private final TrendPageRenderer trendRenderer;

    OverviewSummarySharer(
            MainActivity activity,
            OverviewPageRenderer overviewRenderer,
            TrendPageRenderer trendRenderer
    ) {
        this.activity = activity;
        this.overviewRenderer = overviewRenderer;
        this.trendRenderer = trendRenderer;
    }

    void copyAssetSummary() {
        PortfolioSummary portfolio = AssetMath.summarize(activity.assets, activity.settings);
        List<AssetSnapshot> trendSnapshots = TrendAnalytics.snapshotsForBase(activity.snapshots, portfolio.baseCurrency);
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            activity.toast("无法访问剪贴板。");
            return;
        }

        clipboard.setPrimaryClip(ClipData.newPlainText("Money Manager 资产摘要", buildAssetSummary(portfolio, trendSnapshots)));
        activity.toast("资产摘要已复制。");
    }

    private String buildAssetSummary(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        List<String> lines = new ArrayList<>();
        lines.add("Money Manager 资产摘要");
        lines.add("生成时间：" + activity.dateFormat.format(new Date()));
        lines.add("基准币种：" + portfolio.baseCurrency);
        lines.add("");
        lines.add("净资产：" + activity.formatMoney(portfolio.netWorth, portfolio.baseCurrency));
        lines.add("资产总额：" + activity.formatMoney(portfolio.grossAssets, portfolio.baseCurrency));
        lines.add("负债：" + activity.formatMoney(portfolio.liabilities, portfolio.baseCurrency));
        lines.add("更新状态：" + portfolio.staleCount + " 项待更新 / 共 " + portfolio.assetCount + " 项");
        if (activity.settings.hasNetWorthTarget()) {
            lines.add("年度目标：" + overviewRenderer.netWorthGoalText(portfolio));
        }
        addTrendSummary(lines, portfolio, trendSnapshots);
        addCategorySummary(lines, portfolio);
        addAllocationTargetSummary(lines, portfolio);
        addInstitutionSummary(lines, portfolio);
        addUpdateSummary(lines);
        return activity.joinLines(lines);
    }

    private void addTrendSummary(
            List<String> lines,
            PortfolioSummary portfolio,
            List<AssetSnapshot> trendSnapshots
    ) {
        lines.add("");
        lines.add("一年趋势：" + trendRenderer.summaryText(portfolio, trendSnapshots));
        List<String> trendReview = trendRenderer.reviewLines(portfolio, trendSnapshots);
        if (!trendReview.isEmpty()) {
            lines.add("趋势复盘：");
            for (String line : trendReview) {
                lines.add("- " + line);
            }
        }
    }

    private void addCategorySummary(List<String> lines, PortfolioSummary portfolio) {
        if (portfolio.categories.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add("资产比例 Top 3");
        double total = portfolio.grossAssets + portfolio.liabilities;
        int limit = Math.min(3, portfolio.categories.size());
        for (int index = 0; index < limit; index += 1) {
            CategoryBreakdown category = portfolio.categories.get(index);
            lines.add("- " + category.category + "："
                    + activity.formatMoney(category.value, portfolio.baseCurrency)
                    + "，" + activity.formatPercent(category.value, total));
        }
    }

    private void addAllocationTargetSummary(List<String> lines, PortfolioSummary portfolio) {
        if (!activity.settings.hasAllocationTargets()) {
            return;
        }
        List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, activity.settings);
        if (drifts.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add("目标比例提醒");
        int limit = Math.min(3, drifts.size());
        for (int index = 0; index < limit; index += 1) {
            lines.add(allocationTargetLine(drifts.get(index), portfolio));
        }
    }

    private String allocationTargetLine(AllocationAnalytics.Drift drift, PortfolioSummary portfolio) {
        double gap = drift.targetPercent - drift.currentPercent;
        String status = Math.abs(gap) < 0.5 ? "接近目标" : (gap > 0 ? "低配" : "超配");
        String line = "- " + drift.category + "：当前 "
                + activity.formatPercentValue(drift.currentPercent)
                + "，目标 " + activity.formatPercentValue(drift.targetPercent)
                + "，" + status + " " + activity.formatPercentValue(Math.abs(gap));
        if (!activity.settings.hideAmounts && Math.abs(gap) >= 0.5) {
            line += "，建议" + (drift.amountDelta > 0 ? "增加 " : "减少 ")
                    + activity.formatMoney(Math.abs(drift.amountDelta), portfolio.baseCurrency);
        }
        return line;
    }

    private void addInstitutionSummary(List<String> lines, PortfolioSummary portfolio) {
        if (portfolio.institutions.isEmpty()) {
            return;
        }
        lines.add("");
        lines.add("机构分布 Top 3");
        double total = portfolio.grossAssets + portfolio.liabilities;
        int limit = Math.min(3, portfolio.institutions.size());
        for (int index = 0; index < limit; index += 1) {
            InstitutionBreakdown institution = portfolio.institutions.get(index);
            lines.add("- " + institution.institution + "："
                    + activity.formatMoney(institution.value, portfolio.baseCurrency)
                    + "，" + activity.formatPercent(institution.value, total)
                    + "，" + institution.assetCount + " 项");
        }
    }

    private void addUpdateSummary(List<String> lines) {
        int urgentCount = 0;
        int soonCount = 0;
        for (AssetRecord asset : activity.assets) {
            int days = activity.daysUntilDue(asset);
            if (days <= 0) {
                urgentCount += 1;
            } else if (days <= 3) {
                soonCount += 1;
            }
        }
        lines.add("");
        lines.add("更新计划：" + activity.updatePlanSummaryText(urgentCount, soonCount));
        lines.add("最近更新：" + (activity.updateEvents.isEmpty()
                ? "还没有更新记录。"
                : trendRenderer.recentUpdateSummaryText()));
        List<String> reasonLines = trendRenderer.updateReasonSummaryLines(false);
        if (!reasonLines.isEmpty()) {
            lines.add("变化原因：");
            for (String line : reasonLines) {
                lines.add("- " + line);
            }
        }
    }
}
