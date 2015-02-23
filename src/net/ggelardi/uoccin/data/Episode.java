package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
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

public class Episode {
	private static final String LOGTAG = "Episode";
	private static final String TABLE = "episode";
	
	private static final Set<Episode> cache = Collections.newSetFromMap(new WeakHashMap<Episode, Boolean>());
	
	private static final List<OnTitleListener> listeners = new ArrayList<OnTitleListener>();
	
	private final Context context;
	private final Session session;

	private boolean watchlist = false;
	private boolean collected = false;
	private boolean watched = false;
	
	public String tvdb_id;
	public String series; // series tvdb_id
	public int season;
	public int episode;
	public String name;
	public String plot;
	public String poster;
	public List<String> writers = new ArrayList<String>();
	public String director;
	public List<String> guestStars = new ArrayList<String>();
	public long firstAired;
	public String imdb_id;
	public List<String> subtitles = new ArrayList<String>();
	public long timestamp = 0;
	public boolean modified = false;
	
	/*
	public Episode(Context context, String tvdb_id) {
		this.session = Session.getInstance(context);
		this.tvdb_id = tvdb_id;
	}
	*/
	
	public Episode(Context context, String series, int season, int episode) {
		this.context = context;
		this.session = Session.getInstance(context);
		this.series = series;
		this.season = season;
		this.episode = episode;
	}
	
	private static void dispatch(String state, Throwable error) {
		for (OnTitleListener listener: listeners)
			//if (listener != null)
				listener.changed(state, error);
	}
	
	private static String getEID(String series, int season, int episode) {
		return series + "." + String.format(Locale.getDefault(), "S%1$02dE%2$02d", season, episode);
	}
	
	private static synchronized Episode getInstance(Context context, String series, int season, int episode) {
		String eid = getEID(series, season, episode);
		for (Episode m: cache)
			if (m.extendedEID().equals(eid)) {
				Log.v("Series", eid + " found in cache");
				return m;
			}
		Episode res = new Episode(context, series, season, episode);
		cache.add(res);
		Cursor cur = Session.getInstance(context).getDB().query("episode", null, "series=? and season=? and episode=?",
				new String[] { series, Integer.toString(season), Integer.toString(episode) }, null, null, null);
		try {
			if (cur.moveToFirst()) {
				res.load(cur);
			}
		} finally {
			cur.close();
		}
		return res;
	}

	public static Episode get(Context context, String series, int season, int episode) {
		Episode res = Episode.getInstance(context, series, season, episode);
		if (res.isNew() || res.isOld())
			res.refresh();
		return res;
	}

	public static Episode get(Context context, TVDB.Episode source) {
		Episode res = Episode.getInstance(context, source.seriesid, source.season, source.episode);
		res.load(source);
		if (res.modified && !res.isNew())
			res.commit();
		return res;
	}
	
	public static List<Episode> get(Context context, String query, String ... args) {
		List<Episode> res = new ArrayList<Episode>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			String series;
			int season;
			int episode;
			if (cur.moveToFirst()) {
				int c1 = cur.getColumnIndex("series");
				int c2 = cur.getColumnIndex("season");
				int c3 = cur.getColumnIndex("episode");
				do {
					series = cur.getString(c1);
					season = cur.getInt(c2);
					episode = cur.getInt(c3);
					res.add(Episode.get(context, series, season, episode));
				} while (cur.moveToNext());
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	protected void updateFromTVDB(TVDB.Episode data) {
		if (!TextUtils.isEmpty(data.tvdb_id) && (TextUtils.isEmpty(tvdb_id) || !tvdb_id.equals(data.tvdb_id))) {
			tvdb_id = data.tvdb_id;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.seriesid) && (TextUtils.isEmpty(series) || !series.equals(data.seriesid))) {
			series = data.seriesid;
			modified = true;
		}
		if (data.season > 0 && season != data.season) {
			season = data.season;
			modified = true;
		}
		if (data.episode > 0 && episode != data.episode) {
			episode = data.episode;
			modified = true;
		}
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
		if (!TextUtils.isEmpty(data.writers)) {
			List<String> chk = Arrays.asList(data.writers.split("|"));
			if (!Commons.sameStringLists(writers, chk)) {
				writers = new ArrayList<String>(chk);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.director) && (TextUtils.isEmpty(director) || !director.equals(data.director))) {
			director = data.director;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.guestStars)) {
			List<String> chk = Arrays.asList(data.guestStars.split("|"));
			if (!Commons.sameStringLists(guestStars, chk)) {
				guestStars = new ArrayList<String>(chk);
				modified = true;
			}
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
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.imdb_id) && (TextUtils.isEmpty(imdb_id) || !imdb_id.equals(data.imdb_id))) {
			imdb_id = data.imdb_id;
			modified = true;
		}
	}
	
	protected void load(Object source) {
		updateFromTVDB((TVDB.Episode) source);
	}

	protected void load(Cursor cr) {
		Log.v(LOGTAG, "Loading episode " + tvdb_id);
		
		int ci;
		tvdb_id = cr.getString(cr.getColumnIndex("tvdb_id"));
		series = cr.getString(cr.getColumnIndex("series"));
		season = cr.getInt(cr.getColumnIndex("season"));
		episode = cr.getInt(cr.getColumnIndex("episode"));
		ci = cr.getColumnIndex("name");
		if (!cr.isNull(ci))
			name = cr.getString(ci);
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("guestStars");
		if (!cr.isNull(ci))
			guestStars = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
		ci = cr.getColumnIndex("imdb_id");
		if (!cr.isNull(ci))
			imdb_id = cr.getString(ci);
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
	}
	
	protected void save(boolean isnew) {
		Log.v(LOGTAG, "Saving episode " + tvdb_id);
		
		ContentValues cv = new ContentValues();
		cv.put("tvdb_id", tvdb_id);
		cv.put("series", series);
		cv.put("season", season);
		cv.put("episode", episode);
		cv.put("name", name);
		cv.put("plot", plot);
		cv.put("poster", poster);
		cv.put("writers", TextUtils.join(",", writers));
		cv.put("director", director);
		cv.put("actors", TextUtils.join(",", guestStars));
		cv.put("firstAired", firstAired);
		cv.put("imdb_id", imdb_id);
		cv.put("subtitles", TextUtils.join(",", subtitles));
		cv.put("collected", collected);
		cv.put("watched", watched);
		timestamp = System.currentTimeMillis();
		cv.put("timestamp", timestamp);
		
		if (isnew)
			session.getDB().insertOrThrow(TABLE, null, cv);
		else
			session.getDB().update(TABLE, cv, "tvdb_id=?", new String[] { tvdb_id });
	}
	
	protected void delete() {
		Log.v(LOGTAG, "Deleting episode " + tvdb_id);
		
		session.getDB().delete(TABLE, "tvdb_id=?", new String[] { tvdb_id });
	}
	
	public void refresh() {
		if (TextUtils.isEmpty(tvdb_id) && (TextUtils.isEmpty(series) || season <= 0 || episode <= 0)) {
			Log.v(LOGTAG, "Missing tvdb_id/series/season/episode, cannot update...");
			return;
		}
		Log.v(LOGTAG, "Refreshing episode " + tvdb_id);
		dispatch(OnTitleListener.LOADING, null);
		Callback<TVDB.Data> callback = new Callback<TVDB.Data>() {
			@Override
			public void success(TVDB.Data result, Response response) {
				updateFromTVDB(result.episodes.get(0));
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
			TVDB.getInstance().getEpisodeById(tvdb_id, Locale.getDefault().getLanguage(), callback);
		else
			TVDB.getInstance().getEpisode(series, season, episode, Locale.getDefault().getLanguage(), callback);
	}
	
	public final void commit() {
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			if (watchlist || collected || watched)
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
	
	public boolean inCollection() {
		return collected;
	}
	
	public boolean isWatched() {
		return watched;
	}
	
	public String simpleEID() {
		return String.format(Locale.getDefault(), "%dx%d", season, episode);
	}
	
	public String standardEID() {
		return getEID(series, season, episode).split("\\.")[1];
	}
	
	public String extendedEID() {
		return getEID(series, season, episode);
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
	}
	
	public Series getSeries() {
		return Series.get(context, series);
	}
}