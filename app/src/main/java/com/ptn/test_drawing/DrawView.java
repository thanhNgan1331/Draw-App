package com.ptn.test_drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import com.xiaopo.flying.sticker.StickerView;

public class DrawView extends StickerView {
    boolean isErasing = false;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<Integer> itemPath = new ArrayList<>();//xác định thứ tự item đã vẽ (0: stroke,1: rectangle,2: oval,3: line, 4: image, 5: text)
    private ArrayList<Integer> redoItemPath = new ArrayList<>();//xác định thứ tự item đã undo
    private ArrayList<Stroke> strokePaths = new ArrayList<>(); // Lưu các stroke đã vẽ
    private ArrayList<Stroke> redoStrokePaths = new ArrayList<>(); // Lưu các stroke đã undo
    private ArrayList<MyRectangle> rectShapePaths = new ArrayList<>();//mảng chứa các đối tượng Rect hiện có trên màn hình
    private ArrayList<MyRectangle> redoRectShapePaths = new ArrayList<>();//lưu các đối tượng Rect đã undo
    private ArrayList<MyOval> ovalShapePaths = new ArrayList<>();
    private ArrayList<MyOval> redoOvalShapePaths = new ArrayList<>();
    private ArrayList<MyLine> lineShapePaths = new ArrayList<>();
    private ArrayList<MyLine> redoLineShapePaths = new ArrayList<>();
    private ArrayList<Bitmap> imagePaths = new ArrayList<>();
    private ArrayList<Bitmap> redoImagePaths = new ArrayList<>();



    private int currentColor;
    private int currentStrokeWidth;
    int sizePen;
    private int currentAlpha;
    private Bitmap mBitmap;

    private Canvas mCanvas;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);

    private boolean drawShapeStatus = false;//kiểm tra trạng thái vẽ Shape
    private boolean drawRectStatus = false;
    private boolean drawCircleStatus = false;
    private boolean drawLineStatus = false;
    private boolean finalDrawRectShapeStatus = false;//=true khi touch_up, dùng để xác định hình dáng cuối cùng của hcn được tạo ra khi touch_move
    private boolean finalDrawOvalShapeStatus = false;
    private boolean finalDrawLineShapeStatus = false;
    private float startShapeX, startShapeY;//tọa độ điểm cố định khi vẽ hình chữ nhật
    private ArrayList<Rect> drawingRectShapePaths = new ArrayList<>(); //mảng tạm chứa các đối tượng Rect được tạo ra khi touch_move
    private ArrayList<MyOval> drawingOvalShapePaths = new ArrayList<>();
    private ArrayList<MyLine> drawingLineShapePaths = new ArrayList<>();

    String imageString, ip;
    int port;
    ListView listView, listMenu;
    LinearLayout layoutMenu, layoutSizeAndOpacity, layoutSizeEraser, shapeLayout, textLayout;

    ImageView btnUndo, btnRedo;
    private View drawingView;
    private Connection connection;

    // Các biến để quản lý các chế độ vẽ
    public enum TouchMode {
        DRAWVIEW, STICKERVIEW
    }

    private TouchMode touchMode = TouchMode.DRAWVIEW;

    public void setTouchMode(TouchMode touchMode) {
        this.touchMode = touchMode;
    }


    public void init(int height, int width, String ip, int port) {
        this.ip = ip;
        this.port = port;
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        currentColor = Color.RED;
        currentStrokeWidth = 10;
        currentAlpha = 200;
        connection = new Connection(ip, port);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d("StickerAndDrawView", "DrawView Constructor called");

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        drawingView = new View(context) {
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                canvas.save();

                mCanvas.drawColor(Color.WHITE);

                //vẽ lại toàn bộ các Stroke đang được lưu trữ
                int strokeIndex = 0, rectIndex = 0, ovalIndex = 0, lineIndex = 0, imageIndex = 0;
                for (int item : itemPath) {
                    switch (item) {
                        case 0:
                            Stroke fp = strokePaths.get(strokeIndex);
                            strokeIndex++;
                            mPaint.setColor(fp.color);
                            mPaint.setStrokeWidth(fp.strokeWidth);
                            mPaint.setAlpha(fp.alpha);
                            mCanvas.drawPath(fp.path, mPaint);
                            break;
                        case 1:
                            MyRectangle rect = rectShapePaths.get(rectIndex);
                            rectIndex++;
                            mPaint.setColor(rect.color);
                            mPaint.setStrokeWidth(rect.strokeWidth);
                            mPaint.setAlpha(rect.alpha);
                            mCanvas.drawRect(rect.rect, mPaint);
                            break;
                        case 2:
                            MyOval oval = ovalShapePaths.get(ovalIndex);
                            ovalIndex++;
                            mPaint.setColor(oval.color);
                            mPaint.setStrokeWidth(oval.strokeWidth);
                            mPaint.setAlpha(oval.alpha);
                            mCanvas.drawOval(oval.left, oval.top, oval.right, oval.bottom, mPaint);
                            break;
                        case 3:
                            MyLine line = lineShapePaths.get(lineIndex);
                            lineIndex++;
                            mPaint.setColor(line.color);
                            mPaint.setStrokeWidth(line.strokeWidth);
                            mPaint.setAlpha(line.alpha);
                            mCanvas.drawLine(line.startX, line.startY, line.stopX, line.stopY, mPaint);
                            break;
                        case 4:
                            Bitmap image = imagePaths.get(imageIndex);
                            imageIndex++;
                            mCanvas.drawBitmap(image, 0, 0, mBitmapPaint);
                            break;
                    }
                }
                mPaint.setColor(currentColor);
                mPaint.setStrokeWidth(currentStrokeWidth);
                mPaint.setAlpha(currentAlpha);

                if (drawShapeStatus == true)//xử lý sự kiện vẽ shape
                {
                    if (drawRectStatus == true)//xử lý sự kiện vẽ hcn
                    {
                        if (drawingRectShapePaths.size() > 0) //vẫn đang ở sự kiện touch_move
                        {
                            Rect lastRect = drawingRectShapePaths.get(drawingRectShapePaths.size() - 1);
                            mCanvas.drawRect(lastRect, mPaint);//chỉ vẽ hcn ở vị trí đang chạm
                        }
                        if (finalDrawRectShapeStatus == true) //khi ở sự kiện touch_up
                        {
                            Rect lastRect = drawingRectShapePaths.get(drawingRectShapePaths.size() - 1);
                            MyRectangle newRect = new MyRectangle(currentColor, currentStrokeWidth, lastRect, currentAlpha);
                            rectShapePaths.add(newRect);//thêm hcn vào final paths
                            itemPath.add(1);
                            drawingRectShapePaths.clear();//xóa toàn bộ nội dung của arraylist tạm
                            finalDrawRectShapeStatus = false;
                            mCanvas.drawRect(lastRect, mPaint);//vẽ hcn
                        }
                    } else if (drawCircleStatus == true) //xử lí sự kiện vẽ oval
                    {
                        if (drawingOvalShapePaths.size() > 0) //vẫn đang ở sự kiện touch_move
                        {
                            MyOval lastOval = drawingOvalShapePaths.get(drawingOvalShapePaths.size() - 1);
                            mCanvas.drawOval(lastOval.left, lastOval.top, lastOval.right, lastOval.bottom, mPaint);//chỉ vẽ oval ở vị trí đang chạm
                        }
                        if (finalDrawOvalShapeStatus == true) //khi ở sự kiện touch_up
                        {
                            MyOval lastOval = drawingOvalShapePaths.get(drawingOvalShapePaths.size() - 1);
                            MyOval newOval = new MyOval(lastOval.left, lastOval.top, lastOval.right, lastOval.bottom, currentColor, currentStrokeWidth, currentAlpha);
                            ovalShapePaths.add(newOval);//thêm oval vào final paths
                            itemPath.add(2);
                            drawingOvalShapePaths.clear();//xóa toàn bộ nội dung của arraylist tạm
                            finalDrawOvalShapeStatus = false;
                            mCanvas.drawOval(lastOval.left, lastOval.top, lastOval.right, lastOval.bottom, mPaint);//vẽ oval
                        }
                    } else if (drawLineStatus == true) {
                        if (drawingLineShapePaths.size() > 0) //vẫn đang ở sự kiện touch_move
                        {
                            MyLine lastLine = drawingLineShapePaths.get(drawingLineShapePaths.size() - 1);
                            mCanvas.drawLine(lastLine.startX, lastLine.startY, lastLine.stopX, lastLine.stopY, mPaint);//chỉ vẽ line ở vị trí đang chạm
                        }
                        if (finalDrawLineShapeStatus == true) //khi ở sự kiện touch_up
                        {
                            MyLine lastLine = drawingLineShapePaths.get(drawingLineShapePaths.size() - 1);
                            MyLine newLine = new MyLine(lastLine.startX, lastLine.startY, lastLine.stopX, lastLine.stopY, currentColor, currentStrokeWidth, currentAlpha);
                            lineShapePaths.add(newLine);//thêm oval vào final paths
                            itemPath.add(3);
                            drawingLineShapePaths.clear();//xóa toàn bộ nội dung của arraylist tạm
                            finalDrawLineShapeStatus = false;
                            mCanvas.drawLine(lastLine.startX, lastLine.startY, lastLine.stopX, lastLine.stopY, mPaint);//vẽ line
                        }
                    }
                }
                canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
                canvas.restore();
            }
        };
        addView(drawingView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }





//    private boolean isTouchInsideView(float x, float y) {
//        // Xác định khu vực hợp lệ dựa trên scaleFactor và kích thước của View
//        float scaledWidth = drawingView.getWidth() * scaleFactor;
//        float scaledHeight = drawingView.getHeight() * scaleFactor;
//
//        // Kiểm tra xem tọa độ x, y có nằm trong khu vực này không
//        return x >= 0 && x <= scaledWidth && y >= 0 && y <= scaledHeight;
//    }

    public void setColor(int color) {
        currentColor = color;
    }

    public void setStrokeWidth(int width) {
        currentStrokeWidth = width;
        sizePen = width;
    }

    public void setAlpha(int opacity) {
        currentAlpha = opacity;
    }

    public void setObjectInActivity(ListView listMenu,
                                    ListView listView,
                                    LinearLayout layoutMenu,
                                    LinearLayout layoutSizeAndOpacity,
                                    LinearLayout layoutSizeEraser,
                                    LinearLayout shapeLayout,
                                    LinearLayout textLayout,
                                    ImageView btnUndo,
                                    ImageView btnRedo) {
        this.listMenu = listMenu;
        this.listView = listView;
        this.layoutMenu = layoutMenu;
        this.layoutSizeAndOpacity = layoutSizeAndOpacity;
        this.layoutSizeEraser = layoutSizeEraser;
        this.shapeLayout = shapeLayout;
        this.textLayout = textLayout;
        this.btnUndo = btnUndo;
        this.btnRedo = btnRedo;
    }

    public void undo() {
        if (itemPath.size() != 0) {
            int item = itemPath.remove(itemPath.size() - 1);//lấy ra item mới được vẽ gần nhất
            redoItemPath.add(item);//thêm vào list redo
            switch (item) {
                case 0://Stroke
                    if (strokePaths.size() != 0) {
                        redoStrokePaths.add(strokePaths.remove(strokePaths.size() - 1));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 1:
                    if (rectShapePaths.size() != 0) {
                        redoRectShapePaths.add(rectShapePaths.remove(rectShapePaths.size() - 1));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 2:
                    if (ovalShapePaths.size() != 0) {
                        redoOvalShapePaths.add(ovalShapePaths.remove((ovalShapePaths.size() - 1)));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 3:
                    if (lineShapePaths.size() != 0) {
                        redoLineShapePaths.add(lineShapePaths.remove((lineShapePaths.size() - 1)));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 4:
                    if (imagePaths.size() != 0) {
                        redoImagePaths.add(imagePaths.remove((imagePaths.size() - 1)));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;

            }
        }
        if (itemPath.size() <= 0) {
            setUnableUndo();
        }
    }

    public void redo() {
        if (redoItemPath.size() != 0) {
            int item = redoItemPath.remove(redoItemPath.size() - 1);//lấy ra item vừa mới undo gần nhất
            itemPath.add(item);//thêm vào list item cần hiển thị
            switch (item) {
                case 0:
                    if (redoStrokePaths.size() != 0) {
                        strokePaths.add(redoStrokePaths.remove(redoStrokePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 1:
                    if (redoRectShapePaths.size() != 0) {
                        rectShapePaths.add(redoRectShapePaths.remove(redoRectShapePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 2:
                    if (redoOvalShapePaths.size() != 0) {
                        ovalShapePaths.add(redoOvalShapePaths.remove(redoOvalShapePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 3:
                    if (redoLineShapePaths.size() != 0) {
                        lineShapePaths.add(redoLineShapePaths.remove(redoLineShapePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 4:
                    if (redoImagePaths.size() != 0) {
                        imagePaths.add(redoImagePaths.remove(redoImagePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;

            }
        }
        if (redoItemPath.size() <= 0) {
            setUnableRedo();
        }
        if (itemPath.size() > 0) {
            setEnableUndo();
        }
    }

    // Lưu nội dung của view vào bitmap
    public Bitmap save() {
        return connection.getBitmapFromViewUsingCanvas(DrawView.this);
    }


    public void setImageBackground(Bitmap bitmap) {
        if (bitmap != null) {
            // Nếu mBitmap là null hoặc không phải là bitmap có thể chỉnh sửa, khởi tạo nó
            Bitmap imageBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            imagePaths.add(imageBitmap);
            itemPath.add(4);
            sentImgage();
            drawingView.invalidate();
            // Yêu cầu vẽ lại view
        } else {
            Log.e("SetImage", "Bitmap is null");
        }
    }


    public void newImage() {
        // Xóa hết các stroke đã vẽ
        strokePaths.clear();
        redoStrokePaths.clear();
        rectShapePaths.clear();
        redoRectShapePaths.clear();
        ovalShapePaths.clear();
        redoOvalShapePaths.clear();
        lineShapePaths.clear();
        redoLineShapePaths.clear();
        itemPath.clear();
        redoItemPath.clear();
        imagePaths.clear();
        redoImagePaths.clear();
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Vẽ lại view
        sentImgage();
        drawingView.invalidate();
        setUnableRedo();
        setUnableUndo();
        listView.setVisibility(GONE);
    }


//    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            scaleFactor *= detector.getScaleFactor();
//
//            // Giới hạn giá trị scaleFactor để tránh phóng to quá mức hoặc thu nhỏ quá mức
//            scaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(MAX_SCALE_FACTOR, scaleFactor));
//
//            // Cập nhật điểm neo dựa trên vị trí chạm của ScaleGestureDetector
//            pivotX = detector.getFocusX();
//            pivotY = detector.getFocusY();
//
//            drawingView.invalidate(); // Vẽ lại khi có sự thay đổi
//            return true;
//        }
//    }


    private void touchStart(float x, float y) {
        mPath = new Path();
        if (isErasing == false) {
            Stroke fp = new Stroke(currentColor, currentStrokeWidth, mPath, currentAlpha);
            strokePaths.add(fp);
            itemPath.add(0);
        } else {
            Stroke fp = new Stroke(Color.WHITE, currentStrokeWidth, mPath, 255);
            fp.useErase = true;
            strokePaths.add(fp);
            itemPath.add(0);
        }
        redoStrokePaths.clear();
        redoItemPath.clear();
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
        //  mPath.reset();
    }


    private Handler handler = new Handler();
    private Runnable sentImageRunnable = new Runnable() {
        @Override
        public void run() {
            sentImgage();
            // Lên lịch chạy lại sau 1 giây
            handler.postDelayed(this, 100);
        }
    };


    private boolean handleDrawViewTouchEvent(MotionEvent event) {
        // Gọi onTouchEvent của ScaleGestureDetector để xử lý sự kiện phóng to và thu nhỏ
        //scaleGestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        if (true) {
            if (drawShapeStatus == false) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStart(x, y);
                        drawingView.invalidate();
                        setEnableUndo();
                        hideAllLayout();
                        // Bắt đầu Runnable khi bắt đầu chạm
                        //handler.post(sentImageRunnable);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        touchMove(x, y);
                        drawingView.invalidate();
                        break;
                    case MotionEvent.ACTION_UP:
                        touchUp();
                        sentImgage();
                        drawingView.invalidate();
                        // Dừng Runnable khi kết thúc chạm
                        //handler.removeCallbacks(sentImageRunnable);
                        break;
                }
            } else {
                if (drawRectStatus == true) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startShapeX = event.getX();
                            startShapeY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            drawRectShape(startShapeX, startShapeY, x, y);
                            drawingView.invalidate();
                            break;
                        case MotionEvent.ACTION_UP:
                            drawRectShape(startShapeX, startShapeY, x, y);
                            finalDrawRectShapeStatus = true;
                            sentImgage();
                            drawingView.invalidate();
                            break;
                    }
                } else if (drawCircleStatus == true) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startShapeX = event.getX();
                            startShapeY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            drawOvalShape(startShapeX, startShapeY, x, y);
                            drawingView.invalidate();
                            break;
                        case MotionEvent.ACTION_UP:
                            drawOvalShape(startShapeX, startShapeY, x, y);
                            finalDrawOvalShapeStatus = true;
                            sentImgage();
                            drawingView.invalidate();
                            break;
                    }
                } else if (drawLineStatus == true) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startShapeX = event.getX();
                            startShapeY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            drawLineShape(startShapeX, startShapeY, x, y);
                            drawingView.invalidate();
                            break;
                        case MotionEvent.ACTION_UP:
                            drawLineShape(startShapeX, startShapeY, x, y);
                            finalDrawLineShapeStatus = true;
                            sentImgage();
                            drawingView.invalidate();
                            break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (touchMode) {
            case DRAWVIEW:
                // Xử lý sự kiện cho DrawView
                return handleDrawViewTouchEvent(event);
            case STICKERVIEW:
                // Xử lý sự kiện bằng cách gọi onTouchEvent của StickerView
                return super.onTouchEvent(event);
            default:
                return false;
        }
    }


    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }


    public void drawRectShape(float x1, float y1, float x2, float y2) {
        Rect rect = new Rect((int) x1, (int) y1, (int) x2, (int) y2);
        drawingRectShapePaths.add(rect);
        redoRectShapePaths.clear();
        redoItemPath.clear();
    }

    public void drawOvalShape(float x1, float y1, float x2, float y2) {
        MyOval oval = new MyOval((int) x1, (int) y1, (int) x2, (int) y2, currentColor, currentStrokeWidth, currentAlpha);
        drawingOvalShapePaths.add(oval);
        redoOvalShapePaths.clear();
        redoItemPath.clear();
    }

    public void drawLineShape(float x1, float y1, float x2, float y2) {
        MyLine line = new MyLine(x1, y1, x2, y2, currentColor, currentStrokeWidth, currentAlpha);
        drawingLineShapePaths.add(line);
        redoLineShapePaths.clear();
        redoItemPath.clear();
    }

    private Bitmap getBitmapFromViewUsingCanvas(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        view.draw(canvas);

        return bitmap;
    }


    public void sentImgage() {
        if (ip == null) {
            return;
        } else {
            connection.sendData(connection.convertImg(connection.getBitmapFromViewUsingCanvas(DrawView.this)));
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

    public void erasingStatus(boolean status) {
        isErasing = status;
    }

    public void drawShapeStatus(boolean status) {
        drawShapeStatus = status;
        if (status == false) {
            drawRectStatus = false;
            drawCircleStatus = false;
            drawLineStatus = false;
        }
    }

    public void drawRectStatus(boolean status) {
        drawRectStatus = status;
    }

    public void drawCircleStatus(boolean status) {
        drawCircleStatus = status;
    }

    public void drawLineStatus(boolean status) {
        drawLineStatus = status;
    }

    public void hideAllLayout() {
        listView.setVisibility(View.GONE);
        listMenu.setVisibility(View.GONE);
        layoutSizeAndOpacity.setVisibility(View.GONE);
        layoutSizeEraser.setVisibility(View.GONE);
        shapeLayout.setVisibility(View.GONE);
        textLayout.setVisibility(View.GONE);
    }
}
