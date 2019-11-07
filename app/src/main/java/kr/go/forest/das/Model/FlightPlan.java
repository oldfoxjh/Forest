package kr.go.forest.das.Model;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class FlightPlan {
    public String key;
    public String title;
    public int angle;
    public int altitude;
    public float speed;
    public int front_lap;
    public int side_lap;
    public List<BigdataPoint> points;
}
