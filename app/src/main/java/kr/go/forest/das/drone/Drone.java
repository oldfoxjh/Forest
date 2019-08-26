package kr.go.forest.das.drone;

import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.gimbal.GimbalState;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.base.BaseProduct;
import dji.common.camera.SettingsDefinitions;

public abstract class Drone {

    public static final int DRONE_MANUFACTURE_DJI = 0;
    public static final int DRONE_MANUFACTURE_PIXHWAK = 0;

    //region 제품정보
    /**
     * 제조사 정보를 반환한다.
     */
    public abstract String getManufacturer();

    /**
     * 드롬 모델명을 반환한다.
     * @return
     */
    public abstract String getAircaftModel();

    /**
     * 드론 시리얼번호를 반환한다.
     * @return
     */
    public abstract String getSerialNumber();

    /**
     * 드론의 구성정보를 반환한다.
     */
    public abstract BaseProduct getProductInstance();
    //endregion

    //region 카메라 촬영
    /**
     * 카메라 동작 설정값을 반환한다.
     * @return
     */
    public abstract SettingsDefinitions.CameraMode getCameraMode();

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
     * 카메라 노출값을 설정한다.
     */
    public abstract void setExposureMode(SettingsDefinitions.ExposureMode modeType);

    /**
     * 카메라 ISO 설정값을 반환한다.
     * @return Auto, 100, 200, 400, 800, 1600, 3200...
     */
    public abstract SettingsDefinitions.ISO getISO();

    /**
     * 카메라 ISO값을 설정한다.
     * @param isoType
     */
    public abstract void setISO(SettingsDefinitions.ISO isoType);

    /**
     * 카메라의 셔터 속도값을 반환한다.
     * @return 1/2, 1/3/ 1/4, 1/5...
     */
    public abstract SettingsDefinitions.ShutterSpeed getShutterSpeed();

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
    public abstract SettingsDefinitions.Aperture getAperture();

    /**
     * 조리개값을 설정한다.
     */
    public abstract void setAperture(SettingsDefinitions.Aperture apertureType);

    /**
     * 카메라 노출 보정값을 반환한다.
     * @return 0.0ev, -0.3ev, -0.7ev....
     */
    public abstract SettingsDefinitions.ExposureCompensation getExposureCompensation();

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
    public abstract SettingsDefinitions.WhiteBalancePreset getWhiteBalance();

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
     * 드론의 위도,경도,고도값을 반환한다.
     * @return
     */
    public abstract LocationCoordinate3D getAircraftLocation();

    /**
     * 드론 수평방향 속도값을 가져온다.
     * @return
     */
    public abstract float getVelocityX();

    /**
     * 드론 수직방향 속도값을 가져온다.
     * @return
     */
    public abstract float getVelocityZ();

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
    public abstract void setHomeLocation();
    //endregion

    //region 조종기

    //endregion

    //region 짐벌
    public abstract void setGimbalStateCallback(GimbalState.Callback callback);
    public abstract void setGimbalRotate(float yaw, float pitch, float roll);
    //endregion
}
