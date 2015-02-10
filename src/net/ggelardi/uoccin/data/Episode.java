package net.ggelardi.uoccin.data;

import android.content.Context;
import android.database.Cursor;

public class Episode extends Title {
	
	public Episode(Context context) {
		super(context);
		
		type = EPISODE;
	}
	
	public Episode(Context context, Cursor cr) {
		super(context, cr);
		
		//
		
		if (isOld() && session.isOnWIFI())
			refresh();
	}

	public int season;
	public int episode;
	public long firstAired;
	public boolean collected = false;
	public boolean watched = false;
	
	@Override
	protected void refresh() {
		dispatch(TitleEvent.LOADING);
		
	}
}