package net.ggelardi.uoccin.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.TMDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Movie extends Title {

    public int tmdb_id = 0;
    public String imdb_id;
    public String language;
    public String director;
    public String writers;
    public String country;
    public long released;
    public int runtime;
    public double tmdbRating;
    public int tmdbVotes;
    public boolean collected = false;
    public boolean watched = false;
    public List<String> subtitles = new ArrayList<>();
    public List<String> people = new ArrayList<>();

    public static void broadcast(Context context, Integer movie, boolean metadata) {
        Intent intent = new Intent(Commons.UE.MOVIE).putExtra("metadata", metadata);
        if (movie != null)
            intent.putExtra("movie", movie);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public Movie(Context context, int tmdb_id) {
        super(context);

        this.tmdb_id = tmdb_id;
    }

    public Movie(Context context, String imdb_id) {
        super(context);

        this.imdb_id = imdb_id;

        Cursor cur = session.getDB().query("movie", null, "imdb_id = ?", new String[]{imdb_id}, null, null, null);
        try {
            if (cur.moveToFirst()) {
                this.tmdb_id = cur.getInt(cur.getColumnIndex("tmdb_id"));
                load(cur);
                loaded = true;
            }
        } finally {
            cur.close();
        }
    }

    public long age() {
        return System.currentTimeMillis() - timestamp;
    }

    public boolean isNew() {
        return timestamp <= 0 || imdb_id == null || tmdb_id <= 0;
    }

    @Override
    public boolean isRecent() {
        return released <= 0 || !Commons.olderThan(released, Commons.days(31));
    }

    public boolean isOld() {
        if (timestamp <= 0)
            return false;
        if (timestamp == 1)
            return true;
        if (!Commons.olderThan(released, Commons.days(30)))
            return Commons.olderThan(timestamp, Commons.days(7));
        if (Commons.olderThan(released, Commons.days(365)))
            return Commons.olderThan(timestamp, Commons.days(90));
        return Commons.olderThan(timestamp, Commons.days(30));
    }

    public boolean isLoaded() {
        return loaded;
    }

    public Movie load(Cursor cursor) {
        Log.v(tag(), "Loading movie " + Integer.toString(tmdb_id));
        //dispatch(OnTitleListener.WORKING, null);

        name = cursor.getString(cursor.getColumnIndex("name"));
        language = cursor.getString(cursor.getColumnIndex("language"));
        watchlist = cursor.getInt(cursor.getColumnIndex("watchlist")) == 1;
        collected = cursor.getInt(cursor.getColumnIndex("collected")) == 1;
        watched = cursor.getInt(cursor.getColumnIndex("watched")) == 1;
        timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
        people.clear();

        int colIdx;

        colIdx = cursor.getColumnIndex("year");
        if (!cursor.isNull(colIdx))
            year = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("plot");
        if (!cursor.isNull(colIdx))
            plot = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("poster");
        if (!cursor.isNull(colIdx))
            poster = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("genres");
        if (!cursor.isNull(colIdx))
            genres = new ArrayList<>(Arrays.asList(cursor.getString(colIdx).split(",")));

        colIdx = cursor.getColumnIndex("director");
        if (!cursor.isNull(colIdx))
            director = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("writers");
        if (!cursor.isNull(colIdx))
            writers = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("actors");
        if (!cursor.isNull(colIdx))
            actors = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("country");
        if (!cursor.isNull(colIdx))
            country = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("imdb_id");
        if (!cursor.isNull(colIdx))
            imdb_id = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("released");
        if (!cursor.isNull(colIdx))
            released = cursor.getLong(colIdx);

        colIdx = cursor.getColumnIndex("runtime");
        if (!cursor.isNull(colIdx))
            runtime = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("rated");
        if (!cursor.isNull(colIdx))
            rated = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("siteRating");
        if (!cursor.isNull(colIdx))
            tmdbRating = cursor.getDouble(colIdx);

        colIdx = cursor.getColumnIndex("siteVotes");
        if (!cursor.isNull(colIdx))
            tmdbVotes = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("rating");
        if (!cursor.isNull(colIdx))
            rating = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("subtitles");
        if (!cursor.isNull(colIdx))
            subtitles = new ArrayList<>(Arrays.asList(cursor.getString(colIdx).split(",")));

        tags.clear();
        Cursor ct = session.getDB().query("movtag", new String[]{"tag"}, "movie = ?",
                new String[]{Integer.toString(tmdb_id)}, null, null, "tag");
        try {
            while (ct.moveToNext())
                tags.add(ct.getString(0));
        } finally {
            ct.close();
        }

        //dispatch(OnTitleListener.READY, null);
        Log.d(tag(), "Loaded movie " + Integer.toString(tmdb_id));

        return this;
    }

    public Movie load() {
        if (tmdb_id <= 0)
            return this;
        Cursor cur = session.getDB().query("movie", null, "tmdb_id = ?", new String[]{Integer.toString(tmdb_id)},
                null, null, null);
        try {
            if (cur.moveToFirst()) {
                load(cur);
                loaded = true;
            }
        } finally {
            cur.close();
        }
        return this;
    }

    public Movie save(boolean metadata) {
        Log.v(tag(), "Saving movie " + Integer.toString(tmdb_id));
        //dispatch(OnTitleListener.WORKING, null);

        ContentValues cv = new ContentValues();

        cv.put("name", name);
        cv.put("watchlist", watchlist);
        cv.put("collected", collected);
        cv.put("watched", watched);

        if (year > 0)
            cv.put("year", year);
        else
            cv.putNull("year");

        if (!TextUtils.isEmpty(plot))
            cv.put("plot", plot);
        else
            cv.putNull("plot");

        if (!TextUtils.isEmpty(poster))
            cv.put("poster", poster);
        else
            cv.putNull("poster");

        if (!genres.isEmpty())
            cv.put("genres", TextUtils.join(",", genres));
        else
            cv.putNull("genres");

        if (!TextUtils.isEmpty(language))
            cv.put("language", language);
        else
            cv.putNull("language");

        if (!TextUtils.isEmpty(director))
            cv.put("director", director);
        else
            cv.putNull("director");

        if (!TextUtils.isEmpty(writers))
            cv.put("writers", writers);
        else
            cv.putNull("writers");

        if (!TextUtils.isEmpty(actors))
            cv.put("actors", actors);
        else
            cv.putNull("actors");

        if (!TextUtils.isEmpty(imdb_id))
            cv.put("imdb_id", imdb_id);
        else
            cv.putNull("imdb_id");

        if (!TextUtils.isEmpty(country))
            cv.put("country", country);
        else
            cv.putNull("country");

        if (released > 0)
            cv.put("released", released);
        else
            cv.putNull("released");

        if (runtime > 0)
            cv.put("runtime", runtime);
        else
            cv.putNull("runtime");

        if (!TextUtils.isEmpty(rated))
            cv.put("rated", rated);
        else
            cv.putNull("rated");

        if (tmdbRating > 0)
            cv.put("siteRating", tmdbRating);
        else
            cv.putNull("siteRating");

        if (tmdbVotes > 0)
            cv.put("siteVotes", tmdbVotes);
        else
            cv.putNull("siteVotes");

        if (rating > 0)
            cv.put("rating", rating);
        else
            cv.putNull("rating");

        if (!subtitles.isEmpty())
            cv.put("subtitles", TextUtils.join(",", subtitles));
        else
            cv.putNull("subtitles");

        boolean exists = timestamp > 0;
        if (metadata) {
            timestamp = System.currentTimeMillis();
            cv.put("timestamp", timestamp);
        } else if (!exists) {
            timestamp = 1;
            cv.put("timestamp", timestamp);
        }

        SQLiteDatabase db = session.getDB();
        if (exists)
            db.update("movie", cv, "tmdb_id = ?", new String[]{Integer.toString(tmdb_id)});
        else {
            cv.put("tmdb_id", tmdb_id);
            cv.put("createtime", System.currentTimeMillis());
            db.insertOrThrow("movie", null, cv);
        }
        db.delete("movtag", "movie = ?", new String[]{Integer.toString(tmdb_id)});
        for (String tag : tags) {
            cv = new ContentValues();
            cv.put("movie", tmdb_id);
            cv.put("tag", tag.trim());
            db.insertOrThrow("movtag", null, cv);
        }

        broadcast(session.getContext(), tmdb_id, metadata);

        Log.d(tag(), "Saved movie " + Integer.toString(tmdb_id));

        return this;
    }

    public Movie update(TMDB.MovieData data, TMDB.TMDBPeople people) {
        if (!TextUtils.isEmpty(data.title)) {
            name = data.title;
        }
        if (!TextUtils.isEmpty(data.overview)) {
            plot = data.overview;
        }
        if (!TextUtils.isEmpty(data.poster_path)) {
            poster = "https://image.tmdb.org/t/p/w640" + data.poster_path;
        }
        if (data.genres != null && data.genres.length > 0) {
            List<String> tmp = new ArrayList<>();
            for (TMDB.MovieData.GenreData c: data.genres)
                tmp.add(c.name);
            genres = tmp;
        }
        if (data.spoken_languages != null && data.spoken_languages.length > 0) {
            List<String> tmp = new ArrayList<>();
            for (TMDB.MovieData.Iso31661 c: data.spoken_languages)
                tmp.add(c.name);
            language = TextUtils.join(", ", tmp);
        } else if (!TextUtils.isEmpty(data.original_language)) {
            language = data.original_language;
        }
        if (!TextUtils.isEmpty(data.release_date))
            try {
                released = Commons.SDF.eng("yyyy-MM-dd").parse(data.release_date).getTime();
                year = Commons.getDatePart(released, Calendar.YEAR);
            } catch (Exception err) {
                Log.e(tag(), data.release_date, err);
            }
        if (data.runtime != null && data.runtime > 0) {
            runtime = data.runtime;
        }
        if (data.vote_average != null && data.vote_average > 0) {
            tmdbRating = data.vote_average;
        }
        if (data.vote_count != null && data.vote_count > 0) {
            tmdbVotes = data.vote_count;
        }
        if (data.production_countries != null && data.production_countries.length > 0) {
            List<String> tmp = new ArrayList<>();
            for (TMDB.MovieData.Iso31661 c: data.production_countries)
                tmp.add(c.iso_3166_1);
            country = TextUtils.join("-", tmp);
        }
        if (people != null) {
            // cast
            List<String> act = new ArrayList<>();
            for (TMDB.TMDBPeople.PeopleData guy: people.cast)
                if (guy.gender > 0 && guy.profile_path != null && !guy.character.contains("uncredited"))
                    act.add(guy.name);
            this.actors = TextUtils.join(",", act);
            // crew
            List<String> dir = new ArrayList<>();
            List<String> wrt = new ArrayList<>();
            for (TMDB.TMDBPeople.PeopleData guy: people.crew)
                if (guy.job.equals("Director"))
                    dir.add(guy.name);
                else if (guy.job.equals("Writer"))
                    wrt.add(guy.name);
            this.director = TextUtils.join(", ", dir);
            this.writers = TextUtils.join(", ", wrt);
        }
        return this;
    }

    public void refresh(boolean force) {
        session.getContext().startService(new Intent(session.getContext(), Service.class)
                .setAction(Commons.SR.REFRESH_MOVIE).putExtra("tmdb_id", tmdb_id).putExtra("forced", force));
    }

    @Override
    public String people() {
        return null; //TODO
    }

    public String imdbUrl() {
        return "http://www.imdb.com/title/" + imdb_id;
    }

    public String runtime() {
        if (runtime <= 0)
            return "N/A";
        return session.getString(R.string.movact_fmt_runtime, runtime);
    }

    public String language() {
        if (language == null)
            return "N/A";
        return language;
    }

    public String subtitles() {
        return subtitles.isEmpty() ? null : TextUtils.join(", ", subtitles);
    }

    public void setWatchlist(boolean value) {
        if (value != watchlist) {
            watchlist = value;
            save(false);
            session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "watchlist", Boolean.toString(watchlist));
        }
    }

    public void setCollected(boolean value) {
        if (value != collected) {
            collected = value;
            if (!collected)
                subtitles.clear();
            save(false);
            session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "collected", Boolean.toString(collected));
        }
    }

    public void setWatched(boolean value) {
        if (value != watched) {
            watched = value;
            save(false);
            session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "watched", Boolean.toString(watched));
        }
    }

    public void setRating(int value) {
        if (value != rating) {
            rating = value;
            save(false);
            session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "rating", Integer.toString(rating));
            if (!watched)
                setWatched(true);
        }
    }

    @Override
    public void setTags(String[] values) {
        tags = new ArrayList<>(Arrays.asList(values));
        save(false);
        session.driveQueue(Session.QUEUE_MOVIE, imdb_id, "tags", TextUtils.join(",", tags));
    }
}
