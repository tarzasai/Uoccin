package net.ggelardi.uoccin.serv;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

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

public class Commons {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 5.1; rv:16.0) Gecko/20100101 Firefox/16.0";

    public static final String[] weekdaysFull = new DateFormatSymbols(Locale.getDefault()).getWeekdays();
    public static final String[] weekdaysShort = new DateFormatSymbols(Locale.getDefault()).getShortWeekdays();

    // preference keys
    public static class PK {
        public static final String STARTUPV = "pk_startupv";
        public static final String LANGUAGE = "pk_language";
        public static final String SPECIALS = "pk_specials";
        public static final String METAWIFI = "pk_metawifi";
        public static final String GDRVAUTH = "pk_gdrvauth";
        public static final String GDRVPHOT = "pk_gdrvphot";
        public static final String GDRVSYNC = "pk_gdrvsync";
        public static final String GDRVUUID = "pk_gdrvuuid";
        public static final String GDRVWIFI = "pk_gdrvwifi";
        public static final String GDRVINTV = "pk_gdrvintv";
        public static final String GDRVLCID = "pk_gdrvlcid";
        public static final String NOTMOVWL = "pk_notmovwl";
        public static final String NOTMOVCO = "pk_notmovco";
        public static final String NOTSERWL = "pk_notserwl";
        public static final String NOTSERCO = "pk_notserco";
        public static final String NOTIFSND = "pk_notifsnd";
        public static final String SPLRPROT = "pk_splrprot";
        public static final String SYNCLOCK = "pk_synclock";
        public static final String TVDBTOKV = "pk_tvdbtokv";
        public static final String BAKABORT = "pk_bakabort";
    }

    // service requests
    public static class SR {
        public static final String CLEAN_DB_CACHE = "net.ggelardi.uoccin.CLEAN_DB_CACHE";
        public static final String REFRESH_MOVIE = "net.ggelardi.uoccin.REFRESH_MOVIE";
        public static final String REFRESH_SERIES = "net.ggelardi.uoccin.REFRESH_SERIES";
        public static final String REFRESH_EPISODE = "net.ggelardi.uoccin.REFRESH_EPISODE";
        public static final String GDRIVE_SYNCNOW = "net.ggelardi.uoccin.GDRIVE_SYNC";
        public static final String GDRIVE_BACKUP = "net.ggelardi.uoccin.GDRIVE_BACKUP";
        public static final String GDRIVE_RESTORE = "net.ggelardi.uoccin.GDRIVE_RESTORE";
    }

    // service notifications
    public static class SN {
        public static final String CONNECT_FAIL = "net.ggelardi.uoccin.CONNECT_FAIL";
        public static final String GENERAL_FAIL = "net.ggelardi.uoccin.GENERAL_FAIL";
        public static final String GENERAL_INFO = "net.ggelardi.uoccin.GENERAL_INFO";
        public static final String ABORT_RESTOR = "net.ggelardi.uoccin.ABORT_RESTOR";
        public static final String MOV_WLST = "net.ggelardi.uoccin.MOV_WLST";
        public static final String MOV_COLL = "net.ggelardi.uoccin.MOV_COLL";
        public static final String SER_WLST = "net.ggelardi.uoccin.SER_WLST";
        public static final String SER_COLL = "net.ggelardi.uoccin.SER_COLL";
        public static final String SER_CANC = "net.ggelardi.uoccin.SER_CANC";
    }

    public static class MA {
        public static final String MOVIE_INFO = "net.ggelardi.uoccin.MOVIE_INFO";
        public static final String SERIES_INFO = "net.ggelardi.uoccin.SERIES_INFO";
        public static final String EPISODE_INFO = "net.ggelardi.uoccin.EPISODE_INFO";
    }

    // MIME types
    public static class MT {
        public static final String TEXT = "text/plain";
        public static final String JSON = "application/json";
    }

    //
    public static class UE {
        public static final String MOVIE = "net.ggelardi.uoccin.update.MOVIE";
        public static final String SERIES = "net.ggelardi.uoccin.update.SERIES";
        public static final String EPISODE = "net.ggelardi.uoccin.update.EPISODE";
    }

    public static long minutes(long m) {
        return m * 60 * 1000;
    }

    public static long hours(long h) {
        return h * minutes(60);
    }

    public static long days(long d) {
        return d * hours(24);
    }

    public static long months(long m) {
        return m * days(30);
    }

    public static boolean olderThan(long time, long howMuch) {
        return time > 0 && (time < System.currentTimeMillis()) && ((System.currentTimeMillis() - time) > howMuch);
    }

    public static boolean newerThan(long time, long howMuch) {
        return time > 0 && time < (System.currentTimeMillis() + howMuch);
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

    public static int getColumnIndex(Cursor cr, String names) {
        String[] lst = names.split(",");
        int res;
        for (String name: lst) {
            res = cr.getColumnIndex(name);
            if (res >= 0)
                return res;
        }
        return -1;
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

    public static String cleanCRLFs(String text) {
        text = text.replace("\r\n", "\n");
        while (text.contains("\n\n"))
            text = text.replace("\n\n", "\n");
        if (text.endsWith("\n"))
            text = TextUtils.substring(text, 0, text.length() - 1);
        return text;
    }

    public static class SDF {
        private static final List<String> engdays = Arrays.asList("sunday", "monday", "tuesday", "wednesday",
            "thursday", "friday", "saturday");

        public static final String RFC3339 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        public static final String TIMESTAMP = "yyyyMMddHHmmss";

        public static SimpleDateFormat eng(String format) {
            SimpleDateFormat res = new SimpleDateFormat(format, Locale.ENGLISH);
            res.setTimeZone(TimeZone.getTimeZone("UTC"));
            return res;
        }

        public static SimpleDateFormat loc(String format) {
            SimpleDateFormat res = new SimpleDateFormat(format, Locale.getDefault());
            return res;
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
}
