package com.mediatek.rcs.message.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Profile;
import android.provider.Telephony.Sms;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineHeightSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.provider.Telephony;
import android.provider.Telephony.Mms;

import com.google.android.mms.pdu.PduHeaders;


/// M: @{
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.MailTo;
import android.provider.Browser;
import android.text.style.LeadingMarginSpan;
import android.text.Spannable;
import android.widget.CheckBox;
import android.widget.Toast;

import com.mediatek.drm.OmaDrmClient;
import com.mediatek.drm.OmaDrmUiUtils;
import com.mediatek.storage.StorageManagerEx;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;


/// @}
import android.app.Activity;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.ProgressBar;
/// M: @{


//add for attachment enhance
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.content.ContentResolver;
import android.os.Environment;
import com.mediatek.rcs.message.R;



public class MsgListItem extends LinearLayout {

    private Context mContext;

    /// deviders
    private View mTimeDivider; // time_divider
    private TextView mTimeDividerStr; // time_divider_str
    private View mUnreadDivider; // unread_divider
    private TextView mUnreadDividerStr; // unread_divider_str
    private View mOnLineDivider; // on_line_divider
    private TextView mOnLineDividertextView; // on_line_divider_str
    private View mSubDivider;

    //checkbox for muliti select
    public CheckBox mSelectedBox;

    // sender info
    private QuickContactBadge mSenderPhoto;
    private TextView mSenderName;

    //common message
    private TextView mBodyTextView; //sms or mms text
    private View mMmsView;  //mms frame layout, contain mImageView and mSlideShowButton
    private ImageView mImageView; //mms Image or video or slideshow
    private ImageButton mSlideShowButton; //video or slideshow play button

    //push information
    private Button mDownloadButton;
    private TextView mDownloadingLabel;
    private TextView mExpireText;

  /// M: add for vcard
    private View mFileAttachmentView;


    //last line sub info && indicators
//    private ImageView mSubIcon;  //sub icon
    private TextView mSubStatus; // sub name
    private TextView mDateView;  // send/receive time
    private ImageView mDeliveredIndicator; //send fail
    private ImageView mDetailsIndicator;   //deliver report
    private ImageView mLockedIndicator;  //locked

    private boolean mIsLastItemInList;

 // / M: add for ip message, download file, accept or reject
    private View mIpmsgFileDownloadContrller; // ipmsg_file_downloading_controller_view
    private TextView mIpmsgResendButton; // ipmsg_resend
    private Button mIpmsgAcceptButton; // ipmsg_accept
    private Button mIpmsgRejectButton; // ipmsg_reject
    private View mIpmsgFileDownloadView; // ipmsg_file_download
    private TextView mIpmsgFileSize; // ipmsg_download_file_size
    private ImageView mIpmsgCancelDownloadButton; // ipmsg_download_file_cancel
    private ImageView mIpmsgPauseResumeButton; // ipmsg_download_file_resume
    private ProgressBar mIpmsgDownloadFileProgress; // ipmsg_download_file_progress

    // / M: add for ip message
    // / M: add for image and video
    private View mIpImageView; // ip_image
    private ImageView mImageContent; // image_content
    private View mIpImageSizeBg; // image_size_bg
    private ImageView mActionButton; // action_btn
    private TextView mContentSize; // content_size
    private TextView mCaption; // text_caption
    private ImageView mMediaPlayView;
    // / M: add for audio
    private View mIpAudioView; // ip_audio
    private ImageView mAudioIcon; // ip_audio_icon
    private TextView mAudioInfo; // audio_info
    private View mIpVCardView;
    private TextView mVCardInfo;
    // / M: add for vcalendar
    private View mIpVCalendarView;

    public MsgListItem(Context context) {
        super(context);
        mContext = context;
        // TODO Auto-generated constructor stub
    }

    public MsgListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        String viewClassLoader = getClass().getClassLoader().toString();
        mBodyTextView = (TextView) findViewById(R.id.text_view);
        mDateView = (TextView) findViewById(R.id.date_view);
        /// M: @{
        mSubStatus = (TextView) findViewById(R.id.sim_status);
//        mSubIcon = (ImageView) findViewById(R.id.account_icon);
        /// @}
        mLockedIndicator = (ImageView) findViewById(R.id.locked_indicator);
        mDeliveredIndicator = (ImageView) findViewById(R.id.delivered_indicator);
        mDetailsIndicator = (ImageView) findViewById(R.id.details_indicator);
        /// M: @{
        //add for multi-delete
        mSelectedBox = (CheckBox) findViewById(R.id.select_check_box);
        /// @}

        /// M: add for time divider
        mTimeDivider = (View) findViewById(R.id.time_divider);
        if (null != mTimeDivider) {
            mTimeDividerStr = (TextView) mTimeDivider.findViewById(R.id.time_divider_str);
        }
        mUnreadDivider = (View) findViewById(R.id.unread_divider);
        if (null != mUnreadDivider) {
            mUnreadDividerStr = (TextView) mUnreadDivider.findViewById(R.id.unread_divider_str);
        }
        mOnLineDivider = (View) findViewById(R.id.on_line_divider);
        if (null != mOnLineDivider) {
            mOnLineDividertextView = (TextView) mOnLineDivider.findViewById(R.id.on_line_divider_str);
        }
        mSubDivider = (View) findViewById(R.id.sim_divider);
        mExpireText = (TextView) findViewById(R.id.text_expire);
    }

    private void showMmsView(boolean visible) {
        if (mMmsView == null) {
            mMmsView = findViewById(R.id.mms_view);
            // if mMmsView is still null here, that mean the mms section hasn't been inflated

            if (visible && mMmsView == null) {
                //inflate the mms view_stub
                View mmsStub = findViewById(R.id.mms_layout_view_stub);
                mmsStub.setVisibility(View.VISIBLE);
                mMmsView = findViewById(R.id.mms_view);
            }
        }
        if (mMmsView != null) {
            if (mImageView == null) {
                mImageView = (ImageView) findViewById(R.id.image_view);
            }
            if (mSlideShowButton == null) {
                mSlideShowButton = (ImageButton) findViewById(R.id.play_slideshow_button);
            }
            mMmsView.setVisibility(visible ? View.VISIBLE : View.GONE);
            mImageView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public String toString() {
        return "com.mediatek.rcs.message.ui.MsgListItem";
    }
}
