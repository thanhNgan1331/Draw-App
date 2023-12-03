package com.ptn.test_drawing.itemL;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.ptn.test_drawing.R;

import java.util.List;

public class CustomGridAdapter  extends BaseAdapter {

    private List<Item_draw> listData;
    private LayoutInflater layoutInflater;
    private Context context;

    public CustomGridAdapter(Context aContext,  List<Item_draw> listData) {
        this.context = aContext;
        this.listData = listData;
        layoutInflater = LayoutInflater.from(aContext);
    }

    @Override
    public int getCount() {
        return listData.size();
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.grid_item_layout, null);
            holder = new ViewHolder();
            holder.itemView = (ImageView) convertView.findViewById(R.id.imgaeView_item);
            holder.itemName = (TextView) convertView.findViewById(R.id.textView_itemName);
            holder.constraintLayout = (ConstraintLayout) convertView.findViewById(R.id.constraintLayout_item);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Item_draw itemdraw = this.listData.get(position);
        holder.itemName.setText(capitalizeFirstLetter(itemdraw.getitemName()));

        int imageId = this.getMipmapResIdByName(itemdraw.getitemName());

        holder.itemView.setImageResource(imageId);


        return convertView;
    }


    public static String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return Character.toUpperCase(input.charAt(0)) + input.substring(1).toLowerCase();
    }

    // Find Image ID corresponding to the name of the image (in the directory mipmap).
    public int getMipmapResIdByName(String resName)  {
        String pkgName = context.getPackageName();

        // Return 0 if not found.
        int resID = context.getResources().getIdentifier(resName , "drawable", pkgName);
        Log.i("CustomGridView", "Res Name: "+ resName+"==> Res ID = "+ resID);
        return resID;
    }

    static class ViewHolder {
        ImageView itemView;
        TextView itemName;
        ConstraintLayout constraintLayout;
    }

}