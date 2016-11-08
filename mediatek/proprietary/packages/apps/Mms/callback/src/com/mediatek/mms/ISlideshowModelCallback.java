package com.mediatek.mms.callback;

import java.util.ArrayList;

import android.content.Context;
import android.net.Uri;

import com.google.android.mms.pdu.PduBody;

public interface ISlideshowModelCallback {
    ArrayList getAttachFilesCallback();
    int sizeCallback();
    ISlideModelCallback removeCallback(int location);
    void removeAllAttachFilesCallback();
    ISlideModelCallback getCallback(int location);
}
