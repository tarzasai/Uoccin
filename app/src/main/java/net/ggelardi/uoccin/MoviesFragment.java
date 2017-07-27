package net.ggelardi.uoccin;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import net.ggelardi.uoccin.api.TMDB;
import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.serv.Session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MoviesFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener {

    static class Scopes {
        private static int SEARCH = -1;
        private static int WATCHLIST = 0;
        private static int COLLECTED = 1;
        private static int EVERYTHING = 2;
    }

    public static int[] titles = new int[] {
            R.string.maiact_page_movlist,
            R.string.maiact_page_movcoll,
            R.string.maiact_page_movseen
    };

    public static int[] icons = new int[] {
            R.drawable.ic_pagtab_watchlist,
            R.drawable.ic_pagtab_collected,
            R.drawable.ic_pagtab_movies
    };

    private static String PK_LST = "LastMoviesSearchText";
    private static String PK_LSR = "LastMoviesSearchResults";

    public static MoviesFragment create(int scope) {
        Bundle args = new Bundle();
        args.putInt("scope", scope);
        MoviesFragment fragment = new MoviesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static MoviesFragment create(String search) {
        Bundle args = new Bundle();
        args.putInt("scope", Scopes.SEARCH);
        args.putString("search", search);
        MoviesFragment fragment = new MoviesFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private int scope;
    private String query;
    private String search;
    private int listPos = 0;
    private int boxHeight = 1;
    private int posterWidth = 1;
    private MoviesAdapter adapter;
    private SwipeRefreshLayout lstContainer;
    private RecyclerView.LayoutManager lstLayout;
    private RecyclerView lstView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        scope = args.getInt("scope", Scopes.WATCHLIST);
        search = args.getString("search", null);

        if (scope == Scopes.SEARCH)
            query = "select * from movie where name like '%" + search + "%' order by name";
        else if (scope == Scopes.WATCHLIST)
            query = "select * from movie where watchlist = 1 order by name";
        else if (scope == Scopes.COLLECTED)
            query = "select * from movie where collected = 1 and watched = 0 order by name";
        else if (scope == Scopes.EVERYTHING)
            query = "select * from movie where collected = 1 or watched = 1 order by name";
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
            adapter = new MoviesAdapter();
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
    protected void updateMovie(Bundle data) {
        super.updateMovie(data);

        String movieId = data.getString("movie");
        if (movieId == null)
            adapter.reload(false);
        else {
            //boolean metadata = data.getBoolean("metadata");
            Movie movie;
            for (int i = 0; i < adapter.getItemCount(); i++) {
                movie = adapter.items.get(i);
                if (movieId == movie.imdb_id) {
                    movie.load();
                    adapter.notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    class MoviesAdapter extends RecyclerView.Adapter<BaseMovieHolder> {

        AsyncTask<Void, Void, List<Movie>> task;
        List<Movie> items = new ArrayList<>();
        String lst;
        int lsr;

        MoviesAdapter() {
            SharedPreferences prefs = session.getPrefs();
            lst = prefs.getString(PK_LST, "");
            lsr = prefs.getInt(PK_LSR, 0);
        }

        void reload(final boolean forced) {
            if (task != null)
                return;
            task = new AsyncTask<Void, Void, List<Movie>>() {
                @Override
                protected void onPreExecute() {
                    lstContainer.setRefreshing(true);
                }
                @Override
                protected List<Movie> doInBackground(Void... params) {
                    List<Movie> res = new ArrayList<>();
                    if (scope != Scopes.SEARCH || (!forced && search.equals(lst) && lsr > 0)) {
                        Cursor cr = Session.getInstance(getContext()).getDB().rawQuery(query, null);
                        try {
                            int ci = cr.getColumnIndex("imdb_id");
                            while (cr.moveToNext()) {
                                //Commons.logCursor("Movie", cr);
                                res.add(new Movie(getContext(), cr.getString(ci)).load(cr));
                            }
                        } finally {
                            cr.close();
                        }
                        Log.d(tag(), Integer.toString(res.size()) + " results for " + query);
                    }
                    if (res.isEmpty() && scope == Scopes.SEARCH) {
                        TMDB.TMDBMovies results = null;
                        try {
                            results = new TMDB(getContext()).findMovie(search);
                        } catch (Exception err) {
                            Log.e(tag(), "doInBackground", err);
                            //TODO:?
                        }
                        if (results != null) {
                            for (TMDB.MovieData md : results.results)
                                res.add(new Movie(getContext(), md.id).load().update(md, null).save(true));
                            Collections.sort(res, new Comparator<Movie>() {
                                @Override
                                public int compare(Movie m1, Movie m2) {
                                    return m1.name.toLowerCase(Locale.getDefault())
                                            .compareTo(m2.name.toLowerCase(Locale.getDefault()));
                                }
                            });
                        }
                        Log.d(tag(), Integer.toString(results.results.length) + " results for \"" + search + "\"");
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
                protected void onPostExecute(List<Movie> result) {
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
        public BaseMovieHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return onCreateHolder(parent);
        }

        @Override
        public void onBindViewHolder(BaseMovieHolder holder, int position) {
            holder.doBind(items.get(position));
        }
    }

    private BaseMovieHolder onCreateHolder(ViewGroup parent) {
        LayoutInflater li = LayoutInflater.from(parent.getContext());
        if (scope == Scopes.SEARCH)
            return new MovieListHolder(li.inflate(R.layout.item_movlist, parent, false));
        if (scope == Scopes.WATCHLIST)
            return new MovieListHolder(li.inflate(R.layout.item_movlist, parent, false));
        if (scope == Scopes.COLLECTED)
            return new MovieCollHolder(li.inflate(R.layout.item_movcoll, parent, false));
        if (scope == Scopes.EVERYTHING)
            return new MovieSeenHolder(li.inflate(R.layout.item_movseen, parent, false));
        return null;
    }

    abstract class BaseMovieHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Movie movie;
        ImageView poster;
        LinearLayout sizebox;
        TextView movname;
        TextView movcast;
        TextView movyear;
        ImageView movstat;

        BaseMovieHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            poster = (ImageView) itemView.findViewById(R.id.img_poster);
            sizebox = (LinearLayout) itemView.findViewById(R.id.box_content);
            movname = (TextView) itemView.findViewById(R.id.txt_title);
            movcast = (TextView) itemView.findViewById(R.id.txt_cast);
            movyear = (TextView) itemView.findViewById(R.id.txt_year);
            movstat = (ImageView) itemView.findViewById(R.id.img_status);

            if (boxHeight <= 1) {
                sizebox.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                boxHeight = sizebox.getMeasuredHeight();
                posterWidth = Math.round((boxHeight * 300) / 450);
            }
            if (posterWidth > 1) {
                poster.setMinimumWidth(posterWidth);
                poster.setMaxWidth(posterWidth);
                poster.setMinimumHeight(boxHeight);
                poster.setMaxHeight(boxHeight);
            }
        }

        protected void doBind(Movie movie) {
            this.movie = movie;
            if (movie.isNew() || movie.isOld())
                movie.refresh(true);

            movname.setText(movie.name);
            movcast.setText(movie.actors());
            movyear.setText(movie.year());
            if (movie.watchlist) {
                movstat.setImageResource(R.drawable.ic_active_watchlist);
                movstat.setVisibility(View.VISIBLE);
            } else if (movie.watched) {
                movstat.setImageResource(R.drawable.ic_active_seen);
                movstat.setVisibility(View.VISIBLE);
            } else if (movie.collected) {
                movstat.setImageResource(R.drawable.ic_active_storage);
                movstat.setVisibility(View.VISIBLE);
            } else
                movstat.setVisibility(View.GONE);

            session.picasso(movie.poster, true).resize(posterWidth, boxHeight).into(poster);
        }

        @Override
        public void onClick(View view) {
            MovieActivity.start(getContext(), movie);
        }
    }

    class MovieListHolder extends BaseMovieHolder {

        TextView movrams;

        MovieListHolder(View itemView) {
            super(itemView);

            movrams = (TextView) itemView.findViewById(R.id.txt_ratms);
        }

        @Override
        protected void doBind(Movie movie) {
            super.doBind(movie);

            if (movie.tmdbRating <= 0)
                movrams.setVisibility(View.GONE);
            else {
                movrams.setVisibility(View.VISIBLE);
                movrams.setText(String.format(getContext().getString(R.string.serfra_lbl_votes),
                        movie.tmdbRating, movie.tmdbVotes));
            }
        }
    }

    class MovieCollHolder extends BaseMovieHolder {

        TextView movsubs;

        MovieCollHolder(View itemView) {
            super(itemView);

            movsubs = (TextView) itemView.findViewById(R.id.txt_subs);
        }

        @Override
        protected void doBind(Movie movie) {
            super.doBind(movie);

            if (movie.subtitles.size() <= 0)
                movsubs.setVisibility(View.GONE);
            else {
                movsubs.setVisibility(View.VISIBLE);
                movsubs.setText(movie.subtitles());
            }
        }
    }

    class MovieSeenHolder extends BaseMovieHolder {

        RatingBar movmyrt;

        MovieSeenHolder(View itemView) {
            super(itemView);

            movmyrt = (RatingBar) itemView.findViewById(R.id.rat_ratme);
        }

        @Override
        protected void doBind(Movie movie) {
            super.doBind(movie);

            if (movie.rating <= 0)
                movmyrt.setVisibility(View.GONE);
            else {
                movmyrt.setVisibility(View.VISIBLE);
                movmyrt.setRating(movie.rating);
            }
        }
    }
}
