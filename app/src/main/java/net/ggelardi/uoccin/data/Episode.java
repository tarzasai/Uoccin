package net.ggelardi.uoccin.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import net.ggelardi.uoccin.api.TVDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Episode {
    private static final String TAG = "Episode";

    private Session session;

    public int series; // series id
    public int tvdb_id; // episode id
    public String imdb_id; // episode id
    public int season;
    public int episode;
    public String name;
    public String plot;
    public String thumb;
    public String director;
    public String writers;
    public String guests;
    public long firstAired;
    public int thumbWidth;
    public int thumbHeight;
    public boolean collected = false;
    public boolean watched = false;
    public List<String> subtitles = new ArrayList<>();
    public List<String> people = new ArrayList<>();
    public long timestamp = 0;

    public static void broadcast(Context context, int seriesId, int episodeId, int seasonNo, int episodeNo, boolean metadata) {
        Intent intent = new Intent(Commons.UE.EPISODE).putExtra("metadata", metadata);
        intent.putExtra("series", seriesId);
        intent.putExtra("episode", episodeId);
        intent.putExtra("seasonNo", seasonNo);
        intent.putExtra("episodeNo", episodeNo);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public Episode(Context context, int tvdb_id) {
        this.session = Session.getInstance(context);
        this.tvdb_id = tvdb_id;
    }

    public Episode(Context context, int series, int season, int episode) {
        this.session = Session.getInstance(context);
        this.series = series;
        this.season = season;
        this.episode = episode;
    }

    public Episode(Context context, EID eid) {
        this(context, eid.series, eid.season, eid.episode);
    }

    public EID eid() {
        return new EID(series, season, episode);
    }

    public long age() {
        return System.currentTimeMillis() - timestamp;
    }

    public boolean isNew() {
        return timestamp <= 0;
    }

    public boolean isOld() {
        if (timestamp <= 0)
            return false;
        String txt = String.format(Locale.getDefault(), " - series %d, season %d, episode %d (%d)",
                series, season, episode, tvdb_id);
        if (timestamp == 1) {
            Log.i(TAG, "isOld(1): timestamp == 1" + txt);
            return true;
        }
        if (TextUtils.isEmpty(thumb) &&
                Commons.newerThan(firstAired, Commons.days(2)) &&
                Commons.olderThan(timestamp, Commons.hours(6))) {
            Log.i(TAG, "isOld(2): thumb == null && timestamp > 6h" + txt);
            return true;
        }
        if (!Commons.olderThan(firstAired, Commons.days(15))) {
            if (Commons.olderThan(timestamp, Commons.days(2))) {
                Log.i(TAG, "isOld(3): firstAired < 15d && timestamp > 2d" + txt);
                return true;
            }
            return false;
        }
        if (Commons.olderThan(timestamp, Commons.days(180))) {
            Log.i(TAG, "isOld(4): timestamp > 180d" + txt);
            return true;
        }
        return false;
    }

    public Episode load(Cursor cursor) {
        Log.v(TAG, "Loading episode " + eid());

        int colIdx;

        colIdx = Commons.getColumnIndex(cursor, "tvdb_id,episode_id");
        tvdb_id = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "series,series_id");
        series = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "season,episode_season");
        season = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "episode,episode_number");
        episode = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "collected,episode_collected");
        collected = cursor.getInt(colIdx) == 1;

        colIdx = Commons.getColumnIndex(cursor, "watched,episode_watched");
        watched = cursor.getInt(colIdx) == 1;

        colIdx = Commons.getColumnIndex(cursor, "timestamp,episode_timestamp");
        timestamp = cursor.getLong(colIdx);
        if (timestamp <= 1)
            Log.i(TAG, "load(): timestamp == " + Long.toString(timestamp) + " !!!!!!!!!!!!!");

        colIdx = Commons.getColumnIndex(cursor, "name,episode_title");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            name = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "plot,episode_plot");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            plot = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "thumb,episode_still");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            thumb = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "thumbWidth,episode_stillW");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            thumbWidth = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "thumbHeight,episode_stillH");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            thumbHeight = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "writers,episode_writers");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            writers = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "director,episode_directors");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            director = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "guestStars,episode_guestStars");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            guests = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "firstAired,episode_firstAired");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            firstAired = cursor.getLong(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "imdb_id,episode_imdb_id");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            imdb_id = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "subtitles,episode_subtitles");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            subtitles = new ArrayList<>(Arrays.asList(cursor.getString(colIdx).split(",")));

        Log.d(TAG, "Loaded episode " + eid());

        return this;
    }

    public Episode load() {
        SQLiteDatabase db = session.getDB();
        Cursor cur = tvdb_id > 0 ?
                db.query("episode", null, "tvdb_id = ?", new String[]{Integer.toString(tvdb_id)}, null, null, null) :
                db.query("episode", null, "series = ? and season = ? and episode = ?", eid().toArray(), null, null, null);
        try {
            if (cur.moveToFirst())
                load(cur);
        } finally {
            cur.close();
        }
        return this;
    }

    public Episode save(boolean metadata) {
        Log.v(TAG, "Saving episode " + eid());
        //dispatch(OnTitleListener.WORKING, null);

        ContentValues cv = new ContentValues();

        cv.put("series", series);
        cv.put("season", season);
        cv.put("episode", episode);
        cv.put("collected", collected);
        cv.put("watched", watched);

        if (!TextUtils.isEmpty(name))
            cv.put("name", name);
        else
            cv.putNull("name");

        if (!TextUtils.isEmpty(plot))
            cv.put("plot", plot);
        else
            cv.putNull("plot");

        if (!TextUtils.isEmpty(thumb))
            cv.put("thumb", thumb);
        else
            cv.putNull("thumb");

        if (thumbWidth > 0)
            cv.put("thumbWidth", thumbWidth);
        else
            cv.putNull("thumbWidth");

        if (thumbHeight > 0)
            cv.put("thumbHeight", thumbHeight);
        else
            cv.putNull("thumbHeight");

        if (!TextUtils.isEmpty(writers))
            cv.put("writers", writers);
        else
            cv.putNull("writers");

        if (!TextUtils.isEmpty(director))
            cv.put("director", director);
        else
            cv.putNull("director");

        if (!TextUtils.isEmpty(guests))
            cv.put("guestStars", guests);
        else
            cv.putNull("guestStars");

        if (firstAired > 0)
            cv.put("firstAired", firstAired);
        else
            cv.putNull("firstAired");

        if (!TextUtils.isEmpty(imdb_id))
            cv.put("imdb_id", imdb_id);
        else
            cv.putNull("imdb_id");

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
        boolean trans = !db.inTransaction();
        if (trans)
            db.beginTransaction();
        try {
            if (!exists) {
                cv.put("tvdb_id", tvdb_id);
                db.insertOrThrow("episode", null, cv);
            } else if (metadata)
                db.update("episode", cv, "tvdb_id = ?", new String[] {Integer.toString(tvdb_id)});
            else
                db.update("episode", cv, "series = ? and season = ? and episode = ?", eid().toArray());
            if (trans)
                db.setTransactionSuccessful();
            Log.d(TAG, "Saved episode " + eid());
            if (!metadata)
                broadcast(session.getContext(), series, tvdb_id, season, episode, metadata);
        } catch (Exception err) {
            Log.e("Episode", "save", err);
            throw err;
        } finally {
            if (trans)
                db.endTransaction();
        }

        return this;
    }

    public Episode update(TVDB.EpisodeData data) {
        tvdb_id = data.id;
        series = data.seriesId;
        season = data.airedSeason;
        episode = data.airedEpisodeNumber;
        if (!TextUtils.isEmpty(data.episodeName)) {
            name = data.episodeName;
        }
        if (!TextUtils.isEmpty(data.overview)) {
            plot = Commons.cleanCRLFs(data.overview);
        }
        if (!TextUtils.isEmpty(data.imdbId)) {
            imdb_id = data.imdbId;
        }
        if (!TextUtils.isEmpty(data.filename)) {
            thumb = "http://thetvdb.com/banners/" + data.filename;
            thumbWidth = data.thumbWidth;
            thumbHeight = data.thumbHeight;
        }
        if (data.directors != null && data.directors.length > 0) {
            director = TextUtils.join(", ", data.directors);
        }
        if (data.writers != null && data.writers.length > 0) {
            writers = TextUtils.join(", ", data.writers);
        }
        if (data.guestStars != null && data.guestStars.length > 0) {
            guests = TextUtils.join(", ", data.guestStars);
        }
        if (!TextUtils.isEmpty(data.firstAired))
            try {
                long t = Commons.SDF.eng("yyyy-MM-dd").parse(data.firstAired).getTime();
                if (t > 0)
                    firstAired = t;
            } catch (Exception err) {
                Log.e(TAG, data.firstAired, err);
            }
        return this;
    }

    public void refresh(boolean force) {
        session.getContext().startService(new Intent(session.getContext(), Service.class)
                .setAction(Commons.SR.REFRESH_EPISODE).putExtra("tvdb_id", tvdb_id).putExtra("forced", force));
    }

    public boolean isPilot() {
        return season == 1 && episode == 1;
    }

    public boolean isSpecial() {
        return season == 0 || episode == 0;
    }

    public boolean isAired() {
        return firstAired > 0 && firstAired <= System.currentTimeMillis();
    }

    public boolean isToday() {
        return firstAired > 0 && DateUtils.isToday(Commons.convertTZ(firstAired, "UTC", TimeZone.getDefault().getID()));
    }

    public String name() {
        return TextUtils.isEmpty(name) ? "N/A" : name;
    }

    public String plot() {
        if (TextUtils.isEmpty(plot))
            return "N/A";
        return plot;
    }

    public String firstAired() {
        if (firstAired <= 0)
            return "N/A";
        Calendar c1 = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c1.setTimeInMillis(firstAired);
        Calendar c2 = Calendar.getInstance();//new GregorianCalendar(TimeZone.getDefault());
        c2.setTimeInMillis(c1.getTimeInMillis());
        long loc = c2.getTimeInMillis();
        long now = Commons.convertTZ(System.currentTimeMillis(), "UTC", TimeZone.getDefault().getID());
        if (isToday())
            return DateUtils.getRelativeTimeSpanString(loc, now, DateUtils.MINUTE_IN_MILLIS).toString();
        String res = DateUtils.getRelativeTimeSpanString(loc, now, DateUtils.DAY_IN_MILLIS).toString();
        if (Math.abs(now - loc)/(1000 * 60 * 60) < 168)
            res += " (" + Commons.SDF.loc("EEE").format(loc) + ")";
        return res;
    }

    public String guests() {
        return TextUtils.isEmpty(guests) ? "N/A" : guests.replace(",", ", ");
    }

    public String writers() {
        return TextUtils.isEmpty(writers) ? "N/A" : writers.replace(",", ", ");
    }

    public String directors() {
        return TextUtils.isEmpty(director) ? "N/A" : director.replace(",", ", ");
    }

    public String subtitles() {
        return subtitles.isEmpty() ? null : TextUtils.join(", ", subtitles);
    }

    public String tvdbUrl() {
        return "http://thetvdb.com/?tab=episode&id=" + tvdb_id;
    }

    public String imdbUrl() {
        if (!TextUtils.isEmpty(imdb_id))
            return "http://www.imdb.com/title/" + imdb_id;
        return null;
    }

    public boolean hasThumb() {
        return !TextUtils.isEmpty(thumb) && thumbWidth > 0 && thumbHeight > 0;
    }

    public void setCollected(boolean value) {
        if (value != collected) {
            collected = value;
            if (!collected)
                subtitles.clear();
            save(false);
            session.driveQueue(Session.QUEUE_SERIES, eid().toString(), "collected", Boolean.toString(collected));
        }
    }

    public void setWatched(boolean value) {
        if (value != watched) {
            watched = value;
            save(false);
            session.driveQueue(Session.QUEUE_SERIES, eid().toString(), "watched", Boolean.toString(watched));
        }
    }

    public static class EID implements Comparable<EID> {
        public final int series;
        public final int season;
        public final int episode;

        public EID(int series, int season, int episode) {
            this.series = series;
            this.season = season;
            this.episode = episode;
        }

        public EID(int season, int episode) {
            this.series = 0;
            this.season = season;
            this.episode = episode;
        }

        public EID(String diffCode) {
            String[] parts = diffCode.split("\\.");
            this.series = Integer.parseInt(parts[0]);
            this.season = Integer.parseInt(parts[1]);
            this.episode = Integer.parseInt(parts[2]);
        }

        public EID(EID clone) {
            this.series = clone.series;
            this.season = clone.season;
            this.episode = clone.episode;
        }

        public EID(Cursor cursor, int colSeries, int colSeason, int colEpisode) {
            this.series = cursor.getInt(colSeries);
            this.season = cursor.getInt(colSeason);
            this.episode = cursor.getInt(colEpisode);
        }

        public boolean isValid(boolean specials) {
            return series > 0 && ((season > 0 && episode > 0) || (specials && season >= 0 && episode >= 0));
        }

        public String sequence() {
            return String.format(Locale.getDefault(), "S%1$02dE%2$02d", season, episode);
        }

        public String readable() {
            return String.format(Locale.getDefault(), "%dx%d", season, episode);
        }

        public String[] toArray() {
            return new String[]{Integer.toString(series), Integer.toString(season), Integer.toString(episode)};
        }

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "%d.%d.%d", series, season, episode);
        }

        @Override
        public boolean equals(Object obj2) {
            EID cmp = obj2 instanceof EID ? (EID) obj2 : obj2 instanceof Episode ? ((Episode) obj2).eid() : null;
            return cmp != null && cmp.series == series && cmp.season == season && cmp.episode == episode;
        }

        @Override
        public int compareTo(EID obj2) {
            int res = series - obj2.series;
            if (res == 0)
                res = season - obj2.season;
            if (res == 0)
                res = episode - obj2.episode;
            return res;
        }
    }
}
