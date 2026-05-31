package com.computerascience.moneymanager.domain;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class UpdateClient {
    private UpdateClient() {
    }

    public static UpdateInfo fetchLatest(String infoUrl) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(infoUrl).openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "MoneyManagerAndroid");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Update HTTP " + status);
            }

            String raw;
            try (InputStream input = connection.getInputStream()) {
                raw = readUtf8(input);
            }
            return UpdateInfo.fromJson(raw);
        } finally {
            connection.disconnect();
        }
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

    public static final class UpdateInfo {
        public final String versionName;
        public final long versionCode;
        public final String apkUrl;
        public final String commit;
        public final String builtAt;

        private UpdateInfo(
                String versionName,
                long versionCode,
                String apkUrl,
                String commit,
                String builtAt
        ) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.apkUrl = apkUrl;
            this.commit = commit;
            this.builtAt = builtAt;
        }

        static UpdateInfo fromJson(String raw) throws Exception {
            JSONObject json = new JSONObject(raw);
            return new UpdateInfo(
                    json.optString("versionName", ""),
                    json.optLong("versionCode", 0),
                    json.optString("apkUrl", ""),
                    json.optString("commit", ""),
                    json.optString("builtAt", "")
            );
        }

        public boolean isNewerThan(long currentVersionCode) {
            return versionCode > currentVersionCode;
        }

        public String displayVersion() {
            String name = versionName.isEmpty() ? "--" : versionName;
            return name + " (" + versionCode + ")";
        }
    }
}
