package dev.islandone.app;

import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

public final class IslandService extends Service implements IslandEvents.Listener {
    public static final String START="dev.islandone.app.START";
    public static final String STOP="dev.islandone.app.STOP";
    public static final String SETTINGS="dev.islandone.app.SETTINGS";
    private static final String CHANNEL="islandone_foreground";
    private static final int WHITE=0xFFF5F5FC, MUTED=0xFF9BA0AF, PURPLE=0xFFC6AAFF;
    private final Handler main=new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private FrameLayout bubble;
    private LinearLayout mini, details, alertPanel;
    private TextView miniLeft,miniRight,songName,artist,play,batteryText,timerText,alertText,elapsed,durationLabel;
    private ImageView miniArt;
    private ImageView cover;
    private SeekBar progress;
    private boolean expanded=false,charging=false,batteryInitialized=false,scrubbing=false;
    private int batteryPercent=-1;
    private long timerRemaining=-1;
    private CountDownTimer timer;
    private MediaSessionManager mediaSessions;
    private MediaController mediaController;
    private boolean registered=false;
    private ValueAnimator animation;
    private IslandEvents.Alert currentAlert;
    private final Runnable collapse=()->resize(false,true);
    private final Runnable clearAlert=()->{
        currentAlert=null;
        if(alertPanel!=null)alertPanel.setVisibility(View.GONE);
        if(expanded)resize(true,false);
    };
    private final Runnable tick=new Runnable(){
        int counter=0;
        @Override public void run(){
            if(bubble==null)return;
            if(counter++%3==0)scanMedia();
            if(expanded || timerRemaining>=0 || charging)update();
            main.postDelayed(this,(expanded&&playing()) || timerRemaining>=0?1000:5000);
        }
    };
    private final BroadcastReceiver powerReceiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){
            if(!Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction()))return;
            int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
            boolean plugged=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
            boolean newCharge=batteryInitialized&&!charging&&plugged;
            batteryInitialized=true;charging=plugged;
            batteryPercent=level>=0&&scale>0?level*100/scale:-1;
            update();
            if(newCharge)peek("Charging",batteryPercent+"% battery");
        }
    };
    private final MediaController.Callback mediaCallback=new MediaController.Callback(){
        @Override public void onMetadataChanged(MediaMetadata m){update();}
        @Override public void onPlaybackStateChanged(PlaybackState s){update();}
        @Override public void onSessionDestroyed(){scanMedia();}
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionChanges=active->scanMedia();

    private int d(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(d(radius));return g;
    }
    private LinearLayout row(){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);return r;
    }
    private LinearLayout col(){
        LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL);t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);return t;
    }
    private LinearLayout.LayoutParams space(int w,int h,int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:d(w),h<0?h:d(h));
        p.topMargin=d(top);return p;
    }
    private TextView control(String icon,Runnable run){
        TextView v=text(icon,23,WHITE,true);v.setGravity(Gravity.CENTER);
        v.setBackground(bg(0xFF262833,24));
        v.setOnClickListener(view->{run.run();autoClose(10000);});
        return v;
    }
    private void createBubble(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        bubble=new FrameLayout(this);
        bubble.setBackground(bg(Color.BLACK,27));
        bubble.setElevation(d(12));
        bubble.setClipToOutline(true);

        mini=row();mini.setPadding(d(7),0,d(14),0);
        miniArt=new ImageView(this);
        miniArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        miniArt.setBackground(bg(0xFF332246,9));
        miniArt.setClipToOutline(true);
        miniArt.setVisibility(View.GONE);
        mini.addView(miniArt,space(28,28,0));
        miniLeft=text("●",17,PURPLE,true);
        mini.addView(miniLeft,space(35,-1,0));
        miniRight=text("●",12,0xFF7FE3B8,true);
        TextView center=text(" ",12,WHITE,false);
        mini.addView(center,new LinearLayout.LayoutParams(0,-1,1));
        mini.addView(miniRight);
        mini.setOnClickListener(v->resize(true,true));
        FrameLayout.LayoutParams compactLp=new FrameLayout.LayoutParams(-1,-1);
        bubble.addView(mini,compactLp);

        details=col();details.setPadding(d(20),d(16),d(20),d(15));
        details.setVisibility(View.GONE);
        FrameLayout.LayoutParams detailLp=new FrameLayout.LayoutParams(-1,-1);
        bubble.addView(details,detailLp);

        LinearLayout top=row();
        TextView eyebrow=text("NOW PLAYING",10,0xFFD5D0DF,true);
        eyebrow.setLetterSpacing(.12f);
        top.addView(eyebrow,new LinearLayout.LayoutParams(0,d(23),1));
        TextView close=text("⌄",23,MUTED,true);close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v->resize(false,true));
        top.addView(close,space(35,30,0));
        details.addView(top);

        LinearLayout media=row();
        cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(bg(0xFF2E2442,15));cover.setClipToOutline(true);
        media.addView(cover,space(72,72,0));
        LinearLayout labels=col();labels.setPadding(d(14),0,d(4),0);
        songName=text("Not playing",17,WHITE,true);
        artist=text("Play something to get started",12,MUTED,false);
        labels.addView(songName);labels.addView(artist,space(-1,-2,5));
        media.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        details.addView(media,space(-1,73,13));

        progress=new SeekBar(this);progress.setMax(1000);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        progress.setThumbTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser){}
            @Override public void onStartTrackingTouch(SeekBar s){scrubbing=true;}
            @Override public void onStopTrackingTouch(SeekBar s){
                MediaMetadata m=mediaController==null?null:mediaController.getMetadata();
                if(m!=null&&mediaController!=null){
                    long duration=m.getLong(MediaMetadata.METADATA_KEY_DURATION);
                    if(duration>0)mediaController.getTransportControls().seekTo(duration*s.getProgress()/1000);
                }
                scrubbing=false;autoClose(10000);
            }
        });
        details.addView(progress,space(-1,30,8));
        LinearLayout times=row();
        elapsed=text("0:00",12,MUTED,false);
        durationLabel=text("0:00",12,MUTED,false);
        times.addView(elapsed,new LinearLayout.LayoutParams(0,-2,1));
        times.addView(durationLabel);
        details.addView(times,space(-1,19,0));
        LinearLayout transport=row();transport.setGravity(Gravity.CENTER);
        TextView previous=control("⏮",()->mediaAction(-1));
        play=control("▶",()->mediaAction(0));
        TextView next=control("⏭",()->mediaAction(1));
        transport.addView(previous,space(58,48,0));
        LinearLayout.LayoutParams middle=space(67,52,0);middle.leftMargin=d(18);middle.rightMargin=d(18);
        play.setBackground(bg(0xFF342845,25));transport.addView(play,middle);
        transport.addView(next,space(58,48,0));
        details.addView(transport,space(-1,56,3));

        LinearLayout divider=row();divider.setBackgroundColor(0xFF252732);
        details.addView(divider,space(-1,1,16));
        LinearLayout data=row();
        batteryText=text("BATTERY  —",12,0xFF9FECCC,true);
        timerText=text("No timer",12,MUTED,true);
        data.addView(batteryText,new LinearLayout.LayoutParams(0,d(33),1));
        data.addView(timerText);
        details.addView(data,space(-1,33,8));

        LinearLayout timers=row();timers.setGravity(Gravity.CENTER);
        for(int minutes:new int[]{5,10,25}){
            TextView item=text(minutes+" MIN",11,WHITE,true);item.setGravity(Gravity.CENTER);
            item.setBackground(bg(0xFF262833,12));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,d(37),1);
            p.setMargins(d(3),0,d(3),0);timers.addView(item,p);
            item.setOnClickListener(v->{startTimer(minutes);autoClose(8000);});
        }
        TextView stop=text("✕",17,0xFFFAB7BB,true);stop.setGravity(Gravity.CENTER);
        stop.setBackground(bg(0xFF342126,12));
        timers.addView(stop,space(40,37,0));
        stop.setOnClickListener(v->{stopTimer();autoClose(8000);});
        details.addView(timers,space(-1,38,5));

        alertPanel=col();alertPanel.setVisibility(View.GONE);
        alertPanel.setPadding(d(13),d(12),d(13),d(12));
        alertPanel.setBackground(bg(0xFF20212D,16));
        TextView caption=text("NEW ACTIVITY",9,PURPLE,true);
        alertText=text("",13,WHITE,false);
        alertPanel.addView(caption);alertPanel.addView(alertText,space(-1,22,2));
        alertPanel.setOnClickListener(v->{
            if(currentAlert!=null&&currentAlert.action!=null){
                try{currentAlert.action.send();}catch(PendingIntent.CanceledException ignored){}
            }
            resize(false,true);
        });
        details.addView(alertPanel,space(-1,61,12));

        int width=d(preferredWidth());
        params=new WindowManager.LayoutParams(width,d(preferredHeight()),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        params.y=d(getSharedPreferences("island_config",MODE_PRIVATE).getInt("offset",0));
        params.x=d(getSharedPreferences("island_config",MODE_PRIVATE).getInt("horizontal",0));
        applyStyle();
        wm.addView(bubble,params);
    }
    private int screenWidthDp(){
        return (int)(getResources().getDisplayMetrics().widthPixels/
                getResources().getDisplayMetrics().density);
    }
    private int preferredWidth(){
        return 100+getSharedPreferences("island_config",MODE_PRIVATE).getInt("width",50);
    }
    private int preferredHeight(){
        return getSharedPreferences("island_config",MODE_PRIVATE).getInt("height",42);
    }
    private void applyStyle(){
        int style=getSharedPreferences("island_config",MODE_PRIVATE).getInt("style",0);
        if(bubble!=null)bubble.setBackground(bg(Color.BLACK,style==0?29:13));
    }
    private void safeUpdate(){
        try{if(bubble!=null&&wm!=null)wm.updateViewLayout(bubble,params);}
        catch(RuntimeException ignored){}
    }
    private void resize(boolean show,boolean animate){
        if(bubble==null||wm==null)return;
        main.removeCallbacks(collapse);
        if(animation!=null)animation.cancel();
        expanded=show;
        int width=d(show?Math.min(screenWidthDp()-16,360):preferredWidth());
        int height=d(show?(alertPanel.getVisibility()==View.VISIBLE?463:389):preferredHeight());
        params.y=d(getSharedPreferences("island_config",MODE_PRIVATE).getInt("offset",0));
        params.x=d(getSharedPreferences("island_config",MODE_PRIVATE).getInt("horizontal",0));
        if(!animate){
            params.width=width;params.height=height;
            mini.setVisibility(show?View.GONE:View.VISIBLE);
            details.setVisibility(show?View.VISIBLE:View.GONE);
            details.setAlpha(1f);applyStyle();
            safeUpdate();return;
        }
        final int startW=params.width,startH=params.height;
        if(show){mini.setVisibility(View.GONE);details.setVisibility(View.VISIBLE);details.setAlpha(0f);}
        animation=ValueAnimator.ofFloat(0f,1f);
        animation.setDuration(330);
        animation.setInterpolator(new PathInterpolator(.2f,0f,.0f,1f));
        animation.addUpdateListener(a->{
            float t=(float)a.getAnimatedValue();
            params.width=startW+(int)((width-startW)*t);
            params.height=startH+(int)((height-startH)*t);
            details.setAlpha(show?t:1f-t);
            safeUpdate();
            if(t>=1f){
                if(!show){details.setVisibility(View.GONE);mini.setVisibility(View.VISIBLE);}
                applyStyle();
            }
        });
        animation.start();
        if(show)autoClose(8500);
    }
    private void autoClose(long delay){
        main.removeCallbacks(collapse);
        if(expanded)main.postDelayed(collapse,delay);
    }
    private void peek(String title,String body){
        if(alertPanel==null)return;
        main.removeCallbacks(clearAlert);
        currentAlert=null;
        alertText.setText(title+"  ·  "+body);
        alertPanel.setVisibility(View.VISIBLE);
        resize(true,true);
        main.postDelayed(clearAlert,5200);
    }
    @Override public void onAlert(IslandEvents.Alert alert){
        if(alertPanel==null)return;
        main.removeCallbacks(clearAlert);
        currentAlert=alert;
        alertText.setText(alert.title+"  ·  "+alert.text);
        alertPanel.setVisibility(View.VISIBLE);
        resize(true,true);
        main.postDelayed(clearAlert,6000);
    }
    private void scanMedia(){
        if(mediaSessions==null)return;
        ComponentName listener=new ComponentName(this,ListenerService.class);
        try{
            if(!registered){
                mediaSessions.addOnActiveSessionsChangedListener(sessionChanges,listener,main);
                registered=true;
            }
            List<MediaController> active=mediaSessions.getActiveSessions(listener);
            MediaController found=null;
            for(MediaController session:active){
                PlaybackState state=session.getPlaybackState();
                if(state!=null&&state.getState()==PlaybackState.STATE_PLAYING){found=session;break;}
                if(found==null&&session.getMetadata()!=null)found=session;
            }
            if(mediaController!=null&&(found==null||
                    !mediaController.getSessionToken().equals(found.getSessionToken()))){
                mediaController.unregisterCallback(mediaCallback);mediaController=null;
            }
            if(mediaController==null&&found!=null){
                mediaController=found;mediaController.registerCallback(mediaCallback,main);
            }
            update();
        }catch(SecurityException ignored){
            // User has not granted the optional notification listener permission.
        }catch(RuntimeException ignored){
            // Some media apps do not expose their sessions.
        }
    }
    private boolean playing(){
        PlaybackState state=mediaController==null?null:mediaController.getPlaybackState();
        return state!=null&&state.getState()==PlaybackState.STATE_PLAYING;
    }
    private void mediaAction(int direction){
        if(mediaController==null)return;
        MediaController.TransportControls controls=mediaController.getTransportControls();
        if(direction<0)controls.skipToPrevious();
        else if(direction>0)controls.skipToNext();
        else if(playing())controls.pause();else controls.play();
        update();
    }
    private void update(){
        if(bubble==null)return;
        boolean music=playing();
        miniLeft.setText(music?"♫":(timerRemaining>=0?"◷":"●"));
        miniRight.setText(timerRemaining>=0?clock(timerRemaining):
                charging?"⚡"+batteryPercent+"%":music?"▂▅▃":"●");
        miniRight.setTextColor(charging?0xFF98F3CA:PURPLE);
        miniArt.setVisibility(music?View.VISIBLE:View.GONE);
        miniLeft.setVisibility(music?View.GONE:View.VISIBLE);
        batteryText.setText("BATTERY  "+(batteryPercent<0?"—":batteryPercent+"%")+(charging?"  ⚡":""));
        timerText.setText(timerRemaining>=0?clock(timerRemaining):"No timer");
        play.setText(music?"Ⅱ":"▶");
        MediaMetadata m=mediaController==null?null:mediaController.getMetadata();
        if(m==null){
            songName.setText("Nothing playing");
            artist.setText("Open your favorite music app");
            cover.setImageDrawable(null);
            miniArt.setImageDrawable(null);
            elapsed.setText("0:00"); durationLabel.setText("0:00");
            progress.setEnabled(false);
            if(!scrubbing)progress.setProgress(0);
            return;
        }
        CharSequence name=m.getText(MediaMetadata.METADATA_KEY_TITLE);
        CharSequence who=m.getText(MediaMetadata.METADATA_KEY_ARTIST);
        songName.setText(TextUtils.isEmpty(name)?"Audio playing":name);
        artist.setText(TextUtils.isEmpty(who)?"Music":who);
        Bitmap art=m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if(art==null)art=m.getBitmap(MediaMetadata.METADATA_KEY_ART);
        cover.setImageBitmap(art);
        miniArt.setImageBitmap(art);
        long duration=m.getLong(MediaMetadata.METADATA_KEY_DURATION);
        durationLabel.setText(formatTime(duration));
        PlaybackState state=mediaController.getPlaybackState();
        progress.setEnabled(duration>0);
        if(duration>0&&state!=null&&!scrubbing){
            long position=state.getPosition();
            if(state.getState()==PlaybackState.STATE_PLAYING&&state.getLastPositionUpdateTime()>0){
                position+=(long)((android.os.SystemClock.elapsedRealtime()-
                    state.getLastPositionUpdateTime())*state.getPlaybackSpeed());
            }
            progress.setProgress((int)(1000L*Math.max(0,Math.min(position,duration))/duration));
            elapsed.setText(formatTime(position));
        }
    }
    private String formatTime(long ms){
        long seconds=Math.max(0,ms/1000);
        return String.format(Locale.US,"%d:%02d",seconds/60,seconds%60);
    }
    private String clock(long ms){
        long sec=Math.max(0,(ms+999)/1000);
        return String.format(Locale.US,"%02d:%02d",sec/60,sec%60);
    }
    private void startTimer(int minutes){
        stopTimer();timerRemaining=minutes*60000L;
        timer=new CountDownTimer(timerRemaining,1000){
            @Override public void onTick(long left){timerRemaining=left;update();}
            @Override public void onFinish(){timerRemaining=-1;timer=null;update();peek("Timer","Finished!");}
        }.start();update();
    }
    private void stopTimer(){
        if(timer!=null)timer.cancel();
        timer=null;timerRemaining=-1;update();
    }
    private void foreground(){
        NotificationManager manager=getSystemService(NotificationManager.class);
        NotificationChannel ch=new NotificationChannel(CHANNEL,"IslandOne status",NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Keeps the user-enabled island overlay running");
        manager.createNotificationChannel(ch);
        PendingIntent settings=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,2,
                new Intent(this,IslandService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(R.drawable.ic_island).setContentTitle("IslandOne is on")
                .setContentText("Tap to customize").setContentIntent(settings).setOngoing(true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop",stop).build()).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(1001,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1001,n);
    }
    @Override public void onCreate(){
        super.onCreate();foreground();
        if(!Settings.canDrawOverlays(this)){stopSelf();return;}
        mediaSessions=(MediaSessionManager)getSystemService(Context.MEDIA_SESSION_SERVICE);
        createBubble();
        registerReceiver(powerReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        IslandEvents.listen(this);
        scanMedia();main.post(tick);
        getSharedPreferences("island_config",MODE_PRIVATE).edit().putBoolean("running",true).apply();
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(intent!=null&&SETTINGS.equals(intent.getAction()))resize(expanded,false);
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){
        main.removeCallbacks(tick);main.removeCallbacks(collapse);main.removeCallbacks(clearAlert);
        if(animation!=null)animation.cancel();
        if(timer!=null)timer.cancel();
        try{unregisterReceiver(powerReceiver);}catch(IllegalArgumentException ignored){}
        IslandEvents.listen(null);
        if(mediaSessions!=null&&registered){
            try{mediaSessions.removeOnActiveSessionsChangedListener(sessionChanges);}catch(RuntimeException ignored){}
        }
        if(mediaController!=null){
            try{mediaController.unregisterCallback(mediaCallback);}catch(RuntimeException ignored){}
        }
        if(wm!=null&&bubble!=null){
            try{wm.removeView(bubble);}catch(RuntimeException ignored){}
        }
        bubble=null;
        getSharedPreferences("island_config",MODE_PRIVATE).edit().putBoolean("running",false).apply();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
