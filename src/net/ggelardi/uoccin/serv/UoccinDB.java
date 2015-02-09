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
		db.execSQL(CREATE_TABLE_TITLE);
		db.execSQL(CREATE_TABLE_SERIES);
		db.execSQL(CREATE_TABLE_EPISODES);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion < 1) {
			// Development phase: destroy everything.
			db.execSQL("drop table episodes");
			db.execSQL("drop table series");
			db.execSQL("drop table title");
			onCreate(db);
		}
	}
	
	private static final String CS = ", ";
	private static final String PK = " PRIMARY KEY NOT NULL";
	private static final String DT_INT = " INTEGER";
	private static final String DT_STR = " TEXT";
	private static final String DT_FLG = " INTEGER NOT NULL DEFAULT 0";
	private static final String DT_DAT = " TEXT";
	private static final String CC_NNU = " NOT NULL";
	
	private static final String CREATE_TABLE_TITLE = "CREATE TABLE title (" +
		"id" + DT_INT + PK + " AUTOINCREMENT" + CS +
		"type" + DT_STR + CC_NNU + " CHECK (type IN ('movie', 'series', 'episode'))" + CS +
		"name" + DT_STR + CC_NNU + CS +
		"year" + DT_INT + CC_NNU + CS +
		"plot" + DT_STR + CS +
		"imdb_id" + DT_STR + CC_NNU + CS +
		"language" + DT_STR + CC_NNU + " DEFAULT 'en'" + CS +
		"director" + DT_STR + CS +
		"writers" + DT_STR + CS +
		"actors" + DT_STR + CS +
		"poster" + DT_STR + CS +
		"banner" + DT_STR + CS +
		"rating" + DT_INT + CS +
		"runtime" + DT_INT + CS +
		"timestamp" + DT_DAT + CC_NNU + " DEFAULT CURRENT_TIMESTAMP" + CS +
		"watchlist" + DT_FLG + CS +
		"collected" + DT_FLG + CS +
		"watched" + DT_FLG + CS +
		")";
	
	private static final String CREATE_TABLE_SERIES = "CREATE TABLE series (" +
		"title_id" + DT_INT + " REFERENCES title(id) ON DELETE CASCADE" + CS +
		"tvdb_id" + DT_INT + CC_NNU + CS +
		"status" + DT_STR + CC_NNU + " CHECK (status IN ('continuing', 'ended'))" + CS +
		"genres" + DT_STR + CS +
		"network" + DT_STR + CS +
		"airsDay" + DT_INT + CS +
		"airsTime" + DT_STR + CS +
		"firstAired" + DT_DAT + CS +
		"contentRating" + DT_STR + CS +
		")";
	
	private static final String CREATE_TABLE_EPISODES = "CREATE TABLE episodes (" +
		"title_id" + DT_INT + " REFERENCES title(id) ON DELETE CASCADE" + CS +
		"tvdb_id" + DT_INT + CC_NNU + CS +
		"season" + DT_INT + CC_NNU + CS +
		"number" + DT_INT + CC_NNU + CS +
		"firstAired" + DT_DAT + CS +
		")";
}