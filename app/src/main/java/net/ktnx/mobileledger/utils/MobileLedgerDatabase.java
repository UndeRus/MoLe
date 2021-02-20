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

package net.ktnx.mobileledger.utils;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.lifecycle.MutableLiveData;

import net.ktnx.mobileledger.db.DB;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class MobileLedgerDatabase extends SQLiteOpenHelper {
    public static final MutableLiveData<Boolean> initComplete = new MutableLiveData<>(false);
    public MobileLedgerDatabase(Application context) {
        super(context, DB.DB_NAME, null, DB.REVISION);
        debug("db", "creating helper instance");
        super.setWriteAheadLoggingEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        throw new IllegalStateException("Should not happen. Where's Room!?");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("Should not happen. Where's Room!?");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // force a check by Room to ensure everything is OK
        // TODO: remove when all DB access is via Room
        DB.get()
          .compileStatement("SELECT COUNT(*) FROM profiles");
    }
}
