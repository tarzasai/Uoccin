package net.ggelardi.uoccin;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import net.ggelardi.uoccin.api.TVDB;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.serv.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SeriesFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    static class Scopes {
        private static int SEARCH = -1;
        private static int WATCHLIST = 0;
        private static int NEWSERIES = 1;
        private static int UPCOMING = 2;
        private static int MISSING = 3;
        private static int COLLECTED = 4;
        private static int EVERYTHING = 5;
    }

    public static int[] titles = new int[] {
            R.string.maiact_page_serlist,
            R.string.maiact_page_sernews,
            R.string.maiact_page_sernext,
            R.string.maiact_page_sermiss,
            R.string.maiact_page_sercoll,
            R.string.maiact_page_serseen
    };

    public static int[] icons = new int[] {
            R.drawable.ic_pagtab_watchlist,
            R.drawable.ic_pagtab_news,
            R.drawable.ic_pagtab_future,
            R.drawable.ic_pagtab_missing,
            R.drawable.ic_pagtab_collected,
            R.drawable.ic_pagtab_series
    };

    private static String PK_LST = "LastSeriesSearchText";
    private static String PK_LSR = "LastSeriesSearchResults";

    public static SeriesFragment create(int scope) {
        Bundle args = new Bundle();
        args.putInt("scope", scope);
        SeriesFragment fragment = new SeriesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static SeriesFragment create(String search) {
        Bundle args = new Bundle();
        args.putInt("scope", Scopes.SEARCH);
        args.putString("search", search);
        SeriesFragment fragment = new SeriesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private int scope;
    private String query;
    private String search;
    private int listPos = 0;
    private int boxHeight = 1;
    private int posterWidth = 1;
    private SeriesAdapter adapter;
    private SwipeRefreshLayout lstContainer;
    private RecyclerView.LayoutManager lstLayout;
    private RecyclerView lstView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        scope = args.getInt("scope", Scopes.WATCHLIST);
        search = args.getString("search", null);

        String columns = TextUtils.join(", ", getResources().getStringArray(R.array.sql_series_columns));
        boolean specials = session.specials();
        StringBuilder sb = new StringBuilder();
        if (scope == Scopes.SEARCH) {
            query = "select * from series where name like '%" + search + "%' order by name";
        } else if (scope == Scopes.WATCHLIST) {
            sb.append("select ");
            sb.append(columns);
            sb.append(" from (select s.tvdb_id as series_id, (select e.tvdb_id from episode e where ");
            sb.append("e.series = s.tvdb_id and e.firstAired is not null ");
            if (!specials)
                sb.append("and not (e.season <= 0 or e.episode <= 0) ");
            sb.append("order by abs(strftime('%s', 'now') - (e.firstAired/1000)), ");
            sb.append("episode * (((strftime('%s', 'now') - (e.firstAired/1000))/1000)) desc limit 1) as episode_id ");
            sb.append("from series s where s.watchlist = 1) x ");
            sb.append("join series s on (s.tvdb_id = x.series_id) left join episode e on (e.tvdb_id = x.episode_id) ");
            sb.append("order by series_name");
            query = sb.toString();
        } else if (scope == Scopes.NEWSERIES) {
            sb.append("select ");
            sb.append(columns);
            sb.append(" from (select s.tvdb_id as series_id, (select e.tvdb_id from episode e where ");
            sb.append("e.series = s.tvdb_id and e.firstAired is not null ");
            if (!specials)
                sb.append("and not (e.season <= 0 or e.episode <= 0) ");
            sb.append("order by abs(strftime('%s', 'now') - (e.firstAired/1000)), ");
            sb.append("episode * (((strftime('%s', 'now') - (e.firstAired/1000))/1000)) desc limit 1) as episode_id ");
            sb.append("from series s where s.firstAired between strftime('%s','now','-15 day')*1000 and strftime('%s','now','+15 day')*1000 ");
            sb.append("and (s.watchlist = 1 or (select count(*) from episode c where c.series = s.tvdb_id and (c.collected = 1 or c.watched = 1)) > 0)) x ");
            sb.append("join series s on (s.tvdb_id = x.series_id) left join episode e on (e.tvdb_id = x.episode_id) ");
            sb.append("order by series_name");
            query = sb.toString();
        } else if (scope == Scopes.UPCOMING) {
            sb.append("select ");
            sb.append(columns);
            sb.append(" from (select s.tvdb_id as series_id, (select e.tvdb_id from episode e where e.series = s.tvdb_id ");
            if (!specials)
                sb.append("and not (e.season <= 0 or e.episode <= 0) ");
            sb.append("and datetime(e.firstAired/1000, 'unixepoch') between datetime('now') and ");
            sb.append("datetime('now', '+6 days')) as episode_id from series s where s.watchlist = 1 and episode_id is not null) x ");
            sb.append("join series s on (s.tvdb_id = x.series_id) join episode e on (e.tvdb_id = x.episode_id) ");
            sb.append("order by episode_firstAired, series_airsTime");
            query = sb.toString();
        } else if (scope == Scopes.MISSING) {
            sb.append("select ");
            sb.append(columns);
            sb.append(" from (select s.tvdb_id as series_id, (select e.tvdb_id from episode e where e.series = s.tvdb_id ");
            if (!specials)
                sb.append("and not (e.season <= 0 or e.episode <= 0) ");
            sb.append("and e.collected = 0 and e.watched = 0 and ");
            sb.append("datetime(e.firstAired/1000, 'unixepoch') <= datetime('now')) as episode_id from series s ");
            sb.append("where s.watchlist = 1) x join series s on (s.tvdb_id = x.series_id) ");
            sb.append("join episode e on (e.tvdb_id = x.episode_id) order by episode_firstAired");
            query = sb.toString();
        } else if (scope == Scopes.COLLECTED) {
            sb.append("select ");
            sb.append(columns);
            sb.append(" from (select s.tvdb_id as series_id, (select e.tvdb_id from episode e where e.series = s.tvdb_id ");
            sb.append("and e.collected = 1 and e.watched = 0 order by e.season, e.episode limit 1) as episode_id from series s) x ");
            sb.append("join series s on (s.tvdb_id = x.series_id) join episode e on (e.tvdb_id = x.episode_id)");
            sb.append("order by series_name");
            query = sb.toString();
        } else if (scope == Scopes.EVERYTHING) {
            sb.append("select ");
            sb.append(columns);
            sb.append(" from (select s.tvdb_id as series_id, (select e.tvdb_id from episode e where ");
            sb.append("e.series = s.tvdb_id and e.firstAired is not null ");
            if (!specials)
                sb.append("and not (e.season <= 0 or e.episode <= 0) ");
            sb.append("order by abs(strftime('%s', 'now') - (e.firstAired/1000)), ");
            sb.append("episode * (((strftime('%s', 'now') - (e.firstAired/1000))/1000)) desc limit 1) as episode_id ");
            sb.append("from series s where s.watchlist = 1 or (select count(*) from episode c where ");
            sb.append("c.series = s.tvdb_id and (c.collected = 1 or c.watched = 1)) > 0) x ");
            sb.append("join series s on (s.tvdb_id = x.series_id) left join episode e on (e.tvdb_id = x.episode_id) ");
            sb.append("order by series_name");
            query = sb.toString();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_series, container, false);

        lstContainer = (SwipeRefreshLayout) view.findViewById(R.id.box_swiper);
        lstContainer.setOnRefreshListener(this);

        lstLayout = new LinearLayoutManager(view.getContext()); //TODO: grid?
        lstView = (RecyclerView) view.findViewById(R.id.recycler);
        lstView.setLayoutManager(lstLayout);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (adapter == null) {
            adapter = new SeriesAdapter();
            lstView.setAdapter(adapter);
        }
        adapter.reload(false);

        if (listPos > 0) {
            RecyclerView.LayoutManager lm = lstView.getLayoutManager();
            if (lm instanceof LinearLayoutManager) //TODO: grid
                lm.scrollToPosition(listPos);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (adapter != null)
            adapter.cancel();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("scope", scope);
        outState.putString("search", search);
        RecyclerView.LayoutManager lm = lstView.getLayoutManager();
        if (lm instanceof LinearLayoutManager) //TODO: grid
            outState.putInt("listPos", ((LinearLayoutManager)lm).findFirstCompletelyVisibleItemPosition());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            scope = savedInstanceState.getInt("season", scope);
            search = savedInstanceState.getString("search", null);
            listPos = savedInstanceState.getInt("listPos", 0);
        }
    }

    @Override
    public void onRefresh() {
        adapter.reload(true);
    }

    @Override
    protected void updateSeries(Bundle data) {
        super.updateSeries(data);

        int seriesId = data.getInt("series");
        if (seriesId <= 0)
            adapter.reload(false);
        else {
            boolean metadata = data.getBoolean("metadata");
            Series series;
            for (int i = 0; i < adapter.getItemCount(); i++) {
                series = adapter.items.get(i);
                if (seriesId == series.tvdb_id) {
                    series.load(metadata);
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    private BaseSeriesHolder onCreateHolder(ViewGroup parent) {
        LayoutInflater li = LayoutInflater.from(parent.getContext());
        if (scope == Scopes.SEARCH)
            return new SeriesFindHolder(li.inflate(R.layout.item_serfind, parent, false));
        if (scope == Scopes.WATCHLIST)
            return new SeriesListHolder(li.inflate(R.layout.item_serlist, parent, false));
        if (scope == Scopes.NEWSERIES)
            return new SeriesListHolder(li.inflate(R.layout.item_serlist, parent, false));
        if (scope == Scopes.UPCOMING)
            return new SeriesNextHolder(li.inflate(R.layout.item_sernext, parent, false));
        if (scope == Scopes.MISSING)
            return new SeriesMissHolder(li.inflate(R.layout.item_sermiss, parent, false));
        if (scope == Scopes.COLLECTED)
            return new SeriesCollHolder(li.inflate(R.layout.item_sercoll, parent, false));
        if (scope == Scopes.EVERYTHING)
            return new SeriesListHolder(li.inflate(R.layout.item_serlist, parent, false));
        return null;
    }

    class SeriesAdapter extends RecyclerView.Adapter<BaseSeriesHolder> {

        AsyncTask<Void, Void, List<Series>> task;
        List<Series> items = new ArrayList<>();
        String lst;
        int lsr;

        SeriesAdapter() {
            SharedPreferences prefs = session.getPrefs();
            lst = prefs.getString(PK_LST, "");
            lsr = prefs.getInt(PK_LSR, 0);
        }

        void reload(final boolean forced) {
            if (task != null)
                return;
            task = new AsyncTask<Void, Void, List<Series>>() {
                @Override
                protected void onPreExecute() {
                    lstContainer.setRefreshing(true);
                }
                @Override
                protected List<Series> doInBackground(Void... params) {
                    List<Series> res = new ArrayList<>();
                    if (scope != Scopes.SEARCH || (!forced && search.equals(lst) && lsr > 0)) {
                        Cursor cr = Session.getInstance(getContext()).getDB().rawQuery(query, null);
                        try {
                            int ci = cr.getColumnIndex("series_id");
                            if (ci < 0)
                                ci = cr.getColumnIndex("tvdb_id");
                            while (cr.moveToNext()) {
                                //Commons.logCursor("Series", cr);
                                res.add(new Series(getContext(), cr.getInt(ci)).load(cr));
                            }
                        } finally {
                            cr.close();
                        }
                        Log.d(tag(), Integer.toString(res.size()) + " results for " + query);
                    }
                    if (res.isEmpty() && scope == Scopes.SEARCH) {
                        TVDB.TVDBResults results = null;
                        try {
                            results = new TVDB(getContext()).findSeries(search);
                        } catch (Exception err) {
                            Log.e(tag(), "doInBackground", err);
                            //TODO:?
                        }
                        if (results != null) {
                            for (TVDB.SeriesData sd : results.data)
                                res.add(new Series(getContext(), sd.id).load(true).update(sd, null, null, null)
                                        .save(true));
                            Collections.sort(res, new Comparator<Series>() {
                                @Override
                                public int compare(Series s1, Series s2) {
                                    return s1.name.toLowerCase(Locale.getDefault())
                                            .compareTo(s2.name.toLowerCase(Locale.getDefault()));
                                }
                            });
                        }
                        Log.d(tag(), Integer.toString(results.data.length) + " results for \"" + search + "\"");
                        lst = search;
                        lsr = res.size();
                        SharedPreferences.Editor editor = session.getPrefs().edit();
                        editor.putString(PK_LST, lst);
                        editor.putInt(PK_LSR, lsr);
                        editor.commit();
                    }
                    return res;
                }
                @Override
                protected void onPostExecute(List<Series> result) {
                    task = null;
                    items = result;
                    notifyDataSetChanged();
                    lstContainer.setRefreshing(false);
                }
            };
            task.execute();
        }

        void cancel() {
            if (task != null)
                task.cancel(true);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public BaseSeriesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return onCreateHolder(parent);
        }

        @Override
        public void onBindViewHolder(BaseSeriesHolder holder, int position) {
            holder.doBind(items.get(position));
        }
    }

    abstract class BaseSeriesHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        Series series;
        ImageView poster;
        LinearLayout sizebox;
        TextView sername;
        TextView airinfo;
        ImageView favorite;

        BaseSeriesHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);

            poster = (ImageView) itemView.findViewById(R.id.img_poster);
            sizebox = (LinearLayout) itemView.findViewById(R.id.box_content);
            sername = (TextView) itemView.findViewById(R.id.txt_is_sename);
            airinfo = (TextView) itemView.findViewById(R.id.txt_is_seinfo);
            favorite = (ImageView) itemView.findViewById(R.id.img_is_favorite);

            if (boxHeight <= 1) {
                sizebox.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                boxHeight = sizebox.getMeasuredHeight();
                posterWidth = Math.round((boxHeight * 340) / 500);
            }
            if (posterWidth > 1) {
                poster.setMinimumWidth(posterWidth);
                poster.setMaxWidth(posterWidth);
                poster.setMinimumHeight(boxHeight);
                poster.setMaxHeight(boxHeight);
            }
        }

        protected void doBind(Series series) {
            this.series = series;
            if (series.isNew() || series.isOld())
                series.refresh(true);

            sername.setText(series.name);
            sername.setCompoundDrawablesWithIntrinsicBounds(series.isRecent() ?
                    R.drawable.ics_action_news : 0, 0, 0, 0);

            airinfo.setText(series.airsTime());
            airinfo.setTextColor(ContextCompat.getColor(getContext(), series.isEnded() ?
                    android.R.color.holo_red_dark : R.color.textColorSecondary));

            favorite.setImageResource(series.watchlist ? R.drawable.ic_active_watchlist :
                    R.drawable.ic_action_watchlist);

            session.picasso(series.poster, true).resize(posterWidth, boxHeight).into(poster);
        }

        @Override
        public void onClick(View view) {
            SeriesActivity.start(getContext(), series);
        }

        @Override
        public boolean onLongClick(View view) {
            if (series.sampleEpisode == null)
                return false;
            EpisodeActivity.start(getContext(), series, series.sampleEpisode);
            return true;
        }
    }

    class SeriesListHolder extends BaseSeriesHolder {

        TextView seplot;
        TextView epname;
        TextView epdate;
        TextView setvrt;
        RatingBar semyrt;
        LinearLayout epinfo;

        SeriesListHolder(View itemView) {
            super(itemView);

            seplot = (TextView) itemView.findViewById(R.id.txt_is_seplot);
            epname = (TextView) itemView.findViewById(R.id.txt_is_epname);
            epdate = (TextView) itemView.findViewById(R.id.txt_is_epdate);
            setvrt = (TextView) itemView.findViewById(R.id.txt_is_setvrt);
            semyrt = (RatingBar) itemView.findViewById(R.id.rat_is_semyrt);
            epinfo = (LinearLayout) itemView.findViewById(R.id.box_is_epinfo);
        }

        @Override
        protected void doBind(Series series) {
            super.doBind(series);

            Episode ep = series.sampleEpisode;
            if (ep == null) {
                epinfo.setVisibility(View.GONE);
                seplot.setVisibility(View.VISIBLE);
                seplot.setText(series.plot());
            } else {
                seplot.setVisibility(View.GONE);
                epinfo.setVisibility(View.VISIBLE);
                epname.setText(ep.eid().readable() + " - " + ep.name());
                epname.setCompoundDrawablesWithIntrinsicBounds(ep.collected ?
                        R.drawable.ics_action_collected : 0, 0, 0, 0);
                epdate.setText(ep.firstAired());
                epdate.setCompoundDrawablesWithIntrinsicBounds(ep.isToday() ?
                        R.drawable.ics_action_calendar : 0, 0, 0, 0);
            }

            if (series.rating > 0) {
                setvrt.setVisibility(View.GONE);
                semyrt.setVisibility(View.VISIBLE);
                semyrt.setRating(series.rating);
            } else {
                semyrt.setVisibility(View.GONE);
                if (series.tvdbRating <= 0)
                    setvrt.setVisibility(View.GONE);
                else {
                    setvrt.setVisibility(View.VISIBLE);
                    setvrt.setText(String.format(getContext().getString(R.string.serfra_lbl_votes),
                            series.tvdbRating, series.tvdbVotes));
                }
            }
        }
    }

    class SeriesNextHolder extends BaseSeriesHolder {

        TextView epname;
        TextView epplot;
        TextView epdate;

        SeriesNextHolder(View itemView) {
            super(itemView);

            epname = (TextView) itemView.findViewById(R.id.txt_is_epname);
            epplot = (TextView) itemView.findViewById(R.id.txt_is_epplot);
            epdate = (TextView) itemView.findViewById(R.id.txt_is_epdate);
        }

        @Override
        protected void doBind(Series series) {
            super.doBind(series);

            Episode ep = series.sampleEpisode;
            epname.setText(ep.eid().readable() + " - " + ep.name());
            epname.setCompoundDrawablesWithIntrinsicBounds(ep.collected ? R.drawable.ics_action_collected : 0, 0, 0, 0);
            epplot.setText(ep.plot());
            epdate.setText(ep.firstAired());
            epdate.setCompoundDrawablesWithIntrinsicBounds(ep.isToday() ? R.drawable.ics_action_calendar : 0, 0, 0, 0);
        }
    }

    class SeriesMissHolder extends BaseSeriesHolder {

        TextView epname;
        TextView epplot;
        TextView epdate;

        SeriesMissHolder(View itemView) {
            super(itemView);

            epname = (TextView) itemView.findViewById(R.id.txt_is_epname);
            epplot = (TextView) itemView.findViewById(R.id.txt_is_epplot);
            epdate = (TextView) itemView.findViewById(R.id.txt_is_epdate);
        }

        @Override
        protected void doBind(Series series) {
            super.doBind(series);

            if (!series.isLoaded())
                series.load(true); // we need to know how many

            Episode ep = series.sampleEpisode;
            String s = ep.eid().readable();
            long n = series.missingEpisodes();
            if (n > 1)
                s += " (+" + Long.toString(n - 1) + ") ";
            s += " - " + ep.name();
            epname.setText(s);
            epname.setCompoundDrawablesWithIntrinsicBounds(ep.collected ? R.drawable.ics_action_collected : 0, 0, 0, 0);
            epplot.setText(ep.plot());
            epdate.setText(ep.firstAired());
            epdate.setCompoundDrawablesWithIntrinsicBounds(ep.isToday() ? R.drawable.ics_action_calendar : 0, 0, 0, 0);
        }
    }

    class SeriesCollHolder extends BaseSeriesHolder {

        TextView epname;
        TextView epplot;
        TextView epsubs;

        SeriesCollHolder(View itemView) {
            super(itemView);

            epname = (TextView) itemView.findViewById(R.id.txt_is_epname);
            epplot = (TextView) itemView.findViewById(R.id.txt_is_epplot);
            epsubs = (TextView) itemView.findViewById(R.id.txt_is_epsubs);
        }

        @Override
        protected void doBind(Series series) {
            super.doBind(series);

            if (!series.isLoaded())
                series.load(true); // we need to know how many

            Episode ep = series.sampleEpisode;
            String s = ep.eid().readable();
            long n = series.awaitingEpisodes();
            if (n > 1)
                s += " (+" + Long.toString(n - 1) + ") ";
            s += " - " + ep.name();
            epname.setText(s);
            epplot.setText(ep.plot());
            if (ep.subtitles.isEmpty())
                epsubs.setVisibility(View.GONE);
            else {
                epsubs.setVisibility(View.VISIBLE);
                epsubs.setText(TextUtils.join(", ", ep.subtitles));
            }
        }
    }

    class SeriesFindHolder extends BaseSeriesHolder {

        TextView seplot;
        TextView seyear;

        SeriesFindHolder(View itemView) {
            super(itemView);

            seplot = (TextView) itemView.findViewById(R.id.txt_is_seplot);
            seyear = (TextView) itemView.findViewById(R.id.txt_is_seyear);
        }

        @Override
        protected void doBind(Series series) {
            super.doBind(series);

            seplot.setText(series.plot());
            seyear.setText(series.year());
        }
    }
}
