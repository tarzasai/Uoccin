package net.ggelardi.uoccin.data;

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
		
		items.add(new DrawerItem(R.string.drawer_dashboard, R.drawable.ic_action_dashboard));
		items.add(new DrawerItem(R.string.drawer_calendar, R.drawable.ic_action_calendar));
		items.add(new DrawerItem(R.string.drawer_favseries, R.drawable.ic_action_tv));
		items.add(new DrawerItem(R.string.drawer_favmovies, R.drawable.ic_action_movie));
		items.add(new DrawerItem(R.string.drawer_settings, R.drawable.ic_action_settings));
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
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_drawer, parent, false);
			vh = new ViewHolder();
			vh.txt = (TextView) view.findViewById(R.id.txt_itemdrawer);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		DrawerItem itm = getItem(position);
		vh.txt.setText(itm.label);
		vh.txt.setCompoundDrawablesWithIntrinsicBounds(itm.icon, 0, 0, 0);
		return view;
	}
	
	static class DrawerItem {
		int label;
		int icon;
		public DrawerItem(int lbl, int ico) {
			label = lbl;
			icon = ico;
		}
	}
	
	static class ViewHolder {
		public TextView txt;
	}
}