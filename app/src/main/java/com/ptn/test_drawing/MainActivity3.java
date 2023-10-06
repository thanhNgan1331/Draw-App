package com.ptn.test_drawing;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity3 extends AppCompatActivity {

    // Initialize variable
    Button btnEncode, btnDecode, btnChangeColor;
    TextView textView;
    ImageView imageView;
    String sImage;
    byte[] bytes;

    private float startX = -1, startY = -1, endX = -1, endY = -1;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint = new Paint();

    String imageString;
    int DefaultColor = Color.BLACK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        btnEncode = findViewById(R.id.btnSaveImg);
        btnDecode = findViewById(R.id.btnDel);
        btnChangeColor = findViewById(R.id.btnChangeColor);
        imageView = findViewById(R.id.imageView);

        btnChangeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenColorPickerDialog(false);
            }
        });

    }

    private void OpenColorPickerDialog(boolean AlphaSupport) {

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(MainActivity3.this, DefaultColor, AlphaSupport, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog ambilWarnaDialog, int color) {
                paint.setColor(color);
                btnChangeColor.setBackgroundColor(color);
            }

            @Override
            public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {

                Toast.makeText(MainActivity3.this, "Color Picker Closed", Toast.LENGTH_SHORT).show();
            }
        });
        ambilWarnaDialog.show();

    }


    public void buttonSaveImage(View view) {
        Uri images;
        ContentResolver contentResolver = getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            images = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "images/*");
        Uri uri = contentResolver.insert(images, contentValues);
        try {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            Objects.requireNonNull(outputStream);
            Toast.makeText(MainActivity3.this, "Lưu ảnh thành công", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity3.this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


    public void convertImg_v1() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap bitmap = bitmapDrawable.getBitmap();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendData_v1() {
        String mess = imageString;
        try {
            Client_send c1 = new Client_send();
            c1.execute(mess);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendData_v2() {
        String mess = imageString;
        try {
            Client_send c1 = new Client_send();
            c1.execute(mess);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // Class gửi đi dữ liệu
    class Client_send extends AsyncTask<String, Void, Void> {
        Socket s;
        PrintWriter writer;

        @Override
        protected Void doInBackground(String... voids) {
            try {
                String mess = voids[0];
                s = new Socket("192.168.1.2", 6862);
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


    private void DrawPaintOnImg() {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            canvas.drawColor(Color.WHITE);


            paint.setColor(DefaultColor);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);

        }
        canvas.drawLine(startX,
                startY - 220,
                endX,
                endY - 220,
                paint);
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();

        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            endX = event.getX();
            endY = event.getY();

            DrawPaintOnImg();

            startX = event.getX();
            startY = event.getY();
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            endX = event.getX();
            endY = event.getY();

            DrawPaintOnImg();
            convertImg_v1();
            sendData_v1();

        }
        return super.onTouchEvent(event);
    }


    public void btnDelete(View view) {
        if (bitmap != null) {
            // Xóa hết nội dung trên Bitmap
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            // Cập nhật ImageView để hiển thị Bitmap sau khi xóa
            imageView.setImageBitmap(bitmap);
            canvas.drawColor(Color.WHITE);
            convertImg_v1();
            sendData_v1();
        }
    }
}
