package net.ggelardi.uoccin;

import java.util.Arrays;
import java.util.List;

import net.ggelardi.uoccin.adapters.MovieAdapter;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.Commons.TitleList;
import net.ggelardi.uoccin.serv.MovieTask;
import net.ggelardi.uoccin.serv.MovieTask.MovieTaskContainer;
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

public class MovieListFragment extends BaseFragment implements AbsListView.OnItemClickListener, OnTitleListener,
	MovieTaskContainer {
	
	private static final String[] QUERIES = new String[] {
		"select imdb_id from movie where watchlist = 1",
		"select imdb_id from movie where collected = 1 and watched = 0",
		"select imdb_id from movie where watched = 1"
	};
	
	private String type;
	private String data;
	
	private boolean forceReload = false;
	
	private AbsListView mListView;
	private MovieAdapter mAdapter;
	private MovieTask mTask;
	
	public static MovieListFragment newFragment(String type, String data) {
		MovieListFragment fragment = new MovieListFragment();
		Bundle args = new Bundle();
		args.putString("type", type);
		args.putString("data", data);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getString("type");
		data = args.getString("data");
		
		mAdapter = new MovieAdapter(getActivity(), this, type, data);
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
		
		getActivity().setTitle(type.equals(TitleList.SEARCH) ?
			String.format(getString(R.string.title_search), data) :
			session.getRes().getStringArray(R.array.view_defmov_titles)[queryIdx()]);
		
		Title.addOnTitleEventListener(this);
		
		reload();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Title.removeOnTitleEventListener(this);
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
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		mListener.setIcon(R.drawable.ic_action_movie);
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.img_im_star) {
			int pos;
			try {
				pos = (Integer) v.getTag();
				Movie mov = mAdapter.getItem(pos);
				mov.setWatchlist(!mov.inWatchlist());
			} catch (Exception err) {
				Log.e(tag(), "onClick", err);
			}
			return;
		}
	}
	
	@Override
	public void onTitleEvent(final String state, final Throwable error) {
		final Activity context = getActivity();
		if (context != null)
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (state.equals(OnTitleListener.NOTFOUND)) {
						showHourGlass(false);
						Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_SHORT).show();
						if (type.equals(TitleList.SEARCH))
							((ActionBarActivity) context).getSupportFragmentManager().popBackStack();
						else
							mAdapter.notifyDataSetChanged();
					} else if (state.equals(OnTitleListener.WORKING)) {
						showHourGlass(true);
					} else if (state.equals(OnTitleListener.RELOAD)) {
						forceReload = true;
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
		showHourGlass(false);
		mTask = null;
		mAdapter.setTitles(result, forceReload);
		forceReload = false;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = mListView instanceof GridView ? position - 2 : position - 1;
		mListener.openMovieInfo(mAdapter.getItem(pos).imdb_id);
	}
	
	private int queryIdx() {
		return Arrays.asList(getResources().getStringArray(R.array.view_defmov_ids)).indexOf(data);
	}
	
	private void reload() {
		Log.v(getTag(), "reload()");
		mTask = new MovieTask(this, type);
		mTask.execute(type.equals(TitleList.SEARCH) ? data : QUERIES[queryIdx()]);
	}
}