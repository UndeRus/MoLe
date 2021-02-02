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
import net.ktnx.mobileledger.dao.TemplateAccountDAO;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

@Database(version = 54, entities = {TemplateHeader.class, TemplateAccount.class, Currency.class})
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
                        }, new Migration(52, 53) {
                            @Override
                            public void migrate(@NonNull SupportSQLiteDatabase db) {
                                db.execSQL(
                                        "alter table pattern_accounts add negate_amount boolean;");
                            }
                        }, new Migration(53, 54) {
                            @Override
                            public void migrate(@NonNull SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE templates (id INTEGER PRIMARY KEY " +
                                           "AUTOINCREMENT NOT NULL, name TEXT NOT NULL, " +
                                           "regular_expression TEXT NOT NULL, test_text TEXT, " +
                                           "transaction_description TEXT, " +
                                           "transaction_description_match_group INTEGER, " +
                                           "transaction_comment TEXT, " +
                                           "transaction_comment_match_group INTEGER, date_year " +
                                           "INTEGER, date_year_match_group INTEGER, date_month " +
                                           "INTEGER, date_month_match_group INTEGER, date_day " +
                                           "INTEGER, date_day_match_group INTEGER)");
                                db.execSQL(
                                        "CREATE TABLE template_accounts (id INTEGER PRIMARY KEY " +
                                        "AUTOINCREMENT NOT NULL, template_id INTEGER NOT NULL, " +
                                        "acc TEXT, position INTEGER NOT NULL, acc_match_group " +
                                        "INTEGER, currency INTEGER, currency_match_group INTEGER," +
                                        " amount REAL, amount_match_group INTEGER, comment TEXT, " +
                                        "comment_match_group INTEGER, negate_amount INTEGER, " +
                                        "FOREIGN KEY(template_id) REFERENCES templates(id) ON " +
                                        "UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY" +
                                        "(currency) REFERENCES currencies(id) ON UPDATE NO ACTION" +
                                        " ON DELETE NO ACTION )");
                                db.execSQL("insert into templates(id, name, regular_expression, " +
                                           "test_text, transaction_description, " +
                                           "transaction_description_match_group, " +
                                           "transaction_comment, transaction_comment_match_group," +
                                           " date_year, date_year_match_group, date_month, " +
                                           "date_month_match_group, date_day, " +
                                           "date_day_match_group)" +
                                           " select id, name, regular_expression, test_text, " +
                                           "transaction_description, " +
                                           "transaction_description_match_group, " +
                                           "transaction_comment, transaction_comment_match_group," +
                                           " date_year, date_year_match_group, date_month, " +
                                           "date_month_match_group, date_day, " +
                                           "date_day_match_group from patterns");
                                db.execSQL("insert into template_accounts(id, template_id, acc, " +
                                           "position, acc_match_group, currency, " +
                                           "currency_match_group, amount, amount_match_group, " +
                                           "amount, amount_match_group, comment, " +
                                           "comment_match_group, negate_amount) select id, " +
                                           "pattern_id, acc, position, acc_match_group, " +
                                           "currency, " +
                                           "currency_match_group, amount, amount_match_group, " +
                                           "amount, amount_match_group, comment, " +
                                           "comment_match_group, negate_amount from " +
                                           "pattern_accounts");
                                db.execSQL("create index fk_template_accounts_template on " +
                                           "template_accounts(template_id)");
                                db.execSQL("create index fk_template_accounts_currency on " +
                                           "template_accounts(currency)");
                                db.execSQL("drop table pattern_accounts");
                                db.execSQL("drop table patterns");
                            }
                        }
                        })
                        .build();
        }
    }
    public abstract TemplateHeaderDAO getPatternDAO();

    public abstract TemplateAccountDAO getPatternAccountDAO();

    public abstract CurrencyDAO getCurrencyDAO();
}
