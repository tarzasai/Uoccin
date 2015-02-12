package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.SeriesAPI;
import net.ggelardi.uoccin.serv.SeriesAPI.TVDB_Series;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class Series extends Title {

	public static Series get(String imdb_id) {
		Title res = titlesCache.get(imdb_id);
		return res != null ? (Series) res : null;
	}
	
	public Series(Context context) {
		super(context);
		
		type = SERIES;
	}
	
	public Series(Context context, Cursor cr) {
		super(context, cr);
		
		language = cr.getString(cr.getColumnIndex("language"));
		year = cr.getInt(cr.getColumnIndex("year"));
		tvdb_id = cr.getInt(cr.getColumnIndex("tvdb_id"));
		status = cr.getString(cr.getColumnIndex("status"));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		
		int ci = cr.getColumnIndex("rated");
		if (!cr.isNull(ci))
			rated = cr.getString(ci);
		
		ci = cr.getColumnIndex("genres");
		if (!cr.isNull(ci))
			genres = Arrays.asList(cr.getString(ci).split(","));
		
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
		
		if (isOld() && session.isOnWIFI())
			refresh();
	}
	
	public String language;
	public int year;
	public String rated;
	public List<String> genres = new ArrayList<String>();
	public int tvdb_id;
	public String status;
	public String network;
	public long firstAired;
	public int airsDay;
	public String airsTime;
	public boolean watchlist = false;
	
	@Override
	protected void refresh() {
		dispatch(TitleEvent.LOADING);
		Callback<SeriesAPI.TVDB_Series> callback = new Callback<SeriesAPI.TVDB_Series>() {
			@Override
			public void success(TVDB_Series result, Response response) {
				// title
				plot = result.overview;
				actors = result.actors;
				poster = result.poster;
				banner = result.banner;
				runtime = result.runtime;
				// series
				language = result.language;
				year = Commons.getDatePart(result.firstAired, Calendar.YEAR);
				rated = result.contentRating;
				genres = result.genres;
				tvdb_id = result.tvdb_id;
				status = result.status;
				network = result.network;
				firstAired = result.firstAired.getTime();
				airsDay = result.airsDay;
				airsTime = result.airsTime;
				// ok
				refreshed = true;
				save();
				dispatch(TitleEvent.READY);
			}
			@Override
			public void failure(RetrofitError error) {
				// TODO Auto-generated method stub
				
				dispatch(TitleEvent.ERROR);
			}
		};
		SeriesAPI.client().getSeries(tvdb_id, Locale.getDefault().getLanguage(), callback);
	}
	
	@Override
	protected void update(SQLiteDatabase db) {
		ContentValues cv = new ContentValues();
		cv.put("language", language);
		cv.put("year", year);
		cv.put("rated", rated);
		cv.put("genres", TextUtils.join(",", genres));
		cv.put("tvdb_id", tvdb_id);
		cv.put("status", status);
		cv.put("network", network);
		cv.put("firstAired", firstAired);
		cv.put("airsDay", airsDay);
		cv.put("airsTime", airsTime);
		cv.put("watchlist", watchlist);
		if (newTitle) {
			cv.put("imdb_id", imdb_id);
			db.insertOrThrow(SERIES, null, cv);
		} else {
			db.update(SERIES, cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
}