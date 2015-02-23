package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DashboardAdapter extends BaseAdapter {
	private static final int HDR_CAL = 0;
	private static final int HDR_PRE = 1;
	private static final int HDR_CSE = 2;
	private static final int HDR_CMV = 3;
	private static final int ITM_SER = 4;
	private static final int ITM_SEP = 5;
	private static final int ITM_MOV = 6;
	
	private final Session session;
	private final LayoutInflater inflater;
	private final List<DashboardItem> items;
	
	public DashboardAdapter(Context context) {
		super();
		
		session = Session.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		items = new ArrayList<DashboardAdapter.DashboardItem>();
		
		List<String> ids = new ArrayList<String>();
		
		String today = Commons.DateStuff.locale("yyyy-MM-dd").format(new Date(System.currentTimeMillis()));
		
		// calendar
		String query = "select e.series, e.season, e.episode from episode e join series s on (e.series = s.tvdb_id) " +
			"where date(e.firstAired) = ? and s.watchlist = 1 order by s.name, e.season, e.episode";
		List<Episode> eps = Episode.get(context, query, today);
		if (!eps.isEmpty()) {
			items.add(new DashboardItem(HDR_CAL));
			for (Episode ep: eps) {
				items.add(new DashboardItem(ITM_SEP, ep));
				ids.add(ep.extendedEID());
			}
		}
		
		// premieres
		query = "select s.tvdb_id from episode e join series s on (e.series = s.tvdb_id) " +
			"where date(e.firstAired) = ? and s.watchlist = 0 and e.season = 1 and e.episode = 1 order by s.name";
		List<Series> ses = Series.get(context, query, today);
		if (!ids.isEmpty()) {
			items.add(new DashboardItem(HDR_PRE));
			for (Series ser: ses)
				items.add(new DashboardItem(ITM_SER, ser));
		}
		
		// available episodes
		query = "select e.series, e.season, e.episode from episode e join series s on (e.series = s.tvdb_id) " +
			"where e.collected = 1 and e.watched = 0 order by s.name, e.season, e.episode";
		eps = Episode.get(context, query, (String[]) null);
		if (!eps.isEmpty()) {
			boolean hdr = false;
			for (Episode ep: eps)
				if (!ids.contains(ep.extendedEID())) {
					if (!hdr)
						hdr = items.add(new DashboardItem(HDR_CSE));
					items.add(new DashboardItem(ITM_SEP, ep));
					ids.add(ep.extendedEID());
				}
		}
		
		// available movies
		query = "select imdb_id from movie where collected = 1 and watched = 0 order by name";
		List<Movie> mvs = Movie.get(context, query, (String[]) null);
		if (!mvs.isEmpty()) {
			items.add(new DashboardItem(HDR_CMV));
			for (Movie mv: mvs)
				items.add(new DashboardItem(ITM_MOV, mv));
		}
	}
	
	/*
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return !getItem(position).header;
	}
	*/
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public DashboardItem getItem(int position) {
		return items.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		return getItem(position).kind;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			vh = new ViewHolder();
			switch (getItemViewType(position)) {
				case HDR_CAL:
				case HDR_PRE:
				case HDR_CSE:
					view = inflater.inflate(R.layout.header_dashboard, parent, false);
					vh.txt_header = (TextView) view.getRootView();
					break;
				case HDR_CMV:
					break;
				case ITM_SER:
					break;
				case ITM_SEP:
					view = inflater.inflate(R.layout.item_episode, parent, false);
					vh.txt_ep_head = (TextView) view.findViewById(R.id.txt_ep_head);
					vh.txt_ep_name = (TextView) view.findViewById(R.id.txt_ep_name);
					vh.txt_ep_plot = (TextView) view.findViewById(R.id.txt_ep_plot);
					vh.txt_ep_flgs = (TextView) view.findViewById(R.id.txt_ep_flgs);
					vh.img_ep_coll = (ImageView) view.findViewById(R.id.img_ep_coll);
					vh.img_ep_seen = (ImageView) view.findViewById(R.id.img_ep_seen);
					vh.img_ep_subs = (ImageView) view.findViewById(R.id.img_ep_subs);
					vh.img_ep_scrn = (ImageView) view.findViewById(R.id.img_ep_scrn);
					break;
				case ITM_MOV:
					break;
			}
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		DashboardItem itm = getItem(position);
		switch (itm.kind) {
			case HDR_CAL:
				vh.txt_header.setText(R.string.dashboard_calendar);
				vh.txt_header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_calendar, 0, 0, 0);
				break;
			case HDR_PRE:
				vh.txt_header.setText(R.string.dashboard_premieres);
				vh.txt_header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_premiere, 0, 0, 0);
				break;
			case HDR_CSE:
				vh.txt_header.setText(R.string.dashboard_availeps);
				vh.txt_header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_tv, 0, 0, 0);
				break;
			case HDR_CMV:
				vh.txt_header.setText(R.string.dashboard_availmvs);
				vh.txt_header.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_movie, 0, 0, 0);
				break;
			case ITM_SER:
				break;
			case ITM_SEP:
				Episode ep = (Episode) itm.title;
				Series series = ep.getSeries();
				vh.txt_ep_head.setText(series.name);
				vh.txt_ep_name.setText(ep.simpleEID() + " - " + session.defaultText(ep.name, R.string.unknown_title));
				vh.txt_ep_plot.setText(session.defaultText(ep.plot, R.string.unknown_plot));
				vh.txt_ep_flgs.setVisibility(ep.inCollection() || ep.isWatched() || ep.hasSubtitles() ? View.VISIBLE : View.GONE);
				vh.img_ep_coll.setVisibility(ep.inCollection() ? View.VISIBLE : View.GONE);
				vh.img_ep_seen.setVisibility(ep.isWatched() ? View.VISIBLE : View.GONE);
				vh.img_ep_subs.setVisibility(ep.hasSubtitles() ? View.VISIBLE : View.GONE);
				if (!TextUtils.isEmpty(ep.poster))
					session.picasso().load(ep.poster).placeholder(R.drawable.ic_action_image).fit().into(vh.img_ep_scrn);
				else if (!TextUtils.isEmpty(series.fanart))
					session.picasso().load(series.fanart).placeholder(R.drawable.ic_action_image).fit().into(vh.img_ep_scrn);
				else
					vh.img_ep_scrn.setImageResource(R.drawable.ic_action_image);
				break;
			case ITM_MOV:
				break;
		}
		return view;
	}
	
	static class DashboardItem {
		int kind;
		String key;
		Object title;
		
		public DashboardItem(int k) {
			kind = k;
		}
		
		public DashboardItem(int k, Movie o) {
			kind = k;
			key = o.imdb_id;
			title = o;
		}
		
		public DashboardItem(int k, Series o) {
			kind = k;
			key = o.tvdb_id;
			title = o;
		}
		
		public DashboardItem(int k, Episode o) {
			kind = k;
			key = o.extendedEID();
			title = o;
		}
	}
	
	static class ViewHolder {
		// headers
		public TextView txt_header;
		// movies
		// series
		// episodes
		public TextView txt_ep_head;
		public TextView txt_ep_name;
		public TextView txt_ep_plot;
		public TextView txt_ep_flgs;
		public ImageView img_ep_coll;
		public ImageView img_ep_seen;
		public ImageView img_ep_subs;
		public ImageView img_ep_scrn;
	}
}