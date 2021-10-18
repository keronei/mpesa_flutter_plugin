import 'dart:async';
import 'package:intl/intl.dart';
import './payment_enums.dart';
import './universal_api/api_caller.dart';

class MpesaFlutterPlugin {
  static bool _consumerKeySet = false;
   static late String _mConsumerKeyVariable;

  static setConsumerKey(String consumerKey) {
    ///Value of Consumer Key MUST be set before the party starts.
    _mConsumerKeyVariable = consumerKey;
    _consumerKeySet = true;
  }

  static bool _consumerSecretSet = false;
  static late String _mConsumerSecretVariable;

  static setConsumerSecret(String consumerSecret) {
    ///ConsumerSecret MUST be set prior to placing
    ///token request, otherwise auth will not work
    _mConsumerSecretVariable = consumerSecret;
    _consumerSecretSet = true;
  }

  static Future<dynamic> initializeMpesaSTKPush(
      {

      ///BusinessShortCode is the org paybill
      ///Which is same as PartyB
      ///Phone Number should be a registered MPESA number
      ///Which is same as PartyA

      required String businessShortCode,
      required TransactionType transactionType,
      required double amount,
      required String partyA,
      required String partyB,
      required Uri callBackURL,
      required String accountReference,
      String? transactionDesc,
      required String phoneNumber,
      required Uri baseUri,
      required String passKey}) async {
    /*Inject some sanity*/
    if (amount < 1.0) {
      throw "error: you provided $amount  as the amount which is not valid.";
    }
    if (phoneNumber.length < 9) {
      throw "error: $phoneNumber  doesn\'t seem to be a valid phone number";
    }
    if (!phoneNumber.startsWith('254')) {
      throw "error: $phoneNumber need be in international format";
    }

    /*Mine the secrets from Config*/

    if (!_consumerSecretSet || !_consumerKeySet) {
      throw "error: ensure consumer key & secret is set. Use MpesaFlutterPlugin.setConsumer...";
    }
    var rawTimeStamp = new DateTime.now();
    var formatter = new DateFormat('yyyyMMddHHmmss');
    String actualTimeStamp = formatter.format(rawTimeStamp);

    return RequestHandler(
            consumerKey: _mConsumerKeyVariable,
            consumerSecret: _mConsumerSecretVariable,
            baseUrl: baseUri.host)
        .mSTKRequest(
            mAccountReference: accountReference,
            mAmount: amount,
            mBusinessShortCode: businessShortCode,
            mCallBackURL: callBackURL,
            mPhoneNumber: phoneNumber,
            mTimeStamp: actualTimeStamp,
            mTransactionDesc: transactionDesc,
            nPassKey: passKey,
            partyA: partyA,
            partyB: partyB,
            mTransactionType:
                transactionType == TransactionType.CustomerPayBillOnline
                    ? "CustomerPayBillOnline"
                    : "CustomerBuyGoodsOnline");
  }
}
