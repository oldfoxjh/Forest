package kr.go.forest.das.UI;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ImageView;

import kr.go.forest.das.R;

/**
 * Override PreFlightStatusWidget with custom appearance
 */
public class CustomPreFlightStatusWidget extends dji.ux.widget.PreFlightStatusWidget {

    //region Properties
    private Paint status_paint;
    private static final int STROKE_WIDTH = 5;
    private int width;
    private int height;
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
        status_paint.setColor(Color.RED);
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
        String text = "테스트";
        Rect bounds = new Rect();
        status_paint.getTextBounds(text, 0, text.length(), bounds);
        canvas.drawText(text, 26, height + bounds.top + bounds.bottom, status_paint);
    }

    /** Called when connection status changes */
    @Override
    public void onStatusChange(String status, StatusType type, boolean blink) {
        if (type != StatusType.OFFLINE) {
            this.setBackgroundResource(R.mipmap.connected_status);
        } else {
            this.setBackgroundResource(R.mipmap.top_bg_discon);
        }
    }
    //endregion
}
