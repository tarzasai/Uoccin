package net.ggelardi.uoccin.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.WaitingUCC;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class TVDB {
	private static final String apiUrl = "http://thetvdb.com/api";
	private static final String apiKey = "A74D017DA5F2C3B0";
	private static API apiInstance;
	
	public static String preferredLanguage = Locale.getDefault().getLanguage();
	
	public interface API {
		/*
		 * http://thetvdb.com/api/GetSeries.php?seriesname=interest&language=en
		 */
		@GET("/GetSeries.php")
		String findSeries(@Query("seriesname") String text, @Query("language") String language);
		/*
		 * http://thetvdb.com/api/GetSeriesByRemoteID.php?imdbid=tt1839578&language=en
		 */
		@GET("/GetSeriesByRemoteID.php")
		void getSeriesByImdb(@Query("imdbid") String imdb_id, @Query("language") String language,
			Callback<Data> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/en.xml
		 */
		@GET("/" + apiKey + "/series/{tvdb_id}/{language}.xml")
		void getSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language, Callback<Data> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/all/en.xml
		 */
		@GET("/" + apiKey + "/series/{tvdb_id}/all/{language}.xml")
		void getFullSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language, Callback<Data> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/default/4/13/en.xml
		 */
		@GET("/" + apiKey + "/series/{tvdb_id}/default/{season}/{episode}/{language}.xml")
		void getEpisode(@Path("tvdb_id") String tvdb_id, @Path("season") int season, @Path("episode") int episode,
			@Path("language") String language, Callback<Data> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/episodes/4099507/en.xml
		 */
		@GET("/" + apiKey + "/episodes/{tvdb_id}/{language}.xml")
		void getEpisodeById(@Path("tvdb_id") String tvdb_id, @Path("language") String language, Callback<Data> callback);
	}
	
	public static API getInstance() {
		if (apiInstance == null) {
			apiInstance = new RestAdapter.Builder().setEndpoint(apiUrl).
				//setConverter(new SimpleXMLConverter()).
				setConverter(new StringConverter()).
				setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						request.addHeader("User-Agent", Commons.USER_AGENT);
					}
				}).setLogLevel(RestAdapter.LogLevel.BASIC).setClient(new WaitingUCC()).build().create(API.class);
		}
		return apiInstance;
	}
	
	@Root(name = "Data")
	public static class Data {
		/*
		@Element(name = "Series", required = false)
		private Series s1;
		*/
		
		@ElementList(name = "Series", inline = true, required = false)
		private List<TVDBSeries> s2;
		
		public List<TVDBSeries> series() {
			List<TVDBSeries> res = s2;
			/*if (res == null || res.isEmpty()) {
				res = new ArrayList<Series>();
				if (s1 != null)
					res.add(s1);
			}*/
			return res;
		}
		
		@ElementList(name = "Episode", inline = true, required = false)
		public List<Episode> episodes;
	}
	
	static class BaseType {
		@Element(name = "id")
		public String tvdb_id;
		
		@Element(name = "IMDB_ID", required = false)
		public String imdb_id;

		@Element(required = false)
		public String seriesid;

		@Element(required = false)
		public String language;
		
		@Element(required = false)
		public String Language;

		@Element(required = false)
		private String SeriesID;
		
		@Element(required = false)
		private double Rating;
		
		@Element(required = false)
		private int RatingCount;
		
		@Element(required = false)
		private long lastupdated;
	}
	
	@Root
	public static class TVDBSeries extends BaseType {
		@Element(name = "SeriesName")
		public String name;

		@Element(name = "Overview", required = false)
		public String overview;

		@Element(name = "Network", required = false)
		public String network;

		@Element(name = "Status", required = false)
		public String status;

		@Element(name = "FirstAired", required = false)
		public String firstAired;

		@Element(name = "ContentRating", required = false)
		public String contentRating;
		
		@Element(name = "Airs_DayOfWeek", required = false)
		public String airsDay;

		@Element(name = "Airs_Time", required = false)
		public String airsTime;
		
		@Element(name = "Runtime", required = false)
		public int runtime;
		
		@Element(required = false)
		public String banner;
		
		@Element(required = false)
		public String poster;
		
		@Element(required = false)
		public String fanart;
		
		@Element(name = "Genre", required = false)
		public String genres;

		@Element(name = "Actors", required = false)
		public String actors;

		@Element(required = false)
		private String zap2it_id;
	}
	
	public static class Episode extends BaseType {
		@Element(name = "SeasonNumber")
		public int season;
		
		@Element(name = "EpisodeNumber")
		public int episode;
		
		@Element(name = "EpisodeName")
		public String name;
		
		@Element(name = "Overview")
		public String overview;
		
		@Element(name = "FirstAired")
		public String firstAired;
		
		@Element(name = "Director")
		public String director;

		@Element(name = "Writer")
		public String writers;
		
		@Element(name = "GuestStars")
		public String guestStars;
		
		@Element(name = "filename")
		public String poster;
		
		@Element(name = "seasonid")
		public int tvdb_season;
	}
	
	static class StringConverter implements Converter {
		@Override
		public Object fromBody(TypedInput typedInput, Type arg1) throws ConversionException {
			String text = null;
	        try {
	            text = fromStream(typedInput.in());
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return text;
		}
		@Override
		public TypedOutput toBody(Object arg0) {
			return null;
		}
		// Custom method to convert stream from request to string
	    public static String fromStream(InputStream in) throws IOException {
	        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	        StringBuilder out = new StringBuilder();
	        String newLine = System.getProperty("line.separator");
	        String line;
	        while ((line = reader.readLine()) != null) {
	            out.append(line);
	            out.append(newLine);
	        }
	        return out.toString();
	    }
	}
	
	//@formatter:off
	/*
	// I tried converters but I wasn't able to use them...
	
	static class ImageUrlConverter implements Converter<String> {
		private static String bannerUrl = "http://thetvdb.com/banners/";
		@Override
		public String read(InputNode node) throws Exception {
			return node.isEmpty() ? "" : bannerUrl + node.getValue();
		}
		@Override
		public void write(OutputNode node, String value) throws Exception {
			if (value == null)
				node.remove();
			else if (value.startsWith(bannerUrl))
				node.setValue(value.substring(bannerUrl.length()));
			else
				node.setValue(value);
		}
	}
	
	static class PipedStringsConverter implements Converter<List<String>> {
		@Override
		public List<String> read(InputNode node) throws Exception {
			return !node.isEmpty() ? Arrays.asList(node.getValue().split("\\|")) : new ArrayList<String>();
		}
		@Override
		public void write(OutputNode node, List<String> value) throws Exception {
			node.setValue(TextUtils.join("|", value));
		}
	}
	
	static class DayName2IntConverter implements Converter<Integer> {
		private static final List<String> days = Arrays.asList("sunday", "monday", "tuesday", "wednesday", "thursday",
			"friday", "saturday");
		@Override
		public Integer read(InputNode node) throws Exception {
			return days.indexOf(node.isEmpty() ? "null" : node.getValue().toLowerCase(Locale.getDefault())) + 1;
		}
		@Override
		public void write(OutputNode node, Integer value) throws Exception {
			if (value < 0)
				node.remove(); //node.setValue(""); ???
			else {
				String d = days.get(value-1);
				node.setValue(d.substring(0, 0).toUpperCase(Locale.getDefault()) + d.substring(1));
			}
		}
	}
	
	*/
	//@formatter:on
}

/*

========================================================================================================================
SERIES SUMMARY:
http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/en.xml
========================================================================================================================

<Series>
	<id>248742</id>
	<Actors>|Michael Emerson|Jim Caviezel|Kevin Chapman|Amy Acker|Sarah Shahi|Enrico Colantoni|Taraji P. Henson|</Actors>
	<Airs_DayOfWeek>Tuesday</Airs_DayOfWeek>
	<Airs_Time>10:00 PM</Airs_Time>
	<ContentRating>TV-14</ContentRating>
	<FirstAired>2011-09-22</FirstAired>
	<Genre>|Action|Adventure|Drama|Mystery|Thriller|</Genre>
	<IMDB_ID>tt1839578</IMDB_ID>
	<Language>en</Language>
	<Network>CBS</Network>
	<NetworkID/>
	<Overview>Person of Interest is an American crime drama television series broadcasting on CBS. It is...</Overview>
	<Rating>8.9</Rating>
	<RatingCount>223</RatingCount>
	<Runtime>60</Runtime>
	<SeriesID>80967</SeriesID>
	<SeriesName>Person of Interest</SeriesName>
	<Status>Continuing</Status>
	<added>2011-05-14 07:42:59</added>
	<addedBy>235881</addedBy>
	<banner>graphical/248742-g11.jpg</banner>
	<fanart>fanart/original/248742-21.jpg</fanart>
	<lastupdated>1423834521</lastupdated>
	<poster>posters/248742-15.jpg</poster>
	<tms_wanted_old>1</tms_wanted_old>
	<zap2it_id>EP01419847</zap2it_id>
</Series>

========================================================================================================================
EPISODE:
http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/default/4/13/en.xml
========================================================================================================================

<Episode>
	<id>5050213</id>
	<seasonid>589267</seasonid>
	<EpisodeNumber>13</EpisodeNumber>
	<EpisodeName>M.I.A.</EpisodeName>
	<FirstAired>2015-02-03</FirstAired>
	<GuestStars/>
	<Director>Kevin Bray</Director>
	<Writer>Lucas O'Connor</Writer>
	<Overview>Reese and Root’s hunt for Shaw takes them to a small town in upstate New York where it...</Overview>
	<ProductionCode/>
	<lastupdated>1423651313</lastupdated>
	<flagged>0</flagged>
	<DVD_discid/>
	<DVD_season/>
	<DVD_episodenumber/>
	<DVD_chapter/>
	<absolute_number/>
	<filename>episodes/248742/5050213.jpg</filename>
	<seriesid>248742</seriesid>
	<thumb_added>2015-01-22 14:13:23</thumb_added>
	<thumb_width>400</thumb_width>
	<thumb_height>225</thumb_height>
	<tms_export/>
	<mirrorupdate>2015-02-11 02:41:56</mirrorupdate>
	<IMDB_ID>tt4157140</IMDB_ID>
	<EpImgFlag>2</EpImgFlag>
	<Rating>7.3</Rating>
	<SeasonNumber>4</SeasonNumber>
	<Language>en</Language>
</Episode>

========================================================================================================================
SERIES FULL:
http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/all/en.xml
========================================================================================================================

<Data>
  <Series>
    ...series data...
  </Series>
  <Episode>
    ...episode data...
  </Episode>
  <Episode>
    ...episode data...
  </Episode>
  <Episode>
    ...episode data...
  </Episode>
  ...more episodes...
</Data>

========================================================================================================================
RICERCA:
http://thetvdb.com/api/GetSeries.php?seriesname=interest
========================================================================================================================

<Data>
	<Series>
		<seriesid>274762</seriesid>
		<language>en</language>
		<SeriesName>Prime Interest</SeriesName>
		<Overview>Broadcast live from Washington, D.C., Prime Interest navigates the financial and...</Overview>
		<Network>RT</Network>
		<id>274762</id>
	</Series>
	<Series>
		<seriesid>248742</seriesid>
		<language>en</language>
		<SeriesName>Person of Interest</SeriesName>
		<banner>graphical/248742-g11.jpg</banner>
		<Overview>Person of Interest is an American crime drama television series broadcasting on CBS...</Overview>
		<FirstAired>2011-09-22</FirstAired>
		<Network>CBS</Network>
		<IMDB_ID>tt1839578</IMDB_ID>
		<zap2it_id>EP01419847</zap2it_id>
		<id>248742</id>
	</Series>
	<Series>
		<seriesid>277040</seriesid>
		<language>en</language>
		<SeriesName>Persons of Interest - The ASIO files</SeriesName>
		<banner>graphical/277040-g.jpg</banner>
		<Overview>In each episode of this new Walkley-nominated series, a 'person of interest' is given...</Overview>
		<FirstAired>2014-01-07</FirstAired>
		<Network>SBS</Network>
		<id>277040</id>
	</Series>
</Data>

*/