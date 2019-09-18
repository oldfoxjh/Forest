package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;
import dji.common.model.LocationCoordinate2D;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.StorageInfo;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

public class FlightView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver {

    private final String TAG = "FlightView";
    private final int period = 500;                     // 드론정보 수십 주기 0.5 second
    private Context context;
    Timer timer;                                        // 드론정보 수집 타이머
    private Handler handler_ui;                         // UI 업데이트 핸들러
    private boolean is_recording = false;               // 녹화 여부
    MediaPlayer media_player = null;
    int original_volume;

    private int device_width;                           // 화면 폭
    private int device_height;                          // 화면 높이
    private int height;                                 // view 높이
    private int width;                                  // View 폭
    private int margin;                                 // View 마진

    // Map
    private MapView map_view = null;                    // 지도 뷰
    Marker marker_drone_location = null;                // 드론 위치 정보 마커
    Marker marker_home_location = null;                 // 드론 이륙지점 위치 마커
    Marker marker_my_location = null;                   // 조종자 위치
    boolean is_map_mini = true;                         // 맵 크기
    HashMap<String, Polygon> no_fly_zone = null;        // 비행금지구역 정보
    List<Marker> forest_fires = new ArrayList<Marker>();     // 산불발생지역 정보

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
    TextView tv_flight_format_info;
    TextView tv_flight_capacity;

    SeekBar sb_flight_gimbal_pitch;
    Button btn_flight_ae;
    Button btn_select_movie;
    Button btn_select_shoot;
    Button btn_record;
    Button btn_shoot;
    Button btn_camera_setting;
    TextView tv_record_time;

    // RTH
    Button btn_flight_takeoff;
    Button btn_flight_return_home;
    Button btn_flight_return_my_location;


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

    /**
     * 뷰가 보여지고 나서 처리
     */
    @Override
    protected void onAttachedToWindow() {
        handler_ui = new Handler(Looper.getMainLooper());

        // 오디오 볼륨 최대
        AudioManager audio_manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        original_volume = audio_manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, audio_manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

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
            DroneApplication.getDroneInstance().getCameraMode();
        }

        super.onAttachedToWindow();
    }

    /**
     * 뷰가 종료될 때 처리
     */
    @Override
    protected void onDetachedFromWindow() {
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;
        timer = null;

        if(no_fly_zone != null)
        {
            no_fly_zone.clear();
            no_fly_zone = null;
        }

        if(media_player != null) {
            media_player.release();
            media_player = null;
        }

        forest_fires.clear();
        forest_fires = null;

        map_view.getOverlays().clear();
        map_view = null;

        AudioManager audio_manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        audio_manager.setStreamVolume(AudioManager.STREAM_MUSIC, original_volume, 0);

        super.onDetachedFromWindow();
    }

    /**
     * 지도 및 기타 컨트롤 설정
     */
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

    /**
     * UI 위셋 설정
     */
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
        tv_flight_format_info = (TextView) findViewById(R.id.tv_flight_format_info);
        tv_flight_capacity = (TextView) findViewById(R.id.tv_flight_capacity);

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
        sb_flight_gimbal_pitch = (SeekBar) findViewById(R.id.sb_flight_gimbal_pitch);
        sb_flight_gimbal_pitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        btn_flight_ae = (Button) findViewById(R.id.btn_flight_ae);

        // Map Top
        btn_flight_location = (Button) findViewById(R.id.btn_flight_location);
        btn_flight_location.setOnClickListener(this);
        btn_flight_nofly = (Button) findViewById(R.id.btn_flight_nofly);
        btn_flight_nofly.setOnClickListener(this);
        btn_flight_fires = (Button) findViewById(R.id.btn_flight_fires);
        btn_flight_fires.setOnClickListener(this);
        btn_flight_save_path = (Button) findViewById(R.id.btn_flight_save_path);
        btn_flight_save_path.setOnClickListener(this);

        // RTH
        btn_flight_takeoff = (Button) findViewById(R.id.btn_flight_takeoff);
        btn_flight_takeoff.setOnClickListener(this);
        btn_flight_return_home = (Button) findViewById(R.id.btn_flight_return_home);
        btn_flight_return_home.setOnClickListener(this);
        btn_flight_return_my_location = (Button) findViewById(R.id.btn_flight_return_my_location);
        btn_flight_return_my_location.setOnClickListener(this);
    }

    /**
     * 클릭 이벤트 처리
     */
    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_mission_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.btn_flight_select_shoot:
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
                break;
            case R.id.btn_flight_select_movie:
                DroneApplication.getDroneInstance().setCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                break;
            case R.id.btn_flight_record:
                // 촬영가능용량 확인
                // 촬영중인지..확인
                if(is_recording == true) {
                    DroneApplication.getDroneInstance().stopRecordVideo();
                }else{
                    DroneApplication.getDroneInstance().startRecordVideo();
                }
                break;
            case R.id.btn_flight_shoot:
                // 촬영 용량 확인
                DroneApplication.getDroneInstance().startShootPhoto();
                break;
            case R.id.btn_flight_camera_setting:

                break;
            case R.id.btn_flight_location:
                // 현재 위치로 가운데.
                break;
            case R.id.btn_flight_nofly:
                // 비행금지 제한구역 불러오기
                if(no_fly_zone == null) {
                    no_fly_zone = MapLayer.getInstance().getNoFlyZoneFromValue("");
                }

                for(String key: no_fly_zone.keySet())
                {
                    Polygon _zone = no_fly_zone.get(key);
                    if(map_view.getOverlayManager().contains(_zone)) map_view.getOverlayManager().remove(_zone);
                    else map_view.getOverlayManager().add(_zone);
                }

                map_view.invalidate();
                break;
            case R.id.btn_flight_fires:
                // 산불발생현황 불러오기
                // 요청 다이얼로그

                // 기존에 있는 마커 삭제
                for (Marker _marker : forest_fires) {
                    map_view.getOverlays().remove(_marker);
                }
                forest_fires.clear();

                // 마커 생성
                // request api
                List<GeoPoint> _response = new ArrayList<GeoPoint>();
                _response.add(new GeoPoint(36.361481, 127.384841));
                for( GeoPoint _point : _response){
                    Marker _marker = new Marker(map_view);
                    _marker.setIcon(ResourcesCompat.getDrawable(getResources(), R.mipmap.forest_fire, null));
                    _marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                    _marker.setPosition(_point);
                    map_view.getOverlays().add(_marker);
                    forest_fires.add(_marker);
                }
                map_view.invalidate();

                break;
            case R.id.btn_flight_save_path:
                // 비행경로 저장하기
                Date _date = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("HHmmss", Locale.getDefault());
                _date.setTime(System.currentTimeMillis());
                StringBuilder ret = new StringBuilder(80);
                ret.append(formatter.format(_date));

                String _file_name = ret.toString() + ".shp";

                List<GeoPoint> _lines = new ArrayList<GeoPoint>();
                _lines.add(new GeoPoint(36.361481, 127.384841, 1.1));
                _lines.add(new GeoPoint(36.362235, 127.383603, 1.2));
                _lines.add(new GeoPoint(36.362252, 127.382052, 1.3));
                _lines.add(new GeoPoint(36.362200, 127.379499, 1.4));
                _lines.add(new GeoPoint(36.364714, 127.379553, 1.5));
                _lines.add(new GeoPoint(36.364662, 127.382064, 1.6));
                _lines.add(new GeoPoint(36.364705, 127.384907, 1.7));
                _lines.add(new GeoPoint(36.364800, 127.387654, 1.8));
                _lines.add(new GeoPoint(36.364800, 127.390347, 1.9));

                _lines.add(new GeoPoint(36.362225, 127.390207, 2.9));
                _lines.add(new GeoPoint(36.359788, 127.390143, 1.9));
                _lines.add(new GeoPoint(36.359823, 127.387697, 1.8));
                _lines.add(new GeoPoint(36.362173, 127.387686, 2.9));
                _lines.add(new GeoPoint(36.362225, 127.386173, 11));

                if(GeoManager.getInstance().saveShapeFile(_file_name, _lines) == 0){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, R.string.save_complete));
                }else{
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, R.string.save_fail));
                }
                break;
            case R.id.btn_flight_takeoff:
                // 드론 이륙
                break;
            case R.id.btn_flight_return_home:
                // 드론 이륙위치로 복귀
                break;
            case R.id.btn_flight_return_my_location:
                // 조종기 위치로 복귀
                break;
        }
    }

    /**
     * 지도 위젯 터치 이벤트 처리
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        RelativeLayout _layout =  (RelativeLayout) findViewById(R.id.flightMapLayout);
        LinearLayout _camera = (LinearLayout) findViewById(R.id.layout_camera_info);
        LinearLayout _map = (LinearLayout) findViewById(R.id.layout_flight_map_top);
        RelativeLayout _ae_layout = (RelativeLayout) findViewById(R.id.aeLayout);

        if(is_map_mini == true)
        {
            ResizeAnimation mapViewAnimation = new ResizeAnimation(_layout, width, height, device_width, device_height, 0);
            _layout.startAnimation(mapViewAnimation);

            _camera.setVisibility(INVISIBLE);
            _ae_layout.setVisibility(INVISIBLE);
            sb_flight_gimbal_pitch.setVisibility(INVISIBLE);
            _map.setVisibility(VISIBLE);
            is_map_mini = false;
        }else{
            ResizeAnimation mapViewAnimation = new ResizeAnimation(_layout, device_width, device_height, width, height, margin);
            _layout.startAnimation(mapViewAnimation);

            _camera.setVisibility(VISIBLE);
            _ae_layout.setVisibility(VISIBLE);
            sb_flight_gimbal_pitch.setVisibility(VISIBLE);
            _map.setVisibility(INVISIBLE);
            is_map_mini = true;
        }
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     * 시동 걸리고 0.5초 단위로 기체정보 수집
     */
    private class CollectDroneInformationTimer extends TimerTask {
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
                        tv_flight_format_info.setText("N/A");
                        tv_flight_capacity.setText("N/A");

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
    public void onCameraStatusChange(final MainActivity.DroneCameraStatus camera) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    if(camera.iso != null) tv_iso.setText(camera.iso);
                    if(camera.shutter != null) tv_shutter.setText(camera.shutter);
                    if(camera.aperture != null) tv_aperture.setText(camera.aperture);
                    if(camera.ev != null) tv_exposure.setText(camera.ev);
                    if(camera.wb != null) tv_wb.setText(camera.wb);

                    StorageInfo _info = DroneApplication.getDroneInstance().getStorageInfo();

                    if(camera.mode == 0) {
                        // 사진 촬영
                        tv_flight_format_info.setText(_info.photo_file_format);
                        tv_flight_capacity.setText(_info.capture_count);
                        btn_select_shoot.setVisibility(VISIBLE);
                        btn_shoot.setVisibility(VISIBLE);
                        btn_camera_setting.setVisibility(VISIBLE);

                        if(btn_select_movie.getVisibility() == VISIBLE) btn_select_movie.setVisibility(INVISIBLE);
                        if(btn_record.getVisibility() == VISIBLE) btn_record.setVisibility(INVISIBLE);
                        if(tv_record_time.getVisibility() == VISIBLE) tv_record_time.setVisibility(INVISIBLE);
                    }else if(camera.mode == 1){
                        // 동영상 촬영
                        tv_flight_format_info.setText(_info.video_resolution_framerate);
                        tv_flight_capacity.setText(_info.recording_remain_time);
                        tv_record_time.setText(_info.recording_time);
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

    @Subscribe
    public void onCameraShootInfo(final MainActivity.DroneCameraShootInfo camera){

        if(camera.mode == SettingsDefinitions.CameraMode.RECORD_VIDEO){
            if(camera.is_record == true){
                // 동영상 촬영 시작음
                media_player = MediaPlayer.create(context, R.raw.shoot_photo);
                //media_player.start();
                if (handler_ui != null) {
                    handler_ui.post(new Runnable() {
                        @Override
                        public void run() {
                            btn_record.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_recording_selector));
                        }
                    });
                }
            }else{
                // 동영상 촬영 종료음
                media_player = MediaPlayer.create(context, R.raw.shoot_photo);
                //media_player.start();

                if (handler_ui != null) {
                    handler_ui.post(new Runnable() {
                        @Override
                        public void run() {
                            btn_record.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_record_selector));
                        }
                    });
                }
            }
            is_recording = camera.is_record;
        } else if(camera.mode == SettingsDefinitions.CameraMode.SHOOT_PHOTO){
            // 사진촬영음
            media_player = MediaPlayer.create(context, R.raw.shoot_photo);
            //media_player.start();
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
