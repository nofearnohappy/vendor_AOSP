
package com.mediatek.simservs.client;

import android.util.Log;

import com.mediatek.simservs.client.policy.Rule;
import com.mediatek.simservs.client.policy.RuleSet;
import com.mediatek.simservs.xcap.RuleType;
import com.mediatek.simservs.xcap.XcapElement;
import com.mediatek.simservs.xcap.XcapException;
import com.mediatek.xcap.client.uri.XcapUri;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.LinkedList;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Communication Diversion class.
 *
 */
public class CommunicationDiversion extends SimservType implements RuleType {

    public static final String NODE_NAME = "communication-diversion";

    NoReplyTimer mNoReplyTimer;
    RuleSet mRuleSet;

    /**
     * Constructor.
     *
     * @param documentUri XCAP document URI
     * @param parentUri   XCAP root directory URI
     * @param intendedId  X-3GPP-Intended-Id
     * @throws XcapException if XCAP error
     * @throws ParserConfigurationException if parser configuration error
     */
    public CommunicationDiversion(XcapUri documentUri, String parentUri, String intendedId)
            throws XcapException, ParserConfigurationException {
        super(documentUri, parentUri, intendedId);
    }

    @Override
    protected String getNodeName() {
        return NODE_NAME;
    }

    /**
     * Instantiate from DOM XML.
     *
     * @param   domDoc  DOM document
     */
    @Override
    public void initServiceInstance(Document domDoc) {
        NodeList noReplyTimerNode = domDoc.getElementsByTagName("NoReplyTimer");
        if (noReplyTimerNode.getLength() > 0) {
            Log.d("CommunicationDiversion", "Got NoReplyTimer");
            Element noReplyTimerElement = (Element) noReplyTimerNode.item(0);
            String val = noReplyTimerElement.getTextContent();
            int noReplyTimer = Integer.parseInt(val);
            mNoReplyTimer = new NoReplyTimer(mXcapUri, NODE_NAME, mIntendedId,
                    noReplyTimer);
            if (mNetwork != null) {
                mNoReplyTimer.setNetwork(mNetwork);
            }

            if (mContext != null) {
                mNoReplyTimer.setContext(mContext);
            }

            if (mEtag != null) {
                mNoReplyTimer.setEtag(mEtag);
            }
        } else {
            noReplyTimerNode = domDoc.getElementsByTagNameNS(XCAP_NAMESPACE,
                    "NoReplyTimer");
            if (noReplyTimerNode.getLength() > 0) {
                Log.d("CommunicationDiversion", "Got NoReplyTimer");
                Element noReplyTimerElement = (Element) noReplyTimerNode.item(0);
                String val = noReplyTimerElement.getTextContent();
                int noReplyTimer = Integer.parseInt(val);
                mNoReplyTimer = new NoReplyTimer(mXcapUri, NODE_NAME, mIntendedId,
                        noReplyTimer);
                if (mNetwork != null) {
                    mNoReplyTimer.setNetwork(mNetwork);
                }

                if (mContext != null) {
                    mNoReplyTimer.setContext(mContext);
                }

                if (mEtag != null) {
                    mNoReplyTimer.setEtag(mEtag);
                }
            } else {
                noReplyTimerNode = domDoc.getElementsByTagName(XCAP_ALIAS + ":" + "NoReplyTimer");
                if (noReplyTimerNode.getLength() > 0) {
                    Log.d("CommunicationDiversion", "Got NoReplyTimer");
                    Element noReplyTimerElement = (Element) noReplyTimerNode.item(0);
                    String val = noReplyTimerElement.getTextContent();
                    int noReplyTimer = Integer.parseInt(val);
                    mNoReplyTimer = new NoReplyTimer(mXcapUri, NODE_NAME, mIntendedId,
                            noReplyTimer);
                    if (mNetwork != null) {
                        mNoReplyTimer.setNetwork(mNetwork);
                    }

                    if (mContext != null) {
                        mNoReplyTimer.setContext(mContext);
                    }

                    if (mEtag != null) {
                        mNoReplyTimer.setEtag(mEtag);
                    }
                } else {
                    mNoReplyTimer = new NoReplyTimer(mXcapUri, NODE_NAME, mIntendedId,
                            -1);
                    if (mNetwork != null) {
                        mNoReplyTimer.setNetwork(mNetwork);
                    }

                    if (mContext != null) {
                        mNoReplyTimer.setContext(mContext);
                    }

                    if (mEtag != null) {
                        mNoReplyTimer.setEtag(mEtag);
                    }
                }
            }
        }

        NodeList ruleSetNode = domDoc.getElementsByTagName("ruleset");
        if (ruleSetNode.getLength() > 0) {
            Log.d("CommunicationDiversion", "Got ruleset");
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
                Log.d("CommunicationDiversion", "Got ruleset");
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
                    Log.d("CommunicationDiversion", "Got ruleset");
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

    public int getNoReplyTimer() {
        return mNoReplyTimer.getValue();
    }

    /**
     * Set noreply timer.
     *
     * @param   timerValue  time value in second
     * @throws  XcapException if XCAP error
     */
    public void setNoReplyTimer(int timerValue) throws XcapException {
        mNoReplyTimer.setValue(timerValue);
        String noReplyTimerXml = mNoReplyTimer.toXmlString();
        mNoReplyTimer.setContent(noReplyTimerXml);
        if (mNoReplyTimer.getEtag() != null) {
            this.mEtag = mNoReplyTimer.getEtag();
        }
    }

    /**
    * Get rule set.
    *
    * @return Ruleset
    */
    @Override
    public RuleSet getRuleSet() {
        return mRuleSet;
    }

    /**
     * Save ruleset to server.
     *
     * @throws  XcapException if XCAP error
     */
    @Override
    public void saveRuleSet() throws XcapException {
        String ruleXml = mRuleSet.toXmlString();
        mRuleSet.setContent(ruleXml);
        if (mRuleSet.getEtag() != null) {
            this.mEtag = mRuleSet.getEtag();
        }
    }

    /**
     * Create ruleset.
     *
     * @return  ruleset
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
                    if (rule.getEtag() != null) {
                        this.mEtag = rule.getEtag();
                    }
                    break;
                }
            }
        } else {
            Log.d("saveRule", "ruleId is null");
        }
    }

    /**
     * NoReplyTimer class.
     *
     */
    public class NoReplyTimer extends XcapElement {

        public static final String NODE_NAME = "NoReplyTimer";
        public int mValue;

        /**
         * Constructor without initial time value.
         *
         * @param cdUri       XCAP document URI
         * @param parentUri   XCAP root directory URI
         * @param intendedId  X-3GPP-Intended-Id
         */
        public NoReplyTimer(XcapUri cdUri, String parentUri, String intendedId) {
            super(cdUri, parentUri, intendedId);
        }

        /**
         * Constructor with initial time value.
         *
         * @param cdUri       XCAP document URI
         * @param parentUri   XCAP root directory URI
         * @param intendedId  X-3GPP-Intended-Id
         * @param initValue   time value
         */
        public NoReplyTimer(XcapUri cdUri, String parentUri, String intendedId, int initValue) {
            super(cdUri, parentUri, intendedId);
            mValue = initValue;
        }

        @Override
        protected String getNodeName() {
            return NODE_NAME;
        }

        public int getValue() {
            return mValue;
        }

        public void setValue(int value) {
            mValue = value;
        }

        /**
         * Convert to XML string.
         *
         * @return XML string
         */
        public String toXmlString() {
            return "<NoReplyTimer>" + String.valueOf(mValue)
                    + "</NoReplyTimer>";
        }
    }
}
