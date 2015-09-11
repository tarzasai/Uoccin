package net.ggelardi.uoccin;

import java.util.Arrays;
import java.util.List;

import net.ggelardi.uoccin.adapters.MovieAdapter;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.Title;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
import net.ggelardi.uoccin.serv.Commons.TL;
import net.ggelardi.uoccin.serv.MovieTask;
import net.ggelardi.uoccin.serv.MovieTask.MovieTaskContainer;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
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
	
	private AbsListView listView;
	private MenuItem miSortDir;
	private MenuItem miSortName;
	private MenuItem miSortYear;
	private MenuItem miSortRats;
	private Spinner spnFilter;
	private EditText edtFltText;
	private ImageView imgFltClear;
	
	private MovieAdapter mAdapter;
	private MovieTask mTask;
	private ArrayAdapter<CharSequence> mFilter;
	
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
		mFilter = ArrayAdapter.createFromResource(getActivity(), R.array.sel_filter_names,
			android.R.layout.simple_spinner_item);
		mFilter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	}
	
	@SuppressLint("InflateParams")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_movie_items, container, false);

		listView = (AbsListView) view.findViewById(android.R.id.list);
		spnFilter = (Spinner) view.findViewById(R.id.spn_ml_flt);
		edtFltText = (EditText) view.findViewById(R.id.edt_ml_flt);
		imgFltClear = (ImageView) view.findViewById(R.id.img_ml_flt);
		
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
		
		spnFilter.setAdapter(mFilter);
		spnFilter.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				mAdapter.setFilter(position, edtFltText.getText().toString());
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		edtFltText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				imgFltClear.setVisibility(TextUtils.isEmpty(edtFltText.getText()) ? View.GONE : View.VISIBLE);
				mAdapter.setFilter(spnFilter.getSelectedItemPosition(), edtFltText.getText().toString());
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
		
		getActivity().setTitle(type.equals(TL.SEARCH) ?
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
		super.onCreateOptionsMenu(menu, inflater);
		
		inflater.inflate(R.menu.lists, menu);

		miSortDir = menu.findItem(R.id.action_sort_toggle);
		miSortName = menu.findItem(R.id.action_sort_name);
		miSortYear = menu.findItem(R.id.action_sort_year);
		miSortRats = menu.findItem(R.id.action_sort_rats);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
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
		
		mListener.setIcon(R.drawable.ic_action_movie);
	}
	
	@Override
	public void onClick(View v) {
		hideKeyboard();
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
						if (type.equals(TL.SEARCH))
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
		int pos = listView instanceof GridView ? position - 2 : position - 1;
		mListener.openMovieInfo(mAdapter.getItem(pos).imdb_id);
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
		return Arrays.asList(getResources().getStringArray(R.array.view_defmov_ids)).indexOf(data);
	}
	
	private void reload() {
		Log.v(tag(), "reload()");
		mTask = new MovieTask(this, type);
		if (type.equals(TL.SEARCH))
			mTask.execute(data);
		else {
			String query = QUERIES[queryIdx()] + " and not '" + Title.FORGET_TAG +
				"' in (select tag from movtag where movie = imdb_id)";
			if (sortVal == 0)
				query += " order by name collate nocase" + (sortDesc ? " desc" : " asc");
			else if (sortVal == 1)
				query += " order by year" + (sortDesc ? " desc" : " asc") + ", name collate nocase";
			else
				query += " order by rating" + (sortDesc ? " desc" : " asc") + ", name collate nocase";
			mTask.execute(query);
		}
	}
	
	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(imgFltClear.getWindowToken(), 0);
		listView.requestFocus();
	}
}