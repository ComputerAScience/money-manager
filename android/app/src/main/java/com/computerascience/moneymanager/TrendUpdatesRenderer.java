package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.UpdateAnalytics;
import com.computerascience.moneymanager.model.AssetUpdateEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class TrendUpdatesRenderer {
    private final MainActivity activity;

    TrendUpdatesRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
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

    void render() {
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
        UpdateAnalytics.Summary summary = UpdateAnalytics.summarize(activity.updateEvents, activity.assets, activity.settings, 30);
        if (summary.count == 0) {
            return "保留最近一年更新记录；近 30 天还没有新的金额变化。";
        }
        if (activity.settings.hideAmounts) {
            return "近 30 天记录 " + summary.count + " 次更新，金额变化已隐藏。";
        }
        return "近 30 天记录 " + summary.count + " 次更新，折算净资产影响 "
                + activity.formatSignedMoney(summary.delta, activity.settings.baseCurrency) + "。";
    }

    List<String> updateReasonSummaryLines(boolean includeEmpty) {
        UpdateAnalytics.Summary summary = UpdateAnalytics.summarize(activity.updateEvents, activity.assets, activity.settings, 30);
        List<String> lines = new ArrayList<>();
        int limit = Math.min(3, summary.reasons.size());
        for (int index = 0; index < limit; index += 1) {
            UpdateAnalytics.Bucket bucket = summary.reasons.get(index);
            String line = bucket.label + " " + bucket.count + " 次";
            if (!activity.settings.hideAmounts) {
                line += "，净资产影响 " + activity.formatSignedMoney(bucket.delta, activity.settings.baseCurrency);
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

    View updateEventRow(AssetUpdateEvent event) {
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
