package net.ggelardi.uoccin;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.List;
import java.util.Locale;

public class SearchActivity extends BaseActivity {
    public static String SERIES = "series";
    public static String MOVIES = "movies";

    private String type = null;
    private String text = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        setUoccinActionBar(findViewById(R.id.toolbar));

        if (savedInstanceState != null) {
            type = savedInstanceState.getString("type");
            text = savedInstanceState.getString("text");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (type == null) {
            type = getIntent().getStringExtra("type");
            text = getIntent().getStringExtra("text");
        }
        setTitle(String.format(Locale.getDefault(), getString(R.string.finact_title), text));

        FragmentManager fragman = getSupportFragmentManager();
        if (fragman.getFragments() == null) {
            BaseFragment vf = null;
            if (type.equals(SERIES))
                vf = SeriesFragment.create(text);
            else if (type.equals(MOVIES))
                vf = MoviesFragment.create(text);
            fragman.beginTransaction().replace(R.id.container, vf, type).commit();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            type = savedInstanceState.getString("type");
            text = savedInstanceState.getString("text");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString("type", type);
        outState.putString("text", text);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return false;
    }
}
