package net.ggelardi.uoccin.serv;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.data.Series;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;

public class SeriesTask extends AsyncTask<String, Void, List<Series>> {
	
	private final SeriesTaskContainer container;
	
	private String query = null;
	
	public SeriesTask(SeriesTaskContainer container) {
		this.container = container;
	}
	
	public SeriesTask(SeriesTaskContainer container, String query) {
		this(container);
		
		this.query = query;
	}
	
    @Override
    protected void onPreExecute() {
    	container.preExecuteTask();
	}
	
    @Override
	protected List<Series> doInBackground(String... params) {
    	List<Series> res;
    	if (TextUtils.isEmpty(query))
    		res = Series.find(container.getContext(), params[0]);
    	else {
    		Session session = Session.getInstance(container.getContext());
    		res = new ArrayList<Series>();
        	Cursor cr = session.getDB().rawQuery(query, params);
        	try {
        		int ci = cr.getColumnIndex("tvdb_id");
        		while (cr.moveToNext())
        			res.add(Series.get(container.getContext(), cr.getString(ci)));
        	} finally {
        		cr.close();
        	}
    	}
		return res;
	}
    
    @Override
    protected void onPostExecute(List<Series> result) {
    	container.postExecuteTask(result);
	}
    
    public interface SeriesTaskContainer {
    	public Context getContext();
    	public void preExecuteTask();
    	public void postExecuteTask(List<Series> result);
    }
}