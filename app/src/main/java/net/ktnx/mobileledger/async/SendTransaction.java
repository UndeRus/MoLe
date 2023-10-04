package net.ktnx.mobileledger.async;

import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.LedgerTransaction;

public interface SendTransaction {

    String getError();

    void send(Profile profile, LedgerTransaction transaction, boolean simulate, TaskCallback taskCallback);
}