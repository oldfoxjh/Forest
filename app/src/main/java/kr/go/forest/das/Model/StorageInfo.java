package kr.go.forest.das.Model;

public class StorageInfo {
    public boolean inserted = false;
    public int remain_storage = 0;
    public String capture_count = null;
    public String recording_remain_time = null;
    public String photo_file_format = null;
    public String video_resolution_framerate = null;
    public String recording_time = null;

    public String camera_aperture;
    public String camera_shutter;
    public  String camera_iso;
    public String camera_exposure;
    public String camera_whitebalance;
    public Boolean camera_ae_lock;
    public Boolean is_camera_auto_exposure_unlock_enabled;
}
