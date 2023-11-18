package com.ptn.test_drawing.ui.theme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

public class CircleViewGroup extends ViewGroup
{
    private Paint circlePaint;
    private float circleRadius = 100f;
    public CircleViewGroup(Context context) {
        super(context);
        init();
    }
    private void init() {
        // Khởi tạo Paint cho hình tròn
        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE);
        circlePaint.setStyle(Paint.Style.FILL);
    }
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // Sắp xếp các View con bên trong CustomViewGroup
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            child.layout(0, 0, getWidth(), getHeight()); //sắp xếp
        }
    }
    protected void dispatchDraw(Canvas canvas) {
        // Gọi dispatchDraw để vẽ các View con lên Canvas
        super.dispatchDraw(canvas);

        // Vẽ hình tròn lên Canvas
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint);
    }
}
