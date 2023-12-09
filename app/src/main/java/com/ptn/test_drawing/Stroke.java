package com.ptn.test_drawing;

import android.graphics.Path;

public class Stroke {
    // color of the stroke
    public int color;

    // width of the stroke
    public int strokeWidth;

    // a Path object to
    // represent the path drawn
    public Path path;

    public int alpha;
    public boolean useErase=false;
    // constructor to initialise the attributes
    public Stroke(int color, int strokeWidth, Path path, int alpha) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
        this.alpha = alpha;
    }
}
