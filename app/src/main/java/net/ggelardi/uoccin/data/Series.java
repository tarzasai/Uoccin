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

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.TVDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class Series extends Title {

    public int tvdb_id;
    public String banner;
    public String imdb_id;
    public String status;
    public String network;
    public long firstAired;
    public int airsDay;
    public String airsTime;
    public int runtime;
    public double tvdbRating;
    public int tvdbVotes;
    public List<String> people = new ArrayList<>();
    public List<FanArt> images = new ArrayList<>();
    public EpisodeList episodes = null;
    public Episode sampleEpisode = null;

    public static void broadcast(Context context, Integer series, boolean metadata) {
        Intent intent = new Intent(Commons.UE.SERIES).putExtra("metadata", metadata);
        if (series != null)
            intent.putExtra("series", series);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public Series(Context context, int tvdb_id) {
        super(context);

        this.tvdb_id = tvdb_id;
        this.banner = "http://thetvdb.com/banners/graphical/" + tvdb_id + "-g.jpg";
        this.poster = "http://thetvdb.com/banners/posters/" + tvdb_id + "-1.jpg";
    }

    public boolean isOld() {
        if (timestamp <= 0)
            return false;
        if (timestamp == 1)
            return true;
        if (isEnded())
            return Commons.olderThan(timestamp, Commons.days(180));
        if (isRecent())
            return Commons.olderThan(timestamp, Commons.days(7));
        return Commons.olderThan(timestamp, Commons.days(14));
    }

    @Override
    public boolean isRecent() {
        return firstAired <= 0 || !Commons.olderThan(firstAired, Commons.days(21));
    }

    public boolean isEnded() {
        return !TextUtils.isEmpty(status) && status.toLowerCase(Locale.getDefault()).equals("ended");
    }

    public Series load(Cursor cursor) {
        Log.v(tag(), "Loading series " + Integer.toString(tvdb_id));

        people.clear();
        int colIdx;

        colIdx = Commons.getColumnIndex(cursor, "name,series_name");
        name = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "watchlist,series_watchlist");
        watchlist = cursor.getInt(colIdx) == 1;

        colIdx = Commons.getColumnIndex(cursor, "timestamp,series_timestamp");
        timestamp = cursor.getLong(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "year,series_year");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            year = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "plot,series_plot");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            plot = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "banner,series_banner");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            banner = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "poster,series_poster");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            poster = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "genres,series_genres");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            genres = new ArrayList<>(Arrays.asList(cursor.getString(colIdx).split(",")));

        colIdx = Commons.getColumnIndex(cursor, "actors,series_actors");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            actors = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "imdb_id,series_imdb_id");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            imdb_id = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "status,series_status");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            status = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "network,series_network");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            network = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "firstAired,series_firstAired");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            firstAired = cursor.getLong(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "airsDay,series_airsDay");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            airsDay = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "airsTime,series_airsTime");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            airsTime = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "runtime,series_runtime");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            runtime = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "rated,series_rated");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            rated = cursor.getString(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "siteRating,series_siteRating");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            tvdbRating = cursor.getDouble(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "siteVotes,series_siteVotes");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            tvdbVotes = cursor.getInt(colIdx);

        colIdx = Commons.getColumnIndex(cursor, "rating,series_rating");
        if (colIdx >= 0 && !cursor.isNull(colIdx))
            rating = cursor.getInt(colIdx);

        tags.clear();
        Cursor ct = session.getDB().query("sertag", new String[]{"tag"},
                "series = ?", new String[]{Integer.toString(tvdb_id)}, null, null, "tag");
        try {
            while (ct.moveToNext())
                tags.add(ct.getString(0));
        } finally {
            ct.close();
        }

        Log.d(tag(), "Loaded series " + Integer.toString(tvdb_id));

        colIdx = Commons.getColumnIndex(cursor, "episode_id");
        if (colIdx >= 0 && !cursor.isNull(colIdx)) {
            sampleEpisode = new Episode(session.getContext(), cursor.getInt(colIdx));
            sampleEpisode.load(cursor);
        }

        return this;
    }

    public Series load(boolean full) {
        String[] sid = new String[]{Integer.toString(tvdb_id)};
        boolean exists = false;

        SQLiteDatabase db = session.getDB();
        Cursor cur = db.query("series", null, "tvdb_id = ?", sid, null, null, null);
        try {
            if (cur.moveToFirst()) {
                load(cur);
                exists = true;
            }
        } finally {
            cur.close();
        }

        if (full && exists) {
            if (episodes == null)
                episodes = new EpisodeList(session.getContext(), this);
            episodes.reload();
            // images
            cur = db.query(false, "serpic", null, "series = ?", sid, null, null, null, null);
            try {
                images.clear();
                while (cur.moveToNext())
                    images.add(new FanArt(cur));
            } finally {
                cur.close();
            }
            loaded = true;
        }
        return this;
    }

    public Series save(boolean metadata) {
        Log.v(tag(), "Saving series " + name);

        ContentValues cv = new ContentValues();
        cv.put("watchlist", watchlist);
        cv.put("rating", rating);

        if (metadata) {
            cv.put("name", name);
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
            if (!TextUtils.isEmpty(actors))
                cv.put("actors", actors);
            else
                cv.putNull("actors");
            if (!TextUtils.isEmpty(imdb_id))
                cv.put("imdb_id", imdb_id);
            else
                cv.putNull("imdb_id");
            if (!TextUtils.isEmpty(status))
                cv.put("status", status);
            else
                cv.putNull("status");
            if (!TextUtils.isEmpty(network))
                cv.put("network", network);
            else
                cv.putNull("network");
            if (firstAired > 0)
                cv.put("firstAired", firstAired);
            else
                cv.putNull("firstAired");
            if (airsDay > 0)
                cv.put("airsDay", airsDay);
            else
                cv.putNull("airsDay");
            if (!TextUtils.isEmpty(airsTime))
                cv.put("airsTime", airsTime);
            else
                cv.putNull("airsTime");
            if (runtime > 0)
                cv.put("runtime", runtime);
            else
                cv.putNull("runtime");
            if (!TextUtils.isEmpty(rated))
                cv.put("rated", rated);
            else
                cv.putNull("rated");
            if (!TextUtils.isEmpty(banner))
                cv.put("banner", banner);
            else
                cv.putNull("banner");
            if (tvdbRating > 0)
                cv.put("siteRating", tvdbRating);
            else
                cv.putNull("siteRating");
            if (tvdbVotes > 0)
                cv.put("siteVotes", tvdbVotes);
            else
                cv.putNull("siteVotes");
        }

        boolean exists = timestamp > 0;
        if (metadata) {
            timestamp = System.currentTimeMillis();
            cv.put("timestamp", timestamp);
        } else if (!exists) {
            timestamp = 1;
            cv.put("timestamp", timestamp);
        }

        SQLiteDatabase db = session.getDB();
        String[] sid = new String[]{Integer.toString(tvdb_id)};
        boolean trans = !db.inTransaction();
        if (trans)
            db.beginTransaction();
        try {
            if (exists)
                db.update("series", cv, "tvdb_id = ?", sid);
            else {
                cv.put("tvdb_id", tvdb_id);
                cv.put("createtime", System.currentTimeMillis());
                db.insertOrThrow("series", null, cv);
            }
            db.delete("sertag", "series = ?", sid);
            for (String tag : tags) {
                cv = new ContentValues();
                cv.put("series", tvdb_id);
                cv.put("tag", tag.trim());
                db.insertOrThrow("sertag", null, cv);
            }
            if (metadata) {
                db.delete("serpic", "series = ?", sid);
                for (FanArt pic : images)
                    if (pic.isValid()) {
                        cv = new ContentValues();
                        cv.put("series", tvdb_id);
                        cv.put("tvdb_id", pic.tvdb_id);
                        cv.put("fullres", pic.fullres);
                        cv.put("thumbnail", pic.thumbnail);
                        cv.put("text", pic.text);
                        cv.put("width", pic.width);
                        cv.put("height", pic.height);
                        db.insertOrThrow("serpic", null, cv);
                    }
            }
            if (trans)
                db.setTransactionSuccessful();
            Log.d(tag(), "Saved series " + name);
            if (!metadata)
                broadcast(session.getContext(), tvdb_id, metadata);
        } catch (Exception err) {
            Log.e(tag(), "save", err);
            throw err;
        } finally {
            if (trans)
                db.endTransaction();
        }

        return this;
    }

    public Series update(TVDB.SeriesData data, TVDB.TVDBImages.ImageData[] fanarts,
                         TVDB.TVDBImages.ImageData[] posters, TVDB.TVDBActors.ActorData[] actors) {
        Log.v(tag(), "Updating series " + Integer.toString(tvdb_id));

        tvdb_id = data.id;
        if (!TextUtils.isEmpty(data.seriesName)) {
            name = data.seriesName;
        }
        if (!TextUtils.isEmpty(data.overview)) {
            plot = Commons.cleanCRLFs(data.overview);
        }
        if (!TextUtils.isEmpty(data.imdbId)) {
            imdb_id = data.imdbId;
        }
        if (!TextUtils.isEmpty(data.status)) {
            status = data.status;
        }
        if (!TextUtils.isEmpty(data.network)) {
            network = data.network;
        }
        if (!TextUtils.isEmpty(data.banner)) {
            banner = "http://thetvdb.com/banners/" + data.banner;
        }
        if (!TextUtils.isEmpty(data.airsDayOfWeek)) {
            int d = Commons.SDF.day(data.airsDayOfWeek);
            if (d > 0)
                airsDay = d;
        }
        if (!TextUtils.isEmpty(data.airsTime)) {
            airsTime = data.airsTime.trim().toUpperCase(Locale.getDefault())
                    .replace("(\\d+)(\\.|:)(\\d\\d)(\\s?(A|P)\\.?(M)\\.?)?", "$1:$3 $5$6");
        }
        if (!TextUtils.isEmpty(data.runtime)) {
            try {
                int r = Integer.parseInt(data.runtime);
                if (r > 0)
                    runtime = r;
            } catch (Exception err) {
                Log.e(tag(), data.runtime, err);
            }
        }
        if (!TextUtils.isEmpty(data.firstAired))
            try {
                firstAired = Commons.SDF.eng("yyyy-MM-dd").parse(data.firstAired).getTime();
                year = Commons.getDatePart(firstAired, Calendar.YEAR);
            } catch (Exception err) {
                Log.e(tag(), data.firstAired, err);
            }
        if (data.siteRating != null && data.siteRating > 0) {
            tvdbRating = data.siteRating;
        }
        if (data.siteRatingCount != null && data.siteRatingCount > 0) {
            tvdbVotes = data.siteRatingCount;
        }
        if (!TextUtils.isEmpty(data.rating)) {
            rated = data.rating;
        }
        if (data.genre != null && data.genre.length > 0) {
            genres = new ArrayList<>(Arrays.asList(data.genre));
        }
        if (posters != null) {
            poster = "http://thetvdb.com/banners/" + posters[posters.length - 1].thumbnail;
        }
        if (fanarts != null) {
            FanArt pic;
            images.clear();
            for (TVDB.TVDBImages.ImageData img : fanarts) {
                pic = new FanArt(img);
                if (pic.isValid())
                    images.add(pic);
            }
        }
        if (actors != null) {
            List<String> tmp = new ArrayList<>();
            for (TVDB.TVDBActors.ActorData act : actors)
                tmp.add(act.name);
            this.actors = TextUtils.join(",", tmp);
        }

        return this;
    }

    public void refresh(boolean force) {
        session.getContext().startService(new Intent(session.getContext(), Service.class)
                .setAction(Commons.SR.REFRESH_SERIES).putExtra("tvdb_id", tvdb_id).putExtra("forced", force));
    }

    public String tvdbUrl() {
        return "http://thetvdb.com/?tab=series&id=" + tvdb_id;
    }

    public String imdbUrl() {
        if (!TextUtils.isEmpty(imdb_id))
            return "http://www.imdb.com/title/" + imdb_id;
        return null;
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
        if (DateUtils.isToday(Commons.convertTZ(firstAired, "UTC", TimeZone.getDefault().getID())))
            return DateUtils.getRelativeTimeSpanString(loc, now, DateUtils.MINUTE_IN_MILLIS).toString();
        String res = DateUtils.getRelativeTimeSpanString(loc, now, DateUtils.DAY_IN_MILLIS).toString();
        if (Math.abs(now - loc) / (1000 * 60 * 60) < 168)
            res += " (" + Commons.SDF.loc("EEE").format(loc) + ")";
        return res;
    }

    public String network() {
        return TextUtils.isEmpty(network) ? "N/A" : network;
    }

    public String airsTime() {
        if (isEnded())
            return session.getString(R.string.seract_lbl_ended);
        if (airsDay <= 0)
            return "N/A";
        String res = Commons.weekdaysShort[airsDay];
        if (!TextUtils.isEmpty(airsTime))
            res += " " + airsTime;
        return res;
    }

    @Override
    public String people() {
        if (people.isEmpty()) {
            people.addAll(Arrays.asList(actors.split(",")));
            //TODO: episode's directors and guests???
            /*
            for (Episode ep: episodes) {
                ep.people();
                people.addAll(ep.people);
            }
            */
            people.removeAll(Arrays.asList("", "N/A", null));
            Set<String> hs = new HashSet<>();
            hs.addAll(people);
            people.clear();
            people.addAll(hs);
        }
        return people.isEmpty() ? "N/A" : TextUtils.join(", ", people);
    }

    public long airedEpisodes(Integer season) {
        int res = 0;
        if (episodes != null)
            for (Episode ep: episodes)
                if ((!ep.isSpecial() || session.specials()) && ep.isAired() && (season == null || ep.season == season))
                    res++;
        return res;
    }

    public long airedEpisodes() {
        return airedEpisodes(null);
    }

    public long watchedEpisodes(Integer season) {
        int res = 0;
        if (episodes != null)
            for (Episode ep: episodes)
                if ((!ep.isSpecial() || session.specials()) && ep.watched && (season == null || ep.season == season))
                    res++;
        return res;
    }

    public long watchedEpisodes() {
        return watchedEpisodes(null);
    }

    public long collectedEpisodes(Integer season) {
        int res = 0;
        if (episodes != null)
            for (Episode ep: episodes)
                if ((!ep.isSpecial() || session.specials()) && ep.collected && (season == null || ep.season == season))
                    res++;
        return res;
    }

    public long collectedEpisodes() {
        return collectedEpisodes(null);
    }

    public long missingEpisodes(Integer season) {
        int res = 0;
        if (episodes != null)
            for (Episode ep: episodes)
                if ((!ep.isSpecial() || session.specials()) && ep.isAired() && !(ep.collected || ep.watched) &&
                        (season == null || ep.season == season))
                    res++;
        return res;
    }

    public long missingEpisodes() {
        return missingEpisodes(null);
    }

    public long awaitingEpisodes(Integer season) {
        int res = 0;
        if (episodes != null)
            for (Episode ep: episodes)
                if ((!ep.isSpecial() || session.specials()) && ep.collected && !ep.watched &&
                        (season == null || ep.season == season))
                    res++;
        return res;
    }

    public long awaitingEpisodes() {
        return awaitingEpisodes(null);
    }

    public FanArt getLastImage() {
        FanArt res = null;
        int i = images.size();
        while (i > 0 && res == null) {
            i--;
            res = images.get(i);
            if (!(res.isValid() && res.text))
                res = null;
        }
        if (res == null)
            res = images.get(images.size() - 1);
        return res;
    }

    public void setWatchlist(boolean value) {
        if (value != watchlist) {
            watchlist = value;
            save(false);
            session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "watchlist", Boolean.toString(watchlist));
        }
    }

    public void setCollected(boolean value, Integer season) {
        for (Episode ep: episodes) // if episodes is null something is wrong
            if ((!ep.isSpecial() || session.specials()) && ep.collected != value &&
                    (season == null || ep.season == season))
                ep.setCollected(value);
    }

    public void setWatched(boolean value, Integer season) {
        for (Episode ep: episodes) // if episodes is null something is wrong
            if ((!ep.isSpecial() || session.specials()) && ep.watched != value &&
                    (season == null || ep.season == season))
                ep.setWatched(value);
    }

    public void setRating(int value) {
        if (value != rating) {
            rating = value;
            save(false);
            session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "rating", Integer.toString(rating));
        }
    }

    @Override
    public void setTags(String[] values) {
        tags = new ArrayList<>(Arrays.asList(values));
        save(false);
        session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "tags", TextUtils.join(",", tags));
    }

    public class FanArt {
        public int tvdb_id;
        public String fullres;
        public String thumbnail;
        public boolean text;
        public int width = 0;
        public int height = 0;

        FanArt(int tvdb_id) {
            this.tvdb_id = tvdb_id;
        }

        FanArt(Cursor c) {
            this(c.getInt(c.getColumnIndex("tvdb_id")));

            fullres = c.getString(c.getColumnIndex("fullres"));
            thumbnail = c.getString(c.getColumnIndex("thumbnail"));
            text = c.getInt(c.getColumnIndex("text")) == 1;
            width = c.getInt(c.getColumnIndex("width"));
            height = c.getInt(c.getColumnIndex("height"));
        }

        FanArt(TVDB.TVDBImages.ImageData timg) {
            this(timg.id);

            fullres = "http://thetvdb.com/banners/" + timg.fileName;
            thumbnail = "http://thetvdb.com/banners/" + timg.thumbnail;
            text = timg.subKey != null && timg.subKey.equals("text");
            try {
                String[] res = timg.resolution.split("x");
                width = Integer.parseInt(res[0]);
                height = Integer.parseInt(res[1]);
            } catch (Exception err) {
                Log.e(tag(), "FanArt()", err);
                width = 0;
                height = 0;
            }
        }

        boolean isValid() {
            return width > 0 && height > 0;
        }
    }

    public static class EpisodeList extends ArrayList<Episode> {

        final private Session session;
        final public int tvdb_id;
        final public String name;

        EpisodeList(Context context, Series series) {
            super();

            this.session = Session.getInstance(context);
            this.tvdb_id = series.tvdb_id;
            this.name = series.name();
        }

        void reload() {
            String query = "select * from episode where series = ?";
            if (!session.specials())
                query += " and season > 0 and episode > 0";
            query += " order by season, episode";
            Cursor cr = session.getDB().rawQuery(query, new String[]{Integer.toString(tvdb_id)}, null);
            try {
                clear();
                int i = cr.getColumnIndex("tvdb_id");
                while (cr.moveToNext())
                    add(new Episode(session.getContext(), cr.getInt(i)).load(cr));
            } finally {
                cr.close();
            }
        }

        public Episode getItem(int season, int episode) {
            int idx = getItemIndex(season, episode);
            return idx >= 0 ? get(idx) : null;
        }

        public Episode getItem(int tvdb_id) {
            int idx = getItemIndex(tvdb_id);
            return idx >= 0 ? get(idx) : null;
        }

        public int getItemIndex(int season, int episode) {
            Episode chk;
            for (int i = 0; i < size(); i++) {
                chk = get(i);
                if (chk.season == season && chk.episode == episode)
                    return i;
            }
            return -1;
        }

        public int getItemIndex(int tvdb_id) {
            for (int i = 0; i < size(); i++)
                if (get(i).tvdb_id == tvdb_id)
                    return i;
            return -1;
        }

        public List<Integer> getSeasons() {
            List<Integer> res = new ArrayList<>();
            for (Episode ep: this)
                if ((!ep.isSpecial() || session.specials()) && !res.contains(ep.season))
                    res.add(ep.season);
            return res;
        }
    }
}
