package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;

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

        switch (v.getId())
        {
            case R.id.missionButton:
                ViewWrapper wrapper = new ViewWrapper(new MissionView(context), true);
                DroneApplication.getEventBus().post(wrapper);
                break;
            case R.id.flightButton:

                break;
            case R.id.settingButton:

                break;
        }
    }
}