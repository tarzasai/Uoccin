package net.ggelardi.uoccin.serv;

import net.ggelardi.uoccin.MainActivity;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.PK;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Receiver extends BroadcastReceiver {
	private static final String TAG = "Receiver";
	
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	
	private static final int NOTIF_CONNECT_FAIL = 1;
	private static final int NOTIF_GENERAL_FAIL = 2;
	private static final int NOTIF_GENERAL_INFO = 3;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Session session = Session.getInstance(context);
		String action = intent.getAction();
		Log.v(TAG, action);
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			session.registerAlarms();
		} else if (action.equals(Commons.SN.CONNECT_FAIL)) {
			NotificationCompat.Builder nb = makeNotification(session, Commons.SN.CONNECT_FAIL,
				true).setContentText(session.getString(R.string.notif_srv_gac_conn_fail));
			nm.notify(NOTIF_CONNECT_FAIL, nb.build());
		} else if (action.equals(Commons.SN.GENERAL_FAIL)) {
			NotificationCompat.Builder nb = makeNotification(session, null, true).setContentText(
				intent.getExtras().getString("what"));
			nm.notify(NOTIF_GENERAL_FAIL, nb.build());
		} else if (action.equals(Commons.SN.GENERAL_INFO)) {
			NotificationCompat.Builder nb = makeNotification(session, null, false).setContentText(
				intent.getExtras().getString("what"));
			nm.notify(NOTIF_GENERAL_INFO, nb.build());
		} else if (action.equals(Service.CLEAN_DB_CACHE) || action.equals(Service.GDRIVE_SYNC)) {
			Intent si = new Intent(context, Service.class);
			si.setAction(action);
			WakefulIntentService.sendWakefulWork(context, si);
		}
	}
	
	private NotificationCompat.Builder makeNotification(Session session, String action, boolean sound) {
		Intent ai = new Intent(session.getContext(), MainActivity.class);
		if (action != null)
			ai.setAction(action);
		PendingIntent pi = PendingIntent.getActivity(session.getContext(), 0, ai, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder nb = new NotificationCompat.Builder(session.getContext()).setSmallIcon(
			R.drawable.ic_notification).setContentTitle(session.getString(R.string.app_name)).setContentIntent(pi);
		if (sound && session.getPrefs().getBoolean(PK.NOTIFSND, true))
			nb.setSound(NOTIF_SOUND);
		return nb;
	}
}