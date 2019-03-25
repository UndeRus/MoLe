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

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.ktnx.mobileledger.utils.MLDB;

import java.util.concurrent.BlockingQueue;

class DbOpRunner extends Thread {
    private final BlockingQueue<DbOpItem> queue;
    public DbOpRunner(BlockingQueue<DbOpItem> queue) {
        this.queue = queue;
    }
    @Override
    public void run() {
        while (!interrupted()) {
            try {
                DbOpItem item = queue.take();
                Log.d("opQrunner", "Got "+item.sql);
                SQLiteDatabase db = MLDB.getDatabase();
                Log.d("opQrunner", "Executing "+item.sql);
                db.execSQL(item.sql, item.params);
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }
}
