
package com.mediatek.rcs.message.ui;


import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.mediatek.widget.AccountItemView;
import com.mediatek.rcs.common.binder.RCSServiceManager;
import com.mediatek.rcs.message.R;
import com.mediatek.rcs.message.utils.RcsMessageUtils;


public class RcsSubChooseAdapter extends BaseAdapter {
    private LayoutInflater mInf;
    Context mContext;
    List<SubscriptionInfo> mList;
    public RcsSubChooseAdapter(Context context, List<SubscriptionInfo> list) {
        mInf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContext = context;
        mList = list;
    }
    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = mInf.inflate(R.layout.rcs_sub_select_item, null);
        AccountItemView accountView;
        if (convertView != null && (convertView instanceof AccountItemView)) {
            accountView = (AccountItemView) convertView;
        } else {
            accountView = (AccountItemView) view.findViewById(R.id.subItem);
        }
        SubscriptionInfo subRecord = mList.get(position);
        // Set theme of the item is LIGHT
        // accountView.setThemeType(SubscriptionView.LIGHT_THEME);
        if (subRecord.getSimSlotIndex() == SubscriptionManager.SIM_NOT_INSERTED) {
            accountView.setAccountName(subRecord.getDisplayName().toString());
            accountView.setAccountNumber(null);
            accountView.findViewById(com.android.internal.R.id.icon).setVisibility(View.GONE);
            accountView.setClickable(true);
        } else {
            accountView.setClickable(false);
            accountView.setAccountIcon(new BitmapDrawable(mContext.getResources(), subRecord
                    .createIconBitmap(mContext)));
            accountView.setAccountName(subRecord.getDisplayName().toString());
            accountView.setAccountNumber(subRecord.getNumber());
            accountView.findViewById(com.android.internal.R.id.icon).setVisibility(View.VISIBLE);
            TextView tvTitle = (TextView)accountView.findViewById(com.android.internal.R.id.title);
            tvTitle.setTextColor(Color.BLACK);
            TextView tvSummary = (TextView)accountView.findViewById(com.android.internal.R.id.summary);
            tvSummary.setTextColor(Color.BLACK);
        }
        ImageView rcsIcon = (ImageView) view.findViewById(R.id.rcsicon);
        rcsIcon.setVisibility(View.GONE);
        int rcsSubId = SubscriptionManager.getDefaultDataSubId();
        if (SubscriptionManager.isValidSubscriptionId(rcsSubId)) {
            if (subRecord.getSubscriptionId() == rcsSubId && RcsMessageUtils.getConfigStatus()) {
                rcsIcon.setVisibility(View.VISIBLE);
                if (RCSServiceManager.getInstance().serviceIsReady()) {
                    rcsIcon.setImageResource(R.drawable.ic_rcs_list_light);
                }
            }
        }
        return view;
    }

    public void setAdapterData(List<SubscriptionInfo> list) {
        mList = list;
    }

}
