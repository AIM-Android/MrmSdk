package mrm.demo.util;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class SpinnerCustomAdapter extends BaseAdapter {
    Context mCtx = null;
    ArrayList<SpinnerItem> mItemList = new ArrayList<SpinnerItem>();

    public SpinnerCustomAdapter(Context ctx, ArrayList<SpinnerItem> ItemList) {
        mCtx = ctx;
        mItemList = ItemList;
    }

    @Override
    public int getCount() {
        return mItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return mItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SpinnerItem currentItem = mItemList.get(position);
        TextView textView = new TextView (mCtx);
        textView.setTextSize(15.0f);
        textView.setPadding(5,10,0,10);
        textView.setText (currentItem.displayStr );
        textView.setTextColor (Color.BLACK);
        return textView;
    }

}
