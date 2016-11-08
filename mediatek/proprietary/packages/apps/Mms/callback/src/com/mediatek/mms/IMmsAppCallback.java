package com.mediatek.mms.callback;

import java.util.HashMap;

public interface IMmsAppCallback {
    void initMuteCache();
    void setSmsValues(HashMap<String, String> values);
    void registerSmsStateReceiver();
}
