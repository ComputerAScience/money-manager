package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class BottomNavBar extends LinearLayout {
    public static final String PAGE_OVERVIEW = "overview";
    public static final String PAGE_INVESTMENT = "investment";
    public static final String PAGE_TREND = "trend";
    public static final String PAGE_ASSETS = "assets";

    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int ACCENT_DARK = Color.rgb(9, 75, 67);
    private static final int ACCENT_SOFT = Color.rgb(232, 246, 242);
    private static final int PRESSED = Color.rgb(241, 245, 249);
    private static final String ICON_OVERVIEW = "overview";
    private static final String ICON_INVESTMENT = "investment";
    private static final String ICON_TREND = "trend";
    private static final String ICON_ASSETS = "assets";

    private final Activity activity;
    private final List<TabItem> tabs = new ArrayList<>();

    public BottomNavBar(Activity activity, TabSelectionListener listener) {
        super(activity);
        this.activity = activity;
        setOrientation(VERTICAL);
        setBackgroundColor(PANEL);
        setElevation(dp(10));
        setClipChildren(false);
        setClipToPadding(false);
        setPadding(0, 0, 0, dp(10));

        addView(dividerLine(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(1))
        ));

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(dp(12), dp(5), dp(12), dp(10));
        addTab(row, "总览", ICON_OVERVIEW, PAGE_OVERVIEW, listener);
        addTab(row, "投资", ICON_INVESTMENT, PAGE_INVESTMENT, listener);
        addTab(row, "数据", ICON_TREND, PAGE_TREND, listener);
        addTab(row, "资产", ICON_ASSETS, PAGE_ASSETS, listener);
        addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(76)
        ));
    }

    public void setBottomInset(int inset) {
        setPadding(0, 0, 0, Math.max(dp(10), inset + dp(10)));
    }

    public void setSelectedPage(String page) {
        for (TabItem tab : tabs) {
            boolean active = tab.page.equals(page);
            tab.container.setBackground(tabBackground(active));
            tab.indicator.setVisibility(active ? View.VISIBLE : View.INVISIBLE);
            tab.icon.setActive(active);
            tab.label.setTextColor(active ? ACCENT_DARK : MUTED);
            tab.label.setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void addTab(
            LinearLayout row,
            String label,
            String iconKind,
            String page,
            TabSelectionListener listener
    ) {
        LinearLayout tab = new LinearLayout(activity);
        tab.setOrientation(VERTICAL);
        tab.setGravity(Gravity.CENTER);
        tab.setClipChildren(false);
        tab.setClipToPadding(false);
        tab.setPadding(0, dp(4), 0, dp(6));
        tab.setContentDescription(label);
        tab.setOnClickListener(view -> listener.onSelected(page));

        View indicator = new View(activity);
        indicator.setBackground(roundedBackground(ACCENT, Color.TRANSPARENT, 999));
        indicator.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(dp(18), dp(3));
        indicatorParams.bottomMargin = dp(4);
        tab.addView(indicator, indicatorParams);

        TabIconView iconView = new TabIconView(activity, iconKind);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(28), dp(24));
        tab.addView(iconView, iconParams);

        TextView labelView = text(label, 12, MUTED, Typeface.NORMAL);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setIncludeFontPadding(true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-2, -2);
        labelParams.topMargin = dp(2);
        tab.addView(labelView, labelParams);

        tabs.add(new TabItem(page, tab, indicator, iconView, labelView));
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
        tabParams.leftMargin = dp(3);
        tabParams.rightMargin = dp(3);
        row.addView(tab, tabParams);
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

    private StateListDrawable tabBackground(boolean active) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},
                roundedBackground(active ? ACCENT_SOFT : PRESSED, Color.TRANSPARENT, 16));
        states.addState(new int[]{android.R.attr.state_focused},
                roundedBackground(active ? ACCENT_SOFT : PRESSED, Color.TRANSPARENT, 16));
        states.addState(new int[]{},
                roundedBackground(active ? ACCENT_SOFT : Color.TRANSPARENT, Color.TRANSPARENT, 16));
        return states;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    public interface TabSelectionListener {
        void onSelected(String page);
    }

    private static final class TabItem {
        final String page;
        final LinearLayout container;
        final View indicator;
        final TabIconView icon;
        final TextView label;

        TabItem(String page, LinearLayout container, View indicator, TabIconView icon, TextView label) {
            this.page = page;
            this.container = container;
            this.indicator = indicator;
            this.icon = icon;
            this.label = label;
        }
    }

    private final class TabIconView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();
        private final String kind;
        private boolean active;

        TabIconView(Activity activity, String kind) {
            super(activity);
            this.kind = kind;
        }

        void setActive(boolean active) {
            this.active = active;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(dp(active ? 2 : 1));
            paint.setColor(active ? ACCENT_DARK : INK);
            if (ICON_TREND.equals(kind)) {
                drawTrend(canvas);
            } else if (ICON_INVESTMENT.equals(kind)) {
                drawInvestment(canvas);
            } else if (ICON_ASSETS.equals(kind)) {
                drawAssets(canvas);
            } else {
                drawOverview(canvas);
            }
        }

        private void drawOverview(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            rect.set(w * 0.18f, h * 0.14f, w * 0.82f, h * 0.86f);
            canvas.drawRoundRect(rect, dp(8), dp(8), paint);
            canvas.drawLine(w * 0.32f, h * 0.36f, w * 0.68f, h * 0.36f, paint);
            canvas.drawLine(w * 0.32f, h * 0.56f, w * 0.54f, h * 0.56f, paint);
        }

        private void drawTrend(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            path.reset();
            path.moveTo(w * 0.14f, h * 0.74f);
            path.lineTo(w * 0.34f, h * 0.56f);
            path.lineTo(w * 0.52f, h * 0.64f);
            path.lineTo(w * 0.78f, h * 0.30f);
            canvas.drawPath(path, paint);
            canvas.drawLine(w * 0.14f, h * 0.84f, w * 0.84f, h * 0.84f, paint);
        }

        private void drawInvestment(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            rect.set(w * 0.16f, h * 0.18f, w * 0.84f, h * 0.82f);
            canvas.drawRoundRect(rect, dp(7), dp(7), paint);
            canvas.drawLine(w * 0.28f, h * 0.64f, w * 0.42f, h * 0.48f, paint);
            canvas.drawLine(w * 0.42f, h * 0.48f, w * 0.56f, h * 0.56f, paint);
            canvas.drawLine(w * 0.56f, h * 0.56f, w * 0.72f, h * 0.34f, paint);
        }

        private void drawAssets(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            rect.set(w * 0.16f, h * 0.24f, w * 0.78f, h * 0.72f);
            canvas.drawRoundRect(rect, dp(6), dp(6), paint);
            rect.set(w * 0.28f, h * 0.36f, w * 0.88f, h * 0.84f);
            canvas.drawRoundRect(rect, dp(6), dp(6), paint);
            canvas.drawLine(w * 0.48f, h * 0.60f, w * 0.68f, h * 0.60f, paint);
        }
    }
}
