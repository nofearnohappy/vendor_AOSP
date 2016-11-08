package com.mediatek.hotknotbeam;

import android.net.Uri;

interface IHotKnotBeamService {
    void sendUris(in Uri[] uris, in String ipAddress, in int flag);
    void prepareReceive(in int flag);
}
