package kr.go.forest.das.Model;

import java.util.List;

public class LoginResponse {
    //
    public int result;
    public String live_url;                 // 실시간 동영상 URL
    public String aes_key;                  // AES 정보

    public String name;
    public String service;
    public String department;

    // 기체정보 리스트
    public List<DroneList> drone_list;
    // 비행계획 리스트
    public List<FlightPlan> plan;
    // 산불 발생지역
    public List<FireInfo> fireInfoList;
    // 기상정보
    public List<WeatherInfo> weatherInfoList;

    public class DroneList{
        public String manage_number;
        public String department;
        public String model;
        public String manufacturer;
        public String kind;
        public String camera;
    }
}


