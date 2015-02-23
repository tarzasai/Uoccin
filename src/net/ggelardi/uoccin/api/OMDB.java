package net.ggelardi.uoccin.api;

import java.util.ArrayList;
import java.util.List;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Commons.WaitingUCC;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.SimpleXMLConverter;
import retrofit.http.GET;
import retrofit.http.Query;

public class OMDB {
	private static final String apiEndpoint = "http://www.omdbapi.com/";
	private static API apiInstance;
	
	public interface API {
		/*
		 * http://www.omdbapi.com/?s=terminator&type=movie&r=xml
		 */
		@GET("/?type=movie&r=xml")
		Data findMovie(@Query("s") String text);
		/*
		 * http://www.omdbapi.com/?i=tt0088247&type=movie&plot=full&r=xml
		 */
		@GET("/?plot=full&r=xml")
		void getMovie(@Query("i") String imdb_id, Callback<Data> callback);
	}
	
	public static API getInstance() {
		if (apiInstance == null) {
			//Strategy strategy = new AnnotationStrategy();
			//Serializer serializer = new Persister(strategy);
			apiInstance = new RestAdapter.Builder().setEndpoint(apiEndpoint).
				//setConverter(new SimpleXMLConverter(serializer)).
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

	@Root(name = "root")
	public static class Data {
		@Attribute
		public boolean response;
		@Element(required = false)
		public String error;
		
		@ElementList(entry = "movie", inline = true, required = false)
		private List<Movie> ml1;
		
		@ElementList(entry = "Movie", inline = true, required = false)
		private List<Movie> ml2;
		
		public List<Movie> movies() {
			return ml1 != null && !ml1.isEmpty() ? ml1 :
				(ml2 != null && !ml2.isEmpty() ? ml2 : new ArrayList<Movie>());
		}
	}
	
	public static class Movie {
		@Attribute
		public String imdbID;
		
		@Attribute(required = false)
		public String title;
		@Attribute(required = false)
		public String Title;

		@Attribute(required = false)
		public String year;
		@Attribute(required = false)
		public String Year;
		
		@Attribute(required = false)
		public String type;
		@Attribute(required = false)
		public String Type;
		
		@Attribute(required = false)
		public String plot;
		
		@Attribute(required = false)
		public String director;
		
		@Attribute(required = false)
		public String writer;
		
		@Attribute(required = false)
		public String actors;
		
		@Attribute(required = false)
		public String genre;
		
		@Attribute(required = false)
		public String released;
		
		@Attribute(required = false)
		public String rated;
		
		@Attribute(required = false)
		public String awards;
		
		@Attribute(required = false)
		public String metascore;
		
		@Attribute(required = false)
		public String imdbRating;
		
		@Attribute(required = false)
		public String imdbVotes;
		
		@Attribute(required = false)
		public String runtime;
		
		@Attribute(required = false)
		public String language;
		
		@Attribute(required = false)
		public String country;
		
		@Attribute(required = false)
		public String poster;
	}
	
	//@formatter:off
	/*
	// I tried converters but I wasn't able to use them...
	
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
		private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
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
	
	*/
	//@formatter:on
}

//@formatter:off
/*

<root response="True">
	<Movie Title="Terminator 2: Judgment Day" Year="1991" imdbID="tt0103064" Type="movie"/>
	<Movie Title="The Terminator" Year="1984" imdbID="tt0088247" Type="movie"/>
	<Movie Title="Terminator 3: Rise of the Machines" Year="2003" imdbID="tt0181852" Type="movie"/>
	<Movie Title="Terminator Salvation" Year="2009" imdbID="tt0438488" Type="movie"/>
	<Movie Title="Lady Terminator" Year="1989" imdbID="tt0095483" Type="movie"/>
	<Movie Title="Ninja Terminator" Year="1985" imdbID="tt0199849" Type="movie"/>
	<Movie Title="Alien Terminator" Year="1995" imdbID="tt0112320" Type="movie"/>
	<Movie Title="The Making of 'Terminator 2: Judgment Day'" Year="1991" imdbID="tt0271049" Type="movie"/>
	<Movie Title="Russian Terminator" Year="1989" imdbID="tt0100531" Type="movie"/>
	<Movie Title="The Making of 'Terminator'" Year="1984" imdbID="tt0267719" Type="movie"/>
</root>

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

*/
//@formatter:on