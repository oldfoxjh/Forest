package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.osmdroid.util.GeoPoint;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;

public class MenuView extends RelativeLayout implements View.OnClickListener {

    private Context context;
    private Handler handler_ui;                                             // UI 업데이트 핸들러

    Button missionButton;
    Button flightButton;
    Button settingButton;

    public MenuView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public MenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {

        handler_ui = new Handler(Looper.getMainLooper());

        // LoginView Pop
        DroneApplication.getEventBus().post(new MainActivity.PopdownView(0, MainActivity.PopupDialog.REMOVE_PRE_VIEW, null));

        // 로그인 되었을 경우 드론 목록 팝업
        if(DroneApplication.getSystemInfo().isLogin()){
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_SELECT_DRONE, 0, 0));
        }

        // 드론이 연결되어 있으면 버튼 처리
        if(DroneApplication.getDroneInstance() != null){
            missionButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_mission_connect_selector));
            flightButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_flight_connect_selector));
            settingButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_setting_connect_selector));
        }

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        DroneApplication.getEventBus().unregister(this);
        handler_ui.removeCallbacksAndMessages(null);
        handler_ui = null;

        super.onDetachedFromWindow();
    }

    protected void initUI(){
        DroneApplication.getEventBus().register(this);

        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.content_menu, this, true);

        missionButton = findViewById(R.id.missionButton);
        missionButton.setOnClickListener(this);
        flightButton = findViewById(R.id.flightButton);
        flightButton.setOnClickListener(this);
        settingButton = findViewById(R.id.settingButton);
        settingButton.setOnClickListener(this);

        BigdataSystemInfo _info = DroneApplication.getSystemInfo();

        if(!_info.isLogin()){
            findViewById(R.id.propertiesLayout).setVisibility(GONE);
        }else{
            ((TextView)findViewById(R.id.textview_location)).setText(_info.weather.locale);
            ((TextView)findViewById(R.id.textview_weather)).setText(_info.weather.weather);
            ((TextView)findViewById(R.id.textview_temperature)).setText(_info.weather.temperature + " \u2103");
            ((TextView)findViewById(R.id.textview_wind_speed)).setText(String.valueOf(_info.weather.wind_speed) + " m/s");
        }


        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        //연결된 드론이 없을 경우 연결 요청
        if(DroneApplication.getDroneInstance() == null || DroneApplication.getDroneInstance().isConnect() == false){
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.check_drone_connection));
            return;
        }

        ViewWrapper wrapper = null;
        switch (v.getId())
        {
            case R.id.missionButton:
                if(DroneApplication.getDroneInstance().getManufacturer().equals("DJI")) {
                    wrapper = new ViewWrapper(new MissionView(context), false);
                }else{
                    wrapper = new ViewWrapper(new PixhawkMissionView(context), false);
                }
                break;
            case R.id.flightButton:
                if(DroneApplication.getDroneInstance().getManufacturer().equals("DJI")) {
                    wrapper = new ViewWrapper(new FlightView(context), false);
                }else{
                    wrapper = new ViewWrapper(new PixhawkFlightView(context), false);
                }
                break;
            case R.id.settingButton:
                wrapper = new ViewWrapper(new SettingView(context), false);
                break;
        }

        DroneApplication.getEventBus().post(wrapper);
    }

    /**
     * 드론 연결 정보
     * @param drone 드론 연결 정보
     */
    @Subscribe
    public void onDroneConnectChange(final MainActivity.DroneConnect drone) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {
                    if(drone.isConnect() == true){
                        missionButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_mission_connect_selector));
                        flightButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_flight_connect_selector));
                        settingButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_setting_connect_selector));
                    }else{
                        missionButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_mission_selector));
                        flightButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_flight_selector));
                        settingButton.setBackground(ContextCompat.getDrawable(context, R.drawable.menu_setting_selector));
                    }
                }
            });
        }
    }
}