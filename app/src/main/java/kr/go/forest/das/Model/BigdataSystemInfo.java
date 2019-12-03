package kr.go.forest.das.Model;

import android.location.Location;
import android.util.Log;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import kr.go.forest.das.geo.GeoManager;

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

    public GeoPoint my_location = null;                                // 현재 조종자 위치

    public ArrayList<LoginResponse.DroneList> drone_list;
    public ArrayList<FlightPlan> flight_plan;
    public ArrayList<FireInfo> fireInfoList;
    public ArrayList<WeatherInfo> weatherInfoList;

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

    public void setFireInfo(List<FireInfo> fireInfo){
        fireInfoList = new ArrayList<>();
        if(fireInfo != null) fireInfoList.addAll(fireInfo);
    }

    public void setWeatherInfo(List<WeatherInfo> weatherInfo){
        weatherInfoList = new ArrayList<>();
        if(weatherInfo != null) weatherInfoList.addAll(weatherInfo);
    }

    public WeatherInfo getWeatherInfo(double latitude, double longitude){
        double _distance = 1000*1000;
        WeatherInfo _result = null;

        for(WeatherInfo info : weatherInfoList){
            double _temp = GeoManager.getInstance().distance(info.wtherObsrrLctnYcrd, info.wtherObsrrLctnXcrd, latitude, longitude);

            if(_temp > 1 &&  _temp < _distance) {
                _distance = _temp;
                _result = info;
                Log.e("distance", "지역 : " + _result.wtherObsrrNm + " distance : " + _temp);
            }
        }

        return _result;
    }

    public void setMyLocation(Location location){
        if(my_location == null){
            my_location = new GeoPoint(location.getLatitude(), location.getLongitude());
        }
    }
}
