package com.mediatek.rcs.pam;

import com.mediatek.rcs.pam.IPAServiceCallback;

interface IPAService {
	// ---- Callback Management ----
	
	/**
	 * Register callback handler to service.
	 * @return A token for this binding. We need this because official 
	 *         development guide says "However, the system calls your 
	 *         service's onBind() method to retrieve the IBinder only
	 *         when the first client binds. The system then delivers 
	 *         the same IBinder to any additional clients that bind, 
	 *         without calling onBind() again.".
	 */
	long registerCallback(in IPAServiceCallback callback);
	
	/**
	 * Internal sync primitive. Used only by PAService.
	 */
	void registerAck(in long token);
	
	/**
	 * Unregister callback handler.
	 */
	void unregisterCallback(in long token);

	// ---- Service State ----
	
	/**
	 * Check whether the service is connected.
	 */
	boolean isServiceConnected();
	
	/**
	 * Check whether the service is registered to server.
	 */
	boolean isServiceRegistered();
	
	// ---- Messaging ----

	/**
	 * Send text message to public account.
	 * @param accountId ID of the public account in provider
	 * @param message text message
	 * @return message ID in provider
	 */
	long sendMessage(in long token, in long accountId, in String message, in boolean system);
	
	/**
	 * Resend a failed message.
	 * @param token
	 * @param messageId the message ID to resend
	 */
	void resendMessage(in long token, in long messageId);
	
	long setFavourite(in long token, in long messageId, in int index);
	
	/**
	 * Send image to public account.
	 * @param accountId ID of the public account in provider
	 * @param path file path of the media
	 * @return message ID in provider
	 */
	long sendImage(in long token, in long accountId, in String path, in String thumbnailPath);
	
	/**
	 * Send audio to public account.
	 * @param accountId ID of the public account in provider
	 * @param path file path of the media
	 * @return message ID in provider
	 */
	long sendAudio(in long token, in long accountId, in String path, in int duration);
	
	/**
	 * Send video to public account.
	 * @param accountId ID of the public account in provider
	 * @param path file path of the media
	 * @return message ID in provider
	 */
	long sendVideo(in long token, in long accountId, in String path, in String thumbnailPath, in int duration);
	
	/**
	 * Send geometry location to public account.
	 * @param accountId ID of the public account in provider
	 * @param data XML string of geometry location data
	 * @return message ID in provider
	 */
	long sendGeoLoc(in long token, in long accountId, in String data);
	
	/**
	 * Send vCard to public account.
	 * @param accountId ID of the public account in provider
	 * @param data XML string of vCard data
	 * @return message ID in provider
	 */
	long sendVcard(in long token, in long accountId, in String data);
	
	/**
	 * Report spam message to the server. The result will be notified via
	 * IPAServiceCallback with the reported message ID.
	 * @param messageId the ID of the message in content provider to report.
	 */
	void complainSpamMessage(in long token, in long messageId);
	
	/**
	 * Delete a single message by message ID.
	 * @param messageId ID of the message ID
	 */
	boolean deleteMessage(in long token, in long messageId);
	
	/**
	 * Delete all the messages associated with account ID.
	 * @param accountId Account ID
	 */
	long deleteMessageByAccount(in long token, in long accountId);
	
	/**
	 * Query RcsFTService's max transfer size.
	 */
	long getMaxFileTransferSize();
	
	// ---- Management ----
	
	/**
	 * Subscribe.
	 *
	 * @param ids An array of public account UUIDs.
	 * @return An ID which uniquely identify this request.
	 *         The client can differentiate the request by this ID.
	 */
    long subscribe(in long token, in String id);
    
	/**
	 * Unsubscribe.
	 *
	 * @param ids An array of public account UUIDs.
	 * @return An ID which uniquely identify this request.
	 *         The client can differentiate the request by this ID.
	 */
    long unsubscribe(in long token, in String id);

	/**
	 * Get subscribed accounts. This method only inserts the results into 
	 * content provider. If client need to replace the accounts info, it
	 * has to clear existing data itself.
	 * According to CMCC specs, this method should only be invoked when the
	 * user logs in for the first time or user switches SIM cards. In either
	 * case, the client/service should clear the data, so the implementation
	 * of this method only need to insert the new data into provider.
	 *
	 * @param order Sorting order.
	 * @param pageSize The accounts count in one page.
	 * @param pageNumber The index of the requested page.
	 * @return An ID which uniquely identify this request.
	 *         The client can differentiate the request by this ID.
	 */
    long getSubscribedList(in long token, int order, int pageSize, int pageNumber);

    /* get recommends is not provided here as the activities should invoke it directly via UIPAMClient */

	/**
	 * Get the detail information of the account specified by uuid.
	 * @param uuid UUID of the account.
	 * @param timestamp the last time client updated this info, null means the latest
	 */
    long getDetails(in long token, in String uuid, in String timestamp);
    
	/**
	 * Get the menu information of the account specified by uuid.
	 * @param uuid UUID of the account.
	 * @param timestamp the last time client updated this info
	 */
    long getMenu(in long token, in String uuid, in String timestamp);
    
	/**
	 * Set the accept status of the account specified by uuid.
	 * @param uuid UUID of the account.
	 * @param acceptStatus desired accept status
	 */
    long setAcceptStatus(in long token, in String uuid, in int acceptStatus);
    
    /* get message history is not provided here the activities should invoke it directly via UIPAMClient */
    /* long getMessageHistory(String uuid, String timestamp, int order, int number); */
    
    /* complain is not provided here the activities should invoke it directly via UIPAMClient */
    /* long complain(String uuid, String reason); */
    
    /**
     * Download image/video/audio through HTTP.
     * @return request id
     */
    long downloadObject(in long token, in long requestId, in String url, in int type);
    
    /**
     * Cancel current downloading.
     */
    void cancelDownload(in long cancelId);
}