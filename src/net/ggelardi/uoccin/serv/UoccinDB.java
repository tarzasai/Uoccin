package net.ggelardi.uoccin.serv;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class UoccinDB extends SQLiteOpenHelper {
	
	public static final String NAME = "Uoccin.db";
	public static final int VERSION = 1;
	
	public UoccinDB(Context context) {
		super(context, NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_TITLES);
		db.execSQL(CREATE_TABLE_MOVIES);
		db.execSQL(CREATE_TABLE_SERIES);
		db.execSQL(CREATE_TABLE_EPISODES);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion < 1) {
			// Development phase: destroy everything.
			db.execSQL("drop table episodes");
			db.execSQL("drop table series");
			db.execSQL("drop table movies");
			db.execSQL("drop table titles");
			onCreate(db);
		}
	}
	
	private static final String CS = ", ";
	private static final String PK = " PRIMARY KEY NOT NULL";
	private static final String DT_STR = " TEXT";
	private static final String DT_DBL = " REAL";
	private static final String DT_INT = " INTEGER";
	private static final String DT_FLG = " INTEGER NOT NULL DEFAULT 0";
	private static final String CC_NNU = " NOT NULL";
	
	private static final String CREATE_TABLE_TITLES = "CREATE TABLE titles (" +
		"imdb_id" + DT_STR + PK + CC_NNU + CS +
		"type" + DT_STR + CC_NNU + " CHECK (type IN ('movie', 'series', 'episode'))" + CS +
		"name" + DT_STR + CS +
		"plot" + DT_STR + CS +
		"director" + DT_STR + CS +
		"writers" + DT_STR + CS + // comma delimited
		"actors" + DT_STR + CS + // comma delimited
		"poster" + DT_STR + CS + // url
		"banner" + DT_STR + CS + // url
		"runtime" + DT_INT + CS + // minutes
		"timestamp" + DT_INT + CC_NNU + " DEFAULT CURRENT_TIMESTAMP" + CS +
		"rating" + DT_INT + CS +
		")";
	
	private static final String CREATE_TABLE_MOVIES = "CREATE TABLE movies (" +
		"imdb_id" + DT_STR + " REFERENCES titles(imdb_id) ON DELETE CASCADE" + CS +
		"language" + DT_STR + CC_NNU + " DEFAULT 'en'" + CS +
		"year" + DT_INT + CC_NNU + CS +
		"rated" + DT_STR + CS +
		"genres" + DT_STR + CS + // comma delimited
		"country" + DT_STR + CS +
		"released" + DT_INT + CS +
		"awards" + DT_STR + CS +
		"metascore" + DT_INT + CS +
		"imdbRating" + DT_DBL + CS +
		"imdbVotes" + DT_INT + CS +
		"watchlist" + DT_FLG + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		")";
	
	private static final String CREATE_TABLE_SERIES = "CREATE TABLE series (" +
		"imdb_id" + DT_STR + " REFERENCES titles(imdb_id) ON DELETE CASCADE" + CS +
		"language" + DT_STR + CC_NNU + " DEFAULT 'en'" + CS +
		"year" + DT_INT + CC_NNU + CS +
		"rated" + DT_STR + CS +
		"genres" + DT_STR + CS + // comma delimited
		"tvdb_id" + DT_INT + CC_NNU + CS +
		"status" + DT_STR + CC_NNU + " CHECK (status IN ('continuing', 'ended'))" + CS +
		"network" + DT_STR + CS +
		"firstAired" + DT_INT + CS +
		"airsDay" + DT_INT + CS +
		"airsTime" + DT_STR + CS +
		"watchlist" + DT_FLG + CS +
		")";
	
	private static final String CREATE_TABLE_EPISODES = "CREATE TABLE episodes (" +
		"imdb_id" + DT_STR + " REFERENCES titles(imdb_id) ON DELETE CASCADE" + CS +
		"tvdb_id" + DT_INT + CC_NNU + CS +
		"series_imdb_id" + DT_STR + CC_NNU + CS + // series imdb_id
		"series_tvdb_id" + DT_INT + CC_NNU + CS + // series tvdb_id
		"season" + DT_INT + CC_NNU + CS +
		"episode" + DT_INT + CC_NNU + CS +
		"firstAired" + DT_INT + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		")";
}