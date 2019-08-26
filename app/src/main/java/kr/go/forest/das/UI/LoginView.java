package kr.go.forest.das.UI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import kr.go.forest.das.R;

public class LoginView extends RelativeLayout {
    private Context context;

    public LoginView(Context context){
        super(context);
        this.context = context;
        init();
    }

    public LoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public void init(){
        String inflaterService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(inflaterService);
        View view = layoutInflater.inflate(R.layout.login_layout, LoginView.this, false);
        addView(view);
    }

    protected void initView(){
        //초기화
    }
}
