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

import android.content.res.Resources;
import android.database.SQLException;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.dao.AccountDAO;
import net.ktnx.mobileledger.dao.CurrencyDAO;
import net.ktnx.mobileledger.dao.TemplateAccountDAO;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.dao.TransactionDAO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.ktnx.mobileledger.utils.Logger.debug;

@Database(version = DB.REVISION,
          entities = {TemplateHeader.class, TemplateAccount.class, Currency.class, Account.class,
                      Profile.class, Option.class, AccountValue.class, DescriptionHistory.class,
                      Transaction.class, TransactionAccount.class
          })
abstract public class DB extends RoomDatabase {
    public static final int REVISION = 58;
    public static final String DB_NAME = "MoLe.db";
    private static DB instance;
    public static DB get() {
        if (instance != null)
            return instance;
        synchronized (DB.class) {
            if (instance != null)
                return instance;

            return instance = Room.databaseBuilder(App.instance, DB.class, DB_NAME)
                                  .addMigrations(new Migration[]{singleVersionMigration(17),
                                                                 singleVersionMigration(18),
                                                                 singleVersionMigration(19),
                                                                 singleVersionMigration(20),
                                                                 multiVersionMigration(20, 22),
                                                                 multiVersionMigration(22, 30),
                                                                 multiVersionMigration(30, 32),
                                                                 multiVersionMigration(32, 34),
                                                                 multiVersionMigration(34, 40),
                                                                 singleVersionMigration(41),
                                                                 multiVersionMigration(41, 58),
                                                                 })
                                  .addCallback(new Callback() {
                                      @Override
                                      public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                          super.onOpen(db);
                                          db.execSQL("PRAGMA foreign_keys = ON");
                                          db.execSQL("pragma case_sensitive_like=ON;");

                                      }
                                  })
                                  .build();
        }
    }
    private static Migration singleVersionMigration(int toVersion) {
        return new Migration(toVersion - 1, toVersion) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase db) {
                String fileName = String.format(Locale.US, "db_%d", toVersion);

                applyRevisionFile(db, fileName);
            }
        };
    }
    private static Migration multiVersionMigration(int fromVersion, int toVersion) {
        return new Migration(fromVersion, toVersion) {
            @Override
            public void migrate(@NonNull SupportSQLiteDatabase db) {
                String fileName = String.format(Locale.US, "db_%d_%d", fromVersion, toVersion);

                applyRevisionFile(db, fileName);
            }
        };
    }
    public static void applyRevisionFile(@NonNull SupportSQLiteDatabase db, String fileName) {
        final Resources rm = App.instance.getResources();
        int res_id = rm.getIdentifier(fileName, "raw", App.instance.getPackageName());
        if (res_id == 0)
            throw new SQLException(String.format(Locale.US, "No resource for %s", fileName));

        try (InputStream res = rm.openRawResource(res_id)) {
            debug("db", "Applying " + fileName);
            InputStreamReader isr = new InputStreamReader(res);
            BufferedReader reader = new BufferedReader(isr);

            Pattern endOfStatement = Pattern.compile(";\\s*(?:--.*)?$");

            String line;
            String sqlStatement = null;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.startsWith("--"))
                    continue;
                if (line.isEmpty())
                    continue;

                if (sqlStatement == null)
                    sqlStatement = line;
                else
                    sqlStatement = sqlStatement.concat(" " + line);

                Matcher m = endOfStatement.matcher(line);
                if (!m.find())
                    continue;

                try {
                    db.execSQL(sqlStatement);
                    sqlStatement = null;
                }
                catch (Exception e) {
                    throw new RuntimeException(
                            String.format("Error applying %s, line %d, statement: %s", fileName,
                                    lineNo, sqlStatement), e);
                }
            }

            if (sqlStatement != null)
                throw new RuntimeException(String.format(
                        "Error applying %s: EOF after continuation. Line %s, Incomplete " +
                        "statement: %s", fileName, lineNo, sqlStatement));

        }
        catch (IOException e) {
            throw new RuntimeException(String.format("Error opening raw resource for %s", fileName),
                    e);
        }
    }
    public abstract TemplateHeaderDAO getTemplateDAO();

    public abstract TemplateAccountDAO getTemplateAccountDAO();

    public abstract CurrencyDAO getCurrencyDAO();

    public abstract AccountDAO getAccountDAO();

    public abstract TransactionDAO getTransactionDAO();
}
