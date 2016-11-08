/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 *
 * MediaTek Inc. (C) 2013. All rights reserved.
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

package com.mediatek.drm;

import android.content.ContentValues;
import android.content.Context;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
import android.drm.DrmManagerClient;
import android.drm.DrmInfo;
import android.drm.DrmInfoRequest;
import android.drm.DrmInfoStatus;
import android.drm.DrmRights;
import android.graphics.Bitmap;
import android.os.SystemProperties;

import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The main programming interface for OMA DRM v1.0 feature: Mediatek's DRM framework proxy
 * An application dealing with OMA DRM v1.0 need to instantiate this class and
 * access proper API methods.
 */
public class OmaDrmClient {
    /**
     * Indicates that a request was successful or that no error occurred.
     * @internal
     */
    public static final int ERROR_NONE = DrmManagerClient.ERROR_NONE;
    /**
     * Indicates that an error occurred and the reason is not known.
     * @internal
     */
    public static final int ERROR_UNKNOWN = DrmManagerClient.ERROR_UNKNOWN;

    private static final String TAG = "OmaDrmClient";

    private DrmManagerClient mDrmManagerClient;
    private Context mContext;

    // the dialog array list to deal with dialog UI operation
    public static ArrayList<OmaDrmUiUtils.CustomAlertDialog> sSecureTimerDialogQueue =
        new ArrayList<OmaDrmUiUtils.CustomAlertDialog>();
    public static ArrayList<OmaDrmUiUtils.CustomAlertDialog> sConsumeDialogQueue =
        new ArrayList<OmaDrmUiUtils.CustomAlertDialog>();
    public static ArrayList<OmaDrmUiUtils.CustomAlertDialog> sProtectionInfoDialogQueue =
        new ArrayList<OmaDrmUiUtils.CustomAlertDialog>();
    public static ArrayList<OmaDrmUiUtils.CustomAlertDialog> sLicenseDialogQueue =
        new ArrayList<OmaDrmUiUtils.CustomAlertDialog>();

    private static boolean sIsOmaDrmSupport = false;

    //for cta
    private static boolean sIsCtaDrmSupport = false;

    static {
        sIsOmaDrmSupport = (SystemProperties.getInt("ro.mtk_oma_drm_support", 0) == 1) ? true : false;
        sIsCtaDrmSupport = (SystemProperties.getInt("ro.mtk_cta_drm_support", 0) == 1) ? true : false;
    }

    /**
     * Creates a OmaDrmClient.
     *
     * @param context Context of the caller.
     */
    public OmaDrmClient(Context context) {
        Log.d(TAG, "create OmaDrmClient instance");
        mContext = context;
        mDrmManagerClient = new DrmManagerClient(context);
    }

    //Remove finalize method
   /* protected void finalize() {
        Log.d(TAG, "finalize OmaDrmClient instance");
    }*/

    // M: @{
    // ALPS00772785, add release() function
    /**
     * Releases resources associated with the current session of DrmManagerClient.
     *
     * It is considered good practice to call this method when the {@link DrmManagerClient} object
     * is no longer needed in your application. After release() is called,
     * {@link DrmManagerClient} is no longer usable since it has lost all of its required resource.
     * @internal
     */
    public void release() {
        Log.d(TAG, "release OmaDrmClient instance");
        if (mDrmManagerClient != null) {
            mDrmManagerClient.release();
        }
    }
    // M: @}

    /**
     * Factory: get a newly created OmaDrmClient instance.
     *
     * @param context Context of the caller.
     * @return OmaDrmClient
     */
    public static OmaDrmClient newInstance(Context context) {
        Log.d(TAG, "new OmaDrmClient instance");
        return new OmaDrmClient(context);
    }

    /**
     * Get the internal reference to generic DrmManagerClient instance
     *
     * @return android.drm.DrmManagerClient
     */
    public DrmManagerClient getDrmClient() {
        return mDrmManagerClient;
    }

    /**
     * Get the application context where the DrmManagerClient instance was created
     *
     * @return The application Context
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Retrieves information about all the DRM plug-ins (agents) that are registered with
     * the DRM framework.
     *
     * @return A <code>String</code> array of DRM plug-in descriptions.
     */
    public String[] getAvailableDrmEngines() {
        return mDrmManagerClient.getAvailableDrmEngines();
    }

    /**
     * Retrieves constraint information for rights-protected content.
     *
     * @param path Path to the content from which you are retrieving DRM constraints.
     * @param action Action defined in {@link DrmStore.Action}.
     *
     * @return A {@link android.content.ContentValues} instance that contains
     * key-value pairs representing the constraints. Null in case of failure.
     * The keys are defined in {@link DrmStore.ConstraintsColumns}.
     * @internal
     */
    public ContentValues getConstraints(String path, int action) {
        return mDrmManagerClient.getConstraints(path, action);
    }

   /**
    * Retrieves metadata information for rights-protected content.
    *
    * @param path Path to the content from which you are retrieving metadata information.
    *
    * @return A {@link android.content.ContentValues} instance that contains
    * key-value pairs representing the metadata. Null in case of failure.
    */
    public ContentValues getMetadata(String path) {
        return mDrmManagerClient.getMetadata(path);
    }

    /**
     * Retrieves constraint information for rights-protected content.
     *
     * @param uri URI for the content from which you are retrieving DRM constraints.
     * @param action Action defined in {@link DrmStore.Action}.
     *
     * @return A {@link android.content.ContentValues} instance that contains
     * key-value pairs representing the constraints. Null in case of failure.
     * @internal
     */
    public ContentValues getConstraints(Uri uri, int action) {
        return getConstraints(uri, action);
    }

   /**
    * Retrieves metadata information for rights-protected content.
    *
    * @param uri URI for the content from which you are retrieving metadata information.
    *
    * @return A {@link android.content.ContentValues} instance that contains
    * key-value pairs representing the constraints. Null in case of failure.
    */
    public ContentValues getMetadata(Uri uri) {
        return mDrmManagerClient.getMetadata(uri);
    }

    /**
     * Saves rights to a specified path and associates that path with the content path.
     *
     * <p class="note"><strong>Note:</strong> For OMA or WM-DRM, <code>rightsPath</code> and
     * <code>contentPath</code> can be null.</p>
     *
     * @param drmRights The {@link DrmRights} to be saved.
     * @param rightsPath File path where rights will be saved.
     * @param contentPath File path where content is saved.
     *
     * @return ERROR_NONE for success; ERROR_UNKNOWN for failure.
     *
     * @throws IOException If the call failed to save rights information at the given
     * <code>rightsPath</code>.
     * @internal
     */
    public int saveRights(
            DrmRights drmRights, String rightsPath, String contentPath) throws IOException {
        return mDrmManagerClient.saveRights(drmRights, rightsPath, contentPath);
    }

    /**
     * Installs a new DRM plug-in (agent) at runtime.
     *
     * @param engineFilePath File path to the plug-in file to be installed.
     *
     * {@hide}
     */
    public void installDrmEngine(String engineFilePath) {
        mDrmManagerClient.installDrmEngine(engineFilePath);
    }

    /**
     * Checks whether the given MIME type or path can be handled.
     *
     * @param path Path of the content to be handled.
     * @param mimeType MIME type of the object to be handled.
     *
     * @return True if the given MIME type or path can be handled; false if they cannot be handled.
     * @internal
     */
    public boolean canHandle(String path, String mimeType) {
        return mDrmManagerClient.canHandle(path, mimeType);
    }

    /**
     * Checks whether the given MIME type or URI can be handled.
     *
     * @param uri URI for the content to be handled.
     * @param mimeType MIME type of the object to be handled
     *
     * @return True if the given MIME type or URI can be handled; false if they cannot be handled.
     * @internal
     */
    public boolean canHandle(Uri uri, String mimeType) {
        return mDrmManagerClient.canHandle(uri, mimeType);
    }

    /**
     * Processes the given DRM information based on the information type.
     *
     * @param drmInfo The {@link DrmInfo} to be processed.
     * @return ERROR_NONE for success; ERROR_UNKNOWN for failure.
     */
    public int processDrmInfo(DrmInfo drmInfo) {
        return mDrmManagerClient.processDrmInfo(drmInfo);
    }

    /**
     * Retrieves information for registering, unregistering, or acquiring rights.
     *
     * @param drmInfoRequest The {@link DrmInfoRequest} that specifies the type of DRM
     * information being retrieved.
     *
     * @return A {@link DrmInfo} instance.
     * @internal
     */
    public DrmInfo acquireDrmInfo(DrmInfoRequest drmInfoRequest) {
        return mDrmManagerClient.acquireDrmInfo(drmInfoRequest);
    }

    /**
     * Processes a given {@link DrmInfoRequest} and returns the rights information asynchronously.
     *<p>
     * This is a utility method that consists of an
     * {@link #acquireDrmInfo(DrmInfoRequest) acquireDrmInfo()} and a
     * {@link #processDrmInfo(DrmInfo) processDrmInfo()} method call. This utility method can be
     * used only if the selected DRM plug-in (agent) supports this sequence of calls. Some DRM
     * agents, such as OMA, do not support this utility method, in which case an application must
     * invoke {@link #acquireDrmInfo(DrmInfoRequest) acquireDrmInfo()} and
     * {@link #processDrmInfo(DrmInfo) processDrmInfo()} separately.
     *
     * @param drmInfoRequest The {@link DrmInfoRequest} used to acquire the rights.
     * @return ERROR_NONE for success; ERROR_UNKNOWN for failure.
     */
    public int acquireRights(DrmInfoRequest drmInfoRequest) {
        return mDrmManagerClient.acquireRights(drmInfoRequest);
    }

    /**
     * Retrieves the type of rights-protected object (for example, content object, rights
     * object, and so on) using the specified path or MIME type. At least one parameter must
     * be specified to retrieve the DRM object type.
     *
     * @param path Path to the content or null.
     * @param mimeType MIME type of the content or null.
     *
     * @return An <code>int</code> that corresponds to a {@link DrmStore.DrmObjectType}.
     * @internal
     */
    public int getDrmObjectType(String path, String mimeType) {
        return mDrmManagerClient.getDrmObjectType(path, mimeType);
    }

    /**
     * Retrieves the type of rights-protected object (for example, content object, rights
     * object, and so on) using the specified URI or MIME type. At least one parameter must
     * be specified to retrieve the DRM object type.
     *
     * @param uri URI for the content or null.
     * @param mimeType MIME type of the content or null.
     *
     * @return An <code>int</code> that corresponds to a {@link DrmStore.DrmObjectType}.
     * @internal
     */
    public int getDrmObjectType(Uri uri, String mimeType) {
        return mDrmManagerClient.getDrmObjectType(uri, mimeType);
    }

    /**
     * Retrieves the MIME type embedded in the original content.
     *
     * @param path Path to the rights-protected content.
     *
     * @return The MIME type of the original content, such as <code>video/mpeg</code>.
     * @internal
     */
    public String getOriginalMimeType(String path) {
        return mDrmManagerClient.getOriginalMimeType(path);
    }

    /**
     * Retrieves the MIME type embedded in the original content.
     *
     * @param uri URI of the rights-protected content.
     *
     * @return MIME type of the original content, such as <code>video/mpeg</code>.
     * @internal
     */
    public String getOriginalMimeType(Uri uri) {
        return mDrmManagerClient.getOriginalMimeType(uri);
    }

    /**
     * Checks whether the given content has valid rights.
     *
     * @param path Path to the rights-protected content.
     *
     * @return An <code>int</code> representing the {@link DrmStore.RightsStatus} of the content.
     */
    public int checkRightsStatus(String path) {
        return mDrmManagerClient.checkRightsStatus(path);
    }

    /**
     * Check whether the given content has valid rights.
     *
     * @param uri URI of the rights-protected content.
     *
     * @return An <code>int</code> representing the {@link DrmStore.RightsStatus} of the content.
     */
    public int checkRightsStatus(Uri uri) {
        return mDrmManagerClient.checkRightsStatus(uri);
    }

    /**
     * Checks whether the given rights-protected content has valid rights for the specified
     * {@link DrmStore.Action}.
     *
     * @param path Path to the rights-protected content.
     * @param action The {@link DrmStore.Action} to perform.
     *
     * @return An <code>int</code> representing the {@link DrmStore.RightsStatus} of the content.
     * @internal
     */
    public int checkRightsStatus(String path, int action) {
        Log.v(TAG, "checkRightsStatus : " + path + " with action " + action);
        int result = mDrmManagerClient.checkRightsStatus(path, action);
        Log.v(TAG, "checkRightsStatus : result " + result);
        /*
        if (result == OmaDrmStore.RightsStatus.SECURE_TIMER_INVALID) {
            Log.d(TAG, "checkRightsStatus : secure timer indicates invalid state");
            result = OmaDrmStore.RightsStatus.RIGHTS_INVALID;
        }*/
        return result;
    }

    /**
     * Checks whether the given rights-protected content has valid rights for the specified
     * {@link DrmStore.Action}.
     *
     * @param uri URI for the rights-protected content.
     * @param action The {@link DrmStore.Action} to perform.
     *
     * @return An <code>int</code> representing the {@link DrmStore.RightsStatus} of the content.
     * @internal
     */
    public int checkRightsStatus(Uri uri, int action) {
        Log.v(TAG, "checkRightsStatus : " + uri + " with action " + action);
        int result = mDrmManagerClient.checkRightsStatus(uri, action);
        Log.v(TAG, "checkRightsStatus : result " + result);
        /*
        if (result == OmaDrmStore.RightsStatus.SECURE_TIMER_INVALID) {
            Log.d(TAG, "checkRightsStatus : secure timer indicates invalid state");
            result = OmaDrmStore.RightsStatus.RIGHTS_INVALID;
        }*/
        return result;
    }

    /**
     * Removes the rights associated with the given rights-protected content.
     *
     * @param path Path to the rights-protected content.
     *
     * @return ERROR_NONE for success; ERROR_UNKNOWN for failure.
     */
    public int removeRights(String path) {
        return mDrmManagerClient.removeRights(path);
    }

    /**
     * Removes the rights associated with the given rights-protected content.
     *
     * @param uri URI for the rights-protected content.
     *
     * @return ERROR_NONE for success; ERROR_UNKNOWN for failure.
     */
    public int removeRights(Uri uri) {
        return mDrmManagerClient.removeRights(uri);
    }

    /**
     * Removes all the rights information of every DRM plug-in (agent) associated with
     * the DRM framework. Will be used during a master reset.
     *
     * @return ERROR_NONE for success; ERROR_UNKNOWN for failure.
     * @internal
     */
    public int removeAllRights() {
        return mDrmManagerClient.removeAllRights();
    }

    /**
     * Installing the drm message file (.dm).
     *
     * @param uri Uri of the downloaded protected content (FL, CD, FLSD) in .dm format
     * @return ERROR_NONE for success ERROR_UNKNOWN for failure
     */
    public int installDrmMsg(Uri uri) {
        Log.v(TAG, "installDrmMsg : " + uri);

        if (null == uri || Uri.EMPTY == uri) {
            Log.e(TAG, "installDrmMsg : Given uri is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }

        String path = null;
        try {
            path = OmaDrmUtils.convertUriToPath(mContext, uri);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException @installDrmMsg : " + e.getMessage());
            return DrmManagerClient.ERROR_UNKNOWN;
        }
        return installDrmMsg(path);
    }

    /**
     * Installing the drm message file (.dm).
     *
     * @param path Path of the downloaded protected content (FL, CD, FLSD) in .dm format
     * @return ERROR_NONE for success ERROR_UNKNOWN for failure
     * @internal
     */
    public int installDrmMsg(String path) {
        Log.v(TAG, "installDrmMsg : " + path);

        /*if (null == path || path.equals("")) {
            Log.e(TAG, "installDrmMsg : Given path is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }

        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_INSTALL_DRM_MSG);
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA, path); // path

        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);

        // get message from returned DrmInfo
        byte[] data = info.getData();
        String message = "";
        if (null != data) {
            try {
                // the information shall be in format of ASCII string
                message = new String(data, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type of the returned DrmInfo data");
                message = "";
            }
        }
        Log.v(TAG, "installDrmMsg : >" + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                DrmManagerClient.ERROR_NONE : DrmManagerClient.ERROR_UNKNOWN;*/
        return installDrmMsg(path, true);
    }


    /**
     * Installing the drm message file By Fd (.dm).
     *
     * @param path Path of the downloaded protected content (FL, CD, FLSD) in .dm format
     * @param useFd true or false will use fd to install
     * @return ERROR_NONE for success ERROR_UNKNOWN for failure
     * @internal
     */
    public int installDrmMsg(String path, boolean useFd) {
        Log.v(TAG, "installDrmMsg FD path : " + path);

        if (null == path || path.equals("")) {
            Log.e(TAG, "installDrmMsg : Given path is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }
        DrmInfo info = null;
        RandomAccessFile dmStream = null;
        FileOutputStream dcfStream = null;
        FileDescriptor dmFd = null;
        FileDescriptor dcfFd = null;
        try {
            File dmFile = new File(path);
            if (dmFile.exists()) {
                dmStream = new RandomAccessFile(dmFile, "rw");
                dmFd = dmStream.getFD();
            }
            String dcfPath = OmaDrmUtils.generateDcfFilePath(path);
            Log.v(TAG, "installDrmMsg :dcfPathL: " + dcfPath);
            if (dcfPath == null) {
                Log.e(TAG, "installDrmMsg : dcfPath is " + dcfPath);
                if (dmStream != null) {
                    try {
                        dmStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, "close dm stream: I/O Exception: " + e.getMessage());
                    }
                }
                return DrmManagerClient.ERROR_UNKNOWN;
            }
            File dcfFile = new File(dcfPath);
            if (!dcfFile.exists()) {
                dcfFile.createNewFile();
            }
            if (dmFile.exists()) {
                dcfStream = new FileOutputStream(dcfFile);
                dcfFd = dcfStream.getFD();
            }

            // constructs the request and process it by acquireDrmInfo
            DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                    OmaDrmStore.DrmObjectMime.MIME_DRM_MESSAGE);
            request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_INSTALL_DRM_MSG_BY_FD);
            request.put(OmaDrmStore.DrmRequestKey.KEY_DM_FD, dmFd); // dm
                                                                                       // file
                                                                                       // descriptor
            request.put(OmaDrmStore.DrmRequestKey.KEY_DCF_FD, dcfFd);
            Log.d(TAG, "installDrmMsg FD:" + dmFd + "," + dcfFd);
            info = mDrmManagerClient.acquireDrmInfo(request);
            // delete file
            if (dmFile.exists()) {
                dmFile.delete();
            }
            if (dcfFile.exists()) {
                dcfFile.renameTo(new File(path));
            }

        } catch (IOException ioe) {
            // / M: Added for debug.
            Log.d(TAG, "getOriginalMimeType: File I/O exception: " + ioe.getMessage());
        } finally {
            if (dmStream != null) {
                try {
                    dmStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "close dm stream: I/O Exception: " + e.getMessage());
                }
            }
            if (dcfStream != null) {
                try {
                    dcfStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "close dcf stream: I/O Exception: " + e.getMessage());
                }
            }
        }

        // get message from returned DrmInfo
        byte[] data = null;
        if (info != null) {
            data = info.getData();
        }
        String message = "";
        if (null != data) {
            try {
                // the information shall be in format of ASCII string
                message = new String(data, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type of the returned DrmInfo data");
                message = "";
            }
        }
        Log.v(TAG, "installDrmMsg FD path: >" + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                DrmManagerClient.ERROR_NONE : DrmManagerClient.ERROR_UNKNOWN;
    }

    /**
     * Consume the rights associated with the given protected content
     *
     * @param uri Uri of the protected content
     * @param action The action it performs to use the content
     * @return ERROR_NONE for success ERROR_UNKNOWN for failure
     * @internal
     */
    public int consumeRights(Uri uri, int action) {
        Log.v(TAG, "consumeRights: " + uri + " with action " + action);

        if (null == uri || Uri.EMPTY == uri) {
            Log.e(TAG, "consumeRights : Given uri is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }
        if (!OmaDrmStore.Action.isValid(action)) {
            Log.e(TAG, "consumeRights : Given action is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }

        String path = null;
        try {
            path = OmaDrmUtils.convertUriToPath(mContext, uri);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException @consumeRights : " + e.getMessage());
            return DrmManagerClient.ERROR_UNKNOWN;
        }
        return consumeRights(path, action);
    }

   /**
     * Consume the rights associated with the given protected content
     *
     * @param path Path of the protected content
     * @param action The action it performs to use the content
     * @return ERROR_NONE for success ERROR_UNKNOWN for failure
     * @internal
     */
    public int consumeRights(String path, int action) {
        Log.v(TAG, "consumeRights : " + path + " with action " + action);

        if (null == path || path.equals("")) {
            Log.e(TAG, "consumeRights : Given path is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }
        if (!OmaDrmStore.Action.isValid(action)) {
            Log.e(TAG, "consumeRights : Given action is not valid");
            return DrmManagerClient.ERROR_UNKNOWN;
        }

        // constructs the request and process it by acquireDrmInfo
        DrmInfoRequest request =
            new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                               OmaDrmStore.DrmObjectMime.MIME_DRM_CONTENT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                    OmaDrmStore.DrmRequestAction.ACTION_CONSUME_RIGHTS);
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA, path); // path
        request.put(OmaDrmStore.DrmRequestKey.KEY_DATA_EXTRA_1, String.valueOf(action)); // action

        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);

        // get message from returned DrmInfo
        byte[] data = info.getData();
        String message = "";
        if (null != data) {
            try {
                // the information shall be in format of ASCII string
                message = new String(data, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported encoding type of the returned DrmInfo data");
                message = "";
            }
        }
        Log.v(TAG, "consumeRights : >" + message);

        return OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ?
                DrmManagerClient.ERROR_NONE : DrmManagerClient.ERROR_UNKNOWN;
    }

    /**
     * Get drm method from drm content
     *
     * @param uri Uri of the protected content
     * @return int OmaDrmStore.DrmMethod
     * @internal
     */
    public int getMethod(Uri uri) {
        Log.v(TAG, "getMethod : " + uri);

        ContentValues cv = mDrmManagerClient.getMetadata(uri);
        if (cv != null && cv.containsKey(OmaDrmStore.MetadataKey.META_KEY_METHOD)) {
            Integer m = cv.getAsInteger(OmaDrmStore.MetadataKey.META_KEY_METHOD);
            if (m != null) {
                return m.intValue();
            }
        }
        return OmaDrmStore.DrmMethod.METHOD_NONE;
    }

    /**
     * Get drm method from drm content
     *
     * @param path Path of the protected content
     * @return int OmaDrmStore.DrmMethod
     */
    public int getMethod(String path) {
        Log.v(TAG, "getMethod : " + path);

        ContentValues cv = mDrmManagerClient.getMetadata(path);
        if (cv != null && cv.containsKey(OmaDrmStore.MetadataKey.META_KEY_METHOD)) {
            Integer m = cv.getAsInteger(OmaDrmStore.MetadataKey.META_KEY_METHOD);
            if (m != null) {
                return m.intValue();
            }
        }
        return OmaDrmStore.DrmMethod.METHOD_NONE;
    }
    
    /**
     * when the RO file was downloaded / received, rescan corresponding DRM file
     * this can be used by Download Provider or Drm Provider module to process
     * separate delivery (SD) scenario
     *
     * @param context The application context
     * @param rights The DrmRights object it has received
     * @param callback OnDrmScanCompletedListener. may be null
     * @return ERROR_NONE for success ERROR_UNKNOWN for failure
     * @internal
     */
    public int rescanDrmMediaFiles(Context context, DrmRights rights,
            OmaDrmUtils.OnDrmScanCompletedListener callback) {
        // first we get the content-id. register a OnEventListener
        mDrmManagerClient.setOnEventListener(new getCidListener(context, callback));

        // constructs the DrmInfo and process it with processDrmInfo
        DrmInfo info =
            new DrmInfo(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                        rights.getData(), rights.getMimeType());
        info.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                 OmaDrmStore.DrmRequestAction.ACTION_GET_CONTENT_ID);

        int result = mDrmManagerClient.processDrmInfo(info);
        Log.d(TAG, "OmaDrmClient#rescanDrmMediaFiles: > " + result);

        return result;
    }
    
    /**
     * Check if OMA DRM feature is supported or not.
     *
     * @return true if OMA DRM feature is supported, otherwise return false
     * @internal
     */
    public static boolean isOmaDrmEnabled() {
        return sIsOmaDrmSupport;
    }

    private class getCidListener implements DrmManagerClient.OnEventListener {
        private Context mContext = null;
        private OmaDrmUtils.OnDrmScanCompletedListener mCallback = null;

        public getCidListener(Context context,
                OmaDrmUtils.OnDrmScanCompletedListener callback) {
            mContext = context;
            mCallback = callback;
        }

        public void onEvent(DrmManagerClient client, DrmEvent event) {
            DrmInfoStatus status =
                (DrmInfoStatus) (event.getAttribute(DrmEvent.DRM_INFO_STATUS_OBJECT));
            if (status != null) {
                String cid = OmaDrmUtils.getMsgFromInfoStatus(status);

                // then rescan the corresponding Drm media file(s)
                int result = OmaDrmUtils.rescanDrmMediaFiles(mContext, cid, mCallback);
                Log.d(TAG, "OmaDrmUtils.rescanDrmMediaFiles: > " + result);
            } else {
                Log.e(TAG, "getCidListener.onEvent, status is a null pointer");
            }
        }
    }

    ///////////////////////////////////////////
    //The bellow part is for CTA feature.
    /**
     * No error.
     */
    public static int CTA_ERROR_NONE = 0;
    /**
     * A normal error.
     */
    public static int CTA_ERROR_GENRIC = -1;
    /**
     * The key is wrong.
     */
    public static int CTA_ERROR_BADKEY = -2;
    /**
     * The space is not enough.
     */
    public static int CTA_ERROR_NOSPACE = -3;
    /**
     * A encrypt/decrypt process is done which cannot be cancled.
     */
    public static int CTA_ERROR_CANCEL = -5;
    /**
     * Input is invalid
     */
    public static int CTA_ERROR_INVALID_INPUT = -6;
    /**
     * Enrypt or Decrypt error.
     */
    public static int CTA_ERROR = -7;
    /**
     * Encrypt or Decrypt done.
     */
    public static int CTA_DONE = 100;
    /**
     * Encrypt or Decrypt canceled.
     */
    public static int CTA_CANCEL_DONE = 101;
    /**
     * Encrypt or Decrypt updating.
     */
    public static int CTA_UPDATING = 102;

    /**
     * Multimedia file encrypt done.
     */
    public static int CTA_MULTI_MEDIA_ENCRYPT_DONE = 110;

    /**
     * Multimedia file decrypt done.
     */
    public static int CTA_MULTI_MEDIA_DECRYPT_DONE = 111;

    /**
     * Encrypt clear file to chipher file. It's a asynchronize method.
     *
     * @param clear_fd A file descriptor used to be encrypted
     * @param cipher_fd A file descriptor used to be stored chipher content
     * @return {@link CTA_ERROR_NONE} if encrypt successfully,
     *         otherwise return {@link CTA_ERROR_NOSPACE}, {@link CTA_ERROR_CANCLED_BY_USER}
     */
    public int encrypt(FileDescriptor clear_fd, FileDescriptor cipher_fd) {
        Log.d(TAG, "encrypt() : cipher_fd = " + cipher_fd + ", clear_fd = " + clear_fd);
        if (clear_fd == null || cipher_fd == null) {
            Log.e(TAG, "encrypt bad input parameters");
            return CTA_ERROR_INVALID_INPUT;
        }
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_ENCRYPT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CLEAR_FD, clear_fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CIPHER_FD, cipher_fd);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        return CTA_ERROR_NONE;
    }

    /**
     * Encrypt clear file to chipher file. It's a asynchronize method.
     *
     * @param clear_fd A file descriptor used to be encrypted
     * @param cipher_fd A file descriptor used to be stored chipher content
     * @param mime The mime type of clear file
     * @return {@link CTA_ERROR_NONE} if encrypt successfully,
     *         otherwise return {@link CTA_ERROR_NOSPACE}, {@link CTA_ERROR_CANCLED_BY_USER}
     */
    public int encrypt(FileDescriptor clear_fd, FileDescriptor cipher_fd, String mime) {
        Log.d(TAG, "encrypt() : cipher_fd = " + cipher_fd + ", clear_fd = " + clear_fd + "mime = " + mime);
        if (clear_fd == null || cipher_fd == null) {
            Log.e(TAG, "encrypt bad input parameters");
            return CTA_ERROR_INVALID_INPUT;
        }
        if (mime == null) {
            return encrypt(clear_fd, cipher_fd);
        }
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_ENCRYPT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CLEAR_FD, clear_fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CIPHER_FD, cipher_fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_RAW_MIME, mime);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);

        return CTA_ERROR_NONE;
    }

    /**
     * Decrypt chipher file to clear file. It's a asynchronize method.
     *
     * @param cipher_fd A file descriptor used to be decrypted
     * @param clear_fd A file descriptor used to be stored clear content
     * @return {@link CTA_ERROR_NONE} if encrypt successfully,
     *         otherwise return {@link CTA_ERROR_BADKEY}, {@link CTA_ERROR_NOSPACE},
     *         {@link CTA_ERROR_CANCLED_BY_USER}
     */
    public int decrypt(FileDescriptor cipher_fd, FileDescriptor clear_fd) {
        Log.d(TAG, "decrypt() : cipher_fd = " + cipher_fd + ", clear_fd = " + clear_fd);
        if (clear_fd == null || cipher_fd == null) {
            Log.e(TAG, "encrypt bad input parameters");
            return CTA_ERROR_INVALID_INPUT;
        }
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_DECRYPT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CLEAR_FD, clear_fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CIPHER_FD, cipher_fd);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        return CTA_ERROR_NONE;
    }

    /**
     * Decrypt chipher file to clear file. It's a asynchronize method
     *
     * @param chipher_fd A file descriptor used to be decrypted
     * @param clear_fd A file descriptor used to be stored clear content
     * @param key The key used to decrypt file
     * @return {@link CTA_ERROR_NONE} if encrypt successfully,
     *         otherwise return {@link CTA_ERROR_BADKEY}, {@link CTA_ERROR_NOSPACE},
     *         {@link CTA_ERROR_CANCLED_BY_USER}
     */
    public int decrypt(FileDescriptor cipher_fd, FileDescriptor clear_fd, byte[] key) {
        Log.d(TAG, "decrypt() : cipher_fd = " + cipher_fd + ", clear_fd = " + clear_fd + ", key = " + key);
        if (clear_fd == null || cipher_fd == null) {
            Log.e(TAG, "encrypt bad input parameters");
            return CTA_ERROR_INVALID_INPUT;
        }
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_DECRYPT);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CLEAR_FD, clear_fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_CIPHER_FD, cipher_fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_KEY, key);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        return CTA_ERROR_NONE;
    }


    /**
     * Set key used to decrypt/encrypt
     *
     * @param key A key used to decrypt/encrypt cta file
     * return {@link CTA_ERROR_NONE} if sucessfully, otherwise return {@link CTA_ERROR_GENRIC}
     */
    public int setKey(byte[] key)
    {
        Log.d(TAG, "setKey() : key = " + key);
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_SETKEY);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_KEY, key);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        return CTA_ERROR_NONE;
    }

    /**
     * Change password
     *
     * @param fd which file's key will be changed
     * @param oldKey The old key
     * @param newKey The new key
     * @return {@link CTA_ERROR_NONE} if sucessfully, otherwise return {@link CTA_ERROR_GENRIC}
     */
    public int changePassword(FileDescriptor fd, byte[] oldKey, byte[] newKey) {
        Log.d(TAG, "changePassword() : oldKey = " + oldKey + ", newKey = " + newKey);
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION,
                OmaDrmStore.DrmRequestAction.ACTION_CTA5_CHANGEPASSWORD);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FD, fd);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_OLDKEY, oldKey);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_NEWKEY, newKey);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        return CTA_ERROR_NONE;
    }

    /**
     * Get thubnail
     * @param fd The CTA file
     * @return A thubnail of CTA file
     */
    public Bitmap getThubnail(FileDescriptor fd) {
        Log.d(TAG, "getThubnail()");
        //TODO get thubnail from retriver
        return null;
    }

    /**
     * Get thubnail
     * @param fd The CTA file
     * @param height The thubnail height wanted by caller - 0 indicate default value
     * @param width The thubnail width wanted by caller - 0 indicate default value
     * @return A thubnail of CTA file
     */
    public Bitmap getThubnail(FileDescriptor fd, int height, int width) {
        Log.d(TAG, "getThubnail():fd = " + fd + ", height = " + height + ",width = " + width);
        //TODO get thubnail from retriver
        return null;
    }

    /**
     * Get encrypt or decrypt progress
     * @param fd The CTA file
     * @return The encrypted/decrypted progress
     * @see Progress
     */
    public Progress getProgress(FileDescriptor fd) {
        Log.d(TAG, "getProgress()");
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_GETPROGESS);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FD, fd);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        return new Progress(1, 1, 0);
    }

    /**
     * Set a encrypt/decrypt progress listener
     */
    public int setProgressListener(ProgressListener progressListener) {
        mProgressListener = progressListener;
        mDrmManagerClient.setOnInfoListener(mProgressInfoListener);
        return CTA_ERROR_NONE;
    }

    /**
     * Cancel a encrypt/decrypt process
     * @param fd The CTA file
     * @return {@link CTA_ERROR_NONE} if sucessfully, otherwise return {@link CTA_ERROR_DONE}
     */
    public int cancel(FileDescriptor fd) {
        Log.d(TAG, "cancel() fd " + fd.valid() + ",toString " + fd.toString());
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_CANCEL);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FD, fd);

        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        String message = getResultFromDrmInfo(info);
        int result = OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message)
                   ? CTA_ERROR_NONE : CTA_ERROR_CANCEL;
        return result;
    }

    /**
     * Check if it's cta file
     * @param fd The file to be checked
     * @return true if fd is a cta file, otherwise return false
     */
    public boolean isCTAFile(FileDescriptor fd) {
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_ISCTAFILE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FD, fd);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        String message = getResultFromDrmInfo(info);
        boolean result = OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ? true : false;
        return result;
    }

    /**
     * Check if it's cta file
     * @param filePath is the file path
     * @return true if filePath is a cta file, otherwise return false
     */
    public boolean isCTAFile(String filePath) {
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_SET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_ISCTAFILE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FILEPATH, filePath);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        String message = getResultFromDrmInfo(info);
        boolean result = OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ? true : false;
        return result;
    }

    /**
     * A listnerner to noify decrypt/encrypt progress
     */
    public interface ProgressListener {
        /**
         * Notify decrypt/encrypt progress
         * @param fd The file processed
         * @param currentSize The decrypted/encrypted file size
         * @param totalSize The total size of file
         * @param error Error code
         * @return {@link CTA_ERROR_NONE} if sucessfully, otherwise return {@link CTA_ERROR_GENRIC}
         */
        public int onProgressUpdate(String inPath, Long currentSize, Long totalSize, int error);
    }

    /**
     * A listener to receive drminfo which indicate encrypt/decrypt progess
     */
    private class ProgressInfoListener implements DrmManagerClient.OnInfoListener {
        @Override
        public void onInfo(DrmManagerClient client, DrmInfoEvent event) {
            if (event.getType() == /*DrmInfoEvent.TYPE_CTA_DECRTYPT_PROGESS*/10001) {
                if (mProgressListener != null) {
                    String message = event.getMessage();
                    Log.d(TAG, "hongen callback: message=" + message);
                    HashMap<String, String> hashMap = parseEventMsg(message);
                    Long dataSize = Long.parseLong(hashMap.get("data_s"));
                    Long cntSize = Long.parseLong(hashMap.get("cnt_s"));
                    String path = hashMap.get("path");
                    String result = hashMap.get("result");
                    if ("cta5_cancel_done".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_CANCEL_DONE);
                    } else if ("cta5_done".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_DONE);
                    } else if ("cta5_error".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_ERROR);
                    } else if ("cta5_updating".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_UPDATING);
                    } else if ("cta5_multimedia_encrypt_done".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_MULTI_MEDIA_ENCRYPT_DONE);
                    } else if ("cta5_multimedia_decrypt_done".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_MULTI_MEDIA_DECRYPT_DONE);
                    } else if ("cta5_error_key".equals(result)) {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_ERROR_BADKEY);
                    } else {
                        mProgressListener.onProgressUpdate(path, cntSize, dataSize, CTA_ERROR_GENRIC);
                    }

                }
            } else {
                Log.e(TAG, "type can not be known:type=" + event.getType());
            }

        }
    }

    private HashMap<String, String> parseEventMsg(String message) {
        HashMap<String, String> hashMap = new HashMap<String, String>(4);
        String[] dataArr = message.split("::");
        int size = dataArr.length;
        for (int i = 0; i < size; i++) {
            String[] keyValue = dataArr[i].split(":");
            if (2 == keyValue.length) {
                hashMap.put(keyValue[0], keyValue[1]);
            } else {
                //Log.e(TAG, "hongen map is not right,key=" + keyValue[0]);
                //hashMap.put(keyValue[0], "error");
                Log.e(TAG, "hongen map is not right:" + Arrays.toString(dataArr));
            }
        }
        return hashMap;
    }

    /**
     * A helper class to encapsulate encrypt/decrypt progress
     */
    public static class Progress {
        private long mCurrentSize;
        private long mTotalSize;
        private int mError;

        public Progress(long currentSize, long totalSize, int error) {
            mCurrentSize = currentSize;
            mTotalSize = totalSize;
        }

        /**
         * Get current encrypted/decrypted size.
         * @return The current encrypted/decrypted size.
         */
        public long getCurrenetSize() {
            return mCurrentSize;
        }

        /**
         * Get Total file size.
         * @return The total size.
         */
        public long getTotalSize() {
            return mTotalSize;
        }

        /**
         * Get error code.
         * @return The error code.
         */
        public int getError() {
            return mError;
        }

        /**
         * Get the progress
         * @return The progress.
         */
        public float getProgress() {
            return mCurrentSize / (float) mTotalSize;
        }
    }

    private ProgressListener mProgressListener = null;
    private ProgressInfoListener mProgressInfoListener = new ProgressInfoListener();

    // get the result from DrmInfo, sucess or fail
    private String getResultFromDrmInfo(DrmInfo info) {
        // get message from returned DrmInfo
        byte[] data = null;
        if (info != null) {
            data = info.getData();
        }
        String message = "";
        if (null != data) {
            try {
                // the information shall be in format of ASCII string
                message = new String(data, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unsupported hongen encoding type of the returned DrmInfo data");
                message = "";
            }
        }
        return message;
    }

    /**
     * Check if CTA DRM feature is supported or not.
     *
     * @return true if CTA DRM feature is supported, otherwise return false
     */
    public static boolean isCtaDrmSupport() {
        return sIsCtaDrmSupport;
    }

    /**
     * get token from native
     */
    public String getToken(String filePath) {
        Log.d(TAG, "getToken filePath:" + filePath);
        String result = null;
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_GETTOKEN);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FILEPATH, filePath);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        result = getResultFromDrmInfo(info);
        return result;
    }

    /**
     * check token is valid
     */
    public boolean isTokenValid(String filePath, String token) {
        Log.d(TAG, "isTokenValid filePath:" + filePath);
        boolean result = false;
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_CHECKTOKEN);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FILEPATH, filePath);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_TOKEN, token);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        String message = getResultFromDrmInfo(info);
        result = OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ? true : false;
        return result;
    }

    /**
     * clear token
     */
    public boolean clearToken(String filePath, String token) {
        Log.d(TAG, "clearToken filePath:" + filePath);
        boolean result = false;
        DrmInfoRequest request = new DrmInfoRequest(OmaDrmStore.DrmRequestType.TYPE_GET_DRM_INFO,
                OmaDrmStore.DrmObjectMime.MIME_CTA5_MESSAGE);
        request.put(OmaDrmStore.DrmRequestKey.KEY_ACTION, OmaDrmStore.DrmRequestAction.ACTION_CTA5_CLEARTOKEN);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_FILEPATH, filePath);
        request.put(OmaDrmStore.DrmRequestKey.KEY_CTA5_TOKEN, token);
        DrmInfo info = mDrmManagerClient.acquireDrmInfo(request);
        String message = getResultFromDrmInfo(info);
        result = OmaDrmStore.DrmRequestResult.RESULT_SUCCESS.equals(message) ? true : false;
        return result;
    }

}

