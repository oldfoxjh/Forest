package kr.go.forest.das.Model;

import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;

public class MavlinkMission {
    public MavFrame frame;              // 웨이포인트의 좌표 시스템, 	MAV_FRAME_MISSION : 좌표가 아닌 임무 명령, MAV_FRAME_GLOBAL_RELATIVE_ALT : 글로벌 좌표 및 상대 고도
    public MavCmd command;              // 웨이포인트 명령 : MAV_CMD_NAV_TAKEOFF, MAV_CMD_NAV_LAND, MAV_CMD_NAV_RETURN_TO_LAUNCH
                                        // 카메라 명령 : MAV_CMD_IMAGE_START_CAPTURE, MAV_CMD_IMAGE_STOP_CAPTURE, MAV_CMD_VIDEO_START_CAPTURE, MAV_CMD_VIDEO_STOP_CAPTURE
    public int current;                 // ?? 0 or 1
    public int auto_continue;           // ??
    public float param1;
    public float param2;
    public float param3;
    public float param4;
    public float x;                    // latitude
    public float y;                    // longitude
    public float z;                    // altitude

    public MavlinkMission( MavFrame frame, MavCmd command, int current, float param1, float param2, float param3, float param4, float x, float y, float z){
        this.frame = frame;
        this.command = command;
        this.current = current;
        this.x = x;
        this.y = y;
        this.z = z;
        auto_continue = 1;

        this.param1 = param1;
        this.param2 = param2;
        this.param3 = param3;
        this.param4 = param4;
    }

    public String toString(){
        return "Mavlink Mission x : " + x;
    }
}
