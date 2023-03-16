import 'dart:async';

import 'package:datecs_printer/method/datecs_device.dart';
import 'package:flutter/services.dart';

class DatecsPrinter {
  static const MethodChannel _channel = MethodChannel('datecs_printer');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<List<dynamic>> get getListBluetoothDevice async {
    var value = await _channel.invokeMethod('getListBluetoothDevice');
    print(value);
    final List<dynamic> list = value;
    return list;
  }

  static Future<bool> connectBluetooth(String address, String name) async {
    try {
      var value = await _channel
          .invokeMethod('connectBluetooth', {"address": address, "name": name});
      print(value);
      final bool result = value;
      return result;
    } catch (e) {
      return false;
    }
  }

  static Future<bool> checkConnectionState() async {
    try {
      var value = await _channel.invokeMethod('connectionState');
      print(value);
      final bool result = value;
      return result;
    } catch (e) {
      return false;
    }
  }

  static Future<DatecsDevice?> getConnectedDevice() async {
    DatecsDevice? device;
    try {
      Map<String, dynamic>? value =
          await _channel.invokeMethod('getConnectedDevice');
      print(value);
      if (value != null) {
        device = DatecsDevice.fromMap(value);
      }
      return device;
    } catch (e) {
      throw Exception("Failed to get connected device");
    }
  }

  static Future<void> get printTest async {
    final bool result = await _channel.invokeMethod('testPrint');
    return Future.value();
  }

  static Future<bool> printText(List<String> args) async {
    try {
      final bool result =
          await _channel.invokeMethod('printText', {"args": args});
      print(result);
      return result;
    } catch (e) {
      return false;
    }
  }

  static Future<bool> get disconnect async {
    try {
      final bool result = await _channel.invokeMethod('disconnectBluetooth');
      return Future.value(result);
    } on PlatformException catch (e) {
      print("Failed to write bytes: '${e.message}'.");
      return Future.value(false);
    }
  }
}
