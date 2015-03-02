package net.ggelardi.uoccin;

import java.util.List;

import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.data.MoviesAdapter;
import net.ggelardi.uoccin.data.Title.OnTitleListener;
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
import android.widget.Toast;

public class SearchMoviesFragment extends BaseFragment implements AbsListView.OnItemClickListener {

	private AbsListView mListView;
	private MoviesAdapter mAdapter;
	private String mSearchText;
	private SearchTask finder;
	
	public static SearchMoviesFragment newInstance(String text) {
		SearchMoviesFragment fragment = new SearchMoviesFragment();
		Bundle args = new Bundle();
		args.putString("text", text);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSearchText = getArguments().getString("text");
		mAdapter = new MoviesAdapter(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_search, container, false);
		
		getActivity().setTitle(getString(R.string.drawer_fndmovies));

		mListView = (AbsListView) view.findViewById(android.R.id.list);
		
		mListView.setOnItemClickListener(this);
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Movie.addOnTitleEventListener(new OnTitleListener() {
			@Override
			public void changed(final String state, final Throwable error) {
				Log.v(logTag(), state);
				final Activity context = getActivity();
				if (context != null)
					context.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (state.equals(OnTitleListener.NOTFOUND)) {
								Toast.makeText(context, R.string.search_not_found, Toast.LENGTH_LONG).show();
								//mAdapter.notifyDataSetChanged();
							} else if (state.equals(OnTitleListener.WORKING)) {
								
							} else if (state.equals(OnTitleListener.ERROR)) {
								Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
								//mAdapter.notifyDataSetChanged();
							} else if (state.equals(OnTitleListener.READY)) {
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		
	}
	
	private class SearchTask extends AsyncTask<String, Void, List<Movie>> {
        @Override
        protected void onPreExecute() {
        	// ???
		}
		@Override
		protected List<Movie> doInBackground(String... params) {
			return Movie.find(getActivity(), params[0]);
		}
        @Override
        protected void onPostExecute(List<Movie> result) {
        	if (!result.isEmpty())
        		mAdapter.setTitles(result);
        	else
        		getActivity().getFragmentManager().popBackStack();
        }
	}
}