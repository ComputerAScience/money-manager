package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

public final class SectionProgressHandle extends FrameLayout {
    private static final int PANEL = Color.WHITE;
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int ACCENT = Color.rgb(18, 107, 95);

    private final Activity activity;
    private final View thumb;

    public SectionProgressHandle(Activity activity, View.OnClickListener listener) {
        super(activity);
        this.activity = activity;
        setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        setElevation(dp(8));
        setClickable(true);
        setFocusable(true);
        setContentDescription("点击打开本页目录");
        setOnClickListener(listener);

        View rail = new View(activity);
        rail.setBackground(roundedBackground(PANEL_BORDER, Color.TRANSPARENT, 4));
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(dp(4), -1, Gravity.CENTER);
        railParams.topMargin = dp(10);
        railParams.bottomMargin = dp(10);
        addView(rail, railParams);

        thumb = new View(activity);
        thumb.setBackground(roundedBackground(ACCENT, Color.TRANSPARENT, 4));
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp(6), dp(32), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        thumbParams.topMargin = dp(10);
        addView(thumb, thumbParams);
    }

    public void setProgress(float value) {
        if (getHeight() == 0) {
            post(() -> setProgress(value));
            return;
        }
        float progress = Math.max(0f, Math.min(1f, value));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        int topPadding = dp(10);
        int available = Math.max(0, getHeight() - topPadding * 2 - params.height);
        params.topMargin = topPadding + Math.round(available * progress);
        thumb.setLayoutParams(params);
    }

    private StateListDrawable buttonBackground(int fill, int pressedFill, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(pressedFill, ACCENT, 12));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(pressedFill, ACCENT, 12));
        states.addState(new int[]{}, roundedBackground(fill, border, 12));
        return states;
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
}
