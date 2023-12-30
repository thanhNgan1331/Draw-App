package com.ptn.test_drawing;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ptn.test_drawing.itemL.CustomListAdapter;
import com.ptn.test_drawing.itemL.Item_draw;
import com.ptn.test_drawing.usingSticker.HelloIconEvent;
import com.xiaopo.flying.sticker.BitmapStickerIcon;
import com.xiaopo.flying.sticker.DeleteIconEvent;
import com.xiaopo.flying.sticker.DrawableSticker;
import com.xiaopo.flying.sticker.FlipHorizontallyEvent;
import com.xiaopo.flying.sticker.Sticker;
import com.xiaopo.flying.sticker.StickerView;
import com.xiaopo.flying.sticker.TextSticker;
import com.xiaopo.flying.sticker.ZoomIconEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


import yuku.ambilwarna.AmbilWarnaDialog;


public class ActivityPaint extends AppCompatActivity {

    private DrawView paint;
    private ImageView
            btnUndo, btnRedo, btnColor, btnPen, btnMenu, btnEraser,
            btnFullScreenHide, btnFullScreenShow,
            btnCircle, btnRect, btnLine,
            btnCloseEditText, btnChooseSizeText, btnFlipText;
    LinearLayout layoutMenu;

    int DefaultColor = Color.BLACK;

    ListView listItemForNew, listMenu;
    LinearLayout layoutSizeAndOpacity, shapeLayout, layoutSizeEraser, textLayout;

    SeekBar seekBarSize, seekBarOpacity, seekBarEraser;
    TextView txtCountSize, txtCountOpacity, txtCountEraser;

    String ip;
    int port;

    private ProgressDialog progressDialog;
    private Connection connection;

    int flagNew = 0;

    private static final String TAG = "ABCDEFGHIJKL";

    // Khai báo các biến dùng cho vẽ text
    TextSticker textSticker;


    // Khởi tạo dữ liệu cho menu
    private List<Item_draw> getListDataMenu() {
        List<Item_draw> list = new ArrayList<Item_draw>();
        Item_draw newImage = new Item_draw("new_page");
        Item_draw save = new Item_draw("save");
        Item_draw openImage = new Item_draw("image");
        Item_draw text = new Item_draw("text");
        Item_draw shapes = new Item_draw("shapes");
        Item_draw exit = new Item_draw("exit");

        list.add(newImage);
        list.add(save);
        list.add(openImage);
        list.add(text);
        list.add(shapes);
        list.add(exit);

        return list;
    }


    private List<Item_draw> getListDataList() {
        List<Item_draw> list = new ArrayList<Item_draw>();
        Item_draw save = new Item_draw("save");
        Item_draw discard = new Item_draw("discard");

        list.add(save);
        list.add(discard);

        return list;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hiện thị ứng dụng full màn hình
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_paint);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        paint = findViewById(R.id.draw_view);
        btnRedo = findViewById(R.id.btnRedo);
        btnUndo = findViewById(R.id.btnUndo);
        btnColor = findViewById(R.id.btnColor);
        btnMenu = findViewById(R.id.btnMenu);
        btnPen = findViewById(R.id.btnPen);
        btnEraser = findViewById(R.id.btnEraser);

        // Các button vẽ hình
        btnCircle = findViewById(R.id.btnCircle);
        btnRect = findViewById(R.id.btnRect);
        btnLine = findViewById(R.id.btnLine);

        // Các button full screen
        btnFullScreenHide = findViewById(R.id.btnFullScreenHide);
        btnFullScreenShow = findViewById(R.id.btnFullScreenShow);

        // Các button text
        btnCloseEditText = findViewById(R.id.btnCloseEditText);
        btnChooseSizeText = findViewById(R.id.btnChooseSizeText);
        btnFlipText = findViewById(R.id.btnFlipText);

        // Các seekbar
        seekBarSize = findViewById(R.id.seekBarSize);
        seekBarOpacity = findViewById(R.id.seekBarOpacity);
        seekBarEraser = findViewById(R.id.seekBarSizeEraser);

        // Các textview
        txtCountSize = findViewById(R.id.txtCountSize);
        txtCountOpacity = findViewById(R.id.txtCountOpacity);
        txtCountEraser = findViewById(R.id.txtCountSizeEraser);

        // Các layout
        layoutMenu = findViewById(R.id.layoutMenu);
        layoutSizeAndOpacity = findViewById(R.id.layoutSizeAndOpacity);
        layoutSizeEraser = findViewById(R.id.layoutSizeEraser);
        shapeLayout = findViewById(R.id.shapeLayout);
        textLayout = findViewById(R.id.textLayout);

        // Các listview
        listItemForNew = findViewById(R.id.listItemForNew);
        listMenu = findViewById(R.id.listMenu);

        // Dùng để truyền dữ liệu từ các view trong Activity sang class DrawView
        paint.setObjectInActivity(listMenu, listItemForNew, layoutMenu, layoutSizeAndOpacity, layoutSizeEraser, shapeLayout, btnUndo, btnRedo);

        layoutMenu.bringToFront();

        Intent intent = getIntent();
        ip = intent.getStringExtra("ip_key");
        port = intent.getIntExtra("port_key", 6862);
        Log.d("IPadddd", "" + ip);
        connection = new Connection(ip, port);

        ViewTreeObserver vto = paint.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                paint.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = paint.getMeasuredWidth();
                int height = paint.getMeasuredHeight();
                paint.init(height, width, ip, port);
            }
        });


        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paint.undo();
            }
        });
        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paint.redo();
            }
        });

        btnColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                OpenColorPickerDialog(false);
            }
        });

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutSizeAndOpacity.setVisibility(View.GONE);
                layoutSizeEraser.setVisibility(View.GONE);
                shapeLayout.setVisibility(View.GONE);
                showHide(v);
            }
        });


        List<Item_draw> itemList = getListDataList();
        listItemForNew.setAdapter(new CustomListAdapter(this, itemList));
        listItemForNew.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = listItemForNew.getItemAtPosition(position);
                switch (position) {
                    case 0: // Save
                        flagNew = 1;
                        btnSaveImage(v);
                        break;
                    case 1: // Discard
                        showAlertDialog();
                        break;
                }
            }
        });


        List<Item_draw> itemMenu = getListDataMenu();
        listMenu.setAdapter(new CustomListAdapter(this, itemMenu));
        listMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = listMenu.getItemAtPosition(position);
                switch (position) {
                    case 0: // New menu
                        listMenu.setVisibility(View.GONE);
                        listItemForNew.setVisibility(View.VISIBLE);
                        break;
                    case 1: // Save
                        flagNew = 0;
                        btnSaveImage(v);
                        break;
                    case 2: // Open Image
                        btnOpenImage(v);
                        break;
                    case 3: // Text

                        textLayout.setVisibility(View.VISIBLE);
                        textLayout.bringToFront();
                        listMenu.setVisibility(View.GONE);

                        textSticker = paint.addText();
                        paint.addSticker(textSticker);

                        paint.setTouchMode(DrawView.TouchMode.STICKERVIEW);
                        paint.configDefaultIcons(); // Gọi sau khi khởi tạo paint
                        paint.setLocked(false);
                        paint.setConstrained(true);
                        paint.setOnStickerOperationListener(new StickerView.OnStickerOperationListener() {
                            @Override
                            public void onStickerAdded(@NonNull Sticker sticker) {
                                Log.d(TAG, "onStickerAdded");
                            }

                            @Override
                            public void onStickerClicked(@NonNull Sticker sticker) {
                                if (sticker instanceof TextSticker) {
                                    ((TextSticker) sticker).setText("Hello, world!\nHello, world!");
                                    paint.replace(sticker);
                                    paint.invalidate();
                                }
                                Log.d(TAG, "onStickerClicked");
                            }

                            @Override
                            public void onStickerDeleted(@NonNull Sticker sticker) {
                                Log.d(TAG, "onStickerDeleted");
                            }

                            @Override
                            public void onStickerDragFinished(@NonNull Sticker sticker) {
                                Log.d(TAG, "onStickerDragFinished");
                            }

                            @Override
                            public void onStickerTouchedDown(@NonNull Sticker sticker) {
                                Log.d(TAG, "onStickerTouchedDown");
                            }

                            @Override
                            public void onStickerZoomFinished(@NonNull Sticker sticker) {
                                Log.d(TAG, "onStickerZoomFinished");
                            }

                            @Override
                            public void onStickerFlipped(@NonNull Sticker sticker) {
                                Log.d(TAG, "onStickerFlipped");
                            }

                            @Override
                            public void onStickerDoubleTapped(@NonNull Sticker sticker) {
                                Log.d(TAG, "onDoubleTapped: double tap will be with two click");
                            }
                        });


                        break;
                    case 4: // Shapes
                        shapeLayout.setVisibility(View.VISIBLE);
                        shapeLayout.bringToFront();
                        listMenu.setVisibility(View.GONE);
                        break;
                    case 5: // Exit
                        btnLogout(v);
                        break;
                    case 6: // Switch user
                        paint.setTouchMode(DrawView.TouchMode.DRAWVIEW);
                        break;
                }
            }
        });

        // Các button vẽ hình
        btnLine.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shapeLayout.setVisibility(View.GONE);
                paint.drawShapeStatus(false);//để tắt toàn bộ trạng thái vẽ circle,rect
                paint.drawShapeStatus(true);
                paint.drawLineStatus(true);
            }
        });
        btnRect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shapeLayout.setVisibility(View.GONE);
                paint.drawShapeStatus(false);//để tắt toàn bộ trạng thái vẽ circle,line
                paint.drawShapeStatus(true);
                paint.drawRectStatus(true);
            }
        });
        btnCircle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                shapeLayout.setVisibility(View.GONE);
                paint.drawShapeStatus(false);//để tắt toàn bộ trạng thái vẽ rect,line
                paint.drawShapeStatus(true);
                paint.drawCircleStatus(true);

            }
        });

        // Các button vẽ text
        btnCloseEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paint.setTouchMode(DrawView.TouchMode.DRAWVIEW);
                textLayout.setVisibility(View.GONE);
            }
        });

        btnChooseSizeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutSizeAndOpacity.setVisibility(View.VISIBLE);
                layoutSizeAndOpacity.bringToFront();
            }
        });

        btnFlipText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //paint.flipText();

            }
        });

        // Các button full screen
        btnFullScreenHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutMenu.setVisibility(View.GONE);
                btnFullScreenShow.setVisibility(View.VISIBLE);
                btnFullScreenShow.bringToFront();
            }
        });
        btnFullScreenShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutMenu.setVisibility(View.VISIBLE);
                btnFullScreenShow.setVisibility(View.GONE);
            }
        });


        txtCountSize.setText(seekBarSize.getProgress() + "");
        txtCountOpacity.setText(seekBarOpacity.getProgress() + "%");
        txtCountEraser.setText(seekBarEraser.getProgress() + "");


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

                int percentage = (int) ((progress * 255) / 100.0);
                txtCountOpacity.setText(progress + "%");
                paint.setAlpha(percentage);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarEraser.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtCountEraser.setText(progress + "");
                paint.setStrokeWidth(progress);
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
                paint.erasingStatus(false);
                paint.drawShapeStatus(false);
                listMenu.setVisibility(View.GONE);
                layoutSizeEraser.setVisibility(View.GONE);
                shapeLayout.setVisibility(View.GONE);
                if (layoutSizeAndOpacity.getVisibility() == View.VISIBLE) {
                    layoutSizeAndOpacity.setVisibility(View.GONE);
                } else {
                    layoutSizeAndOpacity.setVisibility(View.VISIBLE);
                    layoutSizeAndOpacity.bringToFront();
                }
            }
        });

        btnEraser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paint.erasingStatus(true);
                paint.drawShapeStatus(false);
                listMenu.setVisibility(View.GONE);
                layoutSizeAndOpacity.setVisibility(View.GONE);
                shapeLayout.setVisibility(View.GONE);
                if (layoutSizeEraser.getVisibility() == View.VISIBLE) {
                    layoutSizeEraser.setVisibility(View.GONE);
                } else {
                    layoutSizeEraser.setVisibility(View.VISIBLE);
                    layoutSizeEraser.bringToFront();
                }
            }
        });
    }

    // Phương thức để hiển thị dialog chỉnh sửa văn bản
    private void showEditDialog(final TextSticker textSticker) {
        final EditText editText = new EditText(this);
        editText.setText(textSticker.getText());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Text")
                .setView(editText)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newText = editText.getText().toString();
                        textSticker.setText(newText);
                        paint.replace(textSticker);
                        paint.invalidate();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    public void textAdd(View view) {
        final TextSticker sticker = new TextSticker(this);
        sticker.setText("Hello, world!");
        sticker.setTextColor(Color.BLUE);
        sticker.resizeText();
        paint.addSticker(sticker);
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
                Bitmap selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                selectedBitmap = Bitmap.createScaledBitmap(selectedBitmap, paint.getMeasuredWidth(), paint.getMeasuredHeight(), false);
                paint.setImageBackground(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("SetImage", "Not Success: " + e.getMessage());

            }
        }
    }

    public void btnLogout(View view) {
        connection.sendData("logout");
        Intent intent = new Intent(this, ConnectToTheServerActivity.class);
        startActivity(intent);
        finish();
    }

    public void btnSaveImage(View view) {
        // Tạo và hiển thị ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang lưu ảnh...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        Uri images;
        Bitmap bitmap = paint.save();
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
            OutputStream outputStream = contentResolver.openOutputStream(Objects.requireNonNull(uri));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            Objects.requireNonNull(outputStream);
        } catch (Exception e) {
            Toast.makeText(this, "Lưu ảnh thất bại", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            // Đóng ProgressDialog sau một khoảng thời gian
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    paint.hideAllLayout();
                    progressDialog.dismiss();
                    if (flagNew == 1)
                        paint.newImage();
                }
            }, 1000);
        }
    }

    private void showAlertDialog() {
        // Tạo một đối tượng AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Thiết lập tiêu đề và thông điệp cho thông báo
        builder.setTitle("Discard current Sketch");
        builder.setMessage("You will lose your sketch if you discard it. Are you sure you want to discard?");

        // Thiết lập nút OK và hành động khi nút đó được nhấn
        builder.setPositiveButton("DISCARD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Hành động khi nút OK được nhấn
                paint.newImage();
            }
        });
        // Thiết lập nút No và hành động khi nút đó được nhấn
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Hành động khi nút No được nhấn
                dialog.dismiss();
            }
        });

        // Tạo và hiển thị AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    // Hàm ẩn hiện gridview
    public void showHide(View view) {
        if (listMenu.getVisibility() == View.VISIBLE) {
            listMenu.setVisibility(View.GONE);
        } else {
            listMenu.setVisibility(View.VISIBLE);
            listMenu.bringToFront();
        }
    }


    private void OpenColorPickerDialog(boolean AlphaSupport) {

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(ActivityPaint.this, DefaultColor, AlphaSupport, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog ambilWarnaDialog, int color) {
                paint.setColor(color);
            }

            @Override
            public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {

                Toast.makeText(ActivityPaint.this, "Color Picker Closed", Toast.LENGTH_SHORT).show();
            }
        });
        ambilWarnaDialog.show();

    }
}
