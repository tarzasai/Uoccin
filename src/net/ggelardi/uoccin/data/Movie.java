package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class Movie extends Title {

	public static Movie get(Context context, String imdb_id) {
		return (Movie) Title.get(context, Movie.class, imdb_id, MOVIE);
	}
	
	public Movie(Context context, String imdb_id) {
		super(context, imdb_id);
		
		type = MOVIE;
	}
	
	public String language;
	public int year;
	public String rated;
	public List<String> genres = new ArrayList<String>();
	public String director;
	public List<String> writers = new ArrayList<String>();
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
	protected void load(Cursor cr) {
		super.load(cr);
		
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
		ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
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
	}
	
	@Override
	protected void save(boolean isnew) {
		super.save(isnew);
		
		ContentValues cv = new ContentValues();
		cv.put("language", language);
		cv.put("year", year);
		cv.put("rated", rated);
		cv.put("genres", TextUtils.join(",", genres));
		cv.put("director", director);
		cv.put("writers", TextUtils.join(",", writers));
		cv.put("country", country);
		cv.put("released", released);
		cv.put("awards", awards);
		cv.put("metascore", metascore);
		cv.put("imdbRating", imdbRating);
		cv.put("imdbVotes", imdbVotes);
		cv.put("watchlist", watchlist);
		cv.put("collected", collected);
		cv.put("watched", watched);
		
		if (isnew) {
			cv.put("imdb_id", imdb_id);
			session.getDB().insertOrThrow(MOVIE, null, cv);
		} else {
			session.getDB().update(MOVIE, cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
	
	@Override
	public void refresh() {
		dispatch(TitleEvent.LOADING);
		
	}
}