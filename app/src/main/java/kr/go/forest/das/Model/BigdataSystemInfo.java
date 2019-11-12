package kr.go.forest.das.Model;

import java.util.ArrayList;
import java.util.List;

public class BigdataSystemInfo {
    public String mobile_device_id;         // 전송기기 ID
    public String sn;                       // Mobile Serial Number
    public String imei;                     // IMEI
    public String phone_number;             // 전화번호
    public String drone_id;                 // 드론 등록정보
    public String user_id = null;           // 사용자 ID
    public boolean is_realtime = false;     // 실시간 전송여부
    public String live_url = null;          // 실시간 전송 URL
    public String aes_key;                  // 암호화 정보

    public String name;                     // 사용자 이름
    public String service;                  // 사용자 소속
    public String department;               // 사용자 부서
    public int drone_index = 0;             // 현재 사용중인 드론 인덱스

    public WeatherInfo weather = new WeatherInfo(); // 기상정보

    public List<LoginResponse.DroneList> drone_list;
    public List<FlightPlan> flight_plan;

    public boolean isLogin() {
     return user_id == null ? false : true;
    }

    public void setDroneInfo(List<LoginResponse.DroneList> list){
        drone_list = new ArrayList<>();
        drone_list.addAll(list);
    }

    public void setFlightPlan(List<FlightPlan> plan){
        flight_plan = new ArrayList<>();
        flight_plan.addAll(plan);
    }
}
