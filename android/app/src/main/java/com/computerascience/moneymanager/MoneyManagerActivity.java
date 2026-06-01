package com.computerascience.moneymanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.content.pm.PackageManager;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import com.computerascience.moneymanager.data.AssetStore;
import com.computerascience.moneymanager.domain.AssetCategories;
import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.model.PortfolioSettings;
import com.computerascience.moneymanager.ui.AllocationChartView;
import com.computerascience.moneymanager.ui.BottomNavBar;
import com.computerascience.moneymanager.ui.SectionDrawer;
import com.computerascience.moneymanager.ui.SectionProgressHandle;
import com.computerascience.moneymanager.ui.TrendChartView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class MoneyManagerActivity extends MoneyManagerUiActivity {
    protected static final String ADD_CATEGORY_OPTION = "新增资产类型...";

    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    protected AssetStore store;
    protected List<AssetRecord> assets = new ArrayList<>();
    protected List<AssetSnapshot> snapshots = new ArrayList<>();
    protected List<AssetUpdateEvent> updateEvents = new ArrayList<>();
    protected PortfolioSettings settings;
    protected ScrollView mainScrollView;
    protected TextView pageTitle;
    protected TextView pageSubtitle;
    protected LinearLayout overviewPage;
    protected LinearLayout investmentPage;
    protected LinearLayout trendPage;
    protected LinearLayout assetsPage;
    protected FrameLayout contentFrame;
    protected BottomNavBar bottomNavBar;
    protected SectionDrawer sectionDrawer;
    protected SectionProgressHandle sectionProgressHandle;
    protected SectionDrawer.Item[] overviewSections;
    protected SectionDrawer.Item[] investmentSections;
    protected SectionDrawer.Item[] trendSections;
    protected SectionDrawer.Item[] assetSections;
    protected LinearLayout assetList;
    protected TextView actionCenterSummary;
    protected LinearLayout actionCenterList;
    protected LinearLayout allocationLegend;
    protected LinearLayout allocationTargetList;
    protected LinearLayout institutionList;
    protected LinearLayout updatePlanList;
    protected LinearLayout recentUpdateList;
    protected LinearLayout managementBody;
    protected AllocationChartView allocationChart;
    protected TrendChartView trendChart;
    protected TextView netWorthValue;
    protected TextView grossAssetsValue;
    protected TextView liabilitiesValue;
    protected TextView freshnessValue;
    protected Button privacyToggle;
    protected TextView currencyNote;
    protected TextView netWorthGoalSummary;
    protected TextView currencySettingsSummary;
    protected TextView allocationTargetSummary;
    protected TextView trendSummary;
    protected LinearLayout trendMetricsList;
    protected LinearLayout trendHistoryList;
    protected TextView monthlyReviewSummary;
    protected LinearLayout monthlyReviewList;
    protected TextView targetProgressSummary;
    protected LinearLayout targetProgressList;
    protected TextView distributionTrendSummary;
    protected LinearLayout distributionTrendList;
    protected TextView flowAttributionSummary;
    protected LinearLayout flowAttributionList;
    protected TextView insightSummary;
    protected TextView dataHealthSummary;
    protected LinearLayout dataHealthList;
    protected TextView updatePlanSummary;
    protected TextView recentUpdateSummary;
    protected TextView investmentSummaryText;
    protected TextView investmentReviewSummary;
    protected LinearLayout investmentReviewList;
    protected TextView investmentDiagnosticSummary;
    protected LinearLayout investmentDiagnosticList;
    protected TextView investmentFlowSummary;
    protected LinearLayout investmentFlowList;
    protected TextView investmentStructureSummary;
    protected LinearLayout investmentStructureList;
    protected TextView investmentInstitutionSummary;
    protected LinearLayout investmentInstitutionList;
    protected TextView investmentPlanSummary;
    protected LinearLayout investmentPlanList;
    protected LinearLayout investmentAccountList;
    protected TextView managementSummary;
    protected TextView assetResultSummary;
    protected View assetManagementCard;
    protected EditText assetSearchInput;
    protected LinearLayout assetFilterButtons;
    protected Button managementToggle;
    protected String pendingLaunchAssetId;
    protected boolean waitingForExternalReturn;
    protected boolean managementExpanded = true;
    protected String assetSearchQuery = "";
    protected String assetFilterMode = "all";
    protected boolean suppressAssetTrendSelection;
    protected String selectedTrendAssetId = "";
    protected Spinner assetTrendSpinner;
    protected TrendChartView assetTrendChart;
    protected TextView assetTrendSummary;
    protected LinearLayout assetTrendHistoryList;
    protected List<AssetRecord> assetTrendOptions = new ArrayList<>();
    protected final Set<String> collapsedAssetGroups = new HashSet<>();
    protected int sectionDragIndex = -1;

    protected int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    protected double parsePositiveDouble(String value, double fallback) {
        try {
            double parsed = Double.parseDouble(value.trim().replace(",", ""));
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    protected int indexOf(String[] values, String value) {
        for (int index = 0; index < values.length; index += 1) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    protected String clean(String value) {
        return value == null ? "" : value.trim();
    }

    protected String cleanReason(String value) {
        return clean(value).isEmpty() ? "余额核对" : clean(value);
    }

    protected AssetRecord copyOf(AssetRecord asset) {
        return AssetRecord.copyOf(asset);
    }

    protected boolean isStale(AssetRecord asset) {
        return AssetMath.isStale(asset);
    }

    protected int daysUntilDue(AssetRecord asset) {
        return AssetMath.daysUntilDue(asset, System.currentTimeMillis());
    }

    protected int statusColor(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return AMBER;
        }
        return isStale(asset) ? DANGER : ACCENT;
    }

    protected String statusText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "待更新";
        }
        return isStale(asset) ? "已过期" : "新鲜";
    }

    protected String lastUpdatedText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "最后更新：从未更新";
        }
        long days = Math.max(0, (System.currentTimeMillis() - asset.lastUpdatedAt) / AssetMath.DAY_MS);
        return "最后更新：" + dateFormat.format(new Date(asset.lastUpdatedAt)) + " · " + days + " 天前";
    }

    protected String shortUpdatedText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "从未更新";
        }
        long days = Math.max(0, (System.currentTimeMillis() - asset.lastUpdatedAt) / AssetMath.DAY_MS);
        return days == 0 ? "今天更新" : days + " 天前";
    }

    protected String appDisplayName(AssetRecord asset) {
        String text = appBindingText(asset.appName, asset.packageName, asset.launchUri);
        return "未选择 App".equals(text) ? "未绑定 App" : text;
    }

    protected String appBindingText(String appName, String packageName, String launchUri) {
        String name = clean(appName);
        if (!name.isEmpty()) {
            return name;
        }
        String resolved = resolveAppLabel(packageName);
        if (!resolved.isEmpty()) {
            return resolved;
        }
        if (!clean(packageName).isEmpty() || !clean(launchUri).isEmpty()) {
            return "已绑定 App";
        }
        return "未选择 App";
    }

    protected String resolveAppLabel(String packageName) {
        String cleaned = clean(packageName);
        if (cleaned.isEmpty()) {
            return "";
        }
        try {
            PackageManager manager = getPackageManager();
            return manager.getApplicationLabel(manager.getApplicationInfo(cleaned, 0)).toString();
        } catch (PackageManager.NameNotFoundException error) {
            return "";
        }
    }

    protected Drawable resolveAppIcon(String packageName) {
        String cleaned = clean(packageName);
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            return getPackageManager().getApplicationIcon(cleaned);
        } catch (PackageManager.NameNotFoundException error) {
            return null;
        }
    }

    protected String formatAmount(AssetRecord asset) {
        if (settings.hideAmounts) {
            return "•••• " + asset.currency;
        }
        if (asset.amount.isEmpty()) {
            return "-- " + asset.currency;
        }
        try {
            Double value = parseNumber(asset.amount);
            if (value == null) {
                return asset.amount + " " + asset.currency;
            }
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(value) + " " + asset.currency;
        } catch (NumberFormatException error) {
            return asset.amount + " " + asset.currency;
        }
    }

    protected List<String> assetBreakdownLines(AssetRecord asset) {
        List<String> lines = new ArrayList<>();
        if (settings.hideAmounts) {
            return lines;
        }
        return lines;
    }

    protected double assetMagnitude(AssetRecord asset) {
        return AssetMath.assetGrossAmount(asset) + AssetMath.assetLiabilityAmount(asset);
    }

    protected String formatMoney(double value, String currency) {
        if (settings.hideAmounts) {
            return "•••• " + currency;
        }
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return format.format(value) + " " + currency;
    }

    protected String formatRawAmount(String value) {
        try {
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(Double.parseDouble(value.replace(",", "")));
        } catch (NumberFormatException error) {
            return value;
        }
    }

    protected String formatRawAmount(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return format.format(value);
    }

    protected String formatInputNumber(double value) {
        DecimalFormat format = new DecimalFormat("0.##");
        return format.format(value);
    }

    protected String formatRate(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.####");
        return format.format(value);
    }

    protected String formatPercent(double value, double total) {
        if (total <= 0) {
            return "0.0%";
        }
        return String.format(Locale.getDefault(), "%.1f%%", value / total * 100);
    }

    protected String formatPercentValue(double value) {
        return String.format(Locale.getDefault(), "%.1f%%", value);
    }

    protected String formatPoint(double value) {
        return String.format(Locale.getDefault(), "%+.1f 个百分点", value);
    }

    protected String formatSignedMoney(double value, String currency) {
        String sign = value > 0 ? "+" : "";
        return sign + formatMoney(value, currency);
    }

    protected String formatSignedRawAmount(double value) {
        String sign = value > 0 ? "+" : "";
        DecimalFormat format = new DecimalFormat("#,##0.##");
        return sign + format.format(value);
    }

    protected String joinLines(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
        return builder.toString();
    }

    protected String joinInline(List<String> lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(line);
        }
        return builder.toString();
    }

    protected String backupDate() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(new Date());
    }

    protected String dayKey(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
    }

    protected String defaultYearEnd() {
        return new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()) + "-12-31";
    }

    protected int daysUntilTimestamp(long timestamp) {
        long remaining = timestamp - System.currentTimeMillis();
        return Math.max(0, (int) Math.ceil(remaining / (double) AssetMath.DAY_MS));
    }

    protected Date parseDay(String value) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            format.setLenient(false);
            return format.parse(value);
        } catch (Exception error) {
            return null;
        }
    }

    protected Double parseNumber(String value) {
        try {
            return Double.parseDouble(value.replace(",", ""));
        } catch (NumberFormatException error) {
            return null;
        }
    }

    protected double doubleValue(Map<String, Double> values, String key) {
        Double value = values.get(key);
        return value == null ? 0.0 : value;
    }

    protected int intValue(Map<String, Integer> values, String key) {
        Integer value = values.get(key);
        return value == null ? 0 : value;
    }

    protected AssetRecord findAsset(String id) {
        for (AssetRecord asset : assets) {
            if (asset.id.equals(id)) {
                return asset;
            }
        }
        return null;
    }

    protected void replaceAsset(AssetRecord updated) {
        for (int index = 0; index < assets.size(); index += 1) {
            if (assets.get(index).id.equals(updated.id)) {
                assets.set(index, updated);
                return;
            }
        }
    }

    protected void removeAssetById(String assetId) {
        for (int index = assets.size() - 1; index >= 0; index -= 1) {
            if (assets.get(index).id.equals(assetId)) {
                assets.remove(index);
            }
        }
    }

    protected Spinner currencySpinner(String selectedCurrency) {
        String selected = PortfolioSettings.cleanCurrency(selectedCurrency);
        if (selected.isEmpty()) {
            selected = "CNY";
        }
        List<String> options = currencyOptions(selected);
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
        styleSpinner(spinner);
        int selectedIndex = options.indexOf(selected);
        spinner.setSelection(Math.max(0, selectedIndex));
        return spinner;
    }

    protected List<String> currencyOptions(String selectedCurrency) {
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

    protected String[] categoryOptions(String selectedCategory) {
        List<String> options = categoryOptionList(selectedCategory, true);
        return options.toArray(new String[0]);
    }

    protected List<String> categoryOptionList(String selectedCategory, boolean includeAddOption) {
        List<String> options = new ArrayList<>();
        for (String category : AssetCategories.ALL) {
            addCategoryOption(options, category);
        }
        if (settings != null) {
            for (String category : settings.customCategories) {
                addCategoryOption(options, category);
            }
        }
        for (AssetRecord asset : assets) {
            addCategoryOption(options, asset.category);
        }
        addCategoryOption(options, selectedCategory);
        if (includeAddOption) {
            options.add(ADD_CATEGORY_OPTION);
        }
        return options;
    }

    protected void addCategoryOption(List<String> options, String category) {
        String cleaned = clean(category);
        if (!cleaned.isEmpty() && !ADD_CATEGORY_OPTION.equals(cleaned) && !options.contains(cleaned)) {
            options.add(cleaned);
        }
    }

    protected boolean isDefaultAssetCategory(String category) {
        for (String option : AssetCategories.ALL) {
            if (option.equals(category)) {
                return true;
            }
        }
        return false;
    }

    protected TextView categoryMark(AssetRecord asset) {
        int color = AssetMath.colorForCategory(asset.category);
        TextView mark = text(categoryIcon(asset.category), 18, color, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(roundedBackground(SURFACE_ALT, Color.TRANSPARENT, 8));
        return mark;
    }

    protected String categoryIcon(String category) {
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
        String cleaned = clean(category);
        return cleaned.isEmpty() ? "•" : cleaned.substring(0, 1);
    }

    protected TextView statusChip(AssetRecord asset) {
        TextView chip = text(statusText(asset), 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(statusColor(asset));
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

}
