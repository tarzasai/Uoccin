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
import net.ggelardi.uoccin.api.OMDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Movie extends Title {

    public String imdb_id;
    public String language;
    public String director;
    public String writers;
    public String country;
    public long released;
    public int runtime;
    public String awards;
    public int metascore;
    public int tomatoMeter;
    public double imdbRating;
    public int imdbVotes;
    public boolean collected = false;
    public boolean watched = false;
    public List<String> subtitles = new ArrayList<>();
    public List<String> people = new ArrayList<>();

    public static void broadcast(Context context, String movie, boolean metadata) {
        Intent intent = new Intent(Commons.UE.MOVIE).putExtra("metadata", metadata);
        if (movie != null)
            intent.putExtra("movie", movie);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public Movie(Context context, String imdb_id) {
        super(context);

        this.imdb_id = imdb_id;
    }

    public long age() {
        return System.currentTimeMillis() - timestamp;
    }

    public boolean isNew() {
        return timestamp <= 0;
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
        Log.v(tag(), "Loading movie " + imdb_id);
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

        colIdx = cursor.getColumnIndex("released");
        if (!cursor.isNull(colIdx))
            released = cursor.getLong(colIdx);

        colIdx = cursor.getColumnIndex("runtime");
        if (!cursor.isNull(colIdx))
            runtime = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("rated");
        if (!cursor.isNull(colIdx))
            rated = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("awards");
        if (!cursor.isNull(colIdx))
            awards = cursor.getString(colIdx);

        colIdx = cursor.getColumnIndex("metascore");
        if (!cursor.isNull(colIdx))
            metascore = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("tomatoMeter");
        if (!cursor.isNull(colIdx))
            tomatoMeter = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("imdbRating");
        if (!cursor.isNull(colIdx))
            imdbRating = cursor.getDouble(colIdx);

        colIdx = cursor.getColumnIndex("imdbVotes");
        if (!cursor.isNull(colIdx))
            imdbVotes = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("rating");
        if (!cursor.isNull(colIdx))
            rating = cursor.getInt(colIdx);

        colIdx = cursor.getColumnIndex("subtitles");
        if (!cursor.isNull(colIdx))
            subtitles = new ArrayList<>(Arrays.asList(cursor.getString(colIdx).split(",")));

        tags.clear();
        Cursor ct = session.getDB().query("movtag", new String[]{"tag"},
                "movie = ?", new String[]{imdb_id}, null, null, "tag");
        try {
            while (ct.moveToNext())
                tags.add(ct.getString(0));
        } finally {
            ct.close();
        }

        //dispatch(OnTitleListener.READY, null);
        Log.d(tag(), "Loaded movie " + imdb_id);

        return this;
    }

    public Movie load() {
        Cursor cur = session.getDB().query("movie", null, "imdb_id = ?", new String[]{imdb_id}, null, null, null);
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
        Log.v(tag(), "Saving movie " + imdb_id);
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

        if (!TextUtils.isEmpty(awards))
            cv.put("awards", awards);
        else
            cv.putNull("awards");

        if (metascore > 0)
            cv.put("metascore", metascore);
        else
            cv.putNull("metascore");

        if (tomatoMeter > 0)
            cv.put("tomatoMeter", tomatoMeter);
        else
            cv.putNull("tomatoMeter");

        if (imdbRating > 0)
            cv.put("imdbRating", imdbRating);
        else
            cv.putNull("imdbRating");

        if (imdbVotes > 0)
            cv.put("imdbVotes", imdbVotes);
        else
            cv.putNull("imdbVotes");

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
            db.update("movie", cv, "imdb_id = ?", new String[]{imdb_id});
        else {
            cv.put("imdb_id", imdb_id);
            cv.put("createtime", System.currentTimeMillis());
            db.insertOrThrow("movie", null, cv);
        }
        db.delete("movtag", "movie = ?", new String[]{imdb_id});
        for (String tag : tags) {
            cv = new ContentValues();
            cv.put("movie", imdb_id);
            cv.put("tag", tag.trim());
            db.insertOrThrow("movtag", null, cv);
        }

        broadcast(session.getContext(), imdb_id, metadata);

        Log.d(tag(), "Saved movie " + imdb_id);

        return this;
    }

    public Movie update(OMDB.MovieData data) {
        if (!TextUtils.isEmpty(data.Title)) {
            name = data.Title;
        }
        if (!TextUtils.isEmpty(data.Year)) {
            try {
                int r = Integer.parseInt(data.Year);
                if (r > 0 && r != year)
                    year = r;
            } catch (Exception err) {
                Log.e(tag(), data.Year, err);
            }
        }
        if (!TextUtils.isEmpty(data.Plot)) {
            plot = data.Plot;
        }
        if (!TextUtils.isEmpty(data.Poster)) {
            poster = data.Poster;
        }
        if (!TextUtils.isEmpty(data.Genre)) {
            genres = new ArrayList<>(Arrays.asList(data.Genre.split(", ")));
        }
        if (!TextUtils.isEmpty(data.Language)) {
            language = data.Language;
        }
        if (!TextUtils.isEmpty(data.Director)) {
            director = data.Director;
        }
        if (!TextUtils.isEmpty(data.Writer)) {
            writers = data.Writer;
        }
        if (!TextUtils.isEmpty(data.Actors)) {
            actors = data.Actors;
        }
        if (!TextUtils.isEmpty(data.Country)) {
            country = data.Country;
        }
        if (!TextUtils.isEmpty(data.Released)) {
            try {
                long t = Commons.SDF.eng("dd MMM yyyy").parse(data.Released).getTime();
                if (t > 0)
                    released = t;
            } catch (Exception err) {
                Log.e(tag(), data.Released, err);
            }
        }
        if (!TextUtils.isEmpty(data.Runtime)) {
            String chk = data.Runtime;
            if (chk.contains(" min"))
                chk = chk.split(" ")[0];
            try {
                int r = NumberFormat.getInstance(Locale.ENGLISH).parse(chk).intValue();
                if (r > 0 && r != runtime)
                    runtime = r;
            } catch (Exception err) {
                Log.e(tag(), data.Runtime, err);
            }
        }
        if (!TextUtils.isEmpty(data.Rated)) {
            rated = data.Rated;
        }
        if (!TextUtils.isEmpty(data.Awards)) {
            awards = data.Awards;
            if (awards != null && awards.equals("N/A"))
                awards = null;
        }
        if (!TextUtils.isEmpty(data.Metascore)) {
            try {
                int r = NumberFormat.getInstance(Locale.ENGLISH).parse(data.Metascore).intValue();
                if (r > 0 && r != metascore)
                    metascore = r;
            } catch (Exception err) {
                Log.e(tag(), data.Metascore, err);
            }
        }
        if (!TextUtils.isEmpty(data.tomatoMeter) && !data.tomatoMeter.equals("N/A")) {
            try {
                int r = NumberFormat.getInstance(Locale.ENGLISH).parse(data.tomatoMeter).intValue();
                if (r > 0 && r != tomatoMeter)
                    tomatoMeter = r;
            } catch (Exception err) {
                Log.e(tag(), data.tomatoMeter, err);
            }
        }
        if (!TextUtils.isEmpty(data.imdbRating)) {
            try {
                double r = Double.parseDouble(data.imdbRating);
                if (r > 0 && r != imdbRating)
                    imdbRating = r;
            } catch (Exception err) {
                Log.e(tag(), data.imdbRating, err);
            }
        }
        if (!TextUtils.isEmpty(data.imdbVotes)) {
            try {
                int r = NumberFormat.getInstance(Locale.ENGLISH).parse(data.imdbVotes).intValue();
                if (r > 0 && r != imdbVotes)
                    imdbVotes = r;
            } catch (Exception err) {
                Log.e(tag(), data.imdbVotes, err);
            }
        }
        return this;
    }

    public void refresh(boolean force) {
        session.getContext().startService(new Intent(session.getContext(), Service.class)
                .setAction(Commons.SR.REFRESH_MOVIE).putExtra("imdb_id", imdb_id).putExtra("forced", force));
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
