package com.mediatek.mediatekdm.mdm;

/**
 * An abstract porting layer factory interface.
 */
public interface PLFactory {

    /**
     * Allocate a new Download Package instance.
     *
     * @return a new instance of Download Package.
     */
    PLDlPkg getDownloadPkg();

    /**
     * Allocate a new Registry instance.
     *
     * @return a new instance of Registry.
     */
    PLRegistry getRegistry();

    /**
     * Allocate a new Storage instance.
     *
     * @return a new instance of Storage.
     */
    PLStorage getStorage();

    /**
     * Allocate a new HttpConnection instance.
     *
     * @param url
     *        URL of the host, which can be either HTTP of HTTPS.
     * @param proxyType
     *        0 -- DIRECT, 1 -- PROXY(HTTP??), 2 --SOCKS
     * @param proxyAddr
     *        URL of the proxy.
     * @param proxyPort
     *        port of the proxy.
     * @return A new HttpConnection instance or null. If it returns null, then engine will use its
     *         own implementation.
     */
    PLHttpConnection getHttpConnection();
}
