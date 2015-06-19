package net.ggelardi.uoccin.serv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Commons.SN;
import net.ggelardi.uoccin.serv.Commons.SR;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

public class Session implements OnSharedPreferenceChangeListener {
	private static final String TAG = "Session";
	
	private static Session singleton;
	
	public static Session getInstance(Context context) {
		if (singleton == null)
			singleton = new Session(context);
		return singleton;
	}
	
	private final Context acntx;
	private final SharedPreferences prefs;
	private final Storage dbhlp;
	private SQLiteDatabase dbconn;
	private Picasso picasso;
	
	public Session(Context context) {
		acntx = context.getApplicationContext();
		prefs = PreferenceManager.getDefaultSharedPreferences(acntx);
		dbhlp = new Storage(acntx);
		driveDeviceID();
		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.TVDBFEED))
			registerAlarms();
		else if (key.equals(PK.GDRVSYNC)) {
			registerAlarms();
			if (driveSyncEnabled()) {
				if (!driveAccountSet())
					acntx.sendBroadcast(new Intent(SN.CONNECT_FAIL));
				else
					WakefulIntentService.sendWakefulWork(acntx,
						new Intent(acntx, Service.class).setAction(SR.GDRIVE_SYNCNOW));
			}
		} else if (key.equals(PK.GDRVUUID)) {
			// ?
		}
	}
	
	public Context getContext() {
		return acntx;
	}
	
	public SharedPreferences getPrefs() {
		return prefs;
	}
	
	public Resources getRes() {
		return acntx.getResources();
	}
	
	public String getString(int id) {
		return acntx.getResources().getString(id);
	}
	
	public String[] getStringArray(int id) {
		return acntx.getResources().getStringArray(id);
	}
	
	public SQLiteDatabase getDB() {
		if (dbconn == null)
			dbconn = dbhlp.getWritableDatabase();
		return dbconn;
	}
	
	public boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) acntx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}
	
	public boolean isOnWIFI() {
		ConnectivityManager cm = (ConnectivityManager) acntx.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting() && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}
	
	private PendingIntent getPI(String action, boolean create) {
		return PendingIntent.getBroadcast(acntx, 0, new Intent(acntx, Receiver.class).setAction(action),
			create ? PendingIntent.FLAG_UPDATE_CURRENT : PendingIntent.FLAG_NO_CREATE);
	}
	
	public void registerAlarms() {
		Log.d(TAG, "registerAlarms() begin");
		AlarmManager am = (AlarmManager) acntx.getSystemService(Context.ALARM_SERVICE);
		PendingIntent cc = getPI(SR.CLEAN_DB_CACHE, false);
		if (cc == null) {
			cc = getPI(SR.CLEAN_DB_CACHE, true);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 60000, AlarmManager.INTERVAL_HOUR, cc);
			Log.d(TAG, "CLEAN_DB_CACHE alarm set");
		}
		PendingIntent tv = getPI(SR.CHECK_TVDB_RSS, false);
		if (tvdbCheckFeed() && tv == null) {
			tv = getPI(SR.CHECK_TVDB_RSS, true);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5 * 60000, AlarmManager.INTERVAL_HOUR, tv);
			Log.d(TAG, "CHECK_TVDB_RSS alarm set");
		} else if (!tvdbCheckFeed() && tv != null) {
			am.cancel(tv);
			tv.cancel();
			Log.d(TAG, "CHECK_TVDB_RSS alarm canceled");
		}
		PendingIntent gd = getPI(SR.GDRIVE_SYNCNOW, false);
		boolean should = driveSyncEnabled() && driveAccountSet();
		if (should && gd == null) {
			gd = getPI(SR.GDRIVE_SYNCNOW, true);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 2 * 60000, driveSyncInterval(), gd);
			Log.d(TAG, "GDRIVE_SYNCNOW alarm set");
		} else if (!should && gd != null) {
			am.cancel(gd);
			gd.cancel();
			Log.d(TAG, "GDRIVE_SYNCNOW alarm canceled");
		}
		Log.d(TAG, "registerAlarms() end");
	}
	
	public String versionName() {
		try {
			return acntx.getPackageManager().getPackageInfo(acntx.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			return "Unknown";
		}
	}
	
	// user preferences
	
	public String language() {
		return prefs.getString(PK.LANGUAGE, Locale.getDefault().getLanguage());
	}
	
	public boolean specials() {
		return prefs.getBoolean(PK.SPECIALS, false);
	}
	
	public boolean autorefrWifiOnly() {
		return prefs.getBoolean(PK.METAWIFI, true);
	}
	
	public boolean tvdbCheckFeed() {
		return prefs.getBoolean(PK.TVDBFEED, false);
	}
	
	public List<String> tvdbGenreFilter() {
		return new ArrayList<String>(Arrays.asList(prefs.getString(PK.TVDBGFLT,
			"").toLowerCase(Locale.getDefault()).split(",")));
	}
	
	public long tvdbLastCheck() {
		return prefs.getLong(PK.TVDBLAST, 0);
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
	
	// utilities
	
	private Picasso getPicasso() {
		if (picasso == null) {
			OkHttpClient client = new OkHttpClient();
			client.setConnectTimeout(15, TimeUnit.SECONDS);
			client.setReadTimeout(15, TimeUnit.SECONDS);
			client.setWriteTimeout(15, TimeUnit.SECONDS);
			client.interceptors().add(new Interceptor() {
				@Override
				public Response intercept(Chain chain) throws IOException {
					return chain.proceed(chain.request().newBuilder().addHeader("User-Agent", Commons.USER_AGENT).build());
				}
			});
			picasso = new Picasso.Builder(acntx).downloader(new OkHttpDownloader(client)).build();
		}
		return picasso;
	}
	
	public Picasso picasso() {
		return getPicasso();
	}
	
	public RequestCreator picasso(String path) {
		return getPicasso().load(path).noPlaceholder();
	}
	
	public RequestCreator picasso(String path, int placeholder) {
		return getPicasso().load(path).placeholder(placeholder);
	}
	
	public RequestCreator picasso(String path, boolean placeholder) {
		if (!placeholder)
			return picasso(path);
		return getPicasso().load(path).placeholder(R.drawable.ic_action_image);
	}
	
	public String defaultText(String value, int resId) {
		if (TextUtils.isEmpty(value))
			return acntx.getResources().getString(resId);
		return value;
	}
	
	public List<String> getAllTags() {
		List<String> res = new ArrayList<String>();
		Cursor cr = getDB().query(true, "movtag", new String[] { "tag" }, null, null, null, null, null, null, null);
		try {
			while (cr.moveToNext())
				res.add(cr.getString(0));
		} finally {
			cr.close();
		}
		cr = getDB().query(true, "sertag", new String[] { "tag" }, null, null, null, null, null, null, null);
		try {
			while (cr.moveToNext())
				res.add(cr.getString(0));
		} finally {
			cr.close();
		}
		res.removeAll(Arrays.asList("", null));
		Set<String> hs = new HashSet<String>();
		hs.addAll(res);
		res.clear();
		res.addAll(hs);
		Collections.sort(res.subList(1, res.size()));
		return res;
	}
	
	public static final String QUEUE_MOVIE = "movie";
	public static final String QUEUE_SERIES = "series";
	
	public synchronized void driveQueue(String target, String title, String field, String value) {
		if (!driveSyncEnabled())
			return;
		ContentValues cv = new ContentValues();
		cv.put("timestamp", System.currentTimeMillis());
		cv.put("target", target);
		cv.put("title", title);
		cv.put("field", field);
		cv.put("value", value);
		getDB().insertOrThrow("queue_out", null, cv);
	}
}