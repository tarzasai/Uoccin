package net.ggelardi.uoccin.serv;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.data.Series;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

public class EpisodeTask extends AsyncTask<String, Void, List<Episode>> {
	public static final String QUERY = "EpisodeTask.QUERY";
	public static final String LIST = "EpisodeTask.LIST";
	
	private final EpisodeTaskContainer container;
	private final String type;
	
	public EpisodeTask(EpisodeTaskContainer container, String type) {
		super();

		this.container = container;
		this.type = type;
	}
	
    @Override
    protected void onPreExecute() {
    	container.preExecuteTask();
	}
	
    @Override
	protected List<Episode> doInBackground(String... params) {
    	List<Episode> res;
    	if (type.equals(LIST)) {
    		res = Series.get(container.getContext(), params[0]).episodes(Integer.parseInt(params[1]));
    	} else {
    		String query = params[0];
    		String[] args = new String[params.length - 1];
    		System.arraycopy(params, 1, args, 0, params.length - 1);
    		res = new ArrayList<Episode>();
			Cursor cr = Session.getInstance(container.getContext()).getDB().rawQuery(query, args);
	    	try {
	    		int c1 = cr.getColumnIndex("series");
	    		int c2 = cr.getColumnIndex("season");
	    		int c3 = cr.getColumnIndex("episode");
	    		while (cr.moveToNext())
	    			res.add(Episode.get(container.getContext(), cr.getString(c1), cr.getInt(c2), cr.getInt(c3)));
	    	} finally {
	    		cr.close();
	    	}
    	}
		return res;
	}
    
    @Override
    protected void onPostExecute(List<Episode> result) {
    	container.postExecuteTask(result);
	}
    
    public interface EpisodeTaskContainer {
    	public Context getContext();
    	public void preExecuteTask();
    	public void postExecuteTask(List<Episode> result);
    }
}