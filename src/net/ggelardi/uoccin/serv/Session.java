package net.ggelardi.uoccin.serv;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.PK;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
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
	
	private final Context appContext;
	private final SharedPreferences prefs;
	private final Storage dbhlp;
	private SQLiteDatabase dbconn;
	private String gdruid;
	
	public Session(Context context) {
		appContext = context.getApplicationContext();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		dbhlp = new Storage(appContext);
		
		driveDeviceID();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.TVDBFEED))
			registerAlarms();
		else if (key.equals(PK.GDRVSYNC)) {
			registerAlarms();
			if (driveSyncEnabled() && TextUtils.isEmpty(driveUserAccount()))
				appContext.sendBroadcast(new Intent(Commons.SN.CONNECT_FAIL));
		} else if (key.equals(PK.GDRVAUTH) && !TextUtils.isEmpty(driveUserAccount())) {
			Intent si = new Intent(appContext, Service.class);
			si.setAction(Service.GDRIVE_RESTORE);
			//appContext.startService(si);
			WakefulIntentService.sendWakefulWork(appContext, si);
		}
	}
	
	public Context getContext() {
		return appContext;
	}
	
	public SharedPreferences getPrefs() {
		return prefs;
	}
	
	public Resources getRes() {
		return appContext.getResources();
	}
	
	public String getString(int id) {
		return appContext.getResources().getString(id);
	}
	
	public SQLiteDatabase getDB() {
		if (dbconn == null)
			dbconn = dbhlp.getWritableDatabase();
		return dbconn;
	}
	
	public boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}
	
	public boolean isOnWIFI() {
		ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting() && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}
	
	private PendingIntent mkPI(String action) {
		return PendingIntent.getBroadcast(appContext, 0, new Intent(appContext, Receiver.class).setAction(action),
			PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	public void registerAlarms() {
		AlarmManager am = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
		// clean database cache every hour
		PendingIntent cc = mkPI(Service.CLEAN_DB_CACHE);
		am.cancel(cc);
		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HOUR,
			AlarmManager.INTERVAL_HOUR, cc);
		// check TVDB rss feed for premiers a couple of times a day
		PendingIntent tv = mkPI(Service.CHECK_TVDB_RSS);
		am.cancel(tv);
		if (checkPremieres())
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_HALF_DAY,
				AlarmManager.INTERVAL_HALF_DAY, tv);
		// check Uoccin files changes in Drive every 15 mins
		PendingIntent gd = mkPI(Service.GDRIVE_SYNC);
		am.cancel(gd);
		if (driveSyncEnabled())
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, AlarmManager.INTERVAL_FIFTEEN_MINUTES,
				driveSyncInterval(), gd);
	}
	
	// user preferences
	
	public String language() {
		return prefs.getString(PK.LANGUAGE, Locale.getDefault().getLanguage());
	}
	
	public boolean specials() {
		return prefs.getBoolean(PK.SPECIALS, false);
	}
	
	public boolean checkPremieres() {
		return prefs.getBoolean(PK.TVDBFEED, false);
	}
	
	public boolean driveSyncEnabled() {
		return prefs.getBoolean(PK.GDRVSYNC, false);
	}
	
	public long driveSyncInterval() {
		return prefs.getInt(PK.GDRVINTV, 15) * 60000;
	}
	
	// app saved stuff
	
	public String driveDeviceID() {
		if (TextUtils.isEmpty(gdruid))
			gdruid = getPrefs().getString(PK.GDRVUUID, "");
		if (TextUtils.isEmpty(gdruid)) {
			gdruid = UUID.randomUUID().toString();
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(PK.GDRVUUID, gdruid);
			editor.commit();
			Log.v(TAG, "Device ID for Drive Sync: " + gdruid);
		}
		return gdruid;
	}
	
	public String driveUserAccount() {
		return prefs.getString(PK.GDRVAUTH, null);
	}
	
	/*
	public long driveLastSyncUTC() {
		return prefs.getLong(PK.GDRVLASY, 0);
	}
	*/
	
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
	
	/*
	public void setDriveLastSyncUTC(long value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.GDRVLASY, value);
		editor.commit();
	}
	*/
	
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
	
	public Picasso picasso() {
		// @formatter:off
		/* DEBUG ONLY!!!
	    Picasso.Builder builder = new Picasso.Builder(appContext);
	    builder.downloader(new UrlConnectionDownloader(appContext) {
	        @Override
	        protected HttpURLConnection openConnection(Uri uri) throws IOException {
	            HttpURLConnection connection = super.openConnection(uri);
	            connection.setRequestProperty("User-Agent", USER_AGENT);
	            return connection;
	        }
	    });
	    builder.listener(new Picasso.Listener() {
			@Override
			public void onImageLoadFailed(Picasso picasso, Uri uri, Exception error) {
				Log.v("picasso", error.getLocalizedMessage() + " -- " + uri.toString());
			}});
	    return builder.build();
	    */
		// @formatter:on
		return Picasso.with(appContext);
	}
	
	public RequestCreator picasso(String path) {
		return Picasso.with(appContext).load(path).noPlaceholder();
	}
	
	public RequestCreator picasso(String path, boolean placeholder) {
		if (!placeholder)
			return picasso(path);
		return Picasso.with(appContext).load(path).placeholder(R.drawable.ic_action_image);
	}
	
	public String defaultText(String value, int resId) {
		if (TextUtils.isEmpty(value))
			return appContext.getResources().getString(resId);
		return value;
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