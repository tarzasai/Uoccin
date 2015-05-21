package net.ggelardi.uoccin.serv;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.data.Movie;
import net.ggelardi.uoccin.serv.Commons.TL;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

public class MovieTask extends AsyncTask<String, Void, List<Movie>> {
	
	private final MovieTaskContainer container;
	private final String type;
	
	public MovieTask(MovieTaskContainer container, String type) {
		super();

		this.container = container;
		this.type = type;
	}
	
    @Override
    protected void onPreExecute() {
    	container.preExecuteTask();
	}
    
	@Override
	protected List<Movie> doInBackground(String... params) {
    	List<Movie> res;
    	if (type.equals(TL.SEARCH)) {
    		res = Movie.find(container.getContext(), params[0]);
    	} else {
    		String query = params[0];
    		String[] args = new String[params.length - 1];
    		System.arraycopy(params, 1, args, 0, params.length - 1);
    		res = new ArrayList<Movie>();
        	Cursor cr = Session.getInstance(container.getContext()).getDB().rawQuery(query, args);
        	try {
        		int ci = cr.getColumnIndex("imdb_id");
        		while (cr.moveToNext())
        			res.add(Movie.get(container.getContext(), cr.getString(ci)));
        	} finally {
        		cr.close();
        	}
    	}
		return res;
	}
    
    @Override
    protected void onPostExecute(List<Movie> result) {
    	container.postExecuteTask(result);
	}
    
    public interface MovieTaskContainer {
    	public Context getContext();
    	public void preExecuteTask();
    	public void postExecuteTask(List<Movie> result);
    }
}