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

public class DialogConfirm extends RelativeLayout implements View.OnClickListener{

    private Context context;
    Button mBtnYes;
    Button mBtnNo;
    TextView mTextView;

    public DialogConfirm(Context context, int contentId){
        super(context);
        this.context = context;
        initUI(contentId);
    }

    public DialogConfirm(Context context, AttributeSet attrs, int contentId) {
        super(context, attrs);
        this.context = context;
        initUI(contentId);
    }

    @Override
    protected void onAttachedToWindow() {

        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void initUI(int contentId){
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.dialog_confirm, this, true);

        RelativeLayout _layout = (RelativeLayout)findViewById(R.id.dialog_confirm_bg);
        LayoutParams _params = (LayoutParams)_layout.getLayoutParams();

        if(contentId == R.string.check_login_info)
        {
            _params.height += 50;
        }

        mBtnYes = (Button)findViewById(R.id.btn_dialog_confirm_yes);
        mBtnYes.setOnClickListener(this);

        mBtnNo = (Button)findViewById(R.id.btn_dialog_confirm_no);
        mBtnNo.setOnClickListener(this);

        mTextView = (TextView)findViewById(R.id.dialog_confirm_text);
        mTextView.setText(contentId);
    }

    @Override
    public void onClick(View v) {
        DroneApplication.getEventBus().post(new MainActivity.PopdownView());
    }
}