/*
 * Copyright © 2019 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.async;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;

public class CommitAccountsTask
        extends AsyncTask<CommitAccountsTaskParams, Void, ArrayList<LedgerAccount>> {
    protected ArrayList<LedgerAccount> doInBackground(CommitAccountsTaskParams... params) {
        Data.backgroundTaskCount.incrementAndGet();
        ArrayList<LedgerAccount> newList = new ArrayList<>();
        String profile = Data.profile.get().getUuid();
        try {

            SQLiteDatabase db = MLDB.getWritableDatabase();
            db.beginTransaction();
            try {
                for (LedgerAccount acc : params[0].accountList) {
                    Log.d("CAT", String.format("Setting %s to %s", acc.getName(),
                            acc.isHiddenToBe() ? "hidden" : "starred"));
                    db.execSQL("UPDATE accounts SET hidden=? WHERE profile=? AND name=?",
                            new Object[]{acc.isHiddenToBe() ? 1 : 0, profile, acc.getName()});

                    acc.setHidden(acc.isHiddenToBe());
                    if (!params[0].showOnlyStarred || !acc.isHidden()) newList.add(acc);
                }
                db.setTransactionSuccessful();
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            Data.backgroundTaskCount.decrementAndGet();
        }

        return newList;
    }
}
