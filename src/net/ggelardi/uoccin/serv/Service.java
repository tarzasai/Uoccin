package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.api.XML;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Episode.EID;
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
import com.google.api.services.drive.model.File;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

public class Service extends WakefulIntentService {
	private static final String TAG = "Service";
	
	private static final List<String> queue = new ArrayList<String>();
	
	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String GDRIVE_SYNC = "net.ggelardi.uoccin.GDRIVE_SYNC";
	public static final String GDRIVE_BACKUP = "net.ggelardi.uoccin.GDRIVE_BACKUP";
	public static final String GDRIVE_RESTORE = "net.ggelardi.uoccin.GDRIVE_RESTORE";
	
	public static boolean isQueued(String titleId) {
		return queue.contains(titleId);
	}
	
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
			} else if (act.equals(GDRIVE_BACKUP) && session.driveAccountSet()) {
				driveBackup();
			} else if (act.equals(GDRIVE_RESTORE) && session.driveAccountSet()) {
				driveRestore();
			}
		} catch (UserRecoverableAuthIOException err) {
			sendBroadcast(new Intent(Commons.SN.CONNECT_FAIL));
		} catch (Exception err) {
			sendNotification(err);
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
			String age = "timestamp < " + Long.toString(System.currentTimeMillis() - Commons.weekLong);
			// old stuff
			db.execSQL("delete from movie where " + age + " and watchlist = 0 and collected = 0 and watched = 0");
			db.execSQL("delete from series where " + age + " and watchlist = 0 and not tvdb_id in " +
				"(select distinct series from episode where collected = 1 or watched = 1)");
			// fields cleaning
			db.execSQL("update movie set subtitles = null where subtitles = ''");
			db.execSQL("update episode set subtitles = null where subtitles = ''");
			// done
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	private void refreshMovie(String imdb_id, boolean forceRefresh, boolean forceCommit) {
		if (isQueued(imdb_id))
			return;
		queue.add(imdb_id);
		try {
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
		} finally {
			queue.remove(imdb_id);
		}
	}
	
	private void refreshSeries(String tvdb_id, boolean forceRefresh, boolean forceCommit) {
		if (isQueued(tvdb_id))
			return;
		queue.add(tvdb_id);
		try {
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
			Series.get(this, (Element) doc.getElementsByTagName("Series").item(0),
				doc.getElementsByTagName("Episode"));
		} finally {
			queue.remove(tvdb_id);
		}
	}
	
	private void refreshEpisode(String series, int season, int episode) {
		String eid = new EID(series, season, episode).toString();
		if (isQueued(eid))
			return;
		queue.add(eid);
		try {
			Episode ep = Series.get(this, series).checkEpisode(season, episode);
			if (!(ep.isNew() || ep.isOld())) // TODO wifi check?
				return;
			Log.d(TAG, "refreshing episode " + ep.eid());
			Document doc;
			try {
				doc = XML.TVDB.getInstance().getEpisode(series, season, episode, session.language());
			} catch (Exception err) {
				Log.e(TAG, "refreshEpisode", err);
				if (!err.getLocalizedMessage().startsWith("404"))
					sendNotification(err);
				return;
			}
			ep.update((Element) doc.getElementsByTagName("Episode").item(0));
		} finally {
			queue.remove(eid);
		}
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
		// TODO: use XmlPullParser instead?
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
			Element node;
			EID check;
			Series ser;
			for (int i = 0; i < items.getLength(); i++) {
				link = Commons.XML.nodeText((Element) items.item(i), "link");
				try {
					String eid = Uri.parse(link).getQueryParameter("id");
					Document doc = XML.TVDB.getInstance().getEpisodeById(eid, "en");
					NodeList lst = doc.getElementsByTagName("Episode");
					if (lst != null && lst.getLength() > 0) {
						node = (Element) lst.item(0);
						check = new EID(node);
						if (check.isValid(session.specials()) && check.season == 1 && check.episode == 1) {
							doc = XML.TVDB.getInstance().getSeries(check.series, session.language());
							ser = Series.get(this, (Element)doc.getElementsByTagName("Series").item(0), null);
							Log.d(TAG, "Evalutaing series: " + ser.name);
							// check unwanted genres
							List<String> gflt = session.tvdbGenreFilter();
							if (gflt.size() <= 0 || ser.genres.size() <= 0) {
								boolean good = true;
								for (String g: ser.genres)
									if (gflt.contains(g.toLowerCase(Locale.getDefault()))) {
										good = false;
										break;
									}
								if (good) {
									String msg = ser.name + " (" + ser.genres() + ")";
									Log.d(TAG, "Tagging series: " + msg);
									ser.addTag(Series.TAG_DISCOVER);
									sendNotification(String.format(session.getString(R.string.msg_imdb_news), msg));
								}
							}
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
	
	private int loadDiff(String fileId) {
		String[] lines = null;
		try {
			lines = drive.readFile(fileId).split("\n");
		} catch (Exception err) {
			Log.e(TAG, "Error loading file " + fileId, err);
			return 0;
		}
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
				Log.d(TAG, Commons.logContentValue("Adding command", cv));
				session.getDB().insertOrThrow("queue_in", null, cv);
				res++;
			} catch (Exception err) {
				Log.e(TAG, "Error loading line: " + line, err);
			}
		return res;
	}
	
	private void saveMovie(ContentValues data) {
		SQLiteDatabase db = session.getDB();
		String cond = "imdb_id = ?";
		String[] args = new String[] { data.getAsString("imdb_id") };
		boolean chk;
		Cursor cur = db.query("movie", null, cond, args, null, null, null);
		try {
			chk = cur.moveToFirst();
		} finally {
			cur.close();
		}
		if (chk)
			db.update("movie", data, cond, args);
		else {
			data.put("name", "N/A");
			data.put("timestamp", 1);
			db.insertOrThrow("movie", null, data);
		}
	}
	
	private void saveSeries(ContentValues data) {
		SQLiteDatabase db = session.getDB();
		String cond = "tvdb_id = ?";
		String[] args = new String[] { data.getAsString("tvdb_id") };
		boolean chk;
		Cursor cur = db.query("series", null, cond, args, null, null, null);
		try {
			chk = cur.moveToFirst();
		} finally {
			cur.close();
		}
		if (!chk) {
			data.put("name", "N/A");
			data.put("timestamp", 1);
			db.insertOrThrow("series", null, data);
		} else if (data.size() > 1) // saveEpisode() check
			db.update("series", data, cond, args);
	}
	
	private void saveEpisode(ContentValues data) {
		SQLiteDatabase db = session.getDB();
		String cond = "series = ? and season = ? and episode = ?";
		String tvdb_id = data.getAsString("series");
		String[] args = new String[] { tvdb_id, data.getAsString("season"), data.getAsString("episode") };
		boolean chk;
		Cursor cur = db.query("episode", null, cond, args, null, null, null);
		try {
			chk = cur.moveToFirst();
		} finally {
			cur.close();
		}
		ContentValues series = new ContentValues();
		series.put("tvdb_id", tvdb_id);
		saveSeries(series);
		if (chk)
			db.update("episode", data, cond, args);
		else {
			data.put("timestamp", 1);
			db.insertOrThrow("episode", null, data);
		}
	}
	
	private void applyChange(long id, String target, String title, String field, String value) {
		try {
			ContentValues cv = new ContentValues();
			if (target.equals(Session.QUEUE_MOVIE)) {
				cv.put("imdb_id", title);
				if (field.equals("watchlist"))
					cv.put("watchlist", Boolean.parseBoolean(value));
				else if (field.equals("collected")) {
					boolean coll = Boolean.parseBoolean(value);
					cv.put("collected", coll);
					if (!coll)
						cv.putNull("subtitles");
				} else if (field.equals("watched"))
					cv.put("watched", Boolean.parseBoolean(value));
				else if (field.equals("rating")) {
					int rating = Integer.parseInt(value);
					cv.put("rating", rating);
					if (rating > 0)
						cv.put("watched", true);
				} else if (field.equals("tags"))
					cv.put("tags", value);
				else if (field.equals("subtitles"))
					cv.put("subtitles", value);
				else
					throw new Exception("Invalid field '" + field + "'");
				saveMovie(cv);
			} else if (target.equals(Session.QUEUE_SERIES)) {
				String[] parts = title.split("\\.");
				if (parts.length != 1 && parts.length != 3)
					throw new Exception("Invalid key '" + title + "'");
				if (field.equals("watchlist")) {
					cv.put("tvdb_id", title);
					cv.put("watchlist", Boolean.parseBoolean(value));
				} else if (field.equals("rating")) {
					cv.put("tvdb_id", title);
					cv.put("rating", Integer.parseInt(value));
				} else if (field.equals("tags")) {
					cv.put("tvdb_id", title);
					cv.put("tags", value);
				} else if (field.equals("collected")) {
					cv.put("series", parts[0]);
					cv.put("season", Integer.parseInt(parts[1]));
					cv.put("episode", Integer.parseInt(parts[2]));
					boolean coll = Boolean.parseBoolean(value);
					cv.put("collected", coll);
					if (!coll)
						cv.putNull("subtitles");
				} else if (field.equals("watched")) {
					cv.put("series", parts[0]);
					cv.put("season", Integer.parseInt(parts[1]));
					cv.put("episode", Integer.parseInt(parts[2]));
					cv.put("watched", Boolean.parseBoolean(value));
				} else if (field.equals("subtitles")) {
					cv.put("series", parts[0]);
					cv.put("season", Integer.parseInt(parts[1]));
					cv.put("episode", Integer.parseInt(parts[2]));
					cv.put("subtitles", value);
				} else
					throw new Exception("Invalid field '" + field + "'");
				if (cv.size() == 2)
					saveSeries(cv);
				else
					saveEpisode(cv);
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
			List<String> others = drive.getOtherFoldersIds();
			if (others.size() > 0) {
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
				if (sb.length() > 0) {
					String fn = SDF.timestamp(System.currentTimeMillis()) + "." + session.driveDeviceID() + ".diff";
					for (String fid: others)
						drive.writeFile(null, fid, fn, MIME.TEXT, sb.toString());
				}
			}
			db.delete("queue_out", null, null);
			// load other devices' diffs
			int lines = 0;
			for (String fid: drive.getNewDiffs()) {
				lines += loadDiff(fid);
				drive.deleteFile(fid); // yes, we might have found errors, so what?
			}
			if (lines <= 0) {
				Log.d(TAG, "Nothing to process, drive sync terminated.");
				return;
			}
			// apply changes
			Cursor qi = db.query("queue_in", null, null, null, null, null, "timestamp");
			try {
				while (qi.moveToNext()) {
					Log.d(TAG, Commons.logCursor("apply change", qi));
					applyChange(qi.getLong(0), qi.getString(1), qi.getString(2), qi.getString(3), qi.getString(4));
				}
			} finally {
				qi.close();
				db.delete("queue_in", null, null);
			}
			Title.dispatch(OnTitleListener.RELOAD, null);
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
								strlst = eps.isNull(4) ? new String[] {} : eps.getString(4).split(",\\s*");
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
			File bak = drive.getFile(Commons.GD.BACKUP, drive.getRootFolder(true), null);
			drive.writeFile(bak != null ? bak.getId() : null, drive.getRootFolder(true), Commons.GD.BACKUP,
				MIME.JSON, content);
		} catch (Exception err) {
			Log.e(TAG, "driveBackup", err);
			throw err;
		}
	}
	
	private void driveRestore() throws Exception {
		sendNotification(session.getString(R.string.msg_restore_1));
		SQLiteDatabase db = session.getDB();
		try {
			checkDrive();
			File bak = drive.getFile(Commons.GD.BACKUP, drive.getRootFolder(true), null);
			if (bak == null) {
				sendNotification(session.getString(R.string.msg_restore_2a));
				return;
			}
			String content = drive.readFile(bak);
			if (TextUtils.isEmpty(content)) {
				sendNotification(session.getString(R.string.msg_restore_2b));
				return;
			}
			checkGenson();
			UFile file = genson.deserialize(content, UFile.class);
			sendNotification(String.format(session.getString(R.string.msg_restore_3),
				file.movies.keySet().size(), file.series.keySet().size()));
			db.beginTransaction();
			try {
				ContentValues cv;
				// movies
				db.delete("movie", null, null);
				UMovie umo;
				for (String imdb_id: file.movies.keySet()) {
					umo = file.movies.get(imdb_id);
					cv = new ContentValues();
					cv.put("imdb_id", imdb_id);
					cv.put("name", umo.name);
					cv.put("watchlist", umo.watchlist);
					cv.put("collected", umo.collected);
					cv.put("watched", umo.watched);
					cv.put("timestamp", 1);
					if (umo.rating > 0)
						cv.put("rating", umo.rating);
					if (umo.tags != null && umo.tags.length > 0)
						cv.put("tags", TextUtils.join(",", umo.tags));
					if (umo.subtitles != null && umo.subtitles.length > 0)
						cv.put("subtitles", TextUtils.join(",", umo.subtitles));
					db.insertOrThrow("movie", null, cv);
				}
				// series
				db.delete("series", null, null);
				USeries use;
				List<String> chk;
				for (String tvdb_id: file.series.keySet()) {
					use = file.series.get(tvdb_id);
					cv = new ContentValues();
					cv.put("tvdb_id", tvdb_id);
					cv.put("name", use.name);
					cv.put("watchlist", use.watchlist);
					if (use.rating > 0)
						cv.put("rating", use.rating);
					if (use.tags != null && use.tags.length > 0)
						cv.put("tags", TextUtils.join(",", use.tags));
					cv.put("timestamp", 1);
					db.insertOrThrow("series", null, cv);
					// episodes
					chk = new ArrayList<String>();
					Map<String, String[]> smap;
					for (String season: use.collected.keySet()) {
						smap = use.collected.get(season);
						for (String episode: smap.keySet()) {
							cv = new ContentValues();
							cv.put("series", tvdb_id);
							cv.put("season", Integer.parseInt(season));
							cv.put("episode", Integer.parseInt(episode));
							cv.put("collected", true);
							Object[] tmp = smap.get(episode);
							if (tmp != null && tmp.length > 0)
								cv.put("subtitles", TextUtils.join(",", smap.get(episode)));
							cv.put("timestamp", 1);
							db.insertOrThrow("episode", null, cv);
							chk.add(season + "|" + episode);
						}
					}
					for (String season: use.watched.keySet())
						for (Integer episode: use.watched.get(season)) {
							cv = new ContentValues();
							cv.put("series", tvdb_id);
							cv.put("season", Integer.parseInt(season));
							cv.put("episode", episode);
							cv.put("watched", true);
							if (!chk.contains(season + "|" + episode.toString())) {
								cv.put("timestamp", 1);
								db.insertOrThrow("episode", null, cv);
							} else
								db.update("episode", cv, "series = ? and season = ? and episode = ?",
									new String[] { tvdb_id, season, episode.toString() });
						}
				}
				db.delete("queue_out", null, null);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			Movie.drop();
			Series.drop();
			sendNotification(session.getString(R.string.msg_restore_4));
			Title.dispatch(OnTitleListener.RELOAD, null);
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