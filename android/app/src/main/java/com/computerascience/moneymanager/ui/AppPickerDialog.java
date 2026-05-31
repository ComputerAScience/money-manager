package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppPickerDialog {
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int SURFACE_ALT = Color.rgb(232, 246, 242);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int BLUE = Color.rgb(51, 94, 170);
    private static final int DANGER = Color.rgb(190, 67, 80);

    private final Activity activity;

    private AppPickerDialog(Activity activity) {
        this.activity = activity;
    }

    public static void show(Activity activity, String title, String helperText, SelectionHandler handler) {
        new AppPickerDialog(activity).showPicker(title, helperText, handler);
    }

    private void showPicker(String title, String helperText, SelectionHandler handler) {
        List<LaunchableApp> apps = getLaunchableApps();
        if (apps.isEmpty()) {
            Toast.makeText(activity, "没有找到可启动的 App。", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        content.setPadding(pad, dp(6), pad, dp(4));

        TextView helper = text(helperText, 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams helperParams = lp(-1, -2);
        helperParams.bottomMargin = dp(8);
        content.addView(helper, helperParams);

        TextView countLabel = text(appPickerCountText(apps.size(), apps.size()), 12, MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams countParams = lp(-1, -2);
        countParams.bottomMargin = dp(10);
        content.addView(countLabel, countParams);

        EditText search = input("搜索 App 名称", "", InputType.TYPE_CLASS_TEXT);
        content.addView(search);

        FrameLayout listFrame = new FrameLayout(activity);
        listFrame.setBackground(cardBackground(PANEL, PANEL_BORDER));
        listFrame.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams listParams = lp(-1, appPickerListHeight());
        listParams.bottomMargin = dp(14);
        listFrame.setLayoutParams(listParams);

        ListView list = new ListView(activity);
        list.setDivider(new ColorDrawable(PANEL_BORDER));
        list.setDividerHeight(dp(1));
        list.setPadding(0, 0, 0, 0);
        list.setCacheColorHint(Color.TRANSPARENT);
        list.setSelector(new ColorDrawable(Color.TRANSPARENT));
        LaunchableAppAdapter adapter = new LaunchableAppAdapter(apps);
        list.setAdapter(adapter);
        listFrame.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        TextView empty = text("没有匹配的 App", 14, MUTED, Typeface.BOLD);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        listFrame.addView(empty, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        list.setEmptyView(empty);
        content.addView(listFrame);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(content)
                .setNegativeButton("取消", null)
                .create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            LaunchableApp selected = adapter.getItem(position);
            handler.onSelected(selected);
            dialog.dismiss();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                int matches = adapter.filter(text == null ? "" : text.toString());
                updateAppPickerCount(apps.size(), matches, countLabel);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        showStyledDialog(dialog);
    }

    private List<LaunchableApp> getLaunchableApps() {
        PackageManager packageManager = activity.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolvedApps = packageManager.queryIntentActivities(launcherIntent, 0);
        List<LaunchableApp> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo resolvedApp : resolvedApps) {
            String packageName = resolvedApp.activityInfo == null ? "" : resolvedApp.activityInfo.packageName;
            if (packageName == null
                    || packageName.isEmpty()
                    || packageName.equals(activity.getPackageName())
                    || seenPackages.contains(packageName)) {
                continue;
            }
            seenPackages.add(packageName);
            CharSequence label = resolvedApp.loadLabel(packageManager);
            String appLabel = label == null ? packageName : label.toString();
            Drawable icon = resolvedApp.loadIcon(packageManager);
            apps.add(new LaunchableApp(appLabel, packageName, icon));
        }
        Collections.sort(apps, (left, right) -> left.label.compareToIgnoreCase(right.label));
        return apps;
    }

    private String appPickerCountText(int total, int matches) {
        if (matches == total) {
            return total + " 个可绑定 App";
        }
        return "匹配 " + matches + " / " + total + " 个 App";
    }

    private void updateAppPickerCount(int total, int matches, TextView count) {
        count.setText(appPickerCountText(total, matches));
        count.setTextColor(matches == 0 ? DANGER : MUTED);
    }

    private int appPickerListHeight() {
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        return Math.min(dp(320), Math.max(dp(210), screenHeight - dp(430)));
    }

    private EditText input(String label, String value, int inputType) {
        EditText input = new EditText(activity);
        input.setHint(label);
        input.setText(value);
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setTextSize(15);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(cardBackground(PANEL, PANEL_BORDER));
        LinearLayout.LayoutParams params = lp(-1, dp(48));
        params.bottomMargin = dp(12);
        input.setLayoutParams(params);
        return input;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    private void showStyledDialog(AlertDialog dialog) {
        dialog.setOnShowListener(view -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(roundedBackground(PANEL, PANEL_BORDER, 8));
                window.setDimAmount(0.42f);
            }
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive != null) {
                positive.setAllCaps(false);
                positive.setTextColor(ACCENT);
                positive.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negative != null) {
                negative.setAllCaps(false);
                negative.setTextColor(MUTED);
                negative.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
        });
        dialog.show();
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

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public interface SelectionHandler {
        void onSelected(LaunchableApp app);
    }

    private final class LaunchableAppAdapter extends BaseAdapter {
        private final List<LaunchableApp> source;
        private final List<LaunchableApp> filtered = new ArrayList<>();

        LaunchableAppAdapter(List<LaunchableApp> apps) {
            source = apps;
            filtered.addAll(apps);
        }

        int filter(String query) {
            String normalized = clean(query).toLowerCase(Locale.ROOT);
            filtered.clear();
            for (LaunchableApp app : source) {
                if (normalized.isEmpty()
                        || app.label.toLowerCase(Locale.ROOT).contains(normalized)
                        || app.packageName.toLowerCase(Locale.ROOT).contains(normalized)) {
                    filtered.add(app);
                }
            }
            notifyDataSetChanged();
            return filtered.size();
        }

        @Override
        public int getCount() {
            return filtered.size();
        }

        @Override
        public LaunchableApp getItem(int position) {
            return filtered.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LaunchableApp app = getItem(position);
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(8), dp(12), dp(8));
            row.setMinimumHeight(dp(58));
            row.setBackground(buttonBackground(PANEL, ROW_SURFACE, Color.TRANSPARENT));
            row.setLayoutParams(new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(58)
            ));

            ImageView icon = new ImageView(activity);
            icon.setImageDrawable(app.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setBackground(roundedBackground(SURFACE_ALT, PANEL_BORDER, 8));
            icon.setPadding(dp(6), dp(6), dp(6), dp(6));
            row.addView(icon, new LinearLayout.LayoutParams(dp(38), dp(38)));

            LinearLayout texts = new LinearLayout(activity);
            texts.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
            textParams.leftMargin = dp(12);
            row.addView(texts, textParams);

            TextView label = text(app.label, 15, INK, Typeface.BOLD);
            label.setSingleLine(true);
            texts.addView(label);

            TextView chevron = text("›", 22, BLUE, Typeface.BOLD);
            chevron.setGravity(Gravity.CENTER);
            row.addView(chevron, new LinearLayout.LayoutParams(dp(20), dp(38)));
            return row;
        }
    }

    public static final class LaunchableApp {
        public final String label;
        public final String packageName;
        private final Drawable icon;

        LaunchableApp(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }
}
