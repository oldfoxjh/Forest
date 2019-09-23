package kr.go.forest.das.drone;

import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.base.BaseProduct;
import dji.common.camera.SettingsDefinitions;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.StorageInfo;

public abstract class Drone {

    public static final int DRONE_MANUFACTURE_DJI = 0;
    public static final int DRONE_MANUFACTURE_PIXHWAK = 0;

    public static final int DRONE_STATUS_DISCONNECT = 0x00;
    public static final int DRONE_STATUS_CONNECT = 0x01;
    public static final int DRONE_STATUS_ARMING = 0x02;
    public static final int DRONE_STATUS_FLYING = 0x04;
    public static final int DRONE_STATUS_DISARM = 0x08;
    public static final int DRONE_STATUS_RETURN_HOME = 0x10;
    public static final int DRONE_STATUS_CANCEL_RETURN_HOME = 0x11;

    public static final int DRONE_STATUS_MISSION = 0x20;

    public static final int CAMERA_ISO = 0x00;
    public static final int CAMERA_SHUTTER_SPEED = 0x01;
    public static final int CAMERA_APERTURE = 0x02;
    public static final int CAMERA_EXPOSURE = 0x03;
    public static final int CAMERA_WHITE_BALANCE = 0x04;

    public static final int CAMERA_ALL = 0xFF;

    int drone_status = DRONE_STATUS_DISCONNECT;

    /**
     * 드론 Data
     */
    double drone_latitude;
    double drone_longitude;
    float drone_altitude;

    float velocyty_x = 0.0f;
    float velocyty_y = 0.0f;
    float velocyty_z = 0.0f;

    int flight_time;

    double drone_pitch;
    double drone_roll;
    double drone_yaw;

    String flight_mode;

    double home_latitude;
    double home_longitude;
    boolean home_set = false;

    float heading;

    String seral_number = "";
    String model = "";

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
    double rc_latitude;
    double rc_longitude;
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
    Boolean camera_ae_lock;
    Boolean is_camera_auto_exposure_unlock_enabled = false;

    //region 제품정보

    /**
     * 드론 기체 및 비행정보를 반환한다.
     */
    public abstract DroneInfo getDroneInfo();

    public abstract int getDroneStatus();

    /**
     * 제조사 정보를 반환한다.
     */
    public abstract String getManufacturer();

    /**
     * 드롬 모델명을 반환한다.
     * @return
     */
    public abstract void getAircaftModel();

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
    //endregion

    //region 카메라 촬영
    /**
     * 카메라 동작 설정값을 반환한다.
     * @return
     */
    public abstract void getCameraMode();

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
    public abstract StorageInfo getStorageInfo();
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
    public abstract SettingsDefinitions.PhotoAspectRatio getPhotoAspectRatio();

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
    //endregion

    //region 임무비행
    //endregion

    //region RTL
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
