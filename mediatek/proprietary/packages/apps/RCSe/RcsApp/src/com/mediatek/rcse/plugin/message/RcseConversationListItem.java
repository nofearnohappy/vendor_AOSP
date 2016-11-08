package com.mediatek.rcse.plugin.message;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.provider.Telephony.Threads;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.QuickContactBadge;

import com.mediatek.mms.ipmessage.IIpContactExt;
import com.mediatek.mms.ipmessage.IIpConversationExt;
import com.mediatek.mms.ipmessage.DefaultIpConversationListItemExt;
import com.mediatek.rcse.plugin.message.IpMessageConsts;

public class RcseConversationListItem extends DefaultIpConversationListItemExt {
    private static String TAG = "RcseConversationListItem";
    /// M: New feature for rcse, adding IntegrationMode.
    private ImageView mFullIntegrationModeView;
    private Context mContext;
    private QuickContactBadge mAvatarView;

    @Override
    public void onIpSyncView(Context context, ImageView fullIntegrationModeView, QuickContactBadge avatarView) {
        mContext = context;
        mFullIntegrationModeView = fullIntegrationModeView;
        mAvatarView = avatarView;
    }

    @Override
    public String onIpFormatMessage(IIpContactExt ipContact,
            long threadId, String number, String name) {
        if (IpMmsConfig.isServiceEnabled(mContext)) {
            Log.d("avatar", "ConvListItem.formatMessage(): number = " + number
                + ", name = " + name);
            if(number == null)
            	return name;
            if (number.startsWith(IpMessageConsts.GROUP_START)
                    || number.startsWith(IpMessageConsts.JOYN_START)) {
                if (threadId != 0) {
                    ((RcseContact)ipContact).setThreadId(threadId);
                }
                if (TextUtils.isEmpty(name) || number.equals(name)) {
                    name = IpMessageContactManager.getInstance(mContext).getNameByThreadId(threadId);
                }
                Log.d("avatar", "ConvListItem.formatMessage(): number = " + number
                    + ", group name = " + name);
            }
        }
        return name;
    }

    @Override
    public boolean updateIpAvatarView(final IIpContactExt ipContact,
            String number, QuickContactBadge avatarView, final Uri uri) {
        Drawable avatarDrawable;
        boolean isGroup = number.startsWith(IpMessageConsts.GROUP_START);
        Log.d("avatar", "ConvListItem.updateAvatarView(): isGroup = " + isGroup + ", number = "
                + number);
        // / M: add for ipmessage,fix bug ALPS01608034 @{
        if (isGroup) {
//            Log.d("avatar",
//                    "ConvListItem.updateAvatarView(): get avatart by threadId, threadId = "
//                            + mConversation.getThreadId());
            Handler avatarHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (msg.what > 0) {
                        Drawable drawable = ((RcseContact)ipContact).getGroupAvatar();
                        if (null != drawable) {
                            updateGroupAvatarView(drawable, uri);
                            Log.d("avatar", "ConvListItem.updateAvatarView(): set group avatar.");
                            return;
                        }
                    }
                }
            };

            avatarDrawable = ((RcseContact)ipContact).getGroupAvatar(avatarHandler);
            Log.d("avatar", "ConvListItem.updateAvatarView(): bitmap is null ?= "
                    + (null == avatarDrawable));
            if (avatarDrawable != null) {
                updateGroupAvatarView(avatarDrawable, uri);
            }
        }
        // / @}

        if (number.startsWith(IpMessageConsts.JOYN_START)) {
            return true;
        }
        return false;
    }
    
    public boolean onIpBind(IIpConversationExt ipConv, int convType,
            TextView fromView, TextView subjectView, TextView dateView) {
        if (convType == Telephony.Threads.IP_MESSAGE_GUIDE_THREAD) {
            // this is ipmessage guide thread
            fromView.setText(IpMessageResourceMananger.getInstance(mContext)
                .getSingleString(IpMessageConsts.string.ipmsg_service_title));
            subjectView.setText(IpMessageResourceMananger.getInstance(mContext)
                    .getSingleString(IpMessageConsts.string.ipmsg_introduction));

            mAvatarView.assignContactUri(null);
            //Drawable image = context.getResources().getDrawable(R.drawable.ipmsg_service);
            Drawable image = IpMessageResourceMananger.getInstance(mContext)
                                        .getSingleDrawable(IpMessageConsts.drawable.ipmsg_service);
            mAvatarView.setImageDrawable(image);
            mAvatarView.setVisibility(View.VISIBLE);
            return true;
        }

        if (RcseConversationList.sConversationListOption == RcseConversationList.OPTION_CONVERSATION_LIST_SPAM
                && ((RcseConversation)ipConv).isSpam()) {
            dateView.setText(IpMessageUtils.formatTimeStampStringExtend(mContext, ((RcseConversation)ipConv).getSpamDate()));
        }

        /// M: New feature for rcse, adding IntegrationMode. @{
        if (IpMmsConfig.isActivated(mContext) && ((RcseConversation)ipConv).getIsFullIntegrationMode()) {
            mFullIntegrationModeView.setVisibility(View.VISIBLE);
            mFullIntegrationModeView.setBackgroundDrawable(IpMessageResourceMananger.getInstance(mContext)
                    .getSingleDrawable(IpMessageConsts.drawable.ipmsg_full_integrated));
        } else {
            mFullIntegrationModeView.setVisibility(View.GONE);
        }
        return false;
    }
    
    /// M: add for ipmessage,fix bug ALPS01608034 @{
    private void updateGroupAvatarView(Drawable drawable, Uri uri) {
        mAvatarView.setImageDrawable(drawable);
        mAvatarView.setVisibility(View.VISIBLE);
        // mAvatarView.setThreadId(mConversation.getThreadId());
        // mAvatarView.setGroupAvator(true);
        mAvatarView.assignContactUri(uri);
    }
    /// @}
}
