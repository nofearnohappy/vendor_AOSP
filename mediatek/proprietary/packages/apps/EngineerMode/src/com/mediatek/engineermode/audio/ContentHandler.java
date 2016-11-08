package com.mediatek.engineermode.audio;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 *This class deal with the xml file.
*/
public class ContentHandler extends DefaultHandler {

    private String mNodeName;

    private StringBuilder mOption;

    AudioLoggerXMLData mAudioLoggerXMLData;

    DumpOptions mDumpOptions = null;

    /**
     * This class get the AudioLoggerXMLData get from main.
     * @param xmlData
     *            xmlData
     * */
    public ContentHandler(AudioLoggerXMLData xmlData) {
        mAudioLoggerXMLData = xmlData;
    }

    @Override
    public void startDocument() throws SAXException {

        mOption = new StringBuilder();

        Log.d(Audio.TAG, "startDocument");
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
       Log.d(Audio.TAG, "uri:" + uri + " localName:" + localName + " qName:" + qName);

        mNodeName = localName;

        if ("Category".equals(mNodeName)) {

            String myTitle = attributes.getValue("title");

            if (!myTitle.isEmpty()) {
                Log.d(Audio.TAG, "myTitle:" + myTitle);

                mDumpOptions = new DumpOptions();

                mDumpOptions.mCategoryTitle = myTitle;

            }

        } else if ("Option".equals(mNodeName)) {

            String type = attributes.getValue("type");
            String mCmd = attributes.getValue("cmd");
            String check = attributes.getValue("check");
            String uncheck = attributes.getValue("uncheck");

            Log.d(Audio.TAG, "attributes.getValue(type):" + type);
            Log.d(Audio.TAG, "attributes.getValue(cmd):" + mCmd);
            Log.d(Audio.TAG, "attributes.getValue(check):" + check);
            Log.d(Audio.TAG, "attributes.getValue(uncheck):" + uncheck);

            // if(!str.isEmpty())
            mDumpOptions.mType.add(type);
            mDumpOptions.mCmd.add(mCmd);
            mDumpOptions.mCheck.add(check);
            mDumpOptions.mUncheck.add(uncheck);
        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {

        if ("Option".equals(mNodeName)) {
            mOption.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("Category".equals(localName)) {

            Log.d(Audio.TAG, "endElement,Category->mOption:" + mOption.toString());

            if (!mOption.toString().isEmpty()) {
                String[] str = mOption.toString().trim().replaceAll("\r|\n", ",")
                        .replaceAll("\\s*", "").split(",");

                for (int i = 0; i < str.length; i++) {
                    mDumpOptions.mCmdName.add(str[i]);
                }

                mAudioLoggerXMLData.mAudioDumpOperation.add(mDumpOptions);
            }
            mOption.setLength(0);
        } else if ("SetAudioCommand".equals(localName)) {
            String[] str = mOption.toString().trim().replaceAll("\r|\n", "@")
                    .replaceAll("\\s*", "").split("@");
            for (int i = 0; i < str.length; i++) {
                mAudioLoggerXMLData.setAudioCommandSetOperation(str[i]);
            }
            mOption.setLength(0);
        } else if ("GetAudioCommand".equals(localName)) {
            String[] str = mOption.toString().trim().replaceAll("\r|\n", ",")
                    .replaceAll("\\s*", "").split(",");
            for (int i = 0; i < str.length; i++) {
                mAudioLoggerXMLData.setAudioCommandGetOperation(str[i]);
            }
            mOption.setLength(0);
        } else if ("SetParameters".equals(localName)) {
            String[] str = mOption.toString().trim().replaceAll("\r|\n", ",")
                    .replaceAll("\\s*", "").split(",");
            for (int i = 0; i < str.length; i++) {
                mAudioLoggerXMLData.setParametersSetOperation(str[i]);
            }
            mOption.setLength(0);
        } else if ("GetParameters".equals(localName)) {
            String[] str = mOption.toString().trim().replaceAll("\r|\n", ",")
                    .replaceAll("\\s*", "").split(",");
            for (int i = 0; i < str.length; i++) {
                mAudioLoggerXMLData.setParametersGetOperation(str[i]);
            }
            mOption.setLength(0);
        }
    }

    @Override
    public void endDocument() throws SAXException {

        Log.d(Audio.TAG, "endDocument");

        for (int i = 0; i < mAudioLoggerXMLData.mAudioCommandSetOperation.size(); i++) {
            Log.d(Audio.TAG, "mAudioLoggerXMLData.mAudioCommandSetOperation:" + i + " : "
                    + mAudioLoggerXMLData.mAudioCommandSetOperation.get(i));
        }
        for (int i = 0; i < mAudioLoggerXMLData.mAudioCommandGetOperation.size(); i++) {
            Log.d(Audio.TAG, "mAudioLoggerXMLData.mAudioCommandGetOperation:" + i + " : "
                    + mAudioLoggerXMLData.mAudioCommandGetOperation.get(i));
        }
        for (int i = 0; i < mAudioLoggerXMLData.mParametersSetOperationItems.size(); i++) {
            Log.d(Audio.TAG, "mAudioLoggerXMLData.mParametersSetOperationItems:" + i
                    + " : " + mAudioLoggerXMLData.mParametersSetOperationItems.get(i));
        }

        for (int i = 0; i < mAudioLoggerXMLData.mParametersGetOperationItems.size(); i++) {
            Log.d(Audio.TAG, "mAudioLoggerXMLData.mParametersGetOperationItems:" + i
                    + " : " + mAudioLoggerXMLData.mParametersGetOperationItems.get(i));
        }

        for (int i = 0; i < mAudioLoggerXMLData.mAudioDumpOperation.size(); i++) {
            Log.d(Audio.TAG, "mAudioLoggerXMLData.mAudioDumpOperation,title:" + i + " : "
                    + mAudioLoggerXMLData.mAudioDumpOperation.get(i).mCategoryTitle);
            for (int j = 0; j < mAudioLoggerXMLData.mAudioDumpOperation.get(i).mCmdName
                    .size(); j++) {
                Log.d(Audio.TAG, "mAudioLoggerXMLData.mAudioDumpOperation,mCmd:"
                        + mAudioLoggerXMLData.mAudioDumpOperation.get(i).mCmd.get(j));
                Log.d(Audio.TAG, "mAudioLoggerXMLData.mAudioDumpOperation,mCmd name:"
                        + mAudioLoggerXMLData.mAudioDumpOperation.get(i).mCmdName.get(j));
            }

        }

    }

}
