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

package com.mediatek.mediatekdm;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.mediatek.mediatekdm.DmConst.TAG;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class DmContentProvider extends ContentProvider {

    private Document mDocTree = null;
    private Document mDocAPN = null;
    private UriMatcher mUriMatcher;

    private static final String DM_CONTENT_URI = "com.mediatek.providers.mediatekdm";

    private static final int DM_CONTENT_CMWAP0 = 0;
    private static final int DM_CONTENT_CMWAP1 = 1;
    private static final int DM_CONTENT_DEVINFO = 2;
    private static final int DM_CONTENT_OMSACC = 3;

    private static final String[] QUERYMASK = { "cmwap0", "cmwap1", "DevInfo", "OMSAcc" };
    private static final String[] CMWAPPROJ = { "name", "numeric", "mcc", "mnc", "apn", "server",
            "proxy", "port" };
    private static final String[] DEVINFOPROJ = { "Mod", "Man" };
    private static final String[] OMSACCPROJ = { "Addr" };

    @Override
    public int delete(Uri arg0, String arg1, String[] arg2) {
        return 0;
    }

    @Override
    public String getType(Uri arg0) {
        return null;
    }

    @Override
    public Uri insert(Uri arg0, ContentValues arg1) {
        return null;
    }

    @Override
    public boolean onCreate() {

        Log.i(TAG.PROVIDER, "DmContentProvider onCreate..");

        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        for (int i = 0; i < QUERYMASK.length; i++) {
            mUriMatcher.addURI(DM_CONTENT_URI, QUERYMASK[i], i);
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();

            File fTree = new File(PlatformManager.getInstance().getPathInSystem(
                    DmConst.Path.DM_TREE_FILE));
            Log.i(TAG.PROVIDER,
                    "DmContentProvider start to parse xml file : " + fTree.getAbsolutePath());
            mDocTree = builder.parse(fTree);

            File fApn = new File(PlatformManager.getInstance().getPathInSystem(
                    DmConst.Path.DM_APN_INFO_FILE));
            Log.i(TAG.PROVIDER,
                    "DmContentProvider start to parse xml file : " + fApn.getAbsolutePath());
            mDocAPN = builder.parse(fApn);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        } catch (SAXException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3, String arg4) {

        Log.i(TAG.PROVIDER, "DmContentProvider query..URI : " + arg0);

        switch (mUriMatcher.match(arg0)) {
            case DM_CONTENT_CMWAP0:
                return queryDMApn("0");
            case DM_CONTENT_CMWAP1:
                return queryDMApn("1");
            case DM_CONTENT_DEVINFO:
                return queryDMDevInfo("DevInfo");
            case DM_CONTENT_OMSACC:
                return queryDMOmsacc("OMSAcc");
            default:
                throw new IllegalArgumentException("Illegal URI : " + arg0);
        }
    }

    @Override
    public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
        return 0;
    }

    private MatrixCursor queryDMApn(String id) {
        Log.i(TAG.PROVIDER, "DmContentProvider queryDMApn..cmwap" + id);

        if (mDocAPN == null) {
            Log.i(TAG.PROVIDER, "-------docAPN is null!!!!!!" + id);
            return null;
        }

        NodeList nl = mDocAPN.getElementsByTagName("id");
        if (nl == null) {
            Log.i(TAG.PROVIDER, "-------node list of tag <id> is null!!!!!!" + id);
            return null;
        }

        Node cmcc = nl.item(Integer.parseInt(id)).getParentNode();

        nl = cmcc.getChildNodes();
        int len = nl.getLength();
        Bundle bundle = new Bundle();

        for (int i = 0; i < len; i++) {
            String name = nl.item(i).getNodeName();
            String value = nl.item(i).getNodeValue();
            Log.i(TAG.PROVIDER, "attributes " + i + " : name - " + name + ", value - " + value);
            bundle.putString(name, value);
        }

        String[] row = new String[CMWAPPROJ.length];
        for (int i = 0; i < CMWAPPROJ.length; i++) {
            row[i] = bundle.getString(CMWAPPROJ[i]);
        }

        MatrixCursor cur = new MatrixCursor(CMWAPPROJ);
        cur.addRow(row);
        return cur;
    }

    private MatrixCursor queryDMDevInfo(String id) {
        Log.i(TAG.PROVIDER, "DmContentProvider queryDMDevInfo..");

        if (mDocTree == null) {
            Log.i(TAG.PROVIDER, "-------docTree is null!!!!!!" + id);
            return null;
        }

        NodeList nl = mDocTree.getElementsByTagName("name");
        if (nl == null) {
            Log.i(TAG.PROVIDER, "-------node list of tag <name> is null!!!!!!" + id);
            return null;
        }

        int len = nl.getLength();
        String[] row = new String[DEVINFOPROJ.length];

        for (int i = 0; i < len; i++) {
            Node node = nl.item(i);
            for (int j = 0; j < DEVINFOPROJ.length; j++) {
                if (DEVINFOPROJ[j].equals(node.getNodeValue())) {
                    row[j] = node.getParentNode().getLastChild().getNodeValue();
                }
            }
        }

        MatrixCursor cur = new MatrixCursor(DEVINFOPROJ);
        cur.addRow(row);
        return cur;
    }

    private MatrixCursor queryDMOmsacc(String id) {
        Log.i(TAG.PROVIDER, "DmContentProvider queryDMOmsacc..");

        if (mDocTree == null) {
            Log.i(TAG.PROVIDER, "-------docTree is null!!!!!!" + id);
            return null;
        }

        NodeList nodeList = mDocTree.getElementsByTagName("value");
        if (nodeList == null) {
            Log.i(TAG.PROVIDER, "-------node list of tag <value> is null!!!!!!" + id);
            return null;
        }

        int length = nodeList.getLength();
        Log.i(TAG.PROVIDER, "-------there are " + length + " nodes with tag <value>");

        String[] row = { "" };
        String tc;
        for (int i = 0; i < length; i++) {
            tc = nodeList.item(i).getTextContent();
            Log.i(TAG.PROVIDER, "-------<value> [ " + i + " ] getTextContent : " + tc);
            if (tc.contains("http")) {
                row[0] = tc;
                break;
            }
        }

        MatrixCursor cur = new MatrixCursor(OMSACCPROJ);
        cur.addRow(row);
        return cur;
    }
}
