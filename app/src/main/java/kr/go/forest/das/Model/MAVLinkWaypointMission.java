package kr.go.forest.das.Model;

import org.osmdroid.util.GeoPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import kr.go.forest.das.geo.GeoManager;

public class MAVLinkWaypointMission {

    private ArrayList<MavlinkMission> missions;

    public MAVLinkWaypointMission(List<GeoPoint> waypoints, GeoPoint base_point, float flight_speed){

        // 3D 정보 반영
        GeoManager.getInstance().getElevations(waypoints, base_point);

        missions = new ArrayList<>();

        // take-off
        missions.add(new MavlinkMission(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, MavCmd.MAV_CMD_NAV_TAKEOFF, 1, 0, 0, 0, 0, (float)base_point.getLatitude(), (float)base_point.getLongitude(), 1.2f));

        // flight speed
        // param1 : Speed Type 1=Ground Speed
        // param2 :
        missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_DO_CHANGE_SPEED, 1, 1, flight_speed, -1, 0, 0, 0, 0));

        // change-speed? 178

        // waypoint
        for(GeoPoint waypoint : waypoints){
            missions.add(new MavlinkMission(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, MavCmd.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 0, (float)waypoint.getLatitude(), (float)waypoint.getLongitude(), (float)waypoint.getAltitude()));
        }

        // rtl
        missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    public ArrayList<MavlinkMission> getMission(){
        return missions;
    }
}
