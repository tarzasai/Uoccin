package net.ggelardi.uoccin.data;

import android.content.Context;
import android.database.Cursor;

public class Series extends Title {
	
	private int tvdb_id;
	
	public Series(Context context, int tvdb_id) {
		super(context);

		this.tvdb_id = tvdb_id;
		
		if (!exists() || isOld())
			refresh();
	}
	
	@Override
	protected void read(Cursor cr) {
		super.read(cr);
		
	}
	
	protected boolean exists() {
		String sql = "select * from title t inner join series s on (s.title_id = t.id) where s.tvdb_id = ?";
		Cursor cr = dbconn.rawQuery(sql, new String[] { Integer.toString(tvdb_id) });
		try {
			if (cr.moveToFirst()) {
				read(cr);
				return true;
			}
		} finally {
			cr.close();
		}
		return false;
	}
	
	protected void refresh() {
		
	}
}