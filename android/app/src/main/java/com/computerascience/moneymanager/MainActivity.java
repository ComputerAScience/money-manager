package com.computerascience.moneymanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.computerascience.moneymanager.data.AssetStore;
import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.AssetPresets;
import com.computerascience.moneymanager.domain.ExchangeRateClient;
import com.computerascience.moneymanager.model.AssetBackup;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.InstitutionBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;
import com.computerascience.moneymanager.ui.AllocationChartView;
import com.computerascience.moneymanager.ui.AppPickerDialog;
import com.computerascience.moneymanager.ui.BottomNavBar;
import com.computerascience.moneymanager.ui.SectionDrawer;
import com.computerascience.moneymanager.ui.SectionProgressHandle;
import com.computerascience.moneymanager.ui.TrendChartView;
import com.computerascience.moneymanager.ui.UpdateDialog;

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
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_BACKUP = 4101;
    private static final int REQUEST_IMPORT_BACKUP = 4102;
    private static final String APK_DOWNLOAD_URL = BuildConfig.APK_DOWNLOAD_URL;
    private static final String UPDATE_INFO_URL = BuildConfig.UPDATE_INFO_URL;
    private static final String SHARE_PAGE_URL = "https://computerascience.github.io/money-manager/";
    private static final String ADD_CATEGORY_OPTION = "新增资产类型...";
    private static final String[] UPDATE_REASONS = {"余额核对", "入金", "出金", "买入卖出", "市场涨跌", "转账", "利息分红", "手续费税费", "负债变化", "仅更新时间", "其他"};
    private static final int BG = Color.rgb(247, 248, 250);
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int ACCENT_DARK = Color.rgb(9, 75, 67);
    private static final int SURFACE = Color.rgb(249, 251, 252);
    private static final int SURFACE_ALT = Color.rgb(232, 246, 242);
    private static final int BLUE = Color.rgb(51, 94, 170);
    private static final int DANGER = Color.rgb(190, 67, 80);
    private static final int AMBER = Color.rgb(166, 121, 24);
    private static final String PAGE_OVERVIEW = BottomNavBar.PAGE_OVERVIEW;
    private static final String PAGE_INVESTMENT = BottomNavBar.PAGE_INVESTMENT;
    private static final String PAGE_TREND = BottomNavBar.PAGE_TREND;
    private static final String PAGE_ASSETS = BottomNavBar.PAGE_ASSETS;

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
    private LinearLayout investmentPage;
    private LinearLayout trendPage;
    private LinearLayout assetsPage;
    private FrameLayout contentFrame;
    private BottomNavBar bottomNavBar;
    private SectionDrawer sectionDrawer;
    private SectionProgressHandle sectionProgressHandle;
    private SectionDrawer.Item[] overviewSections;
    private SectionDrawer.Item[] investmentSections;
    private SectionDrawer.Item[] trendSections;
    private SectionDrawer.Item[] assetSections;
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
    private TextView distributionTrendSummary;
    private LinearLayout distributionTrendList;
    private TextView insightSummary;
    private TextView dataHealthSummary;
    private LinearLayout dataHealthList;
    private TextView updatePlanSummary;
    private TextView recentUpdateSummary;
    private TextView investmentSummaryText;
    private LinearLayout investmentAccountList;
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
    private boolean suppressAssetTrendSelection;
    private String selectedTrendAssetId = "";
    private Spinner assetTrendSpinner;
    private TrendChartView assetTrendChart;
    private TextView assetTrendSummary;
    private LinearLayout assetTrendHistoryList;
    private List<AssetRecord> assetTrendOptions = new ArrayList<>();
    private final Set<String> collapsedAssetGroups = new HashSet<>();

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

        ScrollView scrollView = new ScrollView(this);
        mainScrollView = scrollView;
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);
        scrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> updateSectionProgress());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(42), dp(28));
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
        View actionCenter = actionCenterCard();
        overviewSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("总资产概览", overviewSummary),
                new SectionDrawer.Item("资产比例", allocation),
                new SectionDrawer.Item("目标比例", allocationTarget),
                new SectionDrawer.Item("机构分布", institution),
                new SectionDrawer.Item("年度目标", netWorthGoal),
                new SectionDrawer.Item("行动中心", actionCenter)
        };
        overviewPage.addView(overviewSummary);
        overviewPage.addView(allocation);
        overviewPage.addView(allocationTarget);
        overviewPage.addView(institution);
        overviewPage.addView(netWorthGoal);
        overviewPage.addView(actionCenter);
        root.addView(overviewPage);

        investmentPage = page();
        View investmentSummary = investmentSummaryCard();
        View investmentAccounts = investmentAccountsCard();
        investmentSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("投资总览", investmentSummary),
                new SectionDrawer.Item("投资资产", investmentAccounts)
        };
        investmentPage.addView(investmentSummary);
        investmentPage.addView(investmentAccounts);
        root.addView(investmentPage);

        trendPage = page();
        View totalTrend = trendCard();
        View distributionTrend = distributionTrendCard();
        View assetTrend = assetTrendCard();
        trendSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("一年趋势", totalTrend),
                new SectionDrawer.Item("分布变化", distributionTrend),
                new SectionDrawer.Item("单项资产", assetTrend)
        };
        trendPage.addView(totalTrend);
        trendPage.addView(distributionTrend);
        trendPage.addView(assetTrend);
        root.addView(trendPage);

        assetsPage = page();
        View assetManagement = assetManagementSection();
        View recentUpdates = recentUpdatesCard();
        assetSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("资产管理", assetManagement),
                new SectionDrawer.Item("最近更新", recentUpdates)
        };
        assetsPage.addView(assetManagement);
        assetsPage.addView(recentUpdates);
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
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(dp(26), dp(124), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        progressParams.rightMargin = dp(8);
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
            return "一年趋势";
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return "资产管理";
        }
        return "总览";
    }

    private String pageSubtitleText() {
        if (PAGE_INVESTMENT.equals(currentPage)) {
            return "由资产类型决定哪些条目进入投资页，可在设置里调整。";
        }
        if (PAGE_TREND.equals(currentPage)) {
            return "记录总资产快照，查看总额、分布和单项资产变化。";
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return "新增、筛选、绑定、核对资产，并回看最近更新。";
        }
        return "净资产、资产分布、年度目标和需要处理的提醒。";
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
        if (sectionDrawer == null) {
            return;
        }
        SectionDrawer.Item[] sections = currentSections();
        if (sections.length == 0) {
            return;
        }
        sectionDrawer.setItems(pageTitleText(), sections);
        sectionDrawer.setSelectedIndex(nearestSectionIndexToScroll(sections));
        sectionDrawer.setVisibility(View.VISIBLE);
    }

    private void updateSectionDrag(float progress) {
        if (sectionDrawer == null) {
            return;
        }
        if (sectionDrawer.getVisibility() != View.VISIBLE) {
            beginSectionDrag();
        }
        int index = sectionIndexForProgress(progress);
        if (index >= 0) {
            sectionDrawer.setSelectedIndex(index);
        }
    }

    private void finishSectionDrag(float progress) {
        SectionDrawer.Item[] sections = currentSections();
        int index = sectionIndexForProgress(progress);
        if (index < 0 || index >= sections.length) {
            hideSectionDrawer();
            return;
        }
        if (sectionDrawer != null) {
            sectionDrawer.setSelectedIndex(index);
        }
        scrollToSection(sections[index].target);
        if (sectionDrawer != null) {
            sectionDrawer.postDelayed(this::hideSectionDrawer, 260);
        }
    }

    private void cancelSectionDrag() {
        hideSectionDrawer();
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

    private void updateSectionProgress() {
        if (sectionProgressHandle == null || mainScrollView == null || mainScrollView.getChildCount() == 0) {
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

    private void render() {
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

        List<AssetSnapshot> trendSnapshots = snapshotsForBase(portfolio.baseCurrency);
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
        List<AssetRecord> visibleAssets = visibleAssets();
        assetResultSummary.setText("按机构分组显示 " + visibleAssets.size() + " / " + assets.size()
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

    private View actionCenterCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("行动中心"));

        insightSummary = text("", 15, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams insightParams = lp(-1, -2);
        insightParams.topMargin = dp(10);
        insightParams.bottomMargin = dp(12);
        card.addView(insightSummary, insightParams);

        TextView updateTitle = text("优先核对", 13, MUTED, Typeface.BOLD);
        card.addView(updateTitle, lp(-1, -2));

        updatePlanSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams updateSummaryParams = lp(-1, -2);
        updateSummaryParams.topMargin = dp(6);
        updateSummaryParams.bottomMargin = dp(6);
        card.addView(updatePlanSummary, updateSummaryParams);

        updatePlanList = new LinearLayout(this);
        updatePlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(updatePlanList, lp(-1, -2));

        TextView healthTitle = text("数据质量", 13, MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams healthTitleParams = lp(-1, -2);
        healthTitleParams.topMargin = dp(14);
        card.addView(healthTitle, healthTitleParams);

        dataHealthSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(6);
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
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        body.setPadding(pad, dp(8), pad, dp(6));

        ScrollView scrollBody = new ScrollView(this);
        scrollBody.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("设置")
                .setView(scrollBody)
                .setNegativeButton("关闭", null)
                .create();

        addSettingsSection(body, "偏好");
        body.addView(settingsActionRow(
                "¥",
                "基准币种与汇率",
                currencySettingsText(),
                ACCENT,
                view -> {
                    dialog.dismiss();
                    showCurrencySettingsDialog();
                }
        ));
        body.addView(settingsActionRow(
                "类",
                "资产类型",
                "新增自定义类型，并选择哪些类型进入投资页。",
                BLUE,
                view -> {
                    dialog.dismiss();
                    showCategorySettingsDialog();
                }
        ));

        addSettingsSection(body, "数据");
        body.addView(settingsActionRow(
                "享",
                "分享看板",
                "生成 GitHub Pages 只读链接，别人打开即可查看当前资产概览。",
                ACCENT,
                view -> {
                    dialog.dismiss();
                    sharePortfolioPage();
                }
        ));
        body.addView(settingsActionRow(
                "⇧",
                "导出备份",
                "保存资产、App 绑定、趋势快照、更新记录和设置。",
                BLUE,
                view -> {
                    dialog.dismiss();
                    startBackupExport();
                }
        ));
        body.addView(settingsActionRow(
                "⇩",
                "导入备份",
                "用备份文件覆盖当前本机数据。",
                AMBER,
                view -> {
                    dialog.dismiss();
                    startBackupImport();
                }
        ));

        addSettingsSection(body, "版本与更新");
        body.addView(settingsActionRow(
                "↻",
                "检查更新",
                "当前版本 " + appVersionLabel() + "，读取远端最新版本。",
                ACCENT,
                view -> {
                    dialog.dismiss();
                    showUpdateDialog();
                }
        ));
        body.addView(settingsActionRow(
                "↓",
                "直接下载 APK",
                "打开固定下载地址，适合网络检查失败时使用。",
                BLUE,
                view -> {
                    dialog.dismiss();
                    openApkDownload();
                }
        ));
        body.addView(settingsActionRow(
                "⛓",
                "复制下载链接",
                "把最新 APK 地址复制到剪贴板。",
                MUTED,
                view -> {
                    dialog.dismiss();
                    copyApkDownloadLink();
                }
        ));

        showStyledDialog(dialog);
    }

    private void showUpdateDialog() {
        UpdateDialog.show(this, appVersionLabel(), appVersionCode(), UPDATE_INFO_URL, APK_DOWNLOAD_URL);
    }

    private void addSettingsSection(LinearLayout body, String title) {
        TextView section = label(title);
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = body.getChildCount() == 0 ? 0 : dp(18);
        params.bottomMargin = dp(8);
        body.addView(section, params);
    }

    private View settingsActionRow(
            String icon,
            String title,
            String description,
            int iconColor,
            View.OnClickListener listener
    ) {
        LinearLayout row = row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        row.setOnClickListener(listener);
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);

        TextView iconView = text(icon, 18, iconColor, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        row.addView(iconView, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 15, INK, Typeface.BOLD);
        titleView.setIncludeFontPadding(false);
        copy.addView(titleView);

        TextView descriptionView = text(description, 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(4);
        copy.addView(descriptionView, descriptionParams);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.leftMargin = dp(12);
        row.addView(copy, copyParams);

        TextView arrow = text("›", 22, MUTED, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(22), dp(34)));
        return row;
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
        if (investmentSummaryText == null || investmentAccountList == null) {
            return;
        }
        List<AssetRecord> investments = investmentAssets();
        List<AssetInstitutionGroups.Group> groups = AssetInstitutionGroups.groupByInstitution(investments, settings);
        double investmentTotal = 0;
        for (AssetRecord asset : investments) {
            String currency = AssetMath.cleanCurrency(asset.currency);
            double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
            investmentTotal += AssetMath.assetGrossAmount(asset) * rate;
        }

        investmentSummaryText.setText("投资总额 " + formatMoney(investmentTotal, settings.baseCurrency)
                + " · " + groups.size() + " 个机构 · " + investments.size() + " 项资产");

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
            double currentValue = doubleValue(currentValues, category);
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

        showStyledDialog(dialog);
    }

    private void showCurrencySettingsDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        TextView description = text("会自动获取常用币种的最新公开汇率；网络不可用时仍可手动修改。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        Spinner baseCurrency = currencySpinner(draft.baseCurrency);
        form.addView(fieldBox("基准币种", baseCurrency));

        Button refreshButton = secondaryButton("获取实时汇率");
        LinearLayout.LayoutParams refreshParams = lp(-1, dp(44));
        refreshParams.bottomMargin = dp(12);
        form.addView(refreshButton, refreshParams);

        List<CurrencyRateField> rateFields = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            EditText rateInput = input("1 " + currency + " 等于多少基准币种", formatRate(draft.rateFor(currency)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rateFields.add(new CurrencyRateField(currency, rateInput));
            form.addView(rateInput);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("汇率设置")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            refreshButton.setOnClickListener(button -> {
                String base = String.valueOf(baseCurrency.getSelectedItem());
                refreshButton.setEnabled(false);
                refreshButton.setText("获取中...");
                fetchRealtimeRates(base, rates -> {
                    draft.baseCurrency = base;
                    draft.ratesToBase.putAll(rates);
                    draft.ensureBaseRate();
                    for (CurrencyRateField field : rateFields) {
                        field.input.setText(formatRate(draft.rateFor(field.currency)));
                    }
                    refreshButton.setEnabled(true);
                    refreshButton.setText("获取实时汇率");
                    toast("实时汇率已填入。");
                }, message -> {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("获取实时汇率");
                    toast(message);
                });
            });

            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                String base = PortfolioSettings.cleanCurrency(String.valueOf(baseCurrency.getSelectedItem()));
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

        showStyledDialog(dialog);
    }

    private void refreshExchangeRates(boolean showToast) {
        fetchRealtimeRates(settings.baseCurrency, rates -> {
            settings.ratesToBase.putAll(rates);
            settings.ensureBaseRate();
            store.saveSettings(settings);
            render();
            if (showToast) {
                toast("实时汇率已更新。");
            }
        }, message -> {
            if (showToast) {
                toast(message);
            }
        });
    }

    private void fetchRealtimeRates(
            String baseCurrency,
            RateSuccessHandler successHandler,
            RateFailureHandler failureHandler
    ) {
        String base = PortfolioSettings.cleanCurrency(baseCurrency);
        if (!isCommonCurrency(base)) {
            failureHandler.onFailure("实时汇率暂只支持 CNY / USD / HKD / EUR / JPY。");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Double> rates = ExchangeRateClient.fetchRatesToBase(base);
                runOnUiThread(() -> successHandler.onSuccess(rates));
            } catch (Exception error) {
                runOnUiThread(() -> failureHandler.onFailure("实时汇率获取失败，请稍后重试。"));
            }
        }).start();
    }

    private boolean isCommonCurrency(String currency) {
        String cleanCurrency = PortfolioSettings.cleanCurrency(currency);
        for (String option : PortfolioSettings.COMMON_CURRENCIES) {
            if (option.equals(cleanCurrency)) {
                return true;
            }
        }
        return false;
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
        for (String category : categoryOptionList("", false)) {
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

        showStyledDialog(dialog);
    }

    private void showCategorySettingsDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        TextView description = text("资产类型会出现在新增资产、资产比例和目标比例里；勾选“投资页”的类型会单独汇总到投资 Tab。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        List<CategorySettingField> categoryFields = new ArrayList<>();
        for (String category : categoryOptionList("", false)) {
            CheckBox investment = styledCheckBox("投资页", draft.isInvestmentCategory(category));
            categoryFields.add(new CategorySettingField(category, investment));
            form.addView(categorySettingRow(category, investment));
        }

        TextView addTitle = label("新增类型");
        LinearLayout.LayoutParams addTitleParams = lp(-1, -2);
        addTitleParams.topMargin = dp(10);
        addTitleParams.bottomMargin = dp(8);
        form.addView(addTitle, addTitleParams);

        EditText newCategory = input("例如：美股、港股、期权、保险", "", InputType.TYPE_CLASS_TEXT);
        form.addView(newCategory);
        CheckBox newCategoryInvestment = styledCheckBox("添加后显示在投资页", false);
        LinearLayout.LayoutParams newInvestmentParams = lp(-1, -2);
        newInvestmentParams.bottomMargin = dp(8);
        form.addView(newCategoryInvestment, newInvestmentParams);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("资产类型")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                draft.clearInvestmentCategories();
                for (CategorySettingField field : categoryFields) {
                    draft.setInvestmentCategory(field.category, field.investment.isChecked());
                }

                String addedCategory = clean(newCategory.getText().toString());
                if (!addedCategory.isEmpty()) {
                    if (ADD_CATEGORY_OPTION.equals(addedCategory)) {
                        toast("资产类型名称不能使用系统选项名称。");
                        return;
                    }
                    draft.addCustomCategory(addedCategory);
                    draft.setInvestmentCategory(addedCategory, newCategoryInvestment.isChecked());
                }

                settings = draft;
                store.saveSettings(settings);
                snapshots = store.recordSnapshot(assets, settings);
                render();
                toast("资产类型已保存。");
                dialog.dismiss();
            });
        });

        showStyledDialog(dialog);
    }

    private View categorySettingRow(String category, CheckBox investment) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(9), dp(8), dp(9));
        row.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.bottomMargin = dp(8);
        row.setLayoutParams(rowParams);

        TextView mark = text(categoryIcon(category), 15, AssetMath.colorForCategory(category), Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        row.addView(mark, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(text(category, 14, INK, Typeface.BOLD));
        TextView origin = text(categoryOriginText(category), 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams originParams = lp(-1, -2);
        originParams.topMargin = dp(3);
        copy.addView(origin, originParams);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.leftMargin = dp(10);
        row.addView(copy, copyParams);

        row.addView(investment, new LinearLayout.LayoutParams(dp(92), dp(40)));
        return row;
    }

    private CheckBox styledCheckBox(String label, boolean checked) {
        CheckBox checkbox = new CheckBox(this);
        checkbox.setText(label);
        checkbox.setTextSize(13);
        checkbox.setTextColor(INK);
        checkbox.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        checkbox.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        checkbox.setChecked(checked);
        checkbox.setGravity(Gravity.CENTER_VERTICAL);
        checkbox.setPadding(0, 0, 0, 0);
        return checkbox;
    }

    private String categoryOriginText(String category) {
        if (isDefaultAssetCategory(category)) {
            return "内置类型";
        }
        if (settings.customCategories.contains(category)) {
            return "自定义类型";
        }
        return "已在资产中使用";
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
                    assetMagnitude(right),
                    assetMagnitude(left)
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
            return AssetMath.assetLiabilityAmount(asset) > 0;
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

    private double assetMagnitude(AssetRecord asset) {
        return AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset);
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
        String institution = clean(asset.institution);
        String currency = AssetMath.cleanCurrency(asset.currency);
        return missingAssetAmount(asset)
                || invalidAssetAmount(asset)
                || institution.isEmpty()
                || institution.contains("待绑定")
                || (asset.packageName.isEmpty() && asset.launchUri.isEmpty())
                || !settings.hasRateFor(currency)
                || asset.lastUpdatedAt <= 0
                || isStale(asset);
    }

    private boolean missingAssetAmount(AssetRecord asset) {
        return clean(asset.amount).isEmpty();
    }

    private boolean invalidAssetAmount(AssetRecord asset) {
        String amount = clean(asset.amount);
        return !amount.isEmpty() && parseNumber(amount) == null;
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

    private void renderDistributionTrend(List<AssetSnapshot> trendSnapshots) {
        distributionTrendList.removeAllViews();
        List<AssetSnapshot> available = snapshotsWithCategoryValues(trendSnapshots);
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
        List<CategoryShift> shifts = categoryShifts(first, last);
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

    private List<AssetSnapshot> snapshotsWithCategoryValues(List<AssetSnapshot> trendSnapshots) {
        List<AssetSnapshot> available = new ArrayList<>();
        for (AssetSnapshot snapshot : trendSnapshots) {
            if (!snapshot.categoryValues.isEmpty()) {
                available.add(snapshot);
            }
        }
        return available;
    }

    private List<CategoryShift> categoryShifts(AssetSnapshot first, AssetSnapshot last) {
        Set<String> categories = new HashSet<>();
        categories.addAll(first.categoryValues.keySet());
        categories.addAll(last.categoryValues.keySet());

        double firstTotal = categoryTotal(first);
        double lastTotal = categoryTotal(last);
        List<CategoryShift> shifts = new ArrayList<>();
        for (String category : categories) {
            double firstValue = categoryValue(first, category);
            double lastValue = categoryValue(last, category);
            double firstPercent = firstTotal <= 0 ? 0 : firstValue / firstTotal * 100;
            double lastPercent = lastTotal <= 0 ? 0 : lastValue / lastTotal * 100;
            shifts.add(new CategoryShift(
                    category,
                    firstValue,
                    lastValue,
                    lastValue - firstValue,
                    firstPercent,
                    lastPercent,
                    lastPercent - firstPercent
            ));
        }
        Collections.sort(shifts, (left, right) -> Double.compare(
                Math.abs(right.delta) + Math.abs(right.percentDelta),
                Math.abs(left.delta) + Math.abs(left.percentDelta)
        ));
        return shifts;
    }

    private String distributionTrendSummaryText(
            AssetSnapshot first,
            AssetSnapshot last,
            List<CategoryShift> shifts,
            int count
    ) {
        CategoryShift biggest = shifts.get(0);
        if (settings.hideAmounts) {
            return "已记录 " + count + " 个带分布快照，范围 "
                    + first.dayKey + " 到 " + last.dayKey + "；金额已隐藏。";
        }
        return "从 " + first.dayKey + " 到 " + last.dayKey
                + "，变化最大的是 " + biggest.category + "："
                + formatSignedMoney(biggest.delta, last.baseCurrency)
                + "，占比 " + formatPoint(biggest.percentDelta) + "。";
    }

    private View categoryShiftRow(CategoryShift shift, String currency) {
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

    private double categoryTotal(AssetSnapshot snapshot) {
        double total = 0;
        for (double value : snapshot.categoryValues.values()) {
            total += value;
        }
        return total;
    }

    private double categoryValue(AssetSnapshot snapshot, String category) {
        Double value = snapshot.categoryValues.get(category);
        return value == null ? 0 : value;
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
        List<String> issues = dataHealthIssues(portfolio);
        if (issues.isEmpty()) {
            dataHealthSummary.setText("数据状态良好：金额、机构、App 绑定和汇率都已覆盖。");
            dataHealthList.addView(text("继续保持定期核对即可。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        List<AssetInstitutionGroups.Group> groups = dataHealthInstitutionGroups();
        dataHealthSummary.setText("发现 " + issues.size() + " 类数据维护问题，分布在 "
                + groups.size() + " 个机构，建议优先处理。");
        for (AssetInstitutionGroups.Group group : groups) {
            dataHealthList.addView(healthInstitutionRow(group));
        }
    }

    private List<String> dataHealthIssues(PortfolioSummary portfolio) {
        List<String> issues = new ArrayList<>();
        int missingAmount = 0;
        int invalidAmount = 0;
        int missingInstitution = 0;
        int missingBinding = 0;
        int missingRate = 0;

        for (AssetRecord asset : assets) {
            if (missingAssetAmount(asset)) {
                missingAmount += 1;
            } else if (invalidAssetAmount(asset)) {
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
        return issues;
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

    private List<AssetInstitutionGroups.Group> dataHealthInstitutionGroups() {
        List<AssetRecord> issueAssets = new ArrayList<>();
        for (AssetRecord asset : assets) {
            if (!assetHealthIssues(asset).isEmpty()) {
                issueAssets.add(asset);
            }
        }
        return AssetInstitutionGroups.groupByInstitution(issueAssets, settings);
    }

    private List<String> assetHealthIssues(AssetRecord asset) {
        List<String> issues = new ArrayList<>();
        if (missingAssetAmount(asset)) {
            issues.add("缺金额");
        } else if (invalidAssetAmount(asset)) {
            issues.add("金额无法识别");
        }
        String institution = clean(asset.institution);
        if (institution.isEmpty() || institution.contains("待绑定")) {
            issues.add("缺机构");
        }
        if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
            issues.add("未绑定 App");
        }
        String currency = AssetMath.cleanCurrency(asset.currency);
        if (!settings.hasRateFor(currency)) {
            issues.add("缺汇率");
        }
        if (asset.lastUpdatedAt <= 0) {
            issues.add("从未更新");
        } else if (isStale(asset)) {
            issues.add("待更新");
        }
        return issues;
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
            String detail = asset.name + " · " + joinInline(assetHealthIssues(asset));
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
        List<AssetRecord> planned = new ArrayList<>(assets);
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

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(PANEL, PANEL_BORDER));
        card.setElevation(dp(3));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(14);
        card.setLayoutParams(params);
        return card;
    }

    private TextView sectionTitle(String title) {
        TextView text = text(title, 17, INK, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private LinearLayout metric(String label, TextView value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
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

    private TextView categoryMark(AssetRecord asset) {
        int color = AssetMath.colorForCategory(asset.category);
        TextView mark = text(categoryIcon(asset.category), 18, color, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        return mark;
    }

    private String categoryIcon(String category) {
        if (AssetCategories.BANK_ACCOUNT.equals(category)) return "¥";
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(category)) return "投";
        if ("银行".equals(category) || AssetCategories.BANK_DEPOSIT.equals(category)) return "¥";
        if (AssetCategories.BANK_WEALTH.equals(category)) return "%";
        if ("券商".equals(category) || AssetCategories.BROKER_HOLDING.equals(category)) return "↗";
        if (AssetCategories.STOCK_HOLDING.equals(category)) return "股";
        if (AssetCategories.BROKER_CASH.equals(category)) return "$";
        if (AssetCategories.FUND.equals(category)) return "%";
        if (AssetCategories.CRYPTO.equals(category)) return "◇";
        if (AssetCategories.REAL_ESTATE.equals(category)) return "⌂";
        if (AssetCategories.DEBT.equals(category)) return "!";
        String cleaned = clean(category);
        return cleaned.isEmpty() ? "•" : cleaned.substring(0, 1);
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
        showEditDialog(original, null);
    }

    private void showCreatePreset(AssetPresets.Preset preset) {
        String currency = settings == null ? "CNY" : settings.baseCurrency;
        showEditDialog(null, AssetPresets.createRecord(preset, currency));
    }

    private void showEditDialog(AssetRecord original, AssetRecord preset) {
        boolean creating = original == null;
        AssetRecord draft = creating ? (preset == null ? new AssetRecord() : preset) : copyOf(original);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, dp(6));

        form.addView(assetEditHeader(draft, creating));

        EditText name = input("资产名称", draft.name, InputType.TYPE_CLASS_TEXT);
        form.addView(name);

        EditText institution = input("机构", draft.institution, InputType.TYPE_CLASS_TEXT);
        String[] selectedPackageName = {draft.packageName};
        String[] selectedAppName = {draft.appName};
        String[] selectedLaunchUri = {draft.launchUri};
        TextView selectedApp = text(appBindingText(selectedAppName[0], selectedPackageName[0], selectedLaunchUri[0]), 15, INK, Typeface.BOLD);
        selectedApp.setGravity(Gravity.CENTER_VERTICAL);
        selectedApp.setPadding(dp(14), 0, dp(14), 0);
        selectedApp.setBackground(cardBackground(SURFACE, PANEL_BORDER));
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

        form.addView(fieldBox("机构", institution));
        TextView institutionHelp = text("机构是主要管理粒度；一条资产属于某个机构，App 只是核对时打开的入口。", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams institutionHelpParams = lp(-1, -2);
        institutionHelpParams.bottomMargin = dp(10);
        form.addView(institutionHelp, institutionHelpParams);

        Spinner category = new Spinner(this);
        String[] categoryOptions = categoryOptions(draft.category);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categoryOptions));
        styleSpinner(category);
        category.setSelection(indexOf(categoryOptions, draft.category));
        form.addView(fieldBox("资产类型", category));

        EditText customCategory = input("新增资产类型", "", InputType.TYPE_CLASS_TEXT);
        form.addView(customCategory);
        CheckBox customCategoryInvestment = styledCheckBox("在投资 Tab 显示这个类型", false);
        LinearLayout.LayoutParams customInvestmentParams = lp(-1, -2);
        customInvestmentParams.bottomMargin = dp(10);
        form.addView(customCategoryInvestment, customInvestmentParams);
        updateCustomCategoryFields(String.valueOf(category.getSelectedItem()), customCategory, customCategoryInvestment);
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCustomCategoryFields(String.valueOf(category.getSelectedItem()), customCategory, customCategoryInvestment);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        LinearLayout amountRow = row();
        EditText amount = input("金额", draft.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner currency = currencySpinner(draft.currency);
        View amountBox = fieldBox("金额", amount);
        amountRow.addView(amountBox, new LinearLayout.LayoutParams(0, -2, 1));
        amountRow.addView(new SpaceView(this, dp(8), 1));
        amountRow.addView(fieldBox("币种", currency), new LinearLayout.LayoutParams(0, -2, 0.62f));
        form.addView(amountRow);

        EditText cadence = input("更新周期（天）", String.valueOf(draft.updateEveryDays), InputType.TYPE_CLASS_NUMBER);
        form.addView(cadence);

        EditText note = input("备注", draft.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

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

                String selectedCategory = String.valueOf(category.getSelectedItem());
                if (ADD_CATEGORY_OPTION.equals(selectedCategory)) {
                    selectedCategory = clean(customCategory.getText().toString());
                    if (selectedCategory.isEmpty()) {
                        toast("请输入新的资产类型。");
                        return;
                    }
                    if (ADD_CATEGORY_OPTION.equals(selectedCategory)) {
                        toast("资产类型名称不能使用系统选项名称。");
                        return;
                    }
                    settings.addCustomCategory(selectedCategory);
                    settings.setInvestmentCategory(selectedCategory, customCategoryInvestment.isChecked());
                    store.saveSettings(settings);
                }

                int everyDays = parsePositiveInt(cadence.getText().toString(), 7);
                draft.name = assetName;
                draft.category = selectedCategory;
                draft.institution = clean(institution.getText().toString());
                draft.bankDepositAmount = "";
                draft.bankWealthAmount = "";
                draft.bankDebtAmount = "";
                draft.investmentHoldingAmount = "";
                draft.investmentCashAmount = "";
                draft.investmentPositions = "";
                draft.amount = clean(amount.getText().toString());
                draft.currency = String.valueOf(currency.getSelectedItem());
                draft.updateEveryDays = everyDays;
                draft.appName = clean(selectedAppName[0]);
                draft.packageName = clean(selectedPackageName[0]);
                draft.launchUri = clean(selectedLaunchUri[0]);
                draft.note = clean(note.getText().toString());
                draft.lastUpdatedAt = System.currentTimeMillis();

                if (creating) {
                    assets.add(draft);
                } else {
                    replaceAsset(draft);
                }
                store.save(assets);
                snapshots = store.recordSnapshot(assets, settings);
                render();
                toast(creating ? "资产已新增并标记更新。" : "资产已保存并标记更新。");
                dialog.dismiss();
            });

            Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (delete != null) {
                delete.setTextColor(DANGER);
                delete.setOnClickListener(button -> {
                    AlertDialog confirmDialog = new AlertDialog.Builder(this)
                            .setTitle("删除资产")
                            .setMessage("确定删除「" + original.name + "」吗？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("删除", (confirm, which) -> {
                                removeAssetById(original.id);
                                store.save(assets);
                                snapshots = store.recordSnapshot(assets, settings);
                                render();
                                dialog.dismiss();
                            })
                            .create();
                    showStyledDialog(confirmDialog);
                });
            }
        });

        showStyledDialog(dialog);
    }

    private void updateCustomCategoryFields(String selectedCategory, View customCategory, View investmentToggle) {
        boolean adding = ADD_CATEGORY_OPTION.equals(selectedCategory);
        customCategory.setVisibility(adding ? View.VISIBLE : View.GONE);
        investmentToggle.setVisibility(adding ? View.VISIBLE : View.GONE);
    }

    private View assetEditHeader(AssetRecord asset, boolean creating) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        LinearLayout.LayoutParams panelParams = lp(-1, -2);
        panelParams.bottomMargin = dp(12);
        panel.setLayoutParams(panelParams);

        TextView mark = creating ? text("+", 20, ACCENT, Typeface.BOLD) : categoryMark(asset);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        markParams.rightMargin = dp(12);
        panel.addView(mark, markParams);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        String title = creating ? (asset.name.isEmpty() ? "新增资产" : asset.name) : asset.name;
        copy.addView(text(title, 15, INK, Typeface.BOLD));

        String description = creating
                ? (asset.category.isEmpty() ? "记录金额、周期和要打开的 App。" : asset.category + " · " + (asset.institution.isEmpty() ? "未填写机构" : asset.institution))
                : asset.category + " · " + (asset.institution.isEmpty() ? "未填写机构" : asset.institution);
        TextView detail = text(description, 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(4);
        copy.addView(detail, detailParams);
        panel.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        return panel;
    }

    private void showAppPicker(AppPickerDialog.SelectionHandler handler) {
        AppPickerDialog.show(this, "选择已安装 App", "搜索银行、券商、钱包或 App 名称。", handler);
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
        AppPickerDialog.show(this, "绑定并打开 App", "「" + asset.name + "」还没有绑定 App。先选择一次，以后就能一键打开。", selected -> {
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
        form.setPadding(pad, dp(6), pad, dp(6));

        form.addView(assetUpdateHeader(asset));

        String updateDescription = "核对「" + asset.name + "」后，录入这个机构资产的最新总金额。保存后会更新时间并记录今日总资产快照。";
        TextView description = text(updateDescription, 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(12);
        descriptionParams.bottomMargin = dp(12);
        form.addView(description, descriptionParams);

        EditText amount = input("最新金额", asset.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amount);

        Spinner reason = new Spinner(this);
        reason.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, UPDATE_REASONS));
        styleSpinner(reason);
        reason.setSelection(indexOf(UPDATE_REASONS, "余额核对"));
        form.addView(fieldBox("变化原因", reason));

        EditText note = input("备注（可选）", asset.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("更新资产")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("仅更新时间", null)
                .setPositiveButton("保存更新", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                asset.bankDepositAmount = "";
                asset.bankWealthAmount = "";
                asset.bankDebtAmount = "";
                asset.investmentHoldingAmount = "";
                asset.investmentCashAmount = "";
                asset.investmentPositions = "";
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

        showStyledDialog(dialog);
    }

    private View assetUpdateHeader(AssetRecord asset) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));

        LinearLayout top = row();
        TextView mark = categoryMark(asset);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(40), dp(40));
        markParams.rightMargin = dp(12);
        top.addView(mark, markParams);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(text(asset.name, 15, INK, Typeface.BOLD));

        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        TextView meta = text(asset.category + " · " + institution, 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(4);
        titleGroup.addView(meta, metaParams);
        top.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(statusChip(asset));
        panel.addView(top);

        LinearLayout detail = row();
        LinearLayout.LayoutParams detailParams = lp(-1, -2);
        detailParams.topMargin = dp(12);
        panel.addView(detail, detailParams);

        TextView amount = text(formatAmount(asset), 18, INK, Typeface.BOLD);
        detail.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView updated = text(lastUpdatedText(asset), 12, MUTED, Typeface.BOLD);
        updated.setGravity(Gravity.RIGHT);
        detail.addView(updated, new LinearLayout.LayoutParams(0, -2, 1));
        return panel;
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

    private void sharePortfolioPage() {
        try {
            String link = buildShareLink();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Money Manager 资产看板");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "我的资产看板：\n" + link);
            startActivity(Intent.createChooser(shareIntent, "分享资产看板"));
        } catch (ActivityNotFoundException error) {
            toast("没有找到可分享的应用。");
        } catch (Exception error) {
            toast("生成分享链接失败，请重试。");
        }
    }

    private String buildShareLink() throws IOException, JSONException {
        JSONObject payload = sharePayload();
        byte[] compressed = gzip(payload.toString().getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.encodeToString(
                compressed,
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
        );
        return SHARE_PAGE_URL + "#data=" + encoded;
    }

    private JSONObject sharePayload() throws JSONException {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);
        boolean includeAmounts = !settings.hideAmounts;

        JSONObject root = new JSONObject();
        root.put("schema", "money-manager-share-v1");
        root.put("generatedAt", System.currentTimeMillis());
        root.put("baseCurrency", portfolio.baseCurrency);
        root.put("includeAmounts", includeAmounts);
        root.put("assetCount", portfolio.assetCount);
        root.put("staleCount", portfolio.staleCount);

        JSONObject totals = new JSONObject();
        if (includeAmounts) {
            totals.put("netWorth", portfolio.netWorth);
            totals.put("grossAssets", portfolio.grossAssets);
            totals.put("liabilities", portfolio.liabilities);
        }
        root.put("totals", totals);
        root.put("categories", shareCategories(portfolio, includeAmounts));
        root.put("institutions", shareInstitutions(portfolio, includeAmounts));
        root.put("assets", shareAssets(includeAmounts));
        root.put("snapshots", shareSnapshots(includeAmounts));
        return root;
    }

    private JSONArray shareCategories(PortfolioSummary portfolio, boolean includeAmounts) throws JSONException {
        JSONArray array = new JSONArray();
        for (CategoryBreakdown category : portfolio.categories) {
            JSONObject item = new JSONObject();
            item.put("name", category.category);
            item.put("color", category.color);
            if (includeAmounts) {
                item.put("value", category.value);
            }
            array.put(item);
        }
        return array;
    }

    private JSONArray shareInstitutions(PortfolioSummary portfolio, boolean includeAmounts) throws JSONException {
        JSONArray array = new JSONArray();
        for (InstitutionBreakdown institution : portfolio.institutions) {
            JSONObject item = new JSONObject();
            item.put("name", institution.institution);
            item.put("assetCount", institution.assetCount);
            if (includeAmounts) {
                item.put("value", institution.value);
            }
            array.put(item);
        }
        return array;
    }

    private JSONArray shareAssets(boolean includeAmounts) throws JSONException {
        List<AssetRecord> sortedAssets = new ArrayList<>(assets);
        Collections.sort(sortedAssets, (left, right) -> Double.compare(
                shareAssetMagnitude(right),
                shareAssetMagnitude(left)
        ));

        JSONArray array = new JSONArray();
        for (AssetRecord asset : sortedAssets) {
            JSONObject item = new JSONObject();
            item.put("name", asset.name);
            item.put("category", asset.category);
            item.put("institution", AssetMath.cleanInstitution(asset.institution));
            item.put("currency", AssetMath.cleanCurrency(asset.currency));
            item.put("lastUpdatedAt", asset.lastUpdatedAt);
            item.put("stale", AssetMath.isStale(asset));
            if (includeAmounts) {
                String currency = AssetMath.cleanCurrency(asset.currency);
                double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
                double grossBase = AssetMath.assetGrossAmount(asset) * rate;
                double liabilityBase = AssetMath.assetLiabilityAmount(asset) * rate;
                item.put("grossBase", grossBase);
                item.put("liabilityBase", liabilityBase);
                item.put("netBase", grossBase - liabilityBase);
            }
            array.put(item);
        }
        return array;
    }

    private double shareAssetMagnitude(AssetRecord asset) {
        String currency = AssetMath.cleanCurrency(asset.currency);
        double rate = settings.hasRateFor(currency) ? settings.rateFor(currency) : 1.0;
        return (AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset)) * rate;
    }

    private JSONArray shareSnapshots(boolean includeAmounts) throws JSONException {
        JSONArray array = new JSONArray();
        if (!includeAmounts) {
            return array;
        }
        int start = Math.max(0, snapshots.size() - 24);
        for (int index = start; index < snapshots.size(); index += 1) {
            AssetSnapshot snapshot = snapshots.get(index);
            JSONObject item = new JSONObject();
            item.put("dayKey", snapshot.dayKey);
            item.put("timestamp", snapshot.timestamp);
            item.put("netWorth", snapshot.netWorth);
            item.put("grossAssets", snapshot.grossAssets);
            item.put("liabilities", snapshot.liabilities);
            array.put(item);
        }
        return array;
    }

    private byte[] gzip(byte[] source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(source);
        }
        return output.toByteArray();
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
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("导入备份？")
                .setMessage(importBackupMessage(backup))
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

    private String importBackupMessage(AssetBackup backup) {
        List<String> lines = new ArrayList<>();
        lines.add("将导入 " + backup.assets.size() + " 项资产和 "
                + backup.snapshots.size() + " 个趋势快照、"
                + backup.updateEvents.size() + " 条更新记录，以及汇率和目标设置。");
        lines.add("备份版本：" + backupVersionText(backup));
        if (backup.migratedLegacyAssets) {
            lines.add("检测到旧版银行 / 券商资产结构，导入时已自动转换为按机构管理的扁平资产。");
        }
        lines.add("导入会覆盖当前本机数据。");
        return joinLines(lines);
    }

    private String backupVersionText(AssetBackup backup) {
        if (backup.schemaVersion <= 0 && backup.version <= 0) {
            return "旧版或未知";
        }
        if (backup.schemaVersion > 0) {
            return "schema " + backup.schemaVersion;
        }
        return "version " + backup.version;
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

    private void removeAssetById(String assetId) {
        for (int index = assets.size() - 1; index >= 0; index -= 1) {
            if (assets.get(index).id.equals(assetId)) {
                assets.remove(index);
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

    private String shortUpdatedText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "从未更新";
        }
        long days = Math.max(0, (System.currentTimeMillis() - asset.lastUpdatedAt) / AssetMath.DAY_MS);
        return days == 0 ? "今天更新" : days + " 天前";
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

    private Drawable resolveAppIcon(String packageName) {
        String cleanPackageName = clean(packageName);
        if (cleanPackageName.isEmpty()) {
            return null;
        }
        try {
            PackageManager packageManager = getPackageManager();
            return packageManager.getApplicationIcon(cleanPackageName);
        } catch (Exception error) {
            return null;
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
            Double value = parseNumber(asset.amount);
            if (value == null) {
                return asset.amount + " " + asset.currency;
            }
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(value) + " " + asset.currency;
        } catch (NumberFormatException error) {
            return asset.amount + " " + asset.currency;
        }
    }

    private List<String> assetBreakdownLines(AssetRecord asset) {
        List<String> lines = new ArrayList<>();
        if (settings.hideAmounts) {
            return lines;
        }
        return lines;
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

    private String joinInline(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(line);
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

    private double doubleValue(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? 0.0 : value;
    }

    private int intValue(Map<String, Integer> values, String key) {
        Integer value = values.get(key);
        return value == null ? 0 : value;
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
        input.setBackground(cardBackground(PANEL, PANEL_BORDER));

        LinearLayout.LayoutParams params = lp(-1, dp(52));
        params.bottomMargin = dp(10);
        input.setLayoutParams(params);
        return input;
    }

    private View fieldBox(String label, View field) {
        return fieldBox(label, field, dp(48));
    }

    private View fieldBox(String label, View field, int fieldHeight) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView text = label(label);
        box.addView(text);
        box.addView(field, lp(-1, fieldHeight));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(10);
        box.setLayoutParams(params);
        return box;
    }

    private Spinner currencySpinner(String selectedCurrency) {
        String selected = PortfolioSettings.cleanCurrency(selectedCurrency);
        if (selected.isEmpty()) {
            selected = "CNY";
        }
        List<String> options = currencyOptions(selected);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        styleSpinner(spinner);
        int selectedIndex = options.indexOf(selected);
        spinner.setSelection(Math.max(0, selectedIndex));
        return spinner;
    }

    private List<String> currencyOptions(String selectedCurrency) {
        List<String> options = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            options.add(currency);
        }
        String selected = PortfolioSettings.cleanCurrency(selectedCurrency);
        if (!selected.isEmpty() && !options.contains(selected)) {
            options.add(selected);
        }
        return options;
    }

    private String[] categoryOptions(String selectedCategory) {
        List<String> options = categoryOptionList(selectedCategory, true);
        return options.toArray(new String[0]);
    }

    private List<String> categoryOptionList(String selectedCategory, boolean includeAddOption) {
        List<String> options = new ArrayList<>();
        for (String category : AssetCategories.ALL) {
            addCategoryOption(options, category);
        }
        if (settings != null) {
            for (String category : settings.customCategories) {
                addCategoryOption(options, category);
            }
        }
        for (AssetRecord asset : assets) {
            addCategoryOption(options, asset.category);
        }
        addCategoryOption(options, selectedCategory);
        if (includeAddOption) {
            options.add(ADD_CATEGORY_OPTION);
        }
        return options;
    }

    private void addCategoryOption(List<String> options, String category) {
        String cleaned = clean(category);
        if (!cleaned.isEmpty() && !ADD_CATEGORY_OPTION.equals(cleaned) && !options.contains(cleaned)) {
            options.add(cleaned);
        }
    }

    private boolean isDefaultAssetCategory(String category) {
        for (String option : AssetCategories.ALL) {
            if (option.equals(category)) {
                return true;
            }
        }
        return false;
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

    private Button iconButton(String label) {
        Button button = secondaryButton(label);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
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
        button.setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        return button;
    }

    private GradientDrawable cardBackground(int fill, int border) {
        return roundedBackground(fill, border, 8);
    }

    private View dividerLine(int leftMargin) {
        View line = new View(this);
        line.setBackgroundColor(PANEL_BORDER);
        LinearLayout.LayoutParams params = lp(-1, dp(1));
        params.leftMargin = leftMargin;
        line.setLayoutParams(params);
        return line;
    }

    private GradientDrawable headerBackground() {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(241, 247, 246), BG}
        );
        bg.setCornerRadius(0);
        return bg;
    }

    private void styleSpinner(Spinner spinner) {
        spinner.setPadding(dp(12), 0, dp(12), 0);
        spinner.setBackground(cardBackground(PANEL, PANEL_BORDER));
        spinner.setMinimumHeight(dp(48));
    }

    private void showStyledDialog(AlertDialog dialog) {
        dialog.show();
        styleDialog(dialog);
    }

    private void styleDialog(AlertDialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(roundedBackground(PANEL, PANEL_BORDER, 8));
            window.setDimAmount(0.42f);
        }
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            String label = String.valueOf(positive.getText());
            styleDialogButton(positive, label.contains("删除") ? DANGER : ACCENT);
        }
        styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), MUTED);
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutral != null) {
            String label = String.valueOf(neutral.getText());
            styleDialogButton(neutral, label.contains("删除") || label.contains("清空") ? DANGER : MUTED);
        }
    }

    private void styleDialogButton(Button button, int color) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setTextColor(color);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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

    private int statusBarHeight() {
        return systemDimension("status_bar_height");
    }

    private int navigationBarHeight() {
        return systemDimension("navigation_bar_height");
    }

    private int systemDimension(String name) {
        int resourceId = getResources().getIdentifier(name, "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
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

    private interface RateSuccessHandler {
        void onSuccess(Map<String, Double> rates);
    }

    private interface RateFailureHandler {
        void onFailure(String message);
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

    private static final class CategorySettingField {
        final String category;
        final CheckBox investment;

        CategorySettingField(String category, CheckBox investment) {
            this.category = category;
            this.investment = investment;
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

    private static final class CategoryShift {
        final String category;
        final double firstValue;
        final double lastValue;
        final double delta;
        final double firstPercent;
        final double lastPercent;
        final double percentDelta;

        CategoryShift(
                String category,
                double firstValue,
                double lastValue,
                double delta,
                double firstPercent,
                double lastPercent,
                double percentDelta
        ) {
            this.category = category;
            this.firstValue = firstValue;
            this.lastValue = lastValue;
            this.delta = delta;
            this.firstPercent = firstPercent;
            this.lastPercent = lastPercent;
            this.percentDelta = percentDelta;
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
