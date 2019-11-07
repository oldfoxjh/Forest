package kr.go.forest.das.Model;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

public class DeviceInfo {

    /**
     * 모바일 장치의 Serial Number를 불러온다.
     * @return SN 문자열
     */
    public static String getSerialNumber(){
        String serial_number = "";
        try{
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);

            serial_number = (String) get.invoke(c, "gsm.sn1");

            if (serial_number.equals(""))
                // Samsung Galaxy S5 (SM-G900F) : 6.0.1
                // Samsung Galaxy S6 (SM-G920F) : 7.0
                // Samsung Galaxy Tab 4 (SM-T530) : 5.0.2
                // (?) Samsung Galaxy Tab 2 (https://gist.github.com/jgold6/f46b1c049a1ee94fdb52)
                serial_number = (String) get.invoke(c, "ril.serialnumber");

            if (serial_number.equals(""))
                // Archos 133 Oxygen : 6.0.1
                // Google Nexus 5 : 6.0.1
                // Hannspree HANNSPAD 13.3" TITAN 2 (HSG1351) : 5.1.1
                // Honor 5C (NEM-L51) : 7.0
                // Honor 5X (KIW-L21) : 6.0.1
                // Huawei M2 (M2-801w) : 5.1.1
                // (?) HTC Nexus One : 2.3.4 (https://gist.github.com/tetsu-koba/992373)
                serial_number = (String) get.invoke(c, "ro.serialno");

            if (serial_number.equals(""))
                // (?) Samsung Galaxy Tab 3 (https://stackoverflow.com/a/27274950/1276306)
                serial_number = (String) get.invoke(c, "sys.serialnumber");

            if (serial_number.equals(""))
                // Archos 133 Oxygen : 6.0.1
                // Hannspree HANNSPAD 13.3" TITAN 2 (HSG1351) : 5.1.1
                // Honor 9 Lite (LLD-L31) : 8.0
                // Xiaomi Mi 8 (M1803E1A) : 8.1.0

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    serial_number = getOserial();
                else
                    serial_number = Build.SERIAL;

            // If none of the methods above worked
            if (serial_number.equals(""))
                serial_number = null;
        }catch (Exception e) {
            e.printStackTrace();
            serial_number = null;
        }

        return serial_number;
    }

    /**
     * 안드로이드 플랫폼 버전 8.0 이상일 때
     * @return SN 문자열
     */
    @TargetApi(Build.VERSION_CODES.O)
    private static String getOserial() {
        String serial_number = "";
        try
        {
            serial_number = Build.getSerial();
        }catch(SecurityException e)
        {
            e.printStackTrace();
        }

        return serial_number;
    }

    /**
     * 모바일 장치의 IMEI 값을 불러온다.
     * @param ctx : Device Context
     * @return : IMEI 문자열
     */
    public static String getIMEI(Context ctx) {
        String IMEINumber = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    IMEINumber = tm.getImei();
                }
            } else {
                IMEINumber = tm.getDeviceId();
            }
        }

        return IMEINumber;
    }

    public static String getPhoneNumber(Context ctx){
        String number = "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)
        {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            number = tm.getLine1Number();
            if(number != null && number.length() > 0 && number.startsWith("+82")){
                number = number.replace("+82", "0");
            }
        }

        return number;
    }
}
