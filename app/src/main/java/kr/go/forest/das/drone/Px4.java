package kr.go.forest.das.drone;


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
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DroneInfo;

public class Px4 extends Drone {

    public Px4()
    {
    }

    //region 제품정보

    public DroneInfo getDroneInfo(){return null;};

    /*=============================================================*
     *  현재 드론 상태를 반환한다.
     *==============================================================*/
    public int getDroneStatus(){return drone_status;};

    /*=============================================================*
     *  현재 드론 비행여부를 확인한다.
     *==============================================================*/
    public boolean isFlying(){ return false; }

    /**
     * 제조사 정보를 반환한다.
     */
    @Override
    public String getManufacturer(){
        return  null;
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

    }

    /**
     * 자동착륙 명령
     */
    public void startLanding(){
    }

    /**
     * 자동착륙 명령 취소
     */
    public void cancelLanding(){

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
     * 설정된 임무를 드론에 업로드
     */
    public String uploadMission(WaypointMission mission){
        return null;
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
    public void startMission(){

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
    //endregion

    //region RTL
    @Override
    public boolean isHomeLocationSet(){
        return false;
    }

    @Override
    public LocationCoordinate2D getHomeLocation(){
        return null;
    }

    @Override
    public GoHomeExecutionState getGoHomeExecutionState(){
        return  null;
    }

    @Override
    public void startGoHome(){

    }

    @Override
    public void cancelGoHome(){

    }

    @Override
    public void setHomeLocation(LocationCoordinate2D home){

    }
    //endregion

    //region 조종기

    //endregion

    //region 짐벌

    @Override
    public void setGimbalRotate(float pitch){

    }
    //endregion
}
