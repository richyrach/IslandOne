package dev.islandone.app;

import android.app.KeyguardManager;
import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import java.util.ArrayList;

public final class ListenerService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if(sbn==null||getPackageName().equals(sbn.getPackageName()))return;
        Notification n=sbn.getNotification();
        if(n==null||(n.flags&Notification.FLAG_ONGOING_EVENT)!=0||
            n.visibility==Notification.VISIBILITY_SECRET||
            Notification.CATEGORY_SERVICE.equals(n.category)||
            Notification.CATEGORY_TRANSPORT.equals(n.category))return;
        KeyguardManager lock=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        if(lock!=null&&lock.isKeyguardLocked())return;

        CharSequence title=n.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence message=n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if(TextUtils.isEmpty(title)||TextUtils.isEmpty(message))return;

        ArrayList<Notification.Action> available=new ArrayList<>();
        if(n.actions!=null) {
            for(Notification.Action action:n.actions) {
                if(action!=null&&action.actionIntent!=null&&
                    !TextUtils.isEmpty(action.title)&&available.size()<3){
                    available.add(action);
                }
            }
        }
        IslandEvents.dispatch(new IslandEvents.Alert(
            title.toString(),message.toString(),n.contentIntent,
            available.toArray(new Notification.Action[0])));
    }
}
