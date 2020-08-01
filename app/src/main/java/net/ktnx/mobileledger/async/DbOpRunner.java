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

import android.database.sqlite.SQLiteDatabase;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.BuildConfig;

import java.util.concurrent.BlockingQueue;

import static net.ktnx.mobileledger.utils.Logger.debug;

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
                debug("opQrunner", "Got " + item.sql);
                {
                    SQLiteDatabase db = App.getDatabase();
                    if (BuildConfig.DEBUG) {
                        StringBuilder b = new StringBuilder("Executing ");
                        b.append(item.sql);
                        if (item.params.length > 0) {
                            boolean first = true;
                            b.append(" [");
                            for (Object p : item.params) {
                                if (first)
                                    first = false;
                                else
                                    b.append(", ");
                                b.append(p.toString());
                            }
                            b.append("]");
                        }
                        debug("opQrunner", b.toString());
                    }
                    db.execSQL(item.sql, item.params);
                }
                if (item.onReady != null)
                    item.onReady.run();
            }
            catch (InterruptedException e) {
                break;
            }
        }
    }
}
