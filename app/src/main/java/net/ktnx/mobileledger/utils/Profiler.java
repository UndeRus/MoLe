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

import net.ktnx.mobileledger.BuildConfig;

import java.util.Locale;

public class Profiler {
    private final String name;
    private long opStart = 0;
    private long opCount = 0;
    private long opMills = 0;
    public Profiler(String name) {
        this.name = name;
    }
    public void opStart() {
        if (!BuildConfig.DEBUG)
            return;

        if (opStart != 0)
            throw new IllegalStateException("opStart() already called with no opEnd()");
        this.opStart = System.currentTimeMillis();
        opCount++;
    }
    public void opEnd() {
        if (!BuildConfig.DEBUG)
            return;

        if (opStart == 0)
            throw new IllegalStateException("opStart() not called");
        opMills += System.currentTimeMillis() - opStart;
        opStart = 0;
    }
    public void dumpStats() {
        if (!BuildConfig.DEBUG)
            return;

        Logger.debug("profiler", String.format(Locale.ROOT,
                "Operation '%s' executed %d times for %d ms. Average time %4.2fms", name, opCount,
                opMills, 1.0 * opMills / opCount));
    }
}
