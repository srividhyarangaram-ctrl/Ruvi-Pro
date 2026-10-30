package app.ruvi.privateapp;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Bundle;
import android.app.Notification;
import org.json.JSONArray;
import org.json.JSONObject;

/** User-opt-in notification listener. Keeps only redacted possible payment alerts on device. */
public class PaymentNotificationListener extends NotificationListenerService {
    private static final String PREFS = "ruvi_alerts";
    private static final String KEY = "events";
    private static final int MAX = 100;

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        Bundle e = sbn.getNotification().extras;
        if (e == null) return;
        String title = String.valueOf(e.getCharSequence(Notification.EXTRA_TITLE, ""));
        String body = String.valueOf(e.getCharSequence(Notification.EXTRA_TEXT, ""));
        String combined = (title + " " + body).toLowerCase(java.util.Locale.ROOT);
        if (!combined.matches("(?s).*(paid|debited|payment|charged|subscription|renewal|autopay|mandate|upi).*")) return;
        // No full notification content, card number, account number, or sender text is persisted.
        try {
            JSONObject record = new JSONObject();
            record.put("source", sbn.getPackageName());
            record.put("time", sbn.getPostTime());
            record.put("title", title.substring(0,Math.min(title.length(),100)));
            record.put("snippet", body.substring(0,Math.min(body.length(),240)));
            record.put("category", combined.contains("subscription") || combined.contains("renewal") ? "subscription_possible" : "payment_possible");
            String hash = sbn.getPackageName() + ":" + sbn.getId() + ":" + (sbn.getPostTime()/60000);
            record.put("id", Integer.toHexString(hash.hashCode()));
            synchronized (PaymentNotificationListener.class) {
                JSONArray old = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "[]"));
                JSONArray next = new JSONArray();
                next.put(record);
                for (int i=0; i<old.length() && next.length()<MAX; i++) {
                    JSONObject item = old.optJSONObject(i);
                    if (item != null && !record.optString("id").equals(item.optString("id"))) next.put(item);
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, next.toString()).apply();
            }
        } catch (Exception ignored) { }
    }
}
