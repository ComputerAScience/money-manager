package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.SpaceView;
import com.computerascience.moneymanager.ui.TrendChartView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class TrendSnapshotRenderer {
    private final MainActivity activity;

    TrendSnapshotRenderer(MainActivity activity) {
        this.activity = activity;
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

    void render(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        activity.trendChart.setSnapshots(trendSnapshots);
        activity.trendSummary.setText(summaryText(portfolio, trendSnapshots));
        renderTrendMetrics(portfolio, trendSnapshots);
        renderTrendHistory(trendSnapshots);
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
            activity.trendHistoryList.addView(snapshotRow(trendSnapshots.get(index)));
        }
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
            save.setOnClickListener(button -> saveBackfillSnapshot(dialog, portfolio, day, netWorth, grossAssets, liabilities));
        });

        activity.showStyledDialog(dialog);
    }

    private void saveBackfillSnapshot(
            AlertDialog dialog,
            PortfolioSummary portfolio,
            EditText day,
            EditText netWorth,
            EditText grossAssets,
            EditText liabilities
    ) {
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
    }
}
