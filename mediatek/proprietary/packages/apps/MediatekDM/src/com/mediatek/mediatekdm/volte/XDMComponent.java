
package com.mediatek.mediatekdm.volte;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.SubscriptionManager;

import com.android.ims.ImsManager;
import com.mediatek.mediatekdm.DmOperation;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.IDmComponent;
import com.mediatek.mediatekdm.SessionHandler;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;
import com.mediatek.mediatekdm.volte.imsio.RCSRealmHandler;
import com.mediatek.mediatekdm.volte.imsio.RCSUserNameHandler;
import com.mediatek.mediatekdm.volte.imsio.RCSUserPwdHandler;
import com.mediatek.mediatekdm.volte.xdmio.UriHandler;

public class XDMComponent implements IDmComponent {
    static final String NAME = "XDM";
    private ImsManager mImsManager;
    private static final String ROOT_URI = "./XDMMO";

    @Override
    public boolean acceptOperation(SessionInitiator initiator, DmOperation operation) {
        return false;
    }

    @Override
    public void attach(DmService service) {
        mImsManager = ImsManager.getInstance(service, SubscriptionManager.getDefaultSubId());
    }

    @Override
    public void detach(DmService service) {
        mImsManager = null;
    }

    @Override
    public DispatchResult dispatchBroadcast(Context context, Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public DispatchResult dispatchCommand(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public void dispatchMmiProgressUpdate(DmOperation operation, int current, int total) {
    }

    @Override
    public DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation) {
        return DispatchResult.IGNORE;
    }

    @Override
    public SessionHandler dispatchSessionStateChange(
            SessionType type,
            SessionState state,
            int lastError,
            SessionInitiator initiator,
            DmOperation operation) {
        return null;
    }

    @Override
    public boolean forceSilentMode() {
        return false;
    }

    @Override
    public IBinder getBinder(Intent intent) {
        return null;
    }

    @Override
    public String getDlPackageFilename() {
        return null;
    }

    @Override
    public String getDlResumeFilename() {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void kickoff(Context context) {
    }

    @Override
    public void configureDmTree(MdmTree tree) {
        // RCS-related
        try {
            String uri = MdmTree.makeUri(ROOT_URI, "URI");
            tree.registerNodeIoHandler(uri, new UriHandler(uri, mImsManager));
            uri = MdmTree.makeUri(ROOT_URI, "AAUTHNAME");
            tree.registerNodeIoHandler(uri, new RCSRealmHandler(uri, mImsManager));
            uri = MdmTree.makeUri(ROOT_URI, "AAUTHSECRET");
            tree.registerNodeIoHandler(uri, new RCSUserNameHandler(uri, mImsManager));
            uri = MdmTree.makeUri(ROOT_URI, "AAUTHTYPE");
            tree.registerNodeIoHandler(uri, new RCSUserPwdHandler(uri, mImsManager));
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    @Override
    public DispatchResult validateWapPushMessage(Intent intent) {
        return DispatchResult.IGNORE;
    }

    @Override
    public boolean checkPrerequisites() {
        return true;
    }

}
