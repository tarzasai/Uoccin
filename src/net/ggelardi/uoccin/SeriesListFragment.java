package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.adapters.SeriesAdapter;
import net.ggelardi.uoccin.data.Episode.EID;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.SeriesTask;
import net.ggelardi.uoccin.serv.SeriesTask.SeriesTaskContainer;
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

public class SeriesListFragment extends BaseFragment implements SeriesTaskContainer, AbsListView.OnItemClickListener {
	
	protected AbsListView mListView;
	protected SeriesAdapter mAdapter;
	protected SeriesTask mTask;
	
	private String type;
	private String title;
	private String query;
	private String[] params;
	private String details = SeriesAdapter.SERIES_STORY;
	private String search;
	
	public static SeriesListFragment newQuery(String title, String query, String details, String ... params) {
		SeriesListFragment fragment = new SeriesListFragment();
		Bundle args = new Bundle();
		args.putString("type", SeriesTask.QUERY);
		args.putString("title", title);
		args.putString("query", query);
		args.putStringArray("params", params);
		args.putString("details", details);
		fragment.setArguments(args);
		return fragment;
	}
	
	public static SeriesListFragment newSearch(String search) {
		SeriesListFragment fragment = new SeriesListFragment();
		Bundle args = new Bundle();
		args.putString("type", SeriesTask.SEARCH);
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
			details = args.getString("details");
		} else {
			search = args.getString("search");
			title = String.format(getString(R.string.title_search), search);
		}
		
		mAdapter = new SeriesAdapter(getActivity(), this, details);
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
				final Activity context = getActivity();
				if (context != null)
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (state.equals(OnTitleListener.NOTFOUND)) {
								showHourGlass(false);
								Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_SHORT).show();
								if (type.equals(SeriesTask.SEARCH))
									((ActionBarActivity) context).getSupportFragmentManager().popBackStack();
								else
									mAdapter.notifyDataSetChanged();
							} else if (state.equals(OnTitleListener.WORKING)) {
								showHourGlass(true);
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
		
		mTask = new SeriesTask(this, type);
		if (type.equals(SeriesTask.SEARCH))
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
		if (v.getId() == R.id.img_is_star) {
			int pos;
			try {
				pos = (Integer) v.getTag();
				Series ser = mAdapter.getItem(pos);
				ser.setWatchlist(!ser.inWatchlist());
			} catch (Exception err) {
				Log.e(tag(), "onClick", err);
			}
			return;
		}
		if (v.getId() == R.id.box_is_avail) {
			Object tmp = v.getTag();
			if (tmp instanceof EID) {
				EID eid = (EID) tmp;
				mListener.openSeriesEpisode(eid.series, eid.season, eid.episode);
			} else if (tmp instanceof String) {
				mListener.openSeriesInfo((String) tmp);
			}
			return;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = mListView instanceof GridView ? position - 2 : position - 1;
		mListener.openSeriesInfo(mAdapter.getItem(pos).tvdb_id);
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
}