package net.ggelardi.uoccin.data;

import java.util.List;
import java.util.Map;

public class UFile {
	
	public Map<String, UMovie> movies;
	public Map<String, USeries> series;

	public static class UMovie {
		public String name;
		public boolean watchlist;
		public boolean collected;
		public boolean watched;
		public int rating;
		public List<String> tags;
		public List<String> subtitles;
	}
	
	public static class USeries {
		public String name;
		public boolean watchlist;
		public int rating;
		public List<String> tags;
		public Map<String, Map<String, List<String>>> collected;
		public Map<String, List<Integer>> watched;
	}
}