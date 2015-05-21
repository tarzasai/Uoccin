package net.ggelardi.uoccin.serv;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.data.Series;
import net.ggelardi.uoccin.serv.Commons.TL;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

public class SeriesTask extends AsyncTask<String, Void, List<Series>> {
	
	private final SeriesTaskContainer container;
	private final String type;
	
	public SeriesTask(SeriesTaskContainer container, String type) {
		super();

		this.container = container;
		this.type = type;
	}
	
    @Override
    protected void onPreExecute() {
    	container.preExecuteTask();
	}
	
    @Override
	protected List<Series> doInBackground(String... params) {
    	List<Series> res;
    	if (type.equals(TL.SEARCH)) {
    		res = Series.find(container.getContext(), params[0]);
    	} else {
    		String query = params[0];
    		String[] args = new String[params.length - 1];
    		System.arraycopy(params, 1, args, 0, params.length - 1);
    		res = new ArrayList<Series>();
        	Cursor cr = Session.getInstance(container.getContext()).getDB().rawQuery(query, args);
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