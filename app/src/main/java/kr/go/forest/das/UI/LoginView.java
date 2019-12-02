package kr.go.forest.das.UI;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.BigdataSystemInfo;
import kr.go.forest.das.Model.DeviceInfo;
import kr.go.forest.das.Model.DroneInfo;
import kr.go.forest.das.Model.DroneInfoRequest;
import kr.go.forest.das.Model.LoginRequest;
import kr.go.forest.das.Model.LoginResponse;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;
import kr.go.forest.das.network.NetworkStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * 로그인화면
 */
public class LoginView extends RelativeLayout implements View.OnClickListener {
    private Context context;
    private Button mBtnLogin;                       // 로그인 요청 버튼
    private InputMethodManager imm;                 // 입력방법 관리 인스턴스
    private EditText loginIDEditText;               // 텍스트 입력 : ID
    private EditText loginPWEditText;               // 텍스트 입력 : 비밀번호

    private ProgressDialog progress;

    public LoginView(Context context){
        super(context);
        this.context = context;
    }

    public LoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    /**
     * View가 정상적으로 화면에 추가 되었을 때
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initUI();
    }

    @Override
    protected void onAttachedToWindow() {
        // 사용자 정보 저장
        BigdataSystemInfo _info = DroneApplication.getSystemInfo();
        _info.imei = DeviceInfo.getIMEI(context);
        _info.sn = DeviceInfo.getSerialNumber();
        _info.phone_number = DeviceInfo.getPhoneNumber(context);

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    /**
     * 초기 화면 설정
     */
    protected void initUI(){

        progress = new ProgressDialog(context);
        progress.setCancelable(false);
        progress.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);
        imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);

        loginIDEditText = findViewById(R.id.loginIDEditText);
        loginPWEditText = findViewById(R.id.loginPWEditText);

        mBtnLogin = findViewById(R.id.loginProcessButton);
        mBtnLogin.setOnClickListener(this);
    }

    /**
     * 위젯 클릭 이벤트 처리
     */
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.loginProcessButton)
        {
            if(NetworkStatus.isInternetConnected(context))
            {
                // ID
                if(loginIDEditText.length() == 0 && loginPWEditText.length() == 0) {
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.check_login_title, R.string.check_login));
                    return;
                }else{
                    if(loginIDEditText.length() == 0) {
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.check_id));
                        return;
                    }else if(loginPWEditText.length() == 0) {
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.check_password));
                        return;
                    }
                }
                // 로그인 요청
                LoginRequest _user = new LoginRequest();
                _user.id = loginIDEditText.getText().toString();
                _user.password = loginPWEditText.getText().toString();
                _user.phone = DroneApplication.getSystemInfo().phone_number;
                _user.sn = DroneApplication.getSystemInfo().sn;
                _user.imei = DroneApplication.getSystemInfo().imei;

                progress.setMessage("로그인 요청중입니다.");
                progress.show();
                DroneApplication.getApiInstance().postLogin(_user).enqueue(new Callback<LoginResponse>(){
                    @Override
                    public  void onResponse(Call<LoginResponse> call, Response<LoginResponse> response){

                        if(response.code() == 200) {
                            LoginResponse _response = response.body();
                            BigdataSystemInfo _info = DroneApplication.getSystemInfo();
                            _info.user_id = loginIDEditText.getText().toString();
                            _info.weather.locale = _response.weather.locale;
                            _info.weather.weather = _response.weather.weather;
                            _info.weather.temperature = _response.weather.temperature;
                            _info.weather.wind_speed = _response.weather.wind_speed;
                            _info.drone_id = _response.drone_list.get(0).manage_number;

                            _info.is_realtime = true;
                            _info.live_url = _response.live_url;
                            _info.aes_key = _response.aes_key;

                            _info.name = _response.name;
                            _info.service = _response.service;
                            _info.department = _response.department;

                            _info.setDroneInfo(_response.drone_list);
                            _info.setFlightPlan(_response.plan);

                            // 키보드 체크
                            hideSoftInput();
                            DroneApplication.getEventBus().post(new ViewWrapper(new MenuView(context), false));
                        }else{
                            // 서버 오류
                            DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.api_error));
                        }

                        progress.dismiss();
                    }

                    @Override
                    public  void onFailure(Call<LoginResponse> call, Throwable t){
                        progress.dismiss();
                        DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.check_internet_title, R.string.check_internet));
                    }
                });

            }else{
                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.check_internet_title, R.string.check_internet));
            }
        }
    }

    /**
     * 터치 키보드 제어
     */
    private void hideSoftInput()
    {
        if(loginIDEditText.hasFocus())
        {
            imm.hideSoftInputFromWindow(loginIDEditText.getWindowToken(), 0);
        }else if(loginPWEditText.hasFocus())
        {
            imm.hideSoftInputFromWindow(loginPWEditText.getWindowToken(), 0);
        }
    }
}

