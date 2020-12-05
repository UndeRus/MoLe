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

package net.ktnx.mobileledger.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

public class HledgerVersion {
    private final int major;
    private final int minor;
    private final int patch;
    private final boolean isPre_1_20;
    private final boolean hasPatch;
    public HledgerVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
        this.patch = 0;
        this.isPre_1_20 = false;
        this.hasPatch = false;
    }
    public HledgerVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.isPre_1_20 = false;
        this.hasPatch = true;
    }
    public HledgerVersion(boolean pre_1_20) {
        if (!pre_1_20)
            throw new IllegalArgumentException("pre_1_20 argument must be true");
        this.major = this.minor = this.patch = 0;
        this.isPre_1_20 = true;
        this.hasPatch = false;
    }
    public HledgerVersion(HledgerVersion origin) {
        this.major = origin.major;
        this.minor = origin.minor;
        this.isPre_1_20 = origin.isPre_1_20;
        this.patch = origin.patch;
        this.hasPatch = origin.hasPatch;
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof HledgerVersion))
            return false;
        HledgerVersion that = (HledgerVersion) obj;

        return (this.isPre_1_20 == that.isPre_1_20 && this.major == that.major &&
                this.minor == that.minor && this.patch == that.patch &&
                this.hasPatch == that.hasPatch);
    }
    public boolean isPre_1_20() {
        return isPre_1_20;
    }
    public int getMajor() {
        return major;
    }
    public int getMinor() {
        return minor;
    }
    public int getPatch() {
        return patch;
    }
    @NonNull
    @Override
    public String toString() {
        if (isPre_1_20)
            return "(before 1.20)";
        return hasPatch ? String.format(Locale.ROOT, "%d.%d.%d", major, minor, patch)
                        : String.format(Locale.ROOT, "%d.%d", major, minor);
    }
}
