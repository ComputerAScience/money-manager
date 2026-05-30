package com.computerascience.moneymanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_BACKUP = 4101;
    private static final int REQUEST_IMPORT_BACKUP = 4102;
    private static final String[] CATEGORIES = {"银行", "券商", "基金", "加密资产", "房产", "负债", "其他"};
    private static final String[] UPDATE_REASONS = {"余额核对", "入金", "出金", "市场涨跌", "转账", "利息分红", "手续费税费", "负债变化", "仅更新时间", "其他"};
    private static final int BG = Color.rgb(245, 246, 241);
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(30, 35, 32);
    private static final int MUTED = Color.rgb(102, 112, 104);
    private static final int LINE = Color.rgb(217, 221, 213);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int ACCENT_DARK = Color.rgb(15, 81, 72);
    private static final int SURFACE = Color.rgb(252, 253, 250);
    private static final int SURFACE_ALT = Color.rgb(238, 245, 241);
    private static final int BLUE = Color.rgb(55, 95, 150);
    private static final int DANGER = Color.rgb(183, 73, 85);
    private static final int AMBER = Color.rgb(154, 119, 32);
    private static final String PAGE_OVERVIEW = "overview";
    private static final String PAGE_DISTRIBUTION = "distribution";
    private static final String PAGE_TREND = "trend";
    private static final String PAGE_ASSETS = "assets";
    private static final String PAGE_UPDATES = "updates";
    private static final String PAGE_SETTINGS = "settings";

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private AssetStore store;
    private List<AssetRecord> assets = new ArrayList<>();
    private List<AssetSnapshot> snapshots = new ArrayList<>();
    private List<AssetUpdateEvent> updateEvents = new ArrayList<>();
    private PortfolioSettings settings;
    private ScrollView mainScrollView;
    private TextView pageTitle;
    private TextView pageSubtitle;
    private LinearLayout overviewPage;
    private LinearLayout distributionPage;
    private LinearLayout trendPage;
    private LinearLayout assetsPage;
    private LinearLayout updatesPage;
    private LinearLayout settingsPage;
    private Button overviewTab;
    private Button distributionTab;
    private Button trendTab;
    private Button assetsTab;
    private Button updatesTab;
    private Button settingsTab;
    private LinearLayout assetList;
    private LinearLayout allocationLegend;
    private LinearLayout allocationTargetList;
    private LinearLayout institutionList;
    private LinearLayout updatePlanList;
    private LinearLayout recentUpdateList;
    private LinearLayout managementBody;
    private AllocationChartView allocationChart;
    private TrendChartView trendChart;
    private TextView netWorthValue;
    private TextView grossAssetsValue;
    private TextView liabilitiesValue;
    private TextView freshnessValue;
    private Button privacyToggle;
    private TextView currencyNote;
    private TextView netWorthGoalSummary;
    private TextView currencySettingsSummary;
    private TextView allocationTargetSummary;
    private TextView trendSummary;
    private LinearLayout trendMetricsList;
    private LinearLayout trendHistoryList;
    private TextView insightSummary;
    private TextView dataHealthSummary;
    private LinearLayout dataHealthList;
    private TextView updatePlanSummary;
    private TextView recentUpdateSummary;
    private TextView managementSummary;
    private TextView assetResultSummary;
    private View assetManagementCard;
    private EditText assetSearchInput;
    private LinearLayout assetFilterButtons;
    private Button managementToggle;
    private String pendingLaunchAssetId;
    private boolean waitingForExternalReturn;
    private boolean managementExpanded = true;
    private String assetSearchQuery = "";
    private String assetFilterMode = "all";
    private String currentPage = PAGE_OVERVIEW;
    private final Set<String> collapsedAssetGroups = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AssetStore(this);
        assets = store.load();
        settings = store.loadSettings();
        snapshots = store.loadSnapshots();
        updateEvents = store.loadUpdateEvents();
        if (snapshots.isEmpty()) {
            snapshots = store.recordSnapshot(assets, settings);
        }
        buildUi();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pendingLaunchAssetId != null) {
            waitingForExternalReturn = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForExternalReturn && pendingLaunchAssetId != null) {
            String assetId = pendingLaunchAssetId;
            pendingLaunchAssetId = null;
            waitingForExternalReturn = false;
            AssetRecord asset = findAsset(assetId);
            if (asset != null) {
                showMarkUpdatedDialog(asset);
            }
        }
    }

    private void buildUi() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18), dp(18), dp(18), dp(10));
        header.setBackgroundColor(BG);

        LinearLayout brand = row();
        TextView mark = text("M", 13, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setIncludeFontPadding(false);
        mark.setBackground(roundedBackground(ACCENT, ACCENT_DARK, 8));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView eyebrow = label("Money Manager");
        LinearLayout.LayoutParams eyebrowParams = lp(-2, -2);
        eyebrowParams.leftMargin = dp(10);
        brand.addView(eyebrow, eyebrowParams);
        header.addView(brand);

        pageTitle = text("", 26, INK, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = lp(-1, -2);
        titleParams.topMargin = dp(4);
        header.addView(pageTitle, titleParams);

        pageSubtitle = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = lp(-1, -2);
        subtitleParams.topMargin = dp(4);
        subtitleParams.bottomMargin = dp(12);
        header.addView(pageSubtitle, subtitleParams);

        HorizontalScrollView tabsScroll = new HorizontalScrollView(this);
        tabsScroll.setHorizontalScrollBarEnabled(false);
        tabsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);

        LinearLayout tabs = row();
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        overviewTab = tabButton("总览", PAGE_OVERVIEW);
        distributionTab = tabButton("分布", PAGE_DISTRIBUTION);
        trendTab = tabButton("趋势", PAGE_TREND);
        assetsTab = tabButton("资产", PAGE_ASSETS);
        updatesTab = tabButton("更新", PAGE_UPDATES);
        settingsTab = tabButton("设置", PAGE_SETTINGS);
        addTab(tabs, overviewTab);
        addTab(tabs, distributionTab);
        addTab(tabs, trendTab);
        addTab(tabs, assetsTab);
        addTab(tabs, updatesTab);
        addTab(tabs, settingsTab);
        tabsScroll.addView(tabs, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        header.addView(tabsScroll, lp(-1, -2));
        screen.addView(header, lp(-1, -2));

        ScrollView scrollView = new ScrollView(this);
        mainScrollView = scrollView;
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(10), dp(18), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        overviewPage = page();
        overviewPage.addView(overviewCard());
        overviewPage.addView(netWorthGoalCard());
        overviewPage.addView(insightCard());
        overviewPage.addView(dataHealthCard());
        root.addView(overviewPage);

        distributionPage = page();
        distributionPage.addView(allocationCard());
        distributionPage.addView(allocationTargetCard());
        distributionPage.addView(institutionCard());
        root.addView(distributionPage);

        trendPage = page();
        trendPage.addView(trendCard());
        root.addView(trendPage);

        assetsPage = page();
        assetsPage.addView(assetManagementSection());
        root.addView(assetsPage);

        updatesPage = page();
        updatesPage.addView(updatePlanCard());
        updatesPage.addView(recentUpdatesCard());
        root.addView(updatesPage);

        settingsPage = page();
        settingsPage.addView(currencyCard());
        settingsPage.addView(backupCard());
        root.addView(settingsPage);

        screen.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));
        setContentView(screen);
        updatePageVisibility();
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutParams(lp(-1, -2));
        return page;
    }

    private Button tabButton(String label, String page) {
        Button button = secondaryButton(label);
        button.setTextSize(13);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setOnClickListener(view -> selectPage(page));
        return button;
    }

    private void addTab(LinearLayout tabs, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(74), dp(40));
        params.rightMargin = dp(8);
        tabs.addView(button, params);
    }

    private void selectPage(String page) {
        if (currentPage.equals(page)) {
            return;
        }
        currentPage = page;
        updatePageVisibility();
        if (mainScrollView != null) {
            mainScrollView.post(() -> mainScrollView.smoothScrollTo(0, 0));
        }
    }

    private void updatePageVisibility() {
        setPageVisible(overviewPage, PAGE_OVERVIEW.equals(currentPage));
        setPageVisible(distributionPage, PAGE_DISTRIBUTION.equals(currentPage));
        setPageVisible(trendPage, PAGE_TREND.equals(currentPage));
        setPageVisible(assetsPage, PAGE_ASSETS.equals(currentPage));
        setPageVisible(updatesPage, PAGE_UPDATES.equals(currentPage));
        setPageVisible(settingsPage, PAGE_SETTINGS.equals(currentPage));

        styleTab(overviewTab, PAGE_OVERVIEW.equals(currentPage));
        styleTab(distributionTab, PAGE_DISTRIBUTION.equals(currentPage));
        styleTab(trendTab, PAGE_TREND.equals(currentPage));
        styleTab(assetsTab, PAGE_ASSETS.equals(currentPage));
        styleTab(updatesTab, PAGE_UPDATES.equals(currentPage));
        styleTab(settingsTab, PAGE_SETTINGS.equals(currentPage));

        if (pageTitle != null) {
            pageTitle.setText(pageTitleText());
        }
        if (pageSubtitle != null) {
            pageSubtitle.setText(pageSubtitleText());
        }
    }

    private void setPageVisible(View page, boolean visible) {
        if (page != null) {
            page.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void styleTab(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.setTextColor(active ? Color.WHITE : INK);
        button.setBackground(buttonBackground(
                active ? ACCENT : Color.WHITE,
                active ? ACCENT_DARK : SURFACE_ALT,
                active ? ACCENT_DARK : LINE
        ));
    }

    private String pageTitleText() {
        if (PAGE_DISTRIBUTION.equals(currentPage)) {
            return "资产分布";
        }
        if (PAGE_TREND.equals(currentPage)) {
            return "一年趋势";
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return "资产管理";
        }
        if (PAGE_UPDATES.equals(currentPage)) {
            return "更新";
        }
        if (PAGE_SETTINGS.equals(currentPage)) {
            return "设置";
        }
        return "总览";
    }

    private String pageSubtitleText() {
        if (PAGE_DISTRIBUTION.equals(currentPage)) {
            return "查看类型比例、目标比例和资金所在机构。";
        }
        if (PAGE_TREND.equals(currentPage)) {
            return "记录快照，复盘近 30 天、90 天和一年的变化。";
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return "新增、筛选、绑定和核对每一项资产。";
        }
        if (PAGE_UPDATES.equals(currentPage)) {
            return "按更新周期处理待核对资产，回看最近变化。";
        }
        if (PAGE_SETTINGS.equals(currentPage)) {
            return "维护基准币种、手动汇率和本地备份。";
        }
        return "先看净资产、年度目标和需要处理的提醒。";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        if (requestCode == REQUEST_EXPORT_BACKUP) {
            writeBackup(data.getData());
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            readBackup(data.getData());
        }
    }

    private void render() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);

        netWorthValue.setText(formatMoney(portfolio.netWorth, portfolio.baseCurrency));
        grossAssetsValue.setText(formatMoney(portfolio.grossAssets, portfolio.baseCurrency));
        liabilitiesValue.setText(formatMoney(portfolio.liabilities, portfolio.baseCurrency));
        freshnessValue.setText(portfolio.staleCount + " 项待更新");
        privacyToggle.setText(settings.hideAmounts ? "显示金额" : "隐藏金额");
        currencyNote.setText(currencyNoteText(portfolio));
        netWorthGoalSummary.setText(netWorthGoalText(portfolio));
        currencySettingsSummary.setText(currencySettingsText());

        allocationChart.setCategories(portfolio.categories);
        renderAllocationLegend(portfolio);
        renderAllocationTargets(portfolio);
        renderInstitutionList(portfolio);

        List<AssetSnapshot> trendSnapshots = snapshotsForBase(portfolio.baseCurrency);
        trendChart.setSnapshots(trendSnapshots);
        trendSummary.setText(trendSummaryText(portfolio, trendSnapshots));
        renderTrendMetrics(portfolio, trendSnapshots);
        renderTrendHistory(trendSnapshots);

        insightSummary.setText(buildInsightText(portfolio));
        renderDataHealth(portfolio);
        renderUpdatePlan();
        renderRecentUpdates();

        managementSummary.setText("共 " + portfolio.assetCount + " 项资产，"
                + portfolio.staleCount + " 项需要更新，"
                + portfolio.missingBindingCount + " 项还没绑定 App。");
        managementToggle.setText(managementExpanded ? "折叠" : "展开");
        managementBody.setVisibility(managementExpanded ? View.VISIBLE : View.GONE);

        renderAssetFilterButtons();
        List<AssetRecord> visibleAssets = visibleAssets();
        assetResultSummary.setText("显示 " + visibleAssets.size() + " / " + assets.size()
                + " 项，当前筛选：" + assetFilterLabel() + "。");

        assetList.removeAllViews();
        if (visibleAssets.isEmpty()) {
            String message = assets.isEmpty()
                    ? "还没有资产。先新增一项，再绑定对应 App。"
                    : "没有匹配的资产。换个关键词或筛选条件试试。";
            TextView empty = text(message, 16, MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            assetList.addView(empty, lp(-1, -2));
        } else {
            renderAssetGroups(visibleAssets, portfolio);
        }

        updatePageVisibility();
    }

    private View overviewCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("总资产概览"));

        netWorthValue = text("--", 34, INK, Typeface.BOLD);
        LinearLayout.LayoutParams netParams = lp(-1, -2);
        netParams.topMargin = dp(10);
        card.addView(netWorthValue, netParams);

        currencyNote = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams noteParams = lp(-1, -2);
        noteParams.topMargin = dp(8);
        noteParams.bottomMargin = dp(10);
        card.addView(currencyNote, noteParams);

        LinearLayout overviewActions = row();
        privacyToggle = secondaryButton("隐藏金额");
        privacyToggle.setOnClickListener(view -> {
            settings.hideAmounts = !settings.hideAmounts;
            store.saveSettings(settings);
            render();
        });
        overviewActions.addView(privacyToggle, new LinearLayout.LayoutParams(0, dp(42), 1));
        overviewActions.addView(new SpaceView(this, dp(10), 1));

        Button copySummary = secondaryButton("复制摘要");
        copySummary.setOnClickListener(view -> copyAssetSummary());
        overviewActions.addView(copySummary, new LinearLayout.LayoutParams(0, dp(42), 1));
        LinearLayout.LayoutParams actionParams = lp(-1, -2);
        actionParams.bottomMargin = dp(14);
        card.addView(overviewActions, actionParams);

        LinearLayout row1 = row();
        grossAssetsValue = text("--", 18, INK, Typeface.BOLD);
        liabilitiesValue = text("--", 18, INK, Typeface.BOLD);
        row1.addView(metric("资产总额", grossAssetsValue), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(new SpaceView(this, dp(10), 1));
        row1.addView(metric("负债", liabilitiesValue), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row1);

        freshnessValue = text("--", 18, INK, Typeface.BOLD);
        LinearLayout.LayoutParams freshParams = lp(-1, -2);
        freshParams.topMargin = dp(10);
        card.addView(metric("更新状态", freshnessValue), freshParams);
        return card;
    }

    private void copyAssetSummary() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        List<AssetSnapshot> trendSnapshots = snapshotsForBase(portfolio.baseCurrency);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            toast("无法访问剪贴板。");
            return;
        }

        clipboard.setPrimaryClip(ClipData.newPlainText("Money Manager 资产摘要", buildAssetSummary(portfolio, trendSnapshots)));
        toast("资产摘要已复制。");
    }

    private String buildAssetSummary(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        List<String> lines = new ArrayList<>();
        lines.add("Money Manager 资产摘要");
        lines.add("生成时间：" + dateFormat.format(new Date()));
        lines.add("基准币种：" + portfolio.baseCurrency);
        lines.add("");
        lines.add("净资产：" + formatMoney(portfolio.netWorth, portfolio.baseCurrency));
        lines.add("资产总额：" + formatMoney(portfolio.grossAssets, portfolio.baseCurrency));
        lines.add("负债：" + formatMoney(portfolio.liabilities, portfolio.baseCurrency));
        lines.add("更新状态：" + portfolio.staleCount + " 项待更新 / 共 " + portfolio.assetCount + " 项");
        if (settings.hasNetWorthTarget()) {
            lines.add("年度目标：" + netWorthGoalText(portfolio));
        }
        lines.add("");
        lines.add("一年趋势：" + trendSummaryText(portfolio, trendSnapshots));
        List<String> trendReview = trendReviewLines(portfolio, trendSnapshots);
        if (!trendReview.isEmpty()) {
            lines.add("趋势复盘：");
            for (String line : trendReview) {
                lines.add("- " + line);
            }
        }

        if (!portfolio.categories.isEmpty()) {
            lines.add("");
            lines.add("资产比例 Top 3");
            double total = portfolio.grossAssets + portfolio.liabilities;
            int limit = Math.min(3, portfolio.categories.size());
            for (int index = 0; index < limit; index += 1) {
                CategoryBreakdown category = portfolio.categories.get(index);
                lines.add("- " + category.category + "："
                        + formatMoney(category.value, portfolio.baseCurrency)
                        + "，" + formatPercent(category.value, total));
            }
        }

        if (settings.hasAllocationTargets()) {
            List<AllocationDrift> drifts = allocationDrifts(portfolio);
            if (!drifts.isEmpty()) {
                lines.add("");
                lines.add("目标比例提醒");
                int limit = Math.min(3, drifts.size());
                for (int index = 0; index < limit; index += 1) {
                    AllocationDrift drift = drifts.get(index);
                    double gap = drift.targetPercent - drift.currentPercent;
                    String status = Math.abs(gap) < 0.5
                            ? "接近目标"
                            : (gap > 0 ? "低配" : "超配");
                    String line = "- " + drift.category + "：当前 "
                            + formatPercentValue(drift.currentPercent)
                            + "，目标 " + formatPercentValue(drift.targetPercent)
                            + "，" + status + " " + formatPercentValue(Math.abs(gap));
                    if (!settings.hideAmounts && Math.abs(gap) >= 0.5) {
                        line += "，建议" + (drift.amountDelta > 0 ? "增加 " : "减少 ")
                                + formatMoney(Math.abs(drift.amountDelta), portfolio.baseCurrency);
                    }
                    lines.add(line);
                }
            }
        }

        if (!portfolio.institutions.isEmpty()) {
            lines.add("");
            lines.add("机构分布 Top 3");
            double total = portfolio.grossAssets + portfolio.liabilities;
            int limit = Math.min(3, portfolio.institutions.size());
            for (int index = 0; index < limit; index += 1) {
                InstitutionBreakdown institution = portfolio.institutions.get(index);
                lines.add("- " + institution.institution + "："
                        + formatMoney(institution.value, portfolio.baseCurrency)
                        + "，" + formatPercent(institution.value, total)
                        + "，" + institution.assetCount + " 项");
            }
        }

        int urgentCount = 0;
        int soonCount = 0;
        for (AssetRecord asset : assets) {
            int days = daysUntilDue(asset);
            if (days <= 0) {
                urgentCount += 1;
            } else if (days <= 3) {
                soonCount += 1;
            }
        }
        lines.add("");
        lines.add("更新计划：" + updatePlanSummaryText(urgentCount, soonCount));
        lines.add("最近更新：" + (updateEvents.isEmpty()
                ? "还没有更新记录。"
                : recentUpdateSummaryText()));
        List<String> reasonLines = updateReasonSummaryLines(false);
        if (!reasonLines.isEmpty()) {
            lines.add("变化原因：");
            for (String line : reasonLines) {
                lines.add("- " + line);
            }
        }
        return joinLines(lines);
    }

    private View netWorthGoalCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("年度目标"));

        netWorthGoalSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(12);
        card.addView(netWorthGoalSummary, summaryParams);

        Button editButton = secondaryButton("编辑年度目标");
        editButton.setOnClickListener(view -> showNetWorthGoalDialog());
        card.addView(editButton, lp(-1, dp(44)));
        return card;
    }

    private View currencyCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("基准币种与汇率"));

        currencySettingsSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(12);
        card.addView(currencySettingsSummary, summaryParams);

        Button editButton = secondaryButton("编辑汇率");
        editButton.setOnClickListener(view -> showCurrencySettingsDialog());
        card.addView(editButton, lp(-1, dp(44)));
        return card;
    }

    private View allocationCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("资产比例"));

        LinearLayout body = row();
        LinearLayout.LayoutParams bodyParams = lp(-1, -2);
        bodyParams.topMargin = dp(12);
        body.setLayoutParams(bodyParams);

        allocationChart = new AllocationChartView(this);
        body.addView(allocationChart, new LinearLayout.LayoutParams(dp(148), dp(148)));

        allocationLegend = new LinearLayout(this);
        allocationLegend.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(0, -2, 1);
        legendParams.leftMargin = dp(14);
        body.addView(allocationLegend, legendParams);
        card.addView(body);
        return card;
    }

    private View allocationTargetCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("目标比例"));

        allocationTargetSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(allocationTargetSummary, summaryParams);

        allocationTargetList = new LinearLayout(this);
        allocationTargetList.setOrientation(LinearLayout.VERTICAL);
        card.addView(allocationTargetList, lp(-1, -2));

        Button editButton = secondaryButton("编辑目标比例");
        editButton.setOnClickListener(view -> showAllocationTargetDialog());
        LinearLayout.LayoutParams buttonParams = lp(-1, dp(44));
        buttonParams.topMargin = dp(10);
        card.addView(editButton, buttonParams);
        return card;
    }

    private View institutionCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("机构分布"));

        TextView description = text("按银行、券商或钱包汇总，方便核对资金主要放在哪里。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(8);
        descriptionParams.bottomMargin = dp(8);
        card.addView(description, descriptionParams);

        institutionList = new LinearLayout(this);
        institutionList.setOrientation(LinearLayout.VERTICAL);
        card.addView(institutionList, lp(-1, -2));
        return card;
    }

    private View trendCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("一年变化趋势"));

        trendSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        card.addView(trendSummary, summaryParams);

        trendChart = new TrendChartView(this);
        LinearLayout.LayoutParams chartParams = lp(-1, dp(190));
        chartParams.topMargin = dp(10);
        card.addView(trendChart, chartParams);

        trendMetricsList = new LinearLayout(this);
        trendMetricsList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams metricsParams = lp(-1, -2);
        metricsParams.topMargin = dp(10);
        metricsParams.bottomMargin = dp(4);
        card.addView(trendMetricsList, metricsParams);

        LinearLayout snapshotActions = row();
        Button snapshotButton = secondaryButton("记录今日快照");
        snapshotButton.setOnClickListener(view -> {
            snapshots = store.recordSnapshot(assets, settings);
            render();
            toast("已记录今日总资产快照。");
        });
        snapshotActions.addView(snapshotButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        snapshotActions.addView(new SpaceView(this, dp(10), 1));

        Button backfillButton = secondaryButton("补录快照");
        backfillButton.setOnClickListener(view -> showSnapshotBackfillDialog());
        snapshotActions.addView(backfillButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        LinearLayout.LayoutParams actionParams = lp(-1, -2);
        actionParams.topMargin = dp(8);
        actionParams.bottomMargin = dp(12);
        card.addView(snapshotActions, actionParams);

        trendHistoryList = new LinearLayout(this);
        trendHistoryList.setOrientation(LinearLayout.VERTICAL);
        card.addView(trendHistoryList, lp(-1, -2));
        return card;
    }

    private View insightCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("待办提醒"));
        insightSummary = text("", 15, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = dp(10);
        card.addView(insightSummary, params);
        return card;
    }

    private View dataHealthCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("数据健康"));

        dataHealthSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(dataHealthSummary, summaryParams);

        dataHealthList = new LinearLayout(this);
        dataHealthList.setOrientation(LinearLayout.VERTICAL);
        card.addView(dataHealthList, lp(-1, -2));

        Button reviewButton = secondaryButton("查看待处理资产");
        reviewButton.setOnClickListener(view -> showAssetManagement("issues"));
        LinearLayout.LayoutParams buttonParams = lp(-1, dp(42));
        buttonParams.topMargin = dp(10);
        card.addView(reviewButton, buttonParams);
        return card;
    }

    private View updatePlanCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("更新计划"));

        updatePlanSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(updatePlanSummary, summaryParams);

        updatePlanList = new LinearLayout(this);
        updatePlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(updatePlanList, lp(-1, -2));
        return card;
    }

    private View recentUpdatesCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("最近更新"));

        recentUpdateSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(recentUpdateSummary, summaryParams);

        recentUpdateList = new LinearLayout(this);
        recentUpdateList.setOrientation(LinearLayout.VERTICAL);
        card.addView(recentUpdateList, lp(-1, -2));
        return card;
    }

    private View backupCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("数据备份"));

        TextView description = text("导出会保存资产、App 绑定、更新时间、趋势快照、更新记录、汇率、年度目标和目标比例；导入会覆盖当前本机数据。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(8);
        descriptionParams.bottomMargin = dp(12);
        card.addView(description, descriptionParams);

        LinearLayout actions = row();
        Button exportButton = secondaryButton("导出备份");
        exportButton.setOnClickListener(view -> startBackupExport());
        actions.addView(exportButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(new SpaceView(this, dp(10), 1));

        Button importButton = secondaryButton("导入备份");
        importButton.setOnClickListener(view -> startBackupImport());
        actions.addView(importButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        card.addView(actions);
        return card;
    }

    private View assetManagementSection() {
        LinearLayout card = card();
        assetManagementCard = card;

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(sectionTitle("资产管理"));
        managementSummary = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(4);
        titleGroup.addView(managementSummary, summaryParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        managementToggle = secondaryButton("折叠");
        managementToggle.setOnClickListener(view -> {
            managementExpanded = !managementExpanded;
            render();
        });
        header.addView(managementToggle, new LinearLayout.LayoutParams(dp(86), dp(42)));
        card.addView(header);

        managementBody = new LinearLayout(this);
        managementBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = lp(-1, -2);
        bodyParams.topMargin = dp(14);
        card.addView(managementBody, bodyParams);

        assetSearchInput = input("搜索资产、机构、备注", "", InputType.TYPE_CLASS_TEXT);
        assetSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                assetSearchQuery = text == null ? "" : text.toString().trim();
                render();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        managementBody.addView(assetSearchInput);

        assetFilterButtons = new LinearLayout(this);
        assetFilterButtons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams filterParams = lp(-1, -2);
        filterParams.bottomMargin = dp(10);
        managementBody.addView(assetFilterButtons, filterParams);

        assetResultSummary = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams resultParams = lp(-1, -2);
        resultParams.bottomMargin = dp(10);
        managementBody.addView(assetResultSummary, resultParams);

        Button addButton = primaryButton("新增资产");
        addButton.setOnClickListener(view -> showEditDialog(null));
        LinearLayout.LayoutParams actionParams = lp(-1, dp(48));
        actionParams.bottomMargin = dp(14);
        managementBody.addView(addButton, actionParams);

        assetList = new LinearLayout(this);
        assetList.setOrientation(LinearLayout.VERTICAL);
        managementBody.addView(assetList, lp(-1, -2));
        return card;
    }

    private void showAssetManagement(String filterMode) {
        managementExpanded = true;
        assetFilterMode = filterMode;
        assetSearchQuery = "";
        currentPage = PAGE_ASSETS;
        render();
        scrollToAssetManagement();
    }

    private void scrollToAssetManagement() {
        if (mainScrollView == null || assetManagementCard == null) {
            return;
        }
        mainScrollView.post(() -> mainScrollView.smoothScrollTo(0, assetManagementCard.getTop()));
    }

    private void renderAllocationLegend(PortfolioSummary portfolio) {
        allocationLegend.removeAllViews();
        double total = portfolio.grossAssets + portfolio.liabilities;
        if (portfolio.categories.isEmpty() || total <= 0) {
            allocationLegend.addView(text("暂无可展示的资产比例。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        for (CategoryBreakdown category : portfolio.categories) {
            LinearLayout row = row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = lp(-1, -2);
            rowParams.bottomMargin = dp(8);
            row.setLayoutParams(rowParams);

            TextView dot = text("●", 16, category.color, Typeface.BOLD);
            row.addView(dot);

            TextView label = text("  " + category.category, 14, INK, Typeface.BOLD);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            double ratio = category.value / total * 100;
            TextView value = text(String.format(Locale.getDefault(), "%.1f%%", ratio), 14, MUTED, Typeface.BOLD);
            row.addView(value);
            allocationLegend.addView(row);
        }
    }

    private void renderAllocationTargets(PortfolioSummary portfolio) {
        allocationTargetList.removeAllViews();
        if (!settings.hasAllocationTargets()) {
            allocationTargetSummary.setText("还没有设置目标比例。配置后，这里会提示哪些类型低配或超配。");
            allocationTargetList.addView(text("适合给银行现金、券商、基金、负债等设置一个长期目标。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        List<AllocationDrift> drifts = allocationDrifts(portfolio);
        int offTrack = 0;
        for (AllocationDrift drift : drifts) {
            if (Math.abs(drift.currentPercent - drift.targetPercent) >= 5) {
                offTrack += 1;
            }
        }
        allocationTargetSummary.setText("已设置目标比例；"
                + offTrack + " 类资产偏离目标超过 5 个百分点。");

        if (drifts.isEmpty()) {
            allocationTargetList.addView(text("目标已保存。新增或更新资产后，这里会显示偏离情况。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        int limit = Math.min(5, drifts.size());
        for (int index = 0; index < limit; index += 1) {
            allocationTargetList.addView(allocationTargetRow(drifts.get(index)));
        }
    }

    private List<AllocationDrift> allocationDrifts(PortfolioSummary portfolio) {
        double total = portfolio.grossAssets + portfolio.liabilities;
        Map<String, Double> currentValues = new HashMap<>();
        Set<String> categories = new HashSet<>();
        for (CategoryBreakdown category : portfolio.categories) {
            currentValues.put(category.category, category.value);
            categories.add(category.category);
        }
        for (Map.Entry<String, Double> target : settings.allocationTargets.entrySet()) {
            if (target.getValue() > 0) {
                categories.add(target.getKey());
            }
        }

        List<AllocationDrift> drifts = new ArrayList<>();
        for (String category : categories) {
            double currentValue = currentValues.getOrDefault(category, 0.0);
            double currentPercent = total <= 0 ? 0 : currentValue / total * 100;
            double targetPercent = settings.targetForCategory(category);
            if (currentPercent <= 0 && targetPercent <= 0) {
                continue;
            }
            drifts.add(new AllocationDrift(
                    category,
                    currentPercent,
                    targetPercent,
                    total * targetPercent / 100 - currentValue,
                    AssetMath.colorForCategory(category)
            ));
        }

        Collections.sort(drifts, (left, right) -> {
            int driftCompare = Double.compare(
                    Math.abs(right.currentPercent - right.targetPercent),
                    Math.abs(left.currentPercent - left.targetPercent)
            );
            if (driftCompare != 0) {
                return driftCompare;
            }
            return left.category.compareToIgnoreCase(right.category);
        });
        return drifts;
    }

    private View allocationTargetRow(AllocationDrift drift) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        TextView dot = text("●", 15, drift.color, Typeface.BOLD);
        header.addView(dot);
        TextView title = text("  " + drift.category, 14, INK, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView status = allocationDriftChip(drift);
        header.addView(status);
        row.addView(header);

        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(6);
        row.addView(text("当前 " + formatPercentValue(drift.currentPercent)
                + " · 目标 " + formatPercentValue(drift.targetPercent)
                + " · 偏离 " + formatPoint(drift.currentPercent - drift.targetPercent),
                13, MUTED, Typeface.NORMAL), detailParams);

        if (!settings.hideAmounts) {
            LinearLayout.LayoutParams actionParams = lp(-1, -2);
            actionParams.topMargin = dp(4);
            row.addView(text(allocationRecommendationText(drift), 12, MUTED, Typeface.NORMAL), actionParams);
        }
        return row;
    }

    private TextView allocationDriftChip(AllocationDrift drift) {
        double gap = drift.targetPercent - drift.currentPercent;
        String label;
        int color;
        if (Math.abs(gap) < 0.5) {
            label = "接近目标";
            color = ACCENT;
        } else if (gap > 0) {
            label = "低配";
            color = AMBER;
        } else {
            label = "超配";
            color = DANGER;
        }
        TextView chip = text(label, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private String allocationRecommendationText(AllocationDrift drift) {
        double amount = Math.abs(drift.amountDelta);
        if (Math.abs(drift.targetPercent - drift.currentPercent) < 0.5) {
            return "已接近目标，无需特别调整。";
        }
        if (amount < 0.01) {
            return "录入资产金额后会估算需要调整的金额。";
        }
        String direction = drift.amountDelta > 0 ? "增加" : "减少";
        return "按当前总额估算，" + direction + "约 "
                + formatMoney(amount, settings.baseCurrency) + " 可接近目标。";
    }

    private void renderInstitutionList(PortfolioSummary portfolio) {
        institutionList.removeAllViews();
        double total = portfolio.grossAssets + portfolio.liabilities;
        if (portfolio.institutions.isEmpty() || total <= 0) {
            institutionList.addView(text("暂无可展示的机构分布。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        int limit = Math.min(5, portfolio.institutions.size());
        for (int index = 0; index < limit; index += 1) {
            InstitutionBreakdown institution = portfolio.institutions.get(index);
            institutionList.addView(institutionRow(institution, total));
        }
    }

    private View institutionRow(InstitutionBreakdown institution, double total) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout labelGroup = new LinearLayout(this);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        labelGroup.addView(text(institution.institution, 14, INK, Typeface.BOLD));

        LinearLayout.LayoutParams countParams = lp(-1, -2);
        countParams.topMargin = dp(4);
        labelGroup.addView(text(institution.assetCount + " 项资产", 12, MUTED, Typeface.NORMAL), countParams);
        row.addView(labelGroup, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout valueGroup = new LinearLayout(this);
        valueGroup.setOrientation(LinearLayout.VERTICAL);
        valueGroup.setGravity(Gravity.END);
        valueGroup.addView(text(formatMoney(institution.value, settings.baseCurrency), 14, INK, Typeface.BOLD));

        LinearLayout.LayoutParams ratioParams = lp(-1, -2);
        ratioParams.topMargin = dp(4);
        TextView ratio = text(formatPercent(institution.value, total), 12, MUTED, Typeface.NORMAL);
        ratio.setGravity(Gravity.END);
        valueGroup.addView(ratio, ratioParams);
        row.addView(valueGroup);
        return row;
    }

    private String currencyNoteText(PortfolioSummary portfolio) {
        if (portfolio.missingRateCount > 0) {
            return "总额以 " + portfolio.baseCurrency + " 显示；"
                    + portfolio.missingRateCount + " 项资产缺少汇率，暂按 1:1 估算。";
        }
        if (portfolio.hasMixedCurrencies) {
            return "总额以 " + portfolio.baseCurrency + " 显示，多币种资产已按本地汇率换算。";
        }
        return "总额以 " + portfolio.baseCurrency + " 显示。";
    }

    private String netWorthGoalText(PortfolioSummary portfolio) {
        if (!settings.hasNetWorthTarget()) {
            return "还没有设置年度目标。设置一个目标净资产和截止日期后，这里会显示进度和所需月均增量。";
        }

        String targetDate = dayKey(settings.netWorthTargetDate);
        if (settings.hideAmounts) {
            return "已设置 " + targetDate + " 前的年度目标；隐私模式已开启，金额和进度暂不显示。";
        }

        double gap = settings.netWorthTarget - portfolio.netWorth;
        double progress = settings.netWorthTarget <= 0 ? 0 : portfolio.netWorth / settings.netWorthTarget * 100;
        int daysLeft = daysUntilTimestamp(settings.netWorthTargetDate);
        if (gap <= 0) {
            return "目标 " + formatMoney(settings.netWorthTarget, portfolio.baseCurrency)
                    + "，截止 " + targetDate + "；当前进度 "
                    + formatPercentValue(progress) + "，已达到目标。";
        }

        if (daysLeft <= 0) {
            return "目标 " + formatMoney(settings.netWorthTarget, portfolio.baseCurrency)
                    + "，目标日 " + targetDate + " 已到；当前仍差 "
                    + formatMoney(gap, portfolio.baseCurrency) + "。";
        }

        double monthsLeft = Math.max(1.0, daysLeft / 30.4375);
        return "目标 " + formatMoney(settings.netWorthTarget, portfolio.baseCurrency)
                + "，截止 " + targetDate + "；当前进度 "
                + formatPercentValue(progress) + "，还差 "
                + formatMoney(gap, portfolio.baseCurrency)
                + "，剩余 " + daysLeft + " 天，约每月需要增加 "
                + formatMoney(gap / monthsLeft, portfolio.baseCurrency) + "。";
    }

    private String currencySettingsText() {
        List<String> rows = new ArrayList<>();
        rows.add("基准：" + settings.baseCurrency);
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            if (currency.equals(settings.baseCurrency)) {
                continue;
            }
            double rate = settings.rateFor(currency);
            if (rate > 0) {
                rows.add("1 " + currency + " = " + formatRate(rate) + " " + settings.baseCurrency);
            }
        }
        return joinLines(rows);
    }

    private void showNetWorthGoalDialog() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        PortfolioSettings draft = PortfolioSettings.copyOf(settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        TextView description = text("目标按当前基准币种 " + portfolio.baseCurrency + " 记录；切换基准币种后建议重新确认目标。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        String targetValue = draft.netWorthTarget > 0
                ? formatInputNumber(draft.netWorthTarget)
                : formatInputNumber(Math.max(0, portfolio.netWorth));
        EditText target = input("目标净资产（" + portfolio.baseCurrency + "）", targetValue, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(target);

        String dateValue = draft.netWorthTargetDate > 0 ? dayKey(draft.netWorthTargetDate) : defaultYearEnd();
        EditText targetDate = input("截止日期（yyyy-MM-dd）", dateValue, InputType.TYPE_CLASS_TEXT);
        form.addView(targetDate);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑年度目标")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("清空目标", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button clear = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            clear.setTextColor(DANGER);
            clear.setOnClickListener(button -> {
                settings.netWorthTarget = 0;
                settings.netWorthTargetDate = 0;
                store.saveSettings(settings);
                render();
                toast("已清空年度目标。");
                dialog.dismiss();
            });

            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                Double value = parseNumber(clean(target.getText().toString()));
                if (value == null || value <= 0) {
                    toast("目标净资产需要是大于 0 的数字。");
                    return;
                }

                Date parsedDate = parseDay(clean(targetDate.getText().toString()));
                if (parsedDate == null) {
                    toast("截止日期格式应为 yyyy-MM-dd。");
                    return;
                }
                if (parsedDate.getTime() < System.currentTimeMillis() - AssetMath.DAY_MS) {
                    toast("截止日期不能早于今天。");
                    return;
                }

                draft.netWorthTarget = value;
                draft.netWorthTargetDate = parsedDate.getTime();
                settings = draft;
                store.saveSettings(settings);
                render();
                toast("年度目标已保存。");
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showCurrencySettingsDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        EditText baseCurrency = input("基准币种", draft.baseCurrency, InputType.TYPE_CLASS_TEXT);
        form.addView(baseCurrency);

        List<CurrencyRateField> rateFields = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            EditText rateInput = input("1 " + currency + " 等于多少基准币种", formatRate(draft.rateFor(currency)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rateFields.add(new CurrencyRateField(currency, rateInput));
            form.addView(rateInput);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑汇率")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                String base = PortfolioSettings.cleanCurrency(baseCurrency.getText().toString());
                if (base.isEmpty()) {
                    toast("基准币种不能为空。");
                    return;
                }

                draft.baseCurrency = base;
                for (CurrencyRateField field : rateFields) {
                    double rate = parsePositiveDouble(field.input.getText().toString(), field.currency.equals(base) ? 1.0 : 0.0);
                    if (field.currency.equals(base)) {
                        rate = 1.0;
                    }
                    draft.setRate(field.currency, rate);
                }
                draft.ensureBaseRate();

                settings = draft;
                store.saveSettings(settings);
                snapshots = store.recordSnapshot(assets, settings);
                render();
                toast("汇率已更新。");
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showAllocationTargetDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        TextView description = text("填写各类型目标占比，合计需要等于 100%。留空表示 0%。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        List<AllocationTargetField> targetFields = new ArrayList<>();
        for (String category : CATEGORIES) {
            double current = draft.targetForCategory(category);
            EditText targetInput = input(
                    category + " 目标占比（%）",
                    current <= 0 ? "" : formatInputNumber(current),
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
            targetFields.add(new AllocationTargetField(category, targetInput));
            form.addView(targetInput);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑目标比例")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("清空目标", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button clear = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            clear.setTextColor(DANGER);
            clear.setOnClickListener(button -> {
                settings.clearAllocationTargets();
                store.saveSettings(settings);
                render();
                toast("已清空目标比例。");
                dialog.dismiss();
            });

            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                draft.clearAllocationTargets();
                double total = 0;
                for (AllocationTargetField field : targetFields) {
                    String raw = clean(field.input.getText().toString());
                    if (raw.isEmpty()) {
                        continue;
                    }
                    Double value = parseNumber(raw);
                    if (value == null || value < 0 || value > 100) {
                        toast(field.category + " 的目标占比需要在 0 到 100 之间。");
                        return;
                    }
                    if (value > 0) {
                        draft.setAllocationTarget(field.category, value);
                        total += value;
                    }
                }

                if (total > 0 && Math.abs(total - 100) > 0.5) {
                    toast("目标比例合计需要等于 100%。当前为 " + formatPercentValue(total) + "。");
                    return;
                }

                settings = draft;
                store.saveSettings(settings);
                render();
                toast(total <= 0 ? "已清空目标比例。" : "目标比例已保存。");
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void renderAssetFilterButtons() {
        assetFilterButtons.removeAllViews();
        assetFilterButtons.addView(assetFilterButton("全部", "all"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("待更新", "stale"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("未绑定", "unbound"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("待完善", "issues"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("负债", "debt"), new LinearLayout.LayoutParams(0, dp(40), 1));
    }

    private Button assetFilterButton(String label, String mode) {
        Button button = secondaryButton(label);
        if (assetFilterMode.equals(mode)) {
            button.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ACCENT);
            bg.setCornerRadius(dp(8));
            button.setBackground(bg);
        }
        button.setOnClickListener(view -> {
            assetFilterMode = mode;
            render();
        });
        return button;
    }

    private List<AssetRecord> visibleAssets() {
        List<AssetRecord> visible = new ArrayList<>();
        for (AssetRecord asset : assets) {
            if (matchesAssetQuery(asset) && matchesAssetFilter(asset)) {
                visible.add(asset);
            }
        }
        Collections.sort(visible, (left, right) -> {
            int leftPriority = assetPriority(left);
            int rightPriority = assetPriority(right);
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }
            int amountCompare = Double.compare(
                    Math.abs(AssetMath.parseAmount(right.amount)),
                    Math.abs(AssetMath.parseAmount(left.amount))
            );
            if (amountCompare != 0) {
                return amountCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return visible;
    }

    private boolean matchesAssetQuery(AssetRecord asset) {
        String query = assetSearchQuery.toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        return asset.name.toLowerCase(Locale.ROOT).contains(query)
                || asset.category.toLowerCase(Locale.ROOT).contains(query)
                || asset.institution.toLowerCase(Locale.ROOT).contains(query)
                || appDisplayName(asset).toLowerCase(Locale.ROOT).contains(query)
                || asset.currency.toLowerCase(Locale.ROOT).contains(query)
                || asset.note.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesAssetFilter(AssetRecord asset) {
        if ("stale".equals(assetFilterMode)) {
            return isStale(asset);
        }
        if ("unbound".equals(assetFilterMode)) {
            return asset.packageName.isEmpty() && asset.launchUri.isEmpty();
        }
        if ("debt".equals(assetFilterMode)) {
            return AssetMath.isLiability(asset);
        }
        if ("issues".equals(assetFilterMode)) {
            return hasDataIssue(asset);
        }
        return true;
    }

    private int assetPriority(AssetRecord asset) {
        if (isStale(asset)) {
            return 0;
        }
        if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
            return 1;
        }
        return 2;
    }

    private String assetFilterLabel() {
        if ("stale".equals(assetFilterMode)) {
            return "待更新";
        }
        if ("unbound".equals(assetFilterMode)) {
            return "未绑定";
        }
        if ("issues".equals(assetFilterMode)) {
            return "待完善";
        }
        if ("debt".equals(assetFilterMode)) {
            return "负债";
        }
        return "全部";
    }

    private boolean hasDataIssue(AssetRecord asset) {
        String amount = clean(asset.amount);
        String institution = clean(asset.institution);
        String currency = AssetMath.cleanCurrency(asset.currency);
        return amount.isEmpty()
                || parseNumber(amount) == null
                || institution.isEmpty()
                || institution.contains("待绑定")
                || (asset.packageName.isEmpty() && asset.launchUri.isEmpty())
                || !settings.hasRateFor(currency)
                || asset.lastUpdatedAt <= 0
                || isStale(asset);
    }

    private void renderAssetGroups(List<AssetRecord> visibleAssets, PortfolioSummary portfolio) {
        for (String category : visibleCategoryOrder(visibleAssets)) {
            List<AssetRecord> groupAssets = assetsForCategory(visibleAssets, category);
            if (groupAssets.isEmpty()) {
                continue;
            }

            assetList.addView(assetGroupHeader(category, groupAssets, portfolio.baseCurrency));
            if (!collapsedAssetGroups.contains(category)) {
                for (AssetRecord asset : groupAssets) {
                    assetList.addView(assetCard(asset));
                }
            }
        }
    }

    private List<String> visibleCategoryOrder(List<AssetRecord> visibleAssets) {
        Set<String> present = new HashSet<>();
        for (AssetRecord asset : visibleAssets) {
            present.add(asset.category);
        }

        List<String> ordered = new ArrayList<>();
        for (String category : CATEGORIES) {
            if (present.contains(category)) {
                ordered.add(category);
                present.remove(category);
            }
        }
        List<String> rest = new ArrayList<>(present);
        Collections.sort(rest);
        ordered.addAll(rest);
        return ordered;
    }

    private List<AssetRecord> assetsForCategory(List<AssetRecord> visibleAssets, String category) {
        List<AssetRecord> group = new ArrayList<>();
        for (AssetRecord asset : visibleAssets) {
            if (category.equals(asset.category)) {
                group.add(asset);
            }
        }
        return group;
    }

    private View assetGroupHeader(String category, List<AssetRecord> groupAssets, String baseCurrency) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout labelGroup = new LinearLayout(this);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        labelGroup.addView(text(category + " · " + groupAssets.size() + " 项", 14, INK, Typeface.BOLD));

        int stale = 0;
        double total = 0;
        for (AssetRecord asset : groupAssets) {
            if (isStale(asset)) {
                stale += 1;
            }
            String currency = AssetMath.cleanCurrency(asset.currency);
            double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
            total += Math.abs(AssetMath.parseAmount(asset.amount)) * rate;
        }

        String detail = "小计 " + formatMoney(total, baseCurrency) + " · " + stale + " 项待更新";
        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(4);
        labelGroup.addView(text(detail, 12, MUTED, Typeface.NORMAL), detailParams);
        row.addView(labelGroup, new LinearLayout.LayoutParams(0, -2, 1));

        Button toggle = secondaryButton(collapsedAssetGroups.contains(category) ? "展开" : "折叠");
        toggle.setOnClickListener(view -> {
            if (collapsedAssetGroups.contains(category)) {
                collapsedAssetGroups.remove(category);
            } else {
                collapsedAssetGroups.add(category);
            }
            render();
        });
        row.addView(toggle, new LinearLayout.LayoutParams(dp(72), dp(38)));
        return row;
    }

    private List<AssetSnapshot> snapshotsForBase(String baseCurrency) {
        List<AssetSnapshot> filtered = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (baseCurrency.equals(snapshot.baseCurrency)) {
                filtered.add(snapshot);
            }
        }
        return filtered;
    }

    private String trendSummaryText(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        if (trendSnapshots.size() < 2) {
            return "当前基准 " + portfolio.baseCurrency + " 已记录 " + trendSnapshots.size()
                    + " 个快照。每天或每次核对后记录一次，趋势会逐渐形成。";
        }
        if (settings.hideAmounts) {
            return "近一年记录 " + trendSnapshots.size() + " 个 " + portfolio.baseCurrency
                    + " 快照。隐私模式已开启，金额变化暂不显示。";
        }
        AssetSnapshot first = trendSnapshots.get(0);
        AssetSnapshot last = trendSnapshots.get(trendSnapshots.size() - 1);
        double change = last.netWorth - first.netWorth;
        double ratio = Math.abs(first.netWorth) < 0.0001 ? 0 : change / Math.abs(first.netWorth) * 100;
        return "近一年记录 " + trendSnapshots.size() + " 个 " + portfolio.baseCurrency + " 快照，净资产变化 "
                + formatSignedMoney(change, portfolio.baseCurrency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private void renderTrendMetrics(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        trendMetricsList.removeAllViews();
        trendMetricsList.addView(text("趋势复盘", 13, MUTED, Typeface.BOLD));

        List<TrendMetric> metrics = trendMetrics(portfolio, trendSnapshots);
        if (metrics.isEmpty()) {
            TextView empty = text("至少记录两次快照后，会显示近 30 天、90 天和一年的变化。", 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = lp(-1, -2);
            emptyParams.topMargin = dp(8);
            trendMetricsList.addView(empty, emptyParams);
            return;
        }

        for (TrendMetric metric : metrics) {
            trendMetricsList.addView(trendMetricRow(metric));
        }
    }

    private List<String> trendReviewLines(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        List<String> lines = new ArrayList<>();
        for (TrendMetric metric : trendMetrics(portfolio, trendSnapshots)) {
            if (metric.complete) {
                lines.add(metric.label + "：" + metricSummaryText(metric));
            }
        }
        return lines;
    }

    private List<TrendMetric> trendMetrics(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        List<TrendMetric> metrics = new ArrayList<>();
        metrics.add(trendMetric(portfolio, trendSnapshots, "近 30 天", 30));
        metrics.add(trendMetric(portfolio, trendSnapshots, "近 90 天", 90));
        metrics.add(trendMetric(portfolio, trendSnapshots, "近一年", 365));
        return metrics;
    }

    private TrendMetric trendMetric(
            PortfolioSummary portfolio,
            List<AssetSnapshot> trendSnapshots,
            String label,
            int days
    ) {
        long cutoff = System.currentTimeMillis() - days * AssetMath.DAY_MS;
        List<AssetSnapshot> window = new ArrayList<>();
        for (AssetSnapshot snapshot : trendSnapshots) {
            if (snapshot.timestamp >= cutoff) {
                window.add(snapshot);
            }
        }

        if (window.size() < 2) {
            return new TrendMetric(label, window.size(), portfolio.baseCurrency);
        }

        AssetSnapshot first = window.get(0);
        AssetSnapshot last = window.get(window.size() - 1);
        AssetSnapshot high = first;
        AssetSnapshot low = first;
        for (AssetSnapshot snapshot : window) {
            if (snapshot.netWorth > high.netWorth) {
                high = snapshot;
            }
            if (snapshot.netWorth < low.netWorth) {
                low = snapshot;
            }
        }

        return new TrendMetric(
                label,
                window.size(),
                portfolio.baseCurrency,
                true,
                first,
                last,
                high,
                low,
                last.netWorth - first.netWorth
        );
    }

    private View trendMetricRow(TrendMetric metric) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        header.addView(text(metric.label, 14, INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(text(metric.count + " 个快照", 12, MUTED, Typeface.BOLD));
        row.addView(header);

        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(6);
        row.addView(text(metricSummaryText(metric), 13, MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private String metricSummaryText(TrendMetric metric) {
        if (!metric.complete) {
            return "快照不足，继续记录后再计算阶段变化。";
        }
        if (settings.hideAmounts) {
            return "金额变化已隐藏，区间为 " + metric.first.dayKey + " 到 " + metric.last.dayKey + "。";
        }

        double ratio = Math.abs(metric.first.netWorth) < 0.0001
                ? 0
                : metric.change / Math.abs(metric.first.netWorth) * 100;
        return metric.first.dayKey + " 到 " + metric.last.dayKey
                + "，变化 " + formatSignedMoney(metric.change, metric.currency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）"
                + "；高点 " + metric.high.dayKey + " "
                + formatMoney(metric.high.netWorth, metric.currency)
                + "，低点 " + metric.low.dayKey + " "
                + formatMoney(metric.low.netWorth, metric.currency) + "。";
    }

    private void renderTrendHistory(List<AssetSnapshot> trendSnapshots) {
        trendHistoryList.removeAllViews();
        trendHistoryList.addView(text("最近快照", 13, MUTED, Typeface.BOLD));

        if (trendSnapshots.isEmpty()) {
            TextView empty = text("暂无快照。记录一次后会出现在这里。", 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = lp(-1, -2);
            emptyParams.topMargin = dp(8);
            trendHistoryList.addView(empty, emptyParams);
            return;
        }

        int start = Math.max(0, trendSnapshots.size() - 6);
        for (int index = trendSnapshots.size() - 1; index >= start; index -= 1) {
            AssetSnapshot snapshot = trendSnapshots.get(index);
            trendHistoryList.addView(snapshotRow(snapshot));
        }
    }

    private View snapshotRow(AssetSnapshot snapshot) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.addView(text(snapshot.dayKey + " · " + snapshot.baseCurrency, 14, INK, Typeface.BOLD));

        String details = "净资产 " + formatMoney(snapshot.netWorth, snapshot.baseCurrency)
                + " · 资产 " + formatMoney(snapshot.grossAssets, snapshot.baseCurrency)
                + " · 负债 " + formatMoney(snapshot.liabilities, snapshot.baseCurrency);
        LinearLayout.LayoutParams detailsParams = lp(-1, -2);
        detailsParams.topMargin = dp(4);
        textGroup.addView(text(details, 12, MUTED, Typeface.NORMAL), detailsParams);
        row.addView(textGroup, new LinearLayout.LayoutParams(0, -2, 1));

        Button delete = secondaryButton("删除");
        delete.setTextColor(DANGER);
        delete.setOnClickListener(view -> confirmDeleteSnapshot(snapshot));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(72), dp(38));
        deleteParams.leftMargin = dp(10);
        row.addView(delete, deleteParams);
        return row;
    }

    private void confirmDeleteSnapshot(AssetSnapshot snapshot) {
        new AlertDialog.Builder(this)
                .setTitle("删除快照？")
                .setMessage("确定删除 " + snapshot.dayKey + " 的 " + snapshot.baseCurrency + " 快照吗？趋势图会立刻更新。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    snapshots = store.deleteSnapshot(snapshot.dayKey, snapshot.baseCurrency);
                    render();
                    toast("已删除趋势快照。");
                })
                .show();
    }

    private void showSnapshotBackfillDialog() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        TextView description = text("按当前基准币种 " + portfolio.baseCurrency + " 补录近一年历史快照；同一天会覆盖原快照。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        EditText day = input("日期（yyyy-MM-dd）", dayKey(System.currentTimeMillis()), InputType.TYPE_CLASS_TEXT);
        form.addView(day);

        EditText netWorth = input("净资产", formatInputNumber(portfolio.netWorth), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        form.addView(netWorth);

        EditText grossAssets = input("资产总额", formatInputNumber(portfolio.grossAssets), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(grossAssets);

        EditText liabilities = input("负债", formatInputNumber(portfolio.liabilities), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(liabilities);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("补录历史快照")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存快照", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                Date parsedDay = parseDay(clean(day.getText().toString()));
                if (parsedDay == null) {
                    toast("日期格式应为 yyyy-MM-dd。");
                    return;
                }
                long timestamp = parsedDay.getTime();
                long now = System.currentTimeMillis();
                if (timestamp > now) {
                    toast("不能补录未来日期。");
                    return;
                }
                if (timestamp < now - 370L * AssetMath.DAY_MS) {
                    toast("只能补录近一年快照。");
                    return;
                }

                Double net = parseNumber(clean(netWorth.getText().toString()));
                Double gross = parseNumber(clean(grossAssets.getText().toString()));
                Double debt = parseNumber(clean(liabilities.getText().toString()));
                if (net == null || gross == null || debt == null) {
                    toast("金额必须是数字。");
                    return;
                }
                if (gross < 0 || debt < 0) {
                    toast("资产总额和负债不能为负数。");
                    return;
                }

                snapshots = store.upsertSnapshot(new AssetSnapshot(
                        dayKey(timestamp),
                        timestamp,
                        portfolio.baseCurrency,
                        net,
                        gross,
                        debt
                ));
                render();
                toast("已补录历史快照。");
                dialog.dismiss();
            });
        });

        dialog.show();
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
        if (settings.hasAllocationTargets()) {
            List<AllocationDrift> drifts = allocationDrifts(portfolio);
            if (!drifts.isEmpty()) {
                AllocationDrift largestDrift = drifts.get(0);
                double gap = largestDrift.targetPercent - largestDrift.currentPercent;
                if (Math.abs(gap) >= 5) {
                    lines.add("比例偏离最大：" + largestDrift.category + " "
                            + (gap > 0 ? "低配 " : "超配 ")
                            + formatPercentValue(Math.abs(gap)) + "。");
                }
            }
        }
        if (settings.hasNetWorthTarget()) {
            double gap = settings.netWorthTarget - portfolio.netWorth;
            int daysLeft = daysUntilTimestamp(settings.netWorthTargetDate);
            if (settings.hideAmounts && daysLeft <= 30) {
                lines.add("年度目标临近，金额暂不显示。");
            } else if (gap <= 0) {
                lines.add("年度净资产目标已达到。");
            } else if (daysLeft <= 30) {
                lines.add("年度目标还差 " + formatMoney(gap, portfolio.baseCurrency)
                        + "，剩余 " + Math.max(0, daysLeft) + " 天。");
            }
        }
        if (portfolio.staleCount > 0) {
            lines.add(portfolio.staleCount + " 项资产已到更新周期。");
        } else {
            lines.add("所有资产都在更新周期内。");
        }
        if (portfolio.missingBindingCount > 0) {
            lines.add(portfolio.missingBindingCount + " 项资产还没绑定 App，可在资产管理里选择已安装 App。");
        }
        if (portfolio.grossAssets > 0 && portfolio.liabilities / portfolio.grossAssets > 0.4) {
            lines.add("负债率偏高，建议单独关注还款节奏。");
        }
        if (portfolio.missingRateCount > 0) {
            lines.add(portfolio.missingRateCount + " 项资产缺少汇率，建议在“基准币种与汇率”里补齐。");
        }
        if (portfolio.hasMixedCurrencies) {
            lines.add("当前存在多币种资产，总额按本地汇率换算。");
        }
        return joinLines(lines);
    }

    private void renderDataHealth(PortfolioSummary portfolio) {
        dataHealthList.removeAllViews();
        List<String> issues = dataHealthIssues(portfolio);
        if (issues.isEmpty()) {
            dataHealthSummary.setText("数据状态良好：金额、机构、App 绑定、汇率和更新时间都已覆盖。");
            dataHealthList.addView(text("继续保持定期核对即可。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        dataHealthSummary.setText("发现 " + issues.size() + " 类数据维护问题，建议优先处理。");
        for (String issue : issues) {
            dataHealthList.addView(healthIssueRow(issue));
        }
    }

    private List<String> dataHealthIssues(PortfolioSummary portfolio) {
        List<String> issues = new ArrayList<>();
        int missingAmount = 0;
        int invalidAmount = 0;
        int missingInstitution = 0;
        int missingBinding = 0;
        int missingRate = 0;
        int neverUpdated = 0;
        int staleUpdated = 0;

        for (AssetRecord asset : assets) {
            String amount = clean(asset.amount);
            if (amount.isEmpty()) {
                missingAmount += 1;
            } else if (parseNumber(amount) == null) {
                invalidAmount += 1;
            }
            String institution = clean(asset.institution);
            if (institution.isEmpty() || institution.contains("待绑定")) {
                missingInstitution += 1;
            }
            if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
                missingBinding += 1;
            }
            String currency = AssetMath.cleanCurrency(asset.currency);
            if (!settings.hasRateFor(currency)) {
                missingRate += 1;
            }
            if (asset.lastUpdatedAt <= 0) {
                neverUpdated += 1;
            } else if (isStale(asset)) {
                staleUpdated += 1;
            }
        }

        if (assets.isEmpty()) {
            issues.add("还没有资产，请先新增至少一项资产。");
        }
        if (missingAmount > 0) {
            issues.add(missingAmount + " 项资产缺少金额。");
        }
        if (invalidAmount > 0) {
            issues.add(invalidAmount + " 项资产金额无法识别。");
        }
        if (missingInstitution > 0) {
            issues.add(missingInstitution + " 项资产缺少明确机构。");
        }
        if (missingBinding > 0) {
            issues.add(missingBinding + " 项资产还没有绑定 App。");
        }
        if (missingRate > 0) {
            issues.add(missingRate + " 项资产缺少到 " + portfolio.baseCurrency + " 的汇率。");
        }
        if (neverUpdated > 0) {
            issues.add(neverUpdated + " 项资产从未标记更新。");
        }
        if (staleUpdated > 0) {
            issues.add(staleUpdated + " 项资产已超过更新周期。");
        }
        return issues;
    }

    private View healthIssueRow(String issue) {
        TextView row = text("• " + issue, 14, MUTED, Typeface.NORMAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private void renderUpdatePlan() {
        updatePlanList.removeAllViews();
        if (assets.isEmpty()) {
            updatePlanSummary.setText("还没有资产。新增资产后，这里会按更新周期自动排计划。");
            return;
        }

        List<AssetRecord> planned = plannedAssets();
        int urgentCount = 0;
        int soonCount = 0;
        for (AssetRecord asset : assets) {
            int days = daysUntilDue(asset);
            if (days <= 0) {
                urgentCount += 1;
            } else if (days <= 3) {
                soonCount += 1;
            }
        }

        updatePlanSummary.setText(updatePlanSummaryText(urgentCount, soonCount));

        int limit = Math.min(5, planned.size());
        for (int index = 0; index < limit; index += 1) {
            updatePlanList.addView(updatePlanRow(planned.get(index)));
        }
    }

    private List<AssetRecord> plannedAssets() {
        List<AssetRecord> planned = new ArrayList<>(assets);
        Collections.sort(planned, (left, right) -> {
            int daysCompare = Integer.compare(daysUntilDue(left), daysUntilDue(right));
            if (daysCompare != 0) {
                return daysCompare;
            }
            int amountCompare = Double.compare(
                    Math.abs(AssetMath.parseAmount(right.amount)),
                    Math.abs(AssetMath.parseAmount(left.amount))
            );
            if (amountCompare != 0) {
                return amountCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return planned;
    }

    private String updatePlanSummaryText(int urgentCount, int soonCount) {
        return urgentCount + " 项需要现在核对，"
                + soonCount + " 项将在 3 天内到期。";
    }

    private View updatePlanRow(AssetRecord asset) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(12));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(text(asset.name, 14, INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(4);
        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        titleGroup.addView(text(asset.category + " · " + institution, 12, MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView due = updateDueChip(asset);
        header.addView(due);
        row.addView(header);

        LinearLayout actions = row();
        LinearLayout.LayoutParams actionsParams = lp(-1, -2);
        actionsParams.topMargin = dp(10);
        actions.setLayoutParams(actionsParams);

        Button launch = secondaryButton("打开 App");
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(40), 1));
        actions.addView(new SpaceView(this, dp(8), 1));

        Button mark = secondaryButton("已更新");
        mark.setOnClickListener(view -> showAssetUpdateDialog(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, dp(40), 1));
        row.addView(actions);
        return row;
    }

    private TextView updateDueChip(AssetRecord asset) {
        int days = daysUntilDue(asset);
        String label;
        int color;
        if (asset.lastUpdatedAt <= 0) {
            label = "从未更新";
            color = AMBER;
        } else if (days < 0) {
            label = "逾期 " + Math.abs(days) + " 天";
            color = DANGER;
        } else if (days == 0) {
            label = "今天到期";
            color = DANGER;
        } else if (days <= 3) {
            label = days + " 天后到期";
            color = AMBER;
        } else {
            label = days + " 天后";
            color = ACCENT;
        }

        TextView chip = text(label, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private void renderRecentUpdates() {
        recentUpdateList.removeAllViews();
        if (updateEvents.isEmpty()) {
            recentUpdateSummary.setText("还没有更新记录。录入一次最新金额后，这里会显示变化。");
            return;
        }

        recentUpdateSummary.setText(recentUpdateSummaryText());
        List<String> reasonLines = updateReasonSummaryLines(true);
        for (String line : reasonLines) {
            recentUpdateList.addView(reasonSummaryRow(line));
        }
        int limit = Math.min(5, updateEvents.size());
        for (int index = 0; index < limit; index += 1) {
            recentUpdateList.addView(updateEventRow(updateEvents.get(index)));
        }
        if (updateEvents.size() > limit) {
            TextView more = text("还有 " + (updateEvents.size() - limit) + " 条更新记录会随备份保留。", 12, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = lp(-1, -2);
            moreParams.topMargin = dp(8);
            recentUpdateList.addView(more, moreParams);
        }
    }

    private String recentUpdateSummaryText() {
        long cutoff = System.currentTimeMillis() - 30L * AssetMath.DAY_MS;
        int count = 0;
        double deltaInBase = 0;
        for (AssetUpdateEvent event : updateEvents) {
            if (event.timestamp < cutoff) {
                continue;
            }
            count += 1;
            String currency = AssetMath.cleanCurrency(event.currency);
            double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
            double previous = AssetMath.parseAmount(event.previousAmount);
            double current = AssetMath.parseAmount(event.newAmount);
            deltaInBase += (current - previous) * rate;
        }

        if (count == 0) {
            return "保留最近一年更新记录；近 30 天还没有新的金额变化。";
        }
        if (settings.hideAmounts) {
            return "近 30 天记录 " + count + " 次更新，金额变化已隐藏。";
        }
        return "近 30 天记录 " + count + " 次更新，折算净变化 "
                + formatSignedMoney(deltaInBase, settings.baseCurrency) + "。";
    }

    private List<String> updateReasonSummaryLines(boolean includeEmpty) {
        long cutoff = System.currentTimeMillis() - 30L * AssetMath.DAY_MS;
        Map<String, Integer> counts = new HashMap<>();
        Map<String, Double> deltas = new HashMap<>();
        for (AssetUpdateEvent event : updateEvents) {
            if (event.timestamp < cutoff) {
                continue;
            }
            String reason = cleanReason(event.reason);
            counts.put(reason, counts.getOrDefault(reason, 0) + 1);

            String currency = AssetMath.cleanCurrency(event.currency);
            double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
            double previous = AssetMath.parseAmount(event.previousAmount);
            double current = AssetMath.parseAmount(event.newAmount);
            deltas.put(reason, deltas.getOrDefault(reason, 0.0) + (current - previous) * rate);
        }

        List<String> reasons = new ArrayList<>(counts.keySet());
        Collections.sort(reasons, (left, right) -> {
            int countCompare = Integer.compare(counts.getOrDefault(right, 0), counts.getOrDefault(left, 0));
            if (countCompare != 0) {
                return countCompare;
            }
            return left.compareToIgnoreCase(right);
        });

        List<String> lines = new ArrayList<>();
        int limit = Math.min(3, reasons.size());
        for (int index = 0; index < limit; index += 1) {
            String reason = reasons.get(index);
            String line = reason + " " + counts.getOrDefault(reason, 0) + " 次";
            if (!settings.hideAmounts) {
                line += "，折算变化 " + formatSignedMoney(deltas.getOrDefault(reason, 0.0), settings.baseCurrency);
            }
            lines.add(line);
        }

        if (lines.isEmpty() && includeEmpty) {
            lines.add("近 30 天还没有可汇总的变化原因。");
        }
        return lines;
    }

    private View reasonSummaryRow(String line) {
        TextView row = text("原因汇总 · " + line, 13, MUTED, Typeface.NORMAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(cardBackground(SURFACE_ALT, LINE));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private View updateEventRow(AssetUpdateEvent event) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(SURFACE_ALT, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        TextView name = text(event.assetName.isEmpty() ? "未知资产" : event.assetName, 14, INK, Typeface.BOLD);
        header.addView(name, new LinearLayout.LayoutParams(0, -2, 1));

        TextView time = text(dateFormat.format(new Date(event.timestamp)), 12, MUTED, Typeface.NORMAL);
        time.setGravity(Gravity.END);
        header.addView(time);
        row.addView(header);

        LinearLayout.LayoutParams changeParams = lp(-1, -2);
        changeParams.topMargin = dp(6);
        row.addView(text(updateEventChangeText(event), 13, MUTED, Typeface.NORMAL), changeParams);

        LinearLayout.LayoutParams reasonParams = lp(-1, -2);
        reasonParams.topMargin = dp(4);
        row.addView(text("原因：" + cleanReason(event.reason), 12, MUTED, Typeface.NORMAL), reasonParams);

        if (!event.note.isEmpty()) {
            LinearLayout.LayoutParams noteParams = lp(-1, -2);
            noteParams.topMargin = dp(4);
            row.addView(text(event.note, 12, MUTED, Typeface.NORMAL), noteParams);
        }

        Button delete = secondaryButton("删除记录");
        delete.setTextColor(DANGER);
        delete.setOnClickListener(view -> confirmDeleteUpdateEvent(event));
        LinearLayout.LayoutParams deleteParams = lp(-1, dp(38));
        deleteParams.topMargin = dp(8);
        row.addView(delete, deleteParams);
        return row;
    }

    private void confirmDeleteUpdateEvent(AssetUpdateEvent event) {
        String assetName = event.assetName.isEmpty() ? "这条资产" : event.assetName;
        new AlertDialog.Builder(this)
                .setTitle("删除更新记录？")
                .setMessage("确定删除「" + assetName + "」这条更新记录吗？这只删除历史记录，不会回滚资产金额或趋势快照。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    updateEvents = store.deleteUpdateEvent(event.assetId, event.timestamp);
                    render();
                    toast("已删除更新记录。");
                })
                .show();
    }

    private String updateEventChangeText(AssetUpdateEvent event) {
        if (settings.hideAmounts) {
            return "金额变化已隐藏 · " + event.currency;
        }
        String before = event.previousAmount.isEmpty() ? "--" : formatRawAmount(event.previousAmount);
        String after = event.newAmount.isEmpty() ? "--" : formatRawAmount(event.newAmount);
        double previous = AssetMath.parseAmount(event.previousAmount);
        double current = AssetMath.parseAmount(event.newAmount);
        double delta = current - previous;
        return before + " -> " + after + " " + event.currency
                + "（变化 " + formatSignedRawAmount(delta) + " " + event.currency + "）";
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(PANEL, Color.rgb(228, 233, 225)));
        card.setElevation(dp(1));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(14);
        card.setLayoutParams(params);
        return card;
    }

    private TextView sectionTitle(String title) {
        return text(title, 18, INK, Typeface.BOLD);
    }

    private LinearLayout metric(String label, TextView value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(cardBackground(SURFACE, LINE));
        box.addView(text(label, 12, MUTED, Typeface.BOLD));
        LinearLayout.LayoutParams valueParams = lp(-1, -2);
        valueParams.topMargin = dp(6);
        box.addView(value, valueParams);
        return box;
    }

    private View assetCard(AssetRecord asset) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        card.setBackground(cardBackground(PANEL, statusColor(asset)));

        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = text(asset.name, 19, INK, Typeface.BOLD);
        titleGroup.addView(name);

        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        TextView meta = text(asset.category + " · " + institution, 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(3);
        titleGroup.addView(meta, metaParams);

        TextView status = statusChip(asset);
        header.addView(status);
        card.addView(header);

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valueParams = lp(-1, -2);
        valueParams.topMargin = dp(16);
        card.addView(valueRow, valueParams);

        TextView amount = text(formatAmount(asset), 25, INK, Typeface.BOLD);
        valueRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView cadence = text("每 " + asset.updateEveryDays + " 天更新", 13, MUTED, Typeface.BOLD);
        valueRow.addView(cadence);

        TextView updated = text(lastUpdatedText(asset), 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams updatedParams = lp(-1, -2);
        updatedParams.topMargin = dp(10);
        card.addView(updated, updatedParams);

        if (!asset.packageName.isEmpty() || !asset.launchUri.isEmpty()) {
            TextView boundApp = text("绑定 App：" + appDisplayName(asset), 13, BLUE, Typeface.BOLD);
            LinearLayout.LayoutParams appParams = lp(-1, -2);
            appParams.topMargin = dp(8);
            card.addView(boundApp, appParams);
        }

        if (!asset.note.isEmpty()) {
            TextView note = text(asset.note, 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams noteParams = lp(-1, -2);
            noteParams.topMargin = dp(8);
            card.addView(note, noteParams);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = lp(-1, -2);
        actionsParams.topMargin = dp(14);
        card.addView(actions, actionsParams);

        Button launch = secondaryButton("打开 App");
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap1 = new SpaceView(this, dp(8), 1);
        actions.addView(gap1);

        Button mark = secondaryButton("已更新");
        mark.setOnClickListener(view -> showAssetUpdateDialog(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap2 = new SpaceView(this, dp(8), 1);
        actions.addView(gap2);

        Button edit = secondaryButton("编辑");
        edit.setOnClickListener(view -> showEditDialog(asset));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));

        return card;
    }

    private TextView statusChip(AssetRecord asset) {
        TextView chip = text(statusText(asset), 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(statusColor(asset));
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private void showEditDialog(AssetRecord original) {
        boolean creating = original == null;
        AssetRecord draft = creating ? new AssetRecord() : copyOf(original);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        EditText name = input("资产名称", draft.name, InputType.TYPE_CLASS_TEXT);
        form.addView(name);

        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        category.setSelection(indexOf(CATEGORIES, draft.category));
        form.addView(fieldBox("类型", category));

        EditText institution = input("机构", draft.institution, InputType.TYPE_CLASS_TEXT);
        form.addView(institution);

        LinearLayout amountRow = row();
        EditText amount = input("金额", draft.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText currency = input("币种", draft.currency, InputType.TYPE_CLASS_TEXT);
        amountRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));
        amountRow.addView(new SpaceView(this, dp(8), 1));
        amountRow.addView(currency, new LinearLayout.LayoutParams(0, -2, 0.55f));
        form.addView(amountRow);

        EditText cadence = input("更新周期（天）", String.valueOf(draft.updateEveryDays), InputType.TYPE_CLASS_NUMBER);
        form.addView(cadence);

        String[] selectedPackageName = {draft.packageName};
        String[] selectedAppName = {draft.appName};
        String[] selectedLaunchUri = {draft.launchUri};
        TextView selectedApp = text(appBindingText(selectedAppName[0], selectedPackageName[0], selectedLaunchUri[0]), 15, INK, Typeface.BOLD);
        selectedApp.setGravity(Gravity.CENTER_VERTICAL);
        selectedApp.setPadding(dp(14), 0, dp(14), 0);
        selectedApp.setBackground(cardBackground(SURFACE, LINE));
        form.addView(fieldBox("绑定 App", selectedApp));

        Button chooseApp = secondaryButton(selectedPackageName[0].isEmpty() && selectedLaunchUri[0].isEmpty()
                ? "选择 App"
                : "更换 App");
        chooseApp.setOnClickListener(view -> showAppPicker(selected -> {
            selectedPackageName[0] = selected.packageName;
            selectedAppName[0] = selected.label;
            selectedLaunchUri[0] = "";
            selectedApp.setText(selected.label);
            chooseApp.setText("更换 App");
            String institutionValue = clean(institution.getText().toString());
            if (institutionValue.isEmpty() || institutionValue.contains("待绑定")) {
                institution.setText(selected.label);
            }
            toast("已选择 " + selected.label);
        }));
        LinearLayout.LayoutParams chooseAppParams = lp(-1, dp(44));
        chooseAppParams.bottomMargin = dp(10);
        form.addView(chooseApp, chooseAppParams);

        EditText note = input("备注", draft.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(creating ? "新增资产" : "编辑资产")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null);
        if (!creating) {
            builder.setNeutralButton("删除", null);
        }
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                String assetName = clean(name.getText().toString());
                if (assetName.isEmpty()) {
                    toast("资产名称不能为空");
                    return;
                }

                int everyDays = parsePositiveInt(cadence.getText().toString(), 7);
                draft.name = assetName;
                draft.category = String.valueOf(category.getSelectedItem());
                draft.institution = clean(institution.getText().toString());
                draft.amount = clean(amount.getText().toString());
                draft.currency = clean(currency.getText().toString()).isEmpty()
                        ? "CNY"
                        : clean(currency.getText().toString()).toUpperCase(Locale.ROOT);
                draft.updateEveryDays = everyDays;
                draft.appName = clean(selectedAppName[0]);
                draft.packageName = clean(selectedPackageName[0]);
                draft.launchUri = clean(selectedLaunchUri[0]);
                draft.note = clean(note.getText().toString());

                if (creating) {
                    assets.add(draft);
                } else {
                    replaceAsset(draft);
                }
                store.save(assets);
                snapshots = store.recordSnapshot(assets, settings);
                render();
                dialog.dismiss();
            });

            Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (delete != null) {
                delete.setTextColor(DANGER);
                delete.setOnClickListener(button -> new AlertDialog.Builder(this)
                        .setTitle("删除资产")
                        .setMessage("确定删除「" + original.name + "」吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (confirm, which) -> {
                            assets.removeIf(asset -> asset.id.equals(original.id));
                            store.save(assets);
                            snapshots = store.recordSnapshot(assets, settings);
                            render();
                            dialog.dismiss();
                        })
                        .show());
            }
        });

        dialog.show();
    }

    private void showAppPicker(AppSelectionHandler handler) {
        showLaunchableAppPicker("选择已安装 App", "搜索银行、券商、钱包或 App 名称。", handler);
    }

    private List<LaunchableApp> getLaunchableApps() {
        PackageManager packageManager = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolvedApps = packageManager.queryIntentActivities(launcherIntent, 0);
        List<LaunchableApp> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo resolvedApp : resolvedApps) {
            String packageName = resolvedApp.activityInfo == null ? "" : resolvedApp.activityInfo.packageName;
            if (packageName == null
                    || packageName.isEmpty()
                    || packageName.equals(getPackageName())
                    || seenPackages.contains(packageName)) {
                continue;
            }
            seenPackages.add(packageName);
            CharSequence label = resolvedApp.loadLabel(packageManager);
            String appLabel = label == null ? packageName : label.toString();
            Drawable icon = resolvedApp.loadIcon(packageManager);
            apps.add(new LaunchableApp(appLabel, packageName, icon));
        }
        Collections.sort(apps, (left, right) -> left.label.compareToIgnoreCase(right.label));
        return apps;
    }

    private void openLinkedApp(AssetRecord asset) {
        if (asset.launchUri.isEmpty() && asset.packageName.isEmpty()) {
            showAssetAppBindingDialog(asset);
            return;
        }

        if (!asset.launchUri.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(asset.launchUri));
                if (!asset.packageName.isEmpty()) {
                    intent.setPackage(asset.packageName);
                }
                pendingLaunchAssetId = asset.id;
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException error) {
                pendingLaunchAssetId = null;
            }
        }

        if (!asset.packageName.isEmpty()) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(asset.packageName);
            if (launchIntent != null) {
                pendingLaunchAssetId = asset.id;
                startActivity(launchIntent);
                return;
            }
            openMarket(asset.packageName);
            return;
        }

        toast("没有找到可打开的 App。");
    }

    private void showAssetAppBindingDialog(AssetRecord asset) {
        showLaunchableAppPicker("绑定并打开 App", "「" + asset.name + "」还没有绑定 App。先选择一次，以后就能一键打开。", selected -> {
            asset.appName = selected.label;
            asset.packageName = selected.packageName;
            asset.launchUri = "";
            if (asset.institution.isEmpty() || asset.institution.contains("待绑定")) {
                asset.institution = selected.label;
            }
            store.save(assets);
            render();
            toast("已绑定 " + selected.label + "。");
            openLinkedApp(asset);
        });
    }

    private void showLaunchableAppPicker(String title, String helperText, AppSelectionHandler handler) {
        List<LaunchableApp> apps = getLaunchableApps();
        if (apps.isEmpty()) {
            toast("没有找到可启动的 App。");
            return;
        }

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        content.setPadding(pad, dp(6), pad, 0);

        TextView helper = text(helperText, 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helperParams = lp(-1, -2);
        helperParams.bottomMargin = dp(12);
        content.addView(helper, helperParams);

        EditText search = input("搜索 App 名称", "", InputType.TYPE_CLASS_TEXT);
        content.addView(search);

        FrameLayout listFrame = new FrameLayout(this);
        LinearLayout.LayoutParams frameParams = lp(-1, dp(360));
        listFrame.setLayoutParams(frameParams);

        ListView list = new ListView(this);
        list.setDivider(null);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setSelector(buttonBackground(SURFACE_ALT, Color.rgb(226, 238, 232), LINE));
        LaunchableAppAdapter adapter = new LaunchableAppAdapter(apps);
        list.setAdapter(adapter);
        listFrame.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView empty = text("没有匹配的 App", 14, MUTED, Typeface.BOLD);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        listFrame.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        list.setEmptyView(empty);
        content.addView(listFrame);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(content)
                .setNegativeButton("取消", null)
                .create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            LaunchableApp selected = adapter.getItem(position);
            handler.onSelected(selected);
            dialog.dismiss();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                adapter.filter(text == null ? "" : text.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        dialog.setOnShowListener(view -> {
            Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (cancel != null) {
                cancel.setTextColor(MUTED);
            }
        });
        dialog.show();
    }

    private void openMarket(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException error) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            } catch (ActivityNotFoundException ignored) {
                toast("没有找到这个 App，请重新选择绑定 App。");
            }
        }
    }

    private void showMarkUpdatedDialog(AssetRecord asset) {
        showAssetUpdateDialog(asset);
    }

    private void showAssetUpdateDialog(AssetRecord asset) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        TextView description = text("核对「" + asset.name + "」后，可直接录入最新金额。保存后会更新时间并记录今日总资产快照。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        EditText amount = input("最新金额", asset.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amount);

        Spinner reason = new Spinner(this);
        reason.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, UPDATE_REASONS));
        reason.setSelection(indexOf(UPDATE_REASONS, "余额核对"));
        form.addView(fieldBox("变化原因", reason));

        EditText note = input("备注（可选）", asset.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("更新资产")
                .setView(form)
                .setNegativeButton("取消", null)
                .setNeutralButton("仅更新时间", null)
                .setPositiveButton("保存更新", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                applyAssetUpdate(
                        asset,
                        clean(amount.getText().toString()),
                        clean(note.getText().toString()),
                        String.valueOf(reason.getSelectedItem())
                );
                dialog.dismiss();
            });

            Button onlyTime = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            onlyTime.setTextColor(MUTED);
            onlyTime.setOnClickListener(button -> {
                applyAssetUpdate(asset, asset.amount, asset.note, "仅更新时间");
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void applyAssetUpdate(AssetRecord asset, String amount, String note, String reason) {
        String previousAmount = asset.amount;
        long now = System.currentTimeMillis();
        asset.amount = amount;
        asset.note = note;
        asset.lastUpdatedAt = now;
        store.save(assets);
        updateEvents = store.recordUpdateEvent(new AssetUpdateEvent(
                asset.id,
                asset.name,
                now,
                asset.currency,
                previousAmount,
                amount,
                cleanReason(reason),
                note
        ));
        snapshots = store.recordSnapshot(assets, settings);
        render();
        toast("已更新「" + asset.name + "」。");
    }

    private void startBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "money-manager-backup-" + backupDate() + ".json");
        try {
            startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
        } catch (ActivityNotFoundException error) {
            toast("没有找到可保存文件的应用。");
        }
    }

    private void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
        } catch (ActivityNotFoundException error) {
            toast("没有找到可选择文件的应用。");
        }
    }

    private void writeBackup(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                toast("无法写入备份文件。");
                return;
            }
            String raw = store.exportJson(assets, snapshots, updateEvents, settings);
            output.write(raw.getBytes(StandardCharsets.UTF_8));
            toast("备份已导出。");
        } catch (Exception error) {
            toast("导出失败，请重试。");
        }
    }

    private void readBackup(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                toast("无法读取备份文件。");
                return;
            }
            String raw = readUtf8(input);
            AssetBackup backup = store.parseBackup(raw);
            confirmImportBackup(backup);
        } catch (Exception error) {
            toast("导入失败，请确认文件是 Money Manager 备份。");
        }
    }

    private void confirmImportBackup(AssetBackup backup) {
        new AlertDialog.Builder(this)
                .setTitle("导入备份？")
                .setMessage("将导入 " + backup.assets.size() + " 项资产和 "
                        + backup.snapshots.size() + " 个趋势快照、"
                        + backup.updateEvents.size() + " 条更新记录，以及汇率和目标设置，并覆盖当前本机数据。")
                .setNegativeButton("取消", null)
                .setPositiveButton("导入", (dialog, which) -> {
                    store.replaceAll(backup);
                    assets = store.load();
                    settings = store.loadSettings();
                    snapshots = store.loadSnapshots();
                    updateEvents = store.loadUpdateEvents();
                    if (snapshots.isEmpty()) {
                        snapshots = store.recordSnapshot(assets, settings);
                    }
                    render();
                    toast("备份已导入。");
                })
                .show();
    }

    private String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private AssetRecord findAsset(String id) {
        for (AssetRecord asset : assets) {
            if (asset.id.equals(id)) {
                return asset;
            }
        }
        return null;
    }

    private void replaceAsset(AssetRecord updated) {
        for (int index = 0; index < assets.size(); index += 1) {
            if (assets.get(index).id.equals(updated.id)) {
                assets.set(index, updated);
                return;
            }
        }
    }

    private AssetRecord copyOf(AssetRecord asset) {
        return AssetRecord.copyOf(asset);
    }

    private boolean isStale(AssetRecord asset) {
        return AssetMath.isStale(asset);
    }

    private int daysUntilDue(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return -10_000;
        }
        long dueAt = asset.lastUpdatedAt + asset.updateEveryDays * AssetMath.DAY_MS;
        long remaining = dueAt - System.currentTimeMillis();
        if (remaining <= 0) {
            return (int) (remaining / AssetMath.DAY_MS);
        }
        return (int) Math.ceil(remaining / (double) AssetMath.DAY_MS);
    }

    private int statusColor(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return AMBER;
        }
        return isStale(asset) ? DANGER : ACCENT;
    }

    private String statusText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "待更新";
        }
        return isStale(asset) ? "已过期" : "新鲜";
    }

    private String lastUpdatedText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "最后更新：从未更新";
        }
        long days = Math.max(0, (System.currentTimeMillis() - asset.lastUpdatedAt) / AssetMath.DAY_MS);
        return "最后更新：" + dateFormat.format(new Date(asset.lastUpdatedAt)) + " · " + days + " 天前";
    }

    private String appDisplayName(AssetRecord asset) {
        String text = appBindingText(asset.appName, asset.packageName, asset.launchUri);
        return "未选择 App".equals(text) ? "未绑定 App" : text;
    }

    private String appBindingText(String appName, String packageName, String launchUri) {
        String name = clean(appName);
        if (!name.isEmpty()) {
            return name;
        }
        String resolved = resolveAppLabel(packageName);
        if (!resolved.isEmpty()) {
            return resolved;
        }
        if (!clean(packageName).isEmpty() || !clean(launchUri).isEmpty()) {
            return "已绑定 App";
        }
        return "未选择 App";
    }

    private String resolveAppLabel(String packageName) {
        String cleanPackageName = clean(packageName);
        if (cleanPackageName.isEmpty()) {
            return "";
        }
        try {
            PackageManager packageManager = getPackageManager();
            return String.valueOf(packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(cleanPackageName, 0)
            ));
        } catch (Exception error) {
            return "";
        }
    }

    private String formatAmount(AssetRecord asset) {
        if (settings.hideAmounts) {
            return "•••• " + asset.currency;
        }
        if (asset.amount.isEmpty()) {
            return "-- " + asset.currency;
        }
        try {
            double value = Double.parseDouble(asset.amount);
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(value) + " " + asset.currency;
        } catch (NumberFormatException error) {
            return asset.amount + " " + asset.currency;
        }
    }

    private String formatMoney(double value, String currency) {
        if (settings.hideAmounts) {
            return "•••• " + currency;
        }
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return format.format(value) + " " + currency;
    }

    private String formatRawAmount(String value) {
        try {
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(Double.parseDouble(value.replace(",", "")));
        } catch (NumberFormatException error) {
            return value;
        }
    }

    private String formatInputNumber(double value) {
        DecimalFormat format = new DecimalFormat("0.##");
        return format.format(value);
    }

    private String formatRate(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.####");
        return format.format(value);
    }

    private String formatPercent(double value, double total) {
        if (total <= 0) {
            return "0.0%";
        }
        return String.format(Locale.getDefault(), "%.1f%%", value / total * 100);
    }

    private String formatPercentValue(double value) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }

    private String formatPoint(double value) {
        return String.format(Locale.getDefault(), "%+.1f 个百分点", value);
    }

    private String formatSignedMoney(double value, String currency) {
        String sign = value > 0 ? "+" : "";
        return sign + formatMoney(value, currency);
    }

    private String formatSignedRawAmount(double value) {
        String sign = value > 0 ? "+" : "";
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return sign + format.format(value);
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.size(); index += 1) {
            if (index > 0) {
                builder.append("\n");
            }
            builder.append(lines.get(index));
        }
        return builder.toString();
    }

    private String backupDate() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(new Date());
    }

    private String dayKey(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
    }

    private String defaultYearEnd() {
        return new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()) + "-12-31";
    }

    private int daysUntilTimestamp(long timestamp) {
        long remaining = timestamp - System.currentTimeMillis();
        return Math.max(0, (int) Math.ceil(remaining / (double) AssetMath.DAY_MS));
    }

    private Date parseDay(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            format.setLenient(false);
            return format.parse(value);
        } catch (Exception error) {
            return null;
        }
    }

    private Double parseNumber(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    private EditText input(String label, String value, int inputType) {
        EditText input = new EditText(this);
        input.setHint(label);
        input.setText(value);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setInputType(inputType);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setTextSize(15);
        input.setPadding(dp(14), dp(8), dp(14), dp(8));
        input.setBackground(cardBackground(SURFACE, LINE));

        LinearLayout.LayoutParams params = lp(-1, dp(52));
        params.bottomMargin = dp(10);
        input.setLayoutParams(params);
        return input;
    }

    private View fieldBox(String label, View field) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView text = label(label);
        box.addView(text);
        box.addView(field, lp(-1, dp(48)));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(10);
        box.setLayoutParams(params);
        return box;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(lp(-1, -2));
        return row;
    }

    private TextView label(String value) {
        TextView text = text(value.toUpperCase(Locale.ROOT), 12, MUTED, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setIncludeFontPadding(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setStateListAnimator(null);
        button.setBackground(buttonBackground(ACCENT, ACCENT_DARK, ACCENT_DARK));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(INK);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setIncludeFontPadding(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setStateListAnimator(null);
        button.setBackground(buttonBackground(Color.WHITE, SURFACE_ALT, LINE));
        return button;
    }

    private GradientDrawable cardBackground(int fill, int border) {
        return roundedBackground(fill, border, 8);
    }

    private StateListDrawable buttonBackground(int fill, int pressedFill, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(pressedFill, border, 8));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(pressedFill, border, 8));
        states.addState(new int[]{}, roundedBackground(fill, border, 8));
        return states;
    }

    private GradientDrawable roundedBackground(int fill, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), border);
        return bg;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private double parsePositiveDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(",", ""));
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private int indexOf(String[] values, String value) {
        for (int index = 0; index < values.length; index += 1) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanReason(String value) {
        return clean(value).isEmpty() ? "余额核对" : clean(value);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class SpaceView extends FrameLayout {
        SpaceView(Activity activity, int width, int height) {
            super(activity);
            setLayoutParams(new LinearLayout.LayoutParams(width, height));
        }
    }

    private interface AppSelectionHandler {
        void onSelected(LaunchableApp app);
    }

    private final class LaunchableAppAdapter extends BaseAdapter {
        private final List<LaunchableApp> source;
        private final List<LaunchableApp> filtered = new ArrayList<>();

        LaunchableAppAdapter(List<LaunchableApp> apps) {
            source = apps;
            filtered.addAll(apps);
        }

        void filter(String query) {
            String normalized = clean(query).toLowerCase(Locale.ROOT);
            filtered.clear();
            for (LaunchableApp app : source) {
                if (normalized.isEmpty()
                        || app.label.toLowerCase(Locale.ROOT).contains(normalized)) {
                    filtered.add(app);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return filtered.size();
        }

        @Override
        public LaunchableApp getItem(int position) {
            return filtered.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LaunchableApp app = getItem(position);
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setMinimumHeight(dp(70));
            row.setBackground(cardBackground(Color.WHITE, Color.TRANSPARENT));

            ImageView icon = new ImageView(MainActivity.this);
            icon.setImageDrawable(app.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            GradientDrawable iconBg = roundedBackground(SURFACE_ALT, LINE, 8);
            icon.setBackground(iconBg);
            icon.setPadding(dp(6), dp(6), dp(6), dp(6));
            row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

            LinearLayout texts = new LinearLayout(MainActivity.this);
            texts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
            textParams.leftMargin = dp(12);
            row.addView(texts, textParams);

            TextView label = text(app.label, 15, INK, Typeface.BOLD);
            label.setSingleLine(true);
            texts.addView(label);

            TextView hint = text("点击选择此 App", 12, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams hintParams = lp(-1, -2);
            hintParams.topMargin = dp(4);
            texts.addView(hint, hintParams);

            TextView chevron = text("›", 24, BLUE, Typeface.BOLD);
            chevron.setGravity(Gravity.CENTER);
            row.addView(chevron, new LinearLayout.LayoutParams(dp(24), dp(46)));
            return row;
        }
    }

    private static final class LaunchableApp {
        final String label;
        final String packageName;
        final Drawable icon;

        LaunchableApp(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private static final class CurrencyRateField {
        final String currency;
        final EditText input;

        CurrencyRateField(String currency, EditText input) {
            this.currency = currency;
            this.input = input;
        }
    }

    private static final class AllocationTargetField {
        final String category;
        final EditText input;

        AllocationTargetField(String category, EditText input) {
            this.category = category;
            this.input = input;
        }
    }

    private static final class TrendMetric {
        final String label;
        final int count;
        final String currency;
        final boolean complete;
        final AssetSnapshot first;
        final AssetSnapshot last;
        final AssetSnapshot high;
        final AssetSnapshot low;
        final double change;

        TrendMetric(String label, int count, String currency) {
            this(label, count, currency, false, null, null, null, null, 0);
        }

        TrendMetric(
                String label,
                int count,
                String currency,
                boolean complete,
                AssetSnapshot first,
                AssetSnapshot last,
                AssetSnapshot high,
                AssetSnapshot low,
                double change
        ) {
            this.label = label;
            this.count = count;
            this.currency = currency;
            this.complete = complete;
            this.first = first;
            this.last = last;
            this.high = high;
            this.low = low;
            this.change = change;
        }
    }

    private static final class AllocationDrift {
        final String category;
        final double currentPercent;
        final double targetPercent;
        final double amountDelta;
        final int color;

        AllocationDrift(
                String category,
                double currentPercent,
                double targetPercent,
                double amountDelta,
                int color
        ) {
            this.category = category;
            this.currentPercent = currentPercent;
            this.targetPercent = targetPercent;
            this.amountDelta = amountDelta;
            this.color = color;
        }
    }
}
