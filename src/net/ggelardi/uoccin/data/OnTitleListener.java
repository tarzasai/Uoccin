package net.ggelardi.uoccin.data;

public interface OnTitleListener {
	public static String NOTFOUND = "TitleEvent.NOTFOUND";
	public static String LOADING = "TitleEvent.LOADING";
	public static String READY = "TitleEvent.READY";
	public static String ERROR = "TitleEvent.ERROR";
	
	void changed(String state, Throwable error);
}