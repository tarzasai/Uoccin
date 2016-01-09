package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.ggelardi.uoccin.BuildConfig;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.api.XML;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Episode.EID;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.Commons.MT;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Commons.SN;
import net.ggelardi.uoccin.serv.Commons.SR;
import net.ggelardi.uoccin.serv.Service.UFile.UMovie;
import net.ggelardi.uoccin.serv.Service.UFile.USeries;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
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
			} else if (act.equals(SR.CLEAN_DB_CACHE)) {
				cleanDBCache();
			} else if (act.equals(SR.REFRESH_MOVIE)) {
				Bundle extra = intent.getExtras();
				String imdb_id = extra.getString("imdb_id");
				boolean forced = extra.getBoolean("forced", false);
				refreshMovie(imdb_id, forced);
			} else if (act.equals(SR.REFRESH_SERIES)) {
				Bundle extra = intent.getExtras();
				String tvdb_id = extra.getString("tvdb_id");
				boolean forced = extra.getBoolean("forced", false);
				refreshSeries(tvdb_id, forced);
			} else if (act.equals(SR.REFRESH_EPISODE)) {
				Bundle extra = intent.getExtras();
				String series = extra.getString("series");
				int season = extra.getInt("season");
				int episode = extra.getInt("episode");
				boolean forced = extra.getBoolean("forced", false);
				refreshEpisode(series, season, episode, forced);
			} else if (act.equals(SR.CHECK_TVDB_RSS)) {
				checkTVDB();
			} else if (act.equals(SR.GDRIVE_SYNCNOW)) {
				driveSync();
			} else if (act.equals(SR.GDRIVE_BACKUP)) {
				driveBackup();
			} else if (act.equals(SR.GDRIVE_RESTORE)) {
				driveRestore();
			}
		} catch (UserRecoverableAuthIOException err) {
			sendBroadcast(new Intent(SN.CONNECT_FAIL));
		} catch (Exception err) {
			sendNotification(err);
		}
	}
	
	private void sendNotification(String what) {
		Intent si = new Intent(SN.GENERAL_INFO);
		si.putExtra("what", what);
		sendBroadcast(si);
	}
	
	private void sendNotification(Throwable what) {
		Intent si = new Intent(SN.GENERAL_FAIL);
		si.putExtra("what", what.getLocalizedMessage());
		sendBroadcast(si);
	}
	
	private void cleanDBCache() {
		Log.d(TAG, "cleaning database...");
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			String age = "createtime < " + Long.toString(System.currentTimeMillis() - Commons.days(7));
			// old stuff
			db.execSQL("delete from movie where " + age + " and watchlist = 0 and collected = 0 and watched = 0");
			db.execSQL("delete from series where " + age + " and watchlist = 0 and not tvdb_id in " +
				"(select distinct series from episode where collected = 1 or watched = 1)");
			// fields cleaning
			db.execSQL("update movie set subtitles = null where subtitles = ''");
			db.execSQL("update episode set subtitles = null where subtitles = ''");
			// specials
			if (!session.specials())
				db.execSQL("delete from episode where season <= 0 or episode <= 0");
			// done
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			Title.dispatch(OnTitleListener.RELOAD, null);
		}
	}
	
	private void refreshMovie(String imdb_id, boolean forceRefresh) {
		if (isQueued(imdb_id) || !session.isConnected())
			return;
		if (!(forceRefresh || !session.autorefrWifiOnly() || session.isOnWIFI()))
			return;
		queue.add(imdb_id);
		try {
			Movie mov = Movie.get(this, imdb_id);
			if (!(forceRefresh || mov.isNew() || mov.isOld()))
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
	
	private void refreshSeries(String tvdb_id, boolean forceRefresh) {
		if (isQueued(tvdb_id) || !session.isConnected())
			return;
		if (!(forceRefresh || !session.autorefrWifiOnly() || session.isOnWIFI()))
			return;
		queue.add(tvdb_id);
		try {
			Series ser = Series.get(this, tvdb_id);
			if (!(forceRefresh || ser.isNew() || ser.isOld()))
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
	
	private void refreshEpisode(String series, int season, int episode, boolean forceRefresh) {
		String eid = new EID(series, season, episode).toString();
		if (isQueued(eid) || !session.isConnected())
			return;
		if (!(forceRefresh || !session.autorefrWifiOnly() || session.isOnWIFI()))
			return;
		queue.add(eid);
		try {
			Episode ep = Series.get(this, series).checkEpisode(season, episode);
			if (!(ep.isNew() || ep.isOld()))
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
	
	private void checkTVDB() throws Exception {
		if (!(session.isOnWIFI() && (Commons.olderThan(session.tvdbLastCheck(), Commons.hours(6)) || BuildConfig.DEBUG)))
			return;
		List<String> genFilter = session.tvdbGenreFilter();
		List<String> netFilter = session.tvdbNetworkFilter();
		Log.d(TAG, "checkTVdbNews() begin (unwanted genres: " + TextUtils.join(", ", genFilter) +
			" - unwanted networks: " + TextUtils.join(", ", netFilter) + ")");
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
			/*int nPrems = 0;
			int nGoods = 0;*/
			for (int i = 0; i < items.getLength(); i++) {
				link = Commons.XML.nodeText((Element) items.item(i), "link");
				try {
					String eid = Uri.parse(link).getQueryParameter("id");
					Document doc = XML.TVDB.getInstance().getEpisodeById(eid, "en");
					NodeList lst = doc.getElementsByTagName("Episode");
					if (lst == null || lst.getLength() <= 0)
						continue;
					node = (Element) lst.item(0);
					check = new EID(node);
					Log.d(TAG, "Evaluating episode " + check.toString() + " -> " + link);
					if (!check.isValid(session.specials()) || check.season != 1 || check.episode != 1)
						continue;
					//nPrems++;
					Cursor cur = session.getDB().query("series", null, "tvdb_id = ?", new String[] { check.series },
						null, null, null);
					try {
						if (cur.moveToFirst()) {
							Log.d(TAG, "Skipping because it's not new to us.");
							continue;
						}
					} finally {
						cur.close();
					}
					doc = XML.TVDB.getInstance().getSeries(check.series, session.language());
					ser = Series.get(this, (Element)doc.getElementsByTagName("Series").item(0), null);
					Log.d(TAG, "Evaluating series \"" + ser.name + "\"");
					boolean good = true;
					// check genres (or not)
					if (!genFilter.isEmpty()) {
						if (ser.genres.isEmpty()) {
							Log.d(TAG, "Skipping because it's undefined.");
							good = false;
						} else
							for (String g: ser.genres)
								if (genFilter.contains(g.toLowerCase(Locale.getDefault()))) {
									Log.d(TAG, "Skipping because it's a " + g);
									good = false;
									break;
								}
					}
					if (good && !netFilter.isEmpty())
						for (String n: netFilter)
							if (ser.network().toLowerCase(Locale.getDefault()).contains(n)) {
								Log.d(TAG, "Skipping because it's aired by " + ser.network());
								good = false;
								break;
							}
					if (good) {
						//nGoods++;
						String text = ser.name + " (" + ser.genres() + ")";
						Log.d(TAG, "Tagging series: " + text);
						ser.addTag(Series.TAG_DISCOVER);
						// notification
						Intent notif = new Intent(SN.SER_PREM);
						notif.putExtra("tvdb_id", ser.tvdb_id);
						notif.putExtra("name", text);
						notif.putExtra("plot", ser.plot);
						notif.putExtra("poster", ser.poster);
						sendBroadcast(notif);
					}
				} catch (Exception err) {
					Log.e(TAG, link, err);
				}
			}
			/*if (BuildConfig.DEBUG) {
				Intent notif = new Intent(SN.DBG_TVDB_RSS);
				notif.putExtra("tot", items.getLength());
				notif.putExtra("chk", nPrems);
				notif.putExtra("oks", nGoods);
				sendBroadcast(notif);
			}*/
		}
		SharedPreferences.Editor editor = session.getPrefs().edit();
		editor.putLong(PK.TVDBLAST, System.currentTimeMillis());
		editor.commit();
		Log.d(TAG, "checkTVdbNews() end");
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
	
	private void saveMovie(ContentValues data, String tags) {
		Set<String> tagSet = new HashSet<String>(Arrays.asList((tags != null ? tags : "").split(",")));
		if (tagSet.isEmpty() && data.size() <= 1)
			return; // wtf?
		SQLiteDatabase db = session.getDB();
		String cond = "imdb_id = ?";
		String[] flds = new String[] { "name", "watchlist", "collected" };
		String[] args = new String[] { data.getAsString("imdb_id") };
		boolean chk;
		String name = null;
		boolean wlst = false;
		boolean coll = false;
		Cursor cur = db.query("movie", flds, cond, args, null, null, null);
		try {
			chk = cur.moveToFirst();
			if (chk) {
				name = cur.getString(0);
				wlst = cur.getInt(1) == 1;
				coll = cur.getInt(2) == 1;
			}
		} finally {
			cur.close();
		}
		// insert or update the title
		if (chk) {
			if (data.size() > 1) // tags check
				db.update("movie", data, cond, args);
			Movie.setUpdated(args[0]);
		} else {
			data.put("name", "N/A");
			data.put("timestamp", 1);
			data.put("createtime", System.currentTimeMillis());
			db.insertOrThrow("movie", null, data);
		}
		if (!tagSet.isEmpty()) {
			db.delete("movtag", "movie = ?", args);
			ContentValues cv;
			for (String tag: tagSet) {
				cv = new ContentValues();
				cv.put("movie", args[0]);
				cv.put("tag", tag.trim());
				db.insertOrThrow("movtag", null, cv);
			}
		}
		// notify the user
		Intent notif = null;
		if (data.containsKey("watchlist") && data.getAsBoolean("watchlist") && !wlst && session.notifyMovWlst())
			notif = new Intent(SN.MOV_WLST);
		else if (data.containsKey("collected") && data.getAsBoolean("collected") && !coll && session.notifyMovColl())
			notif = new Intent(SN.MOV_COLL);
		if (notif != null) {
			notif.putExtra("imdb_id", args[0]);
			if (!TextUtils.isEmpty(name))
				notif.putExtra("name", name);
			sendBroadcast(notif);
		}
	}
	
	private void saveSeries(ContentValues data, String tags) {
		Set<String> tagSet = new HashSet<String>(Arrays.asList((tags != null ? tags : "").split(",")));
		if (tagSet.isEmpty() && data.size() <= 1)
			return; // wtf?
		SQLiteDatabase db = session.getDB();
		String cond = "tvdb_id = ?";
		String[] flds = new String[] { "name", "watchlist" };
		String[] args = new String[] { data.getAsString("tvdb_id") };
		boolean chk;
		String name = null;
		boolean wlst = false;
		Cursor cur = db.query("series", flds, cond, args, null, null, null);
		try {
			chk = cur.moveToFirst();
			if (chk) {
				name = cur.getString(0);
				wlst = cur.getInt(1) == 1;
			}
		} finally {
			cur.close();
		}
		// insert or update the title
		if (chk) {
			if (data.size() > 1) // tags and saveEpisode() check
				db.update("series", data, cond, args);
			Series.setUpdated(args[0]);
		} else {
			data.put("name", "N/A");
			data.put("timestamp", 1);
			data.put("createtime", System.currentTimeMillis());
			db.insertOrThrow("series", null, data);
		}
		if (!tagSet.isEmpty()) {
			db.delete("sertag", "series = ?", args);
			ContentValues cv;
			for (String tag: tagSet) {
				cv = new ContentValues();
				cv.put("series", args[0]);
				cv.put("tag", tag.trim());
				db.insertOrThrow("sertag", null, cv);
			}
		}
		// notify the user
		if (data.containsKey("watchlist") && data.getAsBoolean("watchlist") && !wlst && session.notifySerWlst()) {
			Intent notif = new Intent(SN.SER_WLST);
			notif.putExtra("tvdb_id", args[0]);
			if (!TextUtils.isEmpty(name))
				notif.putExtra("name", name);
			sendBroadcast(notif);
		}
	}
	
	private void saveEpisode(ContentValues data) {
		SQLiteDatabase db = session.getDB();
		String cond = "series = ? and season = ? and episode = ?";
		String tvdb_id = data.getAsString("series");
		String[] flds = new String[] { "name", "collected" };
		String[] args = new String[] { tvdb_id, data.getAsString("season"), data.getAsString("episode") };
		boolean chk;
		String name = null;
		boolean coll = false;
		Cursor cur = db.query("episode", flds, cond, args, null, null, null);
		try {
			chk = cur.moveToFirst();
			if (chk) {
				name = cur.getString(0);
				coll = cur.getInt(1) == 1;
			}
		} finally {
			cur.close();
		}
		// check to insert the master record (series)
		ContentValues series = new ContentValues();
		series.put("tvdb_id", tvdb_id);
		saveSeries(series, null);
		// insert or update the title
		if (chk)
			db.update("episode", data, cond, args);
		else {
			data.put("timestamp", 1);
			db.insertOrThrow("episode", null, data);
		}
		// notify the user
		if (data.containsKey("collected") && data.getAsBoolean("collected") && !coll && session.notifySerColl()) {
			Intent notif = new Intent(SN.SER_COLL);
			notif.putExtra("series", args[0]);
			notif.putExtra("season", data.getAsInteger("season"));
			notif.putExtra("episode", data.getAsInteger("episode"));
			if (!(TextUtils.isEmpty(name) || name.equals("N/A")))
				notif.putExtra("name", name);
			sendBroadcast(notif);
		}
	}
	
	private void applyChange(long id, String target, String title, String field, String value) {
		try {
			ContentValues cv = new ContentValues();
			String tags = null;
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
				} else if (field.equals("subtitles"))
					cv.put("subtitles", value);
				else if (field.equals("tags"))
					tags = value;
				else
					throw new Exception("Invalid field '" + field + "'");
				saveMovie(cv, tags);
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
					tags = value;
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
				if (cv.size() <= 2)
					saveSeries(cv, tags);
				else
					saveEpisode(cv);
			}
		} catch (Exception err) {
			Log.e(TAG, "Error on command " + Long.toString(id), err);
		}
	}
	
	private void driveSync() throws Exception {
		if (!session.driveSyncEnabled() || !session.isConnected() || (session.driveSyncWifiOnly() && !session.isOnWIFI()))
			return;
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
					String fn = Long.toString(System.currentTimeMillis()) + "." + session.driveDeviceID() + ".diff";
					for (String fid: others)
						drive.writeFile(null, fid, fn, MT.TEXT, sb.toString());
				}
			}
			db.delete("queue_out", null, null);
			// load other devices' diffs
			int lines = 0;
			for (String fid: drive.getNewDiffs()) {
				lines += loadDiff(fid); // ignore errors
				try {
					drive.deleteFile(fid);
				} catch (Exception err) {
					Log.e(TAG, "drive.deleteFile() failed", err); // ignore errors
				}
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
		if (!(session.driveAccountSet() && session.isConnected()))
			return;
		SQLiteDatabase db = session.getDB();
		try {
			checkDrive();
			UFile file = new UFile();
			file.movies = new HashMap<String, Service.UFile.UMovie>();
			file.series = new HashMap<String, Service.UFile.USeries>();
			Cursor cur;
			// movies
			cur = db.query("movie", new String[] { "imdb_id", "name", "watchlist", "collected", "watched", "rating",
				"subtitles" }, "watchlist = 1 or collected = 1 or watched = 1", null, null, null,
				"name collate nocase", null);
			try {
				UMovie mov;
				String mid;
				while (cur.moveToNext()) {
					mid = cur.getString(0);
					mov = new UMovie();
					mov.read(db, mid, cur);
					file.movies.put(mid, mov);
				}
			} finally {
				cur.close();
			}
			// series
			cur = db.query("series", new String[] { "tvdb_id", "name", "watchlist", "rating" }, null, null, null, null,
				"name collate nocase", null);
			try {
				USeries ser;
				String sid;
				while (cur.moveToNext()) {
					sid = cur.getString(0);
					ser = new USeries();
					ser.read(db, sid, cur);
					if (ser.watchlist || !ser.collected.isEmpty() || !ser.watched.isEmpty())
						file.series.put(sid, ser);
				}
			} finally {
				cur.close();
			}
			checkGenson();
			String content = genson.serialize(file);
			File bak = drive.getFile(GSA.BACKUP, drive.getRootFolderId(true), null);
			drive.writeFile(bak != null ? bak.getId() : null, drive.getRootFolderId(true), GSA.BACKUP,
				MT.JSON, content);
		} catch (Exception err) {
			Log.e(TAG, "driveBackup", err);
			throw err;
		}
	}
	
	private void driveRestore() throws Exception {
		if (!(session.driveAccountSet() && session.isConnected()))
			return;
		sendNotification(session.getString(R.string.msg_restore_1));
		SQLiteDatabase db = session.getDB();
		try {
			checkDrive();
			File bak = drive.getFile(GSA.BACKUP, drive.getRootFolderId(true), null);
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
				db.delete("movie", null, null);
				for (String imdb_id: file.movies.keySet())
					file.movies.get(imdb_id).write(db, imdb_id);
				db.delete("series", null, null);
				for (String tvdb_id: file.series.keySet())
					file.series.get(tvdb_id).write(db, tvdb_id);
				db.delete("queue_out", null, null);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			Movie.resetCache();
			Series.resetCache();
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
			
			public void read(SQLiteDatabase db, String imdb_id, Cursor cur) {
				name = cur.getString(1);
				watchlist = cur.getInt(2) == 1;
				collected = cur.getInt(3) == 1;
				watched = cur.getInt(4) == 1;
				if (!cur.isNull(5))
					rating = cur.getInt(5);
				if (!cur.isNull(6))
					subtitles = cur.getString(6).split(",\\s*");
				// tags
				List<String> list = new ArrayList<String>();
				Cursor cr = db.query(true, "movtag", new String[] { "tag" }, "movie = ?", new String[] { imdb_id },
					null, null, "tag", null);
				try {
					while (cr.moveToNext())
						list.add(cr.getString(0));
				} finally {
					cr.close();
				}
				tags = list.isEmpty() ? null : list.toArray(new String[list.size()]);
			}
			
			public void write(SQLiteDatabase db, String imdb_id) {
				ContentValues cv = new ContentValues();
				cv.put("imdb_id", imdb_id);
				cv.put("name", name);
				cv.put("watchlist", watchlist);
				cv.put("collected", collected);
				cv.put("watched", watched);
				cv.put("timestamp", 1);
				cv.put("createtime", System.currentTimeMillis());
				if (rating > 0)
					cv.put("rating", rating);
				if (subtitles != null && subtitles.length > 0)
					cv.put("subtitles", TextUtils.join(",", subtitles));
				db.insertOrThrow("movie", null, cv);
				db.delete("movtag", "movie = ?", new String[] { imdb_id });
				if (tags != null)
					for (String tag: tags) {
						cv = new ContentValues();
						cv.put("movie", imdb_id);
						cv.put("tag", tag.trim());
						db.insertOrThrow("movtag", null, cv);
					}
			}
		}
		
		public static class USeries {
			public String name;
			public boolean watchlist;
			public int rating;
			public String[] tags;
			public Map<String, Map<String, String[]>> collected;
			public Map<String, List<Integer>> watched;
			
			public void read(SQLiteDatabase db, String tvdb_id, Cursor cur) {
				name = cur.getString(1);
				watchlist = cur.getInt(2) == 1;
				if (!cur.isNull(3))
					rating = cur.getInt(3);
				// episodes
				collected = new HashMap<String, Map<String,String[]>>();
				watched = new HashMap<String, List<Integer>>();
				Cursor eps = db.query("episode", new String[] { "season", "episode", "collected", "watched",
					"subtitles" }, "series = ? and (collected = 1 or watched = 1)", new String[] { tvdb_id },
					null, null, "season, episode");
				try {
					int season;
					int episode;
					boolean coll;
					boolean seen;
					String tmp;
					List<Integer> intlst;
					while (eps.moveToNext()) {
						season = eps.getInt(0);
						episode = eps.getInt(1);
						tmp = Integer.toString(season);
						coll = eps.getInt(2) == 1;
						seen = eps.getInt(3) == 1;
						if (coll) {
							if (!collected.containsKey(tmp))
								collected.put(tmp, new HashMap<String, String[]>());
							collected.get(tmp).put(Integer.toString(episode),
								(eps.isNull(4) ? new String[] {} : eps.getString(4).split(",\\s*")));
						}
						if (seen) {
							if (!watched.containsKey(tmp))
								watched.put(tmp, new ArrayList<Integer>());
							intlst = watched.get(tmp);
							if (!intlst.contains(episode))
								intlst.add(episode);
						}
					}
				} finally {
					eps.close();
				}
				// tags
				if (watchlist || !collected.isEmpty() || !watched.isEmpty()) {
					List<String> list = new ArrayList<String>();
					Cursor cr = db.query(true, "sertag", new String[] { "tag" }, "series = ?",
						new String[] { tvdb_id }, null, null, "tag", null);
					try {
						while (cr.moveToNext())
							list.add(cr.getString(0));
					} finally {
						cr.close();
					}
					tags = list.isEmpty() ? null : list.toArray(new String[list.size()]);
				}
			}
			
			public void write(SQLiteDatabase db, String tvdb_id) {
				ContentValues cv = new ContentValues();
				cv.put("tvdb_id", tvdb_id);
				cv.put("name", name);
				cv.put("watchlist", watchlist);
				cv.put("timestamp", 1);
				cv.put("createtime", System.currentTimeMillis());
				if (rating > 0)
					cv.put("rating", rating);
				db.insertOrThrow("series", null, cv);
				// tags
				if (tags != null)
					for (String tag: tags) {
						cv = new ContentValues();
						cv.put("series", tvdb_id);
						cv.put("tag", tag.trim());
						db.insertOrThrow("sertag", null, cv);
					}
				// episodes
				List<String> chk = new ArrayList<String>();
				Map<String, String[]> smap;
				for (String season: collected.keySet()) {
					smap = collected.get(season);
					for (String episode: smap.keySet()) {
						cv = new ContentValues();
						cv.put("series", tvdb_id);
						cv.put("season", Integer.parseInt(season));
						cv.put("episode", Integer.parseInt(episode));
						cv.put("collected", true);
						cv.put("timestamp", 1);
						Object[] tmp = smap.get(episode);
						if (tmp != null && tmp.length > 0)
							cv.put("subtitles", TextUtils.join(",", smap.get(episode)));
						db.insertOrThrow("episode", null, cv);
						chk.add(season + "|" + episode);
					}
				}
				for (String season: watched.keySet())
					for (Integer episode: watched.get(season)) {
						cv = new ContentValues();
						cv.put("series", tvdb_id);
						cv.put("season", Integer.parseInt(season));
						cv.put("episode", episode);
						cv.put("watched", true);
						if (chk.contains(season + "|" + episode.toString()))
							db.update("episode", cv, "series = ? and season = ? and episode = ?",
								new String[] { tvdb_id, season, episode.toString() });
						else {
							cv.put("timestamp", 1);
							db.insertOrThrow("episode", null, cv);
						}
					}
			}
		}
	}
}