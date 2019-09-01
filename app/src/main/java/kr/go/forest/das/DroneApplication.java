package kr.go.forest.das;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import kr.go.forest.das.drone.DJI;
import kr.go.forest.das.drone.Drone;

public class DroneApplication extends Application{
    private static Application app = null;
    private static Drone drone = null;
    private static Bus bus = new Bus(ThreadEnforcer.ANY);

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

    public static void setDroneInstance(int type) {

        if(type == Drone.DRONE_MANUFACTURE_DJI)
        {
            drone = new DJI();
        }else{

        }
    }

    public static Bus getEventBus() {
        return bus;
    }
}
