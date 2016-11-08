package com.mediatek.op01.tests;

//import android.provider.MediaStore;
import android.test.InstrumentationTestCase;
//import com.jayway.android.robotium.solo.Solo;
//import com.jayway.android.robotium.solo.Solo;

//import com.mediatek.mms.plugin.Op01MmsDialogNotifyExt;
//import com.android.mms.ui.ComposeMessageActivity;

public class Op01MmsDialogNotifyExtTest extends InstrumentationTestCase {
/*
    private static final String TAG = "Dialog test";
    public static final String URI_SMS = "content://sms";

    private Context mContext = null;
    private Instrumentation mInst = null;
    private Op01MmsDialogNotifyExt mPlugin = null;
    private Uri mUri = null;
    //private Solo mSolo;

    @Override
    protected void setUp() throws Exception {
        Log.d("@M_" + TAG, "setUp");
        super.setUp();

        mContext = this.getInstrumentation().getContext();
        mInst = this.getInstrumentation();
        mPlugin =
            (Op01MmsDialogNotifyExt)PluginManager.createPluginObject(mContext, "com.mediatek.mms.ext.IMmsDialogNotify");

        if (mPlugin == null) {
            Log.d("@M_" + TAG, "get plugin failed");
        }

        genTestSms();
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d("@M_" + TAG, "tearDown");
        super.tearDown();
        clearTestSms();
    }

    private void genTestSms() {
        Log.d("@M_" + TAG, "genTestSms");

        ContentValues values = new ContentValues();
        String timeStamp = "0";
        String readStamp = "0";
        String seenStamp = "0";
        String boxStamp = "0";
        String simcardStamp = "0";

        values.put(Sms.PROTOCOL, 0);
        values.put(Sms.ADDRESS, "10086");
        values.put(Sms.REPLY_PATH_PRESENT, 0);
        values.put(Sms.SERVICE_CENTER, "13800138000");
        values.put(Sms.BODY, "Auto Test");
        values.put(Sms.READ, "0");
        values.put(Sms.SEEN, "0");
        values.put(Sms.SIM_ID, "0");
        values.put(Sms.DATE, "0");
        values.put(Sms.TYPE, "1");
        values.put("import_sms", true);

        //ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Sms.Inbox.CONTENT_URI);
        //builder.withValues(values);
        //private ArrayList<ContentProviderOperation> mOperationList = new ArrayList<ContentProviderOperation>();
        //mOperationList.add(builder.build());
        ContentResolver resolver = mContext.getContentResolver();
        mUri = SqliteWrapper.insert(mContext, resolver, Inbox.CONTENT_URI, values);

        Log.d("@M_" + TAG, "mUri=" + mUri);
    }

    public void testNewSmsInLauncher() {
        Log.d("@M_" + TAG, "testNewSmsInLauncher");

        // Go to launcher
        //mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);
        gotoLauncher();

        try {
        Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Log.d("@M_" + TAG, "sleep1");
        }
        if (mUri == null) {
            Log.d("@M_" + TAG, "no uri");
            assertEquals(0, 1);
            return;
        }
        mPlugin.notifyNewSmsDialog(mUri);
        try {
        Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Log.d("@M_" + TAG, "sleep1");
        }

        boolean showDialog = isDialogShown(mContext);
        Log.d("@M_" + TAG, "show dialog?" + showDialog);
        assertEquals(true, showDialog);
    }

    public void testNewSmsNotInLauncher() {
        Log.d("@M_" + TAG, "testNewSmsNotInLauncher");

        // Go to search
        //mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_HOME);
        gotoLauncher();
        try {
        Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Log.d("@M_" + TAG, "sleep1");
        }
        //mInst.sendKeyDownUpSync(KeyEvent.KEYCODE_SEARCH);
        openAnotherActivity();
        try {
        Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Log.d("@M_" + TAG, "sleep1");
        }
        if (mUri == null) {
            Log.d("@M_" + TAG, "no uri");
            assertEquals(0, 1);
            return;
        }
        mPlugin.notifyNewSmsDialog(mUri);
        try {
        Thread.sleep(1000);
        } catch (InterruptedException ie) {
            Log.d("@M_" + TAG, "sleep1");
        }

        boolean showDialog = isDialogShown(mContext);
        Log.d("@M_" + TAG, "show dialog?" + showDialog);
        assertEquals(false, showDialog);
    }

    private boolean isDialogShown(Context context){
        String packageName;
        String className;
        boolean ret = false;

        Log.d("@M_" + TAG, "isDialogShown");

        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> rti = activityManager.getRunningTasks(2);

        packageName = rti.get(0).topActivity.getPackageName();
        className = rti.get(0).topActivity.getClassName();
        Log.d("@M_" + TAG, "package0="+packageName+"class0="+className);

        if (className.equals("com.android.mms.ui.DialogModeActivity")) {
            ret = true;
        }
        return ret;
    }

    private void clearTestSms() {
        if (mUri == null) {
            return;
        }

        int count = mContext.getContentResolver().delete(mUri, null, null);
        Log.d("@M_" + TAG, "delete count=" + count);
        gotoLauncher();
    }

    private void gotoLauncher() {
        Intent intent = new Intent();
        intent.setAction("action.intent.action.MAIN");
        intent.setClassName("com.android.launcher", "com.android.launcher2.Launcher");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void openAnotherActivity() {
        //startActivity(ComposeMessageActivity.createIntent(mContext, 0));
        Intent i = new Intent().setClassName("com.android.email", "com.android.email.activity.setup.AccountSetupBasics");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
*/
}

