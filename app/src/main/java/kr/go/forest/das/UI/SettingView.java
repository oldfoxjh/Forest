package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.Model.ViewWrapper;
import kr.go.forest.das.R;

public class SettingView extends RelativeLayout implements View.OnClickListener {

    private Context context;

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
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.setting_layout, this, true);

        setClickable(true);
    }

    @Override
    public void onClick(View v) {

    }
}