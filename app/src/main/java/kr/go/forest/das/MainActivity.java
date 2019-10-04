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
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import dji.common.camera.SettingsDefinitions;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.UI.DialogConfirm;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.UI.DialogLoadShape;
import kr.go.forest.das.UI.DialogOk;
import kr.go.forest.das.UI.DialogUploadMission;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;

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

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private List<String> missingPermission = new ArrayList<>();

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
            return;
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
                if(popup.type == PopupDialog.DIALOG_TYPE_OK) {
                    wrapper = new ViewWrapper(new DialogOk(MainActivity.this, popup.contentId, popup.message), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_CONFIRM) {
                    wrapper = new ViewWrapper(new DialogConfirm(MainActivity.this, popup.titleId, popup.contentId), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_LOAD_SHAPE) {
                    wrapper = new ViewWrapper(new DialogLoadShape(MainActivity.this), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_LOAD_MISSION) {
                    wrapper = new ViewWrapper(new DialogConfirm(MainActivity.this, 0, popup.contentId), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_UPLOAD_MISSION){
                    wrapper = new ViewWrapper(new DialogUploadMission(MainActivity.this), false);
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

                if(popup.type == PopupDialog.DIALOG_TYPE_LOAD_SHAPE) {
                    DroneApplication.getEventBus().post(new Mission(Mission.MISSION_FROM_FILE, popup.data));
                }else if(popup.type == PopupDialog.DIALOG_TYPE_LOAD_MISSION){
                    DroneApplication.getEventBus().post(new Mission(Mission.MISSION_FROM_ONLINE, popup.data));
                }else if(popup.type == PopupDialog.DIALOG_TYPE_UPLOAD_MISSION){
                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_UPLOAD, null));
                }else if(popup.type == PopupDialog.DIALOG_TYPE_CONFIRM){
                    if(popup.command == Mission.MISSION_CLEAR) {
                        DroneApplication.getEventBus().post(new Mission(Mission.MISSION_CLEAR, null));
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_REQUEST_TAKEOFF) {
                        DroneApplication.getEventBus().post(new ReturnHome(ReturnHome.REQUEST_TAKEOFF, null));
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_REQUEST_LANDING) {
                        DroneApplication.getEventBus().post(new ReturnHome(ReturnHome.REQUEST_LANDING, null));
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_CANCEL_LANDING) {
                        DroneApplication.getEventBus().post(new ReturnHome(ReturnHome.CANCEL_LANDING, null));
                    } else if(popup.command == PopupDialog.DIALOG_TYPE_START_RETURN_HOME) {
                        DroneApplication.getEventBus().post(new ReturnHome(ReturnHome.REQUEST_RETURN_HOME, null));
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_CANCEL_RETURN_HOME) {
                        DroneApplication.getEventBus().post(new ReturnHome(ReturnHome.CANCEL_RETURN_HOME, null));
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_SET_RETURN_HOME_LOCATION) {
                        DroneApplication.getEventBus().post(new ReturnHome(ReturnHome.SET_RETURN_HOME_LOCATION, null));
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_MAX_FLIGHT_HEIGHT_LOW) {
                        DroneApplication.getDroneInstance().setMaxFlightHeight(500);
                    }else if(popup.command == PopupDialog.DIALOG_TYPE_START_MISSION){
                        DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_START_SUCCESS, null));
                    }
                }
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

    public static class PopView{
    }

    public static class PopupDialog {

        public final static int DIALOG_TYPE_OK = 0x10;
        public final static int DIALOG_TYPE_CONFIRM = 0x11;
        public final static int DIALOG_TYPE_LOAD_SHAPE = 0x12;
        public final static int DIALOG_TYPE_LOAD_MISSION = 0x13;
        public final static int DIALOG_TYPE_REQUEST_TAKEOFF = 0x14;
        public final static int DIALOG_TYPE_REQUEST_LANDING = 0x15;
        public final static int DIALOG_TYPE_CANCEL_LANDING = 0x16;
        public final static int DIALOG_TYPE_CONFIRM_LANDING = 0x17;
        public final static int DIALOG_TYPE_START_RETURN_HOME = 0x18;
        public final static int DIALOG_TYPE_CANCEL_RETURN_HOME = 0x19;
        public final static int DIALOG_TYPE_SET_RETURN_HOME_LOCATION = 0x20;
        public final static int DIALOG_TYPE_UPLOAD_MISSION = 0x21;
        public final static int DIALOG_TYPE_MAX_FLIGHT_HEIGHT_LOW = 0x22;
        public final static int DIALOG_TYPE_START_MISSION = 0x23;

        public int type;
        public int contentId;
        public int titleId;
        public String message;

        public PopupDialog(int type, int titleId, int contentId){
            this.type = type;
            this.titleId = titleId;
            this.contentId = contentId;
        }

        public PopupDialog(int type, int titleId, int contentId, String msg){
            this.type = type;
            this.titleId = titleId;
            this.contentId = contentId;
            message = msg;
        }
    }

    public static class PopdownView {
        public int type = 0;
        public int command = 0;
        public String data = null;

        public PopdownView(){
        }

        public PopdownView(int _type, int _cmd, String _data){
            type = _type;
            data = _data;
            command = _cmd;
        }
    }

    public static class DroneStatusChange{
        public int status;
        public DroneStatusChange(int drone_status)
        {
            status = drone_status;
        }
    }

    public static class DroneCameraStatus{
        public int mode = -1;

        public DroneCameraStatus(){}

        public void setMode(int _mode){ mode = _mode; }
    }

    public static class DroneCameraShootInfo{
        public SettingsDefinitions.CameraMode mode;
        public boolean is_record = false;
        public DroneCameraShootInfo(SettingsDefinitions.CameraMode _mode, boolean isRecord){
            mode = _mode;
            is_record = isRecord;
        }
    }

    public static class LocationUpdate {
        public double latitude;
        public double longitude;

        public LocationUpdate(double lat, double lng)
        {
            latitude = lat;
            longitude = lng;
        }
    }

    public static class Mission{
        public final static int MISSION_CLEAR = 0x20;
        public final static int MISSION_FROM_FILE = 0x21;
        public final static int MISSION_FROM_ONLINE = 0x22;

        public final static int MISSION_UPLOAD = 0x23;
        public final static int MISSION_UPLOAD_FAIL = 0x24;
        public final static int MISSION_UPLOAD_SUCCESS = 0x25;

        public final static int MISSION_START_FAIL = 0x26;
        public final static int MISSION_START_SUCCESS = 0x27;
        public final static int MAX_FLIGHT_HEIGHT_SET_SUCCESS = 0x28;

        public int command = 0;
        public String data = null;

        public Mission(int cmd, String _data)
        {
            command = cmd;
            data = _data;
        }
    }

    public static class ReturnHome{

        public final static int REQUEST_TAKEOFF = 0x30;
        public final static int REQUEST_TAKEOFF_FAIL = 0x31;
        public final static int REQUEST_TAKEOFF_SUCCESS = 0x32;

        public final static int REQUEST_LANDING = 0x33;
        public final static int REQUEST_LANDING_FAIL = 0x34;
        public final static int REQUEST_LANDING_SUCCESS = 0x35;

        public final static int CANCEL_LANDING = 0x36;
        public final static int CANCEL_LANDING_FAIL = 0x37;
        public final static int CANCEL_LANDING_SUCCESS = 0x38;

        public final static int CONFIRM_LANDING = 0x39;
        public final static int CONFIRM_LANDING_FAIL = 0x40;
        public final static int CONFIRM_LANDING_SUCCESS = 0x41;

        public final static int LANDING_SUCCESS = 0x38;

        public final static int REQUEST_RETURN_HOME = 0x50;
        public final static int REQUEST_RETURN_HOME_FAIL = 0x51;
        public final static int REQUEST_RETURN_HOME_SUCCESS = 0x52;

        public final static int CANCEL_RETURN_HOME = 0x53;
        public final static int CANCEL_RETURN_HOME_FAIL = 0x54;
        public final static int CANCEL_RETURN_HOME_SUCCESS = 0x55;

        public final static int SET_RETURN_HOME_LOCATION = 0x56;
        public final static int SET_RETURN_HOME_LOCATION_FAIL = 0x57;
        public final static int SET_RETURN_HOME_LOCATION_SUCCESS = 0x58;

        public int mode;
        public String message;

        public ReturnHome(int mode, String msg){
            this.mode = mode;
            message = msg;
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
