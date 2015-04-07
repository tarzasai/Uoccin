package net.ggelardi.uoccin;

import net.ggelardi.uoccin.serv.Session;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class BaseFragment extends Fragment implements OnClickListener {
	public static final String ROOT_FRAGMENT = "net.ggelardi.uoccin.ROOT_FRAGMENT";
	public static final String LEAF_FRAGMENT = "net.ggelardi.uoccin.LEAF_FRAGMENT";
	
	protected Session session;
	protected OnFragmentListener mListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(tag(), "onCreate");
		
		session = Session.getInstance(getActivity());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.v(tag(), "onResume");
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Log.v(tag(), "onPause");
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		Log.v(tag(), "onSaveInstanceState");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Log.v(tag(), "onActivityCreated");
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Log.v(tag(), "onAttach");
		
		try {
			mListener = (OnFragmentListener) activity;
			mListener.fragmentAttached(this);
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		Log.v(tag(), "onDetach");
		
		mListener = null;
	}
	
	@Override
	public void onClick(View v) {
		// nothing here.
	}
	
	protected String tag() {
		return this.getClass().getSimpleName();
	}
	
	protected void showHourGlass(boolean value) {
		if (mListener != null)
			mListener.showHourGlass(value);
	}
	
	public interface OnFragmentListener {
		void fragmentAttached(BaseFragment fragment);
		void showHourGlass(boolean value);
		void openSeriesInfo(String tvdb_id);
		void openSeriesSeason(String tvdb_id, int season);
		void openSeriesEpisode(String tvdb_id, int season, int episode);
	}
}