package com.ptn.test_drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.text.Layout;
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
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import com.xiaopo.flying.sticker.BitmapStickerIcon;
import com.xiaopo.flying.sticker.DeleteIconEvent;
import com.xiaopo.flying.sticker.DrawableSticker;
import com.xiaopo.flying.sticker.FlipHorizontallyEvent;
import com.xiaopo.flying.sticker.Sticker;
import com.xiaopo.flying.sticker.StickerView;
import com.xiaopo.flying.sticker.TextSticker;
import com.xiaopo.flying.sticker.ZoomIconEvent;
import com.ptn.test_drawing.util.FileUtil;

public class DrawView extends RelativeLayout {

    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<Stroke> paths = new ArrayList<>(); // Lưu các stroke đã vẽ
    private ArrayList<Stroke> redoPaths = new ArrayList<>(); // Lưu các stroke đã undo

    private int currentColor;
    private int strokeWidth;

    private int alpha;
    private Bitmap mBitmap;

    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);


    private boolean useEraser = false;
    private boolean isBrushThicknessSliderVisible = false;
    private boolean isEraserThicknessSliderVisible = false;

    ImageView imageView = new ImageView(getContext());
    String imageString;
    String ip = "127.0.0.0";
    int port = 6862;
    ListView listView, listMenu;
    LinearLayout layoutSizeAndOpacity;

    private ScaleGestureDetector scaleGestureDetector;

    // Các biến để giữ giá trị zoom
    private float scaleFactor = 1.0f;
    private static final float MIN_SCALE_FACTOR = 0.2f;
    private static final float MAX_SCALE_FACTOR = 5.0f;

    private float pivotX = 0f, pivotY = 0f;


    ImageView btnUndo, btnRedo;
    private View drawingView;

    private StickerView stickerView = new StickerView(getContext());

    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        drawingView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                canvas.save();
                canvas.scale(scaleFactor, scaleFactor, pivotX, pivotY);

                // Vẽ ảnh nền nếu có
                if (mBitmap != null) {
                    canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
                }

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


    public void addSticker() {

        // Khởi tạo TextView
        TextView textView = new TextView(getContext());

        // Thiết lập các thuộc tính cho TextView
        textView.setText("Hello, World!");
        textView.setTextColor(Color.BLUE);
        textView.setTextSize(18); // 18sp

        this.addView(textView);

    }

    public void init(int height, int width, String ip, int port) {
        this.ip = ip;
        this.port = port;
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        currentColor = Color.RED;
        strokeWidth = 10;
        alpha = 200;
    }

    private boolean isTouchInsideView(float x, float y) {
        // Xác định khu vực hợp lệ dựa trên scaleFactor và kích thước của View
        float scaledWidth = drawingView.getWidth() * scaleFactor;
        float scaledHeight = drawingView.getHeight() * scaleFactor;

        // Kiểm tra xem tọa độ x, y có nằm trong khu vực này không
        return x >= 0 && x <= scaledWidth && y >= 0 && y <= scaledHeight;
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

    public void setObjectInActivity(ListView listMenu,
                                    ListView listView,
                                    LinearLayout layoutSizeAndOpacity,
                                    ImageView btnUndo,
                                    ImageView btnRedo) {
        this.listMenu = listMenu;
        this.listView = listView;
        this.layoutSizeAndOpacity = layoutSizeAndOpacity;
        this.btnUndo = btnUndo;
        this.btnRedo = btnRedo;
    }

    public void undo() {
        if (paths.size() != 0) {
            redoPaths.add(paths.remove(paths.size() - 1));
            setEnableRedo();
            drawingView.invalidate();
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
            drawingView.invalidate();
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


    // Lưu nội dung của view vào bitmap
    public Bitmap save() {
        return mBitmap;
    }

    public void open(Bitmap bitmap) {
        // Copy nội dung của bitmap vào mBitmap để có thể vẽ lên đó
        mCanvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);
        // Vẽ lại view
        invalidate();
    }

    public void setImageBackground(Bitmap bitmap) {
        // Đặt bitmap làm nền cho việc vẽ
        if (bitmap != null) {
            mBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            mCanvas = new Canvas(mBitmap);
            drawingView.invalidate(); // Yêu cầu vẽ lại view
        }
    }


    public void newImage() {
        // Xóa hết các stroke đã vẽ
        paths.clear();
        redoPaths.clear();
        // Vẽ lại view
        drawingView.invalidate();
        setUnableRedo();
        setUnableUndo();
        listView.setVisibility(GONE);
    }



    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();

            // Giới hạn giá trị scaleFactor để tránh phóng to quá mức hoặc thu nhỏ quá mức
            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(MAX_SCALE_FACTOR, scaleFactor));

            // Cập nhật điểm neo dựa trên vị trí chạm của ScaleGestureDetector
            pivotX = detector.getFocusX();
            pivotY = detector.getFocusY();

            drawingView.invalidate(); // Vẽ lại khi có sự thay đổi
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

        if (isTouchInsideView(x, y)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchStart(x, y);
                    drawingView.invalidate();
                    setEnableUndo();
                    listMenu.setVisibility(View.GONE);
                    listView.setVisibility(View.GONE);
                    layoutSizeAndOpacity.setVisibility(View.GONE);
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
