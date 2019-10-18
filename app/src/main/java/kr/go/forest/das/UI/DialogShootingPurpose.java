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

public class DialogShootingPurpose extends RelativeLayout implements View.OnClickListener{

    private Context context;
    Button btn_open;
    Button btn_cancel;
    Spinner spinner_purpose;
    ArrayAdapter<String> arrayAdapter;
    List<String> list_purpose = new ArrayList<>();
    int selected_index = 0;

    public DialogShootingPurpose(Context context, List<String> purpose){
        super(context);
        this.context = context;
        initUI(purpose);
    }

    public DialogShootingPurpose(Context context, AttributeSet attrs, List<String> purpose) {
        super(context, attrs);
        this.context = context;
        initUI(purpose);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void initUI(List<String> purpose){
        //초기화
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.dialog_shooting_purpose, this, true);

        spinner_purpose = findViewById(R.id.spinner_purpose);
        list_purpose.addAll(purpose);
        arrayAdapter = new ArrayAdapter<>(context, R.layout.spinner_item, list_purpose);
        spinner_purpose.setAdapter(arrayAdapter);
        spinner_purpose.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selected_index = i;
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        btn_open = findViewById(R.id.btn_dialog_purpose_yes);
        btn_open.setOnClickListener(this);

        btn_cancel = findViewById(R.id.btn_dialog_purpose_no);
        btn_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DroneApplication.getEventBus().post(new MainActivity.PopdownView());
            }
        });
    }

    @Override
    public void onClick(View v) {
        if(selected_index == 1) {
            DroneApplication.getEventBus().post(new MainActivity.Realtime(true));
        }else{
            DroneApplication.getEventBus().post(new MainActivity.Realtime(false));
        }

        DroneApplication.getEventBus().post(new MainActivity.PopdownView());
    }
}
