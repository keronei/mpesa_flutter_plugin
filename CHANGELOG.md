## 0.0.1

* Basic functioning of STK push.
## 0.0.2
* Bug fixes
## 0.0.3
* Added support for *iOS*
* Migrated android version to support *androidx*
* Changed following properties:
    1. String amount to double amount
    2. String callbackUrl to Uri callbackUrl
    3. String baseUrl to Uri baseUri
    3. Removed _enableDebugModeWithLogging_ method.

## 0.0.4
* Removed unnecessary dependencies in android.

## 0.0.5
* Upgraded intl version to 0.16.0

## 1.0.0
* Fixed a bug where the Initializer expected a return of type Map but
an exception thrown returns a string.
* Removed platform specific folders since they are no longer in use

## 1.5.0-beta1
* Migrated to sound null safety

## 1.6.0-beta1
* Added documentation for store till number support (thanks to [Dedan Kibere](https://github.com/ndungudedan))
* Upgraded example app to use flutter embedding v2

## 1.6.1
* Implement use of prompt to collect phone number to be charged in the example.
* Upgrade gradle version.
* Remove phone number in payload received from callback in documentation - subject to be removed due to 
  privacy policy.