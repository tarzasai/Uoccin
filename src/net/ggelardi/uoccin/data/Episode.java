package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.SR;
import net.ggelardi.uoccin.serv.Commons.XML;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;

import org.w3c.dom.Element;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Episode extends Title implements Comparable<Episode> {
	private static final String TAG = "Episode";
	private static final String TABLE = "episode";
	
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
	
	public Episode(Context context, EID eid) {
		this(context, eid.series, eid.season, eid.episode);
	}
	
	public Episode(Context context, String series, int season, int episode) {
		this.session = Session.getInstance(context);
		this.series = series;
		this.season = season;
		this.episode = episode;
		reload();
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
			session.getDB().update(TABLE, cv, "series = ? and season = ? and episode = ?",
				new String[] { series, Integer.toString(season), Integer.toString(episode) });
		Log.i(TAG, "Saved episode " + eid());
		
		dispatch(OnTitleListener.READY, null);
	}
	
	public synchronized void reload() {
		dispatch(OnTitleListener.WORKING, null);
		Cursor cur = session.getDB().query("episode", null, "series=? and season=? and episode=?",
			new String[] { series, Integer.toString(season), Integer.toString(episode) }, null, null, null);
		try {
			if (cur.moveToFirst()) {
				Log.v(TAG, "Loading episode " + eid());
				int ci;
				tvdb_id = cur.getString(cur.getColumnIndex("tvdb_id"));
				ci = cur.getColumnIndex("name");
				if (!cur.isNull(ci))
					name = cur.getString(ci);
				ci = cur.getColumnIndex("plot");
				if (!cur.isNull(ci))
					plot = cur.getString(ci);
				ci = cur.getColumnIndex("poster");
				if (!cur.isNull(ci))
					poster = cur.getString(ci);
				ci = cur.getColumnIndex("writers");
				if (!cur.isNull(ci))
					writers = Arrays.asList(cur.getString(ci).split(","));
				ci = cur.getColumnIndex("director");
				if (!cur.isNull(ci))
					director = cur.getString(ci);
				ci = cur.getColumnIndex("guestStars");
				if (!cur.isNull(ci))
					guestStars = Arrays.asList(cur.getString(ci).split(","));
				ci = cur.getColumnIndex("firstAired");
				if (!cur.isNull(ci))
					firstAired = cur.getLong(ci);
				ci = cur.getColumnIndex("imdb_id");
				if (!cur.isNull(ci))
					imdb_id = cur.getString(ci);
				ci = cur.getColumnIndex("subtitles");
				if (!cur.isNull(ci))
					subtitles = Arrays.asList(cur.getString(ci).split(","));
				collected = cur.getInt(cur.getColumnIndex("collected")) == 1;
				watched = cur.getInt(cur.getColumnIndex("watched")) == 1;
				timestamp = cur.getLong(cur.getColumnIndex("timestamp"));
				Log.v(TAG, "Loaded episode " + eid());
			}
		} finally {
			cur.close();
		}
		dispatch(OnTitleListener.READY, null);
	}
	
	public void update(Element xml) {
		String chk;
		chk = Commons.XML.nodeText(xml, "id");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(tvdb_id) || !tvdb_id.equals(chk))) {
			tvdb_id = chk;
		}
		chk = Commons.XML.nodeText(xml, "IMDB_ID");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(imdb_id) || !imdb_id.equals(chk))) {
			imdb_id = chk;
		}
		String lang = Commons.XML.nodeText(xml, "language", "Language");
		if (!TextUtils.isEmpty(lang)) {
			chk = Commons.XML.nodeText(xml, "EpisodeName");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(name) ||
				(!name.equals(chk) && lang.equals(session.language()))))
				name = chk;
			chk = Commons.XML.nodeText(xml, "Overview");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(plot) ||
				(!plot.equals(chk) && lang.equals(session.language()))))
				plot = chk;
		}
		chk = Commons.XML.nodeText(xml, "filename");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(poster) || !poster.equals(chk))) {
			poster = "http://thetvdb.com/banners/" + chk;
		}
		chk = Commons.XML.nodeText(xml, "FirstAired");
		if (!TextUtils.isEmpty(chk)) {
			try {
				long t = Commons.SDF.eng("yyyy-MM-dd").parse(chk).getTime();
				if (t > 0)
					firstAired = t;
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "GuestStars");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(guestStars, lst))
				guestStars = new ArrayList<String>(lst);
		}
		chk = Commons.XML.nodeText(xml, "Writer");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(writers, lst))
				writers = new ArrayList<String>(lst);
		}
		chk = Commons.XML.nodeText(xml, "Director");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			chk = TextUtils.join(", ", lst);
			if (TextUtils.isEmpty(director) || !director.equals(chk))
				director = chk;
		}
		// we want to update the timestamp in any case
		save(true);
	}
	
	public void refresh(boolean force) {
		if (isValid() && (isOld() || force) && !Service.isQueued(eid().toString())) {
			Intent si = new Intent(session.getContext(), Service.class);
			si.setAction(SR.REFRESH_EPISODE);
			si.putExtra("series", series);
			si.putExtra("season", season);
			si.putExtra("episode", episode);
			si.putExtra("forced", force);
			WakefulIntentService.sendWakefulWork(session.getContext(), si);
		}
	}
	
	public boolean isValid() {
		return eid().isValid(session.specials());
	}
	
	public boolean isOld() {
		if (timestamp == 1)
			return true;
		if (timestamp > 0) {
			long now = System.currentTimeMillis();
			long ageLocal = now - timestamp;
			if (firstAired > 0) {
				long ageAired = Math.abs(now - firstAired);
				if (ageAired < Commons.weekLong)
					return ageLocal > Commons.dayLong;
				if (ageAired > Commons.yearLong)
					return ageLocal > Commons.monthLong;
			}
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
			session.driveQueue(Session.QUEUE_SERIES, series + "." + Integer.toString(season) + "." +
				Integer.toString(episode), "collected", Boolean.toString(collected));
		}
	}
	
	public void setWatched(boolean value) {
		if (value != watched) {
			watched = value;
			if (isValid())
				save(false);
			session.driveQueue(Session.QUEUE_SERIES, series + "." + Integer.toString(season) + "." +
				Integer.toString(episode), "watched", Boolean.toString(watched));
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
		if (TextUtils.isEmpty(plot))
			return "N/A";
		if (plot.length() > 300 && session.blockSpoilers())
			return Commons.shortenText(plot, 300) +
				" ... <b><i>[spoiler protection enabled, see the complete synopsis on TVDB/IMDB]</i></b>";
		return plot;
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
			return TextUtils.join(", ", subtitles);
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
		return eid().compareTo(new EID(seasonNo, episodeNo)) > 0;
	}
	
	public boolean isAfter(Episode ep) {
		return isAfter(ep.season, ep.episode);
	}
	
	public boolean isBefore(int seasonNo, int episodeNo) {
		return eid().compareTo(new EID(seasonNo, episodeNo)) < 0;
	}
	
	public boolean isBefore(Episode ep) {
		return isBefore(ep.season, ep.episode);
	}
	
	@Override
	public int compareTo(Episode another) {
		return eid().compareTo(another.eid());
	}
	
	public static class EID implements Comparable<EID> {
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
		
		public EID(EID eid) {
			this.series = eid.series;
			this.season = eid.season;
			this.episode = eid.episode;
		}
		
		public EID(Element xml) {
			String sid = "";
			int sno = -1;
			int eno = -1;
			try {
				sid = XML.nodeText(xml, "seriesid", "seriesId", "SeriesId");
				sno = Integer.parseInt(XML.nodeText(xml, "SeasonNumber"));
				eno = Integer.parseInt(XML.nodeText(xml, "EpisodeNumber"));
			} catch (Exception err) {
				Log.d(TAG, "EID() invalid XML", err);
			}
			this.series = sid;
			this.season = sno;
			this.episode = eno;
		}
		
		public boolean isValid(boolean specials) {
			return !TextUtils.isEmpty(series) &&
				((season > 0 && episode > 0) || (specials && season >= 0 && episode >= 0));
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
		
		@Override
		public boolean equals(Object obj2) {
			return obj2 instanceof EID && ((EID)obj2).series.equals(series) && ((EID)obj2).season == season &&
				((EID)obj2).episode == episode;
		}

		@Override
		public int compareTo(EID another) {
			int res = season - another.season;
	    	if (res == 0)
	    		res = episode - another.episode;
	        return res;
		}
	}
}