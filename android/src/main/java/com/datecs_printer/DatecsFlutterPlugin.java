package com.datecs_printer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class DatecsFlutterPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private DatecsSDKWrapper printer;
    private FlutterPlugin.FlutterPluginBinding pluginBinding;
    private ActivityPluginBinding activityBinding;
    private Application application;
    private Context context;
    private Activity activity;


    private static final String TAG = "DatecsPrinterPlugin";

    private enum Option {
        listBluetoothDevices,
        connect,
        disconnect,
        feedPaper,
        printText,
        getStatus,
        getTemperature,
        setBarcode,
        printBarcode,
        printQRCode,
        printImage,
        printLogo,
        printSelfTest,
        setPageRegion,
        selectPageMode,
        selectStandardMode,
        drawPageRectangle,
        drawPageFrame,
        printPage,
        write,
        writeHex;
    }

    public void initialize() {
        printer = new DatecsSDKWrapper(this.activity);
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {

    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        pluginBinding = binding;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        io.flutter.Log.i(TAG, "Attaching Plugin to Activity");
        this.activityBinding = binding;
        this.activity = binding.getActivity();
        this.context = pluginBinding.getApplicationContext();
        this.application = (Application) pluginBinding.getApplicationContext();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        Option option = null;
        try {
            option = Option.valueOf(call.method);
        } catch (Exception e) {
            result.success(false);
        }

        switch (option) {
            case listBluetoothDevices:
                printer.getBluetoothPairedDevices(result);
                break;
            case connect:
                printer.setAddress(call.argument("address"));
                printer.connect(result);
                break;
            case disconnect:
                try {
                    printer.closeActiveConnections();
                    result.success(DatecsUtil.getStringFromStringResource(this.activity.getApplication(), "printer_disconnected"));
                } catch (Exception e) {
                    result.success(DatecsUtil.getStringFromStringResource(this.activity.getApplication(), "err_disconnect_printer"));
                }
                break;
            case feedPaper:
                printer.feedPaper(call.argument("lines"));
                break;
            case printText:
                String text = call.argument("text");
                String charset = call.argument("charset");
                printer.printTaggedText(text, charset);
                break;
            case getStatus:
                printer.getStatus();
                break;
            case getTemperature:
                printer.getTemperature();
                break;
            case setBarcode:
                int align = (int) call.argument("alignment"); //int
                boolean small = (boolean) call.argument( "isSmall");
                int scale = (int) call.argument("scale");
                int hri = (int) call.argument("hri");
                int height = call.argument("height");
                printer.setBarcode(align, small, scale, hri, height);
                break;
            case printBarcode:
                int type = (int) call.argument("type");
                String data = call.argument("barcodeData");
                printer.printBarcode(type, data);
                break;
            case printQRCode:
                int size = (int) call.argument("size");
                int eccLv = (int) call.argument("eccLvl");
                data = (String) call.argument("codeData");
                printer.printQRCode(size, eccLv, data);
                break;
            case printImage:
                String image = (String) call.argument("imageString");
                int imgWidth = (int) call.argument("imageWidth");
                int imgHeight = (int) call.argument("imageHeight");
                int imgAlign = (int) call.argument("imageAlignment");
                printer.printImage(image, imgWidth, imgHeight, imgAlign);
                break;
            case printLogo:
                break;
            case printSelfTest:
                printer.printSelfTest();
                break;
            case drawPageRectangle:
                int x = (int) call.argument("x");
                int y = (int) call.argument("y");
                int width = (int) call.argument("width");
                height = (int) call.argument("height");
                int fillMode = (int) call.argument("fillMode");
                printer.drawPageRectangle(x, y, width, height, fillMode);
                break;
            case selectPageMode:
                printer.selectPageMode();
                break;
            case selectStandardMode:
                printer.selectStandardMode();
                break;
            case setPageRegion:
                x = (int) call.argument("x");
                y = (int) call.argument("y");
                width = (int) call.argument("width");
                height =(int) call.argument("height");
                int direction = (int) call.argument("direction");
                printer.setPageRegion(x, y, width, height, direction);
                break;
            case drawPageFrame:
                x = (int) call.argument("x");
                y = (int) call.argument("y");
                width = (int) call.argument("width");
                height = (int) call.argument("height");
                fillMode = (int) call.argument("fillMode");
                int thickness = (int) call.argument("thickness");
                printer.drawPageFrame(x, y, width, height, fillMode, thickness);
                break;
            case printPage:
                printer.printPage();
                break;
            case write:
                byte[] bytes = (byte[]) call.argument("bytes");
                printer.write(bytes);
                break;
            case writeHex:
                String hex = (String) call.argument("hexString");
                printer.writeHex(hex);
                break;
        }
        result.success(true);
    }
}