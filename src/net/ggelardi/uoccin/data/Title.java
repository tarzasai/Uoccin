package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.serv.Session;
import android.content.ContentValues;
import android.text.TextUtils;

public abstract class Title {
	protected static final String QUEUE_SET = "QUEUE_SET";
	protected static final String QUEUE_DEL = "QUEUE_DEL";
	
	protected static final List<OnTitleListener> listeners = new ArrayList<OnTitleListener>();
	
	protected synchronized static void queue(Session session, String command, String id, String field, String value) {
		ContentValues cv = new ContentValues();
		cv.put("command", command.equals(QUEUE_SET) ? "set" : "del");
		cv.put("object", id);
		if (!TextUtils.isEmpty(field))
			cv.put("field", field);
		if (!TextUtils.isEmpty(value))
			cv.put("value", value);
		session.getDB().insertOrThrow("queue", null, cv);
	}

	protected Session session;
	
	public static boolean ongoingServiceOperation = false;
	
	public static void addOnTitleEventListener(OnTitleListener aListener) {
		listeners.add(aListener);
	}
	
	public static void dispatch(String state, Throwable error) {
		for (OnTitleListener listener: listeners)
			listener.changed(state, error);
	}
	
	public interface OnTitleListener {
		public static String NOTFOUND = "TitleEvent.NOTFOUND";
		public static String WORKING = "TitleEvent.WORKING";
		public static String READY = "TitleEvent.READY";
		public static String ERROR = "TitleEvent.ERROR";
		
		void changed(final String state, final Throwable error);
	}
}