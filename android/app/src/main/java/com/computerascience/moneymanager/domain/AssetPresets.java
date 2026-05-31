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
        return asset;
    }

    private static List<Preset> buildQuickAddPresets() {
        List<Preset> presets = new ArrayList<>();
        presets.add(new Preset(
                "银行账户",
                "银行账户",
                AssetCategories.BANK_ACCOUNT,
                "",
                7,
                "按机构记录银行账户总额，需要核对时打开绑定的银行 App。"
        ));
        presets.add(new Preset(
                "投资账户",
                "投资账户",
                AssetCategories.INVESTMENT_ACCOUNT,
                "",
                1,
                "按机构记录投资账户总额；持仓明细可写在备注里。"
        ));
        presets.add(new Preset(
                "负债",
                "其他负债",
                AssetCategories.DEBT,
                "",
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
