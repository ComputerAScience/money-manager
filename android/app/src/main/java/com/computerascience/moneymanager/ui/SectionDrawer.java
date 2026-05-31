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

import java.util.Locale;

public final class SectionDrawer extends LinearLayout {
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int SURFACE_ALT = Color.rgb(232, 246, 242);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int ACCENT_DARK = Color.rgb(9, 75, 67);

    private final Activity activity;
    private final SelectionListener listener;

    public SectionDrawer(Activity activity, SelectionListener listener) {
        super(activity);
        this.activity = activity;
        this.listener = listener;
        setOrientation(VERTICAL);
        setPadding(dp(12), dp(12), dp(12), dp(12));
        setBackground(roundedBackground(PANEL, PANEL_BORDER, 12));
        setElevation(dp(12));
    }

    public void setItems(String pageTitle, Item... items) {
        removeAllViews();

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(VERTICAL);
        titleGroup.addView(text("本页目录", 12, MUTED, Typeface.BOLD));
        TextView page = text(pageTitle == null || pageTitle.isEmpty() ? "当前页面" : pageTitle, 15, INK, Typeface.BOLD);
        LinearLayout.LayoutParams pageParams = new LinearLayout.LayoutParams(-1, -2);
        pageParams.topMargin = dp(3);
        titleGroup.addView(page, pageParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView close = text("×", 20, MUTED, Typeface.BOLD);
        close.setGravity(Gravity.CENTER);
        close.setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        close.setOnClickListener(view -> setVisibility(GONE));
        header.addView(close, new LinearLayout.LayoutParams(dp(34), dp(34)));
        addView(header);

        for (int index = 0; index < items.length; index += 1) {
            addView(itemView(items[index], index), rowParams(index));
        }
    }

    private View itemView(Item item, int index) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), 0, dp(8), 0);
        row.setBackground(buttonBackground(ROW_SURFACE, SURFACE_ALT, PANEL_BORDER));
        row.setOnClickListener(view -> {
            setVisibility(GONE);
            listener.onSelected(item.target);
        });

        TextView number = text(String.format(Locale.getDefault(), "%02d", index + 1), 11, ACCENT_DARK, Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        number.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        row.addView(number, new LinearLayout.LayoutParams(dp(32), dp(28)));

        TextView label = text(item.label, 14, INK, Typeface.BOLD);
        label.setSingleLine(true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.leftMargin = dp(9);
        row.addView(label, labelParams);

        TextView arrow = text("›", 18, MUTED, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(14), dp(28)));
        return row;
    }

    private LinearLayout.LayoutParams rowParams(int index) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(44));
        params.topMargin = index == 0 ? dp(12) : dp(8);
        return params;
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

    private StateListDrawable buttonBackground(int fill, int pressedFill, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(pressedFill, ACCENT, 8));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(pressedFill, ACCENT, 8));
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

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    public interface SelectionListener {
        void onSelected(View target);
    }

    public static final class Item {
        public final String label;
        public final View target;

        public Item(String label, View target) {
            this.label = label;
            this.target = target;
        }
    }
}
