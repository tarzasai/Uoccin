package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import net.ggelardi.uoccin.api.TVDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class Series {
	private static final String LOGTAG = "Series";
	private static final String TABLE = "series";
	
	private static final Set<Series> cache = Collections.newSetFromMap(new WeakHashMap<Series, Boolean>());
	
	private static final List<OnTitleListener> listeners = new ArrayList<OnTitleListener>();
	
	private final Session session;
	
	private int rating = 0;
	private boolean watchlist = false;
	
	public String tvdb_id;
	public String name;
	public int year;
	public String plot;
	public String poster;
	public List<String> genres = new ArrayList<String>();
	public String language;
	public List<String> actors = new ArrayList<String>();
	public String imdb_id;
	public String status;
	public String network;
	public long firstAired;
	public int airsDay;
	public String airsTime;
	public int runtime;
	public String rated;
	public String fanart;
	public long timestamp = 0;
	public boolean modified = false;
	
	public Series(Context context, String tvdb_id) {
		this.session = Session.getInstance(context);
		this.tvdb_id = tvdb_id;
	}
	
	private static void dispatch(String state, Throwable error) {
		for (OnTitleListener listener: listeners)
			//if (listener != null)
				listener.changed(state, error);
	}
	
	private static synchronized Series getInstance(Context context, String tvdb_id) {
		for (Series m: cache)
			if (m.tvdb_id.equals(tvdb_id)) {
				Log.v(LOGTAG, tvdb_id + " found in cache");
				return m;
			}
		Series res = new Series(context, tvdb_id);
		cache.add(res);
		Cursor cur = Session.getInstance(context).getDB().query(TABLE, null, "tvdb_id=?", new String[] { tvdb_id },
				null, null, null);
		try {
			if (cur.moveToFirst()) {
				res.load(cur);
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static Series get(Context context, String tvdb_id) {
		Series res = Series.getInstance(context, tvdb_id);
		if (res.isNew() || res.isOld())
			res.refresh();
		return res;
	}
	
	public static Series get(Context context, TVDB.Series source) {
		Series res = Series.getInstance(context, source.tvdb_id);
		res.load(source);
		if (res.modified && !res.isNew())
			res.commit();
		return res;
	}
	
	public static List<Series> get(Context context, List<String> tvdb_ids) {
		List<Series> res = new ArrayList<Series>();
		for (String sid: tvdb_ids)
			res.add(Series.get(context, sid));
		return res;
	}
	
	public static List<Series> get(Context context, String query, String ... args) {
		List<Series> res = new ArrayList<Series>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			int ci = cur.getColumnIndex("tvdb_id");
			String tvdb_id;
			while (cur.moveToNext()) {
				tvdb_id = cur.getString(ci);
				res.add(Series.get(context, tvdb_id));
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static List<Series> find(Context context, String text) {
		List<Series> res = new ArrayList<Series>();
		TVDB.Data lst = TVDB.getInstance().findSeries(text, Locale.getDefault().getLanguage());
		if (!lst.series().isEmpty())
			for (TVDB.Series series: lst.series())
				res.add(Series.get(context, series));
		else
			dispatch(OnTitleListener.NOTFOUND, null);
		return res;
	}
	
	public static void addOnTitleEventListener(OnTitleListener aListener) {
		listeners.add(aListener);
	}
	
	private void updateFromTVDB(TVDB.Series data) {
		if (!TextUtils.isEmpty(data.name) && (TextUtils.isEmpty(name) || !name.equals(data.name))) {
			name = data.name;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.overview) && (TextUtils.isEmpty(plot) || !plot.equals(data.overview))) {
			plot = data.overview;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.poster) && (TextUtils.isEmpty(poster) || !poster.equals(data.poster))) {
			poster = data.poster;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.genres)) {
			List<String> chk = Arrays.asList(data.genres.split("|"));
			if (!Commons.sameStringLists(genres, chk)) {
				genres = new ArrayList<String>(chk);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.language) && (TextUtils.isEmpty(language) || !language.equals(data.language))) {
			language = data.language;
			modified = true;
		} else if (!TextUtils.isEmpty(data.Language) && (TextUtils.isEmpty(language) || !language.equals(data.Language))) {
			language = data.Language;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.actors)) {
			List<String> chk = Arrays.asList(data.actors.split("|"));
			if (!Commons.sameStringLists(actors, chk)) {
				actors = new ArrayList<String>(chk);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.imdb_id) && (TextUtils.isEmpty(imdb_id) || !imdb_id.equals(data.imdb_id))) {
			imdb_id = data.imdb_id;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.status) && (TextUtils.isEmpty(status) || !status.equals(data.status))) {
			status = data.status;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.network) && (TextUtils.isEmpty(network) || !network.equals(data.network))) {
			network = data.network;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.firstAired)) {
			long chk;
			try {
				chk = Commons.DateStuff.english("yyyy-MM-dd").parse(data.firstAired).getTime();
			} catch (Exception err) {
				Log.e("updateFromTVDB", data.firstAired, err);
				chk = 0;
			}
			if (chk > 0) {
				firstAired = chk;
				year = Commons.getDatePart(firstAired, Calendar.YEAR);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.airsDay)) {
			int chk = Commons.DateStuff.day2int(data.airsDay);
			if (chk > 0) {
				airsDay = chk;
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.airsTime) && (TextUtils.isEmpty(airsTime) || !airsTime.equals(data.airsTime))) {
			airsTime = data.airsTime;
			modified = true;
		}
		if (data.runtime > 0 && runtime != data.runtime) {
			runtime = data.runtime;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.contentRating) && (TextUtils.isEmpty(rated) || !rated.equals(data.contentRating))) {
			rated = data.contentRating;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.fanart) && (TextUtils.isEmpty(fanart) || !fanart.equals(data.fanart))) {
			fanart = data.fanart;
			modified = true;
		}
	}

	protected void load(Object source) {
		updateFromTVDB((TVDB.Series) source);
	}
	
	protected void load(Cursor cr) {
		Log.v(LOGTAG, "Loading series " + tvdb_id);
		
		int ci;
		tvdb_id = cr.getString(cr.getColumnIndex("tvdb_id")); // it's already set btw...
		name = cr.getString(cr.getColumnIndex("name"));
		ci = cr.getColumnIndex("year");
		if (!cr.isNull(ci))
			year = cr.getInt(ci);
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		ci = cr.getColumnIndex("genres");
		if (!cr.isNull(ci))
			genres = Arrays.asList(cr.getString(ci).split(","));
		language = cr.getString(cr.getColumnIndex("language"));
		ci = cr.getColumnIndex("actors");
		if (!cr.isNull(ci))
			actors = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("imdb_id");
		if (!cr.isNull(ci))
			imdb_id = cr.getString(ci);
		ci = cr.getColumnIndex("status");
		if (!cr.isNull(ci))
			status = cr.getString(ci);
		ci = cr.getColumnIndex("network");
		if (!cr.isNull(ci))
			network = cr.getString(ci);
		ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
		ci = cr.getColumnIndex("airsDay");
		if (!cr.isNull(ci))
			airsDay = cr.getInt(ci);
		ci = cr.getColumnIndex("airsTime");
		if (!cr.isNull(ci))
			airsTime = cr.getString(ci);
		ci = cr.getColumnIndex("runtime");
		if (!cr.isNull(ci))
			runtime = cr.getInt(ci);
		ci = cr.getColumnIndex("rated");
		if (!cr.isNull(ci))
			rated = cr.getString(ci);
		ci = cr.getColumnIndex("fanart");
		if (!cr.isNull(ci))
			fanart = cr.getString(ci);
		ci = cr.getColumnIndex("rating");
		if (!cr.isNull(ci))
			rating = cr.getInt(ci);
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
	}
	
	protected void save(boolean isnew) {
		Log.v(LOGTAG, "Saving series " + tvdb_id);
		
		ContentValues cv = new ContentValues();
		cv.put("tvdb_id", tvdb_id);
		cv.put("name", name);
		cv.put("year", year);
		cv.put("plot", plot);
		cv.put("poster", poster);
		cv.put("genres", TextUtils.join(",", genres));
		cv.put("language", language);
		cv.put("actors", TextUtils.join(",", actors));
		cv.put("imdb_id", imdb_id);
		cv.put("status", status);
		cv.put("network", network);
		cv.put("firstAired", firstAired);
		cv.put("airsDay", airsDay);
		cv.put("airsTime", airsTime);
		cv.put("runtime", runtime);
		cv.put("rated", rated);
		cv.put("fanart", fanart);
		cv.put("rating", rating);
		cv.put("watchlist", watchlist);
		timestamp = System.currentTimeMillis();
		cv.put("timestamp", timestamp);
		
		if (isnew)
			session.getDB().insertOrThrow(TABLE, null, cv);
		else
			session.getDB().update(TABLE, cv, "tvdb_id=?", new String[] { tvdb_id });
	}
	
	protected void delete() {
		Log.v(LOGTAG, "Deleting series " + tvdb_id);
		
		session.getDB().delete(TABLE, "tvdb_id=?", new String[] { tvdb_id });
	}
	
	public void refresh() {
		Log.v(LOGTAG, "Refreshing series " + tvdb_id);
		dispatch(OnTitleListener.LOADING, null);
		Callback<TVDB.Data> callback = new Callback<TVDB.Data>() {
			@Override
			public void success(TVDB.Data result, Response response) {
				updateFromTVDB(result.series().get(0));
				commit();
				dispatch(OnTitleListener.READY, null);
			}
			@Override
			public void failure(RetrofitError error) {
				Log.e(LOGTAG, "refresh", error);
				
				plot = error.getLocalizedMessage();
				
				dispatch(OnTitleListener.ERROR, error);
			}
		};
		if (!TextUtils.isEmpty(tvdb_id))
			TVDB.getInstance().getSeries(tvdb_id, Locale.getDefault().getLanguage(), callback);
		else if (!TextUtils.isEmpty(imdb_id))
			TVDB.getInstance().getSeriesByImdb(imdb_id, Locale.getDefault().getLanguage(), callback);
	}
	
	public final void commit() {
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			if (watchlist)
				save(isNew());
			else
				delete();
			db.setTransactionSuccessful();
			modified = false;
		} finally {
			db.endTransaction();
		}
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 24; // TODO preferences
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public int getRating() {
		return rating;
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			commit();
		}
	}
	
	public void setRating(int value) {
		if (value != rating) {
			rating = value;
			commit();
		}
	}
}