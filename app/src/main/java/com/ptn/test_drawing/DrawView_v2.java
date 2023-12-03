package com.ptn.test_drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class DrawView_v2 extends RelativeLayout {

    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;

    private Paint mPaint;
    private ArrayList<Stroke> paths = new ArrayList<>();
    private ArrayList<Stroke> redoPaths = new ArrayList<>();

    private int currentColor;
    private int strokeWidth;

    private int alpha;
    private Bitmap mBitmap;

    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);


    ImageView imageView = new ImageView(getContext());
    String imageString;
    String ip = "127.0.0.0";
    int port = 6862;


    private ScaleGestureDetector scaleGestureDetector;

    // Các biến để giữ giá trị zoom
    private float scaleFactor = 1.0f;
    private static final float MIN_SCALE_FACTOR = 0.2f;
    private static final float MAX_SCALE_FACTOR = 5.0f;


    ImageView btnUndo, btnRedo;

    // View con cho việc vẽ
    private View drawingView;


    // Constructors to initialise all the attributes
    public DrawView_v2(Context context) {
        this(context, null);
    }

    public DrawView_v2(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mPaint.setAlpha(0);
        // Khởi tạo drawingView
        drawingView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                canvas.save();

                canvas.scale(scaleFactor, scaleFactor);


                int backgroundColor = Color.WHITE;
                mCanvas.drawColor(backgroundColor);

                for (Stroke fp : paths) {
                    mPaint.setColor(fp.color);
                    mPaint.setStrokeWidth(fp.strokeWidth);
                    mPaint.setAlpha(fp.alpha);
                    mCanvas.drawPath(fp.path, mPaint);
                }
                canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
                canvas.restore();
            }
        };
        addView(drawingView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

    }

    public void init(int height, int width) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = Color.RED;

        strokeWidth = 10;
        alpha = 200;
    }



    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Giới hạn giá trị scaleFactor để tránh phóng to quá mức hoặc thu nhỏ quá mức
            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(MAX_SCALE_FACTOR, scaleFactor));

            invalidate(); // Vẽ lại khi có sự thay đổi
            return true;
        }
    }



    private void touchStart(float x, float y) {
        mPath = new Path();
        Stroke fp = new Stroke(currentColor, strokeWidth, mPath, alpha);
        paths.add(fp);
        redoPaths.clear();


        mPath.reset();


        mPath.moveTo(x, y);

        mX = x;
        mY = y;
    }


    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }


    private void touchUp() {
        mPath.lineTo(mX, mY);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Gọi onTouchEvent của ScaleGestureDetector để xử lý sự kiện phóng to và thu nhỏ
        scaleGestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                drawingView.invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                drawingView.invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                sentImgage();
                Log.d("SentImage", "Image:" + mBitmap);
                drawingView.invalidate();
                break;
        }
        return true;
    }


    public void convertImg_v1() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendData_v1(String mess) {
        try {
            Client_send c1 = new Client_send();
            c1.execute(mess);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void sentImgage() {
        convertImg_v1();
        sendData_v1(imageString);
    }

    // Class dùng để gửi đi dữ liệu
    class Client_send extends AsyncTask<String, Void, Void> {
        Socket s;
        PrintWriter writer;

        @Override
        protected Void doInBackground(String... voids) {
            try {
                String mess = voids[0];
                s = new Socket(ip, port);
                writer = new PrintWriter(s.getOutputStream());
                writer.write(mess);
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }


    private void setEnableUndo() {
        btnUndo.setEnabled(true);
        btnUndo.setImageResource(R.drawable.undo);
    }

    private void setEnableRedo() {
        btnRedo.setEnabled(true);
        btnRedo.setImageResource(R.drawable.redo);
    }

    private void setUnableUndo() {
        btnUndo.setEnabled(false);
        btnUndo.setImageResource(R.drawable.undo_disable);
    }

    private void setUnableRedo() {
        btnRedo.setEnabled(false);
        btnRedo.setImageResource(R.drawable.redo_disable);
    }
}
