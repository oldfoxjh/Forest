package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.drone.Px4;

public class MenuView extends RelativeLayout implements View.OnClickListener {

    private Context context;
    private Button mBtnMission;
    private Button mBtnFlight;
    private Button mBtnSetting;

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

        mBtnMission = (Button)findViewById(R.id.missionButton);
        mBtnMission.setOnClickListener(this);
        mBtnFlight = (Button)findViewById(R.id.flightButton);
        mBtnFlight.setOnClickListener(this);
        mBtnSetting = (Button)findViewById(R.id.settingButton);
        mBtnSetting.setOnClickListener(this);

        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        if(DroneApplication.getDroneInstance() == null) DroneApplication.setDroneInstance(Drone.DRONE_MANUFACTURE_PIXHWAK);
        //연결된 드론이 없을 경우 연결 요청
        if(DroneApplication.getDroneInstance() == null){
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
                wrapper = null; return;
                //break;
        }

        DroneApplication.getEventBus().post(wrapper);
    }
}