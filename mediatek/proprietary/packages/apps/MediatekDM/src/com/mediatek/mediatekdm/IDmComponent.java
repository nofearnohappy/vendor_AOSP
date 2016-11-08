package com.mediatek.mediatekdm;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.SessionInitiator;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

public interface IDmComponent {

    enum DispatchResult {
        /** The request is recognized by this component and it is invalid. */
        ABORT,
        /** The request dispatched is accepted and processed. */
        ACCEPT,
        /**
         * Same as ACCEPT, but this also requires DmService to trigger the network for further
         * processing. NOTE: This is only valid for dispatchCommand().
         */
        ACCEPT_AND_TRIGGER,
        /** This component is not interested in this request. */
        IGNORE,
    }

    enum OperationAction {
        NEW, RECOVER, RETRY,
    }

    /**
     * Test whether this operation is specific to this component.
     *
     * @param initiator
     *        Session initiator.
     * @param operation
     *        DmOperation.
     * @return
     */
    boolean acceptOperation(SessionInitiator initiator, DmOperation operation);

    /**
     * Attach to DmService. This method should be called after DmService is created.
     *
     * @param service
     */
    void attach(DmService service);

    /**
     * Component can configure the DM tree as needed in this method, e.g. register IO handlers.
     */
    void configureDmTree(MdmTree tree);

    /**
     * Detach from DmService. This method should be called before DmService is destroyed.
     *
     * @param service
     */
    void detach(DmService service);

    /**
     * Dispatch broadcast from DmReceiver.
     *
     * @param context
     *        Android context object from onReceive().
     * @param intent
     *        Broadcast intent object from onReceive().
     * @return
     */
    DispatchResult dispatchBroadcast(Context context, Intent intent);

    /**
     * Dispatch start command from DmService.
     *
     * @param intent
     *        Command intent object from onStartService().
     * @return
     */
    DispatchResult dispatchCommand(Intent intent);

    /**
     * Dispatch MMI progress update request.
     *
     * @param operation
     *        The operation during which this update request is dispatched.
     * @param current
     *        Current value.
     * @param total
     *        Total value.
     */
    void dispatchMmiProgressUpdate(DmOperation operation, int current, int total);

    /**
     * This request is dispatched by the handler in DmService from main loop. The argument action
     * reflects the current DmOperation action, which can be NEW, RECOVER or RETRY.
     *
     * @param action
     *        DmOperation life-cycle action which is triggered by DmService.
     * @param operation
     *        The DmOperation instance associated with the action.
     * @return
     */
    DispatchResult dispatchOperationAction(OperationAction action, DmOperation operation);

    /**
     * DmService will dispatch the session state notification as it is notified by MdmEngine via
     * SessionStateObserver interface. Instance of IDmComponent should return their SessionHandler
     * to perform their specific actions. If the component has no interest in in some notification,
     * it can simply return null and let DmService perform default actions instead.
     *
     * @param type
     *        Session type from engine.
     * @param state
     *        Session state from engine.
     * @param lastError
     *        Error reason from engine.
     * @param initiator
     *        Initiator from engine.
     * @param operation
     *        The DmOperation instance associated with the action.
     * @return Component's handler if it is interested, null otherwise.
     */
    SessionHandler dispatchSessionStateChange(SessionType type, SessionState state, int lastError,
            SessionInitiator initiator, DmOperation operation);

    /**
     * Whether DM operations should be carried out in silent mode. This flag affects NIA UI, Alert
     * 1101 UI and network connectivity.
     *
     * @return true means DM will ignore any UI confirmations and network settings to connect to DM
     *         server silently, false means we will process these normally.
     */
    boolean forceSilentMode();

    /**
     * Each component may provides their own Binder in DmService's onBind() method according to the
     * intent it received. DmService will ask each component for the binder instance according to
     * the intent. If component does not need to provide its own binder, it can simply return null
     * in this method. DmBinder provides the common interface of DmService, so it's recommended to
     * subclass it when writing your own binder.
     *
     * @param intent
     *        Intent object from onBind().
     * @return Binder object if component decides to provide specific functionality according to
     *         intent, null otherwise.
     */
    IBinder getBinder(Intent intent);

    /**
     * This DL-specific method returns the package file name of DL.
     *
     * @return
     */
    String getDlPackageFilename();

    /**
     * This DL-specific method returns the resume file name of DL.
     *
     * @return
     */
    String getDlResumeFilename();

    /**
     * Unique name of this component. DmService and other modules may use this to find the specific
     * component.
     *
     * @return Name of this component.
     */
    String getName();

    /**
     * This method is called when the prerequisites of DM is satisfied, i.e. component can trigger
     * DM session to the server. Components can trigger their own action such as scanning for
     * pending operations in this method.
     *
     * @param context
     *        Android context object.
     */
    void kickoff(Context context);

    /**
     * This method checks whether the intent of WAP push message is valid for this component.
     *
     * @param intent
     *        The intent has several extras: 1) "data": the WAP push message data, 2) "simId": which
     *        SIM card this message is from.
     * @return ACCEPT if the component is interested in the message and the message is valid, IGNORE
     *         if the component is not interested, ABORT if the component is interested in the
     *         message but the message is not valid.
     */
    DispatchResult validateWapPushMessage(Intent intent);

    boolean checkPrerequisites();
}
