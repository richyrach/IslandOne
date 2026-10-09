package dev.islandone.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

/** Native settings UI: intentionally no WebView or UI framework dependency. */
public final class MainActivity extends Activity {
    private static final int INK=0xFF1A1B24, QUIET=0xFF787C8F, SURFACE=Color.WHITE;
    private static final int BG=0xFFF6F6F9, STROKE=0xFFE9E8EF, PURPLE=0xFF7456DE;
    private static final int SOFT=0xFFF1EDFF, NAVY=0xFF171822;

    private LinearLayout content;
    private SharedPreferences prefs;
    private Switch master;
    private TextView state, overlayState, listenerState, batteryState, versionState;
    private TextView styleRoundedLabel, styleFlatLabel;
    private LinearLayout shapePreview, styleRound, styleFlat;
    private boolean updatingToggle;
    private int shownUpdateVersion;

    private int dp(float value) {
        return (int)(value*getResources().getDisplayMetrics().density+.5f);
    }
    private GradientDrawable background(int color,int radius,int border) {
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));
        if(border!=0)g.setStroke(dp(1),border);
        return g;
    }
    private LinearLayout column() {
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
    }
    private LinearLayout line() {
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;
    }
    private TextView label(String title,int size,int color,boolean strong) {
        TextView v=new TextView(this);v.setText(title);v.setTextSize(size);v.setTextColor(color);
        v.setFontFeatureSettings("kern");
        v.setTypeface(Typeface.create("sans-serif",strong?Typeface.BOLD:Typeface.NORMAL));
        v.setIncludeFontPadding(false);v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    private LinearLayout.LayoutParams lp(int width,int height) {
        return new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));
    }
    private LinearLayout.LayoutParams mt(int height,int top) {
        LinearLayout.LayoutParams p=lp(-1,height);p.topMargin=dp(top);return p;
    }
    private LinearLayout card() {
        LinearLayout card=column();card.setPadding(dp(17),dp(18),dp(17),dp(18));
        card.setBackground(background(SURFACE,24,STROKE));return card;
    }
    private void divider(LinearLayout parent) {
        View v=new View(this);v.setBackgroundColor(0xFFF0EFF4);parent.addView(v,mt(1,16));
    }
    private void open(Intent i) {
        try{startActivity(i);}catch(ActivityNotFoundException ignored){}
    }
    private void appSettings() {
        open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));
    }
    private void touchFeedback(View view,int radius) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x167456DE),
                view.getBackground(),background(Color.WHITE,radius,0)));
    }
    private TextView action(String text) {
        TextView button=label(text,13,PURPLE,true);
        button.setGravity(Gravity.CENTER);button.setPadding(dp(12),dp(9),dp(12),dp(9));
        button.setBackground(background(SOFT,13,0));touchFeedback(button,13);return button;
    }
    private void section(String heading,String trailing,Runnable onTrailing) {
        LinearLayout row=line();
        TextView title=label(heading,19,INK,true);
        row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        if(trailing!=null){TextView right=action(trailing);right.setOnClickListener(v->onTrailing.run());row.addView(right);}
        content.addView(row,mt(-2,26));
    }
    private void note(LinearLayout parent,String copy) {
        TextView text=label(copy,12,QUIET,false);text.setLineSpacing(dp(3),1f);
        parent.addView(text,mt(-2,12));
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs=getSharedPreferences("island_config",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        scroll.setVerticalScrollBarEnabled(false);
        content=column();content.setPadding(dp(22),dp(28),dp(22),dp(36));
        scroll.addView(content);setContentView(scroll);

        LinearLayout brand=line();
        TextView mark=label("◉",28,PURPLE,true);mark.setGravity(Gravity.CENTER);
        mark.setBackground(background(0xFFE9E2FF,18,0));brand.addView(mark,lp(48,48));
        LinearLayout brandText=column();brandText.setPadding(dp(12),0,0,0);
        brandText.addView(label("IslandOne",22,INK,true));
        brandText.addView(label("FOR ANDROID",10,PURPLE,true),mt(-2,3));
        brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));
        TextView github=action("GitHub ↗");
        github.setOnClickListener(v->open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/richyrach/IslandOne"))));
        brand.addView(github);content.addView(brand);

        TextView heading=label("Everything happening.\nOne little island.",30,INK,true);
        heading.setLineSpacing(dp(4),1f);content.addView(heading,mt(-2,32));
        content.addView(label("Your music, alerts, charging and timers. Right where you need them.",14,QUIET,false),mt(-2,12));

        LinearLayout active=card();active.setPadding(dp(18),dp(17),dp(18),dp(17));
        LinearLayout activeRow=line();
        LinearLayout left=column();left.addView(label("Floating island",16,INK,true));
        state=label("Off · Tap to enable",12,QUIET,false);left.addView(state,mt(-2,5));
        activeRow.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        master=new Switch(this);master.setThumbTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                new int[]{PURPLE,0xFFDFDFE5}));
        master.setTrackTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
                new int[]{0xFFD7C8FF,0xFFE3E2E8}));
        activeRow.addView(master);
        master.setOnCheckedChangeListener((button,isChecked)->{
            if(!updatingToggle)toggle(isChecked);
        });
        active.addView(activeRow);content.addView(active,mt(-2,25));

        section("Live preview",null,null);
        LinearLayout preview=column();preview.setGravity(Gravity.CENTER);
        preview.setPadding(dp(15),dp(30),dp(15),dp(29));
        GradientDrawable shade=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF262436,0xFF171725,0xFF10111A});
        shade.setCornerRadius(dp(26));preview.setBackground(shade);

        LinearLayout mini=line();mini.setGravity(Gravity.CENTER_VERTICAL);
        mini.setPadding(dp(10),dp(5),dp(13),dp(5));
        mini.setBackground(background(Color.BLACK,26,0));
        TextView album=label("♫",19,Color.WHITE,true);album.setGravity(Gravity.CENTER);
        GradientDrawable art=new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF59DBE,0xFF9276E8,0xFF5A87E9});
        art.setCornerRadius(dp(13));album.setBackground(art);
        mini.addView(album,lp(41,41));
        LinearLayout now=column();now.setPadding(dp(12),0,0,0);
        now.addView(label("Now playing",13,Color.WHITE,true));
        now.addView(label("Your favorite track",11,0xFFA4A4B1,false),mt(-2,3));
        mini.addView(now,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout wave=line();
        int[] heights={8,17,25,13,20};
        for(int h:heights){
            View bar=new View(this);bar.setBackground(background(0xFFD2B2FF,3,0));
            LinearLayout.LayoutParams p=lp(3,h);p.leftMargin=dp(3);wave.addView(bar,p);
        }
        mini.addView(wave);shapePreview=mini;
        LinearLayout.LayoutParams miniParams=lp(-1,66);miniParams.leftMargin=dp(8);miniParams.rightMargin=dp(8);
        preview.addView(mini,miniParams);
        TextView hint=label("Designed to feel at home on your screen",11,0xFFACABBD,false);
        hint.setGravity(Gravity.CENTER);preview.addView(hint,mt(30,15));
        content.addView(preview,mt(-2,13));

        section("Island style",null,null);
        LinearLayout styles=line();
        styleRound=styleChoice("Rounded",true);
        styleFlat=styleChoice("Compact",false);
        styles.addView(styleRound,new LinearLayout.LayoutParams(0,dp(108),1f));
        LinearLayout.LayoutParams second=new LinearLayout.LayoutParams(0,dp(108),1f);second.leftMargin=dp(12);
        styles.addView(styleFlat,second);
        styleRound.setOnClickListener(v->selectStyle(0));
        styleFlat.setOnClickListener(v->selectStyle(1));
        content.addView(styles,mt(-2,13));
        refreshStyles();

        section("Position","Reset",()->{
            prefs.edit().putInt("offset",0).putInt("horizontal",0).apply();
            renderSettings();
        });
        LinearLayout position=card();
        position.addView(sliderRow("Vertical",0,110,"offset",0,false));
        divider(position);
        position.addView(sliderRow("Horizontal",-100,100,"horizontal",0,false),mt(-2,14));
        note(position,"Fine-tune the island around your phone's camera cutout.");
        content.addView(position,mt(-2,12));

        section("Island size","Reset",()->{
            prefs.edit().putInt("width",50).putInt("height",42).apply();
            renderSettings();
        });
        LinearLayout size=card();
        size.addView(sliderRow("Width",100,265,"width",50,true));
        divider(size);
        size.addView(sliderRow("Height",32,64,"height",42,false),mt(-2,14));
        content.addView(size,mt(-2,12));

        section("Permissions",null,null);
        LinearLayout permissions=card();
        LinearLayout overlay=permissionRow("01","Display over apps","Required for the island",()->open(
                new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
        overlayState=(TextView)overlay.getTag();permissions.addView(overlay);
        divider(permissions);
        LinearLayout notification=permissionRow("02","Notification access","Music controls and alert previews",()->
                open(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        listenerState=(TextView)notification.getTag();
        permissions.addView(notification,mt(-2,14));content.addView(permissions,mt(-2,12));

        section("Keep it running",null,null);
        LinearLayout power=card();
        power.addView(label("Samsung battery settings",15,INK,true));
        batteryState=label("",12,QUIET,false);power.addView(batteryState,mt(-2,8));
        note(power,"Set IslandOne to Unrestricted under App info → Battery. Samsung may still stop overlays under heavy background restrictions.");
        TextView powerLink=action("Open app settings  ↗");powerLink.setOnClickListener(v->appSettings());
        power.addView(powerLink,mt(-2,15));content.addView(power,mt(-2,12));

        section("Updates",null,null);
        LinearLayout updater=card();
        updater.addView(label("IslandOne preview",15,INK,true));
        versionState=label("Checking release info…",12,QUIET,false);
        updater.addView(versionState,mt(-2,8));
        TextView update=action("Check for updates  ↗");
        update.setOnClickListener(v->checkUpdates(true));
        updater.addView(update,mt(-2,14));
        note(updater,"A new APK opens in your browser. Preview builds may require uninstalling the previous version until permanent signing is configured.");
        content.addView(updater,mt(-2,12));

        TextView footer=label("Made for Android • Open source • MIT",12,QUIET,false);
        footer.setGravity(Gravity.CENTER);content.addView(footer,mt(56,18));
        refreshStatus();
        checkUpdates(false);
    }
    private LinearLayout styleChoice(String name,boolean round) {
        LinearLayout card=column();card.setGravity(Gravity.CENTER);
        card.setBackground(background(SURFACE,20,STROKE));
        TextView symbol=label(round?"━━━━":"▰",26,INK,true);symbol.setGravity(Gravity.CENTER);
        card.addView(symbol);
        TextView caption=label(name,13,INK,true);caption.setGravity(Gravity.CENTER);
        card.addView(caption,mt(29,3));
        if(round)styleRoundedLabel=caption;else styleFlatLabel=caption;
        return card;
    }
    private void selectStyle(int style) {
        prefs.edit().putInt("style",style).apply();refreshStyles();notifyService();
    }
    private void refreshStyles() {
        if(styleRound==null)return;
        int selected=prefs.getInt("style",0);
        styleRound.setBackground(background(selected==0?SOFT:SURFACE,20,selected==0?PURPLE:STROKE));
        styleFlat.setBackground(background(selected==1?SOFT:SURFACE,20,selected==1?PURPLE:STROKE));
        styleRoundedLabel.setTextColor(selected==0?PURPLE:INK);
        styleFlatLabel.setTextColor(selected==1?PURPLE:INK);
        shapePreview.setBackground(background(Color.BLACK,selected==0?26:12,0));
    }
    private LinearLayout sliderRow(String title,int min,int max,String key,int defaultValue,boolean width) {
        LinearLayout outer=column();
        LinearLayout header=line();
        header.addView(label(title,14,INK,true),new LinearLayout.LayoutParams(0,-2,1));
        TextView value=label("",13,PURPLE,true);header.addView(value);outer.addView(header);
        LinearLayout controls=line();
        TextView minus=action("−");minus.setTextSize(18);controls.addView(minus,lp(37,38));
        SeekBar slider=new SeekBar(this);slider.setMax(max-min);
        slider.setProgressTintList(ColorStateList.valueOf(PURPLE));
        slider.setThumbTintList(ColorStateList.valueOf(PURPLE));
        LinearLayout.LayoutParams track=new LinearLayout.LayoutParams(0,dp(38),1f);
        track.leftMargin=dp(8);track.rightMargin=dp(8);controls.addView(slider,track);
        TextView plus=action("+");plus.setTextSize(18);controls.addView(plus,lp(37,38));
        outer.addView(controls,mt(40,7));
        Runnable sync=()->{
            int n=(width?100:0)+prefs.getInt(key,defaultValue);
            n=Math.max(min,Math.min(max,n));
            value.setText(String.format(Locale.US,"%d dp",n));
            slider.setProgress(n-min);
        };
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s,int amount,boolean byUser) {
                if(!byUser)return;
                int n=amount+min;
                prefs.edit().putInt(key,width?n-100:n).apply();value.setText(n+" dp");notifyService();
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){}
        });
        minus.setOnClickListener(v->{slider.setProgress(Math.max(0,slider.getProgress()-2));prefs.edit().putInt(key,width?slider.getProgress()+min-100:slider.getProgress()+min).apply();sync.run();notifyService();});
        plus.setOnClickListener(v->{slider.setProgress(Math.min(max-min,slider.getProgress()+2));prefs.edit().putInt(key,width?slider.getProgress()+min-100:slider.getProgress()+min).apply();sync.run();notifyService();});
        sync.run();
        slider.setTag(sync);
        outer.setTag(slider);
        if(settingsRows==null)settingsRows=new java.util.ArrayList<>();
        settingsRows.add(slider);
        return outer;
    }
    private java.util.ArrayList<SeekBar> settingsRows;
    private void renderSettings() {
        if(settingsRows!=null)for(SeekBar s:settingsRows){
            Runnable r=(Runnable)s.getTag();
            if(r!=null)r.run();
        }
        notifyService();
    }
    private LinearLayout permissionRow(String number,String title,String subtitle,Runnable callback) {
        LinearLayout line=line();
        TextView n=label(number,13,PURPLE,true);n.setGravity(Gravity.CENTER);
        n.setBackground(background(SOFT,13,0));line.addView(n,lp(38,38));
        LinearLayout text=column();text.setPadding(dp(12),0,0,0);
        text.addView(label(title,14,INK,true));
        text.addView(label(subtitle,12,QUIET,false),mt(-2,5));
        line.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        TextView status=label("SET UP ›",11,PURPLE,true);line.addView(status);
        line.setTag(status);line.setOnClickListener(v->callback.run());return line;
    }
    private boolean notificationAllowed() {
        String listeners=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
        return listeners!=null&&listeners.contains(getPackageName());
    }
    private void toggle(boolean on) {
        if(!on) {
            startService(new Intent(this,IslandService.class).setAction(IslandService.STOP));
            prefs.edit().putBoolean("running",false).apply();refreshStatus();return;
        }
        if(!Settings.canDrawOverlays(this)) {
            open(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
            refreshStatus();return;
        }
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=
                getPackageManager().PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},17);
        }
        startForegroundService(new Intent(this,IslandService.class).setAction(IslandService.START));
        prefs.edit().putBoolean("running",true).apply();refreshStatus();
    }
    private void notifyService() {
        if(prefs.getBoolean("running",false))startService(
                new Intent(this,IslandService.class).setAction(IslandService.SETTINGS));
    }
    private void refreshStatus() {
        if(master==null)return;
        boolean enabled=prefs.getBoolean("running",false), allowed=Settings.canDrawOverlays(this);
        updatingToggle=true;master.setChecked(enabled&&allowed);updatingToggle=false;
        state.setText(!allowed?"Needs display permission":enabled?"On · Listening for activities":"Off · Tap to enable");
        state.setTextColor(enabled&&allowed?0xFF34816F:QUIET);
        if(overlayState!=null){overlayState.setText(allowed?"READY ✓":"SET UP ›");overlayState.setTextColor(allowed?0xFF34816F:PURPLE);}
        boolean notifications=notificationAllowed();
        if(listenerState!=null){listenerState.setText(notifications?"READY ✓":"SET UP ›");listenerState.setTextColor(notifications?0xFF34816F:PURPLE);}
        if(batteryState!=null) {
            PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
            batteryState.setText(pm.isIgnoringBatteryOptimizations(getPackageName())?
                    "Android battery exemption detected":"Check background battery restrictions");
        }
    }
    private void checkUpdates(boolean manual) {
        if(versionState!=null&&manual)versionState.setText("Checking GitHub…");
        UpdateChecker.check(this,result->{
            if(isFinishing()||isDestroyed())return;
            if(result.error!=null) {
                if(versionState!=null)versionState.setText(manual?"Could not check. Try again later.":"Version "+BuildConfig.VERSION_NAME);
                return;
            }
            boolean newer=result.versionCode>UpdateChecker.installedVersion(this);
            if(versionState!=null)versionState.setText(
                    newer?"New preview available · build "+result.versionCode:
                            "Up to date · "+BuildConfig.VERSION_NAME);
            if(!newer){if(manual)new AlertDialog.Builder(this).setMessage("You're on the newest available preview.")
                    .setPositiveButton("OK",null).show();return;}
            if(!manual&&prefs.getInt("last_update_prompt",0)==result.versionCode)return;
            prefs.edit().putInt("last_update_prompt",result.versionCode).apply();
            new AlertDialog.Builder(this)
                    .setTitle("New IslandOne preview")
                    .setMessage("Build "+result.versionCode+" is available. Download it from GitHub?\n\nNote: preview builds may need a reinstall until stable signing is configured.")
                    .setNegativeButton("Not now",null)
                    .setPositiveButton("Download", (d,w)->open(new Intent(Intent.ACTION_VIEW,Uri.parse(result.downloadUrl))))
                    .show().getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(PURPLE);
        });
    }
    @Override protected void onResume(){super.onResume();refreshStatus();}
}
