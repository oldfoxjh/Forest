package kr.go.forest.das.Usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.widget.Toast;

import java.util.HashMap;

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

    public interface UsbStatusCallbacks {
        void onReceive(int status, int type);
    }

    public UsbStatusCallbacks m_cb;

    public  void setUsbStatusCallbacks(UsbStatusCallbacks cb)
    {
        m_cb=cb;
    }

    public static class UsbStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(UsbStatus.getInstance().m_cb != null) {
                if(intent.getExtras().getBoolean("connected"))
                {
                    Toast.makeText(context, "connected", Toast.LENGTH_LONG).show();
                    UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                    // check accessory for dji
                    UsbAccessory[] accessories = manager.getAccessoryList();

                    if(accessories != null && accessories.length > 0)
                    {
                        UsbAccessory accessory = (accessories == null ? null : accessories[0]);

                        Toast.makeText(context, accessory.getDescription() + " " + accessory.getManufacturer()
                                + " " + accessory.getModel() + " " + accessory.getVersion(), Toast.LENGTH_LONG).show();
                        UsbStatus.getInstance().m_cb.onReceive(UsbStatus.USB_CONNECTED, UsbStatus.USB_ACCESSORY);
                        DroneApplication.setDroneInstance(Drone.DRONE_MANUFACTURE_DJI);
                    }

                    // check accessory for pixhawk
                    HashMap<String, UsbDevice> devices = manager.getDeviceList();

                    if(devices != null && devices.size() > 0)
                    {

                    }
                }else{
                    Toast.makeText(context, "disconnected", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
