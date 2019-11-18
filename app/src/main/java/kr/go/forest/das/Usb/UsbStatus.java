package kr.go.forest.das.Usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.List;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.drone.Drone;

public class UsbStatus {
    public  static  final int USB_DISCONNECTED = 0;
    public  static  final int USB_ACCESSORY_CONNECTED = 1;
    public  static  final int USB_CONNECTED = 2;

    private static final UsbStatus instance = new UsbStatus();
    public static UsbStatus getInstance() {
        return instance;
    }

    public static int isConnected = USB_DISCONNECTED;

    public static class UsbStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("UsbStateReceiver", "" + intent.getExtras().getBoolean("connected"));

            if(intent.getExtras().getBoolean("connected") && isConnected == USB_DISCONNECTED)
            {
                UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                // check accessory for dji
                UsbAccessory[] accessories = manager.getAccessoryList();

                if(accessories != null && accessories.length > 0)
                {
                    UsbAccessory accessory = (accessories == null ? null : accessories[0]);

                    if(accessory.getManufacturer().contains("DJI"))
                    {
                        DroneApplication.setDroneInstance(Drone.DRONE_MANUFACTURE_DJI);
                        isConnected = USB_ACCESSORY_CONNECTED;
                    }
                }
            }else{
                isConnected = USB_DISCONNECTED;
            }
        }
    }
}
