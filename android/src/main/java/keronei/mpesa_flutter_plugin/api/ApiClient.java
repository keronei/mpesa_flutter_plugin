package keronei.mpesa_flutter_plugin.api;

import android.util.Log;

import keronei.mpesa_flutter_plugin.MpesaFlutterPlugin;
import keronei.mpesa_flutter_plugin.api.interceptor.AccessTokenInterceptor;
import keronei.mpesa_flutter_plugin.api.interceptor.AuthInterceptor;
import keronei.mpesa_flutter_plugin.api.services.STKPushService;

import java.io.Console;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static keronei.mpesa_flutter_plugin.helper.Basics.CONNECT_TIMEOUT;
import static keronei.mpesa_flutter_plugin.helper.Basics.READ_TIMEOUT;
import static keronei.mpesa_flutter_plugin.helper.Basics.WRITE_TIMEOUT;

/**
 * API Client helper class used to configure Retrofit object.
 *
 * @author Thomas Kioko
 */

public class ApiClient {

    private Retrofit retrofit;
    private boolean isDebug;
    private boolean isGetAccessToken;
    private HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();

    /**
     * Set the {@link Retrofit} log level. This allows one to view network traffic.
     *
     * @param isDebug If true, the log level is set to
     *                {@link HttpLoggingInterceptor.Level#BODY}. Otherwise
     *                {@link HttpLoggingInterceptor.Level#NONE}.
     */
    public ApiClient setIsDebug(boolean isDebug) {
        this.isDebug = isDebug;
        return this;
    }



    /**
     * Helper method used to determine if get token enpoint has been invoked. This should be called
     * only when requesting of an accessToken
     *
     * @param getAccessToken {@link Boolean}
     */
    public ApiClient setGetAccessToken(boolean getAccessToken) {
        isGetAccessToken = getAccessToken;
        return this;
    }

    /**
     * Configure OkHttpClient
     *
     * @return OkHttpClient
     */
    private OkHttpClient.Builder okHttpClient() {
        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        okHttpClient
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor);

        return okHttpClient;
    }

    /**
     * Return the current {@link Retrofit} instance. If none exists (first call, API key changed),
     * builds a new one.
     * <p/>
     * When building, sets the endpoint and a {@link HttpLoggingInterceptor} which adds the API key as query param.
     */
    private Retrofit getRestAdapter(String url, String mConsumerKey, String mConsumerSecret, String authToken) {
        Retrofit.Builder builder = new Retrofit.Builder();
        builder.baseUrl(url);
        builder.addConverterFactory(GsonConverterFactory.create());

        if (isDebug) {
            httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        }

        OkHttpClient.Builder okhttpBuilder = okHttpClient();

        if (isGetAccessToken) {
            okhttpBuilder.addInterceptor(new AccessTokenInterceptor(mConsumerKey, mConsumerSecret));
        }

        if (!authToken.isEmpty()) {
            okhttpBuilder.addInterceptor(new AuthInterceptor(authToken));

        }

        builder.client(okhttpBuilder.build());

        retrofit = builder.build();

        return retrofit;
    }

    /**
     * Create service instance.
     *
     * @return STKPushService Service.
     */
    public STKPushService mpesaService(String url, String mConsumerKey, String mConsumerSecret, String authToken) {
        return getRestAdapter(url, mConsumerKey, mConsumerSecret, authToken).create(STKPushService.class);
    }

}
