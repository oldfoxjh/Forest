package kr.go.forest.das.Model;

import android.os.Environment;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import kr.go.forest.das.geo.GeoManager;

public class WaypointMission {
    private dji.common.mission.waypoint.WaypointMission waypoint_mission;
    private int waypoint_count = 0;

    public WaypointMission(List<GeoPoint> waypoints, float flight_speed){
        createMission(waypoints, flight_speed);
    }

    private void createMission(List<GeoPoint> waypoints, float flight_speed){
        dji.common.mission.waypoint.WaypointMission.Builder builder = new dji.common.mission.waypoint.WaypointMission.Builder();

        builder.autoFlightSpeed(flight_speed);
        builder.maxFlightSpeed(flight_speed);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.POINT_TO_POINT);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(0);

        // 3D 정보 반영
        GeoManager.getInstance().getElevations(waypoints);

        waypoint_count = waypoints.size();
        //builder.waypointList(waypoints).waypointCount(waypoint_count);
        //waypoint_mission =  builder.build();
    }
}
