package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetInstitutionGroups;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetRecord;

import java.util.List;

final class InvestmentInstitutionRenderer {
    private final MainActivity activity;

    InvestmentInstitutionRenderer(MainActivity activity) {
        this.activity = activity;
    }

    void render(List<AssetInstitutionGroups.Group> groups, double total) {
        activity.investmentInstitutionList.removeAllViews();
        if (groups.isEmpty()) {
            activity.investmentInstitutionSummary.setText("暂无投资机构。新增投资资产后会按机构汇总。");
            activity.investmentInstitutionList.addView(activity.emptyText("还没有可展示的投资机构。"));
            return;
        }

        int dueNow = 0;
        int unbound = 0;
        for (AssetInstitutionGroups.Group group : groups) {
            InstitutionStats stats = statsFor(group);
            dueNow += stats.dueNow;
            unbound += stats.unbound;
        }

        AssetInstitutionGroups.Group top = groups.get(0);
        activity.investmentInstitutionSummary.setText("最大机构是 " + top.title
                + "，占投资资产 " + activity.formatPercent(top.total, total)
                + "；" + dueNow + " 项到期，" + unbound + " 项未绑定 App。");

        int limit = Math.min(5, groups.size());
        for (int index = 0; index < limit; index += 1) {
            activity.investmentInstitutionList.addView(institutionRow(groups.get(index), total));
        }
    }

    private View institutionRow(AssetInstitutionGroups.Group group, double total) {
        InstitutionStats stats = statsFor(group);
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(12));
        card.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams cardParams = activity.lp(-1, -2);
        cardParams.topMargin = activity.dp(8);
        card.setLayoutParams(cardParams);

        LinearLayout header = activity.row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(activity.institutionGroupIcon(group), new LinearLayout.LayoutParams(activity.dp(34), activity.dp(34)));

        LinearLayout titleGroup = new LinearLayout(activity);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.leftMargin = activity.dp(10);
        titleGroup.addView(activity.text(group.title, 14, MoneyManagerActivity.INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = activity.lp(-1, -2);
        metaParams.topMargin = activity.dp(3);
        titleGroup.addView(activity.text(group.assets.size() + " 项 · " + group.displayApps(),
                12, MoneyManagerActivity.MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, titleParams);

        String value = activity.settings.hideAmounts
                ? activity.formatPercent(group.total, total)
                : activity.formatMoney(group.total, activity.settings.baseCurrency)
                + "\n" + activity.formatPercent(group.total, total);
        TextView amount = activity.text(value, 12, MoneyManagerActivity.MUTED, Typeface.BOLD);
        amount.setGravity(Gravity.RIGHT);
        header.addView(amount);
        card.addView(header);

        addDetail(card, mixText(stats), MoneyManagerActivity.BLUE);
        addDetail(card, nextStepText(stats), nextStepColor(stats));
        addDetail(card, largestText(stats), MoneyManagerActivity.AMBER);
        addActions(card, stats);
        return card;
    }

    private void addDetail(LinearLayout card, String text, int color) {
        LinearLayout row = activity.row();
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(7);
        TextView dot = activity.text("●", 11, color, Typeface.BOLD);
        row.addView(dot);
        TextView body = activity.text("  " + text, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        row.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row, rowParams);
    }

    private void addActions(LinearLayout card, InstitutionStats stats) {
        LinearLayout actions = activity.row();
        LinearLayout.LayoutParams actionParams = activity.lp(-1, activity.dp(38));
        actionParams.topMargin = activity.dp(10);
        card.addView(actions, actionParams);

        Button launch = activity.secondaryButton("打开 App");
        launch.setTextSize(12);
        launch.setEnabled(stats.launchAsset != null);
        launch.setAlpha(stats.launchAsset == null ? 0.45f : 1f);
        launch.setOnClickListener(view -> {
            if (stats.launchAsset != null) {
                activity.openLinkedApp(stats.launchAsset);
            }
        });
        actions.addView(launch, new LinearLayout.LayoutParams(0, activity.dp(38), 1));

        addGap(actionParams, actions);

        Button update = activity.secondaryButton("核对下一项");
        update.setTextSize(12);
        update.setEnabled(stats.nextAsset != null);
        update.setAlpha(stats.nextAsset == null ? 0.45f : 1f);
        update.setOnClickListener(view -> {
            if (stats.nextAsset != null) {
                activity.showAssetUpdateDialog(stats.nextAsset);
            }
        });
        actions.addView(update, new LinearLayout.LayoutParams(0, activity.dp(38), 1));
    }

    private void addGap(LinearLayout.LayoutParams actionParams, LinearLayout actions) {
        View gap = new View(activity);
        actions.addView(gap, new LinearLayout.LayoutParams(activity.dp(8), actionParams.height));
    }

    private InstitutionStats statsFor(AssetInstitutionGroups.Group group) {
        InstitutionStats stats = new InstitutionStats();
        for (AssetRecord asset : group.assets) {
            double gross = amountInBase(asset, AssetMath.assetGrossAmount(asset));
            double holding = amountInBase(asset, AssetMath.investmentHoldingAmount(asset));
            double cash = amountInBase(asset, AssetMath.investmentCashAmount(asset));
            stats.total += gross;
            stats.holding += holding;
            stats.cash += cash;
            if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
                stats.unbound += 1;
            } else if (stats.launchAsset == null) {
                stats.launchAsset = asset;
            }

            int days = activity.daysUntilDue(asset);
            if (days <= 0) {
                stats.dueNow += 1;
            } else if (days <= 3) {
                stats.dueSoon += 1;
            }
            if (stats.nextAsset == null || dueRank(asset, gross) < dueRank(stats.nextAsset, stats.nextValue)) {
                stats.nextAsset = asset;
                stats.nextValue = gross;
            }
            if (stats.largestAsset == null || gross > stats.largestValue) {
                stats.largestAsset = asset;
                stats.largestValue = gross;
            }
        }
        return stats;
    }

    private double amountInBase(AssetRecord asset, double amount) {
        String currency = AssetMath.cleanCurrency(asset.currency);
        double rate = activity.settings.hasRateFor(currency) ? activity.settings.rateFor(currency) : 1.0;
        return amount * rate;
    }

    private long dueRank(AssetRecord asset, double value) {
        long days = activity.daysUntilDue(asset);
        long amountRank = Math.max(0, 1_000_000_000L - Math.round(Math.min(value, 1_000_000_000L)));
        return days * 1_000_000_000L + amountRank;
    }

    private String mixText(InstitutionStats stats) {
        double cashRatio = stats.total <= 0 ? 0 : stats.cash / stats.total * 100;
        double holdingRatio = stats.total <= 0 ? 0 : stats.holding / stats.total * 100;
        if (activity.settings.hideAmounts) {
            return "持仓 " + activity.formatPercentValue(holdingRatio)
                    + "，现金 " + activity.formatPercentValue(cashRatio) + "。";
        }
        return "持仓 " + activity.formatMoney(stats.holding, activity.settings.baseCurrency)
                + "，现金 " + activity.formatMoney(stats.cash, activity.settings.baseCurrency)
                + "（" + activity.formatPercentValue(cashRatio) + "）。";
    }

    private String nextStepText(InstitutionStats stats) {
        String freshness;
        if (stats.dueNow > 0) {
            freshness = stats.dueNow + " 项已到期";
        } else if (stats.dueSoon > 0) {
            freshness = stats.dueSoon + " 项 3 天内到期";
        } else {
            freshness = "核对节奏正常";
        }
        String binding = stats.unbound == 0 ? "App 已绑定" : stats.unbound + " 项未绑定 App";
        String next = stats.nextAsset == null ? "暂无下一项" : "下一项：" + stats.nextAsset.name;
        return freshness + "，" + binding + "；" + next + "。";
    }

    private int nextStepColor(InstitutionStats stats) {
        if (stats.dueNow > 0) {
            return MoneyManagerActivity.DANGER;
        }
        if (stats.dueSoon > 0 || stats.unbound > 0) {
            return MoneyManagerActivity.AMBER;
        }
        return MoneyManagerActivity.ACCENT;
    }

    private String largestText(InstitutionStats stats) {
        if (stats.largestAsset == null) {
            return "暂无最大项。";
        }
        if (activity.settings.hideAmounts) {
            return "最大项：" + stats.largestAsset.name + "，金额已隐藏。";
        }
        return "最大项：" + stats.largestAsset.name + "，"
                + activity.formatMoney(stats.largestValue, activity.settings.baseCurrency) + "。";
    }

    private static final class InstitutionStats {
        double total;
        double holding;
        double cash;
        double nextValue;
        double largestValue;
        int dueNow;
        int dueSoon;
        int unbound;
        AssetRecord launchAsset;
        AssetRecord nextAsset;
        AssetRecord largestAsset;
    }
}
