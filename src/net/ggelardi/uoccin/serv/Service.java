package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ggelardi.uoccin.api.TNT;
import net.ggelardi.uoccin.api.XML;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.owlike.genson.GenericType;
import com.owlike.genson.Genson;
import com.owlike.genson.GensonBuilder;

public class Service extends IntentService {
	private static final String TAG = "Service";

	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String CHECK_GD_FOLDER = "net.ggelardi.uoccin.CHECK_GD_FOLDER";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String GDRIVE_BACKUP = "net.ggelardi.uoccin.GDRIVE_BACKUP";
	public static final String GDRIVE_RESTORE = "net.ggelardi.uoccin.GDRIVE_RESTORE";
	
	private Session session;
	private GoogleApiClient gdClient;
	private DriveFolder gdFolder;
	private Genson genson;
	
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
			session.getDB().execSQL("delete from series where watchlist = 0 and tags is null and " +
				"(rating is null or rating = 0) and not tvdb_id in (select distinct series from " +
				"episode where collected = 1 or watched = 1)");
		} else if (act.equals(CHECK_GD_FOLDER)) {
			
		} else if (act.equals(REFRESH_MOVIE)) {
			refreshMovie(intent.getExtras().getString("imdb_id"));
		} else if (act.equals(REFRESH_SERIES)) {
			refreshSeries(intent.getExtras().getString("tvdb_id"));
		} else if (act.equals(REFRESH_EPISODE)) {
			Bundle extra = intent.getExtras();
			String series = extra.getString("series");
			int season = extra.getInt("season");
			int episode = extra.getInt("episode");
			refreshEpisode(series, season, episode);
		} else if (act.equals(CHECK_TVDB_RSS)) {
			checkTVdbNews();
		} else if (act.equals(GDRIVE_BACKUP) && session.gDriveBackup()) {
			Bundle extra = intent.getExtras();
			String what = extra == null ? "*" : extra.getString("what");
			if (what.equals("*") || what.equals(Commons.DRIVE.MOV_WLST))
				backupMovieWatchlist();
			if (what.equals("*") || what.equals(Commons.DRIVE.MOV_COLL))
				backupMovieCollection();
			if (what.equals("*") || what.equals(Commons.DRIVE.MOV_SEEN))
				backupMovieWatched();
			if (what.equals("*") || what.equals(Commons.DRIVE.SER_WLST))
				backupSeriesWatchlist();
			if (what.equals("*") || what.equals(Commons.DRIVE.SER_COLL))
				backupSeriesCollection();
			if (what.equals("*") || what.equals(Commons.DRIVE.SER_SEEN))
				backupSeriesWatched();
		} else if (act.equals(GDRIVE_RESTORE) && session.gDriveBackup()) {
			Bundle extra = intent.getExtras();
			String what = extra == null ? "*" : extra.getString("what");
			if (what.equals("*") || what.equals(Commons.DRIVE.MOV_WLST))
				restoreMovieWatchlist();
			if (what.equals("*") || what.equals(Commons.DRIVE.MOV_COLL))
				restoreMovieCollection();
			if (what.equals("*") || what.equals(Commons.DRIVE.MOV_SEEN))
				restoreMovieWatched();
			if (what.equals("*") || what.equals(Commons.DRIVE.SER_WLST))
				restoreSeriesWatchlist();
			if (what.equals("*") || what.equals(Commons.DRIVE.SER_COLL))
				restoreSeriesCollection();
			if (what.equals("*") || what.equals(Commons.DRIVE.SER_SEEN))
				restoreSeriesWatched();
		}
		stopSelf();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (gdClient != null)
			gdClient.disconnect();
	}
	
	private void refreshMovie(String imdb_id) {
		Movie mov = Movie.get(this, imdb_id);
		if (!(mov.isNew() || mov.isOld())) // TODO wifi check?
			return;
		Log.v(TAG, "refreshing movie " + imdb_id);
		//
	}
	
	private void refreshSeries(String tvdb_id) {
		Series ser = Series.get(this, tvdb_id);
		if (!(ser.isNew() || ser.isOld())) // TODO wifi check?
			return;
		Log.v(TAG, "refreshing series " + tvdb_id);
		final boolean commit = ser.isNew() && (ser.inWatchlist() || ser.getRating() > 0 || ser.hasTags());
		try {
			Document doc = XML.TVDB.getInstance().sync_getFullSeries(tvdb_id, session.language());
			Series.get(this, (Element) doc.getElementsByTagName("Series").item(0));
			// episodes
			NodeList lst = doc.getElementsByTagName("Episode");
			if (lst != null && lst.getLength() > 0) {
				Episode ep;
				for (int i = 0; i < lst.getLength(); i++) {
					ep = Episode.get(this, (Element) lst.item(i));
					if (ep != null && !ser.episodes.contains(ep))
						ser.episodes.add(ep);
				}
				Collections.sort(ser.episodes, new Episode.EpisodeComparator());
			}
			// save it?
			if (!ser.isNew() || commit)
				ser.commit();
			else
				Series.dispatch(OnTitleListener.READY, null);
		} catch (Exception err) {
			Log.e(TAG, "refreshSeries", err);
			Series.dispatch(OnTitleListener.ERROR, err);
		}
	}
	
	private void refreshEpisode(String series, int season, int episode) {
		Episode epi = Episode.get(this, series, season, episode);
		if (!(epi.isNew() || epi.isOld())) // TODO wifi check?
			return;
		Log.v(TAG, "refreshing episode " + epi.standardEID());
		try {
			Document doc = XML.TVDB.getInstance().sync_getEpisode(series, season, episode, session.language());
			epi = Episode.get(this, (Element) doc.getElementsByTagName("Episode").item(0));
			if (epi != null)
				epi.commit();
		} catch (Exception err) {
			Log.e(TAG, "refreshSeries", err);
			Episode.dispatch(OnTitleListener.ERROR, err);
		}
	}
	
	private void checkTVdbNews() {
		try {
			List<String> links = new TNT().getLinks();
			for (String url : links) {
				try {
					String eid = Uri.parse(url).getQueryParameter("id");
					Document doc = XML.TVDB.getInstance().sync_getEpisodeById(eid, "en");
					NodeList lst = doc.getElementsByTagName("Episode");
					if (lst != null && lst.getLength() > 0) {
						Episode ep = Episode.get(session.getContext(), (Element) lst.item(0));
						if (ep != null && ep.season == 1 && ep.episode == 1 && !ep.getSeries().inWatchlist())
							ep.getSeries().addTag("premiere");
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
	
	private void initGenson() {
		if (genson != null)
			return;
		genson = new GensonBuilder().useIndentation(true).create();
	}
	
	private void initDriveAPI() throws Exception {
		if (gdClient == null) {
			gdClient = new GoogleApiClient.Builder(session.getContext()).addApi(Drive.API).
				addScope(Drive.SCOPE_FILE).build();
			ConnectionResult cr = gdClient.blockingConnect();
			if (!cr.isSuccess())
				throw new Exception(cr.toString());
		}
		if (gdFolder == null) {
			MetadataBufferResult br = Drive.DriveApi.query(gdClient, new Query.Builder().addFilter(
				Filters.eq(SearchableField.TITLE, Commons.DRIVE.FOLDER)).build()).await();
			if (!br.getStatus().isSuccess())
				throw new Exception(br.getStatus().getStatusMessage());
			MetadataBuffer mb = br.getMetadataBuffer();
			try {
				for (int i = 0; i < mb.getCount(); i++)
					if (!mb.get(i).isTrashed()) {
						gdFolder = Drive.DriveApi.getFolder(gdClient, mb.get(i).getDriveId());
						break;
					}
			} finally {
				mb.release();
			}
			if (gdFolder == null) {
				MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(Commons.DRIVE.FOLDER).build();
				DriveFolderResult fr = Drive.DriveApi.getRootFolder(gdClient).createFolder(gdClient, cs).await();
				if (!fr.getStatus().isSuccess())
					throw new Exception(fr.getStatus().getStatusMessage());
				gdFolder = fr.getDriveFolder();
			}
		}
	}
	
	private DriveFileCombo getDriveFile(String name, boolean create) throws Exception {
		DriveFileCombo res = new DriveFileCombo();
		MetadataBufferResult br = gdFolder.queryChildren(gdClient, new Query.Builder().addFilter(
			Filters.eq(SearchableField.TITLE, name)).build()).await();
		if (!br.getStatus().isSuccess())
			throw new Exception(br.getStatus().getStatusMessage());
		MetadataBuffer mb = br.getMetadataBuffer();
		try {
			for (int i = 0; i < mb.getCount(); i++)
				if (!mb.get(i).isTrashed()) {
					res.driveFile = Drive.DriveApi.getFile(gdClient, mb.get(i).getDriveId());
					res.metadata = mb.get(i);
					break;
				}
		} finally {
			mb.release();
		}
		if (res.driveFile == null && create) {
			MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(name).
				setMimeType("application/json").build();
			DriveContentsResult cr = Drive.DriveApi.newDriveContents(gdClient).await();
			DriveFileResult fr = gdFolder.createFile(gdClient, cs, cr.getDriveContents()).await();
			if (!fr.getStatus().isSuccess())
				throw new Exception(fr.getStatus().getStatusMessage());
			res.driveFile = fr.getDriveFile();
			res.metadata = null;
		}
		return res;
	}
	
	private String readDriveContent(DriveFile df) throws Exception {
		DriveContentsResult cr = df.open(gdClient, DriveFile.MODE_READ_ONLY, null).await();
		if (!cr.getStatus().isSuccess())
			throw new Exception(cr.getStatus().getStatusMessage());
		DriveContents dc = cr.getDriveContents();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(dc.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				builder.append(line);
			return builder.toString();
		} finally {
			dc.discard(gdClient);
		}
	}
	
	private void writeDriveContent(DriveFile df, String content) throws Exception {
        DriveContentsResult cr = df.open(gdClient, DriveFile.MODE_WRITE_ONLY, null).await();
        if (!cr.getStatus().isSuccess())
        	throw new Exception(cr.getStatus().getStatusMessage());
        DriveContents dc = cr.getDriveContents();
        OutputStream stream = dc.getOutputStream();
        stream.write(content.getBytes());
        Status status = dc.commit(gdClient, null).await();
        if (!status.getStatus().isSuccess())
        	throw new Exception(status.getStatus().getStatusMessage());
	}
	
	private void backupMovieWatchlist() {
	}
	
	private void restoreMovieWatchlist() {
	}
	
	private void backupMovieCollection() {
	}
	
	private void restoreMovieCollection() {
	}
	
	private void backupMovieWatched() {
	}
	
	private void restoreMovieWatched() {
	}
	
	private void backupSeriesWatchlist() {
		try {
			initDriveAPI();
			DriveFileCombo dc = getDriveFile(Commons.DRIVE.SER_WLST, true);
			if (dc == null)
				return;
			Map<String, JsonSerWlst> map = new HashMap<String, JsonSerWlst>();
			Cursor cur = session.getDB().query("series", new String[] { "tvdb_id" }, "watchlist = 1", null, null, null,
				"name collate nocase", null);
			try {
				Series ser;
				JsonSerWlst obj;
				while (cur.moveToNext()) {
					ser = Series.get(this, cur.getString(0));
					obj = new JsonSerWlst();
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
				initGenson();
				content = genson.serialize(map);
			}
			Log.i(TAG, "Saving " + Commons.DRIVE.SER_WLST + " (" + content.getBytes().length + " bytes)...");
			writeDriveContent(dc.driveFile, content);
		} catch (Exception err) {
			Log.e(TAG, "backupSeriesWatchlist", err);
			// TODO: notification
		}
	}
	
	private void restoreSeriesWatchlist() {
		try {
			initDriveAPI();
			DriveFileCombo dc = getDriveFile(Commons.DRIVE.SER_WLST, true);
			if (dc == null)
				return;
			String content = readDriveContent(dc.driveFile);
			if (!TextUtils.isEmpty(content)) {
				initGenson();
				Map<String, JsonSerWlst> map = genson.deserialize(content, new GenericType<Map<String, JsonSerWlst>>(){});
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
					db.update("series", cv, null, null);
					// set new values
					JsonSerWlst jsw;
					Series ser;
					for (String tvdb_id: map.keySet()) {
						jsw = map.get(tvdb_id);
						Log.v(TAG, "Setting watchlist for " + jsw.name);
						cv = new ContentValues();
						cv.put("watchlist", true);
						cv.put("tags", TextUtils.join(",", jsw.tags));
						if (db.update("series", cv, "tvdb_id=?", new String[] { tvdb_id }) < 1) {
							refreshSeries(tvdb_id);
							ser = Series.get(this, tvdb_id);
							ser.setWatchlist(true);
							ser.setTags(jsw.tags);
							ser.setRating(jsw.rating);
						}
					}
				    db.setTransactionSuccessful();
				    // refresh in memory items
				    for (Series s: Series.cached())
				    	s.reload();
				    Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreSeriesWatchlist", err);
			// TODO: notification
		}
	}
	
	private void backupSeriesCollection() {
		try {
			initDriveAPI();
			DriveFileCombo dc = getDriveFile(Commons.DRIVE.SER_COLL, true);
			if (dc == null)
				return;
			Map<String, String[]> map = new HashMap<String, String[]>();
			Cursor cur = session.getDB().query("episode", new String[] { "series", "season", "episode" },
				"collected = 1", null, null, null, "series, season, episode");
			try {
				Episode ep;
				while (cur.moveToNext()) {
					ep = Episode.get(this, cur.getString(0), cur.getInt(1), cur.getInt(2));
					if (ep != null)
						map.put(ep.extendedEID(), ep.subtitles.toArray(new String[ep.subtitles.size()]));
				}
			} finally {
				cur.close();
			}
			String content = "{}";
			if (!map.isEmpty()) {
				initGenson();
				content = genson.serialize(map);
			}
			Log.i(TAG, "Saving " + Commons.DRIVE.SER_COLL + " (" + content.getBytes().length + " bytes)...");
			writeDriveContent(dc.driveFile, content);
		} catch (Exception err) {
			Log.e(TAG, "backupSeriesCollection", err);
			// TODO: notification
		}
	}
	
	private void restoreSeriesCollection() {
		try {
			initDriveAPI();
			DriveFileCombo dc = getDriveFile(Commons.DRIVE.SER_COLL, true);
			if (dc == null)
				return;
			String content = readDriveContent(dc.driveFile);
			if (!TextUtils.isEmpty(content)) {
				initGenson();
				Map<String, String[]> map = genson.deserialize(content, new GenericType<Map<String, String[]>>(){});
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
					Episode ep;
					for (String eid: map.keySet()) {
						tmp = eid.split("\\.");
						sid = tmp[0];
						sno = Integer.parseInt(tmp[1].substring(1, 3));
						eno = Integer.parseInt(tmp[1].substring(4, 6));
						tmp = map.get(eid); // subtitles
						cv = new ContentValues();
						cv.put("collected", true);
						if (tmp != null && tmp.length > 0)
							cv.put("subtitles", TextUtils.join(",", tmp));
						if (db.update("episode", cv, "series=? and season=? and episode=?", new String[] {
							sid, Integer.toString(sno), Integer.toString(eno)
						}) < 1) {
							refreshEpisode(sid, sno, eno);
							ep = Episode.get(this, sid, sno, eno);
							if (ep != null && !TextUtils.isEmpty(ep.tvdb_id)) {
								ep.subtitles = new ArrayList<String>(Arrays.asList(tmp));
								ep.setCollected(true); // after subtitles
							}
						}
					}
				    db.setTransactionSuccessful();
				    // refresh in memory items
				    for (Series s: Series.cached())
				    	s.reloadEpisodes();
				    Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreSeriesCollection", err);
			// TODO: notification
		}
	}
	
	private void backupSeriesWatched() {
		try {
			initDriveAPI();
			DriveFileCombo dc = getDriveFile(Commons.DRIVE.SER_SEEN, true);
			if (dc == null)
				return;
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
				initGenson();
				content = genson.serialize(map);
			}
			Log.i(TAG, "Saving " + Commons.DRIVE.SER_SEEN + " (" + content.getBytes().length + " bytes)...");
			writeDriveContent(dc.driveFile, content);
		} catch (Exception err) {
			Log.e(TAG, "backupSeriesWatched", err);
			// TODO: notification
		}
	}
	
	private void restoreSeriesWatched() {
		try {
			initDriveAPI();
			DriveFileCombo dc = getDriveFile(Commons.DRIVE.SER_SEEN, true);
			if (dc == null)
				return;
			String content = readDriveContent(dc.driveFile);
			if (!TextUtils.isEmpty(content)) {
				initGenson();
				Map<String, Map<String, Object>> map = genson.deserialize(content, new GenericType<Map<String,
					Map<String, Object>>>(){});
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
					Episode ep;
					for (String sid: map.keySet()) {
						cnt = map.get(sid);
						for (String key: cnt.keySet())
							if (!key.equals("name")) {
								sno = Integer.parseInt(key);
								try {
									eps = (List<Long>) cnt.get(key);
									for (Long eno: eps) {
										cv = new ContentValues();
										cv.put("watched", true);
										if (db.update("episode", cv, "series=? and season=? and episode=?",
											new String[] { sid, Integer.toString(sno), Long.toString(eno) }) < 1) {
											refreshEpisode(sid, sno, eno.intValue());
											ep = Episode.get(this, sid, sno, eno.intValue());
											if (ep != null && !TextUtils.isEmpty(ep.tvdb_id))
												ep.setWatched(true);
										}
									}
								} catch (Exception err) {
									Log.e(TAG, "restoreSeriesWatched(): error on " + sid + ", season " + key, err);
								}
							}
					}
				    db.setTransactionSuccessful();
				    // refresh in memory items
				    for (Series s: Series.cached())
				    	s.reloadEpisodes();
				    Title.dispatch(OnTitleListener.READY, null);
				} finally {
					db.endTransaction();
				}
			}
		} catch (Exception err) {
			Log.e(TAG, "restoreSeriesWatched", err);
			// TODO: notification
		}
	}
	
	static class DriveFileCombo {
		DriveFile driveFile;
		Metadata metadata;
	}
	
	static class JsonSerWlst {
		public String name;
		public int rating;
		public String[] tags;
	}
}