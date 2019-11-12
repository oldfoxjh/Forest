package kr.go.forest.das.UI;

import android.app.Service;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.R;

public class DialogCheckRealtime extends RelativeLayout implements View.OnClickListener{

    private Context context;

    public DialogCheckRealtime(Context context){
        super(context);
        this.context = context;
        initUI();
    }

    public DialogCheckRealtime(Context context, AttributeSet attrs) {
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
        layoutInflater.inflate(R.layout.dialog_check_realtime, this, true);

        findViewById(R.id.btn_dialog_check_realtime_yes).setOnClickListener(this);
        findViewById(R.id.btn_dialog_check_realtime_no).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneApplication.getEventBus().post(new MainActivity.Realtime(false));
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
            }
        });

        setClickable(true);
    }

    @Override
    public void onClick(View v) {
        DroneApplication.getEventBus().post(new MainActivity.Realtime(true));
        DroneApplication.getEventBus().post(new MainActivity.PopdownView());
    }
}
