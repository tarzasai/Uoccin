package net.ggelardi.uoccin.data;

import java.lang.reflect.Constructor;
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

	protected final Context context;
	protected final Session session;
	protected final SQLiteDatabase dbconn;
	
	public Title(Context context) {
		this.context = context;
		this.session = Session.getInstance(context);
		this.dbconn = session.getDB();
	}
	
	public Title(Context context, Cursor cr) {
		this(context);
		
		imdb_id = cr.getString(cr.getColumnIndex("imdb_id"));
		type = cr.getString(cr.getColumnIndex("type"));
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
		
		int ci = cr.getColumnIndex("rating");
		if (!cr.isNull(ci))
			rating = cr.getInt(ci);
		
		ci = cr.getColumnIndex("name");
		if (!cr.isNull(ci))
			name = cr.getString(ci);
		
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
		
		ci = cr.getColumnIndex("runtime");
		if (!cr.isNull(ci))
			runtime = cr.getInt(ci);
	}
	
	protected static Title load(Context context, Class<?> type, String query, String imdb_id) {
		Cursor cr = Session.getInstance(context).getDB().rawQuery(query, new String[] { imdb_id });
		try {
			if (cr.moveToFirst()) {
				Constructor<?> ct = type.getConstructor(new Class[] { Context.class, Cursor.class });
				return ((Title) ct.newInstance(new Object[] { context, cr }));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			cr.close();
		}
		return null;
	}
	
	protected abstract void refresh();

	public String imdb_id;
	public String type;
	public String name;
	public String plot;
	public String director;
	public List<String> actors = new ArrayList<String>();
	public List<String> writers = new ArrayList<String>();
	public String poster;
	public String banner;
	public int runtime;
	public long timestamp = System.currentTimeMillis();
	public int rating; // user's
	
	public boolean isOld() {
		return (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 24;
	}
	
	//
	
	private List<TitleEvent> listeners = new ArrayList<Title.TitleEvent>();
	
	public interface TitleEvent {
		public static String LOADING = "TitleEvent.LOADING";
		public static String READY = "TitleEvent.READY";
		
		void changed(String state);
	}
	
	public void addEventListener(TitleEvent listener) {
		listeners.add(listener);
	}
	
	public void removeEventListener(TitleEvent listener) {
		listeners.remove(listener);
	}
	
	protected void dispatch(String state) {
		for (TitleEvent l: listeners)
			l.changed(state);
	}
}