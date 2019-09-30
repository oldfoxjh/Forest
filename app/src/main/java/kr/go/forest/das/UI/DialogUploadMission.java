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
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;

public class DialogUploadMission extends RelativeLayout implements View.OnClickListener{

    private Context context;
    RadioGroup group;
    RadioButton stop_and_rth;

    public DialogUploadMission(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public DialogUploadMission(Context context, AttributeSet attrs) {
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
        layoutInflater.inflate(R.layout.dialog_upload_mission, this, true);

        group = (RadioGroup) findViewById(R.id.dialog_mission_upload_radio_group);
        Button btn_confirm = (Button)findViewById(R.id.btn_dialog_mission_upload_confirm);
        btn_confirm.setOnClickListener(this);

        Button btn_cancel = (Button)findViewById(R.id.btn_dialog_mission_upload_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
            }
        });

        // 조종기 연결 끊겼을 경우
        // 임무 중지 후 자동복귀
        stop_and_rth = new RadioButton(context);
        stop_and_rth.setText(R.string.mission_stop_and_rth);
        stop_and_rth.setTextSize(getResources().getDimension(R.dimen.radio_font));
        group.addView(stop_and_rth);

        // 임무 완료 후 자동복귀
        RadioButton _continue = new RadioButton(context);
        _continue.setText(R.string.mission_continue);
        _continue.setTextSize(getResources().getDimension(R.dimen.radio_font));
        group.addView(_continue);

        group.check(stop_and_rth.getId());
    }

    @Override
    public void onClick(View v) {
        ConnectionFailSafeBehavior _behavior = DroneApplication.getDroneInstance().getConnectionFailSafeBehavior();
        if(group.getCheckedRadioButtonId() == stop_and_rth.getId()) {
            // 임무 중지 후 자동복귀
            if(_behavior != ConnectionFailSafeBehavior.GO_HOME){
                LogWrapper.i("UploadMission", "Request Go Home");
                DroneApplication.getDroneInstance().setConnectionFailSafeBehavior(ConnectionFailSafeBehavior.GO_HOME);
                return;
            }
        }else{
            // 임무 완료 후 복귀
            if(_behavior == ConnectionFailSafeBehavior.GO_HOME){
                LogWrapper.i("UploadMission", "Request Hover");
                DroneApplication.getDroneInstance().setConnectionFailSafeBehavior(ConnectionFailSafeBehavior.HOVER);
                return;
            }
        }
        // 임무 업로드 진행
        DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_UPLOAD_MISSION, 0, null));
    }
}
