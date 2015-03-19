package net.ggelardi.uoccin;

import net.ggelardi.uoccin.adapters.DrawerAdapter.DrawerItem;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Session;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements DrawerFragment.NavigationDrawerCallbacks,
		BaseFragment.OnFragmentListener {
	
	private Session session;
	private String lastView;
	
	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private DrawerFragment mDrawerFragment;
	
	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mStoredTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		session = Session.getInstance(this);
		
		mDrawerFragment = (DrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
		mDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout));
		
		mStoredTitle = getTitle();
		
		ActionBar ab = getSupportActionBar();
		ab.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
		//ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
		//ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME);
		//ab.setDisplayHomeAsUpEnabled(true);
		//ab.setDisplayUseLogoEnabled(true);
		//ab.setHomeButtonEnabled(true);
		ab.setIcon(R.mipmap.ic_launcher);
		
		lastView = session.getPrefs().getString(PK.STARTUP, "sernext");
		if (savedInstanceState != null)
			lastView = savedInstanceState.getString("lastView", lastView);
	}
	
	@Override
	public void onBackPressed() {
		if (mDrawerFragment.isDrawerOpen())
			mDrawerFragment.closeDrawer();
		else
			super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		mDrawerFragment.selectItem(lastView);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (savedInstanceState != null)
			lastView = savedInstanceState.getString("lastView");
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("lastView", lastView);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_search) {
			searchDialog();
			return true;
		}
		if (id == R.id.action_settings) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void setTitle(CharSequence title) {
		mStoredTitle = title;
		getSupportActionBar().setTitle(mStoredTitle);
	}
	
	@Override
	public void onNavigationDrawerItemSelected(DrawerItem selection) {
		if (selection.id.equals("settings")) {
			return;
		}
		lastView = selection.id;
		if (selection.type.equals(DrawerItem.SERIES))
			getSupportFragmentManager().beginTransaction().replace(R.id.container,
				SeriesListFragment.newQuery(selection.label, selection.query, selection.details,
					(String[]) null)).commit();
		else if (selection.type.equals(DrawerItem.MOVIE)) {
			
		}
	}
	
	@Override
	public void openSeriesInfo(String tvdb_id) {
		getSupportFragmentManager().beginTransaction().replace(R.id.container,
			SeriesInfoFragment.newInstance(tvdb_id)).addToBackStack(null).commit();
	}
	
	@Override
	public void openSeriesSeason(String tvdb_id, int season) {
		EpisodeListFragment f = EpisodeListFragment.newList(tvdb_id, season);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f).addToBackStack(null).commit();
	}
	
	@Override
	public void openSeriesEpisode(String tvdb_id, int season, int episode) {
		getSupportFragmentManager().beginTransaction().replace(R.id.container,
				EpisodeInfoFragment.newInstance(tvdb_id, season, episode)).addToBackStack(null).commit();
	}
	
	private void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mStoredTitle);
	}
	
	@SuppressLint("InflateParams")
	private void searchDialog() {
		LayoutInflater inflater = getLayoutInflater();
		final View view = inflater.inflate(R.layout.dialog_search, null);
		final EditText edt = (EditText) view.findViewById(R.id.edt_search_text);
		final RadioGroup grp = (RadioGroup) view.findViewById(R.id.grp_search_type);
		final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(R.string.search_dialog).setIcon(
			R.drawable.ic_action_search).setPositiveButton(R.string.dlg_btn_ok, null).setNegativeButton(
			R.string.dlg_btn_cancel, null).setView(view).create();
		edt.setTextIsSelectable(true);
		dlg.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
		dlg.setOnShowListener(new DialogInterface.OnShowListener() {
		    @Override
		    public void onShow(DialogInterface dialog) {
		        Button btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
		        btn.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	String text = edt.getText().toString();
		                if (TextUtils.isEmpty(text)) {
		                	Toast.makeText(dlg.getContext(), R.string.search_no_text, Toast.LENGTH_SHORT).show();
		                	return;
		                }
		                if (grp.getCheckedRadioButtonId() == R.id.rbt_search_series)
		            		searchSeries(text);
		            	else
		            		searchMovies(text);
		                dlg.dismiss();
		            }
		        });
		    }
		});
		dlg.show();
	}
	
	private void searchSeries(String text) {
		SeriesListFragment f = SeriesListFragment.newSearch(text);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f).addToBackStack(null).commit();
	}
	
	private void searchMovies(String text) {
		/*
		SearchMoviesFragment f = SearchMoviesFragment.newInstance(text);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f).addToBackStack(null).commit();
		*/
	}
}