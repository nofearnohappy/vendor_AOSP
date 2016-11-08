
package com.mediatek.simservs.test;

import android.util.Log;

import com.mediatek.simservs.client.CommunicationDiversion;
import com.mediatek.simservs.client.CommunicationWaiting;
import com.mediatek.simservs.client.IncomingCommunicationBarring;
import com.mediatek.simservs.client.OriginatingIdentityPresentation;
import com.mediatek.simservs.client.OriginatingIdentityPresentationRestriction;
import com.mediatek.simservs.client.OutgoingCommunicationBarring;
import com.mediatek.simservs.client.SimServs;
import com.mediatek.simservs.client.policy.Actions;
import com.mediatek.simservs.client.policy.Conditions;
import com.mediatek.simservs.client.policy.Rule;
import com.mediatek.simservs.client.policy.RuleSet;
import com.mediatek.simservs.xcap.XcapException;
import com.mediatek.xcap.client.XcapClient;
import com.mediatek.xcap.client.XcapConstants;
import com.mediatek.xcap.client.uri.XcapUri;
import com.mediatek.xcap.client.uri.XcapUri.XcapDocumentSelector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Simservs test class.
 */
public class SimservsTest {

    static final private String TAG = "SimservsTest";

    static final private String INITIAL_DOC =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "    <resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\">\r\n" +
            "        <communication-waiting active=\"true\"/>\r\n" +
            "        <originating-identity-presentation active=\"true\"/>\r\n" +
            "        <originating-identity-presentation-restriction active=\"true\"/>\r\n" +
            "        <outgoing-communication-barring active=\"true\">\r\n" +
            "             <ruleset>\r\n" +
            "                  <rule id=\"AO\">\r\n" +
            "                       <actions>\r\n" +
            "                            <allow>false</allow>\r\n" +
            "                       </actions>\r\n" +
            "                  </rule>\r\n" +
            "             </ruleset>\r\n" +
            "        </outgoing-communication-barring>\r\n" +
            "        <incoming-communication-barring active=\"true\">\r\n" +
            "             <ruleset>\r\n" +
            "                  <rule id=\"IR\">\r\n" +
            "                       <conditions>\r\n" +
            "                            <roaming/>\r\n" +
            "                            <media>audio</media>\r\n" +
            "                       </conditions>\r\n" +
            "                       <actions>\r\n" +
            "                            <allow>false</allow>\r\n" +
            "                       </actions>\r\n" +
            "                  </rule>\r\n" +
            "             </ruleset>\r\n" +
            "        </incoming-communication-barring>\r\n" +
            "        <communication-diversion active=\"true\">\r\n" +
            "            <NoReplyTimer>1000</NoReplyTimer>\r\n" +
            "            <ruleset>\r\n" +
            "                <rule id=\"CFB\">\r\n" +
            "                    <conditions>\r\n" +
            "                        <busy/>\r\n" +
            "                        <media>audio</media>\r\n" +
            "                    </conditions>\r\n" +
            "                    <actions>\r\n" +
            "                        <forward-to>\r\n" +
            "                            <target>\"+886988555222\"</target>\r\n" +
            "                            <notify-caller>true</notify-caller>\r\n" +
            "                        </forward-to>\r\n" +
            "                    </actions>\r\n" +
            "                </rule>\r\n" +
            "                <rule id=\"CFNRc\">\r\n" +
            "                    <conditions>\r\n" +
            "                        <not-reachable/>\r\n" +
            "                        <media>audio</media>\r\n" +
            "                    </conditions>\r\n" +
            "                    <actions>\r\n" +
            "                        <forward-to>\r\n" +
            "                            <target>\"+886988555222\"</target>\r\n" +
            "                            <notify-caller>true</notify-caller>\r\n" +
            "                        </forward-to>\r\n" +
            "                    </actions>\r\n" +
            "                </rule>\r\n" +
            "            </ruleset>\r\n" +
            "        </communication-diversion>\r\n" +
            "        <communication-diversion-serv-cap active=\"true\">\r\n" +
            "          <serv-cap-conditions>\r\n" +
            "             <serv-cap-external-list provisioned=\"false\">\r\n" +
            "               </serv-cap-external-list>\r\n" +
            "             <serv-cap-identity provisioned=\"true\"></serv-cap-identity>\r\n" +
            "             <serv-cap-media>\r\n" +
            "                <media>audio</media>\r\n" +
            "                <media>video</media>\r\n" +
            "              </serv-cap-media>\r\n" +
            "             <serv-cap-presence-status provisioned=\"false\">\r\n" +
            "               </serv-cap-presence-status>\r\n" +
            "             <serv-cap-validity provisioned=\"false\"></serv-cap-validity>\r\n" +
            "          </serv-cap-conditions>\r\n" +
            "          <serv-cap-actions>\r\n" +
            "               <serv-cap-target>\r\n" +
            "                  <telephony-type/>\r\n" +
            "               </serv-cap-target>\r\n" +
            "               <serv-cap-notify-served-user-on-outbound-call provisioned=\"false\">" +
            "                 </serv-cap-notify-served-user-on-outbound-call>\r\n" +
            "              <serv-cap-reveal-identity-to-caller provisioned=\"false\">\r\n" +
            "                </serv-cap-reveal-identity-to-caller>\r\n" +
            "              <serv-cap-reveal-served-user-identity-to-caller provisioned=\"false\">" +
            "                </serv-cap-reveal-served-user-identity-to-caller>\r\n" +
            "              <serv-cap-reveal-identity-to-target provisioned=\"false\">\r\n" +
            "                </serv-cap-reveal-identity-to-target>\r\n" +
            "          </serv-cap-actions>\r\n" +
            "        </communication-diversion-serv-cap>\r\n" +
            "        <communication-barring-serv-cap active=\"true\">\r\n" +
            "            <serv-cap-conditions>\r\n" +
            "                <serv-cap-communication-diverted provisioned=\"false\">\r\n" +
            "                   </serv-cap-communication-diverted>\r\n" +
            "                <serv-cap-external-list provisioned=\"false\">" +
            "                   </serv-cap-external-list>\r\n" +
            "                <serv-cap-identity provisioned=\"false\"></serv-cap-identity>\r\n" +
            "                <serv-cap-media>\r\n" +
            "                    <media>audio</media>\r\n" +
            "                    <media>video</media>\r\n" +
            "                </serv-cap-media>\r\n" +
            "                <serv-cap-other-identity provisioned=\"false\">" +
            "                   </serv-cap-other-identity>\r\n" +
            "                <serv-cap-presence-status provisioned=\"false\">" +
            "                   </serv-cap-presence-status>\r\n" +
            "                <serv-cap-roaming provisioned=\"true\"></serv-cap-roaming>\r\n" +
            "                <serv-cap-rule-deactivated provisioned=\"false\">" +
            "                   </serv-cap-rule-deactivated>\r\n" +
            "                <serv-cap-request-name provisioned=\"false\">" +
            "                   </serv-cap-request-name>\r\n" +
            "                <serv-cap-validity provisioned=\"false\"></serv-cap-validity>\r\n" +
            "            </serv-cap-conditions>\r\n" +
            "        </communication-barring-serv-cap>\r\n" +
            "    </resource-lists>\r\n";

    static final private String XCAP_ROOT = "http://192.168.1.2:8080/";
    static final private String TEST_USER = "sip:user@anritsu-cscf.com";
    static final private String TEST_DOC = "simservs";

    /**
     * Fetch document of supplementary service configuration.
     *
     * @param ra XcapClient instance
     * @param documentURI document location URI
     */
    private static void getDoc(XcapClient ra,
            URI documentURI) throws IOException {

        HttpResponse response;
        response = ra.get(documentURI, null);

        if (response != null) {
            if (response.getStatusLine().getStatusCode() == 200) {
                Log.d("info", "response 200, response = " + response.toString());
            } else {
                Log.d("server", "bad response from xcap server: " + response.toString());
            }
        } else {
            Log.d("server", "unable to retreive document in xcap server...");
        }
    }

    /**
     * Configure supplementary service.
     *
     * @param ra XcapClient instance
     * @param documentURI document location URI
     */
    private static void putDoc(XcapClient ra,
            URI uri) throws IOException {
        URI documentURI = uri;
        HttpResponse response;

        // put the document and get sync response
        response = ra.put(documentURI, "application/resource-lists+xml", INITIAL_DOC);
        // check put response
        if (response != null) {
            if (response.getStatusLine().getStatusCode() == 200
                    || response.getStatusLine().getStatusCode() == 201) {
                Log.d("info", "document created in xcap server...");
            } else {
                Log.d("server", "bad response from xcap server: " + response.toString());
            }
        } else {
            Log.d("server", "unable to create document in xcap server...");
        }

    }

    /**
     * Remove configuration of supplementary service.
     *
     * @param ra XcapClient instance
     * @param documentURI document location URI
     */
    private static void deleteDoc(XcapClient ra,
            URI uri) throws IOException {
        URI documentURI = uri;
        HttpResponse response;

        response = ra.delete(documentURI);

        // check get response
        if (response != null) {
            if (response.getStatusLine().getStatusCode() == 200) {
                Log.d("info", "document retreived in xcap server and content is the expected...");
                Log.d("info", "sync test suceed :)");
            } else {
                Log.d("server", "bad response from xcap server: " + response.toString());
            }
        } else {
            Log.d("server", "unable to retreive document in xcap server...");
        }
    }

    /**
     * Test suite.
     *
     * @throws URISyntaxException if URI syntax error
     * @throws IOException        if I/O error
     */
    static public void syncTest() throws URISyntaxException, IOException {
        String documentUri = null;
        SimServs simservs = SimServs.getInstance();

        simservs.setXui("user@chinaTel.com");
        simservs.setIntendedId("user@chinaTel.com");

        simservs.setXuiByImsiMccMnc("234150999999999", "234", "15");
        String xui = simservs.getXui();
        if (xui == null) {
            Exception e = new Exception("XUI test fail");
            e.printStackTrace();
            throw e;
        }

        simservs.setXcapRootByImpi("user@chinaTel.com");
        documentUri = simservs.getDocumentUri();

        simservs.setXcapRootByImpi("2341509999999999@ims.mnc466.mcc97.3gppnetwork.org");
        documentUri = simservs.getDocumentUri();

        simservs.setXcapRootByMccMnc("466", "97");
        documentUri = simservs.getDocumentUri();

        XcapClient ra = new XcapClient();

        XcapDocumentSelector documentSelector = new XcapDocumentSelector(
                XcapConstants.AUID_RESOURCE_LISTS, TEST_USER, TEST_DOC);
        Log.d(TAG, "document selector is " + documentSelector.toString());
        XcapUri xcapUri = new XcapUri();
        xcapUri.setXcapRoot(XCAP_ROOT).setDocumentSelector(documentSelector);

        // ====================================

        try {
            CommunicationWaiting cw = simservs.getCommunicationWaiting(xcapUri, TEST_USER,
                    "password");
            CommunicationDiversion cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            IncomingCommunicationBarring icb = simservs.getIncomingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            OutgoingCommunicationBarring ocb = simservs.getOutgoingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            OriginatingIdentityPresentation oip = simservs.getOriginatingIdentityPresentation(
                    xcapUri, TEST_USER, "password");
            OriginatingIdentityPresentationRestriction oir = simservs
                    .getOriginatingIdentityPresentationRestriction(xcapUri, TEST_USER,
                            "password");

            // ====================================
            // De-active test
            cw.setActive(false);
            cw = simservs.getCommunicationWaiting(xcapUri, TEST_USER,
                    "password");
            if (cw.isActive()) {
                throw new Exception("UT De-active test fail");
            }

            cd.setActive(false);
            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            if (cd.isActive()) {
                throw new Exception("UT De-active test fail");
            }

            icb.setActive(false);
            icb = simservs.getIncomingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            if (icb.isActive()) {
                throw new Exception("UT De-active test fail");
            }

            ocb.setActive(false);
            ocb = simservs.getOutgoingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            if (ocb.isActive()) {
                throw new Exception("UT De-active test fail");
            }

            oip.setActive(false);
            oip = simservs.getOriginatingIdentityPresentation(
                    xcapUri, TEST_USER, "password");
            if (oip.isActive()) {
                throw new Exception("UT De-active test fail");
            }

            oir.setActive(false);
            oir = simservs.getOriginatingIdentityPresentationRestriction(xcapUri, TEST_USER,
                    "password");
            if (oir.isActive()) {
                throw new Exception("UT De-active test fail");
            }

            // ====================================
            // Active test
            cw.setActive(true);
            cw = simservs.getCommunicationWaiting(xcapUri, TEST_USER,
                    "password");
            if (!cw.isActive()) {
                throw new Exception("UT Active test fail");
            }

            cd.setActive(true);
            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            if (!cd.isActive()) {
                throw new Exception("UT Active test fail");
            }

            icb.setActive(true);
            icb = simservs.getIncomingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            if (!icb.isActive()) {
                throw new Exception("UT Active test fail");
            }

            ocb.setActive(true);
            ocb = simservs.getOutgoingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            if (!ocb.isActive()) {
                throw new Exception("UT Active test fail");
            }

            oip.setActive(true);
            oip = simservs.getOriginatingIdentityPresentation(
                    xcapUri, TEST_USER, "password");
            if (!oip.isActive()) {
                throw new Exception("UT Active test fail");
            }

            oir.setActive(true);
            oir = simservs.getOriginatingIdentityPresentationRestriction(xcapUri, TEST_USER,
                    "password");
            if (!oir.isActive()) {
                throw new Exception("UT Active test fail");
            }

            // ====================================
            // OIR default-behaviour test
            oir.setDefaultPresentationRestricted(false);
            oir = simservs
                    .getOriginatingIdentityPresentationRestriction(xcapUri, TEST_USER,
                            "password");
            if (oir.isDefaultPresentationRestricted()) {
                throw new Exception("UT default-behaviour test fail");
            }

            oir.setDefaultPresentationRestricted(true);
            oir = simservs
                    .getOriginatingIdentityPresentationRestriction(xcapUri, TEST_USER,
                            "password");
            if (!oir.isDefaultPresentationRestricted()) {
                throw new Exception("UT default-behaviour test fail");
            }

            // ====================================
            // CD NoReplyTimer test
            int cdNoReplayTimer = cd.getNoReplyTimer();
            cd.setNoReplyTimer(555);
            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            if (cd.getNoReplyTimer() != 555) {
                throw new Exception("UT CD NoReplyTimer test fail");

            }

            cd.setNoReplyTimer(cdNoReplayTimer);
            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            if (cd.getNoReplyTimer() != cdNoReplayTimer) {
                throw new Exception("UT CD NoReplyTimer test fail");
            }

            // ====================================
            // Clear ruleset test
            RuleSet ruleSet = cd.getRuleSet();
            ruleSet.clearRules();
            cd.saveRuleSet();

            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            ruleSet = cd.getRuleSet();
            if (ruleSet.getRules().size() > 0) {
                throw new Exception("UT CD Clear ruleset test fail");
            }

            // ====================================
            // Add a rule test
            Rule rule = ruleSet.createNewRule("Add a rule test");
            cd.saveRuleSet();

            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            ruleSet = cd.getRuleSet();
            if (ruleSet.getRules().size() == 0) {
                throw new Exception("UT CD create rule test fail");
            }

            // ====================================
            // Add a condition within a rule test
            rule = ruleSet.getRules().get(0);
            Conditions conditions = rule.createConditions();
            conditions.addRoaming();
            cd.saveRuleSet();

            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            ruleSet = cd.getRuleSet();
            if (ruleSet.getRules().size() == 0) {
                throw new Exception("UT Add a condition on a rule test fail");
            } else {
                rule = ruleSet.getRules().get(0);
                if (!rule.getConditions().comprehendRoaming()) {
                    throw new Exception("UT Add a condition on a rule test fail");
                }
            }

            // ====================================
            // Add a media within a condition test
            rule = ruleSet.getRules().get(0);
            conditions = rule.getConditions();
            conditions.addMedia("audio");
            cd.saveRuleSet();

            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            ruleSet = cd.getRuleSet();
            if (ruleSet.getRules().size() == 0) {
                throw new Exception("UT Add a condition media within a rule test fail");
            } else {
                rule = ruleSet.getRules().get(0);
                List<String> medias = rule.getConditions().getMedias();
                if (!medias.get(0).equals("audio")) {
                    throw new Exception("UT Add a condition media within a rule test fail");
                }
            }

            // ====================================
            // Unfold multiple conditions test
            if (!simservs.LIB_CONFIG_MULTIPLE_RULE_CONDITIONS) {
                rule = ruleSet.getRules().get(0);
                conditions = rule.getConditions();
                conditions.addBusy();
                conditions.addMedia("video");
                cd.saveRuleSet();

                cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                        "password");
                ruleSet = cd.getRuleSet();
                if (ruleSet.getRules().size() < 4) {
                    throw new Exception("UT Unfold multiple conditions test fail");
                }
                // Save to server and check again
                cd.saveRuleSet();

                cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                        "password");
                ruleSet = cd.getRuleSet();
                if (ruleSet.getRules().size() < 4) {
                    throw new Exception("UT Unfold multiple conditions test fail");
                }
            }
            // ====================================
            // CD Target modify test
            ruleSet = cd.getRuleSet();
            ruleSet.clearRules();
            rule = ruleSet.createNewRule("CD Target modify test");
            rule.createConditions().addBusy();
            Actions actions = rule.createActions();
            actions.setFowardTo("0912364587", true);
            cd.saveRuleSet();

            cd = simservs.getCommunicationDiversion(xcapUri, TEST_USER,
                    "password");
            ruleSet = cd.getRuleSet();
            if (ruleSet.getRules().size() == 0) {
                throw new Exception("UT CD Target modify test fail");
            } else {
                rule = ruleSet.getRules().get(0);
                if (!rule.getConditions().comprehendBusy()) {
                    throw new Exception("UT CD Target modify test fail");
                } else {
                    if (!rule.getActions().getFowardTo().getTarget().endsWith("0912364587")) {
                        throw new Exception("UT CD Target modify test fail");
                    }
                    if (!rule.getActions().getFowardTo().isNotifyCaller()) {
                        throw new Exception("UT CD Target modify test fail");
                    }
                }
            }
            // ====================================
            // CB Action allow test
            ruleSet = icb.getRuleSet();
            ruleSet.clearRules();

            rule = ruleSet.createNewRule("CB Action allow test");
            rule.createConditions().addInternational();
            actions = rule.createActions();
            actions.setAllow(true);
            icb.saveRuleSet();

            icb = simservs.getIncomingCommunicationBarring(xcapUri,
                    TEST_USER, "password");
            ruleSet = icb.getRuleSet();
            if (ruleSet.getRules().size() == 0) {
                throw new Exception("UT CB Action allow test fail");
            } else {
                rule = ruleSet.getRules().get(0);
                if (!rule.getConditions().comprehendInternational()) {
                    throw new Exception("UT CB Action allow test fail");
                } else {
                    if (!rule.getActions().isAllow()) {
                        throw new Exception("UT CB Action allow test fail");
                    }
                }
            }
            // ====================================
        } catch (XcapException e) {
            int httpErrorCode = -1;
            int exceptionCodeCode = -1;
            if (e.isConnectionError()) {
                exceptionCodeCode = e.getExceptionCodeCode();
                Log.e("Simservs", "httpErrorCode=" + httpErrorCode);
            } else {
                httpErrorCode = e.getHttpErrorCode();
                Log.e("Simservs", "exceptionCodeCode=" + exceptionCodeCode);
            }
            e.printStackTrace();
        }

        /** Delete a XML document **/
        // deleteDoc(ra, xcapUri.toURI(), credentials);
        // ====================================
        ra.shutdown();
    }
}
