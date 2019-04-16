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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class DbOpQueue {
    static private final BlockingQueue<DbOpItem> queue = new LinkedBlockingQueue<>();
    static private DbOpRunner runner;
    synchronized static public void init() {
        if (runner != null) return;
        debug("opQueue", "Starting runner thread");
        runner = new DbOpRunner(queue);
        runner.start();
    }
    static public void done() {
        runner.interrupt();
    }
    public static void add(String sql, Object[] params) {
        init();
        debug("opQueue", "Adding " + sql);
        queue.add(new DbOpItem(sql, params));
    }
    static void add(String sql) {
        queue.add(new DbOpItem(sql));
    }
}
