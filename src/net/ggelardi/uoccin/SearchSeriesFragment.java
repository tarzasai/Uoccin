package net.ggelardi.uoccin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

public class SearchSeriesFragment extends BaseFragment {
	
	/**
	 * The fragment's ListView/GridView.
	 */
	private AbsListView mListView;
	
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

		String searchText = getArguments().getString("text");
		

	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_search, container, false);
		
		//
		
		return view;
	}
	
}