package kr.go.forest.das;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.UI.DialogConfirm;
import kr.go.forest.das.map.MapLayer;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.UI.DialogOk;
import kr.go.forest.das.Usb.UsbStatus;

import static kr.go.forest.das.drone.DJI.registrationCallback;

public class MainActivity extends AppCompatActivity implements  LocationListener //UsbStatus.UsbStatusCallbacks,
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };

    private SCREEN_MODE currentScreenMode = SCREEN_MODE.SCREEN_LOGIN;
    enum SCREEN_MODE{
        SCREEN_LOGIN,
        SCREEN_MENU,
        SCREEN_MISSIION,
        SCREEN_FLIGHT,
        SCREEN_SETTING
    };

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private BaseProduct mProduct;
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    private Stack<ViewWrapper> stack;
    private FrameLayout contentFrameLayout;
    private ObjectAnimator pushInAnimator;
    private ObjectAnimator pushOutAnimator;
    private ObjectAnimator popInAnimator;
    private LayoutTransition popOutTransition;
    LocationManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DroneApplication.getEventBus().register(this);
        setContentView(R.layout.activity_main);

        checkAndRequestPermissions();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        contentFrameLayout = (FrameLayout) findViewById(R.id.framelayout_content);
       // UsbStatus.getInstance().setUsbStatusCallbacks(this);

        initParams();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }

    private void initParams() {
        setupInAnimations();

        stack = new Stack<ViewWrapper>();
        View view = contentFrameLayout.getChildAt(0);
        stack.push(new ViewWrapper(view, true));
    }

    private void setupInAnimations() {
        pushInAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.slide_in_right);
        pushOutAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fade_out);
        popInAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.fade_in);
        ObjectAnimator popOutAnimator =
                (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.slide_out_right);

        pushOutAnimator.setStartDelay(100);

        popOutTransition = new LayoutTransition();
        popOutTransition.setAnimator(LayoutTransition.DISAPPEARING, popOutAnimator);
        popOutTransition.setDuration(popOutAnimator.getDuration());
    }

    //region 퍼미션 획득
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            setLocationManager();
            // DJI SDK 등록
            startSDKRegistration();
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && missingPermission.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (!missingPermission.isEmpty()) {
            LogWrapper.i(TAG, "Missing permissions!!!");
        }else{
            setLocationManager();
            // DJI SDK 등록
            startSDKRegistration();
        }
    }
//region 위치 관리
    private  void setLocationManager()
    {
        mManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청..다시..
        }

        List<String> providers = mManager.getAllProviders();
        for(int i = 0; i < providers.size(); i++)
        {
            mManager.requestLocationUpdates(providers.get(i), 500, 0.0f, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location != null)
        {
            DroneApplication.getEventBus().post(new LocationUpdate(location.getLatitude(), location.getLongitude()));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    //endregion
    //endregion

    //region OTTO Event Subscribe
    @Subscribe
    public void onPushView(final ViewWrapper wrapper) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pushView(wrapper);
            }
        });
    }

    @Subscribe
    public void onPopup(final PopupDialog popup) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                ViewWrapper wrapper = null;
                if(popup.type == PopupDialog.DIALOG_TYPE_OK)
                {
                    wrapper = new ViewWrapper(new DialogOk(MainActivity.this, popup.contentId), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_CONFIRM)
                {
                    wrapper = new ViewWrapper(new DialogConfirm(MainActivity.this, popup.contentId), false);
                }

                pushView(wrapper);
            }
        });
    }

    @Subscribe
    public void onPopdown(final PopdownView popup) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                popView();
            }
        });
    }

    private void pushView(ViewWrapper wrapper) {
        if (stack.size() <= 0) {
            return;
        }

        contentFrameLayout.setLayoutTransition(null);

        View showView = wrapper.getView();

        View preView = stack.peek().getView();

        stack.push(wrapper);

        if (showView.getParent() != null) {
            ((ViewGroup) showView.getParent()).removeView(showView);
        }
        contentFrameLayout.addView(showView);

        if(wrapper.isAnimation()) {
            pushOutAnimator.setTarget(preView);
            pushOutAnimator.start();

            pushInAnimator.setTarget(showView);
            pushInAnimator.setFloatValues(contentFrameLayout.getWidth(), 0);
            pushInAnimator.start();
        }
    }

    private void popView() {

        if (stack.size() <= 1) {
            finish();
            return;
        }

        ViewWrapper removeWrapper = stack.pop();
        ViewWrapper showWrapper = stack.peek();


        if(removeWrapper.isAnimation())
            contentFrameLayout.setLayoutTransition(popOutTransition);

        contentFrameLayout.removeView(removeWrapper.getView());

        if(removeWrapper.isAnimation()) {
            popInAnimator.setTarget(showWrapper.getView());
            popInAnimator.start();
        }
    }

    public static class PopView{ }

    public static class PopupDialog {

        public final static int DIALOG_TYPE_OK = 0;
        public final static int DIALOG_TYPE_CONFIRM = 1;

        public int type;
        public int contentId;

        public PopupDialog(int type, int contentId){
            this.type = type;
            this.contentId = contentId;
        }
    }

    public static class PopdownView { }

    public static class LocationUpdate {
        public double latitude;
        public double longitude;

        public LocationUpdate(double lat, double lng)
        {
            latitude = lat;
            longitude = lng;
        }
    }
    //endregion

    //region DJI SDK
    private void startSDKRegistration() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                DJISDKManager.getInstance().registerApp(MainActivity.this, registrationCallback);
            }
        });
    }
    //endregion

}
