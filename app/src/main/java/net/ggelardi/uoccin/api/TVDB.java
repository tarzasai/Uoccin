package net.ggelardi.uoccin.api;

import android.content.Context;
import android.text.TextUtils;

import net.ggelardi.uoccin.serv.Commons;
import net.ggelardi.uoccin.serv.Session;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class TVDB {
	private static final String apiHost = "api.thetvdb.com";
	private static final String apiUrl = "https://" + apiHost + "/";
	private static final String apiVer = "2.1.0";
	private static final String apiKey = "A74D017DA5F2C3B0";

	private static API apiClient;
	private static Converter<ResponseBody, TVDBError> errorConverter;

	public TVDB(final Context context) {
		// http://square.github.io/retrofit/
		// https://api.thetvdb.com/swagger
		apiClient = getClient(context);
	}

	public static API getClient(Context context) {
		if (apiClient == null) {
			HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BASIC);//BODY);
			OkHttpClient httpClient = new OkHttpClient.Builder()
					.addInterceptor(logging)
					.addInterceptor(new Interceptor() {
						@Override
						public okhttp3.Response intercept(Chain chain) throws IOException {
							Request originalRequest = chain.request();
							return chain.proceed(originalRequest.newBuilder()
									.header("User-Agent", Commons.USER_AGENT)
									.build());
						}
					})
					.addInterceptor(new TVDBInterceptor(context))
					.authenticator(new TVDBAuthenticator(context))
					.connectTimeout(60, TimeUnit.SECONDS)
					.readTimeout(60, TimeUnit.SECONDS)
					.build();
			Retrofit retrofit = new Retrofit.Builder()
					.baseUrl(apiUrl)
					.addConverterFactory(GsonConverterFactory.create())
					.client(httpClient)
					.build();
			errorConverter = retrofit.responseBodyConverter(TVDBError.class, new Annotation[0]);
			apiClient = retrofit.create(API.class);
		}
		return apiClient;
	}

	public TVDBResults findSeries(String text) throws IOException {
		Response<TVDBResults> res = apiClient.findSeries(text).execute();
		if (res.code() == 404)
			return null;
		if (!res.isSuccessful())
			throw new IOException(getError(res));
		return res.body();
	}

	public TVDBSeries getSeries(int tvdb_id) throws IOException {
		Response<TVDBSeries> res = apiClient.getSeries(tvdb_id).execute();
        if (res.code() == 404)
            return null;
		if (!res.isSuccessful())
			throw new IOException(getError(res));
		return res.body();
	}

	public TVDBEpisodes getEpisodes(int tvdb_id, String season, String episode, String page) throws IOException {
		Response<TVDBEpisodes> res = apiClient.getEpisodes(tvdb_id, page, season, episode).execute();
        if (res.code() == 404)
            return null;
		if (!res.isSuccessful())
			throw new IOException(getError(res));
		for (EpisodeData tep: res.body().data)
			tep.seriesId = tvdb_id; // returned episodes don't have the seriesId set, but we need it
		return res.body();
	}

	public TVDBEpisode getEpisode(int tvdb_id) throws IOException {
		Response<TVDBEpisode> res = apiClient.getEpisode(tvdb_id).execute();
        if (res.code() == 404)
            return null;
		if (!res.isSuccessful())
            throw new IOException(getError(res));
		return res.body();
	}

	public TVDBImages getImages(int tvdb_id, String keyType, String subKey) throws IOException {
		Response<TVDBImages> res = apiClient.getImages(tvdb_id, keyType, subKey).execute();
        if (res.code() == 404)
            return null;
		if (!res.isSuccessful())
			throw new IOException(getError(res));
		return res.body();
	}

	public TVDBActors getActors(int tvdb_id) throws IOException {
		Response<TVDBActors> res = apiClient.getActors(tvdb_id).execute();
        if (res.code() == 404)
            return null;
		if (!res.isSuccessful())
			throw new IOException(getError(res));
		return res.body();
	}

	private String getError(Response response) throws IOException {
		TVDBError error = errorConverter.convert(response.errorBody());
		return error.Error;
	}

	private static class TVDBInterceptor implements Interceptor {

		private Session session;

		public TVDBInterceptor(Context context) {
			session = Session.getInstance(context);
		}

		@Override
		public okhttp3.Response intercept(Chain chain) throws IOException {
			Request request = chain.request();
			if (!apiHost.equals(request.url().host()))
				return chain.proceed(request);
			// api version
			Request.Builder builder = request.newBuilder();
			builder.header("Accept", "application/vnd.thetvdb.v" + apiVer);
			// auth token
			String token = session.lastTVDBToken();
			if (!TextUtils.isEmpty(token) && request.header("Authorization") == null)
				builder.header("Authorization", "Bearer " + token);
			return chain.proceed(builder.build());
		}
	}

	private static class TVDBAuthenticator implements Authenticator {

		private Session session;

		public TVDBAuthenticator(Context context) {
			session = Session.getInstance(context);
		}

		@Override
		public Request authenticate(Route route, okhttp3.Response response) throws IOException {
			if (response.request().url().encodedPath().equals("/login") || responseCount(response) >= 2)
				return null;
			// try login
			Call<AuthResp> loginCall = getClient(session.getContext()).login(new AuthKey(apiKey));
			retrofit2.Response<AuthResp> authResp = loginCall.execute();
			if (!authResp.isSuccessful())
				return null;
			String token = authResp.body().token;
			session.setTVDBToken(token);
			// retry original request
			return response.request().newBuilder().header("Authorization", "Bearer " + token).build();
		}

		private static int responseCount(okhttp3.Response response) {
			int result = 1;
			while ((response = response.priorResponse()) != null)
				result++;
			return result;
		}
	}

	private interface API {

		@POST("/login")
		Call<AuthResp> login(@Body AuthKey data);

		@Headers("Accept-Language: en")
		@GET("/search/series")
		Call<TVDBResults> findSeries(@Query("name") String text);

		@Headers("Accept-Language: en")
		@GET("/series/{tvdb_id}")
		Call<TVDBSeries> getSeries(@Path("tvdb_id") int tvdb_id);

		@Headers("Accept-Language: en")
		@GET("/series/{tvdb_id}/episodes/query")
		Call<TVDBEpisodes> getEpisodes(@Path("tvdb_id") int tvdb_id, @Query("page") String page,
									   @Query("airedSeason") String season, @Query("airedEpisode") String episode);

		@Headers("Accept-Language: en")
		@GET("/episodes/{tvdb_id}")
		Call<TVDBEpisode> getEpisode(@Path("tvdb_id") int tvdb_id);

		@Headers("Accept-Language: en")
		@GET("/series/{tvdb_id}/images/query")
		Call<TVDBImages> getImages(@Path("tvdb_id") int tvdb_id, @Query("keyType") String keyType,
								   @Query("subKey") String subKey);

		@GET("/series/{tvdb_id}/actors")
		Call<TVDBActors> getActors(@Path("tvdb_id") int tvdb_id);
	}

	private static class AuthKey {
		String apikey;

		public AuthKey(String ak) {
			apikey = ak;
		}
	}

	private static class AuthResp {
		String token;
	}

	public static class TVDBResults {
		public SeriesData[] data;
	}

	public static class TVDBSeries {
		public SeriesData data;
		public QueryErrors errors;
	}

	public static class TVDBEpisodes {
		public LinkData links;
		public EpisodeData[] data;

		public static class LinkData {
			public int first;
			public int last;
			public Integer next;
			public Integer prev;
		}
	}

	public static class TVDBEpisode {
		public EpisodeData data;
		public QueryErrors errors;
	}

	public static class SeriesData {
		public int id;
		public String seriesName;
		public String banner;
		public String status;
		public String firstAired;
		public String network;
		public String runtime;
		public String[] genre;
		public String[] aliases;
		public String overview;
		public String airsDayOfWeek;
		public String airsTime;
		public String rating;
		public String imdbId;
		public Double siteRating;
		public Integer siteRatingCount;
		public long lastUpdated;
	}

	public static class EpisodeData {
		public int id; // episode tvdb_id
		public int seriesId; // series tvdb_id
		public int airedSeason;
		public int airedEpisodeNumber;
		public int absoluteNumber;
		public String episodeName;
		public String firstAired;
		public String overview;
		public String[] guestStars;
		public String[] directors;
		public String[] writers;
		public String filename;
        public int thumbWidth;
        public int thumbHeight;
		public String imdbId;
		public Double siteRating;
		public Integer siteRatingCount;
		public long lastUpdated;

		public boolean special() {
			return airedSeason == 0 || airedEpisodeNumber == 0;
		}
	}

	public static class TVDBActors {
		public ActorData[] data;
		public QueryErrors errors;

		public static class ActorData {
			public int id;
			public int seriesId;
			public String name;
			public String role;
			public int sortOrder;
			public String image;
		}
	}

	public static class TVDBImages {
		public ImageData[] data;
		public QueryErrors errors;

		public static class ImageData {
			public int id;
			public String keyType;
			public String subKey;
			public String fileName;
			public String resolution;
			public String thumbnail;
		}
	}

	public static class QueryErrors {
		public String invalidLanguage;
		public String[] invalidFilters;
		public String[] invalidQueryParams;
	}

	public static class TVDBError {
		public String Error;
	}
}
