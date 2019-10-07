package kr.go.forest.das.UI;

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
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.model.LocationCoordinate2D;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.RectD;
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

    Timer timer = null;                                         // 드론정보 수집 타이머

    Marker marker_my_location = null;                           // 조종기 위치 정보 마커
    Marker marker_drone_location = null;                        // 드론 위치 정보 마커
    Marker marker_home_location = null;                         // 드론 이륙지점 위치 마커
    GeoPoint my_location = null;                                // 현재 조종자 위치
    int mission_status = 0;                                     // 임무 상태

    List<Marker> selected_points = new ArrayList<Marker>();          // 사용자가 선택한 위치를 나타내는 마커
    List<GeoPoint> mWaypoints = new ArrayList<GeoPoint>();             // 사용자가 선택한 위치
    List<GeoPoint> area_points = new ArrayList<GeoPoint>();            // 촬영영역을 보여주기 위한 위치
    List<GeoPoint> flight_points = new ArrayList<GeoPoint>();          // 촬영영역을 보여주기 위한 위치
    List<Marker> entry_exit = new ArrayList<Marker>();               // 임무 시작점과 종료점 마커

    Polygon flight_area = new Polygon();                        // 촬영영역
    Polyline flight_path = new Polyline();                      // 비행경로

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
        DroneApplication.getEventBus().register(this);
        handler_ui = new Handler(Looper.getMainLooper());
        progress = new ProgressDialog(context);

        // Data 수집 타이머 시작
        if(timer == null) {
            timer = new Timer();
        }
        timer.schedule(new PixhawkMissionView.CollectDroneInformationTimer(), 0, period);

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;

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

        // MapView 설정
        map_view = (MapView) findViewById(R.id.mapView);
        map_view.setBuiltInZoomControls(false);
        map_view.setMultiTouchControls(true);
        map_view.setMinZoomLevel(8.0);
        map_view.setMaxZoomLevel(20.0);
        map_view.setTileSource(VWorldStreet);

        IMapController mapController = map_view.getController();
        mapController.setZoom(18.0);

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
        map_view.setOnClickListener(this);
        map_view.invalidate();

        setClickable(true);
    }

    private void setWidget() {
    }

    @Subscribe
    public void onUpdateLocation(final MainActivity.LocationUpdate location) {

    }

    @Subscribe
    public void onMissionLoad(final MainActivity.Mission mission) {
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    /**
     *  선택한 좌표의 마커를 생성한다.
     */
    private Marker getDefaultMarker(GeoPoint p) {
        return  null;
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
    }

    /**
     * 임무 Polygon 세팅
     */
    private void setMissionPolygon(List<GeoPoint> waypoints) {
    }

    /**
     * 선택한 좌표의 거리, 총거리등을 계산
     */
    private void setMissionInfo() {
    }

    private void clearMission() {
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
