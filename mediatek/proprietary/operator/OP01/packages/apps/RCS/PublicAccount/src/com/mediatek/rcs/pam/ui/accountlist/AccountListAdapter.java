package com.mediatek.rcs.pam.ui.accountlist;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.ui.conversation.PaComposeActivity;

public class AccountListAdapter extends ResourceCursorAdapter {

    private static final String TAG = Constants.TAG_PREFIX
            + "AccountListAdapter";

    private Context mContext;
    private ImageCustomizedLoader mImageCustomizedLoader;

    public AccountListAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        mContext = context;
        mImageCustomizedLoader = new ImageCustomizedLoader(context,
                R.drawable.ic_account_detail,
                ImageCustomizedLoader.TYPE_IMAGE_CIRCLE);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView titleView = (TextView) view
                .findViewById(R.id.tv_account_title);
        ImageView imageView = (ImageView) view
                .findViewById(R.id.iv_account_thumb);

        titleView.setText(cursor.getString(cursor
                .getColumnIndex(AccountColumns.NAME)));

        String logoUrl = cursor.getString(cursor
                .getColumnIndex(AccountColumns.LOGO_URL));
        String logoPath = cursor.getString(cursor
                .getColumnIndex(AccountColumns.LOGO_PATH));

        mImageCustomizedLoader.updateImage(imageView, logoUrl, logoPath);

        addClickOperation(cursor, view);
    }

    private void addClickOperation(Cursor cursor, View view) {
        final long accountId = cursor.getLong(cursor
                .getColumnIndex(AccountColumns.ID));
        final String uuid = cursor.getString(cursor
                .getColumnIndex(AccountColumns.UUID));
        final String logoUrl = cursor.getString(cursor
                .getColumnIndex(AccountColumns.LOGO_URL));
        final String logoPath = cursor.getString(cursor
                .getColumnIndex(AccountColumns.LOGO_PATH));
        final String name = cursor.getString(cursor
                .getColumnIndex(AccountColumns.NAME));

        Log.i(TAG, "AccountId is " + accountId + " uuid is " + uuid);
        Log.i(TAG, "Url is " + logoUrl + " path is " + logoPath);

        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i(TAG, "Show chat " + accountId + ", " + uuid);

                Intent intent = new Intent(mContext, PaComposeActivity.class);
                intent.putExtra(PaComposeActivity.ACCOUNT_ID, accountId);
                intent.putExtra(PaComposeActivity.UUID, uuid);
                intent.putExtra(PaComposeActivity.NAME, name);
                intent.putExtra(PaComposeActivity.IMAGE_PATH, logoPath);
                mContext.startActivity(intent);
            }
        });
    }
}
