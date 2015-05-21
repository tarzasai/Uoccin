package net.ggelardi.uoccin;

import java.util.Arrays;
import java.util.List;

import net.ggelardi.uoccin.adapters.SeriesAdapter;
import net.ggelardi.uoccin.data.Episode.EID;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.Commons.TitleList;
import net.ggelardi.uoccin.serv.SeriesTask;
import net.ggelardi.uoccin.serv.SeriesTask.SeriesTaskContainer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.photos.views.HeaderGridView;

public class SeriesListFragment extends BaseFragment implements AbsListView.OnItemClickListener, OnTitleListener,
	SeriesTaskContainer {
	
	private static final String[] QUERIES = new String[] {
		//
		"select s.tvdb_id from series s where s.watchlist = 1",
		//
		"select distinct s.tvdb_id from series s join episode e on (e.series = s.tvdb_id) where " +
			"datetime(e.firstAired/1000, 'unixepoch') between datetime('now') and datetime('now', '+6 days') " +
			"and (s.watchlist = 1 or (e.season = 1 and e.episode = 1))",
		//
		"select distinct s.tvdb_id from series s join episode e on (e.series = s.tvdb_id) where " +
			"datetime(e.firstAired/1000, 'unixepoch') < datetime('now', '-12 hours') and " +
			"s.watchlist = 1 and e.collected = 0 and e.watched = 0",
		//
		"select distinct s.tvdb_id from series s join episode e on (e.series = s.tvdb_id) where " +
			"e.collected = 1 and e.watched = 0",
		//
		"select distinct s.tvdb_id from series s join episode e on (e.series = s.tvdb_id) where " +
			"datetime(e.firstAired/1000, 'unixepoch') between datetime('now', '-7 days') " +
			"and datetime('now', '+90 days') and e.season = 1 and e.episode = 1",
		//
		"select distinct s.tvdb_id from series s join episode e on (e.series = s.tvdb_id)"
	};
	
	private String type;
	private String data;
	
	private boolean forceReload = false;
	private boolean sortDesc = false;
	private int sortVal = 0;
	
	private AbsListView listView;
	private MenuItem miSortDir;
	private MenuItem miSortName;
	private MenuItem miSortYear;
	private MenuItem miSortRats;
	private EditText edtFltText;
	private ImageView imgFltClear;
	
	private SeriesAdapter mAdapter;
	private SeriesTask mTask;
	
	public static SeriesListFragment newFragment(String type, String data) {
		SeriesListFragment fragment = new SeriesListFragment();
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
		
		mAdapter = new SeriesAdapter(getActivity(), this, type, data);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_series_items, container, false);

		listView = (AbsListView) view.findViewById(android.R.id.list);
		edtFltText = (EditText) view.findViewById(R.id.edt_sl_flt);
		imgFltClear = (ImageView) view.findViewById(R.id.img_sl_flt);
		
		if (listView instanceof ListView) {
			((ListView) listView).addHeaderView(inflater.inflate(R.layout.header_space, null));
			((ListView) listView).addFooterView(inflater.inflate(R.layout.header_space, null));
		} else {
			((HeaderGridView) listView).addHeaderView(inflater.inflate(R.layout.header_space, null));
			//((HeaderGridView) mListView).addFooterView(inflater.inflate(R.layout.header_space, null));
		}
		
		listView.setOnItemClickListener(this);
		listView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (scrollState != OnScrollListener.SCROLL_STATE_IDLE)
					hideKeyboard();
				// TODO: animation to collapse boxFilter and expand listView when scrolling.
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
		
		edtFltText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				imgFltClear.setVisibility(TextUtils.isEmpty(edtFltText.getText()) ? View.GONE : View.VISIBLE);
				mAdapter.getFilter().filter(edtFltText.getText());
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		
		imgFltClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				edtFltText.setText(null);
				hideKeyboard();
			}
		});
		
		((AdapterView<ListAdapter>) listView).setAdapter(mAdapter);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		getActivity().setTitle(type.equals(TitleList.SEARCH) ?
			String.format(getString(R.string.title_search), data) :
			session.getRes().getStringArray(R.array.view_defser_titles)[queryIdx()]);
		
		Title.addOnTitleEventListener(this);
		
		reload();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Title.removeOnTitleEventListener(this);
		if (mTask != null)
			mTask.cancel(true);
		hideKeyboard();
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

		miSortDir = menu.findItem(R.id.action_sort_toggle);
		miSortName = menu.findItem(R.id.action_sort_name);
		miSortYear = menu.findItem(R.id.action_sort_year);
		miSortRats = menu.findItem(R.id.action_sort_rats);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		checkMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == miSortDir) {
			sortDesc = !sortDesc;
			reload();
			return true;
		}
		if (item == miSortName) {
			if (sortVal != 0) {
				sortVal = 0;
				reload();
			}
			return true;
		}
		if (item == miSortYear) {
			if (sortVal != 1) {
				sortVal = 1;
				reload();
			}
			return true;
		}
		if (item == miSortRats) {
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
		
		mListener.setIcon(R.drawable.ic_action_tv);
	}
	
	@Override
	public void onClick(View v) {
		hideKeyboard();
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
	public void postExecuteTask(List<Series> result) {
		showHourGlass(false);
		mTask = null;
		mAdapter.setTitles(result, forceReload);
		forceReload = false;
		checkMenu();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int pos = listView instanceof GridView ? position - 2 : position - 1;
		mListener.openSeriesInfo(mAdapter.getItem(pos).tvdb_id);
	}
	
	private void checkMenu() {
		if (mAdapter == null || miSortDir == null || miSortName == null || miSortYear == null || miSortRats == null)
			return;
		miSortDir.setVisible(mAdapter.getCount() > 0);
		miSortName.setVisible(mAdapter.getCount() > 0);
		miSortYear.setVisible(mAdapter.getCount() > 0);
		miSortDir.setIcon(sortDesc ? R.drawable.ic_action_sort_desc : R.drawable.ic_action_sort_asc);
		if (sortVal == 0)
			miSortName.setChecked(true);
		else if (sortVal == 1)
			miSortYear.setChecked(true);
		else
			miSortRats.setChecked(true);
	}
	
	private int queryIdx() {
		return Arrays.asList(getResources().getStringArray(R.array.view_defser_ids)).indexOf(data);
	}
	
	private void reload() {
		Log.v(getTag(), "reload()");
		mTask = new SeriesTask(this, type);
		if (type.equals(TitleList.SEARCH))
			mTask.execute(data);
		else {
			String query = QUERIES[queryIdx()];
			if (sortVal == 0)
				query += " order by s.name collate nocase" + (sortDesc ? " desc" : " asc");
			else if (sortVal == 1)
				query += " order by s.year" + (sortDesc ? " desc" : " asc") + ", s.name collate nocase";
			else
				query += " order by s.rating" + (sortDesc ? " desc" : " asc") + ", s.name collate nocase";
			mTask.execute(query);
		}
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(imgFltClear.getWindowToken(), 0);
		listView.requestFocus();
	}
}