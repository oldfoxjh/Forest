package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;

public class SettingView extends RelativeLayout implements View.OnClickListener {

    private Context context;
    private SharedPreferences pref;

    Button setting_drone_info;
    Button setting_drone_interface;
    Button setting_real_time;
    LinearLayout setting_interface_layout;
    LinearLayout setting_device_layout;

    public SettingView(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public SettingView(Context context, AttributeSet attrs) {
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
        pref = context.getSharedPreferences("drone", Context.MODE_PRIVATE);
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.setting_layout, this, true);

        setting_interface_layout = findViewById(R.id.setting_interface_layout);
        setting_device_layout = findViewById(R.id.setting_device_layout);

        findViewById(R.id.btn_setting_back).setOnClickListener(this);
        setting_drone_info = findViewById(R.id.setting_drone_info);
        setting_drone_info.setOnClickListener(this);
        setting_drone_info.setSelected(true);

        setting_drone_interface = findViewById(R.id.setting_drone_interface);
        setting_drone_interface.setOnClickListener(this);

        setting_real_time = findViewById(R.id.setting_real_time);
        setting_real_time.setOnClickListener(this);

        String _real_time = pref.getString("real", "off");
        if(_real_time.equals("on")){
            setting_real_time.setSelected(true);
        }
        setClickable(true);
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.setting_drone_info:
                if(!setting_drone_info.isSelected()){
                    setting_drone_info.setSelected(true);
                    setting_drone_interface.setSelected(false);
                    setting_device_layout.setVisibility(VISIBLE);
                    setting_interface_layout.setVisibility(GONE);
                }
                break;
            case R.id.setting_drone_interface:
                if(!setting_drone_interface.isSelected()) {
                    setting_drone_info.setSelected(false);
                    setting_drone_interface.setSelected(true);
                    setting_device_layout.setVisibility(GONE);
                    setting_interface_layout.setVisibility(VISIBLE);
                }
                break;
            case R.id.setting_real_time:
                setting_real_time.setSelected(!setting_real_time.isSelected());

                SharedPreferences.Editor _editor = pref.edit();
                _editor.putString("real", setting_real_time.isSelected() ? "on" : "off");
                _editor.commit();
                break;
            case R.id.btn_setting_back:
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
                break;
        }
    }
}