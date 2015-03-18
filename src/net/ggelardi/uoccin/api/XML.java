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

public class XML {
	
	public static class TVDB {
		private static final String apiUrl = "http://thetvdb.com/api";
		private static final String apiKey = "A74D017DA5F2C3B0";
		private static API apiInstance;
		
		public interface API {
			/*
			 * http://thetvdb.com/api/GetSeries.php?seriesname=interest&language=en
			 */
			@GET("/GetSeries.php")
			Document findSeries(@Query("seriesname") String text, @Query("language") String language);
			/*
			 * http://thetvdb.com/api/GetSeriesByRemoteID.php?imdbid=tt1839578&language=en
			 */
			@GET("/GetSeriesByRemoteID.php")
			void getSeriesByImdb(@Query("imdbid") String imdb_id, @Query("language") String language,
				Callback<Document> callback);
			/*
			 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/en.xml
			 */
			@GET("/" + apiKey + "/series/{tvdb_id}/{language}.xml")
			void getSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language, Callback<Document> callback);
			/*
			 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/all/en.xml
			 */
			@GET("/" + apiKey + "/series/{tvdb_id}/all/{language}.xml")
			void getFullSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language, Callback<Document> callback);
			/*
			 * http://thetvdb.com/api/A74D017DA5F2C3B0/series/248742/default/4/13/en.xml
			 */
			@GET("/" + apiKey + "/series/{tvdb_id}/default/{season}/{episode}/{language}.xml")
			void getEpisode(@Path("tvdb_id") String tvdb_id, @Path("season") int season, @Path("episode") int episode,
				@Path("language") String language, Callback<Document> callback);
			/*
			 * http://thetvdb.com/api/A74D017DA5F2C3B0/episodes/4099507/en.xml
			 */
			@GET("/" + apiKey + "/episodes/{tvdb_id}/{language}.xml")
			void getEpisodeById(@Path("tvdb_id") String tvdb_id, @Path("language") String language, Callback<Document> callback);
			// sync version
			@GET("/" + apiKey + "/episodes/{tvdb_id}/en.xml")
			Document getEpByIdSync(@Path("tvdb_id") String tvdb_id);
		}
		
		public static API getInstance() {
			if (apiInstance == null) {
				apiInstance = new RestAdapter.Builder().setEndpoint(apiUrl).
					//setConverter(new StringConverter()).
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
		private static final String apiUrl = "http://www.omdbapi.com/";
		private static API apiInstance;
		
		public interface API {
			/*
			 * http://www.omdbapi.com/?s=terminator&type=movie&r=xml
			 */
			@GET("/?type=movie&r=xml")
			String findMovie(@Query("s") String text);
			/*
			 * http://www.omdbapi.com/?i=tt0088247&type=movie&plot=full&r=xml
			 */
			@GET("/?plot=full&r=xml")
			void getMovie(@Query("i") String imdb_id, Callback<String> callback);
		}
		
		public static API getInstance() {
			if (apiInstance == null) {
				//Strategy strategy = new AnnotationStrategy();
				//Serializer serializer = new Persister(strategy);
				apiInstance = new RestAdapter.Builder().setEndpoint(apiUrl).
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
	}
	
	static class XmlDomConverter implements Converter {
		
		@Override
		public Object fromBody(TypedInput typedInput, Type arg1) throws ConversionException {
			try {
				DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
				DocumentBuilder bld = fac.newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(fromStream(typedInput.in())));
				Document doc = bld.parse(is);
				doc.getDocumentElement().normalize();
				return doc;
			} catch (Exception err) {
				throw new ConversionException(err);
			}
		}
		
		@Override
		public TypedOutput toBody(Object arg0) {
			// TODO Auto-generated method stub
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
}