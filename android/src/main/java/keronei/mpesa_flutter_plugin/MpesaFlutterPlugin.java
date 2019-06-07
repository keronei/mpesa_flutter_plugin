package keronei.mpesa_flutter_plugin;

import android.support.annotation.NonNull;


import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import keronei.mpesa_flutter_plugin.api.ApiClient;
import keronei.mpesa_flutter_plugin.api.model.AccessToken;
import keronei.mpesa_flutter_plugin.api.model.STKPush;
import keronei.mpesa_flutter_plugin.helper.Utils;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


import com.google.gson.*;


//timing libs
import java.lang.reflect.Array;
import java.util.Timer;
import java.util.TimerTask;


/**
 * MpesaFlutterPlugin
 */
public class MpesaFlutterPlugin implements MethodCallHandler {
    //Initialize stuff
    private ApiClient mApiClient;
    private String mConsumerKeyVar;
    private String mConsumerSecretVar;
    private String mAuthToken = "";

    private Boolean mShouldDebugByDefault = false;

    //timers
    static int interval = 3550; //should be 3599 but for benefit of doubt.
    static Timer timer;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "mpesa_flutter_plugin");
        channel.setMethodCallHandler(new MpesaFlutterPlugin());
    }

    /**
     * Fire timer when token is received...
     */
    private void startTimer() {
        //target is 59 mins, AuthToken should be set as 0 when time elapse
        timer = new Timer();

        int delay = 1000;
        int period = 1000;

        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                /*
                Here, the token countdown runs for 3500 seconds
                 */
                setInterval();
            }
        }, delay, period);


    }

    private int setInterval() {
        if (interval == 1) {
            timer.cancel();

            mAuthToken = "";
        }

        return --interval;
    }

    private void getTokenOnRequest(String sentUrl, final ApiClient initClient, final Result result) {
        //Create new token. This only means there's no previous token.
        initClient.setGetAccessToken(true);
        initClient.mpesaService(sentUrl, mConsumerKeyVar, mConsumerSecretVar, mAuthToken).getAccessToken().enqueue(new Callback<AccessToken>() {
            @Override
            public void onResponse(@NonNull Call<AccessToken> call, @NonNull Response<AccessToken> response) {

                try {
                    if (response.isSuccessful()) {
                        mAuthToken = response.body().accessToken;
                        startTimer();
                        result.success(true);
                    } else {
                        String error = response.errorBody().string();
                        //Just to make sure the message is home success as error/error as success
                        result.success(error);
                    }

                } catch (Exception e) {
                    result.error("EXCEPTION-On-TOKEN-REQUEST", e.getMessage(), null);
                }

            }

            @Override
            public void onFailure(@NonNull Call<AccessToken> call, @NonNull Throwable t) {
                result.error("Failed on token", t.toString(), null);
            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
        mApiClient = new ApiClient();
        mApiClient.setIsDebug(mShouldDebugByDefault); //Set True to enable logging, false to disable.

        if (call.method.equals("setConsumerKey")) {
            if (call.hasArgument("consumerKey")) {
                mConsumerKeyVar = call.argument("consumerKey");
                result.success(true);

            }
        } else if (call.method.equals("setConsumerSecret")) {
            if (call.hasArgument("consumerSecret")) {
                mConsumerSecretVar = call.argument("consumerSecret");
                result.success(true);

            }
        } else if (call.method.equals("setToken")) {
            String sentUrl = call.argument("url");

            //First query if there's a token already
            if (mAuthToken.isEmpty()) {
                getTokenOnRequest(sentUrl, mApiClient, result);

            } else {
                //there's a previous token, reply OK
                result.success(true);
            }
        } else if (call.method.equals("setDebugMode")) {

            mShouldDebugByDefault = call.argument("mode");

        } else if (call.method.equals("InitPayment")) {
        /*Initialise the payment and send the response to the app:
           MPESA will reply with the following if all variables were accepted(
           which includes phone, amount & PartyB code.)

           1. MerchantRequestID
           2. CheckoutRequestID
           3. CustomerMessage

           Else if the initialization did not go through, the following will be the
           expected response.
           1. errorCode (e.g 500.001.1001 may mean invalid phone.)
           2. CustomerMessage
           */
            //Step 1: Extract the variables:

            //base url -- changes with sandbox/live
            String mBaseUrl = call.argument("BASE_URL");

            /*The credit party of the transaction/the party being
             paid in the transaction, hereby being the shortcode
              of the organization. This is the same value as the
             Business Shortcode*/
            String mBusinessShortCode = call.argument("BUSINESS_SHORT_CODE");
            /*The type of transaction being performed.*/
            String mTransactionType = call.argument("TRANSACTION_TYPE");
            String mAmount = call.argument("AMOUNT");
            /*Party B is still the short-code.*/
            String mPartyB = call.argument("PARTY_B");
            /*Party A is the customers phone*/
            String mPhoneNumber = call.argument("PHONE_NUMBER");
            String mCallBackUrl = call.argument("CALLBACK_URL");
            /*Transaction Ref in this case is same as the account REF*/
            String mTransactionRef = call.argument("TRANSACTION_REF");
            String mPassKey = call.argument("PASS_KEY");
            String mTransactionDesc = call.argument("TRANSACTION_DESC");


            String timestamp = Utils.getTimestamp();

            assert mBaseUrl != null;

            assert mPhoneNumber != null;

            assert mPassKey != null;

            STKPush stkPush = new STKPush(
                    mBusinessShortCode,
                    Utils.getPassword(mBusinessShortCode, mPassKey, timestamp),
                    timestamp,
                    mTransactionType,
                    mAmount,
                    Utils.sanitizePhoneNumber(mPhoneNumber),
                    mPartyB,
                    Utils.sanitizePhoneNumber(mPhoneNumber),
                    mCallBackUrl,
                    mTransactionRef, //The account reference
                    mTransactionDesc  //The transaction description
            );
            assert mConsumerSecretVar != null;
            assert mConsumerKeyVar != null;


            mApiClient.setGetAccessToken(false);

            mApiClient.mpesaService(mBaseUrl, mConsumerKeyVar, mConsumerSecretVar, mAuthToken).sendPush(stkPush).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {


                    try {
                        if (response.isSuccessful()) {
                            //Convert the result to JSON for consistency.
                            Object sentResult = response.body();
                            Gson revertedResult = new Gson();
                            String jsoniFied = revertedResult.toJson(sentResult);
                            result.success(jsoniFied);


                        } else {
                            //return Stringified format of response body.

                            ResponseBody x = response.errorBody();
                            result.success(x != null ? x.string() : "Bad error occurred.");


                        }
                    } catch (Exception e) {
                        //throw an exception
                        result.error("EXCEPTION-On-PAYMENT", e.toString(), null);

                    }
                }

                @Override
                public void onFailure(@NonNull Call call, @NonNull Throwable t) {

                    result.error("Failed on payment", t.getMessage(), null);
                }
            });


        } else {
            result.notImplemented();
        }
    }


}
