package com.mediatek.rcs.pam.model;

import com.mediatek.rcs.pam.PAMException;

import java.util.LinkedList;
import java.util.List;

public class AccountsInfo implements SanityCheck {
    public final List<PublicAccount> accounts;

    public AccountsInfo() {
        accounts = new LinkedList<PublicAccount>();
    }

    @Override
    public void checkSanity() throws PAMException {
        for (PublicAccount pa : accounts) {
            pa.checkSanity();
        }
    }

    public void checkBasicSanity() throws PAMException {
        for (PublicAccount pa : accounts) {
            pa.checkBasicSanity();
        }
    }
}
