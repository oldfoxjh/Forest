package kr.go.forest.das.drone;


import android.Manifest;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.model.LocationCoordinate2D;
import dji.common.product.Model;
import dji.sdk.base.BaseProduct;
import dji.sdk.battery.Battery;
import io.dronefleet.mavlink.common.Attitude;
import io.dronefleet.mavlink.common.AutopilotVersion;
import io.dronefleet.mavlink.common.BatteryStatus;
import io.dronefleet.mavlink.common.CommandAck;
import io.dronefleet.mavlink.common.ExtendedSysState;
import io.dronefleet.mavlink.common.GlobalPositionInt;
import io.dronefleet.mavlink.common.GpsFixType;
import io.dronefleet.mavlink.common.GpsRawInt;
import io.dronefleet.mavlink.common.Heartbeat;
import io.dronefleet.mavlink.common.HomePosition;
import io.dronefleet.mavlink.common.LocalPositionNed;
import io.dronefleet.mavlink.common.ManualControl;
import io.dronefleet.mavlink.common.MavAutopilot;
import io.dronefleet.mavlink.common.MavCmd;
import io.dronefleet.mavlink.common.MavFrame;
import io.dronefleet.mavlink.common.MavLandedState;
import io.dronefleet.mavlink.common.MavMissionResult;
import io.dronefleet.mavlink.common.MavMissionType;
import io.dronefleet.mavlink.common.MavModeFlag;
import io.dronefleet.mavlink.common.MavResult;
import io.dronefleet.mavlink.common.MavSeverity;
import io.dronefleet.mavlink.common.MavState;
import io.dronefleet.mavlink.common.MissionAck;
import io.dronefleet.mavlink.common.MissionCurrent;
import io.dronefleet.mavlink.common.MissionRequest;
import io.dronefleet.mavlink.common.MissionRequestInt;
import io.dronefleet.mavlink.common.RcChannels;
import io.dronefleet.mavlink.common.Statustext;
import io.dronefleet.mavlink.common.SysStatus;
import io.dronefleet.mavlink.common.VfrHud;
import io.dronefleet.mavlink.util.EnumValue;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MAVLink.MavDataManager;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.MavlinkMission;
import kr.go.forest.das.R;

public class Px4 extends Drone implements MavDataManager.MavEventListener{

    public static final int MAVLINK_DEVICE_ARDUPILOT = 0x01;
    public static final int MAVLINK_DEVICE_PX4 = 0x02;
    private int mavlink_device = 0x00;

    private final String[] ARDUPILOT_FLIGHT_MODE = new String[] {
            "Stabilize",     // 0
            "Acro",          // 1
            "Altitude Hold", // 2
            "Auto",          // 3
            "Guided",        // 4
            "Loiter",        // 5
            "RTL",           // 6
            "Circle",        // 7
            "",
            "Land",          // 9
            "",
            "Drift",         // 11
            "",
            "Sport",         // 13
            "",
            "",
            "Position Hold", // 16
            "Brake",         // 17
            "Throw",         // 18
            "Avoid ADSB",    // 19
            "Guided No GPS"  // 20
    };

    private final String[] PX4_FLIGHT_MAIN_MODE = new String[] {
            "",
            "Manual",     // 1
            "Altitude",   // 2
            "Position",   // 3
            "Auto",       // 4
            "Acro",       // 5
            "Offboard",   // 6
            "Stabilized", // 7
            "Rattitude",  // 8
            "Simple"      // 9
    };

    private final String[] PX4_FLIGHT_SUB_MODE = new String[] {
            "",
            "Ready",     // 1
            "TakeOff",   // 2
            "Loiter",   // 3
            "Mission",       // 4
            "Return",       // 5
            "Land",   // 6
            "RTGS", // 7
            "FOLLOW_TARGET",  // 8
            "PRECLAND"      // 9
    };

    private boolean request_upload_mission = false;
    private int request_start_mission_count = 0;

    public Px4() {
    }

    public void setMavlinkManager(MavDataManager mdm){
        mavlink_manager = mdm;
    }

    //region 제품정보

    public DroneInfo getDroneInfo(){
        DroneInfo _drone = new DroneInfo();
        _drone.status = ((drone_status & DRONE_STATUS_DISARM) != 0) ? DRONE_STATUS_DISARM :
                ((drone_status & DRONE_STATUS_MISSION) != 0) ? DRONE_STATUS_MISSION :
                        ((drone_status & DRONE_STATUS_RETURN_HOME) != 0) ? DRONE_STATUS_RETURN_HOME :
                                ((drone_status & DRONE_STATUS_FLYING) != 0) ? DRONE_STATUS_FLYING :
                                        ((drone_status & DRONE_STATUS_ARMING) != 0) ? DRONE_STATUS_ARMING : 0;
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

        _drone.satellites_visible_count = satellites_visible_count;
        _drone.eph = eph;
        _drone.rssi = rssi;

        _drone.flight_mode = flight_mode;
        _drone.status = mavlink_device;             // FC 종류

        return _drone;
    }

    /*=============================================================*
     *  현재 드론 상태를 반환한다.
     *==============================================================*/
    public int getDroneStatus(){return drone_status;};

    /*=============================================================*
     *  현재 드론 비행여부를 확인한다.
     *==============================================================*/
    public boolean isFlying(){ return is_flying; }

    /**
     * 제조사 정보를 반환한다.
     */
    @Override
    public String getManufacturer(){
        return  "Pixhawk";
    }

    /**
     * 드롬 모델명을 반환한다.
     * @return
     */
    @Override
    public Model getAircaftModel(){
        return Model.UNKNOWN_AIRCRAFT;
    }

    /**
     * 드론 시리얼번호를 반환한다.
     * @return
     */
    @Override
    public void getSerialNumber(){
    }

    /**
     * 드론의 구성정보를 반환한다.
     */
    @Override
    public BaseProduct getProductInstance(){
        return null;
    }

    /**
     * 드론의 FlightController 정보를 설정한다.
     */
    @Override
    public boolean setDroneDataListener(){ return false;};

    public boolean removeDroneDataListener(){
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

    }

    @Override
    public void getCameraFocalLength(){}

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

    @Override
    public CameraInfo getStorageInfo() {

        return null;
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
        return null;
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
    public void getPhotoAspectRatio(){
        return ;
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
        return  null;
    }

    /**
     * 조종기와 연결이 끊어졌을 때의 동작을 설정한다.
     */
    public void setConnectionFailSafeBehavior(ConnectionFailSafeBehavior behavior){}

    /**
     * 자동이륙 명령
     */
    public void startTakeoff(){
        mavlink_manager.requestArmDisarm(1);
    }

    /**
     * 자동착륙 명령
     */
    public void startLanding(){
        mavlink_manager.requestLand();
    }

    /**
     * 자동착륙 명령 취소
     */
    public void cancelLanding(){
        mavlink_manager.requestLoiter();
    }

    /**
     * 드론 수평방향 속도값을 가져온다.
     * @return
     */
    @Override
    public float getHorizontalVelocity(){
        return  0.0f;
    }

    /**
     * 드론 수직방향 속도값을 가져온다.
     * @return
     */
    @Override
    public float getVerticalVelocity(){
        return 0.0f;
    }

    /**
     * 드론 비행시간값을 가져온다.
     * @return
     */
    @Override
    public int getFlightTimeInSeconds(){
        return 0;
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
    public ConnectionFailSafeBehavior getConnectionFailSafeBehavior() {
        return null;
    }
    //endregion

    //region 임무비행
    /**
     * 임무를 업로드 가능한지 체크
     */
    public boolean isMissionUploadAvailable(){
        return  false;
    }

    /**
     * 임무 시작이 가능한지 체크
     */
    public boolean isMissionStartAvailable(){ return true;}

    /**
     * 설정된 임무를 드론에 업로드
     */
    public String uploadMission(WaypointMission mission){

        return null;
    }

    public void mavlinkUploadMission(ArrayList<MavlinkMission> mission){
        mavlink_manager.setMavlinkMission(mission);
        mavlink_manager.requestMissionCount();
    }

    /**
     * 설정된 임무를 시작하기 위한 조건 설정
     * @param timeIntervalInSeconds : 촬영시간 간격
     */
    public void setMissionCondition(int captureCount, int timeIntervalInSeconds){

    }

    /**
     * 설정된 임무를 시작
     */
    public void startMission(int _shoot_count, int _interval) {
        if (mavlink_device == MAVLINK_DEVICE_ARDUPILOT) {
            mavlink_manager.requestSetModeArdupilot();
        }else{
            mavlink_manager.requestSetModePx4();
        }
    }

    /**
     * 설정된 임무를 멈춤
     */
    public void stopMission(){

    }

    /**
     * 드론 최대비행고도를 설정한다.
     */
    public void setMaxFlightHeight(int height){
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
    //endregion

    //region RTL
    @Override
    public boolean isHomeLocationSet(){
        return false;
    }

    @Override
    public LocationCoordinate2D getHomeLocation(){
        if(home_latitude < 1 || home_longitude < 1) return  null;
        LocationCoordinate2D _location = new LocationCoordinate2D(home_latitude, home_longitude);
        return _location;
    }

    @Override
    public GoHomeExecutionState getGoHomeExecutionState(){
        return  null;
    }

    @Override
    public void startGoHome(){
        mavlink_manager.requestReturn2Home();
    }

    @Override
    public void cancelGoHome(){
        mavlink_manager.requestLoiter();
    }

    @Override
    public void setHomeLocation(LocationCoordinate2D home){

    }
    //endregion

    //region 조종기
    public boolean isConnect(){
        return is_connect;
    }

    public void setConnect(boolean connect){
        is_connect = connect;
    }
    //endregion

    //region 짐벌

    @Override
    public void setGimbalRotate(float pitch){

    }

    @Override
    public void onReceive(Object payload, int type) {
//        int flight_time;                /** 비행시간 */

        if(payload instanceof VfrHud){
            heading = ((VfrHud) payload).heading();
            if(mavlink_device == MAVLINK_DEVICE_ARDUPILOT){
                drone_altitude = ((VfrHud) payload).alt();
            }
            velocyty_x = ((VfrHud) payload).groundspeed();
        }else if(payload instanceof LocalPositionNed){
            velocyty_x = ((LocalPositionNed) payload).vx();
            velocyty_y = ((LocalPositionNed) payload).vy();
            velocyty_z = ((LocalPositionNed) payload).vz();
            if(mavlink_device == MAVLINK_DEVICE_PX4){
                drone_altitude = ((LocalPositionNed) payload).z() * -1;
            }
        }else if(payload instanceof BatteryStatus){
            battery_temperature = ((BatteryStatus) payload).temperature();
            List<Integer> voltages = ((BatteryStatus) payload).voltages();
            battery_voltage = -1;
            battery_remain_percent = ((BatteryStatus) payload).batteryRemaining();
        }else if(payload instanceof SysStatus){
            battery_temperature = -1;
            battery_voltage = -1;
            battery_remain_percent = ((SysStatus) payload).batteryRemaining();
        }else if(payload instanceof HomePosition){
            home_latitude = ((double)((HomePosition) payload).latitude())/10000000;
            home_longitude = ((double)((HomePosition) payload).longitude())/10000000;
            home_set = true;
        }else if(payload instanceof Heartbeat) {
            EnumValue<MavAutopilot> autopilot = ((Heartbeat) payload).autopilot();
            if(autopilot.entry() == MavAutopilot.MAV_AUTOPILOT_ARDUPILOTMEGA) mavlink_device = MAVLINK_DEVICE_ARDUPILOT;
            else mavlink_device = MAVLINK_DEVICE_PX4;

            long _custom_mode = ((Heartbeat) payload).customMode();
            EnumValue<MavState> system_status = ((Heartbeat) payload).systemStatus();   // https://mavlink.io/en/messages/common.html#MAV_STATE

            if(mavlink_device == MAVLINK_DEVICE_ARDUPILOT){                             //http://ardupilot.org/copter/docs/flight-modes.html
                flight_mode = ARDUPILOT_FLIGHT_MODE[(int)_custom_mode];
            }else if(mavlink_device == MAVLINK_DEVICE_PX4){                             // https://github.com/PX4/Firmware/blob/master/src/modules/commander/px4_custom_mode.h#L45
                flight_mode = PX4_FLIGHT_MAIN_MODE[(int)((_custom_mode >> 16) & 0xFF)];
                String sub_mode = PX4_FLIGHT_SUB_MODE[(int)((_custom_mode >> 24) & 0xFF)];
                if(sub_mode.equals("Mission")) flight_mode = sub_mode;
                else if(sub_mode.equals("Return")) {
                    flight_mode = sub_mode;
                }
                else if(sub_mode.equals("TakeOff")) {
                    flight_mode = sub_mode;
                }else if(sub_mode.equals("Land")) {
                    flight_mode = sub_mode;
                }
            }

            EnumValue<MavModeFlag> base_mode = ((Heartbeat) payload).baseMode();
            if((base_mode.value() & 0x80) != 0){
                flight_mode = flight_mode + "(Arm)";
                is_flying = true;
            }else{
                flight_mode = flight_mode + "(Disarm)";
                is_flying = false;
            }
        }else if(payload instanceof ManualControl){
            left_stick_x = ((ManualControl) payload).r();
            left_stick_y = ((ManualControl) payload).z();
            right_stick_x = ((ManualControl) payload).x();
            right_stick_y = ((ManualControl) payload).y();

        }else if(payload instanceof ExtendedSysState){
            EnumValue<MavLandedState> landed_state = ((ExtendedSysState) payload).landedState();
            if(landed_state.entry().equals(MavLandedState.MAV_LANDED_STATE_IN_AIR)
              || landed_state.entry().equals(MavLandedState.MAV_LANDED_STATE_TAKEOFF)
              || landed_state.entry().equals(MavLandedState.MAV_LANDED_STATE_LANDING)){
                is_flying = true;
            }else{
                is_flying = false;
            }
        }else if(payload instanceof GpsRawInt){
            EnumValue<GpsFixType> fix_type = ((GpsRawInt) payload).fixType();
            if(fix_type.entry() == GpsFixType.GPS_FIX_TYPE_NO_GPS){
                satellites_visible_count = -1;
            }else if(fix_type.entry() == GpsFixType.GPS_FIX_TYPE_NO_GPS){
                satellites_visible_count = 0;
                drone_latitude = 0.0;
                drone_longitude = 0.0;
            }else{
                satellites_visible_count = ((GpsRawInt) payload).satellitesVisible();
                eph = ((float)((GpsRawInt) payload).eph())/100;
                drone_latitude = ((double)((GpsRawInt) payload).lat())/10000000;
                drone_longitude = ((double)((GpsRawInt) payload).lon())/10000000;
            }

        }else if(payload instanceof GlobalPositionInt){
            drone_latitude = ((double)((GlobalPositionInt) payload).lat())/10000000;
            drone_longitude = ((double)((GlobalPositionInt) payload).lon())/10000000;
        }else if(payload instanceof AutopilotVersion){

        }else if(payload instanceof RcChannels){
            rssi = ((RcChannels) payload).rssi();
        }else if(payload instanceof Attitude){
            drone_roll = ((Attitude) payload).roll();
            drone_pitch = ((Attitude) payload).pitch();
            drone_yaw = ((Attitude) payload).yaw();
        }else if(payload instanceof MissionRequestInt){
            int seq = ((MissionRequestInt) payload).seq();
            mavlink_manager.requestMissionItem(seq);

        }else if(payload instanceof MissionRequest){
            int seq = ((MissionRequest) payload).seq();
            mavlink_manager.requestMissionItem(seq);

        }else if(payload instanceof Statustext){
            EnumValue<MavSeverity> serverity = ((Statustext) payload).severity();
            if(serverity.value() < 5) DroneApplication.getEventBus().post(new MainActivity.TTS(((Statustext) payload).text()));
        }else if(payload instanceof MissionAck){

            EnumValue<MavMissionResult> result = ((MissionAck) payload).type();
            EnumValue<MavMissionType> _type = ((MissionAck) payload).missionType();
            if(_type == null) {
                Log.e("MissionAck", "type : null");
            }
            else Log.e("MissionAck", "type : " + _type.value());
            if(_type != null && _type.entry() == MavMissionType.MAV_MISSION_TYPE_ALL){
                if(result.entry() == MavMissionResult.MAV_MISSION_ACCEPTED){
                    mavlink_manager.requestMissionCount();
                }else{
                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_UPLOAD_FAIL, null));
                }
            }else{
                Log.e("MissionAck", payload.toString());
                if(result == null || result.value() < 1){
                    request_upload_mission = true;
                } else {
                    DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_UPLOAD_FAIL, null));
                }
            }

        }else if(payload instanceof MissionCurrent){
            if(request_upload_mission == true){
                DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_UPLOAD_SUCCESS, null));
                request_upload_mission = false;
            }
        }else if(payload instanceof CommandAck){
            EnumValue<MavCmd> command = ((CommandAck) payload).command();
            EnumValue<MavResult> result = ((CommandAck) payload).result();
            //Log.e("Command Ack", payload.toString());
            if(command.entry() == MavCmd.MAV_CMD_NAV_LAND){
                if(result.entry() == MavResult.MAV_RESULT_ACCEPTED){
                    // 착륙 명령 성공
                    DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_LANDING_SUCCESS, null));
                }
            }else if(command.entry() == MavCmd.MAV_CMD_NAV_TAKEOFF){
                if(result.entry() == MavResult.MAV_RESULT_ACCEPTED){
                    // 이륙 명령 성공
                    DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_TAKEOFF_SUCCESS, null));
                }
            }else if(command.entry() == MavCmd.MAV_CMD_NAV_RETURN_TO_LAUNCH){
                if(result.entry() == MavResult.MAV_RESULT_ACCEPTED){
                    // 귀환 명령 성공
                    DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.REQUEST_RETURN_HOME_SUCCESS, null));
                }
            }else if(command.entry() == MavCmd.MAV_CMD_COMPONENT_ARM_DISARM ){
                if(result.entry() == MavResult.MAV_RESULT_ACCEPTED){
                    // Arm/Disarm 명령 성공
                    mavlink_manager.requestTakeOff();
                }else if(result.entry() == MavResult.MAV_RESULT_TEMPORARILY_REJECTED){
                    // Arm/Disarm 명령 현재 실행할 수 없음
                }
            }else if(command.entry() == MavCmd.MAV_CMD_DO_REPOSITION){
                if(result.entry() == MavResult.MAV_RESULT_ACCEPTED){
                    // 정지 명령 성공
                    DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.ReturnHome.CANCEL_RETURN_HOME_SUCCESS, null));
                }
            }else if(command.entry() == MavCmd.MAV_CMD_DO_SET_MODE){
                if(result.entry() == MavResult.MAV_RESULT_ACCEPTED){
                    // 모드 변경 성공
                    DroneApplication.getEventBus().post(new MainActivity.ReturnHome(MainActivity.Mission.MISSION_START_SUCCESS, null));
                }else {
                    // 재요청
                    if(request_start_mission_count < 2) {
                        startMission(0, 0);
                        request_start_mission_count++;
                    }
                    else {
                        DroneApplication.getEventBus().post(new MainActivity.Mission(MainActivity.Mission.MISSION_START_FAIL, null));
                        request_start_mission_count = 0;
                    }
                }
            }
        }
    }
    //endregion
}
