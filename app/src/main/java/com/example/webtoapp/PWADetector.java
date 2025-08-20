package com.example.webtoapp;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PWADetector {

    public boolean detectPWA(String websiteUrl) {
        try {
            // Check for web app manifest
            String manifestUrl = findManifestUrl(websiteUrl);
            if (manifestUrl != null) {
                return validateManifest(manifestUrl);
            }

            // Check for service worker
            return checkServiceWorker(websiteUrl);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String findManifestUrl(String websiteUrl) {
        try {
            URL url = new URL(websiteUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("manifest.json") || line.contains("manifest.webmanifest")) {
                    // Extract manifest URL from link tag
                    int start = line.indexOf("href=\"") + 6;
                    int end = line.indexOf("\"", start);
                    if (start > 5 && end > start) {
                        String manifestPath = line.substring(start, end);
                        if (manifestPath.startsWith("/")) {
                            return websiteUrl + manifestPath;
                        } else if (manifestPath.startsWith("http")) {
                            return manifestPath;
                        } else {
                            return websiteUrl + "/" + manifestPath;
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean validateManifest(String manifestUrl) {
        try {
            URL url = new URL(manifestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder jsonString = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();

            JSONObject manifest = new JSONObject(jsonString.toString());

            // Check for required PWA properties
            return manifest.has("name") &&
                    manifest.has("start_url") &&
                    manifest.has("display") &&
                    manifest.has("icons");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkServiceWorker(String websiteUrl) {
        try {
            // Try common service worker paths
            String[] swPaths = {"/sw.js", "/service-worker.js", "/serviceworker.js"};

            for (String path : swPaths) {
                URL url = new URL(websiteUrl + path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");

                if (connection.getResponseCode() == 200) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}