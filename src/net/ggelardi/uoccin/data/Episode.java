package net.ggelardi.uoccin.data;

import android.content.Context;
import android.database.Cursor;

public class Episode extends Title {
	
	public Episode(Context context) {
		super(context);
		
		type = EPISODE;
	}
	
	public Episode(Context context, Cursor cr) {
		super(context, cr);

		series_imdb_id = cr.getString(cr.getColumnIndex("series_imdb_id"));
		series_tvdb_id = cr.getInt(cr.getColumnIndex("series_tvdb_id"));
		tvdb_id = cr.getInt(cr.getColumnIndex("tvdb_id"));
		season = cr.getInt(cr.getColumnIndex("season"));
		episode = cr.getInt(cr.getColumnIndex("episode"));
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;

		int ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
		
		if (isOld() && session.isOnWIFI())
			refresh();
	}
	
	public static Episode load(Context context, String imdb_id) {
		String sql = "select * from titles t inner join episodes e on (e.imdb_id = t.imdb_id) where t.imdb_id = ?";
		Title res = Title.load(context, Episode.class, sql, imdb_id);
		return res != null ? (Episode) res : null;
	}
	
	private Series series;
	
	public String series_imdb_id;
	public int series_tvdb_id;
	public int tvdb_id; // episode tvdb_id
	public int season;
	public int episode;
	public long firstAired;
	public boolean collected = false;
	public boolean watched = false;
	
	@Override
	protected void refresh() {
		dispatch(TitleEvent.LOADING);
		
	}
	
	public Series getSeries() {
		if (series == null)
			series = Series.load(context, series_imdb_id);
		return series;
	}
}