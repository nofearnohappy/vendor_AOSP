package com.mediatek.mediatekdm.mdm;

import com.mediatek.mediatekdm.mdm.MdmException.MdmError;

/**
 * Configuration to the Mediatek DM package.
 */
public class MdmConfig {
    /**
     * DM Account configuration.
     */
    public class DmAccConfiguration {
        public DmVersion activeAccountDmVersion;
        public String dm12root;
        public boolean isExclusive;
        public boolean updateInactiveDmAccount;

        public DmAccConfiguration() {
            activeAccountDmVersion = DmVersion.INVALID;
            dm12root = null;
            isExclusive = false;
            updateInactiveDmAccount = false;
        }
    }

    /**
     * Version of OMA DM specification supported.
     */
    public static enum DmVersion {
        INVALID(0), DM_1_1_2(1), DM_1_2(2);

        public final int val;

        private DmVersion(int value) {
            val = value;
        }

        public static DmVersion fromInt(int value) {
            for (DmVersion v : DmVersion.values()) {
                if (v.val == value) {
                    return v;
                }
            }
            return null;
        }
    }

    public static enum HttpAuthLevel {
        NONE(0), BASIC(1), MD5(2), HMAC(3);

        public final int val;

        private HttpAuthLevel(int value) {
            val = value;
        }

        public static HttpAuthLevel fromInt(int value) {
            for (HttpAuthLevel v : HttpAuthLevel.values()) {
                if (v.val == value) {
                    return v;
                }
            }
            return null;
        }
    }

    /** Notification Verification Modes */
    public static enum NotifVerificationMode {
        /** The verification result of the notification message is ignored. */
        DISABLED(0),
        /**
         * When the client receives a notification message and the verification of the notification
         * fails, the client tries to authenticate the notification again using the special nonce
         * value, 0x00000000.
         */
        RESET_NONCE(1),
        /**
         * When the client receives a notification message and the verification of the notification
         * fails, the client tries to authenticate the notification again using the special nonce
         * value, 0x00000000.
         */
        REVERIFY(2),
        /**
         * Upon authentication failure, the notification message is ignored and no session is
         * initiated.
         */
        STRICT(3);

        public final int val;

        private NotifVerificationMode(int value) {
            val = value;
        }

        public static NotifVerificationMode fromInt(int value) {
            for (NotifVerificationMode v : NotifVerificationMode.values()) {
                if (v.val == value) {
                    return v;
                }
            }
            return null;
        }
    }

    private boolean mInstallNotifySuccessOnly = false;
    private boolean mAbortIfClientCommandFailed = false;
    private boolean mB64EncodeBinDataOverWBXML = false;
    private boolean mDDVersionCheck = false;
    private boolean mStatusCode202NotSupportedByServer = false;
    private boolean mTndsEnabled = true;
    private MdmConfigAgent mAgent;

    public MdmConfig() {
        mAgent = MdmConfigAgent.getInstance();
    }

    public void setMaxMsgSize(int size) throws MdmException {
        mAgent.setMaxMsgSize(size);
    }

    public int getMaxMsgSize() {
        return mAgent.getMaxMsgSize();
    }

    public void setMaxObjSize(int size) throws MdmException {
        mAgent.setMaxObjSize(size);
    }

    public int getMaxObjSize() {
        return mAgent.getMaxObjSize();
    }

    public void setMaxNetRetries(int count) throws MdmException {
        mAgent.setMaxNetRetries(count);
    }

    public int getMaxNetRetries() {
        return mAgent.getMaxNetRetries();
    }

    public void setDmAccSingle(boolean isSingle) throws MdmException {
        mAgent.setDmAccSingle(isSingle);
    }

    public boolean getDmAccSingle() {
        return mAgent.getDmAccSingle();
    }

    /**
     * Setting this configuration to TRUE contradicts OMA DL standard behavior and is not
     * recommended.
     *
     * @param isSuccessOnly
     * @throws MdmException
     */
    public void setInstallNotifySuccessOnly(boolean isSuccessOnly) throws MdmException {
        mInstallNotifySuccessOnly = isSuccessOnly;
    }

    public boolean getInstallNotifySuccessOnly() {
        return mInstallNotifySuccessOnly;
    }

    public void setEncodeWBXMLMsg(boolean isWBXML) throws MdmException {
        mAgent.setWbxmlMsgEncoding(isWBXML);
    }

    public boolean getEncodeWBXMLMsg() {
        return mAgent.getWbxmlMsgEncoding();
    }

    public void setNotificationVerificationMode(NotifVerificationMode mode) throws MdmException {
        mAgent.setNotificationVerificationMode(mode.val);
    }

    public NotifVerificationMode getNotificationVerificationMode() {
        return NotifVerificationMode.fromInt(mAgent.getNotificationVerificationMode());
    }

    public void setDmAccConfiguration(DmAccConfiguration config) throws MdmException {
        mAgent.setActiveDmAccountVersion(config.activeAccountDmVersion.val);
        mAgent.setDm12Root(config.dm12root);
        mAgent.setIsDmAccExclusive(config.isExclusive);
        mAgent.setUpdateInactiveDMAccount(config.updateInactiveDmAccount);
    }

    public DmAccConfiguration getDmAccConfiguration() throws MdmException {
        DmAccConfiguration config = new DmAccConfiguration();
        config.dm12root = mAgent.getDm12Root();
        config.isExclusive = mAgent.getIsDmAccExclusive();
        config.updateInactiveDmAccount = mAgent.getUpdateInactiveDMAccount();
        config.activeAccountDmVersion = DmVersion.fromInt(mAgent.getActiveDmAccountVersion());
        if (config.activeAccountDmVersion == null) {
            throw new MdmException(MdmError.INTERNAL);
        }
        return config;
    }

    public void setDlUserAgentName(String userAgent) throws MdmException {
        mAgent.setDlUserAgentName(userAgent);
    }

    public String getDlUserAgentName() {
        return mAgent.getDlUserAgentName();
    }

    public void setDlProxy(String proxy) throws MdmException {
        mAgent.setDlProxy(proxy);
    }

    public String getDlProxy() {
        return mAgent.getDlProxy();
    }

    public void setDmUserAgentName(String userAgent) throws MdmException {
        mAgent.setDmUserAgentName(userAgent);
    }

    public String getDmUserAgentName() {
        return mAgent.getDmUserAgentName();
    }

    public void setDmProxy(String proxy) throws MdmException {
        mAgent.setDmProxy(proxy);
    }

    public String getDmProxy() {
        return mAgent.getDmProxy();
    }

    /**
     * <b>NOTE:</b> Setting this configuration to TRUE contradicts OMA DM standard behavior and can
     * break the client-server authentication negotiation when the authentication type is other than
     * "none".
     *
     * @param abort
     * @throws MdmException
     */
    public void setAbortIfClientCommandFailed(boolean abort) throws MdmException {
        mAbortIfClientCommandFailed = abort;
    }

    public boolean getAbortIfClientCommandFailed() {
        return mAbortIfClientCommandFailed;
    }

    public void setIsClientNoncePerMessage(boolean isPerMessage) throws MdmException {
        mAgent.setClientNoncePerMessage(isPerMessage);
    }

    public boolean getIsClientNoncePerMessage() {
        return mAgent.getClientNoncePerMessage();
    }

    public void setIsServerNoncePerMessage(boolean isPerMessage) throws MdmException {
        mAgent.setServerNoncePerMessage(isPerMessage);
    }

    public boolean getIsServerNoncePerMessage() {
        return mAgent.getServerNoncePerMessage();
    }

    public void setB64EncodeBinDataOverWBXML(boolean isB64) throws MdmException {
        mB64EncodeBinDataOverWBXML = isB64;
    }

    public boolean getB64EncodeBinDataOverWBXML() {
        return mB64EncodeBinDataOverWBXML;
    }

    public void setSessionIDAsDec(boolean decimal) throws MdmException {
        mAgent.setSessionIDAsDec(decimal);
    }

    public boolean getSessionIDAsDec() {
        return mAgent.getSessionIDAsDec();
    }

    /**
     * Set whether to verify the version and the type of the parsed Download Descriptor. The initial
     * value is FALSE, indicating no verification. Call after MDMEngine has been created and before
     * MDMEngine.start(). <b>Note:</b> Setting this configuration to TRUE contradicts OMA DM
     * standard behavior and is not recommended. Verification should be used only when working with
     * servers producing invalid Download Descriptors.
     *
     * @param check
     * @throws MdmException
     */
    public void setDDVersionCheck(boolean check) throws MdmException {
        mDDVersionCheck = check;
    }

    public boolean getDDVersionCheck() {
        return mDDVersionCheck;
    }

    /**
     * <b>NOTE:</b> Setting this configuration to TRUE contradicts OMA DM standard behavior and is
     * not recommended.
     *
     * @param notSupported
     * @throws MdmException
     */
    public void set202statusCodeNotSupportedByServer(boolean notSupported) throws MdmException {
        mStatusCode202NotSupportedByServer = notSupported;
    }

    public boolean get202statusCodeNotSupportedByServer() {
        return mStatusCode202NotSupportedByServer;
    }

    /**
     * Set whether TNDS support is enabled or not. The initial value is true. Call after MdmEngine
     * has been created and before MdmEngine.start().
     *
     * @param isEnabled
     *        true if TNDS should be enabled.
     * @throws MdmException
     *         OUT_OF_SYNC if the function is called after MdmEngine.start().
     */
    public void setTndsEnabled(boolean isEnabled) throws MdmException {
        mTndsEnabled = isEnabled;
    }

    /**
     * Get whether TNDS is enabled.
     *
     * @return true if TNDS is enabled and false otherwise.
     */
    public boolean getTndsEnabled() {
        return mTndsEnabled;
    }

    public void setDmTreeRootElement(String rootElement) {
        mAgent.setDmTreeRootElement(rootElement);
    }

    public String getDmTreeRootElement() {
        return mAgent.getDmTreeRootElement();
    }

    public boolean getResendCommandInAuth() {
        return mAgent.getResendCommandInAuth();
    }

    public void setResendCommandInAuth(boolean resend) {
        mAgent.setResendCommandInAuth(resend);
    }

    public boolean getUseSequentialNonce() {
        return mAgent.getUseSequentialNonce();
    }

    public void setUseSequentialNonce(boolean sequential) {
        mAgent.setUseSequentialNonce(sequential);
    }

    private static class MdmConfigAgent {
        private static MdmConfigAgent sInstance;

        static {
            System.loadLibrary("jni_mdm");
        }

        private MdmConfigAgent() {
        }

        public static synchronized MdmConfigAgent getInstance() {
            if (sInstance == null) {
                sInstance = new MdmConfigAgent();
            }
            return sInstance;
        }

        public native void setWbxmlMsgEncoding(boolean isWBXML);

        public native boolean getWbxmlMsgEncoding();

        public native void setDefaultClientAuthType(HttpAuthLevel type);

        public native HttpAuthLevel getDefaultClientAuthType();

        public native void setMinServerAuthType(HttpAuthLevel type);

        public native HttpAuthLevel getMinServerAuthType();

        public native void setMaxServerAuthType(HttpAuthLevel type);

        public native HttpAuthLevel getMaxServerAuthType();

        public native void setClientNoncePerMessage(boolean isPerMessage);

        public native boolean getClientNoncePerMessage();

        public native void setServerNoncePerMessage(boolean isPerMessage);

        public native boolean getServerNoncePerMessage();

        public native void setMaxMsgSize(int size);

        public native int getMaxMsgSize();

        public native void setMaxObjSize(int size);

        public native int getMaxObjSize();

        public native void setEnsurePackage1Sent(boolean ensurePkg1Sent);

        public native boolean getEnsurePackage1Sent();

        public native void setDmAccSingle(boolean isSingle) throws MdmException;

        public native boolean getDmAccSingle();

        public native void setDmProxy(String proxy);

        public native String getDmProxy();

        public native void setDmTreeRootElement(String rootElement);

        public native String getDmTreeRootElement();

        public native void setMaxNetRetries(int count) throws MdmException;

        public native int getMaxNetRetries();

        public native void setSessionIDAsDec(boolean decimal) throws MdmException;

        public native boolean getSessionIDAsDec();

        public native String getDlProxy();

        public native void setDlProxy(String proxy);

        public native String getDlUserAgentName();

        public native void setDlUserAgentName(String userAgent);

        public native int getNotificationVerificationMode();

        public native void setNotificationVerificationMode(int val);

        public native String getDmUserAgentName();

        public native void setDmUserAgentName(String userAgent);

        public native boolean getUpdateInactiveDMAccount();

        public native void setUpdateInactiveDMAccount(boolean updateInactiveDmAccount);

        public native boolean getIsDmAccExclusive();

        public native void setIsDmAccExclusive(boolean isExclusive);

        public native String getDm12Root();

        public native void setDm12Root(String dm12root);

        public native int getActiveDmAccountVersion();

        public native void setActiveDmAccountVersion(int i);

        public native boolean getResendCommandInAuth();

        public native void setResendCommandInAuth(boolean resend);

        public native boolean getUseSequentialNonce();

        public native void setUseSequentialNonce(boolean sequential);
    }
}
