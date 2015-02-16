package net.ggelardi.uoccin.api;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.WaitingUCC;
import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import retrofit.http.GET;
import retrofit.http.Query;
import android.text.TextUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class OMDB {
	private static final String apiEndpoint = "http://www.omdbapi.com/";
	private static API apiInstance;
	
	public interface API {
		
		@GET("/?plot=full&r=json")
		void getMovie(@Query("i") String imdb_id, Callback<Movie> callback);
		
		// ricerche?
		
	}
	
	public static API getInstance() {
		if (apiInstance == null) {
			GsonBuilder gb = new GsonBuilder().registerTypeAdapter(List.class, new CommaStringsAdapter());
			apiInstance = new RestAdapter.Builder().setEndpoint(apiEndpoint).
				setConverter(new GsonConverter(gb.create())).
				setRequestInterceptor(new RequestInterceptor() {
					@Override
					public void intercept(RequestFacade request) {
						request.addHeader("User-Agent", Commons.USER_AGENT);
					}
				}).setLogLevel(RestAdapter.LogLevel.NONE).setClient(new WaitingUCC()).build().create(API.class);
		}
		return apiInstance;
	}
	
	public static class Movie {
		@SerializedName("imdbID")
		public String imdb_id;
		
		@SerializedName("Title")
		public String title;
		
		@SerializedName("Year")
		public String year;
		
		@SerializedName("Plot")
		public String plot;
		
		@SerializedName("Director")
		public String director;
		
		@SerializedName("Actors")
		public List<String> actors;
		
		@SerializedName("Writer")
		public List<String> writers;
		
		@SerializedName("Genre")
		public List<String> genres;
		
		@SerializedName("Released")
		public Date released;
		
		@SerializedName("Rated")
		public String rated;
		
		@SerializedName("Awards")
		public String awards;
		
		@SerializedName("Metascore")
		public int metascore;
		
		@SerializedName("imdbRating")
		public double rating;
		
		@SerializedName("imdbVotes")
		public int votes;
		
		@SerializedName("Runtime")
		public String runtime;
		
		@SerializedName("Language")
		public String language;
		
		@SerializedName("Country")
		public String country;
		
		@SerializedName("Poster")
		public String poster;

		public long lastupdated = System.currentTimeMillis();
		
		public long getAge() {
			return System.currentTimeMillis() - lastupdated;
		}
	}
	
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
}

/*

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