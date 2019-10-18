package kr.go.forest.das.UI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.DeviceInfo;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;
import kr.go.forest.das.network.NetworkStatus;

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

    private String IMEI = "";
    private String SN = "";

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

        imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);

        loginIDEditText = (EditText)findViewById(R.id.loginIDEditText);
        loginPWEditText = (EditText)findViewById(R.id.loginPWEditText);

        mBtnLogin = (Button)findViewById(R.id.loginProcessButton);
        mBtnLogin.setOnClickListener(this);
    }

    /**
     * 위젯 클릭 이벤트 처리
     */
    @Override
    public void onClick(View v) {

        IMEI = DeviceInfo.getIMEI(context);
        SN = DeviceInfo.getSerialNumber();

        if(v.getId() == R.id.loginProcessButton)
        {
            if(NetworkStatus.isInternetConnected(context))
            {
                // ID
//            if(loginIDEditText.length() == 0)
//            {
//                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, R.string.check_id));
//                return;
//            }

                // Password
//            if(loginPWEditText.length() == 0)
//            {
//                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, R.string.check_password));
//                return;
//            }
                // 로그인 요청

                // 키보드 체크
                hideSoftInput();
                DroneApplication.getEventBus().post(new ViewWrapper(new MenuView(context), false));

               // DroneApplication.getEventBus().post(new ViewWrapper(new PixhawkMissionView(context), true));
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

