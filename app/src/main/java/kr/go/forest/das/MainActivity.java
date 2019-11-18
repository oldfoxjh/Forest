package kr.go.forest.das;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.Stick;
import dji.common.camera.SettingsDefinitions;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MAVLink.MavDataManager;
import kr.go.forest.das.UI.DialogCheckRealtime;
import kr.go.forest.das.UI.DialogConfirm;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.UI.DialogLoadMission;
import kr.go.forest.das.UI.DialogLoadShape;
import kr.go.forest.das.UI.DialogOk;
import kr.go.forest.das.UI.DialogSelectDrone;
import kr.go.forest.das.UI.DialogShootingPurpose;
import kr.go.forest.das.UI.DialogUploadMission;
import kr.go.forest.das.Usb.UsbStatus;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;

public class MainActivity extends AppCompatActivity implements  LocationListener
{
    private static final String TAG = MainActivity.class.getSimpleName();

    private Stack<ViewWrapper> stack;
    private FrameLayout contentFrameLayout;
    private ObjectAnimator pushInAnimator;
    private ObjectAnimator pushOutAnimator;
    private ObjectAnimator popInAnimator;
    private LayoutTransition popOutTransition;
    LocationManager mManager;
    boolean doubleBackToExitPressedOnce = false;
    TextToSpeech tts;                                           // Test to Speech

    private Timer timer;
    MavDataManager mav_manager;
    UsbDeviceConnection usb_connection;
    private boolean is_permission = false;

    List<String> shooting_purpose = new ArrayList<>();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DroneApplication.getEventBus().register(this);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        contentFrameLayout = findViewById(R.id.framelayout_content);
        setLocationManager();
        initParams();

        // USB 연결 확인
        Drone _drone = DroneApplication.getDroneInstance();
        if(_drone != null && _drone instanceof Px4){
            mav_manager = new MavDataManager(this, MavDataManager.BAUDRATE_115200, (Px4)_drone);
        }

        // USB 연결된 상태로 부팅했을 경우 연결 체크 타이머 시작
        timer = new Timer();
        timer.schedule(new UsbCheckTimer(), 2000, 1000);
    }

    @Override
    protected void onDestroy() {
        // TTS 해제
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }

        // USB 관련 내용 해제
        if(usb_connection != null) {
            usb_connection.close();
            usb_connection = null;
        }
        if(mav_manager != null) {
            mav_manager.close();
            mav_manager = null;
        }

        if(timer != null){
            timer.cancel();
            timer = null;
        }

        super.onDestroy();
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
        stack.push(new ViewWrapper(view, false));

        // TTS 등록
        tts = new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    //사용할 언어를 설정
                    int result = tts.setLanguage(Locale.KOREA);
                    //언어 데이터가 없거나 혹은 언어가 지원하지 않으면...
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(MainActivity.this, "이 언어는 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        //음성 톤
                        tts.setPitch(0.7f);
                        //읽는 속도
                        tts.setSpeechRate(1.0f);
                    }
                }
            }
        });
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

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            finishAffinity();
            System.runFinalization();
            System.exit(0);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "한번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    //region 위치 관리
    private  void setLocationManager() {
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

    /**
     * Text to speech
     * @param text 텍스트
     */
    private void Speech(String text) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            // API 20
        else
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
    //region OTTO Event Subscribe

    /**
     * Toast 메세지
     * @param object Toast Message 정보
     */
    @Subscribe
    public void onToast(final ToastOrb object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, object.content, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 음성지원 서비스
     * @param object TTS 서비스를 사용한 텍스트를 포함한 객체
     */
    @Subscribe
    public void onTTS(final TTS object) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Speech(object.content);
            }
        });
    }

    /**
     * 뷰 추가 이벤트
     * @param wrapper 추가할 뷰
     */
    @Subscribe
    public void onPushView(final ViewWrapper wrapper) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pushView(wrapper);
            }
        });
    }

    /**
     * 팝업창 시작 이벤트
     * @param popup 팝업창 종류
     */
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
                    wrapper = new ViewWrapper(new DialogLoadMission(MainActivity.this), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_UPLOAD_MISSION){
                    wrapper = new ViewWrapper(new DialogUploadMission(MainActivity.this), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_SELECT_DRONE){
                    wrapper = new ViewWrapper(new DialogSelectDrone(MainActivity.this), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_CHECK_REALTIME){
                    wrapper = new ViewWrapper(new DialogCheckRealtime(MainActivity.this), false);
                }else if(popup.type == PopupDialog.DIALOG_TYPE_SHOOTING_PURPOSE){
                    shooting_purpose.add("선택해주세요.");
                    shooting_purpose.add("재난");
                    shooting_purpose.add("병해충");
                    shooting_purpose.add("사법");
                    wrapper = new ViewWrapper(new DialogShootingPurpose(MainActivity.this, shooting_purpose), false);
                }

                pushView(wrapper);
            }
        });
    }

    /**
     * 뷰 종료 이벤트
     * @param popup
     */
    @Subscribe
    public void onPopdown(final PopdownView popup) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(popup.command == PopupDialog.REMOVE_PRE_VIEW){
                    ViewWrapper removeWrapper = stack.remove(stack.size()-2);
                    contentFrameLayout.removeView(removeWrapper.getView());
                }else {
                    popView();
                }

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
                        DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_START, null));
                    }
                }
            }
        });
    }

    /**
     * 뷰 추가
     * @param wrapper 추가할 뷰
     */
    private void pushView(ViewWrapper wrapper) {
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

    /**
     * 현재 최상위 뷰 제거
     */
    private void popView() {

        if (stack.size() <= 1) {
            finish();
            return;
        }

        ViewWrapper removeWrapper = stack.pop();
        ViewWrapper showWrapper = stack.peek();

        contentFrameLayout.removeView(removeWrapper.getView());

        if(showWrapper.isAnimation()) {
            contentFrameLayout.setLayoutTransition(popOutTransition);
            popInAnimator.setTarget(showWrapper.getView());
            popInAnimator.start();
        }
    }

    public static class PopView{
    }

    public static class PopupDialog {

        public final static int DIALOG_TYPE_OK = 0x1010;
        public final static int DIALOG_TYPE_CONFIRM = 0x1011;
        public final static int DIALOG_TYPE_LOAD_SHAPE = 0x1012;
        public final static int DIALOG_TYPE_LOAD_MISSION = 0x1013;
        public final static int DIALOG_TYPE_REQUEST_TAKEOFF = 0x1014;
        public final static int DIALOG_TYPE_REQUEST_LANDING = 0x1015;
        public final static int DIALOG_TYPE_CANCEL_LANDING = 0x1016;
        public final static int DIALOG_TYPE_CONFIRM_LANDING = 0x1017;
        public final static int DIALOG_TYPE_START_RETURN_HOME = 0x1018;
        public final static int DIALOG_TYPE_CANCEL_RETURN_HOME = 0x1019;
        public final static int DIALOG_TYPE_SET_RETURN_HOME_LOCATION = 0x1020;
        public final static int DIALOG_TYPE_UPLOAD_MISSION = 0x1021;
        public final static int DIALOG_TYPE_MAX_FLIGHT_HEIGHT_LOW = 0x1022;
        public final static int DIALOG_TYPE_START_MISSION_SUCCESS = 0x1023;
        public final static int DIALOG_TYPE_SHOOTING_PURPOSE = 0x1024;
        public final static int DIALOG_TYPE_START_MISSION = 0x1025;
        public final static int REMOVE_PRE_VIEW = 0x1026;
        public final static int DIALOG_TYPE_SELECT_DRONE = 0x1027;
        public final static int DIALOG_TYPE_CHECK_REALTIME = 0x1028;


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
        public final static int MISSION_CLEAR = 0x2020;
        public final static int MISSION_FROM_FILE = 0x2021;
        public final static int MISSION_FROM_ONLINE = 0x2022;

        public final static int MISSION_UPLOAD = 0x2023;
        public final static int MISSION_UPLOAD_FAIL = 0x2024;
        public final static int MISSION_UPLOAD_SUCCESS = 0x2025;

        public final static int MISSION_START_FAIL = 0x2026;
        public final static int MISSION_START_SUCCESS = 0x2027;
        public final static int MAX_FLIGHT_HEIGHT_SET_SUCCESS = 0x2028;
        public final static int MISSION_START = 0x2029;

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

    public static class Realtime{
        public boolean is_realtime = false;

        public Realtime(boolean realtime){
            is_realtime = realtime;
        }
    }

    public static class TTS{
        private String content;
        public TTS(String text){
            content = text;
        }
    }

    public static class ToastOrb{
        public String content;
        public ToastOrb(String text){
            content = text;
        }
    }

    class UsbCheckTimer extends TimerTask {
        @Override
        public void run() {
            UsbManager usb_manager = (UsbManager) getSystemService(android.content.Context.USB_SERVICE);

            if(usb_connection == null) {
                // check USB for pixhawk
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb_manager);
                if (availableDrivers.isEmpty()) {
                    return;
                }
                UsbSerialDriver driver = availableDrivers.get(0);
                usb_connection = usb_manager.openDevice(driver.getDevice());
                if (usb_connection == null) {
                    return;
                }

                // USB 권한 획득
                if(is_permission == false) {
                    PendingIntent mPermissionIntent = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent("kr.go.forest.das.USB_PERMISSION"), 0);
                    usb_manager.requestPermission(driver.getDevice(), mPermissionIntent);

                    is_permission = true;
                }

                if(DroneApplication.getDroneInstance() == null) DroneApplication.setDroneInstance((Drone.DRONE_MANUFACTURE_PIXHWAK));
                if(mav_manager == null) mav_manager = new MavDataManager(MainActivity.this, MavDataManager.BAUDRATE_57600, (Px4)(DroneApplication.getDroneInstance()));
                mav_manager.open(usb_connection, driver.getPorts().get(0));
            }else{
                List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usb_manager);
                if (availableDrivers.isEmpty()) {
                    mav_manager.close();
                    mav_manager = null;

                    usb_connection.close();
                    usb_connection = null;
                }
            }
        }
    }
    //endregion
}
