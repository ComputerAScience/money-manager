package com.computerascience.moneymanager;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetFilters;
import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.AssetPresets;
import com.computerascience.moneymanager.domain.DataHealth;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.SpaceView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class AssetsPageRenderer {
    private final MainActivity activity;

    AssetsPageRenderer(MainActivity activity) {
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

    View assetManagementSection() {
        LinearLayout card = activity.card();
        activity.assetManagementCard = card;

        LinearLayout header = activity.row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(activity.sectionTitle("资产管理"));
        activity.managementSummary = activity.text("", 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(4);
        titleGroup.addView(activity.managementSummary, summaryParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        activity.managementToggle = activity.secondaryButton("折叠");
        activity.managementToggle.setOnClickListener(view -> {
            activity.managementExpanded = !activity.managementExpanded;
            activity.render();
        });
        header.addView(activity.managementToggle, new LinearLayout.LayoutParams(activity.dp(86), activity.dp(42)));
        card.addView(header);

        activity.managementBody = new LinearLayout(activity);
        activity.managementBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = activity.lp(-1, -2);
        bodyParams.topMargin = activity.dp(14);
        card.addView(activity.managementBody, bodyParams);

        activity.assetSearchInput = activity.input("搜索资产、机构、备注", "", InputType.TYPE_CLASS_TEXT);
        activity.assetSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                activity.assetSearchQuery = text == null ? "" : text.toString().trim();
                activity.render();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        activity.managementBody.addView(activity.assetSearchInput);

        activity.assetFilterButtons = new LinearLayout(activity);
        activity.assetFilterButtons.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams filterParams = activity.lp(-1, -2);
        filterParams.bottomMargin = activity.dp(10);
        activity.managementBody.addView(activity.assetFilterButtons, filterParams);

        activity.assetResultSummary = activity.text("", 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams resultParams = activity.lp(-1, -2);
        resultParams.bottomMargin = activity.dp(10);
        activity.managementBody.addView(activity.assetResultSummary, resultParams);

        LinearLayout.LayoutParams presetParams = activity.lp(-1, -2);
        presetParams.bottomMargin = activity.dp(14);
        activity.managementBody.addView(assetPresetSection(), presetParams);

        Button addButton = activity.primaryButton("新增资产");
        addButton.setOnClickListener(view -> activity.showEditDialog(null));
        LinearLayout.LayoutParams actionParams = activity.lp(-1, activity.dp(48));
        actionParams.bottomMargin = activity.dp(14);
        activity.managementBody.addView(addButton, actionParams);

        activity.assetList = new LinearLayout(activity);
        activity.assetList.setOrientation(LinearLayout.VERTICAL);
        activity.managementBody.addView(activity.assetList, activity.lp(-1, -2));
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

        activity.managementSummary.setText("共 " + portfolio.assetCount + " 项资产，"
                + portfolio.staleCount + " 项需要更新，"
                + portfolio.missingBindingCount + " 项还没绑定 App。");
        activity.managementToggle.setText(activity.managementExpanded ? "折叠" : "展开");
        activity.managementBody.setVisibility(activity.managementExpanded ? View.VISIBLE : View.GONE);

        renderAssetFilterButtons();
        List<AssetRecord> visibleAssets = AssetFilters.visibleAssets(
                activity.assets,
                activity.settings,
                activity.assetSearchQuery,
                activity.assetFilterMode,
                activity::appDisplayName
        );
        activity.assetResultSummary.setText("按机构分组显示 " + visibleAssets.size() + " / " + activity.assets.size()
                + " 项，当前筛选：" + AssetFilters.label(activity.assetFilterMode) + "。");

        activity.assetList.removeAllViews();
        if (visibleAssets.isEmpty()) {
            String message = activity.assets.isEmpty()
                    ? "还没有资产。先新增一项，再绑定对应 App。"
                    : "没有匹配的资产。换个关键词或筛选条件试试。";
            TextView empty = activity.text(message, 16, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(activity.dp(18), activity.dp(28), activity.dp(18), activity.dp(28));
            activity.assetList.addView(empty, activity.lp(-1, -2));
        } else {
            renderAssetGroups(visibleAssets, portfolio);
        }
    }

    View assetInstitutionGroupHeader(AssetInstitutionGroups.Group group, String baseCurrency) {
        LinearLayout row = activity.row();
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(6);
        rowParams.bottomMargin = activity.dp(10);
        row.setLayoutParams(rowParams);

        row.addView(institutionGroupIcon(group), new LinearLayout.LayoutParams(activity.dp(40), activity.dp(40)));

        LinearLayout labelGroup = new LinearLayout(activity);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = activity.dp(10);
        labelGroup.addView(activity.text(group.title + " · " + group.assets.size() + " 项资产", 14, MoneyManagerActivity.INK, Typeface.BOLD));

        String detail = "小计 " + activity.formatMoney(group.total, baseCurrency)
                + " · " + group.displayKinds()
                + " · " + group.staleCount + " 项待更新"
                + " · " + group.displayApps();
        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(4);
        labelGroup.addView(activity.text(detail, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);
        row.addView(labelGroup, labelParams);

        Button toggle = activity.secondaryButton(activity.collapsedAssetGroups.contains(group.key) ? "展开" : "折叠");
        toggle.setOnClickListener(view -> {
            if (activity.collapsedAssetGroups.contains(group.key)) {
                activity.collapsedAssetGroups.remove(group.key);
            } else {
                activity.collapsedAssetGroups.add(group.key);
            }
            activity.render();
        });
        row.addView(toggle, new LinearLayout.LayoutParams(activity.dp(72), activity.dp(38)));
        return row;
    }

    View institutionGroupIcon(AssetInstitutionGroups.Group group) {
        Drawable icon = activity.resolveAppIcon(group.primaryPackageName());
        if (icon != null) {
            ImageView image = new ImageView(activity);
            image.setImageDrawable(icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(activity.dp(5), activity.dp(5), activity.dp(5), activity.dp(5));
            image.setBackground(activity.roundedBackground(MoneyManagerActivity.PANEL, MoneyManagerActivity.PANEL_BORDER, 8));
            return image;
        }

        TextView fallback = activity.text(group.hasBoundApp() ? "机构" : "未", 12,
                group.hasBoundApp() ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.AMBER,
                Typeface.BOLD);
        fallback.setGravity(Gravity.CENTER);
        fallback.setBackground(activity.roundedBackground(MoneyManagerActivity.SURFACE_ALT, Color.TRANSPARENT, 8));
        return fallback;
    }

    List<AssetRecord> sortedPlannedAssets(List<AssetRecord> source) {
        List<AssetRecord> planned = new ArrayList<>(source);
        Collections.sort(planned, (left, right) -> {
            int daysCompare = Integer.compare(activity.daysUntilDue(left), activity.daysUntilDue(right));
            if (daysCompare != 0) {
                return daysCompare;
            }
            int amountCompare = Double.compare(
                    activity.assetMagnitude(right),
                    activity.assetMagnitude(left)
            );
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
        titleGroup.addView(activity.text(asset.category + " · " + institution, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView due = updateDueChip(asset);
        header.addView(due);
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

    View assetCompactRow(AssetRecord asset) {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(10));
        item.setBackground(activity.cardBackground(MoneyManagerActivity.PANEL, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams itemParams = activity.lp(-1, -2);
        itemParams.bottomMargin = activity.dp(8);
        item.setLayoutParams(itemParams);

        LinearLayout top = activity.row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(top);

        TextView mark = activity.categoryMark(asset);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(activity.dp(34), activity.dp(34));
        markParams.rightMargin = activity.dp(10);
        top.addView(mark, markParams);

        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = activity.text(asset.name, 15, MoneyManagerActivity.INK, Typeface.BOLD);
        name.setSingleLine(true);
        titleGroup.addView(name);

        TextView meta = activity.text(asset.category + " · 每 " + asset.updateEveryDays + " 天", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(3);
        titleGroup.addView(meta, metaParams);

        TextView status = activity.statusChip(asset);
        top.addView(status);

        LinearLayout valueRow = activity.row();
        LinearLayout.LayoutParams valueParams = activity.lp(-1, -2);
        valueParams.topMargin = activity.dp(8);
        item.addView(valueRow, valueParams);

        TextView amount = activity.text(activity.formatAmount(asset), 18, MoneyManagerActivity.INK, Typeface.BOLD);
        amount.setSingleLine(true);
        valueRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView updated = activity.text(activity.shortUpdatedText(asset), 12, MoneyManagerActivity.MUTED, Typeface.BOLD);
        updated.setGravity(Gravity.RIGHT);
        valueRow.addView(updated);

        List<String> details = activity.assetBreakdownLines(asset);
        if (!details.isEmpty()) {
            TextView breakdown = activity.text(activity.joinLines(details), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams breakdownParams = activity.lp(-1, -2);
            breakdownParams.topMargin = activity.dp(6);
            item.addView(breakdown, breakdownParams);
        }

        if (!asset.note.isEmpty()) {
            TextView note = activity.text(asset.note, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            note.setMaxLines(2);
            LinearLayout.LayoutParams noteParams = activity.lp(-1, -2);
            noteParams.topMargin = activity.dp(5);
            item.addView(note, noteParams);
        }

        LinearLayout actions = activity.row();
        LinearLayout.LayoutParams actionsParams = activity.lp(-1, activity.dp(36));
        actionsParams.topMargin = activity.dp(10);
        item.addView(actions, actionsParams);

        Button launch = activity.secondaryButton("打开");
        launch.setTextSize(12);
        launch.setOnClickListener(view -> activity.openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, activity.dp(36), 1));

        actions.addView(new SpaceView(activity, activity.dp(8), 1));

        Button markUpdated = activity.secondaryButton("更新");
        markUpdated.setTextSize(12);
        markUpdated.setOnClickListener(view -> activity.showAssetUpdateDialog(asset));
        actions.addView(markUpdated, new LinearLayout.LayoutParams(0, activity.dp(36), 1));

        actions.addView(new SpaceView(activity, activity.dp(8), 1));

        Button edit = activity.secondaryButton("编辑");
        edit.setTextSize(12);
        edit.setOnClickListener(view -> activity.showEditDialog(asset));
        actions.addView(edit, new LinearLayout.LayoutParams(0, activity.dp(36), 1));
        return item;
    }

    private View assetPresetSection() {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(12));
        panel.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));

        panel.addView(activity.text("快速新增资产", 13, MoneyManagerActivity.INK, Typeface.BOLD));
        TextView help = activity.text("先按机构建资产条目；App 只是核对时的一键打开入口，同一机构下可以有多个 App。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helpParams = activity.lp(-1, -2);
        helpParams.topMargin = activity.dp(4);
        panel.addView(help, helpParams);

        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowsParams = activity.lp(-1, -2);
        rowsParams.topMargin = activity.dp(10);
        panel.addView(rows, rowsParams);

        List<AssetPresets.Preset> presets = AssetPresets.quickAddPresets();
        for (int index = 0; index < presets.size(); index += 2) {
            LinearLayout row = activity.row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            addPresetButton(row, presets.get(index));
            if (index + 1 < presets.size()) {
                row.addView(new SpaceView(activity, activity.dp(8), 1));
                addPresetButton(row, presets.get(index + 1));
            } else {
                row.addView(new SpaceView(activity, activity.dp(8), 1));
                row.addView(new SpaceView(activity, 1, 1), new LinearLayout.LayoutParams(0, activity.dp(42), 1));
            }
            LinearLayout.LayoutParams rowParams = activity.lp(-1, activity.dp(42));
            if (index > 0) {
                rowParams.topMargin = activity.dp(8);
            }
            rows.addView(row, rowParams);
        }
        return panel;
    }

    private void addPresetButton(LinearLayout row, AssetPresets.Preset preset) {
        Button button = activity.secondaryButton(preset.label);
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setOnClickListener(view -> activity.showCreatePreset(preset));
        row.addView(button, new LinearLayout.LayoutParams(0, activity.dp(42), 1));
    }

    private void renderAssetFilterButtons() {
        activity.assetFilterButtons.removeAllViews();

        LinearLayout filterGroup = new LinearLayout(activity);
        filterGroup.setOrientation(LinearLayout.VERTICAL);
        filterGroup.setPadding(activity.dp(3), activity.dp(3), activity.dp(3), activity.dp(3));
        filterGroup.setBackground(activity.cardBackground(MoneyManagerActivity.PANEL, MoneyManagerActivity.PANEL_BORDER));

        LinearLayout firstRow = activity.row();
        firstRow.setGravity(Gravity.CENTER_VERTICAL);
        addSegmentFilterButton(firstRow, "全部", "all");
        addSegmentFilterButton(firstRow, "待更新", "stale");
        addSegmentFilterButton(firstRow, "未绑定", "unbound");
        filterGroup.addView(firstRow, activity.lp(-1, activity.dp(38)));

        LinearLayout secondRow = activity.row();
        secondRow.setGravity(Gravity.CENTER_VERTICAL);
        addSegmentFilterButton(secondRow, "待完善", "issues");
        addSegmentFilterButton(secondRow, "负债", "debt");
        LinearLayout.LayoutParams secondParams = activity.lp(-1, activity.dp(38));
        secondParams.topMargin = activity.dp(3);
        filterGroup.addView(secondRow, secondParams);

        activity.assetFilterButtons.addView(filterGroup, activity.lp(-1, activity.dp(85)));
    }

    private void addSegmentFilterButton(LinearLayout row, String label, String mode) {
        boolean active = activity.assetFilterMode.equals(mode);
        Button button = activity.secondaryButton(label);
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        button.setTextColor(active ? Color.WHITE : MoneyManagerActivity.MUTED);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(activity.buttonBackground(
                active ? MoneyManagerActivity.ACCENT : Color.TRANSPARENT,
                active ? MoneyManagerActivity.ACCENT_DARK : MoneyManagerActivity.ROW_SURFACE,
                active ? MoneyManagerActivity.ACCENT_DARK : Color.TRANSPARENT
        ));
        button.setOnClickListener(view -> {
            activity.assetFilterMode = mode;
            activity.render();
        });
        row.addView(button, new LinearLayout.LayoutParams(0, activity.dp(38), 1));
    }

    private void renderAssetGroups(List<AssetRecord> visibleAssets, PortfolioSummary portfolio) {
        for (AssetInstitutionGroups.Group group : AssetInstitutionGroups.groupByInstitution(visibleAssets, activity.settings)) {
            activity.assetList.addView(assetInstitutionGroupHeader(group, portfolio.baseCurrency));
            if (!activity.collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    activity.assetList.addView(assetCompactRow(asset));
                }
            }
        }
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
            TextView more = activity.text("还有 " + (groups.size() - limit) + " 个机构可在资产管理里继续处理。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
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
            int issueCompare = Boolean.compare(DataHealth.hasIssue(right, activity.settings), DataHealth.hasIssue(left, activity.settings));
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
        header.addView(institutionGroupIcon(group), new LinearLayout.LayoutParams(activity.dp(36), activity.dp(36)));
        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = activity.dp(10);
        titleGroup.addView(activity.text(group.title, 14, MoneyManagerActivity.INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(3);
        titleGroup.addView(activity.text(group.assets.size() + " 项待处理 · " + group.displayApps(), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, titleParams);
        card.addView(header);

        int limit = Math.min(3, group.assets.size());
        for (int index = 0; index < limit; index += 1) {
            card.addView(actionAssetLine(group.assets.get(index)));
        }
        if (group.assets.size() > limit) {
            TextView more = activity.text("还有 " + (group.assets.size() - limit) + " 项可在资产管理继续处理。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
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

        TextView due = updateDueChip(asset);
        row.addView(due);
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

        List<AssetRecord> planned = plannedAssets();
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
            activity.updatePlanList.addView(assetInstitutionGroupHeader(group, activity.settings.baseCurrency));
            if (!activity.collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    activity.updatePlanList.addView(updatePlanRow(asset));
                }
            }
        }
        if (planned.size() > limit) {
            TextView more = activity.text("还有 " + (planned.size() - limit) + " 项资产会按机构继续排队。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = activity.lp(-1, -2);
            moreParams.topMargin = activity.dp(8);
            activity.updatePlanList.addView(more, moreParams);
        }
    }

    private List<AssetRecord> plannedAssets() {
        return sortedPlannedAssets(activity.assets);
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
