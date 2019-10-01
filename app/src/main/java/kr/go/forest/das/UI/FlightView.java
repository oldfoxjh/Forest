/**
 드론 영상 빅데이터 관리시스템 - 드론앱서비스
 * 드론 비행정보 화면 관리
 * <p><b>NOTE:</b>
 * @author 정호
 * @since 2019.06.01
 * @version 1.0
 * @see
 *
 * <pre>
 * == 개정이력(Modification Information) ==
 *
 * 수정일     수정자   수정내용
 * -------    -------- ---------------------------
 * 2019.09.01 홍길동   최초 생성
 *
 * </pre>
 */
package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.graphics.Color;
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
import android.view.ViewGroup;
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
import org.osmdroid.views.overlay.Polyline;

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
import dji.sdk.codec.DJICodecManager;
import dji.ux.widget.FPVWidget;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

public class FlightView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver {

    private final String TAG = "FlightView";
    private final int period = 250;                             // 드론정보 수십 주기 0.25 second
    private Context context;
    Timer timer = null;                                         // 드론정보 수집 타이머
    private Handler handler_ui;                                 // UI 업데이트 핸들러
    private boolean is_recording = false;                       // 녹화 여부
    MediaPlayer media_player = null;

    // 배경지도 & 카메라 전환
    private int device_width;                                   // 화면 폭
    private int device_height;                                  // 화면 높이
    private int height;                                         // view 높이
    private int width;                                          // View 폭
    private int margin;                                         // View 마진

    // Map
    private MapView map_view = null;                            // 지도 뷰
    Marker marker_drone_location = null;                        // 드론 위치 정보 마커
    Marker marker_home_location = null;                         // 드론 이륙지점 위치 마커
    Marker marker_my_location = null;                           // 조종자 위치
    boolean is_map_mini = true;                                 // 맵 크기
    HashMap<String, Polygon> no_fly_zone = null;                // 비행금지구역 정보
    List<Marker> forest_fires = new ArrayList<Marker>();             // 산불발생지역 정보
    List<GeoPoint> flight_paths = new ArrayList<GeoPoint>();            // 비행경로
    Polyline flight_path_line = new Polyline();                  // 비행경로 표시

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
    ViewGroup root_view;
    ViewGroup parent_ae;
    FPVWidget primary_camera;
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
    LinearLayout layout_camera;

    // RTH
    Button btn_flight_takeoff;
    Button btn_flight_return_home;
    Button btn_flight_return_my_location;
    Button btn_flight_cancel;

    // 실시간영상
    // Codec for video live view
    protected DJICodecManager mCodecManager = null;

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

        // 드론 상태 체크
        if(DroneApplication.getDroneInstance() != null) {
            if(DroneApplication.getDroneInstance().isFlying())
            {
                // 현재 비행중이므로 Takeoff 아이콘 => Landing 아이콘으로 변경
                btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_landing_selector));
                btn_flight_takeoff.setTag("landing");
            }
            // Data 수집 타이머 시작
            if(timer == null) {
                timer = new Timer();
            }
            timer.schedule(new CollectDroneInformationTimer(), 0, period);

            // 카메라 설정 정보
            DroneApplication.getDroneInstance().getISO();
            DroneApplication.getDroneInstance().getShutterSpeed();
            DroneApplication.getDroneInstance().getAperture();
            DroneApplication.getDroneInstance().getExposureCompensation();
            DroneApplication.getDroneInstance().getWhiteBalance();
            DroneApplication.getDroneInstance().getAELock();
            DroneApplication.getDroneInstance().getAutoAEUnlockEnabled();

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

        if(no_fly_zone != null) {
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
        // 마지막에 저장된 위치 불러오기
        mapController.setZoom(17.0);
        mapController.setCenter(new GeoPoint(36.361481, 127.384841));

        // 마커 설정
        // 내위치
        marker_my_location = new Marker(map_view);
        marker_my_location.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return true;
            }
        });
        marker_my_location.setIcon(ContextCompat.getDrawable(context, R.mipmap.map_ico_my));
        marker_my_location.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        map_view.getOverlays().add(marker_my_location);

        // 드론 위치
        marker_drone_location = new Marker(map_view);
        marker_drone_location.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return true;
            }
        });
        marker_drone_location.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker_drone_location.setIcon(MapLayer.getInstance().getRotateDrawable(context, R.mipmap.map_ico_drone, 0.0f));
        map_view.getOverlays().add(marker_drone_location);

        // 드론 비행경로
        flight_path_line.setColor(Color.RED);
        flight_path_line.setWidth(3.0f);
        map_view.getOverlayManager().add(flight_path_line);

        // 이륙지점
        marker_home_location = new Marker(map_view);
        marker_home_location.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return true;
            }
        });
        marker_home_location.setIcon(ContextCompat.getDrawable(context, R.mipmap.map_ico_mission));
        marker_home_location.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
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
        root_view = findViewById(R.id.root_view);
        primary_camera = (FPVWidget) findViewById(R.id.dji_primary_widget);
        primary_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                singleTapConfirmedHelper(null);
            }
        });

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
        parent_ae = (ViewGroup) findViewById(R.id.aeLayout);
        btn_flight_ae = (Button) findViewById(R.id.btn_flight_ae);
        layout_camera = (LinearLayout) findViewById(R.id.layout_camera_info);

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
        btn_flight_takeoff.setTag("takeoff");
        btn_flight_takeoff.setOnClickListener(this);
        btn_flight_return_home = (Button) findViewById(R.id.btn_flight_return_home);
        btn_flight_return_home.setOnClickListener(this);
        btn_flight_return_my_location = (Button) findViewById(R.id.btn_flight_return_my_location);
        btn_flight_return_my_location.setOnClickListener(this);
        btn_flight_cancel = (Button) findViewById(R.id.btn_flight_cancel);
        btn_flight_cancel.setOnClickListener(this);

        // 드론 임무상태를 체크해서 적용
        if( DroneApplication.getDroneInstance()!= null
                && (DroneApplication.getDroneInstance().getDroneStatus() & Drone.DRONE_STATUS_MISSION) == Drone.DRONE_STATUS_MISSION){
            setCameraWidgetVisible(INVISIBLE);
            btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_landing_selector));
            btn_flight_takeoff.setTag("landing");
        }
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
                if(GeoManager.getInstance().saveShapeFile(_file_name, flight_paths) == 0){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.save_complete));
                }else{
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.save_fail));
                }
                break;
            case R.id.btn_flight_takeoff:
                // 드론 이륙 - 모델별로 메세지 변경해서
                if(btn_flight_takeoff.getTag().equals("takeoff")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.takeoff_title, R.string.phantom_takeoff_content));
                }else if(btn_flight_takeoff.getTag().equals("landing")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.landing_title, R.string.landing_content));
                }
                break;
            case R.id.btn_flight_return_home:
                // 드론 이륙위치로 복귀
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.return_home_title, R.string.return_home_content));
                break;
            case R.id.btn_flight_return_my_location:
                // 조종기 위치로 Home 설정
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.set_home_location_title, R.string.set_home_location_content));
                break;
            case R.id.btn_flight_cancel:
                if(btn_flight_cancel.getTag().equals("landing")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.landing_cancel_title, 0));
                }else if(btn_flight_cancel.getTag().equals("rtl")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.return_home_cancel_title, 0));
                }
                break;
        }
    }

    /**
     * 지도 위젯 터치 이벤트 처리
     */
    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        RelativeLayout _layout =  (RelativeLayout) findViewById(R.id.flightMapLayout);
        LinearLayout _map = (LinearLayout) findViewById(R.id.layout_flight_map_top);

        if(is_map_mini == true)
        {
            resizeFPVWidget(width, height, margin, 11);
            ResizeAnimation mapViewAnimation = new ResizeAnimation(_layout, width, height, device_width, device_height, 0);
            _layout.startAnimation(mapViewAnimation);
            setCameraSettingWidgetVisible(INVISIBLE);
            _map.setVisibility(VISIBLE);
            is_map_mini = false;
        }else{
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);

            ResizeAnimation mapViewAnimation = new ResizeAnimation(_layout, device_width, device_height, width, height, margin);
            _layout.startAnimation(mapViewAnimation);

            setCameraSettingWidgetVisible(VISIBLE);
            _map.setVisibility(INVISIBLE);
            is_map_mini = true;
        }
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return true;
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout flight_fpv_layout = (RelativeLayout) findViewById(R.id.flight_fpv_layout);
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams)flight_fpv_layout.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.leftMargin = margin;
        fpvParams.bottomMargin = margin;
        if (is_map_mini == true) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }

        flight_fpv_layout.setLayoutParams(fpvParams);

        root_view.removeView(flight_fpv_layout);
        root_view.addView(flight_fpv_layout, fpvInsertPosition);
    }

    /**
     * 시동 걸리고 0.5초 단위로 기체정보 수집
     */
    private class CollectDroneInformationTimer extends TimerTask {
        @Override
        public void run() {
            // Log 추가

            // 화면 표시
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();

                        // 1. 조종기
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

                        // 5. 드론 Heading
                        marker_drone_location.setIcon(MapLayer.getInstance().getRotateDrawable(context, R.mipmap.map_ico_drone, _info.heading));
                        marker_drone_location.setPosition(new GeoPoint(_info.drone_latitude, _info.drone_longitude));

                        // 6. 비행경로(비행중일때만)
                        if(DroneApplication.getDroneInstance().isFlying()) {
                            GeoPoint _dron_location = new GeoPoint(_info.drone_latitude, _info.drone_longitude, _info.drone_altitude);
                            flight_paths.add(_dron_location);
                            flight_path_line.addPoint(_dron_location);
                        }



                        marker_my_location.setPosition(new GeoPoint(_info.rc_latitude, _info.rc_longitude));
                        marker_home_location.setPosition(new GeoPoint(_home.getLatitude(), _home.getLongitude()));
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
                        // Data 수집 타이머 시작
                        if(timer == null) {
                            timer = new Timer();
                        }
                        timer.schedule(new CollectDroneInformationTimer(), 0, period);
                        DroneApplication.getDroneInstance().getCameraMode();

                        if(DroneApplication.getDroneInstance().isFlying())
                        {
                            // 현재 비행중이므로 Takeoff 아이콘 => Landing 아이콘으로 변경
                            btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_landing_selector));
                            btn_flight_takeoff.setTag("landing");
                        }
                    }
                });
            }
        }else if(drone_status.status == Drone.DRONE_STATUS_ARMING){
            LogWrapper.i(TAG, "DRONE_STATUS_ARMING");

        }else if(drone_status.status == Drone.DRONE_STATUS_MISSION){
            LogWrapper.i(TAG, "DRONE_STATUS_MISSION");
            setCameraWidgetVisible(INVISIBLE);
        }else if(drone_status.status == Drone.DRONE_STATUS_RETURN_HOME){
            LogWrapper.i(TAG, "DRONE_STATUS_RETURN_HOME");
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        setReturnHomeCancelWidget(true);
                    }
                });
            }

        }else if(drone_status.status == Drone.DRONE_STATUS_CANCEL_RETURN_HOME){
            LogWrapper.i(TAG, "DRONE_STATUS_CANCEL_RETURN_HOME");
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout _layout = (LinearLayout) findViewById(R.id.layout_flight_cancel);
                        _layout.setVisibility(INVISIBLE);
                        LinearLayout _layout_rth = (LinearLayout) findViewById(R.id.layout_flight_rth);
                        _layout_rth.setVisibility(VISIBLE);
                    }
                });
            }
        }
        else if(drone_status.status == Drone.DRONE_STATUS_DISARM){
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout _layout = (LinearLayout) findViewById(R.id.layout_flight_cancel);
                        _layout.setVisibility(INVISIBLE);
                        LinearLayout _layout_rth = (LinearLayout) findViewById(R.id.layout_flight_rth);
                        _layout_rth.setVisibility(VISIBLE);
                    }
                });
            }
            LogWrapper.i(TAG, "DRONE_STATUS_LANDING");

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
                        sb_flight_gimbal_pitch.setVisibility(INVISIBLE);

                        primary_camera.setVisibility(INVISIBLE);

                        // 자동복귀 위젯 초기화
                        setReturnHomeWidget();
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
                    CameraInfo _info = DroneApplication.getDroneInstance().getStorageInfo();

                    if(_info.camera_iso != null) tv_iso.setText(_info.camera_iso);
                    if(_info.camera_shutter != null) tv_shutter.setText(_info.camera_shutter);
                    if(_info.camera_aperture != null) tv_aperture.setText(_info.camera_aperture);
                    if(_info.camera_exposure != null) tv_exposure.setText(_info.camera_exposure);
                    if(_info.camera_whitebalance != null) tv_wb.setText(_info.camera_whitebalance);
                    if(parent_ae.getVisibility() == VISIBLE && !_info.is_camera_auto_exposure_unlock_enabled) {
                        parent_ae.setVisibility(INVISIBLE);
                    }
                    if(_info.camera_ae_lock != null && _info.camera_ae_lock) btn_flight_ae.setBackground(ContextCompat.getDrawable(context, R.mipmap.ae_n));
                    else btn_flight_ae.setBackground(ContextCompat.getDrawable(context, R.mipmap.ae_s));

                    primary_camera.setVisibility(VISIBLE);

                    // 임무 중이면 카메라 위젯 보이지 않도록 처리
                    if((DroneApplication.getDroneInstance().getDroneStatus() & Drone.DRONE_STATUS_MISSION) == Drone.DRONE_STATUS_MISSION) return;

                    if(camera.mode == 0) {
                        // 사진 촬영
                        tv_flight_format_info.setText(_info.photo_file_format);
                        tv_flight_capacity.setText(_info.capture_count);
                        btn_select_shoot.setVisibility(VISIBLE);
                        btn_shoot.setVisibility(VISIBLE);
                        btn_camera_setting.setVisibility(VISIBLE);
                        sb_flight_gimbal_pitch.setVisibility(VISIBLE);

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
                        sb_flight_gimbal_pitch.setVisibility(VISIBLE);

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
                media_player.start();
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

    @Subscribe
    public void onReturnHome(final MainActivity.ReturnHome rtl) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    if(rtl.mode == MainActivity.ReturnHome.REQUEST_TAKEOFF){
                        // 이륙요청
                        DroneApplication.getDroneInstance().startTakeoff();
                    }else if(rtl.mode == MainActivity.ReturnHome.REQUEST_TAKEOFF_SUCCESS){
                        LogWrapper.i(TAG, "REQUEST_TAKEOFF_SUCCESS");
                        // 이륙성공 - 이륙버튼 아이콘 변경
                        btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_landing_selector));
                        btn_flight_takeoff.setTag("landing");
                    }else if(rtl.mode == MainActivity.ReturnHome.REQUEST_LANDING){
                        // 착륙요청
                        DroneApplication.getDroneInstance().startLanding();
                    }else if(rtl.mode == MainActivity.ReturnHome.REQUEST_LANDING_SUCCESS){
                        // 착륙요청 성공
                        setReturnHomeCancelWidget(false);
                    }else if(rtl.mode == MainActivity.ReturnHome.CANCEL_LANDING) {
                        // 착륙요청 취소
                        DroneApplication.getDroneInstance().cancelLanding();
                    }else if(rtl.mode == MainActivity.ReturnHome.REQUEST_RETURN_HOME){
                        // 자동복귀 요청
                        DroneApplication.getDroneInstance().startGoHome();
                    }else if(rtl.mode == MainActivity.ReturnHome.REQUEST_RETURN_HOME_SUCCESS){
                        setReturnHomeCancelWidget(true);
                    }else if(rtl.mode == MainActivity.ReturnHome.CANCEL_RETURN_HOME){
                        DroneApplication.getDroneInstance().cancelGoHome();
                    }else if(rtl.mode == MainActivity.ReturnHome.CANCEL_RETURN_HOME_SUCCESS
                    || rtl.mode == MainActivity.ReturnHome.CANCEL_LANDING_SUCCESS){
                        LinearLayout _layout = (LinearLayout) findViewById(R.id.layout_flight_cancel);
                        _layout.setVisibility(INVISIBLE);
                        LinearLayout _layout_rth = (LinearLayout) findViewById(R.id.layout_flight_rth);
                        _layout_rth.setVisibility(VISIBLE);
                    }else if(rtl.mode == MainActivity.ReturnHome.SET_RETURN_HOME_LOCATION){
                        //조종기 위치 불러오기
                        DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();
                        // 자동복귀 요청
                       //DroneApplication.getDroneInstance().setHomeLocation(new LocationCoordinate2D(_info.rc_latitude, _info.rc_longitude));
                        DroneApplication.getDroneInstance().setHomeLocation(new LocationCoordinate2D(36.358713, 127.384911));
                    }else if(rtl.mode == MainActivity.ReturnHome.REQUEST_RETURN_HOME_SUCCESS){
                        // 자동복귀 요청
                        DroneApplication.getDroneInstance().startGoHome();

                        // Home 위치 변경??
                    }

                }
            });
        }
    }

    /**
     * 카메라와 지도 View 전환 애니메이션
     */
    private class ResizeAnimation extends Animation {

        private View view;
        private int to_height;
        private int from_height;

        private int to_width;
        private int from_width;
        private int margin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int _margin) {
            to_height = toHeight;
            to_width = toWidth;
            from_height = fromHeight;
            from_width = fromWidth;
            view = v;
            margin = _margin;
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

    /**
     * 카메라 위젯 표시여부를 설정한다.
     * @param visible
     */
    private void setCameraSettingWidgetVisible(int visible){
        layout_camera.setVisibility(visible);
        parent_ae.setVisibility(visible);
        sb_flight_gimbal_pitch.setVisibility(visible);
    }

    private void setCameraWidgetVisible(int visible){
        btn_select_movie.setVisibility(visible);
        btn_record.setVisibility(visible);
        tv_record_time.setVisibility(visible);
        btn_shoot.setVisibility(visible);
        btn_select_shoot.setVisibility(visible);
        btn_camera_setting.setVisibility(visible);
        sb_flight_gimbal_pitch.setVisibility(visible);
    }

    /**
     * 자동귀환 또는 자동차륙 위젯 표시여부를 설정한다.
     * @param is_rth : 자동귀환 여부, true : 자동귀환, false : 자동착륙
     */
    private void setReturnHomeCancelWidget(boolean is_rth){
        if(is_rth) {
            LinearLayout _layout = (LinearLayout) findViewById(R.id.layout_flight_cancel);
            _layout.setVisibility(VISIBLE);
            LinearLayout _layout_rth = (LinearLayout) findViewById(R.id.layout_flight_rth);
            _layout_rth.setVisibility(INVISIBLE);

            btn_flight_cancel.setTag("rtl");
        }else{
            LinearLayout _layout = (LinearLayout) findViewById(R.id.layout_flight_cancel);
            _layout.setVisibility(VISIBLE);
            LinearLayout _layout_rth = (LinearLayout) findViewById(R.id.layout_flight_rth);
            _layout_rth.setVisibility(INVISIBLE);

            btn_flight_cancel.setTag("landing");
            btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_takeoff_selector));
            btn_flight_takeoff.setTag("takeoff");
        }
    }

    private void setReturnHomeWidget(){
        LinearLayout _layout = (LinearLayout) findViewById(R.id.layout_flight_cancel);
        _layout.setVisibility(INVISIBLE);
        LinearLayout _layout_rth = (LinearLayout) findViewById(R.id.layout_flight_rth);
        _layout_rth.setVisibility(VISIBLE);
    }
}
