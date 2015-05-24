package net.ggelardi.uoccin.serv;

import java.util.concurrent.atomic.AtomicInteger;

import net.ggelardi.uoccin.MainActivity;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.serv.Commons.MA;
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
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Receiver extends BroadcastReceiver {
	private static final String TAG = "Receiver";
	
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	
	private static final int NOTIF_CONNECT_FAIL = 1;
	private static final int NOTIF_GENERAL_FAIL = 2;
	private static final int NOTIF_GENERAL_INFO = 3;

	private static AtomicInteger NOTIF_ID = new AtomicInteger(99);
	
	private Session session;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		session = Session.getInstance(context);
		String action = intent.getAction();
		Bundle data = intent.getExtras();
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder ncb;
		
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
			
			Log.v("Title", "recv(wlst): " + data.getString("imdb_id"));
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_movie).setContentTitle(session.getString(
				R.string.notif_mov_wlst)).setContentText(data.getString("name", session.getString(
				R.string.notif_no_data))).setContentIntent(getPI(MA.MOVIE_INFO, data));
			if (session.getPrefs().getBoolean(PK.NOTIFSND, true))
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.MOV_COLL)) {
			
			Log.v("Title", "recv(coll): " + data.getString("imdb_id"));
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_movie).setContentTitle(session.getString(
				R.string.notif_mov_coll)).setContentText(data.getString("name", session.getString(
				R.string.notif_no_data))).setContentIntent(getPI(MA.MOVIE_INFO, data));
			if (session.getPrefs().getBoolean(PK.NOTIFSND, true))
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_WLST)) {
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_series).setContentTitle(session.getString(
				R.string.notif_ser_wlst)).setContentText(data.getString("name", session.getString(
				R.string.notif_no_data))).setContentIntent(getPI(MA.SERIES_INFO, data));
			if (session.getPrefs().getBoolean(PK.NOTIFSND, true))
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
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
	
	private PendingIntent getPI(String action, Bundle data) {
		return PendingIntent.getActivity(session.getContext(), 0, new Intent(session.getContext(),
			MainActivity.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT, data);
			//MainActivity.class).setAction(action), Intent.FLAG_ACTIVITY_SINGLE_TOP, data);
	}
}