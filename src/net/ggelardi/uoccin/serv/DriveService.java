package net.ggelardi.uoccin.serv;

import android.util.Log;

import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.DriveEventService;

public class DriveService extends DriveEventService {
	private static final String TAG = "DriveService";
	
	@Override
	public void onChange(ChangeEvent event) {
		Log.d(TAG, event.toString());
		// Application-specific handling of event.
	}
}