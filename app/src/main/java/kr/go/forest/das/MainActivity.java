package kr.go.forest.das;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.Usb.UsbStatus;

public class MainActivity extends AppCompatActivity implements UsbStatus.UsbStatusCallbacks{

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

    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private static final int REQUEST_PERMISSION_CODE = 12345;
    private BaseProduct mProduct;
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);

        setContentView(R.layout.activity_main);
        UsbStatus.getInstance().setUsbStatusCallbacks(this);
    }

    //region 이벤트 처리
    /**
     * 로그인 진행 버튼 클릭함수
     * @param v
     */
    public void processLogin(View v){

        // 네트워크 상태 체크
        // 로그인 요청

        // 로그인 오류 팝업


        changeLayout(SCREEN_MODE.SCREEN_MENU);
    }

    /**
     * 미션화면으로 진행
     * @param v
     */
    public void processMission(View v) {
        changeLayout(SCREEN_MODE.SCREEN_MISSIION);
    }

    /**
     * 비행화면으로 진행
     * @param v
     */
    public void processFlight(View v) {
        changeLayout(SCREEN_MODE.SCREEN_FLIGHT);
    }

    /**
     * 셋팅화면으로 진행
     * @param v
     */
    public void processSetting(View v) {
        //changeLayout(SCREEN_MODE.SCREEN_SETTING);
    }

    /**
     * 이전화면으로 전환
     * @param v
     */
    public void missionBack(View v) {
        changeLayout(SCREEN_MODE.SCREEN_MENU);
    }
    //endregion

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
        }
    }
    //endregion


    @Override
    public void onReceive(int status, int type) {
        Toast.makeText(MainActivity.this, "" + status, Toast.LENGTH_SHORT).show();

        if(status == UsbStatus.USB_CONNECTED)
        {
            if(type == UsbStatus.USB_ACCESSORY)
            {
                // check accessory or device
                UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
                UsbAccessory[] accessories = manager.getAccessoryList();
                UsbAccessory accessory = (accessories == null ? null : accessories[0]);

                if(accessory.getManufacturer().equals("DJI"))
                {
                    LogWrapper.i(TAG, "start DJI SDK Registration");
                    startSDKRegistration();
                }
            }
            else if(type == UsbStatus.USB_DEVICE)
            {

            }
        }
    }

    //region View 관리
    /**
     * 모드에 따른 뷰 가져오기
     * @param mode
     * @return
     */
    public View getContentView(SCREEN_MODE mode){
        switch (mode){
            case SCREEN_LOGIN:
                return findViewById(R.id.loginLayout);
            case SCREEN_MISSIION:
                return findViewById(R.id.missionLayout);
            case SCREEN_FLIGHT:
                return findViewById(R.id.flightLayout);
            case SCREEN_MENU:
                return findViewById(R.id.menuLayout);
            case SCREEN_SETTING:
                return findViewById(R.id.settingLayout);
            default:
                return null;
        }
    }

    /**
     * 뷰 체인지
     * @param changeMode 변경할 뷰모드
     */
    public void changeLayout(SCREEN_MODE changeMode){
        View currentView = getContentView(currentScreenMode);
        View nextView = getContentView(changeMode);

        //현재 뷰의 화면을 숨김 처리
        currentView.setVisibility(View.INVISIBLE);

        //뷰 바꿔치기
        nextView.setVisibility(View.VISIBLE);

        //뷰 화면처리
        currentScreenMode = changeMode;
        initLayout();
    }

    /**
     * 현재 활성화된 뷰 초기화
     */
    private void initLayout(){
        switch (currentScreenMode){
            case SCREEN_MENU:

                break;
            case SCREEN_LOGIN:

                break;
            case SCREEN_FLIGHT:

                break;
            case SCREEN_SETTING:

                break;
            case SCREEN_MISSIION:

                break;
        }
    }
    //endregion

    //region DJI SDK
    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Handler mHandler = new Handler(Looper.getMainLooper());
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LogWrapper.i(TAG, "Registering, pls wait...");
                        }
                    }, 0);
                    DJISDKManager.getInstance().registerApp(MainActivity.this.getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                LogWrapper.i(TAG, "App registration");
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                LogWrapper.i(TAG, "SDK Registration Failed. Please check the bundle ID and your network");

                                // 팝업 띄워야 됨.
                            }
                            LogWrapper.i(TAG, djiError.getDescription());
                        }
                        @Override
                        public void onProductDisconnect() {
                            LogWrapper.i(TAG, "onProductDisconnect");
                        }
                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            mProduct = DroneApplication.getDroneInstance().getProductInstance();
                            if (mProduct.isConnected()) {
                                Toast.makeText(getApplicationContext(), "Aircraft connected : " + mProduct.getModel().getDisplayName(), Toast.LENGTH_LONG).show();

                            } else if (mProduct instanceof Aircraft){
                                Aircraft aircraft = (Aircraft) mProduct;
                                if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                                    Toast.makeText(getApplicationContext(), "Status: Only RC Connected", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey,
                                                      BaseComponent oldComponent,
                                                      BaseComponent newComponent) {
                            if (newComponent != null) {
                                newComponent.setComponentListener(mDJIComponentListener);
                            }
                            Toast.makeText(getApplicationContext(), "onComponentChange", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {
                            Toast.makeText(getApplicationContext(), "onInitProcess", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }
    //endregion

    //region Wowza SDK

    //endregion

}
