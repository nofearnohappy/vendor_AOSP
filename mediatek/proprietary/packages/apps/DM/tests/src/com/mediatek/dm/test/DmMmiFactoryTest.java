package com.mediatek.dm.test;

import junit.framework.Assert;

import com.mediatek.dm.DmMmiChoiceList;
import com.mediatek.dm.DmMmiConfirmation;
import com.mediatek.dm.DmMmiFactory;
import com.mediatek.dm.DmMmiInfoMsg;
import com.mediatek.dm.DmMmiInputQuery;
import com.mediatek.dm.DmMmiProgress;
import com.redbend.vdm.MmiConfirmation;
import com.redbend.vdm.MmiObserver;
import com.redbend.vdm.MmiViewContext;
import com.redbend.vdm.MmiInfoMsg;
import com.redbend.vdm.MmiInputQuery;

import android.test.AndroidTestCase;
import android.util.Log;

public class DmMmiFactoryTest extends AndroidTestCase {

    private static final String TAG = "[DmMmiFactoryTest]";

    protected void setUp() throws Exception {
        super.setUp();
    }


    public void testFactoryMethods() {
        Log.d(TAG, "test DmMmiFactory methods begin");

        String[] strArr = {"test", "test"};

        MyMmiObserver observer = new MyMmiObserver();

        MmiViewContext context = new MmiViewContext("test", 1, 1);
        DmMmiFactory factory = new DmMmiFactory();

        DmMmiChoiceList list = (DmMmiChoiceList) factory.createChoiceListDlg(observer);
        Assert.assertNotNull(list);
        list.display(context, strArr, 1, true);

        DmMmiConfirmation confirm = (DmMmiConfirmation) factory.createConfirmationDlg(observer);
        Assert.assertNotNull(confirm);
        confirm.display(context, MmiConfirmation.ConfirmCommand.YES);

        DmMmiInfoMsg infoMsg = (DmMmiInfoMsg) factory.createInfoMsgDlg(observer);
        Assert.assertNotNull(infoMsg);
        infoMsg.display(context, MmiInfoMsg.InfoType.GENERIC);

        DmMmiInputQuery query = (DmMmiInputQuery) factory.createInputQueryDlg(observer);
        Assert.assertNotNull(query);
        query.display(context, MmiInputQuery.InputType.NUMERIC, MmiInputQuery.EchoType.PLAIN,
                1, "test");

        DmMmiProgress progress = (DmMmiProgress) factory.createProgress(1);
        Assert.assertNotNull(progress);
        progress.update(10, 100);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    class MyMmiObserver implements MmiObserver {

        public void notfiyChoicelistSelection(int arg0) {
            // TODO Auto-generated method stub
            return;

        }

        public void notifyCancelEvent() {
            // TODO Auto-generated method stub
            return;
        }

        public void notifyConfirmationResult(boolean arg0) {
            // TODO Auto-generated method stub
            return;
        }

        public void notifyInfoMsgClosed() {
            // TODO Auto-generated method stub
            return;
        }

        public void notifyInputResult(String arg0) {
            // TODO Auto-generated method stub
            return;
        }

        public void notifyTimeoutEvent() {
            // TODO Auto-generated method stub
            return;
        }

    }

}
