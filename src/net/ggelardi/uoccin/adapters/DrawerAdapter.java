package net.ggelardi.uoccin.adapters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ggelardi.uoccin.BuildConfig;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class DrawerAdapter extends BaseExpandableListAdapter {
	
	private final Session session;
	private final LayoutInflater inflater;
	private final List<DrawerItem> heads;
	private final Map<DrawerItem, List<DrawerItem>> items;
	
	public DrawerAdapter(Context context) {
		session = Session.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		heads = new ArrayList<DrawerAdapter.DrawerItem>();
		items = new HashMap<DrawerAdapter.DrawerItem, List<DrawerItem>>();
		
		DrawerItem di;
		List<DrawerItem> children;
		String[] views;
		String[] titles;
		
		// series
		di = new DrawerItem("series", session.getString(R.string.drwhdr_series), R.drawable.ic_action_tv);
		heads.add(di);
		items.put(di, new ArrayList<DrawerAdapter.DrawerItem>());
		views = session.getStringArray(R.array.view_defser_ids);
		titles = session.getStringArray(R.array.view_defser_titles);
		children = items.get(di);
		for (int i = 0; i < views.length; i++)
			children.add(new DrawerItem(DrawerItem.SERIES, views[i], titles[i]));
		
		// movies
		di = new DrawerItem("movies", session.getString(R.string.drwhdr_movies), R.drawable.ic_action_movie);
		heads.add(di);
		items.put(di, new ArrayList<DrawerAdapter.DrawerItem>());
		views = session.getStringArray(R.array.view_defmov_ids);
		titles = session.getStringArray(R.array.view_defmov_titles);
		children = items.get(di);
		for (int i = 0; i < views.length; i++)
			children.add(new DrawerItem(DrawerItem.MOVIE, views[i], titles[i]));
		
		// services
		if (session.driveAccountSet()) {
			di = new DrawerItem("services", session.getString(R.string.drwhdr_services), R.drawable.ic_action_cloud);
			heads.add(di);
			items.put(di, new ArrayList<DrawerAdapter.DrawerItem>());
			children = items.get(di);
			children.add(new DrawerItem("action_backup", session.getString(R.string.action_backup)));
			children.add(new DrawerItem("action_restore", session.getString(R.string.action_restore)));
			if (session.driveSyncEnabled())
				children.add(new DrawerItem("action_syncnow", session.getString(R.string.action_syncnow)));
		}
		
		if (BuildConfig.DEBUG) {
			di = new DrawerItem("debug", session.getString(R.string.drwhdr_develop), R.drawable.ic_action_debug);
			heads.add(di);
			items.put(di, new ArrayList<DrawerAdapter.DrawerItem>());
			children = items.get(di);
			children.add(new DrawerItem("action_cleandb", session.getString(R.string.action_cleandb)));
			children.add(new DrawerItem("action_chktvdb", session.getString(R.string.action_chktvdb)));
		}
	}
	
	@Override
	public int getGroupCount() {
		return heads.size();
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return items.get(getGroup(groupPosition)).size();
	}
	
	@Override
	public DrawerItem getGroup(int groupPosition) {
		return heads.get(groupPosition);
	}
	
	@Override
	public DrawerItem getChild(int groupPosition, int childPosition) {
		return items.get(getGroup(groupPosition)).get(childPosition);
	}
	
	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}
	
	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}
	
	@Override
	public boolean hasStableIds() {
		return false;
	}
	
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			vh = new ViewHolder();
			view = inflater.inflate(R.layout.header_drawer, parent, false);
			vh.txt = (TextView) view.getRootView();
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		DrawerItem itm = getGroup(groupPosition);
		vh.txt.setText(itm.label);
		vh.txt.setCompoundDrawablesWithIntrinsicBounds(itm.icon, 0, 0, 0);
		return view;
	}
	
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
		ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			vh = new ViewHolder();
			view = inflater.inflate(R.layout.item_drawer, parent, false);
			vh.txt = (TextView) view.getRootView();
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		DrawerItem itm = getChild(groupPosition, childPosition);
		vh.txt.setText(itm.label);
		return view;
	}
	
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		//return groupPosition == 0 || groupPosition == 1;
		return true;
	}
	
	public DrawerItem findItem(String id) {
		for (DrawerItem dh: items.keySet())
			for (DrawerItem di: items.get(dh))
				if (di.id.equals(id))
					return di;
		return null;
	}
	
	public Pair<Integer, Integer> getChildPos(String id) {
		List<DrawerItem> group;
		for (int g = 0; g < heads.size(); g++) {
			group = items.get(heads.get(g));
			for (int c = 0; c < group.size(); c++)
				if (group.get(c).id.equals(id))
					return new Pair<Integer, Integer>(g, c);
		}
		return null;
	}
	
	static class ViewHolder {
		public TextView txt;
	}
	
	public static class DrawerItem {
		public static final String ACTION = "DrawerItem.ACTION";
		public static final String SERIES = "DrawerItem.SERIES";
		public static final String MOVIE = "DrawerItem.MOVIE";
		
		public boolean header = false;
		public int icon = 0;
		public String id;
		public String type;
		public String label;
		
		public DrawerItem() {
		}
		
		public DrawerItem(String id, String label) {
			this.type = ACTION;
			this.id = id;
			this.label = label;
		}
		
		public DrawerItem(String id, String label, int icon) {
			this.header = true;
			this.id = id;
			this.label = label;
			this.icon = icon;
		}
		
		public DrawerItem(String type, String id, String label) {
			this.type = type;
			this.id = id;
			this.label = label;
		}
	}
}