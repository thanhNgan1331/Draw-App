package com.ptn.test_drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CircleDrawView extends View
{
        private Paint mCirclePaint;
        private Canvas mCanvas;
        private Bitmap mBitmap;

        public CircleDrawView(Context context) {
            super(context);
            init();
        }

        public CircleDrawView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            mCirclePaint = new Paint();
            mCirclePaint.setColor(Color.BLUE);
            mCirclePaint.setStrokeWidth(40);
            mCirclePaint.setStyle(Paint.Style.FILL);

            mBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

        public void drawOnCanvas() {
            // Vẽ hình tròn lên Canvas
            int x = 250; // Tọa độ x của hình tròn
            int y = 250; // Tọa độ y của hình tròn
            int radius = 100; // Bán kính của hình tròn

            mCanvas.drawCircle(x, y, radius, mCirclePaint);

            // Cập nhật lại giao diện
            invalidate();
        }

}
