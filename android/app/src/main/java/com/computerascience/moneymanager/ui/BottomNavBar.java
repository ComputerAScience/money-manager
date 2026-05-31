package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class BottomNavBar extends LinearLayout {
    public static final String PAGE_OVERVIEW = "overview";
    public static final String PAGE_TREND = "trend";
    public static final String PAGE_ASSETS = "assets";

    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int BLUE = Color.rgb(51, 94, 170);
    private static final int BLUE_SOFT = Color.rgb(230, 240, 255);

    private final Activity activity;
    private final List<TabItem> tabs = new ArrayList<>();

    public BottomNavBar(Activity activity, TabSelectionListener listener) {
        super(activity);
        this.activity = activity;
        setOrientation(VERTICAL);
        setBackgroundColor(PANEL);
        setElevation(dp(10));
        setPadding(0, 0, 0, dp(6));

        addView(dividerLine(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(1))
        ));

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(20), dp(4), dp(20), dp(6));
        addTab(row, "总览", "◎", PAGE_OVERVIEW, listener);
        addTab(row, "趋势", "⌁", PAGE_TREND, listener);
        addTab(row, "资产", "▦", PAGE_ASSETS, listener);
        addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
        ));
    }

    public void setBottomInset(int inset) {
        setPadding(0, 0, 0, inset + dp(6));
    }

    public void setSelectedPage(String page) {
        for (TabItem tab : tabs) {
            boolean active = tab.page.equals(page);
            tab.indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            tab.icon.setTextColor(active ? BLUE : INK);
            tab.icon.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
            tab.icon.setBackground(active
                    ? roundedBackground(BLUE_SOFT, Color.TRANSPARENT, 18)
                    : roundedBackground(Color.TRANSPARENT, Color.TRANSPARENT, 18));
            tab.label.setTextColor(active ? BLUE : MUTED);
            tab.label.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void addTab(
            LinearLayout row,
            String label,
            String icon,
            String page,
            TabSelectionListener listener
    ) {
        LinearLayout tab = new LinearLayout(activity);
        tab.setOrientation(VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setPadding(0, 0, 0, 0);
        tab.setOnClickListener(view -> listener.onSelected(page));

        View indicator = new View(activity);
        indicator.setBackground(roundedBackground(BLUE, Color.TRANSPARENT, 2));
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp(26), dp(3));
        indicatorParams.bottomMargin = dp(4);
        tab.addView(indicator, indicatorParams);

        TextView iconView = text(icon, 21, INK, Typeface.NORMAL);
        iconView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(30));
        tab.addView(iconView, iconParams);

        TextView labelView = text(label, 12, MUTED, Typeface.NORMAL);
        labelView.setGravity(Gravity.CENTER);
        labelView.setIncludeFontPadding(false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.topMargin = dp(2);
        tab.addView(labelView, labelParams);

        tabs.add(new TabItem(page, indicator, iconView, labelView));
        row.addView(tab, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private View dividerLine() {
        View line = new View(activity);
        line.setBackgroundColor(PANEL_BORDER);
        return line;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        return text;
    }

    private GradientDrawable roundedBackground(int fill, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), border);
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    public interface TabSelectionListener {
        void onSelected(String page);
    }

    private static final class TabItem {
        final String page;
        final View indicator;
        final TextView icon;
        final TextView label;

        TabItem(String page, View indicator, TextView icon, TextView label) {
            this.page = page;
            this.indicator = indicator;
            this.icon = icon;
            this.label = label;
        }
    }
}
