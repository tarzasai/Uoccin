package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.database.Cursor;

public class Movie extends Title {
	
	public Movie(Context context) {
		super(context);
		
		type = MOVIE;
	}
	
	public Movie(Context context, Cursor cr) {
		super(context, cr);
		
		language = cr.getString(cr.getColumnIndex("language"));
		year = cr.getInt(cr.getColumnIndex("year"));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		
		int ci = cr.getColumnIndex("rated");
		if (!cr.isNull(ci))
			rated = cr.getString(ci);
		
		ci = cr.getColumnIndex("genres");
		if (!cr.isNull(ci))
			genres = Arrays.asList(cr.getString(ci).split(","));
		
		ci = cr.getColumnIndex("country");
		if (!cr.isNull(ci))
			country = cr.getString(ci);
		
		ci = cr.getColumnIndex("released");
		if (!cr.isNull(ci))
			released = cr.getLong(ci);
		
		ci = cr.getColumnIndex("awards");
		if (!cr.isNull(ci))
			awards = cr.getString(ci);
		
		ci = cr.getColumnIndex("metascore");
		if (!cr.isNull(ci))
			metascore = cr.getInt(ci);
		
		ci = cr.getColumnIndex("imdbRating");
		if (!cr.isNull(ci))
			imdbRating = cr.getDouble(ci);
		
		ci = cr.getColumnIndex("imdbVotes");
		if (!cr.isNull(ci))
			imdbVotes = cr.getInt(ci);
		
		if (isOld() && session.isOnWIFI())
			refresh();
	}

	public static Movie load(Context context, String imdb_id) {
		String sql = "select * from titles t inner join movies m on (m.imdb_id = t.imdb_id) where t.imdb_id = ?";
		Title res = Title.load(context, Movie.class, sql, imdb_id);
		return res != null ? (Movie) res : null;
	}
	
	public String language;
	public int year;
	public String rated;
	public List<String> genres = new ArrayList<String>();
	public String country;
	public long released;
	public String awards;
	public int metascore;
	public Double imdbRating;
	public int imdbVotes;
	public boolean watchlist = false;
	public boolean collected = false;
	public boolean watched = false;
	
	@Override
	protected void refresh() {
		dispatch(TitleEvent.LOADING);
		
	}
}