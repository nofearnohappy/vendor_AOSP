/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.dm.test;

import android.test.InstrumentationTestRunner;

//import com.mediatek.dm.test.cc.DmMailNodeIoHandlerTest;
//import com.mediatek.dm.test.cc.DmPIMNodeIoHandlerTest;
//import com.mediatek.dm.test.cc.Pim1NodeIoHandlerTest;
import com.mediatek.dm.test.cc.TestAGPSParamsProvider;
import com.mediatek.dm.test.cc.TestConnParamsProvider;
import com.mediatek.dm.test.cc.TestMMSParamsProvider;
import com.mediatek.dm.test.cc.TestStreamingParamsProvider;
import com.mediatek.dm.test.cc.TestWapParamsProvider;
import com.mediatek.dm.test.lawmo.LawmoHandlerTest;

//import com.mediatek.dm.test.scomo.ScomoInstallTest;
//import com.mediatek.dm.test.scomo.ScomoDcHandlerTest;
//import com.mediatek.dm.test.scomo.ScomoDpHandlerTest;
//import com.mediatek.dm.test.scomo.ScomoHandlerTest;
//import com.mediatek.dm.test.scomo.ScomoStateTest;
//import com.mediatek.dm.test.scomo.ScomoActivityTest;
//import com.mediatek.dm.test.scomo.ScomoConfirmActivityTest;
//import com.mediatek.dm.test.DmServiceTest;
import com.mediatek.dm.test.util.DLProgressNotifierTest;
import com.mediatek.dm.test.util.FileLoggerTest;
import com.mediatek.dm.test.util.NiaDecoderTest;
import com.mediatek.dm.test.util.ScheduleTaskQueueTest;
import com.mediatek.dm.test.scomo.DmPLInventoryTest;
//import com.mediatek.dm.test.scomo.DmScomoNotificationTest;
//import com.mediatek.dm.test.scomo.ScomoDownloadDetailActivityTest;

import com.mediatek.dm.test.fumo.Fumo01UnpackTests;
//import com.mediatek.dm.test.fumo.Fumo02EntryTests;
import com.mediatek.dm.test.fumo.Fumo04UpdateTests;
import com.mediatek.dm.test.ext.MTKMediaContainerTest;

import junit.framework.TestSuite;

public class TestRunner extends InstrumentationTestRunner {
    @Override
    public TestSuite getAllTests() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(StandAloneTests.class);
        suite.addTestSuite(ContextBaseTests.class);
        suite.addTestSuite(DmAgentTests.class);
        suite.addTestSuite(WapConnectionTest.class);

        suite.addTestSuite(TestAGPSParamsProvider.class);
        suite.addTestSuite(TestConnParamsProvider.class);
        suite.addTestSuite(TestMMSParamsProvider.class);
        suite.addTestSuite(TestStreamingParamsProvider.class);
        suite.addTestSuite(TestWapParamsProvider.class);
        suite.addTestSuite(LawmoHandlerTest.class);


        // something bad will happen when running following test cases
        suite.addTestSuite(Fumo01UnpackTests.class);
        //suite.addTestSuite(Fumo02EntryTests.class);
        //suite.addTestSuite(Fumo03FlowTests.class);
        suite.addTestSuite(Fumo04UpdateTests.class);

        suite.addTestSuite(MTKMediaContainerTest.class);
//        suite.addTestSuite(ScomoInstallTest.class);
        suite.addTestSuite(DmControllerTest.class);
        suite.addTestSuite(DmPLDLPkgTest.class);
        //suite.addTestSuite(DmReceiverTest.class);
        //suite.addTestSuite(DmCpObeserverTest.class);
        //suite.addTestSuite(OmaCpReceiverTest.class);

//        suite.addTestSuite(ScomoDcHandlerTest.class);
//        suite.addTestSuite(ScomoDpHandlerTest.class);
//        suite.addTestSuite(ScomoHandlerTest.class);
        //suite.addTestSuite(ScomoStateTest.class);
//        suite.addTestSuite(ScomoActivityTest.class);
//        suite.addTestSuite(ScomoConfirmActivityTest.class);
        //suite.addTestSuite(DmServiceTest.class);
        suite.addTestSuite(DmDownloadNotificationTest.class);
        suite.addTestSuite(DmPlDeltaFile.class);
        suite.addTestSuite(DLProgressNotifierTest.class);
        suite.addTestSuite(DmMmiProgressTest.class);
        suite.addTestSuite(DmPLInventoryTest.class);
//        suite.addTestSuite(DmScomoNotificationTest.class);
        //suite.addTestSuite(ScomoDownloadDetailActivityTest.class);

        suite.addTestSuite(DmMmiFactoryTest.class);
        suite.addTestSuite(DmPlLoggerTest.class);
        suite.addTestSuite(DmXmlWriterTest.class);

        // com.mediatek.dm.util test
        suite.addTestSuite(NiaDecoderTest.class);
        suite.addTestSuite(ScheduleTaskQueueTest.class);
        suite.addTestSuite(FileLoggerTest.class);

        return suite;
    }

}
