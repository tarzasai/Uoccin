package net.ggelardi.uoccin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.Request;
import retrofit.client.UrlConnectionClient;
import retrofit.converter.SimpleXMLConverter;
import retrofit.http.GET;
import retrofit.http.Path;

public class ApiTVDB {
	private static final String API_ENDPOINT = "http://thetvdb.com/api/A74D017DA5F2C3B0";
	
	interface TVDB {
		
		@GET("/series/{tvdb_id}/all/{language}.xml")
		void getSeriesFull(@Path("tvdb_id") int tvdb_id, @Path("language") String language, Callback<Series> callback);
		
		@GET("/series/{tvdb_id}/{language}.xml")
		void getSeries(@Path("tvdb_id") int tvdb_id, @Path("language") String language, Callback<Series> callback);
		
		@GET("/series/{tvdb_id}/default/{season}/{episode}/{language}.xml")
		void getEpisode(@Path("tvdb_id") int tvdb_id, @Path("season") int season, @Path("episode") int episode,
			@Path("language") String language, Callback<Episode> callback);
		
	}
	
	static class BaseType {
		@Element(name = "id")
		int tvdb_id;
		
		@Element(name = "IMDB_ID")
		String imdb_id;
		
		@Element(name = "Language")
		String language;
		
		@Element(name = "lastupdated")
		long lastupdated = System.currentTimeMillis();
		
		public long getAge() {
			return System.currentTimeMillis() - lastupdated;
		}
	}
	
	@Root(name = "Series")
	static class Series extends BaseType {
		String SeriesName;
		String Overview;
		String Network;
		String Status;
		Date FirstAired;
		String ContentRating;
		double Rating;
		int Airs_Day;
		long Airs_Time;
		int Runtime;
		String poster;
		String banner;
		String fanart;
		String zap2it_id;
		List<String> Genres;
		List<String> Actors;
		
		List<Episode> episodes;
	}
	
	static class Episode extends BaseType {
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
	
	/*
	private static class DelimitedListConverter implements JsonDeserializer<Boolean[]> {
		@Override
		public Boolean[] deserialize(JsonElement json, Type type, JsonDeserializationContext context)
			throws JsonParseException {
			final JsonObject jo = json.getAsJsonObject();
			if (jo.has("1")) {
				List<Boolean> res = new ArrayList<Boolean>();
				String s;
				for (int i = 1; i < 100; i++) {
					s = Integer.toString(i);
					if (!jo.has(s))
						break;
					res.add(jo.getAsJsonPrimitive(s).getAsBoolean());
				}
				return res.toArray(new Boolean[res.size()]);
			}
			return null;
		}
	}
	*/
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
    <seasonid>589267</seasonid>
    <seriesid>248742</seriesid>
    <SeasonNumber>4</SeasonNumber>
    <EpisodeNumber>13</EpisodeNumber>
    <EpisodeName>M.I.A.</EpisodeName>
    <FirstAired>2015-02-03</FirstAired>
    <GuestStars></GuestStars>
    <Director>Kevin Bray</Director>
    <Writer>Lucas O'Connor</Writer>
    <Overview>Reese and Root’s hunt for Shaw takes them to a small town in upstate New York where it becomes apparent that not everything is as idyllic as it seems. Also, Fusco teams with a former POI to tackle the newest number.</Overview>
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