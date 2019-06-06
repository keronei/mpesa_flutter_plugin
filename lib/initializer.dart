import 'dart:async';
import 'dart:io' show Platform;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import './payment_enums.dart';

class MpesaFlutterPlugin {
  static const MethodChannel _channel =
      const MethodChannel('mpesa_flutter_plugin');

  static bool _consumerKeySet = false;

  static Future<Null> setConsumerKey(String consumerKey) {
    ///Value of Consumer Key MUST be set before the party starts.
    Map<String, dynamic> arguments = <String, dynamic>{};
    arguments.putIfAbsent("consumerKey", () => consumerKey);
    var result = _channel.invokeMethod('setConsumerKey', arguments);
    _consumerKeySet = true;
  }

  static bool _consumerSecretSet = false;

  static Future<Null> setConsumerSecret(String consumerSecret) {
    ///ConsumerSecret MUST be set prior to placing
    ///token request, otherwise auth will not work
    Map<String, dynamic> arguments = <String, dynamic>{};
    arguments.putIfAbsent("consumerSecret", () => consumerSecret);
    var result = _channel.invokeMethod('setConsumerSecret', arguments);
    _consumerSecretSet = true;
  }

  static Future<dynamic> initializeMpesaSTKPush(
      {

      ///BusinessShortCode is the org paybill
      ///Which is same as PartyB
      ///Phone Number should be a registered MPESA number
      ///Which is same as PartyA

      @required String businessShortCode,
      @required TransactionType transactionType,
      @required String amount,
      @required String partyA,
      @required String partyB,
      @required String callBackURL,
      @required String accountReference,
      String transactionDesc,
      @required String phoneNumber,
      @required String baseUrl,
      @required String passKey}) async {
    /*Inject some sanity*/
    if (double.parse(amount) < 1.0) {
      throw "error: you provided $amount  as the amount which is not valid.";
    }
    if (phoneNumber.length < 9) {
      throw "error: $phoneNumber  doesn\'t seem to be a valid phone number";
    }
    /*Stop iOS here, end of the road*/
    if (Platform.isIOS) {
      throw "iOS not supported yet";
    }

    if (!baseUrl.startsWith("https://")) {
      throw "ensure base url is in the correct format : https://";
    }

    /*Create arguments */

    Map<String, dynamic> arguments = <String, dynamic>{};

    /*Mine the secrets from Config*/

    if (!_consumerSecretSet || !_consumerKeySet) {
      throw "error: ensure consumer key & secret is set. Use MpesaFlutterPlugin.setConsumer...";
    }

    arguments.putIfAbsent("BASE_URL", () => baseUrl);

    arguments.putIfAbsent('BUSINESS_SHORT_CODE', () => businessShortCode);
    arguments.putIfAbsent(
        'TRANSACTION_TYPE',
        () => transactionType == TransactionType.CustomerPayBillOnline
            ? "CustomerPayBillOnline"
            : "CustomerBuyGoodsOnline");
    arguments.putIfAbsent('AMOUNT', () => amount);
    arguments.putIfAbsent('PARTY_B', () => partyB);
    arguments.putIfAbsent('PHONE_NUMBER', () => phoneNumber);
    arguments.putIfAbsent('CALLBACK_URL', () => callBackURL);
    arguments.putIfAbsent('TRANSACTION_REF', () => accountReference);
    arguments.putIfAbsent('PASS_KEY', () => passKey);
    arguments.putIfAbsent('TRANSACTION_DESC', () => transactionDesc);

    ///createToken---> wait and then place the request
    ///The token provided lasts for 3600 seconds.
    ///The best alternative is to set timer after requesting token,
    ///but here is the issue [[expiry time vs +ve ]] response from server.
    ///
    /// Best is to set CountDown iff the initial token request was successful.

    Map<String, dynamic> baseURLHolder = {};
    baseURLHolder.putIfAbsent("url", () => baseUrl);

    return _channel
        .invokeMethod("setToken", baseURLHolder)
        .then((dynamic result) {
      if (result == true) {
        ///Indicate [true] if token was granted, then
        /// start countdown and fire
        /// payment request, else retry.
        ///
        return kickOfPayment(arguments);
      } else {
        return  result.toString();
      }
    });
  }

  static dynamic kickOfPayment(Map<String, dynamic> args) async {
    return await _channel.invokeMethod('InitPayment', args).then((result) {
      return result;
    });
  }
}
