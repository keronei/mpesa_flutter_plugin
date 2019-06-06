package keronei.mpesa_flutter_plugin.api.interceptor;

import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccessTokenInterceptor implements Interceptor {
    private String mConsumerKey;
    private String mConsumerSecret;

    public AccessTokenInterceptor(String mConsumerKey, String mConsumerSecret) {
        this.mConsumerKey = mConsumerKey;
        this.mConsumerSecret = mConsumerSecret;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {

        Log.d("INTERCEPTOR CRED:" , mConsumerKey +" and " +mConsumerSecret );

        String keys = mConsumerKey + ":" + mConsumerSecret;

        Log.d("AFT INTERCEPT", keys);

        Request request = chain.request().newBuilder()
                .addHeader("Authorization", "Basic " + Base64.encodeToString(keys.getBytes(), Base64.NO_WRAP))
                .build();
        return chain.proceed(request);
    }
}