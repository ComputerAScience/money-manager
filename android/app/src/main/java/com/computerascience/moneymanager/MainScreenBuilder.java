package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.computerascience.moneymanager.ui.BottomNavBar;
import com.computerascience.moneymanager.ui.SectionDrawer;
import com.computerascience.moneymanager.ui.SectionProgressHandle;

final class MainScreenBuilder {
    private final MainActivity activity;
    private final SectionNavigationController navigation;
    private final OverviewPageRenderer overviewRenderer;
    private final InvestmentPageRenderer investmentRenderer;
    private final TrendPageRenderer trendRenderer;
    private final AssetsPageRenderer assetsRenderer;
    private final DataHealthRenderer dataHealthRenderer;
    private final TrendUpdatesRenderer updatesRenderer;

    MainScreenBuilder(
            MainActivity activity,
            SectionNavigationController navigation,
            OverviewPageRenderer overviewRenderer,
            InvestmentPageRenderer investmentRenderer,
            TrendPageRenderer trendRenderer,
            AssetsPageRenderer assetsRenderer,
            DataHealthRenderer dataHealthRenderer,
            TrendUpdatesRenderer updatesRenderer
    ) {
        this.activity = activity;
        this.navigation = navigation;
        this.overviewRenderer = overviewRenderer;
        this.investmentRenderer = investmentRenderer;
        this.trendRenderer = trendRenderer;
        this.assetsRenderer = assetsRenderer;
        this.dataHealthRenderer = dataHealthRenderer;
        this.updatesRenderer = updatesRenderer;
    }

    void build() {
        LinearLayout screen = new LinearLayout(activity);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(MoneyManagerActivity.BG);

        LinearLayout header = header();
        screen.addView(header, activity.lp(-1, -2));
        screen.addView(contentFrame(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        activity.bottomNavBar = new BottomNavBar(activity, navigation::selectPage);
        activity.bottomNavBar.setBottomInset(activity.navigationBarHeight());
        screen.addView(activity.bottomNavBar, activity.lp(-1, -2));
        activity.setContentView(screen);
        applySystemBarInsets(screen, header, activity.bottomNavBar);
        navigation.updatePageVisibility();
    }

    private LinearLayout header() {
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(activity.dp(18), activity.statusBarHeight() + activity.dp(14), activity.dp(18), activity.dp(12));
        header.setBackground(activity.headerBackground());

        LinearLayout brand = activity.row();
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark = new ImageView(activity);
        mark.setImageResource(R.drawable.ic_launcher_foreground);
        mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
        mark.setPadding(activity.dp(3), activity.dp(3), activity.dp(3), activity.dp(3));
        mark.setBackground(activity.roundedBackground(MoneyManagerActivity.ACCENT_DARK, MoneyManagerActivity.ACCENT, 8));
        brand.addView(mark, new LinearLayout.LayoutParams(activity.dp(34), activity.dp(34)));

        TextView eyebrow = activity.label("Money Manager · " + BuildConfig.CHANNEL_LABEL);
        LinearLayout.LayoutParams eyebrowParams = activity.lp(-2, -2);
        eyebrowParams.leftMargin = activity.dp(10);
        brand.addView(eyebrow, eyebrowParams);

        brand.addView(new View(activity), new LinearLayout.LayoutParams(0, 1, 1));

        Button settingsButton = activity.iconButton("⚙");
        settingsButton.setTextSize(19);
        settingsButton.setContentDescription("设置");
        settingsButton.setOnClickListener(view -> activity.showSettingsMenu());
        brand.addView(settingsButton, new LinearLayout.LayoutParams(activity.dp(42), activity.dp(38)));
        header.addView(brand);

        activity.pageTitle = activity.text("", 26, MoneyManagerActivity.INK, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = activity.lp(-1, -2);
        titleParams.topMargin = activity.dp(4);
        header.addView(activity.pageTitle, titleParams);

        activity.pageSubtitle = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = activity.lp(-1, -2);
        subtitleParams.topMargin = activity.dp(4);
        subtitleParams.bottomMargin = activity.dp(12);
        header.addView(activity.pageSubtitle, subtitleParams);
        return header;
    }

    private FrameLayout contentFrame() {
        activity.contentFrame = new FrameLayout(activity);
        activity.contentFrame.setClipChildren(false);
        activity.contentFrame.setClipToPadding(false);

        ScrollView scrollView = new ScrollView(activity);
        activity.mainScrollView = scrollView;
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(MoneyManagerActivity.BG);
        scrollView.setOnScrollChangeListener((view, scrollX, scrollY, oldScrollX, oldScrollY) -> navigation.updateProgress());
        scrollView.addView(contentRoot(), new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        activity.contentFrame.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        addProgressHandle();
        addSectionDrawer();
        return activity.contentFrame;
    }

    private LinearLayout contentRoot() {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(activity.dp(18), activity.dp(12), activity.dp(48), activity.dp(28));
        root.addView(overviewPage());
        root.addView(investmentPage());
        root.addView(trendPage());
        root.addView(assetsPage());
        return root;
    }

    private LinearLayout overviewPage() {
        activity.overviewPage = page();
        View overviewSummary = overviewRenderer.overviewCard();
        View allocation = overviewRenderer.allocationCard();
        View institution = overviewRenderer.institutionCard();
        View netWorthGoal = overviewRenderer.netWorthGoalCard();
        activity.overviewSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("总资产概览", overviewSummary),
                new SectionDrawer.Item("资产比例与目标", allocation),
                new SectionDrawer.Item("机构分布", institution),
                new SectionDrawer.Item("年度目标", netWorthGoal)
        };
        addAll(activity.overviewPage, overviewSummary, allocation, institution, netWorthGoal);
        return activity.overviewPage;
    }

    private LinearLayout investmentPage() {
        activity.investmentPage = page();
        View investmentSummary = investmentRenderer.summaryCard();
        View investmentReview = investmentRenderer.reviewCard();
        View investmentTrend = investmentRenderer.trendCard();
        View investmentDiagnostics = investmentRenderer.diagnosticsCard();
        View investmentFlow = investmentRenderer.flowCard();
        View investmentStructure = investmentRenderer.structureCard();
        View investmentInstitutions = investmentRenderer.institutionsCard();
        View investmentAccounts = investmentRenderer.accountsCard();
        activity.investmentSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("投资总览", investmentSummary),
                new SectionDrawer.Item("投资复盘", investmentReview),
                new SectionDrawer.Item("投资趋势", investmentTrend),
                new SectionDrawer.Item("投资诊断", investmentDiagnostics),
                new SectionDrawer.Item("投资变化", investmentFlow),
                new SectionDrawer.Item("投资结构", investmentStructure),
                new SectionDrawer.Item("投资机构", investmentInstitutions),
                new SectionDrawer.Item("投资资产", investmentAccounts)
        };
        addAll(activity.investmentPage, investmentSummary, investmentReview, investmentTrend, investmentDiagnostics,
                investmentFlow, investmentStructure, investmentInstitutions, investmentAccounts);
        return activity.investmentPage;
    }

    private LinearLayout trendPage() {
        activity.trendPage = page();
        View totalTrend = trendRenderer.trendCard();
        View monthlyReview = trendRenderer.monthlyReviewCard();
        View trendHealth = trendRenderer.healthCard();
        View targetProgress = trendRenderer.targetProgressCard();
        View distributionTrend = trendRenderer.distributionTrendCard();
        View flowAttribution = trendRenderer.flowAttributionCard();
        View assetTrend = trendRenderer.assetTrendCard();
        activity.trendSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("一年趋势", totalTrend),
                new SectionDrawer.Item("月度复盘", monthlyReview),
                new SectionDrawer.Item("趋势健康", trendHealth),
                new SectionDrawer.Item("目标追踪", targetProgress),
                new SectionDrawer.Item("分布变化", distributionTrend),
                new SectionDrawer.Item("变化归因", flowAttribution),
                new SectionDrawer.Item("单项资产", assetTrend)
        };
        addAll(activity.trendPage, totalTrend, monthlyReview, trendHealth, targetProgress,
                distributionTrend, flowAttribution, assetTrend);
        return activity.trendPage;
    }

    private LinearLayout assetsPage() {
        activity.assetsPage = page();
        View actionCenter = assetsRenderer.actionCenterCard();
        View assetManagement = assetsRenderer.assetManagementSection();
        View updatePlan = assetsRenderer.updatePlanCard();
        View dataHealth = dataHealthRenderer.card();
        View recentUpdates = updatesRenderer.card();
        activity.assetSections = new SectionDrawer.Item[]{
                new SectionDrawer.Item("行动中心", actionCenter),
                new SectionDrawer.Item("资产管理", assetManagement),
                new SectionDrawer.Item("核对计划", updatePlan),
                new SectionDrawer.Item("数据健康", dataHealth),
                new SectionDrawer.Item("更新流水", recentUpdates)
        };
        addAll(activity.assetsPage, actionCenter, assetManagement, updatePlan, dataHealth, recentUpdates);
        return activity.assetsPage;
    }

    private void addProgressHandle() {
        activity.sectionProgressHandle = new SectionProgressHandle(activity, view -> navigation.showMenu(), new SectionProgressHandle.ProgressDragListener() {
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
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(activity.dp(48), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.RIGHT);
        activity.contentFrame.addView(activity.sectionProgressHandle, progressParams);
    }

    private void addSectionDrawer() {
        activity.sectionDrawer = new SectionDrawer(activity, navigation::scrollToSection);
        activity.sectionDrawer.setVisibility(View.GONE);
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(activity.dp(216), -2, Gravity.RIGHT | Gravity.TOP);
        drawerParams.topMargin = activity.dp(12);
        drawerParams.rightMargin = activity.dp(10);
        activity.contentFrame.addView(activity.sectionDrawer, drawerParams);
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutParams(activity.lp(-1, -2));
        return page;
    }

    private void addAll(LinearLayout parent, View... children) {
        for (View child : children) {
            parent.addView(child);
        }
    }

    private void applySystemBarInsets(View screen, View header, BottomNavBar bottomNav) {
        screen.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset = Math.max(insets.getSystemWindowInsetTop(), activity.statusBarHeight());
            int bottomInset = Math.max(insets.getSystemWindowInsetBottom(), activity.navigationBarHeight());
            header.setPadding(activity.dp(18), topInset + activity.dp(14), activity.dp(18), activity.dp(12));
            bottomNav.setBottomInset(bottomInset);
            return insets;
        });
        screen.requestApplyInsets();
    }
}
