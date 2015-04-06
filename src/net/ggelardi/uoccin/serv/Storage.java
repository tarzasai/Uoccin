package net.ggelardi.uoccin.serv;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Storage extends SQLiteOpenHelper {
	//private static final String TAG = "Storage";
	
	public static final String NAME = "Uoccin.db";
	public static final int VERSION = 1;
	
	public Storage(Context context) {
		super(context, NAME, null, VERSION);
		
		/*
		Log.v(TAG, CREATE_TABLE_MOVIES);
		Log.v(TAG, CREATE_TABLE_SERIES);
		Log.v(TAG, CREATE_TABLE_EPISODES);
		*/
		
		/*
		SQLiteDatabase db = getWritableDatabase();
		db.execSQL("delete from episode where season = 0 or episode = 0");
		db.execSQL("update episode set subtitles = null");
		db.execSQL("delete from movie");
		db.execSQL("update episode set guestStars = null where guestStars = '' or guestStars = ','");
		db.execSQL("update episode set writers = null where writers = '' or writers = ','");
		db.execSQL("update episode set director = null where director = ''");
		*/
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE_MOVIES);
		db.execSQL(CREATE_TABLE_SERIES);
		db.execSQL(CREATE_TABLE_EPISODES);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion < 1) {
			// Development phase: destroy everything.
			db.execSQL("drop table episode");
			db.execSQL("drop table series");
			db.execSQL("drop table movie");
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
	
	private static final String CREATE_TABLE_MOVIES = "CREATE TABLE movie (" +
		"imdb_id" + DT_STR + PK + CC_NNU + CS +
		"name" + DT_STR + CC_NNU + CS +
		"year" + DT_INT + CC_NNU + CS +
		"plot" + DT_STR + CS +
		"poster" + DT_STR + CS + // url
		"genres" + DT_STR + CS + // comma delimited
		"language" + DT_STR + CC_NNU + " DEFAULT 'en'" + CS +
		"director" + DT_STR + CS +
		"writers" + DT_STR + CS + // comma delimited
		"actors" + DT_STR + CS + // comma delimited
		"country" + DT_STR + CS +
		"released" + DT_INT + CS +
		"runtime" + DT_INT + CS + // minutes
		"rated" + DT_STR + CS +
		"awards" + DT_STR + CS +
		"metascore" + DT_INT + CS +
		"imdbRating" + DT_DBL + CS +
		"imdbVotes" + DT_INT + CS +
		"rating" + DT_INT + CS +
		"tags" + DT_STR + CS +
		"subtitles" + DT_STR + CS +
		"watchlist" + DT_FLG + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		"timestamp" + DT_INT + CC_NNU + " DEFAULT CURRENT_TIMESTAMP" +
		")";
	
	private static final String CREATE_TABLE_SERIES = "CREATE TABLE series (" +
		"tvdb_id" + DT_STR + PK + CC_NNU + CS +
		"name" + DT_STR + CC_NNU + CS +
		"year" + DT_INT + CS +
		"plot" + DT_STR + CS +
		"poster" + DT_STR + CS + // url
		"genres" + DT_STR + CS + // comma delimited
		"actors" + DT_STR + CS + // comma delimited
		"imdb_id" + DT_STR + CS +
		"status" + DT_STR + CS +
		"network" + DT_STR + CS +
		"firstAired" + DT_INT + CS +
		"airsDay" + DT_INT + CS +
		"airsTime" + DT_INT + CS +
		"runtime" + DT_INT + CS + // minutes
		"rated" + DT_STR + CS +
		"banner" + DT_STR + CS + // url
		"fanart" + DT_STR + CS + // url
		"rating" + DT_INT + CS +
		"tags" + DT_STR + CS +
		"watchlist" + DT_FLG + CS +
		"timestamp" + DT_INT + CC_NNU + " DEFAULT CURRENT_TIMESTAMP" +
		")";
	
	private static final String CREATE_TABLE_EPISODES = "CREATE TABLE episode (" +
		"tvdb_id" + DT_STR + PK + CC_NNU + CS +
		"series" + DT_STR + CC_NNU + " REFERENCES series(tvdb_id) ON DELETE CASCADE" + CS +
		"season" + DT_INT + CC_NNU + CS +
		"episode" + DT_INT + CC_NNU + CS +
		"name" + DT_STR + CS +
		"plot" + DT_STR + CS +
		"poster" + DT_STR + CS + // url
		"writers" + DT_STR + CS + // comma delimited
		"director" + DT_STR + CS +
		"guestStars" + DT_STR + CS + // comma delimited
		"firstAired" + DT_INT + CS +
		"imdb_id" + DT_STR + CS +
		"subtitles" + DT_STR + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		"timestamp" + DT_INT + CC_NNU + " DEFAULT CURRENT_TIMESTAMP" +
		")";
}