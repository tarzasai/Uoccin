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
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

public class Commons {
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 5.1; rv:16.0) Gecko/20100101 Firefox/16.0";
	
	public static final String[] weekdays = new DateFormatSymbols(Locale.getDefault()).getWeekdays();
	
	public static final long dayLong = (24 * 60 * 60 * 1000);
	public static final long weekLong = (7 * dayLong);
	public static final long monthLong = (30 * dayLong);
	public static final long yearLong = (365 * dayLong);
	
	public static class PK {
		public static final String STARTUPV = "pk_startupv";
		public static final String LANGUAGE = "pk_language";
		public static final String SPECIALS = "pk_specials";
		public static final String METAWIFI = "pk_metawifi";
		public static final String TVDBFEED = "pk_tvdbfeed";
		public static final String TVDBGFLT = "pk_tvdbgflt";
		public static final String GDRVAUTH = "pk_gdrvauth";
		public static final String GDRVSYNC = "pk_gdrvsync";
		public static final String GDRVWIFI = "pk_gdrvwifi";
		public static final String GDRVINTV = "pk_gdrvintv";
		public static final String NOTMOVWL = "pk_notmovwl";
		public static final String NOTMOVCO = "pk_notmovco";
		public static final String NOTSERWL = "pk_notserwl";
		public static final String NOTSERCO = "pk_notserco";
		public static final String NOTIFSND = "pk_notifsnd";
		public static final String SPLRPROT = "pk_splrprot";
		// internal
		public static final String GDRVUUID = "pk_gdrvuuid";
		public static final String GDRVLCID = "pk_gdrvlcid";
	}
	
	public static class SR {
		public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
		public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
		public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
		public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
		public static final String CHECK_TVDB_RSS = "net.ggelardi.uoccin.CHECK_TVDB_RSS";
		public static final String GDRIVE_SYNCNOW = "net.ggelardi.uoccin.GDRIVE_SYNC";
		public static final String GDRIVE_BACKUP = "net.ggelardi.uoccin.GDRIVE_BACKUP";
		public static final String GDRIVE_RESTORE = "net.ggelardi.uoccin.GDRIVE_RESTORE";
	}
	
	public static class SN {
		public static final String CONNECT_FAIL = "net.ggelardi.uoccin.CONNECT_FAIL";
		public static final String GENERAL_FAIL = "net.ggelardi.uoccin.GENERAL_FAIL";
		public static final String GENERAL_INFO = "net.ggelardi.uoccin.GENERAL_INFO";
		public static final String MOV_WLST = "net.ggelardi.uoccin.MOV_WLST";
		public static final String MOV_COLL = "net.ggelardi.uoccin.MOV_COLL";
		public static final String SER_WLST = "net.ggelardi.uoccin.SER_WLST";
		public static final String SER_COLL = "net.ggelardi.uoccin.SER_COLL";
		public static final String SER_PREM = "net.ggelardi.uoccin.SER_PREM";
		public static final String DBG_TVDB_RSS = "net.ggelardi.uoccin.DBG_TVDB_RSS";
	}

	public static class MA {
		public static final String MOVIE_INFO = "net.ggelardi.uoccin.MOVIE_INFO";
		public static final String SERIES_INFO = "net.ggelardi.uoccin.SERIES_INFO";
		public static final String EPISODE_INFO = "net.ggelardi.uoccin.EPISODE_INFO";
	}

	public static class MT {
		public static final String TEXT = "text/plain";
		public static final String JSON = "application/json";
	}
	
	public static class TL {
		public static final String QUERY = "QUERY";
		public static final String SEARCH = "SEARCH";
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
	
	public static String firstUrl(String text) {
		if (!TextUtils.isEmpty(text))
			for (String s : text.split("\\s+"))
				if (Patterns.WEB_URL.matcher(s).matches())
					return s;
		return null;
	}
	
	public static String shortenText(final String content, final int length) {
	    String result = content.substring(0, length);
	    if (content.charAt(length) != ' ')
	        result = result.substring(0, result.lastIndexOf(" "));
	    return result;
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
	
	public static String logCursor(String prefix, Cursor cr) {
		StringBuilder sb = new StringBuilder().append(prefix + ": ");
		for (int i = 0; i < cr.getColumnCount(); i++) {
			sb.append(cr.getColumnName(i)).append("=");
			switch (cr.getType(i)) {
				case Cursor.FIELD_TYPE_NULL:
					sb.append("NULL");
					break;
				case Cursor.FIELD_TYPE_STRING:
					sb.append("\"").append(cr.getString(i)).append("\"");
					break;
				case Cursor.FIELD_TYPE_INTEGER:
					sb.append(cr.getInt(i));
					break;
				case Cursor.FIELD_TYPE_FLOAT:
					sb.append(cr.getFloat(i));
					break;
				case Cursor.FIELD_TYPE_BLOB:
					sb.append("BIN(").append(cr.getBlob(i).length).append("b)");
					break;
			}
			sb.append(" ");
		}
		return sb.toString();
	}
	
	public static String logContentValue(String prefix, ContentValues cv) {
		StringBuilder sb = new StringBuilder().append(prefix + ": ");
		for (String key: cv.keySet())
			sb.append(key).append("=").append(cv.getAsString(key)).append(" ");
		return sb.toString();
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

		public static final String RFC3339 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		public static final String TIMESTAMP = "yyyyMMddHHmmss";
		
		public static SimpleDateFormat eng(String format) {
			return new SimpleDateFormat(format, Locale.ENGLISH);
		}
		
		public static SimpleDateFormat loc(String format) {
			return new SimpleDateFormat(format, Locale.getDefault());
		}
		
		public static int day(String dayname) {
			return TextUtils.isEmpty(dayname) ? 0 : engdays.indexOf(dayname.toLowerCase(Locale.getDefault())) + 1;
		}
		
		public static String rfc3339(long time) {
			return eng(RFC3339).format(new Date(time));
		}
		
		public static String timestamp(long time) {
			return eng(TIMESTAMP).format(new Date(time));
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
			Log.d("Commons.XML", "Node(s) not found in xml: " + TextUtils.join(", ", names));
			return "";
		}
		
		public static String attrText(Element node, String ... names) {
			String val;
			for (String name: names)
				try {
					val = node.getAttribute(name);
					if (!TextUtils.isEmpty(val))
						return val;
				} catch (Exception err) {
					//Log.e("Commons.XML", "attrText(" + name + ")", err); // debug only.
				}
			Log.d("Commons.XML", "Attribute(s) not found in xml: " + TextUtils.join(", ", names));
			return "";
		}
	}
}