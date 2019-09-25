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

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.R;

public class DialogUploadMission extends RelativeLayout implements View.OnClickListener{

    private Context context;
    String data = null;

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

        RadioGroup _radio_group = (RadioGroup) findViewById(R.id.dialog_mission_upload_radio_group);
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
        // 임무 중지 후 Home 귀환
        RadioButton _stop_and_rth = new RadioButton(context);
        _stop_and_rth.setText(R.string.mission_stop_and_rth);
        _stop_and_rth.setTextSize(getResources().getDimension(R.dimen.radio_font));
        _radio_group.addView(_stop_and_rth);

        // 임무 중지 후 Home 귀환
        RadioButton _continue = new RadioButton(context);
        _continue.setText(R.string.mission_continue);
        _continue.setTextSize(getResources().getDimension(R.dimen.radio_font));
        _radio_group.addView(_continue);

        _radio_group.check(_stop_and_rth.getId());
    }

    @Override
    public void onClick(View v) {
        // 조종기 단절시 선택한 내용 확인

        // 임무 업로드 진행
        DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_UPLOAD_MISSION, 0, null));
    }
}
