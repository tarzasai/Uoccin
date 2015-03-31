package net.ggelardi.uoccin.serv;

import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.PK;
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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

public class Session implements OnSharedPreferenceChangeListener {
	
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
	//private SyncGAC gac;
	
	public Session(Context context) {
		appContext = context.getApplicationContext();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		dbhlp = new Storage(appContext);
		
		//getDB().execSQL("update series set timestamp = 1");
		
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.GDRVBAK) && backup()) {
			Intent si = new Intent(getContext(), Service.class);
			si.setAction(Service.GDRIVE_RESTORE);
			getContext().startService(si);
			return;
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
	
	/*
	public SyncGAC getGAC() {
		if (!backup())
			return null;
		if (gac == null)
			gac = new SyncGAC(this);
		return gac;
	}
	*/
	
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
	
	public void registerAlarms() {
		//
	}
	
	// preferences
	
	public String language() {
		return prefs.getString(PK.LOCALE, Locale.getDefault().getLanguage());
	}
	
	public boolean specials() {
		return prefs.getBoolean(PK.SPECIALS, false);
	}
	
	public boolean backup() {
		return prefs.getBoolean(PK.GDRVBAK, false);
	}
	
	public String driveAccount() {
		return prefs.getString(PK.GDRVNAME, null);
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
}