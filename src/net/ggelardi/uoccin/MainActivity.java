package net.ggelardi.uoccin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ggelardi.uoccin.adapters.DrawerAdapter;
import net.ggelardi.uoccin.adapters.DrawerAdapter.DrawerItem;
import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Commons.TitleList;
import net.ggelardi.uoccin.serv.Service;
import net.ggelardi.uoccin.serv.Session;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

public class MainActivity extends ActionBarActivity implements BaseFragment.OnFragmentListener {
	private static final String TAG = "MainActivity";
	private static final int REQUEST_ACCOUNT_PICKER = 1;
	private static final int REQUEST_AUTHORIZATION = 2;
	
	private Session session;
    private Toolbar toolbar;
    private DrawerLayout drawer;
	private DrawerAdapter drawerData;
	private ExpandableListView drawerList;
	private ProgressBar progressBar;
	private CharSequence lastTitle;
	private int lastIcon;
	private String lastView;
	private int lastDrawerGroup = -1;
	private int lastDrawerChild = -1;
	private int pbCount = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		session = Session.getInstance(this);
		
		toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_navigation_menu);
        
        drawer = (DrawerLayout) findViewById(R.id.drawer);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);
        drawer.setDrawerListener(new DrawerListener() {
			@Override
			public void onDrawerStateChanged(int state) {
			}
			@Override
			public void onDrawerSlide(View view, float offset) {
			}
			@Override
			public void onDrawerOpened(View view) {
				supportInvalidateOptionsMenu();
			}
			@Override
			public void onDrawerClosed(View view) {
				supportInvalidateOptionsMenu();
			}
		});
        
        drawerData = new DrawerAdapter(this);
        
		drawerList = (ExpandableListView) findViewById(R.id.drawer_list);
		drawerList.setAdapter(drawerData);
		drawerList.setOnGroupExpandListener(new OnGroupExpandListener() {
			int previousGroup = -1;
			@Override
			public void onGroupExpand(int groupPosition) {
				if (groupPosition != previousGroup)
					drawerList.collapseGroup(previousGroup);
				if (groupPosition != lastDrawerGroup)
					drawerList.setItemChecked(drawerList.getCheckedItemPosition(), false);
				else {
					int index = drawerList.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, lastDrawerChild));
					drawerList.setItemChecked(index, true);
				}
				previousGroup = groupPosition;
			}
		});
		drawerList.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				drawer.closeDrawer(Gravity.START);
				DrawerItem di = drawerData.getChild(groupPosition, childPosition);
				if (di.type.equals(DrawerItem.ACTION)) {
					
				} else {
					openDrawerItem(di);
					lastDrawerGroup = groupPosition;
					lastDrawerChild = childPosition;
					int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition, childPosition));
					parent.setItemChecked(index, true);
				}
				return false;
			}
		});
		
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
		lastTitle = getTitle();
		lastIcon = R.drawable.ic_navigation_menu;
		
		lastView = session.getPrefs().getString(PK.STARTUPV, "sernext");
		if (savedInstanceState != null)
			lastView = savedInstanceState.getString("lastView", lastView);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent: " + intent.toString());
		
		setIntent(intent);
	}
	
	@Override
	public void onBackPressed() {
		if (drawer.isDrawerOpen(Gravity.START))
			drawer.closeDrawer(Gravity.START);
		else
			super.onBackPressed();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		dropHourGlass();
		
		if (session.driveSyncEnabled() && !session.driveAccountSet()) {
			Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
				new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, true, null, null, null, null);
			startActivityForResult(googlePicker, REQUEST_ACCOUNT_PICKER);
		}
		
		Intent intent = getIntent();
		String action = intent.getAction();
		if (TextUtils.isEmpty(action)) // is it even possible?
			action = Intent.ACTION_MAIN;
		if (action.equals(Commons.SN.CONNECT_FAIL)) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						new GSA(MainActivity.this).getRootFolder(true);
					} catch (UserRecoverableAuthIOException e) {
						startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
					} catch (Exception err) {
						Log.e(TAG, "onResume", err);
						Toast.makeText(MainActivity.this, err.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					}
				}
			});
			t.start();
		} else if (action.equals(Intent.ACTION_SEND)) {
			String err = "";
			String text = intent.getStringExtra(Intent.EXTRA_TEXT);
			String url = Commons.firstUrl(text);
			if (TextUtils.isEmpty(url))
				err = "No url found in text: " + text;
			else {
				Uri uri = Uri.parse(url);
				if (uri.getHost().endsWith("imdb.com")) {
					// http://imdb.com/rg/an_share/title/title/tt1392190/
					Pattern pattern = Pattern.compile("/(tt\\d+)/");
					Matcher matcher = pattern.matcher(url);
					if (matcher.find()) {
						openMovieInfo(matcher.group(1));
						return;
					}
				} else if (uri.getHost().endsWith("thetvdb.com")) {
					// http://thetvdb.com/index.php?tab=series&id=78804
					// http://thetvdb.com/?tab=season&seriesid=78804&seasonid=31270&lid=7
					// http://thetvdb.com/?tab=episode&seriesid=78804&seasonid=31270&id=371448&lid=7
					String tab = uri.getQueryParameter("tab");
					if (!TextUtils.isEmpty(tab)) {
						String tvdb_id = tab.equals("series") ? uri.getQueryParameter("id") :
							"season|episode".contains(tab) ? uri.getQueryParameter("seriesid") : null;
						if (!TextUtils.isEmpty(tvdb_id)) {
							openSeriesInfo(tvdb_id);
							return;
						}
					}
				}
				err = "Unknown url: " + url;
			}
			Log.d(TAG, err);
			Toast.makeText(this, err, Toast.LENGTH_LONG).show();
		}
		
		if (!hasRootFragment()) {
			openDrawerItem(drawerData.findItem(lastView));
			// select it on the listview
			Pair<Integer, Integer> p = drawerData.getChildPos(lastView);
			int index = drawerList.getFlatListPosition(ExpandableListView.getPackedPositionForChild(p.first, p.second));
			drawerList.expandGroup(p.first);
			drawerList.setItemChecked(index, true);
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		if (savedInstanceState != null)
			lastView = savedInstanceState.getString("lastView", session.getPrefs().getString(PK.STARTUPV, "sernext"));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		if (!TextUtils.isEmpty(lastView))
			outState.putString("lastView", lastView);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		//
		if (drawer.isDrawerOpen(Gravity.START)) {
			getMenuInflater().inflate(R.menu.global, menu);
			actionBar.setTitle(R.string.app_name);
			toolbar.setNavigationIcon(R.drawable.ic_navigation_menu);
		} else {
			getMenuInflater().inflate(R.menu.main, menu);
			actionBar.setTitle(lastTitle);
			toolbar.setNavigationIcon(lastIcon);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch (item.getItemId()) {
			case android.R.id.home:
				if (drawer.isDrawerOpen(Gravity.START))
					drawer.closeDrawer(Gravity.START);
				else
					drawer.openDrawer(Gravity.START);
				return true;
			case R.id.action_search:
				searchDialog();
				return true;
			case R.id.action_settings:
				startActivity(new Intent(this, SettingsActivity.class));
				return true;
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			// TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST TEST
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			case R.id.action_test_clean:
				WakefulIntentService.sendWakefulWork(this,
					new Intent(this, Service.class).setAction(Service.CLEAN_DB_CACHE));
				return true;
			case R.id.action_test_gdsyn:
				WakefulIntentService.sendWakefulWork(this,
					new Intent(this, Service.class).setAction(Service.GDRIVE_SYNC));
				return true;
			case R.id.action_test_gdbak:
				WakefulIntentService.sendWakefulWork(this,
					new Intent(this, Service.class).setAction(Service.GDRIVE_BACKUP));
				return true;
			case R.id.action_test_gdres:
				WakefulIntentService.sendWakefulWork(this,
					new Intent(this, Service.class).setAction(Service.GDRIVE_RESTORE));
				return true;
			case R.id.action_test_check:
				Cursor cr = session.getDB().rawQuery("select episode, collected, watched, subtitles " +
					"from episode where series = '82459' and season = 7 order by episode", null);
				try {
					while (cr.moveToNext())
						Log.v("Title", Commons.logCursor("episode", cr));
				} finally {
					cr.close();
				}
				return true;
			case R.id.action_test_tvdbn:
				WakefulIntentService.sendWakefulWork(this,
					new Intent(this, Service.class).setAction(Service.CHECK_TVDB_RSS));
				return true;
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
			////////////////////////////////////////////////////////////////////////////////////////////////////////////
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void setTitle(CharSequence title) {
		lastTitle = title;
		getSupportActionBar().setTitle(lastTitle);
	}
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == RESULT_OK && data != null)
			session.setDriveUserAccount(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
	}

	public void openDrawerItem(DrawerItem selection) {
		if (selection.id.equals("settings")) {
			startActivity(new Intent(this, SettingsActivity.class));
			return;
		}
		dropHourGlass();
		lastView = selection.id;
		BaseFragment f = selection.type.equals(DrawerItem.SERIES) ?
			SeriesListFragment.newFragment(TitleList.QUERY, selection.id) :
			MovieListFragment.newFragment(TitleList.QUERY, selection.id);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f, BaseFragment.ROOT_FRAGMENT).commit();
	}
	
	@Override
	public void fragmentAttached(BaseFragment fragment) {
		dropHourGlass();
	}
	
	@Override
	public void setIcon(int toolbarIcon) {
		lastIcon = toolbarIcon;
		if (toolbar != null)
			toolbar.setNavigationIcon(lastIcon);
	}
	
	@Override
	public void showHourGlass(boolean value) {
		if (progressBar == null)
			return;
		if (value)
			pbCount++;
		else
			pbCount--;
		progressBar.setVisibility(pbCount > 0 ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public void openMovieInfo(String imdb_id) {
		BaseFragment f = MovieInfoFragment.newInstance(imdb_id);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT);
		if (hasRootFragment())
			ft.addToBackStack(null);
		ft.commit();
	}
	
	@Override
	public void openSeriesInfo(String tvdb_id) {
		BaseFragment f = SeriesInfoFragment.newInstance(tvdb_id);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT);
		if (hasRootFragment())
			ft.addToBackStack(null);
		ft.commit();
	}
	
	@Override
	public void openSeriesSeason(String tvdb_id, int season) {
		BaseFragment f = EpisodeListFragment.newList(tvdb_id, season);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT);
		if (hasRootFragment())
			ft.addToBackStack(null);
		ft.commit();
	}
	
	@Override
	public void openSeriesEpisode(String tvdb_id, int season, int episode) {
		BaseFragment f = EpisodeInfoFragment.newInstance(tvdb_id, season, episode);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT);
		if (hasRootFragment())
			ft.addToBackStack(null);
		ft.commit();
	}
	
	private void dropHourGlass() {
		pbCount = 0;
		if (progressBar != null)
			progressBar.setVisibility(View.GONE);
	}
	
	private boolean hasRootFragment() {
		return getSupportFragmentManager().findFragmentByTag(BaseFragment.ROOT_FRAGMENT) != null;
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
		BaseFragment f = SeriesListFragment.newFragment(TitleList.SEARCH, text);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
	
	private void searchMovies(String text) {
		BaseFragment f = MovieListFragment.newFragment(TitleList.SEARCH, text);
		getSupportFragmentManager().beginTransaction().replace(R.id.container, f,
			BaseFragment.LEAF_FRAGMENT).addToBackStack(null).commit();
	}
}