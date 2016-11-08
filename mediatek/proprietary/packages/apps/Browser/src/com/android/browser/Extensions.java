package com.android.browser;

import android.content.Context;

import com.mediatek.browser.ext.DefaultBrowserBookmarkExt;
import com.mediatek.browser.ext.DefaultBrowserDownloadExt;
import com.mediatek.browser.ext.DefaultBrowserHistoryExt;
import com.mediatek.browser.ext.DefaultBrowserMiscExt;
import com.mediatek.browser.ext.DefaultBrowserRegionalPhoneExt;
import com.mediatek.browser.ext.DefaultBrowserSettingExt;
import com.mediatek.browser.ext.DefaultBrowserSiteNavigationExt;
import com.mediatek.browser.ext.DefaultBrowserUrlExt;
import com.mediatek.browser.ext.IBrowserBookmarkExt;
import com.mediatek.browser.ext.IBrowserDownloadExt;
import com.mediatek.browser.ext.IBrowserHistoryExt;
import com.mediatek.browser.ext.IBrowserMiscExt;
import com.mediatek.browser.ext.IBrowserRegionalPhoneExt;
import com.mediatek.browser.ext.IBrowserSettingExt;
import com.mediatek.browser.ext.IBrowserSiteNavigationExt;
import com.mediatek.browser.ext.IBrowserUrlExt;
import com.mediatek.common.MPlugin;

/**
 * Helper class to create plugin instance.
 */
public class Extensions {
    private static volatile IBrowserBookmarkExt sBookmarkPlugin = null;
    private static volatile IBrowserDownloadExt sDownloadPlugin = null;
    private static volatile IBrowserHistoryExt sHistoryPlugin = null;
    private static volatile IBrowserMiscExt sMiscPlugin = null;
    private static volatile IBrowserRegionalPhoneExt sRegionalPhonePlugin = null;
    private static volatile IBrowserSettingExt sSettingPlugin = null;
    private static volatile IBrowserSiteNavigationExt sSiteNavigationPlugin = null;
    private static volatile IBrowserUrlExt sUrlPlugin = null;

    private Extensions() {
    };

    /**
     * get browser bookmark plugin instance.
     * @param context browser context
     * @return browser bookmark plugin instance
     */
    public static IBrowserBookmarkExt getBookmarkPlugin(Context context) {
        if (sBookmarkPlugin == null) {
            synchronized (Extensions.class) {
                if (sBookmarkPlugin == null) {
                    sBookmarkPlugin = (IBrowserBookmarkExt) MPlugin.createInstance(
                                        IBrowserBookmarkExt.class.getName(), context);
                    if (sBookmarkPlugin == null) {
                        sBookmarkPlugin = new DefaultBrowserBookmarkExt();
                    }
                }
            }
        }
        return sBookmarkPlugin;
    }

    /**
     * get browser download plugin instance.
     * @param context browser context
     * @return browser download plugin instance
     */
    public static IBrowserDownloadExt getDownloadPlugin(Context context) {
        if (sDownloadPlugin == null) {
            synchronized (Extensions.class) {
                if (sDownloadPlugin == null) {
                    sDownloadPlugin = (IBrowserDownloadExt) MPlugin.createInstance(
                                        IBrowserDownloadExt.class.getName(), context);
                    if (sDownloadPlugin == null) {
                        sDownloadPlugin = new DefaultBrowserDownloadExt();
                    }
                }
            }
        }
        return sDownloadPlugin;
    }

    /**
     * get browser history plugin instance.
     * @param context browser context
     * @return browser history plugin instance
     */
    public static IBrowserHistoryExt getHistoryPlugin(Context context) {
        if (sHistoryPlugin == null) {
            synchronized (Extensions.class) {
                if (sHistoryPlugin == null) {
                    sHistoryPlugin = (IBrowserHistoryExt) MPlugin.createInstance(
                                        IBrowserHistoryExt.class.getName(), context);
                    if (sHistoryPlugin == null) {
                        sHistoryPlugin = new DefaultBrowserHistoryExt();
                    }
                }
            }
        }
        return sHistoryPlugin;
    }

    /**
     * get browser misc plugin instance.
     * @param context browser context
     * @return browser misc plugin instance
     */
    public static IBrowserMiscExt getMiscPlugin(Context context) {
        if (sMiscPlugin == null) {
            synchronized (Extensions.class) {
                if (sMiscPlugin == null) {
                    sMiscPlugin = (IBrowserMiscExt) MPlugin.createInstance(
                                    IBrowserMiscExt.class.getName(), context);
                    if (sMiscPlugin == null) {
                        sMiscPlugin = new DefaultBrowserMiscExt();
                    }
                }
            }
        }
        return sMiscPlugin;
    }

    /**
     * get browser regional phone plugin instance.
     * @param context browser context
     * @return browser regional phone plugin instance
     */
    public static IBrowserRegionalPhoneExt getRegionalPhonePlugin(Context context) {
        if (sRegionalPhonePlugin == null) {
            synchronized (Extensions.class) {
                if (sRegionalPhonePlugin == null) {
                    sRegionalPhonePlugin = (IBrowserRegionalPhoneExt) MPlugin.createInstance(
                                            IBrowserRegionalPhoneExt.class.getName(), context);
                    if (sRegionalPhonePlugin == null) {
                        sRegionalPhonePlugin = new DefaultBrowserRegionalPhoneExt();
                    }
                }
            }
        }
        return sRegionalPhonePlugin;
    }

    /**
     * get browser setting plugin instance.
     * @param context browser context
     * @return browser setting plugin instance
     */
    public static IBrowserSettingExt getSettingPlugin(Context context) {
        if (sSettingPlugin == null) {
            synchronized (Extensions.class) {
                if (sSettingPlugin == null) {
                    sSettingPlugin = (IBrowserSettingExt) MPlugin.createInstance(
                                        IBrowserSettingExt.class.getName(), context);
                    if (sSettingPlugin == null) {
                        sSettingPlugin = new DefaultBrowserSettingExt();
                    }
                }
            }
        }
        return sSettingPlugin;
    }

    /**
     * get browser site navigation plugin instance.
     * @param context browser context
     * @return browser site navigation plugin instance
     */
    public static IBrowserSiteNavigationExt getSiteNavigationPlugin(Context context) {
        if (sSiteNavigationPlugin == null) {
            synchronized (Extensions.class) {
                if (sSiteNavigationPlugin == null) {
                     sSiteNavigationPlugin = (IBrowserSiteNavigationExt) MPlugin.createInstance(
                                                IBrowserSiteNavigationExt.class.getName(), context);
                    if (sSiteNavigationPlugin == null) {
                        sSiteNavigationPlugin = new DefaultBrowserSiteNavigationExt();
                    }
                }
            }
        }
        return sSiteNavigationPlugin;
    }

    /**
     * get browser url plugin instance.
     * @param context browser context
     * @return browser url plugin instance
     */
    public static IBrowserUrlExt getUrlPlugin(Context context) {
        if (sUrlPlugin == null) {
            synchronized (Extensions.class) {
                if (sUrlPlugin == null) {
                    sUrlPlugin = (IBrowserUrlExt) MPlugin.createInstance(
                                    IBrowserUrlExt.class.getName(), context);
                    if (sUrlPlugin == null) {
                        sUrlPlugin = new DefaultBrowserUrlExt();
                    }
                }
            }
        }
        return sUrlPlugin;
    }

}
