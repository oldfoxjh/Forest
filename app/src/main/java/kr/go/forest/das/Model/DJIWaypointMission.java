package kr.go.forest.das.Model;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import dji.common.gimbal.Attitude;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import kr.go.forest.das.geo.GeoManager;

public class DJIWaypointMission {
    private dji.common.mission.waypoint.WaypointMission waypoint_mission;
    public float max_flight_altitude = 0.0f;

    public DJIWaypointMission(List<GeoPoint> waypoints, GeoPoint base_point, float flight_speed){
        createDJIMission(waypoints, base_point, flight_speed);
    }

    private void createDJIMission(List<GeoPoint> waypoints, GeoPoint base_point, float flight_speed){
        dji.common.mission.waypoint.WaypointMission.Builder builder = new dji.common.mission.waypoint.WaypointMission.Builder();

        builder.autoFlightSpeed(flight_speed);
        builder.maxFlightSpeed(flight_speed);

        // 업로드에서 선택한 내용 반영
        builder.setExitMissionOnRCSignalLostEnabled(false);

        // 첫번째 웨이포인트 이동시 안전하게 이동(상승후 이동)
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);

        builder.repeatTimes(0);

        // 3D 정보 반영
        GeoManager.getInstance().getElevations(waypoints, base_point);

        List<Waypoint> _waypoint_mission = new ArrayList<>();

        for(int i = 0; i < waypoints.size(); i++)
        {
            GeoPoint _point = waypoints.get(i);
            float _altitude = (float)_point.getAltitude();
            Waypoint waypoint = new Waypoint(_point.getLatitude(), _point.getLongitude(), _altitude);

            if(i == 0) {
                waypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH, -90));
            }
            _waypoint_mission.add(waypoint);

            if(max_flight_altitude < _altitude) {
                max_flight_altitude = _altitude;
            }
        }

        builder.waypointList(_waypoint_mission).waypointCount(_waypoint_mission.size());
        waypoint_mission =  builder.build();
    }

    public dji.common.mission.waypoint.WaypointMission getDJIMission(){
        return waypoint_mission;
    }
}