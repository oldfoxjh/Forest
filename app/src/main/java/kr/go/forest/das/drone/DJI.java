/**
 Class Name : DJI.java
 Description : DJI 드론 정보 관리 클래스
 드론 앱서비스
 수정일     수정자     수정내용
 ------- -------- ---------------------------
 2019.09.01 정호   최초 생성

 author : 이노드(연구소) 정호
 since : 2019.09.01
 */
package kr.go.forest.das.drone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import dji.common.battery.BatteryState;
import dji.common.camera.ExposureSettings;
import dji.common.camera.ResolutionAndFrameRate;
import dji.common.camera.SettingsDefinitions;
import dji.common.camera.StorageState;
import dji.common.camera.SystemState;
import dji.common.camera.WhiteBalance;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.gimbal.GimbalState;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.product.Model;
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
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.timeline.TimelineElement;
import dji.sdk.mission.timeline.actions.ShootPhotoAction;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.R;

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

    /*=============================================================*
     *  현재 드론 상태를 반환한다.
     *==============================================================*/
    public int getDroneStatus(){
        return drone_status;
    };

    /*=============================================================*
     *  현재 드론 비행여부를 확인한다.
     *==============================================================*/
    public boolean isFlying(){ return is_flying; }

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
    public Model getAircaftModel(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
                this.model = _product.getModel();
                return this.model;
        }

        return Model.UNKNOWN_AIRCRAFT;
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
                    BigdataSystemInfo _info = DroneApplication.getSystemInfo();
                    _info.drone_sn = s;
                }

                @Override
                public void onFailure(DJIError djiError) {
                    LogWrapper.i(TAG, "SN Error : " + djiError.getDescription());
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
    public boolean setDroneDataListener() {
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            if (_product instanceof Aircraft) {

                flight_controller = ((Aircraft) _product).getFlightController();
                cameras = ((Aircraft) _product).getCameras();
                gimbals = ((Aircraft) _product).getGimbals();
                batteries = _product.getBatteries();

                if(flight_controller != null)
                flight_controller.setStateCallback(status_callback);

                // 조종기 callback
                remote_controller = ((Aircraft) _product).getRemoteController();
                if(remote_controller != null)
                remote_controller.setHardwareStateCallback(rc_hardware_callback);

                // 짐벌 callback
                for (Gimbal gimbal:gimbals) {
                    gimbal.setStateCallback(gimbal_callback);
                }

                // 배터리 callback
                for (Battery battery:batteries) {
                    battery.setStateCallback(battery_callback);
                }


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

    public boolean removeDroneDataListener(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            if (_product instanceof Aircraft) {
                // 드론 정보 callback
                flight_controller.setStateCallback(null);

                // 조종기 callback
                remote_controller.setHardwareStateCallback(null);
                remote_controller.setGPSDataCallback(null);

                // 짐벌 callback
                gimbals = ((Aircraft) _product).getGimbals();
                for (Gimbal gimbal:gimbals) {
                    gimbal.setStateCallback(null);
                }

                // 배터리 callback
                batteries = _product.getBatteries();
                for (Battery battery:batteries) {
                    battery.setStateCallback(null);
                }

                // 카메라 callback
                cameras = ((Aircraft) _product).getCameras();
                for (Camera camera:cameras) {
                    camera.setSystemStateCallback(null);
                    camera.setExposureSettingsCallback(null);
                    camera.setStorageStateCallBack(null);
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
                            LogWrapper.i(TAG, "getCameraMode : " + djiError.getDescription());
                        }
                    });
        }
    }

    @Override
    public void getCameraFocalLength(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {

            cmos_factor = (float)(13.2/8.8);

            if(_product.getCamera().isDigitalZoomSupported()){
                _product.getCamera().getOpticalZoomFocalLength(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        LogWrapper.i(TAG, "getOpticalZoomFocalLength : " + integer);
                        cmos_factor = (float)(17.3*10/integer);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        LogWrapper.i(TAG, "getOpticalZoomFocalLength : " + djiError.getDescription());
                    }
                });
            }else{
                if(model == Model.MAVIC_AIR || model == Model.PHANTOM_4 || model == Model.Spark || model == Model.MAVIC_2 || model == Model.MAVIC_PRO){
                    cmos_factor = (float)(6.17/3.57);
                    LogWrapper.i(TAG, "cmos_factor : 6.17");
                }else{
                    cmos_factor = (float)(13.2/8.8);
                    LogWrapper.i(TAG, "cmos_factor : 13.2");
                }
            }
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
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .startRecordVideo(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraShootInfo(SettingsDefinitions.CameraMode.RECORD_VIDEO, true));
                        }
                    });
        }
    }

    /**
     * 비디오 촬영을 종료한다.
     */
    @Override
    public void stopRecordVideo(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraShootInfo(SettingsDefinitions.CameraMode.RECORD_VIDEO, false));
                        }
                    });
        }
    }

    /**
     * 한장 또는 카메라 촬영 설정에 따라 촬영을 시작한다.
     */
    @Override
    public void startShootPhoto(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .startShootPhoto(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            DroneApplication.getEventBus().post(new MainActivity.DroneCameraShootInfo(SettingsDefinitions.CameraMode.SHOOT_PHOTO, false));
                        }
                    });
        }
    }

    /**
     * 사진 촬영을 멈춘다.
     */
    @Override
    public void stopShootPhoto(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .stopShootPhoto(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {

                        }
                    });
        }
    }

    /**
     * 카메라 및 저장장치의 정보값을 얻는다.
     */
    @Override
    public CameraInfo getStorageInfo() {
        CameraInfo _storage_info = new CameraInfo();
        _storage_info.video_resolution_framerate = video_resolution_framerate;
        _storage_info.capture_count = capture_count;
        _storage_info.photo_file_format = photo_file_format;
        _storage_info.recording_remain_time = recording_remain_time;
        _storage_info.recording_time = (recording_time == null) ? "00:00" : recording_time;

        _storage_info.camera_aperture = camera_aperture;
        _storage_info.camera_shutter = camera_shutter;
        _storage_info.camera_iso = camera_iso;
        _storage_info.camera_exposure = camera_exposure;
        _storage_info.camera_whitebalance = camera_whitebalance;
        _storage_info.camera_ae_lock = camera_ae_lock;
        _storage_info.is_camera_auto_exposure_unlock_enabled = is_camera_auto_exposure_unlock_enabled;

        _storage_info.camera_aspect_ratio = camera_aspect_ratio;
        _storage_info.cmos_factor = cmos_factor;

        return _storage_info;
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
     * 카메라 촬영 모드를 반환한다.
     * @return SettingsDefinitions.ShootPhotoMode
     */
    public SettingsDefinitions.ShootPhotoMode getShootPhotoMode(){
        return shoot_photo_mode;
    }

    /**
     * 카메라 노출 설정값을 반환한다.
     * @return  	AperturePriority, Manual, Program. Shutter, Unknown
     */
    @Override
    public SettingsDefinitions.ExposureMode getExposureMode(){
        return  null;
    }

    public void getAutoAEUnlockEnabled(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getAutoAEUnlockEnabled(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean aBoolean) {
                            is_camera_auto_exposure_unlock_enabled = aBoolean;
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_AUTO : " + djiError.getDescription());
                        }
                    });
        }
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
                            camera_iso = getISOString(cameraISO.value());
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_ISO : " + djiError.getDescription());
                        }
                    });
        }else{
            camera_iso = "N/A";
        }
    }

    private String getISOString(int iso) {
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
                            camera_shutter = getShutterSpeedString(speed);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_SHUTTER_SPEED : " + djiError);
                        }
                    });
        }else{
            camera_shutter = "N/A";
        }
    }

    private String getShutterSpeedString(SettingsDefinitions.ShutterSpeed speed) {
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
                            camera_aperture = getApertureString(aperture.value());
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_APERTURE : " + djiError);
                        }
                    });
        }else{
            camera_aperture = "N/A";
        }
    }

    private String getApertureString(int aperture) {
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
                            camera_exposure = getExposureString(exposure.value());
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_EXPOSURE : " + djiError.getDescription());
                        }
                    });
        }else{
            camera_exposure = "N/A";
        }
    }

    private String getExposureString(int exposure) {
        double _result;
        if(exposure < 16){
            _result= -5.0 + 1*(exposure/3) + ((exposure%3 == 2) ? 0.3 : (exposure%3 == 0) ? -0.3 : 0.0f);
        }else{
            _result= -5.0 + 1*(exposure/3) + ((exposure%3 == 2) ? 0.3 : (exposure%3 == 0) ? 0.7-1.0 : 0.0f);
        }

        return (exposure == 65535) ? "UNKNOWN" : (exposure < 16 ) ? String.format("%.1f", _result) : String.format("+%.1f", _result);
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
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getAELock(new CommonCallbacks.CompletionCallbackWith<Boolean>() {
                        @Override
                        public void onSuccess(Boolean is_lock) {
                            camera_ae_lock = is_lock;
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_AE_LOCK : " + djiError.getDescription());
                        }
                    });
        }else{
            camera_ae_lock = false;
        }

        return  true;
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
                            camera_whitebalance = getWhiteBalanceString(whiteBalance);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "CAMERA_WHITE_BALANCE : " + djiError.getDescription());
                        }
                    });
        }else{
            camera_whitebalance = "N/A";
        }
    }

    /**
     * 카메라 화이트밸런스를 설정한다.
     * @param whiteBalancePresetType
     */
    @Override
    public void setWhiteBalance(SettingsDefinitions.WhiteBalancePreset whiteBalancePresetType){

    }

    /**
     * 화이트밸런스 설정값을 한글로 변환한다.
     */
    private String getWhiteBalanceString(WhiteBalance whiteBalance) {
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
    public void getPhotoAspectRatio(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            _product.getCamera()
                    .getPhotoAspectRatio(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoAspectRatio>() {

                        @Override
                        public void onSuccess(SettingsDefinitions.PhotoAspectRatio ratio) {
                            camera_aspect_ratio = (ratio == SettingsDefinitions.PhotoAspectRatio.RATIO_4_3) ? 0.75f
                                                  : (ratio == SettingsDefinitions.PhotoAspectRatio.RATIO_16_9) ? 0.5625f
                                                  : (ratio == SettingsDefinitions.PhotoAspectRatio.RATIO_3_2) ? 0.6667f : 0.0f;

                            LogWrapper.i(TAG, "getPhotoAspectRatio : %f" + camera_aspect_ratio);
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "getPhotoAspectRatio : " + djiError.getDescription());
                        }
                    });
        }
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
     * 자동이륙 고도
     */
    public String getTakeoffAltitude(){
        return "1.2m";
    }

    /**
     * 조종기와 연결이 끊어졌을 때의 동작을 설정한다.
     */
    public void setConnectionFailSafeBehavior(ConnectionFailSafeBehavior behavior){
        flight_controller.setConnectionFailSafeBehavior(behavior, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null){
                    // 동작 설정 실패 팝업
                    LogWrapper.e(TAG, "start Takeoff Fail : " + djiError.getDescription());
                }else{
                    DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_UPLOAD_MISSION, 0, null));
                }
            }
        });
    }

    /**
     * 자동이륙 명령
     */
    public void startTakeoff(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            flight_controller = ((Aircraft) _product).getFlightController();
            flight_controller.startTakeoff(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null){
                        LogWrapper.e(TAG, "start Takeoff Fail : " + djiError.getDescription());
                        // 모터가 켜있을 경우 실패 팝업
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.takeoff_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_TAKEOFF_SUCCESS, null));
                    }
                }
            });
        }
    }

    /**
     * 자동착륙 명령
     */
    public void startLanding(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            flight_controller = ((Aircraft) _product).getFlightController();
            flight_controller.startLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null){
                        LogWrapper.e(TAG, "start Landing Fail : " + djiError.getDescription());
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.landing_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_LANDING_SUCCESS, null));
                    }
                }
            });
        }
    }

    /**
     * 자동착륙 취소
     */
    public void cancelLanding(){
        BaseProduct _product = DJISDKManager.getInstance().getProduct();
        if (_product != null && _product.isConnected()) {
            flight_controller = ((Aircraft) _product).getFlightController();
            flight_controller.cancelLanding(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null){
                        LogWrapper.e(TAG, "cancel Landing Fail : " + djiError.getDescription());
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.landing_cancel_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.CANCEL_LANDING_SUCCESS, null));
                    }
                }
            });
        }
    }

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

    @Override
    public ConnectionFailSafeBehavior getConnectionFailSafeBehavior(){
        return connection_failsafe_behavior;
    }
    //endregion

    //region 임무비행
    /**
     * 임무를 업로드 가능한지 체크
     */
    public boolean isMissionUploadAvailable(){
        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();

        return WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(mission_operator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(mission_operator.getCurrentState());
    }

    /**
     * 임무 시작이 가능한지 체크
     */
    public boolean isMissionStartAvailable(){
        if(ready_start_mission < 0) return false;
        return true;
    }

    /**
     * 설정된 임무를 드론에 업로드
     */
    public String uploadMission(WaypointMission mission){

        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
        waypoint_count = mission.getWaypointCount() - 1;
        // 설정된 임무에 대한 확인
        DJIError  _error = MissionControl.getInstance().getWaypointMissionOperator().loadMission(mission);
        if(_error != null){
            LogWrapper.i("DJI", "loadMission Fail : " + _error.getDescription());
            return _error.getDescription();
        }

        // 임무 업로드
//        mission_operator.uploadMission(new CommonCallbacks.CompletionCallback() {
//            @Override
//            public void onResult(DJIError djiError) {
//                if(djiError != null) {
//                    // 임무 업로드 실패
//                    LogWrapper.i("DJI", "uploadMission Fail : " + djiError.getDescription());
//                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_UPLOAD_FAIL, djiError.getDescription()));
//                }else{
//                    // 임무 업로드 성공
//                    LogWrapper.i("DJI", "uploadMission Sucess");
//                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_UPLOAD_SUCCESS, null));
//                }
//            }
//        });

        return null;
    }

    /**
     * 설정된 임무를 시작하기 위한 조건 설정
     */
    public void setMissionCondition(int captureCount, int timeIntervalInSeconds){
    }

    /**
     * 설정된 임무를 시작
     */
    public void startMission(int _shoot_count, int _interval){
        // check pre-condition
        if(ready_start_mission < 0){
            DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_START_FAIL, "카메라 설정에 실패하였습니다."));
            return;
        }

        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission_operator.addListener(mission_notification_listener);
        shoot_count = _shoot_count;
        interval = _interval;

        mission_operator.startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null) {
                    LogWrapper.i("WaypointMission", "startMission Fail : " + djiError.getDescription());
                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_START_FAIL, djiError.getDescription()));
                    WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
                    mission_operator.removeListener(mission_notification_listener);
                }else{
                    // 임무 시작 성공
                    //DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_START_MISSION_SUCCESS, null));
                    LogWrapper.i("DJI", "임무시작???");
                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_START_SUCCESS, null));
                }
            }
        });
    }

    /**
     * 설정된 임무 종료
     */
    public void stopMission(){
        WaypointMissionOperator mission_operator = MissionControl.getInstance().getWaypointMissionOperator();
        mission_operator.stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if(djiError != null) {
                    LogWrapper.i("WaypointMission", "stopMission Fail : " + djiError.getDescription());
                }else{
                    MissionControl.getInstance().getWaypointMissionOperator().removeListener(mission_notification_listener);
                }
            }
        });
    }

    /**
     * 임무비행 경로 정보를 반환한다.
     * @return 임무비행 경로
     */
    public List<GeoPoint> getMissionPoints(){
        return flight_points;
    }

    /**
     * 임무비행 경로를 설정한다.
     * @param points 임무비행 경로
     */
    public void setMissionPoints(List<GeoPoint> points){

        if(flight_points == null){
            flight_points = new ArrayList<>();
        }

        if(points == null) {
            flight_points.clear();
            flight_points = null;
            return;
        }

        flight_points.clear();
        flight_points.addAll(points);
    }

    /**
     * 드론 최대비행고도를 설정한다.
     */
    public void setMaxFlightHeight(int height){
        if(flight_controller != null){
            flight_controller.setMaxFlightHeight(height, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null){
                        LogWrapper.e(TAG, "setMaxFlightHeight Fail : " + djiError.getDescription());
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.max_flight_height_fail, djiError.getDescription()));
                    }else{
                        DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MAX_FLIGHT_HEIGHT_SET_SUCCESS, null));
                    }
                }
            });
        }
    }

    //endregion

    //region RTL
    @Override
    public boolean isHomeLocationSet(){
        return false;
    }

    /**
     * 자동복귀 위치 불러오기
     */
    @Override
    public LocationCoordinate2D getHomeLocation(){
        LocationCoordinate2D _location = new LocationCoordinate2D(home_latitude, home_longitude);
        return _location;
    }

    @Override
    public GoHomeExecutionState getGoHomeExecutionState(){
        return  null;
    }

    /**
     * 자동복귀 시작
     */
    @Override
    public void startGoHome(){
        if(flight_controller != null){
            flight_controller.startGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null){
                        LogWrapper.e(TAG, "start go home Fail : " + djiError.getDescription());
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.return_home_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_RETURN_HOME_SUCCESS, null));
                    }
                }
            });
        }
    }

    /**
     * 자동복귀 취소
     */
    @Override
    public void cancelGoHome(){
        if(flight_controller != null)
        {
            flight_controller.cancelGoHome(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    // 자동복귀 취소
                    if(djiError != null){
                        LogWrapper.e(TAG, "cancel go home Fail : " + djiError.getDescription());
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.cancel_return_home_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.CANCEL_RETURN_HOME_SUCCESS, null));
                    }
                }
            });
        }
    }

    /**
     * 자동복귀 위치 설정
     */
    @Override
    public void setHomeLocation(LocationCoordinate2D home){
        if(flight_controller != null)
        {
            flight_controller.setHomeLocation(home, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null){
                        // 자동복귀 위치 설정 실패
                        LogWrapper.e(TAG, "start go home Fail : " + djiError.getDescription());
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.set_home_location_fail, djiError.getDescription()));
                    }else{
                        // UI 변경을 위한 메세지
                        DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_RETURN_HOME_SUCCESS, null));
                    }
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

    /**
     * DJI SDK 등록
     */
    public static DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {
        /**
         * DJI API 등록 이벤트
         */
        @Override
        public void onRegister(DJIError error) {
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                LogWrapper.i("onRegister", "Success");
            } else {
                LogWrapper.i("onRegister", "Error : " + error.getDescription());
            }
        }

        /**
         * 드론과 끊겼을 때 이벤트
         */
        @Override
        public void onProductDisconnect() {
            DroneApplication.getDroneInstance().removeDroneDataListener();
            DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_DISCONNECT));
        }

        /**
         * 드론과 연결되었을 때 이벤트
         */
        @Override
        public void onProductConnect(BaseProduct product) {
            if(DroneApplication.getDroneInstance() == null) DroneApplication.setDroneInstance(Drone.DRONE_MANUFACTURE_DJI);

            if (DroneApplication.getDroneInstance().setDroneDataListener()) {
                DroneApplication.getDroneInstance().getAircaftModel();          // 드론 모델정보 설정
                DroneApplication.getDroneInstance().getSerialNumber();          // 드론 시리얼정보 설정
                DroneApplication.getDroneInstance().getCameraFocalLength();     // 드론 카메라 GSD factor 설정
            }
            DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_CONNECT));
            DroneApplication.getEventBus().post(new MainActivity.ToastOrb(DroneApplication.getDroneInstance().getAircaftModel().getDisplayName() + " 연결되었습니다."));
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(new BaseComponent.ComponentListener() {
                    @Override
                    public void onConnectivityChange(boolean isConnected) {
                        if(isConnected == false){
                            DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_DISCONNECT));
                        }
                    }
                });
            }
        }

        @Override
        public void onInitProcess(DJISDKInitEvent event, int totalProcess) {
            LogWrapper.i("onInitProcess", "Success");
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

            is_flying =  current_state.isFlying();

            // 시동 걸려있는지 체크
            if((drone_status & DRONE_STATUS_ARMING) == 0 && current_state.areMotorsOn()) {
                drone_status += DRONE_STATUS_ARMING;
                // 드론 시동 켬
                DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_ARMING));
            }

            // 시동 꺼져있는지 체크
            if(!current_state.areMotorsOn() && (drone_status & DRONE_STATUS_ARMING) == DRONE_STATUS_ARMING){
                drone_status -=  DRONE_STATUS_ARMING;
                // 드론이 연결된 상태에서 모터 체크
                BaseProduct _product = DJISDKManager.getInstance().getProduct();
                if (_product != null && _product.isConnected()) {
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_DISARM));
                }
            }

            // 자동복귀 중
            if((drone_status & DRONE_STATUS_RETURN_HOME) == 0 && current_state.isGoingHome()){
                drone_status += DRONE_STATUS_RETURN_HOME;
                DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(DRONE_STATUS_RETURN_HOME));
            }

            // 자동복귀 취소 됨
            if(!current_state.isGoingHome() && (drone_status & DRONE_STATUS_RETURN_HOME) == DRONE_STATUS_RETURN_HOME) {
                drone_status -= DRONE_STATUS_RETURN_HOME;
                DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_CANCEL_RETURN_HOME));
            }

            if(flight_controller != null && flight_controller.getCompass() != null) {
                heading = flight_controller.getCompass().getHeading();
            }

            // 드론 최대비행고도값 받아오기
            if(max_flight_height < 20) {
                flight_controller.getMaxFlightHeight(new CommonCallbacks.CompletionCallbackWith<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        max_flight_height = integer;
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        LogWrapper.i("DJI", "getMaxFlightHeight Fail : " + djiError.getDescription());
                    }
                });
            }

            // 드론 Connection FailSafe 처리 정보
            if(connection_failsafe_behavior == ConnectionFailSafeBehavior.UNKNOWN) {
                flight_controller.getConnectionFailSafeBehavior(new CommonCallbacks.CompletionCallbackWith<ConnectionFailSafeBehavior>() {
                    @Override
                    public void onSuccess(ConnectionFailSafeBehavior connectionFailSafeBehavior) {
                        connection_failsafe_behavior = connectionFailSafeBehavior;
                        LogWrapper.i("DJI", "getConnectionFailSafeBehavior : " + connection_failsafe_behavior.value());
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        LogWrapper.i("DJI", "getConnectionFailSafeBehavior Fail : " + djiError.getDescription());
                    }
                });
            }

            // 카메라 촬영모드 가져오기
            if(shoot_photo_mode == SettingsDefinitions.ShootPhotoMode.UNKNOWN && cameras != null && cameras.size() > 0) {
                for(Camera camera : cameras){
                    camera.getShootPhotoMode(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.ShootPhotoMode>() {

                        @Override
                        public void onSuccess(SettingsDefinitions.ShootPhotoMode shootPhotoMode) {
                            shoot_photo_mode = shootPhotoMode;
                        }

                        @Override
                        public void onFailure(DJIError djiError) {
                            LogWrapper.i(TAG, "getShootPhotoMode Fail : " + djiError.getDescription());
                        }
                    });
                }
            }
        }
    };

    /*=============================================================*
     *  Gimbal 데이터 콜백함수
     *==============================================================*/
    public GimbalState.Callback gimbal_callback = new GimbalState.Callback() {
        @Override
        public void onUpdate(GimbalState gimbal) {
            gimbal_pitch = gimbal.getAttitudeInDegrees().getPitch();
            gimbal_roll = gimbal.getAttitudeInDegrees().getRoll();
            gimbal_yaw = gimbal.getYawRelativeToAircraftHeading();
        }
    };

    /*=============================================================*
     *  배터리 Data Callback
     *==============================================================*/
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
     * 카메라 설정(조리개,노출, 셔터, ISO) Callback
     */
    public ExposureSettings.Callback camera_setting_callback= new ExposureSettings.Callback() {

        @Override
        public void onUpdate(@NonNull ExposureSettings exposureSettings) {
            camera_aperture = getApertureString(exposureSettings.getAperture().value());
            camera_shutter = getShutterSpeedString(exposureSettings.getShutterSpeed());
            camera_iso = getISOString(exposureSettings.getISO());
            camera_exposure = getExposureString(exposureSettings.getExposureCompensation().value());
            getWhiteBalance();
        }
    };

    /**
     * 카메라 상태 Callback
     */
    public SystemState.Callback camera_state_callback= new SystemState.Callback() {

        @Override
        public void onUpdate(@NonNull SystemState systemState) {
            if(systemState.isRecording()) {
                recording_time = getRemainTime(systemState.getCurrentVideoRecordingTimeInSeconds());
                MainActivity.DroneCameraStatus _status = new MainActivity.DroneCameraStatus();
                _status.setMode(1);
                DroneApplication.getEventBus().post(_status);
            }else if(systemState.isStoringPhoto()){
                MainActivity.DroneCameraStatus _status = new MainActivity.DroneCameraStatus();
                _status.setMode(0);
                DroneApplication.getEventBus().post(_status);
            }else if(!systemState.isRecording() && recording_time != null) {
                recording_time = null;
                MainActivity.DroneCameraStatus _status = new MainActivity.DroneCameraStatus();
                _status.setMode(1);
                DroneApplication.getEventBus().post(_status);
            }
        }
    };

    /**
     * 카메라 저장정보 Callback
     */
    public StorageState.Callback camera_storage_callback= new StorageState.Callback() {
        @Override
        public void onUpdate(@NonNull StorageState storageState) {
            inserted = storageState.isInserted();
            remain_storage = storageState.getRemainingSpaceInMB();
            capture_count = String.format("%d", storageState.getAvailableCaptureCount());
            recording_remain_time = getRemainTime(storageState.getAvailableRecordingTimeInSeconds());

            BaseProduct _product = DJISDKManager.getInstance().getProduct();

            if(photo_file_format == null) {
                _product.getCamera().getPhotoFileFormat(new CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.PhotoFileFormat>() {
                    @Override
                    public void onSuccess(SettingsDefinitions.PhotoFileFormat photoFileFormat) {

                        int _value = photoFileFormat.value();
                        photo_file_format = (_value == 0) ? "RAW" : (_value == 1) ? "JPEG" : "JEPG+RAW";
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        photo_file_format = "N/A";
                    }
                });
            }

            if(video_resolution_framerate == null){
                _product.getCamera().getVideoResolutionAndFrameRate(new CommonCallbacks.CompletionCallbackWith<ResolutionAndFrameRate>() {
                    @Override
                    public void onSuccess(ResolutionAndFrameRate resolutionAndFrameRate) {
                        int _frame_rate = resolutionAndFrameRate.getFrameRate().value();
                        int _resolution = resolutionAndFrameRate.getResolution().value();

                        video_resolution_framerate = getResolution(_resolution) + "/" + getFrameRate(_frame_rate);
                    }

                    @Override
                    public void onFailure(DJIError djiError) {
                        video_resolution_framerate = "N/A";
                    }
                });
            }
        }
    };

    private WaypointMissionOperatorListener mission_notification_listener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {
            if (uploadEvent.getProgress() != null){
                if(uploadEvent.getProgress().isSummaryUploaded
                        && uploadEvent.getProgress().uploadedWaypointIndex == waypoint_count) {
                    LogWrapper.i("WaypointMission", "Upload successful!");
                }else{
                    LogWrapper.i("WaypointMission", "Upload Index : " + uploadEvent.getProgress().uploadedWaypointIndex);
                }
            }
        }

        private boolean start_timeline = false;
        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {
            LogWrapper.i("WaypointMission",
                    (executionEvent.getPreviousState() == null
                            ? ""
                            : executionEvent.getPreviousState().getName())
                            + ", "
                            + executionEvent.getCurrentState().getName()
                            + (executionEvent.getProgress() == null
                            ? ""
                            : executionEvent.getProgress().targetWaypointIndex));

            // Timeline Mission
//            if(executionEvent.getProgress().targetWaypointIndex == 1 && start_timeline == false){
//                List<TimelineElement> elements = new ArrayList<>();
//                elements.add(ShootPhotoAction.newShootIntervalPhotoAction(shoot_count,interval));
//                MissionControl.getInstance().scheduleElements(elements);
//                MissionControl.getInstance().startTimeline();
//                start_timeline = true;
//            }

        }

        @Override
        public void onExecutionStart() {
            if((drone_status & DRONE_STATUS_MISSION) == 0) {
                drone_status += DRONE_STATUS_MISSION;
            }
        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            String _error = (error != null) ? error.getDescription() : "null";
            LogWrapper.e("WaypointMission", "Execution Finish!" + _error);
            if((drone_status & DRONE_STATUS_MISSION) == DRONE_STATUS_MISSION) {
                drone_status -= DRONE_STATUS_MISSION;
            }
            stopShootPhoto();
        }
    };

    private String getResolution(int resolution) {
        return (resolution == 2) ? "720p" : (resolution == 3) ? "1080p" : (resolution == 2) ? "720p" : (resolution == 4 || resolution == 5) ? "2.7K" : (resolution == 4 || resolution == 5) ? "2.7K"
                : (resolution > 5 && resolution < 12) ? "4K" : "UNKNOWN";
    }

    private String getFrameRate(int frame_rate) {
        return (frame_rate == 0) ? "23" : (frame_rate == 1) ? "24" : (frame_rate == 2) ? "25" : (frame_rate == 3) ? "29" : (frame_rate == 4) ? "30" : (frame_rate == 5) ? "47" : (frame_rate == 6) ? "48"
                : (frame_rate == 7) ? "50" : (frame_rate == 8) ? "59" : (frame_rate == 9) ? "60" : (frame_rate == 10) ? "96" : (frame_rate == 11) ? "100" : (frame_rate == 12) ? "120" : (frame_rate == 13) ? "240"
                : (frame_rate == 14) ? "7" : (frame_rate == 15) ? "90" :(frame_rate == 16) ? "8" : "UNKNOWN";
    }

    private String getRemainTime(int second) {
        int min = (second / 60);
        int hour = min / 60;
        int sec = second % 60;
        min = min % 60;

        return (hour > 0) ? String.format("%02d:%02d:%02d", hour, min, sec): String.format("%02d:%02d", min, sec);
    }
}
