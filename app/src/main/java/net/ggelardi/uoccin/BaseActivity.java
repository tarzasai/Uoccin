package net.ggelardi.uoccin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity {

    private BroadcastReceiver updReceiver;
    private IntentFilter updFilter;
    private List<ShowcaseStep> showcaseSteps = new ArrayList<>();

    protected Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(tag(), "onCreate");

        updReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == null)
                    Log.w(tag(), "updReceiver.onReceive: NULL action!?!");
                else if (intent.getAction().equals(Commons.UE.MOVIE))
                    updateMovie(intent.getExtras());
                else if (intent.getAction().equals(Commons.UE.SERIES))
                    updateSeries(intent.getExtras());
                else if (intent.getAction().equals(Commons.UE.EPISODE))
                    updateEpisode(intent.getExtras());
            }
        };
        updFilter = new IntentFilter();
        updFilter.addAction(Commons.UE.MOVIE);
        updFilter.addAction(Commons.UE.SERIES);
        updFilter.addAction(Commons.UE.EPISODE);

        session = Session.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.i(tag(), "onDestroy");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.i(tag(), "onNewIntent: " + intent.toString());

        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(tag(), "onResume");

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(updReceiver, updFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.i(tag(), "onPause");

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(updReceiver);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        Log.i(tag(), "onRestoreInstanceState");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.i(tag(), "onSaveInstanceState");
    }

    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    protected String tag() {
        return this.getClass().getSimpleName();
    }

    protected void setUoccinActionBar(View toolbar) {
        setSupportActionBar((Toolbar) toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void updateMovie(Bundle data) {
        Log.i(tag(), "updateMovie: " + data.toString());
    }

    protected void updateSeries(Bundle data) {
        Log.i(tag(), "updateSeries: " + data.toString());
    }

    protected void updateEpisode(Bundle data) {
        Log.i(tag(), "updateEpisode: " + data.toString());
    }

    protected void showMessage(String text) {
        Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show();
    }

    protected void showMessage(int stringId) {
        Snackbar.make(findViewById(android.R.id.content), stringId, Snackbar.LENGTH_SHORT).show();
    }

    protected ViewTarget getNavigationButtonViewTarget() {
        try {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            Field field = Toolbar.class.getDeclaredField("mNavButtonView");
            field.setAccessible(true);
            View navigationView = (View) field.get(toolbar);
            return new ViewTarget(navigationView);
        } catch (Exception err) {
            Log.e(tag(), "navigationButtonViewTarget", err);
            return null;
        }
    }

    protected void addShowcase(Target target, int titleId, int textId) {
        if (target == null) {
            Log.i(tag(), "addShowcase: NULL target!");
            return;
        }
        ShowcaseStep step = new ShowcaseStep();
        step.target = target;
        step.title = titleId;
        step.text = textId;
        showcaseSteps.add(step);
    }

    protected void runShowcase() {
        List<ShowcaseStep> scl = new ArrayList<>();
        scl.addAll(showcaseSteps);
        _runShowcase(scl);
    }

    private void _runShowcase(final List<ShowcaseStep> scl) {
        if (scl.isEmpty())
            return;
        ShowcaseStep step = scl.remove(0);
        new ShowcaseView.Builder(this)
                .withMaterialShowcase()
                .setStyle(R.style.CustomShowcaseTheme)
                .setTarget(step.target)
                .setContentTitle(step.title)
                .setContentText(step.text)
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        _runShowcase(scl);
                    }
                })
                .build();
    }

    class ShowcaseStep {
        Target target;
        int title;
        int text;
    }

    public class ToolbarActionItemTarget implements Target {

        private final Toolbar toolbar;
        private final int menuItemId;

        public ToolbarActionItemTarget(Toolbar toolbar, @IdRes int itemId) {
            this.toolbar = toolbar;
            this.menuItemId = itemId;
        }

        @Override
        public Point getPoint() {
            return new ViewTarget(toolbar.findViewById(menuItemId)).getPoint();
        }

    }
}
