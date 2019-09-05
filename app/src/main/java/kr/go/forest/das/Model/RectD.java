package kr.go.forest.das.Model;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class RectD {
    private double left = Double.MAX_VALUE;
    private double right = -Double.MAX_VALUE;
    private double top = Double.MAX_VALUE;
    private double bottom = -Double.MAX_VALUE;

    public RectD(List<GeoPoint> points)
    {
        for(int i = 0; i < points.size(); i++)
        {
            GeoPoint _point = points.get(i);

            left = (left > _point.getLongitude()) ?  _point.getLongitude() : left;
            right = (right > _point.getLongitude()) ? right : _point.getLongitude();
            top = (top > _point.getLatitude()) ? _point.getLatitude() : top;
            bottom = (bottom > _point.getLatitude()) ? bottom : _point.getLatitude();
        }
    }

    public List<GeoPoint> getPoints()
    {
        List<GeoPoint> _points = new ArrayList<GeoPoint>();

        GeoPoint _left_top = new GeoPoint(top, left); _points.add(_left_top);
        GeoPoint _right_top = new GeoPoint(top, right); _points.add(_right_top);
        GeoPoint _right_bottom = new GeoPoint(bottom, right); _points.add(_right_bottom);
        GeoPoint _left_bottom = new GeoPoint(bottom, left); _points.add(_left_bottom);

        return _points;
    }
}
