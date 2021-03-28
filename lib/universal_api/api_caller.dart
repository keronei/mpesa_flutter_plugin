import 'dart:io';
import 'dart:async';
import 'dart:convert';

//taken from https://github.com/komuw/zakah

class RequestHandler {
  ///setup values
  final String consumerKey;
  final String consumerSecret;
  final String b64keySecret;
  final String baseUrl;

  late String mAccessToken;
  DateTime? mAccessExpiresAt;

  ///For instantiation, create the key secret on the fly with received values.

  RequestHandler({required this.consumerKey, required this.consumerSecret, required this.baseUrl})
      : b64keySecret =
            base64Url.encode((consumerKey + ":" + consumerSecret).codeUnits);

  Uri getAuthUrl() {
    ///Basically merges the various components of the provided params
    ///to generate one link for getting credentials before placing a request.
    Uri uri = new Uri(
        scheme: 'https',
        host: baseUrl,
        path: '/oauth/v1/generate',
        queryParameters: <String, String>{'grant_type': 'client_credentials'});
    return uri;
  }

  String generatePassword(
      {required String mPassKey, required String mShortCode, required String actualTimeStamp}) {
    ///Adds up the paybill no., the timestamp & passkey to generate a base64
    ///code to be added to the request body as unique password to auth
    ///the request in question.
    String readyPass = mShortCode + mPassKey + actualTimeStamp;

    var bytes = utf8.encode(readyPass);
    return base64.encode(bytes);
  }

  Future<void> setAccessToken() async {
    /// This method ensures that the token is in place before any request is
    /// placed.
    /// When called, it first checks if the previous token exists, if so, is it valid?
    /// if still valid(by expiry time measure), terminates to indicate that
    /// the token is set and ready for usage.
    DateTime now = new DateTime.now();
    if (mAccessExpiresAt != null) {
      if (now.isBefore(mAccessExpiresAt!)) {
        return;
      }
    }

    // todo: handle exceptions
    HttpClient client = new HttpClient();
    HttpClientRequest req = await client.getUrl(getAuthUrl());
    req.headers.add("Accept", "application/json");
    req.headers.add("Authorization", "Basic " + b64keySecret);
    HttpClientResponse res = await req.close();

    // u should use `await res.drain()` if u aren't reading the body
    await res.transform(utf8.decoder).forEach((bodyString) {
      dynamic jsondecodeBody = jsonDecode(bodyString);
      mAccessToken = jsondecodeBody["access_token"].toString();
      mAccessExpiresAt = now.add(new Duration(
          seconds: int.parse(jsondecodeBody["expires_in"].toString())));
    });
  }

  Uri generateSTKPushUrl() {
    ///Nothing much, merges the uri parts to produce one uri  that would be used
    ///to process the actual request.
    ///Note that baseUrl is now instantiated with the call, instead of sending
    ///it as a param with the body, this made it easier to use in generating auth
    ///token before placing the request.
    Uri uri = new Uri(
        scheme: 'https',
        host: baseUrl,
        path: 'mpesa/stkpush/v1/processrequest');
    return uri;
  }

  Future<Map<String, String>> mSTKRequest(
      {required String mBusinessShortCode,
      required String nPassKey,
        required String mTransactionType,
        required String mTimeStamp,
        required double mAmount,
        required String partyA,
        required String partyB,
        required String mPhoneNumber,
        required Uri mCallBackURL,
        required String mAccountReference,
        String? mTransactionDesc}) async {
    ///set access token before starting the party.
    await setAccessToken();

    ///create the payload that should not be changed until the request is done.
    final stkPushPayload = {
      "BusinessShortCode": mBusinessShortCode,
      "Password": generatePassword(
          mShortCode: mBusinessShortCode,
          mPassKey: nPassKey,
          actualTimeStamp: mTimeStamp),
      "Timestamp": mTimeStamp,
      "Amount": mAmount,
      "PartyA": partyA,
      "PartyB": partyB,
      "PhoneNumber": mPhoneNumber,
      "CallBackURL": mCallBackURL.toString(),
      "AccountReference": mAccountReference,
      "TransactionDesc": mTransactionDesc == null? "" : mTransactionDesc,
      "TransactionType": mTransactionType
    };
    final Map<String, String> result = new Map<String, String>();

    ///Actual request starts here.
    HttpClient client = new HttpClient();
    return await client.postUrl(generateSTKPushUrl()).then((req) async {
      req.headers.add("Content-Type", "application/json");
      req.headers.add("Authorization", "Bearer " + mAccessToken);
      req.write(jsonEncode(stkPushPayload)); // write is non-blocking
      HttpClientResponse res = await req.close();

      await res.transform(utf8.decoder).forEach((bodyString) {
        dynamic mJsonDecodeBody = jsonDecode(bodyString);

        if (res.statusCode == 200) {
          result["MerchantRequestID"] =
              mJsonDecodeBody["MerchantRequestID"].toString();
          result["CheckoutRequestID"] =
              mJsonDecodeBody["CheckoutRequestID"].toString();
          result["ResponseCode"] = mJsonDecodeBody["ResponseCode"].toString();
          result["ResponseDescription"] =
              mJsonDecodeBody["ResponseDescription"].toString();
          result["CustomerMessage"] =
              mJsonDecodeBody["CustomerMessage"].toString();
        } else {
          result["requestId"] = mJsonDecodeBody["requestId"].toString();
          result["errorCode"] = mJsonDecodeBody["errorCode"].toString();
          result["errorMessage"] = mJsonDecodeBody["errorMessage"].toString();
        }
      });
      return result;
    }).catchError((error) {
      ///the user should expect anything here, from network errors to
      ///timeout issues or whatever an http error is!

      result["error"] = error.toString();
      return result;
    });
  }
}
