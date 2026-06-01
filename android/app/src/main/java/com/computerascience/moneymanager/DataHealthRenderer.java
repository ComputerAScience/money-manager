package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AllocationAnalytics;
import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.DataHealth;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DataHealthRenderer {
    private final MainActivity activity;

    DataHealthRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
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

    void render(PortfolioSummary portfolio) {
        activity.insightSummary.setText(buildInsightText(portfolio));
        renderDataHealth(portfolio);
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
                addAllocationInsight(lines, drifts.get(0));
            }
        }
        if (activity.settings.hasNetWorthTarget()) {
            addNetWorthTargetInsight(lines, portfolio);
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

    private void addAllocationInsight(List<String> lines, AllocationAnalytics.Drift largestDrift) {
        double gap = largestDrift.targetPercent - largestDrift.currentPercent;
        if (Math.abs(gap) >= 5) {
            lines.add("比例偏离最大：" + largestDrift.category + " "
                    + (gap > 0 ? "低配 " : "超配 ")
                    + activity.formatPercentValue(Math.abs(gap)) + "。");
        }
    }

    private void addNetWorthTargetInsight(List<String> lines, PortfolioSummary portfolio) {
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

    private void renderDataHealth(PortfolioSummary portfolio) {
        activity.dataHealthList.removeAllViews();
        List<String> issues = DataHealth.portfolioIssues(activity.assets, activity.settings, portfolio.baseCurrency);
        if (issues.isEmpty()) {
            activity.dataHealthSummary.setText("数据状态良好：金额、机构、App 绑定和汇率都已覆盖。");
            activity.dataHealthList.addView(activity.text("继续保持定期核对即可。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL));
            return;
        }

        List<AssetInstitutionGroups.Group> groups = DataHealth.issueGroups(activity.assets, activity.settings);
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
}
