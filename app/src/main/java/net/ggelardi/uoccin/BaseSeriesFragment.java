package net.ggelardi.uoccin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;

public abstract class BaseSeriesFragment extends BaseFragment {

    protected static Series cached_series;

    protected int seriesId = 0;
    protected Series series;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        seriesId = args.getInt("seriesId", 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        showData();

        if (series.isNew() || series.isOld() || series.images.isEmpty())
            series.refresh(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null)
            seriesId = savedInstanceState.getInt("seriesId");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("seriesId", seriesId);
    }

    @Override
    protected void updateSeries(Bundle data) {
        super.updateSeries(data);

        int seriesId = data.getInt("series");
        if (seriesId == 0 || seriesId == series.tvdb_id) {
            boolean metadata = data.getBoolean("metadata");
            series.load(metadata);
            showData();
        }
    }

    protected void showData() {
        if (series == null) {
            if (cached_series != null && cached_series.tvdb_id == seriesId)
                series = cached_series;
            else {
                series = new Series(getActivity(), seriesId);
                cached_series = series;
            }
            if (!series.isLoaded())
                series.load(true);
        }
    }
}
