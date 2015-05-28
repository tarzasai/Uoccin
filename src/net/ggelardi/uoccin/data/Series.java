package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.XML.TVDB;
import net.ggelardi.uoccin.data.Episode.EID;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.SR;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;
import net.ggelardi.uoccin.serv.SimpleCache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Series extends Title {
    private static final String TAG = "Series";
	private static final String TABLE = "series";
	
	public static final String TAG_DISCOVER = "tvdb_premiere";
	
	private static final SimpleCache cache = new SimpleCache(500);
	
	private List<String> people = new ArrayList<String>();
	private List<String> tags = new ArrayList<String>();
	private int rating = 0;
	private boolean watchlist = false;
	private boolean updated = false;

	public List<Episode> episodes = new ArrayList<Episode>();
	
	public String tvdb_id;
	public String name;
	public int year;
	public String plot;
	public String poster;
	public List<String> genres = new ArrayList<String>();
	public List<String> actors = new ArrayList<String>();
	public String imdb_id;
	public String status;
	public String network;
	public long firstAired;
	public int airsDay;
	public long airsTime;
	public int runtime;
	public String rated;
	public String banner;
	public String fanart;
	
	public Series(Context context, String tvdb_id) {
		this.session = Session.getInstance(context);
		this.tvdb_id = tvdb_id;
		this.poster = "http://thetvdb.com/banners/posters/" + tvdb_id + "-1.jpg";
	}
	
	private static synchronized Series getInstance(Context context, String tvdb_id) {
		Object tmp = cache.get(tvdb_id);
		if (tmp != null) {
			Log.v(TAG, "Series " + tvdb_id + " found in cache");
			return (Series) tmp;
		}
		Series res = new Series(context, tvdb_id);
		cache.add(tvdb_id, res);
		res.reload();
		return res;
	}
	
	public static Series get(Context context, String tvdb_id) {
		Series res = Series.getInstance(context, tvdb_id);
		if (res.isOld())
			res.refresh(false);
		else if (res.updated)
			res.reload();
		return res;
	}
	
	public static Series get(Context context, Element xml, NodeList episodes) {
		String tvdb_id = xml.getElementsByTagName("id").item(0).getTextContent();
		Series res = Series.getInstance(context, tvdb_id);
		res.load(xml, episodes);
		return res;
	}
	
	public static List<Series> find(Context context, String text) {
		List<Series> res = new ArrayList<Series>();
		Document doc = null;
		try {
			doc = TVDB.getInstance().findSeries(text, "en");
		} catch (Exception err) {
			Log.e(TAG, "find", err);
			dispatch(OnTitleListener.ERROR, null);
			return res;
		}
		if (doc != null) {
			List<String> ids = new ArrayList<String>();
			NodeList lst = doc.getElementsByTagName("Series");
			Series series;
			for (int i = 0; i < lst.getLength(); i++) {
				// for non-english requests the search API can returns the translated version (if exists) AND the
				// english version of each series...
				series = Series.get(context, (Element)lst.item(i), null);
				if (!ids.contains(series.tvdb_id)) {
					res.add(series);
					ids.add(series.tvdb_id);
				}
			}
		}
		if (res.isEmpty())
			dispatch(OnTitleListener.NOTFOUND, null);
		return res;
	}
	
	public static void setUpdated(String tvdb_id) {
		Object tmp = cache.get(tvdb_id);
		if (tmp != null)
			((Series) tmp).updated = true;
	}
	
	public static void resetCache() {
		cache.clear();
	}
	
	protected void load(Element serxml, NodeList epsxml) {
		String chk;
		String lang = Commons.XML.nodeText(serxml, "language", "Language");
		if (!TextUtils.isEmpty(lang)) {
			chk = Commons.XML.nodeText(serxml, "SeriesName");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(name) ||
				(!name.equals(chk) && lang.equals(session.language()))))
				name = chk;
			chk = Commons.XML.nodeText(serxml, "Overview");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(plot) ||
				(!plot.equals(chk) && lang.equals(session.language()))))
				plot = chk;
		}
		chk = Commons.XML.nodeText(serxml, "poster");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(poster) || !poster.equals(chk))) {
			poster = "http://thetvdb.com/banners/" + chk;
		}
		chk = Commons.XML.nodeText(serxml, "banner");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(banner) || !banner.equals(chk))) {
			banner = "http://thetvdb.com/banners/" + chk;
		}
		chk = Commons.XML.nodeText(serxml, "fanart");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(fanart) || !fanart.equals(chk))) {
			fanart = "http://thetvdb.com/banners/" + chk;
		}
		chk = Commons.XML.nodeText(serxml, "Genre");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(genres, lst))
				genres = new ArrayList<String>(lst);
		}
		chk = Commons.XML.nodeText(serxml, "Actors");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(actors, lst))
				actors = new ArrayList<String>(lst);
		}
		chk = Commons.XML.nodeText(serxml, "IMDB_ID");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(imdb_id) || !imdb_id.equals(chk))) {
			imdb_id = chk;
		}
		chk = Commons.XML.nodeText(serxml, "Status");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(status) || !status.equals(chk))) {
			status = chk;
		}
		chk = Commons.XML.nodeText(serxml, "Network");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(network) || !network.equals(chk))) {
			network = chk;
		}
		chk = Commons.XML.nodeText(serxml, "FirstAired");
		if (!TextUtils.isEmpty(chk)) {
			try {
				long t = Commons.SDF.eng("yyyy-MM-dd").parse(chk).getTime();
				if (t > 0) {
					firstAired = t;
					year = Commons.getDatePart(firstAired, Calendar.YEAR);
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(serxml, "Airs_DayOfWeek");
		if (!TextUtils.isEmpty(chk)) {
			int d = Commons.SDF.day(chk);
			if (d > 0)
				airsDay = d;
		}
		chk = Commons.XML.nodeText(serxml, "Airs_Time");
		if (!TextUtils.isEmpty(chk)) {
			chk = chk.replace(".", ":").replace(" ", "").toUpperCase(Locale.getDefault());
			String fmt = chk.contains("M") ? "hh:mma" : "HH:mm";
			try {
				long t = Commons.SDF.eng(fmt).parse(chk).getTime();
				if (t > 0)
					airsTime = t;
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(serxml, "Runtime");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = Integer.parseInt(chk);
				if (r > 0 && r != runtime)
					runtime = r;
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(serxml, "ContentRating");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(rated) || !rated.equals(chk))) {
			rated = chk;
		}
		people.clear();
		int purged = 0;
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			// we want to update the timestamp in any case
			save(true);
			EID last = new EID(0, 0);
			if (epsxml != null && epsxml.getLength() > 0) {
				List<Episode> lst = new ArrayList<Episode>();
				EID check;
				Element node;
				Episode ep;
				for (int i = 0; i < epsxml.getLength(); i++) {
					node = (Element) epsxml.item(i);
					check = new EID(node);
					if (check.isValid(session.specials())) {
						ep = new Episode(session.getContext(), check);
						if (lst.contains(ep))
							Log.i(TAG, "Duplicate found in xml: " + ep.eid().toString());
						else {
							ep.update(node);
							lst.add(ep);
							if (last.compareTo(check) < 0)
								last = check;
						}
					}
				}
				Collections.sort(lst);
				episodes = lst;
			}
			purged = db.delete("episode", "series = ? and (season > ? or (season = ? and episode > ?))",
				new String[] { tvdb_id, Integer.toString(last.season), Integer.toString(last.season),
				Integer.toString(last.episode) });
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		if (purged > 0)
			dispatch(OnTitleListener.RELOAD, null);
	}
	
	protected void load(Cursor cr) {
		dispatch(OnTitleListener.WORKING, null);
		Log.v(TAG, "Loading series " + tvdb_id);
		int ci;
		tvdb_id = cr.getString(cr.getColumnIndex("tvdb_id")); // it's already set btw...
		name = cr.getString(cr.getColumnIndex("name"));
		ci = cr.getColumnIndex("year");
		if (!cr.isNull(ci))
			year = cr.getInt(ci);
		ci = cr.getColumnIndex("plot");
		if (!cr.isNull(ci))
			plot = cr.getString(ci);
		ci = cr.getColumnIndex("poster");
		if (!cr.isNull(ci))
			poster = cr.getString(ci);
		ci = cr.getColumnIndex("genres");
		if (!cr.isNull(ci))
			genres = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("actors");
		if (!cr.isNull(ci))
			actors = Arrays.asList(cr.getString(ci).split(","));
		ci = cr.getColumnIndex("imdb_id");
		if (!cr.isNull(ci))
			imdb_id = cr.getString(ci);
		ci = cr.getColumnIndex("status");
		if (!cr.isNull(ci))
			status = cr.getString(ci);
		ci = cr.getColumnIndex("network");
		if (!cr.isNull(ci))
			network = cr.getString(ci);
		ci = cr.getColumnIndex("firstAired");
		if (!cr.isNull(ci))
			firstAired = cr.getLong(ci);
		ci = cr.getColumnIndex("airsDay");
		if (!cr.isNull(ci))
			airsDay = cr.getInt(ci);
		ci = cr.getColumnIndex("airsTime");
		if (!cr.isNull(ci))
			airsTime = cr.getLong(ci);
		ci = cr.getColumnIndex("runtime");
		if (!cr.isNull(ci))
			runtime = cr.getInt(ci);
		ci = cr.getColumnIndex("rated");
		if (!cr.isNull(ci))
			rated = cr.getString(ci);
		ci = cr.getColumnIndex("banner");
		if (!cr.isNull(ci))
			banner = cr.getString(ci);
		ci = cr.getColumnIndex("fanart");
		if (!cr.isNull(ci))
			fanart = cr.getString(ci);
		ci = cr.getColumnIndex("rating");
		if (!cr.isNull(ci))
			rating = cr.getInt(ci);
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
		Log.v(TAG, "Loaded series " + tvdb_id);
		// tags
		tags.clear();
		Cursor ct = session.getDB().query("sertag", new String[] { "tag" }, "series = ?", new String[] { tvdb_id },
			null, null, "tag");
		try {
			while (ct.moveToNext())
				tags.add(ct.getString(0));
		} finally {
			ct.close();
		}
		people.clear();
		dispatch(OnTitleListener.READY, null);
	}
	
	protected void save(boolean metadata) {
		dispatch(OnTitleListener.WORKING, null);
		
		Log.d(TAG, "Saving series " + tvdb_id);
		
		ContentValues cv = new ContentValues();
		
		cv.put("tvdb_id", tvdb_id);
		cv.put("name", name);
		
		if (year > 0)
			cv.put("year", year);
		else
			cv.putNull("year");
		
		if (!TextUtils.isEmpty(plot))
			cv.put("plot", plot);
		else
			cv.putNull("plot");
		
		if (!TextUtils.isEmpty(poster))
			cv.put("poster", poster);
		else
			cv.putNull("poster");
		
		if (!genres.isEmpty())
			cv.put("genres", TextUtils.join(",", genres));
		else
			cv.putNull("genres");
		
		if (!actors.isEmpty())
			cv.put("actors", TextUtils.join(",", actors));
		else
			cv.putNull("actors");
		
		if (!TextUtils.isEmpty(imdb_id))
			cv.put("imdb_id", imdb_id);
		else
			cv.putNull("imdb_id");
		
		if (!TextUtils.isEmpty(status))
			cv.put("status", status);
		else
			cv.putNull("status");
		
		if (!TextUtils.isEmpty(network))
			cv.put("network", network);
		else
			cv.putNull("network");
		
		if (firstAired > 0)
			cv.put("firstAired", firstAired);
		else
			cv.putNull("firstAired");
		
		if (airsDay > 0)
			cv.put("airsDay", airsDay);
		else
			cv.putNull("airsDay");
		
		if (airsTime > 0)
			cv.put("airsTime", airsTime);
		else
			cv.putNull("airsTime");
		
		if (runtime > 0)
			cv.put("runtime", runtime);
		else
			cv.putNull("runtime");
		
		if (!TextUtils.isEmpty(rated))
			cv.put("rated", rated);
		else
			cv.putNull("rated");
		
		if (!TextUtils.isEmpty(banner))
			cv.put("banner", banner);
		else
			cv.putNull("banner");
		
		if (!TextUtils.isEmpty(fanart))
			cv.put("fanart", fanart);
		else
			cv.putNull("fanart");
		
		if (rating > 0)
			cv.put("rating", rating);
		else
			cv.putNull("rating");
		
		cv.put("watchlist", watchlist);
		
		SQLiteDatabase db = session.getDB();
		
		boolean isnew = timestamp <= 0;
		if (isnew || metadata) {
			timestamp = System.currentTimeMillis();
			cv.put("timestamp", timestamp);
		}
		if (isnew)
			db.insertOrThrow(TABLE, null, cv);
		else
			db.update(TABLE, cv, "tvdb_id = ?", new String[] { tvdb_id });
		
		// tags
		db.delete("sertag", "series = ?", new String[] { tvdb_id });
		for (String tag: tags) {
			cv = new ContentValues();
			cv.put("series", tvdb_id);
			cv.put("tag", tag.trim());
			db.insert("sertag", null, cv);
		}
		
		Log.i(TAG, "Saved series " + tvdb_id);
		
		dispatch(OnTitleListener.READY, null);
	}
	
	protected void delete() {
		dispatch(OnTitleListener.WORKING, null);
		Log.d(TAG, "Deleting series " + tvdb_id);
		session.getDB().delete(TABLE, "tvdb_id=?", new String[] { tvdb_id });
		Log.i(TAG, "Deleted series " + tvdb_id);
		dispatch(OnTitleListener.READY, null);
	}
	
	public synchronized void reload() {
		Cursor cur = session.getDB().query(TABLE, null, "tvdb_id=?", new String[] { tvdb_id },
			null, null, null);
		try {
			if (cur.moveToFirst())
				load(cur);
		} finally {
			cur.close();
		}
		List<Episode> lst = new ArrayList<Episode>();
		cur = session.getDB().query("episode", new String[] { "season", "episode" },
			"series = ?", new String[] { tvdb_id }, null, null, "season, episode");
		try {
			while (cur.moveToNext())
				lst.add(new Episode(session.getContext(), tvdb_id, cur.getInt(0), cur.getInt(1)));
		} finally {
			cur.close();
			updated = false;
		}
		episodes = lst;
	}
	
	public void refresh(boolean force) {
		if ((isOld() || force) && !Service.isQueued(tvdb_id)) {
			Intent si = new Intent(session.getContext(), Service.class);
			si.setAction(SR.REFRESH_SERIES);
			si.putExtra("tvdb_id", tvdb_id);
			si.putExtra("forced", force);
			WakefulIntentService.sendWakefulWork(session.getContext(), si);
		}
	}
	
	public boolean isValid() {
		return !(TextUtils.isEmpty(tvdb_id) || TextUtils.isEmpty(name) || episodes == null || episodes.isEmpty());
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
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public int getRating() {
		return rating;
	}
	
	public List<String> getTags() {
		return new ArrayList<String>(tags);
	}
	
	public boolean hasTags() {
		return !tags.isEmpty();
	}
	
	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "watchlist", Boolean.toString(watchlist));
		}
	}
	
	public void setCollected(boolean flag, Integer season) {
		for (Episode ep: episodes)
			if (season == null || ep.season == season)
				ep.setCollected(flag);
		if (isValid())
			save(false);
		else
			refresh(true);
	}
	
	public void setWatched(boolean flag, Integer season) {
		for (Episode ep: episodes)
			if (season == null || ep.season == season)
				ep.setWatched(flag);
		if (isValid())
			save(false);
		else
			refresh(true);
	}
	
	public void setRating(int value) {
		if (value != rating) {
			rating = value;
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "rating", Integer.toString(rating));
		}
	}
	
	public void setTags(String[] values) {
		tags = new ArrayList<String>(Arrays.asList(values));
		if (isValid())
			save(false);
		else
			refresh(true);
		session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "tags", TextUtils.join(",", tags));
	}
	
	public void addTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (!hasTag(tag)) {
			tags.add(tag);
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "tags", TextUtils.join(",", tags));
		}
	}
	
	public void delTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (hasTag(tag)) {
			tags.remove(tag);
			if (isValid())
				save(false);
			else
				refresh(true);
			session.driveQueue(Session.QUEUE_SERIES, tvdb_id, "tags", TextUtils.join(",", tags));
		}
	}
	
	public String tvdbUrl() {
		return "http://thetvdb.com/?tab=series&id=" + tvdb_id;
	}
	
	public String imdbUrl() {
		if (!TextUtils.isEmpty(imdb_id))
			return "http://www.imdb.com/title/" + imdb_id;
		return null;
	}
	
	public boolean isRecent() {
		long fa = firstAired;
		if (fa <= 0) try {
			fa = episodes(1).get(0).firstAired;
		} catch (Exception err) {
		}
		return fa > System.currentTimeMillis() || Math.abs(fa - System.currentTimeMillis()) < Commons.weekLong;
	}
	
	public boolean isEnded() {
		return !TextUtils.isEmpty(status) && status.equals("Ended");
	}
	
	public String name() {
		return TextUtils.isEmpty(name) ? "N/A" : name;
	}
	
	public String year() {
		return year <= 0 ? "N/A" : Integer.toString(year);
	}
	
	public String plot() {
		if (TextUtils.isEmpty(plot))
			return "N/A";
		return plot;
	}
	
	public String network() {
		return TextUtils.isEmpty(network) ? "N/A" : network;
	}
	
	public String rated() {
		return TextUtils.isEmpty(rated) ? "N/A" : rated;
	}
	
	public String airTime() {
		if (isEnded())
			return session.getString(R.string.fmt_status_end);
		String res = "N/A";
		if (airsDay > 0 && airsTime > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(airsTime);
			cal.set(Calendar.DAY_OF_WEEK, airsDay);
			res = Commons.SDF.loc(session.getString(R.string.fmt_airtime)).format(cal.getTime());
		}
		return res;
	}
	
	public String airInfo() {
		if (isEnded())
			return session.getString(R.string.fmt_status_end);
		return (TextUtils.isEmpty(network) ? "N/A" : network) + " - " + airTime();
	}
	
	public String people() {
		if (people.isEmpty()) {
			people.addAll(actors);
			for (Episode ep: episodes) {
				ep.people();
				people.addAll(ep.people);
			}
			people.removeAll(Arrays.asList("", "N/A", null));
			Set<String> hs = new HashSet<String>();
			hs.addAll(people);
			people.clear();
			people.addAll(hs);
		}
		return people.isEmpty() ? "N/A" : TextUtils.join(", ", people);
	}
	
	public String actors() {
		return actors.isEmpty() ? "N/A" : TextUtils.join(", ", actors);
	}
	
	public String genres() {
		return genres.isEmpty() ? "N/A" : TextUtils.join(", ", genres);
	}
	
	public String tags() {
		return tags.isEmpty() ? "N/D" : TextUtils.join(", ", tags);
	}
	
	public String rating() {
		return rating <= 0 ? "N/A" : Integer.toString(rating);
	}
	
	public List<Episode> episodes(int season) {
		List<Episode> res = new ArrayList<Episode>();
		for (Episode ep:episodes)
			if (ep.season == season)
				res.add(ep);
		return res;
	}
	
	public Episode lastEpisode() {
		for (int i = episodes.size() - 1; i >= 0; i--)
			if (episodes.get(i).firstAired > 0 && episodes.get(i).firstAired < System.currentTimeMillis())
				return episodes.get(i);
		return null;
	}
	
	public Episode lastEpisode(int season) {
		if (!seasons().contains(season))
			return null;
		List<Episode> lst = episodes(season);
		return lst.get(lst.size() - 1);
	}
	
	public Episode nextEpisode() {
		for (Episode ep: episodes)
			if (ep.firstAired >= System.currentTimeMillis())
				return ep;
		return null;
	}
	
	public Episode checkEpisode(int season, int episode) {
		for (Episode ep: episodes)
			if (ep.season == season && ep.episode == episode)
				return ep;
		return null;
	}
	
	public Episode whatsBefore(int season, int episode) {
		for (int i = episodes.size() - 1; i >= 0; i--)
			if (episodes.get(i).isBefore(season, episode))
				return episodes.get(i);
		return null;
	}
	
	public Episode whatsAfter(int season, int episode) {
		for (Episode ep: episodes)
			if (ep.isAfter(season, episode))
				return ep;
		return null;
	}
	
	public List<Integer> seasons() {
		List<Integer> res = new ArrayList<Integer>();
		for (Episode ep: episodes)
			if (!res.contains(ep.season))
				res.add(ep.season);
		return res;
	}
	
	public int episodeCount(Integer season) {
		int res = 0;
		for (Episode ep: episodes)
			if (season == null || ep.season == season)
				res++;
		return res;
	}
	
	public int episodeAired(Integer season) {
		int res = 0;
		for (Episode ep: episodes)
			if (ep.firstAired > 0 && (season == null || ep.season == season))
				res++;
		return res;
	}
	
	public int episodeCollected(Integer season) {
		int res = 0;
		for (Episode ep: episodes)
			if (ep.inCollection() && (season == null || ep.season == season))
				res++;
		return res;
	}
	
	public int episodeWatched(Integer season) {
		int res = 0;
		for (Episode ep: episodes)
			if (ep.isWatched() && (season == null || ep.season == season))
				res++;
		return res;
	}
	
	public int episodeWaiting(Integer season) {
		int res = 0;
		for (Episode ep: episodes)
			if (ep.inCollection() && !ep.isWatched() && (season == null || ep.season == season))
				res++;
		return res;
	}
	
	public int episodeMissing() {
		int res = 0;
		for (Episode ep: episodes)
			if (!ep.inCollection() && !ep.isWatched() && ep.firstAired > 0 && ep.firstAired < System.currentTimeMillis())
				res++;
		return res;
	}
}