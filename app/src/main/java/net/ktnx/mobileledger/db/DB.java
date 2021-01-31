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

package net.ktnx.mobileledger.db;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.dao.CurrencyDAO;
import net.ktnx.mobileledger.dao.PatternAccountDAO;
import net.ktnx.mobileledger.dao.PatternHeaderDAO;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

@Database(version = 52, entities = {PatternHeader.class, PatternAccount.class, Currency.class})
abstract public class DB extends RoomDatabase {
    private static DB instance;
    public static DB get() {
        if (instance != null)
            return instance;
        synchronized (DB.class) {
            if (instance != null)
                return instance;

            return instance =
                    Room.databaseBuilder(App.instance, DB.class, MobileLedgerDatabase.DB_NAME)
                        .addMigrations(new Migration[]{new Migration(51, 52) {
                            @Override
                            public void migrate(@NonNull SupportSQLiteDatabase db) {
                                db.beginTransaction();
                                try {
                                    db.execSQL("create index fk_pattern_accounts_pattern on " +
                                               "pattern_accounts(pattern_id);");
                                    db.execSQL("create index fk_pattern_accounts_currency on " +
                                               "pattern_accounts(currency);");
                                    db.setTransactionSuccessful();
                                }
                                finally {
                                    db.endTransaction();
                                }
                            }
                        }
                        })
                        .build();
        }
    }
    public abstract PatternHeaderDAO getPatternDAO();

    public abstract PatternAccountDAO getPatternAccountDAO();

    public abstract CurrencyDAO getCurrencyDAO();
}
