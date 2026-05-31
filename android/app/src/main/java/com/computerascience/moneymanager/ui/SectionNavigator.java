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

public final class SectionNavigator extends LinearLayout {
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int SURFACE_ALT = Color.rgb(232, 246, 242);
    private static final int ACCENT = Color.rgb(18, 107, 95);

    private final Activity activity;

    public SectionNavigator(Activity activity, SelectionListener listener, Item... items) {
        super(activity);
        this.activity = activity;
        setOrientation(VERTICAL);
        setPadding(dp(14), dp(12), dp(14), dp(12));
        setBackground(cardBackground(PANEL, PANEL_BORDER));
        setElevation(dp(1));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("本页导航", 12, MUTED, Typeface.BOLD);
        title.setIncludeFontPadding(false);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        TextView count = text(items.length + " 项", 12, MUTED, Typeface.NORMAL);
        count.setIncludeFontPadding(false);
        header.addView(count);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.bottomMargin = dp(10);
        addView(header, headerParams);

        for (int index = 0; index < items.length; index += 2) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            addView(row, new LinearLayout.LayoutParams(-1, dp(44)));
            row.addView(itemView(items[index], listener), new LinearLayout.LayoutParams(0, -1, 1));
            if (index + 1 < items.length) {
                SpaceView gap = new SpaceView(activity, dp(8), 1);
                row.addView(gap);
                row.addView(itemView(items[index + 1], listener), new LinearLayout.LayoutParams(0, -1, 1));
            } else {
                SpaceView gap = new SpaceView(activity, dp(8), 1);
                row.addView(gap);
                SpaceView filler = new SpaceView(activity, 0, 1);
                row.addView(filler, new LinearLayout.LayoutParams(0, -1, 1));
            }
            if (index + 2 < items.length) {
                SpaceView bottomGap = new SpaceView(activity, 1, dp(8));
                addView(bottomGap);
            }
        }
    }

    private View itemView(Item item, SelectionListener listener) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(6), 0);
        row.setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        row.setOnClickListener(view -> listener.onSelected(item.target));

        TextView icon = text(item.icon, 15, ACCENT, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        row.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView label = text(item.label, 13, INK, Typeface.BOLD);
        label.setSingleLine(true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(8);
        row.addView(label, labelParams);

        TextView arrow = text("›", 18, MUTED, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(14), dp(28)));
        return row;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        return text;
    }

    private StateListDrawable buttonBackground(int fill, int pressedFill, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(pressedFill, border, 8));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(pressedFill, border, 8));
        states.addState(new int[]{}, roundedBackground(fill, border, 8));
        return states;
    }

    private GradientDrawable cardBackground(int fill, int border) {
        return roundedBackground(fill, border, 8);
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

    public interface SelectionListener {
        void onSelected(View target);
    }

    public static final class Item {
        public final String label;
        public final String icon;
        public final View target;

        public Item(String label, String icon, View target) {
            this.label = label;
            this.icon = icon;
            this.target = target;
        }
    }

    private static final class SpaceView extends View {
        SpaceView(Activity activity, int width, int height) {
            super(activity);
            setLayoutParams(new LinearLayout.LayoutParams(width, height));
        }
    }
}
