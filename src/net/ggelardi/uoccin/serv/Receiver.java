package net.ggelardi.uoccin.serv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Receiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Session session = Session.getInstance(context);
		String act = intent.getAction();
		Log.v(getClass().getSimpleName(), act);
		if (act.equals(Intent.ACTION_BOOT_COMPLETED)) {
			session.registerAlarms();
		}
	}
}