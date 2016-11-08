package com.mediatek.rcs.pam.ui.accountlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.ui.AccountDetailsActivity;

import java.util.List;

public class AccountSearchAdapter extends BaseAdapter {

    private static final String TAG = Constants.TAG_PREFIX
            + "AccountSearchAdapter";

    private Context mContext;
    private List<PublicAccount> mAccountList;
    private ImageCustomizedLoader mImageCustomizedLoader;

    public AccountSearchAdapter(Context context, List<PublicAccount> accountList) {
        mContext = context;
        mAccountList = accountList;
        mImageCustomizedLoader = new ImageCustomizedLoader(context,
                R.drawable.ic_account_detail,
                ImageCustomizedLoader.TYPE_IMAGE_CIRCLE);
    }

    @Override
    public int getCount() {
        return mAccountList.size();
    }

    @Override
    public PublicAccount getItem(int position) {
        return mAccountList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.i(TAG, "getView " + position);
        if (convertView == null) {
            convertView = (LinearLayout) parseLayout(mContext,
                    R.layout.account_search_item);
        }

        TextView titleView = (TextView) convertView
                .findViewById(R.id.tv_search_title);
        TextView introView = (TextView) convertView
                .findViewById(R.id.tv_search_intro);
        ImageView imageView = (ImageView) convertView
                .findViewById(R.id.iv_search_thumb);

        titleView.setText(getItem(position).name);
        introView.setText(getItem(position).introduction);
        mImageCustomizedLoader
                .updateImage(imageView, getItem(position).logoUrl);

        addClickOperation(convertView, position);
        return convertView;
    }

    public List<PublicAccount> getAccountList() {
        return mAccountList;
    }

    public void setAccountList(List<PublicAccount> accountList) {
        mAccountList = accountList;
        notifyDataSetChanged();
    }

    private View parseLayout(Context context, int resId) {
        return LayoutInflater.from(context).inflate(resId, null);
    }

    private void addClickOperation(View layout, int position) {
        final String uuid = getItem(position).uuid;

        layout.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "Open detail activity " + uuid);

                Intent intent = new Intent(mContext,
                        AccountDetailsActivity.class);
                Bundle arguments = new Bundle();
                arguments.putString(AccountDetailsActivity.KEY_UUID, uuid);
                intent.putExtras(arguments);
                mContext.startActivity(intent);
            }
        });
    }
}
