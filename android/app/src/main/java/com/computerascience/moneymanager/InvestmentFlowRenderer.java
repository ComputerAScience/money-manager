package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.UpdateAnalytics;
import com.computerascience.moneymanager.model.AssetRecord;

import java.util.List;

final class InvestmentFlowRenderer {
    private final MainActivity activity;

    InvestmentFlowRenderer(MainActivity activity) {
        this.activity = activity;
    }

    void render(List<AssetRecord> investments) {
        activity.investmentFlowList.removeAllViews();
        UpdateAnalytics.Summary summary = UpdateAnalytics.summarizeKnownAssets(activity.updateEvents, investments, activity.settings, 90);
        UpdateAnalytics.Breakdown breakdown = UpdateAnalytics.breakdownKnownAssets(activity.updateEvents, investments, activity.settings, 90);
        if (investments.isEmpty()) {
            activity.investmentFlowSummary.setText("还没有投资资产。");
            activity.investmentFlowList.addView(activity.emptyText("新增投资资产后，这里会按投资账户回看变化。"));
            return;
        }
        if (summary.count == 0) {
            activity.investmentFlowSummary.setText("近 90 天还没有投资资产更新记录。");
            activity.investmentFlowList.addView(activity.emptyText("更新几次投资资产金额后，这里会按原因、类型和机构拆解。"));
            return;
        }

        if (activity.settings.hideAmounts) {
            activity.investmentFlowSummary.setText("近 " + summary.days + " 天记录 "
                    + summary.count + " 次投资更新，金额已隐藏。");
        } else {
            activity.investmentFlowSummary.setText("近 " + summary.days + " 天投资净变化 "
                    + activity.formatSignedMoney(summary.delta, activity.settings.baseCurrency)
                    + "；流入 " + activity.formatMoney(summary.increase, activity.settings.baseCurrency)
                    + "，流出 " + activity.formatMoney(Math.abs(summary.decrease), activity.settings.baseCurrency) + "。");
        }

        addPerformanceRows(breakdown);
        FlowSectionRenderer.add(activity, activity.investmentFlowList, "按原因", summary.reasons);
        FlowSectionRenderer.add(activity, activity.investmentFlowList, "按资产", summary.assets);
        FlowSectionRenderer.add(activity, activity.investmentFlowList, "按类型", summary.categories);
        FlowSectionRenderer.add(activity, activity.investmentFlowList, "按机构", summary.institutions);
    }

    String summaryText(UpdateAnalytics.Summary summary) {
        if (summary.count == 0) {
            return "没有投资更新记录，先完成一次金额核对。";
        }
        if (activity.settings.hideAmounts) {
            return "金额已隐藏；其中仅更新时间 " + summary.flatCount + " 次。";
        }
        return "净变化 " + activity.formatSignedMoney(summary.delta, activity.settings.baseCurrency)
                + "，流入 " + activity.formatMoney(summary.increase, activity.settings.baseCurrency)
                + "，流出 " + activity.formatMoney(Math.abs(summary.decrease), activity.settings.baseCurrency)
                + "。";
    }

    private void addPerformanceRows(UpdateAnalytics.Breakdown breakdown) {
        if (breakdown.count == 0) {
            return;
        }

        if (activity.settings.hideAmounts) {
            activity.investmentFlowList.addView(infoRow(
                    "表现拆解",
                    breakdown.count + " 次",
                    "估算收益 " + breakdown.performanceCount + " 次，外部资金 "
                            + breakdown.externalFlowCount + " 次，交易/核对 "
                            + (breakdown.tradeCount + breakdown.reconcileCount) + " 次；金额已隐藏。",
                    MoneyManagerActivity.BLUE
            ));
            return;
        }

        activity.investmentFlowList.addView(infoRow(
                "估算收益",
                activity.formatSignedMoney(breakdown.performance, activity.settings.baseCurrency),
                "按市场涨跌、利息分红、手续费税费等原因粗略归类；不等同于严格收益率。",
                breakdown.performance >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER
        ));
        activity.investmentFlowList.addView(infoRow(
                "外部资金",
                activity.formatSignedMoney(breakdown.externalFlow, activity.settings.baseCurrency),
                "入金 " + activity.formatMoney(breakdown.externalInflow, activity.settings.baseCurrency)
                        + "，出金 " + activity.formatMoney(Math.abs(breakdown.externalOutflow), activity.settings.baseCurrency)
                        + "；用于区分投入/取出和资产自身变化。",
                breakdown.externalFlow >= 0 ? MoneyManagerActivity.BLUE : MoneyManagerActivity.AMBER
        ));
        activity.investmentFlowList.addView(infoRow(
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
