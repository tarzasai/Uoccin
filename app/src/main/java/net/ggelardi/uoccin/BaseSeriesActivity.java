package net.ggelardi.uoccin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.data.Title;

public abstract class BaseSeriesActivity extends BaseActivity {

    protected static Series cached_series;

    protected int seriesId = 0;
    protected Series series;
    protected boolean updating = false;

    @Override
    protected void onResume() {
        super.onResume();

        showData();

        if (series.isNew() || series.isOld() || series.images.isEmpty()) {
            updating = true;
            series.refresh(true);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null)
            seriesId = savedInstanceState.getInt("seriesId");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("seriesId", seriesId);
    }

    @Override
    protected void updateSeries(Bundle data) {
        super.updateSeries(data);

        int sid = data.getInt("series");
        if (sid == 0 || sid == seriesId) {
            updating = false;
            boolean metadata = data.getBoolean("metadata");
            series.load(metadata);
            if (metadata)
                showMessage(R.string.seract_msg_updated);
            showData();
        }
    }

    protected void showData() {
        if (series == null) {
            if (seriesId <= 0)
                seriesId = getIntent().getIntExtra("seriesId", 0);
            if (cached_series != null && cached_series.tvdb_id == seriesId)
                series = cached_series;
            else {
                series = new Series(this, seriesId);
                cached_series = series;
            }
        }
        if (!series.isLoaded())
            series.load(true);
        setTitle(series.name());
    }
}
