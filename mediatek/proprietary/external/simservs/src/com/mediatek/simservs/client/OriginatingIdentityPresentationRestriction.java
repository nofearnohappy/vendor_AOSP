
package com.mediatek.simservs.client;

import com.mediatek.simservs.xcap.XcapException;
import com.mediatek.xcap.client.uri.XcapUri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 * Originating Identity Presentation Restriction class.
 */
public class OriginatingIdentityPresentationRestriction extends SimservType {

    public static final String NODE_NAME = "originating-identity-presentation-restriction";
    public DefaultBehaviour mDefaultBehaviour;
    public boolean mContainDefaultBehaviour = false;

    /**
     * Constructor.
     *
     * @param documentUri       XCAP document URI
     * @param parentUri         XCAP root directory URI
     * @param xui               X-3GPP-Intended-Id
     * @throws  Exception       if error
     */
    public OriginatingIdentityPresentationRestriction(XcapUri documentUri, String parentUri,
            String xui) throws Exception {
        super(documentUri, parentUri, xui);
    }

    @Override
    public void initServiceInstance(Document domDoc) {
        NodeList defaultBehaviour = domDoc.getElementsByTagName(DefaultBehaviour.NODE_NAME);
        if (defaultBehaviour.getLength() > 0) {
            mContainDefaultBehaviour = true;
            Element defaultBehaviourElement = (Element) defaultBehaviour.item(0);
            mDefaultBehaviour = new DefaultBehaviour(mXcapUri, NODE_NAME, mIntendedId,
                    defaultBehaviourElement);

            if (mNetwork != null) {
                mDefaultBehaviour.setNetwork(mNetwork);
            }

            if (mContext != null) {
                mDefaultBehaviour.setContext(mContext);
            }
        } else {
            defaultBehaviour = domDoc.getElementsByTagNameNS(XCAP_NAMESPACE,
                    DefaultBehaviour.NODE_NAME);
            if (defaultBehaviour.getLength() > 0) {
                mContainDefaultBehaviour = true;
                Element defaultBehaviourElement = (Element) defaultBehaviour.item(0);
                mDefaultBehaviour = new DefaultBehaviour(mXcapUri, NODE_NAME, mIntendedId,
                        defaultBehaviourElement);

                if (mNetwork != null) {
                    mDefaultBehaviour.setNetwork(mNetwork);
                }

                if (mContext != null) {
                    mDefaultBehaviour.setContext(mContext);
                }
            } else {
                mDefaultBehaviour = new DefaultBehaviour(mXcapUri, NODE_NAME, mIntendedId);

                if (mNetwork != null) {
                    mDefaultBehaviour.setNetwork(mNetwork);
                }

                if (mContext != null) {
                    mDefaultBehaviour.setContext(mContext);
                }
            }
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * Save configuration to server.
     *
     * @throws XcapException    if XCAP error
     */
    public void saveConfiguration() throws XcapException {
        String serviceXml = toXmlString();
        setContent(serviceXml);
        mContainDefaultBehaviour = true;
    }

    /**
     * Convert DOM to XML string.
     *
     * @return XML string
     */
    public String toXmlString() {
        Element root = null;
        String xmlString = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            root = (Element) document.createElement(NODE_NAME);
            document.appendChild(root);
            Element defaultElement = mDefaultBehaviour.toXmlElement(document);
            root.appendChild(defaultElement);
            xmlString = domToXmlText(root);
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            pce.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return xmlString;
    }

    public boolean isContainDefaultBehaviour() {
        return mContainDefaultBehaviour;
    }

    public boolean isDefaultPresentationRestricted() {
        return mDefaultBehaviour.isPresentationRestricted();
    }

    /**
     * set Default Presentation Restricted value.
     *
     * @param presentationRestricted default restricted value
     * @throws XcapException if XCAP error
     */
    public void setDefaultPresentationRestricted(boolean presentationRestricted) throws
            XcapException {
        mDefaultBehaviour.setPresentationRestricted(presentationRestricted);

        if (isDefaultPresentationRestricted()) {
            String defaultBehaviourXml = mDefaultBehaviour.toXmlString();
            mDefaultBehaviour.setContent(defaultBehaviourXml);
        } else {
            saveConfiguration();
        }
    }
}
