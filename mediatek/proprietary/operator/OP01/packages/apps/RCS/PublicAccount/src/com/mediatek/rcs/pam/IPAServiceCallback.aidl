package com.mediatek.rcs.pam;

interface IPAServiceCallback {
	// ---- Service State ----
	void onServiceConnected();
	void onServiceDisconnected(int reason);
	void onServiceRegistered();
	void onServiceUnregistered();
	
	// ---- Messaging ----
	void onNewMessage(in long accountId, in long messageId);

	void onReportMessageFailed(in long messageId);
	
	void onReportMessageDisplayed(in long messageId);
	
	void onReportMessageDelivered(in long messageId);
	
	void onComposingEvent(in long accountId, in boolean status);
	
	void onTransferProgress(in long messageId, in long currentSize, in long totalSize);
	
	void reportComplainSpamSuccess(in long messageId);
	
	void reportComplainSpamFailed(in long messageId, in int errorCode);
	
	void reportDeleteMessageResult(in long requestId, in int resultCode);

	void reportSetFavourite(in long requestId, in int resultCode);

	// ---- Management ----
	
	/**
	 * Report the result of subscribe request.
	 * @param requestId the request ID returned by IPAservice.subscribe
	 * @param resultCode result code returned in HTTP response header
	 * @param results the subscribe request result for each account
	 */
	void reportSubscribeResult(in long requestId, in int resultCode);
	
	/**
	 * Report the result of unsubscribe request.
	 * @param requestId the request ID returned by IPAservice.unsubscribe
	 * @param resultCode result code returned in HTTP response header
	 * @param results the unsubscribe request result for each account
	 */
	void reportUnsubscribeResult(in long requestId, in int resultCode);
	
	/**
	 * Report the result of get subscribed list request.
	 * @param requestId the request ID returned by IPAservice.getSubscribedList
	 * @param resultCode result code returned in HTTP response header
	 */
	void reportGetSubscribedResult(in long requestId, in int resultCode, in long[] accountIds);
	
	
	void reportGetDetailsResult(in long requestId, in int resultCode, long accountId);
    void reportGetMenuResult(in long requestId, in int resultCode);
    void reportSetAcceptStatusResult(in long requestId, in int resultCode);
    
    /**
     * Report download result to client.
	 * @param requestId the request ID returned by IPAservice.downloadObject
	 * @param resultCode result code
     * @param path the absolute path of the downloaded file.
     */
	void reportDownloadResult(in long requestId, in int resultCode, in String path, in long mediaId);
	
	
	void updateDownloadProgress(in long requestId, in int percentage);
	
	void onAccountChanged(in String newAccount);
}
