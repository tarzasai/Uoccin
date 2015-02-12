package net.ggelardi.uoccin.serv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit.RetrofitError;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.client.UrlConnectionClient;
import retrofit.mime.TypedByteArray;
import android.util.Log;

public class Commons {
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 5.1; rv:16.0) Gecko/20100101 Firefox/16.0";
	
	/*
	static class PK {
		public static final String USERNAME = "Username";
		public static final String REMOTEKEY = "RemoteKey";
		public static final String STARTUP = "pk_startup";
		public static final String LOCALE = "pk_locale";
		public static final String PROF_INFO = "pk_prof_info";
		public static final String PROF_LIST = "pk_prof_list";
		public static final String FEED_UPD = "pk_feed_upd";
		public static final String FEED_FOF = "pk_feed_fof";
		public static final String FEED_HID = "pk_feed_hid";
		public static final String FEED_ELC = "pk_feed_elc";
		public static final String FEED_HBK = "pk_feed_hbk";
		public static final String FEED_HBF = "pk_feed_hbf";
		public static final String FEED_SPO = "pk_feed_spo";
		public static final String ENTR_IMCO = "pk_entry_imco";
		public static final String SERV_PROF = "pk_serv_prof";
		public static final String SERV_NOTF = "pk_serv_notf";
		public static final String SERV_MSGS = "pk_serv_msgs";
		//
		public static final String SERV_MSGS_TIME = "pk_serv_msgs_time";
		public static final String SERV_MSGS_CURS = "pk_serv_msgs_cursor";
	}
	*/
	
	public static long convertTime(long timestamp, String fromTimeZone, String toTimeZone) {
		Calendar fromCal = new GregorianCalendar(TimeZone.getTimeZone(fromTimeZone));
		fromCal.setTimeInMillis(timestamp);
		Calendar toCal = new GregorianCalendar(TimeZone.getTimeZone(toTimeZone));
		toCal.setTimeInMillis(fromCal.getTimeInMillis());
		return toCal.getTimeInMillis();
	}
	
	public static int getDatePart(Date date, int part) {
		if (date == null)
			return 0;
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTime(date);
		return cal.get(part);
	}
	
	public static int getDatePart(long time, int part) {
		if (time <= 0)
			return 0;
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTimeInMillis(time);
		return cal.get(part);
	}

	public static int retrofitErrorCode(RetrofitError error) {
		Response r = error.getResponse();
		if (r != null)
			return r.getStatus();
		return -1;
	}

	public static String retrofitErrorText(RetrofitError error) {
		String msg;
		Response r = error.getResponse();
		if (r != null) {
			if (r.getBody() instanceof TypedByteArray) {
				TypedByteArray b = (TypedByteArray) r.getBody();
				msg = new String(b.getBytes());
			} else
				msg = r.getReason();
		} else {
			msg = error.getLocalizedMessage();
		}
		if (msg == null && error.getCause() != null) {
			msg = error.getCause().getLocalizedMessage();
			if (msg == null)
				msg = error.getCause().getClass().getName();
		}
		Log.v("RPC", msg);
		Log.v("RPC", error.getUrl());
		return msg;
	}
	
	static class WaitingUCC extends UrlConnectionClient {
		@Override
		protected HttpURLConnection openConnection(Request request) throws IOException {
			HttpURLConnection connection = super.openConnection(request);
			connection.setConnectTimeout(30 * 1000); // 30 sec
			connection.setReadTimeout(60 * 1000); // 60 sec
			return connection;
		}
	}
}