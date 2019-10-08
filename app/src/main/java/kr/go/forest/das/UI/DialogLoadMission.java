package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FilenameFilter;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.R;

public class DialogLoadMission extends RelativeLayout implements View.OnClickListener, RadioGroup.OnCheckedChangeListener{

    private Context context;
    String data = null;

    public DialogLoadMission(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public DialogLoadMission(Context context, AttributeSet attrs) {
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
        layoutInflater.inflate(R.layout.dialog_load_mission, this, true);

        RadioGroup _file_list = (RadioGroup) findViewById(R.id.load_shape_file_list);
        _file_list.setOnCheckedChangeListener(this);
        Button btn_open = (Button)findViewById(R.id.btn_dialog_load_shape_open);
        btn_open.setOnClickListener(this);

        Button btn_cancel = (Button)findViewById(R.id.btn_dialog_load_shape_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
            }
        });

        // 비행계획 설정하기

//        for(String _file : _files) {
//            RadioButton _radio_button = new RadioButton(context);
//            _radio_button.setText(_file);
//            _radio_button.setTextSize(getResources().getDimension(R.dimen.radio_font));
//            _file_list.addView(_radio_button);
//        }
    }

    @Override
    public void onClick(View v) {
        DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_LOAD_MISSION, 0, data));
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        RadioButton _radio_button = (RadioButton)group.findViewById(checkedId);

        // 선택된 임무 정보 세팅하기
        //file_path = Environment.getExternalStorageDirectory() + File.separator + "DroneAppService/Mission"+ File.separator + _radio_button.getText();
    }
}
