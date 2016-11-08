package com.mediatek.rcs.common;

public interface ISpamReportListener {

    void onSpamReportResult(String contact, String msgId, int errorcode);
}
