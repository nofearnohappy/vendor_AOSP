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

package com.mediatek.ims;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.ims.ImsCallForwardInfo;
/// For OP01 UT
import com.android.ims.ImsCallForwardInfoEx;
import com.android.ims.ImsReasonInfo;
import com.android.ims.ImsSsInfo;
import com.android.ims.ImsUtInterface;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;
import com.android.internal.telephony.CallForwardInfo;

import java.net.UnknownHostException;

/// For OP01 UT
import com.android.internal.telephony.CallForwardInfoEx;
import com.android.internal.telephony.CommandsInterface;
import com.mediatek.simservs.xcap.XcapException;


import static com.android.internal.telephony.imsphone.ImsPhoneMmiCode.UT_BUNDLE_KEY_CLIR;
import static com.android.internal.telephony.imsphone.ImsPhoneMmiCode.UT_BUNDLE_KEY_SSINFO;

/**
 * ImsUT class for handle the IMS UT interface.
 *
 * The implementation is based on IR.92
 *
 *  @hide
 */
public class ImsUtStub extends IImsUt.Stub {
    private static final String TAG = "ImsUtService";
    private static final boolean DBG = true;

    private Context mContext;

    private static final Object mLock = new Object();
    private static int sRequestId = 0;
    private IImsUtListener mListener = null;
    private MMTelSSTransport mMMTelSSTSL;
    private ResultHandler mHandler;

    static final int IMS_UT_EVENT_GET_CB = 1000;
    static final int IMS_UT_EVENT_GET_CF = 1001;
    static final int IMS_UT_EVENT_GET_CW = 1002;
    static final int IMS_UT_EVENT_GET_CLIR = 1003;
    static final int IMS_UT_EVENT_GET_CLIP = 1004;
    static final int IMS_UT_EVENT_GET_COLR = 1005;
    static final int IMS_UT_EVENT_GET_COLP = 1006;
    static final int IMS_UT_EVENT_SET_CB = 1007;
    static final int IMS_UT_EVENT_SET_CF = 1008;
    static final int IMS_UT_EVENT_SET_CW = 1009;
    static final int IMS_UT_EVENT_SET_CLIR = 1010;
    static final int IMS_UT_EVENT_SET_CLIP = 1011;
    static final int IMS_UT_EVENT_SET_COLR = 1012;
    static final int IMS_UT_EVENT_SET_COLP = 1013;
    /// For OP01 UT @{
    static final int IMS_UT_EVENT_GET_CF_TIME_SLOT = 1014;
    static final int IMS_UT_EVENT_SET_CF_TIME_SLOT = 1015;
    /// @}

    static final int HTTP_ERROR_CODE_403 = 403;
    static final int HTTP_ERROR_CODE_404 = 404;
    static final int HTTP_ERROR_CODE_409 = 409;

    /**
    *
    * Construction function for ImsConfigStub.
    *
    * @param context the application context
    *
    */
   public ImsUtStub(Context context) {
       mContext = context;
       mMMTelSSTSL = MMTelSSTransport.getInstance();
       mMMTelSSTSL.registerUtService(mContext);

       HandlerThread thread = new HandlerThread("ImsUtStubResult");
       thread.start();
       Looper looper = thread.getLooper();
       mHandler = new ResultHandler(looper);
   }

   private class ResultHandler extends Handler {
       public ResultHandler(Looper looper) {
            super(looper);
       }

       @Override
       public void handleMessage(Message msg) {
           if (DBG) {
               Log.d(TAG, "handleMessage(): event = " + msg.what + ", requestId = " + msg.arg1);
           }
           switch (msg.what) {
               case IMS_UT_EVENT_GET_CB:
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception) {
                           int[] result = (int []) ar.result;
                           ImsSsInfo[] info = new ImsSsInfo[1];
                           info[0] = new ImsSsInfo();
                           info[0].mStatus = result[0];
                           // TODO: add ServiceClass information
                           if (DBG) {
                               Log.d(TAG, "IMS_UT_EVENT_GET_CB: status = " + result[0]);
                           }

                           try {
                               mListener.utConfigurationCallBarringQueried(ImsUtStub.this,
                                       msg.arg1, info);
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in utConfigurationCallBarringQueried");
                               e.printStackTrace();
                           }
                       } else if (ar.exception instanceof UnknownHostException) {
                           if (DBG) {
                               Log.d(TAG, "IMS_UT_EVENT_GET_CB: UnknownHostException.");
                           }
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                   msg.arg1,
                                   new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CB: "
                                       + "UnknownHostException utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       } else if (ar.exception instanceof XcapException) {
                           XcapException xcapException = (XcapException) ar.exception;
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                   msg.arg1,
                                   xcapExceptionToImsReasonInfo(xcapException));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CB: "
                                       + "utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       } else {
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this, msg.arg1,
                                       new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                               0));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CB: "
                                       + "utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       }
                   }
                   break;
               case IMS_UT_EVENT_GET_CF:
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception) {
                           CallForwardInfo[] cfInfo = (CallForwardInfo[]) ar.result;
                           ImsCallForwardInfo[] imsCfInfo = null;

                           if (cfInfo != null && cfInfo.length != 0) {
                               imsCfInfo = new ImsCallForwardInfo[cfInfo.length];
                               for (int i = 0; i < cfInfo.length; i++) {
                                   if (DBG) {
                                       Log.d(TAG, "IMS_UT_EVENT_GET_CF: cfInfo[" + i + "] = "
                                               + cfInfo[i]);
                                   }
                                   imsCfInfo[i] = getImsCallForwardInfo(cfInfo[i]);
                               }
                           }

                           try {
                               mListener.utConfigurationCallForwardQueried(ImsUtStub.this,
                                       msg.arg1, imsCfInfo);
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in utConfigurationCallForwardQueried");
                               e.printStackTrace();
                           }
                       } else {
                           if (ar.exception instanceof XcapException) {
                               XcapException xcapException = (XcapException) ar.exception;
                               try {
                                   mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                       msg.arg1,
                                       xcapExceptionToImsReasonInfo(xcapException));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF: "
                                           + "utConfigurationQueryFailed");
                                   e.printStackTrace();
                               }
                           } else if (ar.exception instanceof UnknownHostException) {
                               if (DBG) {
                                   Log.d(TAG, "IMS_UT_EVENT_GET_CF: UnknownHostException.");
                               }
                               try {
                                   mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                       msg.arg1,
                                       new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF: "
                                           + "UnknownHostException utConfigurationQueryFailed");
                                   e.printStackTrace();
                               }
                           } else {
                               try {
                                   mListener.utConfigurationQueryFailed(ImsUtStub.this, msg.arg1,
                                           new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                                   0));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF: "
                                           + "utConfigurationQueryFailed");
                                   e.printStackTrace();
                               }
                           }
                       }
                   }
                   break;
               case IMS_UT_EVENT_GET_CW:
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception) {
                           int[] result = (int []) ar.result;
                           ImsSsInfo[] info = new ImsSsInfo[1];
                           info[0] = new ImsSsInfo();
                           info[0].mStatus = result[0];
                           // TODO: add ServiceClass information
                           if (DBG) {
                               Log.d(TAG, "IMS_UT_EVENT_GET_CW: status = " + result[0]);
                           }

                           try {
                               mListener.utConfigurationCallWaitingQueried(ImsUtStub.this,
                                       msg.arg1, info);
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in utConfigurationCallWaitingQueried");
                               e.printStackTrace();
                           }
                       } else if (ar.exception instanceof UnknownHostException) {
                           if (DBG) {
                               Log.d(TAG, "IMS_UT_EVENT_GET_CW: UnknownHostException.");
                           }
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                   msg.arg1,
                                   new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CW: "
                                       + "UnknownHostException utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       } else if (ar.exception instanceof XcapException) {
                           XcapException xcapException = (XcapException) ar.exception;
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                   msg.arg1,
                                   xcapExceptionToImsReasonInfo(xcapException));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CW: "
                                       + "utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       } else {
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this, msg.arg1,
                                       new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                               0));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CW: "
                                       + "utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       }
                   }
                   break;
               case IMS_UT_EVENT_GET_CLIR:
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception) {
                           int[] result = (int []) ar.result;
                           Bundle info = new Bundle();
                           info.putIntArray(UT_BUNDLE_KEY_CLIR, result);

                           try {
                               mListener.utConfigurationQueried(ImsUtStub.this, msg.arg1, info);
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CLIR: "
                                       + "utConfigurationQueried");
                               e.printStackTrace();
                           }
                       } else if (ar.exception instanceof UnknownHostException) {
                           if (DBG) {
                               Log.d(TAG, "IMS_UT_EVENT_GET_CLIR: UnknownHostException.");
                           }
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                   msg.arg1,
                                   new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CLIR: "
                                       + "UnknownHostException utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       } else if (ar.exception instanceof XcapException) {
                           XcapException xcapException = (XcapException) ar.exception;
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                   msg.arg1,
                                   xcapExceptionToImsReasonInfo(xcapException));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CLIR: "
                                       + "utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       } else {
                           try {
                               mListener.utConfigurationQueryFailed(ImsUtStub.this, msg.arg1,
                                       new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                               0));
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CLIR: "
                                       + "utConfigurationQueryFailed");
                               e.printStackTrace();
                           }
                       }
                   }
                   break;
               case IMS_UT_EVENT_GET_CLIP:
               case IMS_UT_EVENT_GET_COLR: // fall through
               case IMS_UT_EVENT_GET_COLP: // fall through
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception) {
                           int[] result = (int []) ar.result;
                           ImsSsInfo ssInfo = new ImsSsInfo();
                           ssInfo.mStatus = result[0];
                           Bundle info = new Bundle();
                           info.putParcelable(UT_BUNDLE_KEY_SSINFO, ssInfo);

                           try {
                               mListener.utConfigurationQueried(ImsUtStub.this, msg.arg1, info);
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in utConfigurationQueried, event = "
                                       + msg.what);
                               e.printStackTrace();
                           }
                       } else {
                           if (ar.exception instanceof XcapException) {
                               XcapException xcapException = (XcapException) ar.exception;
                               try {
                                   mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                       msg.arg1,
                                       xcapExceptionToImsReasonInfo(xcapException));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in utConfigurationQueryFailed, "
                                           + "event = " + msg.what);
                                   e.printStackTrace();
                               }
                           } else if (ar.exception instanceof UnknownHostException) {
                               if (DBG) {
                                   Log.d(TAG, "UnknownHostException. event = " + msg.what);
                               }
                               try {
                                   mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                       msg.arg1,
                                       new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException UnknownHostException "
                                           + "utConfigurationQueryFailed, event" + msg.what);
                                   e.printStackTrace();
                               }
                           } else {
                               try {
                                   mListener.utConfigurationQueryFailed(ImsUtStub.this, msg.arg1,
                                           new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                                   0));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in utConfigurationQueryFailed, "
                                           + "event = " + msg.what);
                                   e.printStackTrace();
                               }
                           }
                       }
                   }
                   break;
               case IMS_UT_EVENT_SET_CB:
               case IMS_UT_EVENT_SET_CF: // fall through
                   // Handle the special case if update CF return cfinto object.
                   // Currently, only DTAG (OP05) and CFU would go though here.
                   // Need carefully handle this part because SET CB would here.
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception && ar.result != null) {
                           if (ar.result instanceof CallForwardInfo[]) {
                               CallForwardInfo[] cfInfo = (CallForwardInfo[]) ar.result;
                               ImsCallForwardInfo[] imsCfInfo = null;

                               if (cfInfo != null && cfInfo.length != 0) {
                                   imsCfInfo = new ImsCallForwardInfo[cfInfo.length];
                                   for (int i = 0; i < cfInfo.length; i++) {
                                       if (DBG) {
                                           Log.d(TAG, "IMS_UT_EVENT_SET_CF: cfInfo[" + i + "] = "
                                                   + cfInfo[i]);
                                       }
                                       imsCfInfo[i] = getImsCallForwardInfo(cfInfo[i]);
                                   }
                               }

                               try {
                                   mListener.utConfigurationCallForwardQueried(ImsUtStub.this,
                                       msg.arg1, imsCfInfo); //Use this function to append the cfinfo.
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in utConfigurationCFUpdateAndQueried");
                                   e.printStackTrace();
                               }
                               break;  //Break here and no need to do the below process.
                                       //If ar.result is null, then use the original flow.
                           }
                       }
                   }
               case IMS_UT_EVENT_SET_CW: // fall through
               case IMS_UT_EVENT_SET_CLIR: // fall through
               case IMS_UT_EVENT_SET_CLIP: // fall through
               case IMS_UT_EVENT_SET_COLR: // fall through
               case IMS_UT_EVENT_SET_COLP: // fall through
               case IMS_UT_EVENT_SET_CF_TIME_SLOT: // For OP01 UT
                   if (null != mListener) {
                       AsyncResult ar = (AsyncResult) msg.obj;

                       if (null == ar.exception) {
                           if (DBG) {
                               Log.d(TAG, "utConfigurationUpdated(): "
                                       + "event = " + msg.what);
                           }
                           try {
                               mListener.utConfigurationUpdated(ImsUtStub.this, msg.arg1);
                           } catch (RemoteException e) {
                               Log.e(TAG, "RemoteException in utConfigurationUpdated, event = "
                                       + msg.what);
                               e.printStackTrace();
                           }
                       } else {
                           if (ar.exception instanceof XcapException) {
                               XcapException xcapException = (XcapException) ar.exception;
                               try {
                                   mListener.utConfigurationUpdateFailed(ImsUtStub.this,
                                       msg.arg1,
                                       xcapExceptionToImsReasonInfo(xcapException));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in utConfigurationUpdateFailed, "
                                           + "event = " + msg.what);
                                   e.printStackTrace();
                               }
                           } else if (ar.exception instanceof UnknownHostException) {
                               if (DBG) {
                                   Log.d(TAG, "UnknownHostException. event = " + msg.what);
                               }
                               try {
                                   mListener.utConfigurationUpdateFailed(ImsUtStub.this,
                                       msg.arg1,
                                       new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException UnknownHostException "
                                           + "utConfigurationUpdateFailed, event" + msg.what);
                                   e.printStackTrace();
                               }
                           } else {
                               try {
                                   mListener.utConfigurationUpdateFailed(ImsUtStub.this, msg.arg1,
                                           new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR,
                                                   0));
                               } catch (RemoteException e) {
                                   Log.e(TAG, "RemoteException in utConfigurationUpdateFailed, "
                                           + "event = " + msg.what);
                                   e.printStackTrace();
                               }
                           }
                       }
                   }
                   break;
                /// For OP01 UT @{
                case IMS_UT_EVENT_GET_CF_TIME_SLOT:
                    if (null != mListener) {
                        AsyncResult ar = (AsyncResult) msg.obj;

                        if (null == ar.exception) {
                            CallForwardInfoEx[] cfInfo = (CallForwardInfoEx[]) ar.result;
                            ImsCallForwardInfoEx[] imsCfInfo = null;

                            if (cfInfo != null && cfInfo.length != 0) {
                                imsCfInfo = new ImsCallForwardInfoEx[cfInfo.length];
                                for (int i = 0; i < cfInfo.length; i++) {
                                    ImsCallForwardInfoEx info = new ImsCallForwardInfoEx();
                                    info.mCondition =
                                            getConditionFromCFReason(cfInfo[i].reason);
                                    info.mStatus = cfInfo[i].status;
                                    info.mServiceClass = cfInfo[i].serviceClass;
                                    info.mToA = cfInfo[i].toa;
                                    info.mNumber = cfInfo[i].number;
                                    info.mTimeSeconds = cfInfo[i].timeSeconds;
                                    info.mTimeSlot = cfInfo[i].timeSlot;
                                    imsCfInfo[i] = info;
                                }
                            }

                            try {
                                mListener.utConfigurationCallForwardInTimeSlotQueried(
                                        ImsUtStub.this, msg.arg1, imsCfInfo);
                            } catch (RemoteException e) {
                                Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF_TIME_SLOT"
                                        + " utConfigurationCallForwardInTimeSlotQueried");
                                e.printStackTrace();
                            }
                        } else {
                            if (ar.exception instanceof XcapException) {
                                XcapException xcapException = (XcapException) ar.exception;
                                try {
                                    mListener.utConfigurationQueryFailed(
                                            ImsUtStub.this, msg.arg1,
                                            xcapExceptionToImsReasonInfo(xcapException));
                                } catch (RemoteException e) {
                                    Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF_TIME_SLOT"
                                            + " utConfigurationQueryFailed");
                                    e.printStackTrace();
                                }
                            } else if (ar.exception instanceof UnknownHostException) {
                                if (DBG) {
                                    Log.d(TAG, "IMS_UT_EVENT_GET_CF_TIME_SLOT: "
                                            + "UnknownHostException.");
                                }
                                try {
                                    mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                        msg.arg1,
                                        new ImsReasonInfo(ImsReasonInfo.CODE_UT_UNKNOWN_HOST, 0));
                                } catch (RemoteException e) {
                                    Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF_TIME_SLOT: "
                                            + "UnknownHostException utConfigurationQueryFailed");
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    mListener.utConfigurationQueryFailed(ImsUtStub.this,
                                            msg.arg1,
                                            new ImsReasonInfo(
                                                    ImsReasonInfo.CODE_UT_NETWORK_ERROR, 0));
                                } catch (RemoteException e) {
                                    Log.e(TAG, "RemoteException in IMS_UT_EVENT_GET_CF_TIME_SLOT"
                                            + " utConfigurationQueryFailed");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    break;
                    /// @}
               default:
                   Log.d(TAG, "Unknown Event: " + msg.what);
                   break;
           }
       }
   };

    /**
     * Closes the object. This object is not usable after being closed.
     */
    public void close() {
    }

    private String getFacilityFromCBType(int cbType) {
        switch (cbType) {
            case ImsUtInterface.CB_BAIC:
                return CommandsInterface.CB_FACILITY_BAIC;
            case ImsUtInterface.CB_BAOC:
                return CommandsInterface.CB_FACILITY_BAOC;
            case ImsUtInterface.CB_BOIC:
                return CommandsInterface.CB_FACILITY_BAOIC;
            case ImsUtInterface.CB_BOIC_EXHC:
                return CommandsInterface.CB_FACILITY_BAOICxH;
            case ImsUtInterface.CB_BIC_WR:
                return CommandsInterface.CB_FACILITY_BAICr;
            // TODO: Barring of Anonymous Communication Rejection (ACR)
            case ImsUtInterface.CB_BIC_ACR:
                return "ACR";
            case ImsUtInterface.CB_BA_ALL:
                return CommandsInterface.CB_FACILITY_BA_ALL;
            case ImsUtInterface.CB_BA_MO:
                return CommandsInterface.CB_FACILITY_BA_MO;
            case ImsUtInterface.CB_BA_MT:
                return CommandsInterface.CB_FACILITY_BA_MT;
            // TODO: Barring of Specific Incoming calls
            case ImsUtInterface.CB_BS_MT:
                return "BS_MT";
            default:
                return null;
        }
    }

    private int getCFActionFromAction(int cfAction) {
        switch(cfAction) {
            case ImsUtInterface.ACTION_DEACTIVATION:
                return CommandsInterface.CF_ACTION_DISABLE;
            case ImsUtInterface.ACTION_ACTIVATION:
                return CommandsInterface.CF_ACTION_ENABLE;
            case ImsUtInterface.ACTION_ERASURE:
                return CommandsInterface.CF_ACTION_ERASURE;
            case ImsUtInterface.ACTION_REGISTRATION:
                return CommandsInterface.CF_ACTION_REGISTRATION;
            default:
                break;
        }

        return CommandsInterface.CF_ACTION_DISABLE;
    }

    private int getCFReasonFromCondition(int condition) {
        switch(condition) {
            case ImsUtInterface.CDIV_CF_UNCONDITIONAL:
                return CommandsInterface.CF_REASON_UNCONDITIONAL;
            case ImsUtInterface.CDIV_CF_BUSY:
                return CommandsInterface.CF_REASON_BUSY;
            case ImsUtInterface.CDIV_CF_NO_REPLY:
                return CommandsInterface.CF_REASON_NO_REPLY;
            case ImsUtInterface.CDIV_CF_NOT_REACHABLE:
                return CommandsInterface.CF_REASON_NOT_REACHABLE;
            case ImsUtInterface.CDIV_CF_ALL:
                return CommandsInterface.CF_REASON_ALL;
            case ImsUtInterface.CDIV_CF_ALL_CONDITIONAL:
                return CommandsInterface.CF_REASON_ALL_CONDITIONAL;
            case ImsUtInterface.CDIV_CF_NOT_LOGGED_IN:
                return CommandsInterface.CF_REASON_NOT_REGISTERED;
            default:
                break;
        }

        return CommandsInterface.CF_REASON_NOT_REACHABLE;
    }

    private int getConditionFromCFReason(int reason) {
        switch(reason) {
            case CommandsInterface.CF_REASON_UNCONDITIONAL:
                return ImsUtInterface.CDIV_CF_UNCONDITIONAL;
            case CommandsInterface.CF_REASON_BUSY:
                return ImsUtInterface.CDIV_CF_BUSY;
            case CommandsInterface.CF_REASON_NO_REPLY:
                return ImsUtInterface.CDIV_CF_NO_REPLY;
            case CommandsInterface.CF_REASON_NOT_REACHABLE:
                return ImsUtInterface.CDIV_CF_NOT_REACHABLE;
            case CommandsInterface.CF_REASON_ALL:
                return ImsUtInterface.CDIV_CF_ALL;
            case CommandsInterface.CF_REASON_ALL_CONDITIONAL:
                return ImsUtInterface.CDIV_CF_ALL_CONDITIONAL;
            case CommandsInterface.CF_REASON_NOT_REGISTERED:
                return ImsUtInterface.CDIV_CF_NOT_LOGGED_IN;
            default:
                break;
        }

        return ImsUtInterface.INVALID;
    }

    private ImsCallForwardInfo getImsCallForwardInfo(CallForwardInfo info) {
        ImsCallForwardInfo imsCfInfo = new ImsCallForwardInfo();
        imsCfInfo.mCondition = getConditionFromCFReason(info.reason);
        imsCfInfo.mStatus = info.status;
        //imsCfInfo.mServiceClass = info.serviceClass; // TODO: Add video service class
        imsCfInfo.mToA = info.toa;
        imsCfInfo.mNumber = info.number;
        imsCfInfo.mTimeSeconds = info.timeSeconds;
        return imsCfInfo;
    }

    /**
     * Retrieves the configuration of the call barring.
     * @param cbType Call Barring Type
     * @return the request ID
     */
    public int queryCallBarring(int cbType) {
        int requestId;
        String facility;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCallBarring(): requestId = " + requestId);
        }

        facility = getFacilityFromCBType(cbType);
        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_CB, requestId, 0, null);
        mMMTelSSTSL.queryFacilityLock(facility, null, CommandsInterface.SERVICE_CLASS_VOICE, msg);

        return requestId;
    }

    /**
     * Retrieves the configuration of the call forward.
     * @param condition Call Forward condition
     * @param number Forwarded to number
     * @return the request ID
     */
    public int queryCallForward(int condition, String number) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCallForward(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_CF, requestId, 0, null);
        mMMTelSSTSL.queryCallForwardStatus(getCFReasonFromCondition(condition),
                CommandsInterface.SERVICE_CLASS_VOICE, number, msg);

        return requestId;
    }

    /**
     * Retrieves the configuration of the call waiting.
     * @return the request ID
     */
    public int queryCallWaiting() {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCallWaiting(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_CW, requestId, 0, null);
        mMMTelSSTSL.queryCallWaiting(CommandsInterface.SERVICE_CLASS_VOICE, msg);

        return requestId;
    }

    /**
     * Retrieves the default CLIR setting.
     * @return the request ID
     */
    public int queryCLIR() {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCLIR(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_CLIR, requestId, 0, null);
        mMMTelSSTSL.getCLIR(msg);

        return requestId;
    }

    /**
     * Retrieves the CLIP call setting.
     * @return the request ID
     */
    public int queryCLIP() {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCLIP(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_CLIP, requestId, 0, null);
        mMMTelSSTSL.queryCLIP(msg);

        return requestId;
    }

    /**
     * Retrieves the COLR call setting.
     * @return the request ID
     */
    public int queryCOLR() {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCOLR(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_COLR, requestId, 0, null);
        mMMTelSSTSL.getCOLR(msg);

        return requestId;
    }

    /**
     * Retrieves the COLP call setting.
     * @return the request ID
     */
    public int queryCOLP() {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCOLP(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_COLP, requestId, 0, null);
        mMMTelSSTSL.getCOLP(msg);

        return requestId;
    }

    /**
     * Updates or retrieves the supplementary service configuration.
     * @param ssInfo supplementary service information
     * @return the request ID
     */
    public int transact(Bundle ssInfo) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }

        return requestId;
    }

    /**
     * Updates the configuration of the call barring.
     * @param cbType Call Barring Type
     * @param enable lock state
     * @param barrList barring list
     * @return the request ID
     */
    public int updateCallBarring(int cbType, boolean enable, String[] barrList) {
        int requestId;
        String facility;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCallBarring(): requestId = " + requestId);
        }

        facility = getFacilityFromCBType(cbType);
        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_CB, requestId, 0, null);
        mMMTelSSTSL.setFacilityLock(facility, enable, null,
                CommandsInterface.SERVICE_CLASS_VOICE, msg);

        return requestId;
    }

    /**
     * Updates the configuration of the call forward.
     * @param action the call forwarding action
     * @param condition the call forwarding condition
     * @param number the call forwarded to number
     * @param timeSeconds seconds for no reply
     * @return the request ID
     */
    public int updateCallForward(int action, int condition, String number, int serviceClass,
                                     int timeSeconds) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCallForward(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_CF, requestId, 0, null);
        mMMTelSSTSL.setCallForward(getCFActionFromAction(action),
                getCFReasonFromCondition(condition), serviceClass,
                number, timeSeconds, msg);

        return requestId;
    }

    /**
     * Updates the configuration of the call waiting.
     * @param enable activate flag
     * @return the request ID
     */
    public int updateCallWaiting(boolean enable, int serviceClass) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCallWaiting(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_CW, requestId, 0, null);
        mMMTelSSTSL.setCallWaiting(enable, serviceClass, msg);

        return requestId;
    }

    /**
     * Updates the configuration of the CLIR supplementary service.
     * @param clirMode CLIR mode
     * @return the request ID
     */
    public int updateCLIR(int clirMode) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCLIR(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_CLIR, requestId, 0, null);
        mMMTelSSTSL.setCLIR(clirMode, msg);

        return requestId;
    }

    /**
     * Updates the configuration of the CLIP supplementary service.
     * @param enable activate flag
     * @return the request ID
     */
    public int updateCLIP(boolean enable) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCLIP(): requestId = " + requestId);
        }

        int enableClip = (enable) ? 1 : 0;
        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_CLIP, requestId, 0, null);
        mMMTelSSTSL.setCLIP(enableClip, msg);

        return requestId;
    }

    /**
     * Updates the configuration of the COLR supplementary service.
     * @param presentation presentation flag
     * @return the request ID
     */
    public int updateCOLR(int presentation) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCOLR(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_COLR, requestId, 0, null);
        mMMTelSSTSL.setCOLR(presentation, msg);

        return requestId;
    }

    /**
     * Updates the configuration of the COLP supplementary service.
     * @param enable activate flag
     * @return the request ID
     */
    public int updateCOLP(boolean enable) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCOLP(): requestId = " + requestId);
        }

        int enableColp = (enable) ? 1 : 0;
        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_COLP, requestId, 0, null);
        mMMTelSSTSL.setCOLP(enableColp, msg);

        return requestId;
    }

    /**
     * Sets the listener.
     * @param listener callback interface
     */
    public void setListener(IImsUtListener listener) {
        mListener = listener;
    }

    /**
     * Convert XcapExcaption to ImsReasonInfo.
     * @param xcapEx the XcapExcaption
     * @return the converted ImsReasonInfo
     */
    ImsReasonInfo xcapExceptionToImsReasonInfo(XcapException xcapEx) {
        ImsReasonInfo reason;

        if ((DBG) && (xcapEx != null)) {
            Log.d(TAG, "xcapExceptionToImsReasonInfo(): XcapException: "
                    + "code = " + xcapEx.getExceptionCodeCode()
                    + ", http error = " + xcapEx.getHttpErrorCode()
                    + ", isConnectionError = " + xcapEx.isConnectionError());
        }

        if ((xcapEx != null) && (xcapEx.getHttpErrorCode() == HTTP_ERROR_CODE_403)) {
            reason = new ImsReasonInfo(ImsReasonInfo.CODE_UT_XCAP_403_FORBIDDEN, 0);
        } else if ((xcapEx != null) && (xcapEx.getHttpErrorCode() == HTTP_ERROR_CODE_404)) {
            reason = new ImsReasonInfo(ImsReasonInfo.CODE_UT_XCAP_404_NOT_FOUND, 0);
        } else if ((xcapEx != null) && (xcapEx.getHttpErrorCode() == HTTP_ERROR_CODE_409)) {
            reason = new ImsReasonInfo(ImsReasonInfo.CODE_UT_XCAP_409_CONFLICT, 0);
        } else {
            reason = new ImsReasonInfo(ImsReasonInfo.CODE_UT_NETWORK_ERROR, 0);
        }

        return reason;
    }

    /// For OP01 UT @{
    /**
     * Retrieves the configuration of the call forward in a time slot.
     */
    public int queryCallForwardInTimeSlot(int condition) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "queryCallForwardInTimeSlot(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_GET_CF_TIME_SLOT,
                requestId, 0, null);
        mMMTelSSTSL.queryCallForwardInTimeSlotStatus(
                getCFReasonFromCondition(condition),
                CommandsInterface.SERVICE_CLASS_VOICE,
                msg);

        return requestId;
    }

    /**
     * Updates the configuration of the call forward in a time slot.
     */
    public int updateCallForwardInTimeSlot(int action, int condition,
            String number, int timeSeconds, long[] timeSlot) {
        int requestId;

        synchronized (mLock) {
            requestId = sRequestId;
            sRequestId++;
        }
        if (DBG) {
            Log.d(TAG, "updateCallForwardInTimeSlot(): requestId = " + requestId);
        }

        Message msg = mHandler.obtainMessage(IMS_UT_EVENT_SET_CF_TIME_SLOT, requestId, 0, null);
        mMMTelSSTSL.setCallForwardInTimeSlot(getCFActionFromAction(action),
                getCFReasonFromCondition(condition),
                CommandsInterface.SERVICE_CLASS_VOICE,
                number, timeSeconds, timeSlot, msg);

        return requestId;
    }
    /// @}
}
