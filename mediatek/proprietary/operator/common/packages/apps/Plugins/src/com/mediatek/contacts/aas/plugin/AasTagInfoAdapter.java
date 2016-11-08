package com.mediatek.contacts.aas.plugin;

import com.mediatek.op03.plugin.R;

//import com.mediatek.common.telephony.AlphaTag;
import com.mediatek.internal.telephony.uicc.AlphaTag;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AasTagInfoAdapter extends BaseAdapter {
    private final static String TAG = "CustomAasAdapter";
    public final static int MODE_NORMAL = 0;
    public final static int MODE_EDIT = 1;
    private int mMode = MODE_NORMAL;

    private Context mContext = null;
    private LayoutInflater mInflater = null;
    //private int mSlotId = -1;
    private int mSubId = -1;
    private ToastHelper mToastHelper = null;
    private static AasTagInfoAdapter sInstance = null;

    private ArrayList<TagItemInfo> mTagItemInfos = new ArrayList<TagItemInfo>();

    public AasTagInfoAdapter(Context context, int subId) {
        mContext = context;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mSubId = subId;
        mToastHelper = new ToastHelper(context);
    }

    public static AasTagInfoAdapter getInstance(Context context,int subId, boolean mode) {
        if(mode == false) {
            LogUtils.d(TAG, "getInstance same");
            return sInstance ;
        }
        sInstance = new AasTagInfoAdapter(context, subId);
        LogUtils.d(TAG, "getInstance new");
        return sInstance;
    
    }
    
    public void updateAlphaTags(boolean mode) {
        if(mode == true) {
            mTagItemInfos.clear();
            SimUtils.refreshAASList(mSubId);
            List<AlphaTag> list = SimUtils.getAAS(mSubId);
            for (AlphaTag tag : list) {
                TagItemInfo tagItemInfo = new TagItemInfo(tag);
                mTagItemInfos.add(tagItemInfo);
                LogUtils.d(TAG, "updateUSIMAAS: " + tag.getPbrIndex());
                LogUtils.d(TAG, "updateUSIMAAS: " + tag.getRecordIndex());
                LogUtils.d(TAG, "updateUSIMAAS: " + tag.getAlphaTag());
            }
            notifyDataSetChanged();
        }
    }

    public void setMode(int mode) {
        LogUtils.d(TAG, "setMode " + mode);
        if (mMode != mode) {
            mMode = mode;
            if (isMode(MODE_NORMAL)) {
                for (TagItemInfo tagInfo : mTagItemInfos) {
                    tagInfo.mChecked = false;
                }
            }
            notifyDataSetChanged();
        }
    }

    public boolean isMode(int mode) {
        return mMode == mode;
    }

    @Override
    public int getCount() {
        return mTagItemInfos.size();
    }

    @Override
    public TagItemInfo getItem(int position) {
        return mTagItemInfos.get(position);
    }

    public void setChecked(int position, boolean checked) {
        TagItemInfo tagInfo = getItem(position);
        tagInfo.mChecked = checked;
        notifyDataSetChanged();
    }

    public void updateChecked(int position) {
        TagItemInfo tagInfo = getItem(position);
        tagInfo.mChecked = !tagInfo.mChecked;
        notifyDataSetChanged();
    }

    public void setAllChecked(boolean checked) {
        LogUtils.d(TAG, "setAllChecked: " + checked);
        for (TagItemInfo tagInfo : mTagItemInfos) {
            tagInfo.mChecked = checked;
        }
        notifyDataSetChanged();
    }

    public void deleteCheckedAasTag() {
        for (TagItemInfo tagInfo : mTagItemInfos) {
            if (tagInfo.mChecked) {
                boolean success = SimUtils.removeUSIMAASById(mSubId, tagInfo.mAlphaTag.getRecordIndex(),
                        tagInfo.mAlphaTag.getPbrIndex());
                if (!success) {
                    String msg = mContext.getResources().getString(R.string.aas_delete_fail,
                            tagInfo.mAlphaTag.getAlphaTag());
                    mToastHelper.showToast(msg);
                    LogUtils.d(TAG, "delete failed:" + tagInfo.mAlphaTag.getAlphaTag());
                }
            }
        }
        updateAlphaTags(true);
    }

    public int getCheckedItemCount() {
        int count = 0;
        if (isMode(MODE_EDIT)) {
            for (TagItemInfo tagInfo : mTagItemInfos) {
                if (tagInfo.mChecked) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public Boolean isExist(String text) {
        for (int i = 0; i < mTagItemInfos.size(); i++) {
            if (mTagItemInfos.get(i).mAlphaTag.getAlphaTag().equals(text)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        final int maxCount = SimUtils.getAASMaxCount(mSubId);
        LogUtils.d(TAG, "isFull():" + getCount() + ", maxCount=" + maxCount);
        return getCount() >= maxCount;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.custom_aas_item, null);
            TextView tagView = (TextView) convertView.findViewById(R.id.aas_item_tag);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.aas_edit);
            CheckBox checkBox = (CheckBox) convertView.findViewById(R.id.aas_item_check);
            viewHolder = new ViewHolder(tagView, imageView, checkBox);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        TagItemInfo tag = getItem(position);
        viewHolder.mTagView.setText(tag.mAlphaTag.getAlphaTag());

        if (isMode(MODE_NORMAL)) {
            viewHolder.mEditView.setVisibility(View.VISIBLE);
            viewHolder.mCheckBox.setVisibility(View.GONE);
            // viewHolder.mEditView.setOnClickListener()
        } else {
            viewHolder.mEditView.setVisibility(View.GONE);
            viewHolder.mCheckBox.setVisibility(View.VISIBLE);
            viewHolder.mCheckBox.setChecked(tag.mChecked);
        }
        return convertView;
    }

    private static class ViewHolder {
        TextView mTagView;
        ImageView mEditView;
        CheckBox mCheckBox;

        public ViewHolder(TextView textView, ImageView imageView, CheckBox checkBox) {
            mTagView = textView;
            mEditView = imageView;
            mCheckBox = checkBox;
        }
    }

    public static class TagItemInfo {
        AlphaTag mAlphaTag = null;
        boolean mChecked = false;

        public TagItemInfo(AlphaTag tag) {
            mAlphaTag = tag;
        }
    }
}
