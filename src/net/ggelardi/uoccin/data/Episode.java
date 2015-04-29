package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;
import net.ggelardi.uoccin.serv.SimpleCache;

import org.w3c.dom.Element;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Episode extends Title {
	private static final String TAG = "Episode";
	private static final String TABLE = "episode";
	
	private static final SimpleCache cache = new SimpleCache(1000);
	
	private Episode epprev = null;
	private Episode epnext = null;
	private boolean collected = false;
	private boolean watched = false;
	
	public String tvdb_id;
	public String series; // series tvdb_id
	public int season;
	public int episode;
	public String name;
	public String plot;
	public String poster;
	public List<String> writers = new ArrayList<String>();
	public String director;
	public List<String> guestStars = new ArrayList<String>();
	public long firstAired;
	public String imdb_id;
	public List<String> subtitles = new ArrayList<String>();
	public long timestamp = 0;
	//public boolean modified = false;
	
	public Episode(Context context, String series, int season, int episode) {
		this.session = Session.getInstance(context);
		this.series = series;
		this.season = season;
		this.episode = episode;
	}
	
	private static synchronized Episode getInstance(Context context, String series, int season, int episode) {
		EID eid = new EID(series, season, episode);
		Object tmp = cache.get(eid.toString());
		if (tmp != null) {
			Log.v(TAG, "Episode " + eid + " found in cache");
			return (Episode) tmp;
		}
		Episode res = new Episode(context, series, season, episode);
		cache.add(eid.toString(), res);
		Cursor cur = Session.getInstance(context).getDB().query("episode", null, "series=? and season=? and episode=?",
			new String[] { series, Integer.toString(season), Integer.toString(episode) }, null, null, null);
		try {
			if (cur.moveToFirst()) {
				res.load(cur);
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static void drop() {
		cache.clear();
	}
	
	public static synchronized void drop(String series, Integer season, Integer episode) {
		Object tmp;
		Episode ep;
		for (String k: cache.getKeys()) {
			tmp = cache.get(k);
			if (tmp != null) {
				ep = (Episode) tmp;
				if (ep.series.equals(series) && (season == null || ep.season == season) &&
					(episode == null || ep.episode == episode))
					cache.del(k);
			}
		}
	}
	
	public static Episode get(Context context, String series, int season, int episode) {
		Episode res = Episode.getInstance(context, series, season, episode);
		if (res.isNew() || res.isOld())
			res.refresh(false);
		return res;
	}
	
	public static Episode get(Context context, Element xml) {
		Episode res = null;
		try {
			String sid = xml.getElementsByTagName("seriesid").item(0).getTextContent();
			int sen = Integer.parseInt(xml.getElementsByTagName("SeasonNumber").item(0).getTextContent());
			int epn = Integer.parseInt(xml.getElementsByTagName("EpisodeNumber").item(0).getTextContent());
			if ((sen > 0 && epn > 0) || Session.getInstance(context).specials()) {
				res = Episode.getInstance(context, sid, sen, epn);
				res.load(xml);
			}
		} catch (Exception err) {
			res = null;
		}
		return res;
	}
	
	public static List<Episode> get(Context context, String query, String ... args) {
		List<Episode> res = new ArrayList<Episode>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			String series;
			int season;
			int episode;
			if (cur.moveToFirst()) {
				int c1 = cur.getColumnIndex("series");
				int c2 = cur.getColumnIndex("season");
				int c3 = cur.getColumnIndex("episode");
				do {
					series = cur.getString(c1);
					season = cur.getInt(c2);
					episode = cur.getInt(c3);
					res.add(Episode.get(context, series, season, episode));
				} while (cur.moveToNext());
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static List<Episode> cached(String series, int season) {
		List<Episode> res = new ArrayList<Episode>();
		String chk = series + ".";
		if (season >= 0)
			chk += String.format("S%1$02d", season);
		Object ep;
		for (String k: cache.getKeys())
			if (k.startsWith(chk)) {
				ep = cache.get(k);
				if (ep != null)
					res.add((Episode) ep);
			}
		Collections.sort(res, new EpisodeComparator());
		return res;
	}
	
	public static synchronized void setDirtyFlags(String series, int season, Boolean collected, Boolean watched) {
		if (collected == null && watched == null)
			return;
		String chk = series + ".";
		if (season >= 0)
			chk += String.format("S%1$02d", season);
		Object ep;
		for (String k: cache.getKeys())
			if (k.startsWith(chk)) {
				ep = cache.get(k);
				if (ep != null) {
					if (collected != null)
						((Episode) ep).collected = collected;
					if (watched != null)
						((Episode) ep).watched = watched;
				}
			}
	}
	
	protected void load(Element xml) {
		boolean modified = false;
		String chk;
		chk = Commons.XML.nodeText(xml, "seriesid");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(series) || !series.equals(chk))) {
			series = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "id");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(tvdb_id) || !tvdb_id.equals(chk))) {
			tvdb_id = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "IMDB_ID");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(imdb_id) || !imdb_id.equals(chk))) {
			imdb_id = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "SeasonNumber");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = Integer.parseInt(chk);
				if (r > 0 && r != season) {
					season = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "EpisodeNumber");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = Integer.parseInt(chk);
				if (r > 0 && r != episode) {
					episode = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		String lang = Commons.XML.nodeText(xml, "language", "Language");
		if (!TextUtils.isEmpty(lang)) {
			chk = Commons.XML.nodeText(xml, "EpisodeName");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(name) ||
				(!name.equals(chk) && lang.equals(session.language())))) {
				name = chk;
				modified = true;
			}
			chk = Commons.XML.nodeText(xml, "Overview");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(plot) ||
				(!plot.equals(chk) && lang.equals(session.language())))) {
				plot = chk;
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "filename");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(poster) || !poster.equals(chk))) {
			poster = "http://thetvdb.com/banners/" + chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "FirstAired");
		if (!TextUtils.isEmpty(chk)) {
			try {
				long t = Commons.SDF.eng("yyyy-MM-dd").parse(chk).getTime();
				if (t > 0) {
					firstAired = t;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "GuestStars");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(guestStars, lst)) {
				guestStars = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "Writer");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(writers, lst)) {
				writers = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "Director");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			chk = TextUtils.join(", ", lst);
			if (TextUtils.isEmpty(director) || !director.equals(chk)) {
				director = chk;
				modified = true;
			}
		}
		//
		if (modified)
			save(true);
	}

	protected void load(Cursor cr) {
		Log.v(TAG, "Loading episode " + eid());
		int ci;
		tvdb_id = cr.getString(cr.getColumnIndex("tvdb_id"));
		series = cr.getString(cr.getColumnIndex("series"));
		season = cr.getInt(cr.getColumnIndex("season"));
		episode = cr.getInt(cr.getColumnIndex("episode"));
		ci = cr.getColumnIndex("name");
		if (!cr.isNull(ci))
			name = cr.getString(ci);
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		ci = cr.getColumnIndex("writers");
		if (!cr.isNull(ci))
			writers = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("director");
		if (!cr.isNull(ci))
			director = cr.getString(ci);
		ci = cr.getColumnIndex("guestStars");
		if (!cr.isNull(ci))
			guestStars = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
		ci = cr.getColumnIndex("imdb_id");
		if (!cr.isNull(ci))
			imdb_id = cr.getString(ci);
		ci = cr.getColumnIndex("subtitles");
		if (!cr.isNull(ci))
			subtitles = Arrays.asList(cr.getString(ci).split(","));
		collected = cr.getInt(cr.getColumnIndex("collected")) == 1;
		watched = cr.getInt(cr.getColumnIndex("watched")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
		Log.v(TAG, "Loaded episode " + eid());
	}
	
	protected void save(boolean metadata) {
		dispatch(OnTitleListener.WORKING, null);
		
		Log.d(TAG, "Saving episode " + eid());
		ContentValues cv = new ContentValues();
		cv.put("tvdb_id", tvdb_id);
		cv.put("series", series);
		cv.put("season", season);
		cv.put("episode", episode);
		if (!TextUtils.isEmpty(name))
			cv.put("name", name);
		else
			cv.putNull("name");
		if (!TextUtils.isEmpty(plot))
			cv.put("plot", plot);
		else
			cv.putNull("plot");
		if (!TextUtils.isEmpty(poster))
			cv.put("poster", poster);
		else
			cv.putNull("poster");
		if (!writers.isEmpty())
			cv.put("writers", TextUtils.join(",", writers));
		else
			cv.putNull("writers");
		if (!TextUtils.isEmpty(director))
			cv.put("director", director);
		else
			cv.putNull("director");
		if (!guestStars.isEmpty())
			cv.put("guestStars", TextUtils.join(",", guestStars));
		else
			cv.putNull("guestStars");
		if (firstAired > 0)
			cv.put("firstAired", firstAired);
		else
			cv.putNull("firstAired");
		if (!TextUtils.isEmpty(imdb_id))
			cv.put("imdb_id", imdb_id);
		else
			cv.putNull("imdb_id");
		if (!subtitles.isEmpty())
			cv.put("subtitles", TextUtils.join(",", subtitles));
		else
			cv.putNull("subtitles");
		cv.put("collected", collected);
		cv.put("watched", watched);
		
		boolean isnew = timestamp <= 0;
		if (isnew || metadata) {
			timestamp = System.currentTimeMillis();
			cv.put("timestamp", timestamp);
		}
		if (isnew)
			session.getDB().insertOrThrow(TABLE, null, cv);
		else
			session.getDB().update(TABLE, cv, "tvdb_id=?", new String[] { tvdb_id });
		Log.i(TAG, "Saved episode " + eid());
		
		dispatch(OnTitleListener.READY, null);
	}
	
	protected void delete() {
		dispatch(OnTitleListener.WORKING, null);
		Log.d(TAG, "Deleting episode " + eid());
		session.getDB().delete(TABLE, "tvdb_id=?", new String[] { tvdb_id });
		Log.i(TAG, "Deleted episode " + eid());
		dispatch(OnTitleListener.READY, null);
	}
	
	public void refresh(boolean force) {
		if (Title.ongoingServiceOperation)
			return;
		if (TextUtils.isEmpty(tvdb_id) && (TextUtils.isEmpty(series) || season <= 0 || episode <= 0)) {
			Log.w(TAG, "Missing tvdb_id/series/season/episode, cannot update...");
			return;
		}
		if (isOld() || force) {
			Intent si = new Intent(session.getContext(), Service.class);
			si.setAction(Service.REFRESH_EPISODE);
			si.putExtra("series", series);
			si.putExtra("season", season);
			si.putExtra("episode", episode);
			//session.getContext().startService(si);
			WakefulIntentService.sendWakefulWork(session.getContext(), si);
		}
	}
	
	public boolean isValid() {
		return !(TextUtils.isEmpty(tvdb_id) || TextUtils.isEmpty(series) || season < 0 || episode < 0);
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		if (timestamp > 0 && firstAired > 0) {
			long now = System.currentTimeMillis();
			long ageAired = Math.abs(now - firstAired);
			long ageLocal = now - timestamp;
			if (ageAired < Commons.weekLong)
				return ageLocal > Commons.dayLong;
			if (ageAired > Commons.yearLong)
				return ageLocal > Commons.monthLong;
			return ageLocal > Commons.weekLong;
		}
		return false;
	}
	
	public boolean inCollection() {
		return collected;
	}
	
	public boolean isWatched() {
		return watched;
	}
	
	public void setCollected(boolean value) {
		if (value != collected) {
			collected = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_SERIES, series + "." + Integer.toString(season) + "." +
				Integer.toString(episode), "collected", Boolean.toString(collected));
			if (!getSeries().massUpdate()) {
				String msg = session.getRes().getString(collected ? R.string.msg_coll_add_epi : R.string.msg_coll_del_epi);
				msg = String.format(msg, eid().readable());
				Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void setWatched(boolean value) {
		if (value != watched) {
			watched = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_SERIES, series + "." + Integer.toString(season) + "." +
				Integer.toString(episode), "watched", Boolean.toString(watched));
			if (!getSeries().massUpdate()) {
				String msg = session.getRes().getString(watched ? R.string.msg_seen_add_epi : R.string.msg_seen_del_epi);
				msg = String.format(msg, eid().readable());
				Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public boolean isPilot() {
		return season == 1 && episode == 1;
	}
	
	public boolean isToday() {
		return DateUtils.isToday(firstAired);
	}
	
	public EID eid() {
		return new EID(series, season, episode);
	}
	
	public String name() {
		return TextUtils.isEmpty(name) ? "N/A" : name;
	}
	
	public String plot() {
		return TextUtils.isEmpty(plot) ? "N/A" : plot;
	}
	
	public String firstAired() {
		if (firstAired <= 0)
			return "N/A";
		long now = System.currentTimeMillis();
		if (isToday())
			return DateUtils.getRelativeTimeSpanString(firstAired, now, DateUtils.MINUTE_IN_MILLIS).toString();
		String res = DateUtils.getRelativeTimeSpanString(firstAired, now, DateUtils.DAY_IN_MILLIS).toString();
		if (Math.abs(now - firstAired)/(1000 * 60 * 60) < 168)
			res += " (" + Commons.SDF.loc("EEE").format(firstAired) + ")";
		return res;
	}
	
	public String guests() {
		return guestStars.isEmpty() ? "N/A" : TextUtils.join(", ", guestStars);
	}
	
	public String writers() {
		return writers.isEmpty() ? "N/A" : TextUtils.join(", ", writers);
	}
	
	public String director() {
		return TextUtils.isEmpty(director) ? "N/A" : director;
	}
	
	public String tvdbUrl() {
		return TextUtils.isEmpty(tvdb_id) ? null : "http://thetvdb.com/?tab=episode&id=" + tvdb_id;
	}
	
	public String imdbUrl() {
		return TextUtils.isEmpty(imdb_id) ? null : "http://www.imdb.com/title/" + imdb_id;
	}
	
	public boolean hasSubtitles() {
		return !subtitles.isEmpty();
	}
	
	public String subtitles() {
		if (!subtitles.isEmpty())
			return TextUtils.join("/", subtitles);
		return null;
	}
	
	public Series getSeries() {
		return Series.get(session.getContext(), series);
	}
	
	public Episode getPrior() {
		if (epprev == null)
			epprev = getSeries().whatsBefore(season, episode);
		return epprev;
	}
	
	public Episode getNext() {
		if (epnext == null)
			epnext = getSeries().whatsAfter(season, episode);
		return epnext;
	}
	
	public boolean isAfter(int seasonNo, int episodeNo) {
		return season > seasonNo || (season == seasonNo && episode > episodeNo);
	}
	
	public boolean isAfter(Episode ep) {
		return isAfter(ep.season, ep.episode);
	}
	
	public boolean isBefore(int seasonNo, int episodeNo) {
		return season < seasonNo || (season == seasonNo && episode < episodeNo);
	}
	
	public boolean isBefore(Episode ep) {
		return isBefore(ep.season, ep.episode);
	}
	
	public static class EID {
		public final String series;
		public final int season;
		public final int episode;
		
		public EID(String series, int season, int episode) {
			this.series = series;
			this.season = season;
			this.episode = episode;
		}
		
		public EID(int season, int episode) {
			this.series = null;
			this.season = season;
			this.episode = episode;
		}
		
		public String sequence() {
			return String.format(Locale.getDefault(), "S%1$02dE%2$02d", season, episode);
		}
		
		public String readable() {
			return String.format(Locale.getDefault(), "%dx%d", season, episode);
		}
		
		@Override
		public String toString() {
			return (TextUtils.isEmpty(series) ? "unknown" : series) + "." + sequence();
		}
	}
	
	public static class EpisodeComparator implements Comparator<Episode> {
	    @Override
	    public int compare(Episode o1, Episode o2) {
	    	int res = o1.season - o2.season;
	    	if (res == 0)
	    		res = o1.episode - o2.episode;
	        return res;
	    }
	}
}