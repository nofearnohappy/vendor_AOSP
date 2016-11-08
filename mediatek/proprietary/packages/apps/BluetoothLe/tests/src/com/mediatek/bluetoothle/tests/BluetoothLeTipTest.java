
package com.mediatek.bluetoothle.test;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

//The test case doesn't need to really enable BT
//import android.bluetooth.BluetoothAdapter;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.android.internal.util.IState;
import com.android.internal.util.StateMachine;
import com.mediatek.bluetoothle.tip.ITimeUpdater;
import com.mediatek.bluetoothle.tip.MockedReferenceTimeUpdateService;
import com.mediatek.bluetoothle.tip.TimeUpdateCallback;

import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BluetoothLeTipTest extends InstrumentationTestCase {
    private static final String TAG = "BluetoothLeTipTest";

    // The test case doesn't need to really enable BT
    // private static final int WAIT_ON_OFF = 5000;
    private static final int WAIT_UPDATE = 1000;
    private static final int WAIT_PROCESS = 300;
    private static final int WAIT_COMMAND = 100;

    static final int STATE_IDLE = 0;
    static final int STATE_UPDATE_PENDING = 1;

    static final int MSG_START_REFERENCE_UPDATE = 0;
    static final int MSG_CANCEL_REFERENCE_UPDATE = 1;
    static final int MSG_NO_CONNECTION = 2;
    static final int MSG_TIME_ERROR = 3;
    static final int MSG_TIMEOUT = 4;
    static final int MSG_NEW_TIME = 5;

    static final int RESULT_SUCCESS = 0;
    static final int RESULT_CANCELED = 1;
    static final int RESULT_NO_CONNECTION = 2;
    static final int RESULT_ERROR = 3;
    static final int RESULT_TIMEOUT = 4;
    static final int RESULT_UPDATE_NOT_ATTEMPED = 5;

    private ITimeUpdater mUpdater = null;
    private MockedReferenceTimeUpdateService mRtus = null;

    private Class mTipSmCls = null;
    private Object mSmObj = null;

    @Override
    protected void setUp() throws Exception {
        // The test case doesn't need to really enable BT
        // BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        // int btState = bt.getState();
        // if (BluetoothAdapter.STATE_ON != btState) {
        // bt.enable();
        // Thread.sleep(WAIT_ON_OFF);
        // }

        // Workaround1: Space for Mock classes generation
        System.setProperty("dexmaker.dexcache", this.getInstrumentation()
                .getTargetContext().getCacheDir().getPath());
        // Workaround2: For sharedUserId
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        // Prepare Mock Object
        mUpdater = mock(ITimeUpdater.class);
        mRtus = mock(MockedReferenceTimeUpdateService.class);

        // Make the Test Target: Time Update State Machine
        mTipSmCls = Class
            .forName("com.mediatek.bluetoothle.tip.TimeUpdateStateMachine");
        final Class rtusCls = Class
                .forName("com.mediatek.bluetoothle.tip.ReferenceTimeUpdateService");
        final Class[] getMakeSmParam = {
            rtusCls
        };
        final Method make = mTipSmCls.getDeclaredMethod("make", getMakeSmParam);
        make.setAccessible(true);
        mSmObj = make.invoke(null, mRtus);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // Quit the Time State Machine
        final Class sm = Class
                .forName("com.android.internal.util.StateMachine");
        final Method quit = sm.getDeclaredMethod("quit");
        quit.setAccessible(true);
        quit.invoke(mSmObj);
    }

    public void testUpdateSuccess() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateNoConnection() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_NO_CONNECTION)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_NO_CONNECTION);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateTimeout() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_TIMEOUT)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_TIMEOUT);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateError() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateNoResponse() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // No update status return
            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            final Class smCls = Class.forName("com.android.internal.util.StateMachine");
            final Method getCurrentState = smCls.getDeclaredMethod("getCurrentState");
            getCurrentState.setAccessible(true);
            final Object returnedState = getCurrentState.invoke(mSmObj);
            final String state = ((IState) returnedState).getName();
            Log.v(TAG, state);
            assertTrue(state.equals("UpdatePendingState"));

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testCancelNoUpdate() {
        // Cancel Update
        ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);

        try {
            Thread.sleep(WAIT_PROCESS);
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Verify
        verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
    }

    public void testUpdateSuccessCancel() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_COMMAND);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Cancel Update
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateErrorCancel() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_COMMAND);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Cancel Update
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateTimeoutCancel() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_COMMAND);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Cancel Update
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_TIMEOUT)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateNoConnectionCancel() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_COMMAND);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Cancel Update
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_NO_CONNECTION)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateUpdateSuccess() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus, times(2)).onStateUpdate(STATE_UPDATE_PENDING);
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateUpdateFail() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus, times(2)).onStateUpdate(STATE_UPDATE_PENDING);
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateSuccessUpdateSuccess() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Second update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater, times(2)).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus, times(2)).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus, times(2)).onTimeUpdate(Matchers.anyLong());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateSuccessUpdateFail() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Second update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Object arg2List[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, arg2List);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater, times(2)).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateFailUpdateSuccess() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Second update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Object arg2List[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, arg2List);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater, times(2)).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateFailUpdateFail() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Second update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Object arg2List[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, arg2List);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mUpdater, times(2)).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testCancelUpdateSuccess() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testCancelUpdateFail() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateCancel() {
        // Start Update
        ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);
        ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);

        try {
            Thread.sleep(WAIT_PROCESS);
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Verify
        verify(mRtus).onStateUpdate(STATE_UPDATE_PENDING);
        verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
    }

    public void testUpdateCancelUpdateSuccess() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(System.currentTimeMillis()), new Integer(RESULT_SUCCESS)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus, times(2)).onStateUpdate(STATE_UPDATE_PENDING);
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater, times(2)).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_SUCCESS);
            verify(mRtus).onTimeUpdate(Matchers.anyLong());

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public void testUpdateCancelUpdateFail() {

        try {
            // Inject mocked time updater
            final Field updaterField = mTipSmCls.getDeclaredField("mTimeUpdater");
            updaterField.setAccessible(true);
            updaterField.set(mSmObj, mUpdater);

            // Get time update callback
            final Field cbField = mTipSmCls.getDeclaredField("mTimeUpdateCb");
            cbField.setAccessible(true);
            final Object cb = cbField.get(mSmObj);

            // Stub updateTime
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    // TODO Auto-generated method stub
                    Log.v(TAG, "Mocked Updater!!");
                    return null;
                }
            })
                    .when(mUpdater).updateTime((TimeUpdateCallback) cb);

            // Start Update
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_CANCEL_REFERENCE_UPDATE);
            ((StateMachine) mSmObj).sendMessage(MSG_START_REFERENCE_UPDATE);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            try {
                Thread.sleep(WAIT_UPDATE);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Return the update status
            final Class cbCls = Class.forName("com.mediatek.bluetoothle.tip.TimeUpdateCallback");
            final Class[] getCbParam = {
                    Long.TYPE, Integer.TYPE
            };
            final Method onTimeUpdated = cbCls.getDeclaredMethod("onTimeUpdated", getCbParam);
            onTimeUpdated.setAccessible(true);
            final Object argList[] = {
                    new Long(0), new Integer(RESULT_ERROR)
            };
            onTimeUpdated.invoke(cb, argList);

            try {
                Thread.sleep(WAIT_PROCESS);
            } catch (final InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // Verify
            verify(mRtus, times(2)).onStateUpdate(STATE_UPDATE_PENDING);
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_CANCELED);
            verify(mUpdater, times(2)).updateTime((TimeUpdateCallback) Matchers.any());
            verify(mRtus).onStateUpdate(STATE_IDLE, RESULT_ERROR);

        } catch (final ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchMethodException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (final IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final NoSuchFieldException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }
}
