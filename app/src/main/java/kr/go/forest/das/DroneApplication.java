package kr.go.forest.das;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dji.sdk.products.Aircraft;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.drone.DJI;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;
import kr.go.forest.das.network.IApiService;

public class DroneApplication extends Application{
    private static Application app = null;
    private static Drone drone = null;
    private static Bus bus = new Bus(ThreadEnforcer.ANY);
    private static IApiService api = IApiService.retrofit.create(IApiService.class);
    private static BigdataSystemInfo system_info = new BigdataSystemInfo();

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        com.secneo.sdk.Helper.install(this);

        app = this;
    }

    public static Application getInstance() {
        return DroneApplication.app;
    }

    public static Drone getDroneInstance() {
        return drone;
    }

    public static IApiService getApiInstance(){ return api; }

    public static synchronized void setDroneInstance(int type) {
        if(drone != null) drone = null;

        if(type == Drone.DRONE_MANUFACTURE_DJI) {
            drone = new DJI();
        }else{
            drone = new Px4();
        }
    }

    public static Bus getEventBus() {
        return bus;
    }

    public static BigdataSystemInfo getSystemInfo(){ return system_info; }
}
