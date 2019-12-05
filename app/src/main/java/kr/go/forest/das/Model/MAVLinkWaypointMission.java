package kr.go.forest.das.Model;

import org.osmdroid.util.GeoPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import kr.go.forest.das.drone.Px4;
import kr.go.forest.das.geo.GeoManager;

public class MAVLinkWaypointMission {

    private ArrayList<MavlinkMission> missions;

    public MAVLinkWaypointMission(List<GeoPoint> waypoints, GeoPoint base_point, float flight_speed, int interval, int device){

        // 3D 정보 반영
        GeoManager.getInstance().getElevations(waypoints, base_point);

        missions = new ArrayList<>();

        if(device == Px4.MAVLINK_DEVICE_ARDUPILOT) missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_DO_SET_HOME , 0, 0, 0, 0, 0, (float)base_point.getLatitude(), (float)base_point.getLongitude(), 0));

        // take-off
        missions.add(new MavlinkMission(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, MavCmd.MAV_CMD_NAV_TAKEOFF, 1, 15, 0, 0, 0, 0, 0, (float)waypoints.get(0).getAltitude()));

        // flight speed
        // param1 : Speed Type 1=Ground Speed
        // param2 :
        missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_DO_CHANGE_SPEED, 0, 1, flight_speed, -1, 0, 0, 0, 0));

        // 첫번째 base 위치의 비행고도까지 올리기
   //     missions.add(new MavlinkMission(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, MavCmd.MAV_CMD_NAV_TAKEOFF, 0, 0, 0, 0, 0, (float)base_point.getLatitude(), (float)base_point.getLongitude(), (float)waypoints.get(0).getAltitude()));

        // 첫번째 base 위치의 비행고도까지 올린후 카메라 모드 설정
    //    missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_SET_CAMERA_MODE , 0, 0, 0, Float.NaN, Float.NaN, 0, 0, 0));

        // 인터벌 모드로 카메라 촬영
    //    missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_IMAGE_START_CAPTURE  , 0, 0, interval, 0, Float.NaN, 0, 0, 0));

        // waypoint
       for(GeoPoint waypoint : waypoints){
            missions.add(new MavlinkMission(MavFrame.MAV_FRAME_GLOBAL_RELATIVE_ALT, MavCmd.MAV_CMD_NAV_WAYPOINT, 0, 0, 0, 0, 0, (float)waypoint.getLatitude(), (float)waypoint.getLongitude(), (float)waypoint.getAltitude()));
        }

        // 카메라 셔터 통합 시간 설정
  //      missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_DO_SET_CAM_TRIGG_DIST  , 0, 0, 0, 0, 0, 0, 0, 0));

        // 카메라 촬영 종료
  //      missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_IMAGE_STOP_CAPTURE   , 0, 0, Float.NaN, Float.NaN, 0, 0, 0, 0));

        // rtl
        missions.add(new MavlinkMission(MavFrame.MAV_FRAME_MISSION, MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    public ArrayList<MavlinkMission> getMission(){
        return missions;
    }
}
