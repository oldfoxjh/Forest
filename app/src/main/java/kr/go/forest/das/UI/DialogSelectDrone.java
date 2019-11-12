package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import dji.common.flightcontroller.ConnectionFailSafeBehavior;
import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.Model.FlightPlan;
import kr.go.forest.das.Model.LoginResponse;
import kr.go.forest.das.R;

public class DialogSelectDrone extends RelativeLayout implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private Context context;

    public DialogSelectDrone(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public DialogSelectDrone(Context context, AttributeSet attrs) {
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
        layoutInflater.inflate(R.layout.dialog_select_drone, this, true);

        RadioGroup _drone_list = findViewById(R.id.select_drone_radio_group);
        _drone_list.setOnCheckedChangeListener(this);
        findViewById(R.id.btn_dialog_select_drone_ok).setOnClickListener(this);

        // 비행계획 설정하기
        BigdataSystemInfo _info = DroneApplication.getSystemInfo();
        for(int i = 0; i < _info.drone_list.size(); i++) {
            LoginResponse.DroneList _drone = _info.drone_list.get(i);
            RadioButton _radio_button = new RadioButton(context);
            _radio_button.setTag(i);
            _radio_button.setText(_drone.manage_number + "/" + _drone.model);
            _radio_button.setTextSize(getResources().getDimension(R.dimen.radio_font));
            _drone_list.addView(_radio_button);

            if(i == 0) _drone_list.check(_radio_button.getId());
        }

        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        DroneApplication.getEventBus().post(new MainActivity.PopdownView());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        RadioButton _radio_button = group.findViewById(checkedId);

        // 선택된 임무 정보 세팅하기
        BigdataSystemInfo _info = DroneApplication.getSystemInfo();
        _info.drone_index = (int)_radio_button.getTag();
    }
}
