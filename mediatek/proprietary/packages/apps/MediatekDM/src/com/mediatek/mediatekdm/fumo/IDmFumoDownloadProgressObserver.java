package com.mediatek.mediatekdm.fumo;

public interface IDmFumoDownloadProgressObserver {
    void updateProgress(long current, long total);
}
