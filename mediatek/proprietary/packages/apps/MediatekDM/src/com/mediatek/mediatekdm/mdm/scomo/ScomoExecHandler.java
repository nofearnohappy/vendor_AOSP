package com.mediatek.mediatekdm.mdm.scomo;

import com.mediatek.mediatekdm.mdm.MdmException;
import com.mediatek.mediatekdm.mdm.PLRegistry;
import com.mediatek.mediatekdm.mdm.scomo.MdmScomo.Registry;

public class ScomoExecHandler {

    protected MdmScomo mScomo;

    public ScomoExecHandler(MdmScomo scomo) {
        mScomo = scomo;
    }

    protected void saveExecInfo(String correlator, String source) {
        try {
            PLRegistry registry = mScomo.getPLRegistry();
            String currentAccount = mScomo.getEngine().getCurrentAccount();

            // 1. update accounts information
            String accountsPath = Registry.makePath(Registry.ROOT, "exec", "accounts");
            String accounts = registry.getStringValue(accountsPath);
            if (accounts != null && accounts.length() != 0) {
                String[] accountList = accounts.split(Registry.SEPERATOR_REGEXP);
                boolean found = false;
                for (String acc : accountList) {
                    if (acc.equals(currentAccount)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    registry.setStringValue(accountsPath, accounts + "|" + currentAccount);
                }
            } else {
                registry.setStringValue(accountsPath, currentAccount);
            }

            // 2. update correlators information
            String correlatorsPath = Registry.makePath(Registry.ROOT, "exec", currentAccount,
                    "correlators");
            String correlators = registry.getStringValue(correlatorsPath);
            if (correlators != null && correlators.length() != 0) {
                String[] correlatorList = correlators.split(Registry.SEPERATOR_REGEXP);
                boolean found = false;
                for (String c : correlatorList) {
                    if (c.equals(correlator)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    registry.setStringValue(correlatorsPath, correlators + "|" + correlator);
                }
            } else {
                registry.setStringValue(correlatorsPath, correlator);
            }
            // 3. save information associated with correlator
            registry.setStringValue(
                    Registry.makePath(Registry.ROOT, "exec", currentAccount, correlator, "source"),
                    source);
        } catch (MdmException e) {
            e.printStackTrace();
        }
    }

}
