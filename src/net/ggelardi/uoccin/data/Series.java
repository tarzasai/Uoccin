package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.XML.TVDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import net.ggelardi.uoccin.serv.SimpleCache;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.Toast;

public class Series extends Title {
    private static final String TAG = "Series";
	private static final String TABLE = "series";

	public static final String ARCHIVE = "CommitRequired";
	public static final String ACTIVE = "Continuing";
	public static final String ENDED = "Ended";
	
	private static final SimpleCache cache = new SimpleCache(500);
	
	private final Session session;
	
	private int rating = 0;
	private List<String> tags = new ArrayList<String>();
	private boolean watchlist = false;
	
	private boolean noRecalc = false;;
	private Episode lastep = null;
	private Episode nextep = null;
	private SparseIntArray seasons = new SparseIntArray();
	private int epcount = 0;
	private int epscoll = 0;
	private int epsseen = 0;
	
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
		Cursor cur = Session.getInstance(context).getDB().query(TABLE, null, "tvdb_id=?", new String[] { tvdb_id },
				null, null, null);
		try {
			if (cur.moveToFirst()) {
				res.load(cur);
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static Series get(Context context, String tvdb_id) {
		Series res = Series.getInstance(context, tvdb_id);
		//if (res.isNew() || res.isOld())
		if (res.isOld())
			res.refresh();
		return res;
	}
	
	public static Series get(Context context, Element xml) {
		String tvdb_id = xml.getElementsByTagName("id").item(0).getTextContent();
		Series res = Series.getInstance(context, tvdb_id);
		res.load(xml);
		if (!res.isNew())
			res.commit();
		return res;
	}
	
	public static List<Series> get(Context context, List<String> tvdb_ids) {
		List<Series> res = new ArrayList<Series>();
		for (String sid: tvdb_ids)
			res.add(Series.get(context, sid));
		return res;
	}
	
	public static List<Series> get(Context context, String query, String ... args) {
		List<Series> res = new ArrayList<Series>();
		Cursor cur = Session.getInstance(context).getDB().rawQuery(query, args);
		try {
			int ci = cur.getColumnIndex("tvdb_id");
			String tvdb_id;
			while (cur.moveToNext()) {
				tvdb_id = cur.getString(ci);
				res.add(Series.get(context, tvdb_id));
			}
		} finally {
			cur.close();
		}
		return res;
	}
	
	public static List<Series> find(Context context, String text) {
		List<Series> res = new ArrayList<Series>();
		String response;
		try {
			response = TVDB.getInstance().findSeries(text, "en");
		} catch (Exception err) {
			Log.e(TAG, "find", err);
			dispatch(OnTitleListener.ERROR, null);
			return res;
		}
		Document doc = Commons.XML.str2xml(response);
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
		recalc();
	}
	
	protected void save(boolean isnew) {
		Log.v(TAG, "Saving series " + tvdb_id);
		
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
	}
	
	protected void delete() {
		Log.v(TAG, "Deleting series " + tvdb_id);
		dispatch(OnTitleListener.WORKING, null);
		session.getDB().delete(TABLE, "tvdb_id=?", new String[] { tvdb_id });
		dispatch(OnTitleListener.READY, null);
	}
	
	public void refresh() {
		Log.v(TAG, "Refreshing series " + tvdb_id);
		dispatch(OnTitleListener.WORKING, null);
		Callback<String> callback = new Callback<String>() {
			@Override
			public void success(String result, Response response) {
				boolean store = !isNew() || (status != null && status.equals(ARCHIVE));
				Document doc = Commons.XML.str2xml(result);
				load((Element) doc.getElementsByTagName("Series").item(0));
				if (store)
					commit();
				// episodes
				noRecalc = true;
				try {
					long now = System.currentTimeMillis();
					if (isNew()) {
						seasons = new SparseIntArray();
						epcount = 0;
						epscoll = 0;
						epsseen = 0;
						lastep = null;
						nextep = null;
					}
					NodeList lst = doc.getElementsByTagName("Episode");
					if (lst != null && lst.getLength() > 0) {
						Episode ep;
						for (int i = 0; i < lst.getLength(); i++) {
							ep = Episode.get(session.getContext(), (Element) lst.item(i));
							if (isNew() && ep != null) {
								int en = seasons.get(ep.season);
								if (ep.episode > en)
									seasons.put(ep.season, ep.episode);
								epcount++;
								if (ep.firstAired <= 0)
									continue;
								if (ep.firstAired < now && (lastep == null || ep.firstAired > lastep.firstAired ||
									ep.episode > lastep.episode))
									lastep = ep;
								else if (ep.firstAired > now && (nextep == null || ep.firstAired < lastep.firstAired ||
									ep.episode < lastep.episode))
									nextep = ep;
							}
						}
					}
				} finally {
					noRecalc = false;
					if (!isNew())
						recalc();
				}
				dispatch(OnTitleListener.READY, null);
			}
			@Override
			public void failure(RetrofitError error) {
				Log.e(TAG, "refresh", error);
				
				plot = error.getLocalizedMessage();
				
				dispatch(OnTitleListener.ERROR, error);
			}
		};
		TVDB.getInstance().getFullSeries(tvdb_id, session.language(), callback);
	}
	
	public final void commit() {
		if (!modified)
			return;
		dispatch(OnTitleListener.WORKING, null);
		SQLiteDatabase db = session.getDB();
		db.beginTransaction();
		try {
			if (watchlist || rating > 0)
				save(isNew());
			else {
				boolean chk = false;
				String sql = "select count(*) from episode where series = ? and collected = 1 or watched = 1";
				Cursor qry = db.rawQuery(sql, new String[] { tvdb_id });
				try {
					chk = (qry.moveToNext() && qry.getInt(0) > 0);
				} finally {
					qry.close();
				}
				if (chk)
					save(isNew());
				else
					delete();
			}
			db.setTransactionSuccessful();
			modified = false;
		} finally {
			db.endTransaction();
		}
		dispatch(OnTitleListener.READY, null);
	}
	
	public synchronized void recalc() {
		if (noRecalc)
			return;
		dispatch(OnTitleListener.WORKING, null);
		String sql;
		String[] key = new String[] { tvdb_id };
		Cursor cur;
		// seasons list
		seasons = new SparseIntArray();
		sql = "select season, max(episode) from episode where series = ? group by season";
		cur = session.getDB().rawQuery(sql, key);
		try {
			while (cur.moveToNext())
				seasons.put(cur.getInt(0), cur.getInt(1));
		} finally {
			cur.close();
		}
		// episodes count
		epcount = 0;
		sql = "select count(*) from episode where series = ?";
		cur = session.getDB().rawQuery(sql, key);
		try {
			if (cur.moveToNext())
				epcount = cur.getInt(0);
		} finally {
			cur.close();
		}
		// collected episodes
		epscoll = 0;
		sql = "select count(*) from episode where series = ? and collected = 1";
		cur = session.getDB().rawQuery(sql, key);
		try {
			if (cur.moveToNext())
				epscoll = cur.getInt(0);
		} finally {
			cur.close();
		}
		// watched episodes
		epsseen = 0;
		sql = "select count(*) from episode where series = ? and watched = 1";
		cur = session.getDB().rawQuery(sql, key);
		try {
			if (cur.moveToNext())
				epsseen = cur.getInt(0);
		} finally {
			cur.close();
		}
		// last episode
		lastep = null;
		sql = "select season, episode from episode where series = ? and " +
			"datetime(firstAired/1000, 'unixepoch') <= datetime('now') order by firstAired desc, episode desc limit 1";
		cur = session.getDB().rawQuery(sql, key);
		try {
			if (cur.moveToNext())
				lastep = Episode.get(session.getContext(), tvdb_id, cur.getInt(0), cur.getInt(1));
		} finally {
			cur.close();
		}
		// next episode
		nextep = null;
		sql = "select season, episode from episode where series = ? and " +
			"datetime(firstAired/1000, 'unixepoch') > datetime('now') order by firstAired, episode limit 1";
		cur = session.getDB().rawQuery(sql, key);
		try {
			if (cur.moveToNext())
				nextep = Episode.get(session.getContext(), tvdb_id, cur.getInt(0), cur.getInt(1));
		} finally {
			cur.close();
		}
		//
		dispatch(OnTitleListener.READY, null);
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return timestamp > 0 && (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 168; // TODO preferences
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			modified = true;
			if (isNew()) {
				status = ARCHIVE;
				refresh();
			} else
				commit();
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
			if (isNew()) {
				status = ARCHIVE;
				refresh();
			} else
				commit();
		}
	}
	
	public List<String> getTags() {
		return new ArrayList<String>(tags);
	}
	
	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}
	
	public void addTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (!hasTag(tag)) {
			tags.add(tag);
			modified = true;
			if (isNew()) {
				status = ARCHIVE;
				refresh();
			} else
				commit();
			String msg = String.format(session.getRes().getString(R.string.msg_tags_add), name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public void delTag(String tag) {
		tag = tag.toLowerCase(Locale.getDefault());
		if (hasTag(tag)) {
			tags.remove(tag);
			modified = true;
			if (isNew()) {
				status = ARCHIVE;
				refresh();
			} else
				commit();
			String msg = String.format(session.getRes().getString(R.string.msg_tags_del), name);
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
	
	public Episode lastEpisode() {
		return lastep;
	}
	
	public Episode nextEpisode() {
		return nextep;
	}
	
	public Episode whatsBefore(int season, int episode) {
		if (isNew()) {
			List<Episode> lst = Episode.cached(tvdb_id, -1);
			Episode ep;
			for (int i = 1; i < lst.size(); i++) {
				ep = lst.get(i);
				if (ep.season == season && ep.episode == episode)
					return lst.get(i - 1);
			}
		} else {
			String sql = "select season, episode from episode where series = ? and season <= ? " +
				"order by season desc, episode desc";
			String[] flt = new String[] { tvdb_id, Integer.toString(season) };
			Cursor cr = session.getDB().rawQuery(sql, flt);
			try {
				while (cr.moveToNext()) {
					if (cr.getInt(0) == season && cr.getInt(1) >= episode)
						continue;
					return Episode.get(session.getContext(), tvdb_id, cr.getInt(0), cr.getInt(1));
				}
			} finally {
				cr.close();
			}
		}
		return null;
	}
	
	public Episode whatsAfter(int season, int episode) {
		if (isNew()) {
			List<Episode> lst = Episode.cached(tvdb_id, -1);
			Episode ep;
			for (int i = 0; i < lst.size() - 1; i++) {
				ep = lst.get(i);
				if (ep.season == season && ep.episode == episode)
					return lst.get(i + 1);
			}
		} else {
			String sql = "select season, episode from episode where series = ? and season >= ? " +
				"order by season, episode";
			String[] flt = new String[] { tvdb_id, Integer.toString(season) };
			Cursor cr = session.getDB().rawQuery(sql, flt);
			try {
				while (cr.moveToNext()) {
					if (cr.getInt(0) == season && cr.getInt(1) <= episode)
						continue;
					return Episode.get(session.getContext(), tvdb_id, cr.getInt(0), cr.getInt(1));
				}
			} finally {
				cr.close();
			}
		}
		return null;
	}
	
	public int seasonCount() {
		return seasons.size();
	}
	
	public int episodeCount() {
		return epcount;
	}
	
	public int episodeCount(int season) {
		return seasons.get(season);
	}
	
	public int episodeCollected() {
		return epscoll;
	}
	
	public int episodeWatched() {
		return epsseen;
	}
	
	public void setCollected(boolean flag, int season) {
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
			Episode.setDirtyFlags(tvdb_id, season, flag, null);
		} finally {
			db.endTransaction();
		}
		recalc();
	}
	
	public void setWatched(boolean flag, int season) {
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
			Episode.setDirtyFlags(tvdb_id, season, null, flag);
		} finally {
			db.endTransaction();
		}
		recalc();
	}
}