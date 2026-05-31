package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.AssetRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AssetPresets {
    private static final List<Preset> QUICK_ADD_PRESETS = buildQuickAddPresets();

    public static List<Preset> quickAddPresets() {
        return QUICK_ADD_PRESETS;
    }

    public static AssetRecord createRecord(Preset preset, String currency) {
        AssetRecord asset = new AssetRecord();
        asset.name = preset.name;
        asset.category = preset.category;
        asset.institution = preset.institution;
        asset.amount = "0";
        asset.currency = currency == null || currency.trim().isEmpty() ? "CNY" : currency.trim().toUpperCase();
        asset.updateEveryDays = preset.updateEveryDays;
        asset.note = preset.note;
        if (AssetCategories.BANK_ACCOUNT.equals(preset.category)) {
            asset.bankDepositAmount = "0";
            asset.bankWealthAmount = "0";
            asset.bankDebtAmount = "0";
        } else if (AssetCategories.INVESTMENT_ACCOUNT.equals(preset.category)) {
            asset.investmentHoldingAmount = "0";
            asset.investmentCashAmount = "0";
        }
        return asset;
    }

    private static List<Preset> buildQuickAddPresets() {
        List<Preset> presets = new ArrayList<>();
        presets.add(new Preset(
                "银行账户",
                "银行账户",
                AssetCategories.BANK_ACCOUNT,
                "待绑定银行 App",
                7,
                "在一个银行账户里填写存款、理财和负债，只需要绑定一次银行 App。"
        ));
        presets.add(new Preset(
                "投资账户",
                "投资账户",
                AssetCategories.INVESTMENT_ACCOUNT,
                "待绑定券商 App",
                1,
                "填写持仓市值、可用现金和持股备注，只需要绑定一次券商 App。"
        ));
        presets.add(new Preset(
                "负债",
                "其他负债",
                AssetCategories.DEBT,
                "待绑定 App",
                30,
                "记录不属于某个银行账户的借款或应付款，金额填正数。"
        ));
        return Collections.unmodifiableList(presets);
    }

    private AssetPresets() {
    }

    public static final class Preset {
        public final String label;
        public final String name;
        public final String category;
        public final String institution;
        public final int updateEveryDays;
        public final String note;

        private Preset(
                String label,
                String name,
                String category,
                String institution,
                int updateEveryDays,
                String note
        ) {
            this.label = label;
            this.name = name;
            this.category = category;
            this.institution = institution;
            this.updateEveryDays = updateEveryDays;
            this.note = note;
        }
    }
}
