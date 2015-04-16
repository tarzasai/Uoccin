package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.api.XML;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.model.Change;
import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

public class Service extends WakefulIntentService {
	private static final String TAG = "Service";
	
	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String GDRIVE_CHECK = "net.ggelardi.uoccin.GDRIVE_CHECK";
	public static final String GDRIVE_BACKUP = "net.ggelardi.uoccin.GDRIVE_BACKUP";
	public static final String GDRIVE_RESTORE = "net.ggelardi.uoccin.GDRIVE_RESTORE";
	
	private Session session;
	private GSA drive;
	private Genson genson;
	
	public Service() {
		super("Service");
	}
	
	@Override
	protected void doWakefulWork(Intent intent) {
		session = Session.getInstance(this);
		String act = intent != null ? intent.getAction() : null;
		Log.v(TAG, act);
		Title.ongoingServiceOperation = true;
		try {
			if (TextUtils.isEmpty(act))
				session.registerAlarms();
			else if (act.equals(CLEAN_DB_CACHE)) {
				session.getDB().execSQL("delete from series where watchlist = 0 and tags is null and " +
					"(rating is null or rating = 0) and not tvdb_id in (select distinct series from " +
					"episode where collected = 1 or watched = 1)");
			} else if (act.equals(REFRESH_MOVIE)) {
				refreshMovie(intent.getExtras().getString("imdb_id"), true, false);
			} else if (act.equals(REFRESH_SERIES)) {
				refreshSeries(intent.getExtras().getString("tvdb_id"), true, false);
			} else if (act.equals(REFRESH_EPISODE)) {
				Bundle extra = intent.getExtras();
				String series = extra.getString("series");
				int season = extra.getInt("season");
				int episode = extra.getInt("episode");
				refreshEpisode(series, season, episode);
			} else if (act.equals(CHECK_TVDB_RSS)) {
				checkTVdbNews();
			} else if (act.equals(GDRIVE_CHECK) && session.driveEnabled()) {
				List<String> files = new ArrayList<String>();
				files.add(Commons.GD.MOV_WLST);
				files.add(Commons.GD.MOV_COLL);
				files.add(Commons.GD.MOV_SEEN);
				files.add(Commons.GD.SER_WLST);
				files.add(Commons.GD.SER_COLL);
				files.add(Commons.GD.SER_SEEN);
				checkDrive();
				List<Change> changes = drive.getChanges();
				Collections.reverse(changes);
				String fn;
				long fd;
				for (Change ch: changes) {
					if (files.isEmpty())
						break;
					fn = ch.getFile().getTitle();
					fd = ch.getFile().getModifiedDate().getValue();
					Log.d(TAG, "File: " + fn + " - Date: " + ch.getFile().getModifiedDate().toString());
					if (files.contains(fn) && fd > session.driveLastFileUpdateUTC(fn)) {
						files.remove(fn);
						if (fn.equals(Commons.GD.MOV_WLST))
							restoreMovieWatchlist();
						else if (fn.equals(Commons.GD.MOV_COLL))
							restoreMovieCollection();
						else if (fn.equals(Commons.GD.MOV_SEEN))
							restoreMovieWatched();
						else if (fn.equals(Commons.GD.SER_WLST))
							restoreSeriesWatchlist();
						else if (fn.equals(Commons.GD.SER_COLL))
							restoreSeriesCollection();
						else if (fn.equals(Commons.GD.SER_SEEN))
							restoreSeriesWatched();
					}
				}
			} else if (act.equals(GDRIVE_BACKUP) && session.driveEnabled()) {
				Bundle extra = intent.getExtras();
				String what = extra == null ? "*" : extra.getString("what");
				if (what.equals("*") || what.equals(Commons.GD.MOV_WLST))
					backupMovieWatchlist();
				if (what.equals("*") || what.equals(Commons.GD.MOV_COLL))
					backupMovieCollection();
				if (what.equals("*") || what.equals(Commons.GD.MOV_SEEN))
					backupMovieWatched();
				if (what.equals("*") || what.equals(Commons.GD.SER_WLST))
					backupSeriesWatchlist();
				if (what.equals("*") || what.equals(Commons.GD.SER_COLL))
					backupSeriesCollection();
				if (what.equals("*") || what.equals(Commons.GD.SER_SEEN))
					backupSeriesWatched();
			} else if (act.equals(GDRIVE_RESTORE) && session.driveEnabled()) {
				Bundle extra = intent.getExtras();
				String what = extra == null ? "*" : extra.getString("what");
				if (what.equals("*") || what.equals(Commons.GD.MOV_WLST))
					restoreMovieWatchlist();
				if (what.equals("*") || what.equals(Commons.GD.MOV_COLL))
					restoreMovieCollection();
				if (what.equals("*") || what.equals(Commons.GD.MOV_SEEN))
					restoreMovieWatched();
				if (what.equals("*") || what.equals(Commons.GD.SER_WLST))
					restoreSeriesWatchlist();
				if (what.equals("*") || what.equals(Commons.GD.SER_COLL))
					restoreSeriesCollection();
				if (what.equals("*") || what.equals(Commons.GD.SER_SEEN))
					restoreSeriesWatched();
			}
		} catch (UserRecoverableAuthIOException err) {
			sendBroadcast(new Intent(Commons.SN.CONNECT_FAIL));
		} catch (Exception err) {
			sendNotification(err);
		} finally {
			Title.ongoingServiceOperation = false;
		}
	}
	
	private void sendNotification(String what) {
		Intent si = new Intent(Commons.SN.GENERAL_INFO);
		si.putExtra("what", what);
		sendBroadcast(si);
	}
	
	private void sendNotification(Throwable what) {
		Intent si = new Intent(Commons.SN.GENERAL_FAIL);
		si.putExtra("what", what.getLocalizedMessage());
		sendBroadcast(si);
	}
	
	private void refreshMovie(String imdb_id, boolean forceRefresh, boolean forceCommit) {
		Movie mov = Movie.get(this, imdb_id);
		if (!(forceRefresh || mov.isNew() || mov.isOld())) // TODO wifi check?
			return;
		Log.d(TAG, "refreshing movie " + imdb_id);
		Document doc;
		try {
			doc = XML.OMDB.getInstance().getMovie(imdb_id);
		} catch (Exception err) {
			Log.e(TAG, "refreshMovie", err);
			if (!err.getLocalizedMessage().startsWith("404"))
				sendNotification(err);
			return;
		}
		String res = Commons.XML.attrText(doc.getDocumentElement(), "response");
		if (TextUtils.isEmpty(res)) {
			Log.e(TAG, "Unknow response for imdb=" + imdb_id);
			return;
		}
		if (!res.toLowerCase(Locale.getDefault()).equals("true")) {
			String error;
			try {
				error = Commons.XML.nodeText(doc.getDocumentElement(), "error");
			} catch (Exception err) {
				error = "unknown error";
			}
			Log.e(TAG, "Error on imdb=" + imdb_id + ": " + error);
			return;
		}
		Movie.get(this, (Element) doc.getElementsByTagName("movie").item(0));
		if (forceCommit || !mov.isNew() || mov.inWatchlist() || mov.inCollection() || mov.isWatched() ||
			mov.getRating() > 0 || mov.hasTags())
			mov.commit(null);
		else
			Series.dispatch(OnTitleListener.READY, null);
	}
	
	private void refreshSeries(String tvdb_id, boolean forceRefresh, boolean forceCommit) {
		Series ser = Series.get(this, tvdb_id);
		if (!(forceRefresh || ser.isNew() || ser.isOld())) // TODO wifi check?
			return;
		Log.d(TAG, "refreshing series " + tvdb_id);
		Document doc;
		try {
			doc = XML.TVDB.getInstance().getFullSeries(tvdb_id, session.language());
		} catch (Exception err) {
			Log.e(TAG, "refreshSeries", err);
			if (!err.getLocalizedMessage().startsWith("404"))
				sendNotification(err);
			return;
		}
		Series.get(this, (Element) doc.getElementsByTagName("Series").item(0));
		// episodes
		int eps2save = 0;
		ser.lastseason = 0;
		NodeList lst = doc.getElementsByTagName("Episode");
		if (lst != null && lst.getLength() > 0) {
			Episode ep;
			for (int i = 0; i < lst.getLength(); i++) {
				ep = Episode.get(this, (Element) lst.item(i));
				if (ep != null) {
					if (ep.season > ser.lastseason)
						ser.lastseason = ep.season;
					if (!ser.episodes.contains(ep))
						ser.episodes.add(ep);
					if (ep.hasSubtitles() || ep.inCollection() || ep.isWatched())
						eps2save++;
				}
			}
			Collections.sort(ser.episodes, new Episode.EpisodeComparator());
		}
		if (forceCommit || !ser.isNew() || ser.inWatchlist() || ser.getRating() > 0 || ser.hasTags() || eps2save > 0)
			ser.commit(null);
		else
			Series.dispatch(OnTitleListener.READY, null);
	}
	
	private void refreshEpisode(String series, int season, int episode) {
		Episode epi = Episode.get(this, series, season, episode);
		if (!(epi.isNew() || epi.isOld())) // TODO wifi check?
			return;
		Log.d(TAG, "refreshing episode " + epi.eid());
		Document doc;
		try {
			doc = XML.TVDB.getInstance().getEpisode(series, season, episode, session.language());
		} catch (Exception err) {
			Log.e(TAG, "refreshEpisode", err);
			if (!err.getLocalizedMessage().startsWith("404"))
				sendNotification(err);
			return;
		}
		epi = Episode.get(this, (Element) doc.getElementsByTagName("Episode").item(0));
		if (epi != null)
			epi.commit(null);
	}
	
	private void checkTVdbNews() throws Exception {
		String content = null;
	    URL url = new URL("http://thetvdb.com/rss/newtoday.php");
		BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
		try {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line);
			content = sb.toString();
		} finally {
			reader.close();
		}
		if (TextUtils.isEmpty(content))
			return;
		DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
		DocumentBuilder bld = fac.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(content));
		Document feed = bld.parse(is);
		feed.getDocumentElement().normalize();
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList items = (NodeList)xPath.evaluate("/rss/channel/item", feed.getDocumentElement(),
			XPathConstants.NODESET);
		if (items != null && items.getLength() > 0) {
			String link;
			for (int i = 0; i < items.getLength(); i++) {
				link = Commons.XML.nodeText((Element) items.item(i), "link");
				try {
					String eid = Uri.parse(link).getQueryParameter("id");
					Document doc = XML.TVDB.getInstance().getEpisodeById(eid, "en");
					NodeList lst = doc.getElementsByTagName("Episode");
					if (lst != null && lst.getLength() > 0) {
						Episode ep = Episode.get(session.getContext(), (Element) lst.item(0));
						if (ep != null && ep.season == 1 && ep.episode == 1 && !ep.getSeries().inWatchlist()) {
							Log.d(TAG, "New series found: " + ep.getSeries().name);
							//ep.getSeries().addTag("premiere");
						}
					}
				} catch (Exception err) {
					Log.e(TAG, link, err);
				}
			}
		}
	}
	
	private void checkDrive() throws Exception {
		if (drive == null)
			drive = new GSA(this);
	}
	
	private void checkGenson() {
		if (genson == null)
			genson = new GensonBuilder().
				//useIndentation(true). // DEBUG ONLY!
				create();
	}
	
	private void backupMovieWatchlist() throws Exception {
		try {
			checkDrive();
			Map<String, MovieWLST> map = new HashMap<String, MovieWLST>();
			Cursor cur = session.getDB().query("movie", new String[] { "imdb_id" }, "watchlist = 1", null, null,
				null, "name collate nocase", null);
			try {
				Movie mov;
				MovieWLST obj;
				while (cur.moveToNext()) {
					mov = Movie.get(this, cur.getString(0));
					obj = new MovieWLST();
					obj.name = mov.name;
					obj.tags = mov.getTags().toArray(new String[mov.getTags().size()]);
					map.put(mov.imdb_id, obj);
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				checkGenson();
				content = genson.serialize(map);
			}
			drive.writeFile(Commons.GD.MOV_WLST, content);
			session.setDriveLastFileUpdate(Commons.GD.MOV_WLST, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "backupMovieWatchlist", err);
			throw err;
		}
	}
	
	private void backupMovieCollection() throws Exception {
		try {
			checkDrive();
			Map<String, MovieCOLL> map = new HashMap<String, MovieCOLL>();
			Cursor cur = session.getDB().query("movie", new String[] { "imdb_id" }, "collected = 1", null, null,
				null, "name collate nocase", null);
			try {
				Movie mov;
				MovieCOLL obj;
				while (cur.moveToNext()) {
					mov = Movie.get(this, cur.getString(0));
					obj = new MovieCOLL();
					obj.name = mov.name;
					map.put(mov.imdb_id, obj);
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				checkGenson();
				content = genson.serialize(map);
			}
			drive.writeFile(Commons.GD.MOV_COLL, content);
			session.setDriveLastFileUpdate(Commons.GD.MOV_COLL, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "backupMovieCollection", err);
			throw err;
		}
	}
	
	private void backupMovieWatched() throws Exception {
		try {
			checkDrive();
			Map<String, MovieSEEN> map = new HashMap<String, MovieSEEN>();
			Cursor cur = session.getDB().query("movie", new String[] { "imdb_id" }, "watched = 1", null, null,
				null, "name collate nocase", null);
			try {
				Movie mov;
				MovieSEEN obj;
				while (cur.moveToNext()) {
					mov = Movie.get(this, cur.getString(0));
					obj = new MovieSEEN();
					obj.name = mov.name;
					obj.rating = mov.getRating();
					map.put(mov.imdb_id, obj);
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				checkGenson();
				content = genson.serialize(map);
			}
			drive.writeFile(Commons.GD.MOV_SEEN, content);
			session.setDriveLastFileUpdate(Commons.GD.MOV_SEEN, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "backupMovieWatched", err);
			throw err;
		}
	}
	
	private void backupSeriesWatchlist() throws Exception {
		try {
			checkDrive();
			Map<String, SeriesWLST> map = new HashMap<String, SeriesWLST>();
			Cursor cur = session.getDB().query("series", new String[] { "tvdb_id" }, "watchlist = 1", null, null,
				null, "name collate nocase", null);
			try {
				Series ser;
				SeriesWLST obj;
				while (cur.moveToNext()) {
					ser = Series.get(this, cur.getString(0));
					obj = new SeriesWLST();
					obj.name = ser.name;
					obj.tags = ser.getTags().toArray(new String[ser.getTags().size()]);
					obj.rating = ser.getRating();
					map.put(ser.tvdb_id, obj);
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				checkGenson();
				content = genson.serialize(map);
			}
			drive.writeFile(Commons.GD.SER_WLST, content);
			session.setDriveLastFileUpdate(Commons.GD.SER_WLST, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "backupSeriesWatchlist", err);
			throw err;
		}
	}
	
	private void backupSeriesCollection() throws Exception {
		try {
			checkDrive();
			Map<String, String[]> map = new HashMap<String, String[]>();
			Cursor cur = session.getDB().query("episode", new String[] { "series", "season", "episode" },
				"collected = 1", null, null, null, "series, season, episode");
			try {
				Episode ep;
				while (cur.moveToNext()) {
					ep = Episode.get(this, cur.getString(0), cur.getInt(1), cur.getInt(2));
					if (ep != null)
						map.put(ep.eid().toString(), ep.subtitles.toArray(new String[ep.subtitles.size()]));
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				checkGenson();
				content = genson.serialize(map);
			}
			drive.writeFile(Commons.GD.SER_COLL, content);
			session.setDriveLastFileUpdate(Commons.GD.SER_COLL, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "backupSeriesCollection", err);
			throw err;
		}
	}
	
	private void backupSeriesWatched() throws Exception {
		try {
			checkDrive();
			Map<String, Map<String, Object>> map = new HashMap<String, Map<String, Object>>();
			Cursor cur = session.getDB().query("episode", new String[] { "series", "season", "episode" },
				"watched = 1", null, null, null, "series, season, episode");
			try {
				Episode ep;
				String sid = null;
				int sno = -1;
				List<Integer> eps = null;
				Map<String, Object> ser = null;
				while (cur.moveToNext()) {
					ep = Episode.get(this, cur.getString(0), cur.getInt(1), cur.getInt(2));
					if (ep == null)
						continue;
					if (!TextUtils.isEmpty(sid)) {
						if (sno != ep.season) {
							ser.put(Integer.toString(sno), eps);
							sno = ep.season;
							eps = new ArrayList<Integer>();
						}
						if (!ep.series.equals(sid)) {
							ser.put(Integer.toString(sno), eps);
							map.put(sid, ser);
							ser = new HashMap<String, Object>();
							ser.put("name", ep.getSeries().name);
							sid = ep.series;
							sno = ep.season;
							eps = new ArrayList<Integer>();
						}
					} else {
						ser = new HashMap<String, Object>();
						ser.put("name", ep.getSeries().name);
						sid = ep.series;
						sno = ep.season;
						eps = new ArrayList<Integer>();
					}
					eps.add(ep.episode);
				}
				if (ser != null) {
					ser.put(Integer.toString(sno), eps);
					map.put(sid, ser);
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				checkGenson();
				content = genson.serialize(map);
			}
			drive.writeFile(Commons.GD.SER_SEEN, content);
			session.setDriveLastFileUpdate(Commons.GD.SER_SEEN, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "backupSeriesWatched", err);
			throw err;
		}
	}
	
	private void checkRestoreMovie(String imdb_id) {
		Log.d(TAG, "checkRestoreMovie: " + imdb_id);
		Movie mov = Movie.get(this, imdb_id);
		if (mov.isNew() || mov.isOld())
			refreshMovie(imdb_id, false, true);
	}
	
	private void restoreMovieWatchlist() throws Exception {
		try {
			checkDrive();
			String content = drive.readFile(Commons.GD.MOV_WLST, session.driveLastFileUpdateUTC(Commons.GD.MOV_WLST));
			if (!TextUtils.isEmpty(content)) {
				checkGenson();
				Map<String, MovieWLST> map = genson.deserialize(content,
					new GenericType<Map<String, MovieWLST>>() {});
				if (map.isEmpty())
					return;
				SQLiteDatabase db = session.getDB();
				db.beginTransaction();
				try {
					Title.dispatch(OnTitleListener.WORKING, null);
					// reset all
					ContentValues cv = new ContentValues();
					cv.put("watchlist", false);
					cv.putNull("tags");
					db.update("movie", cv, null, null);
					// set new values
					MovieWLST jsw;
					for (String imdb_id : map.keySet()) {
						checkRestoreMovie(imdb_id);
						jsw = map.get(imdb_id);
						Log.d(TAG, "Setting watchlist for " + jsw.name);
						cv = new ContentValues();
						cv.put("watchlist", true);
						cv.put("tags", TextUtils.join(",", jsw.tags));
						db.update("movie", cv, "imdb_id=?", new String[] { imdb_id });
					}
					db.setTransactionSuccessful();
					session.setDriveLastFileUpdate(Commons.GD.MOV_WLST, System.currentTimeMillis());
					// refresh in memory items
					for (Movie m : Movie.cached())
						m.reload();
					Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreMovieWatchlist", err);
			throw err;
		}
	}
	
	private void restoreMovieCollection() throws Exception {
		try {
			checkDrive();
			String content = drive.readFile(Commons.GD.MOV_COLL, session.driveLastFileUpdateUTC(Commons.GD.MOV_COLL));
			if (!TextUtils.isEmpty(content)) {
				checkGenson();
				Map<String, MovieCOLL> map = genson.deserialize(content,
					new GenericType<Map<String, MovieCOLL>>() {});
				if (map.isEmpty())
					return;
				SQLiteDatabase db = session.getDB();
				db.beginTransaction();
				try {
					Title.dispatch(OnTitleListener.WORKING, null);
					// reset all
					ContentValues cv = new ContentValues();
					cv.put("collected", false);
					db.update("movie", cv, null, null);
					// set new values
					MovieCOLL jsw;
					for (String imdb_id : map.keySet()) {
						checkRestoreMovie(imdb_id);
						jsw = map.get(imdb_id);
						Log.d(TAG, "Setting collected for " + jsw.name);
						cv = new ContentValues();
						cv.put("collected", true);
						db.update("movie", cv, "imdb_id=?", new String[] { imdb_id });
					}
					db.setTransactionSuccessful();
					session.setDriveLastFileUpdate(Commons.GD.MOV_COLL, System.currentTimeMillis());
					// refresh in memory items
					for (Movie m : Movie.cached())
						m.reload();
					Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreMovieCollection", err);
			throw err;
		}
	}
	
	private void restoreMovieWatched() throws Exception {
		try {
			checkDrive();
			String content = drive.readFile(Commons.GD.MOV_SEEN, session.driveLastFileUpdateUTC(Commons.GD.MOV_SEEN));
			if (!TextUtils.isEmpty(content)) {
				checkGenson();
				Map<String, MovieSEEN> map = genson.deserialize(content,
					new GenericType<Map<String, MovieSEEN>>() {});
				if (map.isEmpty())
					return;
				SQLiteDatabase db = session.getDB();
				db.beginTransaction();
				try {
					Title.dispatch(OnTitleListener.WORKING, null);
					// reset all
					ContentValues cv = new ContentValues();
					cv.put("watched", false);
					cv.putNull("rating");
					db.update("movie", cv, null, null);
					// set new values
					MovieSEEN jsw;
					for (String imdb_id : map.keySet()) {
						checkRestoreMovie(imdb_id);
						jsw = map.get(imdb_id);
						Log.d(TAG, "Setting watched for " + jsw.name);
						cv = new ContentValues();
						cv.put("watched", true);
						cv.put("rating", jsw.rating);
						db.update("movie", cv, "imdb_id=?", new String[] { imdb_id });
					}
					db.setTransactionSuccessful();
					session.setDriveLastFileUpdate(Commons.GD.MOV_SEEN, System.currentTimeMillis());
					// refresh in memory items
					for (Movie m : Movie.cached())
						m.reload();
					Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreMovieWatched", err);
			throw err;
		}
	}
	
	private void checkRestoreSeries(String tvdb_id, Integer season, Integer episode) {
		Log.d(TAG, "checkRestoreSeries: " + tvdb_id);
		Series ser = Series.get(this, tvdb_id);
		if (ser.isNew() || ser.isOld())
			refreshSeries(tvdb_id, false, true);
		else if (season != null && !ser.seasons().contains(season))
			refreshSeries(tvdb_id, false, true);
		else if (season != null && episode != null) {
			Episode ep = ser.lastEpisode(season);
			if (ep == null || ep.episode < episode)
				refreshSeries(tvdb_id, false, true);
		}
	}
	
	private void restoreSeriesWatchlist() throws Exception {
		try {
			checkDrive();
			String content = drive.readFile(Commons.GD.SER_WLST, session.driveLastFileUpdateUTC(Commons.GD.SER_WLST));
			if (!TextUtils.isEmpty(content)) {
				checkGenson();
				Map<String, SeriesWLST> map = genson.deserialize(content,
					new GenericType<Map<String, SeriesWLST>>() {});
				if (map.isEmpty())
					return;
				SQLiteDatabase db = session.getDB();
				db.beginTransaction();
				try {
					Title.dispatch(OnTitleListener.WORKING, null);
					// reset all
					ContentValues cv = new ContentValues();
					cv.put("watchlist", false);
					cv.putNull("rating");
					cv.putNull("tags");
					db.update("series", cv, null, null);
					// set new values
					SeriesWLST jsw;
					for (String tvdb_id : map.keySet()) {
						checkRestoreSeries(tvdb_id, null, null);
						jsw = map.get(tvdb_id);
						Log.d(TAG, "Setting watchlist for " + jsw.name);
						cv = new ContentValues();
						cv.put("watchlist", true);
						cv.put("rating", jsw.rating);
						cv.put("tags", TextUtils.join(",", jsw.tags));
						db.update("series", cv, "tvdb_id=?", new String[] { tvdb_id });
					}
					db.setTransactionSuccessful();
					session.setDriveLastFileUpdate(Commons.GD.SER_WLST, System.currentTimeMillis());
					// refresh in memory items
					for (Series s : Series.cached())
						s.reload();
					Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreSeriesWatchlist", err);
			throw err;
		}
	}
	
	private void restoreSeriesCollection() throws Exception {
		try {
			checkDrive();
			String content = drive.readFile(Commons.GD.SER_COLL, session.driveLastFileUpdateUTC(Commons.GD.SER_COLL));
			if (!TextUtils.isEmpty(content)) {
				checkGenson();
				Map<String, String[]> map = genson.deserialize(content, new GenericType<Map<String, String[]>>() {
				});
				if (map.isEmpty())
					return;
				SQLiteDatabase db = session.getDB();
				db.beginTransaction();
				try {
					Title.dispatch(OnTitleListener.WORKING, null);
					// reset all
					ContentValues cv = new ContentValues();
					cv.put("collected", false);
					cv.putNull("subtitles");
					db.update("episode", cv, null, null);
					// set new values
					String[] tmp;
					String sid;
					int sno;
					int eno;
					for (String eid : map.keySet()) {
						tmp = eid.split("\\.");
						sid = tmp[0];
						sno = Integer.parseInt(tmp[1].substring(1, 3));
						eno = Integer.parseInt(tmp[1].substring(4, 6));
						tmp = map.get(eid); // subtitles
						checkRestoreSeries(sid, sno, eno);
						cv = new ContentValues();
						cv.put("collected", true);
						if (tmp != null && tmp.length > 0)
							cv.put("subtitles", TextUtils.join(",", tmp));
						db.update("episode", cv, "series=? and season=? and episode=?",
							new String[] { sid, Integer.toString(sno), Integer.toString(eno) });
					}
					db.setTransactionSuccessful();
					session.setDriveLastFileUpdate(Commons.GD.SER_COLL, System.currentTimeMillis());
					// refresh in memory items
					for (Series s : Series.cached())
						s.reloadEpisodes();
					Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreSeriesCollection", err);
			throw err;
		}
	}
	
	@SuppressWarnings("unchecked")
	private void restoreSeriesWatched() throws Exception {
		try {
			checkDrive();
			String content = drive.readFile(Commons.GD.SER_SEEN, session.driveLastFileUpdateUTC(Commons.GD.SER_SEEN));
			if (!TextUtils.isEmpty(content)) {
				checkGenson();
				Map<String, Map<String, Object>> map = genson.deserialize(content,
					new GenericType<Map<String, Map<String, Object>>>() {
					});
				if (map.isEmpty())
					return;
				SQLiteDatabase db = session.getDB();
				db.beginTransaction();
				try {
					Title.dispatch(OnTitleListener.WORKING, null);
					// reset all
					ContentValues cv = new ContentValues();
					cv.put("watched", false);
					db.update("episode", cv, null, null);
					// set new values
					Map<String, Object> cnt;
					List<Long> eps;
					int sno;
					for (String sid : map.keySet()) {
						cnt = map.get(sid);
						for (String key : cnt.keySet())
							if (!key.equals("name")) {
								sno = Integer.parseInt(key);
								try {
									eps = (List<Long>) cnt.get(key);
									for (Long eno : eps) {
										checkRestoreSeries(sid, sno, eno.intValue());
										cv = new ContentValues();
										cv.put("watched", true);
										db.update("episode", cv, "series=? and season=? and episode=?",
											new String[] { sid, Integer.toString(sno), Long.toString(eno) });
									}
								} catch (Exception err) {
									Log.e(TAG, "restoreSeriesWatched(): error on " + sid + ", season " + key, err);
								}
							}
					}
					db.setTransactionSuccessful();
					session.setDriveLastFileUpdate(Commons.GD.SER_SEEN, System.currentTimeMillis());
					// refresh in memory items
					for (Series s : Series.cached())
						s.reloadEpisodes();
					Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreSeriesWatched", err);
			throw err;
		}
	}
	
	static class TitleJSON {
		public String name;
	}
	
	static class MovieWLST extends TitleJSON {
		public String[] tags;
	}
	
	static class MovieCOLL extends TitleJSON {
		// nothing here.
	}
	
	static class MovieSEEN extends TitleJSON {
		public int rating;
	}
	
	static class SeriesWLST extends TitleJSON {
		public int rating;
		public String[] tags;
	}
}