package kr.go.forest.das.Model;

public class BigdataSystemInfo {
    public String mobile_device_id;         // 전송기기 ID
    public String sn;                       // SerialNumber
    public String imei;                     // IMEI
    public String drone_id;                 // 드론 등록정보
    public String user_id;                  // 사용자 ID

    public boolean isLogin() {
     return user_id == null ? false : true;
    }
}
