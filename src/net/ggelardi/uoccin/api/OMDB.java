package net.ggelardi.uoccin.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.WaitingUCC;

import org.simpleframework.xml.Attribute;
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
import retrofit.http.Query;
import android.text.TextUtils;
import android.util.Log;

public class OMDB {
	private static final String apiEndpoint = "http://www.omdbapi.com/";
	private static API apiInstance;
	
	public interface API {
		/*
		 * http://www.omdbapi.com/?s=terminator&type=movie&r=xml
		 */
		@GET("/?type=movie&r=xml")
		Movies findMovie(@Query("s") String text);
		/*
		 * http://www.omdbapi.com/?i=tt0088247&type=movie&plot=full&r=xml
		 */
		@GET("/?plot=full&r=xml")
		void getMovie(@Query("i") String imdb_id, Callback<Movie> callback);
	}
	
	public static API getInstance() {
		if (apiInstance == null) {
			/*
			// converters
			GsonBuilder gb = new GsonBuilder();
			gb.registerTypeAdapter(List.class, new CommaStringsAdapter());
			gb.registerTypeAdapter(Date.class, new StringDateAdapter());
			gb.registerTypeAdapter(Integer.class, new MinutesStringAdapter());
			*/
			// retrofit client
			apiInstance = new RestAdapter.Builder().setEndpoint(apiEndpoint).
				//setConverter(new GsonConverter(gb.create())).
				setConverter(new SimpleXMLConverter()).
				setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						request.addHeader("User-Agent", Commons.USER_AGENT);
					}
				}).setLogLevel(RestAdapter.LogLevel.BASIC).setClient(new WaitingUCC()).build().create(API.class);
		}
		return apiInstance;
	}
	
	@Root(name = "movie")
	public static class Movie {
		//@SerializedName("imdbID")
		@Attribute(name = "imdbID")
		public String imdb_id;
		
		//@SerializedName("Title")
		@Attribute(name = "Title")
		public String title;
		
		//@SerializedName("Year")
		@Attribute(name = "Year")
		public int year;
		
		//@SerializedName("Plot")
		@Attribute(name = "Plot")
		public String plot;
		
		//@SerializedName("Director")
		@Attribute(name = "Director")
		public String director;
		
		//@SerializedName("Actors")
		@Attribute(name = "Actors")
		@Convert(CommaStringsConverter.class)
		public List<String> actors;
		
		//@SerializedName("Writer")
		@Attribute(name = "Writer")
		@Convert(CommaStringsConverter.class)
		public List<String> writers;
		
		//@SerializedName("Genre")
		@Attribute(name = "Genre")
		@Convert(CommaStringsConverter.class)
		public List<String> genres;
		
		//@SerializedName("Released")
		@Attribute(name = "Released")
		@Convert(DateStringConverter.class)
		public Date released;
		
		//@SerializedName("Rated")
		@Attribute(name = "Rated")
		public String rated;
		
		//@SerializedName("Awards")
		@Attribute(name = "Awards")
		public String awards;
		
		//@SerializedName("Metascore")
		@Attribute(name = "Metascore")
		public int metascore;
		
		//@SerializedName("imdbRating")
		@Attribute(name = "imdbRating")
		public double imdbRating;
		
		//@SerializedName("imdbVotes")
		@Attribute(name = "imdbVotes")
		public int imdbVotes;
		
		//@SerializedName("Runtime")
		@Attribute(name = "Runtime")
		@Convert(MinutesStringConverter.class)
		public int runtime;
		
		//@SerializedName("Language")
		@Attribute(name = "Language")
		public String language;
		
		//@SerializedName("Country")
		@Attribute(name = "Country")
		public String country;
		
		//@SerializedName("Poster")
		@Attribute(name = "Poster")
		public String poster;
	}
	
	@Root(name = "root")
	public class Movies {
		@ElementList(entry = "Movie", inline = true)
		public List<Movie> movies;
	}
	
	/*
	public static class Search {
		@SerializedName("Search")
		public List<Movie> results;
	}
	*/
	
	static class CommaStringsConverter implements Converter<List<String>> {
		@Override
		public List<String> read(InputNode node) throws Exception {
			return !node.isEmpty() ? Arrays.asList(node.getValue().split(",")) : new ArrayList<String>();
		}
		@Override
		public void write(OutputNode node, List<String> value) throws Exception {
			if (value == null)
				node.remove();
			else
				node.setValue(TextUtils.join(", ", value));
		}
	}
	
	static class MinutesStringConverter implements Converter<Integer> {
		@Override
		public Integer read(InputNode node) throws Exception {
			if (!node.isEmpty())
				try {
					if (node.getValue().equals("N/A"))
						return 0;
					if (node.getValue().endsWith(" min"))
						return Integer.parseInt(node.getValue().split("\\s")[0]);
					return Integer.parseInt(node.getValue());
				} catch (Exception err) {
					Log.e("MinutesStringConverter", node.getValue(), err);
					err.printStackTrace();
				}
			return null;
		}
		@Override
		public void write(OutputNode node, Integer value) throws Exception {
			node.setValue(Integer.toString(value) + " min");
		}
	}
	
	static class DateStringConverter implements Converter<Date> {
		private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
		@Override
		public Date read(InputNode node) throws Exception {
			if (!node.isEmpty() && !node.getValue().equals("N/A"))
				try {
					return df.parse(node.getValue());
				} catch (Exception err) {
					Log.e("DateStringConverter", node.getValue(), err);
					err.printStackTrace();
				}
			return null;
		}
		@Override
		public void write(OutputNode node, Date value) throws Exception {
			if (value == null)
				node.remove();
			else
				node.setValue(df.format(value));
		}
	}
	
	/*
	
	static class CommaStringsAdapter extends TypeAdapter<List<String>> {
		@Override
		public List<String> read(JsonReader reader) throws IOException {
			String value = "";
			if (reader.peek() != JsonToken.NULL)
				value = reader.nextString();
			else
				reader.nextNull();
			return Arrays.asList(value.split(","));
		}
		@Override
		public void write(JsonWriter writer, List<String> value) throws IOException {
			if (value == null)
				writer.nullValue();
			else
				writer.value(TextUtils.join(", ", value));
		}
	}
	
	static class MinutesStringAdapter extends TypeAdapter<Integer> {
		@Override
		public Integer read(JsonReader reader) throws IOException {
			if (reader.peek() != JsonToken.NULL) {
				String text = reader.nextString();
				try {
					if (text.equals("N/A"))
						return 0;
					if (text.endsWith(" min"))
						return Integer.parseInt(text.split("\\s")[0]);
					return Integer.parseInt(text);
				} catch (Exception err) {
					Log.e("MinutesStringAdapter", text, err);
					err.printStackTrace();
				}
			} else
				reader.nextNull();
			return null;
		}
		@Override
		public void write(JsonWriter writer, Integer value) throws IOException {
			if (value == null)
				writer.nullValue();
			else
				writer.value(Integer.toString(value) + " min");
		}
	}
	
	static class StringDateAdapter extends TypeAdapter<Date> {
		private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
		@Override
		public Date read(JsonReader reader) throws IOException {
			if (reader.peek() != JsonToken.NULL) {
				String text = reader.nextString();
				if (text.equals("N/A"))
					return null;
				try {
					return df.parse(text);
				} catch (Exception err) {
					Log.e("StringDateAdapter", text, err);
					err.printStackTrace();
				}
			} else
				reader.nextNull();
			return null;
		}
		@Override
		public void write(JsonWriter writer, Date value) throws IOException {
			if (value == null)
				writer.nullValue();
			else
				writer.value(df.format(value));
		}
	}
	
	*/
}

/*

<?xml version="1.0" encoding="UTF-8"?>
<root response="True">
	<movie
		title="The Terminator"
		year="1984"
		rated="R"
		released="26 Oct 1984"
		runtime="107 min"
		genre="Action, Sci-Fi"
		director="James Cameron"
		writer="James Cameron, Gale Anne Hurd, William Wisher Jr. (additional dialogue)"
		actors="Arnold Schwarzenegger, Michael Biehn, Linda Hamilton, Paul Winfield"
		plot="A cyborg is sent from the future on a deadly mission. He has to kill Sarah Connor, a young woman whose life will have a great significance in years to come. Sarah has only one protector - Kyle Reese - also sent from the future. The Terminator uses his exceptional intelligence and strength to find Sarah, but is there any way to stop the seemingly indestructible cyborg ?"
		language="English, Spanish"
		country="UK, USA"
		awards="5 wins &amp; 6 nominations."
		poster="http://ia.media-imdb.com/images/M/MV5BODE1MDczNTUxOV5BMl5BanBnXkFtZTcwMTA0NDQyNA@@._V1_SX300.jpg"
		metascore="84"
		imdbRating="8.1"
		imdbVotes="478,116"
		imdbID="tt0088247"
		type="movie"/>
</root>

{
	imdbID: "tt0816692",
	Title: "Interstellar",
	Year: "2014",
	Plot: "A team of explorers travel through a wormhole in an attempt to ensure humanity's survival.",
	Genre: "Adventure, Sci-Fi",
	Writer: "Jonathan Nolan, Christopher Nolan",
	Director: "Christopher Nolan",
	Actors: "Ellen Burstyn, Matthew McConaughey, Mackenzie Foy, John Lithgow",
	Rated: "PG-13",
	Runtime: "169 min",
	Language: "English",
	Poster: "http://ia.media-imdb.com/images/M/MV5BMjIxNTU4MzY4MF5BMl5BanBnXkFtZTgwMzM4ODI3MjE@._V1_SX300.jpg"
	
	Country: "USA, UK",
	Released: "07 Nov 2014",
	Awards: "Nominated for 1 Golden Globe. Another 13 wins & 38 nominations.",
	Metascore: "74",
	imdbRating: "8.9",
	imdbVotes: "396,137",
}

*/