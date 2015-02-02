package net.ggelardi.uoccin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import retrofit.client.Request;
import retrofit.client.UrlConnectionClient;
import retrofit.converter.SimpleXMLConverter;
import retrofit.http.GET;
import retrofit.http.Path;
import android.text.TextUtils;

public class ApiTVDB {
	private static final String API_ENDPOINT = "http://thetvdb.com/api/A74D017DA5F2C3B0";
	
	public interface TVDB {
		
		@GET("/series/{tvdb_id}/all/{language}.xml")
		void getSeriesFull(@Path("tvdb_id") int tvdb_id, @Path("language") String language, Callback<Series> callback);
		
		@GET("/series/{tvdb_id}/{language}.xml")
		void getSeries(@Path("tvdb_id") int tvdb_id, @Path("language") String language, Callback<Series> callback);
		
		@GET("/series/{tvdb_id}/default/{season}/{episode}/{language}.xml")
		void getEpisode(@Path("tvdb_id") int tvdb_id, @Path("season") int season, @Path("episode") int episode,
			@Path("language") String language, Callback<Episode> callback);
		
	}
	
	public static class BaseType {
		@Element(name = "IMDB_ID")
		String imdb_id;
		
		@Element(name = "Language")
		String language;
		
		@Element(name = "Rating")
		public double rating;
		
		@Element(name = "lastupdated")
		long lastupdated = System.currentTimeMillis();
		
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
		public long airsTime;

		@Element(name = "Runtime")
		public int runtime;

		@Element(name = "poster")
		public String poster;

		@Element(name = "banner")
		public String banner;

		@Element(name = "fanart")
		public String fanart;

		@Element(name = "zap2it_id")
		public String zap2it_id;
		
		@Element(name = "Genre")
		@Convert(PipedStringsConverter.class)
		public List<String> Genres;

		@Element(name = "Actors")
		@Convert(PipedStringsConverter.class)
		public List<String> Actors;
		
		@ElementList(entry="Episode", inline=true)
		public List<Episode> episodes;
	}
	
	/*
	
  <Episode>
    <id>5050213</id>
    <seriesid>248742</seriesid>
    <seasonid>589267</seasonid>
    <SeasonNumber>4</SeasonNumber>
    <EpisodeNumber>13</EpisodeNumber>
    <EpisodeName>M.I.A.</EpisodeName>
    <Overview>Reese and Root’s hunt for Shaw takes them to a small town in upstate New York where it becomes apparent that not everything is as idyllic as it seems. Also, Fusco teams with a former POI to tackle the newest number.</Overview>
    <FirstAired>2015-02-03</FirstAired>
    <Writer>Lucas O'Connor</Writer>
    <Director>Kevin Bray</Director>
    <GuestStars></GuestStars>
    <filename>episodes/248742/5050213.jpg</filename>
    <thumb_width>400</thumb_width>
    <thumb_height>225</thumb_height>
    <EpImgFlag>2</EpImgFlag>
  </Episode>
	
	*/

	@Root(name = "Episode")
	public static class Episode extends BaseType {
		@Element(name = "seriesid")
		public int tvdb_id;
		
		@Element(name = "seasonid")
		public int season_id;
		
		@Element(name = "id")
		public int episode_id;
		
		@Element(name = "SeasonNumber")
		public int seasonNumber;
		
		@Element(name = "EpisodeNumber")
		public int episodeNumber;
		
		public String getEpisodeID() {
			return String.format("S%1$02dE%2$02d", seasonNumber, episodeNumber);
		}
		
		@Element(name = "Overview")
		public String overview;
		
		@Element(name = "FirstAired")
		public Date firstAired;

		@Element(name = "Writer")
		public String writer;
		
		@Element(name = "Director")
		public String director;
		
		@Element(name = "GuestStars")
		@Convert(PipedStringsConverter.class)
		public List<String> guestStars;
	}
	
	private static TVDB TVDB_CLIENT;
	
	public static TVDB client() {
		if (TVDB_CLIENT == null) {
			TVDB_CLIENT = new RestAdapter.Builder().setEndpoint(API_ENDPOINT).
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
				}).setLogLevel(RestAdapter.LogLevel.NONE).setClient(new WaitingUCC()).build().create(TVDB.class);
		}
		return TVDB_CLIENT;
	}
	
	static class WaitingUCC extends UrlConnectionClient {
		@Override
		protected HttpURLConnection openConnection(Request request) throws IOException {
			HttpURLConnection connection = super.openConnection(request);
			connection.setConnectTimeout(20 * 1000); // 20 sec
			connection.setReadTimeout(60 * 1000); // 60 sec
			return connection;
		}
	}
	
	static class PipedStringsConverter implements Converter<List<String>> {
		@Override
		public List<String> read(InputNode node) throws Exception {
			return !node.isEmpty() ? Arrays.asList(node.getValue().split("\\|")) : new ArrayList<String>();
		}
		@Override
		public void write(OutputNode node, List<String> value) throws Exception {
			node.setValue("|" + TextUtils.join("|", value) + "|");
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

<Data>
  <Series>
    <id>248742</id>
    <IMDB_ID>tt1839578</IMDB_ID>
    <Language>en</Language>
    <lastupdated>1422348908</lastupdated>
    <SeriesName>Person of Interest</SeriesName>
    <Overview>Person of Interest is an American crime drama television series broadcasting on CBS. It is based on a screenplay developed by Jonathan Nolan. The series revolves around a former CIA officer (Jim Caviezel) recruited by a mysterious billionaire (Michael Emerson) to prevent violent crimes in New York City.</Overview>
    <FirstAired>2011-09-22</FirstAired>
    <Network>CBS</Network>
    <Status>Continuing</Status>
    <Airs_DayOfWeek>Tuesday</Airs_DayOfWeek>
    <Airs_Time>10:00 PM</Airs_Time>
    <Runtime>60</Runtime>
    <Rating>8.9</Rating>
    <ContentRating>TV-14</ContentRating>
    <zap2it_id>EP01419847</zap2it_id>
    <poster>posters/248742-15.jpg</poster>
    <banner>graphical/248742-g11.jpg</banner>
    <fanart>fanart/original/248742-21.jpg</fanart>
    <Genre>|Action|Adventure|Drama|Mystery|Thriller|</Genre>
    <Actors>|Michael Emerson|Jim Caviezel|Kevin Chapman|Amy Acker|Sarah Shahi|Enrico Colantoni|Taraji P. Henson|</Actors>
  </Series>
</Data>

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
  ...
</Data>

========================================================================================================================
EPISODE:
http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/default/4/13/en.xml
========================================================================================================================

<Data>
  <Episode>
    <id>5050213</id>
    <seriesid>248742</seriesid>
    <seasonid>589267</seasonid>
    <SeasonNumber>4</SeasonNumber>
    <EpisodeNumber>13</EpisodeNumber>
    <EpisodeName>M.I.A.</EpisodeName>
    <Overview>Reese and Root’s hunt for Shaw takes them to a small town in upstate New York where it becomes apparent that not everything is as idyllic as it seems. Also, Fusco teams with a former POI to tackle the newest number.</Overview>
    <FirstAired>2015-02-03</FirstAired>
    <Writer>Lucas O'Connor</Writer>
    <Director>Kevin Bray</Director>
    <GuestStars></GuestStars>
    <lastupdated>1421964803</lastupdated>
    <filename>episodes/248742/5050213.jpg</filename>
    <thumb_width>400</thumb_width>
    <thumb_height>225</thumb_height>
    <IMDB_ID>tt4157140</IMDB_ID>
    <EpImgFlag>2</EpImgFlag>
    <Rating>0</Rating>
    <Language>en</Language>
  </Episode>
</Data>


*/