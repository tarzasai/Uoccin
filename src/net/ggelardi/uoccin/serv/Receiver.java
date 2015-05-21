package net.ggelardi.uoccin.serv;

import net.ggelardi.uoccin.MainActivity;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.MI;
import net.ggelardi.uoccin.serv.Commons.PK;
import net.ggelardi.uoccin.serv.Commons.SN;
import net.ggelardi.uoccin.serv.Commons.SR;
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

	private static final int NOTIF_MOV_WLST = 4;
	private static final int NOTIF_MOV_COLL = 5;
	private static final int NOTIF_SER_WLST = 6;
	private static final int NOTIF_SER_COLL = 7;
	private static final int NOTIF_SER_PREM = 8;
	
	private Session session;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.v(TAG, action);
		
		session = Session.getInstance(context);
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder nb = new NotificationCompat.Builder(session.getContext());
		
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			
			session.registerAlarms();
			
		} else if (action.equals(SR.CLEAN_DB_CACHE) || action.equals(SR.GDRIVE_SYNCNOW)) {
			
			WakefulIntentService.sendWakefulWork(context, new Intent(context, Service.class).setAction(action));
			
		} else if (action.equals(SN.CONNECT_FAIL)) {
			
			nm.notify(NOTIF_CONNECT_FAIL, makeNotification(SN.CONNECT_FAIL, true).setContentText(
				session.getString(R.string.notif_srv_gac_conn_fail)).build());
			
		} else if (action.equals(Commons.SN.GENERAL_FAIL)) {
			
			nm.notify(NOTIF_GENERAL_FAIL, makeNotification(null, true).setContentText(
				intent.getExtras().getString("what")).build());
			
		} else if (action.equals(Commons.SN.GENERAL_INFO)) {
			
			nm.notify(NOTIF_GENERAL_INFO, makeNotification(null, false).setContentText(
				intent.getExtras().getString("what")).build());
			
		} else if (action.equals(SN.MOV_WLST)) {
			
			nm.notify(NOTIF_MOV_WLST, nb.setSmallIcon(R.drawable.ic_notification_watchlist).setAutoCancel(
				true).setContentTitle(session.getString(R.string.notif_mov_wlst)).setContentIntent(getPI(
				getAI(MI.MOVIE_INFO).putExtra("imdb_id", intent.getExtras().getString("imdb_id")))).build());
			
		} else if (action.equals(SN.MOV_COLL)) {
			
			
		} else if (action.equals(SN.SER_WLST)) {
			
			
		} else if (action.equals(SN.SER_COLL)) {
			
			
		} else if (action.equals(SN.SER_PREM)) {
			
			
		}
	}
	
	private NotificationCompat.Builder makeNotification(String action, boolean sound) {
		Intent ai = new Intent(session.getContext(), MainActivity.class);
		if (action != null)
			ai.setAction(action);
		PendingIntent pi = PendingIntent.getActivity(session.getContext(), 0, ai, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder nb = new NotificationCompat.Builder(session.getContext());
		nb.setSmallIcon(R.drawable.ic_notification).setAutoCancel(true).setContentTitle(session.getString(
			R.string.app_name)).setContentIntent(pi);
		if (sound && session.getPrefs().getBoolean(PK.NOTIFSND, true))
			nb.setSound(NOTIF_SOUND);
		return nb;
	}
	
	private Intent getAI(String action) {
		return new Intent(session.getContext(), MainActivity.class).setAction(action);
	}
	
	private PendingIntent getPI(Intent actionIntent) {
		return PendingIntent.getActivity(session.getContext(), 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
}