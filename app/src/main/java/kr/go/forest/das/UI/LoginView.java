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
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class LoginView extends RelativeLayout implements View.OnClickListener {
    private Context context;
    private Button mBtnLogin;
    private ViewWrapper menu;
    private InputMethodManager imm;
    private EditText loginIDEditText;
    private EditText loginPWEditText;

    public LoginView(Context context){
        super(context);
        this.context = context;
    }

    public LoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initUI();

        menu = new ViewWrapper(new MenuView(context), true);
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

        imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);

        loginIDEditText = (EditText)findViewById(R.id.loginIDEditText);
        loginPWEditText = (EditText)findViewById(R.id.loginPWEditText);

        mBtnLogin = (Button)findViewById(R.id.loginProcessButton);
        mBtnLogin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.loginProcessButton)
        {
            // ID
//            if(loginIDEditText.length() == 0)
//            {
//                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, R.string.check_id));
//                return;
//            }

            // Password
//            if(loginPWEditText.length() == 0)
//            {
//                DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, R.string.check_password));
//                return;
//            }
            // 로그인 요청

            //DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, R.string.check_login_info));

            // 키보드 체크
            hideSoftInput();
            DroneApplication.getEventBus().post(menu);
        }
    }

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

