package net.ggelardi.uoccin.data;

import java.util.ArrayList;
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

public class SeriesAdapter extends BaseAdapter {
	
	private final Session session;
	private final LayoutInflater inflater;
	private List<Series> items;
	
	public SeriesAdapter(Context context) {
		super();
		
		session = Session.getInstance(context);
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
			vh.txt_ser_name = (TextView) view.findViewById(R.id.txt_ser_name);
			vh.txt_ser_info = (TextView) view.findViewById(R.id.txt_ser_info);
			vh.txt_ser_plot = (TextView) view.findViewById(R.id.txt_ser_plot);
			vh.txt_ser_actors = (TextView) view.findViewById(R.id.txt_ser_actors);
			vh.txt_ser_genres = (TextView) view.findViewById(R.id.txt_ser_genres);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		Series ser = getItem(position);
		session.picasso().load(ser.poster).placeholder(R.drawable.ic_action_image).fit().into(vh.img_ser_poster);
		vh.txt_ser_name.setText(ser.name);
		vh.txt_ser_info.setText(ser.infoLine());
		vh.txt_ser_plot.setText(ser.plot);
		if (ser.actors.isEmpty())
			vh.txt_ser_actors.setVisibility(View.GONE);
		else {
			vh.txt_ser_actors.setVisibility(View.VISIBLE);
			vh.txt_ser_actors.setText(TextUtils.join(", ", ser.actors));
		}
		if (ser.genres.isEmpty())
			vh.txt_ser_genres.setVisibility(View.GONE);
		else {
			vh.txt_ser_genres.setVisibility(View.VISIBLE);
			vh.txt_ser_genres.setText(TextUtils.join(", ", ser.genres).toLowerCase(Locale.getDefault()));
		}
		return view;
	}
	
	public void setTitles(List<Series> titles) {
		items = titles;
    	notifyDataSetChanged();
	}
	
	static class ViewHolder {
		public ImageView img_ser_poster;
		public TextView txt_ser_name;
		public TextView txt_ser_info;
		public TextView txt_ser_plot;
		public TextView txt_ser_actors;
		public TextView txt_ser_genres;
	}
}