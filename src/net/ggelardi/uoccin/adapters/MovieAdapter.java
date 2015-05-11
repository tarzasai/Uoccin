package net.ggelardi.uoccin.adapters;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.serv.Commons.TitleList;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

public class MovieAdapter extends BaseAdapter {
	
	private final Session session;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	private final String type;
	private final String data;
	private List<Movie> items;
	private int pstHeight = 1;
	private int pstWidth = 1;
	
	public MovieAdapter(Context context, OnClickListener clickListener, String type, String data) {
		super();
		
		session = Session.getInstance(context);
		listener = clickListener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.type = type;
		this.data = data;
		items = new ArrayList<Movie>();
	}
	
	@Override
	public int getCount() {
		return items.size();
	}
	
	@Override
	public Movie getItem(int position) {
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
			view = inflater.inflate(R.layout.item_movie, parent, false);
			vh = new ViewHolder();
			vh.img_post = (ImageView) view.findViewById(R.id.img_im_poster);
			vh.box_size = (LinearLayout) view.findViewById(R.id.box_im_size);
			vh.img_star = (ImageView) view.findViewById(R.id.img_im_star);
			vh.txt_name = (TextView) view.findViewById(R.id.txt_im_name);
			vh.txt_spac = (TextView) view.findViewById(R.id.txt_im_spac);
			vh.txt_acts = (TextView) view.findViewById(R.id.txt_im_acts);
			vh.txt_subs = (TextView) view.findViewById(R.id.txt_im_subs);
			vh.rat_myrt = (RatingBar) view.findViewById(R.id.rat_im_myrt);
			vh.txt_meta = (TextView) view.findViewById(R.id.txt_im_meta);
			vh.txt_rott = (TextView) view.findViewById(R.id.txt_im_rott);
			vh.txt_imdb = (TextView) view.findViewById(R.id.txt_im_imdb);
			//
			vh.img_star.setOnClickListener(listener);
			//
			if (pstHeight <= 1) {
				view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				pstHeight = view.getMeasuredHeight();
				pstWidth = Math.round((pstHeight*320)/500);
				Log.v("Title", String.format("pstHeight=%d, pstWidth=%d", pstHeight, pstWidth));
			}
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		//
		vh.img_star.setTag(Integer.valueOf(position));
		//
		Movie mov = getItem(position);
		session.picasso(mov.poster, true).resize(pstWidth, pstHeight).into(vh.img_post);
		if (pstWidth > 1) {
			vh.img_post.setMinimumWidth(pstWidth);
			vh.img_post.setMaxWidth(pstWidth);
			vh.img_post.setMinimumHeight(pstHeight);
			vh.img_post.setMaxHeight(pstHeight);
		}
		vh.txt_name.setText(String.format("%s (%d)", mov.name, mov.year));
		vh.img_star.setImageResource(mov.inWatchlist() ? R.drawable.ic_active_loved : R.drawable.ic_action_loved);
		if (type.equals(TitleList.SEARCH)) {
			vh.img_star.setVisibility(View.VISIBLE);
			vh.txt_spac.setVisibility(View.VISIBLE);
			vh.txt_subs.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.GONE);
		} else if (data.equals("movfavs")) {
			vh.img_star.setVisibility(View.VISIBLE);
			vh.txt_spac.setVisibility(View.VISIBLE);
			vh.txt_subs.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.GONE);
		} else if (type.equals("mov2see")) {
			vh.img_star.setVisibility(View.GONE);
			vh.txt_spac.setVisibility(View.GONE);
			vh.txt_subs.setVisibility(View.VISIBLE);
			vh.rat_myrt.setVisibility(View.GONE);
		} else if (type.equals("movrats")) {
			vh.img_star.setVisibility(View.GONE);
			vh.txt_spac.setVisibility(View.GONE);
			vh.txt_subs.setVisibility(View.GONE);
			vh.rat_myrt.setVisibility(View.VISIBLE);
		}
		vh.txt_acts.setText(mov.actors());
		
		if (vh.txt_subs.getVisibility() == View.VISIBLE) {
			if (!mov.hasSubtitles())
				vh.txt_subs.setVisibility(View.GONE);
			else
				vh.txt_subs.setText(mov.subtitles());
		}
		if (vh.rat_myrt.getVisibility() == View.VISIBLE) {
			if (mov.getRating() <= 0)
				vh.rat_myrt.setVisibility(View.GONE);
			else
				vh.rat_myrt.setRating(mov.getRating());
		}
		if (mov.metascore <= 0)
			vh.txt_meta.setVisibility(View.GONE);
		else {
			vh.txt_meta.setVisibility(View.VISIBLE);
			vh.txt_meta.setText(Integer.toString(mov.metascore));
		}
		if (mov.tomatoMeter <= 0)
			vh.txt_rott.setVisibility(View.GONE);
		else {
			vh.txt_rott.setVisibility(View.VISIBLE);
			vh.txt_rott.setText(Integer.toString(mov.tomatoMeter) + "%");
		}
		if (mov.imdbRating <= 0)
			vh.txt_imdb.setVisibility(View.GONE);
		else {
			vh.txt_imdb.setVisibility(View.VISIBLE);
			vh.txt_imdb.setText(String.format("%.1f", mov.imdbRating));
		}
		return view;
	}
	
	public void setTitles(List<Movie> titles, boolean forceReload) {
		items = titles;
		if (forceReload)
			for (Movie mov: items)
				mov.reload();
    	notifyDataSetChanged();
	}
	
	static class ViewHolder {
		ImageView img_post;
		LinearLayout box_size;
		ImageView img_star;
		TextView txt_name;
		TextView txt_spac;
		TextView txt_acts;
		TextView txt_subs;
		RatingBar rat_myrt;
		TextView txt_meta;
		TextView txt_rott;
		TextView txt_imdb;
	}
}