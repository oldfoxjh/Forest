package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;

public class DialogConfirm extends RelativeLayout implements View.OnClickListener{

    private Context context;
    Button mBtnYes;
    Button mBtnNo;
    TextView mTextView;
    int content_id;
    int title_id;

    public DialogConfirm(Context context, int titleId, int contentId){
        super(context);
        this.context = context;
        content_id = contentId;
        title_id = titleId;
        initUI();
    }

    public DialogConfirm(Context context, AttributeSet attrs, int titleId, int contentId) {
        super(context, attrs);
        this.context = context;
        content_id = contentId;
        title_id = titleId;
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
        layoutInflater.inflate(R.layout.dialog_confirm, this, true);

        RelativeLayout _layout = (RelativeLayout)findViewById(R.id.dialog_confirm_bg);
        LayoutParams _params = (LayoutParams)_layout.getLayoutParams();

        if(title_id == R.string.takeoff_title
            || title_id == R.string.landing_title
            || title_id == R.string.return_home_title
            || title_id == R.string.return_home_cancel_title
            || title_id == R.string.set_home_location_title
            || title_id == R.string.max_flight_height_low_title
            || title_id == R.string.check_internet_title
            || title_id == R.string.check_login_title
            || title_id == R.string.mission_start_title                 // 확인
            || title_id == R.string.landing_cancel_title
            || title_id == R.string.clear_mission
        ){
            TextView _title = findViewById(R.id.dialog_confirm_title);
            _title.setText(title_id);
            _title.setVisibility(VISIBLE);

            if(title_id == R.string.clear_mission)  _params.height += 80;
            else if(title_id == R.string.takeoff_title || title_id == R.string.mission_start_title)  _params.height += 120;
            else if(title_id == R.string.check_internet_title  || title_id == R.string.check_login_title)  _params.height += 150;
            else if(title_id == R.string.landing_title || title_id == R.string.max_flight_height_low_title)  _params.height += 180;
            else if(title_id == R.string.return_home_title || title_id == R.string.set_home_location_title)  _params.height += 200;

        }else if(content_id == R.string.max_flight_height_low ) {
            _params.height += 50;
        }

        mBtnYes = (Button)findViewById(R.id.btn_dialog_confirm_yes);
        mBtnYes.setOnClickListener(this);

        mBtnNo = (Button)findViewById(R.id.btn_dialog_confirm_no);
        mBtnNo.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
            }
        });

        if(content_id != 0) {
            mTextView = (TextView) findViewById(R.id.dialog_confirm_text);
            mTextView.setText(content_id);
        }

        setClickable(true);
    }

    @Override
    public void onClick(View v) {

        if(content_id == R.string.clear_mission) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.Mission.MISSION_CLEAR, null));
        }else if(title_id == R.string.takeoff_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_REQUEST_TAKEOFF, null));
        }else if(title_id == R.string.landing_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_REQUEST_LANDING, null));
        }else if(title_id == R.string.landing_cancel_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_CANCEL_LANDING, null));
        }else if(title_id == R.string.return_home_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_START_RETURN_HOME, null));
        }else if(title_id == R.string.return_home_cancel_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_CANCEL_RETURN_HOME, null));
        }else if(title_id == R.string.set_home_location_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_SET_RETURN_HOME_LOCATION, null));
        }else if(title_id == R.string.max_flight_height_low_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_MAX_FLIGHT_HEIGHT_LOW, null));
        }else if(title_id == R.string.mission_start_title) {
            DroneApplication.getEventBus().post(new MainActivity.PopdownView(MainActivity.PopupDialog.DIALOG_TYPE_CONFIRM, MainActivity.PopupDialog.DIALOG_TYPE_START_MISSION, null));
        }else if(title_id == R.string.check_internet_title || title_id == R.string.check_login_title){
            DroneApplication.getEventBus().post(new MainActivity.PopdownView());
            DroneApplication.getEventBus().post(new ViewWrapper(new MenuView(context), true));
        }
    }
}
