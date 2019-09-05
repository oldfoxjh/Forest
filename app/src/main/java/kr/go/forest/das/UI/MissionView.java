package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.RectD;
import kr.go.forest.das.R;

import static kr.go.forest.das.map.MapManager.VWorldStreet;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;

public class MissionView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver, Marker.OnMarkerClickListener, SeekBar.OnSeekBarChangeListener {
    private Context context;
    private MapView map_view = null;
    private Handler handler_ui;

    Marker marker_my_location = null;

    List<Marker> selected_points = new ArrayList<Marker>();
    List<GeoPoint> mWaypoints = new ArrayList<GeoPoint>();
    List<GeoPoint> area_points = new ArrayList<GeoPoint>();
    List<GeoPoint> mListPolyline = new ArrayList<GeoPoint>();

    GeoPoint my_location;
    Polygon mFlightArea = new Polygon();
    Polyline mFlightPath = new Polyline();

    Button mBtnLocation;
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

        selected_points.clear();
        selected_points = null;

        mWaypoints.clear();
        mWaypoints = null;

        area_points.clear();
        area_points = null;

        super.onDetachedFromWindow();
    }

    protected void initUI(){

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
        my_location = new GeoPoint(37.6096409, 126.99769700000002);
        GeoPoint startPoint = new GeoPoint(37.6096409, 126.99769700000002);
        mapController.setCenter(startPoint);

        // 마커 설정
        marker_my_location = new Marker(map_view);
        marker_my_location.setIcon(context.getResources().getDrawable(R.drawable.map_ico_my));
        marker_my_location.setPosition(startPoint);
        map_view.getOverlays().add(marker_my_location);

        // 폴리곤 설정
        mFlightArea.setFillColor(Color.argb(60, 0, 255, 0));
        mFlightArea.setStrokeWidth(1.0f);

        mFlightPath.setColor(Color.WHITE);
        mFlightPath.setWidth(2.0f);

        map_view.getOverlayManager().add(mFlightArea);
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

        mBtnLocation = (Button)findViewById(R.id.btn_mission_location);
        mBtnLocation.setOnClickListener(this);

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
                    LogWrapper.i("onUpdateLocation", "updated");

                    marker_my_location.setPosition(new GeoPoint(location.latitude, location.longitude));
                    map_view.invalidate();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_mission_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.btn_mission_location:
                break;
            case R.id.btn_new_course:
                // clear waypoints
                for(int i = 0; i < selected_points.size(); i++)
                {
                    map_view.getOverlays().remove(selected_points.get(i));
                }
                selected_points.clear();

                mWaypoints.clear();
                mFlightArea.getPoints().clear();
                map_view.invalidate();
                break;
            case R.id.btn_load_shape:
                break;
            case R.id.btn_mission_upload:
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
                setPolygonInfo();
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
        _marker.setIcon(writeOnDrawable(_title, R.drawable.waypoint));
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
    class OnMarkerDragListenerDrawer implements Marker.OnMarkerDragListener {

        @Override public void onMarkerDrag(Marker marker) {
            // 비행경로 Update

            // Polygon 좌표 Update
            int _marker_index = Integer.parseInt(marker.getTitle()) - 1;
            area_points.get(_marker_index).setLatitude(marker.getPosition().getLatitude());
            area_points.get(_marker_index).setLongitude(marker.getPosition().getLongitude());
            mFlightArea.setPoints(area_points);
            setPolygonInfo();
        }

        @Override public void onMarkerDragEnd(Marker marker) {
            clearMarkerIcon();
            map_view.invalidate();
        }

        @Override public void onMarkerDragStart(Marker marker) {
            clearMarkerIcon();
            marker.setIcon(writeOnDrawable(marker.getTitle(), R.drawable.waypoint_s));
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker, MapView mapView) {
        clearMarkerIcon();
        marker.setIcon(writeOnDrawable(marker.getTitle(), R.drawable.waypoint_s));
        map_view.invalidate();
        return true;
    }

    private void clearMarkerIcon()
    {
        for(int i = 0; i < selected_points.size(); i++) {
            selected_points.get(i).setIcon(writeOnDrawable(selected_points.get(i).getTitle(), R.drawable.waypoint));
        }
    }
    //endregion

    private void setPolygonInfo()
    {
        area_points.clear();
        area_points.addAll(mWaypoints);
        area_points.add(area_points.get(0));
        mFlightArea.setPoints(area_points);

        // 면적 계산
        double _area = GeoManager.getInstance().getAreaFromPoints(area_points, "ha");
        tv_mission_area.setText(String.format("%.2f ha", _area));

        RectD _rect = GeoManager.getInstance().getPolygonBoundRect(area_points);

//                mFlightPath.setPoints(_rect.getPoints());
//                Date _t = new Date(System.currentTimeMillis());
//                String _tt = new SimpleDateFormat("yyyyMMdd_HHmmss").format(_t);
//                String path = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS + "/" + _tt + ".shp";
//                GeoManager.getInstance().saveShapeFile(path,_rect.getPoints());

        //거리계산
        List<GeoPoint> _pp = _rect.getPoints();
        // 미션거리
        int _dist_mission = GeoManager.getInstance().getDistanceFromPoints(_pp);

        // 총거리
        _pp.add(my_location);
        int _dist_total = GeoManager.getInstance().getDistanceFromPoints(_pp);

        tv_mission_distance.setText(String.format("%d m/%d m", _dist_mission, _dist_total) );
    }

    /**
     * 마커에 숫자 적용
     */
    private BitmapDrawable writeOnDrawable(String text, int drawableId) {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        if(drawableId == R.drawable.waypoint_s) {
            paint.setColor(Color.WHITE);
        }else{
            paint.setColor(Color.BLACK);
        }

        int textSize = getResources().getDimensionPixelSize(R.dimen.mission_font);
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, bm.getWidth()/2- bounds.right/2 - (text.equals("1") ? 2 : 0), bm.getHeight()/2-bounds.top/2, paint);


        return new BitmapDrawable(getResources(), bm);
    }
}