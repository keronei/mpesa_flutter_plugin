import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mpesa_flutter_plugin/mpesa_flutter_plugin.dart';

void main() {
  const MethodChannel channel = MethodChannel('mpesa_flutter_plugin');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

//  test('getPlatformVersion', () async {
//    expect(await MpesaFlutterPlugin., '42');
//  });
}
