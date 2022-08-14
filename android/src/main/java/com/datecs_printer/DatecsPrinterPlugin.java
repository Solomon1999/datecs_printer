package com.datecs_printer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/** DatecsPrinterPlugin */
public class DatecsPrinterPlugin implements FlutterPlugin, MethodCallHandler {

  private MethodChannel channel;

  private Context mContext;

  private Printer mPrinter;
  private ProtocolAdapter mProtocolAdapter;
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothSocket mmSocket;
  private BluetoothDevice mmDevice;
  private OutputStream mmOutputStream;
  private InputStream mmInputStream;

  private boolean isConnect = false;

  private final ProtocolAdapter.PrinterListener mChannelListener = new ProtocolAdapter.PrinterListener(){
    @Override
    public void onBatteryStateChanged(boolean b) {

    }

    @Override
    public void onThermalHeadStateChanged(boolean b) {

    }

    @Override
    public void onPaperStateChanged(boolean b) {

    }

//    @Override
//    public void onReadEncryptedCard() {
//      // TODO: onReadEncryptedCard
//    }
//
//    @Override
//    public void onReadCard() {
//      // TODO: onReadCard
//    }
//
//    @Override
//    public void onReadBarcode() {
//      // TODO: onReadBarcode
//    }
//
//    @Override
//    public void onPaperReady(boolean state) {
//      if (state) {
//      } else {
//        try {
//          disconnect();
//        } catch (IOException e) {
//          e.printStackTrace();
//        }
//      }
//    }
  };

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "datecs_printer");
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getListBluetoothDevice")){

      // Get the local Bluetooth adapter
      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      ArrayList<Map<String, String>> devices = new ArrayList<Map<String, String>>();
      if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
          Map<String, String> list = new HashMap<String, String>();
          list.put("name", device.getName());
          list.put("address", device.getAddress());
          devices.add(list);
        }
        result.success(devices);


      }else{

        result.error("Error 101", "Error while get list bluetooth device","");
      }

    }else if (call.method.equals("connectBluetooth")){
      String address = call.argument("address");
      try{
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
          isConnect = connect(address);
        }
        result.success(isConnect);
      }catch(IOException e){
        result.success(isConnect);
      }catch(Exception e){
        result.success(isConnect);
      }

    }else if(call.method.equals("disconnectBluetooth")){
      try{
        disconnect();
        result.success(true);
      }catch(IOException e){
        result.success(false);
      }
    }else if(call.method.equals("testPrint")){
      try {
        mPrinter.printSelfTest();
        mPrinter.flush();
        result.success(true);
      } catch (Exception e) {
        result.success(false);
      }
    }else if(call.method.equals("printText")){
      String charset = "ISO-8859-1";
      List<String> args = call.argument("args");
      try {
        mPrinter.reset();
        for(int i = 0; i < args.size(); i++){
          if(args.get(i).contains("feed%20")){
            String[] split = args.get(i).split("%20");
            int feed = Integer.parseInt(split[1]);
            mPrinter.feedPaper(feed);
          }else if(args.get(i).contains("img%2021")){
            String[] split = args.get(i).split("%2021");
            double size = Double.parseDouble(split[1]);
            String img = split[2];

            if(android.os.Build.VERSION.SDK_INT >= 26){
              byte[] decodedString = Base64.getDecoder().decode(img.getBytes("UTF-8"));
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              // resize to desired dimensions
              int height = decodedByte.getHeight();
              int width = decodedByte.getWidth();
              Log.i("DATECS_PLUGIN", "Image Size");
              Log.i("DATECS PLUGIN", String.valueOf(width));
              Log.i("DATECS PLUGIN", String.valueOf(height));
              double x, y;
              if(width > height){
                x = size;
                y = (height * size) / width;
              } else {
                y = size;
                x = (width * size) / height;
              }
              int newWidth = (int) x;
              int newHeight = (int) y;
              Log.i("DATECS_PLUGIN", "Scaled Size");
              Log.i("DATECS PLUGIN", String.valueOf(newWidth));
              Log.i("DATECS PLUGIN", String.valueOf(newHeight));
              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, newWidth, newHeight, true);
              final int[] argb = new int[newWidth * newHeight];
              resized.getPixels(argb, 0, newWidth, 0, 0, newWidth, newHeight);
              resized.recycle();

              mPrinter.printImage(argb, newWidth, newHeight, Printer.ALIGN_CENTER, true);
            }else{
              byte[] decodedString = android.util.Base64.decode(img, android.util.Base64.DEFAULT);
              Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
              // resize to desired dimensions
              int height = decodedByte.getHeight();
              int width = decodedByte.getWidth();
              Log.i("DATECS_PLUGIN", "Image Size");
              Log.i("DATECS PLUGIN", String.valueOf(width));
              Log.i("DATECS PLUGIN", String.valueOf(height));
              double x, y;
              if(width > height){
                x = size;
                y = (height * size) / width;
              } else {
                y = size;
                x = (width * size) / height;
              }
              int newWidth = (int) x;
              int newHeight = (int) y;
              Log.i("DATECS_PLUGIN", "Scaled Size");
              Log.i("DATECS PLUGIN", String.valueOf(newWidth));
              Log.i("DATECS PLUGIN", String.valueOf(newHeight));
              Bitmap resized = Bitmap.createScaledBitmap(decodedByte, newWidth, newHeight, true);
              final int[] argb = new int[newWidth * newWidth];
              resized.getPixels(argb, 0, newWidth, 0, 0, newWidth, newHeight);
              resized.recycle();
              mPrinter.printImage(argb, newWidth, newHeight, Printer.ALIGN_CENTER, true);
            }
          }else{
            mPrinter.printTaggedText(args.get(i));
          }
        }
        mPrinter.flush();
        result.success(true);
      } catch (IOException e) {
        result.success(false);
      } catch (NullPointerException e){
        result.success(false);
      }
    }else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  public boolean connect(String address) throws IOException {
    try{
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      BluetoothDevice device = adapter.getRemoteDevice(address);
      mmDevice = device;
      UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
      mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
      mmSocket.connect();
      mmOutputStream = mmSocket.getOutputStream();
      mmInputStream = mmSocket.getInputStream();
      initializePrinter(mmInputStream, mmOutputStream);
      return true;
    }catch(Exception error){
      throw error;
    }
  }

  protected void initializePrinter(InputStream inputStream, OutputStream outputStream) throws IOException {
    mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
    if (mProtocolAdapter.isProtocolEnabled()) {
      final ProtocolAdapter.Channel channel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);

      // Create new event pulling thread
      new Thread(new Runnable() {
        @Override
        public void run() {
          while (true) {
            try {
              Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            try {
              channel.open();
            } catch (IOException e) {

              break;
            }
          }
        }
      }).start();
      mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
    } else {
      mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
    }

  }

  public void disconnect() throws IOException{
    try {
      mmSocket.close();

      if (mPrinter != null) {
        mPrinter.close();
      }

      if (mProtocolAdapter != null) {
        mProtocolAdapter.close();
      }


    } catch (Exception e) {

    }
  }

}
