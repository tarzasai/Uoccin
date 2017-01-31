package net.ggelardi.uoccin;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import net.ggelardi.uoccin.data.Series;

import java.util.ArrayList;
import java.util.List;

public class SeasonActivity extends BaseSeriesActivity {

    public static void start(Context context, Series series, int season) {
        cached_series = series;
        context.startActivity(new Intent(context, SeasonActivity.class)
                .putExtra("seriesId", series.tvdb_id)
                .putExtra("season", season)
        );
    }

    private int seasonNo = 0;
    private SeasonAdapter adapter;

    private TabLayout box_tabs;
    private ViewPager pag_view;
    private BottomSheetDialog bsd_menu;
    private BottomSheetBehavior bsb_menu;
    private TextView btn_seen;
    private TextView btn_coll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_season);
        setUoccinActionBar(findViewById(R.id.toolbar));

        box_tabs = (TabLayout) findViewById(R.id.tab_layout);
        pag_view = (ViewPager) findViewById(R.id.pager_view);
        FloatingActionButton fab_menu = (FloatingActionButton) findViewById(R.id.fab_menu);

        View bsv = getLayoutInflater().inflate(R.layout.dialog_season_menu, null);
        bsd_menu = new BottomSheetDialog(SeasonActivity.this);
        bsd_menu.setContentView(bsv);
        bsd_menu.setCancelable(true);
        bsd_menu.setCanceledOnTouchOutside(true);
        bsb_menu = BottomSheetBehavior.from((View) bsv.getParent());

        btn_seen = (TextView) bsv.findViewById(R.id.btn_seen);
        btn_coll = (TextView) bsv.findViewById(R.id.btn_coll);

        pag_view.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //
            }
            @Override
            public void onPageSelected(int position) {
                seasonNo = adapter.seasons.get(position);
                highlightTabs();
            }
            @Override
            public void onPageScrollStateChanged(int state) {
                //
            }
        });

        fab_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int sn = adapter.seasons.get(pag_view.getCurrentItem());
                long aired = series.airedEpisodes(sn);
                btn_coll.setText(series.collectedEpisodes(sn) < aired ? R.string.seaact_btn_coll1 : R.string.seaact_btn_coll0);
                btn_seen.setText(series.watchedEpisodes(sn) < aired ? R.string.seaact_btn_seen1 : R.string.seaact_btn_seen0);
                //
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
                int sn = adapter.seasons.get(pag_view.getCurrentItem());
                boolean watched = series.watchedEpisodes(sn) < series.airedEpisodes(sn);
                series.setWatched(watched, sn);
                showMessage(watched ? R.string.seaact_msg_seen1 : R.string.seaact_msg_seen0);
            }
        });

        btn_coll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bsd_menu.dismiss();
                int sn = adapter.seasons.get(pag_view.getCurrentItem());
                boolean collected = series.collectedEpisodes(sn) < series.airedEpisodes(sn);
                series.setCollected(collected, sn);
                showMessage(collected ? R.string.seaact_msg_coll1 : R.string.seaact_msg_coll0);
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null)
            seasonNo = savedInstanceState.getInt("season");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("season", seasonNo);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void showData() {
        super.showData();

        if (adapter == null) {
            if (seasonNo <= 0)
                seasonNo = getIntent().getIntExtra("season", 0);
            adapter = new SeasonAdapter(getSupportFragmentManager());
            pag_view.setAdapter(adapter);
            box_tabs.setupWithViewPager(pag_view);
        }
        adapter.reload();
        TabLayout.Tab tl;
        for (int i = 0; i < box_tabs.getTabCount(); i++) {
            tl = box_tabs.getTabAt(i);
            tl.setCustomView(R.layout.item_tab);
            updateTab(adapter.seasons.get(i), tl);
        }
        int pos = adapter.seasons.indexOf(seasonNo);
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

    private void updateTab(int sn, TabLayout.Tab tl) {
        TextView tv = (TextView) tl.getCustomView();
        long aired = series.airedEpisodes(sn);
        if (aired <= 0)
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
        else if (series.watchedEpisodes(sn) >= aired)
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_watched, 0, 0, 0);
        else if (series.collectedEpisodes(sn) >= aired)
            tv.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_pagtab_collected, 0, 0, 0);
    }

    private class SeasonAdapter extends FragmentStatePagerAdapter {

        List<Integer> seasons = new ArrayList<>();

        SeasonAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return seasons.size();
        }

        @Override
        public Fragment getItem(int position) {
            return SeasonFragment.create(series, seasons.get(position));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return Integer.toString(seasons.get(position));
        }

        void reload() {
            seasons = series.episodes.getSeasons();
            notifyDataSetChanged();
        }
    }
}
