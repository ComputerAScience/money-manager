package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.UpdateAnalytics;

import java.util.List;

final class FlowSectionRenderer {
    private FlowSectionRenderer() {
    }

    static void add(MoneyManagerActivity activity, LinearLayout target, String title, List<UpdateAnalytics.Bucket> buckets) {
        TextView heading = activity.text(title, 13, MoneyManagerActivity.MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = activity.lp(-1, -2);
        headingParams.topMargin = activity.dp(target.getChildCount() == 0 ? 2 : 12);
        target.addView(heading, headingParams);

        int limit = Math.min(3, buckets.size());
        for (int index = 0; index < limit; index += 1) {
            target.addView(row(activity, buckets.get(index)));
        }
        if (limit == 0) {
            target.addView(activity.emptyText("暂无数据。"));
        }
    }

    private static View row(MoneyManagerActivity activity, UpdateAnalytics.Bucket bucket) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(9), activity.dp(12), activity.dp(9));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        header.addView(activity.text(bucket.label, 14, MoneyManagerActivity.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        int color = bucket.delta > 0 ? MoneyManagerActivity.ACCENT : (bucket.delta < 0 ? MoneyManagerActivity.DANGER : MoneyManagerActivity.MUTED);
        String value = activity.settings.hideAmounts
                ? bucket.count + " 次"
                : activity.formatSignedMoney(bucket.delta, activity.settings.baseCurrency);
        TextView delta = activity.text(value, 13, color, Typeface.BOLD);
        delta.setGravity(Gravity.END);
        header.addView(delta);
        row.addView(header);

        TextView detail = activity.text(bucket.count + " 次更新", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(4);
        row.addView(detail, detailParams);
        return row;
    }
}
