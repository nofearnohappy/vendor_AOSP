package com.hesine.nmsg.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.hesine.nmsg.Application;
import com.hesine.nmsg.R;
import com.hesine.nmsg.business.Pipe;
import com.hesine.nmsg.business.bean.AttachInfo;
import com.hesine.nmsg.business.bean.LocalMessageInfo;
import com.hesine.nmsg.business.bean.MessageInfo;
import com.hesine.nmsg.business.bean.MessageItemInfo;
import com.hesine.nmsg.business.bean.ServiceInfo;
import com.hesine.nmsg.business.bo.MsgSender;
import com.hesine.nmsg.business.dao.Config;
import com.hesine.nmsg.business.dao.DBUtils;
import com.hesine.nmsg.business.dao.NmsSMSMMSManager;
import com.hesine.nmsg.common.CommonUtils;
import com.hesine.nmsg.common.DeviceInfo;
import com.hesine.nmsg.common.EnumConstants;
import com.hesine.nmsg.common.FileEx;
import com.hesine.nmsg.common.GlobalData;
import com.hesine.nmsg.common.Image;
import com.hesine.nmsg.common.MLog;
import com.hesine.nmsg.thirdparty.Statistics;
import com.hesine.nmsg.util.Base64;

/**
 * ClassName: ConversationActivity <br/>
 * Function: 会话界面 <br/>
 * date: 2014-12-18 上午9:52:43 <br/>
 * 
 * @author HST00113
 * @version
 */
public class ConversationActivity extends ConversationBaseActivity implements OnItemClickListener,
        OnItemLongClickListener, Pipe {
    private HeaderView mHeader = null;
    private Context mContext = null;
    private ListView mList = null;
    private ConversationListAdapter mAdapter = null;
    private BottomView mBottomView = null;
    private BottomMoreView mBottomMoreView = null;
    private BottomAudioView mBottomAudioView = null;
    private String mPhoneAddToExist = null;
    private long mThreadId = 0;
    private String mServiceAccount = null;
    private ServiceInfo mService = null;
    private MsgSender mMsgSender = null;
    private String mEntranceType = null;
    private PopupDialog mDialog = null;
    private PopupDialog mResendDialog = null;
    private IntentFilter mIntentFilter = null;
    private MyBroadcastReceiver mBroadcastReceiver = null;

    private void sendTextMessage() {
        String body = mBottomView.getContentForSend();
        sendMsg(mServiceAccount, body, null, AttachInfo.ATTACH_NONE);
    }

    private Runnable resendMsg = new Runnable() {
        public void run() {
            LocalMessageInfo msg = (LocalMessageInfo) mResendDialog.getTag();
            DBUtils.deleteMsgViaMsgId(msg.getId());
            String body = msg.getMessageItems().get(0).getBody();
            String attachPath = AttachInfo.getAttachmentAbsPath(msg.getMessageItems().get(0)
                    .getAttachInfo());
            AttachInfo attachInfo = msg.getMessageItems().get(0).getAttachInfo();
            int attachType = Integer.parseInt((attachInfo == null) ? "0" : attachInfo.getType());
            sendMsg(mServiceAccount, body, attachPath, attachType);
        }
    };
    private Runnable sendAudio = new Runnable() {
        public void run() {
            String path = mAudioTempPath + File.separator + audioPath + ".amr";
            String length = mRecorder.sampleLength() + "";
            File oldF = new File(path);
            if (mRecorder.sampleLength() == 0 || !FileEx.isFileExisted(path)) {
                if (oldF.exists()) {
                    Toast.makeText(mContext, R.string.audio_too_short, Toast.LENGTH_SHORT).show();
                    oldF.delete();
                }
                return;
            }
            String newPath = mAudioTempPath + File.separator + length + "_" + audioPath + ".amr";
            File newF = new File(newPath);
            oldF.renameTo(newF);
            sendMsg(mServiceAccount, null, newPath, AttachInfo.ATTACH_AUDIO);
        }
    };

    private Runnable resizePicTip = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(mContext, R.string.chat_waiting, Toast.LENGTH_SHORT).show();
        }
    };

    private Runnable resizePicFailed = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(mContext, R.string.resize_pic_failed, Toast.LENGTH_SHORT).show();
        }
    };

    private Runnable resizePic = new Runnable() {

        @Override
        public void run() {
            byte[] img = Image.resizeImg(mPhotoFilePath, (float) 500);
            if (null == img) {
                return;
            }
            int switchVal = 1024 * 100 * 3;
            if (img.length > switchVal) {
                mHandler.post(resizePicTip);
                img = Image.resizeImg(mPhotoFilePath, switchVal, 1);
                if (null == img) {
                    mHandler.post(resizePicFailed);
                }
            }

            try {
                CommonUtils.nmsStream2File(img, mDstPhotoPath);
            } catch (RuntimeException e) {
                MLog.error(MLog.getStactTrace(e));
                return;
            }catch (IOException e) {
                MLog.error(MLog.getStactTrace(e));
                return;
            } 
            mHandler.postDelayed(sendPic, 100);
        }
    };

    private Runnable sendPic = new Runnable() {
        public void run() {
            if (FileEx.isFileExisted(mDstPhotoPath) && FileEx.getFileSize(mDstPhotoPath) != 0) {
                sendMsg(mServiceAccount, null, mDstPhotoPath, AttachInfo.ATTACH_PIC);
            }
        }
    };

    public String readFile(String fileName) throws IOException {

        File file = new File(fileName);
        FileInputStream fis = new FileInputStream(file);
        int length = fis.available();
        byte[] buffer = new byte[length];
        fis.read(buffer);
        String res = new String(buffer, "ISO-8859-1");// EncodingUtils.getAsciiString(buffer);
        fis.close();
        return res;
    }

    private void sendMsg(String to, String body, String attachPath, int attachType) {
        MessageInfo msg = new MessageInfo();
        List<MessageItemInfo> msgItems = new ArrayList<MessageItemInfo>();
        MessageItemInfo msgItem = new MessageItemInfo();
        msg.setTo(to);
        msg.setFrom(Config.getUuid());
        msgItem.setBody(body);
        long currentTime = System.currentTimeMillis();
        msg.setMsgUuid(GlobalData.instance().getSystemInfo().getImsi() + currentTime);
        msg.setTime(currentTime);
        if (attachPath != null) {
            AttachInfo attachInfo = new AttachInfo();
            attachInfo.setType(attachType + "");
            attachInfo.setSize(FileEx.getFileSize(attachPath) + "");
            attachInfo.setName(attachPath.substring(attachPath.lastIndexOf("/") + 1));
            try {
                String data = readFile(attachPath);
                if (null != data) {
                    String base64Data = Base64.base64Encode(data);
                    attachInfo.setAttachment(base64Data);
                    // String md5 = StringEx.MD5(base64Data);
                    // attachInfo.setMd5(md5);
                }
            } catch (IOException e) {
                MLog.error(MLog.getStactTrace(e));
            }

            msgItem.setAttachInfo(attachInfo);
            msg.setType((Integer.parseInt(attachInfo.getType()) == AttachInfo.ATTACH_PIC) ? LocalMessageInfo.TYPE_IMAGE
                    : LocalMessageInfo.TYPE_AUDIO);
            // msgItem.setAttachmentMark((byte) 1);
        }
        msgItems.add(msgItem);
        msg.setMessageItems(msgItems);
        mMsgSender.setMessageInfo(msg);
        mMsgSender.request(mServiceAccount, mThreadId);
        Statistics.getInstance().msgSend(msg.getMsgUuid(), String.valueOf(msg.getType()));
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                updateChatting();
            }
        });
        if (!DeviceInfo.isNetworkReady(Application.getInstance())) {
            Toast.makeText(this, R.string.network_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(EnumConstants.NMSG_INTENT_ACTION)
                    && intent.getStringExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT).equals(
                            mServiceAccount)) {
                updateChatting();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_activity);
        mContext = this;
        Intent intent = getIntent();
        mThreadId = intent.getLongExtra("thread_id", 0);
        mServiceAccount = intent.getStringExtra("phone_number");
        mService = DBUtils.getServiceInfo(mServiceAccount);
        mEntranceType = intent.getStringExtra("open_type");
        Statistics.getInstance().appClick(mServiceAccount, mEntranceType);
        NmsgNotification.getInstance(Application.getInstance()).removeNotification(mServiceAccount);
        if (FileEx.getStorageFullStatus()) {
            showSpaceFullDialog();
        }
        mMsgSender = new MsgSender();
        mMsgSender.setListener(this);
        initResources();
        NmsSMSMMSManager.updateSmsUnreadStatus(mThreadId);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(EnumConstants.NMSG_INTENT_ACTION);
        mBroadcastReceiver = new MyBroadcastReceiver();
    }

    private Runnable updateAvatar = new Runnable() {
        public void run() {
            mAdapter.refresh();
            mAdapter.closeCursor();
            mAdapter.changeCursor(DBUtils.getMessageCursor(mServiceAccount));
            onlyRefresh();
        }
    };

    @Override
    protected void onResume() {
        GlobalData.instance().setCurServiceAccount(mServiceAccount);
        registerReceiver(mBroadcastReceiver, mIntentFilter);
        mHandler.postDelayed(updateAvatar, 300);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
        GlobalData.instance().setCurServiceAccount(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.closeCursor();
        mMsgSender.setListener(null);
    }

    private void initHeader() {
        mHeader = (HeaderView) findViewById(R.id.header);
        mHeader.setTitle((mService == null) ? mServiceAccount : mService.getName());
        mHeader.getMoreView().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, VendorAccountSetting.class);
                intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_ACCOUNT, mServiceAccount);
                intent.putExtra(EnumConstants.NMSG_INTENT_EXTRA_THREADID, mThreadId);
                mContext.startActivity(intent);
            }
        });
        mHeader.getBackView().setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
                if (Statistics.OpenType.NOTIFICATION.equals(mEntranceType)) {
                    startMmsConversationList();
                }
            }
        });
    }

    private void initList() {
        mList = (ListView) findViewById(R.id.list);
        mAdapter = new ConversationListAdapter(mContext, mServiceAccount);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    adjustingBottomViewStatus();
                    mBottomView.showKeyBoard(false);
                }
                return false;
            }
        });
        mList.setOnItemLongClickListener(this);
    }

    private void initBottom() {
        mBottomView = (BottomView) findViewById(R.id.bottom);
        mBottomView.addActionListener(new ActionListener() {

            @Override
            public void doAction(int type) {
                switch (type) {
                    case BottomView.MsgType.MORE_ACTION:
                        mBottomMoreView.show();
                        mBottomAudioView.hide();
                        break;
                    case BottomView.MsgType.SEND_MSG:
                        Message msg = new Message();
                        msg.what = UiMessageId.NMS_IM_SEND_MSG;
                        mHandler.sendMessage(msg);
                        break;
                    case BottomView.MsgType.SCROLL_TO_BOTTOM:
                        updateChatting();
                        break;
                    case BottomView.MsgType.HIDE_MORE_ACTION:
                        mBottomMoreView.hide();
                        mBottomAudioView.hide();
                        break;
                    default:
                        break;
                }
            }
        });
        mBottomMoreView = (BottomMoreView) findViewById(R.id.bottom_more);
        mBottomMoreView.addActionListener(new ActionListener() {

            @Override
            public void doAction(int type) {
                switch (type) {
                    case BottomMoreView.MsgType.IMAGE:
                        takePhoto();
                        break;
                    case BottomMoreView.MsgType.CAMERA:
                        takeCamera();
                        break;
                    case BottomMoreView.MsgType.RECORDER:
                        mBottomAudioView.show();
                        mBottomMoreView.hide();
                        break;
                    default:
                        break;
                }
            }
        });
        mBottomAudioView = (BottomAudioView) findViewById(R.id.bottom_audio);
        mBottomAudioView.addActionListener(new ActionListener() {

            @Override
            public void doAction(int type) {
                if (type == BottomAudioView.MsgType.STARTRECORD) {
                    MLog.info("onclick: startRecord");
                    long[] pattern = { 300, 50 };
                    vibrator.vibrate(pattern, -1);
                    startRecord();
                } else if (type == BottomAudioView.MsgType.SENDRECORD) {
                    MLog.info("onclick: stopRecord send");
                    stopRecord();
                } else if (type == BottomAudioView.MsgType.CANCELRECORD) {
                    MLog.info("onclick: stopRecord cancel");
                    cancelRecord();
                } else if (type == BottomAudioView.MsgType.MOTIONUPTOCANCELRECORD) {
                    MLog.info("onclick: motion up to cancel");
                    updateRecordMotionUpToRelease();
                    MLog.info("onclick: motion up to cancel");
                    // updateRecordMotionUpToRelease();
                    updateRecordReleaseToSend();
                } else if (type == BottomAudioView.MsgType.FINGERUPTOCANCELRECORD) {
                    MLog.info("onclick: finger up to cancel");
                    updateRecordFingerUpToRelease();
                }
            }
        });
    }

    private void initRecorder() {
        popRecordView = new RecordPopupWindow(mContext, mList.getRootView());
    }

    public PopupDialog getResendDialog() {
        return mResendDialog;
    }

    public long getThreadId() {
        return mThreadId;
    }

    private void initDialog() {
        mDialog = new PopupDialog(mContext).setInfo(R.string.chat_delete_message)
                .setLeft(R.string.btn_cancel).setRight(R.string.btn_ok);
        mDialog.setListener(new ActionListener() {
            @Override
            public void doAction(int type) {
                if (type == PopupDialog.ActionType.ACTION_RIGHT) {
                    int msgId = Integer.parseInt(String.valueOf(mDialog.getTag()));
                    if (msgId >= 0) {
                        DBUtils.deleteMsgViaMsgId(msgId);
                        refreshChatting();
                    }
                }
                mDialog.dismiss();
            }
        });
        mResendDialog = new PopupDialog(mContext).setInfo(R.string.chat_resend_message)
                .setLeft(R.string.btn_cancel).setRight(R.string.btn_ok);
        mResendDialog.setListener(new ActionListener() {

            @Override
            public void doAction(int type) {
                if (type == PopupDialog.ActionType.ACTION_RIGHT) {
                    LocalMessageInfo msg = (LocalMessageInfo) mResendDialog.getTag();
                    if (msg != null) {
                        mHandler.postDelayed(resendMsg, 100);
                    }
                }
                mResendDialog.dismiss();
            }
        });
    }

    private void initResources() {
        initHeader();
        initList();
        initBottom();
        initRecorder();
        initDialog();
    }

    @Override
    public void onBackPressed() {
        if (mBottomMoreView.isShow() || mBottomAudioView.isShow()) {
            mBottomMoreView.hide();
            mBottomAudioView.hide();
            return;
        }
        if (Statistics.OpenType.NOTIFICATION.equals(mEntranceType)) {
            finish();
            startMmsConversationList();
            return;
        } else {
            super.onBackPressed();
        }

    }

    private void startMmsConversationList() {
        try {
            Intent intent = new Intent();
            intent.setAction("com.android.mms.ui.ConversationList");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClassName("com.android.mms", "com.android.mms.ui.ConversationList");
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    public void updateChatting() {
        mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        if (null != mAdapter) {
            mAdapter.closeCursor();
            mAdapter.changeCursor(DBUtils.getMessageCursor(mServiceAccount));
        }
    }

    public void refreshChatting() {
        mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
        if (null != mAdapter) {
            mAdapter.closeCursor();
            mAdapter.changeCursor(DBUtils.getMessageCursor(mServiceAccount));
        }
    }

    private void onlyRefresh() {
        mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
        if (null != mAdapter) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void adjustingBottomViewStatus() {
        if (mBottomMoreView.isShown() || mBottomAudioView.isShown()) {
            mBottomMoreView.hide();
            mBottomAudioView.hide();
        }
    }

    @SuppressLint("DefaultLocale")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case UiMessageId.NMS_IM_TAKE_CAMERA:
                if (!FileEx.isFileExisted(mPhotoFilePath)
                        || FileEx.getFileSize(mPhotoFilePath) == 0) {
                    return;
                }
                new Thread(resizePic).start();
                adjustingBottomViewStatus();
                break;
            case UiMessageId.NMS_IM_TAKE_PHOTO:
                Uri uri = data.getData();
                ContentResolver cr = this.getContentResolver();
                final String[] selectColumn = { "_data" };
                Cursor cursor = cr.query(uri, selectColumn, null, null, null);
                if (null != cursor) {
                    if (cursor.getCount() != 0) {
                        cursor.moveToFirst();
                        String tempFilePath = (cursor.getString(0)).toLowerCase();
                        if (null != cursor && !cursor.isClosed()) {
                            cursor.close();
                        }
                        if (!FileEx.isFileExisted(tempFilePath)
                                || FileEx.getFileSize(tempFilePath) == 0) {
                            return;
                        }
                        // photo
                        mPhotoFilePath = tempFilePath;
                        new Thread(resizePic).start();
                    }
                }
                adjustingBottomViewStatus();
                break;
            case UiMessageId.REQUEST_PICK_CONTACT:

                Uri contactData = data.getData();
                long lId = ContentUris.parseId(contactData);
                updateExistContact((int) lId);
                break;
            case UiMessageId.REQUEST_ADD_TO_NEW_CONTACT:
            case UiMessageId.REQUEST_ADD_TO_EXIST_CONTACT:
                updateChatting();
                break;
            default:
                break;
        }
    }

    private int preState = Recorder.IDLE_STATE;

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);
        if (state == Recorder.IDLE_STATE) {
            if (preState == Recorder.RECORDING_STATE) {
                mHandler.postDelayed(sendAudio, 100);
            }
            mAdapter.setPlayRecordId(-1);
            onlyRefresh();
        }
        preState = state;
    }

    private static class MyHandler extends Handler {
        private final WeakReference<ConversationActivity> mActivity;

        public MyHandler(ConversationActivity activity) {
            mActivity = new WeakReference<ConversationActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ConversationActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case UiMessageId.NMS_IM_SEND_MSG:
                        activity.sendTextMessage();
                        break;
                    case UiMessageId.NMS_IM_REFRESH_LIST:
                        activity.updateChatting();
                        break;
                    case UiMessageId.NMS_IM_GET_WEB:
                        String http = msg.getData().getString(NmsgIntentStrId.NMS_IM_WEB);
                        activity.createWebDialog(http);
                        break;
                    case UiMessageId.NMS_IM_GET_EMAIL:
                        String email = msg.getData().getString(NmsgIntentStrId.NMS_IM_EMAIL);
                        activity.createEmailDialog(email);
                        break;
                    case UiMessageId.NMS_IM_GET_PHONE:
                        String phoneNum = msg.getData().getString(NmsgIntentStrId.NMS_IM_PHONENUM);
                        activity.createPhoneDialog(phoneNum);
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        }
    }

    public final Handler mHandler = new MyHandler(this);

    private void createWebDialog(final String web) {
        new AlertDialog.Builder(mContext).setTitle(web)
                .setItems(R.array.menu_chat_web_click, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                openBrowser(web);
                                break;
                            case 1:
                                copyToClip(web);
                                break;
                            default:
                                break;
                        }
                    }
                }).create().show();
    }

    private void createTextLongclickDialog(final String content, final int msgId) {
        new AlertDialog.Builder(mContext).setTitle(content)
                .setItems(R.array.menu_chat_text_longclick, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mDialog.setTag(msgId);
                                mDialog.show();
                                break;
                            case 1:
                                copyToClip(content);
                                break;
                            default:
                                break;
                        }
                    }
                }).create().show();
    }

    private void createEmailDialog(final String email) {
        final String title = email.substring(7);
        new AlertDialog.Builder(mContext).setTitle(title)
                .setItems(R.array.menu_chat_email_click, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                sendEmail(email);
                                break;
                            case 1:
                                copyToClip(title);
                                break;
                            case 2:
                                addToNewContact(title);
                                break;
                            case 3:
                                addToExistContact(title);
                                break;
                            default:
                                break;
                        }
                    }
                }).create().show();
    }

    private void createPhoneDialog(final String num) {
        final String title = num.substring(4);
        new AlertDialog.Builder(mContext).setTitle(title)
                .setItems(R.array.menu_chat_phone_click, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                call(title);
                                break;
                            case 1:
                                toCompose(title);
                                break;
                            case 2:
                                copyToClip(title);
                                break;
                            case 3:
                                addToNewContact(title);
                                break;
                            case 4:
                                addToExistContact(title);
                                break;
                            default:
                                break;
                        }
                    }
                }).create().show();
    }

    private void call(String num) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + num));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    private void sendEmail(String email) {
        try {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(email));
            startActivity(i);

        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    private void openBrowser(String web) {
        try {
            // Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(web));
            // startActivity(intent);

            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("user_account", mServiceAccount);
            intent.putExtra("Title", mService.getName());
            intent.putExtra("URL", web);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    private void viewWeb(String msgId, int subId, String web, String shortUrl, String subject) {
        try {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("user_account", mServiceAccount);
            intent.putExtra("Title", mService.getName());
            MLog.error( "viewWeb   web= " + web + "_"
                    + GlobalData.instance().getSystemInfo().getUuid());
            intent.putExtra("URL", web + "_" + GlobalData.instance().getSystemInfo().getUuid());
            intent.putExtra("shortUrl", shortUrl);
            intent.putExtra("msgId", msgId);
            intent.putExtra("msgSubId", subId);
            intent.putExtra("thread_id", mThreadId);
            intent.putExtra("subject", subject);
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    private void copyToClip(String str) {
        CommonUtils.copyToClipboard(mContext, str);
        Toast.makeText(mContext, getString(R.string.chat_copy_done), Toast.LENGTH_SHORT).show();
    }

    private void toCompose(String str) {
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + str));
        startActivity(i);
    }

    public void addToNewContact(String strPhone) {
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType("vnd.android.cursor.dir/person");
        intent.setType("vnd.android.cursor.dir/contact");
        intent.setType("vnd.android.cursor.dir/raw_contact");
        if (CommonUtils.isPhoneNumberValid(strPhone)) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, strPhone);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, strPhone);
        }
        try {
            startActivityForResult(intent, UiMessageId.REQUEST_ADD_TO_NEW_CONTACT);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    public void addToExistContact(String strPhone) {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        try {
            startActivityForResult(intent, UiMessageId.REQUEST_PICK_CONTACT);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
        mPhoneAddToExist = strPhone;
    }

    private void updateExistContact(int nContactIdIn) {
        Uri uContact = Uri.parse("content://com.android.contacts/contacts/" + nContactIdIn);
        Intent intent = new Intent(Intent.ACTION_EDIT, uContact);
        if (CommonUtils.isPhoneNumberValid(mPhoneAddToExist)) {
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, mPhoneAddToExist);
        } else {
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, mPhoneAddToExist);
        }

        try {
            startActivityForResult(intent, UiMessageId.REQUEST_ADD_TO_EXIST_CONTACT);
        } catch (ActivityNotFoundException e) {
            MLog.error(MLog.getStactTrace(e));
        }
    }

    private void playAudio(String path) {
        if (!FileEx.getSDCardStatus()) {
            CommonUtils.createLoseSDCardNotice(mContext);
            return;
        }

        String dest = mAudioTempPath + File.separator + "temp.amr";

        CommonUtils.copy(path, dest);
        mRecorder.stop();
        mRecorder.startPlayback(dest);
        onlyRefresh();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == -1) {
            MessageItemInfo itemInfo = (MessageItemInfo) view.getTag();
            viewWeb(itemInfo.getUuid(), itemInfo.getId(), itemInfo.getLongLink(),
                    itemInfo.getShortLink(), itemInfo.getSubject());
            Statistics.getInstance().msgRead(itemInfo.getUuid(), String.valueOf(itemInfo.getId()));
            return;
        }
        LocalMessageInfo msg = mAdapter.getMsg(position);
        Statistics.getInstance().msgRead(msg.getMsgUuid(),
                String.valueOf(msg.getMessageItems().get(0).getId()));
        String attachment = mAdapter.getAttachmentViaMsg(msg, 0);
        switch (msg.getType()) {
            case LocalMessageInfo.TYPE_TEXT:
                break;
            case LocalMessageInfo.TYPE_AUDIO:
                if (msg.getId() == mAdapter.getPlayRecordId()) {
                    mRecorder.stop();
                    mAdapter.setPlayRecordId(-1);
                } else {
                    mAdapter.setPlayRecordId(msg.getId());
                    playAudio(attachment);
                }
                onlyRefresh();
                break;
            case LocalMessageInfo.TYPE_IMAGE:
                openImage(attachment);
                break;
            case LocalMessageInfo.TYPE_VIDEO:
            case LocalMessageInfo.TYPE_SINGLE:
            case LocalMessageInfo.TYPE_MULTIPOLE:
            case LocalMessageInfo.TYPE_SINGLE_LINK:
            case LocalMessageInfo.TYPE_MULTI_LINK:
                MessageItemInfo itemInfo = msg.getMessageItems().get(0);
                viewWeb(msg.getMsgUuid(), itemInfo.getId(), itemInfo.getLongLink(),
                        itemInfo.getShortLink(), itemInfo.getSubject());
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int msgId;
        if (position == -1) {
            MessageItemInfo mii = (MessageItemInfo) view.getTag();
            msgId = mii.getMsgId();
        } else {
            msgId = mAdapter.getMsg(position).getId();
        }
        if ((position != -1) && (mAdapter.getMsg(position).getType() == LocalMessageInfo.TYPE_TEXT)) {
            createTextLongclickDialog(mAdapter.getMsg(position).getMessageItems().get(0).getBody(),
                    msgId);
            return true;
        }
        mDialog.setTag(msgId);
        mDialog.show();
        return true;
    }

    @Override
    public void complete(Object owner, Object data, int success) {
        if (mMsgSender == owner) {
            // if (success >= Pipe.NET_SUCCESS) {
            Message msg = new Message();
            msg.what = UiMessageId.NMS_IM_REFRESH_LIST;
            mHandler.sendMessage(msg);
            // }
        }
    }

    private void showSpaceFullDialog() {
        AlertDialog.Builder builder = new Builder(ConversationActivity.this);
        builder.setMessage(R.string.space_not_enough);
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
                startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    public String getCurrentAccount() {
        return mServiceAccount;
    }

}
