package com.mediatek.rcs.pam.model;

import android.text.TextUtils;

import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.util.Utils;

import java.util.LinkedList;
import java.util.List;

public class MessageHistoryInfo implements SanityCheck {
    public final List<MessageContent> messages;
    public String uuid;

    public MessageHistoryInfo() {
        messages = new LinkedList<MessageContent>();
    }

    @Override
    public void checkSanity() throws PAMException {
        Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT, TextUtils.isEmpty(uuid));

    }
}
