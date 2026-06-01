package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.model.AssetRecord;
import com.computerascience.moneymanager.model.AssetUpdateEvent;
import com.computerascience.moneymanager.ui.TrendChartView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class AssetTrendRenderer {
    private final MainActivity activity;

    AssetTrendRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("单项资产趋势"));

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
                if (activity.suppressAssetTrendSelection || position < 0 || position >= activity.assetTrendOptions.size()) {
                    return;
                }
                activity.selectedTrendAssetId = activity.assetTrendOptions.get(position).id;
                render();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        card.addView(activity.fieldBox("选择资产", activity.assetTrendSpinner));

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
        activity.assetTrendOptions = new ArrayList<>(activity.assets);
        Collections.sort(activity.assetTrendOptions, (left, right) -> left.name.compareToIgnoreCase(right.name));

        activity.assetTrendHistoryList.removeAllViews();
        if (activity.assetTrendOptions.isEmpty()) {
            activity.assetTrendSummary.setText("新增资产后，这里会显示每一项资产的金额变化。");
            activity.assetTrendChart.setPoints(new ArrayList<>(), "还没有资产");
            activity.suppressAssetTrendSelection = true;
            activity.assetTrendSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>()));
            activity.suppressAssetTrendSelection = false;
            return;
        }

        int selectedIndex = selectedIndex();
        activity.selectedTrendAssetId = activity.assetTrendOptions.get(selectedIndex).id;

        List<String> names = new ArrayList<>();
        for (AssetRecord asset : activity.assetTrendOptions) {
            names.add(asset.name);
        }
        activity.suppressAssetTrendSelection = true;
        activity.assetTrendSpinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, names));
        activity.assetTrendSpinner.setSelection(selectedIndex);
        activity.suppressAssetTrendSelection = false;

        AssetRecord selected = activity.assetTrendOptions.get(selectedIndex);
        List<AssetUpdateEvent> events = updateEventsForAsset(selected.id);
        List<TrendChartView.Point> points = assetTrendPoints(selected, events);
        activity.assetTrendChart.setPoints(points, "更新几次金额后显示单项趋势");
        activity.assetTrendSummary.setText(assetTrendSummaryText(selected, points));
        renderAssetHistory(events);
    }

    private int selectedIndex() {
        if (activity.selectedTrendAssetId.isEmpty()) {
            return 0;
        }
        for (int index = 0; index < activity.assetTrendOptions.size(); index += 1) {
            if (activity.selectedTrendAssetId.equals(activity.assetTrendOptions.get(index).id)) {
                return index;
            }
        }
        return 0;
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
}
