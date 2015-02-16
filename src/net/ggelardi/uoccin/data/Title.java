package net.ggelardi.uoccin.data;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.ggelardi.uoccin.serv.Session;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public abstract class Title {
	public static String MOVIE = "movie";
	public static String SERIES = "series";
	public static String EPISODE = "episode";
	
	// cache
	protected static Map<String, Title> titlesCache = new WeakHashMap<String, Title>();
	
	protected static Title get(Context context, Class<?> type, String imdb_id, String table) {
		Title title = titlesCache.get(imdb_id);
		if (title == null) {
			try {
				Constructor<?> con = type.getConstructor(new Class[] { Context.class, String.class });
				title = ((Title) con.newInstance(new Object[] { context, imdb_id }));
				titlesCache.put(imdb_id, title);
			} catch (Exception e) {
				e.printStackTrace();
			}
			String sql = "select * from title t inner join " + table + " x on (x.imdb_id = t.imdb_id) where t.imdb_id = ?";
			Cursor cur = Session.getInstance(context).getDB().rawQuery(sql, new String[] { imdb_id });
			try {
				if (cur.moveToFirst())
					title.load(cur);
			} finally {
				cur.close();
			}
		}
		if (title.isNew() || title.isOld())
			title.refresh();
		return title;
	}
	
	protected final Context context;
	protected final Session session;
	
	public Title(Context context, String imdb_id) {
		this.context = context;
		this.session = Session.getInstance(context);
		this.imdb_id = imdb_id;
	}
	
	protected void load(Cursor cr) {
		imdb_id = cr.getString(cr.getColumnIndex("imdb_id"));
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
		
		ci = cr.getColumnIndex("actors");
		if (!cr.isNull(ci))
			actors = Arrays.asList(cr.getString(ci).split(","));
		
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		
		ci = cr.getColumnIndex("runtime");
		if (!cr.isNull(ci))
			runtime = cr.getInt(ci);
	}
	
	protected void save(boolean isnew) {
		timestamp = System.currentTimeMillis();
		
		ContentValues cv = new ContentValues();
		cv.put("name", name);
		cv.put("plot", plot);
		cv.put("actors", TextUtils.join(",", actors));
		cv.put("poster", poster);
		cv.put("runtime", runtime);
		cv.put("rating", rating);
		cv.put("timestamp", timestamp);
		
		if (isnew) {
			cv.put("imdb_id", imdb_id);
			cv.put("type", type);
			session.getDB().insertOrThrow("title", null, cv);
		} else {
			session.getDB().update("title", cv, "imdb_id=?", new String[] { imdb_id });
		}
	}
	
	public String imdb_id;
	public String type;
	public String name;
	public String plot;
	public List<String> actors = new ArrayList<String>();
	public String poster;
	public int runtime;
	public int rating; // user's
	public long timestamp = 0;
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 24; // TODO preferences
	}
	
	public abstract void refresh();
	
	public void save() {
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			save(isNew());
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	//
	
	private List<TitleEvent> listeners = new ArrayList<Title.TitleEvent>();
	
	public interface TitleEvent {
		public static String LOADING = "TitleEvent.LOADING";
		public static String READY = "TitleEvent.READY";
		public static String ERROR = "TitleEvent.ERROR";
		
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