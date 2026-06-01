package com.computerascience.moneymanager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AllocationAnalytics;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.InstitutionBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.AllocationChartView;
import com.computerascience.moneymanager.ui.SpaceView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class OverviewPageRenderer {
    private final MainActivity activity;
    private final TrendPageRenderer trendRenderer;

    OverviewPageRenderer(MainActivity activity, TrendPageRenderer trendRenderer) {
        this.activity = activity;
        this.trendRenderer = trendRenderer;
    }

    View overviewCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("总资产概览"));

        activity.netWorthValue = activity.text("--", 34, MoneyManagerActivity.INK, Typeface.BOLD);
        activity.netWorthValue.setOnClickListener(view -> activity.showAssetManagement("all"));
        activity.netWorthValue.setContentDescription("调整资产明细");
        LinearLayout.LayoutParams netParams = activity.lp(-1, -2);
        netParams.topMargin = activity.dp(10);
        card.addView(activity.netWorthValue, netParams);

        activity.currencyNote = activity.text("", 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        activity.currencyNote.setOnClickListener(view -> activity.showCurrencySettingsDialog());
        activity.currencyNote.setContentDescription("调整基准币种与汇率");
        LinearLayout.LayoutParams noteParams = activity.lp(-1, -2);
        noteParams.topMargin = activity.dp(8);
        noteParams.bottomMargin = activity.dp(10);
        card.addView(activity.currencyNote, noteParams);

        LinearLayout overviewActions = activity.row();
        activity.privacyToggle = activity.secondaryButton("隐藏金额");
        activity.privacyToggle.setOnClickListener(view -> {
            activity.settings.hideAmounts = !activity.settings.hideAmounts;
            activity.store.saveSettings(activity.settings);
            activity.render();
        });
        overviewActions.addView(activity.privacyToggle, new LinearLayout.LayoutParams(0, activity.dp(42), 1));
        overviewActions.addView(new SpaceView(activity, activity.dp(10), 1));

        Button copySummary = activity.secondaryButton("复制摘要");
        copySummary.setOnClickListener(view -> copyAssetSummary());
        overviewActions.addView(copySummary, new LinearLayout.LayoutParams(0, activity.dp(42), 1));
        LinearLayout.LayoutParams actionParams = activity.lp(-1, -2);
        actionParams.bottomMargin = activity.dp(14);
        card.addView(overviewActions, actionParams);

        LinearLayout row1 = activity.row();
        activity.grossAssetsValue = activity.text("--", 18, MoneyManagerActivity.INK, Typeface.BOLD);
        activity.liabilitiesValue = activity.text("--", 18, MoneyManagerActivity.INK, Typeface.BOLD);
        View grossMetric = activity.metric("资产总额", activity.grossAssetsValue);
        grossMetric.setOnClickListener(view -> activity.showAssetManagement("all"));
        grossMetric.setContentDescription("调整资产总额明细");
        row1.addView(grossMetric, new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(new SpaceView(activity, activity.dp(10), 1));
        View liabilitiesMetric = activity.metric("负债", activity.liabilitiesValue);
        liabilitiesMetric.setOnClickListener(view -> activity.showAssetManagement("debt"));
        liabilitiesMetric.setContentDescription("调整负债明细");
        row1.addView(liabilitiesMetric, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row1);

        activity.freshnessValue = activity.text("--", 18, MoneyManagerActivity.INK, Typeface.BOLD);
        LinearLayout.LayoutParams freshParams = activity.lp(-1, -2);
        freshParams.topMargin = activity.dp(10);
        View freshnessMetric = activity.metric("更新状态", activity.freshnessValue);
        freshnessMetric.setOnClickListener(view -> activity.showAssetManagement("stale"));
        freshnessMetric.setContentDescription("调整待更新资产");
        card.addView(freshnessMetric, freshParams);
        return card;
    }

    View netWorthGoalCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("年度目标"));

        activity.netWorthGoalSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(12);
        card.addView(activity.netWorthGoalSummary, summaryParams);

        Button editButton = activity.secondaryButton("编辑年度目标");
        editButton.setOnClickListener(view -> activity.showNetWorthGoalDialog());
        card.addView(editButton, activity.lp(-1, activity.dp(44)));
        return card;
    }

    View allocationCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("资产比例"));

        LinearLayout body = activity.row();
        LinearLayout.LayoutParams bodyParams = activity.lp(-1, -2);
        bodyParams.topMargin = activity.dp(12);
        body.setLayoutParams(bodyParams);

        activity.allocationChart = new AllocationChartView(activity);
        body.addView(activity.allocationChart, new LinearLayout.LayoutParams(activity.dp(148), activity.dp(148)));

        activity.allocationLegend = new LinearLayout(activity);
        activity.allocationLegend.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(0, -2, 1);
        legendParams.leftMargin = activity.dp(14);
        body.addView(activity.allocationLegend, legendParams);
        card.addView(body);
        return card;
    }

    View allocationTargetCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("目标比例"));

        activity.allocationTargetSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.allocationTargetSummary, summaryParams);

        activity.allocationTargetList = new LinearLayout(activity);
        activity.allocationTargetList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.allocationTargetList, activity.lp(-1, -2));

        Button editButton = activity.secondaryButton("编辑目标比例");
        editButton.setOnClickListener(view -> activity.showAllocationTargetDialog());
        LinearLayout.LayoutParams buttonParams = activity.lp(-1, activity.dp(44));
        buttonParams.topMargin = activity.dp(10);
        card.addView(editButton, buttonParams);
        return card;
    }

    View institutionCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("机构分布"));

        TextView description = activity.text("按银行、券商或钱包汇总，方便核对资金主要放在哪里。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.topMargin = activity.dp(8);
        descriptionParams.bottomMargin = activity.dp(8);
        card.addView(description, descriptionParams);

        activity.institutionList = new LinearLayout(activity);
        activity.institutionList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.institutionList, activity.lp(-1, -2));
        return card;
    }

    void render(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        activity.netWorthValue.setText(activity.formatMoney(portfolio.netWorth, portfolio.baseCurrency));
        activity.grossAssetsValue.setText(activity.formatMoney(portfolio.grossAssets, portfolio.baseCurrency));
        activity.liabilitiesValue.setText(activity.formatMoney(portfolio.liabilities, portfolio.baseCurrency));
        activity.freshnessValue.setText(portfolio.staleCount + " 项待更新");
        activity.privacyToggle.setText(activity.settings.hideAmounts ? "显示金额" : "隐藏金额");
        activity.currencyNote.setText(currencyNoteText(portfolio));
        activity.netWorthGoalSummary.setText(netWorthGoalText(portfolio));
        activity.allocationChart.setCategories(portfolio.categories);
        renderAllocationLegend(portfolio);
        renderAllocationTargets(portfolio);
        renderInstitutionList(portfolio);
    }

    String currencySettingsText() {
        List<String> rows = new ArrayList<>();
        rows.add("基准：" + activity.settings.baseCurrency);
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            if (currency.equals(activity.settings.baseCurrency)) {
                continue;
            }
            double rate = activity.settings.rateFor(currency);
            if (rate > 0) {
                rows.add("1 " + currency + " = " + activity.formatRate(rate) + " " + activity.settings.baseCurrency);
            }
        }
        return activity.joinLines(rows);
    }

    String netWorthGoalText(PortfolioSummary portfolio) {
        if (!activity.settings.hasNetWorthTarget()) {
            return "还没有设置年度目标。设置一个目标净资产和截止日期后，这里会显示进度和所需月均增量。";
        }

        String targetDate = activity.dayKey(activity.settings.netWorthTargetDate);
        if (activity.settings.hideAmounts) {
            return "已设置 " + targetDate + " 前的年度目标；隐私模式已开启，金额和进度暂不显示。";
        }

        double gap = activity.settings.netWorthTarget - portfolio.netWorth;
        double progress = activity.settings.netWorthTarget <= 0 ? 0 : portfolio.netWorth / activity.settings.netWorthTarget * 100;
        int daysLeft = activity.daysUntilTimestamp(activity.settings.netWorthTargetDate);
        if (gap <= 0) {
            return "目标 " + activity.formatMoney(activity.settings.netWorthTarget, portfolio.baseCurrency)
                    + "，截止 " + targetDate + "；当前进度 "
                    + activity.formatPercentValue(progress) + "，已达到目标。";
        }

        if (daysLeft <= 0) {
            return "目标 " + activity.formatMoney(activity.settings.netWorthTarget, portfolio.baseCurrency)
                    + "，目标日 " + targetDate + " 已到；当前仍差 "
                    + activity.formatMoney(gap, portfolio.baseCurrency) + "。";
        }

        double monthsLeft = Math.max(1.0, daysLeft / 30.4375);
        return "目标 " + activity.formatMoney(activity.settings.netWorthTarget, portfolio.baseCurrency)
                + "，截止 " + targetDate + "；当前进度 "
                + activity.formatPercentValue(progress) + "，还差 "
                + activity.formatMoney(gap, portfolio.baseCurrency)
                + "，剩余 " + daysLeft + " 天，约每月需要增加 "
                + activity.formatMoney(gap / monthsLeft, portfolio.baseCurrency) + "。";
    }

    private void copyAssetSummary() {
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
            lines.add("年度目标：" + netWorthGoalText(portfolio));
        }
        lines.add("");
        lines.add("一年趋势：" + trendRenderer.summaryText(portfolio, trendSnapshots));
        List<String> trendReview = trendRenderer.reviewLines(portfolio, trendSnapshots);
        if (!trendReview.isEmpty()) {
            lines.add("趋势复盘：");
            for (String line : trendReview) {
                lines.add("- " + line);
            }
        }

        if (!portfolio.categories.isEmpty()) {
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

        if (activity.settings.hasAllocationTargets()) {
            List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, activity.settings);
            if (!drifts.isEmpty()) {
                lines.add("");
                lines.add("目标比例提醒");
                int limit = Math.min(3, drifts.size());
                for (int index = 0; index < limit; index += 1) {
                    AllocationAnalytics.Drift drift = drifts.get(index);
                    double gap = drift.targetPercent - drift.currentPercent;
                    String status = Math.abs(gap) < 0.5
                            ? "接近目标"
                            : (gap > 0 ? "低配" : "超配");
                    String line = "- " + drift.category + "：当前 "
                            + activity.formatPercentValue(drift.currentPercent)
                            + "，目标 " + activity.formatPercentValue(drift.targetPercent)
                            + "，" + status + " " + activity.formatPercentValue(Math.abs(gap));
                    if (!activity.settings.hideAmounts && Math.abs(gap) >= 0.5) {
                        line += "，建议" + (drift.amountDelta > 0 ? "增加 " : "减少 ")
                                + activity.formatMoney(Math.abs(drift.amountDelta), portfolio.baseCurrency);
                    }
                    lines.add(line);
                }
            }
        }

        if (!portfolio.institutions.isEmpty()) {
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
        return activity.joinLines(lines);
    }

    private void renderAllocationLegend(PortfolioSummary portfolio) {
        activity.allocationLegend.removeAllViews();
        double total = portfolio.grossAssets + portfolio.liabilities;
        if (portfolio.categories.isEmpty() || total <= 0) {
            activity.allocationLegend.addView(activity.text("暂无可展示的资产比例。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        for (CategoryBreakdown category : portfolio.categories) {
            LinearLayout row = activity.row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
            rowParams.bottomMargin = activity.dp(8);
            row.setLayoutParams(rowParams);

            TextView dot = activity.text("●", 16, category.color, Typeface.BOLD);
            row.addView(dot);

            TextView label = activity.text("  " + category.category, 14, MoneyManagerActivity.INK, Typeface.BOLD);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            double ratio = category.value / total * 100;
            TextView value = activity.text(String.format(Locale.getDefault(), "%.1f%%", ratio), 14, MoneyManagerActivity.MUTED, Typeface.BOLD);
            row.addView(value);
            activity.allocationLegend.addView(row);
        }
    }

    private void renderAllocationTargets(PortfolioSummary portfolio) {
        activity.allocationTargetList.removeAllViews();
        if (!activity.settings.hasAllocationTargets()) {
            activity.allocationTargetSummary.setText("还没有设置目标比例。配置后，这里会提示哪些类型低配或超配。");
            activity.allocationTargetList.addView(activity.text("适合给银行现金、券商、基金、负债等设置一个长期目标。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, activity.settings);
        int offTrack = 0;
        for (AllocationAnalytics.Drift drift : drifts) {
            if (Math.abs(drift.currentPercent - drift.targetPercent) >= 5) {
                offTrack += 1;
            }
        }
        activity.allocationTargetSummary.setText("已设置目标比例；"
                + offTrack + " 类资产偏离目标超过 5 个百分点。");

        if (drifts.isEmpty()) {
            activity.allocationTargetList.addView(activity.text("目标已保存。新增或更新资产后，这里会显示偏离情况。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        int limit = Math.min(5, drifts.size());
        for (int index = 0; index < limit; index += 1) {
            activity.allocationTargetList.addView(allocationTargetRow(drifts.get(index)));
        }
    }

    private View allocationTargetRow(AllocationAnalytics.Drift drift) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        TextView dot = activity.text("●", 15, drift.color, Typeface.BOLD);
        header.addView(dot);
        TextView title = activity.text("  " + drift.category, 14, MoneyManagerActivity.INK, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = allocationDriftChip(drift);
        header.addView(status);
        row.addView(header);

        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(6);
        row.addView(activity.text("当前 " + activity.formatPercentValue(drift.currentPercent)
                + " · 目标 " + activity.formatPercentValue(drift.targetPercent)
                + " · 偏离 " + activity.formatPoint(drift.currentPercent - drift.targetPercent),
                13, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);

        if (!activity.settings.hideAmounts) {
            LinearLayout.LayoutParams actionParams = activity.lp(-1, -2);
            actionParams.topMargin = activity.dp(4);
            row.addView(activity.text(allocationRecommendationText(drift), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), actionParams);
        }
        return row;
    }

    private TextView allocationDriftChip(AllocationAnalytics.Drift drift) {
        double gap = drift.targetPercent - drift.currentPercent;
        String label;
        int color;
        if (Math.abs(gap) < 0.5) {
            label = "接近目标";
            color = MoneyManagerActivity.ACCENT;
        } else if (gap > 0) {
            label = "低配";
            color = MoneyManagerActivity.AMBER;
        } else {
            label = "超配";
            color = MoneyManagerActivity.DANGER;
        }
        TextView chip = activity.text(label, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(activity.dp(10), activity.dp(6), activity.dp(10), activity.dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(activity.dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private String allocationRecommendationText(AllocationAnalytics.Drift drift) {
        double amount = Math.abs(drift.amountDelta);
        if (Math.abs(drift.targetPercent - drift.currentPercent) < 0.5) {
            return "已接近目标，无需特别调整。";
        }
        if (amount < 0.01) {
            return "录入资产金额后会估算需要调整的金额。";
        }
        String direction = drift.amountDelta > 0 ? "增加" : "减少";
        return "按当前总额估算，" + direction + "约 "
                + activity.formatMoney(amount, activity.settings.baseCurrency) + " 可接近目标。";
    }

    private void renderInstitutionList(PortfolioSummary portfolio) {
        activity.institutionList.removeAllViews();
        double total = portfolio.grossAssets + portfolio.liabilities;
        if (portfolio.institutions.isEmpty() || total <= 0) {
            activity.institutionList.addView(activity.text("暂无可展示的机构分布。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        int limit = Math.min(5, portfolio.institutions.size());
        for (int index = 0; index < limit; index += 1) {
            InstitutionBreakdown institution = portfolio.institutions.get(index);
            activity.institutionList.addView(institutionRow(institution, total));
        }
    }

    private View institutionRow(InstitutionBreakdown institution, double total) {
        LinearLayout row = activity.row();
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout labelGroup = new LinearLayout(activity);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        labelGroup.addView(activity.text(institution.institution, 14, MoneyManagerActivity.INK, Typeface.BOLD));

        LinearLayout.LayoutParams countParams = activity.lp(-1, -2);
        countParams.topMargin = activity.dp(4);
        labelGroup.addView(activity.text(institution.assetCount + " 项资产", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), countParams);
        row.addView(labelGroup, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout valueGroup = new LinearLayout(activity);
        valueGroup.setOrientation(LinearLayout.VERTICAL);
        valueGroup.setGravity(Gravity.END);
        valueGroup.addView(activity.text(activity.formatMoney(institution.value, activity.settings.baseCurrency), 14, MoneyManagerActivity.INK, Typeface.BOLD));

        LinearLayout.LayoutParams ratioParams = activity.lp(-1, -2);
        ratioParams.topMargin = activity.dp(4);
        TextView ratio = activity.text(activity.formatPercent(institution.value, total), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        ratio.setGravity(Gravity.END);
        valueGroup.addView(ratio, ratioParams);
        row.addView(valueGroup);
        return row;
    }

    private String currencyNoteText(PortfolioSummary portfolio) {
        if (portfolio.missingRateCount > 0) {
            return "总额以 " + portfolio.baseCurrency + " 显示；"
                    + portfolio.missingRateCount + " 项资产缺少汇率，暂按 1:1 估算。";
        }
        if (portfolio.hasMixedCurrencies) {
            return "总额以 " + portfolio.baseCurrency + " 显示，多币种资产已按本地汇率换算。";
        }
        return "总额以 " + portfolio.baseCurrency + " 显示。";
    }
}
