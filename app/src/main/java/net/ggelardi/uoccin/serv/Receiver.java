package net.ggelardi.uoccin.serv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class Receiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String act = intent.getAction();
		if (act == null) {
			// WTF???
		} else if (act.equals(Commons.SN.ABORT_RESTOR)) {
			SharedPreferences.Editor editor = Session.getInstance(context).getPrefs().edit();
			editor.putBoolean(Commons.PK.BAKABORT, true);
			editor.commit();
		} else
			context.startService(new Intent(context, Service.class)
					.setAction(intent.getAction())
					.replaceExtras(intent.getExtras()));
	}
}
