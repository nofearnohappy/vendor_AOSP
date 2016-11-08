package com.mediatek.rcs.pam;

import android.os.RemoteException;

/**
 * Default implementation of IPAServiceCallback.Stub which do nothing.
 * This class is used to simplify the implementation of callbacks.
 */
public abstract class SimpleServiceCallback extends IPAServiceCallback.Stub {

    @Override
    public void onServiceRegistered() throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onServiceUnregistered() throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onNewMessage(long accountId, long messageId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReportMessageFailed(long messageId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReportMessageDisplayed(long messageId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onReportMessageDelivered(long messageId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onComposingEvent(long accountId, boolean status) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTransferProgress(
            long messageId, long currentSize, long totalSize) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportSubscribeResult(long requestId, int resultCode) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportUnsubscribeResult(long requestId, int resultCode) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportGetSubscribedResult(
            long requestId, int resultCode, long[] accountIds) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportGetDetailsResult(
            long requestId, int resultCode, long accountId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportGetMenuResult(long requestId, int resultCode) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportDownloadResult(
            long requestId, int resultCode, String path, long mediaId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateDownloadProgress(long requestId, int percentage) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportSetAcceptStatusResult(long requestId, int resultCode) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportComplainSpamSuccess(long messageId) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportComplainSpamFailed(long messageId, int errorCode) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAccountChanged(String newAccount) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportDeleteMessageResult(long requestId, int resultCode) throws RemoteException {
        // TODO Auto-generated method stub

    }

    @Override
    public void reportSetFavourite(long requestId, int resultCode) throws RemoteException {

    }
}
