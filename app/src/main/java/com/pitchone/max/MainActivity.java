package com.pitchone.max;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class MainActivity extends Activity {
    private static final String LOCAL_ORIGIN = "https://pitchone.local/";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(5, 9, 9));
        getWindow().setNavigationBarColor(Color.rgb(5, 9, 9));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(5, 9, 9));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                String scheme = uri.getScheme();

                if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                        && "pitchone.local".equalsIgnoreCase(host)) {
                    return false;
                }

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });

        if (savedInstanceState == null) {
            loadBundledApp();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void loadBundledApp() {
        try {
            StringBuilder encoded = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                InputStream in = getAssets().open("p" + i + ".txt");
                ByteArrayOutputStream part = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    part.write(buffer, 0, n);
                }
                in.close();
                encoded.append(part.toString("UTF-8"));
            }

            byte[] gzipBytes = Base64.decode(encoded.toString(), Base64.DEFAULT);
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipBytes));
            ByteArrayOutputStream htmlBytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = gzip.read(buffer)) != -1) {
                htmlBytes.write(buffer, 0, n);
            }
            gzip.close();

            String html = new String(htmlBytes.toByteArray(), StandardCharsets.UTF_8);
            webView.loadDataWithBaseURL(LOCAL_ORIGIN, html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            String safeMessage = e.getMessage() == null ? "Unknown startup error" : e.getMessage().replace("<", "&lt;").replace(">", "&gt;");
            String errorHtml = "<html><body style='background:#050909;color:#fff;font-family:sans-serif;padding:24px'>"
                    + "<h2>PITCHONE MAX</h2><p>The offline app could not start.</p><p>" + safeMessage + "</p></body></html>";
            webView.loadDataWithBaseURL(LOCAL_ORIGIN, errorHtml, "text/html", "UTF-8", null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
