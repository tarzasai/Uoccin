package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.SeriesAdapter;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.AsyncTask;
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

public class SearchSeriesFragment extends BaseFragment implements AbsListView.OnItemClickListener {
	
	private AbsListView mListView;
	private SeriesAdapter mAdapter;
	private String mSearchText;
	private SearchTask finder;
	
	public static SearchSeriesFragment newInstance(String text) {
		SearchSeriesFragment fragment = new SearchSeriesFragment();
		Bundle args = new Bundle();
		args.putString("text", text);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchText = getArguments().getString("text");
		mAdapter = new SeriesAdapter(getActivity(), this);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_search, container, false);
		
		getActivity().setTitle(getString(R.string.drawer_fndseries));

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
		
		getActivity().setTitle(String.format(getString(R.string.title_search), mSearchText));
		
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
								showHourGlass(false);
								mAdapter.notifyDataSetChanged();
								Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_LONG).show();
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
		
		finder = (SearchTask) new SearchTask().execute(mSearchText);
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("text", mSearchText);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		//setHasOptionsMenu(true);
		
		if (savedInstanceState == null)
			return;

		mSearchText = savedInstanceState.getString("text", null);
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
	
	private class SearchTask extends AsyncTask<String, Void, List<Series>> {
        @Override
        protected void onPreExecute() {
        	// ???
		}
		@Override
		protected List<Series> doInBackground(String... params) {
			return Series.find(getActivity(), params[0]);
		}
        @Override
        protected void onPostExecute(List<Series> result) {
        	if (!result.isEmpty())
        		mAdapter.setTitles(result);
        	else
        		getActivity().getFragmentManager().popBackStack();
        }
	}
}