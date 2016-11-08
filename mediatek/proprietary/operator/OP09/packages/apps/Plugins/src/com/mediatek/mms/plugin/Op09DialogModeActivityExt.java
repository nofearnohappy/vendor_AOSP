package com.mediatek.mms.plugin;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Telephony.Threads;
import android.telephony.SubscriptionInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.PhoneConstants;
import com.google.android.mms.pdu.PduHeaders;
import com.mediatek.common.PluginImpl;
import com.mediatek.mms.callback.IDialogModeActivityCallback;
import com.mediatek.mms.ext.DefaultOpDialogModeActivityExt;
import com.mediatek.mms.plugin.Op09MessagePluginExt;
import com.mediatek.op09.plugin.R;

/**
 * M: Op09MmsDialogModeExt.
 */
public class Op09DialogModeActivityExt extends DefaultOpDialogModeActivityExt
                    implements OnClickListener {
    private static final String TAG = "Mms/OP09MmsDialogModeExt";
    private Button mMmsDownloadBtn;

    private int mSendSubIdForOp09 = -1;
    private IDialogModeActivityCallback mHost;
    private Op09MmsCancelDownloadExt mMmsCancelDownloadPlugin;

    private TextView mSubName;
    private TextView mRecvTime;
    private TextView mSentTime;
    private LinearLayout mTimeLayout;
    private LinearLayout mCounterLinearLayout;
    private TextView mCounter;

    private static final int SMS_SUB = 6;

    private static final int STATE_DOWNLOADING = 0x01;

    public static final String URI = "bundle_uri";

    public static final String TRANSACTION_TYPE = "type";

    public static final int RETRIEVE_TRANSACTION = 1;

    private int mCurrentSubId = -1;
    private Op09DualSendButton mDualSendButton;
    private View mOriginSendButton;
    private Context mResourceContext;

    /**
     * The Constructor.
     */
    public Op09DialogModeActivityExt(Context context) {
        super(context);
        mResourceContext = context;
        mMmsCancelDownloadPlugin = new Op09MmsCancelDownloadExt(context);
    }

    @Override
    public void setHost(IDialogModeActivityCallback host) {
        mHost = host;
    }

    @Override
    public String getNotificationContentString(String from, String subject, String msgSizeTxt,
            String expireTxt) {
        return from + "\n" + subject + "\n" + msgSizeTxt + "\n" + expireTxt;
    }


    public void setSimTypeDrawable(Context context, long subId, ImageView imageView,
            TextView textView) {
        if (context == null || imageView == null) {
            return;
        }
        textView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        Drawable simTypeDraw = null;
        SubscriptionInfo simInfo = MessageUtils.getSimInfoBySubId(context, subId);
        if (simInfo != null) {
            Bitmap origenBitmap = simInfo.createIconBitmap(context);
            Bitmap bitmap = MessageUtils.resizeBitmap(this, origenBitmap);
            simTypeDraw = new BitmapDrawable(context.getResources(), bitmap);
        } else {
            simTypeDraw = context.getResources().getDrawable(R.drawable.sim_indicator_no_sim_mms);
        }
        if (imageView != null) {
            imageView.setImageDrawable(simTypeDraw);
        }
    }


    public void setRecvtimerViewToFitBigIcon(TextView textView) {
        if (textView == null) {
            return;
        } else {
            int topPadding = this.getResources().getDimensionPixelOffset(
                    R.dimen.ct_dialog_mode_activity_recvtimer_top_padding);
            textView.setPadding(textView.getPaddingLeft(), topPadding, 0, 0);
        }
    }

    public void setSmsTextView(TextView tv) {
        if (tv != null) {
            int padding = this.getResources().getDimensionPixelOffset(
                    R.dimen.sms_content_left_padding);
            tv.setPadding(tv.getPaddingLeft() + padding, tv.getPaddingTop(), tv.getPaddingRight(),
                    tv.getPaddingBottom());
        }
    }

    public void initDialogView(TextView subName, Button download, TextView recvTime,
            TextView sentTime, LinearLayout timeLayout, LinearLayout counterLinearLayout,
            TextView counter, Cursor cursor) {
        mSubName = subName;
        mMmsDownloadBtn = download;
        mRecvTime = recvTime;
        mSentTime = sentTime;
        mTimeLayout = timeLayout;
        mCounterLinearLayout = counterLinearLayout;
        mCounter = counter;
        mDualSendButton = new Op09DualSendButton(mResourceContext);
        mOriginSendButton = mCounterLinearLayout.getChildAt(3);
        mDualSendButton.initView((Context) mHost,
                                (ImageButton) mCounterLinearLayout.getChildAt(0),
                                (ImageButton) mCounterLinearLayout.getChildAt(1),
                                mCounter);
        mDualSendButton.updateSendButton();
        mDualSendButton.setOnClickListener(mDualBtnListener);
    }

    @Override
    public void setDialogView(Context applicationContext, int subId, boolean isCurSMS, int type,
            String sentTime, String receivedTime, ImageView imageView) {
        mRecvTime.setText(sentTime);
        mCurrentSubId = subId;
        Op09MessageListItemExt.setDualTime(mResourceContext, true, subId, mSentTime,
                mTimeLayout, receivedTime);

        setRecvtimerViewToFitBigIcon(mRecvTime);
        setSimTypeDrawable(this, subId, imageView, mSubName);
        if (isCurSMS) {
            mMmsDownloadBtn.setVisibility(View.GONE);

        } else {
            if (PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND == type) {
                mRecvTime.setText("");
                hideDualTimePanel(mSentTime, mTimeLayout);
                mMmsDownloadBtn.setVisibility(View.VISIBLE);
                mMmsDownloadBtn.setOnClickListener(this);
            } else {
                mMmsDownloadBtn.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mMmsDownloadBtn) { // OP09 Feature
            // / M: For OP09 @{

            mMmsCancelDownloadPlugin.markStateExt(mHost.opGetCurUri(), STATE_DOWNLOADING);

            // / @}
            final Context context = this;
            // Start the TransactionService to download.
            Intent intent = Op09MessagePluginExt.sCallback.getTransactionServiceIntent();
            intent.putExtra(URI, mHost.opGetCurUri().toString());
            intent.putExtra(TRANSACTION_TYPE, RETRIEVE_TRANSACTION);
            intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, mCurrentSubId);
            startService(intent);
            // Launch the specific ComposeMessageActivity.
            Intent clickIntent = createIntent(context, mHost.opGetThreadId());
            clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(clickIntent);
        }
    }

    public static Intent createIntent(Context context, long threadId) {
        Intent intent = Op09MessagePluginExt.sCallback.getComposeIntent();

        if (threadId > 0) {
            intent.setData(ContentUris.withAppendedId(Threads.CONTENT_URI, threadId));
        }

        return intent;
    }
    @Override
    public boolean simSelection(int selectedSubId, int subCount, String number, int messageSubId,
            Intent intent, long currentSubId, int[] subIdList,
            IDialogModeActivityCallback callback) {
        return simSelection();
    }

    public boolean simSelection() {
        if (mSendSubIdForOp09 > 0) {
            mHost.opSetSelectedSubId(mSendSubIdForOp09);
            mHost.opConfirmSendMessageIfNeeded();
            mSendSubIdForOp09 = -1;
            return true;
        }
        return false;
    }

    // / M: OP09 Feature: DualSendButton; the button listener;
    Op09DualSendButton.OnClickListener mDualBtnListener = new Op09DualSendButton.OnClickListener() {
        @Override
        public void onClick(View view, int subId) {
            mSendSubIdForOp09 = subId;
            simSelection();
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED)) {
            if (mDualSendButton != null) {
                mDualSendButton.updateSendButton();
            }
            if (mOriginSendButton != null) {
                int count = MessageUtils.getActiveSubCount(context);
                if (count > 0) {
                    mOriginSendButton.setVisibility(View.GONE);
                } else {
                    mOriginSendButton.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void updateCounter(TextView counter, int textLineCount, int remainingInCurrentMessage,
            int msgCount, String counterText) {
        if (mDualSendButton != null) {
            mDualSendButton.updateTextCounter(false, remainingInCurrentMessage, msgCount);
        }

        if (textLineCount <= 1) {
            counter.setVisibility(View.INVISIBLE);
        } else {
            counter.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateSendButtonState(boolean enable) {
        if (mDualSendButton != null) {
            mDualSendButton.setEnabled(enable);
        }
    }

    @Override
    public boolean onResume() {
        return true;
    }

    private void hideDualTimePanel(TextView dateView, LinearLayout linearLayout) {
        if (dateView != null) {
            dateView.setVisibility(View.GONE);
        }
        if (linearLayout != null) {
            linearLayout.setVisibility(View.GONE);
        }
    }
}
