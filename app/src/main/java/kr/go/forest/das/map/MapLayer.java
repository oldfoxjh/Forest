package kr.go.forest.das.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.R;
import kr.go.forest.das.UI.MissionView;
import kr.go.forest.das.geo.GeoManager;

public class MapLayer {
    private static final MapLayer ourInstance = new MapLayer();

    public static MapLayer getInstance() {
        return ourInstance;
    }

    /**
     * Raw 폴더 파일에서 데이터 추출
     */
    private String loadResFromRaw(int rawId) {
        String _result = null;
        try
        {
            InputStream _is = DroneApplication.getInstance().getResources().openRawResource(rawId);
            byte[] _datas = new byte[_is.available()];
            _is.read(_datas);
            _result = new String(_datas);
        }catch (IOException e){
            e.printStackTrace();
        }

        return  _result;
    }

    /**
     * 비행금지구역 정보 가져오기
     */
    public HashMap<String, Polygon> getNoFlyZoneFromValue(String json) {
        HashMap<String, Polygon> _result = null;

       try
       {
           String _info = loadResFromRaw(R.raw.no_fly_zone);

           JSONObject _data = new JSONObject(_info);
           JSONArray _noFlyZone = new JSONArray(_data.getString("NoFlyZone"));
           _result = new HashMap<String, Polygon>();


           for(int i = 0; i < _noFlyZone.length(); i++)
           {
               Polygon _zone = new Polygon();
               _zone.setFillColor(Color.argb(70, 255, 0, 0));
               _zone.setStrokeColor(Color.rgb(255, 0 , 0));
               _zone.setStrokeWidth(2.0f);

               JSONObject _zone_data = _noFlyZone.getJSONObject(i);
               String _name = _zone_data.getString("name");
               JSONArray _points = new JSONArray(_zone_data.getString("points"));

               for(int j = 0; j < _points.length(); j++)
               {
                   JSONArray _point = _points.getJSONArray(j);
                   GeoPoint _geopoint = GeoManager.getInstance().getWGS84Points(_point.getDouble(0), _point.getDouble(1));
                   _zone.addPoint(_geopoint);
               }

               _result.put(_name, _zone);
           }

       }catch (JSONException e)
       {
           e.printStackTrace();
       }

        return  _result;
    }

    /**
     * 마커 아이콘 회전
     */
    public BitmapDrawable getRotateDrawable(Context context, int drawableId, final float degree) {

        final Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Matrix m = new Matrix();
        m.setRotate(degree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);

        return new BitmapDrawable(context.getResources(), Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true));
    }

    /**
     * 마커에 숫자 적용
     */
    public BitmapDrawable writeOnDrawable(Context context, String text, int drawableId) {
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), drawableId).copy(Bitmap.Config.ARGB_8888, true);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);

        int textSize;
        if(drawableId == R.drawable.waypoint_s) {
            paint.setColor(Color.WHITE);
            textSize = context.getResources().getDimensionPixelSize(R.dimen.entry_font);
        }else{
            textSize = context.getResources().getDimensionPixelSize(R.dimen.mission_font);
            paint.setColor(Color.BLACK);
        }


        paint.setTextSize(textSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        Canvas canvas = new Canvas(bm);
        canvas.drawText(text, bm.getWidth()/2- bounds.right/2 - (text.equals("1") ? 2 : 0), bm.getHeight()/2-bounds.top/2, paint);

        return new BitmapDrawable(context.getResources(), bm);
    }
}
