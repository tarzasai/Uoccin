package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.SeriesAdapter;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.SeriesTask;
import net.ggelardi.uoccin.serv.SeriesTask.SeriesTaskContainer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.photos.views.HeaderGridView;

public class SeriesFragment extends BaseFragment implements SeriesTaskContainer, AbsListView.OnItemClickListener {
	public static final String QUERY = "SeriesFragment.QUERY";
	public static final String SEARCH = "SeriesFragment.SEARCH";
	
	protected AbsListView mListView;
	protected SeriesAdapter mAdapter;
	protected SeriesTask mTask;
	
	private String type;
	private String title;
	private String query;
	private String[] params;
	private String search;
	
	public static SeriesFragment newQuery(String title, String query, String ... params) {
		SeriesFragment fragment = new SeriesFragment();
		Bundle args = new Bundle();
		args.putString("type", QUERY);
		args.putString("title", title);
		args.putString("query", query);
		args.putStringArray("params", params);
		fragment.setArguments(args);
		return fragment;
	}
	
	public static SeriesFragment newSearch(String search) {
		SeriesFragment fragment = new SeriesFragment();
		Bundle args = new Bundle();
		args.putString("type", SEARCH);
		args.putString("search", search);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		
		type = args.getString("type");
		
		mAdapter = new SeriesAdapter(getActivity(), this);
		
		if (isQuery()) {
			title = args.getString("title");
			query = args.getString("query");
			params = args.getStringArray("params");
		} else {
			search = args.getString("search");
			title = String.format(getString(R.string.title_search), search);
		}
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
		
		Series.addOnTitleEventListener(new OnTitleListener() {
			@Override
			public void changed(final String state, final Throwable error) {
				Log.v(logTag(), state);
				final Activity context = getActivity();
				if (context != null)
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (state.equals(OnTitleListener.NOTFOUND)) {
								//showHourGlass(false);
								mAdapter.notifyDataSetChanged();
								Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_LONG).show();
							} else if (state.equals(OnTitleListener.WORKING)) {
								//showHourGlass(true);
							} else if (state.equals(OnTitleListener.ERROR)) {
								//showHourGlass(false);
								mAdapter.notifyDataSetChanged();
								Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
							} else if (state.equals(OnTitleListener.READY)) {
								//showHourGlass(false);
								mAdapter.notifyDataSetChanged();
							}
						}
					});
			}
		});
		
		if (isQuery()) {
			mTask = new SeriesTask(this, query);
			mTask.execute(params);
		} else {
			mTask = new SeriesTask(this);
			mTask.execute(search);
		}
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
		int pos;
		try {
			pos = (Integer) v.getTag();
		} catch (Exception err) {
			return; // wtf?
		}
		if (v.getId() == R.id.img_ser_star) {
			Series ser = mAdapter.getItem(pos);
			ser.setWatchlist(!ser.inWatchlist());
			return;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//
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
	public void postExecuteTask(List<Series> result) {
		mAdapter.setTitles(result);
		showHourGlass(false);
		mTask = null;
	}
	
	public boolean isQuery() {
		return type.equals(QUERY);
	}
	
	public boolean isSearch() {
		return type.equals(SEARCH);
	}
}