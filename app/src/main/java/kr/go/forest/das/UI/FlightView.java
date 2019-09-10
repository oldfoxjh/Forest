package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.Timer;
import java.util.TimerTask;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;

public class FlightView extends RelativeLayout implements View.OnClickListener{

    private final String TAG = "FlightView";
    private final int period = 500;             // 0.5 second
    private Context context;
    Timer timer;
    private Handler handler_ui;

    Button btn_upload;
    Button btn_back;

    TextView tv_distance;
    TextView tv_altitude;
    TextView tv_horizontal_speed;
    TextView tv_vertical_speed;

    public FlightView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public FlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {
        DroneApplication.getEventBus().register(this);
        handler_ui = new Handler(Looper.getMainLooper());
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    protected void initUI() {
        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        setSystemUiVisibility(UI_OPTIONS);

        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.content_flight, this, true);

        setWidget();

         timer = new Timer();

         // 드론 상태 체크
        if(DroneApplication.getDroneInstance() != null) {
            int _drone_status = DroneApplication.getDroneInstance().getDroneStatus();
            LogWrapper.i(TAG, "drone_status : " + _drone_status);
            if (((_drone_status & Drone.DRONE_STATUS_ARMING) > 0)
                    || ((_drone_status & Drone.DRONE_STATUS_FLYING) > 0)) {
                timer.schedule(new CollectDroneInformationTimer(), 0, period); // 0.5초 단위로 설정
            }
        }
    }

    private void setWidget() {
        // Button
        btn_back = (Button) findViewById(R.id.btn_mission_back);
        btn_back.setOnClickListener(this);

        btn_upload = (Button) findViewById(R.id.btn_mission_upload);
        btn_upload.setVisibility(INVISIBLE);

        // 드론 정보
        tv_distance = (TextView) findViewById(R.id.tv_flight_distance_from_home);
        tv_altitude = (TextView) findViewById(R.id.tv_flight_altitude);
        tv_horizontal_speed = (TextView) findViewById(R.id.tv_flight_horizontal_speed);
        tv_vertical_speed = (TextView) findViewById(R.id.tv_flight_vertical_speed);
    }

        @Override
    public void onClick(View v) {

    }

    /**
     * 시동 걸리고 0.5초 단위로 기체정보 수집
     */
    private class CollectDroneInformationTimer extends TimerTask
    {
        @Override
        public void run() {
            // 드론 상태 체크
            int _drone_status = DroneApplication.getDroneInstance().getDroneStatus();

            // DaraArray 추가

            // Log 추가

            // 화면 표시
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        DroneInfo _info = DroneApplication.getDroneInstance().getDroneInfo();
                        // 1. 조종기 or 핸드폰과의  거리
                       // tv_distance.setText(String.format("%.2f", _info.drone_distance));
                        // 2. 고도
                        tv_altitude.setText(String.format("%.2f", _info.drone_altitude));
                        // 3. 수평속도
                        tv_altitude.setText(String.format("%.2f", _info.drone_velocity_x));
                        // 4. 수직속도
                        tv_altitude.setText(String.format("%.2f", _info.drone_velocity_z));

                        LogWrapper.i(TAG, "alt : " + _info.drone_altitude);
                    }
                });
            }

        }
    }

    @Subscribe
    public void onConnectionChange(final MainActivity.DroneStatusionChange drone_status) {
        if(drone_status.status == Drone.DRONE_STATUS_CONNECT) {
            LogWrapper.i(TAG, "DRONE_STATUS_CONNECT");
        }else if(drone_status.status == Drone.DRONE_STATUS_ARMING){
            // Data 수집 타이머 시작
            timer.schedule(new CollectDroneInformationTimer(), 0, period); // 0.5초 단위로 설정
            LogWrapper.i(TAG, "DRONE_STATUS_ARMING");
        }else if(drone_status.status == Drone.DRONE_STATUS_FLYING){

            LogWrapper.i(TAG, "DRONE_STATUS_FLYING");
        }else if(drone_status.status == Drone.DRONE_STATUS_MISSION){
            LogWrapper.i(TAG, "DRONE_STATUS_MISSION");
        }else if(drone_status.status == Drone.DRONE_STATUS_RETURN_HOME){
            LogWrapper.i(TAG, "DRONE_STATUS_RETURN_HOME");
        }else if(drone_status.status == Drone.DRONE_STATUS_DISARM){
            // Data 수집 타이머 종료
            LogWrapper.i(TAG, "DRONE_STATUS_LANDING");
            timer.cancel();
        }else if(drone_status.status == Drone.DRONE_STATUS_DISCONNECT){
            // Data 수집 타이머 종료
            LogWrapper.i(TAG, "DRONE_STATUS_DISCONNECT");
            timer.cancel();
        }
    }
}
