package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AllocationAnalytics;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.DataHealth;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.domain.UpdateAnalytics;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.SpaceView;
import com.computerascience.moneymanager.ui.TrendChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TrendPageRenderer {
    private final MainActivity activity;
    private final TrendReviewRenderer reviewRenderer;

    TrendPageRenderer(MainActivity activity) {
        this.activity = activity;
        this.reviewRenderer = new TrendReviewRenderer(activity);
    }

    View trendCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("一年变化趋势"));

        activity.trendSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        card.addView(activity.trendSummary, summaryParams);

        activity.trendChart = new TrendChartView(activity);
        LinearLayout.LayoutParams chartParams = activity.lp(-1, activity.dp(190));
        chartParams.topMargin = activity.dp(10);
        card.addView(activity.trendChart, chartParams);

        activity.trendMetricsList = new LinearLayout(activity);
        activity.trendMetricsList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams metricsParams = activity.lp(-1, -2);
        metricsParams.topMargin = activity.dp(10);
        metricsParams.bottomMargin = activity.dp(4);
        card.addView(activity.trendMetricsList, metricsParams);

        LinearLayout snapshotActions = activity.row();
        Button snapshotButton = activity.secondaryButton("记录今日快照");
        snapshotButton.setOnClickListener(view -> {
            activity.snapshots = activity.store.recordSnapshot(activity.assets, activity.settings);
            activity.render();
            activity.toast("已记录今日总资产快照。");
        });
        snapshotActions.addView(snapshotButton, new LinearLayout.LayoutParams(0, activity.dp(44), 1));
        snapshotActions.addView(new SpaceView(activity, activity.dp(10), 1));

        Button backfillButton = activity.secondaryButton("补录快照");
        backfillButton.setOnClickListener(view -> showSnapshotBackfillDialog());
        snapshotActions.addView(backfillButton, new LinearLayout.LayoutParams(0, activity.dp(44), 1));
        LinearLayout.LayoutParams actionParams = activity.lp(-1, -2);
        actionParams.topMargin = activity.dp(8);
        actionParams.bottomMargin = activity.dp(12);
        card.addView(snapshotActions, actionParams);

        activity.trendHistoryList = new LinearLayout(activity);
        activity.trendHistoryList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.trendHistoryList, activity.lp(-1, -2));
        return card;
    }

    View distributionTrendCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("分布变化"));

        activity.distributionTrendSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.distributionTrendSummary, summaryParams);

        activity.distributionTrendList = new LinearLayout(activity);
        activity.distributionTrendList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.distributionTrendList, activity.lp(-1, -2));
        return card;
    }

    View monthlyReviewCard() {
        return reviewRenderer.monthlyReviewCard();
    }

    View flowAttributionCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("变化归因"));

        activity.flowAttributionSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.flowAttributionSummary, summaryParams);

        activity.flowAttributionList = new LinearLayout(activity);
        activity.flowAttributionList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.flowAttributionList, activity.lp(-1, -2));
        return card;
    }

    View assetTrendCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("单项资产趋势"));

        activity.assetTrendSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(10);
        card.addView(activity.assetTrendSummary, summaryParams);

        activity.assetTrendSpinner = new Spinner(activity);
        activity.styleSpinner(activity.assetTrendSpinner);
        activity.assetTrendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (activity.suppressAssetTrendSelection || position < 0 || position >= activity.assetTrendOptions.size()) {
                    return;
                }
                activity.selectedTrendAssetId = activity.assetTrendOptions.get(position).id;
                renderAssetTrend();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        card.addView(activity.fieldBox("选择资产", activity.assetTrendSpinner));

        activity.assetTrendChart = new TrendChartView(activity);
        LinearLayout.LayoutParams chartParams = activity.lp(-1, activity.dp(160));
        chartParams.topMargin = activity.dp(6);
        chartParams.bottomMargin = activity.dp(10);
        card.addView(activity.assetTrendChart, chartParams);

        activity.assetTrendHistoryList = new LinearLayout(activity);
        activity.assetTrendHistoryList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.assetTrendHistoryList, activity.lp(-1, -2));
        return card;
    }

    View dataHealthCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("数据健康"));

        activity.insightSummary = activity.text("", 15, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams insightParams = activity.lp(-1, -2);
        insightParams.topMargin = activity.dp(10);
        insightParams.bottomMargin = activity.dp(12);
        card.addView(activity.insightSummary, insightParams);

        activity.dataHealthSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(2);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.dataHealthSummary, summaryParams);

        activity.dataHealthList = new LinearLayout(activity);
        activity.dataHealthList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.dataHealthList, activity.lp(-1, -2));

        Button reviewButton = activity.secondaryButton("查看待处理资产");
        reviewButton.setOnClickListener(view -> activity.showAssetManagement("issues"));
        LinearLayout.LayoutParams reviewParams = activity.lp(-1, activity.dp(42));
        reviewParams.topMargin = activity.dp(10);
        card.addView(reviewButton, reviewParams);
        return card;
    }

    View recentUpdatesCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("最近更新"));

        activity.recentUpdateSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.recentUpdateSummary, summaryParams);

        activity.recentUpdateList = new LinearLayout(activity);
        activity.recentUpdateList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.recentUpdateList, activity.lp(-1, -2));
        return card;
    }

    void render(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        activity.trendChart.setSnapshots(trendSnapshots);
        activity.trendSummary.setText(summaryText(portfolio, trendSnapshots));
        renderTrendMetrics(portfolio, trendSnapshots);
        renderTrendHistory(trendSnapshots);
        reviewRenderer.renderMonthlyReview(portfolio, trendSnapshots);
        renderDistributionTrend(trendSnapshots);
        renderFlowAttribution();
        renderAssetTrend();

        activity.insightSummary.setText(buildInsightText(portfolio));
        renderDataHealth(portfolio);
        renderRecentUpdates();
    }

    String summaryText(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        if (trendSnapshots.size() < 2) {
            return "当前基准 " + portfolio.baseCurrency + " 已记录 " + trendSnapshots.size()
                    + " 个快照。每天或每次核对后记录一次，趋势会逐渐形成。";
        }
        if (activity.settings.hideAmounts) {
            return "近一年记录 " + trendSnapshots.size() + " 个 " + portfolio.baseCurrency
                    + " 快照。隐私模式已开启，金额变化暂不显示。";
        }
        AssetSnapshot first = trendSnapshots.get(0);
        AssetSnapshot last = trendSnapshots.get(trendSnapshots.size() - 1);
        double change = last.netWorth - first.netWorth;
        double ratio = Math.abs(first.netWorth) < 0.0001 ? 0 : change / Math.abs(first.netWorth) * 100;
        return "近一年记录 " + trendSnapshots.size() + " 个 " + portfolio.baseCurrency + " 快照，净资产变化 "
                + activity.formatSignedMoney(change, portfolio.baseCurrency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    List<String> reviewLines(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        List<String> lines = new ArrayList<>();
        for (TrendAnalytics.Metric metric : TrendAnalytics.metrics(portfolio, trendSnapshots, System.currentTimeMillis())) {
            if (metric.complete) {
                lines.add(metric.label + "：" + metricSummaryText(metric));
            }
        }
        return lines;
    }

    private void renderTrendMetrics(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        activity.trendMetricsList.removeAllViews();
        activity.trendMetricsList.addView(activity.text("趋势复盘", 13, MoneyManagerActivity.MUTED, Typeface.BOLD));

        List<TrendAnalytics.Metric> metrics = TrendAnalytics.metrics(portfolio, trendSnapshots, System.currentTimeMillis());
        if (metrics.isEmpty()) {
            TextView empty = activity.text("至少记录两次快照后，会显示近 30 天、90 天和一年的变化。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.trendMetricsList.addView(empty, emptyParams);
            return;
        }

        for (TrendAnalytics.Metric metric : metrics) {
            activity.trendMetricsList.addView(trendMetricRow(metric));
        }
    }

    private View trendMetricRow(TrendAnalytics.Metric metric) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        header.addView(activity.text(metric.label, 14, MoneyManagerActivity.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(activity.text(metric.count + " 个快照", 12, MoneyManagerActivity.MUTED, Typeface.BOLD));
        row.addView(header);

        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(6);
        row.addView(activity.text(metricSummaryText(metric), 13, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private String metricSummaryText(TrendAnalytics.Metric metric) {
        if (!metric.complete) {
            return "快照不足，继续记录后再计算阶段变化。";
        }
        if (activity.settings.hideAmounts) {
            return "金额变化已隐藏，区间为 " + metric.first.dayKey + " 到 " + metric.last.dayKey + "。";
        }

        double ratio = Math.abs(metric.first.netWorth) < 0.0001
                ? 0
                : metric.change / Math.abs(metric.first.netWorth) * 100;
        return metric.first.dayKey + " 到 " + metric.last.dayKey
                + "，变化 " + activity.formatSignedMoney(metric.change, metric.currency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）"
                + "；高点 " + metric.high.dayKey + " "
                + activity.formatMoney(metric.high.netWorth, metric.currency)
                + "，低点 " + metric.low.dayKey + " "
                + activity.formatMoney(metric.low.netWorth, metric.currency) + "。";
    }

    private void renderTrendHistory(List<AssetSnapshot> trendSnapshots) {
        activity.trendHistoryList.removeAllViews();
        activity.trendHistoryList.addView(activity.text("最近快照", 13, MoneyManagerActivity.MUTED, Typeface.BOLD));

        if (trendSnapshots.isEmpty()) {
            TextView empty = activity.text("暂无快照。记录一次后会出现在这里。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.trendHistoryList.addView(empty, emptyParams);
            return;
        }

        int start = Math.max(0, trendSnapshots.size() - 6);
        for (int index = trendSnapshots.size() - 1; index >= start; index -= 1) {
            AssetSnapshot snapshot = trendSnapshots.get(index);
            activity.trendHistoryList.addView(snapshotRow(snapshot));
        }
    }

    private void renderDistributionTrend(List<AssetSnapshot> trendSnapshots) {
        activity.distributionTrendList.removeAllViews();
        List<AssetSnapshot> available = TrendAnalytics.snapshotsWithCategoryValues(trendSnapshots);
        if (available.size() < 2) {
            int count = available.size();
            activity.distributionTrendSummary.setText("已记录 " + count + " 个带分布的快照；从这版开始，每次更新或记录快照都会保存类型分布。");
            TextView empty = activity.text("再记录一次快照后，这里会显示各资产类型金额和占比的变化。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.distributionTrendList.addView(empty, emptyParams);
            return;
        }

        AssetSnapshot first = available.get(0);
        AssetSnapshot last = available.get(available.size() - 1);
        List<TrendAnalytics.CategoryShift> shifts = TrendAnalytics.categoryShifts(first, last);
        if (shifts.isEmpty()) {
            activity.distributionTrendSummary.setText("已记录 " + available.size() + " 个带分布的快照，但暂时没有可对比的类型金额。");
            activity.distributionTrendList.addView(activity.text("继续更新资产金额后再查看分布变化。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        activity.distributionTrendSummary.setText(distributionTrendSummaryText(first, last, shifts, available.size()));
        int limit = Math.min(6, shifts.size());
        for (int index = 0; index < limit; index += 1) {
            activity.distributionTrendList.addView(categoryShiftRow(shifts.get(index), last.baseCurrency));
        }
    }

    private String distributionTrendSummaryText(
            AssetSnapshot first,
            AssetSnapshot last,
            List<TrendAnalytics.CategoryShift> shifts,
            int count
    ) {
        TrendAnalytics.CategoryShift biggest = shifts.get(0);
        if (activity.settings.hideAmounts) {
            return "已记录 " + count + " 个带分布快照，范围 "
                    + first.dayKey + " 到 " + last.dayKey + "；金额已隐藏。";
        }
        return "从 " + first.dayKey + " 到 " + last.dayKey
                + "，变化最大的是 " + biggest.category + "："
                + activity.formatSignedMoney(biggest.delta, last.baseCurrency)
                + "，占比 " + activity.formatPoint(biggest.percentDelta) + "。";
    }

    private View categoryShiftRow(TrendAnalytics.CategoryShift shift, String currency) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        header.addView(activity.text(shift.category, 14, MoneyManagerActivity.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView change = activity.text(activity.formatPoint(shift.percentDelta), 12,
                shift.percentDelta >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER,
                Typeface.BOLD);
        change.setGravity(Gravity.END);
        header.addView(change);
        row.addView(header);

        String detail;
        if (activity.settings.hideAmounts) {
            detail = "占比 " + activity.formatPercentValue(shift.firstPercent)
                    + " -> " + activity.formatPercentValue(shift.lastPercent)
                    + "，金额已隐藏。";
        } else {
            detail = activity.formatMoney(shift.firstValue, currency)
                    + " -> " + activity.formatMoney(shift.lastValue, currency)
                    + "，变化 " + activity.formatSignedMoney(shift.delta, currency)
                    + "；占比 " + activity.formatPercentValue(shift.firstPercent)
                    + " -> " + activity.formatPercentValue(shift.lastPercent) + "。";
        }
        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(6);
        row.addView(activity.text(detail, 13, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private void renderFlowAttribution() {
        activity.flowAttributionList.removeAllViews();
        UpdateAnalytics.Summary summary = UpdateAnalytics.summarize(activity.updateEvents, activity.assets, activity.settings, 90);
        if (summary.count == 0) {
            activity.flowAttributionSummary.setText("近 90 天还没有更新记录。录入几次金额变化后，这里会按原因、类型和机构拆解。");
            activity.flowAttributionList.addView(activity.emptyText("暂无可归因的变化。"));
            return;
        }

        if (activity.settings.hideAmounts) {
            activity.flowAttributionSummary.setText("近 " + summary.days + " 天记录 "
                    + summary.count + " 次更新，金额已隐藏。");
        } else {
            activity.flowAttributionSummary.setText("近 " + summary.days + " 天记录 "
                    + summary.count + " 次更新，净变化 "
                    + activity.formatSignedMoney(summary.delta, activity.settings.baseCurrency)
                    + "；流入 " + activity.formatMoney(summary.increase, activity.settings.baseCurrency)
                    + "，流出 " + activity.formatMoney(Math.abs(summary.decrease), activity.settings.baseCurrency) + "。");
        }

        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按原因", summary.reasons);
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按类型", summary.categories);
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按机构", summary.institutions);
    }

    private void renderAssetTrend() {
        activity.assetTrendOptions = new ArrayList<>(activity.assets);
        Collections.sort(activity.assetTrendOptions, (left, right) -> left.name.compareToIgnoreCase(right.name));

        activity.assetTrendHistoryList.removeAllViews();
        if (activity.assetTrendOptions.isEmpty()) {
            activity.assetTrendSummary.setText("新增资产后，这里会显示每一项资产的金额变化。");
            activity.assetTrendChart.setPoints(new ArrayList<>(), "还没有资产");
            activity.suppressAssetTrendSelection = true;
            activity.assetTrendSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
            activity.suppressAssetTrendSelection = false;
            return;
        }

        int selectedIndex = 0;
        if (!activity.selectedTrendAssetId.isEmpty()) {
            for (int index = 0; index < activity.assetTrendOptions.size(); index += 1) {
                if (activity.selectedTrendAssetId.equals(activity.assetTrendOptions.get(index).id)) {
                    selectedIndex = index;
                    break;
                }
            }
        }
        activity.selectedTrendAssetId = activity.assetTrendOptions.get(selectedIndex).id;

        List<String> names = new ArrayList<>();
        for (AssetRecord asset : activity.assetTrendOptions) {
            names.add(asset.name);
        }
        activity.suppressAssetTrendSelection = true;
        activity.assetTrendSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, names));
        activity.assetTrendSpinner.setSelection(selectedIndex);
        activity.suppressAssetTrendSelection = false;

        AssetRecord selected = activity.assetTrendOptions.get(selectedIndex);
        List<AssetUpdateEvent> events = updateEventsForAsset(selected.id);
        List<TrendChartView.Point> points = assetTrendPoints(selected, events);
        activity.assetTrendChart.setPoints(points, "更新几次金额后显示单项趋势");
        activity.assetTrendSummary.setText(assetTrendSummaryText(selected, points));

        activity.assetTrendHistoryList.addView(activity.text("最近变化", 13, MoneyManagerActivity.MUTED, Typeface.BOLD));
        if (events.isEmpty()) {
            TextView empty = activity.text("这项资产还没有更新记录。点“已更新”录入几次金额后，就能看到单项趋势。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.assetTrendHistoryList.addView(empty, emptyParams);
            return;
        }
        int limit = Math.min(5, events.size());
        for (int index = 0; index < limit; index += 1) {
            activity.assetTrendHistoryList.addView(updateEventRow(events.get(index)));
        }
    }

    private List<AssetUpdateEvent> updateEventsForAsset(String assetId) {
        List<AssetUpdateEvent> events = new ArrayList<>();
        for (AssetUpdateEvent event : activity.updateEvents) {
            if (assetId.equals(event.assetId)) {
                events.add(event);
            }
        }
        Collections.sort(events, (left, right) -> Long.compare(right.timestamp, left.timestamp));
        return events;
    }

    private List<TrendChartView.Point> assetTrendPoints(AssetRecord asset, List<AssetUpdateEvent> newestFirst) {
        List<AssetUpdateEvent> ascending = new ArrayList<>(newestFirst);
        Collections.sort(ascending, (left, right) -> Long.compare(left.timestamp, right.timestamp));

        List<TrendChartView.Point> points = new ArrayList<>();
        for (AssetUpdateEvent event : ascending) {
            if (points.isEmpty()) {
                points.add(new TrendChartView.Point(event.timestamp - 1, AssetMath.parseAmount(event.previousAmount)));
            }
            points.add(new TrendChartView.Point(event.timestamp, AssetMath.parseAmount(event.newAmount)));
        }

        if (points.isEmpty() && !asset.amount.isEmpty()) {
            points.add(new TrendChartView.Point(
                    asset.lastUpdatedAt <= 0 ? System.currentTimeMillis() : asset.lastUpdatedAt,
                    AssetMath.parseAmount(asset.amount)
            ));
        }
        return points;
    }

    private String assetTrendSummaryText(AssetRecord asset, List<TrendChartView.Point> points) {
        if (points.size() < 2) {
            return "当前 " + asset.name + " 只有 " + points.size() + " 个记录点，继续更新后会形成单项趋势。";
        }
        if (activity.settings.hideAmounts) {
            return asset.name + " 已记录 " + points.size() + " 个变化点，金额已隐藏。";
        }
        TrendChartView.Point first = points.get(0);
        TrendChartView.Point last = points.get(points.size() - 1);
        double change = last.value - first.value;
        double ratio = Math.abs(first.value) < 0.0001 ? 0 : change / Math.abs(first.value) * 100;
        return asset.name + " 共 " + points.size() + " 个变化点，变化 "
                + activity.formatSignedRawAmount(change) + " " + asset.currency
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private View snapshotRow(AssetSnapshot snapshot) {
        LinearLayout row = activity.row();
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(10), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout textGroup = new LinearLayout(activity);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.addView(activity.text(snapshot.dayKey + " · " + snapshot.baseCurrency, 14, MoneyManagerActivity.INK, Typeface.BOLD));

        String details = "净资产 " + activity.formatMoney(snapshot.netWorth, snapshot.baseCurrency)
                + " · 资产 " + activity.formatMoney(snapshot.grossAssets, snapshot.baseCurrency)
                + " · 负债 " + activity.formatMoney(snapshot.liabilities, snapshot.baseCurrency);
        LinearLayout.LayoutParams detailsParams = activity.lp(-1, -2);
        detailsParams.topMargin = activity.dp(4);
        textGroup.addView(activity.text(details, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailsParams);
        row.addView(textGroup, new LinearLayout.LayoutParams(0, -2, 1));

        Button delete = activity.secondaryButton("删除");
        delete.setTextColor(MoneyManagerActivity.DANGER);
        delete.setOnClickListener(view -> confirmDeleteSnapshot(snapshot));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(activity.dp(72), activity.dp(38));
        deleteParams.leftMargin = activity.dp(10);
        row.addView(delete, deleteParams);
        return row;
    }

    private void confirmDeleteSnapshot(AssetSnapshot snapshot) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("删除快照？")
                .setMessage("确定删除 " + snapshot.dayKey + " 的 " + snapshot.baseCurrency + " 快照吗？趋势图会立刻更新。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (ignoredDialog, which) -> {
                    activity.snapshots = activity.store.deleteSnapshot(snapshot.dayKey, snapshot.baseCurrency);
                    activity.render();
                    activity.toast("已删除趋势快照。");
                })
                .create();
        activity.showStyledDialog(dialog);
    }

    private void showSnapshotBackfillDialog() {
        PortfolioSummary portfolio = AssetMath.summarize(activity.assets, activity.settings);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, 0);

        TextView description = activity.text("按当前基准币种 " + portfolio.baseCurrency + " 补录近一年历史快照；同一天会覆盖原快照。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.bottomMargin = activity.dp(12);
        form.addView(description, descriptionParams);

        EditText day = activity.input("日期（yyyy-MM-dd）", activity.dayKey(System.currentTimeMillis()), InputType.TYPE_CLASS_TEXT);
        form.addView(day);

        EditText netWorth = activity.input("净资产", activity.formatInputNumber(portfolio.netWorth), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(netWorth);

        EditText grossAssets = activity.input("资产总额", activity.formatInputNumber(portfolio.grossAssets), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(grossAssets);

        EditText liabilities = activity.input("负债", activity.formatInputNumber(portfolio.liabilities), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(liabilities);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("补录历史快照")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存快照", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                Date parsedDay = activity.parseDay(activity.clean(day.getText().toString()));
                if (parsedDay == null) {
                    activity.toast("日期格式应为 yyyy-MM-dd。");
                    return;
                }
                long timestamp = parsedDay.getTime();
                long now = System.currentTimeMillis();
                if (timestamp > now) {
                    activity.toast("不能补录未来日期。");
                    return;
                }
                if (timestamp < now - 370L * AssetMath.DAY_MS) {
                    activity.toast("只能补录近一年快照。");
                    return;
                }

                Double net = activity.parseNumber(activity.clean(netWorth.getText().toString()));
                Double gross = activity.parseNumber(activity.clean(grossAssets.getText().toString()));
                Double debt = activity.parseNumber(activity.clean(liabilities.getText().toString()));
                if (net == null || gross == null || debt == null) {
                    activity.toast("金额必须是数字。");
                    return;
                }
                if (gross < 0 || debt < 0) {
                    activity.toast("资产总额和负债不能为负数。");
                    return;
                }

                activity.snapshots = activity.store.upsertSnapshot(new AssetSnapshot(
                        activity.dayKey(timestamp),
                        timestamp,
                        portfolio.baseCurrency,
                        net,
                        gross,
                        debt
                ));
                activity.render();
                activity.toast("已补录历史快照。");
                dialog.dismiss();
            });
        });

        activity.showStyledDialog(dialog);
    }

    private String buildInsightText(PortfolioSummary portfolio) {
        List<String> lines = new ArrayList<>();
        if (!portfolio.categories.isEmpty()) {
            CategoryBreakdown largest = portfolio.categories.get(0);
            double base = portfolio.grossAssets + portfolio.liabilities;
            double ratio = base <= 0 ? 0 : largest.value / base * 100;
            lines.add("最大类别：" + largest.category + "，占比 "
                    + String.format(Locale.getDefault(), "%.1f", ratio) + "%。");
        }
        if (activity.settings.hasAllocationTargets()) {
            List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, activity.settings);
            if (!drifts.isEmpty()) {
                AllocationAnalytics.Drift largestDrift = drifts.get(0);
                double gap = largestDrift.targetPercent - largestDrift.currentPercent;
                if (Math.abs(gap) >= 5) {
                    lines.add("比例偏离最大：" + largestDrift.category + " "
                            + (gap > 0 ? "低配 " : "超配 ")
                            + activity.formatPercentValue(Math.abs(gap)) + "。");
                }
            }
        }
        if (activity.settings.hasNetWorthTarget()) {
            double gap = activity.settings.netWorthTarget - portfolio.netWorth;
            int daysLeft = activity.daysUntilTimestamp(activity.settings.netWorthTargetDate);
            if (activity.settings.hideAmounts && daysLeft <= 30) {
                lines.add("年度目标临近，金额暂不显示。");
            } else if (gap <= 0) {
                lines.add("年度净资产目标已达到。");
            } else if (daysLeft <= 30) {
                lines.add("年度目标还差 " + activity.formatMoney(gap, portfolio.baseCurrency)
                        + "，剩余 " + Math.max(0, daysLeft) + " 天。");
            }
        }
        if (portfolio.grossAssets > 0 && portfolio.liabilities / portfolio.grossAssets > 0.4) {
            lines.add("负债率偏高，建议单独关注还款节奏。");
        }
        if (portfolio.hasMixedCurrencies) {
            lines.add("当前存在多币种资产，总额会按最新或本地汇率换算。");
        }
        if (lines.isEmpty()) {
            lines.add("暂无突出的配置或目标风险，按下面的更新周期处理即可。");
        }
        return activity.joinLines(lines);
    }

    private void renderDataHealth(PortfolioSummary portfolio) {
        activity.dataHealthList.removeAllViews();
        List<String> issues = DataHealth.portfolioIssues(activity.assets, activity.settings, portfolio.baseCurrency);
        if (issues.isEmpty()) {
            activity.dataHealthSummary.setText("数据状态良好：金额、机构、App 绑定和汇率都已覆盖。");
            activity.dataHealthList.addView(activity.text("继续保持定期核对即可。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        List<com.computerascience.moneymanager.domain.AssetInstitutionGroups.Group> groups = DataHealth.issueGroups(activity.assets, activity.settings);
        activity.dataHealthSummary.setText("发现 " + issues.size() + " 类数据维护问题，分布在 "
                + groups.size() + " 个机构；具体处理已放到资产页行动中心。");
        int limit = Math.min(5, issues.size());
        for (int index = 0; index < limit; index += 1) {
            activity.dataHealthList.addView(healthIssueRow(issues.get(index)));
        }
        if (issues.size() > limit) {
            TextView more = activity.text("还有 " + (issues.size() - limit) + " 类问题可在行动中心继续处理。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = activity.lp(-1, -2);
            moreParams.topMargin = activity.dp(8);
            activity.dataHealthList.addView(more, moreParams);
        }
    }

    private View healthIssueRow(String issue) {
        TextView row = activity.text("• " + issue, 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        row.setPadding(activity.dp(12), activity.dp(8), activity.dp(12), activity.dp(8));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams params = activity.lp(-1, -2);
        params.topMargin = activity.dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private void renderRecentUpdates() {
        activity.recentUpdateList.removeAllViews();
        if (activity.updateEvents.isEmpty()) {
            activity.recentUpdateSummary.setText("还没有更新记录。录入一次最新金额后，这里会显示变化。");
            return;
        }

        activity.recentUpdateSummary.setText(recentUpdateSummaryText());
        List<String> reasonLines = updateReasonSummaryLines(true);
        for (String line : reasonLines) {
            activity.recentUpdateList.addView(reasonSummaryRow(line));
        }
        int limit = Math.min(5, activity.updateEvents.size());
        for (int index = 0; index < limit; index += 1) {
            activity.recentUpdateList.addView(updateEventRow(activity.updateEvents.get(index)));
        }
        if (activity.updateEvents.size() > limit) {
            TextView more = activity.text("还有 " + (activity.updateEvents.size() - limit) + " 条更新记录会随备份保留。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = activity.lp(-1, -2);
            moreParams.topMargin = activity.dp(8);
            activity.recentUpdateList.addView(more, moreParams);
        }
    }

    String recentUpdateSummaryText() {
        long cutoff = System.currentTimeMillis() - 30L * AssetMath.DAY_MS;
        int count = 0;
        double deltaInBase = 0;
        for (AssetUpdateEvent event : activity.updateEvents) {
            if (event.timestamp < cutoff) {
                continue;
            }
            count += 1;
            String currency = AssetMath.cleanCurrency(event.currency);
            double rate = activity.settings.hasRateFor(currency) ? activity.settings.rateFor(currency) : 1.0;
            double previous = AssetMath.parseAmount(event.previousAmount);
            double current = AssetMath.parseAmount(event.newAmount);
            deltaInBase += (current - previous) * rate;
        }

        if (count == 0) {
            return "保留最近一年更新记录；近 30 天还没有新的金额变化。";
        }
        if (activity.settings.hideAmounts) {
            return "近 30 天记录 " + count + " 次更新，金额变化已隐藏。";
        }
        return "近 30 天记录 " + count + " 次更新，折算净变化 "
                + activity.formatSignedMoney(deltaInBase, activity.settings.baseCurrency) + "。";
    }

    List<String> updateReasonSummaryLines(boolean includeEmpty) {
        long cutoff = System.currentTimeMillis() - 30L * AssetMath.DAY_MS;
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Double> deltas = new HashMap<>();
        for (AssetUpdateEvent event : activity.updateEvents) {
            if (event.timestamp < cutoff) {
                continue;
            }
            String reason = activity.cleanReason(event.reason);
            counts.put(reason, activity.intValue(counts, reason) + 1);

            String currency = AssetMath.cleanCurrency(event.currency);
            double rate = activity.settings.hasRateFor(currency) ? activity.settings.rateFor(currency) : 1.0;
            double previous = AssetMath.parseAmount(event.previousAmount);
            double current = AssetMath.parseAmount(event.newAmount);
            deltas.put(reason, activity.doubleValue(deltas, reason) + (current - previous) * rate);
        }

        List<String> reasons = new ArrayList<>(counts.keySet());
        Collections.sort(reasons, (left, right) -> {
            int countCompare = Integer.compare(activity.intValue(counts, right), activity.intValue(counts, left));
            if (countCompare != 0) {
                return countCompare;
            }
            return left.compareToIgnoreCase(right);
        });

        List<String> lines = new ArrayList<>();
        int limit = Math.min(3, reasons.size());
        for (int index = 0; index < limit; index += 1) {
            String reason = reasons.get(index);
            String line = reason + " " + activity.intValue(counts, reason) + " 次";
            if (!activity.settings.hideAmounts) {
                line += "，折算变化 " + activity.formatSignedMoney(activity.doubleValue(deltas, reason), activity.settings.baseCurrency);
            }
            lines.add(line);
        }

        if (lines.isEmpty() && includeEmpty) {
            lines.add("近 30 天还没有可汇总的变化原因。");
        }
        return lines;
    }

    private View reasonSummaryRow(String line) {
        TextView row = activity.text("原因汇总 · " + line, 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        row.setPadding(activity.dp(12), activity.dp(8), activity.dp(12), activity.dp(8));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams params = activity.lp(-1, -2);
        params.topMargin = activity.dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private View updateEventRow(AssetUpdateEvent event) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        TextView name = activity.text(event.assetName.isEmpty() ? "未知资产" : event.assetName, 14, MoneyManagerActivity.INK, Typeface.BOLD);
        header.addView(name, new LinearLayout.LayoutParams(0, -2, 1));

        TextView time = activity.text(activity.dateFormat.format(new Date(event.timestamp)), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        time.setGravity(Gravity.END);
        header.addView(time);
        row.addView(header);

        TextView change = activity.text(updateEventChangeText(event), 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams changeParams = activity.lp(-1, -2);
        changeParams.topMargin = activity.dp(6);
        row.addView(change, changeParams);

        LinearLayout.LayoutParams reasonParams = activity.lp(-1, -2);
        reasonParams.topMargin = activity.dp(4);
        row.addView(activity.text("原因：" + activity.cleanReason(event.reason), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), reasonParams);

        if (!event.note.isEmpty()) {
            LinearLayout.LayoutParams noteParams = activity.lp(-1, -2);
            noteParams.topMargin = activity.dp(4);
            row.addView(activity.text(event.note, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), noteParams);
        }

        Button delete = activity.secondaryButton("删除记录");
        delete.setTextColor(MoneyManagerActivity.DANGER);
        delete.setOnClickListener(view -> confirmDeleteUpdateEvent(event));
        LinearLayout.LayoutParams deleteParams = activity.lp(-1, activity.dp(38));
        deleteParams.topMargin = activity.dp(8);
        row.addView(delete, deleteParams);
        return row;
    }

    private void confirmDeleteUpdateEvent(AssetUpdateEvent event) {
        String assetName = event.assetName.isEmpty() ? "这条资产" : event.assetName;
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("删除更新记录？")
                .setMessage("确定删除「" + assetName + "」这条更新记录吗？这只删除历史记录，不会回滚资产金额或趋势快照。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (ignoredDialog, which) -> {
                    activity.updateEvents = activity.store.deleteUpdateEvent(event.assetId, event.timestamp);
                    activity.render();
                    activity.toast("已删除更新记录。");
                })
                .create();
        activity.showStyledDialog(dialog);
    }

    private String updateEventChangeText(AssetUpdateEvent event) {
        if (activity.settings.hideAmounts) {
            return "金额变化已隐藏 · " + event.currency;
        }
        String before = event.previousAmount.isEmpty() ? "--" : activity.formatRawAmount(event.previousAmount);
        String after = event.newAmount.isEmpty() ? "--" : activity.formatRawAmount(event.newAmount);
        double previous = AssetMath.parseAmount(event.previousAmount);
        double current = AssetMath.parseAmount(event.newAmount);
        double delta = current - previous;
        return before + " -> " + after + " " + event.currency
                + "（变化 " + activity.formatSignedRawAmount(delta) + " " + event.currency + "）";
    }
}
