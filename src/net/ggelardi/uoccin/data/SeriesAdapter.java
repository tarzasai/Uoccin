package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Calendar;
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
import android.widget.TextView;

public class SeriesAdapter extends BaseAdapter {
	
	private final Session session;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	private List<Series> items;
	
	public SeriesAdapter(Context context, OnClickListener clickListener) {
		super();
		
		session = Session.getInstance(context);
		listener = clickListener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			vh.txt_ser_plot = (TextView) view.findViewById(R.id.txt_ser_plot);
			vh.txt_ser_epis = (TextView) view.findViewById(R.id.txt_ser_epis);
			vh.txt_ser_date = (TextView) view.findViewById(R.id.txt_ser_date);
			vh.txt_ser_info = (TextView) view.findViewById(R.id.txt_ser_info);
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
		if (ser.isNew()) {
			vh.txt_ser_plot.setVisibility(View.VISIBLE);
			vh.txt_ser_epis.setVisibility(View.GONE);
			vh.txt_ser_date.setVisibility(View.GONE);
			//
			vh.txt_ser_plot.setText(ser.plot);
			vh.txt_ser_info.setText(ser.network);
		} else {
			vh.txt_ser_plot.setVisibility(View.GONE);
			vh.txt_ser_epis.setVisibility(View.VISIBLE);
			vh.txt_ser_date.setVisibility(View.VISIBLE);
			//
			Episode eL = ser.lastEpisode();
			Episode eN = ser.nextEpisode();
			Episode ep = null;
			if (eL != null && eN != null) {
				if (System.currentTimeMillis() - eL.firstAired >= eN.firstAired - System.currentTimeMillis())
					ep = eN;
				else
					ep = eL;
			} else if (eN != null)
				ep = eN;
			else if (eL != null)
				ep = eL;
			if (ep == null) {
				vh.txt_ser_epis.setText("N/A");
				vh.txt_ser_epis.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				vh.txt_ser_date.setText("N/A");
			} else {
				vh.txt_ser_epis.setText(ep.simpleEID() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name));
				vh.txt_ser_epis.setCompoundDrawablesWithIntrinsicBounds(ep.isPilot() ? R.drawable.ic_small_news : 0,
					0, 0, 0);
				vh.txt_ser_date.setText(DateUtils.getRelativeTimeSpanString(ep.firstAired,
					System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString());
			}
			String info = ser.network;
			if (ser.airsDay > 0 && ser.airsTime > 0) {
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(ser.airsTime);
				cal.set(Calendar.DAY_OF_WEEK, ser.airsDay);
				info += " - " + Commons.SDF.loc(session.getString(R.string.fmtdt_airtime)).format(cal.getTime());
			}
			vh.txt_ser_info.setText(info);
		}
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
		public TextView txt_ser_plot;
		public TextView txt_ser_epis;
		public TextView txt_ser_date;
		public TextView txt_ser_info;
	}
}