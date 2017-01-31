package net.ggelardi.uoccin.serv;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Storage;
import net.ggelardi.uoccin.serv.Commons.PK;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class Session implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Session";

    private static final String[] PERMLIST = {
            Manifest.permission.RECEIVE_BOOT_COMPLETED,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.GET_ACCOUNTS,
            Manifest.permission.READ_CONTACTS,
            "android.permission.READ_PROFILE"
    };

    private static Session singleton;

    private final Context appcx;
    private final Storage dbhlp;
    private final SharedPreferences prefs;
    private SQLiteDatabase dbconn;
    private Picasso picasso;

    public static Session getInstance(Context context) {
        if (singleton == null)
            singleton = new Session(context);
        return singleton;
    }

    public Session(Context context) {
        appcx = context.getApplicationContext();
        dbhlp = new Storage(appcx);
        prefs = PreferenceManager.getDefaultSharedPreferences(appcx);
        prefs.registerOnSharedPreferenceChangeListener(this);
        driveSyncRunning(false);
        driveDeviceID();
        buildPicasso();
    }

    private void buildPicasso() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        File cache = new File(appcx.getCacheDir(), "picasso_cache");
        if (!cache.exists())
            cache.mkdirs();

        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                /*.addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        okhttp3.Response originalResponse = chain.proceed(chain.request());*/
                        /*String cacheControl = originalResponse.header("Cache-Control");
                        if (cacheControl == null || cacheControl.contains("no-store") ||
                                cacheControl.contains("no-cache") ||
                                cacheControl.contains("must-revalidate") ||
                                cacheControl.contains("max-age=0")) {
                            return originalResponse.newBuilder()
                                    .header("Cache-Control", "public, max-age=" + 5000)
                                    .build();
                        } else
                            return originalResponse;*/

                        /*return originalResponse.newBuilder()
                                .header("Cache-Control", "public, max-age=" + 5000)
                                .build();
                    }
                })*/
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        return chain.proceed(originalRequest.newBuilder()
                                .header("User-Agent", Commons.USER_AGENT)
                                //.header("Cache-Control", "public, max-age=" + 5000)
                                .build());
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .cache(new Cache(cache, 100000000))
                .build();

        picasso = new Picasso.Builder(appcx)
                .downloader(new OkHttp3Downloader(httpClient))
                .memoryCache(new LruCache(appcx)) // do we need this?
                .build();
        //picasso.setIndicatorsEnabled(true);

        Picasso.setSingletonInstance(picasso);
    }

    public Context getContext() {
        return appcx;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public Resources getRes() {
        return appcx.getResources();
    }

    public SQLiteDatabase getDB() {
        if (dbconn == null)
            dbconn = dbhlp.getWritableDatabase();
        return dbconn;
    }

    public String getString(int id) {
        return appcx.getResources().getString(id);
    }

    public String getString(int id, Object... formatArgs) {
        return appcx.getResources().getString(id, formatArgs);
    }

    public List<String> getMissingPermissions() {
        List<String> perms = new ArrayList<>();
        for (String p : PERMLIST)
            if (ContextCompat.checkSelfPermission(appcx, p) != PackageManager.PERMISSION_GRANTED)
                perms.add(p);
        return perms;
    }

    public String versionName() {
        try {
            return appcx.getPackageManager().getPackageInfo(appcx.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }

    public boolean isConnected(boolean wifiOnly) {
        ConnectivityManager cm = (ConnectivityManager) appcx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting() &&
                (!wifiOnly || ni.getType() == ConnectivityManager.TYPE_WIFI);
    }

    // user preferences

    public int startupView() {
        return prefs.getInt(PK.STARTUPV, 0);
    }

    public String language() {
        return prefs.getString(PK.LANGUAGE, Locale.getDefault().getLanguage());
    }

    public boolean specials() {
        return prefs.getBoolean(PK.SPECIALS, false);
    }

    public boolean wifiOnly() {
        return prefs.getBoolean(PK.METAWIFI, true);
    }

    public boolean notificationSound() {
        return prefs.getBoolean(PK.NOTIFSND, false);
    }

    public boolean notifyMovWlst() {
        return prefs.getBoolean(PK.NOTMOVWL, true);
    }

    public boolean notifyMovColl() {
        return prefs.getBoolean(PK.NOTMOVCO, true);
    }

    public boolean notifySerWlst() {
        return prefs.getBoolean(PK.NOTSERWL, true);
    }

    public boolean notifySerColl() {
        return prefs.getBoolean(PK.NOTSERCO, true);
    }

    public boolean blockSpoilers() {
        return prefs.getBoolean(PK.SPLRPROT, true);
    }

    public boolean driveSyncEnabled() {
        return prefs.getBoolean(PK.GDRVSYNC, false);
    }

    public boolean driveSyncWifiOnly() {
        return prefs.getBoolean(PK.GDRVWIFI, true);
    }

    public long driveSyncInterval() {
        return prefs.getInt(PK.GDRVINTV, 30) * 60000;
    }

    // app saved stuff

    public String driveDeviceID() {
        String res = getPrefs().getString(PK.GDRVUUID, "");
        if (TextUtils.isEmpty(res)) {
            res = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PK.GDRVUUID, res);
            editor.commit();
        }
        return res;
    }

    public boolean driveAccountSet() {
        return !TextUtils.isEmpty(driveAccountName());
    }

    public String driveAccountName() {
        return prefs.getString(PK.GDRVAUTH, "");
    }

    public String driveAccountPhoto() {
        return prefs.getString(PK.GDRVPHOT, "");
    }

    public long driveLastChangeID() {
        return prefs.getLong(PK.GDRVLCID, 0);
    }

    public long driveLastFileUpdate(String filename) {
        return prefs.getLong("pk_lastupd_" + filename, 0);
    }

    public long driveLastFileUpdateUTC(String filename) {
        long res = driveLastFileUpdate(filename);
        if (res > 0 && !Calendar.getInstance().getTimeZone().getID().equals("UTC"))
            res = Commons.convertTZ(res, Calendar.getInstance().getTimeZone().getID(), "UTC");
        return res;
    }

    public void setDriveUserAccount(String value) {
        Log.v(TAG, "Account selected: " + value);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PK.GDRVAUTH, value);
        editor.commit();
    }

    public void setDriveUserPhoto(String value) {
        Log.v(TAG, "Account photo: " + value);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PK.GDRVPHOT, value);
        editor.commit();
    }

    public void setDriveLastChangeID(long value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PK.GDRVLCID, value);
        editor.commit();
    }

    public void setDriveLastFileUpdate(String filename, long datetime) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("pk_lastupd_" + filename, datetime);
        editor.commit();
    }

    public boolean driveSyncRunning(Boolean value) {
        boolean current = prefs.getBoolean(PK.SYNCLOCK, false);
        if (value == null) {
            value = current;
        } else if (!current) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PK.SYNCLOCK, value);
            editor.commit();
        } else if (!value) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(PK.SYNCLOCK);
            editor.commit();
        } else {
            value = false;
        }
        return value;
    }

    public String lastTVDBToken() {
        return prefs.getString(PK.TVDBTOKV, null);
    }

    public void setTVDBToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PK.TVDBTOKV, token);
        editor.commit();
    }

    // scheduler

    private PendingIntent getPI(String action, boolean create) {
        return PendingIntent.getBroadcast(appcx, 0, new Intent(appcx, Receiver.class).setAction(action),
                create ? PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_NO_CREATE);
    }

    public void checkAlarms(boolean reset) {
        Log.d(TAG, "checkAlarms() begin");
        AlarmManager am = (AlarmManager) appcx.getSystemService(Context.ALARM_SERVICE);
        // database cleanup
        PendingIntent cc = getPI(Commons.SR.CLEAN_DB_CACHE, false);
        if (cc == null) {
            cc = getPI(Commons.SR.CLEAN_DB_CACHE, true);
            am.setInexactRepeating(AlarmManager.RTC, 60000, AlarmManager.INTERVAL_HOUR, cc);
            Log.i(TAG, ">>> ALARM <<< CLEAN_DB_CACHE set");
        }
        // google drive sync
        PendingIntent gd = getPI(Commons.SR.GDRIVE_SYNCNOW, false);
        boolean should = driveSyncEnabled() && driveAccountSet();
        if (gd != null && (reset || !should)) {
            am.cancel(gd);
            gd.cancel();
            gd = null;
            Log.d(TAG, "GDRIVE_SYNCNOW alarm canceled");
        }
        if (should && gd == null) {
            gd = getPI(Commons.SR.GDRIVE_SYNCNOW, true);
            long st = System.currentTimeMillis() + 70000;
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP, st, driveSyncInterval(), gd);
            Log.i(TAG, ">>> ALARM <<< GDRIVE_SYNCNOW set to " +
                    new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(st)));
        }
        Log.d(TAG, "checkAlarms() end");
    }

    // picasso

    public RequestCreator picasso(String path) {
        return Picasso.with(appcx).load(path).noPlaceholder();
    }

    public RequestCreator picasso(String path, int placeholder) {
        return Picasso.with(appcx).load(path).placeholder(placeholder);
    }

    public RequestCreator picasso(String path, boolean placeholder) {
        if (!placeholder)
            return picasso(path);
        return Picasso.with(appcx).load(path).placeholder(R.drawable.ic_action_image);
    }

    //

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PK.GDRVSYNC) || key.equals(PK.GDRVINTV)) {
            checkAlarms(true);
        } else if (key.equals(PK.SPECIALS) && !sharedPreferences.getBoolean(PK.SPECIALS, false)) {
            int n = dbconn.delete("episode", "season <= ? OR episode <= ?", new String[] {"0", "0"});
            Log.d(TAG, String.format("%d specials deleted.", n));
        }
    }

    //

    public List<String> getAllTags() {
        List<String> res = new ArrayList<String>();
        Set<String> hs;
        try (Cursor cr = getDB().query(true, "movtag", new String[]{"tag"}, null, null, null, null, null, null, null)) {
            while (cr.moveToNext())
                res.add(cr.getString(0));
        }
        try (Cursor cr = getDB().query(true, "sertag", new String[]{"tag"}, null, null, null, null, null, null, null)) {
            while (cr.moveToNext())
                res.add(cr.getString(0));
        }
        res.removeAll(Arrays.asList("", null));
        hs = new HashSet<>();
        hs.addAll(res);
        res.clear();
        res.addAll(hs);
        Collections.sort(res.subList(1, res.size()));
        return res;
    }

    //

    public static final String QUEUE_MOVIE = "movie";
    public static final String QUEUE_SERIES = "series";

    public synchronized long driveQueue(String target, String title, String field, String value) {
        if (!driveSyncEnabled())
            return -1;
        ContentValues cv = new ContentValues();
        cv.put("timestamp", System.currentTimeMillis());
        cv.put("target", target);
        cv.put("title", title);
        cv.put("field", field);
        cv.put("value", value);
        return getDB().insertOrThrow("queue_out", null, cv);
    }

    public synchronized long driveQueue(String target, int title, String field, String value) {
        return driveQueue(target, Integer.toString(title), field, value);
    }

    public synchronized void driveUndo(long queueId) {
        getDB().delete("queue_out", "rowid = ?", new String[] { Long.toString(queueId) });
    }
}
