package net.ggelardi.uoccin.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Storage extends SQLiteOpenHelper {
    private static final String TAG = "Storage";

    public static final String NAME = "Uoccin.db";
    public static final int VERSION = 5;

    public Storage(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MOVIE);
        db.execSQL(CREATE_TABLE_MOVTAG);
        db.execSQL(CREATE_TABLE_SERIES);
        db.execSQL(CREATE_TABLE_SERTAG);
        db.execSQL(CREATE_TABLE_SERPIC);
        db.execSQL(CREATE_INDEX_PICSER);
        db.execSQL(CREATE_TABLE_EPISODE);
        db.execSQL(CREATE_INDEX_EPISER);
        db.execSQL(CREATE_TABLE_QUEUEIN);
        db.execSQL(CREATE_TABLE_QUEUEOUT);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 4) {
            db.execSQL("DROP TABLE IF EXISTS queue_out");
            db.execSQL("DROP TABLE IF EXISTS queue_in");
            db.execSQL("DROP TABLE IF EXISTS episode");
            db.execSQL("DROP TABLE IF EXISTS sertag");
            db.execSQL("DROP TABLE IF EXISTS serpic");
            db.execSQL("DROP TABLE IF EXISTS series");
            db.execSQL("DROP TABLE IF EXISTS movtag");
            db.execSQL("DROP TABLE IF EXISTS movie");
            db.execSQL(CREATE_TABLE_MOVIE);
            db.execSQL(CREATE_TABLE_MOVTAG);
            db.execSQL(CREATE_TABLE_SERIES);
            db.execSQL(CREATE_TABLE_SERTAG);
            db.execSQL(CREATE_TABLE_SERPIC);
            db.execSQL(CREATE_INDEX_PICSER);
            db.execSQL(CREATE_TABLE_EPISODE);
            db.execSQL(CREATE_INDEX_EPISER);
            db.execSQL(CREATE_TABLE_QUEUEIN);
            db.execSQL(CREATE_TABLE_QUEUEOUT);
            return;
        }
        int upgradeTo = oldVersion + 1;
        while (upgradeTo <= newVersion) {
            Log.d(TAG, "Upgrading database to version " + Integer.toString(upgradeTo));
            switch (upgradeTo) {
                case 5:
                    db.setForeignKeyConstraintsEnabled(false);
                    db.beginTransaction();
                    try {
                        // series
                        db.execSQL(CREATE_TABLE_SERIES.replace("TABLE series (", "TABLE ser_new ("));
                        db.execSQL("INSERT INTO ser_new " +
                                "(tvdb_id, name, year, plot, poster, genres, actors, imdb_id, status, network, " +
                                "firstAired, airsDay, runtime, rated, banner, rating, watchlist, timestamp, createtime) " +
                                "SELECT " +
                                "tvdb_id, name, year, plot, poster, genres, actors, imdb_id, status, network, firstAired, " +
                                "airsDay, runtime, rated, banner, rating, watchlist, timestamp, createtime " +
                                "FROM series");
                        db.execSQL("DROP TABLE series");
                        db.execSQL("ALTER TABLE ser_new RENAME TO series");
                        db.execSQL(CREATE_TABLE_SERPIC);
                        db.execSQL(CREATE_INDEX_PICSER);
                        // episodes
                        db.execSQL(CREATE_TABLE_SERTAG.replace("TABLE sertag (", "TABLE tag_new ("));
                        db.execSQL("INSERT INTO tag_new SELECT * FROM sertag");
                        db.execSQL("DROP TABLE sertag");
                        db.execSQL("ALTER TABLE tag_new RENAME TO sertag");

                        db.execSQL("DELETE FROM episode WHERE tvdb_id IS NULL");
                        db.execSQL(CREATE_TABLE_EPISODE.replace("TABLE episode (", "TABLE eps_new ("));
                        db.execSQL("INSERT INTO eps_new (tvdb_id, series, season, episode, name, plot, writers, " +
                                "director, guestStars, firstAired, imdb_id, subtitles, collected, watched, " +
                                "timestamp) SELECT tvdb_id, series, season, episode, name, plot, writers, " +
                                "director, guestStars, firstAired, imdb_id, subtitles, collected, watched, " +
                                "timestamp FROM episode");
                        db.execSQL("DROP TABLE episode");
                        db.execSQL("ALTER TABLE eps_new RENAME TO episode");
                        db.execSQL(CREATE_INDEX_EPISER);
                        // finish
                        db.setTransactionSuccessful();
                    } catch (Exception err) {
                        Log.e(TAG, "onUpgrade(5)", err);
                        throw err;
                    } finally {
                        db.endTransaction();
                        db.setForeignKeyConstraintsEnabled(true);
                    }
                    break;
            }
            upgradeTo++;
        }
    }

    private static final String CS = ", ";
    private static final String PK = " PRIMARY KEY NOT NULL";
    private static final String DT_STR = " TEXT";
    private static final String DT_DBL = " REAL";
    private static final String DT_INT = " INTEGER";
    private static final String DT_FLG = " INTEGER NOT NULL DEFAULT 0";
    private static final String CC_NNU = " NOT NULL";

    private static final String CREATE_TABLE_MOVIE = "CREATE TABLE movie (" +
            "imdb_id" + DT_STR + PK + CS +
            "name" + DT_STR + CC_NNU + CS +
            "year" + DT_INT + CS +
            "plot" + DT_STR + CS +
            "poster" + DT_STR + CS + // url
            "genres" + DT_STR + CS + // comma delimited
            "language" + DT_STR + CS +
            "director" + DT_STR + CS +
            "writers" + DT_STR + CS + // comma delimited
            "actors" + DT_STR + CS + // comma delimited
            "country" + DT_STR + CS +
            "released" + DT_INT + CS +
            "runtime" + DT_INT + CS + // minutes
            "rated" + DT_STR + CS +
            "awards" + DT_STR + CS +
            "metascore" + DT_INT + CS +
            "tomatoMeter" + DT_INT + CS +
            "imdbRating" + DT_DBL + CS +
            "imdbVotes" + DT_INT + CS +
            "rating" + DT_INT + CS +
            "subtitles" + DT_STR + CS +
            "watchlist" + DT_FLG + CS +
            "collected" + DT_FLG + CS +
            "watched" + DT_FLG + CS +
            "timestamp" + DT_INT + CC_NNU + " DEFAULT 0" + CS +
            "createtime" + DT_INT + CC_NNU +
            ")";

    private static final String CREATE_TABLE_MOVTAG = "CREATE TABLE movtag (" +
            "movie" + DT_STR + CC_NNU + " REFERENCES movie(imdb_id) ON DELETE CASCADE" + CS +
            "tag" + DT_STR + CC_NNU + CS +
            "PRIMARY KEY (movie, tag)" +
            ")";

    private static final String CREATE_TABLE_SERIES = "CREATE TABLE series (" +
            "tvdb_id" + DT_INT + PK + CS +
            "name" + DT_STR + CC_NNU + CS +
            "year" + DT_INT + CS +
            "plot" + DT_STR + CS +
            "poster" + DT_STR + CS + // url
            "banner" + DT_STR + CS + // url
            "genres" + DT_STR + CS + // comma delimited
            "actors" + DT_STR + CS + // comma delimited
            "imdb_id" + DT_STR + CS +
            "status" + DT_STR + CS +
            "network" + DT_STR + CS +
            "firstAired" + DT_INT + CS +
            "airsDay" + DT_INT + CS +
            "airsTime" + DT_STR + CS +
            "runtime" + DT_INT + CS + // minutes
            "rated" + DT_STR + CS +
            "siteRating" + DT_DBL + CS +
            "siteVotes" + DT_INT + CS +
            "rating" + DT_INT + CS +
            "watchlist" + DT_FLG + CS +
            "timestamp" + DT_INT + CC_NNU + " DEFAULT 0" + CS +
            "createtime" + DT_INT + CC_NNU +
            ")";

    private static final String CREATE_TABLE_SERTAG = "CREATE TABLE sertag (" +
            "series" + DT_INT + CC_NNU + " REFERENCES series(tvdb_id) ON DELETE CASCADE" + CS +
            "tag" + DT_STR + CC_NNU + CS +
            "PRIMARY KEY (series, tag)" +
            ")";

    private static final String CREATE_TABLE_SERPIC = "CREATE TABLE serpic (" +
            "tvdb_id" + DT_INT + PK + CS +
            "series" + DT_INT + CC_NNU + " REFERENCES series(tvdb_id) ON DELETE CASCADE" + CS +
            "fullres" + DT_STR + CC_NNU + CS +
            "thumbnail" + DT_STR + CC_NNU + CS +
            "text" + DT_FLG + CS +
            "width" + DT_INT + CC_NNU + CS +
            "height" + DT_INT + CC_NNU +
            ")";

    public static final String CREATE_INDEX_PICSER = "CREATE INDEX picture_series ON serpic(series)";

    private static final String CREATE_TABLE_EPISODE = "CREATE TABLE episode (" +
            "tvdb_id" + DT_INT + PK + CS +
            "series" + DT_INT + CC_NNU + " REFERENCES series(tvdb_id) ON DELETE CASCADE" + CS +
            "season" + DT_INT + CC_NNU + CS +
            "episode" + DT_INT + CC_NNU + CS +
            "name" + DT_STR + CS +
            "plot" + DT_STR + CS +
            "writers" + DT_STR + CS + // comma delimited
            "director" + DT_STR + CS +
            "guestStars" + DT_STR + CS + // comma delimited
            "firstAired" + DT_INT + CS +
            "thumb" + DT_STR + CS + // url
            "thumbWidth" + DT_INT + CS +
            "thumbHeight" + DT_INT + CS +
            "imdb_id" + DT_STR + CS +
            "subtitles" + DT_STR + CS +
            "collected" + DT_FLG + CS +
            "watched" + DT_FLG + CS +
            "timestamp" + DT_INT + CC_NNU + " DEFAULT 0" +
            ")";

    public static final String CREATE_INDEX_EPISER = "CREATE INDEX episode_series ON episode(series)";

    public static final String CREATE_TABLE_QUEUEIN = "CREATE TABLE queue_in (" +
            "timestamp" + DT_INT + CC_NNU + CS + // UTC
            "target" + DT_STR + CC_NNU + " CHECK (target IN ('movie', 'series'))" + CS +
            "title" + DT_STR + CC_NNU + CS +
            "field" + DT_STR + CS +
            "value" + DT_STR +
            ")";

    public static final String CREATE_TABLE_QUEUEOUT = "CREATE TABLE queue_out (" +
            "timestamp" + DT_INT + CC_NNU + CS + // UTC
            "target" + DT_STR + CC_NNU + " CHECK (target IN ('movie', 'series'))" + CS +
            "title" + DT_STR + CC_NNU + CS +
            "field" + DT_STR + CS +
            "value" + DT_STR +
            ")";
}