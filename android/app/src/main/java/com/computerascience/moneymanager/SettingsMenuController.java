package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class SettingsMenuController {
    private final MoneyManagerActivity activity;

    SettingsMenuController(MoneyManagerActivity activity) {
        this.activity = activity;
    }

    void show(
            String currencySummary,
            String appVersionLabel,
            Runnable openCurrencySettings,
            Runnable openCategorySettings,
            Runnable sharePortfolio,
            Runnable exportBackup,
            Runnable importBackup,
            Runnable checkUpdate,
            Runnable openApkDownload,
            Runnable copyApkDownloadLink
    ) {
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        body.setPadding(pad, activity.dp(8), pad, activity.dp(6));

        ScrollView scrollBody = new ScrollView(activity);
        scrollBody.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("设置")
                .setView(scrollBody)
                .setNegativeButton("关闭", null)
                .create();

        addSection(body, "偏好");
        body.addView(actionRow(dialog, "¥", "基准币种与汇率", currencySummary, MoneyManagerActivity.ACCENT, openCurrencySettings));
        body.addView(actionRow(dialog, "类", "资产类型", "新增自定义类型，并选择哪些类型进入投资页。", MoneyManagerActivity.BLUE, openCategorySettings));

        addSection(body, "数据");
        body.addView(actionRow(dialog, "享", "分享看板", "生成 GitHub Pages 只读链接，别人打开即可查看当前资产概览。", MoneyManagerActivity.ACCENT, sharePortfolio));
        body.addView(actionRow(dialog, "⇧", "导出备份", "保存资产、App 绑定、趋势快照、更新记录和设置。", MoneyManagerActivity.BLUE, exportBackup));
        body.addView(actionRow(dialog, "⇩", "导入备份", "用备份文件覆盖当前本机数据。", MoneyManagerActivity.AMBER, importBackup));

        addSection(body, "版本与更新");
        body.addView(actionRow(dialog, "↻", "检查更新", "当前版本 " + appVersionLabel + "，读取远端最新版本。", MoneyManagerActivity.ACCENT, checkUpdate));
        body.addView(actionRow(dialog, "↓", "直接下载 APK", "打开固定下载地址，适合网络检查失败时使用。", MoneyManagerActivity.BLUE, openApkDownload));
        body.addView(actionRow(dialog, "⛓", "复制下载链接", "把最新 APK 地址复制到剪贴板。", MoneyManagerActivity.MUTED, copyApkDownloadLink));

        activity.showStyledDialog(dialog);
    }

    private void addSection(LinearLayout body, String title) {
        TextView section = activity.label(title);
        LinearLayout.LayoutParams params = activity.lp(-1, -2);
        params.topMargin = body.getChildCount() == 0 ? 0 : activity.dp(18);
        params.bottomMargin = activity.dp(8);
        body.addView(section, params);
    }

    private View actionRow(
            AlertDialog dialog,
            String icon,
            String title,
            String description,
            int iconColor,
            Runnable action
    ) {
        LinearLayout row = activity.row();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(10), activity.dp(10));
        row.setBackground(activity.buttonBackground(MoneyManagerActivity.PANEL, MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        row.setOnClickListener(view -> {
            dialog.dismiss();
            action.run();
        });
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.bottomMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        TextView iconView = activity.text(icon, 18, iconColor, Typeface.BOLD);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(activity.roundedBackground(MoneyManagerActivity.SURFACE_ALT, Color.TRANSPARENT, 8));
        row.addView(iconView, new LinearLayout.LayoutParams(activity.dp(38), activity.dp(38)));

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = activity.text(title, 15, MoneyManagerActivity.INK, Typeface.BOLD);
        titleView.setIncludeFontPadding(false);
        copy.addView(titleView);

        TextView descriptionView = activity.text(description, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.topMargin = activity.dp(4);
        copy.addView(descriptionView, descriptionParams);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.leftMargin = activity.dp(12);
        row.addView(copy, copyParams);

        TextView arrow = activity.text("›", 22, MoneyManagerActivity.MUTED, Typeface.BOLD);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(activity.dp(22), activity.dp(34)));
        return row;
    }
}
