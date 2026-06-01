package com.computerascience.moneymanager;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.PortfolioSettings;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

final class AssetDisplaySupport {
    private final MoneyManagerActivity activity;

    AssetDisplaySupport(MoneyManagerActivity activity) {
        this.activity = activity;
    }

    String formatAmount(AssetRecord asset) {
        if (activity.settings.hideAmounts) {
            return "•••• " + asset.currency;
        }
        if (asset.amount.isEmpty()) {
            return "-- " + asset.currency;
        }
        try {
            Double value = activity.parseNumber(asset.amount);
            if (value == null) {
                return asset.amount + " " + asset.currency;
            }
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(value) + " " + asset.currency;
        } catch (NumberFormatException error) {
            return asset.amount + " " + asset.currency;
        }
    }

    Spinner currencySpinner(String selectedCurrency) {
        String selected = PortfolioSettings.cleanCurrency(selectedCurrency);
        if (selected.isEmpty()) {
            selected = "CNY";
        }
        List<String> options = currencyOptions(selected);
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, options));
        activity.styleSpinner(spinner);
        int selectedIndex = options.indexOf(selected);
        spinner.setSelection(Math.max(0, selectedIndex));
        return spinner;
    }

    List<String> currencyOptions(String selectedCurrency) {
        List<String> options = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            options.add(currency);
        }
        String selected = PortfolioSettings.cleanCurrency(selectedCurrency);
        if (!selected.isEmpty() && !options.contains(selected)) {
            options.add(selected);
        }
        return options;
    }

    String[] categoryOptions(String selectedCategory) {
        List<String> options = categoryOptionList(selectedCategory, true);
        return options.toArray(new String[0]);
    }

    List<String> categoryOptionList(String selectedCategory, boolean includeAddOption) {
        List<String> options = new ArrayList<>();
        for (String category : AssetCategories.ALL) {
            addCategoryOption(options, category);
        }
        if (activity.settings != null) {
            for (String category : activity.settings.customCategories) {
                addCategoryOption(options, category);
            }
        }
        for (AssetRecord asset : activity.assets) {
            addCategoryOption(options, asset.category);
        }
        addCategoryOption(options, selectedCategory);
        if (includeAddOption) {
            options.add(MoneyManagerActivity.ADD_CATEGORY_OPTION);
        }
        return options;
    }

    boolean isDefaultAssetCategory(String category) {
        for (String option : AssetCategories.ALL) {
            if (option.equals(category)) {
                return true;
            }
        }
        return false;
    }

    TextView categoryMark(AssetRecord asset) {
        int color = AssetMath.colorForCategory(asset.category);
        TextView mark = activity.text(categoryIcon(asset.category), 18, color, Typeface.BOLD);
        mark.setGravity(android.view.Gravity.CENTER);
        mark.setBackground(activity.roundedBackground(MoneyManagerActivity.SURFACE_ALT, Color.TRANSPARENT, 8));
        return mark;
    }

    static String categoryIcon(String category) {
        if (AssetCategories.BANK_ACCOUNT.equals(category)) return "¥";
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(category)) return "投";
        if ("银行".equals(category) || AssetCategories.BANK_DEPOSIT.equals(category)) return "¥";
        if (AssetCategories.BANK_WEALTH.equals(category)) return "%";
        if ("券商".equals(category) || AssetCategories.BROKER_HOLDING.equals(category)) return "↗";
        if (AssetCategories.STOCK_HOLDING.equals(category)) return "股";
        if (AssetCategories.BROKER_CASH.equals(category)) return "$";
        if (AssetCategories.FUND.equals(category)) return "%";
        if (AssetCategories.CRYPTO.equals(category)) return "◇";
        if (AssetCategories.REAL_ESTATE.equals(category)) return "⌂";
        if (AssetCategories.DEBT.equals(category)) return "!";
        String cleaned = activityClean(category);
        return cleaned.isEmpty() ? "•" : cleaned.substring(0, 1);
    }

    TextView statusChip(AssetRecord asset) {
        TextView chip = activity.text(activity.statusText(asset), 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(android.view.Gravity.CENTER);
        chip.setPadding(activity.dp(10), activity.dp(6), activity.dp(10), activity.dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(activity.statusColor(asset));
        bg.setCornerRadius(activity.dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private void addCategoryOption(List<String> options, String category) {
        String cleaned = activity.clean(category);
        if (!cleaned.isEmpty() && !MoneyManagerActivity.ADD_CATEGORY_OPTION.equals(cleaned) && !options.contains(cleaned)) {
            options.add(cleaned);
        }
    }

    private static String activityClean(String value) {
        return value == null ? "" : value.trim();
    }
}
