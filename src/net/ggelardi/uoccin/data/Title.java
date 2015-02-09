package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class Title {
	public static String MOVIE = "movie";
	public static String SERIES = "series";
	public static String EPISODE = "episode";
	
	protected final Session session;
	protected final SQLiteDatabase dbconn;
	
	public Title(Context context) {
		session = Session.getInstance(context);
		dbconn = session.getDB();
	}

	public String name = "Unknown";
	public String type = MOVIE;
	public int year;
	public String imdb_id;
	public String language;
	public String plot = "Unknown";
	public List<String> genres = new ArrayList<String>();
	public String director;
	public List<String> actors = new ArrayList<String>();
	public List<String> writers = new ArrayList<String>();
	public String poster;
	public String banner;
	//
	public long timestamp = 0;
	public boolean watchlist = false;
	public boolean collected = false;
	public boolean watched = false;
	
	protected void read(Cursor cr) {
		int ci;
		name = cr.getString(cr.getColumnIndex("name"));
		type = cr.getString(cr.getColumnIndex("type"));
		year = cr.getInt(cr.getColumnIndex("year"));
		imdb_id = cr.getString(cr.getColumnIndex("imdb_id"));
		language = cr.getString(cr.getColumnIndex("language"));
		
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		
		ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		
		ci = cr.getColumnIndex("actors");
		if (!cr.isNull(ci))
			actors = Arrays.asList(cr.getString(ci).split(","));
		
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));

		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);

		ci = cr.getColumnIndex("banner");
		if (!cr.isNull(ci))
			banner = cr.getString(ci);

		ci = cr.getColumnIndex("banner");
		if (!cr.isNull(ci))
			banner = cr.getString(ci);
		
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
	}
	
	public long getAge() {
		return System.currentTimeMillis() - timestamp;
	}
	
	public boolean isOld() {
		return getAge()/(1000 * 60 * 60) > 24;
	}
}