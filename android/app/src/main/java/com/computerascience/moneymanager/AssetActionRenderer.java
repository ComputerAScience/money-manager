package com.computerascience.moneymanager;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.DataHealth;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.SpaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AssetActionRenderer {
    private final MainActivity activity;

    AssetActionRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View actionCenterCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("行动中心"));

        activity.actionCenterSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.actionCenterSummary, summaryParams);

        activity.actionCenterList = new LinearLayout(activity);
        activity.actionCenterList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.actionCenterList, activity.lp(-1, -2));

        Button reviewButton = activity.secondaryButton("处理待办资产");
        reviewButton.setOnClickListener(view -> activity.showAssetManagement("issues"));
        LinearLayout.LayoutParams reviewParams = activity.lp(-1, activity.dp(42));
        reviewParams.topMargin = activity.dp(10);
        card.addView(reviewButton, reviewParams);
        return card;
    }

    View updatePlanCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("核对计划"));

        activity.updatePlanSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams updateSummaryParams = activity.lp(-1, -2);
        updateSummaryParams.topMargin = activity.dp(8);
        updateSummaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.updatePlanSummary, updateSummaryParams);

        activity.updatePlanList = new LinearLayout(activity);
        activity.updatePlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.updatePlanList, activity.lp(-1, -2));

        Button reviewButton = activity.secondaryButton("查看待处理资产");
        reviewButton.setOnClickListener(view -> activity.showAssetManagement("issues"));
        LinearLayout.LayoutParams buttonParams = activity.lp(-1, activity.dp(42));
        buttonParams.topMargin = activity.dp(10);
        card.addView(reviewButton, buttonParams);
        return card;
    }

    void render(PortfolioSummary portfolio) {
        renderActionCenter(portfolio);
        renderUpdatePlan();
    }

    private void renderActionCenter(PortfolioSummary portfolio) {
        activity.actionCenterList.removeAllViews();
        if (activity.assets.isEmpty()) {
            activity.actionCenterSummary.setText("还没有资产。新增资产后，这里会按机构汇总需要处理的事项。");
            activity.actionCenterList.addView(activity.emptyText("暂无待办。"));
            return;
        }

        List<AssetRecord> actionAssets = actionAssets();
        int urgentCount = 0;
        int soonCount = 0;
        int dataIssueCount = 0;
        for (AssetRecord asset : actionAssets) {
            int days = activity.daysUntilDue(asset);
            if (days <= 0) {
                urgentCount += 1;
            } else if (days <= 3) {
                soonCount += 1;
            }
            if (DataHealth.hasIssue(asset, activity.settings)) {
                dataIssueCount += 1;
            }
        }

        if (actionAssets.isEmpty()) {
            activity.actionCenterSummary.setText("当前没有到期或数据待完善事项。共 "
                    + portfolio.assetCount + " 项资产，继续按周期维护即可。");
            activity.actionCenterList.addView(activity.emptyText("暂无待办。"));
            return;
        }

        List<AssetInstitutionGroups.Group> groups = AssetInstitutionGroups.groupByInstitution(actionAssets, activity.settings);
        activity.actionCenterSummary.setText(urgentCount + " 项需要现在处理，"
                + soonCount + " 项将在 3 天内到期，"
                + dataIssueCount + " 项存在数据维护问题；按 "
                + groups.size() + " 个机构分组。");

        int limit = Math.min(5, groups.size());
        for (int index = 0; index < limit; index += 1) {
            activity.actionCenterList.addView(actionInstitutionRow(groups.get(index)));
        }
        if (groups.size() > limit) {
            TextView more = activity.text("还有 " + (groups.size() - limit)
                    + " 个机构可在资产管理里继续处理。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = activity.lp(-1, -2);
            moreParams.topMargin = activity.dp(8);
            activity.actionCenterList.addView(more, moreParams);
        }
    }

    private List<AssetRecord> actionAssets() {
        List<AssetRecord> actionAssets = new ArrayList<>();
        for (AssetRecord asset : activity.assets) {
            if (DataHealth.hasIssue(asset, activity.settings) || activity.daysUntilDue(asset) <= 3) {
                actionAssets.add(asset);
            }
        }
        Collections.sort(actionAssets, (left, right) -> {
            int dueCompare = Integer.compare(activity.daysUntilDue(left), activity.daysUntilDue(right));
            if (dueCompare != 0) {
                return dueCompare;
            }
            int issueCompare = Boolean.compare(
                    DataHealth.hasIssue(right, activity.settings),
                    DataHealth.hasIssue(left, activity.settings)
            );
            if (issueCompare != 0) {
                return issueCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return actionAssets;
    }

    private View actionInstitutionRow(AssetInstitutionGroups.Group group) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(12));
        card.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams cardParams = activity.lp(-1, -2);
        cardParams.topMargin = activity.dp(8);
        card.setLayoutParams(cardParams);

        LinearLayout header = activity.row();
        header.addView(activity.institutionGroupIcon(group), new LinearLayout.LayoutParams(activity.dp(36), activity.dp(36)));
        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = activity.dp(10);
        titleGroup.addView(activity.text(group.title, 14, MoneyManagerActivity.INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(3);
        titleGroup.addView(activity.text(group.assets.size() + " 项待处理 · " + group.displayApps(),
                12, MoneyManagerActivity.MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, titleParams);
        card.addView(header);

        int limit = Math.min(3, group.assets.size());
        for (int index = 0; index < limit; index += 1) {
            card.addView(actionAssetLine(group.assets.get(index)));
        }
        if (group.assets.size() > limit) {
            TextView more = activity.text("还有 " + (group.assets.size() - limit)
                    + " 项可在资产管理继续处理。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = activity.lp(-1, -2);
            moreParams.topMargin = activity.dp(6);
            card.addView(more, moreParams);
        }
        return card;
    }

    private View actionAssetLine(AssetRecord asset) {
        LinearLayout row = activity.row();
        row.setPadding(activity.dp(10), activity.dp(8), activity.dp(10), activity.dp(8));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.PANEL, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(activity.text(asset.name, 13, MoneyManagerActivity.INK, Typeface.BOLD));
        TextView detail = activity.text(activity.joinInline(actionReasons(asset)), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(3);
        copy.addView(detail, detailParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));

        row.addView(updateDueChip(asset));
        return row;
    }

    private List<String> actionReasons(AssetRecord asset) {
        List<String> reasons = new ArrayList<>(DataHealth.assetIssues(asset, activity.settings));
        int days = activity.daysUntilDue(asset);
        if (days > 0 && days <= 3 && !reasons.contains("待更新")) {
            reasons.add(days + " 天后到期");
        }
        if (reasons.isEmpty()) {
            reasons.add("按计划核对");
        }
        return reasons;
    }

    private void renderUpdatePlan() {
        activity.updatePlanList.removeAllViews();
        if (activity.assets.isEmpty()) {
            activity.updatePlanSummary.setText("还没有资产。新增资产后，这里会按更新周期自动排计划。");
            return;
        }

        List<AssetRecord> planned = sortedPlannedAssets(activity.assets);
        int urgentCount = 0;
        int soonCount = 0;
        for (AssetRecord asset : activity.assets) {
            int days = activity.daysUntilDue(asset);
            if (days <= 0) {
                urgentCount += 1;
            } else if (days <= 3) {
                soonCount += 1;
            }
        }

        activity.updatePlanSummary.setText(updatePlanSummaryText(urgentCount, soonCount));

        int limit = Math.min(8, planned.size());
        List<AssetRecord> topPlanned = new ArrayList<>(planned.subList(0, limit));
        for (AssetInstitutionGroups.Group group : AssetInstitutionGroups.groupByInstitution(topPlanned, activity.settings)) {
            activity.updatePlanList.addView(activity.assetInstitutionGroupHeader(group, activity.settings.baseCurrency));
            if (!activity.collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    activity.updatePlanList.addView(updatePlanRow(asset));
                }
            }
        }
        if (planned.size() > limit) {
            TextView more = activity.text("还有 " + (planned.size() - limit)
                    + " 项资产会按机构继续排队。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = activity.lp(-1, -2);
            moreParams.topMargin = activity.dp(8);
            activity.updatePlanList.addView(more, moreParams);
        }
    }

    List<AssetRecord> sortedPlannedAssets(List<AssetRecord> source) {
        List<AssetRecord> planned = new ArrayList<>(source);
        Collections.sort(planned, (left, right) -> {
            int daysCompare = Integer.compare(activity.daysUntilDue(left), activity.daysUntilDue(right));
            if (daysCompare != 0) {
                return daysCompare;
            }
            int amountCompare = Double.compare(activity.assetMagnitude(right), activity.assetMagnitude(left));
            if (amountCompare != 0) {
                return amountCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return planned;
    }

    String updatePlanSummaryText(int urgentCount, int soonCount) {
        return urgentCount + " 项需要现在核对，"
                + soonCount + " 项将在 3 天内到期。";
    }

    View updatePlanRow(AssetRecord asset) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(12));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.bottomMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        TextView typeMark = activity.categoryMark(asset);
        LinearLayout.LayoutParams typeMarkParams = new LinearLayout.LayoutParams(activity.dp(34), activity.dp(34));
        typeMarkParams.rightMargin = activity.dp(10);
        header.addView(typeMark, typeMarkParams);

        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(activity.text(asset.name, 14, MoneyManagerActivity.INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(4);
        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        titleGroup.addView(activity.text(asset.category + " · " + institution,
                12, MoneyManagerActivity.MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        header.addView(updateDueChip(asset));
        row.addView(header);

        LinearLayout actions = activity.row();
        LinearLayout.LayoutParams actionsParams = activity.lp(-1, -2);
        actionsParams.topMargin = activity.dp(10);
        actions.setLayoutParams(actionsParams);

        Button launch = activity.secondaryButton("打开 App");
        launch.setOnClickListener(view -> activity.openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, activity.dp(40), 1));
        actions.addView(new SpaceView(activity, activity.dp(8), 1));

        Button mark = activity.secondaryButton("已更新");
        mark.setOnClickListener(view -> activity.showAssetUpdateDialog(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, activity.dp(40), 1));
        row.addView(actions);
        return row;
    }

    private TextView updateDueChip(AssetRecord asset) {
        int days = activity.daysUntilDue(asset);
        String label;
        int color;
        if (asset.lastUpdatedAt <= 0) {
            label = "从未更新";
            color = MoneyManagerActivity.AMBER;
        } else if (days < 0) {
            label = "逾期 " + Math.abs(days) + " 天";
            color = MoneyManagerActivity.DANGER;
        } else if (days == 0) {
            label = "今天到期";
            color = MoneyManagerActivity.DANGER;
        } else if (days <= 3) {
            label = days + " 天后到期";
            color = MoneyManagerActivity.AMBER;
        } else {
            label = days + " 天后";
            color = MoneyManagerActivity.ACCENT;
        }

        TextView chip = activity.text(label, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(activity.dp(10), activity.dp(6), activity.dp(10), activity.dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(activity.dp(999));
        chip.setBackground(bg);
        return chip;
    }
}
