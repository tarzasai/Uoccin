package net.ggelardi.uoccin.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.R;
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
		
		String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
		List<String> ids;
		// calendar
		String query = "select e.imdb_id from episode e join series s on (e.series_imdb_id = s.imdb_id) " +
			"join title t on (s.imdb_id = t.imdb_id) where date(firstAired) = ? and s.watchlist = 1 " +
			"order by t.name, e.season, e.episode";
		ids = check(Title.getIDs(context, query, today));
		if (!ids.isEmpty()) {
			items.add(new DashboardItem(HDR_CAL, null));
			for (String tid: ids)
				items.add(new DashboardItem(ITM_SEP, Episode.get(context, tid)));
		}
		// premieres
		query = "select s.imdb_id from episode e join series s on (e.series_imdb_id = s.imdb_id) " +
			"join title t on (s.imdb_id = t.imdb_id) where date(firstAired) = ? and s.watchlist = 0 and " +
			"e.season = 1 and e.episode = 1 order by t.name";
		ids = check(Title.getIDs(context, query, today));
		if (!ids.isEmpty()) {
			items.add(new DashboardItem(HDR_PRE, null));
			for (String tid: ids)
				items.add(new DashboardItem(ITM_SER, Series.get(context, tid)));
		}
		// available episodes
		query = "select e.imdb_id from episode e join series s on (e.series_imdb_id = s.imdb_id) " +
			"join title t on (s.imdb_id = t.imdb_id) where e.collected = 1 and e.watched = 0 " +
			"order by t.name, e.season, e.episode";
		ids = check(Title.getIDs(context, query));
		if (!ids.isEmpty()) {
			items.add(new DashboardItem(HDR_CSE, null));
			for (String tid: ids)
				items.add(new DashboardItem(ITM_SEP, Episode.get(context, tid)));
		}
		// available movies
		query = "select m.imdb_id from movie m join title t on (m.imdb_id = t.imdb_id) " +
			"where m.collected = 1 and m.watched = 0 order by t.name";
		ids = check(Title.getIDs(context, query));
		if (!ids.isEmpty()) {
			items.add(new DashboardItem(HDR_CMV, null));
			for (String tid: ids)
				items.add(new DashboardItem(ITM_MOV, Movie.get(context, tid)));
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
					break;
				case HDR_PRE:
					break;
				case HDR_CSE:
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
				break;
			case HDR_PRE:
				break;
			case HDR_CSE:
				break;
			case HDR_CMV:
				break;
			case ITM_SER:
				break;
			case ITM_SEP:
				Episode ep = (Episode) itm.title;
				vh.txt_ep_head.setText(ep.series().name);
				vh.txt_ep_name.setText(ep.simpleEID() + " - " + session.defaultText(ep.name, R.string.unknown_title));
				vh.txt_ep_plot.setText(session.defaultText(ep.plot, R.string.unknown_plot));
				vh.img_ep_coll.setVisibility(ep.collected ? View.VISIBLE : View.GONE);
				vh.img_ep_seen.setVisibility(ep.watched ? View.VISIBLE : View.GONE);
				vh.img_ep_subs.setVisibility(/*ep.watched ? View.VISIBLE :*/ View.GONE);
				if (!TextUtils.isEmpty(ep.poster))
					session.picasso().load(ep.poster).placeholder(R.drawable.ic_action_image).fit().into(vh.img_ep_scrn);
				else if (!TextUtils.isEmpty(ep.series().fanart))
					session.picasso().load(ep.series().fanart).placeholder(R.drawable.ic_action_image).fit().into(vh.img_ep_scrn);
				else
					vh.img_ep_scrn.setImageResource(R.drawable.ic_action_image);
				break;
			case ITM_MOV:
				break;
		}
		return view;
	}
	
	private List<String> check(List<String> ids) {
		int pos = -1;
		for (DashboardItem itm: items) {
			pos = itm.title != null ? ids.indexOf(itm.title.imdb_id) : -1;
			if (pos >= 0)
				ids.remove(pos);
		}
		return ids;
	}
	
	static class DashboardItem {
		int kind;
		Title title;
		
		public DashboardItem(int k, Title t) {
			kind = k;
			title = t;
		}
	}
	
	static class ViewHolder {
		// movies
		// series
		// episodes
		public TextView txt_ep_head;
		public TextView txt_ep_name;
		public TextView txt_ep_plot;
		public ImageView img_ep_coll;
		public ImageView img_ep_seen;
		public ImageView img_ep_subs;
		public ImageView img_ep_scrn;
	}
}