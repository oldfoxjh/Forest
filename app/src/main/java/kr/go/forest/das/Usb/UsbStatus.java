package kr.go.forest.das.Usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import java.util.HashMap;

import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.drone.Drone;

public class UsbStatus {
    public  static  final int USB_ACCESSORY = 0;
    public  static  final int USB_DEVICE = 1;
    public  static  final int USB_DISCONNECTED = 0;
    public  static  final int USB_CONNECTED = 1;

    private static final UsbStatus instance = new UsbStatus();
    public static UsbStatus getInstance() {
        return instance;
    }

    private static int isConnected = USB_DISCONNECTED;

    public static class UsbStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //if(UsbStatus.getInstance().m_cb != null)
            {
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
                            isConnected = USB_CONNECTED;

                            Toast.makeText(context, accessory.getDescription() + " " + accessory.getManufacturer()
                                    + " " + accessory.getModel() + " " + accessory.getVersion(), Toast.LENGTH_LONG).show();
                        }
                    }

                    // check accessory for pixhawk
                    HashMap<String, UsbDevice> devices = manager.getDeviceList();
                    if(devices != null && devices.size() > 0)
                    {

                    }


                }else{
                    isConnected = USB_DISCONNECTED;
                }
            }
        }
    }
}
