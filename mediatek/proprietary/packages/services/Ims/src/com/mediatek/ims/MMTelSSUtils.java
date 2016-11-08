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
import android.net.Network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
/// SS OP01 Ut @{
import java.util.Arrays;
import java.util.List;
/// @}
import javax.net.SocketFactory;

import android.os.Build;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;

import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.ims.ImsException;
import com.android.ims.ImsManager;
/// SS OP01 Ut
import com.mediatek.ims.internal.ImsXuiManager;
import com.mediatek.simservs.client.SimServs;
import com.mediatek.telephony.TelephonyManagerEx;

/**
 * Implementation for MMTel SS Utils.
 *
 * {@hide}
 *
 */
public class MMTelSSUtils {
    private static final String LOG_TAG = "MMTelSSUtils";
    static String sXcapUri;
    static String remoteIp = null;
    static boolean queryXcapSrvDone = false;

    //Following Constants definition must be same with EngineerMode/ims/ImsActivity.java
    private final static String PROP_SS_MODE = "persist.radio.ss.mode";
    private final static String MODE_SS_XCAP = "Prefer XCAP";
    private final static String MODE_SS_CS = "Prefer CS";

    private static boolean IS_USER_BUILD = "user".equals(Build.TYPE);
    private static boolean IS_ENG_BUILD = "eng".equals(Build.TYPE);

    /// SS OP01 Ut @{
    private static final List<String> OP01_MCCMNC_LIST = Arrays.asList("46000", "46002",
            "46007", "46008");
    /// @}

    private static final List<String> OP03_MCCMNC_LIST = Arrays.asList("20801", "20802");

    private static final List<String> OP05_MCCMNC_LIST = Arrays.asList("26201", "26206",
            "26278");

    private static final List<String> OP06_MCCMNC_LIST = Arrays.asList("26202", "26204",
            "26209", "20205", "21670", "27402", "27403", "27201", "22210", "27801", "20404",
            "26801", "22601", "21401", "21406", "28602", "23415", "23591", "90128");

    private static final List<String> OP07_MCCMNC_LIST = Arrays.asList("31030", "31070",
            "31080", "31090", "310150", "310170", "310280", "310380", "310410", "310560",
            "310680", "311180");

    private static final List<String> OP15_MCCMNC_LIST = Arrays.asList("26203", "26207",
            "26208", "26211", "26277"); //26203 and 26277 is from latest Telefonica req.

    /// SS OP18 Ut @{
    private static final List<String> OP18_MCCMNC_LIST = Arrays.asList("405854",
            "405855",
            "405856",
            "405872",
            "405857",
            "405858",
            "405859",
            "405860",
            "405861",
            "405862",
            "405873",
            "405863",
            "405864",
            "405874",
            "405865",
            "405866",
            "405867",
            "405868",
            "405869",
            "405871",
            "405870",
            "405840");
    /// @}

    private static final List<String> MCCMNC_BLOCK_LIST = Arrays.asList("22210");

    /**
     * Get the XCAP Root URI for the specific phone ID.
     *
     * @param phoneId phone index
     * @return the Root URI String
     */
    public static String getXcapRootUri(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return "";
        }
        SimServs simSrv = SimServs.getInstance();

        if (isOp01IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(true);
            simSrv.setXcapNSPrefixSS(false);
        } else if (isOp03IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(false);
        } else if (isOp05IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(false);
        } else if (isOp06IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(false);
        } else if (isOp07IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true); //use http in op07.
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(true);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(false);
        } else if (isOp15IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(false);
        } else if (isOp18IccCard(phoneId)) {
            simSrv.setUseHttpProtocolScheme(true);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(true);
        } else {
            simSrv.setUseHttpProtocolScheme(false);
            simSrv.setElementUpdateContentType(true, null);
            simSrv.setHandleError409(false);
            simSrv.setFillCompleteForwardTo(false);
            simSrv.setXcapNSPrefixSS(false);
        }

        String rootUri = simSrv.getXcapRoot();
        Rlog.d(LOG_TAG, "getXcapRootUri():" + rootUri);
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        if (rootUri == null) {
            // get from IMS MO
            //rootUri = IMSMO.getRootUri();
            if (rootUri != null) {
                // verify?
                simSrv.setXcapRoot(rootUri);
            }
            else {
                // still null, assemble it
                String impi = null;
                impi = TelephonyManagerEx.getDefault().getIsimImpi(subId);

                if (impi != null && !impi.isEmpty()) {  // ISIM
                    Rlog.d(LOG_TAG, "getXcapRootUri():get APP_FAM_IMS and impi=" + impi);
                    simSrv.setXcapRootByImpi(impi);
                } else {
                    String mccMnc = null;
                    mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
                    String mcc = "";
                    String mnc = "";
                    if (!TextUtils.isEmpty(mccMnc)) {
                        mcc = mccMnc.substring(0, 3);
                        mnc = mccMnc.substring(3);
                    }

                    if (mnc.length() == 2) {
                        mccMnc = mcc + 0 + mnc;
                        Rlog.d(LOG_TAG, "add 0 to mnc =" + mnc);
                    }
                    Rlog.d(LOG_TAG, "get mccMnc=" + mccMnc + " from the IccRecrods");

                    if (!TextUtils.isEmpty(mccMnc)) {
                        if (mccMnc.equals("460000") || mccMnc.equals("460002")
                                || mccMnc.equals("460007") || mccMnc.equals("460008")
                                || mccMnc.equals("460011")) {
                            simSrv.setXcapRootByMccMnc("460", "000");
                        } else {
                            simSrv.setXcapRootByMccMnc(mccMnc.substring(0, 3),
                                    mccMnc.substring(3));
                        }
                    }
                }
                rootUri = simSrv.getXcapRoot();
                Rlog.d(LOG_TAG, "getXcapRoot():rootUri=" + rootUri);
            }
        }

        return rootUri;
    }

    /**
     * Get the XCAP XUI for the specific phone ID.
     * @param phoneId phone index
     * @return the XUI String
     */
    public static String getXui(int phoneId, Context context) {
        SimServs simSrv = SimServs.getInstance();
        String sXui = simSrv.getXui();
        Rlog.d(LOG_TAG, "getXui():sXui from simSrv=" + sXui);
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        if (sXui == null) {
            //Get XUI (P-Associated-URI header) from storage (updated by IMSA) (IR.92) - wait for
            // XuiManager check-in by AF10
            //[ALPS01654778] As XCAP User Identity (XUI) the UE must use the default public user
            // identity received in P-Associated-URI header in 200 OK for REGISTER
            //IMS Stack will update IMPUs to ImsSimservsDispatcher's handleXuiUpdate(): Store
            // IMPUs to ImsXuiManager
            //Example (from main log):ImsSimservsDispatcher: [ims] ImsSimservsDispatcher
            // handleXuiUpdate xui=sip:14253269846@msg.pc.t-mobile.com,
            //sip:+14253269846@msg.pc.t-mobile.com,
            //sip:+14253269846@ims.mnc260.mcc310.3gppnetwork.org,
            //sip:310260007540985@ims.mnc260.mcc310.3gppnetwork.org
            sXui = ImsXuiManager.getInstance().getXui(phoneId);
            Rlog.d(LOG_TAG, "getXui():sXui from XuiManager=" + sXui);
            if (sXui != null) {
                sXui = sXui.split(",")[0];
                simSrv.setXui(sXui);
                return sXui;
            } else {
                //Check if ISIM or SIM is inserted or not
                //ISIM is ready but it may not read all records successfully at this time
                //[TODO] getImpu() from ISIM:API is not available (Set IMPU if ISIM inserted)
                String sImpu = "";
                String[] impu = null;
                impu = TelephonyManagerEx.getDefault().getIsimImpu(subId);

                if (impu != null) {
                    sImpu = impu[0];
                    Rlog.d(LOG_TAG, "getXui():sImpu=" + sImpu);
                    simSrv.setXuiByImpu(sImpu);
                } else {
                    //SIM/USIM is ready but it may not read all records successfully at this time
                    TelephonyManager telephonyManager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    String sImsi = telephonyManager.getSubscriberId(
                        SubscriptionManager.getSubIdUsingPhoneId(phoneId));
                    Rlog.d(LOG_TAG, "getXui():IMS uiccApp is null, try to select USIM uiccApp");

                    String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
                    Rlog.d(LOG_TAG, "getXui():Imsi=" + sImsi + ", mccMnc=" + mccMnc);

                    if (!TextUtils.isEmpty(mccMnc)) {
                        simSrv.setXuiByImsiMccMnc(sImsi, mccMnc.substring(0, 3),
                                mccMnc.substring(3));
                    }
                }

                //Originally:sXui is null. Now:re-obtain sXui again
                sXui = simSrv.getXui();
                Rlog.d(LOG_TAG, "getXui():sXui=" + sXui);
                return sXui;
            }
        } else {
            sXui = sXui.split(",")[0];
            return sXui;
        }
    }

    /**
     * Get the XIntended Id for the specific phone ID.
     * @param phoneId phone index
     * @return the XIntendedId String
     */
    public static String getXIntendedId(int phoneId, Context context) {
        //[ALPS01654778] Modify for TMO-US XCAP Test: It requires that XUI and
        // X-3GPP-Intended-Identify must be same
        return getXui(phoneId, context);
        /*
        SimServs simSrv = SimServs.getInstance();
        String sIntendedId = "";
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        //Check if ISIM or SIM is inserted or not
        UiccController uiccCtl = UiccController.getInstance();
        UiccCardApplication uiccApp = uiccCtl.getUiccCardApplication(phoneId,
                UiccController.APP_FAM_IMS);
        if (uiccApp != null) {
            //Only ISIM is inserted, it is necessary to update X-3GPP-Intended-Identity
            //Set IMPU for X-3GPP-Intended-Identity
            //ISIM is ready but it may not read all records successfully at this time
            //[TODO] getImpu() from ISIM:API is not available
            String[] impu = null;
            impu = TelephonyManagerEx.getIsimImpu(subId);
            if (impu != null) {
                String sImpu = impu[0];
                sIntendedId = sImpu;
                Rlog.d(LOG_TAG, "getXIntendedId ():sImpu=" + sImpu);
            }
            simSrv.setIntendedId(sIntendedId);
        }

        return sIntendedId;
        */
    }

    public static String getHttpCredentialUserName() {
        //[TODO]Wait for IMS MO API
        String sUserName = "";

        return sUserName;
    }

    public static String getHttpCredentialPassword() {
        //[TODO]Wait for IMS MO API
        String sPassword = "";

        return sPassword;
    }

    /**
     * Check if we use Ut/XCAP.
     * @param phoneId phone index
     * @return ture if use Ut/XCAP
     */
    public static boolean isPreferXcap(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        String ssMode = MODE_SS_CS;
        if (SystemProperties.get("ro.mtk_ims_support").equals("1") &&
                SystemProperties.get("ro.mtk_volte_support").equals("1")) {
            ssMode = SystemProperties.get(PROP_SS_MODE, MODE_SS_XCAP);
            if (isNotSupportXcap(phoneId)) {
                ssMode = MODE_SS_CS;
            }
        } else {
            ssMode = SystemProperties.get(PROP_SS_MODE, MODE_SS_CS);
        }

        if (!SystemProperties.get("ro.mtk_ims_support").equals("1") ||
                !SystemProperties.get("ro.mtk_volte_support").equals("1")) {
            Rlog.d(LOG_TAG, "isPreferXcap(): Not Enable VOLTE feature! "
                    + "Return directly to use CSFB SS");
            return false;
        }

        if (MODE_SS_CS.equals(ssMode)) {
            Rlog.d(LOG_TAG, "isPreferXcap(): Config SS via CS! Return directly!");
            return false;
        }

        return true;
    }

    /**
     * Check if we can connect with the XCAP server.
     * @param phoneId phone index
     * @param network specific Network for XCAP server
     * @return ture if we can connect with the XCAP server
     */
    public static boolean isSupportXcap(int phoneId, Network network) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        sXcapUri = getXcapRootUri(phoneId);
        remoteIp = null;
        InetAddress[] ia = null;
        queryXcapSrvDone = false;

        String ss_mode = "";
        if (SystemProperties.get("ro.mtk_ims_support").equals("1") &&
                SystemProperties.get("ro.mtk_volte_support").equals("1")) { //@L
        //if (FeatureOption.MTK_IMS_SUPPORT == true && FeatureOption.MTK_VOLTE_SUPPORT == true) {
            ss_mode = SystemProperties.get(PROP_SS_MODE, MODE_SS_XCAP);
        } else {
            ss_mode = SystemProperties.get(PROP_SS_MODE, MODE_SS_CS);
        }
        Rlog.d(LOG_TAG, "isSupportXcap(): sXcapUri=" + sXcapUri + ",ss_mode=" + ss_mode);

        String preConfigPort = "";

        if (!SystemProperties.get("ro.mtk_ims_support").equals("1") ||
                !SystemProperties.get("ro.mtk_volte_support").equals("1")) { //@L
        //if (FeatureOption.MTK_IMS_SUPPORT == false && FeatureOption.MTK_VOLTE_SUPPORT == false) {
            Rlog.d(LOG_TAG, "Not Enable VOLTE feature! Return directly to use CSFB SS");
            return false;
        }

        if (MODE_SS_CS.equals(ss_mode)) {
            Rlog.d(LOG_TAG, "Config SS via CS! Return directly!");
            return false;
        }

        if (sXcapUri != null) {
            //get IP for uri & put in non-main thread to avoid exception in Android
            //new Thread(new Runnable() {
                //public void run() {
                    try {
                        //Note by mtk01411: sXcapUri=xcap.ims.mnc01.mcc001.pub.3gppnetwork.org/
                        //It should check if / is existed, it must skip the / to avoid DNS lookup
                        // failed
                        String XcapSrvHostName = null;

                        //Add for NSN Lab IOT Testing
                        if (IS_ENG_BUILD) {
                            String TestingXcapRoot =
                                    SystemProperties.get("mediatek.simserv.xcaproot",
                                    "NON_CONFIG");
                            Rlog.d(LOG_TAG, "mediatek.simserv.xcaproot=" + TestingXcapRoot);
                            if (!"NON_CONFIG".equals(TestingXcapRoot)) {
                                sXcapUri = TestingXcapRoot;
                                Rlog.d(LOG_TAG, "Replace sXcapUri=" + sXcapUri);
                            }

                            if (!"http".equals(sXcapUri.substring(0, sXcapUri.lastIndexOf(':')))
                               && !"https".equals(
                                       sXcapUri.substring(0, sXcapUri.lastIndexOf(':')))) {
                                //Has port - Example:For NSN Lab IOT (In adb shell: setprop
                                // mediatek.simserv.xcaproot
                                //   http://xcap.srnims3.srnnam.nsn-rdnet.net:8090/)
                                //Then sXcapUri =
                                //     http://xcap.srnims3.srnnam.nsn-rdnet.net:8090/myService/
                                String portSubString =
                                        sXcapUri.substring(sXcapUri.lastIndexOf(':') + 1);
                                Rlog.d(LOG_TAG, "portSubString=" + portSubString);
                                preConfigPort =  portSubString.substring(0,
                                        portSubString.indexOf("/"));
                                sXcapUri = sXcapUri.substring(0, sXcapUri.lastIndexOf(':'));
                                //At this time - sXcapUri=http://xcap.srnims3.srnnam.nsn-rdnet.net
                                sXcapUri += "/";
                                //At this time - sXcapUri=http://xcap.srnims3.srnnam.nsn-rdnet.net/
                                Rlog.d(LOG_TAG, "preConfig sXcapUri=" + sXcapUri
                                        + " with preConfigPort=" + preConfigPort);
                            }
                        }

                        if (sXcapUri.startsWith("http://")) {
                            XcapSrvHostName = sXcapUri.substring(7, sXcapUri.lastIndexOf("/"));
                        } else if (sXcapUri.startsWith("https://")) {
                            XcapSrvHostName = sXcapUri.substring(8, sXcapUri.lastIndexOf("/"));
                        }
                        Rlog.d(LOG_TAG, "isSupportXcap():XcapSrvHostName=" + XcapSrvHostName);
                        if (network != null) {
                            ia = network.getAllByName(XcapSrvHostName);
                        } else {
                            ia = InetAddress.getAllByName(XcapSrvHostName);
                        }
                        //For Testing non-main thread query DNS functionality
                        //ia = InetAddress.getByName("www.ntu.edu.tw");
                        for (InetAddress addr : ia) {
                            remoteIp = addr.getHostAddress();
                            if (remoteIp != null) {
                                Rlog.d(LOG_TAG, "xcap server ip : " + remoteIp);
                                break;
                            }
                        }
                    //} catch (UnknownHostException ex) {
                    } catch (Exception ex) {
                        Rlog.d(LOG_TAG, "sXcapUri getHostAddress fail : ");
                        ex.printStackTrace();
                        //For testing purpose
                        if (IS_ENG_BUILD) {
                            //[ALPS01713098]CMCC XCAP IP =
                            // 183.221.242.172(xcap.ims.mnc02.mcc460.pub.3gppnetwork.org)
                            //Example: TMO-US: XCAP IP=66.94.0.187(xcap.msg.pc.t-mobile.com)
                            remoteIp = SystemProperties.get("mediatek.simserv.xcapip", "");
                        }
                    }
                    queryXcapSrvDone = true;
                //}
            //}).start();

            while (queryXcapSrvDone != true) {
                //Wait for check again
            }

            Rlog.d(LOG_TAG, "QueryXcapSrvDone:xcap server ip : " + remoteIp);
            //If the user configures as XCAP Preferred and IP for XCAP server is also available:
            // Execute SS via XCAP
            if (remoteIp != null && MODE_SS_XCAP.equals(ss_mode)) {
                //Check if this ip & port can be reachable or not: For XCAP, it should be port
                // 80/443/or specific Lab IOT Test's port
                SocketFactory sf = SocketFactory.getDefault();
                Socket s = null;
                boolean reachable = false;


                String testingPort = "";
                if (IS_ENG_BUILD) {
                    testingPort = SystemProperties.get("mediatek.simserv.port", "");
                }

                //For Normal Case, XCAP needs authentication (GBA or HTTP Digest), port must be 443
                //But for LAB IOT, user can configure the port by the property
                // "mediatek.simserv.port"
                String portList[] = null;
                if ((isOp01IccCard(phoneId)) || (isOp06IccCard(phoneId))) {
                    portList = new String[3];
                    portList[0] = "80";
                    portList[1] = preConfigPort;
                    portList[2] = testingPort;
                } else if (isOp18IccCard(phoneId)) {
                    portList = new String[4];
                    portList[0] = "7077";
                    portList[1] = "443";
                    portList[2] = preConfigPort;
                    portList[3] = testingPort;
                } else {
                    portList = new String[4];
                    portList[0] = "443";
                    portList[1] = "80";
                    portList[2] = preConfigPort;
                    portList[3] = testingPort;
                }
                for (int i = 0; i < portList.length; i++) {
                    int tempPort = 0;
                    if (!portList[i].equals("")) {
                        tempPort = Integer.parseInt(portList[i]);
                    }

                    Rlog.d(LOG_TAG, "testingPort=" + testingPort + "try connecting to IP="
                            + remoteIp + " and port=" + tempPort);

                    if (portList[i].equals("")) {
                        continue;
                    } else {
                        try {
                            InetSocketAddress sa = new InetSocketAddress(remoteIp, tempPort);
                            s = sf.createSocket();
                            s.connect(sa, 10000);
                            reachable = s.isConnected();
                            Rlog.d(LOG_TAG, "Connect to XCAP_IP=" + remoteIp + " with port=" +
                                portList[i] + ", reachable=" + reachable);
                            s.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (reachable) {
                            return true;
                        }
                    }
                }
            }
        }

        if (IS_ENG_BUILD) {
            Rlog.d(LOG_TAG, "isSupportXcap(): start to get ss tcname");
            //[MMTelSS]Add testing mode by mtk01411 (test case mode is necessary to co-work with
            // simserv's modification)
            String tc_name = SystemProperties.get("ril.ss.tcname", "Empty");
            Rlog.d(LOG_TAG, "isSupportXcap():tc_name=" + tc_name);
            if (tc_name != null && tc_name.startsWith("Single_TC_")) {
                return true;
            }
        }

        return false;
    }

    /// SS OP01 Ut @{

    public static boolean isOp01IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp01IccCard(): mccMnc is " + mccMnc);
        return OP01_MCCMNC_LIST.contains(mccMnc);
    }

    public static boolean isOp03IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp03IccCard(): mccMnc is " + mccMnc);
        boolean retVal = OP03_MCCMNC_LIST.contains(mccMnc);
        Rlog.d(LOG_TAG, "isOp03IccCard()=>" + retVal);
        return retVal;
    }

    public static boolean isOp05IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp05IccCard(): mccMnc is " + mccMnc);
        boolean retVal = OP05_MCCMNC_LIST.contains(mccMnc);
        Rlog.d(LOG_TAG, "isOp05IccCard()=>" + retVal);
        return retVal;
    }

    public static boolean isOp06IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp06IccCard(): mccMnc is " + mccMnc);
        boolean retVal = OP06_MCCMNC_LIST.contains(mccMnc);
        Rlog.d(LOG_TAG, "isOp06IccCard()=>" + retVal);
        return retVal;
    }

    public static boolean isOp07IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp07IccCard(): mccMnc is " + mccMnc);
        return OP07_MCCMNC_LIST.contains(mccMnc);
    }

    public static boolean isOp15IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp15IccCard(): mccMnc is " + mccMnc);
        return OP15_MCCMNC_LIST.contains(mccMnc);
    }

    public static boolean isOp18IccCard(int phoneId) {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }

        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isOp18IccCard(): mccMnc is " + mccMnc);
        return OP18_MCCMNC_LIST.contains(mccMnc);
    }

    public static String addXcapRootPort(String xcapRoot, int phoneId) {
        //xcapRoot without specific port number
        if ("http".equals(xcapRoot.substring(0, xcapRoot.lastIndexOf(':')))
               || "https".equals(
               xcapRoot.substring(0, xcapRoot.lastIndexOf(':')))) {
            if (xcapRoot.charAt(xcapRoot.length() - 1) == '/') {
                xcapRoot = xcapRoot.substring(0, xcapRoot.length() - 1);
            }

            if (isOp18IccCard(phoneId)) {
                xcapRoot += ":7077";
            }

            xcapRoot += "/";
        }


        return xcapRoot;
    }

    public static boolean isNotifyCallerTest() {
        if (SystemProperties.get("persist.xcap.notifycaller.test").equals("test")) {
            return true;
        }
        return false;
    }
    /// @}

    /**
     * Get the default IMS Phone Id.
     *
     * @return phoneId with IMS enabled
     */
    public static int getDefaultImsPhoneId(Context context) {
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();

        try {
            for (int i = 0; i < phoneCount; i++) {
                if (ImsManager.getInstance(context, i).getImsRegInfo()) {
                    Rlog.d(LOG_TAG, "getDefaultImsPhoneId(): IMS registered. phoneId = " + i);
                    return i;
                }
            }
        } catch (ImsException e) {
            Rlog.d(LOG_TAG, "getDefaultImsPhoneId(): ImsException: " + e);
        }

        return -1;
    }

    public static boolean isNotSupportXcap(int phoneId) {
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        String mccMnc = TelephonyManager.getDefault().getSimOperator(subId);
        Rlog.d(LOG_TAG, "isNotSupportXcap, mccMnc is " + mccMnc);
        return MCCMNC_BLOCK_LIST.contains(mccMnc);
    }
}
