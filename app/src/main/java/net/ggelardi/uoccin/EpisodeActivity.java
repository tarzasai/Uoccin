package net.ggelardi.uoccin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;

public class EpisodeActivity extends BaseSeriesActivity {

    public static void start(Context context, Series series, Episode episode) {
        cached_series = series;
        context.startActivity(new Intent(context, EpisodeActivity.class)
                .putExtra("seriesId", series.tvdb_id)
                .putExtra("episodeId", episode.tvdb_id)
        );
    }

    private int episodeId;
    private EpisodeAdapter adapter;

    private TabLayout box_tabs;
    private ViewPager pag_view;
    private BottomSheetDialog bsd_menu;
    private BottomSheetBehavior bsb_menu;
    private TextView btn_coll;
    private TextView btn_seen;
    private TextView btn_imdb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode);
        setUoccinActionBar(findViewById(R.id.toolbar));

        box_tabs = (TabLayout) findViewById(R.id.tab_layout);
        pag_view = (ViewPager) findViewById(R.id.pager_view);
        btn_imdb = (TextView) findViewById(R.id.btn_imdb);

        View bsv = getLayoutInflater().inflate(R.layout.dialog_episode_menu, null);
        bsd_menu = new BottomSheetDialog(EpisodeActivity.this);
        bsd_menu.setContentView(bsv);
        bsd_menu.setCancelable(true);
        bsd_menu.setCanceledOnTouchOutside(true);
        bsb_menu = BottomSheetBehavior.from((View) bsv.getParent());

        btn_coll = (TextView) bsv.findViewById(R.id.btn_coll);
        btn_seen = (TextView) bsv.findViewById(R.id.btn_seen);
        btn_imdb = (TextView) bsv.findViewById(R.id.btn_imdb);

        pag_view.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //
            }
            @Override
            public void onPageSelected(int position) {
                Episode ep = series.episodes.get(position);
                episodeId = ep.tvdb_id;
                btn_coll.setText(ep.collected ? R.string.epiact_btn_coll0 : R.string.epiact_btn_coll1);
                btn_seen.setText(ep.watched ? R.string.epiact_btn_seen0 : R.string.epiact_btn_seen1);
                btn_imdb.setEnabled(!TextUtils.isEmpty(ep.imdb_id));
                btn_imdb.setAlpha((float) (btn_imdb.isEnabled() ? 1 : 0.3));
                highlightTabs();
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                //
            }
        });

        FloatingActionButton fab_menu = (FloatingActionButton) findViewById(R.id.fab_menu);
        fab_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsb_menu.setState(BottomSheetBehavior.STATE_EXPANDED);
                bsd_menu.show();
            }
        });

        bsd_menu.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                bsb_menu.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        btn_seen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                Episode ep = series.episodes.get(pag_view.getCurrentItem());
                ep.setWatched(!ep.watched);
                showMessage(ep.watched ? R.string.epiact_msg_seen1 : R.string.epiact_msg_seen0);
            }
        });

        btn_coll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                Episode ep = series.episodes.get(pag_view.getCurrentItem());
                ep.setCollected(!ep.collected);
                showMessage(ep.collected ? R.string.epiact_msg_coll1 : R.string.epiact_msg_coll0);
            }
        });

        TextView btn_updt = (TextView) bsv.findViewById(R.id.btn_updt);
        btn_updt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                showMessage(R.string.epiact_msg_updating);
                series.episodes.get(pag_view.getCurrentItem()).refresh(true);
            }
        });

        TextView btn_tvdb = (TextView) bsv.findViewById(R.id.btn_tvdb);
        btn_tvdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsd_menu.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(series.episodes.get(pag_view.getCurrentItem()).tvdbUrl())));
            }
        });

        btn_imdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsd_menu.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(series.episodes.get(pag_view.getCurrentItem()).imdbUrl())));
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null)
            episodeId = savedInstanceState.getInt("episodeId");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("episodeId", episodeId);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_episode, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    @Override
    protected void updateEpisode(Bundle data) {
        super.updateEpisode(data);

        if (data.getInt("series") == series.tvdb_id) {
            Episode episode = series.episodes.getItem(data.getInt("episode"));
            updateTab(episode, box_tabs.getTabAt(series.episodes.indexOf(episode)));
        }
    }

    @Override
    protected void showData() {
        super.showData();

        if (adapter == null) {
            if (episodeId <= 0)
                episodeId = getIntent().getIntExtra("episodeId", 0);
            adapter = new EpisodeAdapter(getSupportFragmentManager());
            pag_view.setAdapter(adapter);
            box_tabs.setupWithViewPager(pag_view);
        }
        adapter.notifyDataSetChanged();
        TabLayout.Tab tl;
        for (int i = 0; i < box_tabs.getTabCount(); i++) {
            tl = box_tabs.getTabAt(i);
            tl.setCustomView(R.layout.item_tab);
            updateTab(series.episodes.get(i), tl);
        }
        int pos = series.episodes.getItemIndex(episodeId);
        if (pos >= 0)
            pag_view.setCurrentItem(pos);
        highlightTabs();
    }

    private void highlightTabs() {
        TextView tv;
        for (int i = 0; i < box_tabs.getTabCount(); i++) {
            tv = (TextView) (box_tabs.getTabAt(i)).getCustomView();
            tv.setAlpha(i == pag_view.getCurrentItem() ? 1 : (float) 0.5);
        }
    }

    private void updateTab(Episode ep, TabLayout.Tab tl) {
        TextView tv = (TextView) tl.getCustomView();
        if (ep.watched)
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_watched, 0, 0, 0);
        else if (ep.collected)
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_collected, 0, 0, 0);
        else if (ep.isToday())
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_today, 0, 0, 0);
        else if (ep.firstAired > System.currentTimeMillis())
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_future, 0, 0, 0);
        else if (ep.firstAired > 0)
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_missing, 0, 0, 0);
    }

    private class EpisodeAdapter extends FragmentStatePagerAdapter {

        EpisodeAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return series.episodes.size();
        }

        @Override
        public Fragment getItem(int position) {
            Episode ep = series.episodes.get(position);
            return EpisodeFragment.create(series, ep);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return series.episodes.get(position).eid().readable();
        }
    }
}
