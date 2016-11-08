/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.stk;

import com.android.internal.telephony.cat.CatLog;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyProperties;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.telephony.SubscriptionManager;
import android.os.SystemProperties;

/**
 * Application installer for SIM Toolkit.
 *
 */
class StkAppInstaller {
    private static final String LOG_TAG = "StkAppInstaller";
    Context mContext;
    private static int sSimCount = 0;
    private static StkAppInstaller mInstance = new StkAppInstaller();
    private StkAppInstaller() {
        CatLog.d(LOG_TAG, "init");
    }
    public static StkAppInstaller getInstance() {
        if (mInstance != null) {
            mInstance.initThread();
        }
        return mInstance;
    }
    private void initThread() {
        int i = 0;
        CatLog.d(LOG_TAG, "Init thread");
        // The STK_GEMINI_SIM_NUM index is for STK_MAIN
        sSimCount = StkAppService.STK_GEMINI_SIM_NUM + 2;
        if (installThread == null) {
            CatLog.d(LOG_TAG, "null installThread");
            installThread = new InstallThread[sSimCount];
            for (i = 0; i < sSimCount; i++) {
                if (installThread[i] == null) {
                    installThread[i] = new InstallThread();
                }
            }
        }
        if (uninstallThread == null) {
            CatLog.d(LOG_TAG, "null uninstallThread");
            uninstallThread = new UnInstallThread[sSimCount];
            for (i = 0; i < sSimCount; i++) {
                uninstallThread[i] = new UnInstallThread();
            }
        }
        if (miSTKInstalled == null) {
            CatLog.d(LOG_TAG, "null miSTKInstalled");
            miSTKInstalled = new int[sSimCount];
            for (i = 0; i < sSimCount; i++) {
                miSTKInstalled[i] = -1;
            }
        }
    }

    public static final int STK_NOT_INSTALLED = 1;
    public static final int STK_INSTALLED = 2;
    public static final int STK_LAUNCH_ID = 0xFF;
    public static final int STK_MENU_LAUNCH_ID = 0xFE;

    //private static int miSTKInstalled = -1;  // 1 -not_ready, 2-ready
    private static int[] miSTKInstalled = null;  // 1 -not_ready, 2-ready
    /* TODO: Gemini+ */
    private static final String STK_MAIN = "com.android.stk.StkMain";
    private static final String STK_MENU_LAUNCHER_ACTIVITY = "com.android.stk.StkLauncherActivity";
    private static final String STK1_LAUNCHER_ACTIVITY = "com.android.stk.StkLauncherActivityI";
    private static final String STK2_LAUNCHER_ACTIVITY = "com.android.stk.StkLauncherActivityII";
    private static final String STK3_LAUNCHER_ACTIVITY = "com.android.stk.StkLauncherActivityIII";
    private static final String STK4_LAUNCHER_ACTIVITY = "com.android.stk.StkLauncherActivityIV";

    void install(Context context) {
        setAppState(context, true, STK_LAUNCH_ID);
    }
    void unInstall(Context context) {
        setAppState(context, false, STK_LAUNCH_ID);
    }

    void install(Context context, int sim_id) {
        CatLog.d(LOG_TAG, "[install]+ sim_id: " + sim_id);
        if (STK_LAUNCH_ID == sim_id) {
            sim_id = (sSimCount - 2);
        } else if (STK_MENU_LAUNCH_ID == sim_id) {
            sim_id = (sSimCount - 1);
        }
        if (installThread[sim_id] != null) {
            CatLog.d(LOG_TAG, "[install]start");
            mContext = context;
            installThread[sim_id].setSim(sim_id);
            new Thread(installThread[sim_id]).start();
        }
    }

    void unInstall(Context context, int sim_id) {
        CatLog.d(LOG_TAG, "[unInstall]+ sim_id: " + sim_id);
        if (STK_LAUNCH_ID == sim_id) {
            sim_id = (sSimCount - 2);
        } else if (STK_MENU_LAUNCH_ID == sim_id) {
            sim_id = (sSimCount - 1);
        }
        if (uninstallThread[sim_id] != null) {
            CatLog.d(LOG_TAG, "[unInstall]start");
            mContext = context;
            uninstallThread[sim_id].setSim(sim_id);
            new Thread(uninstallThread[sim_id]).start();
        }
    }

    private static void setAppState(Context context, boolean install, int sim_id) {
        CatLog.d(LOG_TAG, "[setAppState]+ sim_id: " + sim_id);
        if (context == null) {
            CatLog.d(LOG_TAG, "[setAppState]- no context, just return.");
            return;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            CatLog.d(LOG_TAG, "[setAppState]- no package manager, just return.");
            return;
        }
        // check that STK app package is known to the PackageManager
        /* TODO: Gemini+ begin */
        String classname = null;
        switch (sim_id) {
            case PhoneConstants.SIM_ID_1:
                classname = STK1_LAUNCHER_ACTIVITY;
                break;
            case PhoneConstants.SIM_ID_2:
                classname = STK2_LAUNCHER_ACTIVITY;
                break;
            case PhoneConstants.SIM_ID_3:
                classname = STK3_LAUNCHER_ACTIVITY;
                break;
            case PhoneConstants.SIM_ID_4:
                classname = STK4_LAUNCHER_ACTIVITY;
                break;
            case STK_LAUNCH_ID:
                classname = STK_MAIN;
                break;
            case STK_MENU_LAUNCH_ID:
                classname = STK_MENU_LAUNCHER_ACTIVITY;
                break;
            default:
                CatLog.d("StkAppInstaller", "setAppState, ready to return because sim id "
                + sim_id + " is wrong.");
                return;
        }
        /* TODO: Gemini+ end */
        ComponentName cName = new ComponentName("com.android.stk", classname);
        CatLog.d(LOG_TAG, "setAppState, classname: "
        + classname.substring(classname.lastIndexOf('.') + 1)
        + " ComponentEnabledSetting: "
        + (PackageManager.COMPONENT_ENABLED_STATE_ENABLED == pm.getComponentEnabledSetting(cName) ?
        "ENABLED" : "DISABLED"));

        int state = install ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        if (STK_LAUNCH_ID == sim_id) {
            CatLog.d(LOG_TAG, "setAppState - curState[" +
            miSTKInstalled[(sSimCount - 2)] + "] to state[" + install + "]");
        } else if (STK_MENU_LAUNCH_ID == sim_id) {
            CatLog.d(LOG_TAG, "setAppState - curState[" +
            miSTKInstalled[(sSimCount - 1)] + "] to state[" + install + "]");
        } else if (sim_id > 0 && sim_id < miSTKInstalled.length - 2) {
            CatLog.d(LOG_TAG, "setAppState - curState[" + miSTKInstalled[sim_id] +
            "] to state[" + install + "]");
        }
        if (((PackageManager.COMPONENT_ENABLED_STATE_ENABLED == state) &&
                (PackageManager.COMPONENT_ENABLED_STATE_ENABLED ==
                pm.getComponentEnabledSetting(cName))) ||
                ((PackageManager.COMPONENT_ENABLED_STATE_DISABLED == state) &&
                (PackageManager.COMPONENT_ENABLED_STATE_DISABLED ==
                pm.getComponentEnabledSetting(cName)))) {
            CatLog.d(LOG_TAG, "setAppState, sim_id: " + sim_id +
            " Need not change app state!!");
        } else {
            try {
                pm.setComponentEnabledSetting(cName, state, PackageManager.DONT_KILL_APP);
            } catch (Exception e) {
                CatLog.d(LOG_TAG, "setAppState, sim_id: " + sim_id +
                " Could not change STK app state !!!");
                return;
            }
        }
        CatLog.d(LOG_TAG, "setAppState, sim_id: " + sim_id +
        " Change app state[" + install + "]");
        if (STK_LAUNCH_ID == sim_id) {
            try {
                miSTKInstalled[(sSimCount - 2)] = install ? STK_INSTALLED : STK_NOT_INSTALLED;
            } catch (ArrayIndexOutOfBoundsException e) {
                CatLog.d(LOG_TAG, "IOOB of setting miSTKInstalled[]");
            }
        } else if (STK_MENU_LAUNCH_ID == sim_id) {
            try {
                miSTKInstalled[(sSimCount - 1)] = install ? STK_INSTALLED : STK_NOT_INSTALLED;
            } catch (ArrayIndexOutOfBoundsException e) {
                CatLog.d(LOG_TAG, "IOOB of setting miSTKInstalled[]");
            }
        } else if (SubscriptionManager.isValidSlotId(sim_id)) {
            try {
                miSTKInstalled[sim_id] = install ? STK_INSTALLED : STK_NOT_INSTALLED;
            } catch (ArrayIndexOutOfBoundsException e) {
                CatLog.d(LOG_TAG, "IOOB of setting miSTKInstalled[]");
            }
        }
        CatLog.d(LOG_TAG, "[setAppState]-");
    }
    private class InstallThread implements Runnable {
        private int mSimId = -1;
        @Override
        public void run() {
            if ((sSimCount - 2) == mSimId) {
                CatLog.d(LOG_TAG, "InstallThread run, sim id:" + STK_LAUNCH_ID);
                setAppState(mContext, true, STK_LAUNCH_ID);
            } else if ((sSimCount - 1) == mSimId) {
                CatLog.d(LOG_TAG, "InstallThread run, sim id:" + STK_MENU_LAUNCH_ID);
                setAppState(mContext, true, STK_MENU_LAUNCH_ID);
            } else if (SubscriptionManager.isValidSlotId(mSimId)) {
                CatLog.d(LOG_TAG, "InstallThread run, sim id:" + mSimId);
                setAppState(mContext, true, mSimId);
            }
        }

        public void setSim(int sim_id) {
            if ((sSimCount - 2) == sim_id) {
                CatLog.d(LOG_TAG, "InstallThread setSim sim id:" + STK_LAUNCH_ID);
            } else if ((sSimCount - 1) == sim_id) {
                CatLog.d(LOG_TAG, "InstallThread setSim sim id:" + STK_MENU_LAUNCH_ID);
            } else {
                CatLog.d(LOG_TAG, "InstallThread setSim sim id:" + sim_id);
            }
            mSimId = sim_id;
        }
    }
    private class UnInstallThread implements Runnable {
        private int mSimId = -1;
        @Override
        public void run() {
            if ((sSimCount - 2) == mSimId) {
                CatLog.d(LOG_TAG, "UninstallThread run, sim id:" + STK_LAUNCH_ID);
                setAppState(mContext, false, STK_LAUNCH_ID);
            } else if ((sSimCount - 1) == mSimId) {
                CatLog.d(LOG_TAG, "UninstallThread run, sim id:" + STK_MENU_LAUNCH_ID);
                setAppState(mContext, false, STK_MENU_LAUNCH_ID);
            } else if (SubscriptionManager.isValidSlotId(mSimId)) {
                CatLog.d(LOG_TAG, "UninstallThread run, sim id:" + mSimId);
                setAppState(mContext, false, mSimId);
            }
        }

        public void setSim(int sim_id) {
            if ((sSimCount - 2) == sim_id) {
                CatLog.d(LOG_TAG, "UninstallThread setSim sim id:" + STK_LAUNCH_ID);
            } else if ((sSimCount - 1) == sim_id) {
                CatLog.d(LOG_TAG, "UninstallThread setSim sim id:" + STK_MENU_LAUNCH_ID);
            } else {
                CatLog.d(LOG_TAG, "UninstallThread setSim sim id:" + sim_id);
            }
            mSimId = sim_id;
        }
    }
    private InstallThread[] installThread = null;// new InstallThread[StkAppService.STK_GEMINI_SIM_NUM];
    private UnInstallThread[] uninstallThread = null;//new UnInstallThread[StkAppService.STK_GEMINI_SIM_NUM];

    public static int getIsInstalled(int sim_id) {
        if (STK_LAUNCH_ID == sim_id) {
            sim_id = (sSimCount - 2);
            CatLog.d(LOG_TAG, "getIsInstalled, sim id:" + STK_LAUNCH_ID +
            " miSTKInstalled:" + miSTKInstalled[sim_id]);
        } else if (STK_MENU_LAUNCH_ID == sim_id) {
            sim_id = (sSimCount - 1);
            CatLog.d(LOG_TAG, "getIsInstalled, sim id:" + STK_MENU_LAUNCH_ID +
            " miSTKInstalled:" + miSTKInstalled[sim_id]);
        } else {
            CatLog.d(LOG_TAG, "getIsInstalled, sim id:" + sim_id +
            " miSTKInstalled:" + miSTKInstalled[sim_id]);
        }
        return miSTKInstalled[sim_id];
    }
}
