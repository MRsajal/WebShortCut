package com.example.webtoapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etWebsiteUrl, etAppName;
    private Button btnCreateApp, btnPreview, btnSelectIcon;
    private ImageView ivFavicon;
    private TextView tvPreviewTitle;
    private WebView hiddenWebView;
    private RecyclerView rvCreatedApps;
    private WebsiteAdapter adapter;
    private List<WebsiteApp> createdApps;

    private ProgressDialog progressDialog;
    private ExecutorService executor;
    private Handler mainHandler;

    private Bitmap selectedIcon = null;
    private IconManager iconManager;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupWebView();
        setupRecyclerView();
        setupClickListeners();
        setupActivityLaunchers();

        executor = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        iconManager = new IconManager(this);
    }

    private void initViews() {
        etWebsiteUrl = findViewById(R.id.etWebsiteUrl);
        etAppName = findViewById(R.id.etAppName);
        btnCreateApp = findViewById(R.id.btnCreateApp);
        btnPreview = findViewById(R.id.btnPreview);
        btnSelectIcon = findViewById(R.id.btnSelectIcon);
        ivFavicon = findViewById(R.id.ivFavicon);
        tvPreviewTitle = findViewById(R.id.tvPreviewTitle);
        hiddenWebView = findViewById(R.id.hiddenWebView);
        rvCreatedApps = findViewById(R.id.rvCreatedApps);
    }

    private void setupActivityLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            selectedIcon = iconManager.resizeIcon(bitmap, 192, 192);
                            ivFavicon.setImageBitmap(selectedIcon);
                            Toast.makeText(this, "Icon selected successfully", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();
                    } else {
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupWebView() {
        hiddenWebView.getSettings().setJavaScriptEnabled(true);
        hiddenWebView.setVisibility(View.GONE);
        hiddenWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String title = view.getTitle();
                if (title != null && !title.isEmpty()) {
                    etAppName.setText(title);
                    tvPreviewTitle.setText(title);
                }

                extractFavicon(view, url);

                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    private void extractFavicon(WebView view, String url) {
        view.evaluateJavascript(
                "(function() { " +
                        "var link = document.querySelector('link[rel*=\"icon\"]'); " +
                        "return link ? link.href : ''; " +
                        "})()",
                result -> {
                    String faviconUrl = result.replace("\"", "");
                    if (!faviconUrl.isEmpty()) {
                        loadFavicon(faviconUrl);
                    } else {
                        tryAlternativeFavicons(url);
                    }
                }
        );
    }

    private void tryAlternativeFavicons(String baseUrl) {
        executor.execute(() -> {
            try {
                Uri uri = Uri.parse(baseUrl);
                String domain = uri.getScheme() + "://" + uri.getHost();

                String[] faviconPaths = {
                        domain + "/favicon.ico",
                        domain + "/favicon.png",
                        domain + "/apple-touch-icon.png",
                        domain + "/apple-touch-icon-precomposed.png"
                };

                for (String faviconUrl : faviconPaths) {
                    Bitmap favicon = IconDownloader.downloadIcon(faviconUrl);
                    if (favicon != null) {
                        mainHandler.post(() -> {
                            selectedIcon = favicon;
                            ivFavicon.setImageBitmap(favicon);
                        });
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
        btnSelectIcon.setOnClickListener(v -> showIconSelectionDialog());
    }

    private void showIconSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select App Icon");

        String[] options = {
                "Use Website Favicon",
                "Choose from Gallery",
                "Generate Text Icon",
                "Use Default Icon"
        };

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    useWebsiteFavicon();
                    break;
                case 1:
                    selectFromGallery();
                    break;
                case 2:
                    generateTextIcon();
                    break;
                case 3:
                    useDefaultIcon();
                    break;
            }
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void useWebsiteFavicon() {
        String url = etWebsiteUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter a website URL first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        progressDialog = ProgressDialog.show(this, "Loading", "Getting website favicon...", true);
        hiddenWebView.loadUrl(url);
    }

    private void selectFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void generateTextIcon() {
        String appName = etAppName.getText().toString().trim();
        if (appName.isEmpty()) {
            Toast.makeText(this, "Enter an app name first", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedIcon = iconManager.generateTextIcon(appName);
        ivFavicon.setImageBitmap(selectedIcon);
        Toast.makeText(this, "Text icon generated", Toast.LENGTH_SHORT).show();
    }

    private void useDefaultIcon() {
        selectedIcon = iconManager.getDefaultIcon();
        ivFavicon.setImageBitmap(selectedIcon);
        Toast.makeText(this, "Default icon selected", Toast.LENGTH_SHORT).show();
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

        showAppCreationDialog(url, appName);
    }

    private void showAppCreationDialog(String url, String appName) {
        String[] options = {
                "Create Shortcut (Quick)",
                "Create PWA App (Advanced)", 
                "Create Custom WebView App"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose App Type")
               .setItems(options, (dialog, which) -> {
                   switch (which) {
                       case 0:
                           createShortcut(url, appName);
                           break;
                       case 1:
                           createPWA(url, appName);
                           break;
                       case 2:
                           createCustomApp(url, appName);
                           break;
                   }
               })
               .setNegativeButton("Cancel", null)
               .show();
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

                    Icon icon;
                    if (selectedIcon != null) {
                        icon = Icon.createWithBitmap(selectedIcon);
                    } else {
                        icon = Icon.createWithResource(this, android.R.drawable.ic_menu_gallery);
                    }

                    ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "shortcut_" + System.currentTimeMillis())
                            .setShortLabel(appName)
                            .setLongLabel(appName)
                            .setIcon(icon)
                            .setIntent(intent)
                            .build();

                    shortcutManager.requestPinShortcut(shortcut, null);

                    addToCreatedApps(appName, url);
                    Toast.makeText(this, "Shortcut created! Check your home screen.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Shortcut creation not supported", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "This feature requires Android 7.1 or higher", Toast.LENGTH_SHORT).show();
        }
    }

    private void createPWA(String url, String appName) {
        // For simplicity, we'll create an enhanced WebView for PWA
        Toast.makeText(this, "Creating PWA-style app...", Toast.LENGTH_SHORT).show();
        createCustomApp(url, appName);
    }

    private void createCustomApp(String url, String appName) {
        Intent intent = new Intent(this, EnhancedWebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("appName", appName);
        startActivity(intent);

        addToCreatedApps(appName, url);
        Toast.makeText(this, "Custom app created!", Toast.LENGTH_SHORT).show();
    }

    private void addToCreatedApps(String appName, String url) {
        WebsiteApp websiteApp = new WebsiteApp(appName, url);
        if (selectedIcon != null) {
            websiteApp.setIcon(selectedIcon);
        }
        createdApps.add(websiteApp);
        adapter.notifyItemInserted(createdApps.size() - 1);

        // Clear inputs
        etWebsiteUrl.setText("");
        etAppName.setText("");
        ivFavicon.setImageResource(android.R.drawable.ic_menu_gallery);
        tvPreviewTitle.setText("");
        selectedIcon = null;
    }

    private void loadFavicon(String faviconUrl) {
        executor.execute(() -> {
            try {
                Bitmap favicon = IconDownloader.downloadIcon(faviconUrl);
                mainHandler.post(() -> {
                    if (favicon != null) {
                        selectedIcon = iconManager.resizeIcon(favicon, 192, 192);
                        ivFavicon.setImageBitmap(selectedIcon);
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