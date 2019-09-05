package kr.go.forest.das.map;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.R;
import kr.go.forest.das.geo.GeoManager;

public class MapLayer {
    private static final MapLayer ourInstance = new MapLayer();

    public static MapLayer getInstance() {
        return ourInstance;
    }

    private String loadResFromRaw(int rawId)
    {
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

    public HashMap<String, Polygon> getNoFlyZoneFromValue(String json)
    {
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
}
