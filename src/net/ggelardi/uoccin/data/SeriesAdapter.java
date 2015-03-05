package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SeriesAdapter extends BaseAdapter {
	public static final String SERIES_PLOT = "SERIES_PLOT";
	public static final String SERIES_STATS = "SERIES_STATS";
	public static final String LAST_EPISODE = "LAST_EPISODE";
	public static final String NEXT_EPISODE = "NEXT_EPISODE";
	public static final String NEAR_EPISODE = "NEAR_EPISODE";
	
	private final Session session;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	private final String details;
	private List<Series> items;
	
	public SeriesAdapter(Context context, OnClickListener clickListener, String details) {
		super();
		
		session = Session.getInstance(context);
		listener = clickListener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.details = details;
		items = new ArrayList<Series>();
	}
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public Series getItem(int position) {
		return items.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_series, parent, false);
			vh = new ViewHolder();
			vh.img_ser_poster = (ImageView) view.findViewById(R.id.img_ser_poster);
			vh.img_ser_star = (ImageView) view.findViewById(R.id.img_ser_star);
			vh.txt_ser_name = (TextView) view.findViewById(R.id.txt_ser_name);
			vh.txt_ser_info = (TextView) view.findViewById(R.id.txt_ser_info);
			vh.txt_ser_plot = (TextView) view.findViewById(R.id.txt_ser_plot);
			vh.box_ser_stat = (LinearLayout) view.findViewById(R.id.box_ser_stat);
			vh.txt_ser_coll = (TextView) view.findViewById(R.id.txt_ser_coll);
			vh.txt_ser_seen = (TextView) view.findViewById(R.id.txt_ser_seen);
			vh.box_ser_epis = (LinearLayout) view.findViewById(R.id.box_ser_epis);
			vh.txt_ser_epis = (TextView) view.findViewById(R.id.txt_ser_epis);
			vh.txt_ser_date = (TextView) view.findViewById(R.id.txt_ser_date);
			vh.img_ser_star.setOnClickListener(listener);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.img_ser_star.setTag(Integer.valueOf(position));
		Series ser = getItem(position);
		session.picasso().load(ser.poster).placeholder(R.drawable.ic_action_image).fit().into(vh.img_ser_poster);
		vh.img_ser_star.setImageResource(ser.inWatchlist() ? R.drawable.ic_active_loved : R.drawable.ic_action_loved);
		vh.txt_ser_name.setText(ser.name);
		if (ser.isNew() || details.equals(SERIES_PLOT)) {
			vh.txt_ser_plot.setVisibility(View.VISIBLE);
			vh.box_ser_stat.setVisibility(View.GONE);
			vh.box_ser_epis.setVisibility(View.GONE);
			//
			vh.txt_ser_plot.setText(ser.plot);
		} else if (details.equals(SERIES_STATS)) {
			vh.txt_ser_plot.setVisibility(View.GONE);
			vh.box_ser_stat.setVisibility(View.VISIBLE);
			vh.box_ser_epis.setVisibility(View.GONE);
			//
			vh.txt_ser_coll.setText(String.format(session.getString(R.string.fmtno_colls), ser.episodeCollected(), ser.episodeCount()));
			vh.txt_ser_seen.setText(String.format(session.getString(R.string.fmtno_seens), ser.episodeWatched(), ser.episodeCount()));
		} else {
			vh.txt_ser_plot.setVisibility(View.GONE);
			vh.box_ser_stat.setVisibility(View.GONE);
			vh.box_ser_epis.setVisibility(View.VISIBLE);
			//
			List<Episode> chk = new ArrayList<Episode>();
			chk.add(ser.lastEpisode());
			chk.add(ser.nextEpisode());
			chk.removeAll(Collections.singleton(null));
			long now = System.currentTimeMillis();
			Episode ep;
			if (chk.isEmpty())
				ep = null;
			else if (chk.size() == 1)
				ep = chk.get(0);
			else if (details.equals(LAST_EPISODE))
				ep = chk.get(0);
			else if (details.equals(NEXT_EPISODE))
				ep = chk.get(1);
			else if ((now - chk.get(0).firstAired) >= (chk.get(1).firstAired - now))
				ep = chk.get(1);
			else
				ep = chk.get(0);
			if (ep == null) {
				vh.txt_ser_epis.setText("N/A");
				vh.txt_ser_epis.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				vh.txt_ser_date.setText("N/A");
			} else {
				vh.txt_ser_epis.setText(ep.simpleEID() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name));
				vh.txt_ser_epis.setCompoundDrawablesWithIntrinsicBounds(ep.isPilot() ? R.drawable.ic_small_news : 0,
					0, 0, 0);
				if (DateUtils.isToday(ep.firstAired)) {
					vh.txt_ser_date.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_small_clock, 0, 0, 0);
					vh.txt_ser_date.setText(DateUtils.getRelativeTimeSpanString(ep.firstAired,
						System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString());
				} else {
					vh.txt_ser_date.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
					String ts = DateUtils.getRelativeTimeSpanString(ep.firstAired,
						System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS).toString();
					if (Math.abs(now - ep.firstAired)/(1000 * 60 * 60) < 168)
						ts += " (" + Commons.SDF.loc("EEE").format(ep.firstAired) + ")";
					vh.txt_ser_date.setText(ts);
				}
			}
		}
		String info = ser.network;
		if (ser.airsDay > 0 && ser.airsTime > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(ser.airsTime);
			cal.set(Calendar.DAY_OF_WEEK, ser.airsDay);
			info += " - " + Commons.SDF.loc(session.getString(R.string.fmtdt_airtime)).format(cal.getTime());
		}
		vh.txt_ser_info.setText(info);
		return view;
	}
	
	public void setTitles(List<Series> titles) {
		items = titles;
    	notifyDataSetChanged();
	}
	
	static class ViewHolder {
		public ImageView img_ser_poster;
		public ImageView img_ser_star;
		public TextView txt_ser_name;
		public TextView txt_ser_info;
		public TextView txt_ser_plot;
		public LinearLayout box_ser_stat;
		public TextView txt_ser_coll;
		public TextView txt_ser_seen;
		public LinearLayout box_ser_epis;
		public TextView txt_ser_epis;
		public TextView txt_ser_date;
	}
}