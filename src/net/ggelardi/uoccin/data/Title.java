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
	protected static final String MOVIE = "movie";
	protected static final String SERIES = "series";
	protected static final String EPISODE = "episode";
	
	protected static Map<String, Title> cache = new WeakHashMap<String, Title>();
	
	protected static Title get(Context context, Class<?> type, String imdb_id, String table, Object source) {
		Title title = cache.get(imdb_id);
		if (title == null) {
			try {
				Constructor<?> con = type.getConstructor(new Class[] { Context.class, String.class });
				title = ((Title) con.newInstance(new Object[] { context, imdb_id }));
				cache.put(imdb_id, title);
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
		if (source != null) {
			title.load(source);
			if (title.modified && !title.isNew())
				title.commit();
		} else if (title.isNew() || title.isOld())
			title.refresh();
		return title;
	}
	
	protected static List<String> getIDs(Context context, String query, String ... args) {
		List<String> lst = new ArrayList<String>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			while (cur.moveToNext())
				lst.add(cur.getString(0));
		} finally {
			cur.close();
		}
		return lst;
	}
	
	protected final Context context;
	protected final Session session;
	protected final List<TitleEvent> listeners;

	protected boolean watchlist = false;
	protected boolean collected = false;
	protected boolean watched = false;
	
	public String imdb_id;
	public String type;
	public String name;
	public String plot;
	public List<String> actors = new ArrayList<String>();
	public String poster;
	public int runtime;
	public int rating; // user's
	public long timestamp = 0;
	public boolean modified = false;
	
	public Title(Context context, String imdb_id) {
		this.context = context;
		this.session = Session.getInstance(context);
		this.listeners = new ArrayList<Title.TitleEvent>();
		this.imdb_id = imdb_id;
	}
	
	protected abstract void load(Object source);
	
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
	
	protected void delete() {
		session.getDB().delete("title", "imdb_id=?", new String[] { imdb_id });
	}
	
	protected void dispatch(String state) {
		for (TitleEvent l: listeners)
			l.changed(state);
	}
	
	public void addEventListener(TitleEvent listener) {
		listeners.add(listener);
	}
	
	public void removeEventListener(TitleEvent listener) {
		listeners.remove(listener);
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public boolean inCollection() {
		return collected;
	}
	
	public boolean isWatched() {
		return watched;
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			commit();
		}
	}
	
	public void setCollected(boolean value) {
		if (value != collected) {
			collected = value;
			commit();
		}
	}
	
	public void setWatched(boolean value) {
		if (value != watched) {
			watched = value;
			commit();
		}
	}
	
	public boolean isOld() {
		return (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 24; // TODO preferences
	}
	
	public final void commit() {
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			if (watchlist || collected || watched)
				save(isNew());
			else
				delete();
			db.setTransactionSuccessful();
			modified = false;
		} finally {
			db.endTransaction();
		}
	}
	
	public abstract void refresh();
	
	public interface TitleEvent {
		public static String LOADING = "TitleEvent.LOADING";
		public static String READY = "TitleEvent.READY";
		public static String ERROR = "TitleEvent.ERROR";
		
		void changed(String state);
	}
}