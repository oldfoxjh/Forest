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

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.RectD;
import kr.go.forest.das.R;
import kr.go.forest.das.geo.GeoManager;

import static kr.go.forest.das.map.MapManager.VWorldStreet;

public class Px4MissionView extends RelativeLayout implements View.OnClickListener, MapEventsReceiver, Marker.OnMarkerClickListener, SeekBar.OnSeekBarChangeListener {

    private Context context;

    public Px4MissionView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public Px4MissionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void initUI(){
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
