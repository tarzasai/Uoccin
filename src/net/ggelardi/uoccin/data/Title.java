package net.ggelardi.uoccin.data;

import java.util.ArrayList;
import java.util.List;

public class Title {
	
	protected static final List<OnTitleListener> listeners = new ArrayList<OnTitleListener>();
	
	public static boolean ongoingServiceOperation = false;
	
	public static void addOnTitleEventListener(OnTitleListener aListener) {
		listeners.add(aListener);
	}
	
	public static void dispatch(String state, Throwable error) {
		for (OnTitleListener listener: listeners)
			//if (listener != null)
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