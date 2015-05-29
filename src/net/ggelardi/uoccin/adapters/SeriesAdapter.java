package net.ggelardi.uoccin.adapters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.serv.Commons.TL;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

public class SeriesAdapter extends BaseAdapter implements Filterable {
	
	private final Session session;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	private final String type;
	private final String data;
	private List<Series> allItems;
	private List<Series> fltItems;
	private ItemFilter filterObject = new ItemFilter();
	private String filterText = null;
	private int pstHeight = 1;
	private int pstWidth = 1;
	
	public SeriesAdapter(Context context, OnClickListener clickListener, String type, String data) {
		super();
		
		session = Session.getInstance(context);
		listener = clickListener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.type = type;
		this.data = data;
		allItems = new ArrayList<Series>();
		fltItems = new ArrayList<Series>();
	}
	
	@Override
	public int getCount() {
		return fltItems.size();
	}
	
	@Override
	public Series getItem(int position) {
		return fltItems.get(position);
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
			vh.img_post = (ImageView) view.findViewById(R.id.img_is_poster);
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
			vh.rat_myrt = (RatingBar) view.findViewById(R.id.rat_is_myrt);
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
		session.picasso(ser.poster, true).resize(pstWidth, pstHeight).into(vh.img_post);
		if (pstWidth > 1) {
			vh.img_post.setMinimumWidth(pstWidth);
			vh.img_post.setMaxWidth(pstWidth);
			vh.img_post.setMinimumHeight(pstHeight);
			vh.img_post.setMaxHeight(pstHeight);
		}
		vh.txt_name.setText(ser.name);
		vh.txt_name.setCompoundDrawablesWithIntrinsicBounds(ser.isRecent() ? R.drawable.ics_active_news : 0, 0, 0, 0);
		vh.img_star.setImageResource(ser.inWatchlist() ? R.drawable.ic_active_loved : R.drawable.ic_action_loved);
		if (ser.isNew() || type.equals(TL.SEARCH)) {
			vh.txt_plot.setVisibility(View.VISIBLE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.GONE);
			vh.box_stat.setVisibility(View.GONE);
			vh.txt_subs.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.GONE);
			//
			vh.txt_plot.setText(ser.plot);
		} else if (data.equals("sermiss")) {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.VISIBLE);
			vh.box_stat.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.GONE);
			//
			Episode ep = null;
			for (int i = 0; i < ser.episodes.size(); i++) {
				ep = ser.episodes.get(i);
				if (!(ep.inCollection() || ep.isWatched()))
					break;
			}
			vh.box_2see.setTag(ep.eid());
			String title = ep.eid().readable() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name);
			int miss = ser.episodeMissing();
			if (miss > 1)
				title += " (+" + Integer.toString(miss - 1) + ")";
			vh.txt_2tit.setText(title);
			vh.txt_2plo.setText(ep.plot());
			vh.txt_subs.setVisibility(View.GONE);
		} else if (data.equals("ser2see")) {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.VISIBLE);
			vh.box_stat.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.GONE);
			//
			Episode ep = null;
			for (int i = 0; i < ser.episodes.size(); i++) {
				ep = ser.episodes.get(i);
				if (ep.inCollection() && !ep.isWatched())
					break;
			}
			vh.box_2see.setTag(ep.eid());
			String title = ep.eid().readable() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name);
			int waits = ser.episodeWaiting(null);
			if (waits > 1)
				title += " (+" + Integer.toString(waits - 1) + ")";
			vh.txt_2tit.setText(title);
			vh.txt_2plo.setText(ep.plot());
			vh.txt_subs.setText(ep.subtitles());
			vh.txt_subs.setVisibility(ep.hasSubtitles() ? View.VISIBLE : View.GONE);
		} else if (data.equals("serstat")) {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.GONE);
			vh.box_2see.setVisibility(View.GONE);
			vh.box_stat.setVisibility(View.VISIBLE);
			vh.txt_subs.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.VISIBLE);
			//
			int n = ser.episodeAired(null);
			int m = ser.episodeCollected(null);
			vh.txt_coll.setText(m == n ? String.format(session.getString(R.string.fmt_nums_done), m) :
				String.format(session.getString(R.string.fmt_nums_coll), m, n - m));
			m = ser.episodeWatched(null);
			vh.txt_seen.setText(m == n ? String.format(session.getString(R.string.fmt_nums_done), m) :
				String.format(session.getString(R.string.fmt_nums_seen), m, n - m));
			vh.rat_myrt.setRating(ser.getRating());
		} else {
			vh.txt_plot.setVisibility(View.GONE);
			vh.box_epis.setVisibility(View.VISIBLE);
			vh.box_2see.setVisibility(View.GONE);
			vh.box_stat.setVisibility(View.GONE);
			vh.txt_subs.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.VISIBLE);
			//
			vh.rat_myrt.setRating(ser.getRating());
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
			else if (data.equals("sernext"))
				ep = chk.get(1);
			else if ((now - chk.get(0).firstAired) >= (chk.get(1).firstAired - now))
				ep = chk.get(1);
			else
				ep = chk.get(0);
			//
			if (ep == null) {
				vh.txt_epis.setText("N/A");
				vh.txt_epis.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
				vh.txt_date.setText("N/A");
				vh.txt_date.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			} else {
				vh.txt_epis.setText(ep.eid().readable() + " - " + (TextUtils.isEmpty(ep.name) ? "N/A" : ep.name));
				vh.txt_epis.setCompoundDrawablesWithIntrinsicBounds(ep.inCollection() ?
					R.drawable.ics_action_storage : 0, 0, 0, 0);
				vh.txt_date.setText(ep.firstAired());
				vh.txt_date.setCompoundDrawablesWithIntrinsicBounds(ep.isToday() ?
					R.drawable.ics_action_calendar : 0, 0, 0, 0);
			}
		}
		vh.txt_info.setText(ser.airInfo());
		return view;
	}
	
	@Override
	public Filter getFilter() {
		return filterObject;
	}
	
	public void setTitles(List<Series> titles, boolean forceReload) {
		allItems = titles;
		if (forceReload)
			for (Series ser: allItems)
				ser.reload();
		filterObject.filter(filterText);
	}
	
	public void setFilter(int scope, String text) {
		filterObject.scope = scope;
		filterObject.filter(text);
	}
	
	static class ViewHolder {
		ImageView img_post;
		LinearLayout box_size;
		ImageView img_star;
		TextView txt_name;
		TextView txt_plot;
		TextView txt_info;
		TextView txt_subs;
		RatingBar rat_myrt;
		LinearLayout box_epis;
		TextView txt_epis;
		TextView txt_date;
		LinearLayout box_stat;
		TextView txt_coll;
		TextView txt_seen;
		LinearLayout box_2see;
		TextView txt_2tit;
		TextView txt_2plo;
	}
	
	private class ItemFilter extends Filter {
		public int scope = 0;
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			filterText = TextUtils.isEmpty(constraint) ? null : constraint.toString().toLowerCase(Locale.getDefault());
			final List<Series> list = allItems;
			final ArrayList<Series> nlist = new ArrayList<Series>(list.size());
			for (Series ser: list)
				if (TextUtils.isEmpty(filterText) ||
					(scope == 0 && ser.name().toLowerCase(Locale.getDefault()).contains(filterText)) ||
					(scope == 1 && ser.people().toLowerCase(Locale.getDefault()).contains(filterText)) ||
					(scope == 2 && ser.plot().toLowerCase(Locale.getDefault()).contains(filterText)) ||
					(scope == 3 && ser.year().contains(filterText)) ||
					(scope == 4 && ser.hasTag(filterText)) ||
					(scope == 5 && ser.rating().contains(filterText)))
					nlist.add(ser);
			FilterResults results = new FilterResults();
			results.values = nlist;
			results.count = nlist.size();
			return results;
		}
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			fltItems.clear();
			fltItems.addAll((ArrayList<Series>) results.values);
			notifyDataSetChanged();
		}
	}
}