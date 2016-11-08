package com.mediatek.mediatekdm.scomo;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConst.TAG;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.R;
import com.mediatek.mediatekdm.mdm.DownloadDescriptor;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDp;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomoDpHandler;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DmScomoState implements Serializable {
    private static final long serialVersionUID = -3895262068642227223L;

    private static final String SCOMO_STATE_FILE = "scomo_state";

    public static final int IDLE = 0;
    // There is no DP to download. SI session committed no SCOMO download action or SI session
    // failed.
    public static final int NEW_DP_FOUND = 2;
    public static final int DOWNLOADING_STARTED = 3;
    public static final int DOWNLOADING = 4;
    public static final int DOWNLOAD_PAUSED = 5;
    public static final int DOWNLOAD_FAILED = 6;
    public static final int DOWNLOAD_CANCELED = 7;
    public static final int DOWNLOAD_COMPLETE = 8;

    public static final int INSTALLING = 21;
    public static final int CONFIRM_INSTALL = 23; // no one actually uses this

    public String errorMsg;

    // TODO: journal file is needed to fix problems like power off, reboot, ...
    // if rebooted during installing, state should be set to CONFIRM_INSTALLED
    // if rebooted during downloading, state should be set to DOWNLOAD_PAUSED

    public int state = IDLE;
    public long currentSize = 0;
    public long totalSize = 0;
    public MdmScomoDp currentDp;
    public DownloadDescriptor currentDd;
    public DmScomoPackageManager.ScomoPackageInfo pkgInfo;
    public String archiveFilePath = "";

    /**
     * verbose is used by SCOMO listeners to decide whether to interact with user. verbose will be
     * set in MmiConfirmation (ALERT 1101), and be reset after DM session.
     */
    public boolean verbose = false;
    private MdmScomoDpHandler mDpHandler;

    private DmScomoState(MdmScomoDpHandler handler) {
        mDpHandler = handler;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeUTF(this.archiveFilePath);
        out.writeInt(this.state);
        out.writeLong(this.currentSize);
        out.writeLong(this.totalSize);
        out.writeBoolean(verbose);
        if (this.currentDp != null && this.currentDp.getName() != null) {
            out.writeUTF(this.currentDp.getName());
        } else {
            out.writeUTF("");
        }

        // write dd
        if (currentDd == null) {
            out.writeLong(0);
        } else {
            out.writeLong(currentDd.size);
            out.writeObject(currentDd.field);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        archiveFilePath = in.readUTF();
        state = in.readInt();
        currentSize = in.readLong();
        totalSize = in.readLong();
        verbose = in.readBoolean();
        // reload DP
        // FIXME move this to ScomoManager
        String dpName = in.readUTF();
        if (!dpName.equals("")) {
            try {
                currentDp = MdmScomo.getInstance(ScomoComponent.ROOT_URI, null).createDP(dpName,
                        mDpHandler);
            } catch (MdmException e) {
                e.printStackTrace();
                currentDp = null;
            }
        }

        // reload DD
        long ddSize = in.readLong();
        if (ddSize == 0) {
            currentDd = null;
        } else {
            currentDd = new DownloadDescriptor();
            currentDd.size = ddSize;
            currentDd.field = (String[]) in.readObject();
        }

        // reload state
        if (state == DOWNLOAD_PAUSED) {
            Log.d(TAG.SCOMO, "Paused");
        } else if (state == INSTALLING || state == CONFIRM_INSTALL) {
            state = CONFIRM_INSTALL;
            // reload pkgInfo from archivePath;
            setArchivePath(this.archiveFilePath);
        } else if (state == NEW_DP_FOUND || state == DOWNLOADING) {
            // reset state
            state = DOWNLOAD_PAUSED;
        } else {
            // reset state
            Log.e(TAG.SCOMO, "abnormal exit, delete delta files");
            resetState();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DmScomoState = {");
        sb.append("state: " + state + ", ");
        sb.append("verbose: " + verbose + ", ");
        sb.append("currentSize: " + currentSize + ", ");
        sb.append("totalSize: " + totalSize + ", ");
        sb.append("archiveFilePath: " + archiveFilePath + ", ");

        sb.append("}");
        return sb.toString();
    }

    private void resetState() {
        Log.d(TAG.SCOMO, "resetState!");
        this.state = IDLE;
        this.currentDd = null;
        this.currentDp = null;
        this.pkgInfo = null;
        this.currentSize = 0;
        this.totalSize = 0;
    }

    public void setArchivePath(String path) {
        this.archiveFilePath = path;
        this.pkgInfo = DmScomoPackageManager.getInstance().getMinimalPackageInfo(path);
    }

    public static DmScomoState load(Context context, MdmScomoDpHandler dpHandler) {
        Object obj = null;
        obj = PlatformManager.getInstance().atomicRead(context.getFileStreamPath(SCOMO_STATE_FILE));

        if (obj != null) {
            DmScomoState ret = (DmScomoState) obj;
            Log.i(TAG.SCOMO, "DmScomoState: state loaded: state= " + ret.state);
            return ret;
        }
        return new DmScomoState(dpHandler);
    }

    public static void store(Context context, DmScomoState state) {
        if (state == null) {
            return;
        }
        if (state.state == IDLE) {
            Log.i(TAG.SCOMO, "state is IDLE, reset state");
            state.resetState();
        }
        PlatformManager.getInstance().atomicWrite(context.getFileStreamPath(SCOMO_STATE_FILE),
                state);
    }

    // ////////////////// name,icon,version,description may from different
    // source,e.g. PM > dd > dp ////////////////
    public String getName() {
        String ret = "";
        if (pkgInfo != null && pkgInfo.label != null) {
            ret = pkgInfo.label;
        } else if (currentDd != null && currentDd.getField(DownloadDescriptor.Field.NAME) != null) {
            ret = currentDd.getField(DownloadDescriptor.Field.NAME);
        } else if (currentDp != null && currentDp.getName() != null) {
            ret = "";
        }
        return ret;
    }

    public String getVersion() {
        Log.i(TAG.SCOMO, "getVersion begin");
        String ret = "";
        if (pkgInfo != null && pkgInfo.version != null) {
            ret = pkgInfo.version;
        } else if (currentDd != null
                && currentDd.getField(DownloadDescriptor.Field.DD_VERSION) != null) { //
            ret = currentDd.getField(DownloadDescriptor.Field.DD_VERSION);
        }

        if (ret.trim().equals("")) {
            ret = DmApplication.getInstance().getString(R.string.unknown);
            Log.i(TAG.SCOMO, "Only white space, use default " + ret);
        }
        Log.i(TAG.SCOMO, "Return version: " + ret);
        Log.i(TAG.SCOMO, "getVersion end");
        return ret;
    }

    public Drawable getIcon() {
        Drawable ret;
        if (pkgInfo != null) {
            // pkgInfo.icon is assured to be not-null
            ret = pkgInfo.icon;
        } else {
            ret = DmScomoPackageManager.getInstance().getDefaultActivityIcon();
        }
        return ret;
    }

    public long getSize() {
        long ret = -1;
        if (this.totalSize != -1 && this.totalSize != 0) {
            ret = this.totalSize;
        } else if (currentDd != null) {
            ret = (int) currentDd.size;
        }
        return ret;

    }

    public CharSequence getDescription() {
        Log.i(TAG.SCOMO, "getdescription begin");
        String ret = "";
        if (pkgInfo != null && pkgInfo.description != null) {
            ret = pkgInfo.description;
        } else if (currentDd != null
                && currentDd.getField(DownloadDescriptor.Field.DESCRIPTION) != null) {
            ret = currentDd.getField(DownloadDescriptor.Field.DESCRIPTION);
        }

        if (ret.trim().equals("")) {
            ret = DmApplication.getInstance().getString(R.string.default_scomo_description);
            Log.i(TAG.SCOMO, "Only white space, use default " + ret);
        }

        Log.i(TAG.SCOMO, "Return description: " + ret);
        Log.i(TAG.SCOMO, "getdescription end");
        return ret;
    }

    public String getPackageName() {
        String ret = "";
        if (pkgInfo != null && pkgInfo.name != null) {
            ret = pkgInfo.name;
        } else {
            try {
                if (currentDp != null && currentDp.getPkgName() != null) {
                    ret = currentDp.getPkgName();
                }
            } catch (MdmException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}
