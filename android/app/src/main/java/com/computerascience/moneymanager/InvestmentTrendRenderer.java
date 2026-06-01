package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.ui.TrendChartView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class InvestmentTrendRenderer {
    private final MainActivity activity;

    InvestmentTrendRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资趋势"));

        activity.investmentTrendSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentTrendSummary, summaryParams);

        activity.investmentTrendChart = new TrendChartView(activity);
        LinearLayout.LayoutParams chartParams = activity.lp(-1, activity.dp(150));
        chartParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentTrendChart, chartParams);

        activity.investmentTrendList = new LinearLayout(activity);
        activity.investmentTrendList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentTrendList, activity.lp(-1, -2));
        return card;
    }

    void render() {
        List<TrendChartView.Point> points = investmentPoints();
        activity.investmentTrendChart.setPoints(points, "记录两次投资分布后显示趋势");
        activity.investmentTrendSummary.setText(summaryText(points));
        renderMetrics(points);
    }

    private List<TrendChartView.Point> investmentPoints() {
        List<TrendChartView.Point> points = new ArrayList<>();
        for (AssetSnapshot snapshot : activity.snapshots) {
            if (!activity.settings.baseCurrency.equals(snapshot.baseCurrency)) {
                continue;
            }
            double value = investmentValue(snapshot);
            if (value > 0) {
                points.add(new TrendChartView.Point(snapshot.timestamp, value));
            }
        }
        return points;
    }

    private double investmentValue(AssetSnapshot snapshot) {
        double value = 0;
        for (Map.Entry<String, Double> entry : snapshot.categoryValues.entrySet()) {
            if (activity.settings.isInvestmentCategory(entry.getKey())) {
                value += entry.getValue();
            }
        }
        return value;
    }

    private String summaryText(List<TrendChartView.Point> points) {
        if (points.size() < 2) {
            return "已记录 " + points.size() + " 个投资分布点；继续更新投资资产后会形成投资趋势。";
        }
        if (activity.settings.hideAmounts) {
            return "已记录 " + points.size() + " 个投资分布点；隐私模式下金额变化暂不显示。";
        }
        TrendChartView.Point first = points.get(0);
        TrendChartView.Point last = points.get(points.size() - 1);
        double change = last.value - first.value;
        double ratio = Math.abs(first.value) < 0.0001 ? 0 : change / Math.abs(first.value) * 100;
        return "投资资产从 " + activity.formatMoney(first.value, activity.settings.baseCurrency)
                + " 到 " + activity.formatMoney(last.value, activity.settings.baseCurrency)
                + "，变化 " + activity.formatSignedMoney(change, activity.settings.baseCurrency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private void renderMetrics(List<TrendChartView.Point> points) {
        activity.investmentTrendList.removeAllViews();
        addMetricRow(points, "近 30 天", 30);
        addMetricRow(points, "近 90 天", 90);
        addMetricRow(points, "近一年", 365);
    }

    private void addMetricRow(List<TrendChartView.Point> points, String label, int days) {
        long cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L;
        TrendChartView.Point first = null;
        TrendChartView.Point last = null;
        int count = 0;
        for (TrendChartView.Point point : points) {
            if (point.timestamp < cutoff) {
                continue;
            }
            if (first == null) {
                first = point;
            }
            last = point;
            count += 1;
        }
        if (first == null || last == null || count < 2) {
            activity.investmentTrendList.addView(metricRow(label, "数据不足", "需要至少两个带投资分布的快照。", MoneyManagerActivity.AMBER));
            return;
        }

        double change = last.value - first.value;
        double ratio = Math.abs(first.value) < 0.0001 ? 0 : change / Math.abs(first.value) * 100;
        String value = activity.settings.hideAmounts
                ? count + " 个点"
                : activity.formatSignedMoney(change, activity.settings.baseCurrency);
        String detail = activity.settings.hideAmounts
                ? "金额已隐藏；区间内记录 " + count + " 个投资分布点。"
                : "区间变化 " + String.format(Locale.getDefault(), "%+.1f", ratio) + "%，记录 "
                + count + " 个投资分布点。";
        activity.investmentTrendList.addView(metricRow(label, value, detail,
                change >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER));
    }

    private View metricRow(String title, String value, String detail, int color) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        TextView dot = activity.text("●", 15, color, Typeface.BOLD);
        header.addView(dot);
        TextView label = activity.text("  " + title, 14, MoneyManagerActivity.INK, Typeface.BOLD);
        header.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView metric = activity.text(value, 14, MoneyManagerActivity.MUTED, Typeface.BOLD);
        metric.setGravity(Gravity.RIGHT);
        header.addView(metric);
        row.addView(header);

        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(6);
        row.addView(activity.text(detail, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);
        return row;
    }
}
