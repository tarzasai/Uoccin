package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

public class Series extends Title {
	
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
	
	public static Series load(Context context, String imdb_id) {
		String sql = "select * from titles t inner join series s on (s.imdb_id = t.imdb_id) where t.imdb_id = ?";
		Title res = Title.load(context, Series.class, sql, imdb_id);
		return res != null ? (Series) res : null;
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
		
	}
}