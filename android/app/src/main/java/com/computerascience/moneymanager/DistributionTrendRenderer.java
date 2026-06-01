package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.model.AssetSnapshot;

import java.util.List;

final class DistributionTrendRenderer {
    private final MainActivity activity;

    DistributionTrendRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
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

    void render(List<AssetSnapshot> trendSnapshots) {
        activity.distributionTrendList.removeAllViews();
        List<AssetSnapshot> categorySnapshots = TrendAnalytics.snapshotsWithCategoryValues(trendSnapshots);
        List<AssetSnapshot> institutionSnapshots = TrendAnalytics.snapshotsWithInstitutionValues(trendSnapshots);
        if (categorySnapshots.size() < 2 && institutionSnapshots.size() < 2) {
            int count = Math.max(categorySnapshots.size(), institutionSnapshots.size());
            activity.distributionTrendSummary.setText("已记录 " + count + " 个带分布的快照；从这版开始，每次更新会同时保存类型和机构分布。");
            TextView empty = activity.text("再记录一次快照后，这里会显示资产类型、机构金额和占比的变化。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.distributionTrendList.addView(empty, emptyParams);
            return;
        }

        activity.distributionTrendSummary.setText(summaryText(categorySnapshots, institutionSnapshots));
        if (categorySnapshots.size() >= 2) {
            addShiftSection("类型变化", categorySnapshots, true);
        }
        if (institutionSnapshots.size() >= 2) {
            addShiftSection("机构变化", institutionSnapshots, false);
        }
    }

    private String summaryText(List<AssetSnapshot> categorySnapshots, List<AssetSnapshot> institutionSnapshots) {
        List<AssetSnapshot> source = categorySnapshots.size() >= 2 ? categorySnapshots : institutionSnapshots;
        AssetSnapshot first = source.get(0);
        AssetSnapshot last = source.get(source.size() - 1);
        if (activity.settings.hideAmounts) {
            return "已记录 " + source.size() + " 个带分布快照，范围 "
                    + first.dayKey + " 到 " + last.dayKey + "；金额已隐藏。";
        }
        List<TrendAnalytics.CategoryShift> shifts = categorySnapshots.size() >= 2
                ? TrendAnalytics.categoryShifts(first, last)
                : TrendAnalytics.institutionShifts(first, last);
        if (shifts.isEmpty()) {
            return "已记录 " + source.size() + " 个带分布快照；继续更新资产后会展示变化。";
        }
        TrendAnalytics.CategoryShift biggest = shifts.get(0);
        return "从 " + first.dayKey + " 到 " + last.dayKey
                + "，分布变化最大的是 " + biggest.category + "："
                + activity.formatSignedMoney(biggest.delta, last.baseCurrency)
                + "，占比 " + activity.formatPoint(biggest.percentDelta) + "。";
    }

    private void addShiftSection(String title, List<AssetSnapshot> snapshots, boolean category) {
        AssetSnapshot first = snapshots.get(0);
        AssetSnapshot last = snapshots.get(snapshots.size() - 1);
        List<TrendAnalytics.CategoryShift> shifts = category
                ? TrendAnalytics.categoryShifts(first, last)
                : TrendAnalytics.institutionShifts(first, last);
        if (shifts.isEmpty()) {
            return;
        }

        TextView heading = activity.text(title, 13, MoneyManagerActivity.MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = activity.lp(-1, -2);
        headingParams.topMargin = activity.dp(10);
        activity.distributionTrendList.addView(heading, headingParams);

        int limit = Math.min(4, shifts.size());
        for (int index = 0; index < limit; index += 1) {
            activity.distributionTrendList.addView(shiftRow(shifts.get(index), last.baseCurrency));
        }
    }

    private View shiftRow(TrendAnalytics.CategoryShift shift, String currency) {
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

        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(6);
        row.addView(activity.text(detailText(shift, currency), 13, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private String detailText(TrendAnalytics.CategoryShift shift, String currency) {
        if (activity.settings.hideAmounts) {
            return "占比 " + activity.formatPercentValue(shift.firstPercent)
                    + " -> " + activity.formatPercentValue(shift.lastPercent)
                    + "，金额已隐藏。";
        }
        return activity.formatMoney(shift.firstValue, currency)
                + " -> " + activity.formatMoney(shift.lastValue, currency)
                + "，变化 " + activity.formatSignedMoney(shift.delta, currency)
                + "；占比 " + activity.formatPercentValue(shift.firstPercent)
                + " -> " + activity.formatPercentValue(shift.lastPercent) + "。";
    }
}
