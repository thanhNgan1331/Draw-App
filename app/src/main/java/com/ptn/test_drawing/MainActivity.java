package com.ptn.test_drawing;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    // Initialize variable
    ImageView imageView, btnMenu, btnColor, btnPen, btnUndo, btnRedo;

    Button btnNew, btnOpen, btnSave, btnLogout;

    SeekBar seekBarSize, seekBarOpacity;
    TextView txtCountSize, txtCountOpacity;
    LinearLayout layoutSizeAndOpacity;
    private float startX = -1, startY = -1, endX = -1, endY = -1;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint paint = new Paint();

    Path path;



    String imageString;
    int DefaultColor = Color.BLACK;

    String ip;
    int port;

    GridView gridView;

    private ArrayList<Path> paths = new ArrayList<Path>();
    private ArrayList<Path> undonePaths = new ArrayList<Path>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hiện thị ứng dụng full màn hình
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main3);

        imageView = findViewById(R.id.imageView);
        btnMenu = findViewById(R.id.btnMenu);
        btnColor = findViewById(R.id.btnColor);
        btnPen = findViewById(R.id.btnPen);
        layoutSizeAndOpacity = findViewById(R.id.layoutSizeAndOpacity);
        seekBarSize = findViewById(R.id.seekBarSize);
        seekBarOpacity = findViewById(R.id.seekBarOpacity);
        txtCountSize = findViewById(R.id.txtCountSize);
        txtCountOpacity = findViewById(R.id.txtCountOpacity);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);



        btnUndo.setEnabled(false);
        btnRedo.setEnabled(false);


        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                undoDraw();
            }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redoDraw();
            }
        });


        txtCountSize.setText(seekBarSize.getProgress() + "");
        txtCountOpacity.setText(seekBarOpacity.getProgress() + "");

        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtCountSize.setText(progress + "");
                paint.setStrokeWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtCountOpacity.setText(progress + "");
                paint.setAlpha(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btnPen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutSizeAndOpacity.getVisibility() == View.VISIBLE) {
                    layoutSizeAndOpacity.setVisibility(View.GONE);
                } else {
                    layoutSizeAndOpacity.setVisibility(View.VISIBLE);
                    layoutSizeAndOpacity.bringToFront();
                }
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHide(v);
            }
        });

        List<Item_draw> image_details = getListData();
        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setAdapter(new CustomGridAdapter(this, image_details));

        // When the user clicks on the GridItem
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = gridView.getItemAtPosition(position);
                Item_draw itemdraw = (Item_draw) o;
                switch (position) {
                    case 0: // Save
                        btnSaveImage(v);
                        break;
                    case 1: // Import Image
                        btnOpenImage(v);
                        break;
                    case 2: // Shapes
                        break;
                    case 3: // Eraser
                        break;
                    case 4: // Text
                        break;
                    case 5: // Exit
                        btnLogout(v);
                        break;

                }
            }
        });

        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OpenColorPickerDialog(false);
            }

        });

        // Nhận dữ liệu từ Activity trước
        Intent intent = getIntent();
        ip = intent.getStringExtra("ip_key");
        port = intent.getIntExtra("port_key", 6862);


    }


    public void btnOpenImage(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 3);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                // Lấy ảnh từ Gallery
                Bitmap selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);

                // Thay đổi kích thước ảnh theo kích thước ImageView
                selectedBitmap = Bitmap.createScaledBitmap(selectedBitmap, imageView.getWidth(), imageView.getHeight(), false);

                // Tạo Bitmap mới có định dạng ARGB_8888
                bitmap = selectedBitmap.copy(Bitmap.Config.ARGB_8888, true);

                // Tạo Canvas mới với Bitmap
                canvas = new Canvas(bitmap);

                // Draw on the selected image
                DrawPaintOnImg();

                // Gắn Bitmap vừa vẽ vào ImageView
                imageView.setImageBitmap(bitmap);
                sentImgage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Tạo danh sách các item trong menu
    private List<Item_draw> getListData() {
        List<Item_draw> list = new ArrayList<Item_draw>();
        Item_draw save = new Item_draw("save");
        Item_draw shapes = new Item_draw("shapes");
        Item_draw eraser = new Item_draw("eraser");
        Item_draw text = new Item_draw("text");
        Item_draw importImage = new Item_draw("image");
        Item_draw exit = new Item_draw("exit");

        list.add(save);
        list.add(importImage);
        list.add(shapes);
        list.add(eraser);
        list.add(text);
        list.add(exit);

        return list;
    }

    // Hàm ẩn hiện gridview
    public void showHide(View view) {
        if (gridView.getVisibility() == View.VISIBLE) {
            gridView.setVisibility(View.GONE);
        } else {
            gridView.setVisibility(View.VISIBLE);
            gridView.bringToFront();
        }
    }


    // Hàm mở ColorPickerDialog
    private void OpenColorPickerDialog(boolean AlphaSupport) {

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(MainActivity.this, DefaultColor, AlphaSupport, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog ambilWarnaDialog, int color) {
                paint.setColor(color);
            }

            @Override
            public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {

                Toast.makeText(MainActivity.this, "Color Picker Closed", Toast.LENGTH_SHORT).show();
            }
        });
        ambilWarnaDialog.show();

    }

    public void btnLogout(View view) {
        sendData_v1("logout");
        Intent intent = new Intent(this, ConnectToTheServerActivity.class);
        startActivity(intent);
        finish();
    }

    public void btnSaveImage(View view) {
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
            Toast.makeText(MainActivity.this, "Lưu ảnh thành công", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
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


    public void btnNew(View view) {
        if (bitmap != null) {
            // Xóa hết nội dung trên Bitmap
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            // Cập nhật ImageView để hiển thị Bitmap sau khi xóa
            imageView.setImageBitmap(bitmap);
            canvas.drawColor(Color.WHITE);
            sentImgage();
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

    private void DrawPaintOnImg() {
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(imageView.getWidth(), imageView.getHeight(), Bitmap.Config.ARGB_8888);
            canvas = new Canvas(bitmap);

            canvas.drawColor(Color.WHITE);

            paint.setAntiAlias(true);
            paint.setDither(true);
            paint.setColor(DefaultColor);
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.BEVEL);
            paint.setStrokeCap(Paint.Cap.ROUND);

            paint.setAlpha(255);


            paint.setStrokeWidth(8);

        }


        path = new Path();
        path.moveTo(startX, startY - 220);
        path.lineTo(endX, endY - 220);
        paths.add(path);
        canvas.drawPath(path, paint);

        imageView.setImageBitmap(bitmap);


//        canvas.drawLine(startX,
//                startY - 220,
//                endX,
//                endY - 220,
//                paint);
//        imageView.setImageBitmap(bitmap);

    }





    private void undoDraw() {
        if (paths.size() > 0) {
            undonePaths.add(paths.remove(paths.size() - 1));
            setEnableRedo();
            redrawCanvas();
            sentImgage();


            if (paths.size() <= 0) {
                setUnableUndo();
            }

        } else {

        }
        //toast th
    }

    private void redoDraw() {
        if (undonePaths.size() > 0) {
            paths.add(undonePaths.remove(undonePaths.size() - 1));
            redrawCanvas();
            sentImgage();

            if (undonePaths.size() <= 0) {
                setUnableRedo();
            }
        } else {
        }
    }

    private void redrawCanvas() {
        canvas.drawColor(Color.WHITE);
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            gridView.setVisibility(View.GONE);
            layoutSizeAndOpacity.setVisibility(View.GONE);
            undonePaths.clear();
            setEnableUndo();
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
            paths.add(path);

            sentImgage();


        }
        return super.onTouchEvent(event);
    }


}
