package com.ptn.test_drawing;

public class Item_draw {

    private String itemName;

    public Item_draw(String itemName) {
        this.itemName= itemName;
    }


    public String getitemName() {
        return itemName;
    }


    @Override
    public String toString()  {
        return this.itemName;
    }
}