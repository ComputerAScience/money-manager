package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.List;

final class TargetProgressRenderer {
    private static final double MONTH_DAYS = 30.4375;

    private final MainActivity activity;

    TargetProgressRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View targetProgressCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("目标追踪"));

        activity.targetProgressSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.targetProgressSummary, summaryParams);

        activity.targetProgressList = new LinearLayout(activity);
        activity.targetProgressList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.targetProgressList, activity.lp(-1, -2));

        Button editButton = activity.secondaryButton("调整年度目标");
        editButton.setOnClickListener(view -> activity.showNetWorthGoalDialog());
        LinearLayout.LayoutParams buttonParams = activity.lp(-1, activity.dp(42));
        buttonParams.topMargin = activity.dp(10);
        card.addView(editButton, buttonParams);
        return card;
    }

    void render(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        activity.targetProgressList.removeAllViews();
        if (!activity.settings.hasNetWorthTarget()) {
            activity.targetProgressSummary.setText("还没有设置年度目标。设置后这里会结合历史快照估算达成节奏。");
            activity.targetProgressList.addView(activity.emptyText("先设置目标净资产和目标日期。"));
            return;
        }

        TargetSpeed speed = targetSpeed(trendSnapshots);
        if (activity.settings.hideAmounts) {
            activity.targetProgressSummary.setText("年度目标已设置到 "
                    + activity.dayKey(activity.settings.netWorthTargetDate)
                    + "；隐私模式已开启，金额、进度和预测暂不显示。");
            activity.targetProgressList.addView(progressRow(
                    "数据基础",
                    trendSnapshots.size() + " 个快照",
                    speed.complete
                            ? "已有足够快照计算趋势速度，关闭隐私模式后可查看目标预测。"
                            : "至少需要两次快照才能估算目标推进速度。",
                    speed.complete ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.AMBER
            ));
            return;
        }

        double target = activity.settings.netWorthTarget;
        double gap = target - portfolio.netWorth;
        double progress = target <= 0 ? 0 : portfolio.netWorth / target * 100;
        int daysLeft = activity.daysUntilTimestamp(activity.settings.netWorthTargetDate);
        double monthsLeft = Math.max(1.0, daysLeft / MONTH_DAYS);
        double requiredMonthly = gap <= 0 ? 0 : gap / monthsLeft;

        activity.targetProgressSummary.setText(summaryText(portfolio, gap, progress, speed));
        activity.targetProgressList.addView(progressRow(
                "达成率",
                activity.formatPercentValue(progress),
                "目标日 " + activity.dayKey(activity.settings.netWorthTargetDate)
                        + "，目标 " + activity.formatMoney(target, portfolio.baseCurrency) + "。",
                progress >= 100 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.BLUE
        ));
        activity.targetProgressList.addView(progressRow(
                "目标缺口",
                gap <= 0 ? "已达到" : activity.formatMoney(gap, portfolio.baseCurrency),
                gap <= 0 ? "当前净资产已经达到或超过年度目标。"
                        : (daysLeft <= 0 ? "目标日已到，仍有缺口。"
                        : "剩余 " + daysLeft + " 天，约每月需要增加 "
                        + activity.formatMoney(requiredMonthly, portfolio.baseCurrency) + "。"),
                gap <= 0 ? MoneyManagerActivity.ACCENT : (daysLeft <= 0 ? MoneyManagerActivity.DANGER : MoneyManagerActivity.AMBER)
        ));

        if (!speed.complete) {
            activity.targetProgressList.addView(progressRow(
                    "当前速度",
                    "快照不足",
                    "至少记录两次快照后，才能估算月均变化和预计达成时间。",
                    MoneyManagerActivity.AMBER
            ));
            return;
        }

        activity.targetProgressList.addView(progressRow(
                "历史速度",
                activity.formatSignedMoney(speed.monthlyChange, portfolio.baseCurrency) + " / 月",
                speed.first.dayKey + " 到 " + speed.last.dayKey
                        + "，按净资产快照估算的月均变化。",
                speed.monthlyChange >= requiredMonthly ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.AMBER
        ));
        activity.targetProgressList.addView(progressRow(
                "预计达成",
                projectedDateText(gap, speed.monthlyChange),
                projectionDetail(portfolio, gap, daysLeft, speed.monthlyChange),
                projectionColor(gap, daysLeft, speed.monthlyChange)
        ));
    }

    private String summaryText(PortfolioSummary portfolio, double gap, double progress, TargetSpeed speed) {
        if (gap <= 0) {
            return "年度目标已达成，当前进度 " + activity.formatPercentValue(progress) + "。";
        }
        if (!speed.complete) {
            return "当前进度 " + activity.formatPercentValue(progress)
                    + "，还差 " + activity.formatMoney(gap, portfolio.baseCurrency)
                    + "；继续记录快照后会估算达成时间。";
        }
        return "当前进度 " + activity.formatPercentValue(progress)
                + "，还差 " + activity.formatMoney(gap, portfolio.baseCurrency)
                + "；按历史速度预计 " + projectedDateText(gap, speed.monthlyChange) + "。";
    }

    private String projectedDateText(double gap, double monthlyChange) {
        if (gap <= 0) {
            return "已达到";
        }
        if (monthlyChange <= 0.0001) {
            return "速度不足";
        }
        double days = gap / monthlyChange * MONTH_DAYS;
        if (days > 3650) {
            return "超过 10 年";
        }
        long timestamp = System.currentTimeMillis() + Math.round(days * AssetMath.DAY_MS);
        return activity.dayKey(timestamp);
    }

    private String projectionDetail(PortfolioSummary portfolio, double gap, int daysLeft, double monthlyChange) {
        if (gap <= 0) {
            return "可以继续提高目标，或把目标改成阶段性资产配置目标。";
        }
        if (monthlyChange <= 0.0001) {
            return "历史月均变化不为正，按当前趋势无法自动追上目标。";
        }

        double projectedAtTargetDate = portfolio.netWorth + monthlyChange * Math.max(0, daysLeft) / MONTH_DAYS;
        double projectedGap = activity.settings.netWorthTarget - projectedAtTargetDate;
        if (projectedGap <= 0) {
            return "按历史速度，到目标日预计可超过目标 "
                    + activity.formatMoney(Math.abs(projectedGap), portfolio.baseCurrency) + "。";
        }
        return "按历史速度，到目标日预计还差 "
                + activity.formatMoney(projectedGap, portfolio.baseCurrency) + "。";
    }

    private int projectionColor(double gap, int daysLeft, double monthlyChange) {
        if (gap <= 0) {
            return MoneyManagerActivity.ACCENT;
        }
        if (monthlyChange <= 0.0001 || daysLeft <= 0) {
            return MoneyManagerActivity.DANGER;
        }
        double daysToTarget = gap / monthlyChange * MONTH_DAYS;
        return daysToTarget <= daysLeft ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.AMBER;
    }

    private TargetSpeed targetSpeed(List<AssetSnapshot> snapshots) {
        if (snapshots.size() < 2) {
            return new TargetSpeed();
        }
        AssetSnapshot first = snapshots.get(0);
        AssetSnapshot last = snapshots.get(snapshots.size() - 1);
        double days = Math.max(1.0, (last.timestamp - first.timestamp) / (double) AssetMath.DAY_MS);
        double monthlyChange = (last.netWorth - first.netWorth) / days * MONTH_DAYS;
        return new TargetSpeed(true, first, last, monthlyChange);
    }

    private View progressRow(String title, String value, String detail, int color) {
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

    private static final class TargetSpeed {
        final boolean complete;
        final AssetSnapshot first;
        final AssetSnapshot last;
        final double monthlyChange;

        TargetSpeed() {
            this(false, null, null, 0);
        }

        TargetSpeed(boolean complete, AssetSnapshot first, AssetSnapshot last, double monthlyChange) {
            this.complete = complete;
            this.first = first;
            this.last = last;
            this.monthlyChange = monthlyChange;
        }
    }
}
