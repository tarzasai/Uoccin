package net.ggelardi.uoccin.serv;

import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import retrofit.RetrofitError;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.client.UrlConnectionClient;
import retrofit.mime.TypedByteArray;
import android.text.TextUtils;
import android.util.Log;

public class Commons {
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 5.1; rv:16.0) Gecko/20100101 Firefox/16.0";
	
	public static final String[] weekdays = new DateFormatSymbols(Locale.getDefault()).getWeekdays();
	
	public static final long dayLong = (24 * 60 * 60 * 1000);
	public static final long weekLong = (7 * dayLong);
	
	public static class PK {
		public static final String STARTUPV = "pk_startup";
		public static final String LANGUAGE = "pk_locale";
		public static final String SPECIALS = "pk_specials";
		public static final String TVDBFEED = "pk_tvdbrss";
		public static final String GDRVSYNC = "pk_gdrvbak";
		public static final String GDRVWIFI = "pk_gdrvwifi";
		public static final String GDRVINTV = "pk_gdrvint";
		public static final String GDRVAUTH = "pk_gdrv_account";
		public static final String GDRVLCID = "pk_gdrv_lastcid";
		public static final String NOTIFSND = "pk_notif_sound";
	}
	
	public static class GD {
		public static final String FOLDER = "uoccin";
		//public static final String FOLDER = "uoccin_test";
		public static final String MOV_WLST = "movies.watchlist.json";
		public static final String MOV_COLL = "movies.collected.json";
		public static final String MOV_SEEN = "movies.watched.json";
		public static final String SER_WLST = "series.watchlist.json";
		public static final String SER_COLL = "series.collected.json";
		public static final String SER_SEEN = "series.watched.json";
	}
	
	public static class SN {
		public static final String CONNECT_FAIL = "net.ggelardi.uoccin.CONNECT_FAIL";
		public static final String GENERAL_FAIL = "net.ggelardi.uoccin.GENERAL_FAIL";
		public static final String GENERAL_INFO = "net.ggelardi.uoccin.GENERAL_INFO";
	}
	
	public static long convertTZ(long timestamp, String fromTimeZone, String toTimeZone) {
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
	
	public static boolean sameStringLists(List<String> list1, List<String> list2) {
		if (list1.size() != list2.size())
			return false;
		return TextUtils.join(",", list1).equals(TextUtils.join(",", list2));
	}
	
	public static int str2int(String s, int defVal) {
		if (s.equals("N/A"))
			return defVal;
		try {
			s = s.trim();
			s = s.contains(" ") ? s.split(" ")[0] : s;
			return NumberFormat.getInstance(Locale.ENGLISH).parse(s).intValue();
		} catch (Exception err) {
			Log.e("str2int", s, err);
			return defVal;
		}
	}
	
	public static double str2num(String s, double defVal) {
		if (s.equals("N/A"))
			return defVal;
		try {
			s = s.trim();
			s = s.contains(" ") ? s.split(" ")[0] : s;
			return Double.parseDouble(s);
		} catch (Exception err) {
			Log.e("str2num", s, err);
			return defVal;
		}
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
	
	public static class WaitingUCC extends UrlConnectionClient {
		@Override
		protected HttpURLConnection openConnection(Request request) throws IOException {
			HttpURLConnection connection = super.openConnection(request);
			connection.setConnectTimeout(30 * 1000); // 30 sec
			connection.setReadTimeout(60 * 1000); // 60 sec
			return connection;
		}
	}
	
	public static class SDF {
		private static final List<String> engdays = Arrays.asList("sunday", "monday", "tuesday", "wednesday",
			"thursday", "friday", "saturday");
		
		public static SimpleDateFormat eng(String format) {
			return new SimpleDateFormat(format, Locale.ENGLISH);
		}
		
		public static SimpleDateFormat loc(String format) {
			return new SimpleDateFormat(format, Locale.getDefault());
		}
		
		public static int day(String dayname) {
			return TextUtils.isEmpty(dayname) ? 0 : engdays.indexOf(dayname.toLowerCase(Locale.getDefault())) + 1;
		}
	}
	
	public static class XML {
		
		public static Document str2xml(String text) {
			try {
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder bld = fac.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(text));
				Document doc = bld.parse(is);
				doc.getDocumentElement().normalize();
				return doc;
			} catch (Exception err) {
				Log.e("Commons.XML", "str2xml", err);
				return null;
			}
		}
		
		public static String nodeText(Element node, String ... names) {
			for (String name: names)
				try {
					return node.getElementsByTagName(name).item(0).getTextContent();
				} catch (Exception err) {
					//Log.e("Commons.XML", "nodeText(" + name + ")", err); // debug only.
				}
			return "";
		}
	}
}