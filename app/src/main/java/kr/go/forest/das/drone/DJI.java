package kr.go.forest.das.drone;

import android.support.annotation.NonNull;
import java.util.List;

import dji.common.battery.BatteryState;
import dji.common.camera.ExposureSettings;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.StorageState;
import dji.common.camera.SystemState;
import dji.common.camera.WhiteBalance;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.gimbal.GimbalState;
import dji.common.remotecontroller.GPSData;
import 	dji.common.remotecontroller.HardwareState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;

import static dji.common.camera.SettingsDefinitions.ShutterSpeed;

public class DJI extends Drone{

    private static final String TAG = "DJI Drone";

    private FlightController flight_controller = null;
    private RemoteController remote_controller = null;
    private List<Camera> cameras = null;
    private List<Gimbal> gimbals = null;
    private List<Battery> batteries = null;

    private SettingsDefinitions.CameraMode camera_mode;


    //region 제품정보
    public synchronized DroneInfo getDroneInfo(){
        DroneInfo _drone = new DroneInfo();

        _drone.flight_time = flight_time;
        _drone.drone_latitude = drone_latitude;
        _drone.drone_longitude = drone_longitude;
        _drone.drone_altitude = drone_altitude;
        _drone.drone_velocity_x = (float) Math.sqrt(velocyty_x*velocyty_x + velocyty_y*velocyty_y);
        _drone.drone_velocity_z = velocyty_z;
        _drone.drone_pitch = drone_pitch;
        _drone.drone_roll = drone_pitch;
        _drone.drone_yaw = drone_pitch;
        _drone.heading = heading;

        _drone.rc_latitude = rc_latitude;
        _drone.rc_longitude = rc_longitude;
        _drone.left_stick_x = left_stick_x;
        _drone.left_stick_y = left_stick_y;
        _drone.right_stick_x = right_stick_x;
        _drone.right_stick_y = right_stick_y;

        _drone.battery_temperature = battery_temperature;
        _drone.battery_remain_percent = battery_remain_percent;
        _drone.battery_voltage = battery_voltage;

        _drone.gimbal_pitch = gimbal_pitch;
        _drone.gimbal_roll = gimbal_pitch;
        _drone.gimbal_yaw = gimbal_yaw;

        return _drone;
    }

    public int getDroneStatus(){
        return drone_status;
    };

    /**
     * 제조사 정보를 반환한다.
     */
    @Override
    public String getManufacturer() {
        return "DJI";
    }

    /**
     * 드롬 모델명을 반환한다.
     * @return
     */
    @Override
    public void getAircaftModel(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            if (_product instanceof Aircraft) {
                _product.getName(new CommonCallbacks.CompletionCallbackWith<String>() {
                    @Override
                    public void onSuccess(String s) {
                        model = s;
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        // Error
                    }
                });
            }
        }
    }

    /**
     * 드론 시리얼번호를 반환한다.
     * @return
     */
    @Override
    public void getSerialNumber(){
        if(flight_controller != null)
        {
            flight_controller.getSerialNumber(new CommonCallbacks.CompletionCallbackWith<String>() {
                @Override
                public void onSuccess(String s) {
                    seral_number = s;
                }

                @Override
                public void onFailure(DJIError djiError) {
                    // Error
                }
            });
        }
    }

    /**
     * 드론의 구성정보를 반환한다.
     */
    @Override
    public synchronized BaseProduct getProductInstance() {
        return DJISDKManager.getInstance().getProduct();
    }

    @Override
    /**
     * 드론의 FlightController 정보를 설정한다.
     */
    public boolean setDroneDataListener()
    {
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            if (_product instanceof Aircraft) {
                // 드론 정보 callback
                flight_controller = ((Aircraft) _product).getFlightController();
                flight_controller.setStateCallback(status_callback);

                // 조종기 callback
                remote_controller = ((Aircraft) _product).getRemoteController();
                remote_controller.setHardwareStateCallback(rc_hardware_callback);
                remote_controller.setGPSDataCallback(rc_gps_callback);

                // 짐벌 callback
                gimbals = ((Aircraft) _product).getGimbals();
                for (Gimbal gimbal:gimbals) {
                    gimbal.setStateCallback(gimbal_callback);
                }

                // 배터리 callback
                batteries = _product.getBatteries();
                for (Battery battery:batteries) {
                    battery.setStateCallback(battery_callback);
                }

                // 카메라 callback
                cameras = ((Aircraft) _product).getCameras();
                for (Camera camera:cameras) {
                    camera.setSystemStateCallback(camera_state_callback);
                    camera.setExposureSettingsCallback(camera_setting_callback);
                    camera.setStorageStateCallBack(camera_storage_callback);
                }
                return true;
            }
        }

        flight_controller = null;
        return false;
    }
    //endregion

    //region 카메라 촬영
    /**
     * 카메라 동작 설정값을 반환한다.
     * @return
     */
    @Override
    public void getCameraMode(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.CameraMode>() {
                        @Override
                        public void onSuccess(SettingsDefinitions.CameraMode mode) {
                            MainActivity.DroneCameraStatus _status = new MainActivity.DroneCameraStatus();
                            _status.setMode((SettingsDefinitions.CameraMode.SHOOT_PHOTO == mode) ? 0 : 1);
                            camera_mode = mode;
                            DroneApplication.getEventBus().post(_status);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "getCameraMode : " + djiError);
                        }
                    });
        }
    }

    /**
     * 카메라 동작을 설정한다.
     * @param mode : SHOOT_PHOTO, RECORD_VIDEO
     */
    @Override
    public void setCameraMode(SettingsDefinitions.CameraMode mode){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            camera_mode = mode;
            _product.getCamera()
                    .setMode(mode, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            MainActivity.DroneCameraStatus _status = new MainActivity.DroneCameraStatus();
                            _status.setMode((SettingsDefinitions.CameraMode.SHOOT_PHOTO == camera_mode) ? 0 : 1);
                            DroneApplication.getEventBus().post(_status);
                        }
                    });
        }
    }

    /**
     * 비디오 촬영을 시작한다.
     */
    @Override
    public void startRecordVideo(){

    }

    /**
     * 비디오 촬영을 종료한다.
     */
    @Override
    public void stopRecordVideo(){

    }

    /**
     * 한장 또는 카메라 촬영 설정에 따라 촬영을 시작한다.
     */
    @Override
    public void startShootPhoto(){

    }

    /**
     * 사진 촬영을 멈춘다.
     */
    @Override
    public void stopShootPhoto(){

    }
    //endregion

    //region 카메라 정보
    /**
     * 카메라 연결 여부를 확인한다.
     * @return 카메라 연결 true, false
     */
    @Override
    public boolean isCameraConnected(){
        return false;
    }

    /**
     * 카메라 타입을 반환한다.
     */
    @Override
    public String getCameraDisplayName(){
        return  null;
    }

    /**
     * 카메라 노출 설정값을 반환한다.
     * @return  	AperturePriority, Manual, Program. Shutter, Unknown
     */
    @Override
    public SettingsDefinitions.ExposureMode getExposureMode(){
        return  null;
    }

    /**
     * 카메라 노출값을 설정한다.
     */
    @Override
    public void setExposureMode(SettingsDefinitions.ExposureMode modeType){

    }

    /**
     * 카메라 ISO 설정값을 반환한다.
     * @return Auto, 100, 200, 400, 800, 1600, 3200...
     */
    @Override
    public void getISO(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getISO(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ISO>() {
                        @Override
                        public void onSuccess(SettingsDefinitions.ISO cameraISO) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(getISOString(cameraISO.value()), null, null, null, null));
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_ISO : " + djiError);
                            //mHandler.sendMessage(mHandler.obtainMessage(SHOW_GET_RESULT, "GetResultFail"));
                        }
                    });
        }else{
            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus("N/A", null, null, null, null));
        }
    }

    private String getISOString(int iso)
    {
        return (iso == 0) ? "AUTO" : (iso == 100) ? "100" : (iso == 200) ? "200" : (iso == 400) ? "400" : (iso == 800) ? "800" : (iso == 1600) ? "1600"
                : (iso == 3200) ? "3200" : (iso == 6400) ? "6400" :(iso == 12800) ? "12800" : (iso == 25600) ? "25600" : (iso == 255) ? "FIXED" : "UNKNOWN";
    }

    /**
     * 카메라 ISO값을 설정한다.
     * @param isoType
     */
    @Override
    public void setISO(SettingsDefinitions.ISO isoType){

    }

    /**
     * 카메라의 셔터 속도값을 반환한다.
     * @return 1/2, 1/3/ 1/4, 1/5...
     */
    @Override
    public void getShutterSpeed(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getShutterSpeed(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ShutterSpeed>() {
                        @Override
                        public void onSuccess(SettingsDefinitions.ShutterSpeed speed) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, getShutterSpeedString(speed), null, null, null));
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_SHUTTER_SPEED : " + djiError);
                        }
                    });
        }else{
            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, "N/A", null, null, null));
        }
    }

    private String getShutterSpeedString(SettingsDefinitions.ShutterSpeed speed)
    {
        return (speed == ShutterSpeed.SHUTTER_SPEED_1_8000) ? "1/8000" : (speed == ShutterSpeed.SHUTTER_SPEED_1_6400) ? "1/6400" : (speed == ShutterSpeed.SHUTTER_SPEED_1_6000) ? "1/6000"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_5000) ? "1/5000" : (speed == ShutterSpeed.SHUTTER_SPEED_1_4000) ? "1/4000" : (speed == ShutterSpeed.SHUTTER_SPEED_1_3200) ? "1/3200"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_3000) ? "1/3000" : (speed == ShutterSpeed.SHUTTER_SPEED_1_2500) ? "1/2500" : (speed == ShutterSpeed.SHUTTER_SPEED_1_2000) ? "1/2000"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_1600) ? "1/1600" : (speed == ShutterSpeed.SHUTTER_SPEED_1_1500) ? "1/1500" : (speed == ShutterSpeed.SHUTTER_SPEED_1_1250) ? "1/1250"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_1000) ? "1/1000" : (speed == ShutterSpeed.SHUTTER_SPEED_1_800) ? "1/800" : (speed == ShutterSpeed.SHUTTER_SPEED_1_725) ? "1/725"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_640) ? "1/640" : (speed == ShutterSpeed.SHUTTER_SPEED_1_500) ? "1/500" : (speed == ShutterSpeed.SHUTTER_SPEED_1_400) ? "1/400"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_350) ? "1/350" : (speed == ShutterSpeed.SHUTTER_SPEED_1_320) ? "1/320" : (speed == ShutterSpeed.SHUTTER_SPEED_1_250) ? "1/250"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_240) ? "1/240" : (speed == ShutterSpeed.SHUTTER_SPEED_1_200) ? "1/200" : (speed == ShutterSpeed.SHUTTER_SPEED_1_180) ? "1/180"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_160) ? "1/160" : (speed == ShutterSpeed.SHUTTER_SPEED_1_125) ? "1/125" : (speed == ShutterSpeed.SHUTTER_SPEED_1_120) ? "1/120"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_100) ? "1/100" : (speed == ShutterSpeed.SHUTTER_SPEED_1_90) ? "1/90" : (speed == ShutterSpeed.SHUTTER_SPEED_1_80) ? "1/80"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_60) ? "1/60": (speed == ShutterSpeed.SHUTTER_SPEED_1_50) ? "1/50": (speed == ShutterSpeed.SHUTTER_SPEED_1_40) ? "1/40"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_30) ? "1/30": (speed == ShutterSpeed.SHUTTER_SPEED_1_25) ? "1/25": (speed == ShutterSpeed.SHUTTER_SPEED_1_20) ? "1/20"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_15) ? "1/15" : (speed == ShutterSpeed.SHUTTER_SPEED_1_12_DOT_5) ? "1/12.5" : (speed == ShutterSpeed.SHUTTER_SPEED_1_10) ? "1/10"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_8) ? "1/8" : (speed == ShutterSpeed.SHUTTER_SPEED_1_6_DOT_25) ? "1/6.25" : (speed == ShutterSpeed.SHUTTER_SPEED_1_5) ? "1/5"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_4) ? "1/4" : (speed == ShutterSpeed.SHUTTER_SPEED_1_3) ? "1/3" : (speed == ShutterSpeed.SHUTTER_SPEED_1_2_DOT_5) ? "1/2.5"
                : (speed == ShutterSpeed.SHUTTER_SPEED_1_2) ? "1/2" : (speed == ShutterSpeed.SHUTTER_SPEED_1_1_DOT_67) ? "1/1.67" : (speed == ShutterSpeed.SHUTTER_SPEED_1_1_DOT_25) ? "1/1.25"
                : (speed == ShutterSpeed.UNKNOWN) ? "UNKNOWN" : String.format("%.1f", speed.value());
    }

    /**
     * 셔터 속도값을 설정한다.
     * @param shutterSpeedType
     */
    @Override
    public void setShutterSpeed(SettingsDefinitions.ShutterSpeed shutterSpeedType){

    }

    /**
     * 노출 설정을 지원하는지 체크 X5, X5R 지원
     * @return 지원여부 true, false
     */
    @Override
    public boolean isAdjustableApertureSupported(){
        return false;
    }

    /**
     * 조리개 설정값을 반환한다.
     * @return
     */
    @Override
    public void getAperture(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getAperture(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.Aperture>() {
                        @Override
                        public void onSuccess(SettingsDefinitions.Aperture aperture) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, null, null, getApertureString(aperture.value()), null));
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_APERTURE : " + djiError);
                        }
                    });
        }else{
            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, null, null, "N/A", null));
        }
    }

    private String getApertureString(int aperture)
    {
        return (aperture == 255) ? "UNKNOWN" : String.format("%.1f", (float)aperture/100);
    }

    /**
     * 조리개값을 설정한다.
     */
    @Override
    public void setAperture(SettingsDefinitions.Aperture apertureType){

    }

    /**
     * 카메라 노출 보정값을 반환한다.
     * @return 0.0ev, -0.3ev, -0.7ev....
     */
    @Override
    public void getExposureCompensation(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getExposureCompensation(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ExposureCompensation>() {
                        @Override
                        public void onSuccess(SettingsDefinitions.ExposureCompensation exposure) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, null, getExposureString(exposure.value()), null, null));
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_EXPOSURE : " + djiError);
                        }
                    });
        }else{
            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, null, "N/A", null, null));
        }
    }

    private String getExposureString(int exposure)
    {
        double _result= -5.0 + 1*(exposure/3) + ((exposure%3 == 2) ? 0.3 : (exposure%3 == 0) ? 0.7 : 0.0f);
        return (exposure == 65535) ? "UNKNOWN" : (exposure < 16 ) ? String.format("-%.1f", _result) : String.format("+%.1f", _result);
    }

    /**
     * 카메라 노출 보정값을 설정한다.
     */
    @Override
    public void setExposureCompensation(SettingsDefinitions.ExposureCompensation compensationType){

    }

    /**
     * 자동 노출 설정값을 반환한다.
     * @return
     */
    @Override
    public boolean getAELock(){
        return  false;
    }

    /**
     * 자동 노출값을 설정한다.
     * @param isLocked
     */
    @Override
    public void setAELock(boolean isLocked){

    }

    /**
     * 카메라 화이트밸런스 설정값을 반환한다.
     * @return Auto, Cloudy, CustomColorTemperature, Sunny...
     */
    @Override
    public void getWhiteBalance(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getWhiteBalance(new CommonCallbacks.CompletionCallbackWith<WhiteBalance>() {

                        @Override
                        public void onSuccess(WhiteBalance whiteBalance) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, null, null, null, getWhiteBalanceString(whiteBalance)));
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_WHITE_BALANCE : " + djiError);
                        }
                    });
        }else{
            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(null, null, null, null, "N/A"));
        }
    }

    /**
     * 카메라 화이트밸런스를 설정한다.
     * @param whiteBalancePresetType
     */
    @Override
    public void setWhiteBalance(SettingsDefinitions.WhiteBalancePreset whiteBalancePresetType){

    }

    private String getWhiteBalanceString(WhiteBalance whiteBalance)
    {
        SettingsDefinitions.WhiteBalancePreset _preset =  whiteBalance.getWhiteBalancePreset();

        return (_preset == SettingsDefinitions.WhiteBalancePreset.AUTO) ? "자동" : (_preset == SettingsDefinitions.WhiteBalancePreset.SUNNY) ? "맑음"
                : (_preset == SettingsDefinitions.WhiteBalancePreset.CLOUDY) ? "흐림" : (_preset == SettingsDefinitions.WhiteBalancePreset.INDOOR_INCANDESCENT) ? "백열등"
                : (_preset == SettingsDefinitions.WhiteBalancePreset.INDOOR_FLUORESCENT) ? "형광등" : (_preset == SettingsDefinitions.WhiteBalancePreset.WATER_SURFACE) ? "수면"
                : (_preset == SettingsDefinitions.WhiteBalancePreset.CUSTOM) ? "사용자설정": (_preset == SettingsDefinitions.WhiteBalancePreset.PRESET_NEUTRAL) ? "프리셋" : "UNKNOWN";
    }

    /**
     * 사진의 종횡비를 반환한다.
     * @return 16:9, 4:3, Unknown
     */
    @Override
    public SettingsDefinitions.PhotoAspectRatio getPhotoAspectRatio(){
        return null;
    }

    /**
     * 사진의 종횡비값을 설정한다.
     * @param photoAspectRatioType
     */
    @Override
    public void setPhotoAspectRatio(SettingsDefinitions.PhotoAspectRatio photoAspectRatioType){

    }
    //endregion

    //region 드론 비행 정보
    /**
     * 드론 수평방향 속도값을 가져온다.
     * @return
     */
    @Override
    public float getHorizontalVelocity(){
        return  (float) Math.sqrt(velocyty_x*velocyty_x + velocyty_y*velocyty_y);
    }

    /**
     * 드론 수직방향 속도값을 가져온다.
     * @return
     */
    @Override
    public float getVerticalVelocity(){
        return velocyty_z;
    }

    /**
     * 드론 비행시간값을 가져온다.
     * @return
     */
    @Override
    public int getFlightTimeInSeconds(){
        return flight_time;
    }

    /**
     * 드론 비행설정값을 가져온다.
     * @return
     */
    @Override
    public FlightMode getFlightMode(){
        return  null;
    }

    /**
     * GPS 신호강도값을 가져온다.
     * @return
     */
    @Override
    public GPSSignalLevel getGPSSignalLevel(){
        return  null;
    }

    /**
     * 배터리 남은 용량상태를 가져온다.
     * @return
     */
    @Override
    public BatteryThresholdBehavior getBatteryThresholdBehavior(){
        return  null;
    }
    //endregion

    //region 드론 기체 정보
    @Override
    public int getRemainingFlightTime(){
        return  0;
    }
    //endregion

    //region 임무비행
    //endregion

    //region RTL
    @Override
    public boolean isHomeLocationSet(){
        return false;
    }

    @Override
    public LocationCoordinate2D getHomeLocation(){
        LocationCoordinate2D _location = new LocationCoordinate2D(home_latitude, home_longitude);
        return _location;
    }

    @Override
    public GoHomeExecutionState getGoHomeExecutionState(){
        return  null;
    }

    @Override
    public void startGoHome(){
        if(flight_controller != null){
            flight_controller.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    // 자동복귀 완료
                }
            });
        }
    }

    @Override
    public void cancelGoHome(){
        if(flight_controller != null)
        {
            flight_controller.cancelGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    // 자동복귀 취소
                }
            });
        }
    }

    @Override
    public void setHomeLocation(LocationCoordinate2D home){
        if(flight_controller != null)
        {
            flight_controller.setHomeLocation(home, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    // 자동복귀 위치 설정완료
                }
            });
        }
    }
    //endregion

    //region 조종기

    //endregion

    //region 짐벌
    @Override
    public void setGimbalRotate(float pitch){

    }
    //endregion

    public static DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
            } else {

            }
        }
        @Override
        public void onProductDisconnect() {
            DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_DISCONNECT));
        }
        @Override
        public void onProductConnect(BaseProduct product) {
            if(DroneApplication.getDroneInstance() == null) DroneApplication.setDroneInstance(Drone.DRONE_MANUFACTURE_DJI);

            if (!DroneApplication.getDroneInstance().setDroneDataListener()) {
                DroneApplication.getDroneInstance().getAircaftModel();
                DroneApplication.getDroneInstance().getSerialNumber();
            }
            DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_CONNECT));
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                    @Override
                    public void onConnectivityChange(boolean isConnected) {


                    }
                });
            }
        }

        @Override
        public void onInitProcess(DJISDKInitEvent event, int totalProcess) {

        }
    };

    /**
     * 드론 상태 정보 Callback
     */
    public FlightControllerState.Callback status_callback = new FlightControllerState.Callback() {
        @Override
        public void onUpdate(FlightControllerState current_state) {
                drone_latitude = current_state.getAircraftLocation().getLatitude();
                drone_longitude = current_state.getAircraftLocation().getLongitude();
                drone_altitude = current_state.getAircraftLocation().getAltitude();

                velocyty_x = current_state.getVelocityX();
                velocyty_y = current_state.getVelocityY();
                velocyty_z = current_state.getVelocityZ();

                flight_time = current_state.getFlightTimeInSeconds();

                drone_pitch = current_state.getAttitude().pitch;
                drone_roll = current_state.getAttitude().roll;
                drone_yaw = current_state.getAttitude().yaw;

                flight_mode = current_state.getFlightModeString();

                home_latitude = current_state.getHomeLocation().getLatitude();
                home_longitude = current_state.getHomeLocation().getLongitude();
                home_set = current_state.isHomeLocationSet();

                if((drone_status & DRONE_STATUS_ARMING) == 0 && current_state.areMotorsOn()) {
                    drone_status |= DRONE_STATUS_ARMING;
                    // 드론 시동 켬
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_ARMING));
                }
                else if(!current_state.areMotorsOn() && (drone_status & DRONE_STATUS_ARMING) == 1){
                    drone_status -= (drone_status & DRONE_STATUS_ARMING);
                    // 드론 시동 끔
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_DISARM));
                }

                if((drone_status & DRONE_STATUS_FLYING) == 0 && current_state.isFlying()) {
                    drone_status |= DRONE_STATUS_FLYING;
                    // 드론 Takeoff 드론정보 전송 시작
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_FLYING));
                }

                // 자동복귀 중
                if((drone_status & DRONE_STATUS_RETURN_HOME) == 0 && current_state.isGoingHome()){
                    drone_status |= DRONE_STATUS_RETURN_HOME;
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_RETURN_HOME));
                }
                else if(!current_state.isGoingHome()) {
                    drone_status -= (drone_status & DRONE_STATUS_RETURN_HOME);
                }

                heading = flight_controller.getCompass().getHeading();
        }
    };

    /**
     * Gimbal Data  Callback
     */
    public GimbalState.Callback gimbal_callback = new GimbalState.Callback() {
        @Override
        public void onUpdate(GimbalState gimbal) {
            gimbal_pitch = gimbal.getAttitudeInDegrees().getPitch();
            gimbal_roll = gimbal.getAttitudeInDegrees().getRoll();
            gimbal_yaw = gimbal.getYawRelativeToAircraftHeading();
        }
    };

    /**
     * 배터리 Data Callback
     */
    public BatteryState.Callback battery_callback = new BatteryState.Callback() {
        @Override
        public void onUpdate(BatteryState battery) {
            battery_temperature = battery.getTemperature();
            battery_voltage = battery.getVoltage();
            battery_remain_percent = battery.getChargeRemainingInPercent();
        }
    };

    /**
     * 조종기 Data Callback
     */
    public HardwareState.HardwareStateCallback rc_hardware_callback = new HardwareState.HardwareStateCallback() {
        @Override
        public void onUpdate(HardwareState hardware) {
            left_stick_x = hardware.getLeftStick().getHorizontalPosition();
            left_stick_y = hardware.getLeftStick().getVerticalPosition();
            right_stick_x = hardware.getRightStick().getHorizontalPosition();
            right_stick_y = hardware.getRightStick().getVerticalPosition();
        }
    };

    /**
     * 조종기 GPS Data  Callback
     */
    public GPSData.Callback rc_gps_callback = new GPSData.Callback() {
        @Override
        public void onUpdate(GPSData gps) {
            rc_latitude = gps.getLocation().getLatitude();
            rc_longitude = gps.getLocation().getLongitude();
        }
    };

    /**
     * 카메라 설정(조리개,노출, 셔터, ISO) Callback
     */
    public ExposureSettings.Callback camera_setting_callback= new ExposureSettings.Callback() {

        @Override
        public void onUpdate(@NonNull ExposureSettings exposureSettings) {
            String _aperture = getApertureString(exposureSettings.getAperture().value());
            String _speed = getShutterSpeedString(exposureSettings.getShutterSpeed());
            String _iso = getISOString(exposureSettings.getISO());
            String _exposure = getExposureString(exposureSettings.getExposureCompensation().value());
            getWhiteBalance();

            DroneApplication.getEventBus().post(new MainActivity.DroneCameraStatus(_iso, _speed, _exposure, _aperture, "N/A"));
        }
    };

    /**
     * 카메라 상태 Callback
     */
    public SystemState.Callback camera_state_callback= new SystemState.Callback() {

        @Override
        public void onUpdate(@NonNull SystemState systemState) {

        }
    };

    /**
     * 카메라 저장정보 Callback
     */
    public StorageState.Callback camera_storage_callback= new StorageState.Callback() {

        @Override
        public void onUpdate(@NonNull StorageState storageState) {
            boolean _inserted = storageState.isInserted();
            int _remain_storage = storageState.getRemainingSpaceInMB();
            long _capture_count = storageState.getAvailableCaptureCount();
            int _recording_time = storageState.getAvailableRecordingTimeInSeconds();
        }
    };
}
