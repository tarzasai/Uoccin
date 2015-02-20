package net.ggelardi.uoccin;

import net.ggelardi.uoccin.data.MoviesAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class SearchMoviesFragment extends BaseFragment implements AbsListView.OnItemClickListener {

	private AbsListView mListView;
	private MoviesAdapter mAdapter;
	private String mSearchText;
	
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

		mListView = (AbsListView) view.findViewById(android.R.id.list);
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
		
		mListView.setOnItemClickListener(this);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		mAdapter.search(mSearchText);
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
		
		if (savedInstanceState == null)
			return;

		mSearchText = savedInstanceState.getString("text", null);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		mListener.onFragmentAttached(getResources().getString(R.string.drawer_fndmovies));
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		
	}
}