package com.mediatek.mediatekdm.mdm.lawmo;

import com.mediatek.mediatekdm.mdm.MdmEngine;
import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.MdmLogLevel;
import com.mediatek.mediatekdm.mdm.MdmTree;
import com.mediatek.mediatekdm.mdm.NodeExecuteHandler;
import com.mediatek.mediatekdm.mdm.PLLogger;
import com.mediatek.mediatekdm.mdm.PLRegistry;
import com.mediatek.mediatekdm.mdm.SimpleSessionInitiator;

public class MdmLawmo {

    static final class AlertType {
        public static final String OPERATION_COMPLETE = "urn:oma:at:lawmo:1.0:OperationComplete";
    }

    static final class Registry {
        public static final String EXEC_CORRELATOR = "mdm|lawmo|exec|correlator";
        public static final String EXEC_URI = "mdm|lawmo|exec|uri";
        public static final String EXEC_ACCOUNT = "mdm|lawmo|account";
    }

    static final class Uri {
        public static final String STATE = "State";
        public static final String AVAILABLEWIPELIST = "AvailableWipeList";
        public static final String LAWMOCONFIG = "LAWMOConfig";
        public static final String OPERATIONS = "Operations";
        public static final String NOTIFYUSER = "NotifyUser";
        public static final String FULLYLOCK = "FullyLock";
        public static final String PARTIALLYLOCK = "PartiallyLock";
        public static final String UNLOCK = "UnLock";
        public static final String FACTORYRESET = "FactoryReset";
        public static final String WIPE = "Wipe";
    }

    static final String ROOT_URI_TYPE = "urn:oma:mo:oma-lawmo:1.0";

    public static final String SESSION_INITIATOR_PREFIX = "MDM_LAWMO";
    public static final String SESSION_INITIATOR_REPORT = SESSION_INITIATOR_PREFIX + "|REPORT";
    public static final String SESSION_ACTION_KEY = "LAWMO";

    private static final String LOG_TAG = "[LAWMO] ";

    private final String mRootUri;
    private LawmoHandler mHandler;
    private MdmTree mTree;
    private PLLogger mLogger;
    private MdmEngine mEngine;
    private PLRegistry mPLRegistry;

    public MdmLawmo(String lawmoRootURI, LawmoHandler handler) {
        MdmEngine.getLogger().logMsg(MdmLogLevel.INFO, "the lawmo root uri is : " + lawmoRootURI);

        if (lawmoRootURI.charAt(lawmoRootURI.length() - 1) == '/') {
            mRootUri = lawmoRootURI.substring(0, lawmoRootURI.length() - 1);
        } else {
            mRootUri = lawmoRootURI;
        }
        mHandler = handler;
        mTree = new MdmTree();
        mEngine = MdmEngine.getInstance();
        mLogger = MdmEngine.getLogger();
        mPLRegistry = mEngine.getPLRegistry();
        try {
            registerLawmoHandler();
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    public void destroy() {
        logMsg(MdmLogLevel.INFO, "destroy");
        try {
            unregisterLawmoHandler();
        } catch (MdmException e) {
            throw new Error(e);
        }
    }

    public boolean getNotifyUser() throws MdmException {
        return mTree.getBoolValue(MdmTree.makeUri(mRootUri, Uri.LAWMOCONFIG, Uri.NOTIFYUSER));
    }

    public LawmoState getState() throws MdmException {
        int value = mTree.getIntValue(MdmTree.makeUri(mRootUri, Uri.STATE));
        return LawmoState.fromInt(value);
    }

    public void triggerReportSession(LawmoResultCode resultCode) throws MdmException {
        logMsg(MdmLogLevel.INFO, "triggerReportSession..");

        MdmEngine engine = MdmEngine.getInstance();
        String uri = engine.getPLRegistry().getStringValue(Registry.EXEC_URI);
        String account = engine.getPLRegistry().getStringValue(Registry.EXEC_ACCOUNT);
        String correlator = engine.getPLRegistry().getStringValue(Registry.EXEC_CORRELATOR);
        engine.triggerReportSession(uri, resultCode.code, account, AlertType.OPERATION_COMPLETE,
                correlator, new SimpleSessionInitiator(SESSION_INITIATOR_REPORT));
        clearExecInfo();
    }

    public void deleteFromAvailableWipeList(String listItemName) throws MdmException {
        logMsg(MdmLogLevel.INFO, "deleteFromAvailableWipeList..list item name: " + listItemName);
    }

    public void setAvailableWipeList(String listItemName, boolean isToBeWiped) throws MdmException {
        logMsg(MdmLogLevel.INFO, "setAvailableWipeList..list item name:" + listItemName
                + ", wiped: " + isToBeWiped);
    }

    public int querySessionActions() {
        return mEngine.getSessionActions(SESSION_ACTION_KEY) & LawmoAction.ALL;
    }

    private String getPathOperations() {
        return MdmTree.makeUri(mRootUri, Uri.OPERATIONS);
    }

    private void registerLawmoHandler() throws MdmException {
        mTree.registerExecute(MdmTree.makeUri(getPathOperations(), Uri.FACTORYRESET),
                new NodeExecuteHandler() {
                    public int execute(byte[] data, String correlator) throws MdmException {
                        mEngine.setSessionAction(SESSION_ACTION_KEY,
                                LawmoAction.FACTORY_RESET_EXECUTED);
                        saveExecInfo(correlator, Uri.FACTORYRESET);
                        return valueOfResult(mHandler.executeFactoryReset());
                    }
                });

        mTree.registerExecute(MdmTree.makeUri(getPathOperations(), Uri.FULLYLOCK),
                new NodeExecuteHandler() {
                    public int execute(byte[] data, String correlator) throws MdmException {
                        mEngine.setSessionAction(SESSION_ACTION_KEY,
                                LawmoAction.FULLY_LOCK_EXECUTED);
                        saveExecInfo(correlator, Uri.FULLYLOCK);
                        return valueOfResult(mHandler.executeFullyLock());
                    }
                });

        mTree.registerExecute(MdmTree.makeUri(getPathOperations(), Uri.PARTIALLYLOCK),
                new NodeExecuteHandler() {
                    public int execute(byte[] data, String correlator) throws MdmException {
                        mEngine.setSessionAction(SESSION_ACTION_KEY,
                                LawmoAction.PARTIALLY_LOCK_EXECUTED);
                        saveExecInfo(correlator, Uri.PARTIALLYLOCK);
                        return valueOfResult(mHandler.executePartiallyLock());
                    }
                });

        mTree.registerExecute(MdmTree.makeUri(getPathOperations(), Uri.UNLOCK),
                new NodeExecuteHandler() {
                    public int execute(byte[] data, String correlator) throws MdmException {
                        mEngine.setSessionAction(SESSION_ACTION_KEY, LawmoAction.UNLOCK_EXECUTED);
                        saveExecInfo(correlator, Uri.UNLOCK);
                        return valueOfResult(mHandler.executeUnLock());
                    }
                });

        mTree.registerExecute(MdmTree.makeUri(getPathOperations(), Uri.WIPE),
                new NodeExecuteHandler() {
                    public int execute(byte[] data, String correlator) throws MdmException {
                        // TODO: split to wipe array, according to certain format
                        String[] dataToWipe = { new String(data) };
                        mEngine.setSessionAction(SESSION_ACTION_KEY, LawmoAction.WIPE_EXECUTED);
                        saveExecInfo(correlator, Uri.WIPE);
                        return valueOfResult(mHandler.executeWipe(dataToWipe));
                    }
                });

    }

    private void unregisterLawmoHandler() throws MdmException {
        mTree.unregisterExecute(MdmTree.makeUri(getPathOperations(), Uri.FACTORYRESET));
        mTree.unregisterExecute(MdmTree.makeUri(getPathOperations(), Uri.FULLYLOCK));
        mTree.unregisterExecute(MdmTree.makeUri(getPathOperations(), Uri.PARTIALLYLOCK));
        mTree.unregisterExecute(MdmTree.makeUri(getPathOperations(), Uri.UNLOCK));
        mTree.unregisterExecute(MdmTree.makeUri(getPathOperations(), Uri.WIPE));
    }

    private void saveExecInfo(String correlator, String leafnode) throws MdmException {
        // Don't save account, use default
        mPLRegistry.setStringValue(Registry.EXEC_CORRELATOR, correlator);
        mPLRegistry.setStringValue(Registry.EXEC_URI,
                MdmTree.makeUri(getPathOperations(), leafnode));
    }

    private void clearExecInfo() throws MdmException {
        mPLRegistry.deleteKeysByPrefix(Registry.EXEC_CORRELATOR);
        mPLRegistry.deleteKeysByPrefix(Registry.EXEC_URI);
    }

    private int valueOfResult(LawmoOperationResult result) throws MdmException {
        if (result.mAsync) {
            logMsg(MdmLogLevel.INFO, "Report asynchronously, return 0 here");
            return 0;
        } else {
            // not Async, clear exec info
            clearExecInfo();
            logMsg(MdmLogLevel.INFO, "execute result is " + result.mResultCode.code);
            return result.mResultCode.code;
        }
    }

    private void logMsg(MdmLogLevel level, String message) {
        mLogger.logMsg(level, LOG_TAG + message);
    }
}
