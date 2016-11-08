package com.mediatek.rcs.pam.model;

import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

public class XmlMessageBuilder {

    private void buildGeneralInfoTag(XmlSerializer serializer, GeneralInfo info)
            throws IllegalArgumentException, IllegalStateException, IOException {
        serializer
            .startTag(null, CommonXmlTags.GENERALINFO)
            .startTag(null, CommonXmlTags.MSGNAME)
            .text(info.messageName)
            .endTag(null, CommonXmlTags.MSGNAME)
            .startTag(null, CommonXmlTags.VERSION)
            .text(info.version)
            .endTag(null, CommonXmlTags.VERSION)
            .startTag(null, CommonXmlTags.USERID)
            .text(info.userId)
            .endTag(null, CommonXmlTags.USERID)
            .endTag(null, CommonXmlTags.GENERALINFO);
    }

    public String buildSubscribeRequest(String accountUuid, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.SUBSCRIBE, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer.startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(accountUuid)
                .endTag(null, CommonXmlTags.PA_UUID)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * @param ids only ids[0] is used
     * @param userId
     * @return
     */
    public String buildUnsubscribeRequest(String id, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.UNSUBSCRIBE, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer.startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(id)
                .endTag(null, CommonXmlTags.PA_UUID)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildGetSubscribedListRequest(
            int order, int pageSize, int pageNumber, String userId) {
        try {
            GeneralInfo info = GeneralInfo.
                    buildRequestInfo(MessageName.GET_SUBSCRIBED_LIST, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.ORDER)
                .text(Integer.toString(order))
                .endTag(null, CommonXmlTags.ORDER)
                .startTag(null, CommonXmlTags.PAGESIZE)
                .text(Integer.toString(pageSize))
                .endTag(null, CommonXmlTags.PAGESIZE)
                .startTag(null, CommonXmlTags.PAGENUM)
                .text(Integer.toString(pageNumber))
                .endTag(null, CommonXmlTags.PAGENUM)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildSearchRequest(
            String keyword, int order, int pageSize, int pageNumber, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.SEARCH, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.KEYWORD)
                .text(keyword)
                .endTag(null, CommonXmlTags.KEYWORD)
                .startTag(null, CommonXmlTags.ORDER)
                .text(Integer.toString(order))
                .endTag(null, CommonXmlTags.ORDER)
                .startTag(null, CommonXmlTags.PAGESIZE)
                .text(Integer.toString(pageSize))
                .endTag(null, CommonXmlTags.PAGESIZE)
                .startTag(null, CommonXmlTags.PAGENUM)
                .text(Integer.toString(pageNumber))
                .endTag(null, CommonXmlTags.PAGENUM)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildGetDetailsRequest(String uuid, String timestamp, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.GET_DETAILS, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(uuid)
                .endTag(null, CommonXmlTags.PA_UUID);
            if (!TextUtils.isEmpty(timestamp)) {
                serializer
                    .startTag(null, CommonXmlTags.UPDATETIME)
                    .text(timestamp)
                    .endTag(null, CommonXmlTags.UPDATETIME);
            }
            serializer.endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildGetMenuRequest(String uuid, String timestamp, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.GET_MENU, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(uuid)
                .endTag(null, CommonXmlTags.PA_UUID);
            if (!TextUtils.isEmpty(timestamp)) {
                serializer
                    .startTag(null, CommonXmlTags.MENUTIMESTAMP)
                    .text(timestamp)
                    .endTag(null, CommonXmlTags.MENUTIMESTAMP);
            } else {
                serializer
                .startTag(null, CommonXmlTags.MENUTIMESTAMP)
                .endTag(null, CommonXmlTags.MENUTIMESTAMP);
            }
            serializer.endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildGetMessageHistoryRequest(
            String uuid,
            String timestamp,
            int order,
            int pageSize,
            int pageNumber,
            String userId) {
        try {
            GeneralInfo info = GeneralInfo.
                    buildRequestInfo(MessageName.GET_MESSAGE_HISTORY, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(uuid)
                .endTag(null, CommonXmlTags.PA_UUID)
                .startTag(null, CommonXmlTags.TIMESTAMP)
                .text(timestamp)
                .endTag(null, CommonXmlTags.TIMESTAMP)
                .startTag(null, CommonXmlTags.ORDER)
                .text(Integer.toString(order))
                .endTag(null, CommonXmlTags.ORDER)
                .startTag(null, CommonXmlTags.PAGESIZE)
                .text(Integer.toString(pageSize))
                .endTag(null, CommonXmlTags.PAGESIZE)
                .startTag(null, CommonXmlTags.PAGENUM)
                .text(Integer.toString(pageNumber))
                .endTag(null, CommonXmlTags.PAGENUM)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     *
     * @param uuid
     * @param reason may be null
     * @return
     */
    public String buildComplainRequest(
            String uuid,
            int type,
            String reason,
            String data,
            String description,
            String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.COMPLAIN, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(uuid)
                .endTag(null, CommonXmlTags.PA_UUID)
                .startTag(null, CommonXmlTags.TYPE)
                .text(Integer.toString(type))
                .endTag(null, CommonXmlTags.TYPE);
            if (data != null) {
                serializer
                    .startTag(null, CommonXmlTags.DATA)
                    .text(data)
                    .endTag(null, CommonXmlTags.DATA);
            }
            serializer
                .startTag(null, CommonXmlTags.DESCRIPTION)
                .text(description)
                .endTag(null, CommonXmlTags.DESCRIPTION);
            if (reason != null) {
                serializer
                    .startTag(null, CommonXmlTags.REASON)
                    .text(reason)
                    .endTag(null, CommonXmlTags.REASON);
            }
            serializer
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildGetRecommendsRequest(int type, int pageSize, int pageNumber, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.GET_RECOMMENDS, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.TYPE)
                .text(Integer.toString(type))
                .endTag(null, CommonXmlTags.TYPE)
                .startTag(null, CommonXmlTags.PAGESIZE)
                .text(Integer.toString(pageSize))
                .endTag(null, CommonXmlTags.PAGESIZE)
                .startTag(null, CommonXmlTags.PAGENUM)
                .text(Integer.toString(pageNumber))
                .endTag(null, CommonXmlTags.PAGENUM)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public String buildSetAcceptStatusRequest(String uuid, int acceptStatus, String userId) {
        try {
            GeneralInfo info = GeneralInfo.buildRequestInfo(MessageName.SET_ACCEPT_STATUS, userId);
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer
                .startTag(null, CommonXmlTags.BODY);
            buildGeneralInfoTag(serializer, info);
            serializer
                .startTag(null, CommonXmlTags.PA_UUID)
                .text(uuid)
                .endTag(null, CommonXmlTags.PA_UUID)
                .startTag(null, CommonXmlTags.ACCEPTSTATUS2)
                .text(Integer.toString(acceptStatus))
                .endTag(null, CommonXmlTags.ACCEPTSTATUS2)
                .endTag(null, CommonXmlTags.BODY);
            serializer.endDocument();
            return writer.toString();
        } catch (XmlPullParserException e) {
            throw new Error(e);
        } catch (IllegalArgumentException e) {
            throw new Error(e);
        } catch (IllegalStateException e) {
            throw new Error(e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }
}
