package dev.islandone.app;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Optional, read-only version check against IslandOne's public GitHub release.
 * No account, device identifier, telemetry, or analytics is sent.
 */
final class UpdateChecker {
    interface Callback { void finished(Result result); }
    static final class Result {
        final int versionCode;
        final String downloadUrl, error;
        Result(int versionCode,String downloadUrl,String error) {
            this.versionCode=versionCode;this.downloadUrl=downloadUrl;this.error=error;
        }
    }
    static int installedVersion(Context context) {
        try {
            PackageInfo info=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
            return Build.VERSION.SDK_INT>=28?(int)info.getLongVersionCode():info.versionCode;
        } catch (Exception e) {return 0;}
    }
    static void check(Activity activity, Callback callback) {
        new Thread(()->{
            Result response;
            HttpURLConnection connection=null;
            try {
                URL url=new URL("https://github.com/richyrach/IslandOne/releases/download/nightly/version.json");
                connection=(HttpURLConnection)url.openConnection();
                connection.setConnectTimeout(6500);connection.setReadTimeout(6500);
                connection.setRequestProperty("Accept","application/json");
                connection.setRequestProperty("User-Agent","IslandOne-Android");
                if(connection.getResponseCode()!=200)throw new IllegalStateException("HTTP "+connection.getResponseCode());
                StringBuilder json=new StringBuilder();
                try(InputStream stream=connection.getInputStream();
                    BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
                    String line;
                    while((line=reader.readLine())!=null) {
                        json.append(line);
                        if(json.length()>16_384)throw new IllegalStateException("Invalid update response");
                    }
                }
                JSONObject object=new JSONObject(json.toString());
                int code=object.getInt("versionCode");
                String link=object.getString("downloadUrl");
                if(code<1||!link.startsWith("https://github.com/richyrach/IslandOne/releases/download/"))
                    throw new IllegalStateException("Unexpected update URL");
                response=new Result(code,link,null);
            }catch(Exception e){response=new Result(0,null,e.getClass().getSimpleName());}
            finally{if(connection!=null)connection.disconnect();}
            Result result=response;
            activity.runOnUiThread(()->callback.finished(result));
        },"IslandOne-update-check").start();
    }
    private UpdateChecker(){}
}
