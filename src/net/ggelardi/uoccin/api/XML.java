package net.ggelardi.uoccin.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Type;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.WaitingUCC;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class XML {
	
	public static class TVDB {
		private static final String apiUrl = "http://thetvdb.com/api";
		private static final String apiKey = "A74D017DA5F2C3B0";
		private static API apiInstance;
		
		public interface API {
			@GET("/GetSeries.php")
			Document findSeries(@Query("seriesname") String text, @Query("language") String language);
			
			@GET("/" + apiKey + "/series/{tvdb_id}/{language}.xml")
			Document getSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language);
			
			@GET("/" + apiKey + "/series/{tvdb_id}/all/{language}.xml")
			Document getFullSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language);
			
			@GET("/" + apiKey + "/series/{tvdb_id}/default/{season}/{episode}/{language}.xml")
			Document getEpisode(@Path("tvdb_id") String tvdb_id, @Path("season") int season,
				@Path("episode") int episode, @Path("language") String language);
			
			@GET("/" + apiKey + "/episodes/{tvdb_id}/{language}.xml")
			Document getEpisodeById(@Path("tvdb_id") String tvdb_id, @Path("language") String language);
		}
		
		public static API getInstance() {
			if (apiInstance == null) {
				apiInstance = new RestAdapter.Builder().setEndpoint(apiUrl).
					setConverter(new XmlDomConverter()).
					setRequestInterceptor(new RequestInterceptor() {
						@Override
						public void intercept(RequestFacade request) {
							request.addHeader("User-Agent", Commons.USER_AGENT);
						}
					}).setLogLevel(RestAdapter.LogLevel.BASIC).setClient(new WaitingUCC()).build().create(API.class);
			}
			return apiInstance;
		}
	}
	
	public static class OMDB {
		private static final String apiUrl = "http://www.omdbapi.com";
		private static API apiInstance;
		
		public interface API {
			@GET("/?type=movie&r=xml")
			Document findMovie(@Query("s") String text);
			
			@GET("/?type=movie&r=xml&plot=full&tomatoes=true")
			Document getMovie(@Query("i") String imdb_id);
		}
		
		public static API getInstance() {
			if (apiInstance == null) {
				apiInstance = new RestAdapter.Builder().setEndpoint(apiUrl).
					setConverter(new XmlDomConverter()).
					setRequestInterceptor(new RequestInterceptor() {
						@Override
						public void intercept(RequestFacade request) {
							request.addHeader("User-Agent", Commons.USER_AGENT);
						}
					}).setLogLevel(RestAdapter.LogLevel.BASIC).setClient(new WaitingUCC()).build().create(API.class);
			}
			return apiInstance;
		}
	}
	
	static class XmlDomConverter implements Converter {
		private static final String newLine = System.getProperty("line.separator");
		
		@Override
		public Object fromBody(TypedInput body, Type type) throws ConversionException {
			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(fromStream(body.in())));
				Document doc = db.parse(is);
				doc.getDocumentElement().normalize();
				return doc;
			} catch (Exception err) {
				throw new ConversionException(err);
			}
		}
		
		@Override
		public TypedOutput toBody(Object source) {
			return null;
		}
		
		public static String fromStream(InputStream stream) throws IOException {
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line).append(newLine);
			return sb.toString();
		}
	}
}