package kr.go.forest.das.Model;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class FlightPlan {
    public String id;
    public String name;
    public int angle;
    public int altitude;
    public float speed;
    public int front_lap;
    public int side_lap;
    public List<BigdataPoint> points;
}
