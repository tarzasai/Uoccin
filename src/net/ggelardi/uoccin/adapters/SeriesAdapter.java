package net.ggelardi.uoccin.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SeriesAdapter extends BaseAdapter {
	public static final String SERIES_STORY = "SERIES_STORY";
	public static final String EPI_AIR_NEAR = "EPI_AIR_NEAR";
	public static final String EPI_AIR_NEXT = "EPI_AIR_NEXT";
	public static final String EPI_AVAILABL = "EPI_AVAILABL";
	public static final String EPI_COUNTERS = "EPI_COUNTERS";
	
	private final Session session;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	private final String details;
	private List<Series> items;
	private int pstHeight = 1;
	private int pstWidth = 1;
	
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
		final ViewHolder vh;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_series, parent, false);
			vh = new ViewHolder();
			vh.img_pstr = (ImageView) view.findViewById(R.id.img_is_poster);
			vh.box_size = (LinearLayout) view.findViewById(R.id.box_is_size);
			vh.img_star = (ImageView) view.findViewById(R.id.img_is_star);
			vh.txt_name = (TextView) view.findViewById(R.id.txt_is_series);
			vh.txt_plot = (TextView) view.findViewById(R.id.txt_is_seplot);
			vh.box_epis = (LinearLayout) view.findViewById(R.id.box_is_anyep);
			vh.txt_epis = (TextView) view.findViewById(R.id.txt_is_anyid);
			vh.txt_date = (TextView) view.findViewById(R.id.txt_is_anyad);
			vh.box_2see = (LinearLayout) view.findViewById(R.id.box_is_avail);
			vh.txt_2tit = (TextView) view.findViewById(R.id.txt_is_avtit);
			vh.txt_2plo = (TextView) view.findViewById(R.id.txt_is_avplot);
			vh.box_stat = (LinearLayout) view.findViewById(R.id.box_is_count);
			vh.txt_coll = (TextView) view.findViewById(R.id.txt_seritm_coll);
			vh.txt_seen = (TextView) view.findViewById(R.id.txt_seritm_seen);
			vh.txt_subs = (TextView) view.findViewById(R.id.txt_is_subs);
			vh.txt_info = (TextView) view.findViewById(R.id.txt_is_info);
			//
			vh.img_star.setOnClickListener(listener);
			vh.box_2see.setOnClickListener(listener);
			//
			if (pstHeight <= 1) {
				view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				pstHeight = view.getMeasuredHeight();
				pstWidth = Math.round((pstHeight*340)/500);
			}
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		//
		vh.img_star.setTag(Integer.valueOf(position));
		//
		Series ser = getItem(position);
		session.picasso(ser.poster, true).resize(pstWidth, pstHeight).into(vh.img_pstr);
		if (pstWidth > 1)
			vh.img_pstr.setMinimumWidth(pstWidth);
		vh.img_star.setImageResource(ser.inWatchlist() ? R.drawable.ic_active_loved : R.drawable.ic_action_loved);
		vh.txt_name.setText(ser.name);
		vh.txt_name.setCompoundDrawablesWithIntrinsicBounds(ser.isRecent() ? R.drawable.ics_active_news : 0, 0, 0, 0);
		if (ser.isNew() || details.equals(SERIES_STORY)) {
			vh.txt_plot.setVisibility(View.VISIBLE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.GONE);
			vh.box_stat.setVisibility(View.GONE);
			vh.txt_subs.setVisibility(View.GONE);
			//
			vh.txt_plot.setText(ser.plot);
		} else if (details.equals(EPI_AVAILABL)) {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.VISIBLE);
			vh.box_stat.setVisibility(View.GONE);
			//
			Episode ep = null;
			for (int i = 0; i < ser.episodes.size(); i++) {
				ep = ser.episodes.get(i);
				if (ep.inCollection() && !ep.isWatched())
					break;
			}
			vh.box_2see.setTag(ep.eid());
			String title = ep.eid().readable() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name);
			if (ser.episodeWaiting(null) > 1)
				title += " (+" + Integer.toString(ser.episodeWaiting(null) - 1) + ")";
			vh.txt_2tit.setText(title);
			vh.txt_2plo.setText(ep.plot());
			vh.txt_subs.setText(ep.subtitles());
			vh.txt_subs.setVisibility(ep.hasSubtitles() ? View.VISIBLE : View.GONE);
		} else if (details.equals(EPI_COUNTERS)) {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.GONE);
			vh.box_stat.setVisibility(View.VISIBLE);
			vh.txt_subs.setVisibility(View.GONE);
			//
			int n = ser.episodeAired(null);
			int m = ser.episodeCollected(null);
			vh.txt_coll.setText(m == n ? String.format(session.getString(R.string.fmt_nums_done), m) :
				String.format(session.getString(R.string.fmt_nums_coll), m, n - m));
			m = ser.episodeWatched(null);
			vh.txt_seen.setText(m == n ? String.format(session.getString(R.string.fmt_nums_done), m) :
				String.format(session.getString(R.string.fmt_nums_seen), m, n - m));
		} else {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.VISIBLE);
			vh.box_2see.setVisibility(View.GONE);
			vh.box_stat.setVisibility(View.GONE);
			vh.txt_subs.setVisibility(View.GONE);
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
			else if (details.equals(EPI_AIR_NEXT))
				ep = chk.get(1);
			else if ((now - chk.get(0).firstAired) >= (chk.get(1).firstAired - now))
				ep = chk.get(1);
			else
				ep = chk.get(0);
			//
			if (ep == null) {
				vh.txt_epis.setText("N/A");
				//vh.txt_epis.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				vh.txt_date.setText("N/A");
				vh.txt_date.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			} else {
				vh.txt_epis.setText(ep.eid().readable() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name));
				/*vh.txt_epis.setCompoundDrawablesWithIntrinsicBounds(ep.isPilot() ?
					R.drawable.ics_action_news : 0, 0, 0, 0);*/
				vh.txt_date.setText(ep.firstAired());
				vh.txt_date.setCompoundDrawablesWithIntrinsicBounds(ep.isToday() ?
					R.drawable.ics_action_calendar : 0, 0, 0, 0);
			}
		}
		vh.txt_info.setText(ser.airInfo());
		return view;
	}
	
	public void setTitles(List<Series> titles, boolean forceReload) {
		items = titles;
		if (forceReload)
			for (Series ser: items)
				ser.reload();
    	notifyDataSetChanged();
	}
	
	static class ViewHolder {
		public ImageView img_pstr;
		public LinearLayout box_size;
		public ImageView img_star;
		public TextView txt_name;
		public TextView txt_plot;
		public TextView txt_info;
		public TextView txt_subs;
		public LinearLayout box_epis;
		public TextView txt_epis;
		public TextView txt_date;
		public LinearLayout box_stat;
		public TextView txt_coll;
		public TextView txt_seen;
		public LinearLayout box_2see;
		public TextView txt_2tit;
		public TextView txt_2plo;
	}
}