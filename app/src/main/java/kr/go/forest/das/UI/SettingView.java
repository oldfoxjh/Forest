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
import android.widget.TextView;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
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
    LinearLayout setting_login_info;

    TextView setting_name;
    TextView setting_service;
    TextView setting_department;

    TextView textview_manage_number;
    TextView textview_manage_department;
    TextView textview_drone_product;
    TextView textview_drone_manufacturer;
    TextView textview_drone_kind;
    TextView textview_drone_camera;

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
        setting_login_info = findViewById(R.id.setting_login_info);

        findViewById(R.id.btn_setting_back).setOnClickListener(this);

        setting_name = findViewById(R.id.setting_name);
        setting_service = findViewById(R.id.setting_service);
        setting_department = findViewById(R.id.setting_department);

        textview_manage_number = findViewById(R.id.textview_manage_number);
        textview_manage_department = findViewById(R.id.textview_manage_department);
        textview_drone_product = findViewById(R.id.textview_drone_product);
        textview_drone_manufacturer = findViewById(R.id.textview_drone_manufacturer);
        textview_drone_kind = findViewById(R.id.textview_drone_kind);
        textview_drone_camera = findViewById(R.id.textview_drone_camera);

        setting_drone_info = findViewById(R.id.setting_drone_info);
        boolean is_login = DroneApplication.getSystemInfo().isLogin();
        if(is_login == true) {
            setting_drone_info.setOnClickListener(this);
            setting_drone_info.setSelected(true);

            // 사용자 정보 및 드론 정보 설정
            BigdataSystemInfo _info = DroneApplication.getSystemInfo();
            setting_name.setText(_info.name);
            setting_service.setText(_info.service);
            setting_department.setText(_info.department);

            textview_manage_number.setText(_info.drone_list.get(_info.drone_index).manage_number);
            textview_manage_department.setText(_info.drone_list.get(_info.drone_index).department);
            textview_drone_product.setText(_info.drone_list.get(_info.drone_index).model);
            textview_drone_manufacturer.setText(_info.drone_list.get(_info.drone_index).manufacturer);
            textview_drone_kind.setText(_info.drone_list.get(_info.drone_index).kind);
            textview_drone_camera.setText(_info.drone_list.get(_info.drone_index).camera);
        }else{
            setting_login_info.setVisibility(GONE);
            setting_drone_info.setVisibility(GONE);
            setting_device_layout.setVisibility(GONE);
            setting_interface_layout.setVisibility(VISIBLE);
        }

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