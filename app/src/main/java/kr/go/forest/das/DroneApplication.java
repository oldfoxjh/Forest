package kr.go.forest.das;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

public class DroneApplication extends Application{
    private static Application app = null;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        com.secneo.sdk.Helper.install(this);
        app = this;
    }
}
