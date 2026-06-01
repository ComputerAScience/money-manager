package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;

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
                    + summary.count + " 次更新，净变化 "
                    + activity.formatSignedMoney(summary.delta, activity.settings.baseCurrency)
                    + "；流入 " + activity.formatMoney(summary.increase, activity.settings.baseCurrency)
                    + "，流出 " + activity.formatMoney(Math.abs(summary.decrease), activity.settings.baseCurrency) + "。");
        }

        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按原因", summary.reasons);
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按类型", summary.categories);
        FlowSectionRenderer.add(activity, activity.flowAttributionList, "按机构", summary.institutions);
    }
}
