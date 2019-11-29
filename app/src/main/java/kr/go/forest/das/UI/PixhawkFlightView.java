package kr.go.forest.das.UI;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageView;
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
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.DroneInfoRequest;
import kr.go.forest.das.Model.DroneInfoResponse;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;
import kr.go.forest.das.network.NetworkStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

public class PixhawkFlightView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver {

    private final String TAG = "FlightView";
    private final int period = 250;                             // 드론정보 수십 주기 0.25 second
    private final int DRONE_INFO_SEND_SIZE = 20;                // 드론정보 전송 기준
    boolean send_complete = true;                               // 실시간 전송 성공 여부
    private Context context;

    Timer timer = null;                                         // 드론정보 수집 타이머
    ArrayList<DroneInfo> drone_flight_log_4_realtime = new ArrayList<>();   // 실시간 전송용 비행 및 기체정보
    ArrayList<DroneInfo> drone_flight_log_4_bigdata = new ArrayList<>();    // 빅데이터 업로드용 비행 및 기체정보
    int drone_info_seq = 1;                                                 // 실시간 전송 data seq
    List<DroneInfo> drone_flight_log = new ArrayList<>();       // 비행 및 기체정보
    private Handler handler_ui;                                 // UI 업데이트 핸들러
    private SharedPreferences pref;

    // Map
    private MapView map_view = null;                            // 지도 뷰
    Marker marker_drone_location = null;                        // 드론 위치 정보 마커
    Marker marker_home_location = null;                         // 드론 이륙지점 위치 마커
    Marker marker_my_location = null;                           // 조종자 위치

    HashMap<String, Polygon> no_fly_zone = null;                // 비행금지구역 정보
    List<Marker> forest_fires = new ArrayList<Marker>();             // 산불발생지역 정보
    List<GeoPoint> flight_paths = new ArrayList<GeoPoint>();           // 비행경로
    Polyline flight_path_line = new Polyline();                 // 비행경로 표시
    Polyline mission_flight_path_line = new Polyline();         // 임무비행 경로 표시
    GeoPoint my_location = null;                                // 현재 조종자 위치

    // RTH
    Button pixhawk_btn_flight_takeoff;                          // 이륙 버튼
    Button pixhawk_btn_flight_return_home;                      // 자동복귀 버튼
    Button pixhawk_btn_flight_cancel;                           // 자동복귀 취소 버튼

    // TextView
    TextView tv_distance;                                       // 드론과 거리
    TextView tv_altitude;                                       // 드론 고도
    TextView tv_horizontal_speed;                               // 드론 수평속도
    TextView tv_vertical_speed;                                 // 드론 수직속도
    TextView textview_battery_remain_percent;                   // 배터리 남은 용량
    TextView textview_flight_mode;                              // 드론 비행모드

    TextView pixhawk_connect_text;                              // 드론 상태 정보
    TextView textview_gps_count;                                // 드론 GPS 연결정보
    TextView textview_gps_eph;                                  // 드론 GPS 수평 연결

    // ImageView
    ImageView pixhawk_rc_signal;                                // 조종기 연결 정보

    public PixhawkFlightView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public PixhawkFlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    /**
     * 뷰가 보여지고 나서 처리
     */
    @Override
    protected void onAttachedToWindow() {
        ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler_ui = new Handler(Looper.getMainLooper());

        // 드론 상태 체크
        if(DroneApplication.getDroneInstance() != null) {
            // Data 수집 타이머 시작
            timer.schedule(new PixhawkFlightView.CollectDroneInformationTimer(), 0, period);
        }

        super.onAttachedToWindow();
    }

    /**
     * 뷰가 종료될 때 처리
     */
    @Override
    protected void onDetachedFromWindow() {
        ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;
        timer = null;

        if(no_fly_zone != null) {
            no_fly_zone.clear();
            no_fly_zone = null;
        }

        // 산불발생지역 초기화
        forest_fires.clear();
        forest_fires = null;

        // 배경지도 초기화
        map_view.getOverlays().clear();
        map_view = null;

        // 임무비행경로 초기화
        if(DroneApplication.getDroneInstance() != null) {
            DroneApplication.getDroneInstance().setMissionPoints(null);
        }

        super.onDetachedFromWindow();
    }

    /**
     * 지도 및 기타 컨트롤 설정
     */
    protected void initUI() {
        pref = context.getSharedPreferences("drone", Context.MODE_PRIVATE);
        DroneApplication.getEventBus().register(this);

        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        setSystemUiVisibility(UI_OPTIONS);

        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.pixhawk_content_flight, this, true);

        setWidget();

        timer = new Timer();

        // MapView 설정
        map_view = findViewById(R.id.pixhawk_flight_map_view);
        map_view.setBuiltInZoomControls(false);
        map_view.setMultiTouchControls(true);
        map_view.setMinZoomLevel(8.0);
        map_view.setMaxZoomLevel(20.0);
        map_view.setTileSource(VWorldStreet);
        MapEventsOverlay _events = new MapEventsOverlay(this);
        map_view.getOverlays().add(_events);

        IMapController mapController = map_view.getController();
        // 마지막에 저장된 위치 불러오기
        mapController.setZoom(16.0);
        // 현재 GPS 좌표 불러오기
        String _lat = pref.getString("lat", null);
        String _lon = pref.getString("lon", null);

        if(_lat == null && _lon == null) {
            mapController.setCenter(new GeoPoint(36.361481, 127.384841));
        }else{
            mapController.setCenter(new GeoPoint(Double.parseDouble(_lat), Double.parseDouble(_lon)));
        }

        //임무 비행경로
        mission_flight_path_line.setColor(Color.WHITE);
        mission_flight_path_line.setWidth(6.0f);
        map_view.getOverlayManager().add(mission_flight_path_line);

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

        setClickable(true);
    }

    /**
     * UI 위셋 설정
     */
    private void setWidget() {

        // 드론 정보
        tv_distance =  findViewById(R.id.pixhawk_tv_flight_distance_from_home);
        tv_altitude =  findViewById(R.id.pixhawk_tv_flight_altitude);
        tv_horizontal_speed =  findViewById(R.id.pixhawk_tv_flight_horizontal_speed);
        tv_vertical_speed =  findViewById(R.id.pixhawk_tv_flight_vertical_speed);
        pixhawk_connect_text = findViewById(R.id.pixhawk_connect_text);
        textview_gps_count = findViewById(R.id.textview_gps_count);
        textview_gps_eph = findViewById(R.id.textview_gps_eph);
        pixhawk_rc_signal = findViewById(R.id.pixhawk_rc_signal);
        textview_battery_remain_percent = findViewById(R.id.textview_battery_remain_percent);
        textview_flight_mode = findViewById(R.id.textview_flight_mode);

        findViewById(R.id.pixhawk_btn_flight_location).setOnClickListener(this);
        findViewById(R.id.pixhawk_btn_flight_nofly).setOnClickListener(this);
        findViewById(R.id.pixhawk_btn_flight_fires).setOnClickListener(this);
        findViewById(R.id.pixhawk_btn_flight_save_path).setOnClickListener(this);

        // 뒤로가기 버튼
        findViewById(R.id.pixhawk_btn_mission_back).setOnClickListener(this);
        // 업로드버튼 비활성화
        findViewById(R.id.pixhawk_btn_mission_upload).setVisibility(INVISIBLE);

        // RTH
        pixhawk_btn_flight_takeoff = findViewById(R.id.pixhawk_btn_flight_takeoff);
        pixhawk_btn_flight_takeoff.setTag("takeoff");
        pixhawk_btn_flight_takeoff.setOnClickListener(this);
        pixhawk_btn_flight_return_home = findViewById(R.id.pixhawk_btn_flight_return_home);
        pixhawk_btn_flight_return_home.setOnClickListener(this);
        pixhawk_btn_flight_cancel = findViewById(R.id.pixhawk_btn_flight_cancel);
        pixhawk_btn_flight_cancel.setOnClickListener(this);

        // 드론 정보 설정
        if(DroneApplication.getDroneInstance() != null && DroneApplication.getDroneInstance().isConnect() == true){
            pixhawk_connect_text.setBackgroundResource(R.mipmap.top_bg_green);
            pixhawk_connect_text.setText("비행 준비 완료");
        }
    }

    /**
     * 클릭 이벤트 처리
     */
    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.pixhawk_btn_mission_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.pixhawk_btn_flight_location:
                // 현재 위치로 가운데.
                if (my_location != null)
                {
                    marker_my_location.setPosition(my_location);
                    map_view.getController().setCenter(new GeoPoint(my_location.getLatitude(), my_location.getLongitude()));
                    map_view.invalidate();

                    SharedPreferences.Editor _editor = pref.edit();
                    _editor.putString("lat", String.valueOf(my_location.getLatitude()));
                    _editor.putString("lon", String.valueOf(my_location.getLongitude()));
                    _editor.commit();
                }else{
                    // 위치 확인 팝업
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.search_location));
                }
                break;
            case R.id.pixhawk_btn_flight_nofly:
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
            case R.id.pixhawk_btn_flight_fires:
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
            case R.id.pixhawk_btn_flight_save_path:
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
            case R.id.pixhawk_btn_flight_takeoff:
                // 드론 이륙 - 모델별로 메세지 변경해서
                if(pixhawk_btn_flight_takeoff.getTag().equals("takeoff")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.takeoff_title, R.string.phantom_takeoff_content));
                }else if(pixhawk_btn_flight_takeoff.getTag().equals("landing")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.landing_title, R.string.landing_content));
                }
                break;
            case R.id.pixhawk_btn_flight_return_home:
                // 드론 이륙위치로 복귀
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.return_home_title, R.string.return_home_content));
                break;
            case R.id.pixhawk_btn_flight_cancel:
                if(pixhawk_btn_flight_cancel.getTag().equals("landing")){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.landing_cancel_title, 0));
                }else if(pixhawk_btn_flight_cancel.getTag().equals("rtl")){
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
            // 화면 표시
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();

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

                        // 6. 조종기와의 거리
                        if(my_location == null || Math.abs(my_location.getLatitude()) < 1) tv_distance.setText("0.00");
                        else tv_distance.setText(String.format("%.1f", GeoManager.getInstance().distance(my_location.getLatitude(), my_location.getLongitude(), _info.drone_latitude, _info.drone_longitude)));

                        if(my_location != null) marker_my_location.setPosition(my_location);

                        LocationCoordinate2D _home = DroneApplication.getDroneInstance().getHomeLocation();
                        if(_home != null) {
                            marker_home_location.setPosition(new GeoPoint(_home.getLatitude(), _home.getLongitude()));
                        }

                        // 7. 드론 GPS 정보
                        textview_gps_count.setText(String.format("%d",_info.satellites_visible_count));
                        textview_gps_eph.setText(String.format("%.2f", _info.eph));

                        // 8. 조종기 연결 정보
                        if(_info.rssi < 1) pixhawk_rc_signal.setBackgroundResource(R.mipmap.signal_0);
                        else if(_info.rssi < 21) pixhawk_rc_signal.setBackgroundResource(R.mipmap.signal_1);
                        else if(_info.rssi < 41) pixhawk_rc_signal.setBackgroundResource(R.mipmap.signal_2);
                        else if(_info.rssi < 61) pixhawk_rc_signal.setBackgroundResource(R.mipmap.signal_3);
                        else if(_info.rssi < 81) pixhawk_rc_signal.setBackgroundResource(R.mipmap.signal_4);
                        else pixhawk_rc_signal.setBackgroundResource(R.mipmap.signal_5);

                        // 9. 배터리 정보
                        if(_info.battery_remain_percent == -1){
                            textview_battery_remain_percent.setTextColor(Color.RED);
                            textview_battery_remain_percent.setText("N/A");
                        }
                        else{
                            textview_battery_remain_percent.setTextColor(Color.GREEN);
                            textview_battery_remain_percent.setText(String.format("%d",_info.battery_remain_percent));
                        }

                        // 10. 드론 비행모드
                        textview_flight_mode.setText(_info.flight_mode);
                        map_view.invalidate();

                        if(DroneApplication.getDroneInstance().isFlying()){
                            _info.seq = drone_info_seq++;
                            drone_flight_log_4_realtime.add(_info);
                            drone_flight_log_4_bigdata.add(_info);

                            // 재난상황일 때는 전송
                            if(DroneApplication.getSystemInfo().is_realtime && NetworkStatus.isInternetConnected(context) == true){
                                sendFlightLog(false);
                            }

                            GeoPoint _dron_location = new GeoPoint(_info.drone_latitude, _info.drone_longitude, _info.drone_altitude);

                            if(Math.abs(_info.drone_latitude) > 0 && Math.abs(_info.drone_longitude) > 0){
                                flight_paths.add(_dron_location);
                                flight_path_line.addPoint(_dron_location);
                            }

                            // 비행중일 때만 배터리 부족 알림
//                            if (_info.battery_remain_percent < 31 && battery_status == 0) {
//                                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.battery_warning));
//                                DroneApplication.getEventBus().post(new MainActivity.TTS(context.getString(R.string.battery_warning)));
//                                battery_status = 1;
//                            } else if (_info.battery_remain_percent < 21 && battery_status == 1) {
//                                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.battery_emergency));
//                                DroneApplication.getEventBus().post(new MainActivity.TTS(context.getString(R.string.battery_emergency)));
//                                battery_status = 2;
//                            }
                        }
                    }
                });
            }
        }
    }

    @Subscribe
    public void onConnectionChange(final MainActivity.DroneStatusChange drone_status) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    if(drone_status.status == Drone.DRONE_STATUS_CONNECT) {
                        pixhawk_connect_text.setBackgroundResource(R.mipmap.top_bg_green);
                        pixhawk_connect_text.setText("비행 준비 완료");
                    }else if(drone_status.status == Drone.DRONE_STATUS_ARMING){

                    }else if(drone_status.status == Drone.DRONE_STATUS_FLYING){

                    }else if(drone_status.status == Drone.DRONE_STATUS_MISSION){

                    }else if(drone_status.status == Drone.DRONE_STATUS_RETURN_HOME){

                    }else if(drone_status.status == Drone.DRONE_STATUS_DISARM){

                    }else if(drone_status.status == Drone.DRONE_STATUS_DISCONNECT){
                        pixhawk_connect_text.setBackgroundResource(R.mipmap.top_bg_gray);
                        pixhawk_connect_text.setText("기기 연결 끊김");
                    }
                }
            });
        }
    }

    /**
     * 모바일 장치의 위치 정보를 업데이트 한다.
     * @param location 현재 위치
     */
    @Subscribe
    public void onUpdateLocation(final MainActivity.LocationUpdate location) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    if(my_location == null){
                        my_location = new GeoPoint(location.latitude, location.longitude);
                    }else {
                        my_location.setLatitude(location.latitude);
                        my_location.setLongitude(location.longitude);
                    }
                }
            });
        }
    }

    /**
     * 자동복귀 중 이벤트 리시버
     * @param rtl 자동복귀 정보
     */
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
                        // 이륙성공 - 이륙버튼 아이콘 변경
                        pixhawk_btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_landing_selector));
                        pixhawk_btn_flight_takeoff.setTag("landing");
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
                        LinearLayout _layout = findViewById(R.id.pixhawk_layout_flight_cancel);
                        _layout.setVisibility(INVISIBLE);
                        LinearLayout _layout_rth = findViewById(R.id.pixhawk_layout_flight_rth);
                        _layout_rth.setVisibility(VISIBLE);
                    }else if(rtl.mode == MainActivity.ReturnHome.SET_RETURN_HOME_LOCATION){
                        //조종기 위치 불러오기
                        DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();
                        // 자동복귀 요청
                        DroneApplication.getDroneInstance().setHomeLocation(new LocationCoordinate2D(_info.rc_latitude, _info.rc_longitude));
                        //DroneApplication.getDroneInstance().setHomeLocation(new LocationCoordinate2D(36.358713, 127.384911));
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
     * 자동귀환 또는 자동착륙 컨트롤 버튼 표시여부를 설정한다.
     * @param is_rth : 자동귀환 여부, true : 자동귀환, false : 자동착륙
     */
    private void setReturnHomeCancelWidget(boolean is_rth){
        if(is_rth) {
            LinearLayout _layout = findViewById(R.id.pixhawk_layout_flight_cancel);
            _layout.setVisibility(VISIBLE);
            LinearLayout _layout_rth = findViewById(R.id.pixhawk_layout_flight_rth);
            _layout_rth.setVisibility(INVISIBLE);

            pixhawk_btn_flight_cancel.setTag("rtl");
        }else{
            LinearLayout _layout = findViewById(R.id.pixhawk_layout_flight_cancel);
            _layout.setVisibility(VISIBLE);
            LinearLayout _layout_rth = findViewById(R.id.pixhawk_layout_flight_rth);
            _layout_rth.setVisibility(INVISIBLE);

            pixhawk_btn_flight_cancel.setTag("landing");
            pixhawk_btn_flight_takeoff.setBackground(ContextCompat.getDrawable(context, R.drawable.btn_takeoff_selector));
            pixhawk_btn_flight_takeoff.setTag("takeoff");
        }
    }

    /**
     * 자동귀환 또는 자동착률 컨트롤 버튼 표시여부를 설정한다.
     */
    private void setReturnHomeWidget(){
        LinearLayout _layout = findViewById(R.id.layout_flight_cancel);
        _layout.setVisibility(INVISIBLE);
        LinearLayout _layout_rth = findViewById(R.id.layout_flight_rth);
        _layout_rth.setVisibility(VISIBLE);
    }

    /**
     *  선택한 좌표의 마커를 생성한다.
     * @param p 터치한 위치 좌표
     * @return  마커
     */
    private Marker getDefaultMarker(GeoPoint p, String text) {
        Marker _marker = new Marker(map_view);
        _marker.setPosition(new GeoPoint(p.getLatitude(), p.getLongitude()));
        _marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        _marker.setIcon(MapLayer.getInstance().writeOnDrawable(context, text, R.drawable.waypoint_s));
        _marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                return true;
            }
        });

        return  _marker;
    }

    /**
     * 비행경로 시작점과 종료점을 나타내는 마커를 추가한다.
     */
    private void setEntryExit(List<GeoPoint> points) {
        Marker _entry = getDefaultMarker(points.get(0), "S");
        map_view.getOverlays().add(_entry);

        Marker _exit = getDefaultMarker(points.get(points.size() - 1), "E");
        map_view.getOverlays().add(_exit);
    }

    private void sendFlightLog(boolean force_send){
        if( force_send == true || (send_complete == true                        // 전송 성공 여부 확인
                && DroneApplication.getSystemInfo().isLogin()                       // 로그인 여부 확인
                && drone_flight_log_4_realtime.size() > DRONE_INFO_SEND_SIZE))      // 전송 크기 확인
        {
            try {
                DroneInfoRequest _request = new DroneInfoRequest(DroneApplication.getSystemInfo(),  drone_flight_log_4_realtime.subList(0,DRONE_INFO_SEND_SIZE));
                send_complete = false;
                DroneApplication.getApiInstance().postDroneInfo(_request).enqueue(new Callback<DroneInfoResponse>() {
                    @Override
                    public void onResponse(Call<DroneInfoResponse> call, Response<DroneInfoResponse> response) {
                        DroneInfoResponse _test = response.body();

                        if(force_send == false){
                            for (int i = 0; i < DRONE_INFO_SEND_SIZE; i++) {
                                drone_flight_log_4_realtime.remove(0);
                            }
                        }else{
                            drone_flight_log_4_realtime.clear();
                        }

                        send_complete = true;
                        Log.e(TAG, "send onResponse");
                    }

                    @Override
                    public void onFailure(Call<DroneInfoResponse> call, Throwable t) {
                        Log.e(TAG, "send onFailure : " + t.toString());
                        send_complete = true;
                    }
                });
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        return;
    }
}
