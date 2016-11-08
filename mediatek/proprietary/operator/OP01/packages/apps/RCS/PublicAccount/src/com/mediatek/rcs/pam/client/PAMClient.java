package com.mediatek.rcs.pam.client;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.PlatformManager;
import com.mediatek.rcs.pam.util.Utils;
import com.mediatek.rcs.pam.model.AccountsInfo;
import com.mediatek.rcs.pam.model.ComplainInfo;
import com.mediatek.rcs.pam.model.MenuInfo;
import com.mediatek.rcs.pam.model.MessageContent;
import com.mediatek.rcs.pam.model.MessageHistoryInfo;
import com.mediatek.rcs.pam.model.MessageName;
import com.mediatek.rcs.pam.model.PublicAccount;
import com.mediatek.rcs.pam.model.ResultCode;
import com.mediatek.rcs.pam.model.SetAcceptStatusInfo;
import com.mediatek.rcs.pam.model.XmlMessageBuilder;
import com.mediatek.rcs.pam.model.XmlMessageParser;

import java.net.HttpURLConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class PAMClient {
    private static final String TAG = Constants.TAG_PREFIX + "PAMClient";

    public static final String VERSION_1_0 = "1.0";

    public interface Transmitter {
        Response sendRequest(String msgname, String content, boolean postOrGet) throws IOException;
    }

    public static class Response {
        public int result;
        // TODO Can we change the type from String to InputStream
        // to save useless back-and-forth conversions between these
        // two types?
        public String content;
    }

    private XmlMessageBuilder mMessageBuilder;
    private XmlMessageParser mMessageParser;
    private Transmitter mTransmitter;
    private Context mContext;

    public PAMClient(Transmitter transmitter, Context context) {
        mTransmitter = transmitter;
        mMessageBuilder = new XmlMessageBuilder();
        mMessageParser = new XmlMessageParser();
        mContext = context;
    }

    public void subscribe(String id) throws PAMException {
        Log.d(TAG, "+subscribe(" + id + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, TextUtils.isEmpty(id));
        // build message
        String xmlMessage = mMessageBuilder.buildSubscribeRequest(
                id, PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.SUBSCRIBE, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            String uuid = mMessageParser.parseSubscribeMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, !uuid.equals(id));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        Log.d(TAG, "-subscribe");
    }

    public void unsubscribe(String id) throws PAMException {
        Log.d(TAG, "+unsubscribe(" + id + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, TextUtils.isEmpty(id));
        // build message
        String xmlMessage = mMessageBuilder.buildUnsubscribeRequest(
                id, PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.UNSUBSCRIBE, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            String uuid = mMessageParser.parseUnsubscribeMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, !uuid.equals(id));
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        Log.d(TAG, "-unsubscribe");
    }

    public List<PublicAccount> getSubscribedList(
            int order, int pageSize, int pageNumber) throws PAMException {
        Log.d(TAG, "+getSubscribedList(" + order + ", " + pageSize + ", " + pageNumber + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (pageSize <= 0) ||
                (pageNumber <= 0) ||
                (order != Constants.ORDER_BY_REVERSED_TIMESTAMP
                && order != Constants.ORDER_BY_NAME));
        // build message
        String xmlMessage = mMessageBuilder.buildGetSubscribedListRequest(
                order,
                pageSize,
                pageNumber,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.GET_SUBSCRIBED_LIST, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            AccountsInfo result = mMessageParser.parseGetSubscribedListMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Log.d(TAG, "-getSubscribedList");
            return result.accounts;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public List<PublicAccount> search(String keyword,
            int order, int pageSize, int pageNumber) throws PAMException {
        Log.d(TAG, "+search(" + keyword + ", " + order + ", " + pageSize + ", " + pageNumber + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(keyword) || TextUtils.isEmpty(keyword)));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (pageSize <= 0) ||
                (pageNumber <= 0) ||
                (order != Constants.ORDER_BY_REVERSED_TIMESTAMP
                && order != Constants.ORDER_BY_NAME));
        if ((order != Constants.ORDER_BY_REVERSED_TIMESTAMP && order != Constants.ORDER_BY_NAME) ||
            pageSize <= 0 || pageNumber < 0) {
            throw new PAMException(ResultCode.PARAM_ERROR_INVALID_FORMAT);
        }
        // build message
        String xmlMessage = mMessageBuilder.buildSearchRequest(
                keyword,
                order,
                pageSize,
                pageNumber,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.SEARCH, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            AccountsInfo result = mMessageParser.parseSearchMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Log.d(TAG, "-search");
            return result.accounts;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public PublicAccount getDetails(String uuid, String timestamp) throws PAMException {
        Log.d(TAG, "+getDetails(" + uuid + ", " + timestamp + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(uuid)));
        // build message
        String xmlMessage = mMessageBuilder.buildGetDetailsRequest(
                uuid,
                timestamp,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.GET_DETAILS, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            PublicAccount result = mMessageParser.parseGetDetailsMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Log.d(TAG, "-getDetails");
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public MenuInfo getMenu(String uuid, String timestamp) throws PAMException {
        Log.d(TAG, "+getMenu(" + uuid + ", " + timestamp + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(uuid)));
        // build message
        String xmlMessage = mMessageBuilder.buildGetMenuRequest(
                uuid,
                timestamp,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.GET_MENU, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            MenuInfo result = mMessageParser.parseGetMenuMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT, !uuid.equals(result.uuid));
            Log.d(TAG, "-getMenu");
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public List<MessageContent> getMessageHistory(
            String uuid, String timestamp, int order, int pageSize, int pageNumber)
            throws PAMException {
        Log.d(TAG,
              "+getMessageHistory(" + uuid + ", " + timestamp + ", "
                      + order + ", " + pageSize + ", " + pageNumber + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                (TextUtils.isEmpty(uuid) || TextUtils.isEmpty(timestamp)));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (pageSize <= 0) ||
                (pageNumber <= 0) ||
                (order != Constants.ORDER_BY_TIMESTAMP_ASCENDING
                && order != Constants.ORDER_BY_TIMESTAMP_DESCENDING));
        // build message
        String xmlMessage = mMessageBuilder.buildGetMessageHistoryRequest(
                uuid,
                timestamp,
                order,
                pageSize,
                pageNumber,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.GET_MESSAGE_HISTORY, xmlMessage, false);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            MessageHistoryInfo result = mMessageParser.parseGetMessageHistoryMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            // FIXME CMCC Workaround
            // for CMCC server compatibility
//            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT, !uuid.equals(result.uuid));
            Log.d(TAG, "-getMessageHistory");
            return result.messages;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public int complain(String uuid, int type,
            String reason, String data, String description) throws PAMException {
        Log.d(TAG, "+complain(" + uuid + ", " + type + ", "
            + reason + ", " + data + ", " + description + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                TextUtils.isEmpty(uuid));
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                TextUtils.isEmpty(data) && type == Constants.COMPLAIN_TYPE_MESSAGE);
        // build message
        String xmlMessage = mMessageBuilder.buildComplainRequest(
                uuid,
                type,
                reason,
                data,
                description,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.COMPLAIN, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            ComplainInfo result = mMessageParser.parseComplainMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT, !uuid.equals(result.uuid));
            Log.d(TAG, "-complain");
            return result.result;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public List<PublicAccount> getRecommends(
            int type, int pageSize, int pageNumber) throws PAMException {
        Log.d(TAG, "+getRecommends(" + type + ", " + pageSize + ", " + pageNumber + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (type < 0 || pageSize <= 0 || pageNumber < 0));
        // build message
        String xmlMessage = mMessageBuilder.buildGetRecommendsRequest(
                type,
                pageSize,
                pageNumber,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.GET_RECOMMENDS, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            AccountsInfo result = mMessageParser.parseGetRecommendsMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Log.d(TAG, "-getRecommends");
            return result.accounts;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    public int setAcceptStatus(String uuid, int acceptStatus) throws PAMException {
        Log.d(TAG, "+setAcceptStatus(" + uuid + ", " + acceptStatus + ")");
        // sanity check
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                TextUtils.isEmpty(uuid));
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                (acceptStatus != Constants.ACCEPT_STATUS_YES
                && acceptStatus != Constants.ACCEPT_STATUS_NO));
        // build message
        String xmlMessage = mMessageBuilder.buildSetAcceptStatusRequest(
                uuid,
                acceptStatus,
                PlatformManager.getInstance().getUserId(mContext));
        // send & receive
        Response response = sendRequest(MessageName.GET_RECOMMENDS, xmlMessage, true);
        Utils.throwIf(ResultCode.SYSTEM_ERROR_NETWORK,
                (response.result != HttpURLConnection.HTTP_OK));
        try {
            SetAcceptStatusInfo result = mMessageParser.parseSetAcceptStatusMessage(
                    new ByteArrayInputStream(response.content.getBytes("UTF-8")));
            Log.d(TAG, "-setAcceptStatus");
            return result.result;
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }

    private Response sendRequest(String msgname,
            String content, boolean postOrGet) throws PAMException {
        try {
            return mTransmitter.sendRequest(msgname, content, postOrGet);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
    }
}
