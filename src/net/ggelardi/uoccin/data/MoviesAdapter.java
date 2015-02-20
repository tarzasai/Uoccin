package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Session;
import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MoviesAdapter extends BaseAdapter {
	
	private final Context context;
	private final Session session;
	private final LayoutInflater inflater;
	private List<Movie> items;
	private SearchTask finder;
	
	public MoviesAdapter(Context context) {
		super();
		
		this.context = context;
		session = Session.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
			vh.img_mov_poster = (ImageView) view.findViewById(R.id.img_mov_poster);
			vh.txt_mov_name = (TextView) view.findViewById(R.id.txt_mov_name);
			vh.txt_mov_plot = (TextView) view.findViewById(R.id.txt_mov_plot);
			vh.txt_mov_actors = (TextView) view.findViewById(R.id.txt_mov_actors);
			vh.txt_mov_genres = (TextView) view.findViewById(R.id.txt_mov_genres);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		Movie mv = getItem(position);
		session.picasso().load(mv.poster).placeholder(R.drawable.ic_action_image).fit().into(vh.img_mov_poster);
		vh.txt_mov_name.setText(mv.name);
		vh.txt_mov_plot.setText(mv.plot);
		vh.txt_mov_actors.setText(TextUtils.join(", ", mv.actors));
		vh.txt_mov_genres.setText(TextUtils.join(", ", mv.genres));
		return view;
	}
	
	public void search(String text) {
		finder = (SearchTask) new SearchTask().execute(text);
	}
	
	static class ViewHolder {
		public ImageView img_mov_poster;
		public TextView txt_mov_name;
		public TextView txt_mov_plot;
		public TextView txt_mov_actors;
		public TextView txt_mov_genres;
	}
	
	private class SearchTask extends AsyncTask<String, Void, List<Movie>> {
        @Override
        protected void onPreExecute() {
        	// ???
		}
		@Override
		protected List<Movie> doInBackground(String... params) {
			return Movie.find(context, params[0]);
		}
        @Override
        protected void onPostExecute(List<Movie> result) {
        	items = result;
        	notifyDataSetChanged();
        }
	}
}