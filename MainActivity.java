package app.ruvi.privateapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

public class MainActivity extends Activity {

    private static final int AUTH_REQUEST = 754;
    private static final String GMAIL_SCOPE =
        "https://www.googleapis.com/auth/gmail.readonly";

    private WebView web;
    private AuthorizationClient auth;

    private void sendToJs(String function, String value) {
        runOnUiThread(() -> {
            if (web != null) {
                web.evaluateJavascript(
                    "if(typeof window." + function +
                    "==='function'){window." + function +
                    "(" + JSONObject.quote(value) + ");}",
                    null
                );
            }
        });
    }

    public class RuviBridge {

        @JavascriptInterface
        public String alerts() {
            return getSharedPreferences(
                "ruvi_alerts", MODE_PRIVATE
            ).getString("events", "[]");
        }

        @JavascriptInterface
        public void permission() {
            runOnUiThread(() ->
                startActivity(new Intent(
                    Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                ))
            );
        }

        @JavascriptInterface
        public void clearAlerts() {
            getSharedPreferences(
                "ruvi_alerts", MODE_PRIVATE
            ).edit().remove("events").apply();
        }

        @JavascriptInterface
        public void syncGmail() {
            runOnUiThread(() -> authorizeGmail());
        }

        @JavascriptInterface
        public void disconnectGmail() {
            runOnUiThread(() -> {
                auth.revokeAccess(
                    Arrays.asList(new Scope(GMAIL_SCOPE))
                ).addOnSuccessListener(unused ->
                    sendToJs(
                        "ruviGmailStatus",
                        "Gmail disconnected"
                    )
                ).addOnFailureListener(error ->
                    sendToJs(
                        "ruviGmailStatus",
                        "Disconnect failed: " +
                        error.getMessage()
                    )
                );
            });
        }
    }

    private void authorizeGmail() {
        sendToJs(
            "ruviGmailStatus",
            "Checking Gmail authorization..."
        );

        AuthorizationRequest request =
            AuthorizationRequest.builder()
                .setRequestedScopes(
                    Arrays.asList(new Scope(GMAIL_SCOPE))
                )
                .build();

        auth.authorize(request)
            .addOnSuccessListener(result -> {
                if (result.hasResolution()) {
                    try {
                        startIntentSenderForResult(
                            result.getPendingIntent()
                                .getIntentSender(),
                            AUTH_REQUEST,
                            null, 0, 0, 0
                        );
                    } catch (
                        IntentSender.SendIntentException error
                    ) {
                        sendToJs(
                            "ruviGmailStatus",
                            "Authorization failed: " +
                            error.getMessage()
                        );
                    }
                } else {
                    scanGmail(result.getAccessToken());
                }
            })
            .addOnFailureListener(error ->
                sendToJs(
                    "ruviGmailStatus",
                    "Google authorization failed: " +
                    error.getMessage()
                )
            );
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(
            requestCode, resultCode, data
        );

        if (requestCode != AUTH_REQUEST) return;

        if (resultCode != RESULT_OK || data == null) {
            sendToJs(
                "ruviGmailStatus",
                "Gmail authorization cancelled"
            );
            return;
        }

        try {
            scanGmail(
                auth.getAuthorizationResultFromIntent(data)
                    .getAccessToken()
            );
        } catch (Exception error) {
            sendToJs(
                "ruviGmailStatus",
                "Authorization error: " +
                error.getMessage()
            );
        }
    }

    private JSONObject gmailGet(
        String path,
        String token
    ) throws Exception {

        HttpURLConnection connection =
            (HttpURLConnection) new URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/"
                + path
            ).openConnection();

        connection.setRequestProperty(
            "Authorization", "Bearer " + token
        );
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        try {
            int status = connection.getResponseCode();

            if (status != 200) {
                throw new Exception(
                    "Gmail API returned HTTP " + status
                );
            }

            try (InputStream input =
                     connection.getInputStream()) {

                ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int count;

                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }

                return new JSONObject(
                    output.toString("UTF-8")
                );
            }
        } finally {
            connection.disconnect();
        }
    }

    private String getHeader(
        JSONObject message,
        String name
    ) {
        JSONObject payload =
            message.optJSONObject("payload");

        if (payload == null) return "";

        JSONArray headers =
            payload.optJSONArray("headers");

        if (headers == null) return "";

        for (int i = 0; i < headers.length(); i++) {
            JSONObject header =
                headers.optJSONObject(i);

            if (header != null &&
                name.equalsIgnoreCase(
                    header.optString("name")
                )) {
                return header.optString("value");
            }
        }

        return "";
    }

    private void scanGmail(String token) {
        if (token == null || token.isEmpty()) {
            sendToJs(
                "ruviGmailStatus",
                "No Gmail access token received"
            );
            return;
        }

        new Thread(() -> {
            try {
                sendToJs(
                    "ruviGmailStatus",
                    "Scanning payment receipts..."
                );

                String query = URLEncoder.encode(
                    "newer_than:2y " +
                    "(subscription OR renewal OR recurring " +
                    "OR invoice OR payment OR UPI OR debited) " +
                    "-in:spam -in:trash",
                    "UTF-8"
                );

                JSONObject response = gmailGet(
                    "messages?maxResults=100&q=" + query,
                    token
                );

                JSONArray messages =
                    response.optJSONArray("messages");

                JSONArray results = new JSONArray();

                if (messages != null) {
                    for (int i = 0;
                         i < messages.length(); i++) {

                        String id = messages
                            .getJSONObject(i)
                            .optString("id");

                        if (id.isEmpty()) continue;

                        JSONObject message = gmailGet(
                            "messages/" + id +
                            "?format=metadata" +
                            "&metadataHeaders=Subject" +
                            "&metadataHeaders=From" +
                            "&metadataHeaders=Date",
                            token
                        );

                        JSONObject record =
                            new JSONObject();

                        record.put("id", "gmail:" + id);
                        record.put("source", "gmail");
                        record.put(
                            "subject",
                            getHeader(message, "Subject")
                        );
                        record.put(
                            "from",
                            getHeader(message, "From")
                        );
                        record.put(
                            "snippet",
                            message.optString("snippet")
                        );
                        record.put(
                            "time",
                            message.optLong("internalDate")
                        );

                        results.put(record);
                    }
                }

                sendToJs(
                    "ruviGmailResults",
                    results.toString()
                );

                sendToJs(
                    "ruviGmailStatus",
                    "Gmail scan complete: " +
                    results.length() +
                    " possible receipts found"
                );

            } catch (Exception error) {
                sendToJs(
                    "ruviGmailStatus",
                    "Gmail sync failed: " +
                    error.getMessage()
                );
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        auth = Identity.getAuthorizationClient(this);

        web = new WebView(this);
        setContentView(web);

        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);

        web.addJavascriptInterface(
            new RuviBridge(), "RuviNative"
        );

        WebViewAssetLoader loader =
            new WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    new WebViewAssetLoader
                        .AssetsPathHandler(this)
                )
                .build();

        web.setWebChromeClient(
            new WebChromeClient()
        );

        web.setWebViewClient(
            new WebViewClientCompat() {

                @Override
                public WebResourceResponse
                shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
                ) {
                    return loader.shouldInterceptRequest(
                        request.getUrl()
                    );
                }

                @Override
                public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
                ) {
                    Uri uri = request.getUrl();

                    if ("appassets.androidplatform.net"
                            .equalsIgnoreCase(uri.getHost())) {
                        return false;
                    }

                    String scheme = uri.getScheme();

                    if ("https".equals(scheme) ||
                        "http".equals(scheme)) {
                        startActivity(new Intent(
                            Intent.ACTION_VIEW, uri
                        ));
                    }

                    return true;
                }
            }
        );

        web.loadUrl(
            "https://appassets.androidplatform.net/assets/index.html"
        );
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
