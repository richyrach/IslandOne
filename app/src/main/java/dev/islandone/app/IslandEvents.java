package dev.islandone.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;

final class IslandEvents {
    interface Listener { void onAlert(Alert alert); }
    static final class Alert {
        final String title, text;
        final PendingIntent action;
        final Notification.Action[] buttons;
        Alert(String title,String text,PendingIntent action,Notification.Action[] buttons) {
            this.title=title;
            this.text=text;
            this.action=action;
            this.buttons=buttons;
        }
    }
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static Listener listener;
    static void listen(Listener active){listener=active;}
    static void dispatch(Alert alert){
        MAIN.post(()->{if(listener!=null)listener.onAlert(alert);});
    }
    private IslandEvents(){}
}
