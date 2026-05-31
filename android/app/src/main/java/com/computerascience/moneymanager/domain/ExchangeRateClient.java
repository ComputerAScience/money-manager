package com.computerascience.moneymanager.domain;

import com.computerascience.moneymanager.model.PortfolioSettings;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExchangeRateClient {
    private ExchangeRateClient() {
    }

    public static Map<String, Double> fetchRatesToBase(String baseCurrency) throws Exception {
        String targets = realtimeRateTargets();
        URL url = new URL("https://api.frankfurter.dev/v1/latest?base=USD&symbols=" + targets);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "MoneyManagerAndroid");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("FX HTTP " + status);
            }

            String raw;
            try (InputStream input = connection.getInputStream()) {
                raw = readUtf8(input);
            }
            return parseRatesToBase(raw, baseCurrency);
        } finally {
            connection.disconnect();
        }
    }

    private static Map<String, Double> parseRatesToBase(String raw, String baseCurrency) throws Exception {
        JSONObject json = new JSONObject(raw);
        JSONObject rates = json.getJSONObject("rates");
        Map<String, Double> usdToCurrency = new HashMap<>();
        usdToCurrency.put("USD", 1.0);
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            if (!"USD".equals(currency) && rates.has(currency)) {
                double value = rates.optDouble(currency, 0);
                if (value > 0) {
                    usdToCurrency.put(currency, value);
                }
            }
        }

        Double usdToBase = usdToCurrency.get(baseCurrency);
        if (usdToBase == null || usdToBase <= 0) {
            throw new IOException("Missing base rate");
        }

        Map<String, Double> ratesToBase = new HashMap<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            Double usdToTarget = usdToCurrency.get(currency);
            if (usdToTarget != null && usdToTarget > 0) {
                ratesToBase.put(currency, currency.equals(baseCurrency) ? 1.0 : usdToBase / usdToTarget);
            }
        }
        return ratesToBase;
    }

    private static String realtimeRateTargets() {
        List<String> targets = new ArrayList<>();
        for (String currency : PortfolioSettings.COMMON_CURRENCIES) {
            if (!"USD".equals(currency)) {
                targets.add(currency);
            }
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < targets.size(); index += 1) {
            if (index > 0) {
                builder.append(",");
            }
            builder.append(targets.get(index));
        }
        return builder.toString();
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toString(StandardCharsets.UTF_8.name());
    }
}
