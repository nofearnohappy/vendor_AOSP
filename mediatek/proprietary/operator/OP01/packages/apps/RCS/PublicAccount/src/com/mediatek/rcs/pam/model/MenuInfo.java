package com.mediatek.rcs.pam.model;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.mediatek.rcs.pam.Constants;
import com.mediatek.rcs.pam.PAMException;
import com.mediatek.rcs.pam.provider.PAContract.AccountColumns;
import com.mediatek.rcs.pam.util.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
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

public class MenuInfo implements SanityCheck {
    public final List<MenuEntry> menu;
    public long timestamp;
    public String uuid;

    // Android Specific
    public long accountId = Constants.INVALID;

    private static XPathExpression sExprCommandId;
    private static XPathExpression sExprType;
    private static XPathExpression sExprTitle;
    private static XPathExpression sExprPriority;
    private static XPathExpression sExprSubmenu;
    static {
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            sExprCommandId = xpath.compile(CommonXmlTags.COMMANDID);
            sExprType = xpath.compile(CommonXmlTags.TYPE);
            sExprTitle = xpath.compile(CommonXmlTags.TITLE);
            sExprPriority = xpath.compile(CommonXmlTags.PRIORITY);
            sExprSubmenu = xpath.compile(makePath(CommonXmlTags.SUBMENU, CommonXmlTags.MENU));
        } catch (XPathExpressionException e) {
            throw new Error(e);
        }
    }

    public MenuInfo() {
        menu = new LinkedList<MenuEntry>();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{_class:\"MenuInfo\", timestamp:\"").append(timestamp)
            .append("\", uuid:\"").append(uuid).append("\", menu:[");
        for (int i = 0; i < menu.size(); ++i) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(menu.get(i).toString());
        }
        sb.append("]}");
        return sb.toString();
    }

    @Override
    public void checkSanity() throws PAMException {
        Utils.throwIf(ResultCode.PARAM_ERROR_MANDATORY_MISSING,
                timestamp == Constants.INVALID || TextUtils.isEmpty(uuid));
        checkBasicSanity();
    }

    public void checkBasicSanity() throws PAMException {
        for (MenuEntry item : menu) {
            item.checkSanity();
        }
    }

    public void sortSubMenus() {
        Collections.sort(menu, MenuEntry.COMPARATOR);
    }

    public void parseMenuInfoString(String message) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(message.getBytes("UTF-8")));
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = null;

            expr = xpath.compile(makePath(CommonXmlTags.MENULIST, CommonXmlTags.MENU));
            NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                MenuEntry item = new MenuEntry();
                parseMenuItem(node, item, 0);
                menu.add(item);
            }
            sortSubMenus();
            checkBasicSanity();
        } catch (XPathExpressionException e) {
            new Error(e);
        } catch (ParserConfigurationException e) {
            new Error(e);
        } catch (SAXException e) {
            throw new Error(e);
        } catch (IOException e) {
            new Error(e);
        } catch (PAMException e) {
            throw new Error(e);
        }
    }

    private static void parseMenuItem(Node node, MenuEntry item, int depth) throws PAMException,
            XPathExpressionException {
        Node n = (Node) sExprCommandId.evaluate(node, XPathConstants.NODE);
        if (n == null) {
            item.commandId = null;
        } else {
            String idString = n.getTextContent().trim();
            item.commandId = idString.length() == 0 ? null : idString;
        }
        n = (Node) sExprTitle.evaluate(node, XPathConstants.NODE);
        item.title = (n == null) ? null : n.getTextContent().trim();
        n = (Node) sExprType.evaluate(node, XPathConstants.NODE);
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
        n = (Node) sExprPriority.evaluate(node, XPathConstants.NODE);
        if (n == null) {
            item.priority = Constants.INVALID;
        } else {
            item.priority = Integer.parseInt(n.getTextContent());
        }
        if (depth < Constants.MAX_MENU_DEPTH) {
            NodeList nl = (NodeList) sExprSubmenu.evaluate(node, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); ++i) {
                MenuEntry newItem = new MenuEntry();
                parseMenuItem(nl.item(i), newItem, depth + 1);
                item.subMenuItems.add(newItem);
            }
            item.sortSubMenus();
        }
    }

    public String buildMenuInfoString() {
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            StringWriter writer = new StringWriter();
            serializer.setOutput(writer);
            serializer.startDocument("utf-8", null);
            serializer.startTag(null, CommonXmlTags.MENULIST);

            for (MenuEntry item : menu) {
                item.buildMenuItemString(serializer);
            }

            serializer.endTag(null, CommonXmlTags.MENULIST);
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

    private static String makePath(String... segments) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length - 1; ++i) {
            sb.append(segments[i]).append('/');
        }
        sb.append(segments[segments.length - 1]);
        return sb.toString();
    }

    public boolean loadFromContentProvider(ContentResolver cr, long acctId) {
        Cursor c = null;
        try {
            c = cr.query(
                    AccountColumns.CONTENT_URI,
                    new String[] { AccountColumns.MENU, AccountColumns.ID, },
                    AccountColumns.ID + "=?",
                    new String[] { Long.toString(acctId) },
                    null);
            if (c == null || c.getCount() == 0) {
                return false;
            } else {
                c.moveToFirst();
                accountId = c.getLong(c.getColumnIndexOrThrow(AccountColumns.ID));
                final String menuString = c.getString(c.getColumnIndexOrThrow(AccountColumns.MENU));
                if (menuString == null) {
                    return false;
                }
                parseMenuInfoString(menuString);
                return true;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    public void storeToContentProvider(ContentResolver cr) {
        ContentValues values = new ContentValues();
        values.put(AccountColumns.MENU, buildMenuInfoString());
        cr.update(AccountColumns.CONTENT_URI, values, AccountColumns.ID + "=?",
                new String[] { Long.toString(accountId) });
    }

    public static ContentValues storeToContentValues(MenuInfo menuInfo) {
        ContentValues values = new ContentValues();
        values.put(AccountColumns.MENU, menuInfo.buildMenuInfoString());
        return values;
    }
}
