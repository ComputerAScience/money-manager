package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetPresets;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.ui.AppPickerDialog;
import com.computerascience.moneymanager.ui.SpaceView;

final class AssetDialogs {
    private final MainActivity activity;
    private final String[] updateReasons;

    AssetDialogs(MainActivity activity, String[] updateReasons) {
        this.activity = activity;
        this.updateReasons = updateReasons;
    }

    void showEditDialog(AssetRecord original) {
        showEditDialog(original, null);
    }

    void showCreatePreset(AssetPresets.Preset preset) {
        String currency = activity.settings == null ? "CNY" : activity.settings.baseCurrency;
        showEditDialog(null, AssetPresets.createRecord(preset, currency));
    }

    void openLinkedApp(AssetRecord asset) {
        if (asset.launchUri.isEmpty() && asset.packageName.isEmpty()) {
            showAssetAppBindingDialog(asset);
            return;
        }

        if (!asset.launchUri.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(asset.launchUri));
                if (!asset.packageName.isEmpty()) {
                    intent.setPackage(asset.packageName);
                }
                activity.pendingLaunchAssetId = asset.id;
                activity.startActivity(intent);
                return;
            } catch (ActivityNotFoundException error) {
                activity.pendingLaunchAssetId = null;
            }
        }

        if (!asset.packageName.isEmpty()) {
            Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(asset.packageName);
            if (launchIntent != null) {
                activity.pendingLaunchAssetId = asset.id;
                activity.startActivity(launchIntent);
                return;
            }
            openMarket(asset.packageName);
            return;
        }

        activity.toast("没有找到可打开的 App。");
    }

    void showAssetUpdateDialog(AssetRecord asset) {
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, activity.dp(6));

        form.addView(assetUpdateHeader(asset));

        String updateDescription = "核对「" + asset.name + "」后，录入这个机构资产的最新总金额。保存后会更新时间并记录今日总资产快照。";
        TextView description = activity.text(updateDescription, 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.topMargin = activity.dp(12);
        descriptionParams.bottomMargin = activity.dp(12);
        form.addView(description, descriptionParams);

        EditText amount = activity.input("最新金额", asset.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(amount);

        Spinner reason = new Spinner(activity);
        reason.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, updateReasons));
        activity.styleSpinner(reason);
        reason.setSelection(activity.indexOf(updateReasons, "余额核对"));
        form.addView(activity.fieldBox("变化原因", reason));

        EditText note = activity.input("备注（可选）", asset.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("更新资产")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("仅更新时间", null)
                .setPositiveButton("保存更新", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                clearLegacyBreakdown(asset);
                applyAssetUpdate(
                        asset,
                        activity.clean(amount.getText().toString()),
                        activity.clean(note.getText().toString()),
                        String.valueOf(reason.getSelectedItem())
                );
                dialog.dismiss();
            });

            Button onlyTime = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            onlyTime.setTextColor(MoneyManagerActivity.MUTED);
            onlyTime.setOnClickListener(button -> {
                applyAssetUpdate(asset, asset.amount, asset.note, "仅更新时间");
                dialog.dismiss();
            });
        });

        activity.showStyledDialog(dialog);
    }

    private void showEditDialog(AssetRecord original, AssetRecord preset) {
        boolean creating = original == null;
        AssetRecord draft = creating ? (preset == null ? new AssetRecord() : preset) : activity.copyOf(original);

        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, activity.dp(6));

        form.addView(assetEditHeader(draft, creating));

        EditText name = activity.input("资产名称", draft.name, InputType.TYPE_CLASS_TEXT);
        form.addView(name);

        EditText institution = activity.input("机构", draft.institution, InputType.TYPE_CLASS_TEXT);
        String[] selectedPackageName = {draft.packageName};
        String[] selectedAppName = {draft.appName};
        String[] selectedLaunchUri = {draft.launchUri};
        TextView selectedApp = activity.text(
                activity.appBindingText(selectedAppName[0], selectedPackageName[0], selectedLaunchUri[0]),
                15,
                MoneyManagerActivity.INK,
                Typeface.BOLD
        );
        selectedApp.setGravity(Gravity.CENTER_VERTICAL);
        selectedApp.setPadding(activity.dp(14), 0, activity.dp(14), 0);
        selectedApp.setBackground(activity.cardBackground(MoneyManagerActivity.SURFACE, MoneyManagerActivity.PANEL_BORDER));
        form.addView(activity.fieldBox("绑定 App", selectedApp));

        Button chooseApp = activity.secondaryButton(selectedPackageName[0].isEmpty() && selectedLaunchUri[0].isEmpty()
                ? "选择 App"
                : "更换 App");
        chooseApp.setOnClickListener(view -> showAppPicker(selected -> {
            selectedPackageName[0] = selected.packageName;
            selectedAppName[0] = selected.label;
            selectedLaunchUri[0] = "";
            selectedApp.setText(selected.label);
            chooseApp.setText("更换 App");
            String institutionValue = activity.clean(institution.getText().toString());
            if (institutionValue.isEmpty() || institutionValue.contains("待绑定")) {
                institution.setText(selected.label);
            }
            activity.toast("已选择 " + selected.label);
        }));
        LinearLayout.LayoutParams chooseAppParams = activity.lp(-1, activity.dp(44));
        chooseAppParams.bottomMargin = activity.dp(10);
        form.addView(chooseApp, chooseAppParams);

        form.addView(activity.fieldBox("机构", institution));
        TextView institutionHelp = activity.text("机构是主要管理粒度；一条资产属于某个机构，App 只是核对时打开的入口。", 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams institutionHelpParams = activity.lp(-1, -2);
        institutionHelpParams.bottomMargin = activity.dp(10);
        form.addView(institutionHelp, institutionHelpParams);

        Spinner category = new Spinner(activity);
        String[] categoryOptions = activity.categoryOptions(draft.category);
        category.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, categoryOptions));
        activity.styleSpinner(category);
        category.setSelection(activity.indexOf(categoryOptions, draft.category));
        form.addView(activity.fieldBox("资产类型", category));

        EditText customCategory = activity.input("新增资产类型", "", InputType.TYPE_CLASS_TEXT);
        form.addView(customCategory);
        CheckBox customCategoryInvestment = activity.styledCheckBox("在投资 Tab 显示这个类型", false);
        LinearLayout.LayoutParams customInvestmentParams = activity.lp(-1, -2);
        customInvestmentParams.bottomMargin = activity.dp(10);
        form.addView(customCategoryInvestment, customInvestmentParams);
        updateCustomCategoryFields(String.valueOf(category.getSelectedItem()), customCategory, customCategoryInvestment);
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateCustomCategoryFields(String.valueOf(category.getSelectedItem()), customCategory, customCategoryInvestment);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        LinearLayout amountRow = activity.row();
        EditText amount = activity.input("金额", draft.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        Spinner currency = activity.currencySpinner(draft.currency);
        View amountBox = activity.fieldBox("金额", amount);
        amountRow.addView(amountBox, new LinearLayout.LayoutParams(0, -2, 1));
        amountRow.addView(new SpaceView(activity, activity.dp(8), 1));
        amountRow.addView(activity.fieldBox("币种", currency), new LinearLayout.LayoutParams(0, -2, 0.62f));
        form.addView(amountRow);

        EditText cadence = activity.input("更新周期（天）", String.valueOf(draft.updateEveryDays), InputType.TYPE_CLASS_NUMBER);
        form.addView(cadence);

        EditText note = activity.input("备注", draft.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(creating ? "新增资产" : "编辑资产")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null);
        if (!creating) {
            builder.setNeutralButton("删除", null);
        }
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                String assetName = activity.clean(name.getText().toString());
                if (assetName.isEmpty()) {
                    activity.toast("资产名称不能为空");
                    return;
                }

                String selectedCategory = String.valueOf(category.getSelectedItem());
                if (MoneyManagerActivity.ADD_CATEGORY_OPTION.equals(selectedCategory)) {
                    selectedCategory = activity.clean(customCategory.getText().toString());
                    if (selectedCategory.isEmpty()) {
                        activity.toast("请输入新的资产类型。");
                        return;
                    }
                    if (MoneyManagerActivity.ADD_CATEGORY_OPTION.equals(selectedCategory)) {
                        activity.toast("资产类型名称不能使用系统选项名称。");
                        return;
                    }
                    activity.settings.addCustomCategory(selectedCategory);
                    activity.settings.setInvestmentCategory(selectedCategory, customCategoryInvestment.isChecked());
                    activity.store.saveSettings(activity.settings);
                }

                int everyDays = activity.parsePositiveInt(cadence.getText().toString(), 7);
                draft.name = assetName;
                draft.category = selectedCategory;
                draft.institution = activity.clean(institution.getText().toString());
                clearLegacyBreakdown(draft);
                draft.amount = activity.clean(amount.getText().toString());
                draft.currency = String.valueOf(currency.getSelectedItem());
                draft.updateEveryDays = everyDays;
                draft.appName = activity.clean(selectedAppName[0]);
                draft.packageName = activity.clean(selectedPackageName[0]);
                draft.launchUri = activity.clean(selectedLaunchUri[0]);
                draft.note = activity.clean(note.getText().toString());
                draft.lastUpdatedAt = System.currentTimeMillis();

                if (creating) {
                    activity.assets.add(draft);
                } else {
                    activity.replaceAsset(draft);
                }
                activity.store.save(activity.assets);
                activity.snapshots = activity.store.recordSnapshot(activity.assets, activity.settings);
                activity.render();
                activity.toast(creating ? "资产已新增并标记更新。" : "资产已保存并标记更新。");
                dialog.dismiss();
            });

            Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (delete != null) {
                delete.setTextColor(MoneyManagerActivity.DANGER);
                delete.setOnClickListener(button -> {
                    AlertDialog confirmDialog = new AlertDialog.Builder(activity)
                            .setTitle("删除资产")
                            .setMessage("确定删除「" + original.name + "」吗？")
                            .setNegativeButton("取消", null)
                            .setPositiveButton("删除", (confirm, which) -> {
                                activity.removeAssetById(original.id);
                                activity.store.save(activity.assets);
                                activity.snapshots = activity.store.recordSnapshot(activity.assets, activity.settings);
                                activity.render();
                                dialog.dismiss();
                            })
                            .create();
                    activity.showStyledDialog(confirmDialog);
                });
            }
        });

        activity.showStyledDialog(dialog);
    }

    private void showAppPicker(AppPickerDialog.SelectionHandler handler) {
        AppPickerDialog.show(activity, "选择已安装 App", "搜索银行、券商、钱包或 App 名称。", handler);
    }

    private void showAssetAppBindingDialog(AssetRecord asset) {
        AppPickerDialog.show(activity, "绑定并打开 App", "「" + asset.name + "」还没有绑定 App。先选择一次，以后就能一键打开。", selected -> {
            asset.appName = selected.label;
            asset.packageName = selected.packageName;
            asset.launchUri = "";
            if (asset.institution.isEmpty() || asset.institution.contains("待绑定")) {
                asset.institution = selected.label;
            }
            activity.store.save(activity.assets);
            activity.render();
            activity.toast("已绑定 " + selected.label + "。");
            openLinkedApp(asset);
        });
    }

    private void openMarket(String packageName) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException error) {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            } catch (ActivityNotFoundException ignored) {
                activity.toast("没有找到这个 App，请重新选择绑定 App。");
            }
        }
    }

    private View assetEditHeader(AssetRecord asset, boolean creating) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(12));
        panel.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams panelParams = activity.lp(-1, -2);
        panelParams.bottomMargin = activity.dp(12);
        panel.setLayoutParams(panelParams);

        TextView mark = creating ? activity.text("+", 20, MoneyManagerActivity.ACCENT, Typeface.BOLD) : activity.categoryMark(asset);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(activity.roundedBackground(MoneyManagerActivity.SURFACE_ALT, Color.TRANSPARENT, 8));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(activity.dp(40), activity.dp(40));
        markParams.rightMargin = activity.dp(12);
        panel.addView(mark, markParams);

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        String title = creating ? (asset.name.isEmpty() ? "新增资产" : asset.name) : asset.name;
        copy.addView(activity.text(title, 15, MoneyManagerActivity.INK, Typeface.BOLD));

        String description = creating
                ? (asset.category.isEmpty() ? "记录金额、周期和要打开的 App。" : asset.category + " · " + (asset.institution.isEmpty() ? "未填写机构" : asset.institution))
                : asset.category + " · " + (asset.institution.isEmpty() ? "未填写机构" : asset.institution);
        TextView detail = activity.text(description, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(4);
        copy.addView(detail, detailParams);
        panel.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        return panel;
    }

    private View assetUpdateHeader(AssetRecord asset) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(activity.dp(12), activity.dp(12), activity.dp(12), activity.dp(12));
        panel.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));

        LinearLayout top = activity.row();
        TextView mark = activity.categoryMark(asset);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(activity.dp(40), activity.dp(40));
        markParams.rightMargin = activity.dp(12);
        top.addView(mark, markParams);

        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(activity.text(asset.name, 15, MoneyManagerActivity.INK, Typeface.BOLD));

        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        TextView meta = activity.text(asset.category + " · " + institution, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(4);
        titleGroup.addView(meta, metaParams);
        top.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));
        top.addView(activity.statusChip(asset));
        panel.addView(top);

        LinearLayout detail = activity.row();
        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(12);
        panel.addView(detail, detailParams);

        TextView amount = activity.text(activity.formatAmount(asset), 18, MoneyManagerActivity.INK, Typeface.BOLD);
        detail.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView updated = activity.text(activity.lastUpdatedText(asset), 12, MoneyManagerActivity.MUTED, Typeface.BOLD);
        updated.setGravity(Gravity.RIGHT);
        detail.addView(updated, new LinearLayout.LayoutParams(0, -2, 1));
        return panel;
    }

    private void updateCustomCategoryFields(String selectedCategory, View customCategory, View investmentToggle) {
        boolean adding = MoneyManagerActivity.ADD_CATEGORY_OPTION.equals(selectedCategory);
        customCategory.setVisibility(adding ? View.VISIBLE : View.GONE);
        investmentToggle.setVisibility(adding ? View.VISIBLE : View.GONE);
    }

    private void applyAssetUpdate(AssetRecord asset, String amount, String note, String reason) {
        String previousAmount = asset.amount;
        long now = System.currentTimeMillis();
        asset.amount = amount;
        asset.note = note;
        asset.lastUpdatedAt = now;
        activity.store.save(activity.assets);
        activity.updateEvents = activity.store.recordUpdateEvent(new AssetUpdateEvent(
                asset.id,
                asset.name,
                now,
                asset.currency,
                previousAmount,
                amount,
                activity.cleanReason(reason),
                note
        ));
        activity.snapshots = activity.store.recordSnapshot(activity.assets, activity.settings);
        activity.render();
        activity.toast("已更新「" + asset.name + "」。");
    }

    private void clearLegacyBreakdown(AssetRecord asset) {
        asset.bankDepositAmount = "";
        asset.bankWealthAmount = "";
        asset.bankDebtAmount = "";
        asset.investmentHoldingAmount = "";
        asset.investmentCashAmount = "";
        asset.investmentPositions = "";
    }
}
