package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.computerascience.moneymanager.data.AssetStore;
import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.AssetPresets;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.model.AssetBackup;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.BottomNavBar;
import com.computerascience.moneymanager.ui.SectionDrawer;
import com.computerascience.moneymanager.ui.SectionProgressHandle;
import com.computerascience.moneymanager.ui.UpdateDialog;

import java.util.List;

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

    private final SectionNavigationController navigation = new SectionNavigationController(this);
    private final AssetDialogs assetDialogs = new AssetDialogs(this, UPDATE_REASONS);
    private final SettingsDialogs settingsDialogs = new SettingsDialogs(this);
    private final InvestmentPageRenderer investmentRenderer = new InvestmentPageRenderer(this);
    private final TrendPageRenderer trendRenderer = new TrendPageRenderer(this);
    private final OverviewPageRenderer overviewRenderer = new OverviewPageRenderer(this, trendRenderer);
    private final AssetsPageRenderer assetsRenderer = new AssetsPageRenderer(this);
    private final DataHealthRenderer dataHealthRenderer = new DataHealthRenderer(this);
    private final TrendUpdatesRenderer updatesRenderer = new TrendUpdatesRenderer(this);

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
        scrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> navigation.updateProgress());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(48), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        overviewPage = page();
        View overviewSummary = overviewRenderer.overviewCard();
        View allocation = overviewRenderer.allocationCard();
        View allocationTarget = overviewRenderer.allocationTargetCard();
        View institution = overviewRenderer.institutionCard();
        View netWorthGoal = overviewRenderer.netWorthGoalCard();
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
        View investmentSummary = investmentRenderer.summaryCard();
        View investmentReview = investmentRenderer.reviewCard();
        View investmentDiagnostics = investmentRenderer.diagnosticsCard();
        View investmentFlow = investmentRenderer.flowCard();
        View investmentStructure = investmentRenderer.structureCard();
        View investmentInstitutions = investmentRenderer.institutionsCard();
        View investmentPlan = investmentRenderer.planCard();
        View investmentAccounts = investmentRenderer.accountsCard();
        investmentSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("投资总览", investmentSummary),
                new SectionDrawer.Item("投资复盘", investmentReview),
                new SectionDrawer.Item("投资诊断", investmentDiagnostics),
                new SectionDrawer.Item("投资变化", investmentFlow),
                new SectionDrawer.Item("投资结构", investmentStructure),
                new SectionDrawer.Item("投资机构", investmentInstitutions),
                new SectionDrawer.Item("投资待核对", investmentPlan),
                new SectionDrawer.Item("投资资产", investmentAccounts)
        };
        investmentPage.addView(investmentSummary);
        investmentPage.addView(investmentReview);
        investmentPage.addView(investmentDiagnostics);
        investmentPage.addView(investmentFlow);
        investmentPage.addView(investmentStructure);
        investmentPage.addView(investmentInstitutions);
        investmentPage.addView(investmentPlan);
        investmentPage.addView(investmentAccounts);
        root.addView(investmentPage);

        trendPage = page();
        View totalTrend = trendRenderer.trendCard();
        View monthlyReview = trendRenderer.monthlyReviewCard();
        View targetProgress = trendRenderer.targetProgressCard();
        View distributionTrend = trendRenderer.distributionTrendCard();
        View flowAttribution = trendRenderer.flowAttributionCard();
        View assetTrend = trendRenderer.assetTrendCard();
        trendSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("一年趋势", totalTrend),
                new SectionDrawer.Item("月度复盘", monthlyReview),
                new SectionDrawer.Item("目标追踪", targetProgress),
                new SectionDrawer.Item("分布变化", distributionTrend),
                new SectionDrawer.Item("变化归因", flowAttribution),
                new SectionDrawer.Item("单项资产", assetTrend)
        };
        trendPage.addView(totalTrend);
        trendPage.addView(monthlyReview);
        trendPage.addView(targetProgress);
        trendPage.addView(distributionTrend);
        trendPage.addView(flowAttribution);
        trendPage.addView(assetTrend);
        root.addView(trendPage);

        assetsPage = page();
        View actionCenter = assetsRenderer.actionCenterCard();
        View assetManagement = assetsRenderer.assetManagementSection();
        View updatePlan = assetsRenderer.updatePlanCard();
        View dataHealth = dataHealthRenderer.card();
        View recentUpdates = updatesRenderer.card();
        assetSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("行动中心", actionCenter),
                new SectionDrawer.Item("资产管理", assetManagement),
                new SectionDrawer.Item("核对计划", updatePlan),
                new SectionDrawer.Item("数据健康", dataHealth),
                new SectionDrawer.Item("更新流水", recentUpdates)
        };
        assetsPage.addView(actionCenter);
        assetsPage.addView(assetManagement);
        assetsPage.addView(updatePlan);
        assetsPage.addView(dataHealth);
        assetsPage.addView(recentUpdates);
        root.addView(assetsPage);

        contentFrame.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        sectionProgressHandle = new SectionProgressHandle(this, view -> navigation.showMenu(), new SectionProgressHandle.ProgressDragListener() {
            @Override
            public void onDragStart() {
                navigation.beginDrag();
            }

            @Override
            public void onProgress(float progress) {
                navigation.updateDrag(progress);
            }

            @Override
            public void onDragEnd(float progress) {
                navigation.finishDrag(progress);
            }

            @Override
            public void onDragCancel() {
                navigation.cancelDrag();
            }
        });
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        contentFrame.addView(sectionProgressHandle, progressParams);

        sectionDrawer = new SectionDrawer(this, navigation::scrollToSection);
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

        bottomNavBar = new BottomNavBar(this, navigation::selectPage);
        bottomNavBar.setBottomInset(navigationBarHeight());
        screen.addView(bottomNavBar, lp(-1, -2));
        setContentView(screen);
        applySystemBarInsets(screen, header, bottomNavBar);
        navigation.updatePageVisibility();
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
        if (navigation.isSectionDrawerVisible()) {
            navigation.hideSectionDrawer();
            return true;
        }
        if (navigation.isPage(PAGE_ASSETS)) {
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

        if (!navigation.isPage(PAGE_OVERVIEW)) {
            navigation.selectPage(PAGE_OVERVIEW);
            return true;
        }
        return false;
    }

    void render() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        List<AssetSnapshot> trendSnapshots = TrendAnalytics.snapshotsForBase(snapshots, portfolio.baseCurrency);

        overviewRenderer.render(portfolio, trendSnapshots);
        investmentRenderer.render();

        trendRenderer.render(portfolio, trendSnapshots);
        assetsRenderer.render(portfolio);
        dataHealthRenderer.render(portfolio);
        updatesRenderer.render();

        navigation.updatePageVisibility();
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

    void showAssetManagement(String filterMode) {
        managementExpanded = true;
        assetFilterMode = filterMode;
        assetSearchQuery = "";
        navigation.setPage(PAGE_ASSETS);
        render();
        scrollToAssetManagement();
    }

    private void scrollToAssetManagement() {
        if (mainScrollView == null || assetManagementCard == null) {
            return;
        }
        navigation.scrollToSection(assetManagementCard);
    }

    private String currencySettingsText() {
        return overviewRenderer.currencySettingsText();
    }

    void showNetWorthGoalDialog() {
        settingsDialogs.showNetWorthGoalDialog();
    }

    void showCurrencySettingsDialog() {
        settingsDialogs.showCurrencySettingsDialog();
    }

    private void refreshExchangeRates(boolean showToast) {
        settingsDialogs.refreshExchangeRates(showToast);
    }

    void showAllocationTargetDialog() {
        settingsDialogs.showAllocationTargetDialog();
    }

    void showCategorySettingsDialog() {
        settingsDialogs.showCategorySettingsDialog();
    }

    View assetInstitutionGroupHeader(AssetInstitutionGroups.Group group, String baseCurrency) {
        return assetsRenderer.assetInstitutionGroupHeader(group, baseCurrency);
    }

    View institutionGroupIcon(AssetInstitutionGroups.Group group) {
        return assetsRenderer.institutionGroupIcon(group);
    }

    List<AssetRecord> sortedPlannedAssets(List<AssetRecord> source) {
        return assetsRenderer.sortedPlannedAssets(source);
    }

    String updatePlanSummaryText(int urgentCount, int soonCount) {
        return assetsRenderer.updatePlanSummaryText(urgentCount, soonCount);
    }

    View updatePlanRow(AssetRecord asset) {
        return assetsRenderer.updatePlanRow(asset);
    }

    View assetCompactRow(AssetRecord asset) {
        return assetsRenderer.assetCompactRow(asset);
    }

    void showEditDialog(AssetRecord original) {
        assetDialogs.showEditDialog(original);
    }

    void showCreatePreset(AssetPresets.Preset preset) {
        assetDialogs.showCreatePreset(preset);
    }

    void openLinkedApp(AssetRecord asset) {
        assetDialogs.openLinkedApp(asset);
    }

    private void showMarkUpdatedDialog(AssetRecord asset) {
        assetDialogs.showAssetUpdateDialog(asset);
    }

    void showAssetUpdateDialog(AssetRecord asset) {
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
