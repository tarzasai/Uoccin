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
import net.ggelardi.uoccin.serv.Commons.MIME;
import net.ggelardi.uoccin.serv.Commons.SDF;
import net.ggelardi.uoccin.serv.Service.UFile.UMovie;
import net.ggelardi.uoccin.serv.Service.UFile.USeries;

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
import com.google.api.services.drive.model.File;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

public class Service extends WakefulIntentService {
	private static final String TAG = "Service";
	
	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String GDRIVE_SYNC = "net.ggelardi.uoccin.GDRIVE_SYNC";
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
			if (TextUtils.isEmpty(act)) {
				session.registerAlarms();
			} else if (act.equals(CLEAN_DB_CACHE)) {
				cleanDBCache();
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
			} else if (act.equals(GDRIVE_SYNC) && session.driveSyncEnabled()) {
				driveSync();
			} else if (act.equals(GDRIVE_BACKUP) && session.driveSyncEnabled()) {
				driveBackup();
			} else if (act.equals(GDRIVE_RESTORE) && session.driveSyncEnabled()) {
				driveRestore();
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
	
	private void cleanDBCache() {
		Log.d(TAG, "cleaning database...");
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			db.delete("movie", "watchlist = 0 and collected = 0 and watched = 0", null);
			db.execSQL("update movie set subtitles = null where subtitles = ''");
			db.execSQL("delete from series where watchlist = 0 and not tvdb_id in " +
				"(select distinct series from episode where collected = 1 or watched = 1)");
			db.execSQL("update episode set subtitles = null where subtitles = ''");
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
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
			mov.commit();
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
			ser.commit();
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
			epi.commit();
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
			genson = new GensonBuilder().setSkipNull(true).
				//useIndentation(true). // DEBUG ONLY!
				create();
	}
	
	private int loadDiff(File file) {
		String[] lines = null;
		try {
			lines = drive.readFile(file).split("\n");
		} catch (Exception err) {
			Log.e(TAG, "Error loading file " + file.getTitle(), err);
			return 0;
		}
		Log.d(TAG, "Loading " + Integer.toString(lines.length) + " commands from " + file.getTitle());
		ContentValues cv;
		String[] fields;
		int res = 0;
		for (String line: lines)
			try {
				fields = line.split("\\x7C");
				cv = new ContentValues();
				cv.put("timestamp", Long.parseLong(fields[0]));
				cv.put("target", fields[1]);
				cv.put("title", fields[2]);
				cv.put("field", fields[3]);
				cv.put("value", fields[4]);
				session.getDB().insertOrThrow("queue_in", null, cv);
				res++;
			} catch (Exception err) {
				Log.e(TAG, "Error loading line: " + line, err);
			}
		return res;
	}
	
	private void checkRestoreMovie(String imdb_id) {
		Log.d(TAG, "checkRestoreMovie: " + imdb_id);
		Movie mov = Movie.get(this, imdb_id);
		if (mov.isNew() || mov.isOld())
			refreshMovie(imdb_id, false, true);
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
	
	private void applyChange(long id, String target, String title, String field, String value) {
		SQLiteDatabase db = session.getDB();
		try {
			ContentValues cv = new ContentValues();
			cv.put("timestamp", System.currentTimeMillis());
			if (target.equals(Session.QUEUE_MOVIE)) {
				checkRestoreMovie(title);
				if (field.equals("watchlist"))
					cv.put("watchlist", Boolean.parseBoolean(value));
				else if (field.equals("collected"))
					cv.put("collected", Boolean.parseBoolean(value));
				else if (field.equals("watched"))
					cv.put("watched", Boolean.parseBoolean(value));
				else if (field.equals("rating"))
					cv.put("rating", Integer.parseInt(value));
				else if (field.equals("tags"))
					cv.put("tags", value);
				else if (field.equals("subtitles"))
					cv.put("subtitles", value);
				else
					throw new Exception("Invalid field '" + field + "'");
				db.update("movie", cv, "imdb_id = ?", new String[] { title });
				Movie.get(this, title).reload();
			} else if (target.equals(Session.QUEUE_SERIES)) {
				String[] parts = title.split("\\.");
				checkRestoreSeries(parts[0], parts.length > 1 ? Integer.parseInt(parts[1]) : null,
					parts.length > 2 ? Integer.parseInt(parts[2]) : null);
				String table = "watchlist|rating|tags".contains(field) ? "series" : "episode";
				String where = table.equals("series") ? "tvdb_id = ?" : "series = ?";
				List<String> args = new ArrayList<String>();
				args.add(parts[0]);
				if (table.equals("episode")) {
					if (parts.length > 1) {
						where += " and season = ?";
						args.add(parts[1]);
					}
					if (parts.length > 2) {
						where += " and episode = ?";
						args.add(parts[2]);
					}
				}
				if (field.equals("watchlist"))
					cv.put("watchlist", Boolean.parseBoolean(value));
				else if (field.equals("rating"))
					cv.put("rating", Integer.parseInt(value));
				else if (field.equals("tags"))
					cv.put("tags", value);
				else if (field.equals("collected"))
					cv.put("collected", Boolean.parseBoolean(value));
				else if (field.equals("watched"))
					cv.put("watched", Boolean.parseBoolean(value));
				else if (field.equals("subtitles"))
					cv.put("subtitles", value);
				else
					throw new Exception("Invalid field '" + field + "'");
				db.update(table, cv, where, args.toArray(new String[args.size()]));
				Series ser = Series.get(this, parts[0]);
				ser.reload();
				ser.reloadEpisodes();
			}
		} catch (Exception err) {
			Log.e(TAG, "Error on command " + Long.toString(id), err);
		}
	}
	
	private void driveSync() throws Exception {
		SQLiteDatabase db = session.getDB();
		try {
			checkDrive();
			// write diff
			StringBuilder sb = new StringBuilder();
			Cursor qo = db.query("queue_out", null, null, null, null, null, "timestamp");
			try {
				while (qo.moveToNext()) {
					sb.append(qo.getLong(0)).append('|');
					sb.append(qo.getString(1)).append('|');
					sb.append(qo.getString(2)).append('|');
					sb.append(qo.getString(3)).append('|');
					sb.append(qo.getString(4)).append("\n");
				}
			} finally {
				qo.close();
			}
			String fn = "diff." + SDF.timestamp(System.currentTimeMillis()) + "." + session.driveDeviceID();
			drive.writeFile(null, fn, MIME.TEXT, sb.toString());
			db.delete("queue_in", null, null);
			// load other devices' diffs
			int lines = 0;
			File file;
			for (Change ch: drive.getChanges()) {
				file = ch.getFile();
				if (file.getTitle().startsWith("diff.") && !file.getTitle().endsWith(session.driveDeviceID()))
					lines += loadDiff(file);
			}
			if (lines <= 0) {
				Log.d(TAG, "Nothing to process, drive sync terminated.");
				return;
			}
			// apply changes
			Cursor qi = db.query("queue_in", null, null, null, null, null, "timestamp");
			try {
				while (qi.moveToNext())
					applyChange(qi.getLong(0), qi.getString(1), qi.getString(2), qi.getString(3), qi.getString(4));
			} finally {
				qi.close();
				db.delete("queue_in", null, null);
			}
		} catch (Exception err) {
			Log.e(TAG, "driveSync", err);
			throw err;
		}
	}
	
	private void driveBackup() throws Exception {
		SQLiteDatabase db = session.getDB();
		try {
			checkDrive();
			UFile file = new UFile();
			file.movies = new HashMap<String, Service.UFile.UMovie>();
			file.series = new HashMap<String, Service.UFile.USeries>();
			// movies
			Cursor cur = db.query("movie", new String[] { "imdb_id", "name", "watchlist", "collected",
				"watched", "rating", "tags", "subtitles" }, "watchlist = 1 or collected = 1 or watched = 1",
				null, null, null, "name collate nocase", null);
			try {
				UMovie mov;
				String mid;
				while (cur.moveToNext()) {
					mid = cur.getString(0);
					mov = new UMovie();
					mov.name = cur.getString(1);
					mov.watchlist = cur.getInt(2) == 1;
					mov.collected = cur.getInt(3) == 1;
					mov.watched = cur.getInt(4) == 1;
					if (!cur.isNull(5))
						mov.rating = cur.getInt(5);
					if (!cur.isNull(6))
						mov.tags = cur.getString(6).split(",\\s*");
					if (!cur.isNull(7))
						mov.subtitles = cur.getString(7).split(",\\s*");
					file.movies.put(mid, mov);
				}
			} finally {
				cur.close();
			}
			// series
			cur = db.query("series", new String[] { "tvdb_id", "name", "watchlist", "rating", "tags" },
				null, null, null, null, "name collate nocase", null);
			try {
				USeries ser;
				String sid;
				Cursor eps;
				int season;
				int episode;
				boolean coll;
				boolean seen;
				String tmp;
				String[] strlst;
				List<Integer> intlst;
				while (cur.moveToNext()) {
					sid = cur.getString(0);
					ser = new USeries();
					ser.name = cur.getString(1);
					ser.watchlist = cur.getInt(2) == 1;
					if (!cur.isNull(3))
						ser.rating = cur.getInt(3);
					if (!cur.isNull(4))
						ser.tags = cur.getString(4).split(",\\s*");
					// episodes
					ser.collected = new HashMap<String, Map<String,String[]>>();
					ser.watched = new HashMap<String, List<Integer>>();
					eps = db.query("episode", new String[] { "season", "episode", "collected", "watched",
						"subtitles" }, "series = ? and (collected = 1 or watched = 1)", new String[] { sid },
						null, null, "season, episode");
					try {
						while (eps.moveToNext()) {
							season = eps.getInt(0);
							episode = eps.getInt(1);
							tmp = Integer.toString(season);
							coll = eps.getInt(2) == 1;
							seen = eps.getInt(3) == 1;
							if (coll) {
								if (!ser.collected.containsKey(tmp))
									ser.collected.put(tmp, new HashMap<String, String[]>());
								strlst = eps.isNull(4) ? new String[] {} : cur.getString(7).split(",\\s*");
								ser.collected.get(tmp).put(Integer.toString(episode), strlst);
							}
							if (seen) {
								if (!ser.watched.containsKey(tmp))
									ser.watched.put(tmp, new ArrayList<Integer>());
								intlst = ser.watched.get(tmp);
								if (!intlst.contains(episode))
									intlst.add(episode);
							}
						}
					} finally {
						eps.close();
					}
					if (ser.watchlist || !ser.collected.isEmpty() || !ser.watched.isEmpty())
						file.series.put(sid, ser);
				}
			} finally {
				cur.close();
			}
			checkGenson();
			String content = genson.serialize(file);
			File bak = drive.getFile(Commons.GD.BACKUP, null);
			drive.writeFile(bak != null ? bak.getId() : null, Commons.GD.BACKUP, MIME.JSON, content);
			//session.setDriveLastFileUpdate(Commons.GD.BACKUP, System.currentTimeMillis());
		} catch (Exception err) {
			Log.e(TAG, "driveBackup", err);
			throw err;
		}
	}
	
	private void driveRestore() throws Exception {
		SQLiteDatabase db = session.getDB();
		try {
			checkDrive();
			File bak = drive.getFile(Commons.GD.BACKUP, null);
			if (bak == null)
				return;
			String content = drive.readFile(bak);
			if (TextUtils.isEmpty(content))
				return;
			checkGenson();
			UFile file = genson.deserialize(content, UFile.class);
			db.beginTransaction();
			try {
				// movies
				ContentValues cv = new ContentValues();
				cv.put("watchlist", 0);
				cv.put("collected", 0);
				cv.put("watched", 0);
				cv.putNull("rating");
				cv.putNull("tags");
				cv.putNull("subtitles");
				db.update("movie", cv, null, null);
				UMovie umo;
				for (String imdb_id: file.movies.keySet()) {
					checkRestoreMovie(imdb_id);
					umo = file.movies.get(imdb_id);
					cv = new ContentValues();
					cv.put("watchlist", umo.watchlist);
					cv.put("collected", umo.collected);
					cv.put("watched", umo.watched);
					if (umo.rating <= 0)
						cv.putNull("rating");
					else
						cv.put("rating", umo.rating);
					if (umo.tags == null || umo.tags.length <= 0)
						cv.putNull("tags");
					else
						cv.put("tags", TextUtils.join(",", umo.tags));
					if (umo.subtitles == null || umo.subtitles.length <= 0)
						cv.putNull("subtitles");
					else
						cv.put("subtitles", TextUtils.join(",", umo.subtitles));
					db.update("movie", cv, "imdb_id = ?", new String[] { imdb_id });
					Movie.get(this, imdb_id).reload();
				}
				// series
				cv = new ContentValues();
				cv.put("watchlist", 0);
				cv.putNull("rating");
				cv.putNull("tags");
				db.update("series", cv, null, null);
				cv = new ContentValues();
				cv.put("collected", 0);
				cv.put("watched", 0);
				cv.putNull("subtitles");
				db.update("episode", cv, null, null);
				USeries use;
				Series ser;
				String[] subs;
				for (String tvdb_id: file.series.keySet()) {
					checkRestoreSeries(tvdb_id, null, null);
					use = file.series.get(tvdb_id);
					cv = new ContentValues();
					cv.put("watchlist", use.watchlist);
					if (use.rating <= 0)
						cv.putNull("rating");
					else
						cv.put("rating", use.rating);
					if (use.tags == null || use.tags.length <= 0)
						cv.putNull("tags");
					else
						cv.put("tags", TextUtils.join(",", use.tags));
					db.update("series", cv, "tvdb_id = ?", new String[] { tvdb_id });
					// episodes
					ser = Series.get(this, tvdb_id);
					ser.reload();
					ser.reloadEpisodes();
					int changes = 0;
					Map<String, String[]> smap;
					for (String season: use.collected.keySet()) {
						int sno = Integer.parseInt(season);
						smap = use.collected.get(season);
						for (String episode: smap.keySet()) {
							int eno = Integer.parseInt(episode);
							if (ser.checkEpisode(sno, eno) != null) {
								cv = new ContentValues();
								cv.put("collected", true);
								Object[] chk = smap.get(episode);
								if (chk != null && chk.length > 0) {
									/*
									subs = smap.get(episode);
									cv.put("subtitles", TextUtils.join(",", subs));
									*/
									cv.put("subtitles", TextUtils.join(",", smap.get(episode)));
								}
								db.update("episode", cv, "series = ? and season = ? and episode = ?",
									new String[] { tvdb_id, season, episode });
								changes++;
							}
						}
					}
					for (String season: use.watched.keySet()) {
						int sno = Integer.parseInt(season);
						for (Integer eno: use.watched.get(season)) {
							if (ser.checkEpisode(sno, eno) != null) {
								cv = new ContentValues();
								cv.put("watched", true);
								db.update("episode", cv, "series = ? and season = ? and episode = ?",
									new String[] { tvdb_id, season, eno.toString() });
								changes++;
							}
						}
					}
					/*if (changes > 0)
						ser.reloadEpisodes();*/
				}
				db.delete("queue_out", null, null);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			
			for (Movie mov: Movie.cached())
				mov.reload();
			for (Series ser: Series.cached()) {
				ser.reload();
				ser.reloadEpisodes();
			}
			
		} catch (Exception err) {
			Log.e(TAG, "driveRestore", err);
			throw err;
		}
	}
	
	public static class UFile {
		public Map<String, UMovie> movies;
		public Map<String, USeries> series;
	
		public static class UMovie {
			public String name;
			public boolean watchlist;
			public boolean collected;
			public boolean watched;
			public int rating;
			public String[] tags;
			public String[] subtitles;
		}
		
		public static class USeries {
			public String name;
			public boolean watchlist;
			public int rating;
			public String[] tags;
			public Map<String, Map<String, String[]>> collected;
			public Map<String, List<Integer>> watched;
		}
	}
}