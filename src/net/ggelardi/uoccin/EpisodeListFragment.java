package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.adapters.EpisodeAdapter;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.EpisodeTask;
import net.ggelardi.uoccin.serv.EpisodeTask.EpisodeTaskContainer;
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
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.photos.views.HeaderGridView;

public class EpisodeListFragment extends BaseFragment implements EpisodeTaskContainer, AbsListView.OnItemClickListener {
	
	protected AbsListView mListView;
	protected EpisodeAdapter mAdapter;
	protected EpisodeTask mTask;
	
	private String type;
	private String title;
	private String query;
	private String[] params;
	private String series;
	private int season;
	
	public static EpisodeListFragment newQuery(String title, String query, String ... params) {
		EpisodeListFragment fragment = new EpisodeListFragment();
		Bundle args = new Bundle();
		args.putString("type", EpisodeTask.QUERY);
		args.putString("title", title);
		args.putString("query", query);
		args.putStringArray("params", params);
		fragment.setArguments(args);
		return fragment;
	}
	
	public static EpisodeListFragment newList(String title, String series, int season) {
		EpisodeListFragment fragment = new EpisodeListFragment();
		Bundle args = new Bundle();
		args.putString("type", EpisodeTask.LIST);
		args.putString("title", title);
		args.putString("series", series);
		args.putInt("season", season);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		type = args.getString("type");
		title = args.getString("title");
		if (type.equals(EpisodeTask.QUERY)) {
			query = args.getString("query");
			params = args.getStringArray("params");
		} else {
			series = args.getString("series");
			season = args.getInt("season");
		}
		
		mAdapter = new EpisodeAdapter(getActivity(), this);
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
		
		mTask = new EpisodeTask(this, type);
		if (type.equals(EpisodeTask.LIST))
			mTask.execute(new String[] { series, Integer.toString(season) });
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
		int pos;
		try {
			pos = (Integer) v.getTag();
		} catch (Exception err) {
			return; // wtf?
		}
		if (v.getId() == R.id.img_epitm_coll) {
			Episode ep = mAdapter.getItem(pos);
			ep.setCollected(!ep.inCollection());
			return;
		}
		if (v.getId() == R.id.img_epitm_seen) {
			Episode ep = mAdapter.getItem(pos);
			ep.setWatched(!ep.isWatched());
			return;
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = mListView instanceof GridView ? position - 2 : position - 1;
		Episode ep = mAdapter.getItem(pos);
		mListener.openSeriesEpisode(ep.series, ep.season, ep.episode);
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
	public void postExecuteTask(List<Episode> result) {
		mAdapter.setTitles(result);
		showHourGlass(false);
		mTask = null;
	}
}