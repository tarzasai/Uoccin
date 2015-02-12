package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class Episode extends Title {

	public static Episode get(String imdb_id) {
		Title res = titlesCache.get(imdb_id);
		return res != null ? (Episode) res : null;
	}
	
	public Episode(Context context) {
		super(context);
		
		type = EPISODE;
	}
	
	public Episode(Context context, Cursor cr) {
		super(context, cr);

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
		
		if (isOld() && session.isOnWIFI())
			refresh();
	}
	
	public String series_imdb_id;
	public int series_tvdb_id;
	public int tvdb_id; // episode tvdb_id
	public int season;
	public int episode;
	public String director;
	public List<String> writers = new ArrayList<String>();
	public long firstAired;
	public boolean collected = false;
	public boolean watched = false;
	
	@Override
	protected void refresh() {
		dispatch(TitleEvent.LOADING);
		
	}
	
	@Override
	protected void update(SQLiteDatabase db) {
		ContentValues cv = new ContentValues();
		cv.put("series_imdb_id", series_imdb_id);
		cv.put("series_tvdb_id", series_tvdb_id);
		cv.put("tvdb_id", tvdb_id);
		cv.put("season", season);
		cv.put("episode", episode);
		cv.put("director", director);
		cv.put("writers", TextUtils.join(",", writers));
		cv.put("firstAired", firstAired);
		cv.put("collected", collected);
		cv.put("watched", watched);
		if (newTitle) {
			cv.put("imdb_id", imdb_id);
			db.insertOrThrow(EPISODE, null, cv);
		} else {
			db.update(EPISODE, cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
	
	public Series getSeries() {
		Series res = Series.get(series_imdb_id);
		if (res == null)
			res = (Series) Title.get(context, Series.class, series_imdb_id, SERIES);
		return res;
	}
}