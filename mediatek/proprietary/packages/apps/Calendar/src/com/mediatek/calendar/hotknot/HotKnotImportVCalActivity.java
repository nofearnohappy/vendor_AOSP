package com.mediatek.calendar.hotknot;

import com.mediatek.calendar.features.Features;
import com.mediatek.hotknot.HotKnotAdapter;
import com.mediatek.vcalendar.VCalParser;
import com.mediatek.vcalendar.VCalStatusChangeOperator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.widget.Toast;

public class HotKnotImportVCalActivity extends Activity implements
        VCalStatusChangeOperator {
    private static final String TAG = "HotKnotImportVCalActivity";
    protected static final String BUNDLE_KEY_EVENT_ID = "key_event_id";
    protected static final String BUNDLE_KEY_START_MILLIS = "key_start_millis";
    protected static final String BUNDLE_KEY_END_MILLIS = "key_end_millis";
    private static final String HANDLER_NAME = "importVCalendar";
    private byte[] mHotKnotMessage;
    private Handler mImportHandler = null;

    private Handler getsaveContactHandler() {
        if (null == mImportHandler) {
            HandlerThread controllerThread = new HandlerThread(HANDLER_NAME);
            controllerThread.start();
            mImportHandler = new Handler(controllerThread.getLooper());
        }
        return mImportHandler;
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "HotKnotImportVCalActivity, onCreate.");
        if (!Features.isHotKnotSupported()) {
            Log.w(TAG, "MTK_HOTKNOT_SUPPORT is not enabled!");
            return;
        }

        Intent intent = getIntent();
        if (!intent.getAction()
                .equals(HotKnotAdapter.ACTION_MESSAGE_DISCOVERED)) {
            Log.w(TAG, "Unknowon intent " + intent);
            finish();
            return;
        }

        String mimeType = intent.getType();
        if (mimeType == null
                || (!"text/x-vcalendar".equalsIgnoreCase(mimeType) && !"text/calendar"
                        .equalsIgnoreCase(mimeType))) {
            Log.w(TAG, "Not a vcalendar!");
            finish();
            return;
        }
        Toast.makeText(this, "Received HotKnot VCalendar", Toast.LENGTH_SHORT)
                .show();
        mHotKnotMessage = intent.getByteArrayExtra(HotKnotAdapter.EXTRA_DATA);

        doImportAction(this);
    }

    @Override
    protected void onStart(){
        super.onStart();
        setVisible(true);
    }

    private void doImportAction(VCalStatusChangeOperator listener) {
        String vcsContent = new String(mHotKnotMessage);
        Log.v(TAG, "doImportAction, vcsContent=" + vcsContent);
        Handler handler = getsaveContactHandler();
        if (handler != null) {
            handler.post(new ImporterThread(getApplicationContext(),
                    vcsContent, listener));
        }
    }

    private static class ImporterThread extends Thread {
        private Context mContext;
        private VCalStatusChangeOperator mListener;
        private VCalParser mParser;
        private String mVcsContent;

        public ImporterThread(Context context, String vcsContent,
                VCalStatusChangeOperator listener) {
            mContext = context;
            mVcsContent = vcsContent;
            mListener = listener;
        }

        @Override
        public void run() {
            Log.i(TAG, "startParseVcsContent... ... mVcsContent=" + mVcsContent);
            mParser = new VCalParser(mVcsContent, mContext, mListener);
            mParser.startParseVcsContent();
        }
    }

    @Override
    public void vCalOperationFinished(int successCnt, int totalCnt, Object obj) {
        Log.v(TAG, "vCalOperationFinished, obj=" + obj);
        Uri eventUri = null;
        long startMillis = System.currentTimeMillis();
        long endMills = startMillis;
        if (obj != null) {
            Bundle out = (Bundle) obj;
            long eventId = out.getLong(BUNDLE_KEY_EVENT_ID);
            Log.d(TAG, "vCalOperationFinished, eventId=" + eventId);
            startMillis = out.getLong(BUNDLE_KEY_START_MILLIS);
            endMills = out.getLong(BUNDLE_KEY_END_MILLIS);
            eventUri = Uri.withAppendedPath(Events.CONTENT_URI,
                    String.valueOf(eventId));
        }
        Log.v(TAG, "vCalOperationFinished, timeMillis=" + startMillis);
        Log.v(TAG, "vCalOperationFinished, endMills=" + endMills);
        Intent intent = createViewEventIntent(eventUri, startMillis, endMills);
        startActivity(intent);
        finish();
    }

    @Override
    public void vCalOperationStarted(int totalCnt) {
        Log.v(TAG, "vCalOperationStarted");
    }

    @Override
    public void vCalProcessStatusUpdate(int currentCnt, int totalCnt) {
        Log.v(TAG, "vCalProcessStatusUpdate");
    }

    @Override
    public void vCalOperationCanceled(int finishedCnt, int totalCnt) {
        Log.v(TAG, "vCalOperationCanceled");
    }

    @Override
    public void vCalOperationExceptionOccured(int finishedCnt, int totalCnt,
            int type) {
        Log.v(TAG, "vCalOperationExceptionOccured");
    }

    public Intent createViewEventIntent(Uri uri, long startMills, long endMills) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // intent.setClass(this, EventInfoActivity.class);
        intent.setDataAndType(uri, "vnd.android.cursor.item/event");
        intent.putExtra(
                android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                startMills);
        intent.putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME,
                endMills);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

}
