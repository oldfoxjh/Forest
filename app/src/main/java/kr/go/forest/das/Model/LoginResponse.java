package kr.go.forest.das.Model;

import java.util.List;

public class LoginResponse {
    //
    public int result;

    // 기체정보
    public List<DroneList> drone_list;
    // 비행계획 리스트
    public List<FlightPlan> plan;
    // 드론 리스트
    // 기상정보
    public WeatherInfo weather;

    public class DroneList{
        public String manage_number;
        public String department;
        public String model;
        public String manufacturer;
        public String kind;
        public String camera;
    }
}


