package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.model.LocationCoordinate2D;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.RectD;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.Model.WaypointMission;
import kr.go.forest.das.R;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;

public class MissionView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver, Marker.OnMarkerClickListener, SeekBar.OnSeekBarChangeListener {

    private final int period = 250;                             // 드론정보 수십 주기 0.25 second

    private final int MISSION_LOCATION_UPDATED = 0x02;
    private final int MISSION_SEARCH_LOCATION = 0x04;

    private Context context;
    private MapView map_view = null;
    private Handler handler_ui;
    private SharedPreferences pref;

    private float mission_altitude = 0.0f;
    private float mission_flight_speed = 0.0f;
    Timer timer = null;                                         // 드론정보 수집 타이머

    Marker marker_my_location = null;                           // 조종기 위치 정보 마커
    Marker marker_drone_location = null;                        // 드론 위치 정보 마커
    Marker marker_home_location = null;                         // 드론 이륙지점 위치 마커
    GeoPoint my_location = null;                                // 현재 조종자 위치
    int mission_status = 0;                                     // 임무 상태
    WaypointMission waypoint_mission = null;

    int shoot_time_interval = 0;
    int shoot_count = 0;

    List<Marker> selected_points = new ArrayList<Marker>();
    List<GeoPoint> waypoints = new ArrayList<GeoPoint>();
    List<GeoPoint> area_points = new ArrayList<GeoPoint>();



    Polygon flight_area = new Polygon();


    Polyline flight_path = new Polyline();
    Polyline rect_path = new Polyline();

    Button btn_location;;
    Button mBtnLoadShape;
    Button mBtnNew;
    Button btn_upload;
	Button mBtnBack;
	Button btn_mission_start;

	TextView tv_mission_area;
	TextView tv_mission_distance;
    TextView tv_mission_flight_speed;
    TextView tv_mission_angle;
    TextView tv_mission_flight_altitude;
    TextView tv_mission_overlap;
    TextView tv_mission_sidelap;

    SeekBar sb_mission_flight_speed;
    SeekBar sb_mission_angle;
    SeekBar sb_mission_flight_altitude;
    SeekBar sb_mission_overlap;
    SeekBar sb_mission_sidelap;

    public MissionView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public MissionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {

        DroneApplication.getEventBus().register(this);
        handler_ui = new Handler(Looper.getMainLooper());
        // Data 수집 타이머 시작
        if(timer == null) {
            timer = new Timer();
        }
        timer.schedule(new MissionView.CollectDroneInformationTimer(), 0, period);

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        // Clear Overlays
        map_view.getOverlays().clear();
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;

        selected_points.clear();
        selected_points = null;

        waypoints.clear();
        waypoints = null;

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
        layoutInflater.inflate(R.layout.content_mission, this, true);

       setWidget();

        // MapView 설정
        map_view = (MapView) findViewById(R.id.mapView);
        map_view.setBuiltInZoomControls(false);
        map_view.setMultiTouchControls(true);
        map_view.setMinZoomLevel(8.0);
        map_view.setMaxZoomLevel(20.0);
        map_view.setTileSource(VWorldStreet);

        IMapController mapController = map_view.getController();
        mapController.setZoom(17.0);

        // Touch Overlay
        MapEventsOverlay _events = new MapEventsOverlay(this);
        map_view.getOverlays().add(_events);

        // 현재 GPS 좌표 불러오기
        String _lat = pref.getString("lat", null);
        String _lon = pref.getString("lon", null);

        if(_lat == null && _lon == null) {
            mapController.setCenter(new GeoPoint(36.361481, 127.384841));
        }else{
            my_location = new GeoPoint(Double.parseDouble(_lat), Double.parseDouble(_lon));
            mapController.setCenter(my_location);
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

        rect_path.setColor(Color.WHITE);
        rect_path.setWidth(2.0f);

        flight_path.setColor(Color.RED);
        flight_path.setWidth(2.0f);

        map_view.getOverlayManager().add(flight_area);
        map_view.getOverlayManager().add(rect_path);
        map_view.getOverlayManager().add(flight_path);

        HashMap<String, Polygon> _noFlyZone = MapLayer.getInstance().getNoFlyZoneFromValue("");

        for(String key: _noFlyZone.keySet())
        {
            Polygon _zone = _noFlyZone.get(key);
            _zone.setOnClickListener(new Polygon.OnClickListener(){
                @Override
                public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                    return false;
                }
            });
            map_view.getOverlayManager().add(_zone);
        }


        map_view.setOnClickListener(this);
        map_view.invalidate();
        setClickable(true);
    }

    private void setWidget() {
        // Button
        mBtnBack = (Button)findViewById(R.id.btn_mission_back);
        mBtnBack.setOnClickListener(this);

        btn_location = (Button)findViewById(R.id.btn_mission_location);
        btn_location.setOnClickListener(this);

        mBtnNew = (Button)findViewById(R.id.btn_new_course);
        mBtnNew.setOnClickListener(this);

        mBtnLoadShape = (Button)findViewById(R.id.btn_load_shape);
        mBtnLoadShape.setOnClickListener(this);

        btn_upload = (Button)findViewById(R.id.btn_mission_upload);
        btn_upload.setOnClickListener(this);

        btn_mission_start = (Button)findViewById(R.id.btn_mission_start);
        btn_mission_start.setOnClickListener(this);

        // TextView
        tv_mission_area = (TextView)findViewById(R.id.mission_area);
        tv_mission_distance = (TextView)findViewById(R.id.mission_distance);
        tv_mission_flight_speed = (TextView)findViewById(R.id.textview_mission_flight_speed);
        tv_mission_angle = (TextView)findViewById(R.id.textview_mission_angle);
        tv_mission_flight_altitude = (TextView)findViewById(R.id.textview_mission_flight_altitude);
        tv_mission_overlap = (TextView)findViewById(R.id.textview_mission_overlap);
        tv_mission_sidelap = (TextView)findViewById(R.id.textview_mission_sidelap);

        // Seekbar
        sb_mission_flight_speed = (SeekBar)findViewById(R.id.seekbar_mission_flight_speed);
        sb_mission_angle = (SeekBar)findViewById(R.id.seekbar_mission_angle);
        sb_mission_flight_altitude = (SeekBar)findViewById(R.id.seekbar_mission_flight_altitude);
        sb_mission_overlap = (SeekBar)findViewById(R.id.seekbar_mission_overlap);
        sb_mission_sidelap = (SeekBar)findViewById(R.id.seekbar_mission_sidelap);

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

    @Subscribe
    public void onUpdateLocation(final MainActivity.LocationUpdate location) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    LogWrapper.i("MissionView", "onUpdateLocation");
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

    @Subscribe
    public void onMissionLoad(final MainActivity.Mission mission) {
        if(mission.command == MainActivity.Mission.MISSION_FROM_FILE || mission.command == MainActivity.Mission.MISSION_FROM_ONLINE){
            // 임무정보 초기화
            clearMission();

            // 불러온 정보로 Polygon 만들기
            int _ret = GeoManager.getInstance().getPositionsFromShapeFile(mission.data, area_points);

            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                    // 마커 포함 임무 그리기
                    setMissionPolygon(area_points);
                    flight_area.setPoints(area_points);
                    map_view.invalidate();
                    }
                });
            }
            // 파일이 잘못되었을 경우 팝업
        }else if(mission.command == MainActivity.Mission.MISSION_UPLOAD){
            LogWrapper.i("WaypointMission", "MISSION_UPLOAD");
            String _result = DroneApplication.getDroneInstance().uploadMission(waypoint_mission.getMission());

            if(_result != null){
                // 오류 팝업
                LogWrapper.i("WaypointMission", "uploadMission Fail : " +_result);
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_upload_fail, _result));
            }
        }else if(mission.command == MainActivity.Mission.MISSION_UPLOAD_FAIL){
            // 미션 업로드 실패
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_upload_fail, mission.data));
        }else if(mission.command == MainActivity.Mission.MISSION_UPLOAD_SUCCESS){
            // 미션 업로드 성공
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        btn_mission_start.setVisibility(VISIBLE);
                        btn_upload.setVisibility(INVISIBLE);
                    }
                });
            }
            // 임무 시작 조건 설정
            shoot_count  = 9999;
            shoot_time_interval = 2;
            DroneApplication.getDroneInstance().setMissionCondition(shoot_count, shoot_time_interval);

        }else if(mission.command == MainActivity.Mission.MISSION_START_FAIL){
            // 미션 시작 실패
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_start_fail, mission.data));
        }else if(mission.command == MainActivity.Mission.MISSION_START_SUCCESS){
            // 비행화면으로 전환
            DroneApplication.getEventBus().post(new ViewWrapper(new FlightView(context), true));
        }else if(mission.command == MainActivity.Mission.MAX_FLIGHT_HEIGHT_SET_SUCCESS){
            // 드론 최대비행고도 변경 성공
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.max_flight_height_success, null));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_mission_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.btn_mission_location: // 현재 위치로 이동
                if (my_location != null)
                {
                    marker_my_location.setPosition(my_location);
                    map_view.getController().setCenter(my_location);
                    map_view.invalidate();

                    SharedPreferences.Editor _editor = pref.edit();
                    _editor.putString("lat", String.valueOf(my_location.getLatitude()));
                    _editor.putString("lon", String.valueOf(my_location.getLongitude()));
                    _editor.commit();
                }else{
                    // 위치 확인 팝업
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.search_location));
                    mission_status += MISSION_SEARCH_LOCATION;
                }
                break;
            case R.id.btn_new_course:
                // 현재 임무를 초기화 하겠냐는 팝업
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, 0, R.string.clear_mission));
                // clear waypoints
                clearMission();
                break;
            case R.id.btn_load_shape:
                // Mission 파일 목록 팝업
               DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_LOAD_SHAPE, 0,0));
                break;
            case R.id.btn_mission_upload:
                // 웨이 포인트 3개 이상...확인
                if(waypoints.size() < 2) {
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.mission_create_fail, null));
                    return;
                }
                // 비행고도 적용
                for(GeoPoint point : waypoints){
                    point.setAltitude(mission_altitude);
                }

                // 웨이포인트 생성
                waypoint_mission = new WaypointMission(waypoints, mission_flight_speed);

                if(waypoint_mission.max_flight_altitude > DroneApplication.getDroneInstance().max_flight_height){
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.max_flight_height_low_title, R.string.max_flight_height_low, ""));
                }else {
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_UPLOAD_MISSION, 0, 0));
               }
                break;
            case R.id.btn_mission_start:
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.mission_start_title, R.string.mission_start_content, ""));
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String _progress;
        switch (seekBar.getId())
        {
            case R.id.seekbar_mission_flight_speed:         // 비행속도

                mission_flight_speed = ((float)progress)/10;
                _progress = String.format("%.1f m/s", mission_flight_speed);
                tv_mission_flight_speed.setText(_progress);
                break;
            case R.id.seekbar_mission_angle:                // 영역회전
                _progress = String.format("%d", progress);
                tv_mission_angle.setText(_progress + "°");
                break;
            case R.id.seekbar_mission_flight_altitude:      // 비행고도
                mission_altitude = ((float)progress);
                _progress = String.format("%d", progress);
                tv_mission_flight_altitude.setText(_progress + "m");
                break;
            case R.id.seekbar_mission_overlap:              // 종중복도
                _progress = String.format("%d", progress);
                tv_mission_overlap.setText(_progress + "%");
                break;
            case R.id.seekbar_mission_sidelap:              // 횡중복도
                _progress = String.format("%d", progress);
                tv_mission_sidelap.setText(_progress + "%");
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

        if(selected_points.size() < 99) {
            // 마커 생성
            Marker _waypoint = getDefaultMarker(p);
            map_view.getOverlays().add(_waypoint);
            // Add List
            selected_points.add(_waypoint);

            waypoints.add(p);
            if(selected_points.size() > 2) {
                setMissionPolygon();
            }

            map_view.invalidate();
        }

        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     *  선택한 좌표의 마커를 생성한다.
     */
    private Marker getDefaultMarker(GeoPoint p) {
        Marker _marker = new Marker(map_view);
        String _title = String.valueOf(selected_points.size() + 1);
        _marker.setIcon(MapLayer.getInstance().writeOnDrawable(context, _title, R.drawable.waypoint));
        _marker.setPosition(p);
        _marker.setTitle(_title);
        _marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        _marker.setDraggable(true);
        _marker.setOnMarkerDragListener(new OnMarkerDragListenerDrawer());
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
            // 비행경로 Update

            // Polygon 좌표 Update
            int _marker_index = Integer.parseInt(marker.getTitle()) - 1;
            area_points.get(_marker_index).setLatitude(marker.getPosition().getLatitude());
            area_points.get(_marker_index).setLongitude(marker.getPosition().getLongitude());
            flight_area.setPoints(area_points);
            setMissionPolygon();
        }

        @Override public void onMarkerDragEnd(Marker marker) {
            clearMarkerIcon();
            map_view.invalidate();
        }

        @Override public void onMarkerDragStart(Marker marker) {
            clearMarkerIcon();
            marker.setIcon(MapLayer.getInstance().writeOnDrawable(context, marker.getTitle(), R.drawable.waypoint_s));
        }
    }

    /**
     * 마커 선택시 아이콘 변경
     */
    @Override
    public boolean onMarkerClick(Marker marker, MapView mapView) {
        clearMarkerIcon();
        marker.setIcon(MapLayer.getInstance().writeOnDrawable(context, marker.getTitle(), R.drawable.waypoint_s));
        map_view.invalidate();
        return true;
    }

    /**
     * 마커 초기화
     */
    private void clearMarkerIcon() {
        for(int i = 0; i < selected_points.size(); i++) {
            selected_points.get(i).setIcon(MapLayer.getInstance().writeOnDrawable(context, selected_points.get(i).getTitle(), R.drawable.waypoint));
        }
    }
    //endregion


    /**
     * 임무 Polygon 세팅
     */
    private void setMissionPolygon() {
        // 촬영 영역
        area_points.clear();

        // 웨이포인트 위치에 따라 순서 정렬(1번은 확정)
        area_points.addAll(waypoints);
        area_points.add(area_points.get(0));
        flight_area.setPoints(area_points);

        // 촬영영역 정보
        setMissionInfo();

        // 촬영영역을 포함하는 경계
        rect_path.setPoints(GeoManager.getInstance().getPolygonBoundRect(waypoints).getPoints());

        // 경계에서 동쪽으로 20m씩 떨어진 지점의 좌표
        flight_path.setPoints(GeoManager.getInstance().getPositionsFromRectD(waypoints, 20.0f, 0.0f));
    }

    /**
     * 임무 Polygon 세팅
     */
    private void setMissionPolygon(List<GeoPoint> waypoints) {

        for(GeoPoint _point : waypoints)
        {
            Marker _waypoint = getDefaultMarker(_point);
            map_view.getOverlays().add(_waypoint);
            // Add List
            selected_points.add(_waypoint);
            waypoints.add(_point);
        }

        setMissionInfo();
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
        List<GeoPoint> _pp = _rect.getPoints();
        // 미션거리
        int _dist_mission = GeoManager.getInstance().getDistanceFromPoints(_pp);

        // 총거리
        _pp.add(my_location);
        int _dist_total = GeoManager.getInstance().getDistanceFromPoints(_pp);

        tv_mission_distance.setText(String.format("%d m/%d m", _dist_mission, _dist_total) );
    }

    private void clearMission() {
        for(int i = 0; i < selected_points.size(); i++)
        {
            map_view.getOverlays().remove(selected_points.get(i));
        }
        selected_points.clear();
        waypoints.clear();
        flight_area.getPoints().clear();
        area_points.clear();
        map_view.invalidate();
    }

    /**
     * 시동 걸리고 주어진 주기로 기체정보 수집
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

                            // 1. 홈 위치
                            LocationCoordinate2D _home = DroneApplication.getDroneInstance().getHomeLocation();
                            marker_home_location.setPosition(new GeoPoint(_home.getLatitude(), _home.getLongitude()));

                            // 2. 드론 Heading 및 위치
                            marker_drone_location.setIcon(MapLayer.getInstance().getRotateDrawable(context, R.mipmap.map_ico_drone, _info.heading));
                            marker_drone_location.setPosition(new GeoPoint(_info.drone_latitude, _info.drone_longitude));

                            // 3. 조종기 위치
                            marker_my_location.setPosition(new GeoPoint(_info.rc_latitude, _info.rc_longitude));

                            map_view.invalidate();
                        }
                    }
                });
            }

        }
    }

}
