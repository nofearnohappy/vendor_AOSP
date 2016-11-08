package com.mediatek.rcs.common;

public interface IFileSpamReportListener {

    void onFileSpamReportResult(String contact, String msgId, int errorcode);
}
