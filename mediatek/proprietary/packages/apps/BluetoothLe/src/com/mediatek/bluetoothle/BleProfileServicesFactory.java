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
 * MediaTek Inc. (C) 2014. All rights reserved.
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

package com.mediatek.bluetoothle;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.SparseArray;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * It's used to create BluetoothGattServices from profile id
 */
public class BleProfileServicesFactory {
    private static final boolean DBG = true;
    private static final String TAG = BleProfileServicesFactory.class.getSimpleName();
    private static final String UUID_SUFFIX = "-0000-1000-8000-00805f9b34fb";
    private static final String FOLDER = "services";
    private static final String CONFIG = "BleProfilesDef.xml";
    private static BleProfileServicesFactory sInstance = new BleProfileServicesFactory();
    private Context mCtxt;
    private final SparseArray<HashMap<String, BluetoothGattService>> mArray =
            new SparseArray<HashMap<String, BluetoothGattService>>();

    private static enum TagName {
        ble_profiles, ble_profile, ble_service, service, included_service, characteristic,
        descriptor, property, permission, broadcast, read, write_no_response, write, notify,
        indicate, signed_write, extended_props, read_encrypted, read_encrypted_mitm,
        write_encrypted, write_encrypted_mitm, write_signed, write_signed_mitm
    };

    private static enum AttribteName {
        id, name, uuid, service_type
    };

    /**
     * Get the singleton of BleProfileServicesFactory object
     *
     * @return singleton of BleProfileServicesFactory object
     */
    public static BleProfileServicesFactory getInstance() {
        return sInstance;
    }

    private BleProfileServicesFactory() {
    }

    private void checkInit() {
        if (null == mCtxt) {
            throw new RuntimeException("[BleProfileServicesFactory] checkInit - "
                    + "Call init before any method invocation!");
        }
    }

    private Context getContext() {
        return mCtxt;
    }

    private void setCreatedService(final int profile, final String file,
            final BluetoothGattService service) {
        HashMap<String, BluetoothGattService> map = mArray.valueAt(profile);
        if (DBG) {
            Log.d(TAG, "setCreatedService: profile=" + profile + ",file=" + file + ",service="
                    + service + ",map=" + map);
        }
        if (null == map) {
            map = new HashMap<String, BluetoothGattService>();
            mArray.setValueAt(profile, map);
        }

        if (null == map.get(file)) {
            map.put(file, service);
        }
    }

    private BluetoothGattService getCreatedService(final int profile, final String file) {
        BluetoothGattService service = null;
        final HashMap<String, BluetoothGattService> map = mArray.valueAt(profile);

        if (DBG) {
            Log.d(TAG, "getCreatedService: map=" + map + ",profile=" + profile + ",file=" + file);
        }
        if (null != map) {
            service = map.get(file);
            if (DBG) {
                Log.d(TAG, "getCreatedService: map=" + map + ",profile=" + profile + ",service="
                        + service);
            }
        }

        if (null == service) {
            Log.e(TAG, "Illegal Forward Reference Included Service");
            throw new RuntimeException("Illegal Forward Reference Included Service"); // illegal
                                                                                      // forward
                                                                                      // reference
        }
        return service;
    }

    /**
     * Initialize the context
     *
     * @param ctxt the context object
     *
     */
    public void init(final Context ctxt) {
        mCtxt = ctxt;
    }

    /**
     * Use for UT
     */
    void clearServiceCache() {
        mArray.clear();
    }
    /**
     * Used to construct the BluetoothGattService list from profile id
     *
     * @param profile the id of BLE profile
     *
     * @return List<BluetoothGattService> of a profile
     */
    public List<BluetoothGattService> constructProfileServices(final int profile) {
        final List<BluetoothGattService> serviceList = new ArrayList<BluetoothGattService>();
        String[] files = null;

        final long startTime = System.currentTimeMillis();
        checkInit();
        files = getProfileInfo(profile);
        if (null != files) {
            for (final String file : files) {
                final BluetoothGattService service = parseService(profile, file);
                serviceList.add(service);
                setCreatedService(profile, file, service);
            }
        }
        if (DBG) {
            Log.d(TAG,
                    "constructProfileServices: profile=" + profile + " cost="
                    + (System.currentTimeMillis() - startTime) + " ms");
        }
        return serviceList;
    }

    private String[] getProfileInfo(final int profile) {
        String msg = null;
        boolean pass = false;
        final ArrayList<String> fileList = new ArrayList<String>();
        final AssetManager assetManager = getContext().getAssets();

        try {
            final Document doc = createDOMObject(CONFIG);
            final Node node = findProfileNodeInNodes(profile,
                    doc.getElementsByTagName(TagName.ble_profile.name()));

            if (null != node) {
                if (checkProfileNodeIsLegal(assetManager, node, fileList)) {
                    pass = true;
                } else {
                    msg = "!checkProfileNodeIsLegal";
                }
            } else {
                msg = "No match profile id!";
            }

        } catch (final IOException e) {
            msg = "Open " + CONFIG + " Failed!";
        } catch (final SAXException e) {
            msg = e.toString();
        } catch (final ParserConfigurationException e) {
            msg = e.toString();
        }

        if (!pass) {
            throw new IllegalArgumentException("[BleProfileServicesFactory] getProfileInfo- " + msg
                    + " (phase1)");
        }

        return fileList.toArray(new String[fileList.size()]);
    }

    private Node findProfileNodeInNodes(final int profile, final NodeList nodes) {
        Node node = null;
        boolean found = false;

        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            final NamedNodeMap map = node.getAttributes();
            if (null != map) {
                final Node nodeId = map.getNamedItem(AttribteName.id.name());
                if (Integer.parseInt(nodeId.getTextContent()) == profile) {
                    found = true;
                    break;
                }
            }
        }

        if (!found) {
            node = null;
        }

        return node;
    }

    private boolean checkProfileNodeIsLegal(final AssetManager assetManager, final Node node,
            final ArrayList<String> fileList) {
        boolean isLegal = true;

        try {
            final String[] files = assetManager.list(FOLDER);
            final NodeList nodes = node.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node tempNode = nodes.item(i);
                if (TagName.ble_service.name().equalsIgnoreCase(tempNode.getNodeName())) {
                    boolean match = false;
                    for (int j = 0; j < files.length; j++) {
                        final NamedNodeMap map = tempNode.getAttributes();
                        if (null != map) {
                            final Node nodeName = map.getNamedItem(AttribteName.name.name());
                            if (files[j].equalsIgnoreCase(nodeName.getTextContent())) {
                                match = true;
                                fileList.add(files[j]);
                                break;
                            }
                        }
                    }
                    if (!match) {
                        isLegal = false;
                        break;
                    }
                }
            }
        } catch (final IOException e) {
            isLegal = false;
        }

        return isLegal;
    }

    private Document createDOMObject(final String filePath) throws SAXException, IOException,
            ParserConfigurationException {

        final AssetManager assetManager = getContext().getAssets();
        final InputStream input = assetManager.open(filePath);
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(input);
    }

    private BluetoothGattService parseService(final int profile, final String fileName) {
        BluetoothGattService service = null;
        try {
            final Document doc = createDOMObject(FOLDER + File.separator + fileName);
            service = parseService(profile, doc.getElementsByTagName((TagName.service.name()))
                    .item(0));
        } catch (final SAXException e) {
            Log.e(TAG, e.toString());
        } catch (final IOException e) {
            Log.e(TAG, e.toString());
        } catch (final ParserConfigurationException e) {
            Log.e(TAG, e.toString());
        }
        return service;
    }

    private BluetoothGattService parseService(final int profile, final Node node) {
        int type = 0;
        UUID uuid = null;
        BluetoothGattService service = null;

        if (DBG) {
            Log.d(TAG, "parseService");
        }
        checkNode(node, TagName.service);

        final NamedNodeMap map = node.getAttributes();
        Node attrNode = null;
        // / uuid
        attrNode = map.getNamedItem(AttribteName.uuid.name());
        uuid = strToUuid(attrNode.getTextContent());
        // / service type
        attrNode = map.getNamedItem(AttribteName.service_type.name());
        type = strToServiceType(attrNode.getTextContent());

        service = new BluetoothGattService(uuid, type);
        final NodeList nodes = node.getChildNodes();
        if (null != nodes) {
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node tempNode = nodes.item(i);
                if (DBG) {
                    Log.d(TAG, "parseService: tempNode.getNodeName()=" + tempNode.getNodeName());
                }
                try {
                    final TagName tag = TagName.valueOf(tempNode.getNodeName());
                    switch (tag) {
                        case included_service:
                            service.addService(parseIncludedService(profile, tempNode));
                            break;
                        case characteristic:
                            service.addCharacteristic(parseCharacteristic(profile, tempNode));
                            break;
                        default:
                            break;
                    }
                } catch (final IllegalArgumentException e) {
                    // ignore it
                    Log.e(TAG, "parseService - ignore getNodeName()=" + tempNode.getNodeName()
                            + " e=" + e);
                }
            }
        }
        return service;
    }

    private BluetoothGattService parseIncludedService(final int profile, final Node node) {
        BluetoothGattService service = null;
        NamedNodeMap map = null;
        Node attrNode = null;

        checkNode(node, TagName.included_service);

        map = node.getAttributes();
        attrNode = map.getNamedItem(AttribteName.name.name());
        if (DBG) {
            Log.d(TAG,
                    "parseIncludedService: attrNode.getTextContent()=" + attrNode.getTextContent());
        }
        service = getCreatedService(profile, attrNode.getTextContent());

        return service;
    }

    private BluetoothGattCharacteristic parseCharacteristic(final int profile, final Node node) {
        int property = 0;
        int permission = 0;
        UUID uuid = null;
        BluetoothGattCharacteristic characteristic = null;
        final List<BluetoothGattDescriptor> descriptorList =
                new ArrayList<BluetoothGattDescriptor>();

        checkNode(node, TagName.characteristic);

        final NamedNodeMap map = node.getAttributes();
        final Node attrNode = map.getNamedItem(AttribteName.uuid.name());
        uuid = strToUuid(attrNode.getTextContent());

        final NodeList nodes = node.getChildNodes();
        if (null != nodes) {
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node tempNode = nodes.item(i);
                try {
                    final TagName tag = TagName.valueOf(tempNode.getNodeName());
                    switch (tag) {
                        case property:
                            property = parseCharacteristicProperty(profile, tempNode);
                            break;
                        case permission:
                            permission = parseCharacteristicPermission(profile, tempNode);
                            break;
                        case descriptor:
                            descriptorList.add(parseDescriptor(profile, tempNode));
                            break;
                        default:
                            break;
                    }
                } catch (final IllegalArgumentException e) {
                    // ignore it
                    Log.e(TAG,
                            "parseCharacteristic - ignore tempNode.getNodeName()= "
                                    + tempNode.getNodeName() + " e=" + e);
                }
            }
        }

        characteristic = new BluetoothGattCharacteristic(uuid, property, permission);
        for (final BluetoothGattDescriptor d : descriptorList) {
            characteristic.addDescriptor(d);
        }

        return characteristic;
    }

    private int parseCharacteristicProperty(final int profile, final Node node) {
        int property = 0;
        TagName propertyType = null;
        final NodeList nodes = node.getChildNodes();

        checkNode(node, TagName.property);
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node tempNode = nodes.item(i);
            if (0 == "yes".compareTo(tempNode.getTextContent().toLowerCase())
                    || 0 == "y".compareTo(tempNode.getTextContent().toLowerCase())) {
                try {
                    propertyType = TagName.valueOf(tempNode.getNodeName());
                    switch (propertyType) {
                        case broadcast:
                            property |= BluetoothGattCharacteristic.PROPERTY_BROADCAST;
                            break;
                        case read:
                            property |= BluetoothGattCharacteristic.PROPERTY_READ;
                            break;
                        case write_no_response:
                            property |= BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;
                            break;
                        case write:
                            property |= BluetoothGattCharacteristic.PROPERTY_WRITE;
                            break;
                        case notify:
                            property |= BluetoothGattCharacteristic.PROPERTY_NOTIFY;
                            break;
                        case indicate:
                            property |= BluetoothGattCharacteristic.PROPERTY_INDICATE;
                            break;
                        case signed_write:
                            property |= BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE;
                            break;
                        case extended_props:
                            property |= BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS;
                            break;
                        default:
                            break;
                    }
                } catch (final IllegalArgumentException e) {
                    // ignore it
                    Log.e(TAG, "parseCharacteristicProperty - " + "ignore tempNode.getNodeName()="
                            + tempNode.getNodeName() + " e=" + e);
                }
            }
        }
        return property;
    }

    private int parseCharacteristicPermission(final int profile, final Node node) {
        int permission = 0;
        TagName permissionType = null;
        final NodeList nodes = node.getChildNodes();

        checkNode(node, TagName.permission);
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node tempNode = nodes.item(i);
            if ("yes".equalsIgnoreCase(tempNode.getTextContent().toLowerCase())
                    || "y".equalsIgnoreCase(tempNode.getTextContent().toLowerCase())) {
                try {
                    permissionType = TagName.valueOf(tempNode.getNodeName());
                    switch (permissionType) {
                        case read:
                            permission |= BluetoothGattCharacteristic.PERMISSION_READ;
                            break;
                        case read_encrypted:
                            permission |= BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
                            break;
                        case read_encrypted_mitm:
                            permission |=
                                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
                            break;
                        case write:
                            permission |= BluetoothGattCharacteristic.PERMISSION_WRITE;
                            break;
                        case write_encrypted:
                            permission |= BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
                            break;
                        case write_encrypted_mitm:
                            permission |=
                                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM;
                            break;
                        case write_signed:
                            permission |= BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED;
                            break;
                        case write_signed_mitm:
                            permission |= BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM;
                            break;
                        default:
                            break;
                    }
                } catch (final IllegalArgumentException e) {
                    // ignore it
                    Log.e(TAG, "parseCharacteristicPermission - "
                            + "ignore tempNode.getNodeName()=" + tempNode.getNodeName()
                            + " e=" + e);
                }
            }
        }
        return permission;
    }

    private BluetoothGattDescriptor parseDescriptor(final int profile, final Node node) {
        int p = 0;
        UUID uuid = null;
        final NodeList nodes = node.getChildNodes();

        final NamedNodeMap map = node.getAttributes();
        final Node attrNode = map.getNamedItem(AttribteName.uuid.name());
        uuid = strToUuid(attrNode.getTextContent());

        checkNode(node, TagName.descriptor);
        if (null != nodes) {
            for (int i = 0; i < nodes.getLength(); i++) {
                final Node tempNode = nodes.item(i);
                try {
                    final TagName tag = TagName.valueOf(tempNode.getNodeName());
                    switch (tag) {
                        case permission:
                            p = parseDescritporPermission(profile, tempNode);
                            break;
                        default:
                            break;
                    }
                } catch (final IllegalArgumentException e) {
                    // ignore it
                    Log.e(TAG,
                            "parseDescriptor - ignore tempNode.getNodeName()="
                                    + tempNode.getNodeName() + " e=" + e);
                }
            }
        }
        return new BluetoothGattDescriptor(uuid, p);
    }

    private int parseDescritporPermission(final int profile, final Node node) {
        int permission = 0;
        TagName permissionType = null;
        final NodeList nodes = node.getChildNodes();

        checkNode(node, TagName.permission);
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node tempNode = nodes.item(i);
            if ("yes".equalsIgnoreCase(tempNode.getTextContent())
                    || "y".equalsIgnoreCase(tempNode.getTextContent())) {
                try {
                    permissionType = TagName.valueOf(tempNode.getNodeName());
                    switch (permissionType) {
                        case read:
                            permission |= BluetoothGattDescriptor.PERMISSION_READ;
                            break;
                        case read_encrypted:
                            permission |= BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED;
                            break;
                        case read_encrypted_mitm:
                            permission |= BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED_MITM;
                            break;
                        case write:
                            permission |= BluetoothGattDescriptor.PERMISSION_WRITE;
                            break;
                        case write_encrypted:
                            permission |= BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED;
                            break;
                        case write_encrypted_mitm:
                            permission |= BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED_MITM;
                            break;
                        case write_signed:
                            permission |= BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED;
                            break;
                        case write_signed_mitm:
                            permission |= BluetoothGattDescriptor.PERMISSION_WRITE_SIGNED_MITM;
                            break;
                        default:
                            break;
                    }
                } catch (final IllegalArgumentException e) {
                    // ignore it
                    Log.e(TAG, "parseDescritporPermission - ignore tempNode.getNodeName()="
                            + tempNode.getNodeName() + " e=" + e);
                }
            }
        }
        return permission;
    }

    private static void checkNode(final Node node, final TagName tagName) {
        if (!tagName.name().equals(node.getNodeName())) {
            throw new IllegalArgumentException("[BleProfileServicesFactory] checkNode:"
                    + node.getNodeName() + "!=" + tagName.name());
        }
    }

    private static UUID strToUuid(final String strUuid) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(strUuid);
        } catch (final IllegalArgumentException e) {
            uuid = UUID.fromString(strUuid + UUID_SUFFIX);
        }
        return uuid;
    }

    private static int strToServiceType(final String strType) {
        if ("primary".equalsIgnoreCase(strType)) {
            return BluetoothGattService.SERVICE_TYPE_PRIMARY;
        } else if ("secondary".equalsIgnoreCase(strType)) {
            return BluetoothGattService.SERVICE_TYPE_SECONDARY;
        } else {
            throw new IllegalArgumentException("[BleProfileServicesFactory] strToServiceType:"
                    + strType);
        }
    }
}
