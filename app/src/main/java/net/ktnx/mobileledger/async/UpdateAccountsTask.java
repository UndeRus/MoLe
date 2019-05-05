/*
 * Copyright © 2019 Damyan Ivanov.
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

import java.util.ArrayList;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class UpdateAccountsTask extends AsyncTask<Void, Void, ArrayList<LedgerAccount>> {
    protected ArrayList<LedgerAccount> doInBackground(Void... params) {
        Data.backgroundTaskStarted();
        try {
            MobileLedgerProfile profile = Data.profile.getValue();
            if (profile == null) throw new AssertionError();
            String profileUUID = profile.getUuid();
            boolean onlyStarred = Data.optShowOnlyStarred.get();
            ArrayList<LedgerAccount> newList = new ArrayList<>();

            String sql = "SELECT a.name from accounts a WHERE a.profile = ?";
            if (onlyStarred) sql += " AND a.hidden = 0";
            sql += " ORDER BY a.name";

            SQLiteDatabase db = App.getDatabase();
            try (Cursor cursor = db.rawQuery(sql, new String[]{profileUUID})) {
                while (cursor.moveToNext()) {
                    final String accName = cursor.getString(0);
//                    debug("accounts",
//                            String.format("Read account '%s' from DB [%s]", accName, profileUUID));
                    LedgerAccount acc = profile.loadAccount(db, accName);
                    if (acc.isVisible(newList)) newList.add(acc);
                }
            }

            return newList;
        }
        finally {
            debug("UAT", "decrementing background task count");
            Data.backgroundTaskFinished();
        }
    }
}
