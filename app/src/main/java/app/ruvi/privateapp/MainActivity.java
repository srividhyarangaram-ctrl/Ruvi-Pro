package app.ruvi.privateapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends Activity {

    private WebView view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new WebView(this);
        setContentView(view);

        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        WebViewAssetLoader loader =
            new WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    new WebViewAssetLoader.AssetsPathHandler(this)
                )
                .build();

        view.setWebChromeClient(new WebChromeClient());

        view.setWebViewClient(new WebViewClientCompat() {

            @Override
public WebResourceResponse shouldInterceptRequest(
    WebView webView,
    WebResourceRequest request
) {
    return loader.shouldInterceptRequest(request.getUrl());
}

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView webView,
                WebResourceRequest request
            ) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();

                if (!"https".equals(scheme)
                    && !"http".equals(scheme)) {
                    return true;
                }

                if (!"appassets.androidplatform.net"
                        .equalsIgnoreCase(uri.getHost())) {
                    startActivity(
                        new Intent(Intent.ACTION_VIEW, uri)
                    );
                    return true;
                }

                return false;
            }
        });

        view.loadUrl(
            "https://appassets.androidplatform.net/assets/index.html"
        );
    }

    @Override
    public void onBackPressed() {
        if (view.canGoBack()) {
            view.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
