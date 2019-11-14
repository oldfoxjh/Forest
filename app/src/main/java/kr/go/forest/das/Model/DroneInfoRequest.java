package kr.go.forest.das.Model;

import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kr.go.forest.das.DroneApplication;

public class DroneInfoRequest {

    public String mobile_device_id;         // 전송기기 ID
    public String sn;                       // SerialNumber
    public String imei;                     // IMEI
    public String time;                     // 전송시간
    public String drone_id;                 // 드론 등록정보
    public String user_id;                  // 사용자 ID

    public List<DroneInfo> drone_infos;     // 드론 위치 및 상태 정보

    public DroneInfoRequest(BigdataSystemInfo system_info, List<DroneInfo> infos){
        mobile_device_id = system_info.mobile_device_id;
        sn = system_info.sn;
        imei = system_info.imei;
        drone_id = system_info.drone_id;
        user_id = system_info.user_id;
        drone_infos = (infos == null) ? null : infos.subList(0, infos.size()-1);
    }

    public void save_flight_log() {
        String LOG_DIRECTORY = "DroneAppService/Flight_Log";
        String LOG_FILE_NAME = "비행기록.json";

        // 저장할  Directory 체크 - 오늘 날짜
        Date _date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        _date.setTime(System.currentTimeMillis());
        StringBuilder _sb_date = new StringBuilder(80);
        _sb_date.append(formatter.format(_date));
        String _folder_path = Environment.getExternalStorageDirectory() + File.separator + LOG_DIRECTORY + File.separator + _sb_date.toString();
        File folder = new File(_folder_path);

        if(!folder.exists()) {
            folder.mkdirs();
        }
        try {
            //파일 output stream 생성
            formatter = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            StringBuilder _sb_datetime = new StringBuilder(80);
            _sb_datetime.append(formatter.format(_date));
            FileOutputStream fos = new FileOutputStream(_folder_path + File.separator + _sb_datetime.toString() + "_" + LOG_FILE_NAME, true);
            //파일쓰기
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));

            BigdataSystemInfo _info = DroneApplication.getSystemInfo();

            Gson gson = new Gson();
            String json = AES256.encode(gson.toJson(this), _info.aes_key);
            Log.d("DroneInfoRequest", json);
            writer.write(json);
            writer.flush();

            writer.close();
            fos.close();
        }catch (Exception e){

        }
    }
}
