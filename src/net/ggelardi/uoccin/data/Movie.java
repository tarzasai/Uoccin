package net.ggelardi.uoccin.data;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.XML.OMDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import net.ggelardi.uoccin.serv.SimpleCache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class Movie extends Title {
	private static final String TAG = "Movie";
	private static final String TABLE = "movie";
	
	private static final SimpleCache cache = new SimpleCache(500);

	private final Session session;

	private int rating = 0;
	private List<String> tags = new ArrayList<String>();
	private boolean watchlist = false;
	private boolean collected = false;
	private boolean watched = false;
	
	public String imdb_id;
	public String name;
	public int year;
	public String plot;
	public String poster;
	public List<String> genres = new ArrayList<String>();
	public String language;
	public String director;
	public List<String> writers = new ArrayList<String>();
	public List<String> actors = new ArrayList<String>();
	public String country;
	public long released;
	public int runtime;
	public String rated;
	public String awards;
	public int metascore;
	public Double imdbRating;
	public int imdbVotes;
	public List<String> subtitles = new ArrayList<String>();
	public long timestamp = 0;
	public boolean modified = false;
	
	public Movie(Context context, String imdb_id) {
		this.session = Session.getInstance(context);
		this.imdb_id = imdb_id;
	}
	
	private static synchronized Movie getInstance(Context context, String imdb_id) {
		Object tmp = cache.get(imdb_id);
		if (tmp != null) {
			Log.v(TAG, "Movie " + imdb_id + " found in cache");
			return (Movie) tmp;
		}
		Movie res = new Movie(context, imdb_id);
		cache.add(imdb_id, res);
		Cursor cur = Session.getInstance(context).getDB().query(TABLE, null, "imdb_id=?", new String[] { imdb_id },
				null, null, null);
		try {
			if (cur.moveToFirst()) {
				res.load(cur);
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static Movie get(Context context, String imdb_id) {
		Movie res = Movie.getInstance(context, imdb_id);
		if (res.isNew() || res.isOld())
			res.refresh();
		return res;
	}

	//@formatter:off
	/*
	public static Movie get(Context context, OMDB.Movie source) {
		Movie res = Movie.getInstance(context, source.imdbID);
		res.load(source);
		res.setInfoLine();
		if (res.modified && !res.isNew())
			res.commit();
		return res;
	}
	*/
	//@formatter:on
	
	public static List<Movie> get(Context context, List<String> imdb_ids) {
		List<Movie> res = new ArrayList<Movie>();
		for (String id: imdb_ids)
			res.add(Movie.get(context, id));
		return res;
	}
	
	public static List<Movie> get(Context context, String query, String ... args) {
		List<Movie> res = new ArrayList<Movie>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			int ci = cur.getColumnIndex("imdb_id");
			String imdb_id;
			while (cur.moveToNext()) {
				imdb_id = cur.getString(ci);
				res.add(Movie.get(context, imdb_id));
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static List<Movie> find(Context context, String text) {
		List<Movie> res = new ArrayList<Movie>();
		String response;
		try {
			response = OMDB.getInstance().findMovie(text);
		} catch (Exception err) {
			Log.e(TAG, "find", err);
			dispatch(OnTitleListener.ERROR, null);
			return res;
		}
		Document doc = Commons.XML.str2xml(response);
		if (doc != null) {
			NodeList lst = doc.getElementsByTagName("Movie");
			for (int i = 0; i < lst.getLength(); i++) {
				String imdbID = Commons.XML.nodeText((Element) lst.item(i), "imdbID");
				// since OMDB search returns id and title only, we'll use the factory method that retrieves full data
				if (!TextUtils.isEmpty(imdbID))
					res.add(Movie.get(context, imdbID));
			}
		}
		if (res.isEmpty())
			dispatch(OnTitleListener.NOTFOUND, null);
		return res;
	}
	
	protected void load(Element xml) {
		String chk;
		chk = Commons.XML.nodeText(xml, "title", "Title");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(name) || !name.equals(chk))) {
			name = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "plot");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(plot) || !plot.equals(chk))) {
			plot = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "year");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = Integer.parseInt(chk);
				if (r > 0 && r != year) {
					year = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "poster");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(poster) || !poster.equals(chk))) {
			poster = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "genre");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split(",\\")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(genres, lst)) {
				genres = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "language");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(language) || !language.equals(chk))) {
			language = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "director");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(director) || !director.equals(chk))) {
			director = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "writer");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split(",\\")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(writers, lst)) {
				writers = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "actors");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split(",\\")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(actors, lst)) {
				actors = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "country");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(country) || !country.equals(chk))) {
			country = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "released");
		if (!TextUtils.isEmpty(chk)) {
			try {
				long t = Commons.SDF.eng("dd MMM yyyy").parse(chk).getTime();
				if (t > 0) {
					released = t;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "runtime");
		if (!TextUtils.isEmpty(chk)) {
			if (chk.contains(" min"))
				chk = chk.split(" ")[0];
			try {
				int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
				if (r > 0 && r != runtime) {
					runtime = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "rated");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(rated) || !rated.equals(chk))) {
			rated = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "awards");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(awards) || !awards.equals(chk))) {
			awards = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "metascore");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
				if (r > 0 && r != metascore) {
					metascore = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "imdbRating");
		if (!TextUtils.isEmpty(chk)) {
			try {
				double r = Double.parseDouble(chk);
				if (r > 0 && r != imdbRating) {
					imdbRating = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "imdbVotes");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
				if (r > 0 && r != imdbVotes) {
					imdbVotes = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		
	}
	
	protected void load(Cursor cr) {
		Log.v(TAG, "Loading movie " + imdb_id);
		
		int ci;
		imdb_id = cr.getString(cr.getColumnIndex("imdb_id")); // it's already set btw...
		name = cr.getString(cr.getColumnIndex("name"));
		year = cr.getInt(cr.getColumnIndex("year"));
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		ci = cr.getColumnIndex("genres");
		if (!cr.isNull(ci))
			genres = Arrays.asList(cr.getString(ci).split(","));
		language = cr.getString(cr.getColumnIndex("language"));
		ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("actors");
		if (!cr.isNull(ci))
			actors = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("country");
		if (!cr.isNull(ci))
			country = cr.getString(ci);
		ci = cr.getColumnIndex("released");
		if (!cr.isNull(ci))
			released = cr.getLong(ci);
		ci = cr.getColumnIndex("runtime");
		if (!cr.isNull(ci))
			runtime = cr.getInt(ci);
		ci = cr.getColumnIndex("rated");
		if (!cr.isNull(ci))
			rated = cr.getString(ci);
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
		ci = cr.getColumnIndex("rating");
		if (!cr.isNull(ci))
			rating = cr.getInt(ci);
		ci = cr.getColumnIndex("tags");
		if (!cr.isNull(ci))
			tags = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
	}
	
	protected void save(boolean isnew) {
		Log.v(TAG, "Saving movie " + imdb_id);
		
		ContentValues cv = new ContentValues();
		cv.put("name", name);
		cv.put("year", year);
		cv.put("plot", plot);
		cv.put("poster", poster);
		cv.put("genres", TextUtils.join(",", genres));
		cv.put("language", language);
		cv.put("director", director);
		cv.put("writers", TextUtils.join(",", writers));
		cv.put("actors", TextUtils.join(",", actors));
		cv.put("country", country);
		cv.put("released", released);
		cv.put("runtime", runtime);
		cv.put("rated", rated);
		cv.put("awards", awards);
		cv.put("metascore", metascore);
		cv.put("imdbRating", imdbRating);
		cv.put("imdbVotes", imdbVotes);
		cv.put("rating", rating);
		cv.put("tags", TextUtils.join(",", tags));
		cv.put("subtitles", TextUtils.join(",", subtitles));
		cv.put("watchlist", watchlist);
		cv.put("collected", collected);
		cv.put("watched", watched);
		timestamp = System.currentTimeMillis();
		cv.put("timestamp", timestamp);
		
		if (isnew) {
			cv.put("imdb_id", imdb_id);
			session.getDB().insertOrThrow(TABLE, null, cv);
		} else
			session.getDB().update(TABLE, cv, "imdb_id=?", new String[] { imdb_id });
	}
	
	protected void delete() {
		Log.v(TAG, "Deleting movie " + imdb_id);
		dispatch(OnTitleListener.WORKING, null);
		session.getDB().delete(TABLE, "imdb_id=?", new String[] { imdb_id });
		dispatch(OnTitleListener.READY, null);
	}
	
	public void refresh() {
		Log.v(TAG, "Refreshing movie " + imdb_id);
		dispatch(OnTitleListener.WORKING, null);
		Callback<String> callback = new Callback<String>() {
			@Override
			public void success(String result, Response response) {
				Document doc = Commons.XML.str2xml(result);
				load((Element) doc.getElementsByTagName("movie").item(0));
				commit();
				dispatch(OnTitleListener.READY, null);
			}
			@Override
			public void failure(RetrofitError error) {
				Log.e(TAG, "refresh", error);
				
				plot = error.getLocalizedMessage();
				
				dispatch(OnTitleListener.ERROR, error);
			}
		};
		OMDB.getInstance().getMovie(imdb_id, callback);
	}
	
	public final void commit() {
		if (!modified)
			return;
		dispatch(OnTitleListener.WORKING, null);
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
		dispatch(OnTitleListener.READY, null);
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return timestamp > 0 && (System.currentTimeMillis() - timestamp) > Commons.weekLong; // TODO preferences
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			modified = true;
			if (isNew())
				refresh();
			else
				commit();
			String msg = session.getRes().getString(watchlist ? R.string.msg_wlst_add_mov : R.string.msg_wlst_del_mov);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public boolean inCollection() {
		return collected;
	}
	
	public void setCollected(boolean value) {
		if (value != collected) {
			collected = value;
			modified = true;
			if (isNew())
				refresh();
			else
				commit();
			String msg = session.getRes().getString(collected ? R.string.msg_coll_add_mov : R.string.msg_coll_del_mov);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public boolean isWatched() {
		return watched;
	}
	
	public void setWatched(boolean value) {
		if (value != watched) {
			watched = value;
			modified = true;
			if (isNew())
				refresh();
			else
				commit();
			String msg = session.getRes().getString(watched ? R.string.msg_seen_add_mov : R.string.msg_seen_del_mov);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public int getRating() {
		return rating;
	}
	
	public void setRating(int value) {
		if (value != rating) {
			rating = value;
			modified = true;
			if (isNew())
				refresh();
			else
				commit();
		}
	}
	
	public List<String> getTags() {
		return new ArrayList<String>(tags);
	}
	
	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}
	
	public void addTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (!hasTag(tag)) {
			tags.add(tag);
			modified = true;
			if (isNew())
				refresh();
			else
				commit();
			String msg = String.format(session.getRes().getString(R.string.msg_tags_add), name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void delTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (hasTag(tag)) {
			tags.remove(tag);
			modified = true;
			if (isNew())
				refresh();
			else
				commit();
			String msg = String.format(session.getRes().getString(R.string.msg_tags_del), name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public String imdbUrl() {
		return "http://www.imdb.com/title/" + imdb_id;
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
	}
}