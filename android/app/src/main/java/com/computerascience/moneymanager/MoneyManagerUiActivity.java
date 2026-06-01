package com.computerascience.moneymanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

abstract class MoneyManagerUiActivity extends Activity {
    protected static final int BG = Color.rgb(247, 248, 250);
    protected static final int PANEL = Color.WHITE;
    protected static final int INK = Color.rgb(31, 41, 55);
    protected static final int MUTED = Color.rgb(100, 116, 139);
    protected static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    protected static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    protected static final int ACCENT = Color.rgb(18, 107, 95);
    protected static final int ACCENT_DARK = Color.rgb(9, 75, 67);
    protected static final int SURFACE = Color.rgb(249, 251, 252);
    protected static final int SURFACE_ALT = Color.rgb(232, 246, 242);
    protected static final int BLUE = Color.rgb(51, 94, 170);
    protected static final int DANGER = Color.rgb(190, 67, 80);
    protected static final int AMBER = Color.rgb(166, 121, 24);

    protected LinearLayout card() {
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

    protected TextView sectionTitle(String title) {
        TextView text = text(title, 17, INK, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    protected LinearLayout metric(String label, TextView value) {
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

    protected EditText input(String label, String value, int inputType) {
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

    protected View fieldBox(String label, View field) {
        return fieldBox(label, field, dp(48));
    }

    protected View fieldBox(String label, View field, int fieldHeight) {
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

    protected TextView emptyText(String message) {
        TextView empty = text(message, 14, MUTED, Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(14), dp(22), dp(14), dp(22));
        empty.setBackground(cardBackground(ROW_SURFACE, PANEL_BORDER));
        return empty;
    }

    protected LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(lp(-1, -2));
        return row;
    }

    protected TextView label(String value) {
        TextView text = text(value.toUpperCase(Locale.ROOT), 12, MUTED, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    protected TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    protected Button primaryButton(String label) {
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

    protected Button iconButton(String label) {
        Button button = secondaryButton(label);
        button.setPadding(0, 0, 0, 0);
        button.setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        return button;
    }

    protected Button secondaryButton(String label) {
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

    protected CheckBox styledCheckBox(String label, boolean checked) {
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

    protected GradientDrawable cardBackground(int fill, int border) {
        return roundedBackground(fill, border, 8);
    }

    protected View dividerLine(int leftMargin) {
        View line = new View(this);
        line.setBackgroundColor(PANEL_BORDER);
        LinearLayout.LayoutParams params = lp(-1, dp(1));
        params.leftMargin = leftMargin;
        line.setLayoutParams(params);
        return line;
    }

    protected GradientDrawable headerBackground() {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(241, 247, 246), BG}
        );
        bg.setCornerRadius(0);
        return bg;
    }

    protected void styleSpinner(Spinner spinner) {
        spinner.setPadding(dp(12), 0, dp(12), 0);
        spinner.setBackground(cardBackground(PANEL, PANEL_BORDER));
        spinner.setMinimumHeight(dp(48));
    }

    protected void showStyledDialog(AlertDialog dialog) {
        dialog.show();
        styleDialog(dialog);
    }

    protected void styleDialog(AlertDialog dialog) {
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

    protected void styleDialogButton(Button button, int color) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setTextColor(color);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    }

    protected StateListDrawable buttonBackground(int fill, int pressedFill, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(pressedFill, border, 8));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(pressedFill, border, 8));
        states.addState(new int[]{}, roundedBackground(fill, border, 8));
        return states;
    }

    protected GradientDrawable roundedBackground(int fill, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), border);
        return bg;
    }

    protected LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    protected int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    protected int statusBarHeight() {
        return systemDimension("status_bar_height");
    }

    protected int navigationBarHeight() {
        return systemDimension("navigation_bar_height");
    }

    protected int systemDimension(String name) {
        int resourceId = getResources().getIdentifier(name, "dimen", "android");
        return resourceId > 0 ? getResources().getDimensionPixelSize(resourceId) : 0;
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
