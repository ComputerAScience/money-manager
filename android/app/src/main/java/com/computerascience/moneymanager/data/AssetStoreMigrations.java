package com.computerascience.moneymanager.data;

import android.content.SharedPreferences;

import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetRecord;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

final class AssetStoreMigrations {
    private AssetStoreMigrations() {
    }

    static boolean migrateStoredAssets(
            List<AssetRecord> assets,
            SharedPreferences preferences,
            String schemaKey,
            int schemaVersion
    ) {
        int storedSchemaVersion = preferences.getInt(schemaKey, 0);
        boolean changed = false;
        if (storedSchemaVersion < 7) {
            changed = migrateLegacyAccountAssets(assets);
        }
        if (storedSchemaVersion < 8) {
            changed = migrateAccountDetailsToInstitutionAssets(assets) || changed;
        }
        return changed || storedSchemaVersion < schemaVersion;
    }

    static boolean migrateImportedAssets(List<AssetRecord> assets) {
        boolean legacyChanged = migrateLegacyAccountAssets(assets);
        boolean detailChanged = migrateAccountDetailsToInstitutionAssets(assets);
        return legacyChanged || detailChanged;
    }

    static List<AssetRecord> defaultAssets() {
        List<AssetRecord> assets = new ArrayList<>();

        AssetRecord bank = new AssetRecord();
        bank.name = "银行账户";
        bank.category = AssetCategories.BANK_ACCOUNT;
        bank.institution = "";
        bank.amount = "0";
        bank.currency = "CNY";
        bank.updateEveryDays = 7;
        bank.note = "按机构管理账户；需要打开银行 App 时，在这条资产上绑定对应 App。";
        assets.add(bank);

        AssetRecord investment = new AssetRecord();
        investment.name = "投资账户";
        investment.category = AssetCategories.INVESTMENT_ACCOUNT;
        investment.institution = "";
        investment.amount = "0";
        investment.currency = "CNY";
        investment.updateEveryDays = 1;
        investment.note = "按机构记录投资账户总额；持仓明细可以放在备注里，App 只作为打开入口。";
        assets.add(investment);

        return assets;
    }

    private static boolean migrateLegacyAccountAssets(List<AssetRecord> assets) {
        boolean changed = false;
        for (AssetRecord asset : assets) {
            changed = migrateLegacyAccountAsset(asset) || changed;
        }
        return changed;
    }

    private static boolean migrateLegacyAccountAsset(AssetRecord asset) {
        boolean changed = false;
        String category = asset.category == null ? "" : asset.category;
        if ("银行".equals(category) || AssetCategories.BANK_DEPOSIT.equals(category)) {
            if (!hasText(asset.bankDepositAmount)) {
                asset.bankDepositAmount = asset.amount;
            }
            asset.category = AssetCategories.BANK_ACCOUNT;
            changed = true;
        } else if (AssetCategories.BANK_WEALTH.equals(category)) {
            if (!hasText(asset.bankWealthAmount)) {
                asset.bankWealthAmount = asset.amount;
            }
            asset.category = AssetCategories.BANK_ACCOUNT;
            changed = true;
        } else if ("券商".equals(category)
                || AssetCategories.BROKER_HOLDING.equals(category)
                || AssetCategories.STOCK_HOLDING.equals(category)) {
            if (!hasText(asset.investmentHoldingAmount)) {
                asset.investmentHoldingAmount = asset.amount;
            }
            asset.category = AssetCategories.INVESTMENT_ACCOUNT;
            changed = true;
        } else if (AssetCategories.BROKER_CASH.equals(category)) {
            if (!hasText(asset.investmentCashAmount)) {
                asset.investmentCashAmount = asset.amount;
            }
            asset.category = AssetCategories.INVESTMENT_ACCOUNT;
            changed = true;
        }

        if (AssetCategories.BANK_ACCOUNT.equals(asset.category)
                && !AssetMath.hasBankBreakdown(asset)
                && hasText(asset.amount)) {
            asset.bankDepositAmount = asset.amount;
            changed = true;
        }
        if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)
                && !AssetMath.hasInvestmentBreakdown(asset)
                && hasText(asset.amount)) {
            asset.investmentHoldingAmount = asset.amount;
            changed = true;
        }
        return changed;
    }

    private static boolean migrateAccountDetailsToInstitutionAssets(List<AssetRecord> assets) {
        boolean changed = false;
        List<AssetRecord> generatedAssets = new ArrayList<>();
        for (AssetRecord asset : assets) {
            changed = normalizeInstitution(asset) || changed;
            if (AssetCategories.BANK_ACCOUNT.equals(asset.category)) {
                changed = flattenBankAccount(asset, generatedAssets) || changed;
            } else if (AssetCategories.INVESTMENT_ACCOUNT.equals(asset.category)) {
                changed = flattenInvestmentAccount(asset) || changed;
            }
        }
        if (!generatedAssets.isEmpty()) {
            assets.addAll(generatedAssets);
            changed = true;
        }
        return changed;
    }

    private static boolean normalizeInstitution(AssetRecord asset) {
        String institution = asset.institution == null ? "" : asset.institution.trim();
        if (!institution.contains("待绑定")) {
            return false;
        }
        asset.institution = hasText(asset.appName) ? asset.appName.trim() : "";
        return true;
    }

    private static boolean flattenBankAccount(AssetRecord asset, List<AssetRecord> generatedAssets) {
        boolean hasBreakdown = AssetMath.hasBankBreakdown(asset);
        if (!hasBreakdown) {
            return false;
        }

        double deposit = positiveAmount(asset.bankDepositAmount);
        double wealth = positiveAmount(asset.bankWealthAmount);
        double debt = positiveAmount(asset.bankDebtAmount);
        double gross = deposit + wealth;
        if (gross > 0 || !hasText(asset.amount)) {
            asset.amount = formatStoredAmount(gross);
        }
        if (debt > 0) {
            generatedAssets.add(derivedDebtAsset(asset, debt));
        }
        asset.bankDepositAmount = "";
        asset.bankWealthAmount = "";
        asset.bankDebtAmount = "";
        return true;
    }

    private static boolean flattenInvestmentAccount(AssetRecord asset) {
        boolean hasBreakdown = AssetMath.hasInvestmentBreakdown(asset) || hasText(asset.investmentPositions);
        if (!hasBreakdown) {
            return false;
        }

        double holding = positiveAmount(asset.investmentHoldingAmount);
        double cash = positiveAmount(asset.investmentCashAmount);
        double total = holding + cash;
        if (total > 0 || !hasText(asset.amount)) {
            asset.amount = formatStoredAmount(total);
        }
        if (hasText(asset.investmentPositions)) {
            asset.note = appendNote(asset.note, "旧版持股备注：" + asset.investmentPositions.trim());
        }
        asset.investmentHoldingAmount = "";
        asset.investmentCashAmount = "";
        asset.investmentPositions = "";
        return true;
    }

    private static AssetRecord derivedDebtAsset(AssetRecord source, double debtAmount) {
        AssetRecord debt = new AssetRecord();
        String sourceName = hasText(source.name) ? source.name.trim() : "银行账户";
        debt.name = sourceName + "负债";
        debt.category = AssetCategories.DEBT;
        debt.institution = source.institution;
        debt.amount = formatStoredAmount(debtAmount);
        debt.currency = source.currency;
        debt.updateEveryDays = source.updateEveryDays;
        debt.lastUpdatedAt = source.lastUpdatedAt;
        debt.appName = source.appName;
        debt.packageName = source.packageName;
        debt.launchUri = source.launchUri;
        debt.note = appendNote(source.note, "由旧版银行账户负债明细迁移生成。");
        return debt;
    }

    private static double positiveAmount(String raw) {
        return Math.abs(AssetMath.parseAmount(raw));
    }

    private static String formatStoredAmount(double value) {
        return new DecimalFormat("0.##").format(Math.max(0, value));
    }

    private static String appendNote(String note, String addition) {
        String base = note == null ? "" : note.trim();
        String extra = addition == null ? "" : addition.trim();
        if (extra.isEmpty()) {
            return base;
        }
        if (base.isEmpty()) {
            return extra;
        }
        if (base.contains(extra)) {
            return base;
        }
        return base + "\n" + extra;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
