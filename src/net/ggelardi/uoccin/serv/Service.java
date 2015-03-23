package net.ggelardi.uoccin.serv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import net.ggelardi.uoccin.api.TNT;
import net.ggelardi.uoccin.api.XML;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title.OnTitleListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

public class Service extends IntentService {
	private static final String TAG = "Service";

	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String CHECK_GD_FOLDER = "net.ggelardi.uoccin.CHECK_GD_FOLDER";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String CREATE_BACKUP = "net.ggelardi.uoccin.CREATE_BACKUP";
	public static final String RESTORE_BACKUP = "net.ggelardi.uoccin.RESTORE_BACKUP";
	
	private Session session;
	private GoogleApiClient gdClient;
	private DriveFolder gdFolder;
	
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
		} else if (act.equals(CREATE_BACKUP)) {
			//
		} else if (act.equals(RESTORE_BACKUP) && session.gDriveBackup()) {
			restoreMovieWatchlist();
			restoreMovieCollection();
			restoreMovieWatched();
			restoreSeriesWatchlist();
			restoreEpisodesCollection();
			restoreEpisodesWatched();
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
			Episode.get(this, (Element) doc.getElementsByTagName("Episode").item(0)).commit();
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
	
	private boolean initDriveAPI() {
		if (gdClient == null) {
			gdClient = new GoogleApiClient.Builder(session.getContext()).addApi(Drive.API).
				addScope(Drive.SCOPE_FILE).build();
			if (!gdClient.blockingConnect().isSuccess()) {
				// notify error
				return false;
			}
		}
		if (gdFolder == null) {
			MetadataBufferResult br = Drive.DriveApi.query(gdClient, new Query.Builder().addFilter(
				Filters.eq(SearchableField.TITLE, Commons.DRIVE.FOLDER)).build()).await();
			if (!br.getStatus().isSuccess()) {
				// notify error
				return false;
			}
			MetadataBuffer mb = br.getMetadataBuffer();
			if (mb.getCount() > 0) {
				gdFolder = Drive.DriveApi.getFolder(gdClient, mb.get(0).getDriveId());
				// check if trashed
				MetadataResult mr = gdFolder.getMetadata(gdClient).await();
				if (!mr.getStatus().isSuccess()) {
					// notify error
					return false;
				}
				Metadata md = mr.getMetadata();
				if (!md.isTrashed())
					gdFolder = null;
			}
			if (gdFolder == null) {
				MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(Commons.DRIVE.FOLDER).build();
				DriveFolderResult fr = Drive.DriveApi.getRootFolder(gdClient).createFolder(gdClient, cs).await();
				if (!fr.getStatus().isSuccess()) {
					// notify error
					return false;
				}
				gdFolder = fr.getDriveFolder();
			}
		}
		return true;
	}
	
	private DriveFile getDriveFile(String name, boolean create) {
		MetadataBufferResult br = gdFolder.queryChildren(gdClient, new Query.Builder().addFilter(
			Filters.eq(SearchableField.TITLE, name)).build()).await();
		if (!br.getStatus().isSuccess()) {
			// notify error
			return null;
		}
		MetadataBuffer mb = br.getMetadataBuffer();
		if (mb.getCount() > 0)
			return Drive.DriveApi.getFile(gdClient, mb.get(0).getDriveId());
		if (!create)
			return null;
		// new file
		MetadataChangeSet cs = new MetadataChangeSet.Builder().setTitle(name).
			setMimeType("application/json").build();
		DriveContentsResult cr = Drive.DriveApi.newDriveContents(gdClient).await();
		DriveFileResult fr = gdFolder.createFile(gdClient, cs, cr.getDriveContents()).await();
		if (!fr.getStatus().isSuccess()) {
			// notify error
			return null;
		}
		return fr.getDriveFile();
	}
	
	private String readDriveContent(DriveFile df) {
		DriveContentsResult cr = df.open(gdClient, DriveFile.MODE_READ_ONLY, null).await();
		if (!cr.getStatus().isSuccess()) {
			// notify error
			return null;
		}
		DriveContents dc = cr.getDriveContents();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(dc.getInputStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				return builder.toString();
			} catch (IOException err) {
				Log.e(TAG, "Error while reading " + df.getDriveId().encodeToString(), err);
				return null;
			}
		} finally {
			dc.discard(gdClient);
		}
	}
	
	private void writeDriveContent(DriveFile df, String content) {
		//
	}
	
	private void backupMovieWatchlist() {
		if (!initDriveAPI())
			return;
		DriveFile df = getDriveFile(Commons.DRIVE.MOV_WLST, true);
		if (df == null)
			return;
		String content = null;
		
		// ???
		
		writeDriveContent(df, content);
	}
	
	private void restoreMovieWatchlist() {
		if (!initDriveAPI())
			return;
		DriveFile df = getDriveFile(Commons.DRIVE.MOV_WLST, false);
		if (df == null) {
			Log.i(TAG, "File " + Commons.DRIVE.MOV_WLST + " does not exists, skipping...");
			return;
		}
		String content = readDriveContent(df);
		if (TextUtils.isEmpty(content)) {
			Log.i(TAG, "File " + Commons.DRIVE.MOV_WLST + " is empty, skipping...");
			return;
		}

		
		// http://stackoverflow.com/questions/16590377/custom-json-deserializer-using-gson
		
		
	}
	
	private void restoreMovieCollection() {
	}
	
	private void restoreMovieWatched() {
	}
	
	private void restoreSeriesWatchlist() {
	}
	
	private void restoreEpisodesCollection() {
	}
	
	private void restoreEpisodesWatched() {
	}
}