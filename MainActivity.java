package app.ruvi.privateapp;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.webkit.*;
import android.content.IntentSender;

import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.Scope;

import org.json.*;

import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    private WebView web;
    private AuthorizationClient auth;

    private static final int AUTH_REQUEST = 754;
    private static final String SCOPE =
        "https://www.googleapis.com/auth/gmail.readonly";

    private void js(String fn, String json) {
        runOnUiThread(() ->
            web.evaluateJavascript(
                "window." + fn + "(" +
                JSONObject.quote(json) + ")",
                null
            )
        );
    }

    public class Bridge {

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
            runOnUiThread(() -> authorize());
        }

        @JavascriptInterface
        public void disconnectGmail() {
            runOnUiThread(() -> {
                auth.revokeAccess(
                    Arrays.asList(new Scope(SCOPE))
                );
                js("ruviGmailStatus", "Gmail access revoked");
            });
        }
    }

    private void authorize() {
        js(
            "ruviGmailStatus",
            "Requesting Gmail read-only authorization…"
        );

        AuthorizationRequest request =
            AuthorizationRequest.builder()
                .setRequestedScopes(
                    Arrays.asList(new Scope(SCOPE))
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
                        IntentSender.SendIntentException e
                    ) {
                        js(
                            "ruviGmailStatus",
                            "Authorization launch failed: " +
                            e.getMessage()
                        );
                    }
                } else {
                    scan(result.getAccessToken());
                }
            })
            .addOnFailureListener(e ->
                js(
                    "ruviGmailStatus",
                    "Google authorization failed: " +
                    e.getMessage()
                )
            );
    }

    @Override
    protected void onActivityResult(
        int request,
        int result,
        Intent data
    ) {
        super.onActivityResult(request, result, data);

        if (request == AUTH_REQUEST) {
            try {
                if (result != RESULT_OK) {
                    throw new Exception(
                        "User cancelled authorization"
                    );
                }

                scan(
                    auth.getAuthorizationResultFromIntent(data)
                        .getAccessToken()
                );

            } catch (Exception e) {
                js(
                    "ruviGmailStatus",
                    "Gmail authorization: " + e.getMessage()
                );
            }
        }
    }

    private JSONObject get(
        String path,
        String token
    ) throws Exception {

        HttpURLConnection conn =
            (HttpURLConnection) new URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/"
                + path
            ).openConnection();

        conn.setRequestProperty(
            "Authorization",
            "Bearer " + token
        );

        conn.setConnectTimeout(12000);
        conn.setReadTimeout(12000);

        int code = conn.getResponseCode();

        if (code != 200) {
            throw new IOException(
                "Gmail API HTTP " + code +
                " (check Gmail API and OAuth consent)"
            );
        }

        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream b =
                new ByteArrayOutputStream();

            byte[] chunk = new byte[4096];
            int n;

            while ((n = in.read(chunk)) != -1) {
                b.write(chunk, 0, n);
            }

            return new JSONObject(
                b.toString("UTF-8")
            );

        } finally {
            conn.disconnect();
        }
    }

    private String header(
        JSONObject m,
        String key
    ) {
        JSONArray hs =
            m.optJSONObject("payload") != null
            ? m.optJSONObject("payload")
                .optJSONArray("headers")
            : null;

        if (hs != null) {
            for (int i = 0; i < hs.length(); i++) {
                JSONObject h = hs.optJSONObject(i);

                if (h != null &&
                    key.equalsIgnoreCase(
                        h.optString("name")
                    )) {
                    return h.optString("value");
                }
            }
        }

        return "";
    }

    private void scan(String token) {
        if (token == null || token.isEmpty()) {
            js(
                "ruviGmailStatus",
                "No Gmail access token returned"
            );
            return;
        }

        new Thread(() -> {
            try {
                js(
                    "ruviGmailStatus",
                    "Scanning Gmail receipt metadata…"
                );

                String q = URLEncoder.encode(
                    "newer_than:2y " +
                    "(subscription OR renewal OR recurring " +
                    "OR invoice OR payment OR UPI OR debited) " +
                    "-in:spam -in:trash",
                    "UTF-8"
                );

                JSONObject list = get(
                    "messages?maxResults=100&q=" + q,
                    token
                );

                JSONArray msgs =
                    list.optJSONArray("messages");

                JSONArray records = new JSONArray();

                if (msgs != null) {
                    for (int i = 0; i < msgs.length(); i++) {
                        String id = msgs
                            .getJSONObject(i)
                            .optString("id");

                        if (id.isEmpty()) continue;

                        JSONObject m = get(
                            "messages/" + id +
                            "?format=metadata" +
                            "&metadataHeaders=Subject" +
                            "&metadataHeaders=From" +
                            "&metadataHeaders=Date",
                            token
                        );

                        JSONObject rec = new JSONObject();

                        rec.put("id", "gmail:" + id);
                        rec.put("source", "gmail");
                        rec.put(
                            "subject",
                            header(m, "Subject")
                        );
                        rec.put(
                            "from",
                            header(m, "From")
                        );
                        rec.put(
                            "snippet",
                            m.optString("snippet")
                        );
                        rec.put(
                            "time",
                            m.optLong("internalDate")
                        );

                        records.put(rec);
                    }
                }

                js(
                    "ruviGmailResults",
                    records.toString()
                );

                js(
                    "ruviGmailStatus",
                    "Gmail scan complete: " +
                    records.length() +
                    " possible receipts. Review detected records."
                );

            } catch (Exception e) {
                js(
                    "ruviGmailStatus",
                    "Gmail scan failed: " +
                    e.getMessage()
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

        WebSettings s = web.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);

        web.addJavascriptInterface(
            new Bridge(),
            "RuviNative"
        );

        web.setWebViewClient(new WebViewClient());

        web.loadUrl(
            "file:///android_asset/index.html"
        );
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
