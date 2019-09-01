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
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

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

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.R;

import static kr.go.forest.das.map.MapManager.VWorldStreet;
import dji.ux.widget.BatteryWidget;

public class MissionView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver{
    private Context context;
    private MapView mapView = null;
    private Handler mHandlerUI;

    Marker mLocation = null;

    List<Marker> mMarkes = new ArrayList<Marker>();
    List<GeoPoint> mWaypoints = new ArrayList<GeoPoint>();
    List<GeoPoint> mPolygonPoints = new ArrayList<GeoPoint>();
    List<GeoPoint> mListPolyline = new ArrayList<GeoPoint>();

    Polygon mFlightArea = new Polygon();
    Polyline mFlightPath = new Polyline();

    Button mBtnLocation;
    Button mBtnLoadShape;
    Button mBtnNew;
    Button mBtnUpload;
	Button mBtnBack;

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
        mHandlerUI = new Handler(Looper.getMainLooper());

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {

        // Clear Overlays
        mapView.getOverlays().clear();
        DroneApplication.getEventBus().unregister(this);
        mHandlerUI.removeCallbacksAndMessages(null);

        mMarkes.clear();
        mMarkes = null;

        mWaypoints.clear();
        mWaypoints = null;

        mPolygonPoints.clear();
        mPolygonPoints = null;

        super.onDetachedFromWindow();
    }

    protected void initUI(){
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.content_mission, this, true);

        

        // Button
		mBtnBack = (Button)findViewById(R.id.missionBackButton);
        mBtnBack.setOnClickListener(this);
		
        mBtnLocation = (Button)findViewById(R.id.btn_mission_location);
        mBtnLocation.setOnClickListener(this);

        mBtnNew = (Button)findViewById(R.id.btn_new_course);
        mBtnNew.setOnClickListener(this);

        mBtnLoadShape = (Button)findViewById(R.id.btn_load_shape);
        mBtnLoadShape.setOnClickListener(this);

        mBtnUpload = (Button)findViewById(R.id.btn_mission_upload);
        mBtnUpload.setOnClickListener(this);

        // MapView 설정
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(8.0);
        mapView.setMaxZoomLevel(20.0);
        mapView.setTileSource(VWorldStreet);

        IMapController mapController = mapView.getController();
        mapController.setZoom(17.0);

        // Touch Overlay
        MapEventsOverlay _events = new MapEventsOverlay(this);
        mapView.getOverlays().add(_events);

        // 현재 GPS 좌표 불러오기
        GeoPoint startPoint = new GeoPoint(37.6096409, 126.99769700000002);
        mapController.setCenter(startPoint);

        // 마커 설정
        mLocation = new Marker(mapView);
        mLocation.setIcon(context.getResources().getDrawable(R.drawable.map_ico_my));
        mLocation.setPosition(startPoint);
        mapView.getOverlays().add(mLocation);

        // 폴리곤 설정
        mFlightArea.setFillColor(Color.argb(60, 0, 255, 0));
        mFlightArea.setStrokeWidth(1.0f);

        mapView.getOverlayManager().add(mFlightArea);

        mapView.setOnClickListener(this);
        mapView.invalidate();
        setClickable(true);
    }
	
    @Subscribe
    public void onUpdateLocation(final MainActivity.LocationUpdate location) {
        if (mHandlerUI != null) {
            mHandlerUI.post(new Runnable() {
                @Override
                public void run() {
                    LogWrapper.i("onUpdateLocation", "updated");

                    mLocation.setPosition(new GeoPoint(location.latitude, location.longitude));
                    mapView.invalidate();
                }
            });
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.missionBackButton:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
            case R.id.btn_mission_location:
                break;
            case R.id.btn_new_course:
                // clear waypoints
                for(int i = 0; i < mMarkes.size(); i++)
                {
                    mapView.getOverlays().remove(mMarkes.get(i));
                    mFlightArea.getPoints().clear();
                }

                mapView.invalidate();
                break;
            case R.id.btn_load_shape:
                break;
            case R.id.btn_mission_upload:
                break;
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {

        if(mMarkes.size() < 99)
        {
            // 마커 생성
            Marker _waypoint = new Marker(mapView);
            String _title = String.valueOf(mMarkes.size() + 1);
            _waypoint.setIcon(writeOnDrawable(_title));
            _waypoint.setPosition(p);
            mapView.getOverlays().add(_waypoint);
            // Add List
            mMarkes.add(_waypoint);

            mWaypoints.add(p);
            if(mMarkes.size() > 2)
            {
                mPolygonPoints.clear();
                mPolygonPoints.addAll(mWaypoints);
                mPolygonPoints.add(mPolygonPoints.get(0));
                mFlightArea.setPoints(mPolygonPoints);

                // 면적 계산
            }

            mapView.invalidate();
        }

        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }
    //endregion

    private BitmapDrawable writeOnDrawable(String text)
    {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.waypoint).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        int textSize = getResources().getDimensionPixelSize(R.dimen.mission_font);
        paint.setTextSize(textSize);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, bm.getWidth()/2- bounds.right/2 - (text.equals("1") ? 2 : 0), bm.getHeight()/2-bounds.top/2, paint);

        return new BitmapDrawable(getResources(), bm);
    }
}
