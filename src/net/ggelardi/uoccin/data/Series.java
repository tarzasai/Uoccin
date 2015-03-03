package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.api.XML.TVDB;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;

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
	
	private static final Set<Series> cache = Collections.newSetFromMap(new WeakHashMap<Series, Boolean>());
	
	private final Session session;
	
	private SparseIntArray seasons = new SparseIntArray();
	private int rating = 0;
	private List<String> tags = new ArrayList<String>();
	private boolean watchlist = false;
	private Episode lastep = null;
	private Episode nextep = null;
	
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
		for (Series m: cache)
			if (m.tvdb_id.equals(tvdb_id)) {
				Log.v(TAG, "Series " + tvdb_id + " found in cache");
				return m;
			}
		Series res = new Series(context, tvdb_id);
		cache.add(res);
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
		if (res.isNew() || res.isOld())
			res.refresh();
		return res;
	}
	
	public static Series get(Context context, Element xml) {
		String tvdb_id = xml.getElementsByTagName("id").item(0).getTextContent();
		Series res = Series.getInstance(context, tvdb_id);
		res.load(xml);
		if (res.modified && !res.isNew())
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
			response = TVDB.getInstance().findSeries(text, TVDB.preferredLanguage);
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
				(!name.equals(chk) && lang.equals(TVDB.preferredLanguage)))) {
				name = chk;
				modified = true;
			}
			chk = Commons.XML.nodeText(xml, "Overview");
			if (!TextUtils.isEmpty(chk) && (TextUtils.isEmpty(plot) ||
				(!plot.equals(chk) && lang.equals(TVDB.preferredLanguage)))) {
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
				
				//Log.e(TAG, chk, err);
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
		//
		seasons = new SparseIntArray();
		String sql = "select season, max(episode) from episode where series = ? group by season";
		Cursor eps = session.getDB().rawQuery(sql, new String[] { tvdb_id });
		try {
			while (eps.moveToNext())
				seasons.put(eps.getInt(0), eps.getInt(1));
		} finally {
			eps.close();
		}
	}
	
	protected void save(boolean isnew) {
		Log.v(TAG, "Saving series " + tvdb_id);
		
		ContentValues cv = new ContentValues();
		cv.put("tvdb_id", tvdb_id);
		cv.put("name", name);
		cv.put("year", year);
		cv.put("plot", plot);
		cv.put("poster", poster);
		cv.put("genres", TextUtils.join(",", genres));
		cv.put("actors", TextUtils.join(",", actors));
		cv.put("imdb_id", imdb_id);
		cv.put("status", status);
		cv.put("network", network);
		cv.put("firstAired", firstAired);
		cv.put("airsDay", airsDay);
		cv.put("airsTime", airsTime);
		cv.put("runtime", runtime);
		cv.put("rated", rated);
		cv.put("fanart", fanart);
		cv.put("rating", rating);
		cv.put("tags", TextUtils.join(",", tags));
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
				Document doc = Commons.XML.str2xml(result);
				load((Element) doc.getElementsByTagName("Series").item(0));
				commit();
				// episodes
				NodeList lst = doc.getElementsByTagName("Episode");
				if (lst != null && lst.getLength() > 0) {
					seasons = new SparseIntArray();
					Episode ep;
					for (int i = 0; i < lst.getLength(); i++) {
						ep = Episode.get(session.getContext(), (Element) lst.item(i));
						if (ep != null) {
							int chk = seasons.get(ep.season);
							if (ep.episode > chk)
								seasons.put(ep.season, chk);
							if (ep.firstAired > 0) {
								if (ep.firstAired <= System.currentTimeMillis() && (lastep == null || ep.isAfter(lastep)))
									lastep = ep;
								if (ep.firstAired > System.currentTimeMillis() && (nextep == null || ep.isBefore(nextep)))
									nextep = ep;
							}
						}
					}
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
		TVDB.getInstance().getFullSeries(tvdb_id, Locale.getDefault().getLanguage(), callback);
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
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public boolean isOld() {
		return (System.currentTimeMillis() - timestamp)/(1000 * 60 * 60) > 24; // TODO preferences
	}
	
	public boolean inWatchlist() {
		return watchlist;
	}
	
	public void setWatchlist(boolean value) {
		if (value != watchlist) {
			watchlist = value;
			modified = true;
			if (isNew())
				refresh();
			else
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
			if (isNew())
				refresh();
			else
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
			if (isNew())
				refresh();
			else
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
			if (isNew())
				refresh();
			else
				commit();
			String msg = String.format(session.getRes().getString(R.string.msg_tags_del), name);
			Toast.makeText(session.getContext(), msg, Toast.LENGTH_SHORT).show();
		}
	}
	
	public Episode lastEpisode() {
		if (lastep == null && seasons != null && seasons.size() > 0) {
			Episode ep;
			for (int s = 1; s <= seasons.size(); s++)
				for (int e = 1; e <= seasons.get(s); e++) {
					ep = Episode.get(session.getContext(), tvdb_id, s, e);
					if (ep.firstAired > 0 && ep.firstAired <= System.currentTimeMillis() &&
							(lastep == null || ep.isAfter(lastep)))
						lastep = ep;
				}
		}
		return lastep;
	}
	
	public Episode nextEpisode() {
		if (nextep == null && seasons != null && seasons.size() > 0) {
			Episode ep;
			for (int s = 1; s <= seasons.size(); s++)
				for (int e = 1; e <= seasons.get(s); e++) {
					ep = Episode.get(session.getContext(), tvdb_id, s, e);
					if (ep.firstAired > System.currentTimeMillis() && (nextep == null || ep.isBefore(nextep)))
						nextep = ep;
				}
		}
		return nextep;
	}
}