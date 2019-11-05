/**
 Class Name : Drone.java
 Description : 드론 정보 관리 추상 클래스
 드론 앱서비스
 수정일     수정자     수정내용
 ------- -------- ---------------------------
 2019.09.01 정호   최초 생성

 author : 이노드(연구소) 정호
 since : 2019.09.01
 */
package kr.go.forest.das.drone;

import android.content.Context;

import org.osmdroid.util.GeoPoint;

import java.util.List;

import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.common.camera.SettingsDefinitions;
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DroneInfo;

public abstract class Drone {

    public static final int DRONE_MANUFACTURE_DJI = 0;
    public static final int DRONE_MANUFACTURE_PIXHWAK = 1;

    public static final int DRONE_STATUS_DISCONNECT = 0x00;
    public static final int DRONE_STATUS_CONNECT = 0x01;
    public static final int DRONE_STATUS_ARMING = 0x02;
    public static final int DRONE_STATUS_FLYING = 0x04;
    public static final int DRONE_STATUS_RETURN_HOME = 0x08;
    public static final int DRONE_STATUS_CANCEL_RETURN_HOME = 0x10;
    public static final int DRONE_STATUS_MISSION = 0x20;
    public static final int DRONE_STATUS_DISARM = 0x40;

    int drone_status = DRONE_STATUS_DISCONNECT;
    boolean is_flying = false;
    int ready_start_mission = -1;
    SettingsDefinitions.ShootPhotoMode shoot_photo_mode = SettingsDefinitions.ShootPhotoMode.UNKNOWN;
    /**
     * 드론 설정 값
     */
    public int max_flight_height = 0;
    public ConnectionFailSafeBehavior connection_failsafe_behavior = ConnectionFailSafeBehavior.UNKNOWN;

    double drone_latitude;          /** 드론 위도 */
    double drone_longitude;         /** 드론 경도 */
    float drone_altitude;           /** 드론 고도 */
    float velocyty_x = 0.0f;        /** 드론 x축 속도 */
    float velocyty_y = 0.0f;        /** 드론 y축 속도 */
    float velocyty_z = 0.0f;        /** 드론 z축 속도 */

    int flight_time;                /** 비행시간 */

    double drone_pitch;             /** 드론 pitch */
    double drone_roll;              /** 드론 roll */
    double drone_yaw;               /** 드론 yaw */

    String flight_mode;             /** 비행 모드 */

    double home_latitude;           /** 자동복귀지점 위도 */
    double home_longitude;          /** 자동복귀지점 경도 */
    boolean home_set = false;       /** 자동복귀지점 설정여부 */

    float heading;
    Model model = Model.UNKNOWN_AIRCRAFT;

    /**
     * Gimbal Data
     */
    float gimbal_pitch;
    float gimbal_roll;
    float gimbal_yaw;

    /**
     * 배터리 Data
     */
    float battery_temperature;
    int battery_voltage;
    int battery_remain_percent;

    /**
     * 조종기 Data
     */
    int left_stick_x;
    int left_stick_y;
    int right_stick_x;
    int right_stick_y;

    /**
     * 드론 저장장치 정보
     */
    boolean inserted;
    int remain_storage;
    String capture_count;
    String recording_time;
    String recording_remain_time;
    String photo_file_format;
    String video_resolution_framerate;

    /**
     * 드론 카메라 데이터
     */
    String camera_aperture;
    String camera_shutter;
    String camera_iso;
    String camera_exposure;
    String camera_whitebalance;
    float camera_aspect_ratio;
    float cmos_factor;                      // sensor_width / focal_length
    Boolean camera_ae_lock;
    Boolean is_camera_auto_exposure_unlock_enabled = false;

    /**
     * 임무 데이터
     */
    int waypoint_count = 0;
    int interval = 0;
    int shoot_count = 0;
    List<GeoPoint> flight_points = null;          // 비행경로를 보여주기 위한 위치

    //region 제품정보
    /**
     * 드론 기체 및 비행정보를 반환한다.
     * @return 드론기체 및 비행정보
     */
    public abstract DroneInfo getDroneInfo();
    /**
     * 현재 드론 상태를 반환한다.
     * @return 드론 상태
     */
    public abstract int getDroneStatus();
    /**
     * 현재 드론 비행여부를 확인한다.
     * @return 비행여부
     */
    public abstract boolean isFlying();

    /**
     * 제조사 정보를 반환한다.
     */
    public abstract String getManufacturer();

    /**
     * 드롬 모델명을 반환한다.
     * @return
     */
    public abstract Model getAircaftModel();

    /**
     * 드론 시리얼번호를 반환한다.
     * @return
     */
    public abstract void getSerialNumber();

    /**
     * 드론의 구성정보를 반환한다.
     */
    public abstract BaseProduct getProductInstance();

    /**
     * 드론정보 수집을위한 Listener 설정한다.
     */
    public abstract boolean setDroneDataListener();

    /**
     * 드론정보 수집을위한 Listener를 해제한다.
     */
    public abstract boolean removeDroneDataListener();
    //endregion

    //region 카메라 촬영
    /**
     * 카메라 동작 설정값을 반환한다.
     * @return
     */
    public abstract void getCameraMode();

    /**
     * 카메라 포커스 길이를 설정한다.
     */
    public abstract void getCameraFocalLength();

    /**
     * 카메라 동작을 설정한다.
     * @param modeType : SHOOT_PHOTO, RECORD_VIDEO
     */
    public abstract void setCameraMode(SettingsDefinitions.CameraMode modeType);

    /**
     * 비디오 촬영을 시작한다.
     */
    public abstract void startRecordVideo();

    /**
     * 비디오 촬영을 종료한다.
     */
    public abstract void stopRecordVideo();

    /**
     * 한장 또는 카메라 촬영 설정에 따라 촬영을 시작한다.
     */
    public abstract void startShootPhoto();

    /**
     * 사진 촬영을 멈춘다.
     */
    public abstract void stopShootPhoto();

    /**
     * 카메라 동작 설정값을 반환한다.
     */
    public abstract CameraInfo getStorageInfo();
    //endregion

    //region 카메라 정보
    /**
     * 카메라 연결 여부를 확인한다.
     * @return 카메라 연결 true, false
     */
    public abstract boolean isCameraConnected();

    /**
     * 카메라 타입을 반환한다.
     */
    public abstract String getCameraDisplayName();

    /**
     * 카메라 촬영 모드를 반환한다.
     * @return SettingsDefinitions.ShootPhotoMode
     */
    public abstract SettingsDefinitions.ShootPhotoMode getShootPhotoMode();

    /**
     * 카메라 노출 설정값을 반환한다.
     * @return  	AperturePriority, Manual, Program. Shutter, Unknown
     */
    public abstract SettingsDefinitions.ExposureMode getExposureMode();

    /**
     * 카메라 자동노출 정보를 확인한다.
     */
    public abstract void getAutoAEUnlockEnabled();
    /**
     * 카메라 노출값을 설정한다.
     */
    public abstract void setExposureMode(SettingsDefinitions.ExposureMode modeType);

    /**
     * 카메라 ISO 설정값을 반환한다.
     * @return Auto, 100, 200, 400, 800, 1600, 3200...
     */
    public abstract void getISO();

    /**
     * 카메라 ISO값을 설정한다.
     * @param isoType
     */
    public abstract void setISO(SettingsDefinitions.ISO isoType);

    /**
     * 카메라의 셔터 속도값을 반환한다.
     * @return 1/2, 1/3/ 1/4, 1/5...
     */
    public abstract void getShutterSpeed();

    /**
     * 셔터 속도값을 설정한다.
     * @param shutterSpeedType
     */
    public abstract void setShutterSpeed(SettingsDefinitions.ShutterSpeed shutterSpeedType);

    /**
     * 노출 설정을 지원하는지 체크 X5, X5R 지원
     * @return 지원여부 true, false
     */
    public abstract boolean isAdjustableApertureSupported();

    /**
     * 조리개 설정값을 반환한다.
     * @return
     */
    public abstract void getAperture();

    /**
     * 조리개값을 설정한다.
     */
    public abstract void setAperture(SettingsDefinitions.Aperture apertureType);

    /**
     * 카메라 노출 보정값을 반환한다.
     * @return 0.0ev, -0.3ev, -0.7ev....
     */
    public abstract void getExposureCompensation();

    /**
     * 카메라 노출 보정값을 설정한다.
     */
    public abstract void setExposureCompensation(SettingsDefinitions.ExposureCompensation compensationType);

    /**
     * 자동 노출 설정값을 반환한다.
     * @return
     */
    public abstract boolean getAELock();

    /**
     * 자동 노출값을 설정한다.
     * @param isLocked
     */
    public abstract void setAELock(boolean isLocked);

    /**
     * 카메라 화이트밸런스 설정값을 반환한다.
     * @return Auto, Cloudy, CustomColorTemperature, Sunny...
     */
    public abstract void getWhiteBalance();

    /**
     * 카메라 화이트밸런스를 설정한다.
     * @param whiteBalancePresetType
     */
    public abstract void setWhiteBalance(SettingsDefinitions.WhiteBalancePreset whiteBalancePresetType);

    /**
     * 사진의 종횡비를 반환한다.
     * @return 16:9, 4:3, Unknown
     */
    public abstract void getPhotoAspectRatio();

    /**
     * 사진의 종횡비값을 설정한다.
     * @param photoAspectRatioType
     */
    public abstract void setPhotoAspectRatio(SettingsDefinitions.PhotoAspectRatio photoAspectRatioType);
    //endregion

    //region 드론 비행 정보

    /**
     * 자동이륙 고도
     */
    public abstract String getTakeoffAltitude();

    /**
     * 조종기와 연결이 끊어졌을 때의 동작을 설정한다.
     * @param behavior : 연결이 끊어졌을 때의 행동
     */
    public abstract void setConnectionFailSafeBehavior(ConnectionFailSafeBehavior behavior);

    /**
     * 자동이륙 명령
     */
    public abstract void startTakeoff();

    /**
     * 자동착륙 명령
     */
    public abstract void startLanding();

    /**
     * 자동착륙 명령 취소
     */
    public abstract void cancelLanding();

    /**
     * 드론 수평방향 속도값을 가져온다.
     */
    public abstract float getHorizontalVelocity();

    /**
     * 드론 수직방향 속도값을 가져온다.
     */
    public abstract float getVerticalVelocity();

    /**
     * 드론 비행시간값을 가져온다.
     * @return
     */
    public abstract int getFlightTimeInSeconds();

    /**
     * 드론 비행설정값을 가져온다.
     * @return
     */
    public abstract FlightMode getFlightMode();

    /**
     * GPS 신호강도값을 가져온다.
     * @return
     */
    public abstract GPSSignalLevel getGPSSignalLevel();

    /**
     * 배터리 남은 용량상태를 가져온다.
     * @return
     */
    public abstract BatteryThresholdBehavior getBatteryThresholdBehavior();
    //endregion

    //region 드론 기체 정보
    public abstract int getRemainingFlightTime();
    public abstract ConnectionFailSafeBehavior getConnectionFailSafeBehavior();
    //endregion

    //region 임무비행
    /**
     * 임무를 업로드 가능한지 체크
     */
    public abstract boolean isMissionUploadAvailable();

    /**
     * 임무 시작이 가능한지 체크
     */
    public abstract boolean isMissionStartAvailable();

    /**
     * 설정된 임무를 드론에 업로드
     */
    public abstract String uploadMission(WaypointMission mission);

    /**
     * 설정된 임무를 시작하기 위한 조건 설정
     */
    public abstract void setMissionCondition(int captureCount, int timeIntervalInSeconds);

    /**
     * 설정된 임무를 시작
     */
    public abstract void startMission(int _shoot_count, int _interval);

    /**
     * 설정된 임무를 멈춤
     */
    public abstract void stopMission();

    /**
     * 드론 최대비행고도를 설정한다.
     */
    public abstract void setMaxFlightHeight(int height);

    /**
     * 임무비행 경로 정보를 반환한다.
     * @return 임무비행 경로
     */
    public abstract List<GeoPoint> getMissionPoints();

    /**
     * 임무비행 경로를 설정한다.
     * @param points 임무비행 경로
     */
    public abstract void setMissionPoints(List<GeoPoint> points);
    //endregion

    //region 자동복귀
    public abstract boolean isHomeLocationSet();
    public abstract LocationCoordinate2D getHomeLocation();
    public abstract GoHomeExecutionState getGoHomeExecutionState();
    public abstract void startGoHome();
    public abstract void cancelGoHome();
    public abstract void setHomeLocation(LocationCoordinate2D home);
    //endregion

    //region 조종기

    //endregion

    //region 짐벌
    public abstract void setGimbalRotate(float pitch);
    //endregion
}
