/*
 * Copyright © 2020 Damyan Ivanov.
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

import java.util.HashMap;
import java.util.Map;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class RefreshDescriptionsTask extends AsyncTask<Void, Void, Void> {
    @Override
    protected Void doInBackground(Void... voids) {
        Map<String, Boolean> unique = new HashMap<>();

        debug("descriptions", "Starting refresh");
        SQLiteDatabase db = App.getDatabase();

//        Data.backgroundTaskStarted();
        try {
            db.beginTransactionNonExclusive();
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
                debug("descriptions", "Refresh successful");
            }
            finally {
                db.endTransaction();
            }
        }
        finally {
//            Data.backgroundTaskFinished();
            debug("descriptions", "Refresh done");
        }

        return null;
    }
}
