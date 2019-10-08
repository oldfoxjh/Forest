package kr.go.forest.das.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import kr.go.forest.das.DroneApplication;
import kr.go.forest.das.Log.LogWrapper;
import kr.go.forest.das.MainActivity;
import kr.go.forest.das.R;
import kr.go.forest.das.drone.Drone;

/**
 * Override PreFlightStatusWidget with custom appearance
 */
public class CustomPreFlightStatusWidget extends dji.ux.widget.PreFlightStatusWidget {

    //region Properties
    private Paint status_paint;
    private static final int STROKE_WIDTH = 5;
    private int width;
    private int height;
    private String drone_status = "기기연결끊김";
    private boolean is_good = false;
    //endregion

    //region Constructors
    public CustomPreFlightStatusWidget(Context context) {
        this(context, null, 0);
    }

    public CustomPreFlightStatusWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomPreFlightStatusWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    //endregion

    //region Override methods
    /** Inflate custom layout for this widget */
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyleAttr) {

        status_paint = new Paint();
        status_paint.setStyle(Paint.Style.FILL);
        status_paint.setColor(Color.WHITE);
        status_paint.setAlpha(255);
        status_paint.setAntiAlias(true);
        int textSize = getResources().getDimensionPixelSize(R.dimen.status_font);
        status_paint.setTextSize(textSize);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect bounds = new Rect();
        status_paint.getTextBounds(drone_status, 0, drone_status.length(), bounds);
        float y = context.getResources().getDimensionPixelSize(R.dimen.px52);
        canvas.drawText(drone_status, 26, y, status_paint);
    }

    /** Called when connection status changes */
    @Override
    public void onStatusChange(String status, StatusType type, boolean blink) {

        if (type == StatusType.OFFLINE) {
            this.setBackgroundResource(R.mipmap.top_bg_gray);
            drone_status = "기기 연결 끊김";
            is_good = false;
            LogWrapper.i(TAG, status + " OFFLINE :  " + drone_status );
        } else if (type == StatusType.GOOD){
            this.setBackgroundResource(R.mipmap.top_bg_green);
            if(status.contains("In-Flight")) drone_status = status.replace("In-Flight", "비행중");
            else if(status.contains("Ready to Go")) drone_status = status.replace("Ready to Go", "비행 준비 완료");
            else if(status.contains("Ready to GO")) drone_status = status.replace("Ready to GO", "비행 준비 완료");

            LogWrapper.i(TAG, status + " GOOD :  " + drone_status );

            is_good = true;
        } else if (type == StatusType.WARNING){
            this.setBackgroundResource(R.mipmap.top_bg_orange);
            if(status.equals("Image Transmission Signal Weak")) drone_status = "영상전송 신호 약함";
            LogWrapper.i(TAG, status + " WARNING :  " + drone_status );
        } else if (type == StatusType.ERROR){
            this.setBackgroundResource(R.mipmap.top_bg_red);
            if(status.equals("Aircraft Disconnected")) {
                drone_status = "기체 연결 끊김";

                // 드론 시동이 걸린 상태에서 통신이 끊길 경우
                if(is_good == true) {
                    DroneApplication.getEventBus().post(new MainActivity.PopupDialog(MainActivity.PopupDialog.DIALOG_TYPE_OK, 0, R.string.aircraft_disconnected, null));
                    DroneApplication.getEventBus().post(new MainActivity.DroneStatusChange(Drone.DRONE_STATUS_DISCONNECT));
                }
            }
            else if(status.equals("Remote Controller Signal Weak")) drone_status = "조종기 신호 약함";
            else if(status.equals("Cannot take off")) drone_status = "이륙준비 오류";
            else if(status.equals("IMU Error. Calibrate IMU")) drone_status = "IMU 오류";
            else drone_status = "기기 연결 끊김";

            is_good = false;
            LogWrapper.i(TAG, status + " ERROR :  " + drone_status );
        }
    }
    //endregion
}
