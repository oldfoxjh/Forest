package kr.go.forest.das.drone;

import android.util.Log;
import android.widget.Toast;

import java.util.List;

import dji.common.battery.BatteryState;
import dji.common.camera.SettingsDefinitions;
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

public class DJI extends Drone{

    private static final String TAG = "DJI Drone";

    private FlightController flight_controller = null;
    private RemoteController remote_controller = null;
    private List<Gimbal> gimbals = null;
    private List<Battery> batteries = null;


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
                flight_controller = ((Aircraft) _product).getFlightController();
                flight_controller.setStateCallback(status_callback);

                remote_controller = ((Aircraft) _product).getRemoteController();
                remote_controller.setHardwareStateCallback(rc_hardware_callback);
                remote_controller.setGPSDataCallback(rc_gps_callback);

                gimbals = ((Aircraft) _product).getGimbals();
                for (Gimbal gimbal:gimbals) {
                    gimbal.setStateCallback(gimbal_callback);
                }

                batteries = _product.getBatteries();
                for (Battery battery:batteries) {
                    battery.setStateCallback(battery_callback);
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
    public SettingsDefinitions.CameraMode getCameraMode(){
        return  null;
    }

    /**
     * 카메라 동작을 설정한다.
     * @param modeType : SHOOT_PHOTO, RECORD_VIDEO
     */
    @Override
    public void setCameraMode(SettingsDefinitions.CameraMode modeType){

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
    public SettingsDefinitions.ISO getISO(){
        return  null;
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
    public SettingsDefinitions.ShutterSpeed getShutterSpeed(){
        return  null;
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
    public SettingsDefinitions.Aperture getAperture(){
        return null;
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
    public SettingsDefinitions.ExposureCompensation getExposureCompensation(){
        return  null;
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
    public SettingsDefinitions.WhiteBalancePreset getWhiteBalance(){
        return null;
    }

    /**
     * 카메라 화이트밸런스를 설정한다.
     * @param whiteBalancePresetType
     */
    @Override
    public void setWhiteBalance(SettingsDefinitions.WhiteBalancePreset whiteBalancePresetType){

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
            DroneApplication.getEventBus().post(new MainActivity.DroneStatusionChange(Drone.DRONE_STATUS_DISCONNECT));
        }
        @Override
        public void onProductConnect(BaseProduct product) {
            if(DroneApplication.getDroneInstance() == null) DroneApplication.setDroneInstance(Drone.DRONE_MANUFACTURE_DJI);

            if (!DroneApplication.getDroneInstance().setDroneDataListener()) {
                DroneApplication.getDroneInstance().getAircaftModel();
                DroneApplication.getDroneInstance().getSerialNumber();
            }
            DroneApplication.getEventBus().post(new MainActivity.DroneStatusionChange(Drone.DRONE_STATUS_CONNECT));
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
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusionChange(Drone.DRONE_STATUS_ARMING));
                }
                else if(!current_state.areMotorsOn() && (drone_status & DRONE_STATUS_ARMING) == 1){
                    drone_status -= (drone_status & DRONE_STATUS_ARMING);
                    // 드론 시동 끔
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusionChange(Drone.DRONE_STATUS_DISARM));
                }

                if((drone_status & DRONE_STATUS_FLYING) == 0 && current_state.isFlying()) {
                    drone_status |= DRONE_STATUS_FLYING;
                    // 드론 Takeoff 드론정보 전송 시작
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusionChange(Drone.DRONE_STATUS_FLYING));
                }

                // 자동복귀 중
                if((drone_status & DRONE_STATUS_RETURN_HOME) == 0 && current_state.isGoingHome()){
                    drone_status |= DRONE_STATUS_RETURN_HOME;
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusionChange(Drone.DRONE_STATUS_RETURN_HOME));
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
}
