package net.ggelardi.uoccin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;

public abstract class BaseFragment extends Fragment {

    protected Session session;
    protected BroadcastReceiver updates;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(tag(), "onCreate");

        session = Session.getInstance(getActivity());
        updates = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == null)
                    Log.w(tag(), "updates.onReceive: NULL action!?!");
                else if (intent.getAction().equals(Commons.UE.MOVIE))
                    updateMovie(intent.getExtras());
                else if (intent.getAction().equals(Commons.UE.SERIES))
                    updateSeries(intent.getExtras());
                else if (intent.getAction().equals(Commons.UE.EPISODE))
                    updateEpisode(intent.getExtras());
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(tag(), "onResume");

        IntentFilter updflt = new IntentFilter();
        updflt.addAction(Commons.UE.MOVIE);
        updflt.addAction(Commons.UE.SERIES);
        updflt.addAction(Commons.UE.EPISODE);
        LocalBroadcastManager.getInstance(session.getContext()).registerReceiver(updates, updflt);
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(tag(), "onPause");

        LocalBroadcastManager.getInstance(session.getContext()).unregisterReceiver(updates);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.i(tag(), "onSaveInstanceState");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Log.i(tag(), "onActivityCreated");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        Log.i(tag(), "onCreateOptionsMenu");
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Log.i(tag(), "onPrepareOptionsMenu");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.i(tag(), "onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.i(tag(), "onDetach");
    }

    protected String tag() {
        return this.getClass().getSimpleName();
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
}
