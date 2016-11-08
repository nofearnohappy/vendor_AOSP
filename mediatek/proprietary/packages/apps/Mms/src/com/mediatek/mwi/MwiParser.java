package com.mediatek.mwi;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

public class MwiParser {
    private static final String TAG = "Mms/Mwi/MwiParser";
    private static final boolean DEBUG = true;
    private static final String sSeparator = ":";
    private static final String sTerminator = "\n";

    /*
     * Parse whole data.
     */
    public static ArrayList<MwiMessage> parseMwi(Context context, String data) {
        data = data.replaceAll("\r\n", "\n");
        String header;
        String body;
        String mwiBegin = MwiMessage.Label.To.getLabel();
        int begin = data.indexOf(mwiBegin);
        if (begin < 0) {
            return null;
        }
        header = data.substring(0, begin);
        body = data.substring(begin);

        MwiHeader mwiHeader = parseHeader(header);
        ArrayList<MwiMessage> msgList = parseBody(context, body, mwiHeader);
        Log.d(TAG, "Mwi header: " + header + ", body: " + body);
        return msgList;
    }

    /*
     * Parse header of data.
     */
    private static MwiHeader parseHeader(String header) {
        MwiHeader mwiHeader = new MwiHeader();
        int begin = header.indexOf(sSeparator);
        int end = header.indexOf(sTerminator);
        String str = header.substring(0, end);
        String remain = header.substring(end + sTerminator.length());
        String label;
        String content;

        do {
            if (DEBUG) {
                Log.d(TAG, "parseHeader: str: " + str);
            }
            label = str.substring(0, begin).trim();
            content = str.substring(begin + sSeparator.length()).trim();
            if (label.equals(MwiHeader.Label.MsgAccount.getLabel())) {
                mwiHeader.setMsgAccount(content);
            }
            begin = remain.indexOf(sSeparator);
            end = remain.indexOf(sTerminator);
            if (end < 0) {
                str = remain;
                remain = "";
            } else {
                str = remain.substring(0, end);
                remain = remain.substring(end + sTerminator.length());
            }
        } while (begin >= 0);
        return mwiHeader;
    }

    /*
     * Parse body to messages.
     */
    private static ArrayList<MwiMessage> parseBody(Context context, String body, MwiHeader mwiHeader) {
        ArrayList<MwiMessage> msgList = new ArrayList<MwiMessage>();
        // Every message begin with To label
        String remain = body;
        int next = -1;
        String curMsg;

        while (!remain.equals("")) {
            next = remain.indexOf(MwiMessage.Label.To.getLabel(),
                    MwiMessage.Label.To.getLabel().length() + 1);
            if (next >= 0) {
                curMsg = remain.substring(0, next);
                remain = remain.substring(next);
            } else {
                curMsg = remain;
                remain = "";
            }
            if (DEBUG) {
                Log.d(TAG, "parseBody, curMsg: " + curMsg);
            }

            MwiMessage mwiMsg = parseMessage(context, curMsg, mwiHeader);
            msgList.add(mwiMsg);
        }

        return msgList;
    }

    /*
     * Parse one message.
     */
    private static MwiMessage parseMessage(Context context, String message, MwiHeader mwiHeader) {
        MwiMessage mwiMessage = new MwiMessage(context);
        mwiMessage.setMsgAccount(mwiHeader.getMsgAccount());
        int begin = message.indexOf(sSeparator);
        int end = message.indexOf(sTerminator);
        String str = message.substring(0, end);
        String remain = message.substring(end + sTerminator.length());
        String label;
        String content;

        do {
            if (DEBUG) {
                Log.d(TAG, "parseHeader: str: " + str);
            }
            label = str.substring(0, begin).trim();
            content = str.substring(begin + sSeparator.length()).trim();

            if (label.equals(MwiMessage.Label.To.getLabel())) {
                mwiMessage.setTo(content);
            } else if (label.equals(MwiMessage.Label.From.getLabel())) {
                mwiMessage.setFrom(content);
            } else if (label.equals(MwiMessage.Label.Subject.getLabel())) {
                mwiMessage.setSubject(content);
            } else if (label.equals(MwiMessage.Label.Date.getLabel())) {
                mwiMessage.setDate(content);
            } else if (label.equals(MwiMessage.Label.Priority.getLabel())) {
                mwiMessage.setPriority(content);
            } else if (label.equals(MwiMessage.Label.MsgId.getLabel())) {
                mwiMessage.setMsgId(content);
            } else if (label.equals(MwiMessage.Label.MsgContext.getLabel())) {
                mwiMessage.setMsgContext(content);
            }

            begin = remain.indexOf(sSeparator);
            end = remain.indexOf(sTerminator);
            if (end < 0) {
                str = remain;
                remain = "";
            } else {
                str = remain.substring(0, end);
                remain = remain.substring(end + sTerminator.length());
            }
        } while (begin >= 0);
        return mwiMessage;
    }
}
