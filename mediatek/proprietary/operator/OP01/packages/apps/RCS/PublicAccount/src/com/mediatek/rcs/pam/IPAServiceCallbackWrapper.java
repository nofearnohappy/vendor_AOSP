package com.mediatek.rcs.pam;

import android.content.Context;
import android.os.Handler;
import android.os.RemoteException;

public final class IPAServiceCallbackWrapper extends IPAServiceCallback.Stub {
    private final IPAServiceCallback mCallback;
    private final Handler mHandler;

    public IPAServiceCallbackWrapper(IPAServiceCallback callback, Context context) {
        super();
        mCallback = callback;
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void onServiceConnected() throws RemoteException {
        mCallback.onServiceConnected();
    }

    @Override
    public void onServiceDisconnected(int reason) throws RemoteException {
        mCallback.onServiceDisconnected(reason);
    }

    @Override
    public void onServiceRegistered() throws RemoteException {
        mCallback.onServiceRegistered();
    }

    @Override
    public void onServiceUnregistered() throws RemoteException {
        mCallback.onServiceUnregistered();
    }

    @Override
    public void onNewMessage(final long accountId, final long messageId) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onNewMessage(accountId, messageId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void onReportMessageFailed(final long messageId) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onReportMessageFailed(messageId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void onReportMessageDisplayed(final long messageId) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onReportMessageDisplayed(messageId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void onReportMessageDelivered(final long messageId) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onReportMessageDelivered(messageId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void onComposingEvent(long accountId, boolean status) throws RemoteException {
        mCallback.onComposingEvent(accountId, status);
    }

    @Override
    public void onTransferProgress(
            final long messageId, final long currentSize, final long totalSize)
            throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.onTransferProgress(messageId, currentSize, totalSize);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportSubscribeResult(
            final long requestId, final int resultCode) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportSubscribeResult(requestId, resultCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportUnsubscribeResult(
            final long requestId, final int resultCode) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportUnsubscribeResult(requestId, resultCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportGetSubscribedResult(
            final long requestId, final int resultCode, final long[] accountIds)
            throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportGetSubscribedResult(requestId, resultCode, accountIds);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportGetDetailsResult(
            final long requestId, final int resultCode, final long accountId)
            throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportGetDetailsResult(requestId, resultCode, accountId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportGetMenuResult(
            final long requestId, final int resultCode) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportGetMenuResult(requestId, resultCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportDownloadResult(final long requestId,
            final int resultCode, final String path, final long mediaId) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportDownloadResult(requestId, resultCode, path, mediaId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void updateDownloadProgress(
            final long requestId, final int percentage) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.updateDownloadProgress(requestId, percentage);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportSetAcceptStatusResult(
            final long requestId, final int resultCode) throws RemoteException {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCallback.reportSetAcceptStatusResult(requestId, resultCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportComplainSpamSuccess(final long messageId) throws RemoteException {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mCallback.reportComplainSpamSuccess(messageId);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportComplainSpamFailed(
            final long messageId, final int errorCode) throws RemoteException {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mCallback.reportComplainSpamFailed(messageId, errorCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });

    }

    @Override
    public void onAccountChanged(final String newAccount) throws RemoteException {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mCallback.onAccountChanged(newAccount);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportDeleteMessageResult(
            final long requestId, final int resultCode) throws RemoteException {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mCallback.reportDeleteMessageResult(requestId, resultCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

    @Override
    public void reportSetFavourite(
            final long requestId, final int resultCode) throws RemoteException {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mCallback.reportSetFavourite(requestId, resultCode);
                } catch (RemoteException e) {
                    throw new Error(e);
                }
            }
        });
    }

}
