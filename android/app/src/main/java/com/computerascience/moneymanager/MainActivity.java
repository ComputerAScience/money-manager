package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.computerascience.moneymanager.data.AssetStore;
import com.computerascience.moneymanager.domain.AllocationAnalytics;
import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetFilters;
import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.AssetPresets;
import com.computerascience.moneymanager.domain.DataHealth;
import com.computerascience.moneymanager.domain.InvestmentAnalytics;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.model.AssetBackup;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.InstitutionBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.AllocationChartView;
import com.computerascience.moneymanager.ui.BottomNavBar;
import com.computerascience.moneymanager.ui.SectionDrawer;
import com.computerascience.moneymanager.ui.SectionProgressHandle;
import com.computerascience.moneymanager.ui.SpaceView;
import com.computerascience.moneymanager.ui.TrendChartView;
import com.computerascience.moneymanager.ui.UpdateDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends MoneyManagerActivity {
    private static final int REQUEST_EXPORT_BACKUP = 4101;
    private static final int REQUEST_IMPORT_BACKUP = 4102;
    private static final String APK_DOWNLOAD_URL = BuildConfig.APK_DOWNLOAD_URL;
    private static final String UPDATE_INFO_URL = BuildConfig.UPDATE_INFO_URL;
    private static final String SHARE_PAGE_URL = "https://computerascience.github.io/money-manager/";
    private static final String[] UPDATE_REASONS = {"余额核对", "入金", "出金", "买入卖出", "市场涨跌", "转账", "利息分红", "手续费税费", "负债变化", "仅更新时间", "其他"};
    private static final String PAGE_OVERVIEW = BottomNavBar.PAGE_OVERVIEW;
    private static final String PAGE_INVESTMENT = BottomNavBar.PAGE_INVESTMENT;
    private static final String PAGE_TREND = BottomNavBar.PAGE_TREND;
    private static final String PAGE_ASSETS = BottomNavBar.PAGE_ASSETS;

    private String currentPage = PAGE_OVERVIEW;
    private final AssetDialogs assetDialogs = new AssetDialogs(this, UPDATE_REASONS);
    private final SettingsDialogs settingsDialogs = new SettingsDialogs(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();
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
        refreshExchangeRates(false);
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
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
        header.setPadding(dp(18), statusBarHeight() + dp(14), dp(18), dp(12));
        header.setBackground(headerBackground());

        LinearLayout brand = row();
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_launcher_foreground);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(dp(3), dp(3), dp(3), dp(3));
        mark.setBackground(roundedBackground(ACCENT_DARK, ACCENT, 8));
        brand.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));

        TextView eyebrow = label("Money Manager · " + BuildConfig.CHANNEL_LABEL);
        LinearLayout.LayoutParams eyebrowParams = lp(-2, -2);
        eyebrowParams.leftMargin = dp(10);
        brand.addView(eyebrow, eyebrowParams);

        View brandSpacer = new View(this);
        brand.addView(brandSpacer, new LinearLayout.LayoutParams(0, 1, 1));

        Button settingsButton = iconButton("⚙");
        settingsButton.setTextSize(19);
        settingsButton.setContentDescription("设置");
        settingsButton.setOnClickListener(view -> showSettingsMenu());
        brand.addView(settingsButton, new LinearLayout.LayoutParams(dp(42), dp(38)));
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

        screen.addView(header, lp(-1, -2));

        contentFrame = new FrameLayout(this);
        contentFrame.setClipChildren(false);
        contentFrame.setClipToPadding(false);

        ScrollView scrollView = new ScrollView(this);
        mainScrollView = scrollView;
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);
        scrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> updateSectionProgress());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(48), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        overviewPage = page();
        View overviewSummary = overviewCard();
        View allocation = allocationCard();
        View allocationTarget = allocationTargetCard();
        View institution = institutionCard();
        View netWorthGoal = netWorthGoalCard();
        overviewSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("总资产概览", overviewSummary),
                new SectionDrawer.Item("资产比例", allocation),
                new SectionDrawer.Item("目标比例", allocationTarget),
                new SectionDrawer.Item("机构分布", institution),
                new SectionDrawer.Item("年度目标", netWorthGoal)
        };
        overviewPage.addView(overviewSummary);
        overviewPage.addView(allocation);
        overviewPage.addView(allocationTarget);
        overviewPage.addView(institution);
        overviewPage.addView(netWorthGoal);
        root.addView(overviewPage);

        investmentPage = page();
        View investmentSummary = investmentSummaryCard();
        View investmentStructure = investmentStructureCard();
        View investmentInstitutions = investmentInstitutionsCard();
        View investmentPlan = investmentPlanCard();
        View investmentAccounts = investmentAccountsCard();
        investmentSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("投资总览", investmentSummary),
                new SectionDrawer.Item("投资结构", investmentStructure),
                new SectionDrawer.Item("投资机构", investmentInstitutions),
                new SectionDrawer.Item("投资待核对", investmentPlan),
                new SectionDrawer.Item("投资资产", investmentAccounts)
        };
        investmentPage.addView(investmentSummary);
        investmentPage.addView(investmentStructure);
        investmentPage.addView(investmentInstitutions);
        investmentPage.addView(investmentPlan);
        investmentPage.addView(investmentAccounts);
        root.addView(investmentPage);

        trendPage = page();
        View totalTrend = trendCard();
        View distributionTrend = distributionTrendCard();
        View assetTrend = assetTrendCard();
        View dataHealth = dataHealthCard();
        View recentUpdates = recentUpdatesCard();
        trendSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("一年趋势", totalTrend),
                new SectionDrawer.Item("分布变化", distributionTrend),
                new SectionDrawer.Item("单项资产", assetTrend),
                new SectionDrawer.Item("数据健康", dataHealth),
                new SectionDrawer.Item("更新流水", recentUpdates)
        };
        trendPage.addView(totalTrend);
        trendPage.addView(distributionTrend);
        trendPage.addView(assetTrend);
        trendPage.addView(dataHealth);
        trendPage.addView(recentUpdates);
        root.addView(trendPage);

        assetsPage = page();
        View assetManagement = assetManagementSection();
        View updatePlan = updatePlanCard();
        assetSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("资产管理", assetManagement),
                new SectionDrawer.Item("核对计划", updatePlan)
        };
        assetsPage.addView(assetManagement);
        assetsPage.addView(updatePlan);
        root.addView(assetsPage);

        contentFrame.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        sectionProgressHandle = new SectionProgressHandle(this, view -> showSectionMenu(), new SectionProgressHandle.ProgressDragListener() {
            @Override
            public void onDragStart() {
                beginSectionDrag();
            }

            @Override
            public void onProgress(float progress) {
                updateSectionDrag(progress);
            }

            @Override
            public void onDragEnd(float progress) {
                finishSectionDrag(progress);
            }

            @Override
            public void onDragCancel() {
                cancelSectionDrag();
            }
        });
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        contentFrame.addView(sectionProgressHandle, progressParams);

        sectionDrawer = new SectionDrawer(this, this::scrollToSection);
        sectionDrawer.setVisibility(View.GONE);
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(dp(216), -2, Gravity.RIGHT | Gravity.TOP);
        drawerParams.topMargin = dp(12);
        drawerParams.rightMargin = dp(10);
        contentFrame.addView(sectionDrawer, drawerParams);

        screen.addView(contentFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        bottomNavBar = new BottomNavBar(this, this::selectPage);
        bottomNavBar.setBottomInset(navigationBarHeight());
        screen.addView(bottomNavBar, lp(-1, -2));
        setContentView(screen);
        applySystemBarInsets(screen, header, bottomNavBar);
        updatePageVisibility();
    }

    private void applySystemBarInsets(View screen, View header, BottomNavBar bottomNav) {
        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = Math.max(insets.getSystemWindowInsetTop(), statusBarHeight());
            int bottomInset = Math.max(insets.getSystemWindowInsetBottom(), navigationBarHeight());
            header.setPadding(dp(18), topInset + dp(14), dp(18), dp(12));
            bottomNav.setBottomInset(bottomInset);
            return insets;
        });
        screen.requestApplyInsets();
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutParams(lp(-1, -2));
        return page;
    }

    private void selectPage(String page) {
        if (currentPage.equals(page)) {
            return;
        }
        currentPage = page;
        hideSectionDrawer();
        updatePageVisibility();
        if (mainScrollView != null) {
            mainScrollView.post(() -> mainScrollView.smoothScrollTo(0, 0));
        }
    }

    private void updatePageVisibility() {
        setPageVisible(overviewPage, PAGE_OVERVIEW.equals(currentPage));
        setPageVisible(investmentPage, PAGE_INVESTMENT.equals(currentPage));
        setPageVisible(trendPage, PAGE_TREND.equals(currentPage));
        setPageVisible(assetsPage, PAGE_ASSETS.equals(currentPage));

        if (bottomNavBar != null) {
            bottomNavBar.setSelectedPage(currentPage);
        }

        if (pageTitle != null) {
            pageTitle.setText(pageTitleText());
        }
        if (pageSubtitle != null) {
            pageSubtitle.setText(pageSubtitleText());
        }
        updateSectionProgress();
    }

    private void setPageVisible(View page, boolean visible) {
        if (page != null) {
            page.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String pageTitleText() {
        if (PAGE_INVESTMENT.equals(currentPage)) {
            return "投资";
        }
        if (PAGE_TREND.equals(currentPage)) {
            return "趋势与数据";
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return "资产管理";
        }
        return "总览";
    }

    private String pageSubtitleText() {
        if (PAGE_INVESTMENT.equals(currentPage)) {
            return "投资结构、机构集中度、待核对资产和投资明细。";
        }
        if (PAGE_TREND.equals(currentPage)) {
            return "记录趋势快照，复盘分布变化、单项资产、数据质量和更新流水。";
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return "新增、筛选、绑定、核对资产，并处理下一批待更新。";
        }
        return "净资产、资产分布、机构分布和年度目标。";
    }

    private SectionDrawer.Item[] currentSections() {
        if (PAGE_INVESTMENT.equals(currentPage)) {
            return investmentSections == null ? new SectionDrawer.Item[0] : investmentSections;
        }
        if (PAGE_TREND.equals(currentPage)) {
            return trendSections == null ? new SectionDrawer.Item[0] : trendSections;
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return assetSections == null ? new SectionDrawer.Item[0] : assetSections;
        }
        return overviewSections == null ? new SectionDrawer.Item[0] : overviewSections;
    }

    private void showSectionMenu() {
        if (sectionDrawer == null) {
            return;
        }
        if (sectionDrawer.getVisibility() == View.VISIBLE) {
            hideSectionDrawer();
            return;
        }
        SectionDrawer.Item[] sections = currentSections();
        if (sectionProgressHandle != null) {
            sectionProgressHandle.setSectionLabels(sectionLabels(sections));
        }
        sectionDrawer.setItems(pageTitleText(), sections);
        sectionDrawer.setSelectedIndex(nearestSectionIndexToScroll(sections));
        sectionDrawer.setVisibility(View.VISIBLE);
    }

    private void hideSectionDrawer() {
        if (sectionDrawer != null) {
            sectionDrawer.setVisibility(View.GONE);
        }
    }

    private void beginSectionDrag() {
        SectionDrawer.Item[] sections = currentSections();
        if (sections.length == 0) {
            return;
        }
        hideSectionDrawer();
        int index = nearestSectionIndexToScroll(sections);
        sectionDragIndex = index;
        if (sectionProgressHandle != null) {
            sectionProgressHandle.setSectionLabels(sectionLabels(sections));
            sectionProgressHandle.setActiveSection(index);
        }
    }

    private void updateSectionDrag(float progress) {
        SectionDrawer.Item[] sections = currentSections();
        int index = sectionIndexForProgress(progress);
        if (index < 0 || index >= sections.length) {
            return;
        }
        if (sectionProgressHandle != null) {
            sectionProgressHandle.setActiveSection(index);
        }
        if (index != sectionDragIndex) {
            sectionDragIndex = index;
            scrollToSectionImmediate(sections[index].target);
        }
    }

    private void finishSectionDrag(float progress) {
        SectionDrawer.Item[] sections = currentSections();
        int index = sectionIndexForProgress(progress);
        if (index < 0 || index >= sections.length) {
            sectionDragIndex = -1;
            return;
        }
        sectionDragIndex = index;
        if (sectionProgressHandle != null) {
            sectionProgressHandle.setActiveSection(index);
        }
        scrollToSectionImmediate(sections[index].target);
        sectionDragIndex = -1;
        updateSectionProgress();
    }

    private void cancelSectionDrag() {
        sectionDragIndex = -1;
        updateSectionProgress();
    }

    private int sectionIndexForProgress(float progress) {
        SectionDrawer.Item[] sections = currentSections();
        if (sections.length == 0) {
            return -1;
        }
        if (sections.length == 1) {
            return 0;
        }
        int index = Math.round(Math.max(0f, Math.min(1f, progress)) * (sections.length - 1));
        return Math.max(0, Math.min(sections.length - 1, index));
    }

    private int nearestSectionIndexToScroll(SectionDrawer.Item[] sections) {
        if (sections.length == 0 || mainScrollView == null) {
            return -1;
        }
        int anchor = mainScrollView.getScrollY() + dp(24);
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < sections.length; index += 1) {
            int top = Math.max(0, topInsideScroll(sections[index].target) - dp(8));
            int distance = Math.abs(top - anchor);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return bestIndex;
    }

    private String[] sectionLabels(SectionDrawer.Item[] sections) {
        String[] labels = new String[sections.length];
        for (int index = 0; index < sections.length; index += 1) {
            labels[index] = sections[index].label;
        }
        return labels;
    }

    private void updateSectionProgress() {
        if (sectionDragIndex >= 0 || sectionProgressHandle == null || mainScrollView == null || mainScrollView.getChildCount() == 0) {
            return;
        }
        View content = mainScrollView.getChildAt(0);
        int maxScroll = Math.max(0, content.getHeight() - mainScrollView.getHeight());
        float progress = maxScroll == 0 ? 0f : (float) mainScrollView.getScrollY() / maxScroll;
        sectionProgressHandle.setProgress(progress);
    }

    private void scrollToProgress(float progress) {
        if (mainScrollView == null || mainScrollView.getChildCount() == 0) {
            return;
        }
        hideSectionDrawer();
        View content = mainScrollView.getChildAt(0);
        int maxScroll = Math.max(0, content.getHeight() - mainScrollView.getHeight());
        mainScrollView.scrollTo(0, Math.round(maxScroll * Math.max(0f, Math.min(1f, progress))));
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

    @Override
    public void onBackPressed() {
        if (handleBackNavigation()) {
            return;
        }
        super.onBackPressed();
    }

    private boolean handleBackNavigation() {
        if (sectionDrawer != null && sectionDrawer.getVisibility() == View.VISIBLE) {
            hideSectionDrawer();
            return true;
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            if (!managementExpanded) {
                managementExpanded = true;
                render();
                scrollToAssetManagement();
                return true;
            }
            if (!assetSearchQuery.isEmpty() || !"all".equals(assetFilterMode)) {
                assetSearchQuery = "";
                assetFilterMode = "all";
                if (assetSearchInput != null) {
                    assetSearchInput.setText("");
                }
                render();
                scrollToAssetManagement();
                return true;
            }
            if (!collapsedAssetGroups.isEmpty()) {
                collapsedAssetGroups.clear();
                render();
                scrollToAssetManagement();
                return true;
            }
        }

        if (!PAGE_OVERVIEW.equals(currentPage)) {
            selectPage(PAGE_OVERVIEW);
            return true;
        }
        return false;
    }

    void render() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);

        netWorthValue.setText(formatMoney(portfolio.netWorth, portfolio.baseCurrency));
        grossAssetsValue.setText(formatMoney(portfolio.grossAssets, portfolio.baseCurrency));
        liabilitiesValue.setText(formatMoney(portfolio.liabilities, portfolio.baseCurrency));
        freshnessValue.setText(portfolio.staleCount + " 项待更新");
        privacyToggle.setText(settings.hideAmounts ? "显示金额" : "隐藏金额");
        currencyNote.setText(currencyNoteText(portfolio));
        netWorthGoalSummary.setText(netWorthGoalText(portfolio));
        if (currencySettingsSummary != null) {
            currencySettingsSummary.setText(currencySettingsText());
        }

        allocationChart.setCategories(portfolio.categories);
        renderAllocationLegend(portfolio);
        renderAllocationTargets(portfolio);
        renderInstitutionList(portfolio);
        renderInvestmentPage();

        List<AssetSnapshot> trendSnapshots = TrendAnalytics.snapshotsForBase(snapshots, portfolio.baseCurrency);
        trendChart.setSnapshots(trendSnapshots);
        trendSummary.setText(trendSummaryText(portfolio, trendSnapshots));
        renderTrendMetrics(portfolio, trendSnapshots);
        renderTrendHistory(trendSnapshots);
        renderDistributionTrend(trendSnapshots);
        renderAssetTrend();

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
        List<AssetRecord> visibleAssets = AssetFilters.visibleAssets(
                assets,
                settings,
                assetSearchQuery,
                assetFilterMode,
                this::appDisplayName
        );
        assetResultSummary.setText("按机构分组显示 " + visibleAssets.size() + " / " + assets.size()
                + " 项，当前筛选：" + AssetFilters.label(assetFilterMode) + "。");

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
        netWorthValue.setOnClickListener(view -> showAssetManagement("all"));
        netWorthValue.setContentDescription("调整资产明细");
        LinearLayout.LayoutParams netParams = lp(-1, -2);
        netParams.topMargin = dp(10);
        card.addView(netWorthValue, netParams);

        currencyNote = text("", 13, MUTED, Typeface.NORMAL);
        currencyNote.setOnClickListener(view -> showCurrencySettingsDialog());
        currencyNote.setContentDescription("调整基准币种与汇率");
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
        View grossMetric = metric("资产总额", grossAssetsValue);
        grossMetric.setOnClickListener(view -> showAssetManagement("all"));
        grossMetric.setContentDescription("调整资产总额明细");
        row1.addView(grossMetric, new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(new SpaceView(this, dp(10), 1));
        View liabilitiesMetric = metric("负债", liabilitiesValue);
        liabilitiesMetric.setOnClickListener(view -> showAssetManagement("debt"));
        liabilitiesMetric.setContentDescription("调整负债明细");
        row1.addView(liabilitiesMetric, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row1);

        freshnessValue = text("--", 18, INK, Typeface.BOLD);
        LinearLayout.LayoutParams freshParams = lp(-1, -2);
        freshParams.topMargin = dp(10);
        View freshnessMetric = metric("更新状态", freshnessValue);
        freshnessMetric.setOnClickListener(view -> showAssetManagement("stale"));
        freshnessMetric.setContentDescription("调整待更新资产");
        card.addView(freshnessMetric, freshParams);
        return card;
    }

    private void copyAssetSummary() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        List<AssetSnapshot> trendSnapshots = TrendAnalytics.snapshotsForBase(snapshots, portfolio.baseCurrency);
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
            List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, settings);
            if (!drifts.isEmpty()) {
                lines.add("");
                lines.add("目标比例提醒");
                int limit = Math.min(3, drifts.size());
                for (int index = 0; index < limit; index += 1) {
                    AllocationAnalytics.Drift drift = drifts.get(index);
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

    private View distributionTrendCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("分布变化"));

        distributionTrendSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(distributionTrendSummary, summaryParams);

        distributionTrendList = new LinearLayout(this);
        distributionTrendList.setOrientation(LinearLayout.VERTICAL);
        card.addView(distributionTrendList, lp(-1, -2));
        return card;
    }

    private View assetTrendCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("单项资产趋势"));

        assetTrendSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(10);
        card.addView(assetTrendSummary, summaryParams);

        assetTrendSpinner = new Spinner(this);
        styleSpinner(assetTrendSpinner);
        assetTrendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressAssetTrendSelection || position < 0 || position >= assetTrendOptions.size()) {
                    return;
                }
                selectedTrendAssetId = assetTrendOptions.get(position).id;
                renderAssetTrend();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        card.addView(fieldBox("选择资产", assetTrendSpinner));

        assetTrendChart = new TrendChartView(this);
        LinearLayout.LayoutParams chartParams = lp(-1, dp(160));
        chartParams.topMargin = dp(6);
        chartParams.bottomMargin = dp(10);
        card.addView(assetTrendChart, chartParams);

        assetTrendHistoryList = new LinearLayout(this);
        assetTrendHistoryList.setOrientation(LinearLayout.VERTICAL);
        card.addView(assetTrendHistoryList, lp(-1, -2));
        return card;
    }

    private View updatePlanCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("核对计划"));

        updatePlanSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams updateSummaryParams = lp(-1, -2);
        updateSummaryParams.topMargin = dp(8);
        updateSummaryParams.bottomMargin = dp(8);
        card.addView(updatePlanSummary, updateSummaryParams);

        updatePlanList = new LinearLayout(this);
        updatePlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(updatePlanList, lp(-1, -2));

        Button reviewButton = secondaryButton("查看待处理资产");
        reviewButton.setOnClickListener(view -> showAssetManagement("issues"));
        LinearLayout.LayoutParams buttonParams = lp(-1, dp(42));
        buttonParams.topMargin = dp(10);
        card.addView(reviewButton, buttonParams);
        return card;
    }

    private View dataHealthCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("数据健康"));

        insightSummary = text("", 15, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams insightParams = lp(-1, -2);
        insightParams.topMargin = dp(10);
        insightParams.bottomMargin = dp(12);
        card.addView(insightSummary, insightParams);

        dataHealthSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(2);
        summaryParams.bottomMargin = dp(8);
        card.addView(dataHealthSummary, summaryParams);

        dataHealthList = new LinearLayout(this);
        dataHealthList.setOrientation(LinearLayout.VERTICAL);
        card.addView(dataHealthList, lp(-1, -2));

        Button reviewButton = secondaryButton("查看待处理资产");
        reviewButton.setOnClickListener(view -> showAssetManagement("issues"));
        LinearLayout.LayoutParams reviewParams = lp(-1, dp(42));
        reviewParams.topMargin = dp(10);
        card.addView(reviewButton, reviewParams);
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

    private void showSettingsMenu() {
        new SettingsMenuController(this).show(
                currencySettingsText(),
                appVersionLabel(),
                this::showCurrencySettingsDialog,
                this::showCategorySettingsDialog,
                this::sharePortfolioPage,
                this::startBackupExport,
                this::startBackupImport,
                this::showUpdateDialog,
                this::openApkDownload,
                this::copyApkDownloadLink
        );
    }

    private void showUpdateDialog() {
        UpdateDialog.show(this, appVersionLabel(), appVersionCode(), UPDATE_INFO_URL, APK_DOWNLOAD_URL);
    }

    private String appVersionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String name = info.versionName == null || info.versionName.isEmpty() ? "--" : info.versionName;
            return name + " (" + packageVersionCode(info) + ")";
        } catch (PackageManager.NameNotFoundException error) {
            return "--";
        }
    }

    private long packageVersionCode(PackageInfo info) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
    }

    private long appVersionCode() {
        try {
            return packageVersionCode(getPackageManager().getPackageInfo(getPackageName(), 0));
        } catch (PackageManager.NameNotFoundException error) {
            return 0;
        }
    }

    private void openApkDownload() {
        UpdateDialog.openDownload(this, APK_DOWNLOAD_URL);
    }

    private void copyApkDownloadLink() {
        UpdateDialog.copyDownloadLink(this, APK_DOWNLOAD_URL);
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
        assetFilterButtons.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams filterParams = lp(-1, -2);
        filterParams.bottomMargin = dp(10);
        managementBody.addView(assetFilterButtons, filterParams);

        assetResultSummary = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams resultParams = lp(-1, -2);
        resultParams.bottomMargin = dp(10);
        managementBody.addView(assetResultSummary, resultParams);

        LinearLayout.LayoutParams presetParams = lp(-1, -2);
        presetParams.bottomMargin = dp(14);
        managementBody.addView(assetPresetSection(), presetParams);

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

    private View assetPresetSection() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));

        panel.addView(text("快速新增资产", 13, INK, Typeface.BOLD));
        TextView help = text("先按机构建资产条目；App 只是核对时的一键打开入口，同一机构下可以有多个 App。", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helpParams = lp(-1, -2);
        helpParams.topMargin = dp(4);
        panel.addView(help, helpParams);

        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rowsParams = lp(-1, -2);
        rowsParams.topMargin = dp(10);
        panel.addView(rows, rowsParams);

        List<AssetPresets.Preset> presets = AssetPresets.quickAddPresets();
        for (int index = 0; index < presets.size(); index += 2) {
            LinearLayout row = row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            addPresetButton(row, presets.get(index));
            if (index + 1 < presets.size()) {
                row.addView(new SpaceView(this, dp(8), 1));
                addPresetButton(row, presets.get(index + 1));
            } else {
                row.addView(new SpaceView(this, dp(8), 1));
                row.addView(new SpaceView(this, 1, 1), new LinearLayout.LayoutParams(0, dp(42), 1));
            }
            LinearLayout.LayoutParams rowParams = lp(-1, dp(42));
            if (index > 0) {
                rowParams.topMargin = dp(8);
            }
            rows.addView(row, rowParams);
        }
        return panel;
    }

    private void addPresetButton(LinearLayout row, AssetPresets.Preset preset) {
        Button button = secondaryButton(preset.label);
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setOnClickListener(view -> showCreatePreset(preset));
        row.addView(button, new LinearLayout.LayoutParams(0, dp(42), 1));
    }

    private void showCreateInvestmentAccount() {
        for (AssetPresets.Preset preset : AssetPresets.quickAddPresets()) {
            if (AssetCategories.INVESTMENT_ACCOUNT.equals(preset.category)) {
                settings.setInvestmentCategory(preset.category, true);
                store.saveSettings(settings);
                showCreatePreset(preset);
                return;
            }
        }
        showEditDialog(null);
    }

    private View investmentSummaryCard() {
        LinearLayout card = card();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(sectionTitle("投资总览"));
        investmentSummaryText = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(4);
        titleGroup.addView(investmentSummaryText, summaryParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        Button addButton = primaryButton("新增投资资产");
        addButton.setTextSize(13);
        addButton.setOnClickListener(view -> showCreateInvestmentAccount());
        header.addView(addButton, new LinearLayout.LayoutParams(dp(124), dp(42)));
        card.addView(header);

        TextView help = text("投资页由资产类型开关控制。需要新增“美股、港股、期权”等类型时，可在设置里维护。", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helpParams = lp(-1, -2);
        helpParams.topMargin = dp(12);
        card.addView(help, helpParams);

        Button categoryButton = secondaryButton("管理投资类型");
        categoryButton.setOnClickListener(view -> showCategorySettingsDialog());
        LinearLayout.LayoutParams categoryParams = lp(-1, dp(42));
        categoryParams.topMargin = dp(10);
        card.addView(categoryButton, categoryParams);
        return card;
    }

    private View investmentStructureCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("投资结构"));

        investmentStructureSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(investmentStructureSummary, summaryParams);

        investmentStructureList = new LinearLayout(this);
        investmentStructureList.setOrientation(LinearLayout.VERTICAL);
        card.addView(investmentStructureList, lp(-1, -2));
        return card;
    }

    private View investmentInstitutionsCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("投资机构"));

        investmentInstitutionSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(investmentInstitutionSummary, summaryParams);

        investmentInstitutionList = new LinearLayout(this);
        investmentInstitutionList.setOrientation(LinearLayout.VERTICAL);
        card.addView(investmentInstitutionList, lp(-1, -2));
        return card;
    }

    private View investmentPlanCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("投资待核对"));

        investmentPlanSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(investmentPlanSummary, summaryParams);

        investmentPlanList = new LinearLayout(this);
        investmentPlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(investmentPlanList, lp(-1, -2));
        return card;
    }

    private View investmentAccountsCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("投资资产"));
        investmentAccountList = new LinearLayout(this);
        investmentAccountList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listParams = lp(-1, -2);
        listParams.topMargin = dp(12);
        card.addView(investmentAccountList, listParams);
        return card;
    }

    private void renderInvestmentPage() {
        if (investmentSummaryText == null || investmentAccountList == null
                || investmentStructureList == null || investmentInstitutionList == null
                || investmentPlanList == null) {
            return;
        }
        List<AssetRecord> investments = investmentAssets();
        List<AssetInstitutionGroups.Group> groups = AssetInstitutionGroups.groupByInstitution(investments, settings);
        InvestmentAnalytics.Summary stats = InvestmentAnalytics.summarize(investments, settings);

        investmentSummaryText.setText("投资总额 " + formatMoney(stats.total, settings.baseCurrency)
                + " · " + groups.size() + " 个机构 · " + investments.size() + " 项资产");

        renderInvestmentStructure(investments, stats);
        renderInvestmentInstitutions(groups, stats.total);
        renderInvestmentPlan(investments, stats);

        investmentAccountList.removeAllViews();
        if (investments.isEmpty()) {
            investmentAccountList.addView(emptyText("还没有投资资产。可在设置里把某个资产类型加入投资页。"));
            return;
        }
        for (AssetInstitutionGroups.Group group : groups) {
            investmentAccountList.addView(assetInstitutionGroupHeader(group, settings.baseCurrency));
            if (!collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    investmentAccountList.addView(assetCompactRow(asset));
                }
            }
        }
    }

    private void renderInvestmentStructure(List<AssetRecord> investments, InvestmentAnalytics.Summary stats) {
        investmentStructureList.removeAllViews();
        if (investments.isEmpty()) {
            investmentStructureSummary.setText("投资类型开启后，这里会展示持仓、现金和类型分布。");
            investmentStructureList.addView(emptyText("还没有投资资产。"));
            return;
        }

        double cashPercent = stats.total <= 0 ? 0 : stats.cash / stats.total * 100;
        investmentStructureSummary.setText("持仓 " + formatPercent(stats.holding, stats.total)
                + " · 闲置现金 " + formatPercentValue(cashPercent)
                + " · " + investments.size() + " 项投资资产。");

        investmentStructureList.addView(investmentInfoRow(
                "持仓市值",
                formatMoney(stats.holding, settings.baseCurrency),
                "投资账户持仓，以及基金、加密资产等按投资类型纳入的资产。",
                ACCENT
        ));
        investmentStructureList.addView(investmentInfoRow(
                "闲置现金",
                formatMoney(stats.cash, settings.baseCurrency),
                cashPercent >= 30
                        ? "现金占比较高，适合确认是否刻意留仓。"
                        : "现金占比用于观察券商账户里的未投资资金。",
                cashPercent >= 30 ? AMBER : BLUE
        ));

        int limit = Math.min(4, stats.categories.size());
        for (int index = 0; index < limit; index += 1) {
            CategoryBreakdown category = stats.categories.get(index);
            investmentStructureList.addView(investmentInfoRow(
                    "类型 · " + category.category,
                    formatPercent(category.value, stats.total),
                    settings.hideAmounts
                            ? "金额已隐藏。"
                            : formatMoney(category.value, settings.baseCurrency),
                    category.color
            ));
        }
    }

    private void renderInvestmentInstitutions(List<AssetInstitutionGroups.Group> groups, double total) {
        investmentInstitutionList.removeAllViews();
        if (groups.isEmpty()) {
            investmentInstitutionSummary.setText("暂无投资机构。新增投资资产后会按机构汇总。");
            investmentInstitutionList.addView(emptyText("还没有可展示的投资机构。"));
            return;
        }

        AssetInstitutionGroups.Group top = groups.get(0);
        investmentInstitutionSummary.setText("最大机构是 " + top.title
                + "，占投资资产 " + formatPercent(top.total, total)
                + "；共 " + groups.size() + " 个投资机构。");

        int limit = Math.min(5, groups.size());
        for (int index = 0; index < limit; index += 1) {
            investmentInstitutionList.addView(investmentInstitutionRow(groups.get(index), total));
        }
    }

    private void renderInvestmentPlan(List<AssetRecord> investments, InvestmentAnalytics.Summary stats) {
        investmentPlanList.removeAllViews();
        if (investments.isEmpty()) {
            investmentPlanSummary.setText("还没有投资资产。");
            investmentPlanList.addView(emptyText("新增投资资产后，这里会按更新时间排序。"));
            return;
        }

        investmentPlanSummary.setText(stats.dueNow + " 项投资资产需要现在核对，"
                + stats.dueSoon + " 项将在 3 天内到期。");

        List<AssetRecord> planned = sortedPlannedAssets(investments);
        int limit = Math.min(5, planned.size());
        List<AssetRecord> topPlanned = new ArrayList<>(planned.subList(0, limit));
        for (AssetInstitutionGroups.Group group : AssetInstitutionGroups.groupByInstitution(topPlanned, settings)) {
            investmentPlanList.addView(assetInstitutionGroupHeader(group, settings.baseCurrency));
            if (!collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    investmentPlanList.addView(updatePlanRow(asset));
                }
            }
        }
    }

    private View investmentInstitutionRow(AssetInstitutionGroups.Group group, double total) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        row.addView(institutionGroupIcon(group), new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(10);
        titleGroup.addView(text(group.title, 14, INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(3);
        titleGroup.addView(text(group.assets.size() + " 项 · " + group.displayApps(), 12, MUTED, Typeface.NORMAL), metaParams);
        row.addView(titleGroup, titleParams);

        String value = settings.hideAmounts
                ? formatPercent(group.total, total)
                : formatMoney(group.total, settings.baseCurrency) + "\n" + formatPercent(group.total, total);
        TextView amount = text(value, 12, MUTED, Typeface.BOLD);
        amount.setGravity(Gravity.RIGHT);
        row.addView(amount);
        return row;
    }

    private View investmentInfoRow(String title, String value, String detail, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        TextView dot = text("●", 15, color, Typeface.BOLD);
        header.addView(dot);
        TextView label = text("  " + title, 14, INK, Typeface.BOLD);
        header.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        TextView metric = text(value, 14, MUTED, Typeface.BOLD);
        metric.setGravity(Gravity.RIGHT);
        header.addView(metric);
        row.addView(header);

        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(6);
        row.addView(text(detail, 12, MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private TextView emptyText(String message) {
        TextView empty = text(message, 14, MUTED, Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(14), dp(22), dp(14), dp(22));
        empty.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        return empty;
    }

    private List<AssetRecord> investmentAssets() {
        List<AssetRecord> investments = new ArrayList<>();
        for (AssetRecord asset : assets) {
            if (settings.isInvestmentCategory(asset.category)) {
                investments.add(asset);
            }
        }
        Collections.sort(investments, (left, right) -> Double.compare(
                AssetMath.assetGrossAmount(right),
                AssetMath.assetGrossAmount(left)
        ));
        return investments;
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
        scrollToSection(assetManagementCard);
    }

    private void scrollToSection(View target) {
        if (mainScrollView == null || target == null) {
            return;
        }
        target.post(() -> mainScrollView.smoothScrollTo(0, Math.max(0, topInsideScroll(target) - dp(8))));
    }

    private void scrollToSectionImmediate(View target) {
        if (mainScrollView == null || target == null) {
            return;
        }
        mainScrollView.scrollTo(0, Math.max(0, topInsideScroll(target) - dp(8)));
    }

    private int topInsideScroll(View target) {
        int top = target.getTop();
        ViewParent parent = target.getParent();
        while (parent instanceof View && parent != mainScrollView) {
            View parentView = (View) parent;
            top += parentView.getTop();
            parent = parentView.getParent();
        }
        return top;
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

        List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, settings);
        int offTrack = 0;
        for (AllocationAnalytics.Drift drift : drifts) {
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

    private View allocationTargetRow(AllocationAnalytics.Drift drift) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
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

    private TextView allocationDriftChip(AllocationAnalytics.Drift drift) {
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

    private String allocationRecommendationText(AllocationAnalytics.Drift drift) {
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
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
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
        settingsDialogs.showNetWorthGoalDialog();
    }

    private void showCurrencySettingsDialog() {
        settingsDialogs.showCurrencySettingsDialog();
    }

    private void refreshExchangeRates(boolean showToast) {
        settingsDialogs.refreshExchangeRates(showToast);
    }

    private void showAllocationTargetDialog() {
        settingsDialogs.showAllocationTargetDialog();
    }

    private void showCategorySettingsDialog() {
        settingsDialogs.showCategorySettingsDialog();
    }

    private void renderAssetFilterButtons() {
        assetFilterButtons.removeAllViews();

        LinearLayout filterGroup = new LinearLayout(this);
        filterGroup.setOrientation(LinearLayout.VERTICAL);
        filterGroup.setPadding(dp(3), dp(3), dp(3), dp(3));
        filterGroup.setBackground(cardBackground(PANEL, PANEL_BORDER));

        LinearLayout firstRow = row();
        firstRow.setGravity(Gravity.CENTER_VERTICAL);
        addSegmentFilterButton(firstRow, "全部", "all");
        addSegmentFilterButton(firstRow, "待更新", "stale");
        addSegmentFilterButton(firstRow, "未绑定", "unbound");
        filterGroup.addView(firstRow, lp(-1, dp(38)));

        LinearLayout secondRow = row();
        secondRow.setGravity(Gravity.CENTER_VERTICAL);
        addSegmentFilterButton(secondRow, "待完善", "issues");
        addSegmentFilterButton(secondRow, "负债", "debt");
        LinearLayout.LayoutParams secondParams = lp(-1, dp(38));
        secondParams.topMargin = dp(3);
        filterGroup.addView(secondRow, secondParams);

        assetFilterButtons.addView(filterGroup, lp(-1, dp(85)));
    }

    private void addSegmentFilterButton(LinearLayout row, String label, String mode) {
        boolean active = assetFilterMode.equals(mode);
        Button button = secondaryButton(label);
        button.setTextSize(12);
        button.setSingleLine(true);
        button.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        button.setTextColor(active ? Color.WHITE : MUTED);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(buttonBackground(
                active ? ACCENT : Color.TRANSPARENT,
                active ? ACCENT_DARK : ROW_SURFACE,
                active ? ACCENT_DARK : Color.TRANSPARENT
        ));
        button.setOnClickListener(view -> {
            assetFilterMode = mode;
            render();
        });
        row.addView(button, new LinearLayout.LayoutParams(0, dp(38), 1));
    }

    private void renderAssetGroups(List<AssetRecord> visibleAssets, PortfolioSummary portfolio) {
        for (AssetInstitutionGroups.Group group : AssetInstitutionGroups.groupByInstitution(visibleAssets, settings)) {
            assetList.addView(assetInstitutionGroupHeader(group, portfolio.baseCurrency));
            if (!collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    assetList.addView(assetCompactRow(asset));
                }
            }
        }
    }

    private View assetInstitutionGroupHeader(AssetInstitutionGroups.Group group, String baseCurrency) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(6);
        rowParams.bottomMargin = dp(10);
        row.setLayoutParams(rowParams);

        row.addView(institutionGroupIcon(group), new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout labelGroup = new LinearLayout(this);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(10);
        labelGroup.addView(text(group.title + " · " + group.assets.size() + " 项资产", 14, INK, Typeface.BOLD));

        String detail = "小计 " + formatMoney(group.total, baseCurrency)
                + " · " + group.displayKinds()
                + " · " + group.staleCount + " 项待更新"
                + " · " + group.displayApps();
        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(4);
        labelGroup.addView(text(detail, 12, MUTED, Typeface.NORMAL), detailParams);
        row.addView(labelGroup, labelParams);

        Button toggle = secondaryButton(collapsedAssetGroups.contains(group.key) ? "展开" : "折叠");
        toggle.setOnClickListener(view -> {
            if (collapsedAssetGroups.contains(group.key)) {
                collapsedAssetGroups.remove(group.key);
            } else {
                collapsedAssetGroups.add(group.key);
            }
            render();
        });
        row.addView(toggle, new LinearLayout.LayoutParams(dp(72), dp(38)));
        return row;
    }

    private View institutionGroupIcon(AssetInstitutionGroups.Group group) {
        Drawable icon = resolveAppIcon(group.primaryPackageName());
        if (icon != null) {
            ImageView image = new ImageView(this);
            image.setImageDrawable(icon);
            image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setPadding(dp(5), dp(5), dp(5), dp(5));
            image.setBackground(roundedBackground(PANEL, PANEL_BORDER, 8));
            return image;
        }

        TextView fallback = text(group.hasBoundApp() ? "机构" : "未", 12, group.hasBoundApp() ? ACCENT : AMBER, Typeface.BOLD);
        fallback.setGravity(Gravity.CENTER);
        fallback.setBackground(roundedBackground(SURFACE_ALT, PANEL_BORDER, 8));
        return fallback;
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

        List<TrendAnalytics.Metric> metrics = TrendAnalytics.metrics(portfolio, trendSnapshots, System.currentTimeMillis());
        if (metrics.isEmpty()) {
            TextView empty = text("至少记录两次快照后，会显示近 30 天、90 天和一年的变化。", 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = lp(-1, -2);
            emptyParams.topMargin = dp(8);
            trendMetricsList.addView(empty, emptyParams);
            return;
        }

        for (TrendAnalytics.Metric metric : metrics) {
            trendMetricsList.addView(trendMetricRow(metric));
        }
    }

    private List<String> trendReviewLines(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        List<String> lines = new ArrayList<>();
        for (TrendAnalytics.Metric metric : TrendAnalytics.metrics(portfolio, trendSnapshots, System.currentTimeMillis())) {
            if (metric.complete) {
                lines.add(metric.label + "：" + metricSummaryText(metric));
            }
        }
        return lines;
    }

    private View trendMetricRow(TrendAnalytics.Metric metric) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
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

    private String metricSummaryText(TrendAnalytics.Metric metric) {
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

    private void renderDistributionTrend(List<AssetSnapshot> trendSnapshots) {
        distributionTrendList.removeAllViews();
        List<AssetSnapshot> available = TrendAnalytics.snapshotsWithCategoryValues(trendSnapshots);
        if (available.size() < 2) {
            int count = available.size();
            distributionTrendSummary.setText("已记录 " + count + " 个带分布的快照；从这版开始，每次更新或记录快照都会保存类型分布。");
            TextView empty = text("再记录一次快照后，这里会显示各资产类型金额和占比的变化。", 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = lp(-1, -2);
            emptyParams.topMargin = dp(8);
            distributionTrendList.addView(empty, emptyParams);
            return;
        }

        AssetSnapshot first = available.get(0);
        AssetSnapshot last = available.get(available.size() - 1);
        List<TrendAnalytics.CategoryShift> shifts = TrendAnalytics.categoryShifts(first, last);
        if (shifts.isEmpty()) {
            distributionTrendSummary.setText("已记录 " + available.size() + " 个带分布的快照，但暂时没有可对比的类型金额。");
            distributionTrendList.addView(text("继续更新资产金额后再查看分布变化。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        distributionTrendSummary.setText(distributionTrendSummaryText(first, last, shifts, available.size()));
        int limit = Math.min(6, shifts.size());
        for (int index = 0; index < limit; index += 1) {
            distributionTrendList.addView(categoryShiftRow(shifts.get(index), last.baseCurrency));
        }
    }

    private String distributionTrendSummaryText(
            AssetSnapshot first,
            AssetSnapshot last,
            List<TrendAnalytics.CategoryShift> shifts,
            int count
    ) {
        TrendAnalytics.CategoryShift biggest = shifts.get(0);
        if (settings.hideAmounts) {
            return "已记录 " + count + " 个带分布快照，范围 "
                    + first.dayKey + " 到 " + last.dayKey + "；金额已隐藏。";
        }
        return "从 " + first.dayKey + " 到 " + last.dayKey
                + "，变化最大的是 " + biggest.category + "："
                + formatSignedMoney(biggest.delta, last.baseCurrency)
                + "，占比 " + formatPoint(biggest.percentDelta) + "。";
    }

    private View categoryShiftRow(TrendAnalytics.CategoryShift shift, String currency) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        header.addView(text(shift.category, 14, INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView change = text(formatPoint(shift.percentDelta), 12,
                shift.percentDelta >= 0 ? ACCENT : DANGER,
                Typeface.BOLD);
        change.setGravity(Gravity.END);
        header.addView(change);
        row.addView(header);

        String detail;
        if (settings.hideAmounts) {
            detail = "占比 " + formatPercentValue(shift.firstPercent)
                    + " -> " + formatPercentValue(shift.lastPercent)
                    + "，金额已隐藏。";
        } else {
            detail = formatMoney(shift.firstValue, currency)
                    + " -> " + formatMoney(shift.lastValue, currency)
                    + "，变化 " + formatSignedMoney(shift.delta, currency)
                    + "；占比 " + formatPercentValue(shift.firstPercent)
                    + " -> " + formatPercentValue(shift.lastPercent) + "。";
        }
        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(6);
        row.addView(text(detail, 13, MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private void renderAssetTrend() {
        assetTrendOptions = new ArrayList<>(assets);
        Collections.sort(assetTrendOptions, (left, right) -> left.name.compareToIgnoreCase(right.name));

        assetTrendHistoryList.removeAllViews();
        if (assetTrendOptions.isEmpty()) {
            assetTrendSummary.setText("新增资产后，这里会显示每一项资产的金额变化。");
            assetTrendChart.setPoints(new ArrayList<>(), "还没有资产");
            suppressAssetTrendSelection = true;
            assetTrendSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
            suppressAssetTrendSelection = false;
            return;
        }

        int selectedIndex = 0;
        if (!selectedTrendAssetId.isEmpty()) {
            for (int index = 0; index < assetTrendOptions.size(); index += 1) {
                if (selectedTrendAssetId.equals(assetTrendOptions.get(index).id)) {
                    selectedIndex = index;
                    break;
                }
            }
        }
        selectedTrendAssetId = assetTrendOptions.get(selectedIndex).id;

        List<String> names = new ArrayList<>();
        for (AssetRecord asset : assetTrendOptions) {
            names.add(asset.name);
        }
        suppressAssetTrendSelection = true;
        assetTrendSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        assetTrendSpinner.setSelection(selectedIndex);
        suppressAssetTrendSelection = false;

        AssetRecord selected = assetTrendOptions.get(selectedIndex);
        List<AssetUpdateEvent> events = updateEventsForAsset(selected.id);
        List<TrendChartView.Point> points = assetTrendPoints(selected, events);
        assetTrendChart.setPoints(points, "更新几次金额后显示单项趋势");
        assetTrendSummary.setText(assetTrendSummaryText(selected, points));

        assetTrendHistoryList.addView(text("最近变化", 13, MUTED, Typeface.BOLD));
        if (events.isEmpty()) {
            TextView empty = text("这项资产还没有更新记录。点“已更新”录入几次金额后，就能看到单项趋势。", 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = lp(-1, -2);
            emptyParams.topMargin = dp(8);
            assetTrendHistoryList.addView(empty, emptyParams);
            return;
        }
        int limit = Math.min(5, events.size());
        for (int index = 0; index < limit; index += 1) {
            assetTrendHistoryList.addView(updateEventRow(events.get(index)));
        }
    }

    private List<AssetUpdateEvent> updateEventsForAsset(String assetId) {
        List<AssetUpdateEvent> events = new ArrayList<>();
        for (AssetUpdateEvent event : updateEvents) {
            if (assetId.equals(event.assetId)) {
                events.add(event);
            }
        }
        Collections.sort(events, (left, right) -> Long.compare(right.timestamp, left.timestamp));
        return events;
    }

    private List<TrendChartView.Point> assetTrendPoints(AssetRecord asset, List<AssetUpdateEvent> newestFirst) {
        List<AssetUpdateEvent> ascending = new ArrayList<>(newestFirst);
        Collections.sort(ascending, (left, right) -> Long.compare(left.timestamp, right.timestamp));

        List<TrendChartView.Point> points = new ArrayList<>();
        for (AssetUpdateEvent event : ascending) {
            if (points.isEmpty()) {
                points.add(new TrendChartView.Point(event.timestamp - 1, AssetMath.parseAmount(event.previousAmount)));
            }
            points.add(new TrendChartView.Point(event.timestamp, AssetMath.parseAmount(event.newAmount)));
        }

        if (points.isEmpty() && !asset.amount.isEmpty()) {
            points.add(new TrendChartView.Point(
                    asset.lastUpdatedAt <= 0 ? System.currentTimeMillis() : asset.lastUpdatedAt,
                    AssetMath.parseAmount(asset.amount)
            ));
        }
        return points;
    }

    private String assetTrendSummaryText(AssetRecord asset, List<TrendChartView.Point> points) {
        if (points.size() < 2) {
            return "当前 " + asset.name + " 只有 " + points.size() + " 个记录点，继续更新后会形成单项趋势。";
        }
        if (settings.hideAmounts) {
            return asset.name + " 已记录 " + points.size() + " 个变化点，金额已隐藏。";
        }
        TrendChartView.Point first = points.get(0);
        TrendChartView.Point last = points.get(points.size() - 1);
        double change = last.value - first.value;
        double ratio = Math.abs(first.value) < 0.0001 ? 0 : change / Math.abs(first.value) * 100;
        return asset.name + " 共 " + points.size() + " 个变化点，变化 "
                + formatSignedRawAmount(change) + " " + asset.currency
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private View snapshotRow(AssetSnapshot snapshot) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("删除快照？")
                .setMessage("确定删除 " + snapshot.dayKey + " 的 " + snapshot.baseCurrency + " 快照吗？趋势图会立刻更新。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (ignoredDialog, which) -> {
                    snapshots = store.deleteSnapshot(snapshot.dayKey, snapshot.baseCurrency);
                    render();
                    toast("已删除趋势快照。");
                })
                .create();
        showStyledDialog(dialog);
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

        showStyledDialog(dialog);
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
            List<AllocationAnalytics.Drift> drifts = AllocationAnalytics.drifts(portfolio, settings);
            if (!drifts.isEmpty()) {
                AllocationAnalytics.Drift largestDrift = drifts.get(0);
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
        if (portfolio.grossAssets > 0 && portfolio.liabilities / portfolio.grossAssets > 0.4) {
            lines.add("负债率偏高，建议单独关注还款节奏。");
        }
        if (portfolio.hasMixedCurrencies) {
            lines.add("当前存在多币种资产，总额会按最新或本地汇率换算。");
        }
        if (lines.isEmpty()) {
            lines.add("暂无突出的配置或目标风险，按下面的更新周期处理即可。");
        }
        return joinLines(lines);
    }

    private void renderDataHealth(PortfolioSummary portfolio) {
        dataHealthList.removeAllViews();
        List<String> issues = DataHealth.portfolioIssues(assets, settings, portfolio.baseCurrency);
        if (issues.isEmpty()) {
            dataHealthSummary.setText("数据状态良好：金额、机构、App 绑定和汇率都已覆盖。");
            dataHealthList.addView(text("继续保持定期核对即可。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        List<AssetInstitutionGroups.Group> groups = DataHealth.issueGroups(assets, settings);
        dataHealthSummary.setText("发现 " + issues.size() + " 类数据维护问题，分布在 "
                + groups.size() + " 个机构，建议优先处理。");
        for (AssetInstitutionGroups.Group group : groups) {
            dataHealthList.addView(healthInstitutionRow(group));
        }
    }

    private View healthIssueRow(String issue) {
        TextView row = text("• " + issue, 14, MUTED, Typeface.NORMAL);
        row.setPadding(dp(12), dp(8), dp(12), dp(8));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private View healthInstitutionRow(AssetInstitutionGroups.Group group) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(12));
        card.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.topMargin = dp(8);
        card.setLayoutParams(cardParams);

        LinearLayout header = row();
        header.addView(institutionGroupIcon(group), new LinearLayout.LayoutParams(dp(36), dp(36)));
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = dp(10);
        titleGroup.addView(text(group.title, 14, INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(3);
        titleGroup.addView(text(group.assets.size() + " 项需要完善 · " + group.displayApps(), 12, MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, titleParams);
        card.addView(header);

        int limit = Math.min(3, group.assets.size());
        for (int index = 0; index < limit; index += 1) {
            AssetRecord asset = group.assets.get(index);
            String detail = asset.name + " · " + joinInline(DataHealth.assetIssues(asset, settings));
            card.addView(healthIssueRow(detail));
        }
        if (group.assets.size() > limit) {
            TextView more = text("还有 " + (group.assets.size() - limit) + " 项可在资产页继续处理。", 12, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = lp(-1, -2);
            moreParams.topMargin = dp(6);
            card.addView(more, moreParams);
        }
        return card;
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

        int limit = Math.min(8, planned.size());
        List<AssetRecord> topPlanned = new ArrayList<>(planned.subList(0, limit));
        for (AssetInstitutionGroups.Group group : AssetInstitutionGroups.groupByInstitution(topPlanned, settings)) {
            updatePlanList.addView(assetInstitutionGroupHeader(group, settings.baseCurrency));
            if (!collapsedAssetGroups.contains(group.key)) {
                for (AssetRecord asset : group.assets) {
                    updatePlanList.addView(updatePlanRow(asset));
                }
            }
        }
        if (planned.size() > limit) {
            TextView more = text("还有 " + (planned.size() - limit) + " 项资产会按机构继续排队。", 12, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams moreParams = lp(-1, -2);
            moreParams.topMargin = dp(8);
            updatePlanList.addView(more, moreParams);
        }
    }

    private List<AssetRecord> plannedAssets() {
        return sortedPlannedAssets(assets);
    }

    private List<AssetRecord> sortedPlannedAssets(List<AssetRecord> source) {
        List<AssetRecord> planned = new ArrayList<>(source);
        Collections.sort(planned, (left, right) -> {
            int daysCompare = Integer.compare(daysUntilDue(left), daysUntilDue(right));
            if (daysCompare != 0) {
                return daysCompare;
            }
            int amountCompare = Double.compare(
                    assetMagnitude(right),
                    assetMagnitude(left)
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
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        TextView typeMark = categoryMark(asset);
        LinearLayout.LayoutParams typeMarkParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        typeMarkParams.rightMargin = dp(10);
        header.addView(typeMark, typeMarkParams);

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
            counts.put(reason, intValue(counts, reason) + 1);

            String currency = AssetMath.cleanCurrency(event.currency);
            double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
            double previous = AssetMath.parseAmount(event.previousAmount);
            double current = AssetMath.parseAmount(event.newAmount);
            deltas.put(reason, doubleValue(deltas, reason) + (current - previous) * rate);
        }

        List<String> reasons = new ArrayList<>(counts.keySet());
        Collections.sort(reasons, (left, right) -> {
            int countCompare = Integer.compare(intValue(counts, right), intValue(counts, left));
            if (countCompare != 0) {
                return countCompare;
            }
            return left.compareToIgnoreCase(right);
        });

        List<String> lines = new ArrayList<>();
        int limit = Math.min(3, reasons.size());
        for (int index = 0; index < limit; index += 1) {
            String reason = reasons.get(index);
            String line = reason + " " + intValue(counts, reason) + " 次";
            if (!settings.hideAmounts) {
                line += "，折算变化 " + formatSignedMoney(doubleValue(deltas, reason), settings.baseCurrency);
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
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private View updateEventRow(AssetUpdateEvent event) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("删除更新记录？")
                .setMessage("确定删除「" + assetName + "」这条更新记录吗？这只删除历史记录，不会回滚资产金额或趋势快照。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (ignoredDialog, which) -> {
                    updateEvents = store.deleteUpdateEvent(event.assetId, event.timestamp);
                    render();
                    toast("已删除更新记录。");
                })
                .create();
        showStyledDialog(dialog);
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

    private View assetCard(AssetRecord asset) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        card.setBackground(cardBackground(PANEL, PANEL_BORDER));
        card.setElevation(dp(2));

        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView categoryMark = categoryMark(asset);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        markParams.rightMargin = dp(12);
        header.addView(categoryMark, markParams);

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

        boolean hasBoundApp = !asset.packageName.isEmpty() || !asset.launchUri.isEmpty();
        TextView boundApp = text(
                hasBoundApp ? "App · " + appDisplayName(asset) : "App · 未绑定，点击打开时选择",
                13,
                hasBoundApp ? BLUE : AMBER,
                Typeface.BOLD
        );
        LinearLayout.LayoutParams appParams = lp(-1, -2);
        appParams.topMargin = dp(8);
        card.addView(boundApp, appParams);

        List<String> breakdownLines = assetBreakdownLines(asset);
        if (!breakdownLines.isEmpty()) {
            TextView breakdown = text(joinLines(breakdownLines), 13, MUTED, Typeface.NORMAL);
            breakdown.setPadding(dp(12), dp(10), dp(12), dp(10));
            breakdown.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
            LinearLayout.LayoutParams breakdownParams = lp(-1, -2);
            breakdownParams.topMargin = dp(10);
            card.addView(breakdown, breakdownParams);
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

        Button launch = secondaryButton("↗ 打开");
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap1 = new SpaceView(this, dp(8), 1);
        actions.addView(gap1);

        Button mark = secondaryButton("✓ 更新");
        mark.setOnClickListener(view -> showAssetUpdateDialog(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap2 = new SpaceView(this, dp(8), 1);
        actions.addView(gap2);

        Button edit = secondaryButton("✎ 编辑");
        edit.setOnClickListener(view -> showEditDialog(asset));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));

        return card;
    }

    private View assetCompactRow(AssetRecord asset) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(12), dp(12), dp(12), dp(10));
        item.setBackground(cardBackground(PANEL, PANEL_BORDER));
        LinearLayout.LayoutParams itemParams = lp(-1, -2);
        itemParams.bottomMargin = dp(8);
        item.setLayoutParams(itemParams);

        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        item.addView(top);

        TextView mark = categoryMark(asset);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        markParams.rightMargin = dp(10);
        top.addView(mark, markParams);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        top.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = text(asset.name, 15, INK, Typeface.BOLD);
        name.setSingleLine(true);
        titleGroup.addView(name);

        TextView meta = text(asset.category + " · 每 " + asset.updateEveryDays + " 天", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(3);
        titleGroup.addView(meta, metaParams);

        TextView status = statusChip(asset);
        top.addView(status);

        LinearLayout valueRow = row();
        LinearLayout.LayoutParams valueParams = lp(-1, -2);
        valueParams.topMargin = dp(8);
        item.addView(valueRow, valueParams);

        TextView amount = text(formatAmount(asset), 18, INK, Typeface.BOLD);
        amount.setSingleLine(true);
        valueRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView updated = text(shortUpdatedText(asset), 12, MUTED, Typeface.BOLD);
        updated.setGravity(Gravity.RIGHT);
        valueRow.addView(updated);

        List<String> details = assetBreakdownLines(asset);
        if (!details.isEmpty()) {
            TextView breakdown = text(joinLines(details), 12, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams breakdownParams = lp(-1, -2);
            breakdownParams.topMargin = dp(6);
            item.addView(breakdown, breakdownParams);
        }

        if (!asset.note.isEmpty()) {
            TextView note = text(asset.note, 12, MUTED, Typeface.NORMAL);
            note.setMaxLines(2);
            LinearLayout.LayoutParams noteParams = lp(-1, -2);
            noteParams.topMargin = dp(5);
            item.addView(note, noteParams);
        }

        LinearLayout actions = row();
        LinearLayout.LayoutParams actionsParams = lp(-1, dp(36));
        actionsParams.topMargin = dp(10);
        item.addView(actions, actionsParams);

        Button launch = secondaryButton("打开");
        launch.setTextSize(12);
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(36), 1));

        actions.addView(new SpaceView(this, dp(8), 1));

        Button markUpdated = secondaryButton("更新");
        markUpdated.setTextSize(12);
        markUpdated.setOnClickListener(view -> showAssetUpdateDialog(asset));
        actions.addView(markUpdated, new LinearLayout.LayoutParams(0, dp(36), 1));

        actions.addView(new SpaceView(this, dp(8), 1));

        Button edit = secondaryButton("编辑");
        edit.setTextSize(12);
        edit.setOnClickListener(view -> showEditDialog(asset));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(36), 1));
        return item;
    }

    private void showEditDialog(AssetRecord original) {
        assetDialogs.showEditDialog(original);
    }

    private void showCreatePreset(AssetPresets.Preset preset) {
        assetDialogs.showCreatePreset(preset);
    }

    private void openLinkedApp(AssetRecord asset) {
        assetDialogs.openLinkedApp(asset);
    }

    private void showMarkUpdatedDialog(AssetRecord asset) {
        assetDialogs.showAssetUpdateDialog(asset);
    }

    private void showAssetUpdateDialog(AssetRecord asset) {
        assetDialogs.showAssetUpdateDialog(asset);
    }

    private void sharePortfolioPage() {
        BackupActions.sharePortfolioPage(this, SHARE_PAGE_URL, assets, snapshots, settings);
    }

    private void startBackupExport() {
        BackupActions.startBackupExport(this, REQUEST_EXPORT_BACKUP);
    }

    private void startBackupImport() {
        BackupActions.startBackupImport(this, REQUEST_IMPORT_BACKUP);
    }

    private void writeBackup(Uri uri) {
        BackupActions.writeBackup(this, uri, store, assets, snapshots, updateEvents, settings);
    }

    private void readBackup(Uri uri) {
        AssetBackup backup = BackupActions.readBackup(this, uri, store);
        if (backup != null) {
            confirmImportBackup(backup);
        }
    }

    private void confirmImportBackup(AssetBackup backup) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("导入备份？")
                .setMessage(BackupActions.importBackupMessage(this, backup))
                .setNegativeButton("取消", null)
                .setPositiveButton("导入", (ignoredDialog, which) -> {
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
                .create();
        showStyledDialog(dialog);
    }

}
