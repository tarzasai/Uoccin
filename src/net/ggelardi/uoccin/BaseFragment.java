package net.ggelardi.uoccin;

import net.ggelardi.uoccin.serv.Session;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public abstract class BaseFragment extends Fragment implements OnClickListener {
	public static final String ROOT_FRAGMENT = "net.ggelardi.uoccin.ROOT_FRAGMENT";
	public static final String LEAF_FRAGMENT = "net.ggelardi.uoccin.LEAF_FRAGMENT";
	
	protected Session session;
	protected AlphaAnimation blink;
	protected OnFragmentListener mListener;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(tag(), "onCreate");
		
		session = Session.getInstance(getActivity());

		blink = new AlphaAnimation(0.0f, 1.0f);
		blink.setDuration(350);
		blink.setStartOffset(20);
		blink.setRepeatMode(Animation.REVERSE);
		blink.setRepeatCount(2);
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		
		//menu.clear();
		
		Log.v(tag(), "onCreateOptionsMenu");
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		Log.v(tag(), "onPrepareOptionsMenu");
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
	
	protected void setTitle(String text) {
		getActivity().setTitle(text);
	}
	
	public interface OnFragmentListener {
		void fragmentAttached(BaseFragment fragment);
		void setIcon(int toolbarIcon);
		void showHourGlass(boolean value);
		void openMovieInfo(String imdb_id);
		void openSeriesInfo(String tvdb_id);
		void openSeriesSeason(String tvdb_id, int season);
		void openSeriesEpisode(String tvdb_id, int season, int episode);
	}
}