package com.ptn.test_drawing;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectToTheServerActivity extends AppCompatActivity {
    EditText txtIP, txtPort;
    TextView txtHeader;
    Button btnConnect;
    ProgressBar progressBar;
    TextView txtContinueWithoutConnection, txtProcessing;
    String ip;
    int port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hiện thị ứng dụng full màn hình
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.connect_to_the_server);

        txtIP = findViewById(R.id.txtIP);
        txtPort = findViewById(R.id.txtPort);
        btnConnect = findViewById(R.id.btnConnect);
        progressBar = findViewById(R.id.progressBar);
        txtHeader = findViewById(R.id.txtHeader);
        txtContinueWithoutConnection = findViewById(R.id.txtContinueWithoutConnection);
        txtProcessing = findViewById(R.id.txtProcessing);

        txtIP.addTextChangedListener(new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (!isValid(text)) {
                    txtIP.setText(text.substring(0, text.length() - 1));
                    txtIP.setSelection(txtIP.length());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ip = txtIP.getText().toString();
                String portString = (txtPort.getText().toString());

                if (!TextUtils.isEmpty(ip) && !TextUtils.isEmpty(portString)) {
                    port = Integer.parseInt(portString);
                    Toast.makeText(getApplicationContext(), "Đang kết nối tới server...", Toast.LENGTH_SHORT).show();
                    // Gửi yêu cầu kết nối tới server
                    progressBar.setVisibility(View.VISIBLE); // Hiển thị ProgressBar
                    txtProcessing.setVisibility(View.VISIBLE);
                    sendData_v3(ip, port);
                } else {
                    Toast.makeText(getApplicationContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                }
            }
        });

        txtContinueWithoutConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConnectToTheServerActivity.this, ActivityPaint.class);
                // Bắt đầu Activity mới
                startActivity(intent);
            }
        });
    }

    private boolean isValid(String s) {
        String regex = "[0-9.]*";
        return s.matches(regex);
    }


    private CountDownTimer connectionTimer = new CountDownTimer(10000, 1000) { // Thời gian tối đa 10 giây
        public void onTick(long millisUntilFinished) {
            txtProcessing.setText("Đang kết nối tới server... (" + millisUntilFinished / 1000 + " giây)");
        }

        public void onFinish() {
            // Hết thời gian chờ mà không kết nối được
            progressBar.setVisibility(View.GONE);
            Toast.makeText(getApplicationContext(), "Không thể kết nối tới server", Toast.LENGTH_SHORT).show();
            connectionTimer.cancel(); // Hủy bộ đếm
            txtProcessing.setVisibility(View.GONE);
        }
    };


    public void sendData_v3(String ip, int port)
    {
        connectionTimer.start();
        String mess = "1";
        try {
            ConnectToServerTask c1 = new ConnectToServerTask(ip, port);
            c1.execute(mess);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private class ConnectToServerTask extends AsyncTask<String, Void, Void>
    {
        private String ip;
        private int port;

        public ConnectToServerTask(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        protected Void doInBackground(String... voids) {
            try (
                    Socket s = new Socket(ip, port);
                    PrintWriter writer = new PrintWriter(s.getOutputStream());
                    InputStreamReader streamReader = new InputStreamReader(s.getInputStream());
                    BufferedReader reader = new BufferedReader(streamReader);
            ) {

                // Lấy dữ liệu từ EditText
                String clientMessage = voids[0];//là string "1": xác định client gửi yêu cầu kết nối tới server

                // Gửi dữ liệu tới server
                writer.println(clientMessage);
                writer.flush();

                // Đọc dữ liệu trả về từ server
                String responseMessage = reader.readLine();
                if (responseMessage.equals("OK")) {
                    // Tạo Intent để mở Activity mới
                    Intent intent = new Intent(ConnectToTheServerActivity.this, ActivityPaint.class);
                    // Đính kèm địa chỉ Ip và Port vào Intent
                    intent.putExtra("ip_key", ip);
                    intent.putExtra("port_key", port);
                    // Bắt đầu Activity mới
                    startActivity(intent);
                    finish();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            txtProcessing.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), "Kết nối thành công", Toast.LENGTH_SHORT).show();
                            connectionTimer.cancel(); // Hủy bộ đếm
                        }
                    });
                } else if (responseMessage.equals("CANCEL")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            txtProcessing.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), "Bị từ chối kết nối", Toast.LENGTH_SHORT).show();
                            connectionTimer.cancel(); // Hủy bộ đếm
                        }
                    });
                } else {
                    // Hiển thị thông báo lỗi
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            txtProcessing.setVisibility(View.GONE);
                            Toast.makeText(getApplicationContext(), "Kết nối thất bại", Toast.LENGTH_SHORT).show();
                            connectionTimer.cancel(); // Hủy bộ đếm

                        }
                    });
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }


}