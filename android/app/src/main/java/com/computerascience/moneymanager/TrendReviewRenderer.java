package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.domain.UpdateAnalytics;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.List;
import java.util.Locale;

final class TrendReviewRenderer {
    private final MainActivity activity;

    TrendReviewRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View monthlyReviewCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("月度复盘"));

        activity.monthlyReviewSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.monthlyReviewSummary, summaryParams);

        activity.monthlyReviewList = new LinearLayout(activity);
        activity.monthlyReviewList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.monthlyReviewList, activity.lp(-1, -2));
        return card;
    }

    void renderMonthlyReview(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        activity.monthlyReviewList.removeAllViews();
        TrendAnalytics.MonthlyReview review = TrendAnalytics.monthlyReview(
                portfolio,
                trendSnapshots,
                System.currentTimeMillis(),
                30
        );
        UpdateAnalytics.Summary updates = UpdateAnalytics.summarize(activity.updateEvents, activity.assets, activity.settings, 30);

        if (!review.complete && updates.count == 0) {
            activity.monthlyReviewSummary.setText("近 30 天快照和更新流水都还不够，先记录几次资产变化后再复盘。");
            activity.monthlyReviewList.addView(activity.emptyText("建议每次集中核对后记录一次快照。"));
            return;
        }

        activity.monthlyReviewSummary.setText(monthlyReviewSummaryText(review, updates));
        if (review.complete) {
            activity.monthlyReviewList.addView(reviewRow(
                    "净资产变化",
                    activity.settings.hideAmounts
                            ? review.snapshotCount + " 个快照"
                            : activity.formatSignedMoney(review.netWorthDelta, review.currency),
                    review.first.dayKey + " 到 " + review.last.dayKey
                            + "，变化 " + String.format(Locale.getDefault(), "%+.1f%%", review.netWorthRatio) + "。",
                    review.netWorthDelta >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER
            ));
            activity.monthlyReviewList.addView(reviewRow(
                    "资产 / 负债",
                    activity.settings.hideAmounts ? "金额已隐藏" : activity.formatSignedMoney(review.grossAssetsDelta, review.currency)
                            + " / " + activity.formatSignedMoney(review.liabilitiesDelta, review.currency),
                    "资产总额与负债在同一窗口内的变化，用来区分增长来自资产还是杠杆。",
                    Math.abs(review.liabilitiesDelta) > Math.abs(review.grossAssetsDelta) ? MoneyManagerActivity.AMBER : MoneyManagerActivity.BLUE
            ));
        } else {
            activity.monthlyReviewList.addView(activity.emptyText("近 30 天只有 " + review.snapshotCount + " 个快照，暂时无法计算月度净资产变化。"));
        }

        activity.monthlyReviewList.addView(reviewRow(
                "更新流水",
                updates.count == 0 ? "暂无" : updates.count + " 次",
                updateWindowText(updates),
                updates.count == 0 ? MoneyManagerActivity.AMBER : MoneyManagerActivity.ACCENT
        ));

        UpdateAnalytics.Bucket topReason = topBucket(updates.reasons);
        UpdateAnalytics.Bucket topInstitution = topBucket(updates.institutions);
        String sourceDetail = topReason == null
                ? "近 30 天还没有可归因的更新。"
                : "主因 " + topReason.label + "，主要机构 "
                + (topInstitution == null ? "未填写机构" : topInstitution.label) + "。";
        String sourceValue = topReason == null ? "--" : topReason.label;
        activity.monthlyReviewList.addView(reviewRow(
                "主要变化来源",
                sourceValue,
                sourceDetail,
                MoneyManagerActivity.BLUE
        ));

        if (review.complete && updates.count > 0 && !activity.settings.hideAmounts) {
            double unexplained = review.netWorthDelta - updates.delta;
            activity.monthlyReviewList.addView(reviewRow(
                    "快照 vs 流水",
                    activity.formatSignedMoney(unexplained, review.currency),
                    Math.abs(unexplained) <= Math.max(1, Math.abs(review.netWorthDelta) * 0.15)
                            ? "流水基本能解释本月净资产变化。"
                            : "差额较大，可能来自市场涨跌、汇率变化或未记录的资产更新。",
                    Math.abs(unexplained) <= Math.max(1, Math.abs(review.netWorthDelta) * 0.15)
                            ? MoneyManagerActivity.ACCENT
                            : MoneyManagerActivity.AMBER
            ));
        }
    }

    private String monthlyReviewSummaryText(TrendAnalytics.MonthlyReview review, UpdateAnalytics.Summary updates) {
        if (!review.complete) {
            return "近 30 天只有 " + review.snapshotCount + " 个快照；更新流水 "
                    + updates.count + " 次。";
        }
        if (activity.settings.hideAmounts) {
            return "近 30 天记录 " + review.snapshotCount + " 个快照，"
                    + updates.count + " 次更新；金额已隐藏。";
        }
        return "近 30 天净资产 " + activity.formatSignedMoney(review.netWorthDelta, review.currency)
                + "，更新流水净资产影响 "
                + activity.formatSignedMoney(updates.delta, activity.settings.baseCurrency)
                + "。";
    }

    private String updateWindowText(UpdateAnalytics.Summary updates) {
        if (updates.count == 0) {
            return "没有更新记录，复盘只能依赖快照。";
        }
        if (activity.settings.hideAmounts) {
            return "近 " + updates.days + " 天有 " + updates.count + " 次更新，金额已隐藏。";
        }
        return "正向影响 " + activity.formatMoney(updates.increase, activity.settings.baseCurrency)
                + "，负向影响 " + activity.formatMoney(Math.abs(updates.decrease), activity.settings.baseCurrency)
                + "，仅更新时间 " + updates.flatCount + " 次。";
    }

    private UpdateAnalytics.Bucket topBucket(List<UpdateAnalytics.Bucket> buckets) {
        return buckets.isEmpty() ? null : buckets.get(0);
    }

    private View reviewRow(String title, String value, String detail, int color) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        TextView marker = activity.text("●", 15, color, Typeface.BOLD);
        header.addView(marker);
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
