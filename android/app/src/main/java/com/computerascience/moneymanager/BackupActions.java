package com.computerascience.moneymanager;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import com.computerascience.moneymanager.data.AssetStore;
import com.computerascience.moneymanager.domain.SharePayloadBuilder;
import com.computerascience.moneymanager.model.AssetBackup;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class BackupActions {
    private BackupActions() {
    }

    static void sharePortfolioPage(
            MoneyManagerActivity activity,
            String sharePageUrl,
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            PortfolioSettings settings
    ) {
        try {
            String link = SharePayloadBuilder.buildLink(sharePageUrl, assets, snapshots, settings);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Money Manager 资产看板");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "我的资产看板：\n" + link);
            activity.startActivity(Intent.createChooser(shareIntent, "分享资产看板"));
        } catch (ActivityNotFoundException error) {
            activity.toast("没有找到可分享的应用。");
        } catch (Exception error) {
            activity.toast("生成分享链接失败，请重试。");
        }
    }

    static void startBackupExport(MoneyManagerActivity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "money-manager-backup-" + activity.backupDate() + ".json");
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException error) {
            activity.toast("没有找到可保存文件的应用。");
        }
    }

    static void startBackupImport(MoneyManagerActivity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException error) {
            activity.toast("没有找到可选择文件的应用。");
        }
    }

    static void writeBackup(
            MoneyManagerActivity activity,
            Uri uri,
            AssetStore store,
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            List<AssetUpdateEvent> updateEvents,
            PortfolioSettings settings
    ) {
        try (OutputStream output = activity.getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                activity.toast("无法写入备份文件。");
                return;
            }
            String raw = store.exportJson(assets, snapshots, updateEvents, settings);
            output.write(raw.getBytes(StandardCharsets.UTF_8));
            activity.toast("备份已导出。");
        } catch (Exception error) {
            activity.toast("导出失败，请重试。");
        }
    }

    static AssetBackup readBackup(MoneyManagerActivity activity, Uri uri, AssetStore store) {
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            if (input == null) {
                activity.toast("无法读取备份文件。");
                return null;
            }
            return store.parseBackup(readUtf8(input));
        } catch (Exception error) {
            activity.toast("导入失败，请确认文件是 Money Manager 备份。");
            return null;
        }
    }

    static String importBackupMessage(MoneyManagerActivity activity, AssetBackup backup) {
        List<String> lines = new ArrayList<>();
        lines.add("将导入 " + backup.assets.size() + " 项资产和 "
                + backup.snapshots.size() + " 个趋势快照、"
                + backup.updateEvents.size() + " 条更新记录，以及汇率和目标设置。");
        lines.add("备份版本：" + backupVersionText(backup));
        if (backup.migratedLegacyAssets) {
            lines.add("检测到旧版银行 / 券商资产结构，导入时已自动转换为按机构管理的扁平资产。");
        }
        lines.add("导入会覆盖当前本机数据。");
        return activity.joinLines(lines);
    }

    private static String backupVersionText(AssetBackup backup) {
        if (backup.schemaVersion <= 0 && backup.version <= 0) {
            return "旧版或未知";
        }
        if (backup.schemaVersion > 0) {
            return "schema " + backup.schemaVersion;
        }
        return "version " + backup.version;
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
