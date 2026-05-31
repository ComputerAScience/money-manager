package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class SideSectionRail extends LinearLayout {
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int ACCENT = Color.rgb(18, 107, 95);

    private final Activity activity;
    private final SectionNavigator.SelectionListener listener;

    public SideSectionRail(Activity activity, SectionNavigator.SelectionListener listener) {
        super(activity);
        this.activity = activity;
        this.listener = listener;
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setPadding(dp(5), dp(6), dp(5), dp(6));
        setBackground(roundedBackground(PANEL, PANEL_BORDER, 18));
        setElevation(dp(8));
    }

    public void setItems(SectionNavigator.Item... items) {
        removeAllViews();
        for (int index = 0; index < items.length; index += 1) {
            SectionNavigator.Item item = items[index];
            TextView button = text(String.valueOf(index + 1), 12, index == 0 ? ACCENT : INK, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setContentDescription(item.label);
            button.setBackground(buttonBackground(index == 0 ? Color.rgb(232, 246, 242) : ROW_SURFACE));
            button.setOnClickListener(view -> listener.onSelected(item.target));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(34));
            if (index > 0) {
                params.topMargin = dp(6);
            }
            addView(button, params);
        }
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setIncludeFontPadding(false);
        return text;
    }

    private StateListDrawable buttonBackground(int fill) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(Color.rgb(232, 246, 242), ACCENT, 12));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(Color.rgb(232, 246, 242), ACCENT, 12));
        states.addState(new int[]{}, roundedBackground(fill, PANEL_BORDER, 12));
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
