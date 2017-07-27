package net.ggelardi.uoccin.api;

import android.content.Context;
import android.util.Log;

import net.ggelardi.uoccin.serv.Commons;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Path;

public class TMDB {
    private static final String apiHost = "api.themoviedb.org";
    private static final String apiUrl = "https://" + apiHost + "/";
    private static final String apiKey = "4cb75e343ed38c533e19f547c44cf5d0";

    private static API apiClient;

    public TMDB(Context context) {
        // http://square.github.io/retrofit/
        // https://developers.themoviedb.org/3/
        apiClient = getClient(context);
    }

    public static API getClient(Context context) {
        if (apiClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);//BASIC|BODY
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public okhttp3.Response intercept(Chain chain) throws IOException {
                            Request originalRequest = chain.request();
                            okhttp3.Response response = chain.proceed(originalRequest.newBuilder()
                                    .header("User-Agent", Commons.USER_AGENT).build());
                            // https://developers.themoviedb.org/3/getting-started/request-rate-limiting
                            int rl = Integer.parseInt(response.headers().get("X-RateLimit-Remaining"));
                            long rs = Long.parseLong(response.headers().get("X-RateLimit-Reset"));
                            long wt = rl < 5 ? (rs - (System.currentTimeMillis() / 1000) + 750) :
                                    rl < 10 ? 300 : rl < 20 ? 150 : rl < 30 ? 50 : 25;
                            Log.d("TMDB", String.format(Locale.getDefault(), "intercept rl: %d - wt: %d", rl, wt));
                            try {
                                Thread.sleep(wt);
                            } catch (Exception err) {
                                Log.e("TMDB", "intercept", err);
                            }
                            return response;
                        }
                    })
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(apiUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
            apiClient = retrofit.create(API.class);
        }
        return apiClient;
    }

    public TMDBMovies findMovie(String text) throws IOException, InterruptedException {
        Response<TMDBMovies> res = apiClient.search(apiKey, text, 1).execute();
        if (res.code() == 404)
            return null;
        if (!res.isSuccessful())
            throw new IOException(res.message());
        return res.body();
    }

    public int findId(String imdb_id) {
        try {
            Response<TMDBFind> res = apiClient.getId(imdb_id, apiKey).execute();
            if (!res.isSuccessful())
                throw new IOException(res.message());
            TMDBFind tmp = res.body();
            if (tmp.movie_results == null || tmp.movie_results.length < 1)
                return 0;
            if (tmp.movie_results.length > 1)
                throw new IOException("Too many matches: " + Integer.toString(tmp.movie_results.length));
            return tmp.movie_results[0].id;
        } catch (Exception err) {
            Log.e("TMDB", "findId", err);
            return 0;
        }
    }

    public MovieData getMovie(int tmdb_id) throws IOException {
        Response<MovieData> res = apiClient.getMovie(tmdb_id, apiKey).execute();
        if (res.code() == 404)
            return null;
        if (!res.isSuccessful())
            throw new IOException(res.message());
        return res.body();
    }

    public TMDBPeople getPeople(int tmdb_id) throws IOException {
        Response<TMDBPeople> res = apiClient.getPeople(tmdb_id, apiKey).execute();
        if (res.code() == 404)
            return null;
        if (!res.isSuccessful())
            throw new IOException(res.message());
        return res.body();
    }

    private interface API {
        @GET("/3/search/movie?language=en-US&include_adult=false")
        Call<TMDBMovies> search(@Query("api_key") String api_key, @Query("query") String query, @Query("page") int page);

        @GET("/3/find/{imdb_id}?language=en-US&external_source=imdb_id")
        Call<TMDBFind> getId(@Path("imdb_id") String imdb_id, @Query("api_key") String api_key);

        @GET("/3/movie/{tmdb_id}?language=en-US")
        Call<MovieData> getMovie(@Path("tmdb_id") int tmdb_id, @Query("api_key") String api_key);

        @GET("/3/movie/{tmdb_id}/credits")
        Call<TMDBPeople> getPeople(@Path("tmdb_id") int tmdb_id, @Query("api_key") String api_key);
    }

    public static class MovieData {
        public int id;
        public String title;
        public String overview;
        public String poster_path;
        public String backdrop_path;
        public String original_title;
        public String original_language;
        public String release_date;
        public Double vote_average;
        public Integer vote_count;
        public GenreData[] genres;
        public Iso31661[] spoken_languages;
        public Iso31661[] production_countries;
        public String status;
        public Integer runtime;
        public Boolean adult;
        //
        public String imdb_id;

        public static class GenreData {
            public int id;
            public String name;
        }

        public static class Iso31661 {
            public String iso_3166_1;
            public String name;
        }
    }

    public static class TMDBFind {
        public MovieData[] movie_results;
    }

    public static class TMDBMovies {
        public int total_results;
        public int total_pages;
        public int page;
        public MovieData[] results;
    }

    public static class TMDBPeople {
        public int id; // movie id
        public PeopleData[] cast;
        public PeopleData[] crew;

        public static class PeopleData {
            public int id;
            public String name;
            public String job; // if crew
            public String character; // if cast
            public Integer gender; // if cast
            public String profile_path;
        }
    }
}
