package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.ui.TrendChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AssetTrendRenderer {
    private final MainActivity activity;
    private final List<Option> options = new ArrayList<>();

    AssetTrendRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("对象趋势"));

        activity.assetTrendSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(10);
        card.addView(activity.assetTrendSummary, summaryParams);

        activity.assetTrendSpinner = new android.widget.Spinner(activity);
        activity.styleSpinner(activity.assetTrendSpinner);
        activity.assetTrendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (activity.suppressAssetTrendSelection || position < 0 || position >= options.size()) {
                    return;
                }
                activity.selectedTrendAssetId = options.get(position).key;
                render();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        card.addView(activity.fieldBox("选择对象", activity.assetTrendSpinner));

        activity.assetTrendChart = new TrendChartView(activity);
        LinearLayout.LayoutParams chartParams = activity.lp(-1, activity.dp(160));
        chartParams.topMargin = activity.dp(6);
        chartParams.bottomMargin = activity.dp(10);
        card.addView(activity.assetTrendChart, chartParams);

        activity.assetTrendHistoryList = new LinearLayout(activity);
        activity.assetTrendHistoryList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.assetTrendHistoryList, activity.lp(-1, -2));
        return card;
    }

    void render() {
        options.clear();
        options.addAll(institutionOptions());
        options.addAll(assetOptions());
        activity.assetTrendHistoryList.removeAllViews();
        if (options.isEmpty()) {
            activity.assetTrendSummary.setText("新增资产或记录机构分布后，这里会显示单项对象趋势。");
            activity.assetTrendChart.setPoints(new ArrayList<>(), "还没有可选对象");
            activity.suppressAssetTrendSelection = true;
            activity.assetTrendSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
            activity.suppressAssetTrendSelection = false;
            return;
        }

        int selectedIndex = selectedIndex();
        Option selected = options.get(selectedIndex);
        activity.selectedTrendAssetId = selected.key;

        List<String> names = new ArrayList<>();
        for (Option option : options) {
            names.add(option.label);
        }
        activity.suppressAssetTrendSelection = true;
        activity.assetTrendSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, names));
        activity.assetTrendSpinner.setSelection(selectedIndex);
        activity.suppressAssetTrendSelection = false;

        if (selected.asset != null) {
            renderAsset(selected.asset);
        } else {
            renderInstitution(selected.institution);
        }
    }

    private int selectedIndex() {
        if (activity.selectedTrendAssetId.isEmpty()) {
            return 0;
        }
        for (int index = 0; index < options.size(); index += 1) {
            Option option = options.get(index);
            if (activity.selectedTrendAssetId.equals(option.key)
                    || activity.selectedTrendAssetId.equals(option.legacyAssetId)) {
                return index;
            }
        }
        return 0;
    }

    private void renderAsset(AssetRecord asset) {
        List<AssetUpdateEvent> events = updateEventsForAsset(asset.id);
        List<TrendChartView.Point> points = assetTrendPoints(asset, events);
        activity.assetTrendChart.setPoints(points, "更新几次金额后显示资产趋势");
        activity.assetTrendSummary.setText(assetTrendSummaryText(asset, points));
        renderAssetHistory(events);
    }

    private void renderInstitution(String institution) {
        List<TrendChartView.Point> points = institutionTrendPoints(institution);
        activity.assetTrendChart.setPoints(points, "记录两次机构分布后显示趋势");
        activity.assetTrendSummary.setText(institutionTrendSummaryText(institution, points));
        renderInstitutionHistory(points);
    }

    private void renderAssetHistory(List<AssetUpdateEvent> events) {
        activity.assetTrendHistoryList.addView(activity.text("最近变化", 13, MoneyManagerActivity.MUTED, Typeface.BOLD));
        if (events.isEmpty()) {
            TextView empty = activity.text("这项资产还没有更新记录。点“已更新”录入几次金额后，就能看到单项趋势。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.assetTrendHistoryList.addView(empty, emptyParams);
            return;
        }
        int limit = Math.min(5, events.size());
        for (int index = 0; index < limit; index += 1) {
            activity.assetTrendHistoryList.addView(new TrendUpdatesRenderer(activity).updateEventRow(events.get(index)));
        }
    }

    private void renderInstitutionHistory(List<TrendChartView.Point> points) {
        activity.assetTrendHistoryList.addView(activity.text("最近快照", 13, MoneyManagerActivity.MUTED, Typeface.BOLD));
        if (points.isEmpty()) {
            TextView empty = activity.text("这个机构还没有快照记录。更新资产后会自动记录机构分布。", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams emptyParams = activity.lp(-1, -2);
            emptyParams.topMargin = activity.dp(8);
            activity.assetTrendHistoryList.addView(empty, emptyParams);
            return;
        }

        int start = Math.max(0, points.size() - 5);
        for (int index = points.size() - 1; index >= start; index -= 1) {
            TrendChartView.Point point = points.get(index);
            String amount = activity.settings.hideAmounts
                    ? "金额已隐藏"
                    : activity.formatMoney(point.value, activity.settings.baseCurrency);
            activity.assetTrendHistoryList.addView(activity.text(
                    activity.dateFormat.format(new Date(point.timestamp)) + " · " + amount,
                    13,
                    MoneyManagerActivity.MUTED,
                    Typeface.NORMAL
            ));
        }
    }

    private List<AssetUpdateEvent> updateEventsForAsset(String assetId) {
        List<AssetUpdateEvent> events = new ArrayList<>();
        for (AssetUpdateEvent event : activity.updateEvents) {
            if (assetId.equals(event.assetId)) {
                events.add(event);
            }
        }
        Collections.sort(events, (left, right) -> Long.compare(right.timestamp, left.timestamp));
        return events;
    }

    private List<TrendChartView.Point> assetTrendPoints(AssetRecord asset, List<AssetUpdateEvent> newestFirst) {
        List<AssetUpdateEvent> ascending = new ArrayList<>(newestFirst);
        Collections.sort(ascending, (left, right) -> Long.compare(left.timestamp, right.timestamp));

        List<TrendChartView.Point> points = new ArrayList<>();
        for (AssetUpdateEvent event : ascending) {
            if (points.isEmpty()) {
                points.add(new TrendChartView.Point(event.timestamp - 1, AssetMath.parseAmount(event.previousAmount)));
            }
            points.add(new TrendChartView.Point(event.timestamp, AssetMath.parseAmount(event.newAmount)));
        }

        if (points.isEmpty() && !asset.amount.isEmpty()) {
            points.add(new TrendChartView.Point(
                    asset.lastUpdatedAt <= 0 ? System.currentTimeMillis() : asset.lastUpdatedAt,
                    AssetMath.parseAmount(asset.amount)
            ));
        }
        return points;
    }

    private List<TrendChartView.Point> institutionTrendPoints(String institution) {
        List<TrendChartView.Point> points = new ArrayList<>();
        for (AssetSnapshot snapshot : activity.snapshots) {
            if (!activity.settings.baseCurrency.equals(snapshot.baseCurrency)
                    || snapshot.institutionValues.isEmpty()) {
                continue;
            }
            Double value = snapshot.institutionValues.get(institution);
            points.add(new TrendChartView.Point(snapshot.timestamp, value == null ? 0 : value));
        }
        return points;
    }

    private String assetTrendSummaryText(AssetRecord asset, List<TrendChartView.Point> points) {
        if (points.size() < 2) {
            return "当前 " + asset.name + " 只有 " + points.size() + " 个记录点，继续更新后会形成单项趋势。";
        }
        if (activity.settings.hideAmounts) {
            return asset.name + " 已记录 " + points.size() + " 个变化点，金额已隐藏。";
        }
        TrendChartView.Point first = points.get(0);
        TrendChartView.Point last = points.get(points.size() - 1);
        double change = last.value - first.value;
        double ratio = Math.abs(first.value) < 0.0001 ? 0 : change / Math.abs(first.value) * 100;
        return asset.name + " 共 " + points.size() + " 个变化点，变化 "
                + activity.formatSignedRawAmount(change) + " " + asset.currency
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private String institutionTrendSummaryText(String institution, List<TrendChartView.Point> points) {
        if (points.size() < 2) {
            return "机构「" + institution + "」只有 " + points.size() + " 个快照点，继续更新后会形成机构趋势。";
        }
        if (activity.settings.hideAmounts) {
            return "机构「" + institution + "」已记录 " + points.size() + " 个快照点，金额已隐藏。";
        }
        TrendChartView.Point first = points.get(0);
        TrendChartView.Point last = points.get(points.size() - 1);
        double change = last.value - first.value;
        double ratio = Math.abs(first.value) < 0.0001 ? 0 : change / Math.abs(first.value) * 100;
        return "机构「" + institution + "」共 " + points.size() + " 个快照点，变化 "
                + activity.formatSignedMoney(change, activity.settings.baseCurrency)
                + "（" + String.format(Locale.getDefault(), "%+.1f", ratio) + "%）。";
    }

    private List<Option> institutionOptions() {
        Set<String> seen = new HashSet<>();
        List<Option> result = new ArrayList<>();
        for (AssetSnapshot snapshot : activity.snapshots) {
            if (!activity.settings.baseCurrency.equals(snapshot.baseCurrency)) {
                continue;
            }
            for (String institution : snapshot.institutionValues.keySet()) {
                if (seen.add(institution)) {
                    result.add(Option.institution(institution));
                }
            }
        }
        Collections.sort(result, (left, right) -> Double.compare(
                latestInstitutionValue(right.institution),
                latestInstitutionValue(left.institution)
        ));
        return result;
    }

    private List<Option> assetOptions() {
        List<AssetRecord> assets = new ArrayList<>(activity.assets);
        Collections.sort(assets, (left, right) -> left.name.compareToIgnoreCase(right.name));
        List<Option> result = new ArrayList<>();
        for (AssetRecord asset : assets) {
            result.add(Option.asset(asset));
        }
        return result;
    }

    private double latestInstitutionValue(String institution) {
        for (int index = activity.snapshots.size() - 1; index >= 0; index -= 1) {
            AssetSnapshot snapshot = activity.snapshots.get(index);
            if (!activity.settings.baseCurrency.equals(snapshot.baseCurrency)) {
                continue;
            }
            Double value = snapshot.institutionValues.get(institution);
            if (value != null) {
                return value;
            }
        }
        return 0;
    }

    private static final class Option {
        final String key;
        final String legacyAssetId;
        final String label;
        final AssetRecord asset;
        final String institution;

        private Option(String key, String legacyAssetId, String label, AssetRecord asset, String institution) {
            this.key = key;
            this.legacyAssetId = legacyAssetId;
            this.label = label;
            this.asset = asset;
            this.institution = institution;
        }

        static Option asset(AssetRecord asset) {
            return new Option("asset:" + asset.id, asset.id, "资产 · " + asset.name, asset, "");
        }

        static Option institution(String institution) {
            return new Option("institution:" + institution, "", "机构 · " + institution, null, institution);
        }
    }
}
