package com.computerascience.moneymanager;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.computerascience.moneymanager.domain.AssetMath;
import com.computerascience.moneymanager.domain.TrendAnalytics;
import com.computerascience.moneymanager.domain.UpdateAnalytics;
import com.computerascience.moneymanager.model.AssetSnapshot;
import com.computerascience.moneymanager.model.PortfolioSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TrendHealthRenderer {
    private final MainActivity activity;

    TrendHealthRenderer(MainActivity activity) {
        this.activity = activity;
    }

    View card() {
        LinearLayout card = activity.card();
        card.addView(activity.sectionTitle("趋势健康"));

        activity.trendHealthSummary = activity.text("", 14, MoneyManagerActivity.MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams summaryParams = activity.lp(-1, -2);
        summaryParams.topMargin = activity.dp(8);
        summaryParams.bottomMargin = activity.dp(8);
        card.addView(activity.trendHealthSummary, summaryParams);

        activity.trendHealthList = new LinearLayout(activity);
        activity.trendHealthList.setOrientation(LinearLayout.VERTICAL);
        card.addView(activity.trendHealthList, activity.lp(-1, -2));
        return card;
    }

    void render(PortfolioSummary portfolio, List<AssetSnapshot> snapshots) {
        activity.trendHealthList.removeAllViews();
        if (snapshots.size() < 2) {
            activity.trendHealthSummary.setText("趋势健康需要至少 2 个快照。");
            activity.trendHealthList.addView(activity.emptyText("记录几次快照后，会显示稳定性、回撤和流水覆盖。"));
            return;
        }

        TrendAnalytics.Metric metric90 = metricFor(portfolio, snapshots, "近 90 天");
        TrendAnalytics.Metric metric30 = metricFor(portfolio, snapshots, "近 30 天");
        TrendWindow window = trendWindow(snapshots, 90);
        UpdateAnalytics.Summary updates30 = UpdateAnalytics.summarize(activity.updateEvents, activity.assets, activity.settings, 30);
        UpdateAnalytics.Summary updates90 = UpdateAnalytics.summarize(activity.updateEvents, activity.assets, activity.settings, 90);

        int score = trendScore(metric90, window, updates90);
        activity.trendHealthSummary.setText("趋势健康分 " + score + "/100；"
                + directionText(metric90) + "，" + cadenceSummary(snapshots) + "。");

        activity.trendHealthList.addView(healthRow(
                "净值方向",
                metricValue(metric90),
                metricDetail(metric90),
                metric90.change >= 0 ? MoneyManagerActivity.ACCENT : MoneyManagerActivity.DANGER
        ));
        activity.trendHealthList.addView(healthRow(
                "波动压力",
                volatilityValue(metric90),
                volatilityDetail(metric90),
                volatilityColor(metric90)
        ));
        activity.trendHealthList.addView(healthRow(
                "趋势连续性",
                continuityValue(window),
                continuityDetail(window),
                continuityColor(window)
        ));
        activity.trendHealthList.addView(healthRow(
                "记录密度",
                cadenceValue(snapshots),
                cadenceDetail(snapshots),
                cadenceColor(snapshots)
        ));
        activity.trendHealthList.addView(healthRow(
                "流水覆盖",
                coverageValue(metric30, updates30),
                coverageDetail(metric30, updates30),
                coverageColor(metric30, updates30)
        ));
    }

    private TrendAnalytics.Metric metricFor(PortfolioSummary portfolio, List<AssetSnapshot> snapshots, String label) {
        for (TrendAnalytics.Metric metric : TrendAnalytics.metrics(portfolio, snapshots, System.currentTimeMillis())) {
            if (label.equals(metric.label)) {
                return metric;
            }
        }
        return TrendAnalytics.metrics(portfolio, snapshots, System.currentTimeMillis()).get(0);
    }

    private int trendScore(TrendAnalytics.Metric metric, TrendWindow window, UpdateAnalytics.Summary updates) {
        int score = 60;
        if (metric.complete && metric.change > 0) score += 12;
        if (metric.complete && metric.maxDrawdownPercent <= 5) score += 10;
        if (metric.complete && metric.maxDrawdownPercent >= 15) score -= 12;
        if (window.intervals >= 3 && window.upCount >= window.downCount) score += 8;
        if (window.intervals >= 3 && window.downCount > window.upCount) score -= 8;
        if (updates.count >= 3) score += 5;
        if (updates.count == 0) score -= 5;
        return Math.max(0, Math.min(100, score));
    }

    private String directionText(TrendAnalytics.Metric metric) {
        if (!metric.complete) {
            return "快照不足";
        }
        if (activity.settings.hideAmounts) {
            return "金额已隐藏";
        }
        return metric.change >= 0 ? "净资产上行" : "净资产回落";
    }

    private String metricValue(TrendAnalytics.Metric metric) {
        if (!metric.complete) {
            return "不足";
        }
        if (activity.settings.hideAmounts) {
            return metric.count + " 个快照";
        }
        return activity.formatSignedMoney(metric.change, metric.currency);
    }

    private String metricDetail(TrendAnalytics.Metric metric) {
        if (!metric.complete) {
            return "继续记录快照后再判断方向。";
        }
        if (activity.settings.hideAmounts) {
            return metric.first.dayKey + " 到 " + metric.last.dayKey + "，金额已隐藏。";
        }
        double ratio = Math.abs(metric.first.netWorth) < 0.0001
                ? 0
                : metric.change / Math.abs(metric.first.netWorth) * 100;
        return metric.first.dayKey + " 到 " + metric.last.dayKey
                + "，阶段变化 " + String.format(Locale.getDefault(), "%+.1f%%", ratio) + "。";
    }

    private String volatilityValue(TrendAnalytics.Metric metric) {
        if (!metric.complete || activity.settings.hideAmounts) {
            return metric.complete ? activity.formatPercentValue(metric.maxDrawdownPercent) : "--";
        }
        return activity.formatMoney(metric.maxDrawdown, metric.currency);
    }

    private String volatilityDetail(TrendAnalytics.Metric metric) {
        if (!metric.complete) {
            return "快照不足，暂时无法计算回撤和波动。";
        }
        if (activity.settings.hideAmounts) {
            return "最大回撤 " + activity.formatPercentValue(metric.maxDrawdownPercent)
                    + "，平均单次波动金额已隐藏。";
        }
        return "最大回撤 " + activity.formatPercentValue(metric.maxDrawdownPercent)
                + "，平均单次波动 " + activity.formatMoney(metric.averageAbsDelta, metric.currency) + "。";
    }

    private int volatilityColor(TrendAnalytics.Metric metric) {
        if (!metric.complete || metric.maxDrawdownPercent < 8) {
            return MoneyManagerActivity.ACCENT;
        }
        return metric.maxDrawdownPercent < 15 ? MoneyManagerActivity.AMBER : MoneyManagerActivity.DANGER;
    }

    private TrendWindow trendWindow(List<AssetSnapshot> snapshots, int days) {
        long cutoff = System.currentTimeMillis() - days * AssetMath.DAY_MS;
        List<AssetSnapshot> window = new ArrayList<>();
        for (AssetSnapshot snapshot : snapshots) {
            if (snapshot.timestamp >= cutoff) {
                window.add(snapshot);
            }
        }
        TrendWindow result = new TrendWindow();
        result.snapshotCount = window.size();
        for (int index = 1; index < window.size(); index += 1) {
            double delta = window.get(index).netWorth - window.get(index - 1).netWorth;
            result.intervals += 1;
            if (delta > 0.0001) {
                result.upCount += 1;
            } else if (delta < -0.0001) {
                result.downCount += 1;
            } else {
                result.flatCount += 1;
            }
        }
        return result;
    }

    private String continuityValue(TrendWindow window) {
        if (window.intervals == 0) {
            return "--";
        }
        return window.upCount + " 升 / " + window.downCount + " 降";
    }

    private String continuityDetail(TrendWindow window) {
        if (window.intervals == 0) {
            return "近 90 天快照间隔不足，暂时无法判断连续性。";
        }
        return "近 90 天共有 " + window.snapshotCount + " 个快照、"
                + window.intervals + " 个变化间隔，持平 " + window.flatCount + " 次。";
    }

    private int continuityColor(TrendWindow window) {
        if (window.intervals == 0 || window.upCount >= window.downCount) {
            return MoneyManagerActivity.ACCENT;
        }
        return window.downCount > window.upCount + 1 ? MoneyManagerActivity.DANGER : MoneyManagerActivity.AMBER;
    }

    private String cadenceSummary(List<AssetSnapshot> snapshots) {
        int days = daysSinceLastSnapshot(snapshots);
        return days == 0 ? "今天已有快照" : days + " 天未记录快照";
    }

    private String cadenceValue(List<AssetSnapshot> snapshots) {
        int days = daysSinceLastSnapshot(snapshots);
        return days == 0 ? "今天" : days + " 天前";
    }

    private String cadenceDetail(List<AssetSnapshot> snapshots) {
        int days = daysSinceLastSnapshot(snapshots);
        int count90 = trendWindow(snapshots, 90).snapshotCount;
        return "近 90 天 " + count90 + " 个快照；"
                + (days <= 7 ? "记录节奏不错。" : "建议核对后顺手记录快照。");
    }

    private int cadenceColor(List<AssetSnapshot> snapshots) {
        int days = daysSinceLastSnapshot(snapshots);
        if (days <= 7) return MoneyManagerActivity.ACCENT;
        return days <= 14 ? MoneyManagerActivity.AMBER : MoneyManagerActivity.DANGER;
    }

    private int daysSinceLastSnapshot(List<AssetSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return 999;
        }
        AssetSnapshot last = snapshots.get(snapshots.size() - 1);
        return Math.max(0, (int) ((System.currentTimeMillis() - last.timestamp) / AssetMath.DAY_MS));
    }

    private String coverageValue(TrendAnalytics.Metric metric30, UpdateAnalytics.Summary updates30) {
        if (!metric30.complete) {
            return updates30.count + " 次流水";
        }
        if (activity.settings.hideAmounts) {
            return updates30.count + " 次";
        }
        double delta = metric30.change;
        double updates = updates30.delta;
        double gap = delta - updates;
        return activity.formatSignedMoney(gap, metric30.currency);
    }

    private String coverageDetail(TrendAnalytics.Metric metric30, UpdateAnalytics.Summary updates30) {
        if (!metric30.complete) {
            return "近 30 天快照不足，先用 " + updates30.count + " 次流水做参考。";
        }
        if (updates30.count == 0) {
            return "没有更新流水，快照变化无法拆解来源。";
        }
        if (activity.settings.hideAmounts) {
            return "近 30 天有 " + updates30.count + " 次更新流水，金额已隐藏。";
        }
        double gap = metric30.change - updates30.delta;
        return "近 30 天快照变化与同窗口流水净影响的差额；差额越小，来源越清楚。当前差额 "
                + activity.formatSignedMoney(gap, metric30.currency) + "。";
    }

    private int coverageColor(TrendAnalytics.Metric metric30, UpdateAnalytics.Summary updates30) {
        if (!metric30.complete || updates30.count == 0 || activity.settings.hideAmounts) {
            return MoneyManagerActivity.AMBER;
        }
        double tolerance = Math.max(1, Math.abs(metric30.change) * 0.25);
        return Math.abs(metric30.change - updates30.delta) <= tolerance
                ? MoneyManagerActivity.ACCENT
                : MoneyManagerActivity.AMBER;
    }

    private View healthRow(String title, String value, String detail, int color) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(activity.dp(12), activity.dp(10), activity.dp(12), activity.dp(10));
        row.setBackground(activity.cardBackground(MoneyManagerActivity.ROW_SURFACE, MoneyManagerActivity.PANEL_BORDER));
        LinearLayout.LayoutParams rowParams = activity.lp(-1, -2);
        rowParams.topMargin = activity.dp(8);
        row.setLayoutParams(rowParams);

        LinearLayout header = activity.row();
        header.addView(activity.text("●", 15, color, Typeface.BOLD));
        header.addView(activity.text("  " + title, 14, MoneyManagerActivity.INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView metric = activity.text(value, 14, MoneyManagerActivity.MUTED, Typeface.BOLD);
        metric.setGravity(Gravity.RIGHT);
        header.addView(metric);
        row.addView(header);

        LinearLayout.LayoutParams detailParams = activity.lp(-1, -2);
        detailParams.topMargin = activity.dp(6);
        row.addView(activity.text(detail, 12, MoneyManagerActivity.MUTED, Typeface.NORMAL), detailParams);
        return row;
    }

    private static final class TrendWindow {
        int snapshotCount;
        int intervals;
        int upCount;
        int downCount;
        int flatCount;
    }
}
