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

package net.ktnx.mobileledger.utils;

import java.util.concurrent.locks.Lock;

public class LockHolder implements AutoCloseable {
    private Lock rLock, wLock;
    LockHolder(Lock rLock) {
        this.rLock = rLock;
        this.wLock = null;
    }
    public LockHolder(Lock rLock, Lock wLock) {
        this.rLock = rLock;
        this.wLock = wLock;
    }
    @Override
    public void close() {
        if (wLock != null) wLock.unlock();
        if (rLock != null) rLock.unlock();
    }
    public void downgrade() {
        if (rLock == null) throw new IllegalStateException("no locks are held");

        if (wLock == null) return;

        wLock.unlock();
        wLock = null;
    }
}
