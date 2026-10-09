package dev.islandone.app;

import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;

final class IslandEvents {
    interface Listener { void onAlert(Alert alert); }
    static final class Alert {
        final String title, text;
        final PendingIntent action;
        Alert(String title, String text, PendingIntent action) {
            this.title = title;
            this.text = text;
            this.action = action;
        }
    }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Listener listener;
    static void listen(Listener active) { listener = active; }
    static void dispatch(Alert alert) {
        MAIN.post(() -> { if (listener != null) listener.onAlert(alert); });
    }
    private IslandEvents() { }
}
