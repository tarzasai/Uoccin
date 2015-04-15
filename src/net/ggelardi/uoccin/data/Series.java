package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.XML.TVDB;
import net.ggelardi.uoccin.serv.Commons;
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
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Series extends Title {
    private static final String TAG = "Series";
	private static final String TABLE = "series";
	
	public static final String ACTIVE = "Continuing";
	public static final String ENDED = "Ended";
	
	private static final SimpleCache cache = new SimpleCache(500);
	
	private final Session session;
	
	private int rating = 0;
	private List<String> tags = new ArrayList<String>();
	private boolean watchlist = false;
	
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
	public int lastseason = 0; // not stored
	public String rated;
	public String banner;
	public String fanart;
	public long timestamp = 0;
	public boolean modified = false;
	
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
		return res;
	}
	
	public static Series get(Context context, Element xml) {
		String tvdb_id = xml.getElementsByTagName("id").item(0).getTextContent();
		Series res = Series.getInstance(context, tvdb_id);
		res.load(xml);
		/* no commit here
		if (!res.isNew())
			res.commit(null);
		*/
		return res;
	}
	
	public static List<Series> get(Context context, List<String> tvdb_ids) {
		List<Series> res = new ArrayList<Series>();
		for (String sid: tvdb_ids)
			res.add(Series.get(context, sid));
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
				series = Series.get(context, (Element)lst.item(i));
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
	
	public static List<Series> cached() {
		List<Series> res = new ArrayList<Series>();
		Object ser;
		for (String k: cache.getKeys()) {
			ser = cache.get(k);
			if (ser != null)
				res.add((Series) ser);
		}
		return res;
	}
	
	protected void load(Element xml) {
		String chk;
		String lang = Commons.XML.nodeText(xml, "language", "Language");
		if (!TextUtils.isEmpty(lang)) {
			chk = Commons.XML.nodeText(xml, "SeriesName");
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
		chk = Commons.XML.nodeText(xml, "poster");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(poster) || !poster.equals(chk))) {
			poster = "http://thetvdb.com/banners/" + chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "banner");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(banner) || !banner.equals(chk))) {
			banner = "http://thetvdb.com/banners/" + chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "fanart");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(fanart) || !fanart.equals(chk))) {
			fanart = "http://thetvdb.com/banners/" + chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "Genre");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(genres, lst)) {
				genres = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "Actors");
		if (!TextUtils.isEmpty(chk)) {
			List<String> lst = new ArrayList<String>(Arrays.asList(chk.split("[\\x7C]")));
			lst.removeAll(Arrays.asList("", null));
			if (!Commons.sameStringLists(actors, lst)) {
				actors = new ArrayList<String>(lst);
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "IMDB_ID");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(imdb_id) || !imdb_id.equals(chk))) {
			imdb_id = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "Status");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(status) || !status.equals(chk))) {
			status = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "Network");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(network) || !network.equals(chk))) {
			network = chk;
			modified = true;
		}
		chk = Commons.XML.nodeText(xml, "FirstAired");
		if (!TextUtils.isEmpty(chk)) {
			try {
				long t = Commons.SDF.eng("yyyy-MM-dd").parse(chk).getTime();
				if (t > 0) {
					firstAired = t;
					year = Commons.getDatePart(firstAired, Calendar.YEAR);
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "Airs_DayOfWeek");
		if (!TextUtils.isEmpty(chk)) {
			int d = Commons.SDF.day(chk);
			if (d > 0) {
				airsDay = d;
				modified = true;
			}
		}
		chk = Commons.XML.nodeText(xml, "Airs_Time");
		if (!TextUtils.isEmpty(chk)) {
			if (!chk.contains("M")) {
				try {
					long t = Commons.SDF.eng("HH:mm").parse(chk).getTime();
					if (t > 0) {
						airsTime = t;
						modified = true;
					}
				} catch (Exception err) {
					Log.e(TAG, chk, err);
				}
			} else {
				try {
					long t = Commons.SDF.eng("hh:mm a").parse(chk).getTime();
					if (t > 0) {
						airsTime = t;
						modified = true;
					}
				} catch (Exception err) {
					try {
						long t = Commons.SDF.eng("hh:mma").parse(chk).getTime();
						if (t > 0) {
							airsTime = t;
							modified = true;
						}
					} catch (Exception err2) {
						Log.e(TAG, chk, err2);
					}
				}
			}
		}
		chk = Commons.XML.nodeText(xml, "Runtime");
		if (!TextUtils.isEmpty(chk)) {
			try {
				int r = Integer.parseInt(chk);
				if (r > 0 && r != runtime) {
					runtime = r;
					modified = true;
				}
			} catch (Exception err) {
				Log.e(TAG, chk, err);
			}
		}
		chk = Commons.XML.nodeText(xml, "ContentRating");
		if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(rated) || !rated.equals(chk))) {
			rated = chk;
			modified = true;
		}
	}
	
	protected void load(Cursor cr) {
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
		ci = cr.getColumnIndex("tags");
		if (!cr.isNull(ci))
			tags = Arrays.asList(cr.getString(ci).split(","));
		watchlist = cr.getInt(cr.getColumnIndex("watchlist")) == 1;
		timestamp = cr.getLong(cr.getColumnIndex("timestamp"));
		reloadEpisodes();
		Log.v(TAG, "Loaded series " + tvdb_id);
	}
	
	protected void save(boolean isnew) {
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
		if (!tags.isEmpty())
			cv.put("tags", TextUtils.join(",", tags));
		else
			cv.putNull("tags");
		cv.put("watchlist", watchlist);
		timestamp = System.currentTimeMillis();
		cv.put("timestamp", timestamp);
		if (isnew)
			session.getDB().insertOrThrow(TABLE, null, cv);
		else
			session.getDB().update(TABLE, cv, "tvdb_id=?", new String[] { tvdb_id });
		Log.i(TAG, "Saved series " + tvdb_id);
	}
	
	protected void delete() {
		dispatch(OnTitleListener.WORKING, null);
		Log.d(TAG, "Deleting series " + tvdb_id);
		session.getDB().delete(TABLE, "tvdb_id=?", new String[] { tvdb_id });
		Log.i(TAG, "Deleted series " + tvdb_id);
		dispatch(OnTitleListener.READY, null);
	}
	
	public synchronized void reload() {
		//dispatch(OnTitleListener.WORKING, null);
		Cursor cur = session.getDB().query(TABLE, null, "tvdb_id=?", new String[] { tvdb_id },
			null, null, null);
		try {
			if (cur.moveToFirst())
				load(cur);
		} finally {
			cur.close();
		}
		//dispatch(OnTitleListener.READY, null);
	}
	
	public synchronized void reloadEpisodes() {
		dispatch(OnTitleListener.WORKING, null);
		episodes = Episode.get(session.getContext(), "select series, season, episode from episode " +
			"where series = ? order by season, episode", new String[] { tvdb_id });
		lastseason = episodes.get(episodes.size()-1).season;
		dispatch(OnTitleListener.READY, null);
	}
	
	public void refresh(boolean force) {
		if (Title.ongoingServiceOperation)
			return;
		if (isOld() || force) {
			Intent si = new Intent(session.getContext(), Service.class);
			si.setAction(Service.REFRESH_SERIES);
			si.putExtra("tvdb_id", tvdb_id);
			WakefulIntentService.sendWakefulWork(session.getContext(), si);
		}
	}
	
	public final synchronized void commit(String what) {
		if (!isValid())
			return;
		dispatch(OnTitleListener.WORKING, null);
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			int changes = 0;
			if (modified) {
				changes++;
				save(isNew());
			}
			List<Episode> eps = Episode.cached(tvdb_id, -1);
			for (Episode ep: eps)
				if (lastseason > 0 && ep.season > lastseason) {
					changes++;
					Episode.drop(tvdb_id, ep.season, null);
				} else if (ep.modified) {
					changes++;
					ep.save(ep.isNew());
				}
			if (lastseason > 0) {
				Log.v(TAG, "cancello da " + name + " per season > " + Integer.toString(lastseason));
				db.delete("episode", "series = ? and season > ?",
					new String[] { tvdb_id, Integer.toString(lastseason) });
			}
			db.setTransactionSuccessful();
			modified = false;
			for (Episode ep: eps)
				ep.modified = false;
			if (changes > 0)
				reloadEpisodes();
			if (!Title.ongoingServiceOperation && what != null) {
				Intent si = new Intent(session.getContext(), Service.class);
				si.setAction(Service.GDRIVE_BACKUP);
				si.putExtra("what", what);
				WakefulIntentService.sendWakefulWork(session.getContext(), si);
			}
		} catch (Exception err) {
			Log.e(TAG, "commit", err);
		} finally {
			db.endTransaction();
		}
		dispatch(OnTitleListener.READY, null);
	}
	
	public boolean isValid() {
		return !(TextUtils.isEmpty(tvdb_id) || TextUtils.isEmpty(name) || episodes == null || episodes.isEmpty());
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return timestamp > 0 && (System.currentTimeMillis() - timestamp) > Commons.weekLong; // TODO preferences
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			modified = true;
			if (!isValid())
				refresh(true);
			else
				commit(Commons.GD.SER_WLST);
			String msg = session.getRes().getString(watchlist ? R.string.msg_wlst_add_ser : R.string.msg_wlst_del_ser);
			msg = String.format(msg, name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public int getRating() {
		return rating;
	}
	
	public void setRating(int value) {
		if (value != rating) {
			rating = value;
			modified = true;
			if (!isValid())
				refresh(true);
			else
				commit(Commons.GD.SER_WLST);
		}
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
	
	public void setTags(String[] values) {
		tags = new ArrayList<String>(Arrays.asList(values));
		modified = true;
		if (!isValid())
			refresh(true);
		else
			commit(Commons.GD.SER_WLST);
	}
	
	public void addTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (!hasTag(tag)) {
			tags.add(tag);
			modified = true;
			if (!isValid())
				refresh(true);
			else
				commit(Commons.GD.SER_WLST);
			String msg = String.format(session.getRes().getString(R.string.msg_tags_add), tag);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void delTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (hasTag(tag)) {
			tags.remove(tag);
			modified = true;
			if (!isValid())
				refresh(true);
			else
				commit(Commons.GD.SER_WLST);
			String msg = String.format(session.getRes().getString(R.string.msg_tags_del), tag);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
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
		return Math.abs(firstAired - System.currentTimeMillis()) < Commons.weekLong;
	}
	
	public boolean isEnded() {
		return !TextUtils.isEmpty(status) && status.equals(ENDED);
	}
	
	public String plot() {
		return TextUtils.isEmpty(plot) ? "N/A" : plot;
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
	
	public String actors() {
		return actors.isEmpty() ? "N/A" : TextUtils.join(", ", actors);
	}
	
	public String genres() {
		return genres.isEmpty() ? "N/A" : TextUtils.join(", ", genres);
	}
	
	public String tags() {
		return tags.isEmpty() ? "N/D" : TextUtils.join(", ", tags);
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
	
	public void setCollected(boolean flag, int season) {
		if (!isNew()) {
			SQLiteDatabase db = session.getDB();
			db.beginTransaction();
			try {
				ContentValues cv = new ContentValues();
				cv.put("collected", flag);
				List<String> args = new ArrayList<String>();
				String where = "series = ?";
				args.add(tvdb_id);
				if (season >= 0) {
					where += " and season = ?";
					args.add(Integer.toString(season));
				}
				String[] wargs = new String[args.size()];
				wargs = args.toArray(wargs);
				db.update("episode", cv, where, wargs);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			if (!Title.ongoingServiceOperation) {
				Intent si = new Intent(session.getContext(), Service.class);
				si.setAction(Service.GDRIVE_BACKUP);
				si.putExtra("what", Commons.GD.SER_COLL);
				//session.getContext().startService(si);
				WakefulIntentService.sendWakefulWork(session.getContext(), si);
			}
			reloadEpisodes();
		}
		Episode.setDirtyFlags(tvdb_id, season, flag, null);
		if (isNew())
			commit(Commons.GD.SER_COLL);
		else
			dispatch(OnTitleListener.READY, null);
	}
	
	public void setWatched(boolean flag, int season) {
		if (!isNew()) {
			SQLiteDatabase db = session.getDB();
			db.beginTransaction();
			try {
				ContentValues cv = new ContentValues();
				cv.put("watched", flag);
				List<String> args = new ArrayList<String>();
				String where = "series = ?";
				args.add(tvdb_id);
				if (season >= 0) {
					where += " and season = ?";
					args.add(Integer.toString(season));
				}
				String[] wargs = new String[args.size()];
				wargs = args.toArray(wargs);
				db.update("episode", cv, where, wargs);
				db.setTransactionSuccessful();
			} finally {
				db.endTransaction();
			}
			if (!Title.ongoingServiceOperation) {
				Intent si = new Intent(session.getContext(), Service.class);
				si.setAction(Service.GDRIVE_BACKUP);
				si.putExtra("what", Commons.GD.SER_SEEN);
				//session.getContext().startService(si);
				WakefulIntentService.sendWakefulWork(session.getContext(), si);
			}
			reloadEpisodes();
		}
		Episode.setDirtyFlags(tvdb_id, season, null, flag);
		if (isNew())
			commit(Commons.GD.SER_SEEN);
		else
			dispatch(OnTitleListener.READY, null);
	}
}