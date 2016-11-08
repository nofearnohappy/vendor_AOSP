package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.MediaFolder;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.R;
import com.mediatek.rcs.pam.message.PAMFileMessage;
import com.mediatek.rcs.pam.provider.PAContract;
import com.mediatek.rcs.pam.provider.PAContract.MediaArticleColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaBasicColumns;
import com.mediatek.rcs.pam.provider.PAContract.MediaColumns;
import com.mediatek.rcs.pam.provider.PAContract.MessageColumns;
import com.mediatek.rcs.pam.provider.RcseProviderContract;
import com.mediatek.rcs.pam.util.Utils;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class MessageContent implements SanityCheck {
    private static final String TAG = Constants.TAG_PREFIX + "MessageContent";
    private static final int TEXT_DIGEST_LENGTH = 140;

    public int mediaType;
    public long createTime;
    public String messageUuid;
    public String smsDigest;
    public String text;
    public List<MediaArticle> article;
    public MediaBasic basicMedia;
    public String publicAccountUuid;
    public int direction;
    public String chatId;
    public int status;
    public int forwardable;
    public int activeStatus;
    public long timestamp;
    public int system;
    //for CMCC
    public int chatType;
    public String mimeType;
    public String body;

    // Android Specific
    public long id = Constants.INVALID;
    public String sourceId;
    public int sourceTable = Constants.INVALID;
    public long accountId = Constants.INVALID;
    // Used by GeoLoc and vCard
    public long mediaId = Constants.INVALID;
    public String mediaPath;

    public MessageContent() {
        article = new LinkedList<MediaArticle>();
    }

    /*
     * This method will update the smsDigest field.
     */
    public void generateSmsDigest(Context context) {
        switch (mediaType) {
        case Constants.MEDIA_TYPE_TEXT:
            if (text.length() > TEXT_DIGEST_LENGTH) {
                smsDigest = text.substring(0, TEXT_DIGEST_LENGTH - 3) + "...";
            } else {
                smsDigest = text;
            }
            break;
        case Constants.MEDIA_TYPE_PICTURE:
            if (!TextUtils.isEmpty(basicMedia.title)) {
                smsDigest = context.getResources().
                        getString(R.string.sms_digest_template_pic) + basicMedia.title;
            } else {
                smsDigest = context.getResources().
                        getString(R.string.sms_digest_template_pic) + basicMedia.originalUrl;
            }
            break;
        case Constants.MEDIA_TYPE_VIDEO:
            if (!TextUtils.isEmpty(basicMedia.title)) {
                smsDigest = context.getResources().
                        getString(R.string.sms_digest_template_video) + basicMedia.title;
            } else {
                smsDigest = context.getResources().getString(R.string.sms_digest_template_video)
                        + basicMedia.originalUrl;
            }
            break;
        case Constants.MEDIA_TYPE_AUDIO:
            if (!TextUtils.isEmpty(basicMedia.title)) {
                smsDigest = context.getResources().
                        getString(R.string.sms_digest_template_audio) + basicMedia.title;
            } else {
                smsDigest = context.getResources().getString(R.string.sms_digest_template_audio)
                        + basicMedia.originalUrl;
            }
            break;
        case Constants.MEDIA_TYPE_GEOLOC:
            smsDigest = context.getResources().getString(R.string.sms_digest_template_geoloc);
            break;
        case Constants.MEDIA_TYPE_VCARD:
            smsDigest = context.getResources().getString(R.string.sms_digest_template_vcard);
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            // Follow WeChat's style
            smsDigest = context.getResources().
                getString(R.string.sms_digest_template_article) + article.get(0).title;
            break;
        case Constants.MEDIA_TYPE_SMS:
            /* Do nothing. The SMS content should already be stored in sms_digest. */
        default:
            break;
        }
    }

    @Override
    public void checkSanity() throws PAMException {
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                      (createTime == Constants.INVALID || timestamp == Constants.INVALID));
        switch (mediaType) {
        case Constants.MEDIA_TYPE_TEXT:
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, (text == null));
            break;
        case Constants.MEDIA_TYPE_PICTURE:
        case Constants.MEDIA_TYPE_VIDEO:
        case Constants.MEDIA_TYPE_AUDIO:
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, (basicMedia == null));
            basicMedia.checkSanity();
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING, (article == null));
            for (MediaArticle ma : article) {
                ma.checkSanity();
            }
            break;
        case Constants.MEDIA_TYPE_SMS:
            /* do nothing */
        default:
            break;
        }
    }

    public String buildMediaArticleString() {
        if (mediaType != Constants.MEDIA_TYPE_SINGLE_ARTICLE &&
            mediaType != Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            return null;
        }
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer.startTag(null, CommonXmlTags.ARTICLE);
            for (MediaArticle ma : article) {
                serializer
                    .startTag(null, CommonXmlTags.AUTHOR)
                    .text(ma.author)
                    .endTag(null, CommonXmlTags.AUTHOR)
                    .startTag(null, CommonXmlTags.BODY_LINK)
                    .text(ma.bodyUrl)
                    .endTag(null, CommonXmlTags.BODY_LINK)
                    .startTag(null, CommonXmlTags.MAIN_TEXT)
                    .text(ma.mainText)
                    .endTag(null, CommonXmlTags.MAIN_TEXT)
                    .startTag(null, CommonXmlTags.MEDIA_UUID)
                    .text(ma.mediaUuid)
                    .endTag(null, CommonXmlTags.MEDIA_UUID)
                    .startTag(null, CommonXmlTags.ORIGINAL_LINK)
                    .text(ma.originalUrl)
                    .endTag(null, CommonXmlTags.ORIGINAL_LINK)
                    .startTag(null, CommonXmlTags.SOURCE_LINK)
                    .text(ma.sourceUrl)
                    .endTag(null, CommonXmlTags.SOURCE_LINK)
                    .startTag(null, CommonXmlTags.THUMB_LINK)
                    .text(ma.thumbnailUrl)
                    .endTag(null, CommonXmlTags.THUMB_LINK)
                    .startTag(null, CommonXmlTags.TITLE)
                    .text(ma.title)
                    .endTag(null, CommonXmlTags.TITLE);
            }
            serializer.endTag(null, CommonXmlTags.ARTICLE);
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"MessageContent\", mediaType:\"");
        String typeString = getMediaTypeString(mediaType);
        if (typeString == null) {
            throw new Error("Unsupported media type " + mediaType);
        }
        sb.append(typeString + "(" + mediaType + ")").append("\", createTime:\"").append(createTime)
                .append("\", messageUuid:\"").append(messageUuid).append("\", smsDigest:\"")
                .append(smsDigest).append("\", text:\"").append(text).append("\", article:[");
        for (int i = 0; i < article.size(); ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(article.get(i).toString());
        }
        sb.append("], basicMedia:").append(basicMedia)
        .append(", pa_uuid:\"").append(publicAccountUuid).append("\"}");
        return sb.toString();
    }

    private static String getMediaTypeString(int type) {
        switch (type) {
        case Constants.MEDIA_TYPE_TEXT:
            return "text";
        case Constants.MEDIA_TYPE_VCARD:
            return "vcard";
        case Constants.MEDIA_TYPE_GEOLOC:
            return "geoloc";
        case Constants.MEDIA_TYPE_PICTURE:
            return "picture";
        case Constants.MEDIA_TYPE_VIDEO:
            return "video";
        case Constants.MEDIA_TYPE_AUDIO:
            return "audio";
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            return "article";
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            return "articles";
        case Constants.MEDIA_TYPE_SMS:
            return "sms";
        default:
            return null;
        }
    }

    public void loadFromRcseMessageProviderCursor(Context context, Cursor c) {
        // id will be allocated by PAProvider
        sourceId = c.getString(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.MESSAGE_ID));
        sourceTable = Constants.TABLE_MESSAGE;
        timestamp = c.getLong(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.TIMESTAMP));
        publicAccountUuid = c.getString(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.CONTACT_NUMBER));
        publicAccountUuid = Utils.extractUuidFromSipUri(publicAccountUuid);
        accountId = PublicAccount.queryAccountId(context, publicAccountUuid, false);
        if (accountId == Constants.INVALID) {
            Log.e(TAG, "Cannot find uuid " + publicAccountUuid + " in account provider");
        }
        chatId = c.getString(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.CHAT_ID));
        status = c.getInt(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.MESSAGE_STATUS));
        direction = c.getInt(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.DIRECTION));
        mimeType = c.getString(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.MIME_TYPE));
        timestamp = c.getLong(c.getColumnIndexOrThrow(
                RcseProviderContract.MessageColumns.TIMESTAMP));

        // we are assuming incoming messages is either text message or xml message
        if (mimeType.equals("text/plain")) {
            mediaType = Constants.MEDIA_TYPE_TEXT;
            try {
                text = IOUtils.toString(c.getBlob(c.getColumnIndexOrThrow(
                        RcseProviderContract.MessageColumns.BODY)), "utf-8");
                body = text;
            } catch (IllegalArgumentException | IOException e) {
                e.printStackTrace();
                throw new Error(e);
            }
            createTime = timestamp;
            forwardable = Constants.MESSAGE_FORWARDABLE_YES;
        } else if (mimeType.equals("application/xml")) {
            try {
                text = IOUtils.toString(c.getBlob(c.getColumnIndexOrThrow(
                        RcseProviderContract.MessageColumns.BODY)), "utf-8");
                body = text;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(text.getBytes("UTF-8")));
                parseIncomingXmlContent(doc);
                if (createTime == Constants.INVALID) {
                    Log.w(TAG, "Failed to parse timestamp from xml." +
                            " Use timestamp from RCSe Provider instead. [Workaround]");
                    createTime = timestamp;
                }
                if (basicMedia != null) {
                    basicMedia.accountId = accountId;
                }
            } catch (XPathExpressionException | SAXException |
                    ParserConfigurationException | IOException e) {
                e.printStackTrace();
                throw new Error(e);
            }
        } else {
            throw new Error("Invalid mime type " + mimeType);
        }

        /* for CMCC */
        body = text;
        if (mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE ||
            mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            chatType = com.cmcc.ccs.chat.ChatService.XML;
            mimeType = "application/xml";
        } else if (mediaType == Constants.MEDIA_TYPE_TEXT) {
            chatType = com.cmcc.ccs.chat.ChatService.IM;
            mimeType = "text/plain";
        } else if (mediaType == Constants.MEDIA_TYPE_AUDIO ||
                   mediaType == Constants.MEDIA_TYPE_PICTURE ||
                   mediaType == Constants.MEDIA_TYPE_VIDEO) {
            chatType = com.cmcc.ccs.chat.ChatService.XML;
            mimeType = PAMFileMessage.FILE_TYPES.get(basicMedia.fileType);
        } else if (mediaType == Constants.MEDIA_TYPE_GEOLOC ||
                   mediaType == Constants.MEDIA_TYPE_VCARD) {
            chatType = com.cmcc.ccs.chat.ChatService.XML;
            mimeType = (mediaType == Constants.MEDIA_TYPE_GEOLOC) ?
                       "application/vnd.gsma.rcspushlocation+xml" : "text/vcard";
        }
    }

    private void parseMediaArticle(XPath xpath, Node node, MediaArticle mediaArticle)
            throws XPathExpressionException {
        // GET resource-lists/msg_content/article/mediaarticle/title
        XPathExpression expr = xpath.compile(CommonXmlTags.TITLE);
        Node n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.title = n == null ? null : n.getTextContent();

        // GET resource-lists/msg_content/article/mediaarticle/author
        expr = xpath.compile(CommonXmlTags.AUTHOR);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.author = n == null ? null : n.getTextContent();

        // GET resource-lists/msg_content/article/mediaarticle/body_link
        expr = xpath.compile(CommonXmlTags.BODY_LINK);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.bodyUrl = n == null ? null : n.getTextContent();
        if (mediaArticle.bodyUrl != null) {
            mediaArticle.bodyUrl = mediaArticle.bodyUrl.trim();
        }

        // GET resource-lists/msg_content/article/mediaarticle/main_text
        expr = xpath.compile(CommonXmlTags.MEDIA_UUID);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.mediaUuid = n == null ? null : n.getTextContent();

        // GET resource-lists/msg_content/article/mediaarticle/media_uuid
        expr = xpath.compile(CommonXmlTags.MAIN_TEXT);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.mainText = n == null ? null : n.getTextContent();

        // GET resource-lists/msg_content/article/mediaarticle/original_link
        expr = xpath.compile(CommonXmlTags.ORIGINAL_LINK);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.originalUrl = n == null ? null : n.getTextContent();
        if (mediaArticle.originalUrl != null) {
            mediaArticle.originalUrl = mediaArticle.originalUrl.trim();
        }

        // GET resource-lists/msg_content/article/mediaarticle/source_link
        expr = xpath.compile(CommonXmlTags.SOURCE_LINK);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.sourceUrl = n == null ? null : n.getTextContent();
        if (mediaArticle.sourceUrl != null) {
            mediaArticle.sourceUrl = mediaArticle.sourceUrl.trim();
        }

        // GET resource-lists/msg_content/article/mediaarticle/thumb_link
        expr = xpath.compile(CommonXmlTags.THUMB_LINK);
        n = (Node) expr.evaluate(node, XPathConstants.NODE);
        mediaArticle.thumbnailUrl = n == null ? null : n.getTextContent();
        if (mediaArticle.thumbnailUrl != null) {
            mediaArticle.thumbnailUrl = mediaArticle.thumbnailUrl.trim();
        }
    }

    public static MessageContent buildFromRcseMessageProviderCursor(Context context, Cursor c) {
        MessageContent result = new MessageContent();
        result.loadFromRcseMessageProviderCursor(context, c);
        return result;
    }

    public void parseIncomingXmlContent(Document doc) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression expr = null;
        Node node = null;

        // GET resource-lists/msg_content/create_time
        expr = xpath.compile(makePath(
                CommonXmlTags.RESOURCE_LISTS,
                CommonXmlTags.MSG_CONTENT,
                CommonXmlTags.CREATE_TIME));
        node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        createTime = node == null ? Constants.INVALID :
            Utils.convertStringToTimestamp(node.getTextContent().trim());

        // GET resource-lists/msg_content/forwardable
        expr = xpath.compile(makePath(
                CommonXmlTags.RESOURCE_LISTS,
                CommonXmlTags.MSG_CONTENT,
                CommonXmlTags.FORWARDABLE));
        node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        try {
            forwardable = node == null ? Constants.INVALID :
                Integer.parseInt(node.getTextContent().trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            forwardable = Constants.INVALID;
        }


        // GET resource-lists/msg_content/media_type
        expr = xpath.compile(makePath(
                CommonXmlTags.RESOURCE_LISTS,
                CommonXmlTags.MSG_CONTENT,
                CommonXmlTags.MEDIA_TYPE));
        node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        try {
            mediaType = node == null ? Constants.INVALID :
                Integer.parseInt(node.getTextContent().trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            mediaType = Constants.INVALID;
        }

        // GET resource-lists/msg_content/msg_uuid
        expr = xpath.compile(makePath(
                CommonXmlTags.RESOURCE_LISTS,
                CommonXmlTags.MSG_CONTENT,
                CommonXmlTags.MSG_UUID));
        node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        messageUuid = node == null ? null : node.getTextContent().trim();

        // GET resource-lists/msg_content/activeStatus
        expr = xpath.compile(makePath(
                CommonXmlTags.RESOURCE_LISTS,
                CommonXmlTags.MSG_CONTENT,
                CommonXmlTags.ACTIVESTATUS2));
        node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        try {
            activeStatus = node == null ? Constants.INVALID :
                Integer.parseInt(node.getTextContent().trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            activeStatus = Constants.INVALID;
        }

        // GET resource-lists/msg_content/pa_uuid
        expr = xpath.compile(makePath(
                CommonXmlTags.RESOURCE_LISTS,
                CommonXmlTags.MSG_CONTENT,
                CommonXmlTags.PA_UUID));
        node = (Node) expr.evaluate(doc, XPathConstants.NODE);
        publicAccountUuid = node == null ? null : node.getTextContent();

        if (mediaType == Constants.MEDIA_TYPE_TEXT ||
            mediaType == Constants.MEDIA_TYPE_GEOLOC ||
            mediaType == Constants.MEDIA_TYPE_VCARD) {
            // GET resource-lists/msg_content/text
            expr = xpath.compile(
                    makePath(CommonXmlTags.RESOURCE_LISTS,
                            CommonXmlTags.MSG_CONTENT,
                            CommonXmlTags.TEXT));
            node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            text = node == null ? null : node.getTextContent();
        } else if (mediaType == Constants.MEDIA_TYPE_PICTURE ||
                   mediaType == Constants.MEDIA_TYPE_AUDIO ||
                   mediaType == Constants.MEDIA_TYPE_VIDEO) {

            // GET resource-lists/msg_content/media
            expr = xpath.compile(makePath(
                    CommonXmlTags.RESOURCE_LISTS,
                    CommonXmlTags.MSG_CONTENT,
                    CommonXmlTags.MEDIA));
            Node mediaNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (mediaNode == null) {
                Log.e(TAG, "No media node. Try to find tag according to media type.");
                if (mediaType == Constants.MEDIA_TYPE_PICTURE) {
                    expr = xpath.compile(makePath(
                            CommonXmlTags.RESOURCE_LISTS,
                            CommonXmlTags.MSG_CONTENT,
                            CommonXmlTags.PIC));
                    mediaNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
                    if (mediaNode == null) {
                        throw new Error("No pic node");
                    }
                } else if (mediaType == Constants.MEDIA_TYPE_AUDIO) {
                    expr = xpath.compile(makePath(
                            CommonXmlTags.RESOURCE_LISTS,
                            CommonXmlTags.MSG_CONTENT,
                            CommonXmlTags.AUDIO));
                    mediaNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
                    if (mediaNode == null) {
                        throw new Error("No audio node");
                    }
                } else if (mediaType == Constants.MEDIA_TYPE_VIDEO) {
                    expr = xpath.compile(makePath(
                            CommonXmlTags.RESOURCE_LISTS,
                            CommonXmlTags.MSG_CONTENT,
                            CommonXmlTags.VIDEO));
                    mediaNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
                    if (mediaNode == null) {
                        throw new Error("No video node");
                    }
                }
            }

            basicMedia = new MediaBasic();

            // GET resource-lists/msg_content/media/title
            expr = xpath.compile(CommonXmlTags.TITLE);
            Node n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            basicMedia.title = (n == null) ? null : n.getTextContent();

            // GET resource-lists/msg_content/media/description
            expr = xpath.compile(CommonXmlTags.DESCRIPTION);
            n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            basicMedia.description = (n == null) ? null : n.getTextContent();

            // GET resource-lists/msg_content/media/original_link
            expr = xpath.compile(CommonXmlTags.ORIGINAL_LINK);
            n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            basicMedia.originalUrl = (n == null) ? null : n.getTextContent();
            if (basicMedia.originalUrl != null) {
                basicMedia.originalUrl = basicMedia.originalUrl.trim();
            }
            // GET resource-lists/msg_content/media/thumb_link
            expr = xpath.compile(CommonXmlTags.THUMB_LINK);
            n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            basicMedia.thumbnailUrl = (n == null) ? null : n.getTextContent();
            if (basicMedia.thumbnailUrl != null) {
                basicMedia.thumbnailUrl = basicMedia.thumbnailUrl.trim();
            }

            // GET resource-lists/msg_content/media/filesize
            expr = xpath.compile(CommonXmlTags.FILESIZE);
            n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            // Convert back and forth to remove B/KB/MB etc
            basicMedia.fileSize = (n == null) ? null :
                Integer.toString(Utils.extractSize(n.getTextContent()));

            // GET resource-lists/msg_content/media/filetype
            expr = xpath.compile(CommonXmlTags.FILETYPE);
            n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            basicMedia.fileType = (n == null) ? null : n.getTextContent();

            // GET resource-lists/msg_content/media/media_uuid
            expr = xpath.compile(CommonXmlTags.MEDIA_UUID);
            n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
            basicMedia.mediaUuid = (n == null) ? null : n.getTextContent();

            if (mediaType == Constants.MEDIA_TYPE_AUDIO ||
                mediaType == Constants.MEDIA_TYPE_VIDEO) {
                // GET resource-lists/msg_content/media/duration
                expr = xpath.compile(CommonXmlTags.DURATION);
                n = (Node) expr.evaluate(mediaNode, XPathConstants.NODE);
                basicMedia.duration = (n == null) ? null : n.getTextContent().trim();
            }

        } else if (mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                || mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            // GET resource-lists/msg_content/article/mediaarticle
            expr = xpath.compile(makePath(
                    CommonXmlTags.RESOURCE_LISTS,
                    CommonXmlTags.MSG_CONTENT,
                    CommonXmlTags.ARTICLE,
                    CommonXmlTags.MEDIAARTICLE));
            NodeList topicNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            if (topicNodes != null) {
                for (int i = 0; i < topicNodes.getLength(); i++) {
                    Node topicNode = topicNodes.item(i);
                    MediaArticle mediaArticle = new MediaArticle();
                    parseMediaArticle(xpath, topicNode, mediaArticle);
                    article.add(mediaArticle);
                }
            }
        }
    }

    public void storeToContentValues(ContentValues cv) {
        // Android Specific Data
        if (id != Constants.INVALID) {
            cv.put(MessageColumns.ID, id);
        }
        if (sourceTable != Constants.INVALID) {
            cv.put(PAContract.MessageColumns.SOURCE_ID, sourceId);
            cv.put(PAContract.MessageColumns.SOURCE_TABLE, sourceTable);
        }
        if (accountId != Constants.INVALID) {
            cv.put(MessageColumns.ACCOUNT_ID, accountId);
        }
        // Message Content
        cv.put(MessageColumns.ACCOUNT_UUID, publicAccountUuid);
        cv.put(MessageColumns.TYPE, mediaType);
        cv.put(MessageColumns.TIMESTAMP, timestamp);
        cv.put(MessageColumns.CREATE_TIME, createTime);
        cv.put(MessageColumns.UUID, messageUuid);
        cv.put(MessageColumns.SMS_DIGEST, smsDigest);
        cv.put(MessageColumns.TEXT, text);
        cv.put(MessageColumns.DIRECTION, direction);
        cv.put(MessageColumns.CHAT_ID, chatId);
        cv.put(MessageColumns.STATUS, status);
        cv.put(MessageColumns.FORWARDABLE, forwardable);
        cv.put(MessageColumns.SYSTEM, system);
        /* for CMCC */
        cv.put(MessageColumns.CHAT_TYPE, chatType);
        cv.put(MessageColumns.BODY, body);
        cv.put(MessageColumns.MIME_TYPE, mimeType);
        if (basicMedia != null && basicMedia.id != Constants.INVALID) {
            cv.put(MessageColumns.DATA1, basicMedia.id);
        } else if (mediaId != Constants.INVALID) {
            cv.put(PAContract.MessageColumns.DATA1, mediaId);
        } else {
            for (int i = 0; i < article.size(); ++i) {
                MediaArticle ma = article.get(i);
                if (ma.id != Constants.INVALID) {
                    cv.put(PAContract.MESSAGE_DATA_COLUMN_LIST[i], ma.id);
                }
            }
        }
    }

    public static String[] sFullProjection = {
        MessageColumns.ID,
        MessageColumns.ACCOUNT_ID,
        MessageColumns.CHAT_ID,
        MessageColumns.DATA1,
        MessageColumns.DATA2,
        MessageColumns.DATA3,
        MessageColumns.DATA4,
        MessageColumns.DATA5,
        MessageColumns.DIRECTION,
        MessageColumns.FORWARDABLE,
        MessageColumns.SMS_DIGEST,
        MessageColumns.SOURCE_ID,
        MessageColumns.SOURCE_TABLE,
        MessageColumns.STATUS,
        MessageColumns.SYSTEM,
        MessageColumns.TEXT,
        MessageColumns.TIMESTAMP,
        MessageColumns.CREATE_TIME,
        MessageColumns.TYPE,
        MessageColumns.UUID,
    };

    public static MessageContent buildFromPAProviderCursor(ContentResolver cr, Cursor c) {
        MessageContent result = new MessageContent();
        result.id = c.getLong(c
                .getColumnIndexOrThrow(MessageColumns.ID));;
        result.accountId = c.getLong(c
                .getColumnIndexOrThrow(MessageColumns.ACCOUNT_ID));
        result.chatId = c.getString(c
                .getColumnIndexOrThrow(MessageColumns.CHAT_ID));
        result.direction = c.getInt(c
                .getColumnIndexOrThrow(MessageColumns.DIRECTION));
        result.forwardable = c.getInt(c
                .getColumnIndexOrThrow(MessageColumns.FORWARDABLE));
        result.smsDigest = c.getString(c
                .getColumnIndexOrThrow(MessageColumns.SMS_DIGEST));
        result.sourceId = c.getString(c
                .getColumnIndexOrThrow(MessageColumns.SOURCE_ID));
        result.sourceTable = c.getInt(c
                .getColumnIndexOrThrow(MessageColumns.SOURCE_TABLE));
        result.status = c
                .getInt(c.getColumnIndexOrThrow(MessageColumns.STATUS));
        result.text = c.getString(c.getColumnIndexOrThrow(MessageColumns.TEXT));
        result.createTime = c.getLong(c
                .getColumnIndexOrThrow(MessageColumns.CREATE_TIME));
        result.timestamp = c.getLong(c
                .getColumnIndexOrThrow(MessageColumns.TIMESTAMP));
        result.mediaType = c.getInt(c
                .getColumnIndexOrThrow(MessageColumns.TYPE));
        result.messageUuid = c.getString(c
                .getColumnIndexOrThrow(MessageColumns.UUID));
        result.system = c.getInt(c
                .getColumnIndexOrThrow(MessageColumns.SYSTEM));
        if (result.mediaType == Constants.MEDIA_TYPE_GEOLOC
                || result.mediaType == Constants.MEDIA_TYPE_VCARD) {
            result.mediaId = c.getLong(c
                .getColumnIndexOrThrow(MessageColumns.DATA1));
            MediaEntry mediaEntry = MediaEntry.loadFromProvider(result.mediaId, cr);
            if (mediaEntry != null) {
                result.mediaPath = mediaEntry.path;
            }
        } else if (result.mediaType == Constants.MEDIA_TYPE_PICTURE
                    || result.mediaType == Constants.MEDIA_TYPE_AUDIO
                    || result.mediaType == Constants.MEDIA_TYPE_VIDEO) {
            final long id = c.getLong(c.getColumnIndexOrThrow(MessageColumns.DATA1));
            result.basicMedia = MediaBasic.loadFromProvider(id, cr);
        } else if (result.mediaType == Constants.MEDIA_TYPE_SINGLE_ARTICLE
                    || result.mediaType == Constants.MEDIA_TYPE_MULTIPLE_ARTICLE) {
            for (int i = 0; i < PAContract.MESSAGE_DATA_COLUMN_LIST.length; ++i) {
                long articleId = c.getLong(c
                            .getColumnIndexOrThrow(PAContract.MESSAGE_DATA_COLUMN_LIST[i]));
                if (articleId != Constants.INVALID) {
                    MediaArticle ma = MediaArticle.loadFromProvider(articleId,cr);
                    if (ma != null) {
                        result.article.add(ma);
                    } else {
                        Log.d(TAG,"Failed to load MediaArticle from provider: " + articleId);
                    }
                }
            }
        }
        return result;
    }

    public static MessageContent loadFromProvider(long messageId, ContentResolver cr) {
        return loadFromProvider(messageId, cr, false);
    }
    public static MessageContent loadFromProvider(
            long messageId, ContentResolver cr, boolean includingDeleted) {
        MessageContent result = null;
        Cursor c = null;
        try {
            c = cr.query(
                    Uri.parse(MessageColumns.CONTENT_URI_STRING + "?" +
                            PAContract.MESSAGES_PARAM_INCLUDING_DELETED +
                            "=" + Constants.INCLUDING_DELETED_YES),
                    sFullProjection,
                    MessageColumns.ID + "=?",
                    new String[]{Long.toString(messageId)},
                    null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                result = buildFromPAProviderCursor(cr, c);
            } else {
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
         }
        return result;
    }

    public long storeToProvider(ContentResolver cr, Context context) {
        long subId;

        if (TextUtils.isEmpty(smsDigest)) {
            generateSmsDigest(context);
        }
        ContentValues cv = new ContentValues();
        storeToContentValues(cv);
        // store extra resources
        switch (mediaType) {
        case Constants.MEDIA_TYPE_PICTURE:
        case Constants.MEDIA_TYPE_AUDIO:
        case Constants.MEDIA_TYPE_VIDEO:
            subId = basicMedia.storeToProvider(cr, mediaType);
            cv.put(MessageColumns.DATA1, subId);
            break;
        case Constants.MEDIA_TYPE_GEOLOC:
        case Constants.MEDIA_TYPE_VCARD:
            subId = storeContentToMediaFile(cr, text, mediaType);
            cv.put(MessageColumns.DATA1, subId);
            break;
        case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
        case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
            for (int i = 0; i < article.size(); ++i) {
                MediaArticle ma = article.get(i);
                subId = ma.storeToProvider(cr);
                cv.put(PAContract.MESSAGE_DATA_COLUMN_LIST[i], subId);
            }
            break;
        default:
            // do nothing
            break;
        }
        if (accountId != Constants.INVALID) {
            cv.put(MessageColumns.ACCOUNT_ID, accountId);
        }
        Uri uri = cr.insert(MessageColumns.CONTENT_URI, cv);
        id = Long.parseLong(uri.getLastPathSegment());

        return id;
    }

    /*
     * This method only applies to GeoLoc and vCard.
     */
    private long storeContentToMediaFile(ContentResolver cr, String content, int type) {
        mediaId = Constants.INVALID;
        String extension = null;
        if (type == Constants.MEDIA_TYPE_GEOLOC) {
            extension = ".xml";
        } else if (type == Constants.MEDIA_TYPE_VCARD) {
            extension = ".vcf";
        } else {
            throw new Error("Invalid media type: " + type);
        }
        String filename = MediaFolder.generateMediaFileName(Constants.INVALID, type, extension);
        if (filename == null) {
            Log.e(TAG, "storeContentToMediaFile failed to generate new file name");
            return mediaId;
        }

        try {
            File file = new File(filename);
            Utils.storeToFile(content, filename);

            // insert to media table
            ContentValues cv = new ContentValues();
            cv.put(MediaColumns.TYPE, type);
            cv.put(MediaColumns.TIMESTAMP, file.lastModified());
            cv.put(MediaColumns.PATH, file.getAbsolutePath());
            Uri uri = cr.insert(MediaColumns.CONTENT_URI, cv);
            mediaId = Long.parseLong(uri.getLastPathSegment());

            final String newPathString = MediaFolder.generateMediaFileName(
                    mediaId,
                    type,
                    MediaFolder.extractExtension(file.getName()));
            cv = new ContentValues();
            cv.put(MediaColumns.PATH, newPathString);
            cr.update(uri, cv, null, null);
            file.renameTo(new File(newPathString));
            Log.d(TAG, "storeMediaFile: " + file.getAbsolutePath() + " as " + mediaId);
        } catch (IOException e) {
            throw new Error(e);
        }
        return mediaId;
    }

    public void deleteFromProvider(ContentResolver cr) {

        MediaEntry me = null;

        cr.delete(MessageColumns.CONTENT_URI, MessageColumns.ID + "=?",
                new String[]{Long.toString(id)});
        me = new MediaEntry(mediaId, Constants.INVALID, null, null, 0);
        me.deleteFromProvider(cr);

        if (basicMedia != null) {
            me = new MediaEntry(basicMedia.originalId, Constants.INVALID, null, null, 0);
            me.deleteFromProvider(cr);
            me = new MediaEntry(basicMedia.thumbnailId, Constants.INVALID, null, null, 0);
            me.deleteFromProvider(cr);
            cr.delete(
                   MediaBasicColumns.CONTENT_URI,
                   MediaBasicColumns.ID + "=?",
                   new String[]{Long.toString(basicMedia.id)});
        }

        if (article != null && article.size() > 0) {
            for (MediaArticle ma : article) {
                if (ma != null) {
                    me = new MediaEntry(ma.originalId, Constants.INVALID, null, null, 0);
                    me.deleteFromProvider(cr);
                    me = new MediaEntry(ma.thumbnailId, Constants.INVALID, null, null, 0);
                    me.deleteFromProvider(cr);
                    cr.delete(
                            MediaArticleColumns.CONTENT_URI,
                            MediaArticleColumns.ID + "=?",
                            new String[]{Long.toString(ma.id)});
                } else {
                    Log.w(TAG, "MediaArticle is null");
                }
            }
        }
    }

    private static String makePath(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length - 1; ++i) {
            sb.append(segments[i]).append('/');
        }
        sb.append(segments[segments.length - 1]);
        return sb.toString();
    }
}
