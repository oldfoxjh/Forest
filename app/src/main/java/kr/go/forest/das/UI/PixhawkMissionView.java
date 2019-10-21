package kr.go.forest.das.UI;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.modules.ArchiveFileFactory;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dji.common.mission.waypoint.WaypointMission;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.mission.MissionControl;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DJITimelineMission;
import kr.go.forest.das.Model.DJIWaypointMission;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.MAVLinkWaypointMission;
import kr.go.forest.das.Model.RectD;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

public class PixhawkMissionView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver, Marker.OnMarkerClickListener, SeekBar.OnSeekBarChangeListener {

    private final int period = 250;                             // 드론정보 수십 주기 0.25 second
    
    private Context context;
    private MapView map_view = null;
    private Handler handler_ui;
    private SharedPreferences pref;
    private ProgressDialog progress;

    private int mission_angle = 0;                              // 임무비행 영역회전
    private float mission_altitude = 0.0f;                      // 임무비행 고도
    private float mission_flight_speed = 0.0f;                  // 임무비행 속도
    private float mission_side_lap = 0.0f;                      // 임무비행 횡중복도
    private float mission_front_lap = 0.0f;                     // 임무비행 종중복도
    int shoot_time_interval = 0;                                // 임무비행 촬영간격
    int shoot_count = 0;                                        // 임무비행 촬영횟수
    float side_distance = 0;                                    // 임무비행 횡간격
    float front_distance = 0;                                   // 임무비행 종간격

    Timer timer = null;                                         // 드론정보 수집 타이머

    Marker marker_my_location = null;                           // 조종기 위치 정보 마커
    Marker marker_drone_location = null;                        // 드론 위치 정보 마커
    Marker marker_home_location = null;                         // 드론 이륙지점 위치 마커
    GeoPoint my_location = null;                                // 현재 조종자 위치
    int mission_status = 0;                                     // 임무 상태
    MAVLinkWaypointMission waypoint_mission = null;

    List<Marker> selected_points = new ArrayList<Marker>();          // 사용자가 선택한 위치를 나타내는 마커
    List<GeoPoint> mWaypoints = new ArrayList<GeoPoint>();             // 사용자가 선택한 위치
    List<GeoPoint> area_points = new ArrayList<GeoPoint>();            // 촬영영역을 보여주기 위한 위치
    List<GeoPoint> flight_points = new ArrayList<GeoPoint>();          // 촬영영역을 보여주기 위한 위치
    List<Marker> entry_exit = new ArrayList<Marker>();               // 임무 시작점과 종료점 마커

    Polygon flight_area = new Polygon();                        // 촬영영역
    Polyline flight_path = new Polyline();                      // 비행경로

    // 임무설정 Widget Control
    TextView tv_mission_area;
    TextView tv_mission_distance;
    TextView tv_mission_flight_speed;
    TextView tv_mission_angle;
    TextView tv_mission_flight_altitude;
    TextView tv_mission_overlap;
    TextView tv_mission_sidelap;
    TextView tv_mission_lap_distance;
    TextView tv_mission_shoot_interval;

    EditText pixhawk_textview_sensor_width;
    EditText pixhawk_textview_focal_length;
    EditText pixhawk_textview_image_width;
    EditText pixhawk_textview_image_height;

    SeekBar sb_mission_flight_speed;
    SeekBar sb_mission_angle;
    SeekBar sb_mission_flight_altitude;
    SeekBar sb_mission_overlap;
    SeekBar sb_mission_sidelap;

    public PixhawkMissionView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public PixhawkMissionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {
        ((Activity)context).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        DroneApplication.getEventBus().register(this);
        handler_ui = new Handler(Looper.getMainLooper());
        progress = new ProgressDialog(context);

        // Data 수집 타이머 시작
        if(timer == null) {
            timer = new Timer();
        }
        timer.schedule(new PixhawkMissionView.CollectDroneInformationTimer(), 0, period);


        // 비행경로 초기화
        if(DroneApplication.getDroneInstance() != null) {
            DroneApplication.getDroneInstance().setMissionPoints(null);
        }

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        ((Activity)context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;

        selected_points.clear();
        selected_points = null;

        mWaypoints.clear();
        mWaypoints = null;

        area_points.clear();
        area_points = null;

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

        super.onDetachedFromWindow();
    }

    protected void initUI(){

        pref = context.getSharedPreferences("drone", Context.MODE_PRIVATE);

        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        setSystemUiVisibility(UI_OPTIONS);

        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.content_pixhawk_mission, this, true);

        // Widget Control 설정
        setWidget();

        // MapView 설정
        map_view = (MapView) findViewById(R.id.mapView);
        map_view.setBuiltInZoomControls(false);
        map_view.setMultiTouchControls(true);
        map_view.setMinZoomLevel(8.0);
        map_view.setMaxZoomLevel(20.0);
        map_view.setTileSource(VWorldStreet);
        //setOfflineTile();

        IMapController mapController = map_view.getController();
        mapController.setZoom(16.0);

        // Touch Overlay
        MapEventsOverlay _events = new MapEventsOverlay(this);
        map_view.getOverlays().add(_events);

        // 현재 GPS 좌표 불러오기
        String _lat = pref.getString("lat", null);
        String _lon = pref.getString("lon", null);

        if(_lat == null && _lon == null) {
            mapController.setCenter(new GeoPoint(36.361481, 127.384841));
        }else{
            mapController.setCenter(new GeoPoint(Double.parseDouble(_lat), Double.parseDouble(_lon)));
        }

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

        // 폴리곤 설정
        flight_area.setFillColor(Color.argb(60, 0, 255, 0));
        flight_area.setStrokeWidth(1.0f);

        flight_path.setColor(Color.RED);
        flight_path.setWidth(2.0f);

        map_view.getOverlayManager().add(flight_area);
        map_view.getOverlayManager().add(flight_path);

//        HashMap<String, Polygon> _noFlyZone = MapLayer.getInstance().getNoFlyZoneFromValue("");
//
//        for(String key: _noFlyZone.keySet())
//        {
//            Polygon _zone = _noFlyZone.get(key);
//            _zone.setOnClickListener(new Polygon.OnClickListener(){
//                @Override
//                public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
//                    return false;
//                }
//            });
//            map_view.getOverlayManager().add(_zone);
//        }


        map_view.setOnClickListener(this);
        map_view.invalidate();
        setClickable(true);
    }

    /**
     * 특정폴더의 오프라인 맵을 배경지도로 사용한다.
     */
    private void setOfflineTile(){
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/");
        if (f.exists()) {
            File[] list = f.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    if (list[i].isDirectory()) {
                        continue;
                    }

                    String name = list[i].getName().toLowerCase();
                    if (!name.contains(".")) {
                        continue; //skip files without an extension
                    }
                    name = name.substring(name.lastIndexOf(".") + 1);
                    if (name.length() == 0) {
                        continue;
                    }

                    if (ArchiveFileFactory.isFileExtensionRegistered(name)) {
                        try {
                            OfflineTileProvider tileProvider = new OfflineTileProvider(new SimpleRegisterReceiver(context), new File[]{list[i]});
                            map_view.setTileProvider(tileProvider);
                            String source = "";
                            IArchiveFile[] archives = tileProvider.getArchives();
                            if (archives.length > 0) {
                                Set<String> tileSources = archives[0].getTileSources();
                                if (!tileSources.isEmpty()) {
                                    source = tileSources.iterator().next();
                                    map_view.setTileSource(new XYTileSource(source, 4, 18, 256,
                                            ".jpg", new String[]{""}));
                                } else {
                                    map_view.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void setWidget() {

        // Button
        (findViewById(R.id.pixhawk_btn_mission_back)).setOnClickListener(this);
        (findViewById(R.id.pixhawk_btn_mission_location)).setOnClickListener(this);
        (findViewById(R.id.pixhawk_btn_load_shape)).setOnClickListener(this);
        (findViewById(R.id.pixhawk_btn_new_course)).setOnClickListener(this);
        (findViewById(R.id.pixhawk_btn_mission_upload)).setOnClickListener(this);

        // 임무설정 텍스트
        tv_mission_area = findViewById(R.id.pixhawk_mission_area);
        tv_mission_distance = findViewById(R.id.pixhawk_mission_distance);
        tv_mission_flight_speed = findViewById(R.id.pixhawk_textview_mission_flight_speed);
        tv_mission_angle = findViewById(R.id.pixhawk_textview_mission_angle);
        tv_mission_flight_altitude = findViewById(R.id.pixhawk_textview_mission_flight_altitude);
        tv_mission_overlap = findViewById(R.id.pixhawk_textview_mission_overlap);
        tv_mission_sidelap = findViewById(R.id.pixhawk_textview_mission_sidelap);
        tv_mission_lap_distance = findViewById(R.id.pixhawk_textview_mission_lap_distance);
        tv_mission_shoot_interval = findViewById(R.id.pixhawk_textview_mission_shoot_interval);

        // 카메라 설정
        pixhawk_textview_sensor_width = findViewById(R.id.pixhawk_textview_sensor_width);
        pixhawk_textview_sensor_width.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3,2)});
        String _sensor_width = pref.getString("sensor_width", "");
        pixhawk_textview_sensor_width.setText((_sensor_width.length() > 0) ? _sensor_width : "13.2");

        pixhawk_textview_focal_length = findViewById(R.id.pixhawk_textview_focal_length);
        pixhawk_textview_focal_length.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3,2)});
        String _focal_length = pref.getString("focal_length", "");
        pixhawk_textview_focal_length.setText((_focal_length.length() > 0) ? _focal_length : "8.8");

        pixhawk_textview_image_width = findViewById(R.id.pixhawk_textview_image_width);
        String _image_width = pref.getString("image_width", "");
        pixhawk_textview_image_width.setText((_image_width.length() > 0) ? _image_width : "5472");

        pixhawk_textview_image_height = findViewById(R.id.pixhawk_textview_image_height);
        String _image_height = pref.getString("image_height", "");
        pixhawk_textview_image_height.setText((_image_height.length() > 0) ? _image_height : "3078");

        // 임무설정 Seekbar
        sb_mission_flight_speed = findViewById(R.id.pixhawk_seekbar_mission_flight_speed);
        sb_mission_angle = findViewById(R.id.pixhawk_seekbar_mission_angle);
        sb_mission_flight_altitude = findViewById(R.id.pixhawk_seekbar_mission_flight_altitude);
        sb_mission_overlap = findViewById(R.id.pixhawk_seekbar_mission_overlap);
        sb_mission_sidelap = findViewById(R.id.pixhawk_seekbar_mission_sidelap);

        sb_mission_flight_speed.setOnSeekBarChangeListener(this);
        sb_mission_angle.setOnSeekBarChangeListener(this);
        sb_mission_flight_altitude.setOnSeekBarChangeListener(this);
        sb_mission_overlap.setOnSeekBarChangeListener(this);
        sb_mission_sidelap.setOnSeekBarChangeListener(this);

        sb_mission_flight_speed.setProgress(49);
        sb_mission_flight_speed.incrementProgressBy(1);
        sb_mission_angle.setProgress(0);
        sb_mission_flight_altitude.setProgress(150);
        sb_mission_overlap.setProgress(60);
        sb_mission_sidelap.setProgress(60);
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

    private  String path = "";
    /**
     * 임무비행화면의 이벤트를 처리한다.
     * @param mission 임무화면 이벤트
     */
    @Subscribe
    public void onMissionLoad(final MainActivity.Mission mission) {
        LogWrapper.i("MissionView", "cmd : " + mission.command);
        if(mission.command == MainActivity.Mission.MISSION_FROM_FILE || mission.command == MainActivity.Mission.MISSION_FROM_ONLINE){
            // 임무정보 초기화
            clearMission();

            // 불러온 정보로 Polygon 만들기
            path = mission.data;

            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        // 마커 포함 임무 그리기
                        int _ret = GeoManager.getInstance().getPositionsFromShapeFile(path, area_points);

                        // 포인트가 2개 이하면 오류
                        setMissionPolygon(area_points);
                    }
                });
            }
            // 파일이 잘못되었을 경우 팝업
        }else if(mission.command == MainActivity.Mission.MISSION_UPLOAD){
            DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.mission_start_title, R.string.mission_start_content, ""));
        }else if(mission.command == MainActivity.Mission.MISSION_UPLOAD_FAIL){
            // ProgressDialog 닫기
            progress.dismiss();
            // 미션 업로드 실패
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_upload_fail, mission.data));
        }else if(mission.command == MainActivity.Mission.MISSION_UPLOAD_SUCCESS){
            // 임무 시작 조건 설정
            DroneApplication.getDroneInstance().setMissionCondition(shoot_count, shoot_time_interval);
        }else if(mission.command == MainActivity.Mission.MISSION_START_FAIL){
            // 미션 시작 실패
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_start_fail, mission.data));
        }
        else if(mission.command == MainActivity.Mission.MISSION_START_SUCCESS){
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        // 비행경로 저장
                        DroneApplication.getDroneInstance().setMissionPoints(flight_points);
                        // 비행화면으로 전환
                        DroneApplication.getEventBus().post(new ViewWrapper(new FlightView(context), false));
                    }
                });
            }
        }else if(mission.command == MainActivity.Mission.MISSION_START){
            // 웨이포인트 미션일 경우
            //DroneApplication.getDroneInstance().startMission(shoot_count, shoot_time_interval);
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if (MissionControl.getInstance().scheduledCount() > 0) {
                            MissionControl.getInstance().startTimeline();
                        }
                        // 비행경로 저장
                        DroneApplication.getDroneInstance().setMissionPoints(flight_points);
                        // 비행화면으로 전환
                        DroneApplication.getEventBus().post(new ViewWrapper(new FlightView(context), false));
                    }
                });
            }
        }else if(mission.command == MainActivity.Mission.MAX_FLIGHT_HEIGHT_SET_SUCCESS){
            // 드론 최대비행고도 변경 성공
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.max_flight_height_success, null));
        }else if(mission.command == MainActivity.Mission.MISSION_CLEAR){
            clearMission();
        }
    }

    /**
     * 임무화면의 클릭이벤트를 처리한다.
     * @param v 클릭이벤트가 발생한 View
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pixhawk_btn_mission_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.pixhawk_btn_mission_location:
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
                setMissionPolygon();
                break;
            case R.id.pixhawk_btn_new_course:
                // 현재 임무를 초기화 하겠냐는 팝업
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, 0, R.string.clear_mission));
                break;
            case R.id.pixhawk_btn_load_shape:
                // Mission 파일 목록 팝업
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_LOAD_SHAPE, 0,0));
                break;
            case R.id.pixhawk_btn_mission_upload:
                if(DroneApplication.getDroneInstance() == null){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.aircraft_disconnect));
                    return;
                }

                // 웨이 포인트 3개 이상...확인
                if(mWaypoints.size() < 2) {
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_create_fail, null));
                    return;
                }

                // 웨이포인트 생성
                DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();
                waypoint_mission = new MAVLinkWaypointMission(devideFlightPath(), new GeoPoint(_info.drone_latitude, _info.drone_longitude), mission_flight_speed);

                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_UPLOAD_MISSION, 0, 0));

                break;
        }
    }

    /**
     * 비행경로를 촬영거리만큼 나눈다.
     * @return 촬영거리만큼 분할된 경로
     */
    private List<GeoPoint> devideFlightPath() {
        // 경로 나누기
        List<GeoPoint> upload_mission = new ArrayList<>();
        for(int i = 0; i < flight_points.size(); i++){
            // 시작점
            GeoPoint _start = flight_points.get(i);
            // 끝점
            GeoPoint _end = flight_points.get(++i);

            // 진행방향(남북)
            int direction_ns = (_start.getLatitude() > _end.getLatitude()) ? -1 : 1;
            int direction_ew = (_start.getLongitude() > _end.getLongitude()) ? -1 : 1;

            // 시작점과 끝점 사이 거리
            upload_mission.add(_start);
            for(int j = 1; ; j++)
            {
                // 시작점부터 거리 구하기
                double _sin = Math.abs(Math.sin(Math.toRadians(mission_angle)));
                double _cos = Math.abs(Math.cos(Math.toRadians(mission_angle)));
                double _east_west = front_distance*j*_sin*direction_ew;
                double _north_south = front_distance*j*_cos*direction_ns;

                GeoPoint _next = GeoManager.getInstance().getPositionFromDistance(_start, _east_west, _north_south);

                // 남은 거리가 단위거리보다 작을경우 나가기
                double _temp = GeoManager.getInstance().distance(_next.getLatitude(), _next.getLongitude(), _end.getLatitude(), _end.getLongitude());
                if(_temp < front_distance) break;
                upload_mission.add(_next);
            }
            upload_mission.add(_end);
        }

        // 비행고도 적용
        for(GeoPoint point : upload_mission){
            point.setAltitude(mission_altitude);
        }

        return  upload_mission;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String _progress;
        switch (seekBar.getId())
        {
            case R.id.pixhawk_seekbar_mission_flight_speed:         // 비행속도
                if(progress < 10) progress = 10;            // 최저값 1.0m/s
                mission_flight_speed = ((float)progress)/10;
                _progress = String.format("%.1f m/s", mission_flight_speed);
                tv_mission_flight_speed.setText(_progress);
                setMissionPolygon();
                break;
            case R.id.pixhawk_seekbar_mission_angle:                // 영역회전
                mission_angle = progress;
                _progress = String.format("%d", progress);
                tv_mission_angle.setText(_progress + "°");
                setMissionPolygon();
                break;
            case R.id.pixhawk_seekbar_mission_flight_altitude:      // 비행고도
                if(progress < 5) progress = 5;
                mission_altitude = ((float)progress);
                _progress = String.format("%d", progress);
                tv_mission_flight_altitude.setText(_progress + "m");
                setMissionPolygon();
                break;
            case R.id.pixhawk_seekbar_mission_overlap:              // 종중복도
                if(progress < 1) progress = 1;
                mission_front_lap= ((float)progress);
                _progress = String.format("%d", progress);
                tv_mission_overlap.setText(_progress + "%");
                setMissionPolygon();
                break;
            case R.id.pixhawk_seekbar_mission_sidelap:              // 횡중복도
                if(progress < 1) progress = 1;
                mission_side_lap = ((float)progress);
                _progress = String.format("%d", progress);
                tv_mission_sidelap.setText(_progress + "%");
                setMissionPolygon();
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        // 마커 생성
        Marker _marker = getDefaultMarker(p);
        map_view.getOverlays().add(_marker);
        // Add List
        selected_points.add(_marker);

        mWaypoints.add(p);
        setMissionPolygon();

        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     *  선택한 좌표의 마커를 생성한다.
     * @param p 터치한 위치 좌표
     * @return  마커
     */
    private Marker getDefaultMarker(GeoPoint p) {
        Marker _marker = new Marker(map_view);
        String _title;
        _marker.setPosition(new GeoPoint(p.getLatitude(), p.getLongitude()));
        _marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        _title = String.valueOf(selected_points.size() + 1);
        _marker.setDraggable(true);
        _marker.setOnMarkerDragListener(new PixhawkMissionView.OnMarkerDragListenerDrawer());
        _marker.setIcon(MapLayer.getInstance().writeOnDrawable(context, _title, R.drawable.waypoint));

        _marker.setTitle(_title);
        _marker.setOnMarkerClickListener(this);

        return  _marker;
    }

    /**
     * 선택한 좌표의 마커를 생성한다.
     * @param p 터치한 위치 좌표
     * @param text 마커 텍스트
     * @return 마커
     */
    private Marker getDefaultMarker(GeoPoint p, String text) {
        Marker _marker = new Marker(map_view);
        _marker.setPosition(new GeoPoint(p.getLatitude(), p.getLongitude()));
        _marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        _marker.setIcon(MapLayer.getInstance().writeOnDrawable(context, text, R.drawable.waypoint_s));
        _marker.setOnMarkerClickListener(this);

        return  _marker;
    }
    //endregion


    //region 마커 이벤트
    /**
     * 마커 드래그 이벤트
     */
    class OnMarkerDragListenerDrawer implements Marker.OnMarkerDragListener {

        @Override public void onMarkerDrag(Marker marker) {
        }

        @Override public void onMarkerDragEnd(Marker marker) {

        }

        @Override public void onMarkerDragStart(Marker marker) {

        }
    }

    /**
     * 마커 선택시 아이콘 변경
     */
    @Override
    public boolean onMarkerClick(Marker marker, MapView mapView) {
        return true;
    }

    /**
     * 마커 초기화
     */
    private void clearMarkerIcon() {
    }
    //endregion


    /**
     * 임무 Polygon 세팅
     */
    private void setMissionPolygon() {

        // 웨이포인트 위치에 따라 순서 정렬(1번은 확정)
        if(mWaypoints != null && mWaypoints.size() > 0) {
            // 촬영 영역
            area_points.clear();
            area_points.addAll(mWaypoints);
            area_points.add(area_points.get(0));
        }
        if(selected_points.size() < 3) {
            if(map_view != null) map_view.invalidate();
            return;
        }

        flight_area.setPoints(area_points);

        setOverlapDistance();
        // 경계에서 동쪽으로 일정거리만큼 떨어진 지점의 좌표
        flight_points.clear();
        flight_points = GeoManager.getInstance().getPositionsFromRectD(mWaypoints, side_distance, mission_angle);

        setEntryExit();
        flight_path.setPoints(flight_points);
        if(!map_view.getOverlays().contains(flight_path)) map_view.getOverlayManager().add(flight_path);
        map_view.invalidate();

        // 비행경로 정보 재설정
        setMissionInfo();
    }

    /**
     * 비행경로 시작점과 종료점을 나타내는 마커를 추가한다.
     */
    private void setEntryExit() {
        if(flight_points.size() < 2) return;

        // 선택된 마커 제거
        for(Marker marker : selected_points){
            map_view.getOverlayManager().remove(marker);
        }
        // 시작과 종료점 제거
        for(Marker marker : entry_exit){
            map_view.getOverlayManager().remove(marker);
        }
        entry_exit.clear();

        Marker _entry = getDefaultMarker(flight_points.get(0), "S");
        entry_exit.add(_entry);
        map_view.getOverlays().add(_entry);

        Marker _exit = getDefaultMarker(flight_points.get(flight_points.size() - 1), "E");
        entry_exit.add(_exit);
        map_view.getOverlays().add(_exit);

        for(Marker marker : selected_points){
            map_view.getOverlays().add(marker);
        }
    }

    /**
     * 임무 Polygon 세팅
     * @param waypoints : 선택한 웨이포인트
     */
    private void setMissionPolygon(List<GeoPoint> waypoints) {
        if(mWaypoints != null) {
            // 촬영 영역
            mWaypoints.clear();
            mWaypoints.addAll(waypoints);
        }

        for(GeoPoint _point : area_points) {
            Marker _waypoint = getDefaultMarker(new GeoPoint(_point.getLatitude(), _point.getLongitude()));
            map_view.getOverlays().add(_waypoint);
            // Add List
            selected_points.add(_waypoint);
        }

        setOverlapDistance();
        // 경계에서 동쪽으로 일정거리만큼 떨어진 지점의 좌표
        flight_points.clear();
        flight_points = GeoManager.getInstance().getPositionsFromRectD(mWaypoints, side_distance, mission_angle);

        setEntryExit();
        flight_path.setPoints(flight_points);
        if(!map_view.getOverlays().contains(flight_path)) map_view.getOverlayManager().add(flight_path);
        flight_area.setPoints(area_points);

        // 촬영지역의 중심 위치로 배경지도 중심점 변경
        IMapController mapController = map_view.getController();
        GeoPoint _center = GeoManager.getInstance().getCenter(mWaypoints);
        mapController.setCenter(_center);

        map_view.invalidate();

        // 비행경로 정보 재설정
        setMissionInfo();
    }

    /**
     * 종횡중복도 계산
     */
    private void setOverlapDistance(){
        float cmos_factor = (float)(13.2/8.8);
        float camera_aspect_ratio = 0.5625f;

        //
        if(pixhawk_textview_sensor_width.getText().length() > 0
            && pixhawk_textview_focal_length.getText().length() > 0
            && pixhawk_textview_image_width.getText().length() > 0
            && pixhawk_textview_image_height.getText().length() > 0)
        {
            float _sensor_width = Float.parseFloat(pixhawk_textview_sensor_width.getText().toString());
            float _foccal_length = Float.parseFloat(pixhawk_textview_focal_length.getText().toString());

            float _image_width = Float.parseFloat(pixhawk_textview_image_width.getText().toString());
            float _image_height = Float.parseFloat(pixhawk_textview_image_height.getText().toString());

            cmos_factor = _sensor_width/_foccal_length;
            camera_aspect_ratio = _image_height/_image_width;

            SharedPreferences.Editor _editor = pref.edit();
            _editor.putString("sensor_width", pixhawk_textview_sensor_width.getText().toString());
            _editor.putString("focal_length", pixhawk_textview_focal_length.getText().toString());
            _editor.putString("image_width", pixhawk_textview_image_width.getText().toString());
            _editor.putString("image_height", pixhawk_textview_image_height.getText().toString());
            _editor.commit();
        }


        // 종간격 계산
        front_distance = mission_altitude*camera_aspect_ratio*cmos_factor*(100 - mission_front_lap)/100;
        // 횡간격 :
        side_distance = mission_altitude*cmos_factor*(100 - mission_side_lap)/100;
    }

    /**
     * 선택한 좌표의 거리, 총거리등을 계산
     */
    private void setMissionInfo() {
        // 면적 계산
        double _area = GeoManager.getInstance().getAreaFromPoints(area_points, "ha");
        tv_mission_area.setText(String.format("%.2f ha", _area));

        RectD _rect = GeoManager.getInstance().getPolygonBoundRect(area_points);

        //거리계산
        double _dist_mission = flight_path.getDistance();

        // 총거리
        double _dist_total = 0;
        if(my_location != null && flight_points.size() > 1){
            GeoPoint first = flight_points.get(0);
            GeoPoint last = flight_points.get(flight_points.size() - 1);
            _dist_total = _dist_mission + GeoManager.getInstance().distance(my_location.getLatitude(), my_location.getLongitude(), first.getLatitude(), first.getLongitude())
                    + GeoManager.getInstance().distance(my_location.getLatitude(), my_location.getLongitude(), last.getLatitude(), last.getLongitude());
        }else  _dist_total = _dist_mission;

        tv_mission_distance.setText(String.format("%.2f m/%.2f m", _dist_mission, _dist_total) );

        // 촬영간격 계산
        shoot_time_interval = (int)(front_distance/mission_flight_speed);
        shoot_time_interval = Math.max(2, shoot_time_interval);
        shoot_count = 0;
        // 경로 나누기
        for(int i = 0; i < flight_points.size(); i++){
            // 시작점
            GeoPoint _start = flight_points.get(i);
            // 끝점
            GeoPoint _end = flight_points.get(++i);

            // 진행방향(남북)
            int direction_ns = (_start.getLatitude() > _end.getLatitude()) ? -1 : 1;
            int direction_ew = (_start.getLongitude() > _end.getLongitude()) ? -1 : 1;

            // 시작점과 끝점 사이 거리
            for(int j = 1; ; j++)
            {
                // 시작점부터 거리 구하기
                double _sin = Math.abs(Math.sin(Math.toRadians(mission_angle)));
                double _cos = Math.abs(Math.cos(Math.toRadians(mission_angle)));
                double _east_west = front_distance*j*_sin*direction_ew;
                double _north_south = front_distance*j*_cos*direction_ns;

                GeoPoint _next = GeoManager.getInstance().getPositionFromDistance(_start, _east_west, _north_south);

                // 남은 거리가 단위거리보다 작을경우 나가기
                double _temp = GeoManager.getInstance().distance(_next.getLatitude(), _next.getLongitude(), _end.getLatitude(), _end.getLongitude());

                shoot_count++;
                if(_temp < front_distance) break;
            }
        }

        tv_mission_shoot_interval.setText(String.format("%d", shoot_count));
        // 종간격, 횡간격 정보 업데이트
        tv_mission_lap_distance.setText(String.format("F:%.1f m/S:%.1f m", front_distance, side_distance));
    }

    /**
     * 현재 설정된 비행경로를 초기화한다.
     */
    private void clearMission() {
        for(Marker marker :  selected_points){
            map_view.getOverlayManager().remove(marker);
        }

        for(Marker marker : entry_exit){
            map_view.getOverlayManager().remove(marker);
        }

        selected_points.clear();
        entry_exit.clear();
        mWaypoints.clear();

        flight_area.getPoints().clear();
        flight_path.getPoints().clear();

        flight_points.clear();
        area_points.clear();

        map_view.getOverlayManager().remove(flight_path);

        map_view.invalidate();

        tv_mission_shoot_interval.setText("-");
        tv_mission_lap_distance.setText("-");
        tv_mission_distance.setText("-");
        tv_mission_area.setText("-");
    }

    /**
     * 기체정보 수집
     */
    private class CollectDroneInformationTimer extends TimerTask {
        @Override
        public void run() {
            // 화면 표시
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        if(DroneApplication.getDroneInstance() != null){
                            DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();

                            map_view.invalidate();
                        }
                    }
                });
            }

        }
    }

    public class DecimalDigitsInputFilter implements InputFilter {

        Pattern mPattern;

        public DecimalDigitsInputFilter(int digitsBeforeZero,int digitsAfterZero) {
            mPattern=Pattern.compile("[0-9]{0," + (digitsBeforeZero-1) + "}+((\\.[0-9]{0," + (digitsAfterZero-1) + "})?)||(\\.)?");
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

            Matcher matcher=mPattern.matcher(dest);
            if(!matcher.matches())
                return "";
            return null;
        }

    }
}
