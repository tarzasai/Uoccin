package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.adapters.MovieAdapter;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.MovieTask;
import net.ggelardi.uoccin.serv.MovieTask.MovieTaskContainer;
import net.ggelardi.uoccin.serv.SeriesTask;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.photos.views.HeaderGridView;

public class MovieListFragment extends BaseFragment implements MovieTaskContainer, AbsListView.OnItemClickListener {
	
	private String type;
	private String title;
	private String query;
	private String[] params;
	private String search;
	
	private AbsListView mListView;
	private MovieAdapter mAdapter;
	private MovieTask mTask;
	
	public static MovieListFragment newQuery(String title, String query, String ... params) {
		MovieListFragment fragment = new MovieListFragment();
		Bundle args = new Bundle();
		args.putString("type", MovieTask.QUERY);
		args.putString("title", title);
		args.putString("query", query);
		args.putStringArray("params", params);
		fragment.setArguments(args);
		return fragment;
	}
	
	public static MovieListFragment newSearch(String search) {
		MovieListFragment fragment = new MovieListFragment();
		Bundle args = new Bundle();
		args.putString("type", MovieTask.SEARCH);
		args.putString("search", search);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getString("type");
		if (type.equals(SeriesTask.QUERY)) {
			title = args.getString("title");
			query = args.getString("query");
			params = args.getStringArray("params");
		} else {
			search = args.getString("search");
			title = String.format(getString(R.string.title_search), search);
		}
		
		mAdapter = new MovieAdapter(getActivity(), this);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_items, container, false);

		mListView = (AbsListView) view.findViewById(android.R.id.list);
		
		if (mListView instanceof ListView) {
			((ListView) mListView).addHeaderView(inflater.inflate(R.layout.header_space, null));
			((ListView) mListView).addFooterView(inflater.inflate(R.layout.header_space, null));
		} else {
			((HeaderGridView) mListView).addHeaderView(inflater.inflate(R.layout.header_space, null));
			//((HeaderGridView) mListView).addFooterView(inflater.inflate(R.layout.header_space, null));
		}
		
		mListView.setOnItemClickListener(this);
		
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		getActivity().setTitle(title);
		
		Movie.addOnTitleEventListener(new OnTitleListener() {
			@Override
			public void changed(final String state, final Throwable error) {
				final Activity context = getActivity();
				if (context != null)
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (state.equals(OnTitleListener.NOTFOUND)) {
								showHourGlass(false);
								Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_SHORT).show();
								if (type.equals(MovieTask.SEARCH))
									((ActionBarActivity) context).getSupportFragmentManager().popBackStack();
								else
									mAdapter.notifyDataSetChanged();
							} else if (state.equals(OnTitleListener.WORKING)) {
								showHourGlass(true);
							} else if (state.equals(OnTitleListener.RELOAD)) {
								reload();
							} else if (state.equals(OnTitleListener.ERROR)) {
								showHourGlass(false);
								mAdapter.notifyDataSetChanged();
								Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
							} else if (state.equals(OnTitleListener.READY)) {
								showHourGlass(false);
								mAdapter.notifyDataSetChanged();
							}
						}
					});
			}
		});
		
		reload();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		if (mTask != null)
			mTask.cancel(true);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	}
	
	@Override
	public void onClick(View v) {
		/*
		if (v.getId() == R.id.img_im_star) {
			int pos;
			try {
				pos = (Integer) v.getTag();
				Movie ser = mAdapter.getItem(pos);
				ser.setWatchlist(!ser.inWatchlist());
			} catch (Exception err) {
				Log.e(tag(), "onClick", err);
			}
			return;
		}
		if (v.getId() == R.id.box_im_avail) {
			Object tmp = v.getTag();
			if (tmp instanceof EID) {
				EID eid = (EID) tmp;
				mListener.openSeriesEpisode(eid.series, eid.season, eid.episode);
			} else if (tmp instanceof String) {
				mListener.openSeriesInfo((String) tmp);
			}
			return;
		}
		*/
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = mListView instanceof GridView ? position - 2 : position - 1;
		mListener.openSeriesInfo(mAdapter.getItem(pos).imdb_id);
	}

	@Override
	public Context getContext() {
		return getActivity();
	}

	@Override
	public void preExecuteTask() {
		showHourGlass(true);
	}

	@Override
	public void postExecuteTask(List<Movie> result) {
		mAdapter.setTitles(result);
		showHourGlass(false);
		mTask = null;
	}
	
	private void reload() {
		Log.v(getTag(), "reload()");
		mTask = new MovieTask(this, type);
		if (type.equals(MovieTask.SEARCH))
			mTask.execute(search);
		else if (params == null || params.length <= 0)
			mTask.execute(new String[] { query });
		else {
			String[] args = new String[params.length + 1];
			args[0] = query;
			System.arraycopy(params, 0, args, 1, params.length);
			mTask.execute(args);
		}
	}
}