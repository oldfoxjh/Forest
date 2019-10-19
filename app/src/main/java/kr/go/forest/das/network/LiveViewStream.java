package kr.go.forest.das.network;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.sdkmanager.LiveStreamManager;

public class LiveViewStream {
    private LiveStreamManager.OnLiveChangeListener listener;
    private LiveStreamManager.LiveStreamVideoSource currentVideoSource = LiveStreamManager.LiveStreamVideoSource.Primary;

    private String url = null;

    public LiveViewStream(String live_url) {
        url = live_url;
    }

    private void initListener(){
        listener = new LiveStreamManager.OnLiveChangeListener() {
            @Override
            public void onStatusChanged(int i) {

            }
        };
    }

    public void startLiveViewStream(){
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            //ToastUtils.setResultToToast("No live stream manager!");
            return;
        }

        if (DJISDKManager.getInstance().getLiveStreamManager().isStreaming()) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                DJISDKManager.getInstance().getLiveStreamManager().setLiveUrl(url);
                int result = DJISDKManager.getInstance().getLiveStreamManager().startStream();
                DJISDKManager.getInstance().getLiveStreamManager().setStartTime();
            }
        }.start();
    }

    public void stopLiveShow() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().stopStream();
    }

    public void soundOn() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().setAudioMuted(false);
    }

    public void soundOff() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            return;
        }
        DJISDKManager.getInstance().getLiveStreamManager().setAudioMuted(true);
    }

    private void showLiveStartTime() {
        if (DJISDKManager.getInstance().getLiveStreamManager() == null) {
            return;
        }
        if (!DJISDKManager.getInstance().getLiveStreamManager().isStreaming()){
            return;
        }
        long startTime = DJISDKManager.getInstance().getLiveStreamManager().getStartTime();
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        //String sd = sdf.format(new Date(Long.parseLong(String.valueOf(startTime))));
        //ToastUtils.setResultToToast("Live Start Time: " + sd);
    }
}
