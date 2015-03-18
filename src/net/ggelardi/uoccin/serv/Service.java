package net.ggelardi.uoccin.serv;

import java.util.List;

import net.ggelardi.uoccin.api.TNT;
import net.ggelardi.uoccin.api.XML;
import net.ggelardi.uoccin.data.Episode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class Service extends IntentService {
	private static final String TAG = "Service";
	
	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	
	private Session session;
	
	public Service() {
		super("Service");
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		session = Session.getInstance(this);
		String act = intent != null ? intent.getAction() : null;
		Log.v(TAG, act);
		if (TextUtils.isEmpty(act))
			session.registerAlarms();
		else if (act.equals(CLEAN_DB_CACHE)) {
			session.getDB().execSQL("delete from series where watchlist = 0 and (rating is null or rating = 0) " +
				"and not tvdb_id in (select distinct series from episode where collected = 1 or watched = 1)");
		} else if (act.equals(CHECK_TVDB_RSS)) {
			checkTVDB_NewsFeed();
		} else if (act.equals(REFRESH_MOVIE)) {
			//
		} else if (act.equals(REFRESH_SERIES)) {
			//
		} else if (act.equals(REFRESH_EPISODE)) {
			//
		}
	}
	
	private void checkTVDB_NewsFeed() {
		try {
			List<String> links = new TNT().getLinks();
			for (String url : links) {
				try {
					String eid = Uri.parse(url).getQueryParameter("id");
					Document doc = XML.TVDB.getInstance().getEpByIdSync(eid);
					NodeList lst = doc.getElementsByTagName("Episode");
					if (lst != null && lst.getLength() > 0) {
						Episode ep = Episode.get(session.getContext(), (Element) lst.item(0));
						if (ep != null && ep.season == 1 && ep.episode == 1) {
							
						}
					}
				} catch (Exception err) {
					Log.e(TAG, url, err);
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "checkTVDB_NewsFeed", err);
			// ???
		}
	}
}