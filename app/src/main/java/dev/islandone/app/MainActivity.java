package dev.islandone.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final int BG = 0xFF090B12, PANEL = 0xFF171923, SUB = 0xFF9EA3B4;
    private static final int WHITE = 0xFFF8F7FC, ACCENT = 0xFFC1A9FF;
    private TextView status, positionValue, widthValue;
    private Button enable;
    private LinearLayout root;
    private SharedPreferences prefs;

    private int d(float dp) { return (int) (dp * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable b = new GradientDrawable(); b.setColor(color); b.setCornerRadius(d(radius)); return b;
    }
    private GradientDrawable stroke(int color, int radius, int border) {
        GradientDrawable b = shape(color, radius); b.setStroke(d(1), border); return b;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(color); t.setTextSize(size);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        t.setGravity(Gravity.CENTER_VERTICAL); return t;
    }
    private LinearLayout vertical() {
        LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    private LinearLayout.LayoutParams lp(int h, int mt) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, h == -2 ? -2 : d(h)); p.topMargin = d(mt); return p;
    }
    private LinearLayout.LayoutParams child(int w, int h) {
        return new LinearLayout.LayoutParams(w < 0 ? w : d(w), h < 0 ? h : d(h));
    }
    private void title(String s) {
        TextView t = text(s, 19, WHITE, true); t.setPadding(0, d(32), 0, d(12)); root.addView(t);
    }
    private Button button(String label, boolean primary) {
        Button b = new Button(this); b.setAllCaps(false); b.setText(label); b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? 0xFF0D0C14 : WHITE);
        b.setBackground(shape(primary ? ACCENT : 0xFF2B2E3A, 19));
        return b;
    }
    private void addPermission(String number, String name, String explanation, Runnable open) {
        LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL); line.setPadding(d(16),d(13),d(12),d(13));
        line.setBackground(stroke(PANEL,20,0xFF30313B));
        TextView badge = text(number, 14, ACCENT, true);
        badge.setGravity(Gravity.CENTER); badge.setBackground(shape(0xFF332A4B,14));
        line.addView(badge, child(38,38));
        LinearLayout labels = vertical(); labels.setPadding(d(12),0,d(8),0);
        labels.addView(text(name, 14, WHITE, true));
        labels.addView(text(explanation, 11, SUB, false), lp(-2,2));
        line.addView(labels, new LinearLayout.LayoutParams(0,-2,1f));
        TextView arrow = text("›", 29, ACCENT, false); line.addView(arrow);
        line.setOnClickListener(v -> open.run());
        root.addView(line, lp(-2,10));
    }
    private void addSlider(String label, int min, int max, int current, String key, boolean isWidth) {
        LinearLayout panel = vertical(); panel.setPadding(d(17),d(16),d(17),d(13));
        panel.setBackground(stroke(PANEL,20,0xFF30313B));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text(label,14,WHITE,true),new LinearLayout.LayoutParams(0,-2,1f));
        TextView value = text("",13,ACCENT,true); header.addView(value); panel.addView(header);
        SeekBar slider = new SeekBar(this); slider.setMax(max - min);
        slider.setProgress(Math.max(0,Math.min(max-min,current-min)));
        slider.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        slider.setThumbTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                int val = progress + min; value.setText(val + " dp");
                if (user) {
                    prefs.edit().putInt(key, isWidth ? val - 100 : val).apply();
                    notifyService();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        value.setText(current+" dp"); panel.addView(slider,lp(-2,8));
        root.addView(panel,lp(-2,10));
        if (isWidth) widthValue=value; else positionValue=value;
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("island_config",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root = vertical(); root.setPadding(d(22),d(36),d(22),d(36)); scroll.addView(root); setContentView(scroll);

        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("◉",21,ACCENT,true); logo.setGravity(Gravity.CENTER);
        logo.setBackground(shape(0xFF241D38,16)); brand.addView(logo,child(44,44));
        LinearLayout name = vertical(); name.setPadding(d(12),0,0,0);
        name.addView(text("ISLANDONE",12,WHITE,true)); name.addView(text("ANDROID • EARLY ACCESS",10,ACCENT,true),lp(-2,3));
        brand.addView(name); root.addView(brand);

        TextView headline = text("Make the top of your\nphone feel alive.", 34, WHITE, true);
        headline.setLineSpacing(d(3),1f); headline.setPadding(0,d(27),0,d(8)); root.addView(headline);
        root.addView(text("Music, alerts and little moments — all in one place.",14,SUB,false));

        LinearLayout demo = vertical(); demo.setGravity(Gravity.CENTER); demo.setPadding(d(18),d(28),d(18),d(26));
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF26213B,0xFF131721,0xFF10141A});
        gradient.setCornerRadius(d(30)); gradient.setStroke(d(1),0xFF343247); demo.setBackground(gradient);
        TextView demoTop = text("LIVE PREVIEW",10,0xFFAAA2C1,true); demoTop.setLetterSpacing(.22f);
        demoTop.setGravity(Gravity.CENTER); demo.addView(demoTop);
        LinearLayout pill = new LinearLayout(this); pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(d(17),0,d(16),0); pill.setBackground(shape(Color.BLACK,29));
        TextView music = text("♫",20,0xFFF5B4DD,true); pill.addView(music,child(32,-1));
        TextView middle = text("NOW PLAYING",10,0xFFE6E0F8,true); middle.setGravity(Gravity.CENTER);
        pill.addView(middle,new LinearLayout.LayoutParams(0,-1,1f));
        TextView waves = text("▂▅▃▆",17,0xFFC5A9FF,true); pill.addView(waves);
        LinearLayout.LayoutParams pillParams = lp(52,20); pillParams.width=d(260);
        pillParams.gravity=Gravity.CENTER_HORIZONTAL; demo.addView(pill,pillParams);
        demo.addView(text("TAP TO EXPAND   •   FLUID ANIMATIONS",10,SUB,true),lp(-2,19));
        root.addView(demo,lp(-2,25));

        LinearLayout statePanel = vertical(); statePanel.setPadding(d(18),d(18),d(18),d(17));
        statePanel.setBackground(shape(0xFF191B26,22));
        status=text("○  Island is off",14,WHITE,true); statePanel.addView(status);
        enable=button("Turn on IslandOne",true); enable.setOnClickListener(v->toggle());
        statePanel.addView(enable,lp(52,15)); root.addView(statePanel,lp(-2,17));

        title("Get set up");
        addPermission("01","Appear on top","Needed for the floating island",()->{
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())));
        });
        addPermission("02","Notification access","Media controls and notification previews",()->{
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        });
        title("Make it yours");
        addSlider("Distance from top",0,64,prefs.getInt("offset",0),"offset",false);
        addSlider("Pill width",100,220,100+prefs.getInt("width",30),"width",true);
        TextView disclaimer=text("Tip: On the S24 FE, adjust the vertical offset until the pill sits naturally near the selfie camera. System status-bar and lock-screen behavior depends on One UI.",12,SUB,false);
        disclaimer.setPadding(d(2),d(16),d(2),0); root.addView(disclaimer);
        TextView footer=text("NO ADS   ·   NO TRACKING   ·   OPEN SOURCE",10,0xFF777B8C,true);
        footer.setGravity(Gravity.CENTER); root.addView(footer,lp(32,25));
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=getPackageManager().PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},14);
        refresh();
    }
    private void notifyService() {
        if(prefs.getBoolean("running",false)) {
            Intent i=new Intent(this,IslandService.class).setAction(IslandService.SETTINGS);
            startService(i);
        }
    }
    private void toggle() {
        if(prefs.getBoolean("running",false)){
            startService(new Intent(this,IslandService.class).setAction(IslandService.STOP));
            prefs.edit().putBoolean("running",false).apply(); refresh(); return;
        }
        if(!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))); return;
        }
        startForegroundService(new Intent(this,IslandService.class).setAction(IslandService.START));
        prefs.edit().putBoolean("running",true).apply(); refresh();
    }
    @Override protected void onResume(){super.onResume(); if(enable!=null)refresh();}
    private void refresh(){
        boolean on=prefs.getBoolean("running",false),overlay=Settings.canDrawOverlays(this);
        status.setText((on?"●  Island is active":"○  Island is off")+
                (overlay?"  ·  Ready":"  ·  Grant overlay access"));
        status.setTextColor(on?0xFFB3F3D7:WHITE);
        enable.setText(on?"Turn off IslandOne":"Turn on IslandOne");
    }
}
