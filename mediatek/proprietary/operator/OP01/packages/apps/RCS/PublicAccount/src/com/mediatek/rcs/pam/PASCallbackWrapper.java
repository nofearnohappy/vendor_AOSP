package com.mediatek.rcs.pam;

import android.os.RemoteException;

public final class PASCallbackWrapper extends IPAServiceCallback.Stub {
    private final IPAServiceCallback mCallback;
    private final long mToken;
    private boolean mActive;

    public PASCallbackWrapper(IPAServiceCallback callback, long token) {
        super();
        mCallback = callback;
        mToken = token;
        mActive = false;
    }

    public void setActive() {
        mActive = true;
    }

    public long getToken() {
        return mToken;
    }

    @Override
    public void onServiceConnected() throws RemoteException {
        if (mActive) {
            mCallback.onServiceConnected();
        }
    }

    @Override
    public void onServiceDisconnected(int reason) throws RemoteException {
        if (mActive) {
            mCallback.onServiceDisconnected(reason);
        }
    }

    @Override
    public void onServiceRegistered() throws RemoteException {
        if (mActive) {
            mCallback.onServiceRegistered();
        }
    }

    @Override
    public void onServiceUnregistered() throws RemoteException {
        if (mActive) {
            mCallback.onServiceUnregistered();
        }
    }

    @Override
    public void onNewMessage(long accountId, long messageId) throws RemoteException {
        if (mActive) {
            mCallback.onNewMessage(accountId, messageId);
        }
    }

    @Override
    public void onReportMessageFailed(final long messageId) throws RemoteException {
        if (mActive) {
            mCallback.onReportMessageFailed(messageId);
        }
    }

    @Override
    public void onReportMessageDisplayed(final long messageId) throws RemoteException {
        if (mActive) {
            mCallback.onReportMessageDisplayed(messageId);
        }
    }

    @Override
    public void onReportMessageDelivered(final long messageId) throws RemoteException {
        if (mActive) {
            mCallback.onReportMessageDelivered(messageId);
        }
    }

    @Override
    public void onComposingEvent(long accountId, boolean status) throws RemoteException {
        if (mActive) {
            mCallback.onComposingEvent(accountId, status);
        }
    }

    @Override
    public void onTransferProgress(
            final long messageId, final long currentSize, final long totalSize)
            throws RemoteException {
        if (mActive) {
            mCallback.onTransferProgress(messageId, currentSize, totalSize);
        }
    }

    @Override
    public void reportSubscribeResult(
            final long requestId, final int resultCode) throws RemoteException {
        if (mActive) {
            mCallback.reportSubscribeResult(requestId, resultCode);
        }
    }

    @Override
    public void reportUnsubscribeResult(
            final long requestId, final int resultCode) throws RemoteException {
        if (mActive) {
            mCallback.reportUnsubscribeResult(requestId, resultCode);
        }
    }

    @Override
    public void reportGetSubscribedResult(
            final long requestId, final int resultCode, final long[] accountIds)
            throws RemoteException {
        if (mActive) {
            mCallback.reportGetSubscribedResult(requestId, resultCode, accountIds);
        }
    }

    @Override
    public void reportGetDetailsResult(
            final long requestId, final int resultCode, final long accountId)
            throws RemoteException {
        if (mActive) {
            mCallback.reportGetDetailsResult(requestId, resultCode, accountId);
        }
    }

    @Override
    public void reportGetMenuResult(
            final long requestId, final int resultCode) throws RemoteException {
        if (mActive) {
            mCallback.reportGetMenuResult(requestId, resultCode);
        }
    }

    @Override
    public void reportDownloadResult(
            final long requestId, final int resultCode, final String path, final long mediaId)
            throws RemoteException {
        if (mActive) {
            mCallback.reportDownloadResult(requestId, resultCode, path, mediaId);
        }
    }

    @Override
    public void updateDownloadProgress(
            final long requestId, final int percentage) throws RemoteException {
        if (mActive) {
            mCallback.updateDownloadProgress(requestId, percentage);
        }
    }

    @Override
    public void reportSetAcceptStatusResult(
            final long requestId, final int resultCode) throws RemoteException {
        if (mActive) {
            mCallback.reportSetAcceptStatusResult(requestId, resultCode);
        }
    }

    @Override
    public void reportComplainSpamSuccess(final long messageId) throws RemoteException {
        if (mActive) {
            mCallback.reportComplainSpamSuccess(messageId);
        }
    }

    @Override
    public void reportComplainSpamFailed(
            final long messageId, final int errorCode) throws RemoteException {
        if (mActive) {
            mCallback.reportComplainSpamFailed(messageId, errorCode);
        }
    }

    @Override
    public void onAccountChanged(final String newAccount) throws RemoteException {
        if (mActive) {
            mCallback.onAccountChanged(newAccount);
        }
    }

    @Override
    public void reportDeleteMessageResult(
            final long requestId, final int resultCode) throws RemoteException {
        if (mActive) {
            mCallback.reportDeleteMessageResult(requestId, resultCode);
        }
    }

    @Override
    public void reportSetFavourite(long requestId, int resultCode) throws RemoteException {
        if (mActive) {
            mCallback.reportSetFavourite(requestId, resultCode);
        }
    }

}
