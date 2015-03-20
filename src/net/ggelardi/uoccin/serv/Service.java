package net.ggelardi.uoccin.serv;

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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

public class Service extends IntentService implements ConnectionCallbacks, OnConnectionFailedListener {
	private static final String TAG = "Service";
	
	public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
	public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
	public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
	public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
	public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
	public static final String RESTORE_BACKUP = "net.ggelardi.uoccin.RESTORE_BACKUP";
	
	private Session session;
	private GoogleApiClient gdClient;
	
	public Service() {
		super("Service");
	}
	
	// https://developers.google.com/drive/android/events
	
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
		} else if (act.equals(RESTORE_BACKUP) && session.gDriveBackup()) {
			if (gdClient == null)
				gdClient = new GoogleApiClient.Builder(this).addApi(Drive.API).addScope(Drive.SCOPE_FILE).
					addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
			gdClient.connect();
		}
		stopSelf();
	}
	
	@Override
	public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected.");
        try {
			restoreMovieWatchlist();
			restoreMovieCollection();
			restoreMovieWatched();
			restoreSeriesWatchlist();
			restoreEpisodesCollection();
			restoreEpisodesWatched();
        } finally {
        	gdClient.disconnect();
        }
	}
	
	@Override
	public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended.");
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
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
	
	private void restoreMovieWatchlist() {
		
		// http://stackoverflow.com/questions/27108178/google-drive-api-android-how-to-obtain-a-drive-file-id
		// https://developers.google.com/drive/android/queries
		
		Query query = new Query.Builder().addFilter(Filters.eq(SearchableField.TITLE, "movies.watchlist.json")).build();
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