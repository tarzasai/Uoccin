package net.ggelardi.uoccin;

import net.ggelardi.uoccin.serv.Session;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public abstract class BaseFragment extends Fragment implements OnClickListener {
	
	protected Session session;
	protected OnFragmentListener mListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(logTag(), "onCreate");
		
		session = Session.getInstance(getActivity());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.v(logTag(), "onResume");
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Log.v(logTag(), "onPause");
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		Log.v(logTag(), "onSaveInstanceState");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Log.v(logTag(), "onActivityCreated");
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Log.v(logTag(), "onAttach");
		
		try {
			mListener = (OnFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
		}
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		Log.v(logTag(), "onDetach");
		
		mListener = null;
	}
	
	@Override
	public void onClick(View v) {
		//
	}
	
	protected String logTag() {
		return this.getClass().getSimpleName();
	}
	
	protected void showHourGlass(boolean value) {
		getActivity().setProgressBarIndeterminateVisibility(value);
	}
	
	public interface OnFragmentListener {
		void openSeriesInfo(String tvdb_id);
		void openSeriesSeason(String tvdb_id, int season);
		void openSeriesEpisode(String tvdb_id, int season, int episode);
	}
}