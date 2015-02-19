package net.ggelardi.uoccin.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.WaitingUCC;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.convert.Convert;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.SimpleXMLConverter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import android.text.TextUtils;

public class TVDB {
	private static final String apiUrl = "http://thetvdb.com/api";
	private static final String apiKey = "A74D017DA5F2C3B0";
	private static API apiInstance;
	
	public interface API {
		/*
		 * http://thetvdb.com/api/GetSeries.php?seriesname=interest&language=en
		 */
		@GET("/GetSeries.php")
		List<Series> findSeries(@Query("seriesname") String text, @Query("language") String language);
		/*
		 * http://thetvdb.com/api/GetSeriesByRemoteID.php?imdbid=tt1839578&language=en
		 */
		@GET("/GetSeriesByRemoteID.php")
		void getSeriesByImdb(@Query("imdbid") String imdb_id, @Query("language") String language,
			Callback<Series> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/en.xml
		 */
		@GET("/" + apiKey + "/series/{tvdb_id}/{language}.xml")
		void getSeries(@Path("tvdb_id") int tvdb_id, @Path("language") String language, Callback<Series> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/all/en.xml
		 */
		@GET("/" + apiKey + "/series/{tvdb_id}/all/{language}.xml")
		void getFullSeries(@Path("tvdb_id") int tvdb_id, @Path("language") String language, Callback<Series> callback);
		/*
		 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/default/4/13/en.xml
		 */
		@GET("/" + apiKey + "/series/{tvdb_id}/default/{season}/{episode}/{language}.xml")
		void getEpisode(@Path("tvdb_id") int tvdb_id, @Path("season") int season, @Path("episode") int episode,
			@Path("language") String language, Callback<Episode> callback);
	}
	
	public static API getInstance() {
		if (apiInstance == null) {
			apiInstance = new RestAdapter.Builder().setEndpoint(apiUrl).
				setConverter(new SimpleXMLConverter()).
				setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						request.addHeader("User-Agent", Commons.USER_AGENT);
						//String authText = session.getUsername() + ":" + session.getRemoteKey();
						//String authData = "Basic " + Base64.encodeToString(authText.getBytes(), 0);
						//request.addHeader("Authorization", authData);
						//request.addQueryParam("locale", session.getPrefs().getString(PK.LOCALE, "en"));
					}
				}).setLogLevel(RestAdapter.LogLevel.NONE).setClient(new WaitingUCC()).build().create(API.class);
		}
		return apiInstance;
	}
	
	static class BaseType {
		@Element(name = "IMDB_ID")
		public String imdb_id;
		
		@Element(name = "Language")
		public String language;
		
		@Element(name = "Rating")
		public double rating;
		
		@Element(name = "lastupdated")
		public long lastupdated = System.currentTimeMillis();
		
		public long getAge() {
			return System.currentTimeMillis() - lastupdated;
		}
	}
	
	@Root(name = "Series")
	public static class Series extends BaseType {
		@Element(name = "id")
		public int tvdb_id;
		
		@Element(name = "SeriesName")
		public String name;

		@Element(name = "Overview")
		public String overview;

		@Element(name = "Network")
		public String network;

		@Element(name = "Status")
		public String status;

		@Element(name = "FirstAired")
		public Date firstAired;

		@Element(name = "ContentRating")
		public String contentRating;
		
		@Element(name = "Airs_DayOfWeek")
		@Convert(DayName2IntConverter.class)
		public int airsDay;

		@Element(name = "Airs_Time")
		public String airsTime;
		
		@Element(name = "Runtime")
		public int runtime;
		
		@Element(name = "poster")
		@Convert(ImageUrlConverter.class)
		public String poster;
		
		/*
		@Element(name = "banner")
		@Convert(ImageUrlConverter.class)
		public String banner;
		*/
		
		@Element(name = "fanart")
		@Convert(ImageUrlConverter.class)
		public String fanart;

		@Element(name = "zap2it_id")
		public String zap2it_id;
		
		@Element(name = "Genre")
		@Convert(PipedStringsConverter.class)
		public List<String> genres;

		@Element(name = "Actors")
		@Convert(PipedStringsConverter.class)
		public List<String> actors;
		
		@ElementList(entry="Episode", inline=true)
		public List<Episode> episodes;
	}
	
	@Root(name = "Episode")
	public static class Episode extends BaseType {
		@Element(name = "seriesid")
		public int tvdb_id;
		
		@Element(name = "seasonid")
		public int tvdb_season;
		
		@Element(name = "id")
		public int tvdb_episode;
		
		/**
		 * @return The tipical season/episode id string (es. "S02E11").
		 */
		public String signature() {
			return String.format(Locale.getDefault(), "S%1$02dE%2$02d", season, episode);
		}
		
		@Element(name = "SeasonNumber")
		public int season;
		
		@Element(name = "EpisodeNumber")
		public int episode;
		
		@Element(name = "Overview")
		public String overview;
		
		@Element(name = "FirstAired")
		public Date firstAired;
		
		@Element(name = "Director")
		public String director;

		@Element(name = "Writer")
		@Convert(PipedStringsConverter.class)
		public List<String> writers;
		
		@Element(name = "GuestStars")
		@Convert(PipedStringsConverter.class)
		public List<String> guestStars;
		
		@Element(name = "filename")
		@Convert(ImageUrlConverter.class)
		public String poster;
	}
	
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