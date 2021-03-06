package cn.diaovision.omnicontrol.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import cn.diaovision.omnicontrol.R;

/**
 * Created by TaoYimin on 2017/6/7.
 */

public class CustomSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    private Context context;
    private LayoutInflater inflater;
    private String[] arrays;
    private int[] images;

    public CustomSpinnerAdapter(Context context, String[] arrays, int[] images) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.arrays = arrays;
        this.images = images;
    }

    @Override
    public int getCount() {
        return arrays.length;
    }

    @Override
    public Object getItem(int position) {
        return arrays[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * 加载显示布局
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.spinner_item, null);
            holder.textView1 = (TextView) convertView.findViewById(R.id.text1);
            holder.textView2 = (TextView) convertView.findViewById(R.id.text2);
            holder.imageView = (ImageView) convertView.findViewById(R.id.icon);
            holder.button = (ImageView) convertView.findViewById(R.id.button_select);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (images != null && images.length != 0) {
            holder.textView1.setText(arrays[position]);
            holder.imageView.setImageResource(images[position]);
        }else{
            holder.textView2.setText(arrays[position]);
        }
        holder.button.setVisibility(View.VISIBLE);
        return convertView;
    }

    /**
     * 加载下拉布局
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.spinner_item, null);
            holder.textView1 = (TextView) convertView.findViewById(R.id.text1);
            holder.textView2 = (TextView) convertView.findViewById(R.id.text2);
            holder.imageView = (ImageView) convertView.findViewById(R.id.icon);
            holder.button = (ImageView) convertView.findViewById(R.id.button_select);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (images != null && images.length != 0) {
            holder.textView1.setText(arrays[position]);
            holder.imageView.setImageResource(images[position]);
        }else{
            holder.textView2.setText(arrays[position]);
        }
        holder.button.setVisibility(View.INVISIBLE);
        return convertView;
    }

    public static class ViewHolder {
        TextView textView1;
        TextView textView2;
        ImageView imageView;
        ImageView button;
    }
}
