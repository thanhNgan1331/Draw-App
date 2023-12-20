package com.ptn.test_drawing;

import android.graphics.Rect;

public class MyOval {
    public int color;
    public int strokeWidth;
    public int alpha;
    int left;
    int top;
    int right;
    int bottom;
    public MyOval(int left,int top,int right,int bottom,int color, int strokeWidth,  int alpha)
    {
        this.left=left;
        this.top=top;
        this.right=right;
        this.bottom=bottom;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.alpha = alpha;
    }
}

