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
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class DrawView extends View {

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
    private Bitmap openedImage;

    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);


    ImageView imageView = new ImageView(getContext());
    String imageString;
    String ip;
    int port;


    private ScaleGestureDetector scaleGestureDetector;

    // Các biến để giữ giá trị zoom
    private float scaleFactor = 1.0f;
    private static final float MIN_SCALE_FACTOR = 0.2f;
    private static final float MAX_SCALE_FACTOR = 5.0f;


    ImageView btnUndo, btnRedo;


    // Constructors to initialise all the attributes
    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        mPaint.setAlpha(0);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

    }

    public void init(int height, int width) {

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = Color.RED;

        strokeWidth = 10;
        alpha = 200;
    }


    public void setColor(int color) {
        currentColor = color;
    }

    public void setStrokeWidth(int width) {
        strokeWidth = width;
    }

    public void setAlpha(int opacity) {
        alpha = opacity;
    }

    public void setButtonUndo(ImageView btnUndo) {
        this.btnUndo = btnUndo;
    }
    public void setButtonRedo(ImageView btnRedo) {
        this.btnRedo = btnRedo;
    }

    public void undo() {
        if (paths.size() != 0) {
            redoPaths.add(paths.remove(paths.size() - 1));
            setEnableRedo();
            invalidate();
            Log.d("SentImage", "Image:" + mBitmap);
            sentImgage();

            if (paths.size() <= 0) {
                setUnableUndo();
            }

        }
    }

    public void redo() {
        if (redoPaths.size() != 0) {
            paths.add(redoPaths.remove(redoPaths.size() - 1));
            invalidate();
            Log.d("SentImage", "Image:" + mBitmap);
            sentImgage();

            if (redoPaths.size() <= 0) {
                setUnableRedo();
            }
            if (paths.size() > 0) {
                setEnableUndo();
            }
        }
    }


    public int getSizeUndo() {
        return paths.size();
    }

    public int getSizeRedo() {
        return redoPaths.size();
    }


    // this methods returns the current bitmap
    public Bitmap save() {
        return mBitmap;
    }

    public void open(Bitmap bitmap) {
        // Copy nội dung của bitmap vào mBitmap để có thể vẽ lên đó
        mCanvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);
        // Vẽ lại view
        invalidate();
    }

    public void newImage() {
        // Xóa hết các stroke đã vẽ
        paths.clear();
        redoPaths.clear();
        // Vẽ lại view
        invalidate();
    }


    public ImageView getImageView() {
        return imageView;
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

    // this is the main method where
    // the actual drawing takes place
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


    private void touchStart(float x, float y) {
        mPath = new Path();
        Stroke fp = new Stroke(currentColor, strokeWidth, mPath, alpha);
        paths.add(fp);
        redoPaths.clear();

        // finally remove any curve
        // or line from the path
        mPath.reset();

        // this methods sets the starting
        // point of the line being drawn
        mPath.moveTo(x, y);

        // we save the current
        // coordinates of the finger
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
                invalidate();
                setEnableUndo();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                sentImgage();
                Log.d("SentImage", "Image:" + mBitmap);
                invalidate();
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
                s = new Socket("192.168.1.3", 6862);
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