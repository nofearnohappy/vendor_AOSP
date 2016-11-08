/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
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

/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.mediatek.voiceextension;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.mediatek.common.voiceextension.IVoiceExtCommandListener;
import com.mediatek.common.voiceextension.IVoiceExtCommandManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Provides user application the ability to recognize voice commands and reject
 * unlike sound/noise. Once the voice is recognized successfully, listener can
 * be notified of this event with supplementary information.
 *
 */
public class VoiceCommandManager {

    private static final String TAG = "VieCmdMgr";

    private static VoiceCommandManager sMgrSelf;

    private IVoiceExtCommandListener.Stub mCallback = new Token();

    private IVoiceExtCommandManager mService;

    private String mCurSetName;

    private VoiceCommandListener mCurListener;

    private final String mServiceName = "vie_command";

    private DeathMonitor mServiceMonitor = new DeathMonitor();
    /**
     * @hide Used to send an async message while exception happend during the
     *       IPC progress
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (mCurListener != null) {
                switch (msg.what) {
                case VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET:
                    mCurListener.onSetCommands(msg.arg1);
                    break;
                case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_START:
                    mCurListener.onStartRecognition(msg.arg1);
                    break;
                case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_PAUSE:
                    mCurListener.onPauseRecognition(msg.arg1);
                    break;
                case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESUME:
                    mCurListener.onResumeRecognition(msg.arg1);
                    break;
                case VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_STOP:
                    mCurListener.onStopRecognition(msg.arg1);
                    break;
                default:
                    break;
                }
            }
        }
    };

    /**
     * Gets the instance of VoiceCommandManager.
     *
     * @return VoiceCommandManager instance
     *
     */
    public static VoiceCommandManager getInstance() {

        if (sMgrSelf == null) {
            synchronized (VoiceCommandManager.class) {
                if (null == sMgrSelf) {
                    sMgrSelf = new VoiceCommandManager();
                }
            }
        }
        return (sMgrSelf != null && sMgrSelf.getService() != null) ? sMgrSelf : null;
    }

    /**
     * @hide
     */
    private VoiceCommandManager() {
        if (getService() == null) {
            Log.e(TAG, "Can't get vie command service, while init self");
        }
    }

    /**
     * @hide
     * @return
     */
    private IVoiceExtCommandManager getService() {
        if (mService == null) {
            if (mServiceName != null && mServiceMonitor != null) {
                mService = IVoiceExtCommandManager.Stub
                        .asInterface(ServiceManager.getService(mServiceName));
                if (mService != null) {
                    try {
                        mService.asBinder().linkToDeath(mServiceMonitor, 0);
                        // ( mService).linkToDeath(mServiceMonitor, 0);
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        mService = null;
                        Log.e(TAG, "Get voice service " + mServiceName
                                + " error " + e.toString());
                    }
                } else {
                    Log.e(TAG, "Get voice service " + mServiceName + " error "
                            + mService);
                }
            }

            // Register the died notification
            if (mService != null) {
                try {
                    int result = mService.registerListener(mCallback);
                    if (result != VoiceCommonState.RET_COMMON_SUCCESS) {
                        Log.e(TAG, "Register Listener error " + result);
                        handleServiceDisconnected(false);
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    Log.e(TAG, "Register Listener " + mServiceName + " error "
                            + e.toString());
                    handleServiceDisconnected(false);
                }
            }
        }

        return mService;
    }

    /**
     * @hide
     * @param notifyApp
     */
    private void handleServiceDisconnected(boolean notifyApp) {
        // If mService is null, we needn't to unlinkToDeath
        if (mService != null) {
            mService.asBinder().unlinkToDeath(mServiceMonitor, 0);
        }
        mService = null;
        // mCurrentState = STATE_IDLE;
        // mListenerRegistered = false;

        if (notifyApp && mCurListener != null) {
            mCurListener.onError(VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED);
        }

        mCurSetName = null;
        mCurListener = null;

    }

    /**
     * Creates a command set for commands operation(setting commands or commands
     * recognition). If this method doesn't return
     * {@link VoiceCommandResult#SUCCESS} or
     * {@link VoiceCommandResult#COMMANDSET_ALREADY_EXIST}, the third-party
     * application can't call any other method with this command set.
     * <p>
     * One application can create multiple command sets.
     *
     * @param name
     *            the command set of the application created. The name cannot
     *            contain other characters except English letters and Arabic
     *            numerals.
     * @param name
     *            the command set of the application created. The name cannot
     *            contain other characters except English letters and Arabic
     *            numerals. The length is limited to 32.
     * @return {@link VoiceCommandResult#SUCCESS},
     *         {@link VoiceCommandResult#COMMANDSET_ALREADY_EXIST},
     *         {@link VoiceCommandResult#COMMANDSET_NAME_LENGTH_EXCEED_LIMIT},
     *         {@link VoiceCommandResult#COMMANDSET_NAME_ILLEGAL},
     *         {@link VoiceCommandResult#WRITE_STORAGE_FAIL},
     *         {@link VoiceCommandResult#FAILURE},
     *         {@link VoiceCommandResult#SERVICE_NOT_EXIST},
     *         {@link VoiceCommandResult#SERVICE_DISCONNECTTED}
     *
     */
    public int createCommandSet(String name) {

        int result = checkSetFormat(name);

        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            if (getService() != null) {
                try {
                    result = mService.createCommandSet(name);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    result = VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED;
                    handleServiceDisconnected(false);
                    Log.e(TAG, "create Command Set error " + e.toString());
                }
            } else {
                result = VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST;
            }
        }

        return result;
    }

    /**
     * Checks if the command set already exists.
     *
     * @param name
     *            the command set name
     * @return
     *         {@link VoiceCommandResult#COMMANDSET_ALREADY_EXIST},
     *         {@link VoiceCommandResult#COMMANDSET_NOT_EXIST},
     *         {@link VoiceCommandResult#FAILURE},
     *         {@link VoiceCommandResult#SERVICE_NOT_EXIST},
     *         {@link VoiceCommandResult#SERVICE_DISCONNECTTED}
     */
    public int isCommandSetCreated(String name) {

        int result = checkSetFormat(name);

        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {

            if (getService() != null) {
                try {
                    result = mService.isCommandSetCreated(name);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    result = VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED;
                    handleServiceDisconnected(false);
                    Log.e(TAG, "check Command Set error " + e.toString());
                }
            } else {
                result = VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST;
            }
        }

        return result;
    }

    /**
     * Deletes the command set if it's no longer needed.
     *
     * @param name
     *            command set name
     * @return {@link VoiceCommandResult#SUCCESS},
     *         {@link VoiceCommandResult#COMMANDSET_OCCUPIED},
     *         {@link VoiceCommandResult#FAILURE},
     *         {@link VoiceCommandResult#SERVICE_NOT_EXIST},
     *         {@link VoiceCommandResult#SERVICE_DISCONNECTTED}
     */
    public int deleteCommandSet(String name) {

        int result = checkSetFormat(name);

        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {

            if (getService() != null) {
                try {
                    result = mService.deleteCommandSet(name);
                    if (name.equals(mCurSetName)
                            && result == VoiceCommonState.RET_COMMON_SUCCESS) {
                        mCurSetName = null;
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    result = VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED;
                    handleServiceDisconnected(false);
                    Log.e(TAG, "delete Command Set error " + e.toString());
                }
            } else {
                result = VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST;
            }
        }
        return result;
    }

    /**
     * Gets the current selected command set which was set in
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     *
     * @return the current selected command set name
     *
     */
    public String getCommandSetSelected() {

        return mCurSetName;
    }

    /**
     * Selects a command set for setting up commands or command recognition
     * operation.
     * <p>
     * This method must be called before {@link #startRecognition()},
     * {@link #setCommands(File)},{@link #setCommands(String[])}.
     *
     * @param name
     *            the command set name which is created
     * @param listener
     *            a callback that receive asynchronous notification from voice
     *            command service
     * @return {@link VoiceCommandResult#SUCCESS},
     *         {@link VoiceCommandResult#COMMANDSET_ALREADY_SELECTED},
     *         {@link VoiceCommandResult#COMMANDSET_NOT_EXIST},
     *         {@link VoiceCommandResult#MIC_OCCUPIED},
     *         {@link VoiceCommandResult#LISTENER_ILLEGAL},
     *         {@link VoiceCommandResult#SERVICE_NOT_EXIST},
     *         {@link VoiceCommandResult#SERVICE_DISCONNECTTED}
     *
     */
    public int selectCurrentCommandSet(String name,
            VoiceCommandListener listener) {

        int result = checkSetFormat(name);
        if (result == VoiceCommonState.RET_COMMON_SUCCESS) {
            if (listener == null) {
                result = VoiceCommonState.RET_COMMON_LISTENER_ILLEGAL;
            } else if (getService() == null) {
                result = VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST;
            } else {
                if (!name.equals(mCurSetName)) {
                    try {
                        result = mService.selectCurrentCommandSet(name);
                        if (result == VoiceCommonState.RET_COMMON_SUCCESS
                                || result == VoiceCommonState.RET_SET_SELECTED) {
                            mCurSetName = name;
                            // Any time , we need to change the listener if the
                            // set is selected by service
                            mCurListener = listener;
                        }
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        result = VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED;
                        handleServiceDisconnected(false);
                        Log.e(TAG, "select Command Set error " + e.toString());
                    }
                } else {
                    result = VoiceCommonState.RET_SET_SELECTED;
                    // Do nothing because the command set already selected
                }
            }
        }
        return result;
    }

    /**
     * Sets up the commands list to voice command service. This API is only for
     * tests. To achieve a much better performance especially for formally
     * published Apps, be sure to acquire the customization file by using the
     * service we provide or contact us.
     *
     * This function can only be called after
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     * <p>
     * The result will be notified by
     * {@link VoiceCommandListener#onSetCommands(int)}.
     *
     * @param commands
     *            commands list to be set to voice command service. The size of
     *            string array must be less than 5 and the length of each
     *            command must be less than 10 characters.
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public void setCommands(String[] commands) throws IllegalAccessException {

        if (mCurSetName != null && mCurListener != null) {
            // mCurrentState = STATE_COMMANDS_SET;
            if (getService() != null) {
                try {
                    mService.setCommandsStrArray(commands);
                } catch (RemoteException e) {
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    Log.e(TAG, "set Commands error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }
        } else {
            throw new IllegalAccessException("Command set wasn't selected ");
        }
    }

    /**
     * Sets up the file path of commands to voice command service. This function
     * can only be called after
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     * <p>
     * The result will be notified by
     * {@link VoiceCommandListener#onSetCommands(int)}.
     * <p>
     * The file for customization is available via the official website
     * (http://www.mediatek.com/en/products/hero-products/voice-interface-extension-sdk/).
     * Through the latest customization algorithm on the website,
     * the performance of VIE can be improved by using the generated file.
     *
     * @param file
     *            file that contains the commands
     * @throws FileNotFoundException
     *             if the given file does not exist or can not be opened with
     *             the requested mode
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public void setCommands(File file) throws FileNotFoundException,
            IllegalAccessException {
        // if (mCurrentState != VoiceCommonState.VIE) {
        if (mCurSetName != null && mCurListener != null) {
            if (getService() != null) {
                ParcelFileDescriptor pFd = ParcelFileDescriptor.open(file,
                        ParcelFileDescriptor.MODE_READ_ONLY);
                int length = (int) pFd.getStatSize();
                try {
                    mService.setCommandsFile(pFd, 0, length);
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    Log.e(TAG, "set Commands error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }
        } else {
            throw new IllegalAccessException("Command set wasn't selected ");
        }
    }

    /**
     * Sets up the assets file path of commands to voice command service. This
     * function can only be called after
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     * <p>
     * The result will be notified by
     * {@link VoiceCommandListener#onSetCommands(int)}.
     * <p>
     * The file for customization is available via the official website
     * (http://www.mediatek.com/en/products/hero-products/voice-interface-extension-sdk/).
     * Through the latest customization algorithm on the website,
     * the performance of VIE can be improved by using the generated file.
     *
     * @param context
     *            the Context in which this voice recognition is running
     * @param assetsFilePath
     *            the path under the assets folder to open, and it can be
     *            hierarchical. You can set it as example: vie/cammands.xmf (it
     *            will reference to assets/vie/commands.xmf in the application
     *            package)
     * @throws IOException
     *             if the given file path does not exist
     * @throws IllegalAccessException
     *             when command set was not selected
     */
    public void setCommands(Context context, String assetsFilePath)
            throws IOException, IllegalAccessException {
        Log.i(TAG, "setCommands parament context:" + context
                + ", assetsFilePath:" + assetsFilePath);
        if (context == null || assetsFilePath == null) {
            throw new IllegalAccessException("setCommands parament was null");
        }

        if (mCurSetName != null && mCurListener != null) {
            if (getService() != null) {
                ParcelFileDescriptor pFd = null;
                try {
                    AssetManager assetManager = context.getAssets();
                    AssetFileDescriptor fp = assetManager
                            .openFd(assetsFilePath);
                    pFd = fp.getParcelFileDescriptor();
                    int offset = (int) fp.getStartOffset();
                    int length = (int) fp.getLength();
                    if (pFd != null) {
                        mService.setCommandsFile(pFd, offset, length);
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    try {
                        pFd.close();
                    } catch (IOException ex) {
                        // TODO: handle exception
                        Log.e(TAG, "pFd close exception: " + ex.getMessage());
                    }
                    Log.e(TAG, "set Commands error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }
        } else {
            throw new IllegalAccessException("Command set wasn't selected");
        }
    }

    /**
     * Sets up res/raw id of commands to voice command service. This function
     * can only be called after
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     * <p>
     * The result will be notified by
     * {@link VoiceCommandListener#onSetCommands(int)}.
     * <p>
     * The file for customization is available via the official website
     * (http://www.mediatek.com/en/products/hero-products/voice-interface-extension-sdk/).
     * Through the latest customization algorithm on the website,
     * the performance of VIE can be improved by using the generated file.
     *
     * @param context
     *            the Context in which this voice recognition is running
     * @param resid
     *            the resource identifier to open, as generated by the appt
     *            tool. You can set it as example: R.raw.cammands (it will
     *            reference to res/raw/commands.xmf in the application package)
     * @throws NotFoundException
     *             if the given ID does not exist
     * @throws IllegalAccessException
     *             when command set was not selected
     */
    public void setCommands(Context context, int resid)
            throws NotFoundException, IllegalAccessException {
        Log.i(TAG, "setCommands parament context:" + context + ", resid:"
                + resid);
        if (context == null || resid == 0) {
            throw new IllegalAccessException("setCommands parament was null");
        }

        if (mCurSetName != null && mCurListener != null) {
            if (getService() != null) {
                ParcelFileDescriptor pFd = null;
                try {
                    Resources resource = context.getResources();
                    AssetFileDescriptor fp = resource.openRawResourceFd(resid);
                    pFd = fp.getParcelFileDescriptor();
                    int offset = (int) fp.getStartOffset();
                    int length = (int) fp.getLength();
                    if (pFd != null) {
                        mService.setCommandsFile(pFd, offset, length);
                    }
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    try {
                        pFd.close();
                    } catch (IOException ex) {
                        // TODO: handle exception
                        Log.e(TAG, "pFd close exception: " + ex.getMessage());
                    }
                    Log.e(TAG, "set Commands error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }
        } else {
            throw new IllegalAccessException("Command set wasn't selected ");
        }
    }

    /**
     * Requests voice command service to open microphone for voice capture and
     * start command recognition .This function can only be called after
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     * <p>
     * The starting result will be notified by
     * {@link VoiceCommandListener#onStartRecognition(int)}.
     * <p>
     * The Recognition result will be notified by
     * {@link VoiceCommandListener#onCommandRecognized(int, int, String)}.
     *
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public void startRecognition() throws IllegalAccessException {

        if (mCurSetName != null && mCurListener != null) {

            if (getService() != null) {
                try {
                    mService.startRecognition();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_START,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    Log.e(TAG, "start recognition error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_COMMANDS_SET,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }

        } else {
            // We need to select set firstly
            throw new IllegalAccessException("Command set wasn't selected ");
        }

    }

    /**
     * Requests voice command service to close microphone and stop command
     * recognition.
     * <p>
     * This function can only be called after {@link #startRecognition()}.
     * <p>
     * The stopping result will be notified by
     * {@link VoiceCommandListener#onStopRecognition(int)}.
     *
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public void stopRecognition() throws IllegalAccessException {
        if (mCurSetName != null && mCurListener != null) {
            if (getService() != null) {
                try {
                    mService.stopRecognition();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_STOP,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    Log.e(TAG, "stop recognition error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_STOP,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }
        } else {
            // We need to select set firstly
            throw new IllegalAccessException("Command set wasn't selected ");
        }
    }

    /**
     * Requests voice command service to pause command recognition and the
     * microphone is still opened.
     * <p>
     * This function can only be called after {@link #startRecognition()}, and
     * can't been called after recognition stops {@link #stopRecognition()}.
     * <p>
     * The pausing result will be notified by
     * {@link VoiceCommandListener#onPauseRecognition(int)}.
     *
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public void pauseRecognition() throws IllegalAccessException {
        if (mCurSetName != null && mCurListener != null) {
            if (getService() != null) {
                try {
                    mService.pauseRecognition();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_PAUSE,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0,
                            mCurListener));
                    handleServiceDisconnected(false);
                    Log.e(TAG, "stop recognition error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_PAUSE,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0, mCurListener));
            }
        } else {
            // We need to select set firstly
            throw new IllegalAccessException("Command set wasn't selected ");
        }
    }

    /**
     * Requests voice command service to resume command recognition after
     * {@link #pauseRecognition()} succeeds.
     *
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public void resumeRecognition() throws IllegalAccessException {
        if (mCurSetName != null && mCurListener != null) {
            if (getService() != null) {
                try {
                    mService.resumeRecognition();
                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    mHandler.sendMessage(mHandler.obtainMessage(
                            VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESUME,
                            VoiceCommonState.RET_COMMON_SERVICE_DISCONNECTTED, 0));
                    handleServiceDisconnected(false);
                    Log.e(TAG, "stop recognition error " + e.toString());
                }
            } else {
                mHandler.sendMessage(mHandler.obtainMessage(
                        VoiceCommonState.API_TYPE_COMMAND_RECOGNITION_RESUME,
                        VoiceCommonState.RET_COMMON_SERVICE_NOT_EXIST, 0));
            }
        } else {
            // We need to select set firstly
            throw new IllegalAccessException("Command set wasn't selected ");
        }
    }

    /**
     * Gets the commands list set by function {@link #setCommands(String[])} or
     * {@link #setCommands(File)}.
     * <p>
     * This function can only be called after
     * {@link #selectCurrentCommandSet(String, VoiceCommandListener)}.
     *
     * @return commands list of the current selected command set
     *
     * @throws IllegalAccessException
     *             when command set was not selected
     *
     */
    public String[] getCommands() throws IllegalAccessException {

        String[] commands = null;

        if (mCurSetName != null && mCurListener != null) {

            if (getService() != null) {
                try {
                    commands = mService.getCommands();

                } catch (RemoteException e) {
                    // TODO Auto-generated catch block
                    handleServiceDisconnected(false);
                    Log.e(TAG, "get commands error " + e.toString());
                }
            } else {
                Log.e(TAG, "get commands service isn't exist ");
            }

        } else {
            throw new IllegalAccessException("command set wasn't selected ");
        }

        return commands;
    }

    /**
     * Gets all command sets. Application can query all command sets created by
     * {@link #createCommandSet(String)}.
     *
     * @return command set list of the application created
     *
     */
    public String[] getCommandSets() {

        String[] commandSets = null;

        if (getService() != null) {

            try {
                commandSets = mService.getCommandSets();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                handleServiceDisconnected(false);
                Log.e(TAG, "get command sets error " + e.toString());
            }

        }

        return commandSets;
    }

    /**
     * @hide
     *
     */
    class Token extends IVoiceExtCommandListener.Stub {

        // int mCurrentState ;

        @Override
        public void onCommandRecognized(int retCode, int commandId,
                String commandStr) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onCommandRecognized result retCode="
                    + retCode + " commandId=" + commandId + " commandStr="
                    + commandStr);
            if (mCurListener != null) {
                mCurListener.onCommandRecognized(retCode, commandId,
                        commandStr);
            } else {
                // drop the result message if listener error
                Log.e(TAG, "onCommandRecognized drop result");
            }
        }

        @Override
        public void onError(int retCode) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onError result=" + retCode);
            if (mCurListener != null) {
                mCurListener.onError(retCode);
            } else {
                // drop the result message if listener error
                Log.e(TAG, "onError drop result ");
            }
        }

        @Override
        public void onPauseRecognition(int retCode) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onPauseRecognition result=" + retCode);
            if (mCurListener != null) {
                mCurListener.onPauseRecognition(retCode);
            } else {
                // drop the result message if listener error
                Log.e(TAG, "onPauseRecognition drop result retCode=" + retCode);
            }
        }

        @Override
        public void onResumeRecognition(int retCode) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onResumeRecognition result=" + retCode);
            if (mCurListener != null) {
                mCurListener.onResumeRecognition(retCode);
            } else {
                // drop the result message if listener error
                Log
                        .e(TAG, "onResumeRecognition drop result retCode="
                                + retCode);
            }
        }

        @Override
        public void onSetCommands(int retCode) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onSetCommands result=" + retCode);
            if (mCurListener != null) {
                mCurListener.onSetCommands(retCode);
            } else {
                // drop the result message if listener error
                Log.e(TAG, "onSetCommands drop result retCode=" + retCode);
            }
        }

        @Override
        public void onStartRecognition(int retCode) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onStartRecognition result=" + retCode);
            if (mCurListener != null) {
                mCurListener.onStartRecognition(retCode);
            } else {
                // drop the result message if listener error
                Log.e(TAG, "onStartRecognition drop result retCode=" + retCode);
            }
        }

        @Override
        public void onStopRecognition(int retCode) throws RemoteException {
            // TODO Auto-generated method stub
            Log.i(TAG, "onStopRecognition result=" + retCode);
            if (mCurListener != null) {
                mCurListener.onStopRecognition(retCode);
            } else {
                // drop the result message if listener error
                Log.e(TAG, "onStopRecognition drop result retCode=" + retCode);
            }
        }
    }

    /**
     * @hide
     *
     */
    private final class DeathMonitor implements IBinder.DeathRecipient {

        @Override
        public void binderDied() {
            // TODO Auto-generated method stub
            handleServiceDisconnected(true);
            Log.e(TAG, "DeathMonitor, exception happened int VIE Service!");
        }

    }

    private int checkSetFormat(String setName) {
        int result = setName == null ? VoiceCommonState.RET_SET_ILLEGAL
                : VoiceCommonState.RET_COMMON_SUCCESS;
        // Need to check the setName length , character format and so on

        return result;

    }

}