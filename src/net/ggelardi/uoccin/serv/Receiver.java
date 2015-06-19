package net.ggelardi.uoccin.serv;

import java.util.concurrent.atomic.AtomicInteger;

import net.ggelardi.uoccin.MainActivity;
import net.ggelardi.uoccin.R;
import net.ggelardi.uoccin.data.Episode.EID;
import net.ggelardi.uoccin.serv.Commons.MA;
import net.ggelardi.uoccin.serv.Commons.SN;
import net.ggelardi.uoccin.serv.Commons.SR;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.commonsware.cwac.wakeful.WakefulIntentService;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

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
		final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder ncb;
		
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			
			session.registerAlarms();
			
		} else if (action.equals(SR.CLEAN_DB_CACHE) || action.equals(SR.GDRIVE_SYNCNOW) ||
			action.equals(SR.CHECK_TVDB_RSS)) {
			
			session.registerAlarms();
			WakefulIntentService.sendWakefulWork(context, new Intent(context, Service.class).setAction(action));
			
		} else if (action.equals(SN.CONNECT_FAIL)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_error);
			ncb.setContentTitle(session.getString(R.string.notif_gac_fail));
			ncb.setContentText(data.getString("what"));
			ncb.setContentIntent(newPI(newAI(SN.CONNECT_FAIL), false));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_CONNECT_FAIL, ncb.build());
			
		} else if (action.equals(Commons.SN.GENERAL_FAIL)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_error);
			ncb.setContentTitle(session.getString(R.string.notif_srv_fail));
			ncb.setContentText(data.getString("what"));
			ncb.setContentIntent(newPI(newAI(SN.GENERAL_FAIL), false));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_GENERAL_FAIL, ncb.build());
			
		} else if (action.equals(Commons.SN.GENERAL_INFO)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_info);
			ncb.setContentTitle(session.getString(R.string.notif_srv_info));
			ncb.setContentText(data.getString("what"));
			ncb.setContentIntent(newPI(newAI(SN.GENERAL_INFO), false));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_GENERAL_INFO, ncb.build());
			
		} else if (action.equals(SN.MOV_WLST)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_movie);
			ncb.setContentTitle(session.getString(R.string.notif_mov_wlst));
			ncb.setContentText(data.getString("name", session.getString(R.string.notif_gen_miss)));
			ncb.setContentIntent(newPI(newAI(MA.MOVIE_INFO).putExtra("imdb_id", data.getString("imdb_id")), true));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.MOV_COLL)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_movie);
			ncb.setContentTitle(session.getString(R.string.notif_mov_coll));
			ncb.setContentText(data.getString("name", session.getString(R.string.notif_gen_miss)));
			ncb.setContentIntent(newPI(newAI(MA.MOVIE_INFO).putExtra("imdb_id", data.getString("imdb_id")), true));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_WLST)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_series);
			ncb.setContentTitle(session.getString(R.string.notif_ser_wlst));
			ncb.setContentText(data.getString("name", session.getString(R.string.notif_gen_miss)));
			ncb.setContentIntent(newPI(newAI(MA.SERIES_INFO).putExtra("tvdb_id", data.getString("tvdb_id")), true));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_COLL)) {
			
			EID eid = new EID(data.getString("series"), data.getInt("season"), data.getInt("episode"));
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_series);
			ncb.setContentTitle(session.getString(R.string.notif_ser_coll));
			ncb.setContentText(eid.readable() + " " + data.getString("name",
				session.getString(R.string.unknown_title)));
			ncb.setContentIntent(newPI(newAI(MA.EPISODE_INFO).putExtra("series", eid.series).putExtra("season",
				eid.season).putExtra("episode", eid.episode), true));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			
		} else if (action.equals(SN.SER_PREM)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_premiere);
			ncb.setContentTitle(session.getString(R.string.notif_ser_prem));
			ncb.setContentText(data.getString("name", session.getString(R.string.notif_gen_miss)));
			ncb.setContentIntent(newPI(newAI(MA.SERIES_INFO).putExtra("tvdb_id", data.getString("tvdb_id")), true));
			if (session.notificationSound())
				ncb.setSound(NOTIF_SOUND);
			String plot = data.getString("plot");
			if (!TextUtils.isEmpty(plot))
				ncb.setStyle(new NotificationCompat.BigTextStyle().bigText(plot));
			String purl = data.getString("poster");
			if (TextUtils.isEmpty(purl))
				nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
			else
				session.picasso().load(purl).placeholder(R.drawable.ic_notification_premiere).into(new Target() {
					@Override
					public void onPrepareLoad(Drawable arg0) {
					}
					@Override
					public void onBitmapLoaded(Bitmap bitmap, LoadedFrom source) {
						ncb.setLargeIcon(bitmap);
						nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
					}
					@Override
					public void onBitmapFailed(Drawable arg0) {
						nm.notify(NOTIF_ID.incrementAndGet(), ncb.build());
					}
				});
			
		} else if (action.equals(SN.DBG_TVDB_RSS)) {
			
			ncb = new NotificationCompat.Builder(session.getContext()).setAutoCancel(true);
			ncb.setSmallIcon(R.drawable.ic_notification_info);
			ncb.setContentTitle(session.getString(R.string.notif_dbg_rsst));
			ncb.setContentText(String.format(session.getString(R.string.notif_dbg_rssm), data.getInt("tot"),
				data.getInt("chk"), data.getInt("oks")));
			ncb.setContentIntent(newPI(newAI(SN.GENERAL_INFO), false));
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
}