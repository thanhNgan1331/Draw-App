package com.ptn.test_drawing;

import android.graphics.Rect;

public class MyRectangle {
    public int color;
    public int strokeWidth;
    public Rect rect;
    public int alpha;
    public MyRectangle(int color, int strokeWidth, Rect rect, int alpha) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.rect = rect;
        this.alpha = alpha;
    }

}
