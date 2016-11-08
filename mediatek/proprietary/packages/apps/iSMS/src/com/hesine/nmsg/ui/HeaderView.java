package com.hesine.nmsg.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hesine.nmsg.R;

public class HeaderView extends LinearLayout {
    private Context mContext;
    private View convertView;
    private ImageView mBack;
    private TextView mTitle;
    private ImageView mMore;

    public HeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        convertView = LayoutInflater.from(context).inflate(R.layout.header_view, this, true);
        mBack = (ImageView) convertView.findViewById(R.id.back);
        mBack.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity) mContext).finish();
            }
        });
        mTitle = (TextView) convertView.findViewById(R.id.title);
        mMore = (ImageView) convertView.findViewById(R.id.more);
    }

    public void setTitle(final String title) {
        mTitle.setText(title);
    }

    public void setBackRsc(int resId) {
        mBack.setImageResource(resId);
    }

    public View getBackView() {
        return mBack;
    }

    public View getMoreView() {
        return mMore;
    }

    public void setMoreView(int id) {
        mMore.setImageResource(id);
    }
}
