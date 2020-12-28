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

package net.ktnx.mobileledger.json;

import android.content.res.Resources;
import android.util.SparseArray;

import net.ktnx.mobileledger.R;

public enum API {
    auto(0), html(-1), v1_14(-2), v1_15(-3), v1_19_1(-4);
    private static final SparseArray<API> map = new SparseArray<>();
    public static API[] allVersions = {v1_19_1, v1_15, v1_14};

    static {
        for (API item : API.values()) {
            map.put(item.value, item);
        }
    }

    private final int value;

    API(int value) {
        this.value = value;
    }
    public static API valueOf(int i) {
        return map.get(i, auto);
    }
    public int toInt() {
        return this.value;
    }
    public String getDescription(Resources resources) {
        switch (this) {
            case auto:
                return resources.getString(R.string.api_auto);
            case html:
                return resources.getString(R.string.api_html);
            case v1_14:
                return resources.getString(R.string.api_1_14);
            case v1_15:
                return resources.getString(R.string.api_1_15);
            case v1_19_1:
                return resources.getString(R.string.api_1_19_1);
            default:
                throw new IllegalStateException("Unexpected value: " + value);
        }
    }
    public String getDescription() {
        switch (this) {
            case auto:
                return "(automatic)";
            case html:
                return "(HTML)";
            case v1_14:
                return "1.14";
            case v1_15:
                return "1.15";
            case v1_19_1:
                return "1.19.1";
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }
    }
}
