package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.AssetPresets;
import com.computerascience.moneymanager.domain.InvestmentAnalytics;
import com.computerascience.moneymanager.domain.UpdateAnalytics;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.CategoryBreakdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class InvestmentPageRenderer {
    private final MainActivity activity;

    InvestmentPageRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View summaryCard() {
        LinearLayout card = activity.card();
        LinearLayout header = activity.row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(activity.sectionTitle("投资总览"));
        activity.investmentSummaryText = activity.text("", 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(4);
        titleGroup.addView(activity.investmentSummaryText, summaryParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        Button addButton = activity.primaryButton("新增投资资产");
        addButton.setTextSize(13);
        addButton.setOnClickListener(view -> showCreateInvestmentAccount());
        header.addView(addButton, new LinearLayout.LayoutParams(activity.dp(124), activity.dp(42)));
        card.addView(header);

        TextView help = activity.text("投资页由资产类型开关控制。需要新增“美股、港股、期权”等类型时，可在设置里维护。", 13, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helpParams = activity.lp(-1, -2);
        helpParams.topMargin = activity.dp(12);
        card.addView(help, helpParams);

        Button categoryButton = activity.secondaryButton("管理投资类型");
        categoryButton.setOnClickListener(view -> activity.showCategorySettingsDialog());
        LinearLayout.LayoutParams categoryParams = activity.lp(-1, activity.dp(42));
        categoryParams.topMargin = activity.dp(10);
        card.addView(categoryButton, categoryParams);
        return card;
    }

    View structureCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资结构"));

        activity.investmentStructureSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentStructureSummary, summaryParams);

        activity.investmentStructureList = new LinearLayout(activity);
        activity.investmentStructureList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentStructureList, activity.lp(-1, -2));
        return card;
    }

    View reviewCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资复盘"));

        activity.investmentReviewSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentReviewSummary, summaryParams);

        activity.investmentReviewList = new LinearLayout(activity);
        activity.investmentReviewList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentReviewList, activity.lp(-1, -2));
        return card;
    }

    View diagnosticsCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资诊断"));

        activity.investmentDiagnosticSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentDiagnosticSummary, summaryParams);

        activity.investmentDiagnosticList = new LinearLayout(activity);
        activity.investmentDiagnosticList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentDiagnosticList, activity.lp(-1, -2));
        return card;
    }

    View flowCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资变化"));

        activity.investmentFlowSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentFlowSummary, summaryParams);

        activity.investmentFlowList = new LinearLayout(activity);
        activity.investmentFlowList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentFlowList, activity.lp(-1, -2));
        return card;
    }

    View institutionsCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资机构"));

        activity.investmentInstitutionSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentInstitutionSummary, summaryParams);

        activity.investmentInstitutionList = new LinearLayout(activity);
        activity.investmentInstitutionList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentInstitutionList, activity.lp(-1, -2));
        return card;
    }

    View planCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资待核对"));

        activity.investmentPlanSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.investmentPlanSummary, summaryParams);

        activity.investmentPlanList = new LinearLayout(activity);
        activity.investmentPlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.investmentPlanList, activity.lp(-1, -2));
        return card;
    }

    View accountsCard() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("投资资产"));
        activity.investmentAccountList = new LinearLayout(activity);
        activity.investmentAccountList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = activity.lp(-1, -2);
        listParams.topMargin = activity.dp(12);
        card.addView(activity.investmentAccountList, listParams);
        return card;
    }

    void render() {
        if (activity.investmentSummaryText == null || activity.investmentAccountList == null
                || activity.investmentStructureList == null || activity.investmentInstitutionList == null
                || activity.investmentPlanList == null || activity.investmentDiagnosticList == null
                || activity.investmentReviewList == null
                || activity.investmentFlowList == null) {
            return;
        }
        List<AssetRecord> investments = investmentAssets();
        List<AssetInstitutionGroups.Group> groups = AssetInstitutionGroups.groupByInstitution(investments, activity.settings);
        InvestmentAnalytics.Summary stats = InvestmentAnalytics.summarize(investments, activity.settings);

        activity.investmentSummaryText.setText("投资总额 " + activity.formatMoney(stats.total, activity.settings.baseCurrency)
                + " · " + groups.size() + " 个机构 · " + investments.size() + " 项资产");

        renderStructure(investments, stats);
        renderReview(investments, groups, stats);
        renderDiagnostics(investments, groups, stats);
        renderFlow(investments);
        renderInstitutions(groups, stats.total);
        renderPlan(investments, stats);

        activity.investmentAccountList.removeAllViews();
        if (investments.isEmpty()) {
            activity.investmentAccountList.addView(activity.emptyText("还没有投资资产。可在设置里把某个资产类型加入投资页。"));
            return;
        }
        for (AssetInstitutionGroups.Group group : groups) {
            activity.investmentAccountList.addView(activity.assetInstitutionGroupHeader(group, activity.settings.baseCurrency));
            if (!activity.collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    activity.investmentAccountList.addView(activity.assetCompactRow(asset));
                }
            }
        }
    }

    private void renderReview(
            List<AssetRecord> investments,
            List<AssetInstitutionGroups.Group> groups,
            InvestmentAnalytics.Summary stats
    ) {
        activity.investmentReviewList.removeAllViews();
        if (investments.isEmpty()) {
            activity.investmentReviewSummary.setText("还没有投资资产。新增后这里会给出投资账户复盘。");
            activity.investmentReviewList.addView(activity.emptyText("先新增或标记一个投资类型资产。"));
            return;
        }

        UpdateAnalytics.Summary month = UpdateAnalytics.summarizeKnownAssets(activity.updateEvents, investments, activity.settings, 30);
        UpdateAnalytics.Summary quarter = UpdateAnalytics.summarizeKnownAssets(activity.updateEvents, investments, activity.settings, 90);
        activity.investmentReviewSummary.setText(investmentReviewSummaryText(stats, month));

        activity.investmentReviewList.addView(infoRow(
                "30 天投资变化",
                month.count == 0 ? "无记录" : month.count + " 次",
                flowSummaryText(month),
                month.delta >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER
        ));
        activity.investmentReviewList.addView(infoRow(
                "90 天投资变化",
                quarter.count == 0 ? "无记录" : quarter.count + " 次",
                flowSummaryText(quarter),
                quarter.delta >= 0 ? MoneyManagerActivity.BLUE : MoneyManagerActivity.DANGER
        ));

        AssetRecord focus = nextFocusAsset(investments);
        activity.investmentReviewList.addView(infoRow(
                "下一步处理",
                focus == null ? "保持" : focus.name,
                focus == null ? "暂无需要优先处理的投资资产。"
                        : nextFocusDetail(focus),
                focus == null ? MoneyManagerActivity.ACCENT : focusColor(focus)
        ));

        AssetRecord largest = investments.get(0);
        activity.investmentReviewList.addView(infoRow(
                "最大投资项",
                activity.settings.hideAmounts ? "金额已隐藏" : activity.formatAmount(largest),
                largest.institution.isEmpty()
                        ? largest.category + " · 未填写机构。"
                        : largest.category + " · " + largest.institution + "。",
                MoneyManagerActivity.BLUE
        ));

        UpdateAnalytics.Bucket topReason = topBucket(month.reasons);
        UpdateAnalytics.Bucket topInstitution = topBucket(month.institutions);
        if (topReason != null || topInstitution != null) {
            activity.investmentReviewList.addView(infoRow(
                    "主要来源",
                    topReason == null ? "--" : topReason.label,
                    "近 30 天主要机构："
                            + (topInstitution == null ? "暂无" : topInstitution.label)
                            + "。",
                    MoneyManagerActivity.AMBER
            ));
        }
    }

    private String investmentReviewSummaryText(InvestmentAnalytics.Summary stats, UpdateAnalytics.Summary month) {
        if (activity.settings.hideAmounts) {
            return "近 30 天记录 " + month.count + " 次投资更新；金额已隐藏。";
        }
        return "投资总额 " + activity.formatMoney(stats.total, activity.settings.baseCurrency)
                + "；近 30 天投资净变化 "
                + activity.formatSignedMoney(month.delta, activity.settings.baseCurrency)
                + "。";
    }

    private String flowSummaryText(UpdateAnalytics.Summary summary) {
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

    private AssetRecord nextFocusAsset(List<AssetRecord> investments) {
        List<AssetRecord> planned = activity.sortedPlannedAssets(investments);
        if (planned.isEmpty()) {
            return null;
        }
        AssetRecord first = planned.get(0);
        return activity.daysUntilDue(first) <= 3 ? first : null;
    }

    private String nextFocusDetail(AssetRecord asset) {
        int days = activity.daysUntilDue(asset);
        String dueText;
        if (days < 0) {
            dueText = "已逾期 " + Math.abs(days) + " 天";
        } else if (days == 0) {
            dueText = "今天到期";
        } else {
            dueText = days + " 天后到期";
        }
        String appText = asset.packageName.isEmpty() && asset.launchUri.isEmpty()
                ? "还未绑定 App。"
                : "可直接打开绑定 App 核对。";
        return dueText + "，" + appText;
    }

    private int focusColor(AssetRecord asset) {
        return activity.daysUntilDue(asset) <= 0 ? MoneyManagerActivity.DANGER : MoneyManagerActivity.AMBER;
    }

    private UpdateAnalytics.Bucket topBucket(List<UpdateAnalytics.Bucket> buckets) {
        return buckets.isEmpty() ? null : buckets.get(0);
    }

    private void renderDiagnostics(
            List<AssetRecord> investments,
            List<AssetInstitutionGroups.Group> groups,
            InvestmentAnalytics.Summary stats
    ) {
        activity.investmentDiagnosticList.removeAllViews();
        if (investments.isEmpty()) {
            activity.investmentDiagnosticSummary.setText("投资资产还不够，暂时无法给出诊断。");
            activity.investmentDiagnosticList.addView(activity.emptyText("先新增或标记几个投资类型资产。"));
            return;
        }

        int alerts = 0;
        if (stats.cashRatio() >= 30) alerts += 1;
        if (!groups.isEmpty() && stats.total > 0 && groups.get(0).total / stats.total >= 0.5) alerts += 1;
        if (stats.dueNow > 0) alerts += 1;
        if (stats.unboundAppCount > 0 || stats.missingInstitutionCount > 0) alerts += 1;
        activity.investmentDiagnosticSummary.setText(alerts == 0
                ? "暂无突出的投资维护风险，重点继续保持核对节奏。"
                : "发现 " + alerts + " 个需要关注的投资维护点。");

        AssetInstitutionGroups.Group top = groups.isEmpty() ? null : groups.get(0);
        double topRatio = top == null || stats.total <= 0 ? 0 : top.total / stats.total * 100;
        activity.investmentDiagnosticList.addView(infoRow(
                "机构集中度",
                top == null ? "--" : activity.formatPercentValue(topRatio),
                top == null
                        ? "还没有投资机构。"
                        : top.title + " 占投资资产最多；" + concentrationAdvice(topRatio),
                topRatio >= 50 ? MoneyManagerActivity.AMBER : MoneyManagerActivity.ACCENT
        ));

        activity.investmentDiagnosticList.addView(infoRow(
                "闲置现金",
                activity.formatPercentValue(stats.cashRatio()),
                stats.cashRatio() >= 30
                        ? "现金比例偏高，适合确认是否刻意保留弹药。"
                        : "现金比例在可读范围内，可继续按账户更新。",
                stats.cashRatio() >= 30 ? MoneyManagerActivity.AMBER : MoneyManagerActivity.BLUE
        ));

        activity.investmentDiagnosticList.addView(infoRow(
                "核对压力",
                stats.dueNow + " 项到期",
                stats.dueSoon + " 项将在 3 天内到期。",
                stats.dueNow > 0 ? MoneyManagerActivity.DANGER : MoneyManagerActivity.ACCENT
        ));

        int dataIssues = stats.unboundAppCount + stats.missingInstitutionCount;
        activity.investmentDiagnosticList.addView(infoRow(
                "数据完整度",
                dataIssues == 0 ? "完整" : dataIssues + " 项待补",
                stats.unboundAppCount + " 项未绑定 App，"
                        + stats.missingInstitutionCount + " 项未填写机构。",
                dataIssues == 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.AMBER
        ));
    }

    private String concentrationAdvice(double ratio) {
        if (ratio >= 50) {
            return "集中度较高，建议核对是否符合你的风险偏好。";
        }
        if (ratio >= 35) {
            return "集中度中等，适合持续观察。";
        }
        return "集中度相对分散。";
    }

    private void renderFlow(List<AssetRecord> investments) {
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
        FlowSectionRenderer.add(activity, activity.investmentFlowList, "按类型", summary.categories);
        FlowSectionRenderer.add(activity, activity.investmentFlowList, "按机构", summary.institutions);
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

    private void renderStructure(List<AssetRecord> investments, InvestmentAnalytics.Summary stats) {
        activity.investmentStructureList.removeAllViews();
        if (investments.isEmpty()) {
            activity.investmentStructureSummary.setText("投资类型开启后，这里会展示持仓、现金和类型分布。");
            activity.investmentStructureList.addView(activity.emptyText("还没有投资资产。"));
            return;
        }

        double cashPercent = stats.total <= 0 ? 0 : stats.cash / stats.total * 100;
        activity.investmentStructureSummary.setText("持仓 " + activity.formatPercent(stats.holding, stats.total)
                + " · 闲置现金 " + activity.formatPercentValue(cashPercent)
                + " · " + investments.size() + " 项投资资产。");

        activity.investmentStructureList.addView(infoRow(
                "持仓市值",
                activity.formatMoney(stats.holding, activity.settings.baseCurrency),
                "投资账户持仓，以及基金、加密资产等按投资类型纳入的资产。",
                MoneyManagerActivity.ACCENT
        ));
        activity.investmentStructureList.addView(infoRow(
                "闲置现金",
                activity.formatMoney(stats.cash, activity.settings.baseCurrency),
                cashPercent >= 30
                        ? "现金占比较高，适合确认是否刻意留仓。"
                        : "现金占比用于观察券商账户里的未投资资金。",
                cashPercent >= 30 ? MoneyManagerActivity.AMBER : MoneyManagerActivity.BLUE
        ));

        int limit = Math.min(4, stats.categories.size());
        for (int index = 0; index < limit; index += 1) {
            CategoryBreakdown category = stats.categories.get(index);
            activity.investmentStructureList.addView(infoRow(
                    "类型 · " + category.category,
                    activity.formatPercent(category.value, stats.total),
                    activity.settings.hideAmounts
                            ? "金额已隐藏。"
                            : activity.formatMoney(category.value, activity.settings.baseCurrency),
                    category.color
            ));
        }
    }

    private void renderInstitutions(List<AssetInstitutionGroups.Group> groups, double total) {
        activity.investmentInstitutionList.removeAllViews();
        if (groups.isEmpty()) {
            activity.investmentInstitutionSummary.setText("暂无投资机构。新增投资资产后会按机构汇总。");
            activity.investmentInstitutionList.addView(activity.emptyText("还没有可展示的投资机构。"));
            return;
        }

        AssetInstitutionGroups.Group top = groups.get(0);
        activity.investmentInstitutionSummary.setText("最大机构是 " + top.title
                + "，占投资资产 " + activity.formatPercent(top.total, total)
                + "；共 " + groups.size() + " 个投资机构。");

        int limit = Math.min(5, groups.size());
        for (int index = 0; index < limit; index += 1) {
            activity.investmentInstitutionList.addView(institutionRow(groups.get(index), total));
        }
    }

    private void renderPlan(List<AssetRecord> investments, InvestmentAnalytics.Summary stats) {
        activity.investmentPlanList.removeAllViews();
        if (investments.isEmpty()) {
            activity.investmentPlanSummary.setText("还没有投资资产。");
            activity.investmentPlanList.addView(activity.emptyText("新增投资资产后，这里会按更新时间排序。"));
            return;
        }

        activity.investmentPlanSummary.setText(stats.dueNow + " 项投资资产需要现在核对，"
                + stats.dueSoon + " 项将在 3 天内到期。");

        List<AssetRecord> planned = activity.sortedPlannedAssets(investments);
        int limit = Math.min(5, planned.size());
        List<AssetRecord> topPlanned = new ArrayList<>(planned.subList(0, limit));
        for (AssetInstitutionGroups.Group group : AssetInstitutionGroups.groupByInstitution(topPlanned, activity.settings)) {
            activity.investmentPlanList.addView(activity.assetInstitutionGroupHeader(group, activity.settings.baseCurrency));
            if (!activity.collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    activity.investmentPlanList.addView(activity.updatePlanRow(asset));
                }
            }
        }
    }

    private View institutionRow(AssetInstitutionGroups.Group group, double total) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        row.addView(activity.institutionGroupIcon(group), new LinearLayout.LayoutParams(activity.dp(34), activity.dp(34)));

        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = activity.dp(10);
        titleGroup.addView(activity.text(group.title, 14, MoneyManagerActivity.INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(3);
        titleGroup.addView(activity.text(group.assets.size() + " 项 · " + group.displayApps(), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), metaParams);
        row.addView(titleGroup, titleParams);

        String value = activity.settings.hideAmounts
                ? activity.formatPercent(group.total, total)
                : activity.formatMoney(group.total, activity.settings.baseCurrency) + "\n" + activity.formatPercent(group.total, total);
        TextView amount = activity.text(value, 12, MoneyManagerActivity.MUTED, Typeface.BOLD);
        amount.setGravity(Gravity.RIGHT);
        row.addView(amount);
        return row;
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

    private List<AssetRecord> investmentAssets() {
        List<AssetRecord> investments = new ArrayList<>();
        for (AssetRecord asset : activity.assets) {
            if (activity.settings.isInvestmentCategory(asset.category)) {
                investments.add(asset);
            }
        }
        Collections.sort(investments, (left, right) -> Double.compare(
                AssetMath.assetGrossAmount(right),
                AssetMath.assetGrossAmount(left)
        ));
        return investments;
    }

    void showCreateInvestmentAccount() {
        for (AssetPresets.Preset preset : AssetPresets.quickAddPresets()) {
            if (AssetCategories.INVESTMENT_ACCOUNT.equals(preset.category)) {
                activity.settings.setInvestmentCategory(preset.category, true);
                activity.store.saveSettings(activity.settings);
                activity.showCreatePreset(preset);
                return;
            }
        }
        activity.showEditDialog(null);
    }

}
