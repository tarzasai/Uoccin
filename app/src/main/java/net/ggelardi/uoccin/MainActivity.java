package net.ggelardi.uoccin;

import android.accounts.AccountManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAuthIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import net.ggelardi.uoccin.api.GSA;
import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Service;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends BaseActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_PERMISSIONS = 90;
    private static final int REQUEST_ACCOUNT_PICKER = 91;
    private static final int REQUEST_AUTHORIZATION = 92;

    private int scope = 0; // 0) series, 1) movies
    private int lastView = 0;
    private MainSeriesAdapter seriesAdapter;
    private MainMoviesAdapter moviesAdapter;
    private FragmentStatePagerAdapter adapter;

    private DrawerLayout mDLayout;
    private NavigationView mNavView;
    private TabLayout mTLayout;
    private ViewPager mViPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUoccinActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setHomeButtonEnabled(true);

        mDLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle abdt = new ActionBarDrawerToggle(this, mDLayout,
                (Toolbar) findViewById(R.id.toolbar), R.string.maiact_drw_open, R.string.maiact_drw_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                Menu nm = mNavView.getMenu();
                try {
                    boolean conn = session.isConnected(false);
                    boolean sync = session.driveSyncEnabled();
                    boolean driv = session.driveAccountSet();
                    nm.findItem(R.id.nav_tools_sync).setVisible(conn && driv && sync);
                    nm.findItem(R.id.nav_tools_bckp).setVisible(conn && driv);
                    nm.findItem(R.id.nav_tools_rest).setVisible(conn && driv);
                    // PS: nav_tools_heal is in the same group (and always available).
                } catch (Exception err) {
                    Log.e(tag(), "onDrawerOpened", err);
                }
            }
        };
        mDLayout.addDrawerListener(abdt);
        abdt.setDrawerIndicatorEnabled(true);
        abdt.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        abdt.syncState();

        mNavView = (NavigationView) findViewById(R.id.nav_view);
        mNavView.setNavigationItemSelectedListener(this);

        mTLayout = (TabLayout) findViewById(R.id.tab_layout);
        mViPager = (ViewPager) findViewById(R.id.pager_view);
        mViPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float posOffset, int posOffsetPixels) {
                //
            }
            @Override
            public void onPageSelected(int position) {
                lastView = position;
                setTitle(scope == 0 ? SeriesFragment.titles[position] : MoviesFragment.titles[position]);
                highlightTabs();
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                //
            }
        });

        seriesAdapter = new MainSeriesAdapter(getSupportFragmentManager());
        moviesAdapter = new MainMoviesAdapter(getSupportFragmentManager());

        List<String> mp = session.getMissingPermissions();
        if (!mp.isEmpty()) {
            String[] pl = new String[mp.size()];
            pl = mp.toArray(pl);
            ActivityCompat.requestPermissions(this, pl, REQUEST_PERMISSIONS);
        }

        lastView = session.startupView();
        if (savedInstanceState != null) {
            scope = savedInstanceState.getInt("scope", scope);
            lastView = scope == 0 ? savedInstanceState.getInt("lastView", lastView) : 0;
        }

        session.checkAlarms(false);

        // help
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        addShowcase(getNavigationButtonViewTarget(), R.string.maiact_sct_drawer, R.string.maiact_scd_drawer);
        addShowcase(new ToolbarActionItemTarget(toolbar, R.id.action_search), R.string.maiact_sct_search, R.string.maiact_scd_search);
        addShowcase(new ViewTarget(findViewById(R.id.tab_layout)), R.string.maiact_sct_tabs, R.string.maiact_scd_tabs);
        addShowcase(new ViewTarget(findViewById(R.id.pager_view)), R.string.maiact_sct_pages, R.string.maiact_scd_pages);
        addShowcase(new ViewTarget(findViewById(R.id.pager_view)), R.string.maiact_sct_items, R.string.maiact_scd_items);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setTitle(getString(R.string.maiact_title));

        session.getPrefs().registerOnSharedPreferenceChangeListener(this);

        if (adapter == null)
            setScope(scope);
        mNavView.getMenu().findItem(scope == 0 ? R.id.nav_series : R.id.nav_movies).setChecked(true);
        setTitle(scope == 0 ? SeriesFragment.titles[lastView] : MoviesFragment.titles[lastView]);
        highlightTabs();

        if (session.driveSyncEnabled() && !session.driveAccountSet()) {
            Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null);
            startActivityForResult(googlePicker, REQUEST_ACCOUNT_PICKER);
        }

        checkDrawerAccountInfo();

        Intent intent = getIntent();
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            // is it even possible?
            action = Intent.ACTION_MAIN;
        } else if (action.equals(Commons.SN.CONNECT_FAIL)) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        new GSA(MainActivity.this).getRootFolderId(true);
                    } catch (UserRecoverableAuthIOException e) {
                        startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                    } catch (GoogleAuthIOException e) {
                        Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
                                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, true, null, null, null, null);
                        startActivityForResult(googlePicker, REQUEST_AUTHORIZATION);
                    } catch (Exception err) {
                        Log.e(tag(), "onResume", err);
                        new AlertDialog.Builder(MainActivity.this).setTitle("Google Auth Error")
                                .setMessage(err.getLocalizedMessage())
                                .setIcon(R.drawable.ic_dlgico_error)
                                .show();
                    }
                }
            });
            t.start();
            action = Intent.ACTION_MAIN;
        } else if (action.equals(Intent.ACTION_SEND)) {
            //TODO: process data
            action = Intent.ACTION_MAIN;
        }

        /*TODO
        if (action.equals(Commons.SN.MOV_WLST)) {
            runDrawerAction(R.id.nav_movlist);
        } else if (action.equals(Commons.SN.MOV_COLL)) {
            runDrawerAction(R.id.nav_movcoll);
        } else if (action.equals(Commons.SN.SER_WLST)) {
            runDrawerAction(R.id.nav_serlist);
        } else if (action.equals(Commons.SN.SER_COLL)) {
            runDrawerAction(R.id.nav_sercoll);
        } else if (action.equals(Commons.MA.MOVIE_INFO)) {
            if (intent.hasExtra("imdb_id"))
                openMovieInfo(intent.getStringExtra("imdb_id"));
        } else if (action.equals(Commons.MA.SERIES_INFO)) {
            if (intent.hasExtra("tvdb_id"))
                openSeriesInfo(intent.getIntExtra("tvdb_id", -1));
        } else if (action.equals(Commons.MA.EPISODE_INFO)) {
            if (intent.hasExtra("tvdb_id"))
                openSeriesEpisode(intent.getIntExtra("tvdb_id", -1));
            else if (intent.hasExtra("series") && intent.hasExtra("season") && intent.hasExtra("episode"))
                openSeriesEpisode(intent.getIntExtra("series", -1), intent.getIntExtra("season", -1),
                        intent.getIntExtra("episode", -1));
        } else if (fragman.getFragments() == null) {
            runDrawerAction(lastView);
        }
        */
    }

    @Override
    protected void onPause() {
        super.onPause();

        session.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            scope = savedInstanceState.getInt("scope", 0);
            lastView = scope == 0 ? savedInstanceState.getInt("lastView", session.startupView()) : 0;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("scope", scope);
        if (scope == 0)
            outState.putInt("lastView", lastView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return runDrawerAction(item.getItemId());
    }

    @Override
    public void onBackPressed() {
        Log.d(tag(), "onBackPressed");
        if (mDLayout.isDrawerOpen(GravityCompat.START))
            mDLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        runDrawerAction(item.getItemId());
        mDLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == REQUEST_ACCOUNT_PICKER && resultCode == RESULT_OK && data != null)
            session.setDriveUserAccount(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Commons.PK.GDRVAUTH))
            checkDrawerAccountInfo();
    }

    private void checkDrawerAccountInfo() {
        View hv = mNavView.getHeaderView(0);
        CircleImageView mIcon = (CircleImageView) hv.findViewById(R.id.nav_hdr_icon);
        TextView mName = (TextView) hv.findViewById(R.id.nav_hdr_name);
        TextView mMail = (TextView) hv.findViewById(R.id.nav_hdr_mail);

        if (!session.driveAccountSet()) {
            mIcon.setVisibility(View.GONE);
            mName.setVisibility(View.GONE);
            mMail.setVisibility(View.GONE);
        } else {
            String email = session.driveAccountName();
            String uname = "";
            String photo = "";
            Cursor ec = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Email.DATA + " = ?", new String[]{email},
                    null);
            try {
                int cn = ec.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int cp = ec.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
                while (ec.moveToNext()) {
                    uname = ec.getString(cn);
                    if (!TextUtils.isEmpty(uname)) {
                        photo = ec.getString(cp);
                        break;
                    }
                }
            } finally {
                ec.close();
            }
            mIcon.setVisibility(View.VISIBLE);
            mName.setVisibility(View.VISIBLE);
            mMail.setVisibility(View.VISIBLE);
            mName.setText(uname);
            mMail.setText(email);
            if (!TextUtils.isEmpty(photo)) {
                session.setDriveUserPhoto(photo);
                mIcon.setImageURI(Uri.parse(photo));
            }
        }
    }

    private void setScope(int scope) {
        this.scope = scope;
        if (scope == 0) {
            lastView = session.startupView();
            adapter = seriesAdapter;
            setTitle(SeriesFragment.titles[lastView]);
        } else {
            lastView = 0;
            adapter = moviesAdapter;
            setTitle(MoviesFragment.titles[0]);
        }
        adapter.notifyDataSetChanged();
        mViPager.setAdapter(adapter);
        mTLayout.setupWithViewPager(mViPager);
        resetTabs();
        mViPager.setCurrentItem(lastView);
    }

    private void resetTabs() {
        TabLayout.Tab tl;
        TextView tv;
        for (int i = 0; i < mTLayout.getTabCount(); i++) {
            tl = mTLayout.getTabAt(i);
            tl.setCustomView(R.layout.item_tab);
            tv = (TextView) tl.getCustomView();
            tv.setText("");
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    scope == 0 ? SeriesFragment.icons[i] : MoviesFragment.icons[i], 0, 0, 0);
        }
    }

    private void highlightTabs() {
        TextView tv;
        for (int i = 0; i < mTLayout.getTabCount(); i++) {
            tv = (TextView) (mTLayout.getTabAt(i)).getCustomView();
            tv.setAlpha(i == mViPager.getCurrentItem() ? 1 : (float) 0.5);
        }
    }

    private boolean runDrawerAction(int actionId) {
        switch (actionId) {
            case R.id.nav_series:
                setScope(0);
                break;
            case R.id.nav_movies:
                setScope(1);
                break;
            case R.id.nav_tools_sync:
                startService(new Intent(MainActivity.this, Service.class).setAction(Commons.SR.GDRIVE_SYNCNOW));
                showMessage(R.string.maiact_msg_syncreq);
                break;
            /*case R.id.nav_tools_heal:
                startService(new Intent(MainActivity.this, Service.class).setAction(Commons.SR.CLEAN_DB_CACHE));
                showMessage(R.string.maiact_msg_dbclean);
                break;*/
            case R.id.nav_tools_bckp:
                new AlertDialog.Builder(this).setTitle(R.string.action_backup).setMessage(R.string.maiact_ask_backup).
                        setIcon(R.drawable.ic_dlgico_warning).setNegativeButton(R.string.dlg_btn_cancel, null).
                        setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startService(new Intent(MainActivity.this, Service.class).setAction(Commons.SR.GDRIVE_BACKUP));
                            }
                        }).show();
                break;
            case R.id.nav_tools_rest:
                String msg = getString(R.string.maiact_ask_restore);
                if (!session.isConnected(true))
                    msg += getString(R.string.maiact_warn_restore);
                new AlertDialog.Builder(this).setTitle(R.string.action_restore).setMessage(msg).
                        setIcon(R.drawable.ic_dlgico_warning).setNegativeButton(R.string.dlg_btn_cancel, null).
                        setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startService(new Intent(MainActivity.this, Service.class).setAction(Commons.SR.GDRIVE_RESTORE));
                            }
                        }).show();
                break;
            case R.id.action_search:
                searchDialog();
                break;
            case R.id.action_settings:
            case R.id.nav_misc_sett:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.nav_misc_fdbk:
                Intent mi = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
                mi.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mi);
                break;
            case R.id.action_help:
                runShowcase();
                break;
            default:
                return false;
        }
        return true;
    }

    private void searchDialog() {
        LayoutInflater inflater = getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_search, null);
        final EditText edt = (EditText) view.findViewById(R.id.edt_search_text);
        final RadioGroup grp = (RadioGroup) view.findViewById(R.id.grp_search_type);
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(R.string.maiact_find_title).setIcon(
                R.drawable.ic_action_search).setPositiveButton(R.string.dlg_btn_ok, null).setNegativeButton(
                R.string.dlg_btn_cancel, null).setView(view).create();
        edt.setTextIsSelectable(true);
        dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn = dlg.getButton(AlertDialog.BUTTON_POSITIVE);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String text = edt.getText().toString();
                        if (TextUtils.isEmpty(text))
                            return;
                        boolean series = grp.getCheckedRadioButtonId() == R.id.rbt_search_series;
                        startActivity(new Intent(view.getContext(), SearchActivity.class).putExtra("text", text)
                                .putExtra("type", series ? SearchActivity.SERIES : SearchActivity.MOVIES));
                        dlg.dismiss();
                    }
                });
            }
        });
        dlg.show();
    }

    private class MainSeriesAdapter extends FragmentStatePagerAdapter {

        MainSeriesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return SeriesFragment.titles.length;
        }

        @Override
        public Fragment getItem(int i) {
            return SeriesFragment.create(i);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ".";
        }
    }

    private class MainMoviesAdapter extends FragmentStatePagerAdapter {

        MainMoviesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return MoviesFragment.titles.length;
        }

        @Override
        public Fragment getItem(int i) {
            return MoviesFragment.create(i);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ".";
        }
    }
}
