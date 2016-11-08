package com.mediatek.mms.plugin;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import com.mediatek.op09.plugin.R;
public class FontSizeDialogAdapter extends BaseAdapter {

    private String[] mChoices;
    private String[] mValues;
    private Context mContext;
    private LayoutInflater mInflater;
    private CheckedTextView mTV;

    public FontSizeDialogAdapter(Context context, String[] choices, String[] values) {
        mContext = context;
        mChoices = choices;
        mValues = values;
        mInflater = LayoutInflater.from(mContext);
    }

    public int getCount() {
        return mChoices.length;
    }

    public Object getItem(int arg0) {
        return mChoices[arg0];
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mInflater.inflate(R.layout.choose_font_size_item, null);
        mTV = (CheckedTextView) convertView.findViewById(android.R.id.text1);
        mTV.setText(mChoices[position]);
        if (position < mChoices.length && position > 0) {
            mTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, Integer.parseInt(mValues[position]));
        }
        return convertView;
    }
}
