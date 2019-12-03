package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.otto.Subscribe;

import org.osmdroid.util.GeoPoint;

import java.util.Timer;
import java.util.TimerTask;

import dji.common.model.LocationCoordinate2D;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.Model.CameraInfo;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.Model.WeatherInfo;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;
import kr.go.forest.das.geo.GeoManager;
import kr.go.forest.das.map.MapLayer;
import kr.go.forest.das.network.NetworkStatus;

public class MenuView extends RelativeLayout implements View.OnClickListener {

    private final int period = 500;                             // 위치정보 수십 주기 0.5 second

    private Context context;
    private Handler handler_ui;                                 // UI 업데이트 핸들러

    Timer timer = null;

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
            // 위치확인 타이머 시작
            timer = new Timer();
            timer.schedule(new MenuView.CollectDroneInformationTimer(), 0, period);

            // 현재 위치정보 값...
            ((TextView)findViewById(R.id.textview_location)).setText("위치 확인중");
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


    @Subscribe
    public void onUpdateLocation(final MainActivity.LocationUpdate location) {
        if (handler_ui != null) {
            handler_ui.post(new Runnable() {
                @Override
                public void run() {

                }
            });
        }
    }

    /**
     * 모바일 장치의 위치 정보를 확인 한다.
     */
    private class CollectDroneInformationTimer extends TimerTask {
        @Override
        public void run() {
            // 화면 표시
            if (handler_ui != null) {
                handler_ui.post(new Runnable() {
                    @Override
                    public void run() {
                        BigdataSystemInfo _info = DroneApplication.getSystemInfo();

                        if (_info.my_location != null) {
                            WeatherInfo weather = _info.getWeatherInfo(_info.my_location.getLatitude(), _info.my_location.getLongitude());

                            if(weather != null) {
                                findViewById(R.id.menu_imageview_temp).setVisibility(VISIBLE);
                                findViewById(R.id.menu_texview_temp).setVisibility(VISIBLE);

                                findViewById(R.id.menu_imageview_wind).setVisibility(VISIBLE);
                                findViewById(R.id.menu_textview_wind).setVisibility(VISIBLE);

                                if (weather.lgdngNm != null)
                                    ((TextView) findViewById(R.id.textview_location)).setText(weather.wtherObsrrNm + "(" + weather.lgdngNm + ")");
                                else
                                    ((TextView) findViewById(R.id.textview_location)).setText(weather.wtherObsrrNm);
                                ((TextView) findViewById(R.id.textview_temperature)).setText(weather.airTemp + " \u2103");
                                ((TextView) findViewById(R.id.textview_wind_speed)).setText(String.valueOf(weather.windSpeed) + " m/s");
                            }
                            if(timer != null) {
                                timer.cancel();
                                timer = null;
                            }
                        }
                    }
                });
            }
        }
    }
}