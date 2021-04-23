/*
 * Copyright © 2021 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.async;

import android.os.AsyncTask;

import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static net.ktnx.mobileledger.db.Profile.NO_PROFILE_ID;
import static net.ktnx.mobileledger.utils.Logger.debug;

public class UpdateTransactionsTask extends AsyncTask<MainModel, Void, String> {
    protected String doInBackground(MainModel[] parentModel) {
        final Profile profile = Data.getProfile();

        long profileId = (profile == null) ? NO_PROFILE_ID : profile.getId();
        Data.backgroundTaskStarted();
        try {
            Logger.debug("UTT", "Starting DB transaction list retrieval");

            final MainModel model = parentModel[0];
            final String accFilter = model.getAccountFilter()
                                          .getValue();
            final List<TransactionWithAccounts> transactions;

            if (profileId == NO_PROFILE_ID)
                transactions = new ArrayList<>();
            else if (accFilter == null) {
                transactions = DB.get()
                                 .getTransactionDAO()
                                 .getAllWithAccountsSync(profileId);
            }
            else {
                transactions = DB.get()
                                 .getTransactionDAO()
                                 .getAllWithAccountsFilteredSync(profileId, accFilter);
            }

            TransactionAccumulator accumulator = new TransactionAccumulator(model);

            for (TransactionWithAccounts tr : transactions) {
                if (isCancelled())
                    return null;

                accumulator.put(new LedgerTransaction(tr));
            }

            accumulator.done();
            debug("UTT", "transaction list value updated");

            return null;
        }
        finally {
            Data.backgroundTaskFinished();
        }
    }
}
