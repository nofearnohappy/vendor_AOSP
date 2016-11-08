package com.android.mms.draft;

import com.android.mms.model.SlideshowModel;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.SendReq;
import com.google.android.mms.MmsException;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

/*******************************************************************************
 *
 * Class : SaveReq
 * Which extends TaskReq, used to finish the save action
 * (including async save & sync save) according to diffrent type
 *
*******************************************************************************/
public class SaveReq extends TaskReq {

    private static final String TAG = "[Mms][Draft][SaveReq]";

    private SendReq mSendReq;

    private PduPersister mPersister;

    private static final String FILE_NOT_FOUND = "File not found.";
    private static final String READ_WRITE_FAILURE = "Read or write file failure.";

    /**
    * Constructor
    * Subject can call SendReq.getSubject() method to get it.
    */
    public SaveReq(int type, long threadId, Uri uri, SlideshowModel slideshow, SendReq sendReq, Context context, /*Handler handler*/IDraftInterface callback) {
        if (slideshow == null || sendReq == null || context == null) {
            Log.d(TAG, "[SaveReq] The parameters is not satisfied the request");
            return;
        }
        mSlideshowModel = slideshow;
        mMessageUri = uri;
        mPersister = PduPersister.getPduPersister(context);
        mSendReq = sendReq;
        mContext = context;
        mHandler = callback;
        mHandlerWhat = type;
        mThreadId = threadId;
        if (type == DraftManager.SYNC_SAVE_ACTION) {
            mSyncObject = new Object();
        }
//        if (type == DraftManager.ASYNC_SAVE_ACTION) {
//            Log.d(TAG, "[SaveReq] Type is ASYNC_SAVE_ACTION, save the content to result which used to call in load action");
//            MmsDraftData mdd = new MmsDraftData();
//            mdd.setMessageUri(uri);
//            mdd.setBooleanResult(true);
//            mdd.setSlideshow(slideshow);
//            String sub = mSendReq.getSubject().getString();
//
//            if (sub != null && sub.length() != 0) {
//                mdd.setSubject(sub);
//                Log.d(TAG, "[SaveReq] Type is ASYNC_SAVE_ACTION, subject : " + sub);
//            }
//            mResult = mdd;
//        }
    }

    /**
    * return the save req type
    */
    public int getType() {
        return mHandlerWhat;
    }

    /**
    * return the save req handler
    */
    public IDraftInterface getHandler() {
        return mHandler;
    }

    public int getWhat() {
        return mHandlerWhat;
    }

    /**
    * execute the save action
    */
    public void executeReq() {
        Log.d(TAG, "[executeReq] enter");
        DraftAction da = new DraftAction();
        MmsDraftData returnData = new MmsDraftData();
        returnData.setSlideshow(mSlideshowModel);
        if (mMessageUri == null) {
            Log.d(TAG, "[executeReq] mMessageUri is null, call create");
            try {
                Uri tempUri = da.createDraftMmsMessage(mPersister, mSendReq, mSlideshowModel, mMessageUri, mContext);
                returnData.setBooleanResult(true);
                returnData.setMessageUri(tempUri);
                mMessageUri = tempUri;
            } catch (MmsException e) {
                final String eMessage = e.getMessage();
                Log.d(TAG, eMessage);
                if (eMessage.equals(FILE_NOT_FOUND) || eMessage.equals(READ_WRITE_FAILURE)) {
                    returnData.setBooleanResult(false);
                }
                returnData.setMessageUri(null);
            }
            returnData.setCreateOrUpdate(1);
        } else {
            Log.d(TAG, "[executeReq] update, mMessageUri is : " + mMessageUri);
            da.updateDraftMmsMessage(mMessageUri, mPersister, mSlideshowModel, mSendReq);
            returnData.setMessageUri(mMessageUri);
            returnData.setCreateOrUpdate(0);
        }
        mResult = returnData;
    }

}
