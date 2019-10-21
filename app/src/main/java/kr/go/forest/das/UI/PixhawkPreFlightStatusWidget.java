package kr.go.forest.das.UI;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import kr.go.forest.das.R;

public class PixhawkPreFlightStatusWidget extends RelativeLayout {
    private Context context;

    public PixhawkPreFlightStatusWidget(Context context){
        super(context);
        this.context = context;
    }

    public PixhawkPreFlightStatusWidget(Context context, AttributeSet attrs) {
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

    /**
     * 화면 UI 설정
     */
    protected void initUI(){
    }
}
