package com.mediatek.mediatekdm.mdm.fumo;

public class MdmFumoUpdateResult {
    public static enum ResultCode {
        /** Successful - The Request has Succeeded. */
        SUCCESSFUL(200),
        /** Management Client error - based on User or Device behavior. */
        CLIENT_ERROR(400),
        /** User chose not to accept the operation when prompted. */
        USER_CANCELED(401), DOWNLOAD_CANCELED(401),
        /**
         * Corrupted firmware update package, did not store correctly. Detected, for example, by
         * mismatched CRCs between actual and expected.
         */
        FW_UP_CORRUPT(402),
        /**
         * Wrong Firmware Update Package delivered to device based on current device
         * characteristics.
         */
        PACKAGE_MISMATCH(403),
        /** Failure to positively validate digital signature of firmware update package. */
        SIGNATURE_FAILED(404),
        /** Firmware Update Package is Not Acceptable. */
        NOT_ACCEPTABLE(405),
        /**
         * Authentication was Required but Authentication Failure was encountered when downloading
         * Firmware Update Package.
         */
        AUTH_FAILED(406),
        /** Client has encountered a time-out when downloading Firmware Update Package. */
        REQUEST_TIMEOUT(407),
        /** The device does not support the requested operation. */
        NOT_IMPLEMENTED(408),
        /** Indicates failure not defined by any other error code. */
        UNDEFINED_ERROR(409),
        /** Firmware Update operation failed in device. */
        UPDATE_FAILED(410),
        /** The URL provided for alternate download is bad. */
        BAD_URL(411),
        /** The Alternate Download Server is Unavailable or Does not Respond. */
        DL_SERVER_UNAVAILABLE(412),
        /** Alternate Download Server Error Encountered. */
        DL_SERVER_ERROR(500),
        /**
         * The download fails due insufficient memory in the device to save the firmware update
         * package.
         */
        OUT_OF_MEMORY_FOR_DOWNLOAD(501),
        /** The update fails because there isn't sufficient memory to update the device. */
        OUT_OF_MEMORY_FOR_UPDATE(502),
        /** The download fails due to network/transport level errors. */
        NETWORK(503);

        public final int val;

        private ResultCode(int value) {
            val = value;
        }

        public static ResultCode buildFromInt(int code) {
            switch (code) {
                case 200:
                    return SUCCESSFUL;
                case 400:
                    return CLIENT_ERROR;
                case 401:
                    return USER_CANCELED;
                case 402:
                    return FW_UP_CORRUPT;
                case 403:
                    return PACKAGE_MISMATCH;
                case 404:
                    return SIGNATURE_FAILED;
                case 405:
                    return NOT_ACCEPTABLE;
                case 406:
                    return AUTH_FAILED;
                case 407:
                    return REQUEST_TIMEOUT;
                case 408:
                    return NOT_IMPLEMENTED;
                case 409:
                    return UNDEFINED_ERROR;
                case 410:
                    return UPDATE_FAILED;
                case 411:
                    return BAD_URL;
                case 412:
                    return DL_SERVER_UNAVAILABLE;
                case 500:
                    return DL_SERVER_ERROR;
                case 501:
                    return OUT_OF_MEMORY_FOR_DOWNLOAD;
                case 502:
                    return OUT_OF_MEMORY_FOR_UPDATE;
                case 503:
                    return NETWORK;
                default:
                    return null;
            }
        }
    }

    public ResultCode result;
    public boolean isSyncUpdate;

    public MdmFumoUpdateResult(ResultCode rc, boolean update) {
        this.result = rc;
        this.isSyncUpdate = update;
    }
}
