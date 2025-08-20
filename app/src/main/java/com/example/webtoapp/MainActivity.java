package com.example.webtoapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Patterns;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etWebsiteUrl, etAppName;
    private Button btnCreateApp, btnPreview;
    private ImageView ivFavicon;
    private TextView tvPreviewTitle;
    private WebView hiddenWebView;
    private RecyclerView rvCreatedApps;
    private WebsiteAdapter adapter;
    private List<WebsiteApp> createdApps;

    private ProgressDialog progressDialog;
    private ExecutorService executor;
    private Handler mainHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        setupRecyclerView();
        setupClickListeners();

        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initViews() {
        etWebsiteUrl = findViewById(R.id.etWebsiteUrl);
        etAppName = findViewById(R.id.etAppName);
        btnCreateApp = findViewById(R.id.btnCreateApp);
        btnPreview = findViewById(R.id.btnPreview);
        ivFavicon = findViewById(R.id.ivFavicon);
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        hiddenWebView = findViewById(R.id.hiddenWebView);
        rvCreatedApps = findViewById(R.id.rvCreatedApps);
    }

    private void setupWebView() {
        hiddenWebView.getSettings().setJavaScriptEnabled(true);
        hiddenWebView.setVisibility(View.GONE);
        hiddenWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Get page title
                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    etAppName.setText(title);
                    tvPreviewTitle.setText(title);
                }

                // Get favicon
                view.evaluateJavascript(
                        "(function() { " +
                                "var link = document.querySelector('link[rel*=\"icon\"]'); " +
                                "return link ? link.href : ''; " +
                                "})()",
                        result -> {
                            String faviconUrl = result.replace("\"", "");
                            if (!faviconUrl.isEmpty()) {
                                loadFavicon(faviconUrl);
                            }
                        }
                );

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }
    private void setupRecyclerView() {
        createdApps = new ArrayList<>();
        adapter = new WebsiteAdapter(createdApps, this::openWebApp);
        rvCreatedApps.setLayoutManager(new LinearLayoutManager(this));
        rvCreatedApps.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnPreview.setOnClickListener(v -> previewWebsite());
        btnCreateApp.setOnClickListener(v -> createWebApp());
    }

    private void previewWebsite() {
        String url = etWebsiteUrl.getText().toString().trim();
        if (!isValidUrl(url)) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Loading", "Fetching website info...", true);

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        hiddenWebView.loadUrl(url);
    }
    private void createWebApp() {
        String url = etWebsiteUrl.getText().toString().trim();
        String appName = etAppName.getText().toString().trim();

        if (!isValidUrl(url)) {
            Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (appName.isEmpty()) {
            Toast.makeText(this, "Please enter an app name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        createShortcut(url, appName);
    }

    private void createShortcut(String url, String appName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                    Intent intent = new Intent(this, WebViewActivity.class);
                    intent.putExtra("url", url);
                    intent.putExtra("appName", appName);
                    intent.setAction(Intent.ACTION_VIEW);

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "shortcut_" + System.currentTimeMillis())
                            .setShortLabel(appName)
                            .setLongLabel(appName)
                            .setIcon(Icon.createWithResource(this, R.drawable.ic_web_app))
                            .setIntent(intent)
                            .build();

                    shortcutManager.requestPinShortcut(shortcut, null);

                    // Add to created apps list
                    WebsiteApp websiteApp = new WebsiteApp(appName, url);
                    createdApps.add(websiteApp);
                    adapter.notifyItemInserted(createdApps.size() - 1);

                    Toast.makeText(this, "Web app created! Check your home screen.", Toast.LENGTH_LONG).show();

                    // Clear inputs
                    etWebsiteUrl.setText("");
                    etAppName.setText("");
                    ivFavicon.setImageResource(R.drawable.ic_web_app);
                    tvPreviewTitle.setText("");
                } else {
                    Toast.makeText(this, "Shortcut creation not supported", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "This feature requires Android 7.1 or higher", Toast.LENGTH_SHORT).show();
        }
    }
    private void loadFavicon(String faviconUrl) {
        executor.execute(() -> {
            try {
                Bitmap favicon = IconDownloader.downloadIcon(faviconUrl);
                mainHandler.post(() -> {
                    if (favicon != null) {
                        ivFavicon.setImageBitmap(favicon);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean isValidUrl(String url) {
        if (url.isEmpty()) return false;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        return Patterns.WEB_URL.matcher(url).matches();
    }

    private void openWebApp(WebsiteApp app) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", app.getUrl());
        intent.putExtra("appName", app.getName());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}