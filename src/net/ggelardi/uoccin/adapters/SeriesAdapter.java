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
import android.text.format.DateUtils;
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
			vh.img_ser_pstr = (ImageView) view.findViewById(R.id.img_seritm_poster);
			vh.box_ser_size = (LinearLayout) view.findViewById(R.id.box_seritm_size);
			vh.img_ser_star = (ImageView) view.findViewById(R.id.img_seritm_star);
			vh.txt_ser_name = (TextView) view.findViewById(R.id.txt_seritm_name);
			vh.txt_ser_info = (TextView) view.findViewById(R.id.txt_seritm_info);
			vh.txt_ser_plot = (TextView) view.findViewById(R.id.txt_seritm_plot);
			vh.box_ser_stat = (LinearLayout) view.findViewById(R.id.box_seritm_stat);
			vh.txt_ser_coll = (TextView) view.findViewById(R.id.txt_seritm_coll);
			vh.txt_ser_seen = (TextView) view.findViewById(R.id.txt_seritm_seen);
			vh.box_ser_epis = (LinearLayout) view.findViewById(R.id.box_seritm_epis);
			vh.txt_ser_epis = (TextView) view.findViewById(R.id.txt_seritm_epis);
			vh.txt_ser_date = (TextView) view.findViewById(R.id.txt_seritm_date);
			vh.img_ser_star.setOnClickListener(listener);
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
		vh.img_ser_star.setTag(Integer.valueOf(position));
		Series ser = getItem(position);
		session.picasso(ser.poster).resize(pstWidth, pstHeight).into(vh.img_ser_pstr);
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
			vh.txt_ser_coll.setText(String.format(session.getString(R.string.fmt_nums_coll), ser.episodeCollected(null), ser.episodeCount(null)));
			vh.txt_ser_seen.setText(String.format(session.getString(R.string.fmt_nums_seen), ser.episodeWatched(null), ser.episodeCount(null)));
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
			//
			if (ep == null) {
				vh.txt_ser_epis.setText("N/A");
				vh.txt_ser_epis.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				vh.txt_ser_date.setText("N/A");
			} else {
				vh.txt_ser_epis.setText(ep.simpleEID() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name));
				vh.txt_ser_epis.setCompoundDrawablesWithIntrinsicBounds(ep.isPilot() ? R.drawable.ic_small_news : 0,
					0, 0, 0);
				vh.txt_ser_date.setCompoundDrawablesWithIntrinsicBounds(DateUtils.isToday(ep.firstAired) ?
					R.drawable.ic_small_calendar : 0, 0, 0, 0);
				vh.txt_ser_date.setText(ep.firstAired());
			}
		}
		vh.txt_ser_info.setText(ser.airInfo());
		return view;
	}
	
	public void setTitles(List<Series> titles) {
		items = titles;
    	notifyDataSetChanged();
	}
	
	static class ViewHolder {
		public ImageView img_ser_pstr;
		public LinearLayout box_ser_size;
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