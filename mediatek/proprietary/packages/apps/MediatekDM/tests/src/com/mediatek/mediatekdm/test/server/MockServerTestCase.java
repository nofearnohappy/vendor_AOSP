/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.mediatekdm.test.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;
import android.util.Base64;
import android.util.Log;

import com.mediatek.mediatekdm.DmApplication;
import com.mediatek.mediatekdm.DmConfig;
import com.mediatek.mediatekdm.DmConst;
import com.mediatek.mediatekdm.DmConst.IntentAction;
import com.mediatek.mediatekdm.DmConst.NotificationInteractionType;
import com.mediatek.mediatekdm.DmService;
import com.mediatek.mediatekdm.PlatformManager;
import com.mediatek.mediatekdm.SimpleXMLAccessor;
import com.mediatek.mediatekdm.test.Checklist;
import com.mediatek.mediatekdm.test.MockDmNotification;
import com.mediatek.mediatekdm.test.MockPlatformManager;
import com.mediatek.mediatekdm.test.ServiceMockContext;
import com.mediatek.mediatekdm.test.TestEnvironment;
import com.mediatek.mediatekdm.test.Utilities;
import com.mediatek.mediatekdm.test.server.MockServerService.TestResult;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class MockServerTestCase extends ServiceTestCase<DmService> implements IServiceTest {

    private final ServiceConnection mServiceConnection;
    private BlockingQueue<IMockServer> mBinderQueue;
    private IMockServer mServerStub = null;
    private Context mMockContext = null;
    private Handler mHandler = null;
    private final String mTestType;
    private Checklist mChecklist = null;
    private final int mBreakPoint;
    private final int mBreakTime;
    private final int mWaitTime;

    public MockServerTestCase(String testType, int breakPoint, int breakTime, int waitTime) {
        super(DmService.class);
        mTestType = testType;
        mBinderQueue = new ArrayBlockingQueue<IMockServer>(1);
        mBreakPoint = breakPoint;
        mBreakTime = breakTime;
        mWaitTime = waitTime;
        mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.d(getTag(), "onServiceDisconnected");
            }

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                Log.d(getTag(), "onServiceConnected");
                Log.d(getTag(), "binder is " + binder);
                IMockServer stub = IMockServer.Stub.asInterface(binder);
                Log.d(getTag(), "stub is " + stub);
                notifyBindServer(IMockServer.Stub.asInterface(binder));
            }
        };
    }

    public MockServerTestCase(String testType) {
        this(testType, -1, -1, -1);
    }

    public MockServerTestCase(String testType, int waitTime) {
        this(testType, -1, -1, waitTime);
    }

    protected String getTag() {
        return "MDMTest/MockServerTestCase";
    }

    @Override
    public Checklist getChecklist() {
        return mChecklist;
    }

    private byte[] getNIA(int index) {
        InputStream is = null;
        byte[] nia = null;
        try {
            is = Utilities.getTestPackageContext(getSystemContext()).getAssets()
                    .open("NIA" + index);
            nia = new byte[is.available()];
            is.read(nia);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                throw new Error(e);
            }
        }
        return nia;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.d(getTag(), "setUp()");
        mChecklist = new Checklist();
        if (mHandler == null) {
            mHandler = new Handler(getSystemContext().getMainLooper());
        }
        startServer();
        mMockContext = new ServiceMockContext(getSystemContext(), this);
        setContext(mMockContext);
        // We do not create our own DmApplication instance because MeditekDMTestRunner had already
        // created one.
        assertNotNull(DmApplication.getInstance());
        setApplication(DmApplication.getInstance());
        Log.w(getTag(), "Application instance: " + getApplication());
        MockPlatformManager.setUp();
        prepareFiles();
        setServerNonce(new byte[] { (byte) 1 });
        setClientNonce(new byte[] { (byte) 1 });
    }

    @Override
    protected void tearDown() throws Exception {
        Log.d(getTag(), "tearDown()");
        stopServer();
        clearFiles();
        mChecklist = null;
        super.tearDown();
        // DmService.onDestroy is invoked later than this, invoke after super.tearDown
        MockPlatformManager.tearDown();
    }

    private void startServer() {
        Log.d(getTag(), "startServer");
        Intent intent = new Intent(mTestType);
        Log.i(getTag(), "Name of service is " + MockServerService.class.getName());
        intent.setClassName("com.mediatek.mediatekdm.test", MockServerService.class.getName());
        Log.d(getTag(), "intent is " + intent);
        boolean result = getSystemContext().bindService(intent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
        Log.w(getTag(), "bindService returns " + result);
        if (!result) {
            throw new Error("Failed to bind to server.");
        }
        mServerStub = waitBindServer();
        try {
            if (mBreakPoint != -1) {
                mServerStub.setParameter(TemplateDrivenBinder.KEY_BREAKPOINT,
                        Integer.toString(mBreakPoint));
                mServerStub.setParameter(TemplateDrivenBinder.KEY_BREAKTIME,
                        Integer.toString(mBreakTime * 1000));
            }
            if (mWaitTime != -1) {
                mServerStub.setParameter(TemplateDrivenBinder.KEY_TIMEOUT,
                        Integer.toString(mWaitTime * 1000));
            }
            mServerStub.start();
        } catch (RemoteException e) {
            throw new Error(e);
        }
    }

    private void stopServer() {
        try {
            mServerStub.stop();
        } catch (RemoteException e) {
            throw new Error(e);
        }
        mServerStub = null;
        getSystemContext().unbindService(mServiceConnection);
    }

    private void notifyBindServer(IMockServer binder) {
        Log.w(getTag(), "notifyBindServer in thread " + android.os.Process.myTid());
        Log.w(getTag(), "notifyBindServer in process " + android.os.Process.myPid());
        try {
            Log.d(getTag(), "notifyBindService binder is " + binder);
            mBinderQueue.put(binder);
            Log.d(getTag(), "binder has been put to queue");
        } catch (InterruptedException e) {
            throw new Error(e);
        }
    }

    private IMockServer waitBindServer() {
        Log.w(getTag(), "waitBindServer in thread " + android.os.Process.myTid());
        Log.w(getTag(), "waitBindServer in process " + android.os.Process.myPid());
        IMockServer receivedBinder = null;
        try {
            Log.d(getTag(), "wait begin");
            receivedBinder = mBinderQueue.poll(20, TimeUnit.SECONDS);
            Log.d(getTag(), "wait end");
        } catch (InterruptedException e) {
            new Error(e);
        }
        Log.d(getTag(), "waitBindService receivedBinder is " + receivedBinder);
        return receivedBinder;
    }

    private void setServerNonce(byte[] nonce) {
        DmTreeAccessor accessor = new DmTreeAccessor();
        FileInputStream fis;
        try {
            String pathString = PlatformManager.getInstance().getPathInData(getContext(),
                    DmConst.Path.DM_TREE_FILE);
            fis = new FileInputStream(pathString);
            accessor.parse(fis);
            fis.close();
            accessor.setServerNonce(nonce);
            FileOutputStream fos = new FileOutputStream(pathString);
            accessor.write(fos);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void setClientNonce(byte[] nonce) {
        DmTreeAccessor accessor = new DmTreeAccessor();
        FileInputStream fis;
        try {
            String pathString = PlatformManager.getInstance().getPathInData(getContext(),
                    DmConst.Path.DM_TREE_FILE);
            fis = new FileInputStream(pathString);
            accessor.parse(fis);
            fis.close();
            accessor.setClientNonce(nonce);
            FileOutputStream fos = new FileOutputStream(pathString);
            accessor.write(fos);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private class DmTreeAccessor extends SimpleXMLAccessor {
        public byte[] getServerNonce() {
            return getNonce("serverAuth");
        }

        public byte[] getClientNonce() {
            return getNonce("clientAuth");
        }

        public void setServerNonce(byte[] nonce) {
            setNonce("serverAuth", nonce);
        }

        public void setClientNonce(byte[] nonce) {
            setNonce("clientAuth", nonce);
        }

        private byte[] getNonce(String selector) {
            String nonceString = null;
            if (mDocument == null) {
                throw new Error("You should parse document first.");
            }
            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression expression = xpath.compile("//*/leaf[name=\"AAuthData\"]");
                NodeList leafList = (NodeList) expression.evaluate(mDocument,
                        XPathConstants.NODESET);
                for (int i = 0; i < leafList.getLength(); ++i) {
                    boolean selectorFound = false;
                    Node node = leafList.item(i);
                    NodeList children = node.getParentNode().getChildNodes();
                    for (int j = 0; j < children.getLength(); ++j) {
                        Node child = children.item(j);
                        if (child.getNodeName().equals("name")
                                && child.getTextContent().trim().equals(selector)) {
                            selectorFound = true;
                            break;
                        }
                    }
                    if (selectorFound) {
                        children = node.getChildNodes();
                        for (int j = 0; j < children.getLength(); ++j) {
                            Node child = children.item(j);
                            if (child.getNodeName().equals("value")) {
                                nonceString = child.getTextContent().trim();
                                break;
                            }
                        }
                        break;
                    } else {
                        Log.e(getTag(), "Cannot find nonce node for " + selector);
                    }
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }

            if (nonceString == null) {
                return null;
            } else {
                byte[] result = Base64.decode(nonceString.getBytes(), Base64.NO_WRAP);
                return result;
            }
        }

        private void setNonce(String selector, byte[] nonce) {
            if (mDocument == null) {
                throw new Error("You should parse document first.");
            }
            String nonceString = Base64.encodeToString(nonce, Base64.NO_WRAP);
            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression expression = xpath.compile("//*/leaf[name=\"AAuthData\"]");
                NodeList leafList = (NodeList) expression.evaluate(mDocument,
                        XPathConstants.NODESET);
                for (int i = 0; i < leafList.getLength(); ++i) {
                    boolean selectorFound = false;
                    Node node = leafList.item(i);
                    NodeList children = node.getParentNode().getChildNodes();
                    for (int j = 0; j < children.getLength(); ++j) {
                        Node child = children.item(j);
                        if (child.getNodeName().equals("name")
                                && child.getTextContent().trim().equals(selector)) {
                            selectorFound = true;
                            break;
                        }
                    }
                    if (selectorFound) {
                        children = node.getChildNodes();
                        for (int j = 0; j < children.getLength(); ++j) {
                            Node child = children.item(j);
                            if (child.getNodeName().equals("value")) {
                                child.setTextContent(nonceString);
                                break;
                            }
                        }
                        break;
                    }
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }
        }

        public String setServerAddress(String address) {
            String oldValue = null;
            try {
                XPath xpath = XPathFactory.newInstance().newXPath();
                // XPath Query for showing all nodes value
                XPathExpression expr = xpath.compile("//*/leaf[name=\"Addr\"]");
                NodeList candidates = (NodeList) expr.evaluate(mDocument, XPathConstants.NODESET);
                for (int i = 0; i < candidates.getLength(); i++) {
                    Node candidate = candidates.item(i);
                    Node p = candidate;
                    if (!checkParent(p, "SrvAddr")) {
                        continue;
                    }
                    p = p.getParentNode();
                    if (!checkParent(p, "AppAddr")) {
                        continue;
                    }
                    Node nodeServerAddress = findChild(candidate, "value");
                    oldValue = nodeServerAddress.getTextContent().trim();
                    nodeServerAddress.setTextContent(address);
                    break;
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }
            return oldValue;
        }

        public String setServerPort(String port) {
            String oldValue = null;
            try {
                XPath xpath = XPathFactory.newInstance().newXPath();
                // XPath Query for showing all nodes value
                XPathExpression expr = xpath.compile("//*/leaf[name=\"PortNbr\"]");
                NodeList candidates = (NodeList) expr.evaluate(mDocument, XPathConstants.NODESET);
                for (int i = 0; i < candidates.getLength(); i++) {
                    Node candidate = candidates.item(i);
                    Node p = candidate;
                    if (!checkParent(p, "Port")) {
                        continue;
                    }
                    p = p.getParentNode();
                    if (!checkParent(p, "Port")) {
                        continue;
                    }
                    p = p.getParentNode();
                    if (!checkParent(p, "SrvAddr")) {
                        continue;
                    }
                    p = p.getParentNode();
                    if (!checkParent(p, "AppAddr")) {
                        continue;
                    }
                    Node nodePortNumber = findChild(candidate, "value");
                    oldValue = nodePortNumber.getTextContent().trim();
                    nodePortNumber.setTextContent(port);
                    break;
                }
            } catch (XPathExpressionException e) {
                throw new Error(e);
            }
            return oldValue;
        }

        private Node findChild(Node parent, String name, String value) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node child = children.item(i);
                if (child.getNodeName().equals(name) && child.getTextContent().trim().equals(value)) {
                    return child;
                }
            }
            return null;
        }

        private Node findChild(Node parent, String name) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node child = children.item(i);
                if (child.getNodeName().equals(name)) {
                    return child;
                }
            }
            return null;
        }

        private boolean checkParent(Node node, String parentName) {
            Node nodeName = findChild(node.getParentNode(), "name", parentName);
            return (nodeName != null);
        }

        @Override
        protected boolean omitXmlDeclaration() {
            return true;
        }
    }

    protected void testTemplate(final String testName, final int niaIndex,
            final int notificationExpected, final int notificationResponse,
            final int alertExpected, final int alertResponse) {
        Log.d(getTag(), testName + " runs in thread " + Thread.currentThread().getId());
        // Build check items
        if (notificationExpected != NotificationInteractionType.TYPE_INVALID) {
            mChecklist.addCheckItem("notification_type_0", notificationExpected);
        }
        if (alertExpected != NotificationInteractionType.TYPE_INVALID) {
            mChecklist.addCheckItem("alert_type_0", alertExpected);
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(getTag(), testName + " start service in thread "
                        + Thread.currentThread().getId());
                Intent intent = new Intent(IntentAction.DM_WAP_PUSH);
                intent.setClass(getSystemContext(), DmService.class);
                intent.setType(DmConst.IntentType.DM_NIA);
                intent.putExtra("data", MockServerTestCase.this.getNIA(niaIndex));
                intent.putExtra("simId", 0);
                intent.putExtra(PlatformManager.SUBSCRIPTION_KEY, TestEnvironment.REGISTER_SUB_ID);
                Log.d(getTag(), "System context is " + getSystemContext());
                // Context currentContext = getContext();
                // setContext(getSystemContext());
                startService(intent);
                prepareNotification(getService(), notificationResponse, alertResponse);
                // setContext(currentContext);
                Log.d(getTag(), testName + " start service complete in thread "
                        + Thread.currentThread().getId());
            }
        };
        mHandler.post(runnable);
        Log.d(getTag(), "Wait for test result ...");
        int result = TestResult.Fail;
        try {
            result = mServerStub.getResult();
        } catch (RemoteException e) {
            throw new Error(e);
        }
        Log.d(getTag(), "Test result is " + result);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Log.w(getTag(), "Waiting for testee is interrupted. Testee may not exit completely.");
        }

        mChecklist.check();
        assertEquals(TestResult.Success, result);
    }

    private void
            prepareNotification(DmService service, int notificationResponse, int alertResponse) {
        Log.d(getTag(), "prepareService: " + service);
        try {
            Field field = DmService.class.getDeclaredField("mDmNotification");
            field.setAccessible(true);
            field.set(service, new MockDmNotification(service, this, notificationResponse,
                    alertResponse));
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }

    }

    private void prepareFiles() {
        Context context = getContext();
        Log.e(getTag(), "prepareFiles with context: " + context);
        // tree.xml
        File systemTree = new File(PlatformManager.getInstance().getPathInSystem(
                DmConst.Path.DM_TREE_FILE));
        File dataTree = new File(PlatformManager.getInstance().getPathInData(context,
                DmConst.Path.DM_TREE_FILE));
        if (!context.getFilesDir().exists()) {
            Log.e(getTag(), "create: " + context.getFilesDir());
            context.getFilesDir().mkdir();
        } else {
            Log.e(getTag(), "found: " + context.getFilesDir());
        }
        com.mediatek.mediatekdm.util.Utilities.openPermission(context.getFilesDir()
                .getAbsolutePath());
        Utilities.copyFile(systemTree, dataTree);
        assertTrue(dataTree.exists());
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(dataTree);
            DmTreeAccessor accessor = new DmTreeAccessor();
            accessor.parse(fis);
            accessor.setServerAddress("http://" + mServerStub.getHost());
            accessor.setServerPort(Integer.toString(mServerStub.getPort()));
            fos = new FileOutputStream(dataTree);
            accessor.write(fos);
        } catch (FileNotFoundException e) {
            throw new Error(e);
        } catch (RemoteException e) {
            throw new Error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    fis = null;
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                    fos = null;
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
        // config.xml
        File systemConfig = new File(PlatformManager.getInstance().getPathInSystem(
                DmConst.Path.DM_CONFIG_FILE));
        File dataConfig = new File(PlatformManager.getInstance().getPathInData(context,
                DmConst.Path.DM_CONFIG_FILE));
        Properties properties = null;
        try {
            fis = new FileInputStream(systemConfig);
            properties = new Properties();
            properties.loadFromXML(fis);
            properties.setProperty(DmConfig.KEY_SEQUENTIAL_NONCE, "true");
            properties.setProperty(DmConfig.KEY_MOBILE_DATA_ONLY, "false");
            fos = new FileOutputStream(dataConfig);
            properties.storeToXML(fos, null);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                    fis = null;
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                    fos = null;
                } catch (IOException e) {
                    throw new Error(e);
                }
            }
        }
        assertTrue(dataConfig.exists());
    }

    private void clearFiles() {
        // Utilities.removeDirectoryRecursively(getContext().getFilesDir().getParentFile());
    }

    @Override
    public ComponentName startServiceEmulation(Intent intent) {
        Log.d(getName(), "startServiceEmulation: " + intent.getAction());
        startService(intent);
        return null;
    }

    @Override
    public boolean bindServiceEmulation(Intent intent, ServiceConnection conn, int flags) {
        Log.d(getName(), "bindServiceEmulation: " + intent.getAction());
        IBinder binder = bindService(intent);
        conn.onServiceConnected(null, binder);
        return true;
    }
}
