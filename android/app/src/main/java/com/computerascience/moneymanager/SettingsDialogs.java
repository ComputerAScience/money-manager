package com.computerascience.moneymanager;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.ExchangeRateClient;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

final class SettingsDialogs {
    private final MainActivity activity;

    SettingsDialogs(MainActivity activity) {
        this.activity = activity;
    }

    void showNetWorthGoalDialog() {
        PortfolioSummary portfolio = AssetMath.summarize(activity.assets, activity.settings);
        PortfolioSettings draft = PortfolioSettings.copyOf(activity.settings);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, 0);

        TextView description = activity.text(
                "目标按当前基准币种 " + portfolio.baseCurrency + " 记录；切换基准币种后建议重新确认目标。",
                14,
                MoneyManagerActivity.MUTED,
                Typeface.NORMAL
        );
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.bottomMargin = activity.dp(12);
        form.addView(description, descriptionParams);

        String targetValue = draft.netWorthTarget > 0
                ? activity.formatInputNumber(draft.netWorthTarget)
                : activity.formatInputNumber(Math.max(0, portfolio.netWorth));
        EditText target = activity.input("目标净资产（" + portfolio.baseCurrency + "）", targetValue, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        form.addView(target);

        String dateValue = draft.netWorthTargetDate > 0 ? activity.dayKey(draft.netWorthTargetDate) : activity.defaultYearEnd();
        EditText targetDate = activity.input("截止日期（yyyy-MM-dd）", dateValue, InputType.TYPE_CLASS_TEXT);
        form.addView(targetDate);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("编辑年度目标")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("清空目标", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button clear = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            clear.setTextColor(MoneyManagerActivity.DANGER);
            clear.setOnClickListener(button -> {
                activity.settings.netWorthTarget = 0;
                activity.settings.netWorthTargetDate = 0;
                activity.store.saveSettings(activity.settings);
                activity.render();
                activity.toast("已清空年度目标。");
                dialog.dismiss();
            });

            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                Double value = activity.parseNumber(activity.clean(target.getText().toString()));
                if (value == null || value <= 0) {
                    activity.toast("目标净资产需要是大于 0 的数字。");
                    return;
                }

                Date parsedDate = activity.parseDay(activity.clean(targetDate.getText().toString()));
                if (parsedDate == null) {
                    activity.toast("截止日期格式应为 yyyy-MM-dd。");
                    return;
                }
                if (parsedDate.getTime() < System.currentTimeMillis() - AssetMath.DAY_MS) {
                    activity.toast("截止日期不能早于今天。");
                    return;
                }

                draft.netWorthTarget = value;
                draft.netWorthTargetDate = parsedDate.getTime();
                activity.settings = draft;
                activity.store.saveSettings(activity.settings);
                activity.render();
                activity.toast("年度目标已保存。");
                dialog.dismiss();
            });
        });

        activity.showStyledDialog(dialog);
    }

    void showCurrencySettingsDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(activity.settings);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, 0);

        TextView description = activity.text("会自动获取常用币种的最新公开汇率；网络不可用时仍可手动修改。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.bottomMargin = activity.dp(12);
        form.addView(description, descriptionParams);

        Spinner baseCurrency = activity.currencySpinner(draft.baseCurrency);
        form.addView(activity.fieldBox("基准币种", baseCurrency));

        Button refreshButton = activity.secondaryButton("获取实时汇率");
        LinearLayout.LayoutParams refreshParams = activity.lp(-1, activity.dp(44));
        refreshParams.bottomMargin = activity.dp(12);
        form.addView(refreshButton, refreshParams);

        List<CurrencyRateField> rateFields = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            EditText rateInput = activity.input(
                    "1 " + currency + " 等于多少基准币种",
                    activity.formatRate(draft.rateFor(currency)),
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
            rateFields.add(new CurrencyRateField(currency, rateInput));
            form.addView(rateInput);
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("汇率设置")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            refreshButton.setOnClickListener(button -> {
                String base = String.valueOf(baseCurrency.getSelectedItem());
                refreshButton.setEnabled(false);
                refreshButton.setText("获取中...");
                fetchRealtimeRates(base, rates -> {
                    draft.baseCurrency = base;
                    draft.ratesToBase.putAll(rates);
                    draft.ensureBaseRate();
                    for (CurrencyRateField field : rateFields) {
                        field.input.setText(activity.formatRate(draft.rateFor(field.currency)));
                    }
                    refreshButton.setEnabled(true);
                    refreshButton.setText("获取实时汇率");
                    activity.toast("实时汇率已填入。");
                }, message -> {
                    refreshButton.setEnabled(true);
                    refreshButton.setText("获取实时汇率");
                    activity.toast(message);
                });
            });

            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                String base = PortfolioSettings.cleanCurrency(String.valueOf(baseCurrency.getSelectedItem()));
                if (base.isEmpty()) {
                    activity.toast("基准币种不能为空。");
                    return;
                }

                draft.baseCurrency = base;
                for (CurrencyRateField field : rateFields) {
                    double rate = activity.parsePositiveDouble(field.input.getText().toString(), field.currency.equals(base) ? 1.0 : 0.0);
                    if (field.currency.equals(base)) {
                        rate = 1.0;
                    }
                    draft.setRate(field.currency, rate);
                }
                draft.ensureBaseRate();

                activity.settings = draft;
                activity.store.saveSettings(activity.settings);
                activity.snapshots = activity.store.recordSnapshot(activity.assets, activity.settings);
                activity.render();
                activity.toast("汇率已更新。");
                dialog.dismiss();
            });
        });

        activity.showStyledDialog(dialog);
    }

    void refreshExchangeRates(boolean showToast) {
        fetchRealtimeRates(activity.settings.baseCurrency, rates -> {
            activity.settings.ratesToBase.putAll(rates);
            activity.settings.ensureBaseRate();
            activity.store.saveSettings(activity.settings);
            activity.render();
            if (showToast) {
                activity.toast("实时汇率已更新。");
            }
        }, message -> {
            if (showToast) {
                activity.toast(message);
            }
        });
    }

    void showAllocationTargetDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(activity.settings);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, 0);

        TextView description = activity.text("填写各类型目标占比，合计需要等于 100%。留空表示 0%。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.bottomMargin = activity.dp(12);
        form.addView(description, descriptionParams);

        List<AllocationTargetField> targetFields = new ArrayList<>();
        for (String category : activity.categoryOptionList("", false)) {
            double current = draft.targetForCategory(category);
            EditText targetInput = activity.input(
                    category + " 目标占比（%）",
                    current <= 0 ? "" : activity.formatInputNumber(current),
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
            );
            targetFields.add(new AllocationTargetField(category, targetInput));
            form.addView(targetInput);
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("编辑目标比例")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setNeutralButton("清空目标", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button clear = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            clear.setTextColor(MoneyManagerActivity.DANGER);
            clear.setOnClickListener(button -> {
                activity.settings.clearAllocationTargets();
                activity.store.saveSettings(activity.settings);
                activity.render();
                activity.toast("已清空目标比例。");
                dialog.dismiss();
            });

            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                draft.clearAllocationTargets();
                double total = 0;
                for (AllocationTargetField field : targetFields) {
                    String raw = activity.clean(field.input.getText().toString());
                    if (raw.isEmpty()) {
                        continue;
                    }
                    Double value = activity.parseNumber(raw);
                    if (value == null || value < 0 || value > 100) {
                        activity.toast(field.category + " 的目标占比需要在 0 到 100 之间。");
                        return;
                    }
                    if (value > 0) {
                        draft.setAllocationTarget(field.category, value);
                        total += value;
                    }
                }

                if (total > 0 && Math.abs(total - 100) > 0.5) {
                    activity.toast("目标比例合计需要等于 100%。当前为 " + activity.formatPercentValue(total) + "。");
                    return;
                }

                activity.settings = draft;
                activity.store.saveSettings(activity.settings);
                activity.render();
                activity.toast(total <= 0 ? "已清空目标比例。" : "目标比例已保存。");
                dialog.dismiss();
            });
        });

        activity.showStyledDialog(dialog);
    }

    void showCategorySettingsDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(activity.settings);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.dp(18);
        form.setPadding(pad, activity.dp(6), pad, 0);

        TextView description = activity.text(
                "资产类型会出现在新增资产、资产比例和目标比例里；勾选“投资页”的类型会单独汇总到投资 Tab。",
                14,
                MoneyManagerActivity.MUTED,
                Typeface.NORMAL
        );
        LinearLayout.LayoutParams descriptionParams = activity.lp(-1, -2);
        descriptionParams.bottomMargin = activity.dp(12);
        form.addView(description, descriptionParams);

        List<CategorySettingField> categoryFields = new ArrayList<>();
        for (String category : activity.categoryOptionList("", false)) {
            CheckBox investment = activity.styledCheckBox("投资页", draft.isInvestmentCategory(category));
            categoryFields.add(new CategorySettingField(category, investment));
            form.addView(categorySettingRow(category, investment));
        }

        TextView addTitle = activity.label("新增类型");
        LinearLayout.LayoutParams addTitleParams = activity.lp(-1, -2);
        addTitleParams.topMargin = activity.dp(10);
        addTitleParams.bottomMargin = activity.dp(8);
        form.addView(addTitle, addTitleParams);

        EditText newCategory = activity.input("例如：美股、港股、期权、保险", "", InputType.TYPE_CLASS_TEXT);
        form.addView(newCategory);
        CheckBox newCategoryInvestment = activity.styledCheckBox("添加后显示在投资页", false);
        LinearLayout.LayoutParams newInvestmentParams = activity.lp(-1, -2);
        newInvestmentParams.bottomMargin = activity.dp(8);
        form.addView(newCategoryInvestment, newInvestmentParams);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("资产类型")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(MoneyManagerActivity.ACCENT);
            save.setOnClickListener(button -> {
                draft.clearInvestmentCategories();
                for (CategorySettingField field : categoryFields) {
                    draft.setInvestmentCategory(field.category, field.investment.isChecked());
                }

                String addedCategory = activity.clean(newCategory.getText().toString());
                if (!addedCategory.isEmpty()) {
                    if (MoneyManagerActivity.ADD_CATEGORY_OPTION.equals(addedCategory)) {
                        activity.toast("资产类型名称不能使用系统选项名称。");
                        return;
                    }
                    draft.addCustomCategory(addedCategory);
                    draft.setInvestmentCategory(addedCategory, newCategoryInvestment.isChecked());
                }

                activity.settings = draft;
                activity.store.saveSettings(activity.settings);
                activity.snapshots = activity.store.recordSnapshot(activity.assets, activity.settings);
                activity.render();
                activity.toast("资产类型已保存。");
                dialog.dismiss();
            });
        });

        activity.showStyledDialog(dialog);
    }

    private View categorySettingRow(String category, CheckBox investment) {
        LinearLayout row = activity.row();
        row.setPadding(activity.dp(12), activity.dp(9), activity.dp(8), activity.dp(9));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.bottomMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        TextView mark = activity.text(activity.categoryIcon(category), 15, AssetMath.colorForCategory(category), Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(activity.roundedBackground(MoneyManagerActivity.SURFACE_ALT, Color.TRANSPARENT, 8));
        row.addView(mark, new LinearLayout.LayoutParams(activity.dp(34), activity.dp(34)));

        LinearLayout copy = new LinearLayout(activity);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.addView(activity.text(category, 14, MoneyManagerActivity.INK, Typeface.BOLD));
        TextView origin = activity.text(categoryOriginText(category), 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams originParams = activity.lp(-1, -2);
        originParams.topMargin = activity.dp(3);
        copy.addView(origin, originParams);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.leftMargin = activity.dp(10);
        row.addView(copy, copyParams);

        row.addView(investment, new LinearLayout.LayoutParams(activity.dp(92), activity.dp(40)));
        return row;
    }

    private String categoryOriginText(String category) {
        if (activity.isDefaultAssetCategory(category)) {
            return "内置类型";
        }
        if (activity.settings.customCategories.contains(category)) {
            return "自定义类型";
        }
        return "已在资产中使用";
    }

    private void fetchRealtimeRates(
            String baseCurrency,
            RateSuccessHandler successHandler,
            RateFailureHandler failureHandler
    ) {
        String base = PortfolioSettings.cleanCurrency(baseCurrency);
        if (!isCommonCurrency(base)) {
            failureHandler.onFailure("实时汇率暂只支持 CNY / USD / HKD / EUR / JPY。");
            return;
        }

        new Thread(() -> {
            try {
                Map<String, Double> rates = ExchangeRateClient.fetchRatesToBase(base);
                activity.runOnUiThread(() -> successHandler.onSuccess(rates));
            } catch (Exception error) {
                activity.runOnUiThread(() -> failureHandler.onFailure("实时汇率获取失败，请稍后重试。"));
            }
        }).start();
    }

    private boolean isCommonCurrency(String currency) {
        String cleanCurrency = PortfolioSettings.cleanCurrency(currency);
        for (String option : PortfolioSettings.COMMON_CURRENCIES) {
            if (option.equals(cleanCurrency)) {
                return true;
            }
        }
        return false;
    }

    private interface RateSuccessHandler {
        void onSuccess(Map<String, Double> rates);
    }

    private interface RateFailureHandler {
        void onFailure(String message);
    }

    private static final class CurrencyRateField {
        final String currency;
        final EditText input;

        CurrencyRateField(String currency, EditText input) {
            this.currency = currency;
            this.input = input;
        }
    }

    private static final class AllocationTargetField {
        final String category;
        final EditText input;

        AllocationTargetField(String category, EditText input) {
            this.category = category;
            this.input = input;
        }
    }

    private static final class CategorySettingField {
        final String category;
        final CheckBox investment;

        CategorySettingField(String category, CheckBox investment) {
            this.category = category;
            this.investment = investment;
        }
    }
}
