package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;

public class MenuView extends RelativeLayout implements View.OnClickListener {

    private Context context;
    LinearLayout weather_info;

    TextView textview_location;
    TextView textview_weather;
    TextView textview_temperature;
    TextView textview_wind_speed;

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
        // LoginView Pop
        DroneApplication.getEventBus().post(new MainActivity.PopdownView(0, MainActivity.PopupDialog.REMOVE_PRE_VIEW, null));

        // 로그인 되었을 경우 드론 목록 팝업
        if(DroneApplication.getSystemInfo().isLogin()){
            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_SELECT_DRONE, 0, 0));
        }
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void initUI(){
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.content_menu, this, true);

        findViewById(R.id.missionButton).setOnClickListener(this);
        findViewById(R.id.flightButton).setOnClickListener(this);
        findViewById(R.id.settingButton).setOnClickListener(this);

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
//        if(DroneApplication.getDroneInstance() == null){
//            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.check_drone_connection));
//            return;
//        }

        ViewWrapper wrapper = null;
        switch (v.getId())
        {
            case R.id.missionButton:
                //if(DroneApplication.getDroneInstance().getManufacturer().equals("DJI")) {
                    wrapper = new ViewWrapper(new MissionView(context), false);
                //}else{
                //    wrapper = new ViewWrapper(new PixhawkMissionView(context), false);
                //}
                break;
            case R.id.flightButton:
               // if(DroneApplication.getDroneInstance().getManufacturer().equals("DJI")) {
                    wrapper = new ViewWrapper(new FlightView(context), false);
              //  }else{
              //      wrapper = new ViewWrapper(new PixhawkFlightView(context), false);
               // }
                break;
            case R.id.settingButton:
                wrapper = new ViewWrapper(new SettingView(context), false);
                break;
        }

        DroneApplication.getEventBus().post(wrapper);
    }
}