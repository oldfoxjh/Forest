package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.R;

public class DialogOk extends RelativeLayout implements View.OnClickListener{

    private Context context;
    Button mBtnOk;
    TextView mTextView;

    public DialogOk(Context context, int contentId, String msg){
        super(context);
        this.context = context;
        initUI(contentId, msg);
    }

    public DialogOk(Context context, AttributeSet attrs, int contentId, String msg) {
        super(context, attrs);
        this.context = context;
        initUI(contentId, msg);
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void initUI(int contentId, String msg){
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.dialog_ok, this, true);

        RelativeLayout _layout = (RelativeLayout)findViewById(R.id.dialog_ok_bg);
        LayoutParams _params = (LayoutParams)_layout.getLayoutParams();

        if(contentId == R.string.check_login_info
            || contentId == R.string.check_internet
            || contentId == R.string.save_fail
            || contentId == R.string.max_flight_height_success
            || contentId == R.string.max_flight_height_over
            || msg != null
        ) {
            _params.height += 46;
        }else if(contentId == R.string.check_drone_connection){
            _params.height += 69;
        }

        mBtnOk = (Button)findViewById(R.id.btn_dialog_ok);
        mBtnOk.setOnClickListener(this);

        mTextView = (TextView)findViewById(R.id.dialog_text);

        if(msg == null) mTextView.setText(contentId);
        else {
            String _content = context.getResources().getText(contentId).toString() + msg;
            mTextView.setText(_content);
        }
    }

    @Override
    public void onClick(View v) {
        DroneApplication.getEventBus().post(new MainActivity.PopdownView());
    }
}
