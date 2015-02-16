package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

public class Series extends Title {
	
	public static Series get(Context context, String imdb_id) {
		return (Series) Title.get(context, Series.class, imdb_id, SERIES);
	}
	
	public static List<Series> get(Context context, List<String> imdb_ids) {
		List<Series> res = new ArrayList<Series>();
		for (String sid: imdb_ids)
			res.add(Series.get(context, sid));
		return res;
	}
	
	public Series(Context context, String imdb_id) {
		super(context, imdb_id);
		
		type = SERIES;
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
	public String fanart;
	public boolean watchlist = false;
	
	@Override
	protected void load(Cursor cr) {
		super.load(cr);
		
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
		ci = cr.getColumnIndex("fanart");
		if (!cr.isNull(ci))
			fanart = cr.getString(ci);
	}
	
	@Override
	protected void save(boolean isnew) {
		super.save(isnew);
		
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
		cv.put("fanart", fanart);
		cv.put("watchlist", watchlist);
		
		if (isnew) {
			cv.put("imdb_id", imdb_id);
			session.getDB().insertOrThrow(SERIES, null, cv);
		} else {
			session.getDB().update(SERIES, cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
	
	@Override
	public void refresh() {
		dispatch(TitleEvent.LOADING);
		Callback<TVDB.Series> callback = new Callback<TVDB.Series>() {
			@Override
			public void success(TVDB.Series result, Response response) {
				updateFromTVDB(result);
				dispatch(TitleEvent.READY);
			}
			@Override
			public void failure(RetrofitError error) {
				// TODO Auto-generated method stub
				
				dispatch(TitleEvent.ERROR);
			}
		};
		TVDB.getInstance().getSeries(tvdb_id, Locale.getDefault().getLanguage(), callback);
	}
	
	protected void updateFromTVDB(TVDB.Series data) {
		// title
		if (data.overview != null && !data.overview.trim().equals(""))
			plot = data.overview;
		if (data.actors != null && !data.actors.isEmpty())
			actors = new ArrayList<String>(data.actors);
		if (data.poster != null && !data.poster.trim().equals(""))
			poster = data.poster;
		if (data.fanart != null && !data.fanart.trim().equals(""))
			fanart = data.fanart;
		if (data.runtime > 0)
			runtime = data.runtime;
		// series
		tvdb_id = data.tvdb_id;
		if (data.language != null && !data.language.trim().equals(""))
			language = data.language;
		if (data.firstAired != null)
			year = Commons.getDatePart(data.firstAired, Calendar.YEAR);
		if (data.contentRating != null && !data.contentRating.trim().equals(""))
			rated = data.contentRating;
		if (data.genres != null && !data.genres.isEmpty())
			genres = new ArrayList<String>(data.genres);
		if (data.status != null && !data.status.trim().equals(""))
			status = data.status;
		if (data.network != null && !data.network.trim().equals(""))
			network = data.network;
		if (data.firstAired != null)
			firstAired = data.firstAired.getTime();
		if (data.airsDay > 0)
			airsDay = data.airsDay;
		if (data.airsTime != null && !data.airsTime.trim().equals(""))
			airsTime = data.airsTime;
		save();
	}
	
	public List<Episode> getSeason(int season) {
		String sql = "select imdb_id from episode where series_imdb_id = ? and season = ? order by episode";
		return Episode.get(context, Title.getIDs(context, sql, imdb_id, Integer.toString(season)));
	}
}