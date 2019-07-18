package keronei.mpesa_flutter_plugin;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class MpesaFlutterPlugin implements MethodChannel.MethodCallHandler {
    public static void registerWith(PluginRegistry.Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "mpesa_flutter_plugin");
        channel.setMethodCallHandler(new MpesaFlutterPlugin());
    }

    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {

    }
}
