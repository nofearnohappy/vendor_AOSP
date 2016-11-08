package com.mediatek.simservs.client;

import android.util.Log;

import com.mediatek.simservs.client.policy.Rule;
import com.mediatek.simservs.client.policy.RuleSet;
import com.mediatek.simservs.xcap.RuleType;
import com.mediatek.simservs.xcap.XcapException;
import com.mediatek.xcap.client.uri.XcapUri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Incoming Communication Barring class.
 *
 */
public class IncomingCommunicationBarring extends SimservType implements RuleType {

    public static final String NODE_NAME = "incoming-communication-barring";

    RuleSet mRuleSet;

    /**
     * Constructor.
     *
     * @param documentUri       XCAP document URI
     * @param parentUri         XCAP root directory URI
     * @param intendedId        X-3GPP-Intended-Id
     * @throws XcapException    if XCAP error
     * @throws ParserConfigurationException if parser configuration error
     */
    public IncomingCommunicationBarring(XcapUri documentUri, String parentUri, String intendedId)
            throws XcapException, ParserConfigurationException {
        super(documentUri, parentUri, intendedId);
    }

    @Override
    public void initServiceInstance(Document domDoc) {
        NodeList ruleSetNode = domDoc.getElementsByTagName("ruleset");
        if (ruleSetNode.getLength() > 0) {
            Log.d("IncomingCommunicationBarring", "Got ruleset");
            Element nruleSetElement = (Element) ruleSetNode.item(0);
            mRuleSet = new RuleSet(mXcapUri, NODE_NAME, mIntendedId, nruleSetElement);
            if (mNetwork != null) {
                mRuleSet.setNetwork(mNetwork);
            }

            if (mContext != null) {
                mRuleSet.setContext(mContext);
            }

            if (mEtag != null) {
                mRuleSet.setEtag(mEtag);
            }
        } else {
            ruleSetNode = domDoc.getElementsByTagNameNS(COMMON_POLICY_NAMESPACE, "ruleset");
            if (ruleSetNode.getLength() > 0) {
                Log.d("IncomingCommunicationBarring", "Got ruleset");
                Element nruleSetElement = (Element) ruleSetNode.item(0);
                mRuleSet = new RuleSet(mXcapUri, NODE_NAME, mIntendedId,
                        nruleSetElement);
                if (mNetwork != null) {
                    mRuleSet.setNetwork(mNetwork);
                }

                if (mContext != null) {
                    mRuleSet.setContext(mContext);
                }

                if (mEtag != null) {
                    mRuleSet.setEtag(mEtag);
                }
            } else {
                ruleSetNode = domDoc.getElementsByTagName("cp:ruleset");
                if (ruleSetNode.getLength() > 0) {
                    Log.d("IncomingCommunicationBarring", "Got cp:ruleset");
                    Element nruleSetElement = (Element) ruleSetNode.item(0);
                    mRuleSet = new RuleSet(mXcapUri, NODE_NAME, mIntendedId,
                            nruleSetElement);
                    if (mNetwork != null) {
                        mRuleSet.setNetwork(mNetwork);
                    }

                    if (mContext != null) {
                        mRuleSet.setContext(mContext);
                    }

                    if (mEtag != null) {
                        mRuleSet.setEtag(mEtag);
                    }
                } else {
                    mRuleSet = new RuleSet(mXcapUri, NODE_NAME, mIntendedId);
                    if (mNetwork != null) {
                        mRuleSet.setNetwork(mNetwork);
                    }

                    if (mContext != null) {
                        mRuleSet.setContext(mContext);
                    }

                    if (mEtag != null) {
                        mRuleSet.setEtag(mEtag);
                    }
                }
            }
        }
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * Get ruleset.
     *
     * @return ruleset
     */
    @Override
    public RuleSet getRuleSet() {
        return mRuleSet;
    }

    /**
     * Save ruleset configuration to server.
     *
     * @throws XcapException    if XCAP error
     */
    @Override
    public void saveRuleSet() throws XcapException {
        String ruleXml = mRuleSet.toXmlString();
        mRuleSet.setContent(ruleXml);
    }

    /**
     * Create ruleset.
     *
     * @return new ruleset
     */
    @Override
    public RuleSet createNewRuleSet() {
        mRuleSet = new RuleSet(mXcapUri, NODE_NAME, mIntendedId);
        if (mNetwork != null) {
            mRuleSet.setNetwork(mNetwork);
        }
        if (mEtag != null) {
            mRuleSet.setEtag(mEtag);
        }
        return mRuleSet;
    }

    /**
     * Save rule to server.
     *
     * @param ruleId rule to be saved by the id
     * @throws  XcapException if XCAP error
     */
    @Override
    public void saveRule(String ruleId) throws XcapException {
        if (ruleId != null && !ruleId.isEmpty()) {
            LinkedList<Rule> rules =  (LinkedList<Rule>) mRuleSet.getRules();
            for (Rule rule : rules) {
                if (ruleId.equals(rule.mId)) {
                    String ruleXml = rule.toXmlString();
                    rule.setContent(ruleXml);
                }
            }
        } else {
            Log.d("saveRule", "ruleId is null");
        }
    }
}
