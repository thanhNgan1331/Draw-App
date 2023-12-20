package com.ptn.test_drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import androidx.compose.ui.graphics.Outline;

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

    boolean isErasing=false;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<Integer> itemPath= new ArrayList<>();//xác định thứ tự item đã vẽ (0: stroke,1:rectangle,2: oval,3:line)
    private ArrayList<Integer> redoItemPath= new ArrayList<>();//xác định thứ tự item đã undo
    private ArrayList<Stroke> strokePaths = new ArrayList<>(); // Lưu các stroke đã vẽ
    private ArrayList<Stroke> redoStrokePaths = new ArrayList<>(); // Lưu các stroke đã undo

    private ArrayList<MyRectangle> rectShapePaths = new ArrayList<>();//mảng chứa các đối tượng Rect hiện có trên màn hình
    private ArrayList<MyRectangle> redoRectShapePaths = new ArrayList<>();//lưu các đối tượng Rect đã undo
    private ArrayList<MyOval> ovalShapePaths = new ArrayList<>();
    private ArrayList<MyOval> redoOvalShapePaths = new ArrayList<>();
    private ArrayList<MyLine> lineShapePaths = new ArrayList<>();
    private ArrayList<MyLine> redoLineShapePaths = new ArrayList<>();

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
    private boolean finalDrawRectShapeStatus=false;//=true khi touch_up, dùng để xác định hình dáng cuối cùng của hcn được tạo ra khi touch_move
    private boolean finalDrawOvalShapeStatus=false;
    private boolean finalDrawLineShapeStatus=false;
    private float startShapeX,startShapeY;//tọa độ điểm cố định khi vẽ hình chữ nhật
    private ArrayList<Rect> drawingRectShapePaths = new ArrayList<>();//mảng tạm chứa các đối tượng Rect được tạo ra khi touch_move
    private ArrayList<MyOval> drawingOvalShapePaths=new ArrayList<>();
    private ArrayList<MyLine> drawingLineShapePaths=new ArrayList<>();
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

                //vẽ lại toàn bộ các Stroke đang được lưu trữ
                int strokeIndex=0,rectIndex=0,ovalIndex=0,lineIndex=0;
                for(int item : itemPath)
                {
                    switch (item)
                    {
                        case 0:
                            Stroke fp=strokePaths.get(strokeIndex);
                            strokeIndex++;

                            mPaint.setColor(fp.color);
                            mPaint.setStrokeWidth(fp.strokeWidth);
                            mPaint.setAlpha(fp.alpha);
                            //  mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                            // mPaint.setXfermode(null); // Đặt lại chế độ mặc định

                            mCanvas.drawPath(fp.path, mPaint);
                            break;
                        case 1:
                            MyRectangle rect=rectShapePaths.get(rectIndex);
                            rectIndex++;
                            mPaint.setColor(rect.color);
                            mPaint.setStrokeWidth(rect.strokeWidth);
                            mPaint.setAlpha(rect.alpha);
                            mCanvas.drawRect(rect.rect, mPaint);
                            break;
                        case 2:
                            MyOval oval=ovalShapePaths.get(ovalIndex);
                            ovalIndex++;
                            mPaint.setColor(oval.color);
                            mPaint.setStrokeWidth(oval.strokeWidth);
                            mPaint.setAlpha(oval.alpha);
                            mCanvas.drawOval(oval.left,oval.top,oval.right,oval.bottom, mPaint);
                            break;
                        case 3:
                            MyLine line=lineShapePaths.get(lineIndex);
                            lineIndex++;
                            mPaint.setColor(line.color);
                            mPaint.setStrokeWidth(line.strokeWidth);
                            mPaint.setAlpha(line.alpha);
                            mCanvas.drawLine(line.startX,line.startY,line.stopX,line.stopY,mPaint);
                    }
                }
                mPaint.setColor(currentColor);
                mPaint.setStrokeWidth(currentStrokeWidth);
                mPaint.setAlpha(currentAlpha);

                if(drawShapeStatus==true)//xử lý sự kiện vẽ shape
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
                    }
                    else if(drawLineStatus==true)
                    {
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
                    // canvas.drawBitmap(mBitmap, 0, 0, mPaint);
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
        currentStrokeWidth = 10;
        currentAlpha = 200;
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

        currentStrokeWidth = width;
        sizePen=width;
    }

    public void setAlpha(int opacity) {
        currentAlpha = opacity;
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
        if(itemPath.size()!=0)
        {
            int item=itemPath.remove(itemPath.size()-1);//lấy ra item mới được vẽ gần nhất
            redoItemPath.add(item);//thêm vào list redo
            switch(item)
            {
                case 0://Stroke
                    if (strokePaths.size() != 0)
                    {
                        redoStrokePaths.add(strokePaths.remove(strokePaths.size() - 1));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 1:
                    if (rectShapePaths.size() != 0)
                    {
                        redoRectShapePaths.add(rectShapePaths.remove(rectShapePaths.size() - 1));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 2:
                    if(ovalShapePaths.size()!=0)
                    {
                        redoOvalShapePaths.add(ovalShapePaths.remove((ovalShapePaths.size()-1)));
                        setEnableRedo();
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 3:
                    if(lineShapePaths.size()!=0)
                    {
                        redoLineShapePaths.add(lineShapePaths.remove((lineShapePaths.size()-1)));
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
        if(redoItemPath.size()!=0)
        {
            int item=redoItemPath.remove(redoItemPath.size()-1);//lấy ra item vừa mới undo gần nhất
            itemPath.add(item);//thêm vào list item cần hiển thị
            switch(item)
            {
                case 0:
                    if (redoStrokePaths.size() != 0)
                    {
                        strokePaths.add(redoStrokePaths.remove(redoStrokePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 1:
                    if (redoRectShapePaths.size() != 0)
                    {
                        rectShapePaths.add(redoRectShapePaths.remove(redoRectShapePaths.size() - 1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 2:
                    if(redoOvalShapePaths.size()!=0)
                    {
                        ovalShapePaths.add(redoOvalShapePaths.remove(redoOvalShapePaths.size()-1));
                        drawingView.invalidate();
                        Log.d("SentImage", "Image:" + mBitmap);
                        sentImgage();
                    }
                    break;
                case 3:
                    if(redoLineShapePaths.size()!=0)
                    {
                        lineShapePaths.add(redoLineShapePaths.remove(redoLineShapePaths.size()-1));
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
        strokePaths.clear();
        redoStrokePaths.clear();
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


    private void touchStart(float x, float y)
    {
        mPath = new Path();
        if(isErasing==false)
        {
            Stroke fp = new Stroke(currentColor,currentStrokeWidth, mPath, currentAlpha);
            strokePaths.add(fp);
            itemPath.add(0);
        }
        else
        {
            Stroke fp = new Stroke(Color.WHITE, currentStrokeWidth, mPath, 255);
            fp.useErase=true;
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Gọi onTouchEvent của ScaleGestureDetector để xử lý sự kiện phóng to và thu nhỏ
        scaleGestureDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        if (isTouchInsideView(x, y)) {
            if (drawShapeStatus == false)
            {

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
                        drawingView.invalidate();
                        break;
                }
            } else
            {   if(drawRectStatus==true)
                {
                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            startShapeX = event.getX();
                            startShapeY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            drawRectShape(startShapeX,startShapeY,x,y);
                            drawingView.invalidate();
                            break;
                        case MotionEvent.ACTION_UP:
                            drawRectShape(startShapeX,startShapeY,x,y);
                            finalDrawRectShapeStatus=true;
                            drawingView.invalidate();
                            break;
                    }
                }
                else if(drawCircleStatus==true)
                {
                    switch (event.getAction())
                    {
                        case MotionEvent.ACTION_DOWN:
                            startShapeX = event.getX();
                            startShapeY = event.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            drawOvalShape(startShapeX, startShapeY, x, y);
                            drawingView.invalidate();
                            break;
                        case MotionEvent.ACTION_UP:
                            drawRectShape(startShapeX, startShapeY, x, y);
                            finalDrawOvalShapeStatus = true;
                            drawingView.invalidate();
                            break;
                    }
                }
                else if(drawLineStatus==true)
                {
                    switch (event.getAction())
                    {
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
                            drawingView.invalidate();
                            break;
                    }
                }
            }
        }
        return true;
    }
    public void drawRectShape(float x1,float y1,float x2,float y2)
    {
        Rect rect=new Rect((int)x1, (int)y1, (int)x2, (int)y2);
        drawingRectShapePaths.add(rect);
        redoRectShapePaths.clear();
        redoItemPath.clear();
    }
    public void drawOvalShape(float x1,float y1,float x2,float y2)
    {
        MyOval oval=new MyOval((int)x1, (int)y1, (int)x2, (int)y2,currentColor,currentStrokeWidth,currentAlpha);
        drawingOvalShapePaths.add(oval);
        redoOvalShapePaths.clear();
        redoItemPath.clear();
    }
    public void drawLineShape(float x1,float y1,float x2,float y2)
    {
        MyLine line=new MyLine(x1, y1, x2, y2,currentColor,currentStrokeWidth,currentAlpha);
        drawingLineShapePaths.add(line);
        redoLineShapePaths.clear();
        redoItemPath.clear();
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
    public void erasingStatus(boolean status)
    {
        isErasing=status;
    }
    public void drawShapeStatus(boolean status)
    {
        drawShapeStatus=status;
        if(status==false)
        {
            drawRectStatus=false;
            drawCircleStatus=false;
            drawLineStatus=false;
        }
    }
    public void drawRectStatus(boolean status)
    {
        drawRectStatus=status;
    }
    public void drawCircleStatus(boolean status)
    {
        drawCircleStatus=status;
    }
    public void drawLineStatus(boolean status)
    {
        drawLineStatus=status;
    }
}
