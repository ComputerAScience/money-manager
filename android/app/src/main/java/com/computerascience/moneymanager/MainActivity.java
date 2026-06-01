package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

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
        new MainScreenBuilder(
                this,
                navigation,
                overviewRenderer,
                investmentRenderer,
                trendRenderer,
                assetsRenderer,
                dataHealthRenderer,
                updatesRenderer
        ).build();
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

    void showSettingsMenu() {
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
