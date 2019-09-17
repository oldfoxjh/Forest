package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;
import dji.common.model.LocationCoordinate2D;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.geo.GeoManager;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

public class FlightView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver {

    private final String TAG = "FlightView";
    private final int period = 500;             // 0.5 second
    private Context context;
    Timer timer;
    private Handler handler_ui;

    private int device_width;
    private int device_height;
    private int height;
    private int width;
    private int margin;

    // Map
    private MapView map_view = null;
    Marker marker_drone_location = null;
    Marker marker_home_location = null;
    Marker marker_my_location = null;
    boolean is_map_mini = true;

    Button btn_flight_location;
    Button btn_flight_nofly;
    Button btn_flight_fires;
    Button btn_flight_save_path;

    // Button
    Button btn_upload;
    Button btn_back;

    // TextView
    TextView tv_distance;
    TextView tv_altitude;
    TextView tv_horizontal_speed;
    TextView tv_vertical_speed;

    // 카메라 정보
    TextView tv_iso;
    TextView tv_shutter;
    TextView tv_aperture;
    TextView tv_exposure;
    TextView tv_wb;

    // 카메라 버튼
    Button btn_select_movie;
    Button btn_select_shoot;
    Button btn_record;
    Button btn_shoot;
    Button btn_camera_setting;
    TextView tv_record_time;

    public FlightView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public FlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {
        handler_ui = new Handler(Looper.getMainLooper());

        // 드론 상태 체크
        if(DroneApplication.getDroneInstance() != null) {
            int _drone_status = DroneApplication.getDroneInstance().getDroneStatus();
            if (((_drone_status & Drone.DRONE_STATUS_ARMING) > 0)
                    || ((_drone_status & Drone.DRONE_STATUS_FLYING) > 0)) {
                timer.schedule(new CollectDroneInformationTimer(), 0, period); // 0.5초 단위로 설정
            }

            // 카메라 설정 정보
            DroneApplication.getDroneInstance().getISO();
            DroneApplication.getDroneInstance().getShutterSpeed();
            DroneApplication.getDroneInstance().getAperture();
            DroneApplication.getDroneInstance().getExposureCompensation();
        }

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;
        timer = null;

        map_view.getOverlays().clear();
        map_view = null;

        super.onDetachedFromWindow();
    }

    protected void initUI() {

        DroneApplication.getEventBus().register(this);

        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        setSystemUiVisibility(UI_OPTIONS);

        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.content_flight, this, true);

        setWidget();

         timer = new Timer();

        // MapView 설정
        map_view = (MapView) findViewById(R.id.flight_map_view);
        map_view.setBuiltInZoomControls(false);
        map_view.setMultiTouchControls(true);
        map_view.setMinZoomLevel(8.0);
        map_view.setMaxZoomLevel(20.0);
        map_view.setTileSource(VWorldStreet);
        MapEventsOverlay _events = new MapEventsOverlay(this);
        map_view.getOverlays().add(_events);

        IMapController mapController = map_view.getController();
        mapController.setZoom(17.0);
        mapController.setCenter(new GeoPoint(36.361481, 127.384841));

        // 마커 설정
        marker_my_location = new Marker(map_view);
        marker_my_location.setIcon(ContextCompat.getDrawable(context, R.mipmap.map_ico_my));
        map_view.getOverlays().add(marker_my_location);

        marker_drone_location = new Marker(map_view);
        marker_drone_location.setIcon(ContextCompat.getDrawable(context, R.mipmap.map_ico_drone));
        map_view.getOverlays().add(marker_drone_location);

        marker_home_location = new Marker(map_view);
        marker_home_location.setIcon(ContextCompat.getDrawable(context, R.mipmap.map_ico_mission));
        map_view.getOverlays().add(marker_home_location);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        device_height = displayMetrics.heightPixels;
        device_width = displayMetrics.widthPixels;

        height = (int) getResources().getDimension(R.dimen.px180);
        width = (int) getResources().getDimension(R.dimen.px310);
        margin = (int) getResources().getDimension(R.dimen.px30);

        setClickable(true);
    }

    private void setWidget() {
        // Button
        btn_back = (Button) findViewById(R.id.btn_mission_back);
        btn_back.setOnClickListener(this);

        btn_upload = (Button) findViewById(R.id.btn_mission_upload);
        btn_upload.setVisibility(INVISIBLE);

        // 드론 정보
        tv_distance = (TextView) findViewById(R.id.tv_flight_distance_from_home);
        tv_altitude = (TextView) findViewById(R.id.tv_flight_altitude);
        tv_horizontal_speed = (TextView) findViewById(R.id.tv_flight_horizontal_speed);
        tv_vertical_speed = (TextView) findViewById(R.id.tv_flight_vertical_speed);

        // 카메라 정보
        tv_iso = (TextView) findViewById(R.id.tv_flight_iso);
        tv_shutter = (TextView) findViewById(R.id.tv_flight_shutter);
        tv_aperture = (TextView) findViewById(R.id.tv_flight_aperture);
        tv_exposure = (TextView) findViewById(R.id.tv_flight_exposure);
        tv_wb = (TextView) findViewById(R.id.tv_flight_wb);

        btn_select_movie = (Button) findViewById(R.id.btn_flight_select_movie);
        btn_select_movie.setOnClickListener(this);
        btn_select_shoot = (Button) findViewById(R.id.btn_flight_select_shoot);
        btn_select_shoot.setOnClickListener(this);
        btn_record = (Button) findViewById(R.id.btn_flight_record);
        btn_record.setOnClickListener(this);
        btn_shoot = (Button) findViewById(R.id.btn_flight_shoot);
        btn_shoot.setOnClickListener(this);
        btn_camera_setting = (Button) findViewById(R.id.btn_flight_camera_setting);
        btn_camera_setting.setOnClickListener(this);
        tv_record_time = (TextView) findViewById(R.id.tv_flight_record_time);

        // Map Top
        btn_flight_location = (Button) findViewById(R.id.btn_flight_location);
        btn_flight_nofly = (Button) findViewById(R.id.btn_flight_nofly);
        btn_flight_fires = (Button) findViewById(R.id.btn_flight_fires);
        btn_flight_save_path = (Button) findViewById(R.id.btn_flight_save_path);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_flight_select_shoot:
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            case R.id.btn_flight_select_movie:
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            case R.id.btn_flight_record:
                break;
            case R.id.btn_flight_shoot:
                break;
            case R.id.btn_flight_camera_setting:
                break;
            case R.id.btn_flight_location:
                // 현재 위치로 가운데.
                break;
            case R.id.btn_flight_nofly:
                // 비행금지 제한구역 불러오기
                break;
            case R.id.btn_flight_fires:
                // 산불발생현황 불러오기
                break;
            case R.id.btn_flight_save_path:
                // 비행경로 저장하기
                break;
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        RelativeLayout _layout =  (RelativeLayout) findViewById(R.id.flightMapLayout);
        LinearLayout _camera = (LinearLayout) findViewById(R.id.layout_camera_info);
        LinearLayout _map = (LinearLayout) findViewById(R.id.layout_flight_map_top);
        if(is_map_mini == true)
        {
            ResizeAnimation mapViewAnimation = new ResizeAnimation(_layout, width, height, device_width, device_height, 0);
            _layout.startAnimation(mapViewAnimation);

            _camera.setVisibility(INVISIBLE);
            _map.setVisibility(VISIBLE);
            is_map_mini = false;
        }else{
            ResizeAnimation mapViewAnimation = new ResizeAnimation(_layout, device_width, device_height, width, height, margin);
            _layout.startAnimation(mapViewAnimation);

            _camera.setVisibility(VISIBLE);
            _map.setVisibility(INVISIBLE);
            is_map_mini = true;
        }
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     * 시동 걸리고 0.5초 단위로 기체정보 수집
     */
    private class CollectDroneInformationTimer extends TimerTask
    {
        @Override
        public void run() {
            // 드론 상태 체크
            int _drone_status = DroneApplication.getDroneInstance().getDroneStatus();

            // DaraArray 추가

            // Log 추가

            // 화면 표시
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();
                        // 1. 조종기 or 핸드폰과의  거리
                        LocationCoordinate2D _home = DroneApplication.getDroneInstance().getHomeLocation();
                        tv_distance.setText(String.format("%.2f", GeoManager.getInstance().distance(_home.getLatitude(), _home.getLongitude(), _info.drone_latitude, _info.drone_longitude)));

                        // 2. 고도
                        String _alt = String.format("%.2f", _info.drone_altitude);
                        tv_altitude.setText(_alt);

                        // 3. 수평속도
                        String v_x = String.format("%.2f", _info.drone_velocity_x);
                        tv_horizontal_speed.setText(v_x);

                        // 4. 수직속도
                        String v_z = String.format("%.2f", Math.abs(_info.drone_velocity_z));
                        tv_vertical_speed.setText(v_z);

                        marker_my_location.setPosition(new GeoPoint(_info.rc_latitude, _info.rc_longitude));
                        marker_home_location.setPosition(new GeoPoint(_home.getLatitude(), _home.getLongitude()));
                        marker_drone_location.setPosition(new GeoPoint(_info.drone_latitude, _info.drone_longitude));

                        map_view.invalidate();
                    }
                });
            }

        }
    }

    @Subscribe
    public void onConnectionChange(final MainActivity.DroneStatusChange drone_status) {
        if(drone_status.status == Drone.DRONE_STATUS_CONNECT) {
            LogWrapper.i(TAG, "DRONE_STATUS_CONNECT");
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        IMapController mapController = map_view.getController();
                        mapController.setZoom(17.0);
                        mapController.setCenter(new GeoPoint(36.361481, 127.384841));

                        // Draw Marker

                        // 카메라 촬영 Visible
                        DroneApplication.getDroneInstance().getCameraMode();
                    }
                });
            }
        }else if(drone_status.status == Drone.DRONE_STATUS_ARMING){
            // Data 수집 타이머 시작
            if(timer == null) timer = new Timer();
            timer.schedule(new CollectDroneInformationTimer(), 0, period); // 0.5초 단위로 설정
            LogWrapper.i(TAG, "DRONE_STATUS_ARMING");
        }else if(drone_status.status == Drone.DRONE_STATUS_FLYING){
            LogWrapper.i(TAG, "DRONE_STATUS_FLYING");
        }else if(drone_status.status == Drone.DRONE_STATUS_MISSION){
            LogWrapper.i(TAG, "DRONE_STATUS_MISSION");
        }else if(drone_status.status == Drone.DRONE_STATUS_RETURN_HOME){
            LogWrapper.i(TAG, "DRONE_STATUS_RETURN_HOME");
        }else if(drone_status.status == Drone.DRONE_STATUS_DISARM){
            // Data 수집 타이머 종료
            LogWrapper.i(TAG, "DRONE_STATUS_LANDING");
            if(timer != null) {
                timer.cancel();
                timer = null;
            }
        }else if(drone_status.status == Drone.DRONE_STATUS_DISCONNECT){
            // Data 수집 타이머 종료

            if(timer != null) {
                timer.cancel();
                timer = null;
            }

            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        tv_iso.setText("N/A");
                        tv_shutter.setText("N/A");
                        tv_aperture.setText("N/A");
                        tv_exposure.setText("N/A");
                        tv_wb.setText("N/A");

                        btn_select_movie.setVisibility(INVISIBLE);
                        btn_record.setVisibility(INVISIBLE);
                        tv_record_time.setVisibility(INVISIBLE);
                        btn_shoot.setVisibility(INVISIBLE);
                        btn_select_shoot.setVisibility(INVISIBLE);
                        btn_camera_setting.setVisibility(INVISIBLE);
                    }
                });
            }
        }
    }

    @Subscribe
    public void onCameraStatusChange(final MainActivity.DroneCameraStatus camera)
    {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    if(camera.iso != null) tv_iso.setText(camera.iso);
                    if(camera.shutter != null) tv_shutter.setText(camera.shutter);
                    if(camera.aperture != null) tv_aperture.setText(camera.aperture);
                    if(camera.ev != null) tv_exposure.setText(camera.ev);
                    if(camera.wb != null) tv_wb.setText(camera.wb);
                    if(camera.mode == 0) {
                        btn_select_shoot.setVisibility(VISIBLE);
                        btn_shoot.setVisibility(VISIBLE);
                        btn_camera_setting.setVisibility(VISIBLE);

                        if(btn_select_movie.getVisibility() == VISIBLE) btn_select_movie.setVisibility(INVISIBLE);
                        if(btn_record.getVisibility() == VISIBLE) btn_record.setVisibility(INVISIBLE);
                        if(tv_record_time.getVisibility() == VISIBLE) tv_record_time.setVisibility(INVISIBLE);
                    }else if(camera.mode == 1){
                        btn_select_movie.setVisibility(VISIBLE);
                        btn_record.setVisibility(VISIBLE);
                        btn_camera_setting.setVisibility(VISIBLE);
                        tv_record_time.setVisibility(VISIBLE);

                        if(btn_shoot.getVisibility() == VISIBLE) btn_shoot.setVisibility(INVISIBLE);
                        if(btn_select_shoot.getVisibility() == VISIBLE) btn_select_shoot.setVisibility(INVISIBLE);
                    }
                }
            });
        }
    }

    private class ResizeAnimation extends Animation {

        private View view;
        private int to_height;
        private int from_height;

        private int to_width;
        private int from_width;
        private int margin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            to_height = toHeight;
            to_width = toWidth;
            from_height = fromHeight;
            from_width = fromWidth;
            view = v;
            margin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (to_height - from_height) * interpolatedTime + from_height;
            float width = (to_width - from_width) * interpolatedTime + from_width;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) view.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.leftMargin = margin;
            p.bottomMargin = margin;
            view.requestLayout();
        }
    }
}
