package net.ggelardi.uoccin;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;

public class EpisodeFragment extends BaseSeriesFragment {

    public static EpisodeFragment create(Series series, Episode episode) {
        cached_series = series;
        Bundle args = new Bundle();
        args.putInt("seriesId", series.tvdb_id);
        args.putInt("episodeId", episode.tvdb_id);
        EpisodeFragment fragment = new EpisodeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private int episodeId;
    private Episode episode;
    private int pstHeight;
    private int pstWidth;

    private ImageView img_thmb;
    private TextView txt_name;
    private TextView txt_date;
    private TextView txt_plot;
    private TextView txt_subs;
    private LinearLayoutCompat box_gues;
    private TextView txt_gues;
    private LinearLayoutCompat box_writ;
    private TextView txt_writ;
    private LinearLayoutCompat box_dire;
    private TextView txt_dire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        episodeId = args.getInt("episodeId", 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_episode, container, false);

        img_thmb = (ImageView) view.findViewById(R.id.img_thumb);
        txt_name = (TextView) view.findViewById(R.id.txt_name);
        txt_date = (TextView) view.findViewById(R.id.txt_date);
        txt_plot = (TextView) view.findViewById(R.id.txt_plot);
        txt_subs = (TextView) view.findViewById(R.id.txt_subs);
        box_gues = (LinearLayoutCompat) view.findViewById(R.id.box_gues);
        txt_gues = (TextView) view.findViewById(R.id.txt_guests);
        box_writ = (LinearLayoutCompat) view.findViewById(R.id.box_writ);
        txt_writ = (TextView) view.findViewById(R.id.txt_writers);
        box_dire = (LinearLayoutCompat) view.findViewById(R.id.box_dire);
        txt_dire = (TextView) view.findViewById(R.id.txt_director);

        return view;
    }

    @Override
    public void onResume() {
        img_thmb.measure(img_thmb.getMeasuredWidth(), img_thmb.getMeasuredHeight());
        pstWidth = img_thmb.getMeasuredWidth();

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("episodeId", episode.tvdb_id);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null)
            episodeId = savedInstanceState.getInt("episodeId");
    }

    @Override
    protected void updateEpisode(Bundle data) {
        super.updateEpisode(data);

        if (data.getInt("episode") == episode.tvdb_id)
            showData();
    }

    @Override
    protected void showData() {
        super.showData();

        if (episode == null)
            episode = series.episodes.getItem(episodeId);
        episode.load();
        if (episode.isNew() || episode.isOld())
            episode.refresh(true);

        if (!episode.hasThumb())
            img_thmb.setVisibility(View.GONE);
        else {
            img_thmb.setVisibility(View.VISIBLE);
            pstHeight = Math.round((pstWidth * episode.thumbHeight) / episode.thumbWidth);
            session.picasso(episode.thumb, R.drawable.series_fanart).resize(pstWidth, pstHeight).into(img_thmb);
        }
        txt_name.setText(episode.name());
        txt_plot.setText(episode.plot());
        if (episode.firstAired <= 0) {
            txt_date.setVisibility(View.GONE);
            txt_subs.setVisibility(View.GONE);
        } else {
            txt_date.setVisibility(View.VISIBLE);
            txt_date.setText(DateUtils.getRelativeTimeSpanString(episode.firstAired, System.currentTimeMillis(),
                    episode.isToday() ? DateUtils.HOUR_IN_MILLIS : DateUtils.DAY_IN_MILLIS));
            if (episode.subtitles.size() <= 0)
                txt_subs.setVisibility(View.GONE);
            else {
                txt_subs.setVisibility(View.VISIBLE);
                txt_subs.setText(episode.subtitles());
                txt_subs.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ics_action_subtitles, 0, 0, 0);
            }
        }
        if (TextUtils.isEmpty(episode.guests))
            box_gues.setVisibility(View.GONE);
        else {
            txt_gues.setText(episode.guests);
            box_gues.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(episode.writers))
            box_writ.setVisibility(View.GONE);
        else {
            txt_writ.setText(episode.writers);
            box_writ.setVisibility(View.VISIBLE);
        }
        if (TextUtils.isEmpty(episode.director))
            box_dire.setVisibility(View.GONE);
        else {
            txt_dire.setText(episode.director);
            box_dire.setVisibility(View.VISIBLE);
        }
    }
}
