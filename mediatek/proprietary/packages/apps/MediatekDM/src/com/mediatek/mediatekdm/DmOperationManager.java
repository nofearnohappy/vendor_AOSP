package com.mediatek.mediatekdm;

import android.util.Log;

import com.mediatek.mediatekdm.util.Utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DmOperationManager {

    public interface IOperationScannerHandler {
        void notify(boolean pendingOperationFound);
    }

    public interface IOperationStateObserver {
        void notify(State state, State previousState, Object extra);
    }

    public enum State {
        BUSY, IDLE, RECOVERING,
    }

    public enum TriggerResult {
        BUSY, MAX_RETRY_REACHED, NO_PENDING_OPERATION, SKIPPED, SUCCESS,
    }

    private class OperationRemover implements Runnable {
        private final String mFolderPath;
        private final DmOperation mOperation;

        public OperationRemover(DmOperation operation, String folderPath) {
            mOperation = operation;
            mFolderPath = folderPath;
        }

        @Override
        public void run() {
            Log.d(TAG, "+removeOperation(" + mOperation.getId() + ")");
            String path = mFolderPath + File.separator + mOperation.getId();
            File file = new File(path);
            if (file.exists()) {
                if (!file.delete()) {
                    Log.e(TAG, "Deletion failed");
                }
            } else {
                Log.w(TAG, "No file to delete");
            }
            Log.d(TAG, "-removeOperation()");
        }
    }

    private class OperationScanner implements Runnable {
        private final String mFolderPath;
        private final IOperationScannerHandler mHandler;
        private final DmOperationManager mManager;

        public OperationScanner(IOperationScannerHandler handler, String folderPath,
                DmOperationManager manager) {
            mHandler = handler;
            mFolderPath = folderPath;
            mManager = manager;
        }

        @Override
        public void run() {
            Log.d(TAG, "+scanOperation()");
            boolean hasPendingOperations = false;
            try {
                File folder = new File(mFolderPath);
                if (!folder.exists()) {
                    Log.w(TAG, "Operation folder does not exist");
                } else {
                    String[] fileList = folder.list();
                    if (fileList != null && fileList.length > 0) {
                        Log.d(TAG, "Total " + fileList.length + " pending operations found");
                        for (String filePath : fileList) {
                            if (filePath == null || filePath.length() <= 0) {
                                continue;
                            }
                            FileInputStream fis = null;
                            DmOperation operation = null;
                            try {
                                fis = new FileInputStream(mFolderPath + File.separator + filePath);
                                operation = new DmOperation(Long.parseLong(filePath));
                                operation.load(fis);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                                continue;
                            } catch (IOException e) {
                                e.printStackTrace();
                                continue;
                            } finally {
                                try {
                                    if (fis != null) {
                                        fis.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    continue;
                                }
                            }
                            try {
                                mManager.enqueue(operation, false);
                                hasPendingOperations = true;
                            } catch (NullPointerException e) {
                                Log.e(TAG, "Invalid operation data, ignore");
                                mExecutorService.execute(new OperationRemover(operation,
                                        mOperationFolderPath));
                                e.printStackTrace();
                            } catch (ClassCastException e) {
                                Log.e(TAG, "Invalid operation data, ignore");
                                mExecutorService.execute(new OperationRemover(operation,
                                        mOperationFolderPath));
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "notify the result '" + hasPendingOperations + "'");
            mHandler.notify(hasPendingOperations);
            Log.d(TAG, "-scanOperation()");
        }
    }

    private class OperationWriter implements Runnable {
        private final String mFolderPath;
        private final DmOperation mOperation;

        public OperationWriter(DmOperation operation, String folderPath) {
            mOperation = operation;
            mFolderPath = folderPath;
        }

        @Override
        public void run() {
            Log.d(TAG, "+writeOperation(" + mOperation.getId() + ")");
            FileOutputStream fos = null;
            try {
                String path = mFolderPath + File.separator + mOperation.getId();
                fos = new FileOutputStream(path);
                mOperation.store(fos, new Date(mOperation.getId()).toString());
            } catch (IOException e) {
                Log.e(TAG, "Failed to save operation data");
                e.printStackTrace();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to close output stream");
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "-writeOperation()");
        }
    }

    public static final long SKIP_MARK_UNSPECIFIED = -1;
    public static final String TAG = "DM/DmOperationManager";
    private static DmOperationManager sInstance;

    public static DmOperationManager getInstance() {
        synchronized (DmOperationManager.class) {
            if (sInstance == null) {
                sInstance = new DmOperationManager(PlatformManager.getInstance().getPathInData(
                        DmApplication.getInstance(), DmConst.Path.DM_OPERATION_FOLDER));
            }
            return sInstance;
        }
    }

    private DmOperation mCurrent;
    private ExecutorService mExecutorService;
    private Set<IOperationStateObserver> mObservers;
    private final String mOperationFolderPath;
    private TreeSet<DmOperation> mQueue;
    private long mSkipMark = SKIP_MARK_UNSPECIFIED;
    private State mState;

    public DmOperationManager(String operationFolderPath) {
        mQueue = new TreeSet<DmOperation>();
        // We use single thread executor so the file operations are processed in FIFO manner.
        mExecutorService = Executors.newSingleThreadExecutor();
        mCurrent = null;
        mState = State.IDLE;
        mOperationFolderPath = operationFolderPath;
        mObservers = new HashSet<IOperationStateObserver>();
        if (!prepareFolder()) {
            throw new Error("Failed to prepare operation folder!");
        }
    }

    public synchronized DmOperation current() {
        return mCurrent;
    }

    public void enqueue(DmOperation operation) {
        enqueue(operation, true);
    }

    public synchronized void enqueue(DmOperation operation, boolean writeToFS) {
        Log.d(TAG, "+enqueue()");
        if (!mQueue.contains(operation)
                && !(mCurrent != null && mCurrent.getId() == operation.getId())) {
            Log.d(TAG, "Enqueue " + operation);
            mQueue.add(operation);
            if (writeToFS) {
                mExecutorService.execute(new OperationWriter(operation, mOperationFolderPath));
            }
        } else {
            Log.w(TAG, "Operation " + operation.getId()
                    + " is already in queue. Ignore this enqueue action.");
        }
        Log.d(TAG, "-enqueue()");
    }

    public synchronized boolean finishCurrent() {
        if (!isBusy()) {
            Log.e(TAG, "Nothing to finish!");
            return false;
        } else {
            Log.i(TAG, "Finish " + mCurrent);
            mExecutorService.execute(new OperationRemover(mCurrent, mOperationFolderPath));
            mCurrent = null;
            setState(State.IDLE);
            return true;
        }
    }

    public synchronized State getState() {
        return mState;
    }

    public synchronized boolean hasNext() {
        return !mQueue.isEmpty();
    }

    public synchronized void ignoreOperationsBeforeTimestamp(long time) {
        Log.d(TAG, "+ignoreOperationsBeforeTimestamp(" + time + ")");
        setSkipMark(time);
        purgeOperationsBeforeTimestamp(time);
        Log.d(TAG, "-ignoreOperationsBeforeTimestamp()");
    }

    public synchronized boolean isBusy() {
        return mState != State.IDLE;
    }

    public synchronized boolean isInRecovery() {
        return mState == State.RECOVERING;
    }

    public synchronized void notifyCurrentAborted() {
        setState(State.RECOVERING);
    }

    public synchronized void purgeOperationsBeforeTimestamp(long time) {
        Iterator<DmOperation> iterator = mQueue.iterator();
        while (iterator.hasNext() && iterator.next().getId() < time) {
            iterator.remove();
        }
    }

    /**
     * Recover the current operation. This method will not decrease the retry counter of current
     * operation. Client should invoke this method when non-fatal error recovers before the time-out
     * is emitted.
     */
    public synchronized void recoverCurrent() {
        if (mState == State.RECOVERING && mCurrent.getRetry() > 0) {
            mCurrent.recover();
            setState(State.BUSY);
        } else {
            throw new Error("Cannot retry current when the operation is not in recovery state.");
        }
    }

    public void registerObserver(IOperationStateObserver observer) {
        mObservers.add(observer);
    }

    /**
     * Retry the current operation. This method will decrease the retry counter of current
     * operation. Client should invoke this method after the current operation has timed out.
     */
    public synchronized void retryCurrent() {
        if (mState == State.RECOVERING && mCurrent.getRetry() > 0) {
            mCurrent.retry();
            setState(State.BUSY);
        } else {
            throw new Error("Cannot retry current when the operation is not in recovery state.");
        }
    }

    public void scanPendingOperations(IOperationScannerHandler handler) {
        mExecutorService.execute(new OperationScanner(handler, mOperationFolderPath, this));
    }

    public synchronized void setSkipMark(long time) {
        mSkipMark = time;
    }

    public synchronized TriggerResult triggerNext() {
        Log.i(TAG, "+triggerNext()");
        if (mState != State.IDLE) {
            Log.e(TAG, "There is an operation running! Current operation is " + mCurrent);
            Log.i(TAG, "-triggerNext()");
            return TriggerResult.BUSY;
        } else if (mQueue.isEmpty()) {
            Log.w(TAG, "Nothing to start.");
            return TriggerResult.NO_PENDING_OPERATION;
        } else if (mQueue.first().getRetry() <= 0) {
            Log.w(TAG, "Max retry reached. Remove it from queue.");
            mQueue.remove(mQueue.first());
            Log.i(TAG, "-triggerNext()");
            return TriggerResult.MAX_RETRY_REACHED;
        } else {
            mCurrent = mQueue.first();
            mQueue.remove(mCurrent);
            if (mSkipMark != SKIP_MARK_UNSPECIFIED && mCurrent.getId() < mSkipMark) {
                mCurrent = null;
                Log.i(TAG, "Operation " + mCurrent + "is skipped.");
                Log.i(TAG, "-triggerNext()");
                return TriggerResult.SKIPPED;
            } else {
                Log.i(TAG, "Operation " + mCurrent + "started.");
                setState(State.BUSY);
                Log.i(TAG, "-triggerNext()");
                return TriggerResult.SUCCESS;
            }
        }
    }

    /**
     * Equivalent to invoking TriggerResult triggerNow(DmOperation operation, boolean writeToFS)
     * with writeToFS equals to false.
     *
     * @param operation
     * @return
     */
    public synchronized TriggerResult triggerNow(DmOperation operation) {
        return triggerNow(operation, false);
    }

    /**
     * This method can only be called when there is no operation running. It will cut in line and
     * set the operation passed in as current running operation. NOTE: the operation inserted will
     * ignore skip mark.
     *
     * @param operation
     *        the operation to be triggered
     * @param writeToFS
     *        whether write the operation to file system
     * @return
     */
    public synchronized TriggerResult triggerNow(DmOperation operation, boolean writeToFS) {
        Log.i(TAG, "+triggerNow()");
        Log.i(TAG, "Operation is " + operation);
        if (mState != State.IDLE) {
            Log.e(TAG, "There is an operation running! Current operation is " + mCurrent);
            Log.i(TAG, "-triggerNow()");
            return TriggerResult.BUSY;
        } else if (operation.getRetry() <= 0) {
            Log.w(TAG, "Max retry reached.");
            Log.i(TAG, "-triggerNow()");
            return TriggerResult.MAX_RETRY_REACHED;
        } else {
            mCurrent = operation;
            setState(State.BUSY);
            if (writeToFS) {
                mExecutorService.execute(new OperationWriter(operation, mOperationFolderPath));
            }
            Log.i(TAG, "Operation started.");
            Log.i(TAG, "-triggerNow()");
            return TriggerResult.SUCCESS;
        }
    }

    public void unregisterObserver(IOperationStateObserver observer) {
        mObservers.remove(observer);
    }

    private boolean prepareFolder() {
        Log.d(TAG, "+prepareFolder()");
        File dirFolder = DmApplication.getInstance().getFilesDir();
        if (!dirFolder.exists()) {
            Log.e(TAG, dirFolder.getAbsolutePath() + "does not exist, create it");
            if (dirFolder.mkdir()) {
                Utilities.openPermission(dirFolder.getAbsolutePath());
            } else {
                Log.e(TAG, "Failed to create " + dirFolder.getAbsolutePath()
                        + ", failed to save operation data");
                Log.d(TAG, "-prepareFolder()");
                return false;
            }
        }
        File operationFolder = new File(mOperationFolderPath);
        if (!operationFolder.exists()) {
            Log.w(TAG, mOperationFolderPath + " does not exist, create one");
            if (!operationFolder.mkdirs()) {
                Log.e(TAG, "Failed to create " + mOperationFolderPath
                        + ", failed to save operation data");
                Log.d(TAG, "-prepareFolder()");
                return false;
            }
        }
        Log.d(TAG, "-prepareFolder()");
        return true;
    }

    private void setState(State state) {
        setState(state, null);
    }

    private void setState(State state, Object extra) {
        Log.d(TAG, "Set state to " + state);
        State previousState = mState;
        mState = state;
        for (IOperationStateObserver o : mObservers) {
            o.notify(state, previousState, extra);
        }
    }
}
