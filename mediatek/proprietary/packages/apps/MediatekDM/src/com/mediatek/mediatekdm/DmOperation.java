package com.mediatek.mediatekdm;

import android.util.Base64;
import android.util.Log;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

public class DmOperation extends Properties implements Comparable<DmOperation> {
    public static class KEY {
        public static final String TYPE = "Type";
        public static final String NIA = "NIA";
        public static final String RESULT = "Result";
        public static final String INITIATOR = "Initiator";
        public static final String FUMO_TAG = "FUMOTag";
        public static final String SCOMO_TAG = "SCOMOTag";
        public static final String LAWMO_TAG = "LAWMOTag";
        public static final String SCOMO_DP = "SCOMODP";
        public static final String CONNECTION = "Connection";
        public static final String ACTION_MASK = "ActionMask";
    }

    public static final class Type {
        public static final String TYPE_SI = "SI";
        public static final String TYPE_CI = "CI";
        public static final String TYPE_CI_FUMO = "CI_FUMO";
        public static final String TYPE_REPORT_SCOMO = "CI_REPORT_SCOMO";
        public static final String TYPE_REPORT_FUMO = "CI_REPORT_FUMO";
        public static final String TYPE_REPORT_LAWMO = "CI_REPORT_LAWMO";
        public static final String TYPE_DL = "DL";

        public static boolean isSIOperation(String type) {
            return type.startsWith("SI");
        }

        public static boolean isCIOperation(String type) {
            return type.startsWith("CI");
        }

        public static boolean isReportOperation(String type) {
            return type.startsWith("CI_REPORT");
        }

        public static boolean isDLOperation(String type) {
            return type.startsWith("DL");
        }
    }

    public static final class InteractionResponse {
        public enum InteractionType {
            NOTIFICATION, CONFIRMATION, CHOICE_LIST, INPUT_QUERY;
            /* Progress and InfoMsg are not listed here as they do not require responses. */
        }

        /* Invalid response. */
        public static final int INVALID = -1;
        /* User did not specify any response. Usually this is caused by timeout. */
        public static final int TIMEOUT = 0;
        /* User confirmed. */
        public static final int POSITIVE = 1;
        /* User cancelled. */
        public static final int NEGATIVE = 2;

        public final InteractionType type;
        /*
         * response is used for TAG_SCOMO_NOTIFICATION, CONFIRMATION and CHOICE_LIST. If type is
         * TAG_SCOMO_NOTIFICATION or CONFIRMATION, then it can be one of TIMEOUT, POSITIVE or
         * NEGATIVE. If type is CHOICE_LIST, then it is the bit flag of choices.
         */
        public final int response;
        /* input is used only for INPUT_QUERY */
        public final String input;

        public InteractionResponse(InteractionType interactionType, int userResponse,
                String userInput) {
            type = interactionType;
            response = userResponse;
            input = userInput;
        }
    }

    public static final class Connection {
        public static final String AUTO = "Auto";
        public static final String WIFI = "WiFi";
        public static final String MOBILE = "Mobile";
    }

    public static final int DEFAULT_TIMEOUT = 5 * 60 * 1000; // 5 minutes
    public static final int DEFAULT_MAX_RETRY = 3;
    private static final long serialVersionUID = -3639057624439736834L;

    /* mId is used as file name when operation is stored into persistent storage. */
    private long mId;
    /*
     * mInteractionRecords is used for mRetry after recovery from network errors. This state will
     * not be stored into persistent storage.
     */
    private LinkedList<InteractionResponse> mInteractionRecords;
    public int timeout;
    private int mRetry;
    private Iterator<InteractionResponse> mIterator;

    public DmOperation(long id, int timeoutValue, int retry) {
        mId = id;
        mInteractionRecords = new LinkedList<InteractionResponse>();
        timeout = timeoutValue;
        mRetry = retry;
        mIterator = mInteractionRecords.iterator();
        if (timeoutValue <= 0) {
            throw new Error("timeoutValue should be greater than 0");
        }
        if (retry <= 0) {
            throw new Error("retry should be greater than 0");
        }
    }

    /**
     * Create an operation instance with timeout equals to DEFAULT_TIMEOUT and max retry equals to
     * DEFAULT_MAX_RETRY. This method will invoke generateId() to feed itself a ID.
     *
     * @param id
     *        Caller should provide ID via this parameter.
     */
    public DmOperation(long id) {
        this(id, DEFAULT_TIMEOUT, DEFAULT_MAX_RETRY);
    }

    /**
     * Create an operation instance with timeout equals to DEFAULT_TIMEOUT and max retry equals to
     * DEFAULT_MAX_RETRY. This method will invoke generateId() to feed itself a ID.
     */
    public DmOperation() {
        this(generateId());
    }

    public void initSI(byte[] nia) {
        setProperty(KEY.TYPE, Type.TYPE_SI);
        setProperty(KEY.NIA, nia);
    }

    public void initCIFumo() {
        setProperty(KEY.TYPE, Type.TYPE_CI_FUMO);
    }

    public void initDLFumo() {
        setProperty(KEY.TYPE, Type.TYPE_DL);
        setProperty(KEY.FUMO_TAG, true);
    }

    public void initDLScomo() {
        setProperty(KEY.TYPE, Type.TYPE_DL);
        setProperty(KEY.SCOMO_TAG, true);
    }

    public void initDL() {
        setProperty(KEY.TYPE, Type.TYPE_DL);
    }

    public void initReportFumo(int result, Map<String, String> parameters) {
        setProperty(KEY.TYPE, Type.TYPE_REPORT_FUMO);
        setProperty(KEY.RESULT, result);
        if (parameters != null) {
            for (String k : parameters.keySet()) {
                String value = parameters.get(k);
                Log.d(DmOperationManager.TAG, "key: " + k + ", value: " + value);
                if (value == null) {
                    value = "";
                }
                Log.d(DmOperationManager.TAG, "Value: " + value);
                setProperty(k, value);
            }
        }
    }

    public void initReportScomo(int result, Map<String, String> parameters) {
        setProperty(KEY.TYPE, Type.TYPE_REPORT_SCOMO);
        setProperty(KEY.RESULT, result);
        if (parameters != null) {
            for (String k : parameters.keySet()) {
                String value = parameters.get(k);
                Log.d(DmOperationManager.TAG, "key: " + k + ", value: " + value);
                if (value == null) {
                    value = "";
                }
                Log.d(DmOperationManager.TAG, "Value: " + value);
                setProperty(k, value);
            }
        }
    }

    public void initReportLawmo(int result) {
        setProperty(KEY.TYPE, Type.TYPE_REPORT_LAWMO);
        setProperty(KEY.RESULT, result);
    }

    public int setProperty(String key, int value) {
        String old = (String) setProperty(key, String.valueOf(value));
        return old == null ? 0 : Integer.valueOf(old);
    }

    public byte[] setProperty(String key, byte[] value) {
        String old = (String) setProperty(key, encodeByteArray(value));
        return old == null ? null : decodeByteArray(old);
    }

    public boolean setProperty(String key, boolean value) {
        String old = (String) setProperty(key, String.valueOf(value));
        return old == null ? false : Boolean.valueOf(old);
    }

    public int getIntProperty(String key) {
        return Integer.valueOf(super.getProperty(key));
    }

    public int getIntProperty(String key, int defaultValue) {
        if (super.containsKey(key)) {
            return Integer.valueOf(super.getProperty(key));
        } else {
            return defaultValue;
        }
    }

    public byte[] getByteArrayProperty(String key) {
        return decodeByteArray(super.getProperty(key));
    }

    public boolean getBooleanProperty(String key) {
        return Boolean.valueOf(super.getProperty(key));
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        if (super.containsKey(key)) {
            return Boolean.valueOf(super.getProperty(key));
        } else {
            return defaultValue;
        }
    }

    private byte[] decodeByteArray(String string) {
        return Base64.decode(string, Base64.DEFAULT);
    }

    private String encodeByteArray(byte[] data) {
        return Base64.encodeToString(data, Base64.DEFAULT);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("DmOperation = {");
        sb.append("Id: ").append(getId()).append(", ");
        String type = getProperty(KEY.TYPE);
        sb.append("Type: ").append(type).append(", ");
        if (Type.isSIOperation(type)) {
            sb.append("NIA: ").append(getProperty(KEY.NIA)).append(", ");
        } else if (Type.isReportOperation(type)) {
            sb.append("Result: ").append(getProperty(KEY.RESULT)).append(", ");
        }
        if (!mInteractionRecords.isEmpty()) {
            sb.append(", Interactions: {");
            for (InteractionResponse ir : mInteractionRecords) {
                sb.append(ir.type.toString()).append(", ");
            }
            sb.append("}, ");
        }
        sb.append("Timestamp: ").append((new Date(getId())).toGMTString());
        sb.append("}");
        return sb.toString();
    }

    public static long generateId() {
        return System.currentTimeMillis();
    }

    @Override
    public int compareTo(DmOperation another) {
        if (getId() > another.getId()) {
            return 1;
        } else if (getId() < another.getId()) {
            return -1;
        } else {
            return 0;
        }
    }

    public long getId() {
        return mId;
    }

    public int getRetry() {
        return mRetry;
    }

    /**
     * Retry the operation. This method resets the UI interaction iterator and decreases the retry
     * counter.
     */
    public synchronized void retry() {
        mRetry -= 1;
        mIterator = mInteractionRecords.iterator();
    }

    /**
     * Recover the operation. This method resets the UI interaction iterator without decreasing the
     * retry counter.
     */
    public synchronized void recover() {
        mIterator = mInteractionRecords.iterator();
    }

    public synchronized InteractionResponse getNextUIResponse() {
        if (mIterator != null && mIterator.hasNext()) {
            return mIterator.next();
        } else {
            mIterator = null;
            return null;
        }
    }

    public synchronized void addUIResponse(InteractionResponse response) {
        mInteractionRecords.add(response);
    }

    @SuppressWarnings("unchecked")
    public synchronized LinkedList<InteractionResponse> dumpInteractionResponses() {
        return (LinkedList<InteractionResponse>) mInteractionRecords.clone();
    }
}
