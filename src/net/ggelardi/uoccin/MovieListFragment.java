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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
	private boolean sortDesc = false;
	private int sortVal = 0;
	
	private AbsListView mListView;
	private MenuItem mSortDir;
	private MenuItem mSortName;
	private MenuItem mSortYear;
	private MenuItem mSortRats;
	
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
		
		setHasOptionsMenu(true);
		
		Bundle args = getArguments();
		type = args.getString("type");
		data = args.getString("data");
		sortVal = args.getInt("sortVal", 0);
		sortDesc = args.getBoolean("sortDesc", false);
		
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

		outState.putInt("sortVal", sortVal);
		outState.putBoolean("sortDesc", sortDesc);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (savedInstanceState != null) {
			sortVal = savedInstanceState.getInt("sortVal", sortVal);
			sortDesc = savedInstanceState.getBoolean("sortDesc", sortDesc);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.lists, menu);

		mSortDir = menu.findItem(R.id.action_sort_toggle);
		mSortName = menu.findItem(R.id.action_sort_name);
		mSortYear = menu.findItem(R.id.action_sort_year);
		mSortRats = menu.findItem(R.id.action_sort_rats);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		checkMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == mSortDir) {
			sortDesc = !sortDesc;
			reload();
			return true;
		}
		if (item == mSortName) {
			if (sortVal != 0) {
				sortVal = 0;
				reload();
			}
			return true;
		}
		if (item == mSortYear) {
			if (sortVal != 1) {
				sortVal = 1;
				reload();
			}
			return true;
		}
		if (item == mSortRats) {
			if (sortVal != 2) {
				sortVal = 2;
				reload();
			}
			return true;
		}
		return false;
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
						checkMenu();
					} else if (state.equals(OnTitleListener.WORKING)) {
						showHourGlass(true);
						checkMenu();
					} else if (state.equals(OnTitleListener.RELOAD)) {
						forceReload = true;
						reload();
					} else if (state.equals(OnTitleListener.ERROR)) {
						showHourGlass(false);
						mAdapter.notifyDataSetChanged();
						checkMenu();
						Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
					} else if (state.equals(OnTitleListener.READY)) {
						showHourGlass(false);
						mAdapter.notifyDataSetChanged();
						checkMenu();
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
		checkMenu();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = mListView instanceof GridView ? position - 2 : position - 1;
		mListener.openMovieInfo(mAdapter.getItem(pos).imdb_id);
	}
	
	private void checkMenu() {
		if (mAdapter == null || mSortDir == null || mSortName == null || mSortYear == null || mSortRats == null)
			return;
		mSortDir.setVisible(mAdapter.getCount() > 0);
		mSortName.setVisible(mAdapter.getCount() > 0);
		mSortYear.setVisible(mAdapter.getCount() > 0);
		mSortDir.setIcon(sortDesc ? R.drawable.ic_action_sort_desc : R.drawable.ic_action_sort_asc);
		if (sortVal == 0)
			mSortName.setChecked(true);
		else if (sortVal == 1)
			mSortYear.setChecked(true);
		else
			mSortRats.setChecked(true);
	}
	
	private int queryIdx() {
		return Arrays.asList(getResources().getStringArray(R.array.view_defmov_ids)).indexOf(data);
	}
	
	private void reload() {
		Log.v(getTag(), "reload()");
		mTask = new MovieTask(this, type);
		if (type.equals(TitleList.SEARCH))
			mTask.execute(data);
		else {
			String query = QUERIES[queryIdx()];
			if (sortVal == 0)
				query += " order by name collate nocase" + (sortDesc ? " desc" : " asc");
			else if (sortVal == 1)
				query += " order by year" + (sortDesc ? " desc" : " asc") + ", name collate nocase";
			else
				query += " order by rating" + (sortDesc ? " desc" : " asc") + ", name collate nocase";
			mTask.execute(query);
		}
	}
}