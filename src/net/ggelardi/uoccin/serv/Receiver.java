package net.ggelardi.uoccin.serv;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import net.ggelardi.uoccin.MainActivity;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Episode;
import net.ggelardi.uoccin.serv.Commons.MA;
import net.ggelardi.uoccin.serv.Commons.SN;
import net.ggelardi.uoccin.serv.Commons.SR;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class Receiver extends BroadcastReceiver {
	private static final String TAG = "Receiver";
	
	private static final Uri NOTIF_SOUND = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
	
	private static final int NOTIF_CONNECT_FAIL = 1;
	private static final int NOTIF_GENERAL_FAIL = 2;
	private static final int NOTIF_GENERAL_INFO = 3;

	private static AtomicInteger NOTIF_ID = new AtomicInteger(99);
	private static AtomicInteger INTENT_ID = new AtomicInteger(0);
	
	private Session session;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, intent.toString());
		
		session = Session.getInstance(context);
		String action = intent.getAction();
		Bundle data = intent.getExtras();
		NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder ncb;
		String title;
		String text;
		PendingIntent pi;
		
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			
			session.registerAlarms();
			
		} else if (action.equals(SR.CLEAN_DB_CACHE) || action.equals(SR.GDRIVE_SYNCNOW) ||
			action.equals(SR.CHECK_TVDB_RSS)) {
			
			session.registerAlarms();
			WakefulIntentService.sendWakefulWork(context, new Intent(context, Service.class).setAction(action));
			
		} else if (action.equals(SN.CONNECT_FAIL)) {
			
			title = session.getString(R.string.notif_gac_fail);
			text = data.getString("what");
			pi = newPI(newAI(SN.CONNECT_FAIL), false);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_error).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_CONNECT_FAIL, ncb.build());
			
		} else if (action.equals(Commons.SN.GENERAL_FAIL)) {
			
			title = session.getString(R.string.notif_srv_fail);
			text = data.getString("what");
			pi = newPI(newAI(SN.GENERAL_FAIL), false);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_error).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_GENERAL_FAIL, ncb.build());
			
		} else if (action.equals(Commons.SN.GENERAL_INFO)) {
			
			title = session.getString(R.string.notif_srv_info);
			text = data.getString("what");
			pi = newPI(newAI(SN.GENERAL_INFO), false);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_info).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_GENERAL_INFO, ncb.build());
			
		} else if (action.equals(SN.MOV_WLST)) {
			
			title = session.getString(R.string.notif_mov_wlst);
			text = data.getString("name", session.getString(R.string.notif_gen_miss));
			pi = newPI(newAI(MA.MOVIE_INFO).putExtra("imdb_id", data.getString("imdb_id")), true);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_movie).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.MOV_COLL)) {
			
			title = session.getString(R.string.notif_mov_coll);
			text = data.getString("name", session.getString(R.string.notif_gen_miss));
			pi = newPI(newAI(MA.MOVIE_INFO).putExtra("imdb_id", data.getString("imdb_id")), true);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_movie).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_WLST)) {
			
			title = session.getString(R.string.notif_ser_wlst);
			text = data.getString("name", session.getString(R.string.notif_gen_miss));
			pi = newPI(newAI(MA.SERIES_INFO).putExtra("tvdb_id", data.getString("tvdb_id")), true);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_series).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_COLL)) {
			
			int season = data.getInt("season");
			int episode = data.getInt("episode");
			title = session.getString(R.string.notif_ser_coll);
			text = data.getString("name");
			if (TextUtils.isEmpty(text))
				text = session.getString(R.string.notif_gen_miss);
			else
				text = new Episode.EID(season, episode).readable() + " " + text;
			pi = newPI(newAI(MA.EPISODE_INFO).putExtra("series", data.getString("series")).putExtra("season",
				season).putExtra("episode", episode), true);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_series).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_PREM)) {
			
			title = session.getString(R.string.notif_ser_prem);
			text = data.getString("name", session.getString(R.string.notif_gen_miss));
			pi = newPI(newAI(MA.SERIES_INFO).putExtra("tvdb_id", data.getString("tvdb_id")), true);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_premiere).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			String plot = data.getString("plot");
			if (!TextUtils.isEmpty(plot))
				ncb.setStyle(new NotificationCompat.BigTextStyle().bigText(plot));
			String purl = data.getString("poster");
			Bitmap poster = !TextUtils.isEmpty(purl) ? getBitmapFromURL(purl) : null;
			if (poster != null)
				ncb.setLargeIcon(poster);
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.DBG_TVDB_RSS)) {
			
			title = session.getString(R.string.notif_dbg_rsst);
			text = String.format(session.getString(R.string.notif_dbg_rssm), data.getInt("tot"), data.getInt("chk"),
				data.getInt("oks"));
			pi = newPI(newAI(SN.GENERAL_INFO), false);
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(
				true).setSmallIcon(R.drawable.ic_notification_info).setContentTitle(
				title).setContentText(text).setContentIntent(pi);
			nm.notify(NOTIF_GENERAL_INFO, ncb.build());
			
		}
	}
	
	private Intent newAI(String action) {
		return new Intent(session.getContext(), MainActivity.class).setAction(action);
	}
	
	private PendingIntent newPI(Intent action, boolean unique) {
		return PendingIntent.getActivity(session.getContext(), unique ? INTENT_ID.incrementAndGet() : 0, action,
			PendingIntent.FLAG_UPDATE_CURRENT);
	}
	
	private Bitmap getBitmapFromURL(String strURL) {
		try {
			URL url = new URL(strURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);
			return myBitmap;
		} catch (Exception err) {
			Log.e(TAG, "getBitmapFromURL", err);
			return null;
		}
	}
}