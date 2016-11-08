package com.mediatek.rcs.pam.model;

import android.text.TextUtils;
import android.util.Log;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XmlMessageParser {
    private static final String TAG = "PAM/XmlMessageParser";

    private final XPathExpression mExprUuid;
    private final XPathExpression mExprName;
    private final XPathExpression mExprLogo;
    private final XPathExpression mExprResult;
    private final XPathExpression mExprRecommendLevel;
    private final XPathExpression mExprCompany;
    private final XPathExpression mExprIntroduction;
    private final XPathExpression mExprType;
    private final XPathExpression mExprIdType;
    private final XPathExpression mExprUpdateTime;
    private final XPathExpression mExprMenuType;
    private final XPathExpression mExprMenuTimestamp;
    private final XPathExpression mExprSubscribeStatus;
    private final XPathExpression mExprQrcode;
    private final XPathExpression mExprCommandId;
    private final XPathExpression mExprPriority;
    private final XPathExpression mExprTitle;
    private final XPathExpression mExprSubmenu;
    private final XPathExpression mExprMediaType;
    private final XPathExpression mExprCreateTime;
    private final XPathExpression mExprMsgUuid;
    private final XPathExpression mExprSmsDigest;
    private final XPathExpression mExprText;
    private final XPathExpression mExprAudio;
    private final XPathExpression mExprVideo;
    private final XPathExpression mExprPic;
//    private final XPathExpression mExprGeoLoc;
//    private final XPathExpression mExprVcard;
    private final XPathExpression mExprMediaArticle;
    private final XPathExpression mExprMediaUuid;
    private final XPathExpression mExprThumbLink;
    private final XPathExpression mExprOriginalLink;
    private final XPathExpression mExprFileSize;
    private final XPathExpression mExprDuration;
    private final XPathExpression mExprFileType;
    private final XPathExpression mExprAuthor;
    private final XPathExpression mExprSourceLink;
    private final XPathExpression mExprBodyLink;
    private final XPathExpression mExprActiveStatus;
    private final XPathExpression mExprAcceptStatus;
//    private final XPathExpression mExprAcceptStatus2;
    private final XPathExpression mExprTelephone;
    private final XPathExpression mExprEmail;
    private final XPathExpression mExprZipcode;
    private final XPathExpression mExprAddress;
    private final XPathExpression mExprField;
    private final XPathExpression mExprMainText;
    private final XPathExpression mExprMsgName;
    private final XPathExpression mExprVersion;
    private final XPathExpression mExprGeneralInfo;
    private final XPathExpression mExprForwardable;

    public XmlMessageParser() {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            mExprUuid = xpath.compile(CommonXmlTags.PA_UUID);
            mExprName = xpath.compile(CommonXmlTags.NAME);
            mExprLogo = xpath.compile(CommonXmlTags.LOGO);
            mExprResult = xpath.compile(CommonXmlTags.RESULT);
            mExprRecommendLevel = xpath.compile(CommonXmlTags.RECOMMENDLEVEL);
            mExprCompany = xpath.compile(CommonXmlTags.COMPANY);
            mExprIntroduction = xpath.compile(CommonXmlTags.INTRO);
            mExprType = xpath.compile(CommonXmlTags.TYPE);
            mExprIdType = xpath.compile(CommonXmlTags.IDTYPE);
            mExprUpdateTime = xpath.compile(CommonXmlTags.UPDATETIME);
            mExprMenuType = xpath.compile(CommonXmlTags.MENUTYPE);
            mExprMenuTimestamp = xpath.compile(CommonXmlTags.MENUTIMESTAMP);
            mExprSubscribeStatus = xpath.compile(CommonXmlTags.SUBSCRIBESTATUS);
            mExprQrcode = xpath.compile(CommonXmlTags.QRCODE);
            mExprCommandId = xpath.compile(CommonXmlTags.COMMANDID);
            mExprPriority = xpath.compile(CommonXmlTags.PRIORITY);
            mExprTitle = xpath.compile(CommonXmlTags.TITLE);
            mExprSubmenu = xpath.compile(makePath(CommonXmlTags.SUBMENU, CommonXmlTags.MENU));
            mExprMediaType = xpath.compile(CommonXmlTags.MEDIA_TYPE);
            mExprCreateTime = xpath.compile(CommonXmlTags.CREATE_TIME);
            mExprMsgUuid = xpath.compile(CommonXmlTags.MSG_UUID);
            mExprSmsDigest = xpath.compile(CommonXmlTags.SMS_DIGEST);
            mExprText = xpath.compile(CommonXmlTags.TEXT);
            mExprAudio = xpath.compile(CommonXmlTags.AUDIO);
            mExprVideo = xpath.compile(CommonXmlTags.VIDEO);
            mExprPic = xpath.compile(CommonXmlTags.PIC);
//            mExprGeoLoc = xpath.compile(CommonXmlTags.GEOLOC);
//            mExprVcard = xpath.compile(CommonXmlTags.VCARD);
            mExprMediaArticle = xpath.compile(
                    makePath(CommonXmlTags.ARTICLE, CommonXmlTags.MEDIAARTICLE));
            mExprThumbLink = xpath.compile(CommonXmlTags.THUMB_LINK);
            mExprOriginalLink = xpath.compile(CommonXmlTags.ORIGINAL_LINK);
            mExprFileSize = xpath.compile(CommonXmlTags.FILESIZE);
            mExprDuration = xpath.compile(CommonXmlTags.DURATION);
            mExprFileType = xpath.compile(CommonXmlTags.FILETYPE);
            mExprAuthor = xpath.compile(CommonXmlTags.AUTHOR);
            mExprSourceLink = xpath.compile(CommonXmlTags.SOURCE_LINK);
            mExprBodyLink = xpath.compile(CommonXmlTags.BODY_LINK);
            mExprMainText = xpath.compile(CommonXmlTags.MAIN_TEXT);
            mExprMediaUuid = xpath.compile(CommonXmlTags.MEDIA_UUID);
            mExprActiveStatus = xpath.compile(CommonXmlTags.ACTIVE_STATUS);
            mExprForwardable = xpath.compile(CommonXmlTags.FORWARDABLE);
            mExprTelephone = xpath.compile(CommonXmlTags.TELEPHONE);
            mExprEmail = xpath.compile(CommonXmlTags.EMAIL);
            mExprZipcode = xpath.compile(CommonXmlTags.ZIPCODE);
            mExprAddress = xpath.compile(CommonXmlTags.ADDRESS);
            mExprField = xpath.compile(CommonXmlTags.FIELD);
            mExprMsgName = xpath.compile(CommonXmlTags.MSGNAME);
            mExprVersion = xpath.compile(CommonXmlTags.VERSION);
            mExprGeneralInfo = xpath.compile(
                    makePath(CommonXmlTags.BODY, CommonXmlTags.GENERALINFO));
            mExprAcceptStatus = xpath.compile(CommonXmlTags.ACCEPTSTATUS);
//            mExprAcceptStatus2 = xpath.compile(CommonXmlTags.ACCEPTSTATUS2);
        } catch (XPathExpressionException e) {
            throw new Error(e);
        }
    }

    /**
     *
     * @param message
     * @return UUID of the subscribed public account
     * @throws PAMException
     */
    public String parseSubscribeMessage(InputStream message) throws PAMException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.SUBSCRIBE.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr;
            Node node;
            expr = xpath.compile(makePath(CommonXmlTags.BODY, CommonXmlTags.PA_UUID));
            node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null || node.getTextContent().trim().length() == 0) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            }
            return node.getTextContent().trim();

        } catch (XPathExpressionException e) {
            throw new Error(e);
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        } catch (SAXException e) {
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
    }

    public String parseUnsubscribeMessage(InputStream message) throws PAMException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.UNSUBSCRIBE.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr;
            Node node;
            expr = xpath.compile(makePath(CommonXmlTags.BODY, CommonXmlTags.PA_UUID));
            node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null || node.getTextContent().trim().length() == 0) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            }
            return node.getTextContent().trim();

        } catch (XPathExpressionException e) {
            throw new Error(e);
        } catch (ParserConfigurationException e) {
            throw new Error(e);
        } catch (SAXException e) {
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
    }

    /**
     * Parser XML response of GetSubscribedList request.
     *
     * @param message
     * @return
     * @throws PAMException
     */
    public AccountsInfo parseGetSubscribedListMessage(InputStream message) throws PAMException {
        return parsePublicAccounts(message, MessageName.GET_SUBSCRIBED_LIST);
    }

    public AccountsInfo parseSearchMessage(InputStream message) throws PAMException {
        return parsePublicAccounts(message, MessageName.SEARCH);
    }

    private AccountsInfo parsePublicAccounts(
            InputStream message, String messageName) throws PAMException {
        AccountsInfo result = new AccountsInfo();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !messageName.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = null;

            expr = xpath.compile(makePath(CommonXmlTags.BODY, CommonXmlTags.PUBLICACCOUNTS));
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node item = nodes.item(i);
                PublicAccount account = new PublicAccount();
                Node node = (Node) mExprUuid.evaluate(item, XPathConstants.NODE);
                account.uuid = (node == null) ? null : node.getTextContent().trim();
                node = (Node) mExprName.evaluate(item, XPathConstants.NODE);
                account.name = (node == null) ? null : node.getTextContent().trim();
                node = (Node) mExprSubscribeStatus.evaluate(item, XPathConstants.NODE);
                account.subscribeStatus = (node == null) ?
                                          Constants.SUBSCRIPTION_STATUS_YES :
                                          Integer.parseInt(node.getTextContent().trim());
                node = (Node) mExprLogo.evaluate(item, XPathConstants.NODE);
                account.logoUrl = (node == null) ? null : node.getTextContent().trim();
                node = (Node) mExprIntroduction.evaluate(item, XPathConstants.NODE);
                account.introduction = (node == null) ? null : node.getTextContent().trim();
                node = (Node) mExprIdType.evaluate(item, XPathConstants.NODE);
                // this is a workaround for invalid data from test server
                // which may provide empty <idtype> in search result
                try {
                    account.idtype = (node == null) ?
                            Constants.INVALID : Integer.parseInt(node.getTextContent().trim());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid idtype string: "
                            + node.getTextContent() + ", treated as COMMERCIAL");
                    account.idtype = Constants.IDTYPE_COMMERCIAL;
                }
                node = (Node) mExprRecommendLevel.evaluate(item, XPathConstants.NODE);
                if (node == null) {
                    account.recommendLevel = Constants.INVALID;
                } else {
                    String recommendString = node.getTextContent().trim();
                    if (TextUtils.isEmpty(recommendString)) {
                        account.recommendLevel = Constants.INVALID;
                    } else {
                        account.recommendLevel = Integer.parseInt(node.getTextContent().trim());
                    }
                }

                result.accounts.add(account);
            }
            result.checkBasicSanity();
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        }
        return result;
    }


    public PublicAccount parseGetDetailsMessage(InputStream message) throws PAMException {
        PublicAccount result = new PublicAccount();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.GET_DETAILS.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(
                    makePath(CommonXmlTags.BODY, CommonXmlTags.PUBLICACCOUNT));

            Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_ALL_MISSING);
            }
            Node n = (Node) mExprUuid.evaluate(node, XPathConstants.NODE);
            result.uuid = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprName.evaluate(node, XPathConstants.NODE);
            result.name = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprLogo.evaluate(node, XPathConstants.NODE);
            result.logoUrl = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprRecommendLevel.evaluate(node, XPathConstants.NODE);
            result.recommendLevel = (n == null) ?
                    Constants.DEFAULT_RECOMMEND_LEVEL :
                    Integer.parseInt(n.getTextContent());
            n = (Node) mExprCompany.evaluate(node, XPathConstants.NODE);
            result.company = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprIntroduction.evaluate(node, XPathConstants.NODE);
            result.introduction = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprType.evaluate(node, XPathConstants.NODE);
            result.type = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprUpdateTime.evaluate(node, XPathConstants.NODE);
            result.updateTime = (n == null) ? Constants.INVALID :
                Utils.convertStringToTimestamp(n.getTextContent().trim());
            n = (Node) mExprMenuType.evaluate(node, XPathConstants.NODE);
            if (n == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.menuType = Integer.parseInt(n.getTextContent().trim());
            }
            n = (Node) mExprMenuTimestamp.evaluate(node, XPathConstants.NODE);
            result.menuTimestamp = (n == null) ? Constants.INVALID :
                        Utils.convertStringToTimestamp(n.getTextContent().trim());
            n = (Node) mExprSubscribeStatus.evaluate(node, XPathConstants.NODE);
            if (n == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.subscribeStatus = Integer.parseInt(n.getTextContent());
            }
            n = (Node) mExprActiveStatus.evaluate(node, XPathConstants.NODE);
            if (n == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.activeStatus = Integer.parseInt(n.getTextContent());
            }
            n = (Node) mExprAcceptStatus.evaluate(node, XPathConstants.NODE);
            if (n == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.acceptStatus = Integer.parseInt(n.getTextContent());
            }
            n = (Node) mExprTelephone.evaluate(node, XPathConstants.NODE);
            result.telephone = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprEmail.evaluate(node, XPathConstants.NODE);
            result.email = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprZipcode.evaluate(node, XPathConstants.NODE);
            result.zipcode = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprAddress.evaluate(node, XPathConstants.NODE);
            result.address = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprField.evaluate(node, XPathConstants.NODE);
            result.field = (n == null) ? null : n.getTextContent().trim();
            n = (Node) mExprQrcode.evaluate(node, XPathConstants.NODE);
            result.qrcode = (n == null) ? null : n.getTextContent().trim();
            result.checkSanity();
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
        return result;
    }

    public MenuInfo parseGetMenuMessage(InputStream message) throws PAMException {
        MenuInfo result = new MenuInfo();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.GET_MENU.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = null;
            expr = xpath.compile(makePath(
                    CommonXmlTags.BODY,
                    CommonXmlTags.MENUTIMESTAMP));
            Node n = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (n == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.timestamp = Utils.convertStringToTimestamp(n.getTextContent().trim());
            }
            expr = xpath.compile(makePath(
                    CommonXmlTags.BODY,
                    CommonXmlTags.PA_UUID));
            n = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (n == null) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.uuid = n.getTextContent().trim();
            }

            expr = xpath.compile(makePath(
                    CommonXmlTags.BODY,
                    CommonXmlTags.MENULIST,
                    CommonXmlTags.MENU));
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                MenuEntry item = new MenuEntry();
                parseMenuItem(node, item, 0);
                result.menu.add(item);
            }
            result.sortSubMenus();
            result.checkSanity();
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
        return result;
    }

    private void parseMenuItem(Node node, MenuEntry item, int depth)
            throws PAMException, XPathExpressionException {
        Node n = (Node) mExprCommandId.evaluate(node, XPathConstants.NODE);
        if (n == null) {
            item.commandId = null;
        } else {
            String idString = n.getTextContent().trim();
            item.commandId = idString.length() == 0 ? null : idString;
        }
        n = (Node) mExprTitle.evaluate(node, XPathConstants.NODE);
        item.title = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprType.evaluate(node, XPathConstants.NODE);
        if (n == null) {
            item.type = Constants.MENU_TYPE_INVALID;
        } else {
            String typeString = n.getTextContent().trim();
            if (typeString.length() == 0) {
                item.type = Constants.MENU_TYPE_INVALID;
            } else {
                item.type = Integer.parseInt(typeString);
            }
        }
        n = (Node) mExprPriority.evaluate(node, XPathConstants.NODE);
        if (n == null) {
            throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
        } else {
            item.priority = Integer.parseInt(n.getTextContent());
        }
        if (depth < Constants.MAX_MENU_DEPTH) {
            NodeList nl = (NodeList) mExprSubmenu.evaluate(node, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); ++i) {
                MenuEntry newItem = new MenuEntry();
                parseMenuItem(nl.item(i), newItem, depth + 1);
                item.subMenuItems.add(newItem);
            }
            item.sortSubMenus();
        }
    }

    public MessageHistoryInfo parseGetMessageHistoryMessage(InputStream message)
            throws PAMException {
        MessageHistoryInfo result = new MessageHistoryInfo();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.GET_MESSAGE_HISTORY.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = null;
            expr = xpath.compile(makePath(
                    CommonXmlTags.BODY,
                    CommonXmlTags.PA_UUID));
            // FIXME CMCC Workaround
//            Node n = (Node) expr.evaluate(doc, XPathConstants.NODE);
            // Ignore pa_uuid for CMCC server compatibility.
//            if (n == null) {
//                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
//            } else {
//                result.uuid = n.getTextContent().trim();
//            }

            expr = xpath.compile(makePath(
                    CommonXmlTags.BODY,
                    CommonXmlTags.MSGLIST,
                    CommonXmlTags.MSG_CONTENT));
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                MessageContent mc = new MessageContent();
                parseMessageContent(node, mc);
                mc.checkSanity();
                result.messages.add(mc);
            }
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
        return result;
    }

    private static String nodeToString(Node node) {
        if (node == null) {
            return null;
        }

        Transformer transformer;
        StringWriter sw = new StringWriter();
        DOMSource source = new DOMSource(node);
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            if (transformer != null) {
                Result result = new StreamResult(sw);
                transformer.transform(source, result);
                return sw.getBuffer().toString();
            }
        } catch (TransformerFactoryConfigurationError | TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void parseMessageContent(Node node, MessageContent message)
            throws PAMException, XPathExpressionException {
        message.body = nodeToString(node);
        Node n = (Node) mExprCreateTime.evaluate(node, XPathConstants.NODE);
        message.createTime = (n == null) ? Constants.INVALID :
            Utils.convertStringToTimestamp(n.getTextContent().trim());
        // FIXME CMCC Workaround
        if (message.createTime == Constants.INVALID) {
            message.createTime = 0;
        }
        n = (Node) mExprSmsDigest.evaluate(node, XPathConstants.NODE);
        message.smsDigest = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprMsgUuid.evaluate(node, XPathConstants.NODE);
        message.messageUuid = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprUuid.evaluate(node, XPathConstants.NODE);
        message.publicAccountUuid = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprForwardable.evaluate(node, XPathConstants.NODE);
        message.forwardable = (n == null) ? Constants.INVALID :
            Integer.parseInt(n.getTextContent().trim());
        n = (Node) mExprActiveStatus.evaluate(node, XPathConstants.NODE);
        message.activeStatus = (n == null) ? Constants.INVALID :
            Integer.parseInt(n.getTextContent().trim());

        n = (Node) mExprMediaType.evaluate(node, XPathConstants.NODE);
        if (n == null) {
            throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
        } else {
            message.mediaType = Integer.parseInt(n.getTextContent());
            switch (message.mediaType) {
            case Constants.MEDIA_TYPE_TEXT:
                n = (Node) mExprText.evaluate(node, XPathConstants.NODE);
                message.text = (n == null) ? null : n.getTextContent().trim();
                break;
            case Constants.MEDIA_TYPE_PICTURE:
                n = (Node) mExprPic.evaluate(node, XPathConstants.NODE);
                message.basicMedia = new MediaBasic();
                parseMediaBasic(n, message.basicMedia);
                break;
            case Constants.MEDIA_TYPE_VIDEO:
                n = (Node) mExprVideo.evaluate(node, XPathConstants.NODE);
                message.basicMedia = new MediaBasic();
                parseMediaBasic(n, message.basicMedia);
                break;
            case Constants.MEDIA_TYPE_AUDIO:
                n = (Node) mExprAudio.evaluate(node, XPathConstants.NODE);
                message.basicMedia = new MediaBasic();
                parseMediaBasic(n, message.basicMedia);
                break;
            case Constants.MEDIA_TYPE_GEOLOC:
                n = (Node) mExprText.evaluate(node, XPathConstants.NODE);
                message.text = (n == null) ? null : n.getTextContent().trim();
                break;
            case Constants.MEDIA_TYPE_VCARD:
                n = (Node) mExprText.evaluate(node, XPathConstants.NODE);
                message.text = (n == null) ? null : n.getTextContent().trim();
                break;
            case Constants.MEDIA_TYPE_SINGLE_ARTICLE:
            case Constants.MEDIA_TYPE_MULTIPLE_ARTICLE:
                NodeList nl = (NodeList) mExprMediaArticle.evaluate(node, XPathConstants.NODESET);
                for (int i = 0; i < nl.getLength(); ++i) {
                    MediaArticle article = new MediaArticle();
                    parseMediaArticle(nl.item(i), article);
                    message.article.add(article);
                }
                break;
            case Constants.MEDIA_TYPE_SMS:
                // message is stored in smsDigest
                break;
            default:
                throw new PAMException(ResultCode.PARAM_ERROR_INVALID_FORMAT);
            }
        }
    }

    private void parseMediaBasic(
            Node node, MediaBasic mb) throws PAMException, XPathExpressionException {
        Node n = (Node) mExprTitle.evaluate(node, XPathConstants.NODE);
        mb.title = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprThumbLink.evaluate(node, XPathConstants.NODE);
        mb.thumbnailUrl = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprOriginalLink.evaluate(node, XPathConstants.NODE);
        mb.originalUrl = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprFileSize.evaluate(node, XPathConstants.NODE);
        // convert back and forth to remove B/KB/MB etc
        mb.fileSize = (n == null) ? null :
            Integer.toString(Utils.extractSize(n.getTextContent().trim()));
        n = (Node) mExprDuration.evaluate(node, XPathConstants.NODE);
        mb.duration = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprFileType.evaluate(node, XPathConstants.NODE);
        mb.fileType = (n == null) ? null :
            n.getTextContent().trim();
        n = (Node) mExprUuid.evaluate(node, XPathConstants.NODE);
        mb.publicAccountUuid = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprCreateTime.evaluate(node, XPathConstants.NODE);
        mb.createTime = (n == null) ? Constants.INVALID :
            Utils.convertStringToTimestamp(n.getTextContent().trim());
        n = (Node) mExprMediaUuid.evaluate(node, XPathConstants.NODE);
        mb.mediaUuid = (n == null) ? null : n.getTextContent().trim();
    }

    private void parseMediaArticle(
            Node node, MediaArticle ma) throws PAMException, XPathExpressionException {
        Node n = (Node) mExprTitle.evaluate(node, XPathConstants.NODE);
        ma.title = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprAuthor.evaluate(node, XPathConstants.NODE);
        ma.author = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprThumbLink.evaluate(node, XPathConstants.NODE);
        ma.thumbnailUrl = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprOriginalLink.evaluate(node, XPathConstants.NODE);
        ma.originalUrl = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprSourceLink.evaluate(node, XPathConstants.NODE);
        ma.sourceUrl = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprBodyLink.evaluate(node, XPathConstants.NODE);
        ma.bodyUrl = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprMainText.evaluate(node, XPathConstants.NODE);
        ma.mainText = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprMediaUuid.evaluate(node, XPathConstants.NODE);
        ma.mediaUuid = (n == null) ? null : n.getTextContent().trim();
    }

    public AccountsInfo parseGetRecommendsMessage(InputStream message) throws PAMException {
        return parsePublicAccounts(message, MessageName.GET_RECOMMENDS);
    }

    public ComplainInfo parseComplainMessage(InputStream message) throws PAMException {
        ComplainInfo result = new ComplainInfo();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.COMPLAIN.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr;
            Node node;
            expr = xpath.compile(makePath(CommonXmlTags.BODY, CommonXmlTags.PA_UUID));
            node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null || node.getTextContent().trim().length() == 0) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.uuid = node.getTextContent().trim();
            }
            result.result = info.result;
            result.checkSanity();
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
        return result;
    }

    public SetAcceptStatusInfo parseSetAcceptStatusMessage(
            InputStream message) throws PAMException {
        SetAcceptStatusInfo result = new SetAcceptStatusInfo();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(message);
            GeneralInfo info = parseGeneralInfo(doc);
            Utils.throwIf(ResultCode.PARAM_ERROR_INVALID_FORMAT,
                    !MessageName.SET_ACCEPT_STATUS.equals(info.messageName));
            Utils.throwIf(info.result, info.result != ResultCode.SUCCESS);

            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr;
            Node node;
            expr = xpath.compile(makePath(CommonXmlTags.BODY, CommonXmlTags.PA_UUID));
            node = (Node) expr.evaluate(doc, XPathConstants.NODE);
            if (node == null || node.getTextContent().trim().length() == 0) {
                throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
            } else {
                result.uuid = node.getTextContent().trim();
            }
            result.result = info.result;
            result.checkSanity();
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            e.printStackTrace();
            throw new PAMException(ResultCode.PARAM_ERROR_MESSAGE_PARSING_ERROR);
        } catch (IOException e) {
            throw new PAMException(ResultCode.SYSTEM_ERROR_NETWORK);
        }
        return result;
    }

    private GeneralInfo parseGeneralInfo(Node doc) throws PAMException, XPathExpressionException {
        Node infoNode = (Node)mExprGeneralInfo.evaluate(doc, XPathConstants.NODE);
        if (infoNode == null) {
            throw new PAMException(ResultCode.PARAM_ERROR_MANDATORY_MISSING);
        }

        GeneralInfo result = new GeneralInfo();
        Node n = (Node) mExprMsgName.evaluate(infoNode, XPathConstants.NODE);
        result.messageName = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprVersion.evaluate(infoNode, XPathConstants.NODE);
        result.version = (n == null) ? null : n.getTextContent().trim();
        n = (Node) mExprResult.evaluate(infoNode, XPathConstants.NODE);
        result.result = (n == null) ? Constants.INVALID :
            Integer.parseInt(n.getTextContent().trim());
        return result;
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
