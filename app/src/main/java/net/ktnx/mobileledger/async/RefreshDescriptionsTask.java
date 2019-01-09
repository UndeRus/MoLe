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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.HashMap;
import java.util.Map;

public class RefreshDescriptionsTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... voids) {
        Map<String, Boolean> unique = new HashMap<>();

        Log.d("descriptions", "Starting refresh");
        SQLiteDatabase db = MLDB.getWritableDatabase();

        Data.backgroundTaskCount.incrementAndGet();
        try {
            db.beginTransaction();
            try {
                db.execSQL("UPDATE description_history set keep=0");
                try (Cursor c = db
                        .rawQuery("SELECT distinct description from transactions", null))
                {
                    while (c.moveToNext()) {
                        String description = c.getString(0);
                        String descriptionUpper = description.toUpperCase();
                        if (unique.containsKey(descriptionUpper)) continue;

                        db.execSQL(
                                "replace into description_history(description, description_upper, " +
                                "keep) values(?, ?, 1)", new String[]{description, descriptionUpper});
                        unique.put(descriptionUpper, true);
                    }
                }
                db.execSQL("DELETE from description_history where keep=0");
                db.setTransactionSuccessful();
                Log.d("descriptions", "Refresh successful");
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
            Data.backgroundTaskCount.decrementAndGet();
            Log.d("descriptions", "Refresh done");
        }

        return null;
    }
}
