package kr.go.forest.das.video;

import android.content.SharedPreferences;

import com.wowza.gocoder.sdk.api.configuration.WOWZMediaConfig;
import com.wowza.gocoder.sdk.api.configuration.WOWZStreamConfig;
import com.wowza.gocoder.sdk.api.configuration.WowzaConfig;
import com.wowza.gocoder.sdk.api.h264.WOWZProfileLevel;

public class GoCoderSDKPrefs {
    private final static String TAG = GoCoderSDKPrefs.class.getSimpleName();

    private static String getPrefString(SharedPreferences sharedPrefs, String key, String defaultValue){
        String value = sharedPrefs.getString(key, defaultValue);
        if(value.isEmpty()){
            return defaultValue;
        }
        return value;
    }
    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WOWZMediaConfig mediaConfig) {
        // video settings
        mediaConfig.setVideoEnabled(sharedPrefs.getBoolean("wz_video_enabled", true));

        mediaConfig.setVideoFrameWidth(sharedPrefs.getInt("wz_video_frame_width", WOWZMediaConfig.DEFAULT_VIDEO_FRAME_WIDTH));
        mediaConfig.setVideoFrameHeight(sharedPrefs.getInt("wz_video_frame_height", WOWZMediaConfig.DEFAULT_VIDEO_FRAME_HEIGHT));

        String fps = getPrefString(sharedPrefs,"wz_video_framerate", String.valueOf(WOWZMediaConfig.DEFAULT_VIDEO_FRAME_RATE));
        mediaConfig.setVideoFramerate(Integer.parseInt(fps));

        mediaConfig.setVideoKeyFrameInterval(Integer.parseInt(getPrefString(sharedPrefs,"wz_video_keyframe_interval", String.valueOf(WOWZMediaConfig.DEFAULT_VIDEO_KEYFRAME_INTERVAL))));
        mediaConfig.setVideoBitRate(Integer.parseInt(getPrefString(sharedPrefs,"wz_video_bitrate", String.valueOf(WOWZMediaConfig.DEFAULT_VIDEO_BITRATE))));
        mediaConfig.setABREnabled(sharedPrefs.getBoolean("wz_video_use_abr", true));
        mediaConfig.setHLSEnabled(sharedPrefs.getBoolean("wz_use_hls", false));
        mediaConfig.setHLSBackupURL(sharedPrefs.getString("wz_hls_failover", null));

        int profile = sharedPrefs.getInt("wz_video_profile_level_profile", -1);
        int level = sharedPrefs.getInt("wz_video_profile_level_level", -1);
        if (profile != -1 && level != -1) {
            WOWZProfileLevel profileLevel = new WOWZProfileLevel(profile, level);
            if (profileLevel.validate()) {
                mediaConfig.setVideoProfileLevel(profileLevel);
            }
        } else {
            mediaConfig.setVideoProfileLevel(null);
        }

        // audio settings
        mediaConfig.setAudioEnabled(sharedPrefs.getBoolean("wz_audio_enabled", true));

        mediaConfig.setAudioSampleRate(Integer.parseInt(getPrefString(sharedPrefs,"wz_audio_samplerate", String.valueOf(WOWZMediaConfig.DEFAULT_AUDIO_SAMPLE_RATE))));
        mediaConfig.setAudioChannels(sharedPrefs.getBoolean("wz_audio_stereo", true) ? WOWZMediaConfig.AUDIO_CHANNELS_STEREO : WOWZMediaConfig.AUDIO_CHANNELS_MONO);
        mediaConfig.setAudioBitRate(Integer.parseInt(getPrefString(sharedPrefs,"wz_audio_bitrate", String.valueOf(WOWZMediaConfig.DEFAULT_AUDIO_BITRATE))));
    }

    public static void updateConfigFromPrefsForPlayer(SharedPreferences sharedPrefs, WOWZStreamConfig streamConfig) {
        // connection settings
        streamConfig.setHostAddress(sharedPrefs.getString("wz_live_host_address", null));
        String portNumber = sharedPrefs.getString("wz_live_port_number", String.valueOf(WowzaConfig.DEFAULT_PORT));
        if(portNumber!="") {
            streamConfig.setPortNumber(Integer.parseInt(portNumber));
        }
        //streamConfig.setUseSSL(sharedPrefs.getBoolean("wz_live_use_ssl", false));
        streamConfig.setApplicationName(sharedPrefs.getString("wz_live_app_name", WowzaConfig.DEFAULT_APP));
        streamConfig.setStreamName(sharedPrefs.getString("wz_live_stream_name", WowzaConfig.DEFAULT_STREAM));
        streamConfig.setUsername(sharedPrefs.getString("wz_live_username", null));
        streamConfig.setPassword(sharedPrefs.getString("wz_live_password", null));
        streamConfig.setIsPlayback(true);

        updateConfigFromPrefs(sharedPrefs, (WOWZMediaConfig) streamConfig);
    }

    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WOWZStreamConfig streamConfig) {
        // connection settings
        streamConfig.setHostAddress(sharedPrefs.getString("wz_live_host_address", null));
        streamConfig.setPortNumber(Integer.parseInt(sharedPrefs.getString("wz_live_port_number", String.valueOf(WowzaConfig.DEFAULT_PORT))));
        //streamConfig.setUseSSL(sharedPrefs.getBoolean("wz_live_use_ssl", false));
        streamConfig.setApplicationName(sharedPrefs.getString("wz_live_app_name", WowzaConfig.DEFAULT_APP));
        streamConfig.setStreamName(sharedPrefs.getString("wz_live_stream_name", WowzaConfig.DEFAULT_STREAM));
        streamConfig.setUsername(sharedPrefs.getString("wz_live_username", null));
        streamConfig.setPassword(sharedPrefs.getString("wz_live_password", null));

        streamConfig.setIsPlayback(false);
        updateConfigFromPrefs(sharedPrefs, (WOWZMediaConfig) streamConfig);
    }

    public static void updateConfigFromPrefs(SharedPreferences sharedPrefs, WowzaConfig wowzaConfig) {
        // WowzaConfig-specific properties
        wowzaConfig.setCapturedVideoRotates(sharedPrefs.getBoolean("wz_captured_video_rotates", true));

        updateConfigFromPrefs(sharedPrefs, (WOWZStreamConfig) wowzaConfig);
    }

    public static int getScaleMode(SharedPreferences sharedPrefs) {
        return sharedPrefs.getBoolean("wz_video_resize_to_aspect", false) ? WOWZMediaConfig.RESIZE_TO_ASPECT : WOWZMediaConfig.FILL_VIEW;
    }

    public static float getPreBufferDuration(SharedPreferences sharedPrefs) {
        try {
            return Float.parseFloat(sharedPrefs.getString("wz_video_player_prebuffer_duration", "0"));
        } catch (Exception e) {
            return 0f;
        }
    }
}
