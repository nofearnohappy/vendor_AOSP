package com.mediatek.rcs.message.cloudbackup.modules;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlSerializer;

import com.mediatek.rcs.message.cloudbackup.utils.CloudBrUtils.MmsXml;
import com.mediatek.rcs.message.cloudbackup.utils.EntryRecord.MmsXmlInfo;

import android.util.Xml;

class MmsXmlComposer {
    private XmlSerializer mSerializer = null;
    private StringWriter mStringWriter = null;

    boolean startCompose() {
        boolean result = false;
        mSerializer = Xml.newSerializer();
        mStringWriter = new StringWriter();
        try {
            mSerializer.setOutput(mStringWriter);
            // serializer.startDocument("UTF-8", null);
            mSerializer.startDocument(null, false);
            mSerializer.startTag("", MmsXml.ROOT);
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        return result;
    }

    boolean endCompose() {
        boolean result = false;
        try {
            mSerializer.endTag("", MmsXml.ROOT);
            mSerializer.endDocument();
            result = true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    boolean addOneMmsRecord(MmsXmlInfo record) {
        boolean result = false;
        try {
            mSerializer.startTag("", MmsXml.RECORD);
            mSerializer.attribute("", MmsXml.ID, record.getID());
            mSerializer.attribute("", MmsXml.ISREAD, record.getIsRead());
            mSerializer.attribute("", MmsXml.MSGBOX, record.getMsgBox());
            mSerializer.attribute("", MmsXml.DATE, record.getDate());
            mSerializer.attribute("", MmsXml.SIZE, record.getSize());
            mSerializer.attribute("", MmsXml.SIMID, record.getSimId());
            mSerializer.attribute("", MmsXml.ISLOCKED, record.getIsLocked());
            mSerializer.endTag("", MmsXml.RECORD);

            result = true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    String getXmlInfo() {
        if (mStringWriter != null) {
            return mStringWriter.toString();
        }

        return null;
    }
}
