package com.mediatek.telephony;

import android.app.PendingIntent;
import android.os.Bundle;
import android.telephony.Rlog;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;

/**
 * Manages SMS operations such as sending data, text, and PDU SMS messages.
 */
public class SmsManagerEx {

    private static final String TAG = "SMSEx";

    private static final SmsManagerEx sInstance = new SmsManagerEx();

    /**
     * Send a text based SMS.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param text the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     *
     */
    public void sendTextMessage(
            String destinationAddress, String scAddress, String text,
            PendingIntent sentIntent, PendingIntent deliveryIntent,
            int slotId) {
        Rlog.d(TAG, "sendTextMessage, text=" + text + ", destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);

        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
            return;
        }

        SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendTextMessage(destinationAddress,
                scAddress, text, sentIntent, deliveryIntent);
    }

   /**
     * Divide a message text into several fragments, none bigger than
     * the maximum SMS message size.
     *
     * @param text the original message.  Must not be null.
     * @return an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     *
     */
    public ArrayList<String> divideMessage(String text) {
        return SmsManager.getDefault().divideMessage(text);
    }

    /**
     * Send a multi-part text based SMS.  The callee should have already
     * divided the message into correctly sized parts by calling
     * <code>divideMessage</code>.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *   the current default SMSC
     * @param parts an <code>ArrayList</code> of strings that, in order,
     *   comprise the original message
     * @param sentIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been sent.
     *   The result code will be <code>Activity.RESULT_OK</code> for success,
     *   or one of these errors:<br>
     *   <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *   <code>RESULT_ERROR_RADIO_OFF</code><br>
     *   <code>RESULT_ERROR_NULL_PDU</code><br>
     *   For <code>RESULT_ERROR_GENERIC_FAILURE</code> each sentIntent may include
     *   the extra "errorCode" containing a radio technology specific value,
     *   generally only useful for troubleshooting.<br>
     *   The per-application based SMS control checks sentIntent. If sentIntent
     *   is NULL the caller will be checked against all unknown applications,
     *   which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntents if not null, an <code>ArrayList</code> of
     *   <code>PendingIntent</code>s (one for each message part) that is
     *   broadcast when the corresponding message part has been delivered
     *   to the recipient.  The raw pdu of the status report is in the
     *   extended data ("pdu").
     * @param slotId SIM card the user would like to access
     *
     */
    public void sendMultipartTextMessage(
            String destinationAddress, String scAddress, ArrayList<String> parts,
            ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents,
            int slotId) {
        Rlog.d(TAG, "sendMultipartTextMessage, destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);

        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
            return;
        }

        SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendMultipartTextMessage(
                destinationAddress, scAddress, parts, sentIntents, deliveryIntents);
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is successfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applications,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     *
     */
    public void sendDataMessage(
            String destinationAddress, String scAddress, short destinationPort,
            byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent,
            int slotId) {
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);

        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
            return;
        }

        SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendDataMessage(destinationAddress,
                scAddress, destinationPort, data, sentIntent, deliveryIntent);
    }

    /**
     * Get the default instance of the SmsManagerEx.
     *
     * @return the default instance of the SmsManagerEx
     *
     */
    public static SmsManagerEx getDefault() {
        return sInstance;
    }

    private SmsManagerEx() {
    }

    /**
     * Send a data based SMS to a specific application port.
     *
     * @param destinationAddress the address to send the message to
     * @param scAddress is the service center address or null to use
     *  the current default SMSC
     * @param destinationPort the port to deliver the message to
     * @param originalPort the port to deliver the message from
     * @param data the body of the message to send
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     *  The result code will be <code>Activity.RESULT_OK</code> for success,
     *  or one of these errors:<br>
     *  <code>RESULT_ERROR_GENERIC_FAILURE</code><br>
     *  <code>RESULT_ERROR_RADIO_OFF</code><br>
     *  <code>RESULT_ERROR_NULL_PDU</code><br>
     *  For <code>RESULT_ERROR_GENERIC_FAILURE</code> the sentIntent may include
     *  the extra "errorCode" containing a radio technology specific value,
     *  generally only useful for troubleshooting.<br>
     *  The per-application based SMS control checks sentIntent. If sentIntent
     *  is NULL the caller will be checked against all unknown applicaitons,
     *  which cause smaller number of SMS to be sent in checking period.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param slotId SIM card the user would like to access
     *
     */
    public void sendDataMessage(
            String destinationAddress, String scAddress, short destinationPort,
            short originalPort, byte[] data, PendingIntent sentIntent,
            PendingIntent deliveryIntent, int slotId) {
        Rlog.d(TAG, "sendDataMessage, destinationAddress=" + destinationAddress);
        Rlog.d(TAG, "slotId=" + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);

        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
            return;
        }

        SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendDataMessage(destinationAddress,
                scAddress, destinationPort, originalPort, data, sentIntent, deliveryIntent);
    }

    /**
     * Send an SMS with specified encoding type.
     *
     * @param destAddr the address to send the message to
     * @param scAddr the SMSC to send the message through, or NULL for the
     *  default SMSC
     * @param text the body of the message to send
     * @param extraParams extra parameters, such as validity period, encoding type
     * @param sentIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is sucessfully sent, or failed.
     * @param deliveryIntent if not NULL this <code>PendingIntent</code> is
     *  broadcast when the message is delivered to the recipient.  The
     *  raw pdu of the status report is in the extended data ("pdu").
     * @param slotId the sim card that user wants to access
     *
     */
    public void sendTextMessageWithExtraParams(
            String destAddr, String scAddr, String text, Bundle extraParams,
            PendingIntent sentIntent, PendingIntent deliveryIntent,
            int slotId) {
        Rlog.d(TAG, "sendTextMessageWithExtraParams, text=" + text);
        Rlog.d(TAG, "slotId=" + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);

        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
            return;
        }

        SmsManager.getSmsManagerForSubscriptionId(subIds[0]).sendTextMessageWithExtraParams(
                destAddr, scAddr, text, extraParams, sentIntent, deliveryIntent);
    }

    /**
     * Sends a multi-part text based SMS with specified encoding type.
     *
     * @param destAddr Address to send the message to
     * @param scAddr Service center address or null to use the current
     *            default SMSC
     * @param parts <code>ArrayList</code> of strings that, in order,
     *            comprise the original message
     * @param extraParams Extra parameters, such as validity period, encoding
     *            type
     * @param sentIntents If not null, an <code>ArrayList</code> of
     *            <code>PendingIntent</code>s (one for each message part) that
     *            will be broadcasted when the corresponding message part has been
     *            sent.
     * @param deliveryIntents If not null, an <code>ArrayList</code> of
     *            <code>PendingIntent</code>s (one for each message part) that
     *            will be broadcast when the corresponding message part has been
     *            delivered to the recipient. The raw PDU of the status report
     *            is in the extended data ("pdu").
     * @param slotId Identifier for SIM card slot
     *
     */
    public void sendMultipartTextMessageWithExtraParams(String destAddr,
            String scAddr, ArrayList<String> parts, Bundle extraParams,
            ArrayList<PendingIntent> sentIntents,
            ArrayList<PendingIntent> deliveryIntents, int slotId) {
        Rlog.d(TAG, "sendMultipartTextMessageWithExtraParams");
        Rlog.d(TAG, "slotId=" + slotId);

        int[] subIds = SubscriptionManager.getSubId(slotId);

        if (subIds == null || subIds.length == 0) {
            Rlog.d(TAG, "no related sub ids");
            return;
        }

        SmsManager.getSmsManagerForSubscriptionId(subIds[0]).
                sendMultipartTextMessageWithExtraParams(
                destAddr, scAddr, parts, extraParams, sentIntents, deliveryIntents);
    }
}
