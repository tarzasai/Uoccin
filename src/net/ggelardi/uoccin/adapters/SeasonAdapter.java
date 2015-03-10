package net.ggelardi.uoccin.adapters;

import java.util.Locale;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Series;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SeasonAdapter extends BaseAdapter {
	
	private final LayoutInflater inflater;
	private final Series series;
	
	public SeasonAdapter(Context context, Series series) {
		super();

		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.series = series;
	}
	
	@Override
	public int getCount() {
		return series.seasons().size();
	}
	
	@Override
	public Integer getItem(int position) {
		return series.seasons().get(position);
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
			view = inflater.inflate(R.layout.item_season, parent, false);
			vh = new ViewHolder();
			vh.txt = (TextView) view.getRootView();
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.txt.setText(String.format(Locale.getDefault(), "S%1$02d", getItem(position)));
		return view;
	}
	
	static class ViewHolder {
		public TextView txt;
	}
}