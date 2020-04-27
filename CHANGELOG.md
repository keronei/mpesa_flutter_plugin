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