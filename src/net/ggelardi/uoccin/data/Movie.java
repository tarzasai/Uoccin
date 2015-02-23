package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.ggelardi.uoccin.api.OMDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

public class Movie {
	private static final String LOGTAG = "Movie";
	private static final String TABLE = "movie";
	
	private static final Set<Movie> cache = Collections.newSetFromMap(new WeakHashMap<Movie, Boolean>());
	
	private static final List<OnTitleListener> listeners = new ArrayList<OnTitleListener>();
	
	private final Session session;

	private int rating = 0;
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
	
	private static void dispatch(String state, Throwable error) {
		for (OnTitleListener listener: listeners)
			//if (listener != null)
				listener.changed(state, error);
	}
	
	private static synchronized Movie getInstance(Context context, String imdb_id) {
		for (Movie m: cache)
			if (m.imdb_id.equals(imdb_id)) {
				Log.v(LOGTAG, imdb_id + " found in cache");
				return m;
			}
		Movie res = new Movie(context, imdb_id);
		cache.add(res);
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
		OMDB.Data lst = OMDB.getInstance().findMovie(text);
		if (lst.response)
			for (OMDB.Movie movie: lst.movies()) {
				// since OMDB search returns id and title only, we'll use the factory method that retrieves full data
				Movie itm = Movie.get(context, movie.imdbID);
				itm.name = movie.Title;
				res.add(itm);
			}
		else
			dispatch(OnTitleListener.NOTFOUND, null);
		return res;
	}
	
	public static void addOnTitleEventListener(OnTitleListener aListener) {
		listeners.add(aListener);
	}
	
	private void updateFromOMDB(OMDB.Movie data) {
		if (!TextUtils.isEmpty(data.title) && (TextUtils.isEmpty(name) || !name.equals(data.title))) {
			name = data.title;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.plot) && (TextUtils.isEmpty(plot) || !plot.equals(data.plot))) {
			plot = data.plot;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.actors)) {
			List<String> chk = Arrays.asList(data.actors.split(",\\s"));
			if (!Commons.sameStringLists(actors, chk)) {
				actors = new ArrayList<String>(chk);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.poster) && (TextUtils.isEmpty(poster) || !poster.equals(data.poster))) {
			poster = data.poster;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.runtime)) {
			int chk = Commons.str2int(data.runtime, 0);
			if (chk > 0) {
				runtime = chk;
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.language) && (TextUtils.isEmpty(language) || !language.equals(data.language))) {
			language = data.language;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.year)) {
			int chk = Commons.str2int(data.year, 0);
			if (chk > 0) {
				year = chk;
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.rated) && (TextUtils.isEmpty(rated) || !rated.equals(data.rated))) {
			rated = data.rated;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.genre)) {
			List<String> chk = Arrays.asList(data.genre.split(","));
			if (!Commons.sameStringLists(genres, chk)) {
				genres = new ArrayList<String>(chk);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.director) && (TextUtils.isEmpty(director) || !director.equals(data.director))) {
			director = data.director;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.writer)) {
			List<String> chk = Arrays.asList(data.writer.split(","));
			if (!Commons.sameStringLists(writers, chk)) {
				writers = new ArrayList<String>(chk);
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.country) && (TextUtils.isEmpty(country) || !country.equals(data.country))) {
			country = data.country;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.released)) {
			long chk;
			try {
				chk = Commons.DateStuff.english("dd MMM yyyy").parse(data.released).getTime();
			} catch (Exception err) {
				Log.e("updateFromOMDB", data.released, err);
				chk = 0;
			}
			if (chk > 0) {
				released = chk;
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.awards) && (TextUtils.isEmpty(awards) || !awards.equals(data.awards))) {
			awards = data.awards;
			modified = true;
		}
		if (!TextUtils.isEmpty(data.metascore)) {
			int chk = Commons.str2int(data.metascore, 0);
			if (chk > 0) {
				metascore = chk;
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.imdbRating)) {
			double chk = Commons.str2num(data.imdbRating, 0);
			if (chk > 0) {
				imdbRating = chk;
				modified = true;
			}
		}
		if (!TextUtils.isEmpty(data.imdbVotes)) {
			int chk = Commons.str2int(data.imdbVotes, 0);
			if (chk > 0) {
				imdbVotes = chk;
				modified = true;
			}
		}
	}
	
	/*
	protected void load(Object source) {
		updateFromOMDB((OMDB.Movie) source);
	}
	*/
	
	protected void load(Cursor cr) {
		Log.v(LOGTAG, "Loading movie " + imdb_id);
		
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
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
	}
	
	protected void save(boolean isnew) {
		Log.v(LOGTAG, "Saving movie " + imdb_id);
		
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
		Log.v(LOGTAG, "Deleting movie " + imdb_id);
		
		session.getDB().delete(TABLE, "imdb_id=?", new String[] { imdb_id });
	}
	
	public void refresh() {
		Log.v(LOGTAG, "Refreshing movie " + imdb_id);
		dispatch(OnTitleListener.LOADING, null);
		Callback<OMDB.Data> callback = new Callback<OMDB.Data>() {
			@Override
			public void success(OMDB.Data result, Response response) {
				updateFromOMDB(result.movies().get(0));
				commit();
				dispatch(OnTitleListener.READY, null);
			}
			@Override
			public void failure(RetrofitError error) {
				Log.e(LOGTAG, "refresh", error);
				
				plot = error.getLocalizedMessage();
				
				dispatch(OnTitleListener.ERROR, error);
			}
		};
		OMDB.getInstance().getMovie(imdb_id, callback);
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
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 24; // TODO preferences
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
	
	public int getRating() {
		return rating;
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
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
	
	public void setRating(int value) {
		if (value != rating) {
			rating = value;
			commit();
		}
	}
}