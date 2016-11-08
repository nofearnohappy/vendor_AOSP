package com.mediatek.mediatekdm.mdm;

import android.content.Context;

import com.mediatek.mediatekdm.mdm.MdmConfig.DmAccConfiguration;
import com.mediatek.mediatekdm.mdm.MdmException.MdmError;
import com.mediatek.mediatekdm.mdm.NIAMsgHandler.UIMode;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionState;
import com.mediatek.mediatekdm.mdm.SessionStateObserver.SessionType;

import java.nio.charset.Charset;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

public class MdmEngine {
    private static final String TAG = "MDM/MdmEngine";
    private PLLogger mLogger;
    private Context mContext;
    private MmiFactory mMmiFactory;
    private PLStorage mPLStorage;
    private PLFactory mPLFactory;
    private static MdmEngine sInstance = null;
    private MmiObserverImpl mMmiObserver;
    private MmiViewContext mMmiViewContext;
    private Deque<TriggerRecord> mTriggerQueue;
    private Map<String, Integer> mActions;
    private String mNiaServerId;

    private static final Object NULL_OBJECT = new Object();

    static {
        System.loadLibrary("jni_mdm");
    }

    private List<SessionStateObserver> mSessionStateObservers = new LinkedList<SessionStateObserver>();
    private SessionInitiator mSessionInitiator;
    private NIAMsgHandler mNIAMsgHandler;
    private MdmSessionEnableMode mSessionEnableMode = MdmSessionEnableMode.allowAll;
    private DownloadRetryHandler mDownloadRetryHandler;
    private DownloadPromptHandler mDownloadPromptHandler;
    private int mConnectionTimeout = 60;
    private int mReadTimeout = 120;

    /**
     * MDM engine constructor. There can only be one instance of MdmEngine or the constructor will
     * raise a MdmException. Default logger will be used.
     *
     * @param context
     *        Android context.
     * @param mmiFactory
     *        MMI factory.
     * @param plFactory
     *        PL factory.
     * @throws MdmException
     */
    public MdmEngine(Context context, MmiFactory mmiFactory, PLFactory plFactory)
            throws MdmException {
        this(context, mmiFactory, plFactory, new MdmDefaultLogger());
    }

    /**
     * MDM engine constructor. There can only be one instance of MdmEngine or the constructor will
     * raise a MdmException.
     *
     * @param context
     *        Android context.
     * @param mmiFactory
     *        MMI factory.
     * @param plFactory
     *        PL factory.
     * @param logger
     *        PL logger.
     * @throws MdmException
     */
    public MdmEngine(Context context, MmiFactory mmiFactory, PLFactory plFactory, PLLogger logger)
            throws MdmException {
        synchronized (MdmEngine.class) {
            /* Only one instance is allowed */
            Assert.assertEquals(null, sInstance);
            mContext = context;
            mMmiFactory = mmiFactory;
            mPLFactory = plFactory;
            mLogger = logger;
            mPLStorage = plFactory.getStorage();
            mMmiObserver = new MmiObserverImpl();
            mTriggerQueue = new LinkedList<TriggerRecord>();
            mActions = new HashMap<String, Integer>();
            mDownloadRetryHandler = null;
            mDownloadPromptHandler = null;
            mNiaServerId = null;

            /*
             * The value of this object will be filled when MMI component are created.
             */
            mMmiViewContext = new MmiViewContext(null, 0, 0);

            if (0 != createN()) {
                throw new MdmException(MdmError.INTERNAL);
            }

            sInstance = this;
        }
    }

    /**
     * Get the singleton of engine.
     *
     * @return Engine instance.
     */
    public static MdmEngine getInstance() {
        return sInstance;
    }

    /* Interface to native constructor. */
    private native int createN();

    /**
     * Destroy the engine. After active engine instance is destroyed, user can <b>new</b> another
     * instance of engine.
     *
     * @throws MdmException
     */
    public void destroy() throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "MdmEngine destroy");
        MdmTree.destroyAll();
        destroyN();
        sInstance = null;
    }

    /* Interface to native destructor. */
    private native int destroyN();

    /**
     * Start the engine. This method must be called before any other MdmEngine method. If an
     * exception is thrown,then no other MdmEngine method should be called except for
     * {@link MdmEngine#destroy()}.
     *
     * @throws MdmException
     */
    public void start() throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "MdmEngine start");
        if (0 != startN()) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /* Interface to native start. */
    private native int startN();

    /**
     * Check whether there are active sessions and if the queue is empty. There can at most one
     * active session at any time.
     *
     * @return true if there are no active sessions and the queue is empty, false otherwise
     */
    public synchronized boolean isIdle() {
        return isIdleN() && mTriggerQueue.isEmpty();
    }

    public native boolean isIdleN();

    /*
     * Default logger implementation for MdmLog. If no logger is provided in constructor, this
     * default logger will be used. This default logger will output log to Android log.
     */
    private static class MdmDefaultLogger extends MdmLog implements PLLogger {

        public void logMsg(MdmLogLevel level, String message) {
            switch (level) {
                case DEBUG:
                    d(TAG, message);
                    break;
                case ERROR:
                    e(TAG, message);
                    break;
                case INFO:
                    i(TAG, message);
                    break;
                case VERBOSE:
                    v(TAG, message);
                    break;
                case WARNING:
                    w(TAG, message);
                    break;
                default:
                    // discard
                    break;
            }
        }
    }

    /**
     * Stop the engine. Should be called before MO's are destroyed.
     */
    public void stop() {
        if (0 != stopN()) {
            mLogger.logMsg(MdmLogLevel.ERROR, "MdmEngine stop failed");
        }
    }

    /* Interface to native stop. */
    private native int stopN();

    /**
     * Register as a session state observer. A session state observer is notified whenever a session
     * state changes. The SessionStateObserver.notify() method is called once on each change in the
     * session state. There can be multiple session state observers.
     *
     * @param observer
     *        A session state observer.
     */
    public void registerSessionStateObserver(SessionStateObserver observer) {
        mSessionStateObservers.add(observer);

    }

    /**
     * Unregister a session state observer. The observer is removed from the list of session state
     * observers.
     *
     * @param observer
     *        Session observer to unregister.
     */
    public void unregisterSessionStateObserver(SessionStateObserver observer) {
        mSessionStateObservers.remove(observer);
    }

    /**
     * Register a handler for download retry. The handler is notified when a non fatal error occurs
     * while downloading an object, just before the Engine attempts to restore the connection. The
     * handler may approve the retry attempt or return an error code which will cause the session to
     * be aborted, and the code will be passed as lastError to all session-state observers.
     *
     * @param handler
     *        Download Retry observer or null to unregister.
     */
    public void registerDownloadRetryHandler(DownloadRetryHandler handler) {
        mDownloadRetryHandler = handler;
    }

    public DownloadRetryHandler getDownloadRetryHandler() {
        return mDownloadRetryHandler;
    }

    /*
     * Notify the session state changes to observers.
     * @param type Session type.
     * @param state Session state.
     * @param lastError Reason.
     */
    private void notifySessionStateObservers(SessionType type, SessionState state, int lastError)
            throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +MdmEngine.notifySessionStateObservers()");
        for (SessionStateObserver observer : mSessionStateObservers) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "notify observer start");
            try {
                observer.notify(type, state, lastError, mSessionInitiator);
            } catch (Throwable t) {
                mLogger.logMsg(MdmLogLevel.ERROR, "Exception happened in observer, ignore it.");
                t.printStackTrace();
            }
            mLogger.logMsg(MdmLogLevel.DEBUG, "notify observer end");
        }
        if (state == SessionState.COMPLETE || state == SessionState.ABORTED) {
            // Clear states
            clearSessionActions();
            mNiaServerId = null;
            mLogger.logMsg(MdmLogLevel.DEBUG, "check for pending triggers");
            if (!mTriggerQueue.isEmpty()) {
                triggerSession(mTriggerQueue.pop());
            }
            mLogger.logMsg(MdmLogLevel.DEBUG, "check done");
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -MdmEngine.notifySessionStateObservers()");
    }

    public void removePendingTrigger(SessionInitiator initiator) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +removePendingTrigger()");
        TriggerRecord recordToRemove = null;
        for (TriggerRecord tr : mTriggerQueue) {
            mLogger.logMsg(MdmLogLevel.DEBUG,
                    "Check pending trigger " + tr.sessionInitiator.getId());
            if (tr.sessionInitiator == initiator) {
                recordToRemove = tr;
                break;
            }
        }
        mTriggerQueue.remove(recordToRemove);
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -removePendingTrigger()");
    }

    /**
     * Set the connection timeout for default connection implementation.
     *
     * @param seconds
     *        How many seconds to wait on a connection before giving up.
     */
    public void setConnectionTimeout(int seconds) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "setConnectionTimeout: " + seconds);
        mConnectionTimeout = seconds;
        if (0 != setConnectionTimeoutN(seconds)) {
            mLogger.logMsg(MdmLogLevel.ERROR, "setConnectionTimeout failed");
        }
    }

    int getConnectionTimeout() {
        return mConnectionTimeout;
    }

    /**
     * Set the read timeout for default connection implementation.
     *
     * @param seconds
     *        How many seconds to wait on a connection before giving up.
     */
    public void setReadTimeout(int seconds) {
        mLogger.logMsg(MdmLogLevel.DEBUG, "setReadTimeout: " + seconds);
        mReadTimeout = seconds;
    }

    int getReadTimeout() {
        return mReadTimeout;
    }

    /*
     * Interface to native setConnectionTimeout().
     * @param timeout How many seconds to wait on a connection before giving up.
     * @return
     */
    private native int setConnectionTimeoutN(int timeout);

    /**
     * Get Session-Enable mode. The setting for this mode determines which sessions sessions that
     * have been triggered may be started by the Engine.
     *
     * @return Session-Enable mode. @see MdmSessionEnableMode
     */
    public MdmSessionEnableMode getSessionEnableMode() {
        return mSessionEnableMode;
    }

    /**
     * Set Session-Enable mode. The setting for this mode determines which sessions sessions that
     * have been triggered may be started by the Engine. For example, you can allow only
     * server-initiated sessions. The initial value is MdmSessionEnableMode.allowAll.
     *
     * @param mode
     *        A MDMSessionEnableMode value.
     * @throws MdmException
     *         INVALID_CALL if vDirect Mobile is not initialized yet.
     */
    public void setSessionEnableMode(MdmSessionEnableMode mode) throws MdmException {
        mSessionEnableMode = mode;
    }

    /**
     * Notify that user confirmed the download and engine should start to download the media object.
     *
     * @throws MdmException
     */
    public void notifyDLSessionProceed() throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "notifyDLSessionProceed called");
        if (0 != notifyDLSessionProceedN()) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    private native int notifyDLSessionProceedN();

    /**
     * Cancel the active session. If there is no active session, nothing will happen.
     *
     * @throws MdmException
     */
    public void cancelSession() throws MdmException {
        if (0 != cancelSessionN()) {
            throw new MdmException(MdmError.INTERNAL);
        }

    }

    /* Interface to native cancel session. */
    private native int cancelSessionN();

    /*
     * Trigger a new session which is saved as a TriggerRecord previously.
     * @param tr TriggerRecord saved previously.
     * @throws MdmException
     */
    private void triggerSession(TriggerRecord tr) throws MdmException {
        MdmEngine.getLogger().logMsg(MdmLogLevel.DEBUG,
                "[ENGINE] Trigger queued session with type " + tr.triggerType);
        switch (tr.triggerType) {
            case NIA_DM:
                triggerNIADmSessionInternal((byte[]) tr.parameters.get("Message"),
                        tr.sessionInitiator, (NIAMsgHandler) tr.parameters.get("NIAMsgHandler"));
                break;
            case DM:
                triggerDMSessionInternal((String) tr.parameters.get("Account"),
                        (String) tr.parameters.get("GenericAlertType"),
                        (byte[]) tr.parameters.get("Message"), tr.sessionInitiator);
                break;
            case DL:
                Object uri = tr.parameters.get("Uri");
                triggerDLSessionInternal((uri == NULL_OBJECT ? null : ((String) uri)),
                        (DownloadPromptHandler) tr.parameters.get("PromptHandler"),
                        tr.sessionInitiator);
                break;
            case DM_GENERIC_ALERT:
                triggerGenericAlertSessionInternal((String) tr.parameters.get("Correlator"),
                        (String) tr.parameters.get("Format"), (String) tr.parameters.get("Type"),
                        (String) tr.parameters.get("Mark"), (String) tr.parameters.get("Source"),
                        (String) tr.parameters.get("Target"), (String) tr.parameters.get("Data"),
                        (String) tr.parameters.get("Account"), tr.sessionInitiator);
                break;
            default:
                throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Trigger a notification-initiated DM session. The DM client should call this method after
     * receiving a "General Notification Initiated Session Alert" (NIA). To receive notification
     * when a notification-initiated (DM) session state changes, register as a session observer. To
     * unregister as a session state observer, call unregisterSessionStateObserver. If the engine is
     * not idle, then this trigger will be queued and will be triggered when the engine is idle.
     *
     * @param message
     *        Notification message content.
     * @param initiator
     *        Session initiator that will be passed as a parameter upon session state notifications.
     * @param handler
     *        Handler of a parsed NIA message. The handler will be called to handled the parsed
     *        message before a connection with the server is established.
     * @throws MdmException
     */
    public synchronized void triggerNIADmSession(byte[] message, SessionInitiator initiator,
            NIAMsgHandler handler) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +triggerNIADmSession()");
        if (mSessionEnableMode != MdmSessionEnableMode.allowAll) {
            mLogger.logMsg(MdmLogLevel.WARNING,
                    "Server initiated session is not allowed according to configuration.");
            mLogger.logMsg(MdmLogLevel.DEBUG, "-triggerNIADmSession");
            return;
        }
        if (!isIdleN()) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "Engine is not idle, enqueue the trigger.");
            TriggerRecord tr = new TriggerRecord(TriggerRecord.TriggerType.NIA_DM, initiator);
            tr.put("NIAMsgHandler", handler);
            tr.put("Message", message);
            mTriggerQueue.push(tr);
        } else {
            triggerNIADmSessionInternal(message, initiator, handler);
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -triggerNIADmSession()");
    }

    private void triggerNIADmSessionInternal(byte[] message, SessionInitiator initiator,
            NIAMsgHandler handler) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "triggerNIADmSessionInternal called");
        mSessionInitiator = initiator;
        mNIAMsgHandler = handler;
        mNiaServerId = extractServerIdFromNia(message);
        clearSessionActions();
        if (0 != triggerNIADmSessionN(message, initiator, handler)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Interface to native triggerNIADmSession.
     *
     * @param message
     * @param initiator
     *        Not used in native layer.
     * @param handler
     *        Not used in native layer.
     * @return
     */
    private native int triggerNIADmSessionN(byte[] message, SessionInitiator initiator,
            NIAMsgHandler handler);

    /*
     * Notify NIA message handler. Native layer will invoke this method.
     * @param uiMode UI mode.
     * @param dmVersion DM protocol, which is always 0.
     * @param vendorSpecificData Vendor-specific data.
     * @throws MdmException
     */
    private void notifyNIAMsgHandler(UIMode uiMode, short dmVersion, byte[] vendorSpecificData)
            throws MdmException {
        mNIAMsgHandler.notify(uiMode, dmVersion, vendorSpecificData, mSessionInitiator);
    }

    /**
     * Trigger a DM session. To receive notification when a DM session state changes, register as a
     * session observer. To unregister as a session state observer, call
     * unregisterSessionStateObserver.
     *
     * @param account
     *        The URI of the DM Account node in the DM tree (null for default DM account).
     * @param genericAlertType
     *        Generic alert type as defined by OMA. For example:
     *        org.openmobilealliance.dm.firmwareupdate.devicerequest is the generic alert type used
     *        for device initiated firmware update.
     * @param message
     *        Vendor-specific message content. CURRENTLY NOT SUPPORTED and must be null.
     * @param initiator
     *        Session initiator that will be passed as a parameter upon session state notifications.
     * @throws MdmException
     *         with a MdmError error code
     */
    public synchronized void triggerDMSession(String account, String genericAlertType,
            byte[] message, SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +triggerDMSession()");
        if (!isIdleN()) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "Engine is not idle, enqueue the trigger.");
            TriggerRecord tr = new TriggerRecord(TriggerRecord.TriggerType.DM, initiator);
            tr.put("Account", account);
            tr.put("GenericAlertType", genericAlertType);
            if (message != null) {
                tr.put("Message", message);
            }
            mTriggerQueue.push(tr);
        } else {
            triggerDMSessionInternal(account, genericAlertType, message, initiator);
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -triggerDMSession()");
    }

    private void triggerDMSessionInternal(String account, String genericAlertType, byte[] message,
            SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "triggerDMSessionInternal called");
        mSessionInitiator = initiator;
        clearSessionActions();
        if (0 != triggerDMSessionN(account, genericAlertType, message)) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    /**
     * Interface for native triggerDMSession.
     *
     * @param account
     * @param genericAlertType
     * @param message
     * @param initiator
     *        Not used in native layer.
     * @return 0 for success, otherwise for error code.
     */
    private native int triggerDMSessionN(String account, String genericAlertType, byte[] message);

    /**
     * Trigger a DL session.
     *
     * @param uri
     *        Uri to download descriptor. If this parameter is null, then DL will resume the last
     *        download.
     * @param initiator
     *        Session initiator.
     * @throws MdmException
     */
    public synchronized void triggerDLSession(String uri, DownloadPromptHandler promptHandler,
            SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +triggerDLSession()");
        if (!isIdleN()) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "Engine is not idle, enqueue the trigger.");
            TriggerRecord tr = new TriggerRecord(TriggerRecord.TriggerType.DL, initiator);
            tr.put("Uri", (uri == null ? NULL_OBJECT : uri));
            tr.put("PromptHandler", promptHandler);
            mTriggerQueue.push(tr);
        } else {
            triggerDLSessionInternal(uri, promptHandler, initiator);
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -triggerDLSession()");
    }

    private void triggerDLSessionInternal(String uri, DownloadPromptHandler promptHandler,
            SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +triggerDLSessionInternal()");
        mSessionInitiator = initiator;
        mDownloadPromptHandler = promptHandler;
        clearSessionActions();
        if (0 != triggerDLSessionN(uri)) {
            throw new MdmException(MdmError.INTERNAL);
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -triggerDLSessionInternal()");
    }

    private native int triggerDLSessionN(String uri);

    /**
     * Notify that server notification has been handled. Called by the NIA message handler to notify
     * that server notification has been handled.
     *
     * @throws MdmException
     */
    public void notifyNIASessionProceed() throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +notifyNIASessionProceed()");
        if (0 != notifyNIASessionProceedN()) {
            throw new MdmException(MdmError.INTERNAL);
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -notifyNIASessionProceed()");
    }

    private native int notifyNIASessionProceedN();

    /**
     * Trigger a Report session (DM Session). To receive notification when a Report session state
     * changes, register a session observer. To unregister as a session state observer, call
     * unregisterSessionStateObserver.
     *
     * @param uriPath
     *        The URI of a node in the DM tree.
     * @param reasonCode
     *        Result code. Used when the alert is an asynchronous response to an Exec command.
     * @param account
     *        The URI of the DM Account node in the DM tree. (null for default DM account).
     * @param genericAlertType
     *        Generic alert type as defined by OMA. For example:
     *        org.openmobilealliance.dm.firmwareupdate.download is the generic alert type used in
     *        response to the completion of a Download operation.
     * @param correlator
     *        Correlator previously sent by server. Used when the alert is an asynchronous response
     *        to an Exec command.
     * @param initiator
     *        Session initiator that will be passed as a parameter upon session state notifications.
     * @throws MdmException
     */
    public synchronized void triggerReportSession(String uriPath, int reasonCode, String account,
            String genericAlertType, String correlator, SessionInitiator initiator)
            throws MdmException {
        triggerReportSession(correlator, "int", genericAlertType, null, uriPath, null,
                Integer.toString(reasonCode), account, initiator);
    }

    public synchronized void triggerReportSession(String correlator, String format, String type,
            String mark, String source, String target, String data, String account,
            SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +triggerReportSession()");
        if (correlator == null || source == null) {
            throw new MdmException(MdmError.BAD_INPUT);
        }
        triggerGenericAlertSession(correlator, format, type, mark, source, target, data, account,
                initiator);
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -triggerReportSession()");
    }

    /**
     * This is the most general trigger for CI generic alert session. It only do basic sanity check
     * of the parameters.
     *
     * @param correlator
     * @param format
     *        Cannot be null if data is not null. Only "int", "text/plain" and "xml" are supported.
     * @param type
     *        Cannot be null.
     * @param mark
     * @param source
     * @param target
     * @param data
     * @param account
     *        The URI of the DM Account node in the DM tree. (null for default DM account).
     * @param initiator
     *        Cannot be null.
     * @throws MdmException
     */
    public synchronized void triggerGenericAlertSession(String correlator, String format,
            String type, String mark, String source, String target, String data, String account,
            SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] +triggerGenericAlertSession()");
        if (!isIdleN()) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "Engine is not idle, enqueue the trigger.");
            TriggerRecord tr = new TriggerRecord(TriggerRecord.TriggerType.DM_GENERIC_ALERT,
                    initiator);
            tr.put("Correlator", correlator);
            tr.put("Format", format);
            tr.put("Type", type);
            tr.put("Mark", mark);
            tr.put("Source", source);
            tr.put("Target", target);
            tr.put("Data", data);
            tr.put("Account", account);
            mTriggerQueue.push(tr);
        } else {
            triggerGenericAlertSessionInternal(correlator, format, type, mark, source, target,
                    data, account, initiator);
        }
        mLogger.logMsg(MdmLogLevel.DEBUG, "[ENGINE] -triggerGenericAlertSession()");
    }

    private synchronized void triggerGenericAlertSessionInternal(String correlator, String format,
            String type, String mark, String source, String target, String data, String account,
            SessionInitiator initiator) throws MdmException {
        mLogger.logMsg(MdmLogLevel.DEBUG, "triggerGenericAlertSessionInternal called");
        mSessionInitiator = initiator;
        if (type == null) {
            throw new MdmException(MdmError.BAD_INPUT);
        }
        if (data != null
                && !(format != null && (format.equals("int") || format.equals("text/plain") || format
                        .equals("xml")))) {
            throw new MdmException(MdmError.BAD_INPUT);
        }

        clearSessionActions();
        if (0 != triggerGenericAlertSessionN(correlator, format, type, mark, source, target, data,
                (account == null ? getCurrentAccount() : account))) {
            throw new MdmException(MdmError.INTERNAL);
        }
    }

    private native int triggerGenericAlertSessionN(String correlator, String format, String type,
            String mark, String source, String target, String data, String account);

    /**
     * Get the current server id used.
     *
     * @return Server id.
     */
    public String getCurrentAccount() {
        if (mNiaServerId != null) {
            return mNiaServerId;
        } else {
            String result = null;
            String accountRootUri = null;
            try {
                MdmTree tree = new MdmTree();
                MdmConfig config = new MdmConfig();
                DmAccConfiguration accountConfig = config.getDmAccConfiguration();
                switch (accountConfig.activeAccountDmVersion) {
                    case DM_1_2:
                        accountRootUri = accountConfig.dm12root;
                        break;
                    case DM_1_1_2:
                        accountRootUri = "./SyncML/DMAcc";
                        break;
                    default:
                        return null;
                }
                String accountUri = tree.findFirstNodeByName(accountRootUri, "ServerId");
                getLogger().logMsg(MdmLogLevel.DEBUG, "ServerId uri is " + accountUri);
                return tree.getStringValue(accountUri);
            } catch (MdmException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    public SessionInitiator getSessionInitiator() {
        return mSessionInitiator;
    }

    /**
     * Return the logger used by engine. This method is used by MDM engine components.
     *
     * @return Current active logger.
     */
    public static PLLogger getLogger() {
        return sInstance.mLogger;
    }

    public synchronized void setSessionActions(String key, int actions) {
        mActions.put(key, actions);
    }

    public synchronized int getSessionActions(String key) {
        int result;
        if (mActions.containsKey(key)) {
            result = mActions.get(key);
        } else {
            result = 0;
        }
        return result;
    }

    public synchronized void clearSessionActions() {
        mActions.clear();
    }

    /**
     * Set action bits specified by actions.
     *
     * @param actions
     *        Action mask. Bits enabled in this mask will be set.
     */
    public synchronized void setSessionAction(String key, int actions) {
        int last;
        if (mActions.containsKey(key)) {
            last = mActions.get(key);
        } else {
            last = 0;
        }
        mActions.put(key, (last | actions));
    }

    /**
     * Clear action bits specified by actions.
     *
     * @param actions
     *        Action mask. Bits enabled in this mask will be cleared.
     */
    public synchronized void clearSessionAction(String key, int actions) {
        int last;
        if (mActions.containsKey(key)) {
            last = mActions.get(key);
        } else {
            last = 0;
        }
        mActions.put(key, (last & ~actions));
    }

    /*
     * MmiObserver implementation which notify native engine thread with MMI response from user. The
     * heavy-lifting work is done in native land.
     */
    private static class MmiObserverImpl implements MmiObserver {

        /* keep these values consistent with c code */
        public static final int OMC_MMI_UNDEFINED_SCREEN = 0;
        public static final int OMC_MMI_INITIAL_SCREEN = 1;
        public static final int OMC_MMI_AUTH_SCREEN = 2;
        public static final int OMC_MMI_AUTH_FAIL_SCREEN = 3;
        public static final int OMC_MMI_IN_SESSION_SCREEN = 4;
        public static final int OMC_MMI_SERVER_INFO_SCREEN = 5;
        public static final int OMC_MMI_CONTINUE_ABORT_SCREEN = 6;
        public static final int OMC_MMI_ENTER_DETAILS_SCREEN = 7;
        public static final int OMC_MMI_SINGLE_CHOICE_SCREEN = 8;
        public static final int OMC_MMI_MULTIPLE_CHOICE_SCREEN = 9;
        public static final int OMC_MMI_EXIT_FAIL_SCREEN = 10;
        public static final int OMC_MMI_EXIT_OK_SCREEN = 11;
        public static final int OMC_MMI_SYNC_FAIL_SCREEN = 12;

        public int mScreenType = OMC_MMI_UNDEFINED_SCREEN;

        public native void notifyChoicelistSelection(int bitflags);

        public native void notifyCancelEvent();

        public native void notifyConfirmationResult(boolean confirmed);

        public native void notifyInfoMsgClosed();

        public native void notifyInputResult(String userInput);

        public native void notifyTimeoutEvent();
    }

    private static class TriggerRecord {
        public static enum TriggerType {
            NIA_DM, DM, DM_GENERIC_ALERT, DL, DL_RESUME,
        }

        public final TriggerType triggerType;
        public final SessionInitiator sessionInitiator;
        public Properties parameters;

        public TriggerRecord(TriggerType type, SessionInitiator initiator) {
            triggerType = type;
            sessionInitiator = initiator;
            parameters = new Properties();
        }

        private void put(Object key, Object value) {
            if (value != null) {
                parameters.put(key, value);
            }
        }
    }

    private static String extractServerIdFromNia(byte[] nia) {
        final int sidLengthFieldOffset = 23;
        final int sidFieldOffset = 24;
        int sidLength = nia[sidLengthFieldOffset];
        return new String(nia, sidFieldOffset, sidLength, Charset.forName("UTF-8"));
    }

    public PLRegistry getPLRegistry() {
        return mPLFactory.getRegistry();
    }

    public PLDlPkg getPLDlPkg() {
        return mPLFactory.getDownloadPkg();
    }

    public PLStorage getPLStorage() {
        mLogger.logMsg(MdmLogLevel.DEBUG, "getPlStorage..");
        return mPLFactory.getStorage();
    }

    public PLHttpConnection getPLHttpConnection() {
        mLogger.logMsg(MdmLogLevel.DEBUG, "getPLHttpConnection..");
        PLHttpConnection connection = mPLFactory.getHttpConnection();
        if (connection == null) {
            mLogger.logMsg(MdmLogLevel.DEBUG, "Use default implementation..");
            connection = new SimpleHttpConnection(this);
        }
        return connection;
    }

    public DownloadPromptHandler getDownloadPromptHandler() {
        mLogger.logMsg(MdmLogLevel.DEBUG, "getDownloadPromptHandler..");
        return mDownloadPromptHandler;
    }
}
