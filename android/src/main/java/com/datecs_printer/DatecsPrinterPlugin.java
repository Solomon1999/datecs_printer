package com.datecs_printer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

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
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/** DatecsPrinterPlugin */
public class DatecsPrinterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

  private MethodChannel channel;
  private Result result;

  private Context mContext;
  private Activity mActivity;

  private Printer mPrinter;
  private ProtocolAdapter mProtocolAdapter;

  private BluetoothManager mBluetoothManager;
  private BluetoothAdapter mBluetoothAdapter;
  private BluetoothSocket mmSocket;
  private BluetoothDevice mmDevice;
  private OutputStream mmOutputStream;
  private InputStream mmInputStream;

  private boolean isConnect = false;

  private final static int REQUEST_ENABLE_BT = 1;

  private final ProtocolAdapter.ChannelListener mChannelListener = new ProtocolAdapter.ChannelListener(){

//    @Override
//    public void onBatteryStateChanged(boolean b) {
//
//    }
//
//    @Override
//    public void onThermalHeadStateChanged(boolean b) {
//
//    }
//
//    @Override
//    public void onPaperStateChanged(boolean b) {
//
//    }

    @Override
    public void onReadEncryptedCard() {
      // TODO: onReadEncryptedCard
    }

    @Override
    public void onReadCard() {
      // TODO: onReadCard
    }

    @Override
    public void onReadBarcode() {
      // TODO: onReadBarcode
    }

    @Override
    public void onLowBattery(boolean b) {

    }

    @Override
    public void onOverHeated(boolean b) {

    }

    @Override
    public void onPaperReady(boolean state) {
      if (state) {
        Log.i("DATECS_PRINTER", "Checking on paper ready");

      } else {
        try {
          Log.e("DATECS_PRINTER", "On paper ready failed disconnecting");
          disconnect();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  };

  /**
   * This method checks if a runtime permission has been granted.
   * @param permission The permission to check.
   * @return <code>TRUE</code> if the permission has been granted, <code>FALSE</code> otherwise.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkPermission(@NonNull String permission){
    return ActivityCompat.checkSelfPermission(mContext, permission)
            == PackageManager.PERMISSION_GRANTED;
  }
  private void requestRuntimePermissions(
          @NonNull String title, @NonNull String description,
          int requestCode, @NonNull String... permissions){
    if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permissions[0])) {
      AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
      builder
              .setTitle(title)
              .setMessage(description)
              .setCancelable(false)
              .setNegativeButton(android.R.string.no, (dialog, id) -> {
                //do nothing
              })
              .setPositiveButton(
                      android.R.string.ok,
                      (dialog, id) -> ActivityCompat.requestPermissions(mActivity, permissions, requestCode));
      builder.create().show();
    }
    else ActivityCompat.requestPermissions(mActivity, permissions, requestCode);
  }

  void requestEnableBluetooth() {
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }

  void confirmPermissions() {
    ArrayList<String> deniedPermissions = new ArrayList<>();
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
      if (!checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
        deniedPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
      if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION))
        deniedPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
    } else {
        if (!checkPermission(Manifest.permission.BLUETOOTH_SCAN))
          deniedPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        if (!checkPermission(Manifest.permission.BLUETOOTH_CONNECT))
          deniedPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
    }

    if (deniedPermissions.isEmpty()) {
      if (mBluetoothAdapter == null || mBluetoothAdapter.isEnabled()) //check if bluetooth is enabled
        requestEnableBluetooth(); //method to request enable bluetooth
        //        return false;
    } else {
        Log.d("Datecs", "Request bluetooth permissions");
        requestRuntimePermissions(
                "Bluetooth permissions request",
                "Bluetooth permissions request rationale",
                REQUEST_ENABLE_BT,
                deniedPermissions.toArray(new String[0]));
        //      return false;
      }
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "datecs_printer");
    channel.setMethodCallHandler(this);
    mContext = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    this.result = result;
    confirmPermissions();
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getListBluetoothDevice")){

      // Get the local Bluetooth adapter
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

      }
//      mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
      if (mBluetoothAdapter != null) {
//        if (!mBluetoothAdapter.isEnabled()) {
//          Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//          mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
        ArrayList<Map<String, String>> devices = new ArrayList<Map<String, String>>();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
          Map<String, String> list = new HashMap<String, String>();
          list.put("name", device.getName());
          list.put("address", device.getAddress());
          devices.add(list);
        }
        result.success(devices);
      } else{
        result.error("Error 101", "Error while getting list bluetooth device","");
      }
    }else if (call.method.equals("connectBluetooth")){
      String address = call.argument("address");
      try{
        if (BluetoothAdapter.checkBluetoothAddress(address)) {
          isConnect = connect(address);
        }
        result.success(isConnect);
      }catch(IOException e) {
        Log.e("DATECS PRITNER", e.toString());
        result.success(false);
      }catch(Exception e) {
        Log.e("DATECS PRITNER", e.toString());
        result.success(false);
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
          }else if(args.get(i).contains("img%2021")) {
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
      mmInputStream = mmSocket.getInputStream();
      mmOutputStream = mmSocket.getOutputStream();
      initializePrinter(mmInputStream, mmOutputStream);
      return true;
    }catch(Exception error){
      Log.e("DATECS_PRINTER", error.toString());
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
              Log.e("DATECS_PRINTER", e.toString());
            }
            try {
              channel.open();
            } catch (IOException e) {
              Log.e("DATECS_PRINTER", e.toString());
              break;
            }
          }
        }
      }).start();
      mPrinter = new Printer(channel.getInputStream(), channel.getOutputStream());
    } else {
      Log.i("DATECS_PRINTER", "Protocol is not enabled");
      mPrinter = new Printer(mProtocolAdapter.getRawInputStream(), mProtocolAdapter.getRawOutputStream());
    }

  }

  public void disconnect() throws IOException{
    try {
      Log.i("DATECS_PRINTER", "Disconnecting Printer");
      mmSocket.close();

      if (mPrinter != null) {
        mPrinter.release();
      }

      if (mProtocolAdapter != null) {
        mProtocolAdapter.release();
      }


    } catch (Exception e) {
      Log.e("DATECS_PRINTER", "Failed to disconnect printer");
    }
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    mActivity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    mActivity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    mActivity = binding.getActivity();
    binding.addActivityResultListener(this);
  }

  @Override
  public void onDetachedFromActivity() {
    mActivity = null;
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//    Log.d("Datecs", "Intent Data"+data.toString());
    return false;
  }
}
