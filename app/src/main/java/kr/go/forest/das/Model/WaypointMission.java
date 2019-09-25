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
    public float max_flight_altitude = 0.0f;

    public WaypointMission(List<GeoPoint> waypoints, float flight_speed){
        createMission(waypoints, flight_speed);
    }

    private void createMission(List<GeoPoint> waypoints, float flight_speed){
        dji.common.mission.waypoint.WaypointMission.Builder builder = new dji.common.mission.waypoint.WaypointMission.Builder();

        builder.autoFlightSpeed(flight_speed);
        builder.maxFlightSpeed(flight_speed);

        // 업로드에서 선택한 내용 반영
        builder.setExitMissionOnRCSignalLostEnabled(false);

        // 첫번째 웨이포인트 이동시 안전하게 이동(상승후 이동)
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);

        builder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.POINT_TO_POINT);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);

        builder.repeatTimes(0);

        // 3D 정보 반영
        GeoManager.getInstance().getElevations(waypoints);

        List<Waypoint> _waypoint_mission = new ArrayList<>();

        for(GeoPoint point : waypoints)
        {
            float _altitude = (float)point.getAltitude();
            Waypoint waypoint = new Waypoint(point.getLatitude(), point.getLongitude(), _altitude);
            //waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
            //waypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
            _waypoint_mission.add(waypoint);

            if(max_flight_altitude < _altitude) {
                max_flight_altitude = _altitude;
            }
        }

        builder.waypointList(_waypoint_mission).waypointCount(_waypoint_mission.size());
        waypoint_mission =  builder.build();
    }

    public dji.common.mission.waypoint.WaypointMission getMission(){
        return waypoint_mission;
    }
}
