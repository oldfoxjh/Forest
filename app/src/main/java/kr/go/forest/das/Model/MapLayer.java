package kr.go.forest.das.Model;

import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polygon;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import kr.go.forest.das.geo.GeoManager;

public class MapLayer {
    public HashMap<String, Polygon> getNoFlyZoneFromValue(String json)
    {
       try
       {
           String test = "{\n" +
                   "\t\"NoFlyZone\": [{\n" +
                   "\t\t\"name\": \"P73B\",\n" +
                   "\t\t\"points\": [\n" +
                   "\t\t\t[14125285.54764267, 4519349.98953157],\n" +
                   "\t\t\t[14125205.28628981, 4521553.58628233],\n" +
                   "\t\t\t[14125583.10464156, 4523726.2429858],\n" +
                   "\t\t\t[14126402.75005227, 4525772.86477636],\n" +
                   "\t\t\t[14127628.65594463, 4527603.83768809],\n" +
                   "\t\t\t[14129207.27764358, 4529138.84422547],\n" +
                   "\t\t\t[14131069.65272454, 4530310.61674571],\n" +
                   "\t\t\t[14133134.23966053, 4531067.5691736],\n" +
                   "\t\t\t[14135310.59136529, 4531376.49633589],\n" +
                   "\t\t\t[14137503.36269494, 4531223.85958359],\n" +
                   "\t\t\t[14139616.37360944, 4530616.2875945],\n" +
                   "\t\t\t[14141557.11761192, 4529580.50175582],\n" +
                   "\t\t\t[14143240.54661144, 4528161.96383399],\n" +
                   "\t\t\t[14144593.02276483, 4526423.09257381],\n" +
                   "\t\t\t[14145555.54674198, 4524440.06994107],\n" +
                   "\t\t\t[14146086.26241433, 4522299.71875375],\n" +
                   "\t\t\t[14146162.12664731, 4520095.82432244],\n" +
                   "\t\t\t[14145780.1894744, 4517924.6940737],\n" +
                   "\t\t\t[14144957.3714582, 4515881.15202151],\n" +
                   "\t\t\t[14143729.85143322, 4514054.1956715],\n" +
                   "\t\t\t[14143636.06476223, 4513943.57487898],\n" +
                   "\t\t\t[14141595.18887777, 4514801.47816781],\n" +
                   "\t\t\t[14139585.26604564, 4514606.50599617],\n" +
                   "\t\t\t[14138874.03958507, 4513202.67031157],\n" +
                   "\t\t\t[14137884.57629116, 4512422.86803408],\n" +
                   "\t\t\t[14136060.16115655, 4513904.54904074],\n" +
                   "\t\t\t[14134699.55868033, 4514021.55680087],\n" +
                   "\t\t\t[14133339.01186386, 4512071.97244559],\n" +
                   "\t\t\t[14132720.57643275, 4512305.9483178],\n" +
                   "\t\t\t[14131019.83725242, 4514177.52238737],\n" +
                   "\t\t\t[14129628.34361749, 4515035.51504817],\n" +
                   "\t\t\t[14127216.43987022, 4515503.46410656],\n" +
                   "\t\t\t[14125793.99941687, 4517219.66699262],\n" +
                   "\t\t\t[14125285.54764267, 4519349.98953157]\n" +
                   "\t\t]\n" +
                   "\t}]\n" +
                   "}";

           JSONObject _data = new JSONObject(test);
           JSONArray _noFlyZone = new JSONArray(_data.getString("NoFlyZone"));

           HashMap<String, Polygon> _result = new HashMap<String, Polygon>();

           for(int i = 0; i < _noFlyZone.length(); i++)
           {
               Polygon _zone = new Polygon();
               _zone.setFillColor(Color.argb(60, 255, 0, 0));
               _zone.setStrokeWidth(1.0f);

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

        return  null;
    }
}
