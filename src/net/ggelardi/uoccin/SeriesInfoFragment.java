package net.ggelardi.uoccin;

import net.ggelardi.uoccin.data.Series;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SeriesInfoFragment extends BaseFragment {

	private String tvdb_id;
	private Series series;
	
	public static SeriesInfoFragment getInstance(String tvdb_id) {
		SeriesInfoFragment fragment = new SeriesInfoFragment();
		Bundle args = new Bundle();
		args.putString("tvdb_id", tvdb_id);
		fragment.setArguments(args);
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle args = getArguments();
		
		tvdb_id = args.getString("tvdb_id");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_series, container, false);
		
		return view;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		series = Series.get(getActivity(), tvdb_id);
		
		getActivity().setTitle(series.name);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	}
}