package kr.go.forest.das.Model;

public class BigdataSystemInfo {
    public String mobile_device_id;         // 전송기기 ID
    public String sn;                       // Mobile Serial Number
    public String drone_sn;                 // 드론 Serial Number
    public String imei;                     // IMEI
    public String drone_id;                 // 드론 등록정보
    public String user_id = null;           // 사용자 ID
    public boolean is_realtime = false;     // 실시간 전송여부
    public String live_url = null;          // 실시간 전송 URL

    public boolean isLogin() {
     return user_id == null ? false : true;
    }
}
