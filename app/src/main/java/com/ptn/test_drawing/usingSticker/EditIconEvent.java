package com.ptn.test_drawing.usingSticker;

import android.view.MotionEvent;

import com.ptn.test_drawing.Connection;
import com.xiaopo.flying.sticker.StickerIconEvent;
import com.xiaopo.flying.sticker.StickerView;
import com.xiaopo.flying.sticker.TextSticker;

/**
 * @author wupanjie
 * @see StickerIconEvent
 */

public class EditIconEvent implements StickerIconEvent {

    private Connection connection;

    public EditIconEvent(Connection connection) {
          this.connection = connection;
    }

    public EditIconEvent() {
    }

    @Override
    public void onActionDown(StickerView stickerView, MotionEvent event) {

    }

    @Override
    public void onActionMove(StickerView stickerView, MotionEvent event) {

    }

    @Override
    public void onActionUp(StickerView stickerView, MotionEvent event) {
        stickerView.showChange((TextSticker) stickerView.getCurrentSticker(), stickerView);
        connection.sendData(connection.convertImg(connection.getBitmapFromViewUsingCanvas(stickerView)));
    }
}
