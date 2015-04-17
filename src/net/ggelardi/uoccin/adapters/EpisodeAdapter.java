package net.ggelardi.uoccin.adapters;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Episode;
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

public class EpisodeAdapter extends BaseAdapter {
	
	private final Session session;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	private List<Episode> items;
	private int pstHeight = 1;
	private int pstWidth = 1;
	private boolean switches = false;
	
	public EpisodeAdapter(Context context, OnClickListener clickListener) {
		super();
		
		session = Session.getInstance(context);
		listener = clickListener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		items = new ArrayList<Episode>();
	}
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public Episode getItem(int position) {
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
			view = inflater.inflate(R.layout.item_episode, parent, false);
			vh = new ViewHolder();
			vh.img_scrn = (ImageView) view.findViewById(R.id.img_epitm_scrn);
			vh.box_size = (LinearLayout) view.findViewById(R.id.box_epitm_size);
			vh.txt_name = (TextView) view.findViewById(R.id.txt_epitm_name);
			vh.txt_date = (TextView) view.findViewById(R.id.txt_epitm_date);
			vh.txt_subs = (TextView) view.findViewById(R.id.txt_epitm_subs);
			vh.img_coll = (ImageView) view.findViewById(R.id.img_epitm_coll);
			vh.img_seen = (ImageView) view.findViewById(R.id.img_epitm_seen);
			vh.box_flgs = (LinearLayout) view.findViewById(R.id.box_epitm_flgs);
			vh.img_coll.setOnClickListener(listener);
			vh.img_seen.setOnClickListener(listener);
			// calculate poster size
			if (pstHeight <= 1) {
				view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				pstHeight = view.getMeasuredHeight();
				pstWidth = Math.round((pstHeight*400)/225);
			}
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.box_flgs.setVisibility(switches ? View.VISIBLE : View.GONE);
		vh.img_coll.setTag(Integer.valueOf(position));
		vh.img_seen.setTag(Integer.valueOf(position));
		Episode ep = getItem(position);
		String scrn = ep.poster;
		if (TextUtils.isEmpty(scrn))
			scrn = ep.getSeries().fanart;
		if (TextUtils.isEmpty(scrn))
			vh.img_scrn.setVisibility(View.GONE);
		else {
			vh.img_scrn.setVisibility(View.VISIBLE);
			session.picasso(scrn).resize(pstWidth, pstHeight).into(vh.img_scrn);
		}
		vh.txt_name.setText(Integer.toString(ep.episode) + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name));
		if (ep.isWatched())
			vh.txt_name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_seen, 0, 0, 0);
		else if (ep.inCollection())
			vh.txt_name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_storage, 0, 0, 0);
		else if (ep.isPilot())
			vh.txt_name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_active_news, 0, 0, 0);
		else
			vh.txt_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		vh.txt_date.setText(ep.firstAired());
		vh.txt_date.setCompoundDrawablesWithIntrinsicBounds(ep.isToday() ? R.drawable.ics_active_calendar : 0, 0, 0, 0);
		vh.txt_subs.setText(ep.subtitles());
		vh.txt_subs.setVisibility(ep.hasSubtitles() ? View.VISIBLE : View.GONE);
		vh.img_coll.setImageResource(ep.inCollection() ? R.drawable.ic_active_storage : R.drawable.ic_action_storage);
		vh.img_seen.setImageResource(ep.isWatched() ? R.drawable.ic_active_seen : R.drawable.ic_action_seen);
		return view;
	}
	
	public void setTitles(List<Episode> titles) {
		items = titles;
		switches = false;
    	notifyDataSetChanged();
	}
	
	public void setSwitches(boolean value) {
		switches = value;
		notifyDataSetChanged();
	}
	
	public boolean getSwitches() {
		return switches;
	}
	
	static class ViewHolder {
		public ImageView img_scrn;
		public LinearLayout box_size;
		public TextView txt_name;
		public TextView txt_date;
		public TextView txt_subs;
		public LinearLayout box_flgs;
		public ImageView img_coll;
		public ImageView img_seen;
	}
}