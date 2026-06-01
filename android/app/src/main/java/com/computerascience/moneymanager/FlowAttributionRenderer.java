package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.UpdateAnalytics;

final class FlowAttributionRenderer {
    private final MainActivity activity;

    FlowAttributionRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
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

    void render() {
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
                    + summary.count + " 次更新，净资产影响 "
                    + activity.formatSignedMoney(summary.delta, activity.settings.baseCurrency)
                    + "；正向影响 " + activity.formatMoney(summary.increase, activity.settings.baseCurrency)
                    + "，负向影响 " + activity.formatMoney(Math.abs(summary.decrease), activity.settings.baseCurrency) + "。");
        }

        addBreakdownRows(UpdateAnalytics.breakdown(activity.updateEvents, activity.assets, activity.settings, 90));
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按原因", summary.reasons);
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按类型", summary.categories);
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按机构", summary.institutions);
    }

    private void addBreakdownRows(UpdateAnalytics.Breakdown breakdown) {
        if (breakdown.count == 0) {
            return;
        }

        if (activity.settings.hideAmounts) {
            activity.flowAttributionList.addView(infoRow(
                    "资金流拆解",
                    breakdown.count + " 次",
                    "外部资金 " + breakdown.externalFlowCount + " 次，估算资产变化 "
                            + breakdown.performanceCount + " 次，负债变化 " + breakdown.debtCount
                            + " 次，交易/核对 " + (breakdown.tradeCount + breakdown.reconcileCount) + " 次。",
                    MoneyManagerActivity.BLUE
            ));
            return;
        }

        activity.flowAttributionList.addView(infoRow(
                "外部资金",
                activity.formatSignedMoney(breakdown.externalFlow, activity.settings.baseCurrency),
                "入金、出金、转账归在这里，用来区分主动投入/取出和资产自身变化。",
                breakdown.externalFlow >= 0 ? MoneyManagerActivity.BLUE : MoneyManagerActivity.AMBER
        ));
        activity.flowAttributionList.addView(infoRow(
                "估算资产变化",
                activity.formatSignedMoney(breakdown.performance, activity.settings.baseCurrency),
                "市场涨跌、利息分红、手续费税费等原因粗略归类为资产自身变化。",
                breakdown.performance >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER
        ));
        activity.flowAttributionList.addView(infoRow(
                "负债影响",
                activity.formatSignedMoney(breakdown.debt, activity.settings.baseCurrency),
                "负债增加会降低净资产，负债下降会提高净资产。",
                breakdown.debt >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER
        ));
        activity.flowAttributionList.addView(infoRow(
                "交易/核对",
                activity.formatSignedMoney(breakdown.trade + breakdown.reconcile, activity.settings.baseCurrency),
                "买入卖出、余额核对、仅更新时间和其他原因放在这里，避免误算成收益。",
                Math.abs(breakdown.trade + breakdown.reconcile) > 0.0001
                        ? MoneyManagerActivity.AMBER
                        : MoneyManagerActivity.ACCENT
        ));
    }

    private View infoRow(String title, String value, String detail, int color) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        header.addView(activity.text("●", 15, color, Typeface.BOLD));
        header.addView(activity.text("  " + title, 14, MoneyManagerActivity.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
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
