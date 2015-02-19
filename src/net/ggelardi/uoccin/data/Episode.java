package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.api.TVDB;
import net.ggelardi.uoccin.serv.Commons;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

public class Episode extends Title {

	public static Episode get(Context context, String imdb_id) {
		return (Episode) Title.get(context, Episode.class, imdb_id, EPISODE, null);
	}

	public static Episode get(Context context, TVDB.Episode source) {
		return (Episode) Title.get(context, Episode.class, source.imdb_id, EPISODE, source);
	}
	
	public static List<Episode> get(Context context, List<String> imdb_ids) {
		List<Episode> res = new ArrayList<Episode>();
		for (String eid: imdb_ids)
			res.add(Episode.get(context, eid));
		return res;
	}
	
	public String series_imdb_id;
	public int series_tvdb_id;
	public int tvdb_id; // episode tvdb_id
	public int season;
	public int episode;
	public String director;
	public List<String> writers = new ArrayList<String>();
	public long firstAired;
	public List<String> subtitles = new ArrayList<String>();
	
	public Episode(Context context, String imdb_id) {
		super(context, imdb_id);
		
		type = EPISODE;
	}
	
	protected void updateFromTVDB(TVDB.Episode data) {
		// title
		if (data.overview != null && !data.overview.trim().equals("") && !plot.equals(data.overview)) {
			plot = data.overview;
			modified = true;
		}
		if (data.guestStars != null && !data.guestStars.isEmpty() && !Commons.sameSimStrLsts(actors, data.guestStars)) {
			actors = new ArrayList<String>(data.guestStars);
			modified = true;
		}
		if (data.poster != null && !data.poster.trim().equals("") && !poster.equals(data.poster)) {
			poster = data.poster;
			modified = true;
		}
		// episode
		tvdb_id = data.tvdb_id;
		if (data.director != null && !data.director.trim().equals("") && !director.equals(data.director)) {
			director = data.director;
			modified = true;
		}
		if (data.writers != null && !data.writers.isEmpty() && !Commons.sameSimStrLsts(writers, data.writers)) {
			writers = new ArrayList<String>(data.writers);
			modified = true;
		}
		if (data.firstAired != null && firstAired != data.firstAired.getTime()) {
			firstAired = data.firstAired.getTime();
			modified = true;
		}
	}
	
	@Override
	protected void load(Object source) {
		updateFromTVDB((TVDB.Episode) source);
	}

	@Override
	protected void load(Cursor cr) {
		super.load(cr);

		series_imdb_id = cr.getString(cr.getColumnIndex("series_imdb_id"));
		series_tvdb_id = cr.getInt(cr.getColumnIndex("series_tvdb_id"));
		tvdb_id = cr.getInt(cr.getColumnIndex("tvdb_id"));
		season = cr.getInt(cr.getColumnIndex("season"));
		episode = cr.getInt(cr.getColumnIndex("episode"));
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		int ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
	}
	
	@Override
	protected void save(boolean isnew) {
		super.save(isnew);
		
		ContentValues cv = new ContentValues();
		cv.put("series_imdb_id", series_imdb_id);
		cv.put("series_tvdb_id", series_tvdb_id);
		cv.put("tvdb_id", tvdb_id);
		cv.put("season", season);
		cv.put("episode", episode);
		cv.put("director", director);
		cv.put("writers", TextUtils.join(",", writers));
		cv.put("firstAired", firstAired);
		cv.put("subtitles", TextUtils.join(",", subtitles));
		cv.put("collected", collected);
		cv.put("watched", watched);
		
		if (isnew) {
			cv.put("imdb_id", imdb_id);
			session.getDB().insertOrThrow(EPISODE, null, cv);
		} else {
			session.getDB().update(EPISODE, cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
	
	@Override
	protected void delete() {
		session.getDB().delete(EPISODE, "imdb_id = ?", new String[] { imdb_id });
		
		super.delete();
	}
	
	@Override
	public void refresh() {
		if (tvdb_id <= 0 || season <= 0 || episode <= 0) {
			Log.v(logTag(), "Missing tvdb_id/season/episode, cannot update...");
			return;
		}
		dispatch(TitleEvent.LOADING);
		Callback<TVDB.Episode> callback = new Callback<TVDB.Episode>() {
			@Override
			public void success(TVDB.Episode result, Response response) {
				updateFromTVDB(result);
				commit();
				dispatch(TitleEvent.READY);
			}
			@Override
			public void failure(RetrofitError error) {
				// TODO Auto-generated method stub

				dispatch(TitleEvent.ERROR);
			}
		};
		TVDB.getInstance().getEpisode(series_tvdb_id, season, episode, Locale.getDefault().getLanguage(), callback);
	}
	
	public Series series() {
		return Series.get(context, series_imdb_id);
	}
	
	public String simpleEID() {
		return String.format(Locale.getDefault(), "%dx%d", season, episode);
	}
	
	public String standardEID() {
		return String.format(Locale.getDefault(), "S%1$02dE%2$02d", season, episode);
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
	}
}