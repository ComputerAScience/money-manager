package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.computerascience.moneymanager.domain.UpdateClient;

public final class UpdateDialog {
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int BLUE = Color.rgb(51, 94, 170);

    private UpdateDialog() {
    }

    public static void show(
            Activity activity,
            String currentVersionLabel,
            long currentVersionCode,
            String updateInfoUrl,
            String fallbackApkUrl
    ) {
        String[] downloadUrl = {fallbackApkUrl};
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 18);
        body.setPadding(pad, dp(activity, 8), pad, dp(activity, 4));

        TextView status = text(activity, "当前版本 " + currentVersionLabel + "\n正在检查最新版本…", 14, INK, Typeface.BOLD);
        status.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        status.setBackground(roundedBackground(activity, ROW_SURFACE, PANEL_BORDER, 8));
        body.addView(status, new LinearLayout.LayoutParams(-1, -2));

        TextView note = text(activity, "如果从旧安装包升级时提示签名不一致，先卸载旧版再安装一次新版；之后同一个固定下载地址通常可以直接覆盖安装。", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(activity, 10);
        body.addView(note, noteParams);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("版本与更新")
                .setView(body)
                .setNegativeButton("关闭", null)
                .setNeutralButton("复制链接", (view, which) -> copyDownloadLink(activity, downloadUrl[0]))
                .setPositiveButton("下载 APK", (view, which) -> openDownload(activity, downloadUrl[0]))
                .create();
        showStyledDialog(activity, dialog);
        fetchLatestUpdateInfo(activity, status, currentVersionLabel, currentVersionCode, updateInfoUrl, fallbackApkUrl, downloadUrl);
    }

    public static void openDownload(Activity activity, String apkUrl) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrlOrFallback(apkUrl, apkUrl))));
        } catch (ActivityNotFoundException error) {
            copyDownloadLink(activity, apkUrl);
        }
    }

    public static void copyDownloadLink(Activity activity, String apkUrl) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Activity.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(activity, "无法访问剪贴板。", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Money Manager APK", downloadUrlOrFallback(apkUrl, apkUrl)));
        Toast.makeText(activity, "下载链接已复制。", Toast.LENGTH_SHORT).show();
    }

    private static void fetchLatestUpdateInfo(
            Activity activity,
            TextView status,
            String currentVersionLabel,
            long currentVersionCode,
            String updateInfoUrl,
            String fallbackApkUrl,
            String[] downloadUrl
    ) {
        new Thread(() -> {
            try {
                UpdateClient.UpdateInfo update = UpdateClient.fetchLatest(updateInfoUrl);
                String resolvedUrl = downloadUrlOrFallback(update.apkUrl, fallbackApkUrl);
                activity.runOnUiThread(() -> {
                    downloadUrl[0] = resolvedUrl;
                    status.setText(updateStatusText(currentVersionLabel, currentVersionCode, update));
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> status.setText("当前版本 " + currentVersionLabel
                        + "\n暂时读取不到远端版本信息。你仍然可以直接下载固定 APK。"));
            }
        }).start();
    }

    private static String updateStatusText(String currentVersionLabel, long currentVersionCode, UpdateClient.UpdateInfo update) {
        String latest = update.versionCode > 0 ? update.displayVersion() : "--";
        String state = update.isNewerThan(currentVersionCode)
                ? "发现新版本，可以下载更新。"
                : "当前已是最新版本，必要时也可以重新下载安装包。";
        String builtAt = clean(update.builtAt).isEmpty() ? "" : "\n构建时间 " + update.builtAt;
        String apkState = clean(update.apkUrl).isEmpty() ? "" : "\n下载地址已同步到最新 Release。";
        return "当前版本 " + currentVersionLabel
                + "\n最新版本 " + latest
                + "\n" + state
                + builtAt
                + apkState;
    }

    private static String downloadUrlOrFallback(String apkUrl, String fallbackApkUrl) {
        String cleanUrl = clean(apkUrl);
        if (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
            return cleanUrl;
        }
        return clean(fallbackApkUrl);
    }

    private static void showStyledDialog(Activity activity, AlertDialog dialog) {
        dialog.setOnShowListener(view -> {
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_POSITIVE), ACCENT);
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), BLUE);
            styleDialogButton(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), MUTED);
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(roundedBackground(activity, Color.WHITE, Color.TRANSPARENT, 8));
        }
    }

    private static void styleDialogButton(Button button, int color) {
        if (button == null) {
            return;
        }
        button.setTextColor(color);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    }

    private static TextView text(Activity activity, String value, int sp, int color, int style) {
        TextView text = new TextView(activity);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        return text;
    }

    private static GradientDrawable roundedBackground(Activity activity, int fill, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(activity, radius));
        bg.setStroke(dp(activity, 1), border);
        return bg;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
