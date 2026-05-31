package com.computerascience.moneymanager.domain;

public final class AssetCategories {
    public static final String BANK_ACCOUNT = "银行账户";
    public static final String INVESTMENT_ACCOUNT = "投资账户";
    public static final String BANK_DEPOSIT = "银行存款";
    public static final String BANK_WEALTH = "银行理财";
    public static final String BROKER_HOLDING = "券商持仓";
    public static final String STOCK_HOLDING = "股票持仓";
    public static final String BROKER_CASH = "券商现金";
    public static final String FUND = "基金";
    public static final String CRYPTO = "加密资产";
    public static final String REAL_ESTATE = "房产";
    public static final String DEBT = "负债";
    public static final String OTHER = "其他";

    public static final String[] ALL = {
            BANK_ACCOUNT,
            INVESTMENT_ACCOUNT,
            FUND,
            CRYPTO,
            REAL_ESTATE,
            DEBT,
            OTHER
    };

    public static boolean isBankAccount(String category) {
        return BANK_ACCOUNT.equals(category);
    }

    public static boolean isInvestmentAccount(String category) {
        return INVESTMENT_ACCOUNT.equals(category);
    }

    public static boolean isLegacyBankCategory(String category) {
        return "银行".equals(category)
                || BANK_DEPOSIT.equals(category)
                || BANK_WEALTH.equals(category);
    }

    public static boolean isLegacyInvestmentCategory(String category) {
        return "券商".equals(category)
                || BROKER_HOLDING.equals(category)
                || STOCK_HOLDING.equals(category)
                || BROKER_CASH.equals(category);
    }

    private AssetCategories() {
    }
}
