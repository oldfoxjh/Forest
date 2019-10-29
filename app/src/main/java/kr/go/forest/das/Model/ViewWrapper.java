package kr.go.forest.das.Model;

import android.view.View;

public class ViewWrapper {

    private boolean animation;
    private View view;

    public ViewWrapper(View layoutView, boolean animation) {
        view = layoutView;
        this.animation = animation;
    }

    public boolean isAnimation() {
        return animation;
    }

    public View getView() {
        return view;
    }

    public String viewInfo(){ return view.toString(); }
}
