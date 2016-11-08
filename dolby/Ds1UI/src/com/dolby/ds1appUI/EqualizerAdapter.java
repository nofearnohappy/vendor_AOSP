/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2011-2012 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.ds1appUI;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.dolby.ds1appCoreUI.DS1Application;
import com.dolby.ds1appCoreUI.Tag;

public class EqualizerAdapter extends BaseAdapter implements
        View.OnTouchListener {

    private final ArrayList<EqualizerSetting> mSettings;
    private final int mLayout;
    private int mSelectedPosition = -1;

    private final LayoutInflater mInflater;

    private final Drawable mSelectedBg;

    private final IPresetListener mListener;

    private boolean mDobyOn = true;

    private boolean mNewLayout = false;

    public static interface IPresetListener {

        void onPresetChanged(int position);

    }

    public EqualizerAdapter(Context context, int layout,
            IPresetListener listener) {
        super();

        mInflater = LayoutInflater.from(context);
        mListener = listener;

        mSelectedBg = context.getResources().getDrawable(R.drawable.eqlistsel);

        mLayout = layout;
        mSelectedPosition = 0;

        // create default set of equalizer settings
        mNewLayout = context.getResources().getBoolean(R.bool.newLayout);
        mSettings = new ArrayList<EqualizerSetting>();
        mSettings.add(new EqualizerSetting(context.getString(R.string.open), R.drawable.eq1sel, R.drawable.eq1, R.drawable.eq1dis));
        mSettings.add(new EqualizerSetting(context.getString(R.string.rich), R.drawable.eq2sel, R.drawable.eq2, R.drawable.eq2dis));
        mSettings.add(new EqualizerSetting(context.getString(R.string.focused), R.drawable.eq3sel, R.drawable.eq3, R.drawable.eq3dis));
        if (mNewLayout) {
            mSettings.add(new EqualizerSetting(context.getString(R.string.custom), R.drawable.eq4sel, R.drawable.eq4, R.drawable.eq4dis));
        }
    }

    @Override
    public int getCount() {
        return mSettings.size();
    }

    @Override
    public EqualizerSetting getItem(int position) {
        return mSettings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Log.d(Tag.MAIN, "EqualizerAdapter.getView " + position);
        View row = convertView;

        if (convertView == null) {
            row = mInflater.inflate(mLayout, null);
            row.setOnTouchListener(this);
        }

        final EqualizerSetting item = mSettings.get(position);
        final boolean enabled = parent.isEnabled();
        final boolean selected = (position == mSelectedPosition) && enabled;
        final ImageView icon = (ImageView) row.findViewById(R.id.icon);

        icon.setImageResource(item.getIcon(selected, enabled));
        if (mNewLayout) {
            row.setBackgroundResource(selected ? R.drawable.eqlistsel : R.drawable.eqlistoff);
        } else {
            row.setBackground(selected ? mSelectedBg : null);
        }
        // row.setBackgroundColor(selected ? Color.rgb(0x16, 0x30, 0x5c) :
        // Color.TRANSPARENT);
        row.setTag(position);

        return row;
    }

    public int getSelection() {
        return mSelectedPosition;
    }

    public void setSelection(int position) {
        if (mSelectedPosition != position) {
            mSelectedPosition = position;
            scheduleNotifyDataSetChanged();
        }
    }

    public void setDolbyOnOff(boolean on) {
        mDobyOn = on;
    }

    @Override
    public void notifyDataSetChanged() {
        Log.d(Tag.MAIN, "EqualizerAdapter.notifyDataSetChanged");
        super.notifyDataSetChanged();
    }

    public void scheduleNotifyDataSetChanged() {
        Log.d(Tag.MAIN, "EqualizerAdapter.scheduleNotifyDataSetChanged");
        DS1Application.HANDLER.removeCallbacks(mNotifyDataSetChanged);
        DS1Application.HANDLER.post(mNotifyDataSetChanged);
    }

    private final Runnable mNotifyDataSetChanged = new Runnable() {

        @Override
        public void run() {
            notifyDataSetChanged();
        }
    };

    @Override
    public boolean onTouch(View v, MotionEvent e) {

        if (!mDobyOn)
            return false;

        final int action = e.getAction();
        if (MotionEvent.ACTION_DOWN == action || MotionEvent.ACTION_UP == action) {
            if (MotionEvent.ACTION_DOWN == action && mListener != null) {
                final Integer nPos = (Integer) v.getTag();
                if (nPos != null) {
                    mListener.onPresetChanged(nPos);
                }
            }
            return true;
        }
        return false;
    }
}
