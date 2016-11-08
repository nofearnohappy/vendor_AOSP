package com.mediatek.rcs.messageservice.modules;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Telephony.Mms;
import android.util.Log;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.GenericPdu;
import com.mediatek.rcs.messageservice.modules.Utils.MmsXml;
import com.mediatek.rcs.messageservice.modules.Utils.MmsXmlInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class MmsRestoreParser {
    private static final String CLASS_TAG = Utils.MODULE_TAG + "MmsRestoreParser";
    private ArrayList<MmsXmlInfo> mRecordList;

    protected void setRecordList(ArrayList<MmsXmlInfo> RecordList) {
        this.mRecordList = RecordList;
    }

    protected int parseMmsData(File xmlFile) {
        if (xmlFile == null && !xmlFile.exists() && !xmlFile.getName().endsWith("xml")) {
            Log.d(CLASS_TAG, "parseMmsData error. return");
            return Utils.ResultCode.BACKUP_FILE_ERROR;
        }
        int result = Utils.ResultCode.OK;
        Log.d(CLASS_TAG, "xmlFile:" + xmlFile.getName());

        String content = getXmlInfo(xmlFile.getPath());
        if (content != null) {
            result = parse(content);
        }

        return result;
    }

    /**
     * Describe <code>getXmlInfo</code> method here.
     *
     * @param fileName
     *            a <code>String</code> value
     * @return a <code>String</code> value
     */
    private String getXmlInfo(String fileName) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len = -1;
            byte[] buffer = new byte[512];
            while ((len = is.read(buffer, 0, 512)) != -1) {
                baos.write(buffer, 0, len);
            }

            return baos.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    protected int parse(String mmsString) {
        MmsXmlInfo record = null;
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(mmsString));

            int eventType = parser.getEventType();
            String tagName = "";
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;

                case XmlPullParser.START_TAG:
                    record = new MmsXmlInfo();
                    tagName = parser.getName();
                    if (tagName.equals(MmsXml.RECORD)) {
                        int attrNum = parser.getAttributeCount();
                        for (int i = 0; i < attrNum; ++i) {
                            String name = parser.getAttributeName(i);
                            String value = parser.getAttributeValue(i);
                            if (name.equals(MmsXml.ID)) {
                                record.setID(value);
                            } else if (name.equals(MmsXml.ISREAD)) {
                                record.setIsRead(value);
                            } else if (name.equals(MmsXml.MSGBOX)) {
                                record.setMsgBox(value);
                            } else if (name.equals(MmsXml.DATE)) {
                                record.setDate(value);
                            } else if (name.equals(MmsXml.SIZE)) {
                                record.setSize(value);
                            } else if (name.equals(MmsXml.SIMID)) {
                                record.setSimId(value);
                            } else if (name.equals(MmsXml.ISLOCKED)) {
                                record.setIsLocked(value);
                            }

                            Log.d(CLASS_TAG, "name:" + name + ",value:" + value);
                        }
                    }
                    break;

                case XmlPullParser.END_TAG:
                    if (parser.getName().equals(MmsXml.RECORD) && record != null) {
                        mRecordList.add(record);
                    }
                    break;

                case XmlPullParser.END_DOCUMENT:
                    break;

                default:
                    break;
                }

                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            return Utils.ResultCode.OTHER_EXCEPTION;
        } catch (IOException e) {
            e.printStackTrace();
            return Utils.ResultCode.IO_EXCEPTION;
        }

        return Utils.ResultCode.OK;
    }
}
