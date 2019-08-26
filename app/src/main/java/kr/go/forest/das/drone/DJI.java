package kr.go.forest.das.drone;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.flightcontroller.BatteryThresholdBehavior;
import dji.common.flightcontroller.FlightMode;
import dji.common.flightcontroller.GPSSignalLevel;
import dji.common.flightcontroller.GoHomeExecutionState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.gimbal.GimbalState;
import dji.common.model.LocationCoordinate2D;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import kr.go.forest.das.DroneApplication;

public class DJI extends Drone{

    private static final String TAG = "DJI Drone";
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {
        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
        }
    };

    //region 제품정보
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
    public String getAircaftModel(){
        return  null;
    }

    /**
     * 드론 시리얼번호를 반환한다.
     * @return
     */
    @Override
    public String getSerialNumber(){
        return null;
    }

    /**
     * 드론의 구성정보를 반환한다.
     */
    @Override
    public synchronized BaseProduct getProductInstance() {
        return DJISDKManager.getInstance().getProduct();
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
     * 드론의 위도,경도,고도값을 반환한다.
     * @return
     */
    @Override
    public LocationCoordinate3D getAircraftLocation(){
        return null;
    }

    /**
     * 드론 수평방향 속도값을 가져온다.
     * @return
     */
    @Override
    public float getVelocityX(){
        return  0.0f;
    }

    /**
     * 드론 수직방향 속도값을 가져온다.
     * @return
     */
    @Override
    public float getVelocityZ(){
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
    public void setHomeLocation(){

    }
    //endregion

    //region 조종기

    //endregion

    //region 짐벌
    @Override
    public void setGimbalStateCallback(GimbalState.Callback callback){

    }

    @Override
    public void setGimbalRotate(float yaw, float pitch, float roll){

    }
    //endregion

    public  void startSDKRegistration(){
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(DroneApplication.getInstance().getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                        @Override
                        public void onRegister(DJIError djiError) {
                            if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                                DJISDKManager.getInstance().startConnectionToProduct();
                            } else {
                                //ToastUtils.setResultToToast(MainActivity.this.getString(R.string.sdk_registration_message) + djiError.getDescription());
                            }
                        }

                        @Override
                        public void onProductDisconnect() {
                            //Log.d(TAG, "onProductDisconnect");
                        }

                        @Override
                        public void onProductConnect(BaseProduct baseProduct) {
                            //Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                        }

                        @Override
                        public void onComponentChange(BaseProduct.ComponentKey componentKey,
                                                      BaseComponent oldComponent,
                                                      BaseComponent newComponent) {
                            if (newComponent != null) {
                                newComponent.setComponentListener(mDJIComponentListener);
                            }
                        }

                        @Override
                        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                        }
                    });
                }
            });
        }
    }


}
