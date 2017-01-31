package net.ggelardi.uoccin;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;

import java.util.ArrayList;
import java.util.List;

public class SeasonFragment extends BaseSeriesFragment implements SwipeRefreshLayout.OnRefreshListener {

    public static SeasonFragment create(Series series, int season) {
        cached_series = series;
        Bundle args = new Bundle();
        args.putInt("seriesId", series.tvdb_id);
        args.putInt("season", season);
        SeasonFragment fragment = new SeasonFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private int season;
    private EpisodeAdapter adapter;

    private SwipeRefreshLayout lstContainer;
    private RecyclerView.LayoutManager lstLayout;
    private RecyclerView lstView;
    private int listPos;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        season = args.getInt("season", 0);
        listPos = args.getInt("listPos", 0);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_season, container, false);

        lstContainer = (SwipeRefreshLayout) view.findViewById(R.id.box_swiper);
        lstView = (RecyclerView) view.findViewById(R.id.recycler);

        lstContainer.setOnRefreshListener(this);
        lstLayout = new LinearLayoutManager(view.getContext()); //TODO: grid?
        lstView.setLayoutManager(lstLayout);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("season", season);
        RecyclerView.LayoutManager lm = lstView.getLayoutManager();
        if (lm instanceof LinearLayoutManager) //TODO: grid
            outState.putInt("listPos", ((LinearLayoutManager)lm).findFirstCompletelyVisibleItemPosition());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            season = savedInstanceState.getInt("season", season);
            listPos = savedInstanceState.getInt("listPos", 0);
        }
    }

    @Override
    public void onRefresh() {
        adapter.reload();
    }

    @Override
    protected void updateEpisode(Bundle data) {
        super.updateEpisode(data);

        if (data.getInt("series") == series.tvdb_id && data.getInt("seasonNo") == season &&
                !adapter.update(data.getInt("episode")))
            adapter.reload();
    }

    @Override
    protected void showData() {
        super.showData();

        if (adapter == null) {
            adapter = new EpisodeAdapter();
            lstView.setAdapter(adapter);
        }
        adapter.reload();
        if (listPos > 0) {
            RecyclerView.LayoutManager lm = lstView.getLayoutManager();
            if (lm instanceof LinearLayoutManager) //TODO: grid
                lm.scrollToPosition(listPos);
        }
    }

    private class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.EpisodeHolder> {

        private List<Episode> items;
        private int boxHeight = 1;

        void reload() {
            if (lstContainer != null)
                lstContainer.setRefreshing(true);
            List<Episode> tmp = new ArrayList<>();
            for (Episode ep: series.episodes)
                if (ep.season == season)
                    tmp.add(ep);
            items = tmp;
            notifyDataSetChanged();
            if (lstContainer != null)
                lstContainer.setRefreshing(false);
        }

        boolean update(int tvdb_id) {
            Episode chk;
            for (int i = 0; i < items.size(); i++) {
                chk = items.get(i);
                if (chk.tvdb_id == tvdb_id) {
                    chk.load();
                    notifyItemChanged(i);
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public EpisodeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new EpisodeHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_episode, parent, false));
        }

        @Override
        public void onBindViewHolder(EpisodeHolder vh, int position) {
            vh.doBind(items.get(position));
        }

        public class EpisodeHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
                View.OnLongClickListener {

            public Episode episode;
            public ImageView thumb;
            public TextView title;
            public TextView date;
            public TextView subs;

            public EpisodeHolder(View view) {
                super(view);

                thumb = (ImageView) view.findViewById(R.id.img_poster);
                title = (TextView) view.findViewById(R.id.txt_title);
                date = (TextView) view.findViewById(R.id.txt_date);
                subs = (TextView) view.findViewById(R.id.txt_subs);

                if (boxHeight <= 1) {
                    LinearLayout box = (LinearLayout) view.findViewById(R.id.box_content);
                    box.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    boxHeight = box.getMeasuredHeight();
                }
                thumb.setMinimumHeight(boxHeight);
                thumb.setMaxHeight(boxHeight);

                view.setOnClickListener(this);
                view.setOnLongClickListener(this);
            }
            
            public void doBind(Episode episode) {
                this.episode = episode;
                if (episode.isNew() || episode.isOld())
                    episode.refresh(true);

                title.setText(Integer.toString(episode.episode) + " - " + episode.name());

                if (episode.watched)
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_watched, 0, 0, 0);
                else if (episode.collected)
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_collected, 0, 0, 0);
                else if (episode.isToday())
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_today, 0, 0, 0);
                else if (episode.firstAired > System.currentTimeMillis())
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_calendar, 0, 0, 0);
                else if (episode.firstAired > 0)
                    title.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ics_action_missing, 0, 0, 0);
                else
                    title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

                date.setText(episode.firstAired());

                if (episode.subtitles.size() <= 0)
                    subs.setVisibility(View.GONE);
                else {
                    subs.setVisibility(View.VISIBLE);
                    subs.setText(episode.subtitles());
                }

                int tw;
                if (TextUtils.isEmpty(episode.thumb)) {
                    tw = Math.round((boxHeight * 384) / 216);
                    thumb.setMinimumWidth(tw);
                    thumb.setMaxWidth(tw);
                    Picasso.with(session.getContext()).load(R.drawable.series_fanart).noPlaceholder()
                            .resize(tw, boxHeight).into(thumb);
                } else {
                    tw = Math.round((boxHeight * episode.thumbWidth) / episode.thumbHeight);
                    thumb.setMinimumWidth(tw);
                    thumb.setMaxWidth(tw);
                    session.picasso(episode.thumb, R.drawable.ic_action_image).resize(tw, boxHeight)
                            .into(thumb);
                }
            }

            @Override
            public void onClick(View v) {
                EpisodeActivity.start(getContext(), series, episode);
            }

            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        }
    }
}
