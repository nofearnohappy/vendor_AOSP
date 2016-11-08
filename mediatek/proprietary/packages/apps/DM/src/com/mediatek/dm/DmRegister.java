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

package com.mediatek.dm;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.mediatek.dm.DmConst.TAG;
import com.mediatek.dm.xml.DmXMLParser;
import com.redbend.vdm.NodeIoHandler;
import com.redbend.vdm.VdmException;
import com.redbend.vdm.VdmTree;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

public class DmRegister {

    private static final String CC_PACKAGE_NAME = "com.mediatek.dm.cc.Dm";
    private static final String IMS_PACKAGE_NAME = "com.mediatek.dm.ims.Dm";
    private static final String HANDLER_TAIL = "NodeIoHandler";
    private NodeIoHandlerFactory mHandlerFactory;
    private String mMccMnc;

    private VdmTree mDmTree = new VdmTree();

    public DmRegister(Context context, String mccMnc) {
        mHandlerFactory = new NodeIoHandlerFactory(context);
        mMccMnc = mccMnc;
    }

    private void registerIoHandler(String className, String uriStr) {
        NodeIoHandler ioHandler = null;
        ioHandler = mHandlerFactory.createNodeHandler(className, Uri.parse(uriStr), mMccMnc);
        if (ioHandler != null) {
            try {
                mDmTree.registerNodeIoHandler(uriStr, ioHandler);
            } catch (VdmException e) {
                e.printStackTrace();
            }
        }

    }

    public void registerCCNodeIoHandler(String treeFileName) {
        Log.d(TAG.NODE_IO_HANDLER, "DmRegister->registerCCNodeIoHandler:" + treeFileName);
        DmXMLParser treeParser = new DmXMLParser(treeFileName);

        ArrayList<String> uriList = new ArrayList<String>();
        uriList = (ArrayList<String>) getLeafNodeUriList(treeParser,
                getInteriorNode(treeParser, DmConst.NodeName.SETTING));
        String uriStr = null;
        String itemStr = null;
        for (int i = 0; i < uriList.size(); i++) {
            uriStr = uriList.get(i);
            String[] uriStrArray = uriStr.split(File.separator);
            int length = uriStrArray.length - 1;
            for (int j = 0; j < length; j++) {
                if (uriStrArray[j].equals(DmConst.NodeName.SETTING)) {
                    if ((itemStr != uriStrArray[j + 1])) {
                        itemStr = uriStrArray[j + 1];
                    }
                    break;
                }
            }
            Log.i(TAG.NODE_IO_HANDLER, new StringBuilder("registering item:").append(itemStr)
                    .append("(").append(uriStr).append(")").toString());
            if (itemStr != null) {
                String className = CC_PACKAGE_NAME + itemStr + HANDLER_TAIL;
                registerIoHandler(className, uriStr);
            }
        }
    }

    public void registerImsNodeIoHandler(String treeFileName) {
        Log.d(TAG.NODE_IO_HANDLER, "DmRegister->registerImsNodeIoHandler:" + treeFileName);
        DmXMLParser treeParser = new DmXMLParser(treeFileName);

        ArrayList<String> uriList = new ArrayList<String>();
        uriList = (ArrayList<String>) getLeafNodeUriList(treeParser,
                getInteriorNode(treeParser, DmConst.NodeName.IMSMO));
        ArrayList<String> xdmList = (ArrayList<String>) getLeafNodeUriList(treeParser,
                getInteriorNode(treeParser, DmConst.NodeName.XDMMO));
        uriList.addAll(xdmList);

        String uriStr = null;
        for (int i = 0; i < uriList.size(); i++) {
            uriStr = uriList.get(i);
            String[] uriStrArray = uriStr.split(File.separator);
            int length = uriStrArray.length - 1;
            String itemStr = null;
            for (int j = 0; j < length; j++) {
                if (uriStrArray[j].equals(DmConst.NodeName.IMSMO)) {
                    if (length == j + 1) {
                        String strLeaf = uriStrArray[length];
                        if ("AppID".equalsIgnoreCase(strLeaf) || "Name".equalsIgnoreCase(strLeaf)
                                || "PDP_ContextOperPref".equalsIgnoreCase(strLeaf)) {
                            Log.d(TAG.NODE_IO_HANDLER, "no need register io handler");
                        } else {
                            itemStr = "Ims";
                        }
                    } else {
                        String str = uriStrArray[j + 1];
                        if ("Ext".equalsIgnoreCase(str) && (j + 2 <= length)) {
                            String str2 = uriStrArray[j + 2];
                            if ("RCS".equalsIgnoreCase(str2)) {
                                String strLeaf = uriStrArray[length];
                                if ("AuthType".equalsIgnoreCase(strLeaf)
                                        || "Realm".equalsIgnoreCase(strLeaf)
                                        || "UserName".equalsIgnoreCase(strLeaf)
                                        || "UserPwd".equalsIgnoreCase(strLeaf)) {
                                    itemStr = "ImsExtRcs";
                                }
                            }
                        } else if ("Public_user_identity_List".equalsIgnoreCase(str)) {
                            itemStr = "PubUserId";
                        } else if ("ICSI_List".equalsIgnoreCase(str)) {
                            itemStr = "Icsi";
                        } else if ("LBO_P-CSCF_Address".equalsIgnoreCase(str)) {
                            itemStr = "LboPcscf";
                        } else if ("PhoneContext_List".equalsIgnoreCase(str)) {
                            itemStr = "PhoneCtx";
                        }
                    }
                    break;
                } else if (uriStrArray[j].equals(DmConst.NodeName.XDMMO)) {
                    String strLeaf = uriStrArray[length];
                    if ("URI".equalsIgnoreCase(strLeaf)
                            || "AAUTHNAME".equalsIgnoreCase(strLeaf)
                            || "AAUTHSECRET".equalsIgnoreCase(strLeaf)
                            || "AAUTHTYPE".equalsIgnoreCase(strLeaf)) {
                        itemStr = "Xdm";
                    }
                    break;
                }
            }
            Log.i(TAG.NODE_IO_HANDLER, new StringBuilder("registering item:").append(itemStr)
                    .append("(").append(uriStr).append(")").toString());

            if (itemStr != null) {
                String className = IMS_PACKAGE_NAME + itemStr + HANDLER_TAIL;
                registerIoHandler(className, uriStr);
            }
        }
    }

    /* get a Node's(xml style) all leaf node(dm defination) uri string.
     * xmlParser is a parsed xml file which conform to DM DDF, the tree.xml.
     */
    private List<String> getLeafNodeUriList(DmXMLParser xmlParser, Node node) {
        String nodeName = DmConst.NodeName.NAME;
        List<Node> nodeList = new ArrayList<Node>();
        List<String> uriStrList = new ArrayList<String>();
        xmlParser.getChildNode(node, nodeList, nodeName);
        for (int i = 0; i < nodeList.size(); i++) {
            Node tempnode = nodeList.get(i).getParentNode();
            if (tempnode.getNodeName().equalsIgnoreCase(DmConst.NodeName.LEAF) == false) {
                continue; // this is not a leaf node, ignore it.
            }
            String uriStr = getDmTreeNodeName(xmlParser, tempnode);
            while (tempnode != null) {
                tempnode = tempnode.getParentNode();
                String parentsName = getDmTreeNodeName(xmlParser, tempnode);
                if (parentsName == null) {
                    continue;
                }
                uriStr = new StringBuilder(parentsName).append(File.separator).append(uriStr)
                        .toString();
                if (".".equals(parentsName)) {
                    break;
                }
            }
            if (uriStr != null) {
                Log.d(TAG.NODE_IO_HANDLER, "add a leaf uri:" + uriStr);
                uriStrList.add(uriStr);
            }
        }
        return uriStrList;
    }

    private String getDmTreeNodeName(DmXMLParser xmlParser, Node node) {
        if (node == null) {
            return null;
        }
        List<Node> nodeList = new ArrayList<Node>();
        xmlParser.getLeafNode(node, nodeList, DmConst.NodeName.NAME);
        if (nodeList != null && nodeList.size() > 0) {
            return nodeList.get(0).getFirstChild().getNodeValue();
        } else {
            return null;
        }
    }

    private Node getInteriorNode(DmXMLParser xmlParser, String interiorNodeName) {
        // Here the outside loop is for the change of InteriorNode's level start from 1 is to
        // reduce recursive times Anyway, the level should be less than 15, if reasonable
        for (int level = 1; level < 0x000F; level++) {
            List<Node> nodeList = new ArrayList<Node>();
            xmlParser.getChildNodeAtLevel(nodeList, level);
            for (int i = 0; i < nodeList.size(); i++) {
                String nodeName = getDmTreeNodeName(xmlParser, nodeList.get(i));
                if (interiorNodeName.equals(nodeName)) {
                    return nodeList.get(i);
                }
            }
        }
        return null;
    }

}

class NodeIoHandlerFactory {
    private Context mContext;

    // It seems no need to singleton this.
    // private static NodeIoHandlerFactory nodeIoHandlerFact = null;

    // public static NodeIoHandlerFactory creatInstance(Context context,String
    // operator){
    // if(nodeIoHandlerFact == null){
    // nodeIoHandlerFact = new NodeIoHandlerFactory(context, operator);
    // }
    // return nodeIoHandlerFact;
    // }

    public NodeIoHandlerFactory(Context context) {
        mContext = context;
    }

    public NodeIoHandler createNodeHandler(String className, Uri uri, String parameterString) {
        Class<?> ioHandlerClass = null;
        Log.d(TAG.NODE_IO_HANDLER, "[NodeHandlerFactory]loading cls:" + className);
        try {
            ioHandlerClass = Class.forName(className);
        } catch (ClassNotFoundException e1) {
            Log.e(TAG.NODE_IO_HANDLER, "[NodeHandlerFactory]cls not found:" + className);
            return null;
        }
        Constructor<?> conWith3Args = null;
        Constructor<?> conWith2Args = null;
        try {
            conWith3Args = ioHandlerClass.getConstructor(Context.class, Uri.class, String.class);
        } catch (SecurityException e) {

            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            try {
                conWith2Args = ioHandlerClass.getConstructor(Context.class, Uri.class);
            } catch (SecurityException e1) {
                e1.printStackTrace();
                Log.e(TAG.NODE_IO_HANDLER, "constructor with 2 args security exception");
                return null;
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
                Log.e(TAG.NODE_IO_HANDLER, "constructor with 2 args no such method");
                return null;
            }
        }
        try {
            if (conWith3Args != null) {
                Log.d(TAG.NODE_IO_HANDLER, new StringBuilder("[NodeHandlerFactory]created: ")
                        .append("(").append(uri).append(",").append(parameterString).append(")")
                        .toString());
                return (NodeIoHandler) (conWith3Args.newInstance(mContext, uri, parameterString));
            } else {
                if (conWith2Args != null) {
                    Log.d(TAG.NODE_IO_HANDLER, new StringBuilder("[NodeHandlerFactory]created: ")
                            .append("(").append(uri).append(")").toString());
                    return (NodeIoHandler) (conWith2Args.newInstance(mContext, uri));
                }
            }
        } catch (IllegalArgumentException e) {

            e.printStackTrace();
        } catch (InstantiationException e) {

            e.printStackTrace();
        } catch (IllegalAccessException e) {

            e.printStackTrace();
        } catch (InvocationTargetException e) {

            e.printStackTrace();
        }
        // fatal error, not found corresponding IoHandler class;
        Log.w(TAG.NODE_IO_HANDLER, "[NodeHandlerFactory]creating IoHandler failed.");
        return null;
    }
}
