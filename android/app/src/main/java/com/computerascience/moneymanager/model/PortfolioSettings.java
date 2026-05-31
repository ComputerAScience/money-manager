package com.computerascience.moneymanager.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public final class PortfolioSettings {
    public static final String[] COMMON_CURRENCIES = {"CNY", "USD", "HKD", "EUR", "JPY"};

    public String baseCurrency;
    public boolean hideAmounts;
    public double netWorthTarget;
    public long netWorthTargetDate;
    public final Map<String, Double> ratesToBase;
    public final Map<String, Double> allocationTargets;

    public PortfolioSettings() {
        baseCurrency = "CNY";
        hideAmounts = false;
        netWorthTarget = 0;
        netWorthTargetDate = 0;
        ratesToBase = new HashMap<>();
        allocationTargets = new HashMap<>();
        ratesToBase.put("CNY", 1.0);
        ratesToBase.put("USD", 7.2);
        ratesToBase.put("HKD", 0.92);
        ratesToBase.put("EUR", 7.85);
        ratesToBase.put("JPY", 0.05);
    }

    public static PortfolioSettings fromJson(JSONObject json) {
        PortfolioSettings settings = new PortfolioSettings();
        settings.baseCurrency = cleanCurrency(json.optString("baseCurrency", settings.baseCurrency));
        settings.hideAmounts = json.optBoolean("hideAmounts", settings.hideAmounts);
        settings.netWorthTarget = Math.max(0, json.optDouble("netWorthTarget", settings.netWorthTarget));
        settings.netWorthTargetDate = Math.max(0, json.optLong("netWorthTargetDate", settings.netWorthTargetDate));
        JSONObject rates = json.optJSONObject("ratesToBase");
        if (rates != null) {
            Iterator<String> keys = rates.keys();
            while (keys.hasNext()) {
                String key = cleanCurrency(keys.next());
                double value = rates.optDouble(key, 0);
                if (!key.isEmpty() && value > 0) {
                    settings.ratesToBase.put(key, value);
                }
            }
        }
        JSONObject targets = json.optJSONObject("allocationTargets");
        if (targets != null) {
            Iterator<String> keys = targets.keys();
            while (keys.hasNext()) {
                String key = keys.next().trim();
                double value = targets.optDouble(key, 0);
                if (!key.isEmpty() && value > 0) {
                    settings.allocationTargets.put(key, value);
                }
            }
        }
        settings.ensureBaseRate();
        return settings;
    }

    public static PortfolioSettings copyOf(PortfolioSettings source) {
        PortfolioSettings copy = new PortfolioSettings();
        copy.baseCurrency = source.baseCurrency;
        copy.hideAmounts = source.hideAmounts;
        copy.netWorthTarget = source.netWorthTarget;
        copy.netWorthTargetDate = source.netWorthTargetDate;
        copy.ratesToBase.clear();
        copy.ratesToBase.putAll(source.ratesToBase);
        copy.allocationTargets.clear();
        copy.allocationTargets.putAll(source.allocationTargets);
        copy.ensureBaseRate();
        return copy;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("baseCurrency", baseCurrency);
        json.put("hideAmounts", hideAmounts);
        json.put("netWorthTarget", netWorthTarget);
        json.put("netWorthTargetDate", netWorthTargetDate);
        JSONObject rates = new JSONObject();
        for (Map.Entry<String, Double> entry : ratesToBase.entrySet()) {
            rates.put(entry.getKey(), entry.getValue());
        }
        json.put("ratesToBase", rates);
        JSONObject targets = new JSONObject();
        for (Map.Entry<String, Double> entry : allocationTargets.entrySet()) {
            targets.put(entry.getKey(), entry.getValue());
        }
        json.put("allocationTargets", targets);
        return json;
    }

    public double rateFor(String currency) {
        String cleaned = cleanCurrency(currency);
        if (cleaned.isEmpty() || cleaned.equals(baseCurrency)) {
            return 1.0;
        }
        Double rate = ratesToBase.get(cleaned);
        return rate == null || rate <= 0 ? 0 : rate;
    }

    public boolean hasRateFor(String currency) {
        String cleaned = cleanCurrency(currency);
        return cleaned.isEmpty() || cleaned.equals(baseCurrency) || rateFor(cleaned) > 0;
    }

    public void setRate(String currency, double rate) {
        String cleaned = cleanCurrency(currency);
        if (cleaned.isEmpty() || rate <= 0) {
            return;
        }
        ratesToBase.put(cleaned, rate);
    }

    public double targetForCategory(String category) {
        Double target = allocationTargets.get(category);
        return target == null || target <= 0 ? 0 : target;
    }

    public void setAllocationTarget(String category, double percent) {
        String cleaned = category == null ? "" : category.trim();
        if (cleaned.isEmpty()) {
            return;
        }
        if (percent <= 0) {
            allocationTargets.remove(cleaned);
            return;
        }
        allocationTargets.put(cleaned, percent);
    }

    public void clearAllocationTargets() {
        allocationTargets.clear();
    }

    public boolean hasAllocationTargets() {
        return !allocationTargets.isEmpty();
    }

    public boolean hasNetWorthTarget() {
        return netWorthTarget > 0 && netWorthTargetDate > 0;
    }

    public void ensureBaseRate() {
        if (baseCurrency == null || baseCurrency.trim().isEmpty()) {
            baseCurrency = "CNY";
        }
        baseCurrency = cleanCurrency(baseCurrency);
        ratesToBase.put(baseCurrency, 1.0);
    }

    public static String cleanCurrency(String currency) {
        return currency == null ? "" : currency.trim().toUpperCase(Locale.ROOT);
    }
}
