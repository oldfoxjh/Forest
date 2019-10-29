package kr.go.forest.das.Model;

import java.util.List;

public class DroneInfoRequest {
    public String mobile_device_id;         // 전송기기 ID
    public String sn;                       // SerialNumber
    public String imei;                     // IMEI
    public String time;                     // 전송시간
    public String drone_id;                 // 드론 등록정보
    public String user_id;                  // 사용자 ID

    public List<DroneInfo> drone_infos;     // 드론 위치 및 상태 정보

    public DroneInfoRequest(BigdataSystemInfo system_info, List<DroneInfo> drone_infos){
        mobile_device_id = system_info.mobile_device_id;
        sn = system_info.sn;
        imei = system_info.imei;
        drone_id = system_info.drone_id;
        user_id = system_info.user_id;
    }
}
