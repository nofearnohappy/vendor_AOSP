package com.mediatek.simservs.xcap;

import com.mediatek.simservs.client.policy.RuleSet;


/**
 * Rule Type interface.
 *
 */
public interface RuleType {

    /**
    * Get rule set.
    *
    * @return Ruleset
    */
    public RuleSet getRuleSet();

    /**
     * Save ruleset to server.
     *
     * @throws  XcapException if XCAP error
     */
    public void saveRuleSet() throws XcapException;

    /**
     * Create ruleset.
     *
     * @return  ruleset
     */
    public RuleSet createNewRuleSet();

    /**
     * Save rule to server.
     *
     * @param ruleId rule to be saved by the id
     * @throws  XcapException if XCAP error
     */
    public void saveRule(String ruleId) throws XcapException;
}
