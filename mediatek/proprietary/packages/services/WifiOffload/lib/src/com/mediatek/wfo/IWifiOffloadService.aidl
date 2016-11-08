/**
 * {@hide}
 */
package com.mediatek.wfo;

import com.mediatek.wfo.IWifiOffloadListener;
import com.mediatek.wfo.DisconnectCause;

interface IWifiOffloadService {
    void registerForHandoverEvent(in IWifiOffloadListener listener);
    void unregisterForHandoverEvent(in IWifiOffloadListener listener);
    int getRatType();
    DisconnectCause getDisconnectCause();
}