package kr.go.forest.das.Model;

public class DroneInfo {
    /**
     * 비행정보
     */
    public int flight_time;                 // 드론 : 비행시간
    public int status;                      // 드론 : 상태(이벤트)

    public double drone_latitude;           // 드론 위치 : 위도
    public double drone_longitude;          // 드론 위치 : 경도
    public double drone_altitude;           // 드론 위치 : 고도

    public float drone_velocity_x;          // 드론 수평속도
    public float drone_velocity_z;          // 드론 수직속도

    /**
     * 기체정보
     */
    public double drone_pitch;              // 드론 : pitch
    public double drone_roll;               // 드론 : roll
    public double drone_yaw;                // 드론 : yaw

    public float heading;                   // 드론 : heading

    /**
     * 조종기 정보
     */
    public double rc_latitude;              // 조종기 위치 : 위도
    public double rc_longitude;             // 조종기 위치 : 경도
    public int left_stick_x;                // 조종기 왼쪽 스틱 : x
    public int left_stick_y;                // 조종기 왼쪽 스틱 : y
    public int right_stick_x;               // 조종기 오론쪽 스틱 : x
    public int right_stick_y;               // 조종기 오른쪽 스틱 : y

    /**
     * 배터리
     */
    public float battery_temperature;       // 배터리 온도
    public int battery_remain_percent;      // 배터리 남은 용량
    public int battery_voltage;             // 배터리 전압

    /**
     * Gimbal
     */
    public float gimbal_pitch;              // 짐벌 pitch
    public float gimbal_roll;               // 짐벌 roll
    public float gimbal_yaw;                // 짐벌 yaw
}
