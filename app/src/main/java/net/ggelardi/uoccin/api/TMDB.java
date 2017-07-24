package net.ggelardi.uoccin.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDB {
    private static final String apiHost = "api.themoviedb.org";
    private static final String apiUrl = "https://" + apiHost + "/3/";
    private static final String apiVer = "3";
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
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);//BODY);
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
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

    private interface API {
    }
}
