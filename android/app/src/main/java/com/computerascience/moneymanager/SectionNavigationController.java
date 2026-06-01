package com.computerascience.moneymanager;

import android.view.View;
import android.view.ViewParent;

import com.computerascience.moneymanager.ui.BottomNavBar;
import com.computerascience.moneymanager.ui.SectionDrawer;

final class SectionNavigationController {
    private static final String PAGE_OVERVIEW = BottomNavBar.PAGE_OVERVIEW;
    private static final String PAGE_INVESTMENT = BottomNavBar.PAGE_INVESTMENT;
    private static final String PAGE_TREND = BottomNavBar.PAGE_TREND;
    private static final String PAGE_ASSETS = BottomNavBar.PAGE_ASSETS;

    private final MainActivity activity;
    private String currentPage = PAGE_OVERVIEW;

    SectionNavigationController(MainActivity activity) {
        this.activity = activity;
    }

    void selectPage(String page) {
        if (currentPage.equals(page)) {
            return;
        }
        setPage(page);
        updatePageVisibility();
        if (activity.mainScrollView != null) {
            activity.mainScrollView.post(() -> activity.mainScrollView.smoothScrollTo(0, 0));
        }
    }

    void setPage(String page) {
        currentPage = page;
        hideSectionDrawer();
    }

    boolean isPage(String page) {
        return currentPage.equals(page);
    }

    boolean isSectionDrawerVisible() {
        return activity.sectionDrawer != null && activity.sectionDrawer.getVisibility() == View.VISIBLE;
    }

    void updatePageVisibility() {
        setPageVisible(activity.overviewPage, PAGE_OVERVIEW.equals(currentPage));
        setPageVisible(activity.investmentPage, PAGE_INVESTMENT.equals(currentPage));
        setPageVisible(activity.trendPage, PAGE_TREND.equals(currentPage));
        setPageVisible(activity.assetsPage, PAGE_ASSETS.equals(currentPage));

        if (activity.bottomNavBar != null) {
            activity.bottomNavBar.setSelectedPage(currentPage);
        }
        if (activity.pageTitle != null) {
            activity.pageTitle.setText(pageTitleText());
        }
        if (activity.pageSubtitle != null) {
            activity.pageSubtitle.setText(pageSubtitleText());
        }
        updateProgress();
    }

    void showMenu() {
        if (activity.sectionDrawer == null) {
            return;
        }
        if (activity.sectionDrawer.getVisibility() == View.VISIBLE) {
            hideSectionDrawer();
            return;
        }
        SectionDrawer.Item[] sections = currentSections();
        if (activity.sectionProgressHandle != null) {
            activity.sectionProgressHandle.setSectionLabels(sectionLabels(sections));
        }
        activity.sectionDrawer.setItems(pageTitleText(), sections);
        activity.sectionDrawer.setSelectedIndex(nearestSectionIndexToScroll(sections));
        activity.sectionDrawer.setVisibility(View.VISIBLE);
    }

    void hideSectionDrawer() {
        if (activity.sectionDrawer != null) {
            activity.sectionDrawer.setVisibility(View.GONE);
        }
    }

    void beginDrag() {
        SectionDrawer.Item[] sections = currentSections();
        if (sections.length == 0) {
            return;
        }
        hideSectionDrawer();
        int index = nearestSectionIndexToScroll(sections);
        activity.sectionDragIndex = index;
        if (activity.sectionProgressHandle != null) {
            activity.sectionProgressHandle.setSectionLabels(sectionLabels(sections));
            activity.sectionProgressHandle.setActiveSection(index);
        }
    }

    void updateDrag(float progress) {
        SectionDrawer.Item[] sections = currentSections();
        int index = sectionIndexForProgress(progress);
        if (index < 0 || index >= sections.length) {
            return;
        }
        if (activity.sectionProgressHandle != null) {
            activity.sectionProgressHandle.setActiveSection(index);
        }
        if (index != activity.sectionDragIndex) {
            activity.sectionDragIndex = index;
            scrollToSectionImmediate(sections[index].target);
        }
    }

    void finishDrag(float progress) {
        SectionDrawer.Item[] sections = currentSections();
        int index = sectionIndexForProgress(progress);
        if (index < 0 || index >= sections.length) {
            activity.sectionDragIndex = -1;
            return;
        }
        activity.sectionDragIndex = index;
        if (activity.sectionProgressHandle != null) {
            activity.sectionProgressHandle.setActiveSection(index);
        }
        scrollToSectionImmediate(sections[index].target);
        activity.sectionDragIndex = -1;
        updateProgress();
    }

    void cancelDrag() {
        activity.sectionDragIndex = -1;
        updateProgress();
    }

    void updateProgress() {
        if (activity.sectionDragIndex >= 0
                || activity.sectionProgressHandle == null
                || activity.mainScrollView == null
                || activity.mainScrollView.getChildCount() == 0) {
            return;
        }
        View content = activity.mainScrollView.getChildAt(0);
        int maxScroll = Math.max(0, content.getHeight() - activity.mainScrollView.getHeight());
        float progress = maxScroll == 0 ? 0f : (float) activity.mainScrollView.getScrollY() / maxScroll;
        activity.sectionProgressHandle.setProgress(progress);
    }

    void scrollToSection(View target) {
        if (activity.mainScrollView == null || target == null) {
            return;
        }
        target.post(() -> activity.mainScrollView.smoothScrollTo(0, Math.max(0, topInsideScroll(target) - activity.dp(8))));
    }

    private void scrollToSectionImmediate(View target) {
        if (activity.mainScrollView == null || target == null) {
            return;
        }
        activity.mainScrollView.scrollTo(0, Math.max(0, topInsideScroll(target) - activity.dp(8)));
    }

    private int topInsideScroll(View target) {
        int top = target.getTop();
        ViewParent parent = target.getParent();
        while (parent instanceof View && parent != activity.mainScrollView) {
            View parentView = (View) parent;
            top += parentView.getTop();
            parent = parentView.getParent();
        }
        return top;
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
            return activity.investmentSections == null ? new SectionDrawer.Item[0] : activity.investmentSections;
        }
        if (PAGE_TREND.equals(currentPage)) {
            return activity.trendSections == null ? new SectionDrawer.Item[0] : activity.trendSections;
        }
        if (PAGE_ASSETS.equals(currentPage)) {
            return activity.assetSections == null ? new SectionDrawer.Item[0] : activity.assetSections;
        }
        return activity.overviewSections == null ? new SectionDrawer.Item[0] : activity.overviewSections;
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
        if (sections.length == 0 || activity.mainScrollView == null) {
            return -1;
        }
        int anchor = activity.mainScrollView.getScrollY() + activity.dp(24);
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < sections.length; index += 1) {
            int top = Math.max(0, topInsideScroll(sections[index].target) - activity.dp(8));
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
}
