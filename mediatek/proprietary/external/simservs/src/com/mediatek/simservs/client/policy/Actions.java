package com.mediatek.simservs.client.policy;

import com.mediatek.simservs.xcap.ConfigureType;
import com.mediatek.simservs.xcap.XcapElement;
import com.mediatek.xcap.client.uri.XcapUri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Actions class.
 *
 */
public class Actions extends XcapElement implements ConfigureType {

    public static final String NODE_NAME = "cp:actions";

    static final String TAG_ALLOW = "allow";
    static final String TAG_FORWARD_TO = "forward-to";

    public boolean mAllow;
    public ForwardTo mForwardTo;

    /**
     * Constructor without XML element.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     */
    public Actions(XcapUri xcapUri, String parentUri, String intendedId) {
        super(xcapUri, parentUri, intendedId);
    }

    /**
     * Constructor with XML element.
     *
     * @param xcapUri       XCAP document URI
     * @param parentUri     XCAP root directory URI
     * @param intendedId    X-3GPP-Intended-Id
     * @param domElement    DOM XML element
     */
    public Actions(XcapUri xcapUri, String parentUri, String intendedId,
            Element domElement) {
        super(xcapUri, parentUri, intendedId);
        instantiateFromXmlNode(domElement);
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    @Override
    public void instantiateFromXmlNode(Node domNode) {
        Element domElement = (Element) domNode;
        NodeList actionNode = domElement.getElementsByTagName(TAG_ALLOW);
        if (actionNode.getLength() > 0) {
            Element allowElement = (Element) actionNode.item(0);
            String allowed = allowElement.getTextContent();
            mAllow = allowed.equals("true");
        } else {
            actionNode = domElement.getElementsByTagNameNS(XCAP_NAMESPACE, TAG_ALLOW);
            if (actionNode.getLength() > 0) {
                Element allowElement = (Element) actionNode.item(0);
                String allowed = allowElement.getTextContent();
                mAllow = allowed.equals("true");
            } else {
                actionNode = domElement.getElementsByTagName(XCAP_ALIAS + ":" + TAG_ALLOW);
                if (actionNode.getLength() > 0) {
                    Element allowElement = (Element) actionNode.item(0);
                    String allowed = allowElement.getTextContent();
                    mAllow = allowed.equals("true");
                }
            }
        }

        actionNode = domElement.getElementsByTagName(TAG_FORWARD_TO);
        if (actionNode.getLength() > 0) {
            mForwardTo = new ForwardTo(mXcapUri, NODE_NAME, mIntendedId,
                    domElement);
        } else {
            actionNode = domElement.getElementsByTagNameNS(XCAP_NAMESPACE, TAG_FORWARD_TO);
            if (actionNode.getLength() > 0) {
                mForwardTo = new ForwardTo(mXcapUri, NODE_NAME, mIntendedId,
                        domElement);
            } else {
                actionNode = domElement.getElementsByTagName(XCAP_ALIAS + ":" + TAG_FORWARD_TO);
                if (actionNode.getLength() > 0) {
                    mForwardTo = new ForwardTo(mXcapUri, NODE_NAME, mIntendedId,
                            domElement);
                } else {
                    mForwardTo = new ForwardTo(mXcapUri, NODE_NAME, mIntendedId);
                }
            }
        }
    }

    /**
     * Convert to XML element.
     *
     * @param document  dom document
     * @return XML format element
     */
    public Element toXmlElement(Document document) {
        Element actionsElement = (Element) document.createElement(NODE_NAME);

        if (mForwardTo != null) {
            Element forwardToElement = mForwardTo.toXmlElement(document);
            actionsElement.appendChild(forwardToElement);
        } else {
            String useXcapNs = System.getProperty("xcap.ns.ss", "false");

            if ("true".equals(useXcapNs)) {
                Element allowElement = (Element) document.createElementNS(XCAP_NAMESPACE,
                        "ss:" + TAG_ALLOW);
                allowElement.setTextContent(mAllow ? "true" : "false");
                actionsElement.appendChild(allowElement);
            } else {
                Element allowElement = (Element) document.createElement(TAG_ALLOW);
                allowElement.setTextContent(mAllow ? "true" : "false");
                actionsElement.appendChild(allowElement);
            }
        }
        return actionsElement;
    }

    public void setAllow(boolean allow) {
        mAllow = allow;
    }

    public boolean isAllow() {
        return mAllow;
    }

    /**
     * Set ForwardTo value.
     *
     * @param target        forward number
     * @param notifyCaller  whether to notify caller
     */
    public void setFowardTo(String target, boolean notifyCaller) {
        if (mForwardTo == null) {
            mForwardTo = new ForwardTo(mXcapUri, mParentUri, mIntendedId);
        }
        mForwardTo.setTarget(target);
        mForwardTo.setNotifyCaller(notifyCaller);
    }

    public ForwardTo getFowardTo() {
        return mForwardTo;
    }
}
