package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.CategoryBreakdown;
import com.computerascience.moneymanager.model.InstitutionBreakdown;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssetMath {
    public static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private AssetMath() {
    }

    public static PortfolioSummary summarize(List<AssetRecord> assets, PortfolioSettings settings) {
        double grossAssets = 0;
        double liabilities = 0;
        int staleCount = 0;
        int missingBindingCount = 0;
        int missingRateCount = 0;
        String baseCurrency = settings.baseCurrency;
        boolean hasMixedCurrencies = false;
        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Double> institutionTotals = new HashMap<>();
        Map<String, Integer> institutionCounts = new HashMap<>();

        for (AssetRecord asset : assets) {
            String currency = cleanCurrency(asset.currency);
            if (!currency.isEmpty() && !currency.equals(baseCurrency)) {
                hasMixedCurrencies = true;
            }
            double rate = settings.rateFor(currency);
            if (!settings.hasRateFor(currency)) {
                missingRateCount += 1;
                rate = 1.0;
            }
            double grossAmount = assetGrossAmount(asset) * rate;
            double liabilityAmount = assetLiabilityAmount(asset) * rate;
            grossAssets += grossAmount;
            liabilities += liabilityAmount;

            if (isBankAccount(asset)) {
                addTotal(categoryTotals, AssetCategories.BANK_ACCOUNT, grossAmount);
                addTotal(categoryTotals, AssetCategories.DEBT, liabilityAmount);
            } else if (AssetCategories.isLegacyBankCategory(asset.category)) {
                addTotal(categoryTotals, AssetCategories.BANK_ACCOUNT, grossAmount);
            } else if (AssetCategories.isLegacyInvestmentCategory(asset.category)) {
                addTotal(categoryTotals, AssetCategories.INVESTMENT_ACCOUNT, grossAmount);
            } else if (isLiability(asset)) {
                addTotal(categoryTotals, AssetCategories.DEBT, liabilityAmount);
            } else {
                addTotal(categoryTotals, asset.category, grossAmount);
            }
            String institution = cleanInstitution(asset.institution);
            institutionTotals.put(institution, doubleValue(institutionTotals, institution) + grossAmount + liabilityAmount);
            institutionCounts.put(institution, intValue(institutionCounts, institution) + 1);

            if (isStale(asset)) {
                staleCount += 1;
            }
            if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
                missingBindingCount += 1;
            }
        }

        List<CategoryBreakdown> categories = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            categories.add(new CategoryBreakdown(entry.getKey(), entry.getValue(), colorForCategory(entry.getKey())));
        }
        Collections.sort(categories, (left, right) -> Double.compare(right.value, left.value));

        List<InstitutionBreakdown> institutions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : institutionTotals.entrySet()) {
            if (entry.getValue() <= 0) {
                continue;
            }
            String institution = entry.getKey();
            institutions.add(new InstitutionBreakdown(
                    institution,
                    entry.getValue(),
                    intValue(institutionCounts, institution)
            ));
        }
        Collections.sort(institutions, (left, right) -> Double.compare(right.value, left.value));

        return new PortfolioSummary(
                grossAssets - liabilities,
                grossAssets,
                liabilities,
                assets.size(),
                staleCount,
                missingBindingCount,
                missingRateCount,
                baseCurrency,
                hasMixedCurrencies,
                categories,
                institutions
        );
    }

    public static boolean isStale(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return true;
        }
        long days = (System.currentTimeMillis() - asset.lastUpdatedAt) / DAY_MS;
        return days >= asset.updateEveryDays;
    }

    public static boolean isLiability(AssetRecord asset) {
        return AssetCategories.DEBT.equals(asset.category);
    }

    public static boolean isBankAccount(AssetRecord asset) {
        return asset != null && AssetCategories.isBankAccount(asset.category);
    }

    public static boolean isInvestmentAsset(AssetRecord asset) {
        return asset != null && (AssetCategories.isInvestmentAccount(asset.category)
                || AssetCategories.isLegacyInvestmentCategory(asset.category));
    }

    public static double assetGrossAmount(AssetRecord asset) {
        if (asset == null) {
            return 0;
        }
        if (isBankAccount(asset)) {
            if (hasText(asset.amount)) {
                return componentAmount(asset.amount);
            }
            return componentAmount(asset.bankDepositAmount) + componentAmount(asset.bankWealthAmount);
        }
        if (AssetCategories.isInvestmentAccount(asset.category)) {
            if (hasText(asset.amount)) {
                return componentAmount(asset.amount);
            }
            return componentAmount(asset.investmentHoldingAmount) + componentAmount(asset.investmentCashAmount);
        }
        if (isLiability(asset)) {
            return 0;
        }
        return componentAmount(asset.amount);
    }

    public static double assetLiabilityAmount(AssetRecord asset) {
        if (asset == null) {
            return 0;
        }
        if (isBankAccount(asset)) {
            return hasText(asset.amount) ? 0 : componentAmount(asset.bankDebtAmount);
        }
        if (isLiability(asset)) {
            return componentAmount(asset.amount);
        }
        return 0;
    }

    public static double assetNetAmount(AssetRecord asset) {
        return assetGrossAmount(asset) - assetLiabilityAmount(asset);
    }

    public static double investmentHoldingAmount(AssetRecord asset) {
        if (asset == null) {
            return 0;
        }
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)) {
            if (!hasInvestmentBreakdown(asset)) {
                return componentAmount(asset.amount);
            }
            return componentAmount(asset.investmentHoldingAmount);
        }
        if (AssetCategories.BROKER_CASH.equals(asset.category)) {
            return 0;
        }
        if (isInvestmentAsset(asset)) {
            return componentAmount(asset.amount);
        }
        return 0;
    }

    public static double investmentCashAmount(AssetRecord asset) {
        if (asset == null) {
            return 0;
        }
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)) {
            return hasInvestmentBreakdown(asset) ? componentAmount(asset.investmentCashAmount) : 0;
        }
        if (AssetCategories.BROKER_CASH.equals(asset.category)) {
            return componentAmount(asset.amount);
        }
        return 0;
    }

    public static boolean hasBankBreakdown(AssetRecord asset) {
        return hasText(asset.bankDepositAmount)
                || hasText(asset.bankWealthAmount)
                || hasText(asset.bankDebtAmount);
    }

    public static boolean hasInvestmentBreakdown(AssetRecord asset) {
        return hasText(asset.investmentHoldingAmount)
                || hasText(asset.investmentCashAmount);
    }

    public static double parseAmount(String raw) {
        if (raw == null) {
            return 0;
        }
        String cleaned = raw
                .replace(",", "")
                .replace("¥", "")
                .replace("$", "")
                .replace("€", "")
                .replace("￥", "")
                .trim();
        if (cleaned.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    public static String cleanCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }

    public static String cleanInstitution(String institution) {
        if (institution == null || institution.trim().isEmpty()) {
            return "未填写机构";
        }
        return institution.trim();
    }

    private static double doubleValue(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? 0.0 : value;
    }

    private static int intValue(Map<String, Integer> values, String key) {
        Integer value = values.get(key);
        return value == null ? 0 : value;
    }

    private static void addTotal(Map<String, Double> values, String key, double amount) {
        if (amount <= 0) {
            return;
        }
        values.put(key, doubleValue(values, key) + amount);
    }

    private static double componentAmount(String value) {
        return Math.abs(parseAmount(value));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static int colorForCategory(String category) {
        if (AssetCategories.BANK_ACCOUNT.equals(category)) return 0xFF126B5F;
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(category)) return 0xFF335EAA;
        if ("银行".equals(category) || AssetCategories.BANK_DEPOSIT.equals(category)) return 0xFF126B5F;
        if (AssetCategories.BANK_WEALTH.equals(category)) return 0xFF3B7F91;
        if ("券商".equals(category) || AssetCategories.BROKER_HOLDING.equals(category)) return 0xFF335EAA;
        if (AssetCategories.STOCK_HOLDING.equals(category)) return 0xFF7C4DFF;
        if (AssetCategories.BROKER_CASH.equals(category)) return 0xFF5B6F94;
        if (AssetCategories.FUND.equals(category)) return 0xFF3B7F91;
        if (AssetCategories.CRYPTO.equals(category)) return 0xFFB85C2F;
        if (AssetCategories.REAL_ESTATE.equals(category)) return 0xFF72518A;
        if (AssetCategories.DEBT.equals(category)) return 0xFFBE4350;
        return 0xFFA67918;
    }
}
