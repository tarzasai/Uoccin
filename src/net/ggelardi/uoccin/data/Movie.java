package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.ggelardi.uoccin.api.OMDB;
import net.ggelardi.uoccin.serv.Commons;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

public class Movie extends Title {

	public static Movie get(Context context, String imdb_id) {
		return (Movie) Title.get(context, Movie.class, imdb_id, MOVIE, null);
	}

	public static Movie get(Context context, OMDB.Movie source) {
		return (Movie) Title.get(context, Movie.class, source.imdb_id, MOVIE, source);
	}
	
	public static List<Movie> get(Context context, List<String> imdb_ids) {
		List<Movie> res = new ArrayList<Movie>();
		for (String mid: imdb_ids)
			res.add(Movie.get(context, mid));
		return res;
	}
	
	/**
	 * BEWARE!!! SYNCHRONOUS RPC
	 */
	public static List<Movie> find(Context context, String text) {
		List<Movie> res = new ArrayList<Movie>();
		OMDB.Search lst = OMDB.getInstance().findMovie(text);
		for (OMDB.Movie movie: lst.results) {
			Movie itm = Movie.get(context, movie.imdb_id);
			itm.name = movie.title;
			//itm.year = movie.year;
			res.add(itm);
		}
		return res;
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
	public List<String> subtitles = new ArrayList<String>();
	
	public Movie(Context context, String imdb_id) {
		super(context, imdb_id);
		
		type = MOVIE;
	}
	
	private void updateFromOMDB(OMDB.Movie data) {
		// title
		if (data.title != null && !data.title.trim().equals("") && !name.equals(data.title)) {
			name = data.title;
			modified = true;
		}
		if (data.plot != null && !data.plot.trim().equals("") && !plot.equals(data.plot)) {
			plot = data.plot;
			modified = true;
		}
		if (data.actors != null && !data.actors.isEmpty() && !Commons.sameSimStrLsts(actors, data.actors)) {
			actors = new ArrayList<String>(data.actors);
			modified = true;
		}
		if (data.poster != null && !data.poster.trim().equals("") && !poster.equals(data.poster)) {
			poster = data.poster;
			modified = true;
		}
		if (data.runtime > 0 && runtime != data.runtime) {
			runtime = data.runtime;
			modified = true;
		}
		// movie
		if (data.language != null && !data.language.trim().equals("") && !language.equals(data.language)) {
			language = data.language;
			modified = true;
		}
		if (data.year > 0 && year != data.year) {
			year = data.year;
			modified = true;
		}
		if (data.rated != null && !data.rated.trim().equals("") && !rated.equals(data.rated)) {
			rated = data.rated;
			modified = true;
		}
		if (data.genres != null && !data.genres.isEmpty() && !Commons.sameSimStrLsts(genres, data.genres)) {
			genres = new ArrayList<String>(data.genres);
			modified = true;
		}
		if (data.director != null && !data.director.trim().equals("") && !director.equals(data.director)) {
			director = data.director;
			modified = true;
		}
		if (data.writers != null && !data.writers.isEmpty() && !Commons.sameSimStrLsts(writers, data.writers)) {
			writers = new ArrayList<String>(data.writers);
			modified = true;
		}
		if (data.country != null && !data.country.trim().equals("") && !country.equals(data.country)) {
			country = data.country;
			modified = true;
		}
		if (data.released != null && released != data.released.getTime()) {
			released = data.released.getTime();
			modified = true;
		}
		if (data.awards != null && !data.awards.trim().equals("") && !awards.equals(data.awards)) {
			awards = data.awards;
			modified = true;
		}
		if (data.metascore > 0 && metascore != data.metascore) {
			metascore = data.metascore;
			modified = true;
		}
		if (data.imdbRating > 0 && imdbRating != data.imdbRating) {
			imdbRating = data.imdbRating;
			modified = true;
		}
		if (data.imdbVotes > 0 && imdbVotes != data.imdbVotes) {
			imdbVotes = data.imdbVotes;
			modified = true;
		}
	}

	@Override
	protected void load(Object source) {
		updateFromOMDB((OMDB.Movie) source);
	}
	
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
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
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
		cv.put("subtitles", TextUtils.join(",", subtitles));
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
	protected void delete() {
		session.getDB().delete(MOVIE, "imdb_id=?", new String[] { imdb_id });
		
		super.delete();
	}
	
	@Override
	public void refresh() {
		dispatch(OnTitleEventListener.LOADING);
		Callback<OMDB.Movie> callback = new Callback<OMDB.Movie>() {
			@Override
			public void success(OMDB.Movie result, Response response) {
				updateFromOMDB(result);
				commit();
				dispatch(OnTitleEventListener.READY);
			}
			@Override
			public void failure(RetrofitError error) {
				// TODO Auto-generated method stub
				
				dispatch(OnTitleEventListener.ERROR);
			}
		};
		OMDB.getInstance().getMovie(imdb_id, callback);
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
	}
}