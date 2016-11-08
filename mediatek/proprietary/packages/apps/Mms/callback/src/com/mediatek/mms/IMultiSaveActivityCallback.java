package com.mediatek.mms.callback;

import java.util.ArrayList;

import android.content.Context;

import com.google.android.mms.pdu.PduPart;

public interface IMultiSaveActivityCallback {
    void addMultiListItemData(Context context, ArrayList attaches, PduPart part, long msgId);
}
