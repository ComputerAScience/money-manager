package com.computerascience.moneymanager.model;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PortfolioSettings {
    public static final String[] COMMON_CURRENCIES = {"CNY", "USD", "HKD", "EUR", "JPY"};
    private static final String[] DEFAULT_INVESTMENT_CATEGORIES = {
            "投资账户",
            "基金",
            "加密资产",
            "券商持仓",
            "股票持仓",
            "券商现金"
    };

    public String baseCurrency;
    public boolean hideAmounts;
    public double netWorthTarget;
    public long netWorthTargetDate;
    public final Map<String, Double> ratesToBase;
    public final Map<String, Double> allocationTargets;
    public final List<String> customCategories;
    public final Set<String> investmentCategories;

    public PortfolioSettings() {
        baseCurrency = "CNY";
        hideAmounts = false;
        netWorthTarget = 0;
        netWorthTargetDate = 0;
        ratesToBase = new HashMap<>();
        allocationTargets = new HashMap<>();
        customCategories = new ArrayList<>();
        investmentCategories = new LinkedHashSet<>();
        ratesToBase.put("CNY", 1.0);
        ratesToBase.put("USD", 7.2);
        ratesToBase.put("HKD", 0.92);
        ratesToBase.put("EUR", 7.85);
        ratesToBase.put("JPY", 0.05);
        for (String category : DEFAULT_INVESTMENT_CATEGORIES) {
            investmentCategories.add(category);
        }
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
        JSONArray categories = json.optJSONArray("customCategories");
        if (categories != null) {
            for (int index = 0; index < categories.length(); index += 1) {
                settings.addCustomCategory(categories.optString(index, ""));
            }
        }
        if (json.has("investmentCategories")) {
            settings.investmentCategories.clear();
            JSONArray investment = json.optJSONArray("investmentCategories");
            if (investment != null) {
                for (int index = 0; index < investment.length(); index += 1) {
                    String category = cleanCategory(investment.optString(index, ""));
                    if (!category.isEmpty()) {
                        settings.investmentCategories.add(category);
                    }
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
        copy.customCategories.clear();
        copy.customCategories.addAll(source.customCategories);
        copy.investmentCategories.clear();
        copy.investmentCategories.addAll(source.investmentCategories);
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
        JSONArray categories = new JSONArray();
        for (String category : customCategories) {
            categories.put(category);
        }
        json.put("customCategories", categories);
        JSONArray investment = new JSONArray();
        for (String category : investmentCategories) {
            investment.put(category);
        }
        json.put("investmentCategories", investment);
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

    public void addCustomCategory(String category) {
        String cleaned = cleanCategory(category);
        if (cleaned.isEmpty() || customCategories.contains(cleaned)) {
            return;
        }
        customCategories.add(cleaned);
    }

    public boolean isInvestmentCategory(String category) {
        return investmentCategories.contains(cleanCategory(category));
    }

    public void setInvestmentCategory(String category, boolean included) {
        String cleaned = cleanCategory(category);
        if (cleaned.isEmpty()) {
            return;
        }
        if (included) {
            investmentCategories.add(cleaned);
        } else {
            investmentCategories.remove(cleaned);
        }
    }

    public void clearInvestmentCategories() {
        investmentCategories.clear();
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

    public static String cleanCategory(String category) {
        return category == null ? "" : category.trim();
    }
}
