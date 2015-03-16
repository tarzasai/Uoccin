package net.ggelardi.uoccin;

import net.ggelardi.uoccin.serv.Session;
import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class Service extends IntentService {
	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	
	public Service() {
		super("Service");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Session session = Session.getInstance(this);
		String act = intent != null ? intent.getAction() : null;
		Log.v(getClass().getSimpleName(), act);
		if (TextUtils.isEmpty(act))
			session.registerAlarms();
		else if (act.equals(CLEAN_DB_CACHE)) {
			session.getDB().execSQL("delete from series where watchlist = 0 and (rating is null or rating = 0) and " +
				"not tvdb_id in (select distinct series from episode where collected = 1 or watched = 1)");
		} else if (act.equals(CHECK_TVDB_RSS)) {
			//
		}
	}
	
	
}