package com.computerascience.moneymanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_BACKUP = 4101;
    private static final int REQUEST_IMPORT_BACKUP = 4102;
    private static final String[] CATEGORIES = {"银行", "券商", "基金", "加密资产", "房产", "负债", "其他"};
    private static final int BG = Color.rgb(245, 246, 241);
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(30, 35, 32);
    private static final int MUTED = Color.rgb(102, 112, 104);
    private static final int LINE = Color.rgb(217, 221, 213);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int DANGER = Color.rgb(183, 73, 85);
    private static final int AMBER = Color.rgb(154, 119, 32);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private AssetStore store;
    private List<AssetRecord> assets = new ArrayList<>();
    private List<AssetSnapshot> snapshots = new ArrayList<>();
    private PortfolioSettings settings;
    private LinearLayout assetList;
    private LinearLayout allocationLegend;
    private LinearLayout institutionList;
    private LinearLayout updatePlanList;
    private LinearLayout managementBody;
    private AllocationChartView allocationChart;
    private TrendChartView trendChart;
    private TextView netWorthValue;
    private TextView grossAssetsValue;
    private TextView liabilitiesValue;
    private TextView freshnessValue;
    private Button privacyToggle;
    private TextView currencyNote;
    private TextView currencySettingsSummary;
    private TextView trendSummary;
    private LinearLayout trendHistoryList;
    private TextView insightSummary;
    private TextView updatePlanSummary;
    private TextView managementSummary;
    private TextView assetResultSummary;
    private EditText assetSearchInput;
    private LinearLayout assetFilterButtons;
    private Button managementToggle;
    private String pendingLaunchAssetId;
    private boolean waitingForExternalReturn;
    private boolean managementExpanded = true;
    private String assetSearchQuery = "";
    private String assetFilterMode = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AssetStore(this);
        assets = store.load();
        settings = store.loadSettings();
        snapshots = store.loadSnapshots();
        if (snapshots.isEmpty()) {
            snapshots = store.recordSnapshot(assets, settings);
        }
        buildUi();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pendingLaunchAssetId != null) {
            waitingForExternalReturn = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForExternalReturn && pendingLaunchAssetId != null) {
            String assetId = pendingLaunchAssetId;
            pendingLaunchAssetId = null;
            waitingForExternalReturn = false;
            AssetRecord asset = findAsset(assetId);
            if (asset != null) {
                showMarkUpdatedDialog(asset);
            }
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = label("Money Manager");
        root.addView(eyebrow);

        TextView title = text("总资产", 28, INK, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("看总额、比例和一年趋势；资产更新入口也放在这里。", 15, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = lp(-1, -2);
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(18);
        root.addView(subtitle, subtitleParams);

        root.addView(overviewCard());
        root.addView(currencyCard());
        root.addView(allocationCard());
        root.addView(institutionCard());
        root.addView(trendCard());
        root.addView(insightCard());
        root.addView(updatePlanCard());
        root.addView(backupCard());
        root.addView(assetManagementSection());

        setContentView(scrollView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        if (requestCode == REQUEST_EXPORT_BACKUP) {
            writeBackup(data.getData());
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            readBackup(data.getData());
        }
    }

    private void render() {
        PortfolioSummary portfolio = AssetMath.summarize(assets, settings);

        netWorthValue.setText(formatMoney(portfolio.netWorth, portfolio.baseCurrency));
        grossAssetsValue.setText(formatMoney(portfolio.grossAssets, portfolio.baseCurrency));
        liabilitiesValue.setText(formatMoney(portfolio.liabilities, portfolio.baseCurrency));
        freshnessValue.setText(portfolio.staleCount + " 项待更新");
        privacyToggle.setText(settings.hideAmounts ? "显示金额" : "隐藏金额");
        currencyNote.setText(currencyNoteText(portfolio));
        currencySettingsSummary.setText(currencySettingsText());

        allocationChart.setCategories(portfolio.categories);
        renderAllocationLegend(portfolio);
        renderInstitutionList(portfolio);

        List<AssetSnapshot> trendSnapshots = snapshotsForBase(portfolio.baseCurrency);
        trendChart.setSnapshots(trendSnapshots);
        trendSummary.setText(trendSummaryText(portfolio, trendSnapshots));
        renderTrendHistory(trendSnapshots);

        insightSummary.setText(buildInsightText(portfolio));
        renderUpdatePlan();

        managementSummary.setText("共 " + portfolio.assetCount + " 项资产，"
                + portfolio.staleCount + " 项需要更新，"
                + portfolio.missingBindingCount + " 项还没绑定 App。");
        managementToggle.setText(managementExpanded ? "折叠" : "展开");
        managementBody.setVisibility(managementExpanded ? View.VISIBLE : View.GONE);

        renderAssetFilterButtons();
        List<AssetRecord> visibleAssets = visibleAssets();
        assetResultSummary.setText("显示 " + visibleAssets.size() + " / " + assets.size()
                + " 项，按待更新和金额优先排序。");

        assetList.removeAllViews();
        for (AssetRecord asset : visibleAssets) {
            assetList.addView(assetCard(asset));
        }

        if (visibleAssets.isEmpty()) {
            String message = assets.isEmpty()
                    ? "还没有资产。先新增一项，再绑定对应 App。"
                    : "没有匹配的资产。换个关键词或筛选条件试试。";
            TextView empty = text(message, 16, MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            assetList.addView(empty, lp(-1, -2));
        }
    }

    private View overviewCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("总资产概览"));

        netWorthValue = text("--", 34, INK, Typeface.BOLD);
        LinearLayout.LayoutParams netParams = lp(-1, -2);
        netParams.topMargin = dp(10);
        card.addView(netWorthValue, netParams);

        currencyNote = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams noteParams = lp(-1, -2);
        noteParams.topMargin = dp(8);
        noteParams.bottomMargin = dp(10);
        card.addView(currencyNote, noteParams);

        privacyToggle = secondaryButton("隐藏金额");
        privacyToggle.setOnClickListener(view -> {
            settings.hideAmounts = !settings.hideAmounts;
            store.saveSettings(settings);
            render();
        });
        LinearLayout.LayoutParams privacyParams = lp(-1, dp(42));
        privacyParams.bottomMargin = dp(14);
        card.addView(privacyToggle, privacyParams);

        LinearLayout row1 = row();
        grossAssetsValue = text("--", 18, INK, Typeface.BOLD);
        liabilitiesValue = text("--", 18, INK, Typeface.BOLD);
        row1.addView(metric("资产总额", grossAssetsValue), new LinearLayout.LayoutParams(0, -2, 1));
        row1.addView(new SpaceView(this, dp(10), 1));
        row1.addView(metric("负债", liabilitiesValue), new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(row1);

        freshnessValue = text("--", 18, INK, Typeface.BOLD);
        LinearLayout.LayoutParams freshParams = lp(-1, -2);
        freshParams.topMargin = dp(10);
        card.addView(metric("更新状态", freshnessValue), freshParams);
        return card;
    }

    private View currencyCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("基准币种与汇率"));

        currencySettingsSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(12);
        card.addView(currencySettingsSummary, summaryParams);

        Button editButton = secondaryButton("编辑汇率");
        editButton.setOnClickListener(view -> showCurrencySettingsDialog());
        card.addView(editButton, lp(-1, dp(44)));
        return card;
    }

    private View allocationCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("资产比例"));

        LinearLayout body = row();
        LinearLayout.LayoutParams bodyParams = lp(-1, -2);
        bodyParams.topMargin = dp(12);
        body.setLayoutParams(bodyParams);

        allocationChart = new AllocationChartView(this);
        body.addView(allocationChart, new LinearLayout.LayoutParams(dp(148), dp(148)));

        allocationLegend = new LinearLayout(this);
        allocationLegend.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams legendParams = new LinearLayout.LayoutParams(0, -2, 1);
        legendParams.leftMargin = dp(14);
        body.addView(allocationLegend, legendParams);
        card.addView(body);
        return card;
    }

    private View institutionCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("机构分布"));

        TextView description = text("按银行、券商或钱包汇总，方便核对资金主要放在哪里。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(8);
        descriptionParams.bottomMargin = dp(8);
        card.addView(description, descriptionParams);

        institutionList = new LinearLayout(this);
        institutionList.setOrientation(LinearLayout.VERTICAL);
        card.addView(institutionList, lp(-1, -2));
        return card;
    }

    private View trendCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("一年变化趋势"));

        trendSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        card.addView(trendSummary, summaryParams);

        trendChart = new TrendChartView(this);
        LinearLayout.LayoutParams chartParams = lp(-1, dp(190));
        chartParams.topMargin = dp(10);
        card.addView(trendChart, chartParams);

        Button snapshotButton = secondaryButton("记录今日快照");
        snapshotButton.setOnClickListener(view -> {
            snapshots = store.recordSnapshot(assets, settings);
            render();
            toast("已记录今日总资产快照。");
        });
        LinearLayout.LayoutParams buttonParams = lp(-1, dp(44));
        buttonParams.topMargin = dp(8);
        buttonParams.bottomMargin = dp(12);
        card.addView(snapshotButton, buttonParams);

        trendHistoryList = new LinearLayout(this);
        trendHistoryList.setOrientation(LinearLayout.VERTICAL);
        card.addView(trendHistoryList, lp(-1, -2));
        return card;
    }

    private View insightCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("待办提醒"));
        insightSummary = text("", 15, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.topMargin = dp(10);
        card.addView(insightSummary, params);
        return card;
    }

    private View updatePlanCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("更新计划"));

        updatePlanSummary = text("", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(8);
        summaryParams.bottomMargin = dp(8);
        card.addView(updatePlanSummary, summaryParams);

        updatePlanList = new LinearLayout(this);
        updatePlanList.setOrientation(LinearLayout.VERTICAL);
        card.addView(updatePlanList, lp(-1, -2));
        return card;
    }

    private View backupCard() {
        LinearLayout card = card();
        card.addView(sectionTitle("数据备份"));

        TextView description = text("导出会保存资产、App 绑定、更新时间和趋势快照；导入会覆盖当前本机数据。", 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams descriptionParams = lp(-1, -2);
        descriptionParams.topMargin = dp(8);
        descriptionParams.bottomMargin = dp(12);
        card.addView(description, descriptionParams);

        LinearLayout actions = row();
        Button exportButton = secondaryButton("导出备份");
        exportButton.setOnClickListener(view -> startBackupExport());
        actions.addView(exportButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        actions.addView(new SpaceView(this, dp(10), 1));

        Button importButton = secondaryButton("导入备份");
        importButton.setOnClickListener(view -> startBackupImport());
        actions.addView(importButton, new LinearLayout.LayoutParams(0, dp(44), 1));
        card.addView(actions);
        return card;
    }

    private View assetManagementSection() {
        LinearLayout card = card();

        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(sectionTitle("资产管理"));
        managementSummary = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.topMargin = dp(4);
        titleGroup.addView(managementSummary, summaryParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        managementToggle = secondaryButton("折叠");
        managementToggle.setOnClickListener(view -> {
            managementExpanded = !managementExpanded;
            render();
        });
        header.addView(managementToggle, new LinearLayout.LayoutParams(dp(86), dp(42)));
        card.addView(header);

        managementBody = new LinearLayout(this);
        managementBody.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams bodyParams = lp(-1, -2);
        bodyParams.topMargin = dp(14);
        card.addView(managementBody, bodyParams);

        assetSearchInput = input("搜索资产、机构、备注", "", InputType.TYPE_CLASS_TEXT);
        assetSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                assetSearchQuery = text == null ? "" : text.toString().trim();
                render();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        managementBody.addView(assetSearchInput);

        assetFilterButtons = new LinearLayout(this);
        assetFilterButtons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams filterParams = lp(-1, -2);
        filterParams.bottomMargin = dp(10);
        managementBody.addView(assetFilterButtons, filterParams);

        assetResultSummary = text("", 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams resultParams = lp(-1, -2);
        resultParams.bottomMargin = dp(10);
        managementBody.addView(assetResultSummary, resultParams);

        Button addButton = primaryButton("新增资产");
        addButton.setOnClickListener(view -> showEditDialog(null));
        LinearLayout.LayoutParams addParams = lp(-1, dp(48));
        addParams.bottomMargin = dp(14);
        managementBody.addView(addButton, addParams);

        assetList = new LinearLayout(this);
        assetList.setOrientation(LinearLayout.VERTICAL);
        managementBody.addView(assetList, lp(-1, -2));
        return card;
    }

    private void renderAllocationLegend(PortfolioSummary portfolio) {
        allocationLegend.removeAllViews();
        double total = portfolio.grossAssets + portfolio.liabilities;
        if (portfolio.categories.isEmpty() || total <= 0) {
            allocationLegend.addView(text("暂无可展示的资产比例。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        for (CategoryBreakdown category : portfolio.categories) {
            LinearLayout row = row();
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = lp(-1, -2);
            rowParams.bottomMargin = dp(8);
            row.setLayoutParams(rowParams);

            TextView dot = text("●", 16, category.color, Typeface.BOLD);
            row.addView(dot);

            TextView label = text("  " + category.category, 14, INK, Typeface.BOLD);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            double ratio = category.value / total * 100;
            TextView value = text(String.format(Locale.getDefault(), "%.1f%%", ratio), 14, MUTED, Typeface.BOLD);
            row.addView(value);
            allocationLegend.addView(row);
        }
    }

    private void renderInstitutionList(PortfolioSummary portfolio) {
        institutionList.removeAllViews();
        double total = portfolio.grossAssets + portfolio.liabilities;
        if (portfolio.institutions.isEmpty() || total <= 0) {
            institutionList.addView(text("暂无可展示的机构分布。", 14, MUTED, Typeface.NORMAL));
            return;
        }

        int limit = Math.min(5, portfolio.institutions.size());
        for (int index = 0; index < limit; index += 1) {
            InstitutionBreakdown institution = portfolio.institutions.get(index);
            institutionList.addView(institutionRow(institution, total));
        }
    }

    private View institutionRow(InstitutionBreakdown institution, double total) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout labelGroup = new LinearLayout(this);
        labelGroup.setOrientation(LinearLayout.VERTICAL);
        labelGroup.addView(text(institution.institution, 14, INK, Typeface.BOLD));

        LinearLayout.LayoutParams countParams = lp(-1, -2);
        countParams.topMargin = dp(4);
        labelGroup.addView(text(institution.assetCount + " 项资产", 12, MUTED, Typeface.NORMAL), countParams);
        row.addView(labelGroup, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout valueGroup = new LinearLayout(this);
        valueGroup.setOrientation(LinearLayout.VERTICAL);
        valueGroup.setGravity(Gravity.END);
        valueGroup.addView(text(formatMoney(institution.value, settings.baseCurrency), 14, INK, Typeface.BOLD));

        LinearLayout.LayoutParams ratioParams = lp(-1, -2);
        ratioParams.topMargin = dp(4);
        TextView ratio = text(formatPercent(institution.value, total), 12, MUTED, Typeface.NORMAL);
        ratio.setGravity(Gravity.END);
        valueGroup.addView(ratio, ratioParams);
        row.addView(valueGroup);
        return row;
    }

    private String currencyNoteText(PortfolioSummary portfolio) {
        if (portfolio.missingRateCount > 0) {
            return "总额以 " + portfolio.baseCurrency + " 显示；"
                    + portfolio.missingRateCount + " 项资产缺少汇率，暂按 1:1 估算。";
        }
        if (portfolio.hasMixedCurrencies) {
            return "总额以 " + portfolio.baseCurrency + " 显示，多币种资产已按本地汇率换算。";
        }
        return "总额以 " + portfolio.baseCurrency + " 显示。";
    }

    private String currencySettingsText() {
        List<String> rows = new ArrayList<>();
        rows.add("基准：" + settings.baseCurrency);
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            if (currency.equals(settings.baseCurrency)) {
                continue;
            }
            double rate = settings.rateFor(currency);
            if (rate > 0) {
                rows.add("1 " + currency + " = " + formatRate(rate) + " " + settings.baseCurrency);
            }
        }
        return joinLines(rows);
    }

    private void showCurrencySettingsDialog() {
        PortfolioSettings draft = PortfolioSettings.copyOf(settings);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        EditText baseCurrency = input("基准币种", draft.baseCurrency, InputType.TYPE_CLASS_TEXT);
        form.addView(baseCurrency);

        List<CurrencyRateField> rateFields = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            EditText rateInput = input("1 " + currency + " 等于多少基准币种", formatRate(draft.rateFor(currency)), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rateFields.add(new CurrencyRateField(currency, rateInput));
            form.addView(rateInput);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("编辑汇率")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                String base = PortfolioSettings.cleanCurrency(baseCurrency.getText().toString());
                if (base.isEmpty()) {
                    toast("基准币种不能为空。");
                    return;
                }

                draft.baseCurrency = base;
                for (CurrencyRateField field : rateFields) {
                    double rate = parsePositiveDouble(field.input.getText().toString(), field.currency.equals(base) ? 1.0 : 0.0);
                    if (field.currency.equals(base)) {
                        rate = 1.0;
                    }
                    draft.setRate(field.currency, rate);
                }
                draft.ensureBaseRate();

                settings = draft;
                store.saveSettings(settings);
                snapshots = store.recordSnapshot(assets, settings);
                render();
                toast("汇率已更新。");
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void renderAssetFilterButtons() {
        assetFilterButtons.removeAllViews();
        assetFilterButtons.addView(assetFilterButton("全部", "all"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("待更新", "stale"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("未绑定", "unbound"), new LinearLayout.LayoutParams(0, dp(40), 1));
        assetFilterButtons.addView(new SpaceView(this, dp(8), 1));
        assetFilterButtons.addView(assetFilterButton("负债", "debt"), new LinearLayout.LayoutParams(0, dp(40), 1));
    }

    private Button assetFilterButton(String label, String mode) {
        Button button = secondaryButton(label);
        if (assetFilterMode.equals(mode)) {
            button.setTextColor(Color.WHITE);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ACCENT);
            bg.setCornerRadius(dp(8));
            button.setBackground(bg);
        }
        button.setOnClickListener(view -> {
            assetFilterMode = mode;
            render();
        });
        return button;
    }

    private List<AssetRecord> visibleAssets() {
        List<AssetRecord> visible = new ArrayList<>();
        for (AssetRecord asset : assets) {
            if (matchesAssetQuery(asset) && matchesAssetFilter(asset)) {
                visible.add(asset);
            }
        }
        Collections.sort(visible, (left, right) -> {
            int leftPriority = assetPriority(left);
            int rightPriority = assetPriority(right);
            if (leftPriority != rightPriority) {
                return Integer.compare(leftPriority, rightPriority);
            }
            int amountCompare = Double.compare(
                    Math.abs(AssetMath.parseAmount(right.amount)),
                    Math.abs(AssetMath.parseAmount(left.amount))
            );
            if (amountCompare != 0) {
                return amountCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return visible;
    }

    private boolean matchesAssetQuery(AssetRecord asset) {
        String query = assetSearchQuery.toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        return asset.name.toLowerCase(Locale.ROOT).contains(query)
                || asset.category.toLowerCase(Locale.ROOT).contains(query)
                || asset.institution.toLowerCase(Locale.ROOT).contains(query)
                || asset.currency.toLowerCase(Locale.ROOT).contains(query)
                || asset.note.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesAssetFilter(AssetRecord asset) {
        if ("stale".equals(assetFilterMode)) {
            return isStale(asset);
        }
        if ("unbound".equals(assetFilterMode)) {
            return asset.packageName.isEmpty() && asset.launchUri.isEmpty();
        }
        if ("debt".equals(assetFilterMode)) {
            return AssetMath.isLiability(asset);
        }
        return true;
    }

    private int assetPriority(AssetRecord asset) {
        if (isStale(asset)) {
            return 0;
        }
        if (asset.packageName.isEmpty() && asset.launchUri.isEmpty()) {
            return 1;
        }
        return 2;
    }

    private List<AssetSnapshot> snapshotsForBase(String baseCurrency) {
        List<AssetSnapshot> filtered = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (baseCurrency.equals(snapshot.baseCurrency)) {
                filtered.add(snapshot);
            }
        }
        return filtered;
    }

    private String trendSummaryText(PortfolioSummary portfolio, List<AssetSnapshot> trendSnapshots) {
        if (trendSnapshots.size() < 2) {
            return "当前基准 " + portfolio.baseCurrency + " 已记录 " + trendSnapshots.size()
                    + " 个快照。每天或每次核对后记录一次，趋势会逐渐形成。";
        }
        if (settings.hideAmounts) {
            return "近一年记录 " + trendSnapshots.size() + " 个 " + portfolio.baseCurrency
                    + " 快照。隐私模式已开启，金额变化暂不显示。";
        }
        AssetSnapshot first = trendSnapshots.get(0);
        AssetSnapshot last = trendSnapshots.get(trendSnapshots.size() - 1);
        double change = last.netWorth - first.netWorth;
        double ratio = Math.abs(first.netWorth) < 0.0001 ? 0 : change / Math.abs(first.netWorth) * 100;
        return "近一年记录 " + trendSnapshots.size() + " 个 " + portfolio.baseCurrency + " 快照，净资产变化 "
                + formatSignedMoney(change, portfolio.baseCurrency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private void renderTrendHistory(List<AssetSnapshot> trendSnapshots) {
        trendHistoryList.removeAllViews();
        trendHistoryList.addView(text("最近快照", 13, MUTED, Typeface.BOLD));

        if (trendSnapshots.isEmpty()) {
            TextView empty = text("暂无快照。记录一次后会出现在这里。", 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = lp(-1, -2);
            emptyParams.topMargin = dp(8);
            trendHistoryList.addView(empty, emptyParams);
            return;
        }

        int start = Math.max(0, trendSnapshots.size() - 6);
        for (int index = trendSnapshots.size() - 1; index >= start; index -= 1) {
            AssetSnapshot snapshot = trendSnapshots.get(index);
            trendHistoryList.addView(snapshotRow(snapshot));
        }
    }

    private View snapshotRow(AssetSnapshot snapshot) {
        LinearLayout row = row();
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout textGroup = new LinearLayout(this);
        textGroup.setOrientation(LinearLayout.VERTICAL);
        textGroup.addView(text(snapshot.dayKey + " · " + snapshot.baseCurrency, 14, INK, Typeface.BOLD));

        String details = "净资产 " + formatMoney(snapshot.netWorth, snapshot.baseCurrency)
                + " · 资产 " + formatMoney(snapshot.grossAssets, snapshot.baseCurrency)
                + " · 负债 " + formatMoney(snapshot.liabilities, snapshot.baseCurrency);
        LinearLayout.LayoutParams detailsParams = lp(-1, -2);
        detailsParams.topMargin = dp(4);
        textGroup.addView(text(details, 12, MUTED, Typeface.NORMAL), detailsParams);
        row.addView(textGroup, new LinearLayout.LayoutParams(0, -2, 1));

        Button delete = secondaryButton("删除");
        delete.setTextColor(DANGER);
        delete.setOnClickListener(view -> confirmDeleteSnapshot(snapshot));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(72), dp(38));
        deleteParams.leftMargin = dp(10);
        row.addView(delete, deleteParams);
        return row;
    }

    private void confirmDeleteSnapshot(AssetSnapshot snapshot) {
        new AlertDialog.Builder(this)
                .setTitle("删除快照？")
                .setMessage("确定删除 " + snapshot.dayKey + " 的 " + snapshot.baseCurrency + " 快照吗？趋势图会立刻更新。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    snapshots = store.deleteSnapshot(snapshot.dayKey, snapshot.baseCurrency);
                    render();
                    toast("已删除趋势快照。");
                })
                .show();
    }

    private String buildInsightText(PortfolioSummary portfolio) {
        List<String> lines = new ArrayList<>();
        if (!portfolio.categories.isEmpty()) {
            CategoryBreakdown largest = portfolio.categories.get(0);
            double base = portfolio.grossAssets + portfolio.liabilities;
            double ratio = base <= 0 ? 0 : largest.value / base * 100;
            lines.add("最大类别：" + largest.category + "，占比 "
                    + String.format(Locale.getDefault(), "%.1f", ratio) + "%。");
        }
        if (portfolio.staleCount > 0) {
            lines.add(portfolio.staleCount + " 项资产已到更新周期。");
        } else {
            lines.add("所有资产都在更新周期内。");
        }
        if (portfolio.missingBindingCount > 0) {
            lines.add(portfolio.missingBindingCount + " 项资产还没绑定 App，可在资产管理里选择已安装 App。");
        }
        if (portfolio.grossAssets > 0 && portfolio.liabilities / portfolio.grossAssets > 0.4) {
            lines.add("负债率偏高，建议单独关注还款节奏。");
        }
        if (portfolio.missingRateCount > 0) {
            lines.add(portfolio.missingRateCount + " 项资产缺少汇率，建议在“基准币种与汇率”里补齐。");
        }
        if (portfolio.hasMixedCurrencies) {
            lines.add("当前存在多币种资产，总额按本地汇率换算。");
        }
        return joinLines(lines);
    }

    private void renderUpdatePlan() {
        updatePlanList.removeAllViews();
        if (assets.isEmpty()) {
            updatePlanSummary.setText("还没有资产。新增资产后，这里会按更新周期自动排计划。");
            return;
        }

        List<AssetRecord> planned = plannedAssets();
        int urgentCount = 0;
        int soonCount = 0;
        for (AssetRecord asset : assets) {
            int days = daysUntilDue(asset);
            if (days <= 0) {
                urgentCount += 1;
            } else if (days <= 3) {
                soonCount += 1;
            }
        }

        updatePlanSummary.setText(urgentCount + " 项需要现在核对，"
                + soonCount + " 项将在 3 天内到期。");

        int limit = Math.min(5, planned.size());
        for (int index = 0; index < limit; index += 1) {
            updatePlanList.addView(updatePlanRow(planned.get(index)));
        }
    }

    private List<AssetRecord> plannedAssets() {
        List<AssetRecord> planned = new ArrayList<>(assets);
        Collections.sort(planned, (left, right) -> {
            int daysCompare = Integer.compare(daysUntilDue(left), daysUntilDue(right));
            if (daysCompare != 0) {
                return daysCompare;
            }
            int amountCompare = Double.compare(
                    Math.abs(AssetMath.parseAmount(right.amount)),
                    Math.abs(AssetMath.parseAmount(left.amount))
            );
            if (amountCompare != 0) {
                return amountCompare;
            }
            return left.name.compareToIgnoreCase(right.name);
        });
        return planned;
    }

    private View updatePlanRow(AssetRecord asset) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(12));
        row.setBackground(cardBackground(0xFFF8FAF5, LINE));
        LinearLayout.LayoutParams rowParams = lp(-1, -2);
        rowParams.topMargin = dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = row();
        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        titleGroup.addView(text(asset.name, 14, INK, Typeface.BOLD));
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(4);
        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        titleGroup.addView(text(asset.category + " · " + institution, 12, MUTED, Typeface.NORMAL), metaParams);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView due = updateDueChip(asset);
        header.addView(due);
        row.addView(header);

        LinearLayout actions = row();
        LinearLayout.LayoutParams actionsParams = lp(-1, -2);
        actionsParams.topMargin = dp(10);
        actions.setLayoutParams(actionsParams);

        Button launch = secondaryButton("打开 App");
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(40), 1));
        actions.addView(new SpaceView(this, dp(8), 1));

        Button mark = secondaryButton("已更新");
        mark.setOnClickListener(view -> markUpdated(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, dp(40), 1));
        row.addView(actions);
        return row;
    }

    private TextView updateDueChip(AssetRecord asset) {
        int days = daysUntilDue(asset);
        String label;
        int color;
        if (asset.lastUpdatedAt <= 0) {
            label = "从未更新";
            color = AMBER;
        } else if (days < 0) {
            label = "逾期 " + Math.abs(days) + " 天";
            color = DANGER;
        } else if (days == 0) {
            label = "今天到期";
            color = DANGER;
        } else if (days <= 3) {
            label = days + " 天后到期";
            color = AMBER;
        } else {
            label = days + " 天后";
            color = ACCENT;
        }

        TextView chip = text(label, 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(cardBackground(PANEL, LINE));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        return card;
    }

    private TextView sectionTitle(String title) {
        return text(title, 18, INK, Typeface.BOLD);
    }

    private LinearLayout metric(String label, TextView value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(cardBackground(0xFFF8FAF5, LINE));
        box.addView(text(label, 12, MUTED, Typeface.BOLD));
        LinearLayout.LayoutParams valueParams = lp(-1, -2);
        valueParams.topMargin = dp(6);
        box.addView(value, valueParams);
        return box;
    }

    private View assetCard(AssetRecord asset) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        card.setBackground(cardBackground(PANEL, statusColor(asset)));

        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = text(asset.name, 19, INK, Typeface.BOLD);
        titleGroup.addView(name);

        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        TextView meta = text(asset.category + " · " + institution, 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(3);
        titleGroup.addView(meta, metaParams);

        TextView status = statusChip(asset);
        header.addView(status);
        card.addView(header);

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valueParams = lp(-1, -2);
        valueParams.topMargin = dp(16);
        card.addView(valueRow, valueParams);

        TextView amount = text(formatAmount(asset), 25, INK, Typeface.BOLD);
        valueRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView cadence = text("每 " + asset.updateEveryDays + " 天更新", 13, MUTED, Typeface.BOLD);
        valueRow.addView(cadence);

        TextView updated = text(lastUpdatedText(asset), 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams updatedParams = lp(-1, -2);
        updatedParams.topMargin = dp(10);
        card.addView(updated, updatedParams);

        if (!asset.note.isEmpty()) {
            TextView note = text(asset.note, 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams noteParams = lp(-1, -2);
            noteParams.topMargin = dp(8);
            card.addView(note, noteParams);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = lp(-1, -2);
        actionsParams.topMargin = dp(14);
        card.addView(actions, actionsParams);

        Button launch = secondaryButton("打开 App");
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap1 = new SpaceView(this, dp(8), 1);
        actions.addView(gap1);

        Button mark = secondaryButton("已更新");
        mark.setOnClickListener(view -> markUpdated(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap2 = new SpaceView(this, dp(8), 1);
        actions.addView(gap2);

        Button edit = secondaryButton("编辑");
        edit.setOnClickListener(view -> showEditDialog(asset));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));

        return card;
    }

    private TextView statusChip(AssetRecord asset) {
        TextView chip = text(statusText(asset), 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(statusColor(asset));
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private void showEditDialog(AssetRecord original) {
        boolean creating = original == null;
        AssetRecord draft = creating ? new AssetRecord() : copyOf(original);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        EditText name = input("资产名称", draft.name, InputType.TYPE_CLASS_TEXT);
        form.addView(name);

        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        category.setSelection(indexOf(CATEGORIES, draft.category));
        form.addView(fieldBox("类型", category));

        EditText institution = input("机构", draft.institution, InputType.TYPE_CLASS_TEXT);
        form.addView(institution);

        LinearLayout amountRow = row();
        EditText amount = input("金额", draft.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText currency = input("币种", draft.currency, InputType.TYPE_CLASS_TEXT);
        amountRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));
        amountRow.addView(new SpaceView(this, dp(8), 1));
        amountRow.addView(currency, new LinearLayout.LayoutParams(0, -2, 0.55f));
        form.addView(amountRow);

        EditText cadence = input("更新周期（天）", String.valueOf(draft.updateEveryDays), InputType.TYPE_CLASS_NUMBER);
        form.addView(cadence);

        EditText packageName = input("App 包名", draft.packageName, InputType.TYPE_CLASS_TEXT);
        packageName.setHint("可手动填写，也可从已安装 App 选择");
        form.addView(packageName);

        Button chooseApp = secondaryButton("选择已安装 App");
        chooseApp.setOnClickListener(view -> showAppPicker(packageName, institution));
        LinearLayout.LayoutParams chooseAppParams = lp(-1, dp(44));
        chooseAppParams.bottomMargin = dp(10);
        form.addView(chooseApp, chooseAppParams);

        EditText launchUri = input("启动链接（可选）", draft.launchUri, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        launchUri.setHint("例如 bankapp://home");
        form.addView(launchUri);

        EditText note = input("备注", draft.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
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
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                String assetName = clean(name.getText().toString());
                if (assetName.isEmpty()) {
                    toast("资产名称不能为空");
                    return;
                }

                int everyDays = parsePositiveInt(cadence.getText().toString(), 7);
                draft.name = assetName;
                draft.category = String.valueOf(category.getSelectedItem());
                draft.institution = clean(institution.getText().toString());
                draft.amount = clean(amount.getText().toString());
                draft.currency = clean(currency.getText().toString()).isEmpty()
                        ? "CNY"
                        : clean(currency.getText().toString()).toUpperCase(Locale.ROOT);
                draft.updateEveryDays = everyDays;
                draft.packageName = clean(packageName.getText().toString());
                draft.launchUri = clean(launchUri.getText().toString());
                draft.note = clean(note.getText().toString());

                if (creating) {
                    assets.add(draft);
                } else {
                    replaceAsset(draft);
                }
                store.save(assets);
                snapshots = store.recordSnapshot(assets, settings);
                render();
                dialog.dismiss();
            });

            Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (delete != null) {
                delete.setTextColor(DANGER);
                delete.setOnClickListener(button -> new AlertDialog.Builder(this)
                        .setTitle("删除资产")
                        .setMessage("确定删除「" + original.name + "」吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (confirm, which) -> {
                            assets.removeIf(asset -> asset.id.equals(original.id));
                            store.save(assets);
                            snapshots = store.recordSnapshot(assets, settings);
                            render();
                            dialog.dismiss();
                        })
                        .show());
            }
        });

        dialog.show();
    }

    private void showAppPicker(EditText packageNameInput, EditText institutionInput) {
        List<LaunchableApp> apps = getLaunchableApps();
        if (apps.isEmpty()) {
            toast("没有找到可启动的 App。");
            return;
        }

        String[] labels = new String[apps.size()];
        for (int index = 0; index < apps.size(); index += 1) {
            LaunchableApp app = apps.get(index);
            labels[index] = app.label + "\n" + app.packageName;
        }

        new AlertDialog.Builder(this)
                .setTitle("选择已安装 App")
                .setNegativeButton("取消", null)
                .setItems(labels, (dialog, which) -> {
                    LaunchableApp selected = apps.get(which);
                    packageNameInput.setText(selected.packageName);
                    String institution = clean(institutionInput.getText().toString());
                    if (institution.isEmpty() || institution.contains("待绑定")) {
                        institutionInput.setText(selected.label);
                    }
                    toast("已选择 " + selected.label);
                })
                .show();
    }

    private List<LaunchableApp> getLaunchableApps() {
        PackageManager packageManager = getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolvedApps = packageManager.queryIntentActivities(launcherIntent, 0);
        List<LaunchableApp> apps = new ArrayList<>();
        Set<String> seenPackages = new HashSet<>();
        for (ResolveInfo resolvedApp : resolvedApps) {
            String packageName = resolvedApp.activityInfo == null ? "" : resolvedApp.activityInfo.packageName;
            if (packageName == null
                    || packageName.isEmpty()
                    || packageName.equals(getPackageName())
                    || seenPackages.contains(packageName)) {
                continue;
            }
            seenPackages.add(packageName);
            CharSequence label = resolvedApp.loadLabel(packageManager);
            String appLabel = label == null ? packageName : label.toString();
            apps.add(new LaunchableApp(appLabel, packageName));
        }
        Collections.sort(apps, (left, right) -> left.label.compareToIgnoreCase(right.label));
        return apps;
    }

    private void openLinkedApp(AssetRecord asset) {
        if (asset.launchUri.isEmpty() && asset.packageName.isEmpty()) {
            toast("先编辑资产，填写对应 App 包名或启动链接。");
            return;
        }

        if (!asset.launchUri.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(asset.launchUri));
                if (!asset.packageName.isEmpty()) {
                    intent.setPackage(asset.packageName);
                }
                pendingLaunchAssetId = asset.id;
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException error) {
                pendingLaunchAssetId = null;
            }
        }

        if (!asset.packageName.isEmpty()) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(asset.packageName);
            if (launchIntent != null) {
                pendingLaunchAssetId = asset.id;
                startActivity(launchIntent);
                return;
            }
            openMarket(asset.packageName);
            return;
        }

        toast("没有找到可打开的 App。");
    }

    private void openMarket(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException error) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            } catch (ActivityNotFoundException ignored) {
                toast("没有找到这个 App，请检查包名。");
            }
        }
    }

    private void showMarkUpdatedDialog(AssetRecord asset) {
        new AlertDialog.Builder(this)
                .setTitle("标记已更新？")
                .setMessage("刚才打开了「" + asset.name + "」。如果你已经核对余额，可以把更新时间记为现在。")
                .setNegativeButton("暂不", null)
                .setPositiveButton("标记已更新", (dialog, which) -> markUpdated(asset))
                .show();
    }

    private void markUpdated(AssetRecord asset) {
        asset.lastUpdatedAt = System.currentTimeMillis();
        store.save(assets);
        snapshots = store.recordSnapshot(assets, settings);
        render();
        toast("已更新「" + asset.name + "」。");
    }

    private void startBackupExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "money-manager-backup-" + backupDate() + ".json");
        try {
            startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
        } catch (ActivityNotFoundException error) {
            toast("没有找到可保存文件的应用。");
        }
    }

    private void startBackupImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
        } catch (ActivityNotFoundException error) {
            toast("没有找到可选择文件的应用。");
        }
    }

    private void writeBackup(Uri uri) {
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) {
                toast("无法写入备份文件。");
                return;
            }
            String raw = store.exportJson(assets, snapshots, settings);
            output.write(raw.getBytes(StandardCharsets.UTF_8));
            toast("备份已导出。");
        } catch (Exception error) {
            toast("导出失败，请重试。");
        }
    }

    private void readBackup(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                toast("无法读取备份文件。");
                return;
            }
            String raw = readUtf8(input);
            AssetBackup backup = store.parseBackup(raw);
            confirmImportBackup(backup);
        } catch (Exception error) {
            toast("导入失败，请确认文件是 Money Manager 备份。");
        }
    }

    private void confirmImportBackup(AssetBackup backup) {
        new AlertDialog.Builder(this)
                .setTitle("导入备份？")
                .setMessage("将导入 " + backup.assets.size() + " 项资产和 "
                        + backup.snapshots.size() + " 个趋势快照，并覆盖当前本机数据。")
                .setNegativeButton("取消", null)
                .setPositiveButton("导入", (dialog, which) -> {
                    store.replaceAll(backup);
                    assets = store.load();
                    settings = store.loadSettings();
                    snapshots = store.loadSnapshots();
                    if (snapshots.isEmpty()) {
                        snapshots = store.recordSnapshot(assets, settings);
                    }
                    render();
                    toast("备份已导入。");
                })
                .show();
    }

    private String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }

    private AssetRecord findAsset(String id) {
        for (AssetRecord asset : assets) {
            if (asset.id.equals(id)) {
                return asset;
            }
        }
        return null;
    }

    private void replaceAsset(AssetRecord updated) {
        for (int index = 0; index < assets.size(); index += 1) {
            if (assets.get(index).id.equals(updated.id)) {
                assets.set(index, updated);
                return;
            }
        }
    }

    private AssetRecord copyOf(AssetRecord asset) {
        return AssetRecord.copyOf(asset);
    }

    private boolean isStale(AssetRecord asset) {
        return AssetMath.isStale(asset);
    }

    private int daysUntilDue(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return -10_000;
        }
        long dueAt = asset.lastUpdatedAt + asset.updateEveryDays * AssetMath.DAY_MS;
        long remaining = dueAt - System.currentTimeMillis();
        if (remaining <= 0) {
            return (int) (remaining / AssetMath.DAY_MS);
        }
        return (int) Math.ceil(remaining / (double) AssetMath.DAY_MS);
    }

    private int statusColor(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return AMBER;
        }
        return isStale(asset) ? DANGER : ACCENT;
    }

    private String statusText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "待更新";
        }
        return isStale(asset) ? "已过期" : "新鲜";
    }

    private String lastUpdatedText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "最后更新：从未更新";
        }
        long days = Math.max(0, (System.currentTimeMillis() - asset.lastUpdatedAt) / AssetMath.DAY_MS);
        return "最后更新：" + dateFormat.format(new Date(asset.lastUpdatedAt)) + " · " + days + " 天前";
    }

    private String formatAmount(AssetRecord asset) {
        if (settings.hideAmounts) {
            return "•••• " + asset.currency;
        }
        if (asset.amount.isEmpty()) {
            return "-- " + asset.currency;
        }
        try {
            double value = Double.parseDouble(asset.amount);
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(value) + " " + asset.currency;
        } catch (NumberFormatException error) {
            return asset.amount + " " + asset.currency;
        }
    }

    private String formatMoney(double value, String currency) {
        if (settings.hideAmounts) {
            return "•••• " + currency;
        }
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return format.format(value) + " " + currency;
    }

    private String formatRate(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.####");
        return format.format(value);
    }

    private String formatPercent(double value, double total) {
        if (total <= 0) {
            return "0.0%";
        }
        return String.format(Locale.getDefault(), "%.1f%%", value / total * 100);
    }

    private String formatSignedMoney(double value, String currency) {
        String sign = value > 0 ? "+" : "";
        return sign + formatMoney(value, currency);
    }

    private String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < lines.size(); index += 1) {
            if (index > 0) {
                builder.append("\n");
            }
            builder.append(lines.get(index));
        }
        return builder.toString();
    }

    private String backupDate() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(new Date());
    }

    private EditText input(String label, String value, int inputType) {
        EditText input = new EditText(this);
        input.setHint(label);
        input.setText(value);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setInputType(inputType);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(cardBackground(Color.WHITE, LINE));

        LinearLayout.LayoutParams params = lp(-1, dp(52));
        params.bottomMargin = dp(10);
        input.setLayoutParams(params);
        return input;
    }

    private View fieldBox(String label, View field) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView text = label(label);
        box.addView(text);
        box.addView(field, lp(-1, dp(48)));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(10);
        box.setLayoutParams(params);
        return box;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(lp(-1, -2));
        return row;
    }

    private TextView label(String value) {
        TextView text = text(value.toUpperCase(Locale.ROOT), 12, MUTED, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(8));
        button.setBackground(bg);
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(INK);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(cardBackground(Color.WHITE, LINE));
        return button;
    }

    private GradientDrawable cardBackground(int fill, int border) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), border);
        return bg;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private double parsePositiveDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(",", ""));
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private int indexOf(String[] values, String value) {
        for (int index = 0; index < values.length; index += 1) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class SpaceView extends FrameLayout {
        SpaceView(Activity activity, int width, int height) {
            super(activity);
            setLayoutParams(new LinearLayout.LayoutParams(width, height));
        }
    }

    private static final class LaunchableApp {
        final String label;
        final String packageName;

        LaunchableApp(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    private static final class CurrencyRateField {
        final String currency;
        final EditText input;

        CurrencyRateField(String currency, EditText input) {
            this.currency = currency;
            this.input = input;
        }
    }
}
