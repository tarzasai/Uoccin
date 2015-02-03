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
	
	/*
	public UoccinDB(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}
	
	public UoccinDB(Context context, String name, CursorFactory factory, int version, DatabaseErrorHandler errorHandler) {
		super(context, name, factory, version, errorHandler);
		// TODO Auto-generated constructor stub
	}
	*/
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
		
	}
	
	private static final String CS = ", ";
	private static final String PK = " PRIMARY KEY NOT NULL";
	private static final String DT_INT = " INTEGER";
	private static final String DT_STR = " TEXT";
	private static final String DT_DAT = " TEXT";
	private static final String CC_NNU = " NOT NULL";
	
}