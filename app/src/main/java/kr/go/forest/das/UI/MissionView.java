package kr.go.forest.das.UI;

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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.RectD;
import kr.go.forest.das.Model.WaypointMission;
import kr.go.forest.das.R;

import static kr.go.forest.das.map.MapManager.VWorldStreet;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;

public class MissionView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver, Marker.OnMarkerClickListener, SeekBar.OnSeekBarChangeListener {

    private final int MISSION_LOCATION_UPDATED = 0x02;
    private final int MISSION_SEARCH_LOCATION = 0x04;

    private Context context;
    private MapView map_view = null;
    private Handler handler_ui;
    private SharedPreferences pref;

    Marker marker_my_location = null;
    GeoPoint my_location;
    int mission_status = 0;

    List<Marker> selected_points = new ArrayList<Marker>();
    List<GeoPoint> mWaypoints = new ArrayList<GeoPoint>();
    List<GeoPoint> area_points = new ArrayList<GeoPoint>();
    List<GeoPoint> mListPolyline = new ArrayList<GeoPoint>();


    Polygon flight_area = new Polygon();
    Polyline mFlightPath = new Polyline();

    Button btn_location;;
    Button mBtnLoadShape;
    Button mBtnNew;
    Button mBtnUpload;
	Button mBtnBack;

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

        mWaypoints.clear();
        mWaypoints = null;

        area_points.clear();
        area_points = null;

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
        String _lat = null;//pref.getString("lat", null);
        String _lon = null;//pref.getString("lon", null);

        if(_lat == null && _lon == null) {
            my_location = new GeoPoint(36.361481, 127.384841);
        }else{
            my_location = new GeoPoint(Double.parseDouble(_lat), Double.parseDouble(_lon));
        }

        mapController.setCenter(my_location);

        // 마커 설정
        marker_my_location = new Marker(map_view);
        marker_my_location.setIcon(ContextCompat.getDrawable(context, R.mipmap.map_ico_my));
        //marker_my_location.setPosition(my_location);
        map_view.getOverlays().add(marker_my_location);

        // 폴리곤 설정
        flight_area.setFillColor(Color.argb(60, 0, 255, 0));
        flight_area.setStrokeWidth(1.0f);

        mFlightPath.setColor(Color.WHITE);
        mFlightPath.setWidth(2.0f);

        map_view.getOverlayManager().add(flight_area);
        map_view.getOverlayManager().add(mFlightPath);

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
//            mapView.getOverlayManager().add(_zone);
//        }


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

        mBtnUpload = (Button)findViewById(R.id.btn_mission_upload);
        mBtnUpload.setOnClickListener(this);

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
                    my_location.setLatitude(location.latitude);
                    my_location.setLongitude(location.longitude);

                    if ((mission_status & MISSION_SEARCH_LOCATION) == MISSION_SEARCH_LOCATION)
                    {
                        marker_my_location.setPosition(my_location);
                        map_view.getController().setCenter(my_location);
                        map_view.invalidate();
                        mission_status -= MISSION_SEARCH_LOCATION;
                    }

                    mission_status = mission_status | MISSION_LOCATION_UPDATED;
                }
            });
        }
    }

    @Subscribe
    public void onMissionLoad(final MainActivity.Mission mission) {
        // 임무정보 초기화
        clearMission();

        if(mission.command != MainActivity.Mission.MISSION_CLEAR){
            // 불러온 정보로 Polygon 만들기
            int _ret = GeoManager.getInstance().getPositionsFromShapeFile(mission.data, area_points);

            // 파일이 잘못되었을 경우 팝업

            // 마커 포함 임무 그리기
            setMissionPolygon(area_points);
            flight_area.setPoints(area_points);
        }

        map_view.invalidate();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_mission_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.btn_mission_location: // 현재 위치로 이동
//                if ((mission_status & MISSION_LOCATION_UPDATED) == MISSION_LOCATION_UPDATED)
//                {
//                    marker_my_location.setPosition(my_location);
//                    map_view.getController().setCenter(my_location);
//                    map_view.invalidate();
//
//                    SharedPreferences.Editor _editor = pref.edit();
//                    _editor.putString("lat", String.valueOf(my_location.getLatitude()));
//                    _editor.putString("lon", String.valueOf(my_location.getLongitude()));
//                    _editor.commit();
//                }else{
                    // 위치 확인 팝업
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.search_location));
                    mission_status = mission_status | MISSION_SEARCH_LOCATION;
//                }
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
                WaypointMission _waypoint = new WaypointMission(mWaypoints, 5.0f);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        String _progress;
        switch (seekBar.getId())
        {
            case R.id.seekbar_mission_flight_speed:         // 비행속도

                double _convert = ((double)progress)/10;
                _progress = String.format("%.1f m/s", _convert);
                tv_mission_flight_speed.setText(_progress);
                break;
            case R.id.seekbar_mission_angle:                // 영역회전
                _progress = String.format("%d", progress);
                tv_mission_angle.setText(_progress + "°");
                break;
            case R.id.seekbar_mission_flight_altitude:      // 비행고도
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

            mWaypoints.add(p);
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
        area_points.clear();
        area_points.addAll(mWaypoints);
        area_points.add(area_points.get(0));
        flight_area.setPoints(area_points);
        setMissionInfo();
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
            mWaypoints.add(_point);
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
        mWaypoints.clear();
        flight_area.getPoints().clear();
        area_points.clear();
        map_view.invalidate();
    }

}
