package com.ptn.test_drawing;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity {


    ImageButton btnMenu;
    GridView gridView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        List<Item_draw> image_details = getListData();
        gridView.setAdapter(new CustomGridAdapter(this, image_details));

        // When the user clicks on the GridItem
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> a, View v, int position, long id) {
                Object o = gridView.getItemAtPosition(position);
                Item_draw itemdraw = (Item_draw) o;
            }
        });




    }



    private List<Item_draw> getListData() {
        List<Item_draw> list = new ArrayList<Item_draw>();
        Item_draw rule = new Item_draw("Rule");
        Item_draw document = new Item_draw("Document");
        Item_draw paint = new Item_draw("Paint");
        Item_draw fill = new Item_draw("Fill");
        Item_draw eraser = new Item_draw("Eraser");
        Item_draw text = new Item_draw("Text");


        list.add(rule);
        list.add(document);
        list.add(paint);
        list.add(fill);
        list.add(eraser);

        return list;
    }

}