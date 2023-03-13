import Flutter
import UIKit
import PrinterSDK


public class SwiftDatecsPrinterPlugin: NSObject, FlutterPlugin {
    var bluetoothState: PTBluetoothState = PTBluetoothState.unauthorized;
    var isConnect: Bool = false;
    var devices: [PTPrinter]  = [];
    var mPrinter: PTDispatcher = PTDispatcher.share();
//    var mPrinter2: PrinterSDK = PrinterSDK()

    private func connect(device: PTPrinter) throws -> Bool {
            NSLog("Connecting to \(String(describing: device.name))");
            mPrinter.connect(device);
            mPrinter.whenConnectSuccess {
                NSLog("Connected Sucessfully");
                self.isConnect = true;
            }
            mPrinter.whenConnectFailureWithErrorBlock { error in
                NSLog(String(error.rawValue));
                self.isConnect = false;
            }
            return isConnect;
    }
    
    private func disconnect() throws -> Void {
        mPrinter.disconnect()
    }

    private func sendDataToDevice(data: Data) -> Bool {
        var status: Bool? = nil;
//        do {
//            if mPrinter.printerConnected == nil {
//                NSLog("Printer is not connected: \(String(describing: mPrinter.printerConnected))")
//                return false
//            }
            mPrinter.send(data)
            
            mPrinter.whenSendProgressUpdate({ progress in
                NSLog("Sending Progress \(String(describing: progress))");
            })
            
            mPrinter.whenSendSuccess {a,b in
                NSLog("Data sent successfully");
                NSLog(String(describing: a));
                NSLog(String(describing: b));
                status = true;
            }
            
            mPrinter.whenSendFailure {
                NSLog("Failed to send data");
                status = false
            }
            
            mPrinter.whenReceiveData({ (data) in
                NSLog("Received Data \(String(describing: data))");
            })
            if (status != true) {
                return false;
            }
            return true
//        } catch {
//            return false
//        }
  }

  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "datecs_printer", binaryMessenger: registrar.messenger())
    let instance = SwiftDatecsPrinterPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    switch call.method {
    case "getPlatformVersion":
        result("iOS " + UIDevice.current.systemVersion)
    case "getListBluetoothDevice":
        bluetoothState = mPrinter.getBluetoothStatus()
        if (bluetoothState != PTBluetoothState.unauthorized) {
            mPrinter.stopScanBluetooth()
            mPrinter.unconnectBlock = nil
            mPrinter.scanBluetooth()
            mPrinter.whenFindAllBluetooth({ [weak self] in
                guard let self = self else { return }
                guard let temp = $0 as? [PTPrinter] else { return }
                //  Rank the devices by distance
                self.devices = temp.sorted(by: { (pt1, pt2) -> Bool in
                    return pt1.distance.floatValue < pt2.distance.floatValue
                })
            })
            var devicesMapList: [[String:String]] = [];
            for device in devices {
                devicesMapList.append([
                    "name": device.name,
                    "address": device.mac,
                    "uuid": device.uuid
                ])
            }
            result(devicesMapList);
        } else {
            result(FlutterError(code: "ERROR 101", message: "Error getting bluetooth devices", details: ""))
        }
    case "connectBluetooth":
        let arguments: [String:String] = call.arguments as! [String:String]
        let address: String = String(describing: arguments["address"] ?? "")
        let name: String = String(describing: arguments["name"] ?? "")
        NSLog("Got Address: \(String(address))")
        NSLog("Got Device Name: \(String(name))")
        do {
            let device: PTPrinter? = devices.first(where: {$0.mac == address || $0.name == name}) ?? nil;
            NSLog("Got Device: \(String(device?.name ?? "None"))")
            if (device != nil) {
                isConnect = try connect(device: device!)
            }
            result(isConnect);
        } catch _ {
            result(isConnect);
        }
    case "disconnectBluetooth":
        do {
            try disconnect();
            result(true)
        } catch {
            NSLog("\(error)");
            result(false);
        }
    case "testPrint":
//        do {
            let esc = PTCommandESC.init()
            esc.initializePrinter()
            esc.printSelfTest()
            let printStatus: Bool = sendDataToDevice(data: esc.getCommandData())
            result(printStatus)
//        } catch {
//            NSLog("\(error)");
//            result(false)
//        }
    case "printText":
        let arguments: [String:[String]] = call.arguments as! [String:[String]]
        let args: [String] = arguments["args"] ?? []
//        do {
            // Initialise Printer with ESC command
            let esc = PTCommandESC.init()
            esc.initializePrinter()

            for arg in args {
                NSLog(arg);
                if (arg.contains("feed%20")) {
                    let split: [String] = arg.components(separatedBy: "%20")
                    let feed: Int = Int(split[1]) ?? 1
                    esc.printAndFeedLines(feed)
                }
                else if (arg.contains("img%2021")) {
                    let split: [String] = arg.components(separatedBy:"%2021")
                    let size: Double = Double(split[1]) ?? 100.0;
                    let img: String = split[2];

                    let imageData = Data(base64Encoded: img)
                    let image: UIImage? = UIImage(data: imageData!)
                    let height: Double = Double(image?.size.height ?? 100.0)
                    let width: Double = Double(image?.size.width ?? 100.0)
                    print("DATECS_PLUGIN", "Image Size");
                    print("DATECS PLUGIN", width);
                    print("DATECS PLUGIN", height);
                    let x: Double;
                    let y: Double;
                    if(width > height){
                        x = size;
                        y = (height * size) / width;
                    } else {
                        y = size;
                        x = (width * size) / height;
                    }
                    let newSize: CGSize = CGSize(width: x, height: y);
                    if (image != nil) {
                        UIGraphicsBeginImageContextWithOptions(newSize, false, 0.0);
                        image!.draw(in: CGRect(x: 0, y: 0, width: newSize.width, height: newSize.height))
                        let scaledImage:UIImage = UIGraphicsGetImageFromCurrentImageContext()!
                        UIGraphicsEndImageContext()
                        esc.setPrinterAutomaticPosition()
                        esc.appendRasterImage(scaledImage.cgImage, mode: .binary, compress: .LZO, package: true)
                    }
                }
                else {
                    var text: String = arg
                    var doubleWidth: Bool = false
                    var doubleHeight: Bool = false
                    var underline: Bool = false
                    var bold: Bool = false
                    var mini: Bool = false
                    var justification: Int = 0
                    if (args.contains("{reset}")) {
                        text = arg.replacingOccurrences(of: "{reset}", with: "")
                        esc.setLinePrintPositionMode(1)
                    }
                    else if (args.contains("{left}")) {
                        text = text.replacingOccurrences(of: "{left}", with: "")
                        justification = 0;
                    }
                    else if (args.contains("{center}")) {
                        text = text.replacingOccurrences(of: "{center}", with: "")
                        justification = 1;
                    }
                    else if (args.contains("{right}")) {
                        text = text.replacingOccurrences(of: "{right}", with: "")
                        justification = 2;
                    }
                    else if (args.contains("{w}")) {
                        text = text.replacingOccurrences(of: "{w}", with: "")
                        doubleWidth = true
                    }
                    else if (args.contains("{h}")) {
                        text = text.replacingOccurrences(of: "{h}", with: "")
                        doubleHeight = true
                    }
                    else if (args.contains("{u}")) {
                        text = text.replacingOccurrences(of: "{u}", with: "")
                        underline = true
                    }
                    else if (args.contains("{b}")) {
                        text = text.replacingOccurrences(of: "{b}", with: "")
                        bold = true
                    }
                    else if (args.contains("{i}")) {
                        text = text.replacingOccurrences(of: "{i}", with: "")
                    }
                    else if (args.contains("{s}")) {
                        text = text.replacingOccurrences(of: "{s}", with: "")
                        mini = true
                    }
                    esc.setJustification(justification)
                    esc.setTextStyleMini(
                        mini,
                        bold: bold,
                        doubleWidth: doubleWidth,
                        doubleHeight: doubleHeight,
                        underline: underline
                    )
                    esc.appendText(arg)
                }
                
            }
            NSLog("Command Count: \(String(esc.getCommandData().count))")
            result(sendDataToDevice(data: esc.getCommandData()))
//        } catch {
//            NSLog("\(error)");
//            result(false)
//        }
    default:
        result(FlutterMethodNotImplemented)
    }
  }

  private func testBillPrint() -> Data {
        let esc = PTCommandESC.init()
        
        esc.initializePrinter()
        esc.appendText("TEST TITLE\n\n", mode: ESCText.doubleWidth)
        
        let companyMessage = "2F,8#,Gaoqi Nan Shi'er Road\n(Aide Industiral Park)\nXiamen,China 361006\n\nTel:+(86)-(0)592-5885993,5756958\nWebsite:http://www.prttech.com\n\n"
        esc.appendText(companyMessage, mode: ESCText.normal)
        
        var receiptText = ""
        receiptText.append("------------------------------\n")
        receiptText.append("           NO.1008            \n")
        receiptText.append("\(Date.init())\n")
        receiptText.append("------------------------------\n")
        receiptText.append("MPT-II          1      $700.00\n")
        receiptText.append("MPT-III         1      $700.00\n")
        receiptText.append("LPQ58           1      $800.00\n")
        receiptText.append("BLE1            1      $800.00\n")
        receiptText.append("BLE123          1      $900.00\n")
        receiptText.append("POS80A          1      $900.00\n")
        receiptText.append("POS80B          1      $950.00\n")
        receiptText.append("POS80C          1      $950.00\n")
        receiptText.append("SMP-R381        1      $850.00\n")
        receiptText.append("SMP-R385        1      $850.00\n")
        receiptText.append("SMP-R386        1      $850.00\n")
        receiptText.append("MPT-II          1      $700.00\n")
        receiptText.append("MPT-III         1      $700.00\n")
        receiptText.append("LPQ58           1      $800.00\n")
        receiptText.append("BLE-14          1      $800.00\n")
        receiptText.append("BLE-23          1      $900.00\n")
        receiptText.append("E300            1      $900.00\n")
        receiptText.append("E200            1      $900.00\n")
        receiptText.append("T300            1      $900.00\n")
        receiptText.append("POS80C          1      $900.00\n")
        receiptText.append("POS80E          1      $900.00\n")
        receiptText.append("POS808          1      $900.00\n")
        receiptText.append("POS801          1      $900.00\n")
        receiptText.append("------------------------------\n\n")
        receiptText.append("Total                $18500.00\n")
        receiptText.append("-------------------------------\n\n")
        esc.appendText(receiptText, mode: ESCText.normal)
        
        esc.append(ESCBarcode.B_UPCA, data: "075678164125", justification: 0, width: 3, height: 30, hri: 2)
        esc.appendQRCodeData("https://itunes.apple.com/lookup?id=1472096775", justification: 0, leftMargin: 0, eccLevel: 48, model: 49, size: 10)
        
        var copyright = ""
        copyright.append("\(Date.init())\n")
        copyright.append("POS Editor:V1.2.0\n")
        copyright.append("Copyright 2018 POS Editor.\n")
        copyright.append("All rights reserved\n\n\n\n")
        esc.appendText(copyright, mode: ESCText.normal)
        
        esc.setFullCutWithDistance(100)
        return esc.getCommandData()
  }
}
