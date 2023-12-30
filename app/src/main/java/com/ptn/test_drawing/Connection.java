package com.ptn.test_drawing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Base64;
import android.view.View;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connection {
    String ip;
    int port;
    public Connection(String ip, int port){
        this.ip = ip;
        this.port = port;
    }
    public void sendData(String mess) {
        try {
            Client_send c = new Client_send();
            c.execute(mess);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String convertImg(Bitmap mBitmap) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            String imageString = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            return imageString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap getBitmapFromViewUsingCanvas(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        view.draw(canvas);

        return bitmap;
    }


    class Client_send extends AsyncTask<String, Void, Void> {
        Socket s;
        PrintWriter writer;

        @Override
        protected Void doInBackground(String... voids) {
            try {
                String mess = voids[0];
                s = new Socket(ip, port);
                writer = new PrintWriter(s.getOutputStream());
                InputStreamReader streamReader = new InputStreamReader(s.getInputStream());
                BufferedReader reader = new BufferedReader(streamReader);
                writer.write(mess);
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }
    }

}
