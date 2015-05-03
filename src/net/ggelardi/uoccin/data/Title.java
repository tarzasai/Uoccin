package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.serv.Session;

public abstract class Title {
	
	protected static final List<OnTitleListener> listeners = new ArrayList<OnTitleListener>();
	
	public static void addOnTitleEventListener(OnTitleListener aListener) {
		listeners.add(aListener);
	}
	
	public static void removeOnTitleEventListener(OnTitleListener aListener) {
		listeners.remove(aListener);
	}
	
	public static void dispatch(String state, Throwable error) {
		for (OnTitleListener listener: listeners)
			listener.onTitleEvent(state, error);
	}
	
	protected Session session;
	
	public long timestamp = 0;
	
	public long age() {
		return System.currentTimeMillis() - timestamp;
	}
	
	public boolean isNew() {
		return timestamp <= 0;
	}
	
	public interface OnTitleListener {
		public static String NOTFOUND = "OnTitleListener.NOTFOUND";
		public static String WORKING = "OnTitleListener.WORKING";
		public static String RELOAD = "OnTitleListener.RELOAD";
		public static String READY = "OnTitleListener.READY";
		public static String ERROR = "OnTitleListener.ERROR";
		
		void onTitleEvent(final String state, final Throwable error);
	}
}