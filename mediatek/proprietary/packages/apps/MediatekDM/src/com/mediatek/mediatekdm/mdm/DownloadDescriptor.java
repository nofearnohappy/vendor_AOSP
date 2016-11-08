package com.mediatek.mediatekdm.mdm;

import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.mdm.dl.DownloadDescriptorHelper;

public class DownloadDescriptor {
    public static enum Field {
        /** MIME type of the media object. */
        TYPE(0),
        /** Number of bytes to be downloaded from the URI. */
        SIZE(1),
        /** URI (usually URL) from which the media object can be loaded. */
        OBJECT_URI(2),
        /** URI (or URL) to which a installation status report is to be sent. */
        INSTALL_NOTIFY_URI(3),
        /**
         * URL to which the client should navigate in case the end user selects to invoke a browsing
         * action after the download transaction has completed.
         */
        NEXT_URL(4),
        /** Version of the Download Descriptor technology. */
        DD_VERSION(5),
        /** A user readable name of the Media Object that identifies the object to the user. */
        NAME(6),
        /** A short textual description of the media object. */
        DESCRIPTION(7),
        /** The organization that provides the media object. */
        VENDOR(8),
        /** A URL for further describing the media object. */
        INFO_URL(9),
        /** The URI of an icon. */
        ICON_URI(10),
        /** An installation parameter associated with the downloaded media object. */
        INSTALL_PARAM(11);

        public final int val;

        private Field(int value) {
            val = value;
        }

        static int maxValue() {
            return 12;
        }
    }

    /** Array of DD field values. */
    public String[] field;
    /** Size of downloadable object. */
    public long size;

    public String getField(Field name) {
        return field[name.val];
    }

    public DownloadDescriptor() {
        Log.d(TAG.DL, "DownloadDescriptor constructed");
        field = new String[Field.maxValue()];
    }

    public void makeUpField(DownloadDescriptorHelper ddHelper) {
        if (ddHelper == null) {
            Log.e(TAG.DL, "ddHelper is null !");
            return;
        }
        Log.e(TAG.DL, "ddHelper:" + ddHelper.hashCode());
        Log.w(TAG.DL, "makeUpField, the Download Descriptor : ");
        Log.w(TAG.DL, "--------------------------------------");
        Log.w(TAG.DL, ddHelper.toString());
        Log.w(TAG.DL, "--------------------------------------");

        if (field == null) {
            Log.e(TAG.DL, "field is null !");
            field = new String[Field.maxValue()];
        }

        field[Field.TYPE.val] = ddHelper.getType();
        field[Field.SIZE.val] = ddHelper.getSize();
        field[Field.OBJECT_URI.val] = ddHelper.getObjectURI();
        field[Field.INSTALL_NOTIFY_URI.val] = ddHelper.getInstallNotifyURI();
        field[Field.NEXT_URL.val] = ddHelper.getNextURL();
        field[Field.DD_VERSION.val] = ddHelper.getDDVersion();
        field[Field.NAME.val] = ddHelper.getName();
        field[Field.DESCRIPTION.val] = ddHelper.getDescription();
        field[Field.VENDOR.val] = ddHelper.getVendor();
        field[Field.INFO_URL.val] = ddHelper.getInfoURL();
        field[Field.ICON_URI.val] = ddHelper.getIconURI();
        field[Field.INSTALL_PARAM.val] = ddHelper.getInstallParam();
        size = Integer.parseInt(field[Field.SIZE.val]);
    }
}
