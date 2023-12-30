package com.ptn.test_drawing;

public class MyLine {
    public int color;
    public int strokeWidth;
    public int alpha;
    public float startX,startY,stopX,stopY;
    public MyLine(float startX,float startY,float stopX,float stopY,int color,int strokeWidth,int alpha)
    {
        this.startX=startX;
        this.startY=startY;
        this.stopX=stopX;
        this.stopY=stopY;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.alpha = alpha;
    }

}
