package net.ggelardi.uoccin;

import net.ggelardi.uoccin.data.DashboardAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;

public class DashboardFragment extends BaseFragment implements AbsListView.OnItemClickListener {
	
	private AbsListView mListView;
	private DashboardAdapter mAdapter;
	
	public static DashboardFragment newInstance() {
		DashboardFragment fragment = new DashboardFragment();
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mAdapter = new DashboardAdapter(getActivity());
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_items, container, false);
		
		getActivity().setTitle(getString(R.string.drawer_dashboard));
		
		mListView = (AbsListView) view.findViewById(android.R.id.list);
		
		((AdapterView<ListAdapter>) mListView).setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
		
		return view;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		/*
		if (null != mListener) {
			// Notify the active callbacks interface (the activity, if the
			// fragment is attached to one) that an item has been selected.
			mListener.onFragmentInteraction(DummyContent.ITEMS.get(position).id);
		}
		*/
	}
}