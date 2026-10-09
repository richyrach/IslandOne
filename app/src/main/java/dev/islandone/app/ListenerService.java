package dev.islandone.app;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

public final class ListenerService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (notification == null || getPackageName().equals(notification.getPackageName())) return;
        Notification n = notification.getNotification();
        if ((n.flags & Notification.FLAG_ONGOING_EVENT) != 0 ||
                Notification.CATEGORY_SERVICE.equals(n.category) ||
                Notification.CATEGORY_TRANSPORT.equals(n.category)) return;

        CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence c = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (TextUtils.isEmpty(t) || TextUtils.isEmpty(c)) return;
        IslandEvents.dispatch(new IslandEvents.Alert(
                t.toString(), c.toString(), n.contentIntent));
    }
}
