package net.ggelardi.uoccin.adapters;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DrawerAdapter extends BaseAdapter {

	private final LayoutInflater inflater;
	private final List<DrawerItem> items;
	
	public DrawerAdapter(Context context) {
		super();

		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		items = new ArrayList<DrawerAdapter.DrawerItem>();
		
		// series header
		DrawerItem di = new DrawerItem();
		di.header = true;
		di.type = DrawerItem.SERIES;
		di.id = "header";
		di.label = context.getResources().getString(R.string.drwhdr_series);
		di.icon = R.drawable.ic_action_tv;
		items.add(di);
		// series items
		String[] defids = context.getResources().getStringArray(R.array.view_defser_ids);
		String[] deflbs = context.getResources().getStringArray(R.array.view_defser_titles);
		String[] defqrs = context.getResources().getStringArray(R.array.view_defser_queries);
		String[] defdet = context.getResources().getStringArray(R.array.view_defser_details);
		for (int i = 0; i < defids.length; i++) {
			di = new DrawerItem();
			di.type = DrawerItem.SERIES;
			di.id = defids[i];
			di.label = deflbs[i];
			di.query = defqrs[i];
			di.details = defdet[i];
			di.position = items.size();
			items.add(di);
		}
		// movies header
		di = new DrawerItem();
		di.header = true;
		di.type = DrawerItem.MOVIE;
		di.id = "header";
		di.label = context.getResources().getString(R.string.drwhdr_movies);
		di.icon = R.drawable.ic_action_movie;
		items.add(di);
		// movies items
		defids = context.getResources().getStringArray(R.array.view_defmov_ids);
		deflbs = context.getResources().getStringArray(R.array.view_defmov_titles);
		defqrs = context.getResources().getStringArray(R.array.view_defmov_queries);
		defdet = context.getResources().getStringArray(R.array.view_defmov_details);
		for (int i = 0; i < defids.length; i++) {
			di = new DrawerItem();
			di.type = DrawerItem.MOVIE;
			di.id = defids[i];
			di.label = deflbs[i];
			di.query = defqrs[i];
			di.details = defdet[i];
			di.position = items.size();
			items.add(di);
		}
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return !getItem(position).header;
	}
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public DrawerItem getItem(int position) {
		return items.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getItemViewType(int position) {
		return getItem(position).header ? 0 : 1;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			vh = new ViewHolder();
			if (getItemViewType(position) == 0)
				view = inflater.inflate(R.layout.header_drawer, parent, false);
			else
				view = inflater.inflate(R.layout.item_drawer, parent, false);
			vh.txt = (TextView) view.getRootView();
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		DrawerItem itm = getItem(position);
		vh.txt.setText(itm.label);
		if (itm.header)
			vh.txt.setCompoundDrawablesWithIntrinsicBounds(itm.icon, 0, 0, 0);
		return view;
	}
	
	public DrawerItem findItem(String id) {
		for (int i = 0; i < items.size(); i++)
			if (getItem(i).id.equals(id))
				return getItem(i);
		return null;
	}
	
	static class ViewHolder {
		public TextView txt;
	}
	
	public static class DrawerItem {
		public static final String SERIES = "DrawerItem.SERIES";
		public static final String MOVIE = "DrawerItem.MOVIE";
		
		public boolean header = false;
		public int position = -1;
		public int icon = 0;
		public String id;
		public String type;
		public String label;
		public String query;
		public String details;
	}
}