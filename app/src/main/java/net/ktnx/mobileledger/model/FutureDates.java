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

package net.ktnx.mobileledger.model;

import android.content.res.Resources;
import android.util.SparseArray;

import net.ktnx.mobileledger.R;

public enum FutureDates {
    None(0), OneWeek(7), TwoWeeks(14), OneMonth(30), TwoMonths(60), ThreeMonths(90),
    SixMonths(180), OneYear(365), All(-1);
    private static final SparseArray<FutureDates> map = new SparseArray<>();

    static {
        for (FutureDates item : FutureDates.values()) {
            map.put(item.value, item);
        }
    }

    private final int value;
    FutureDates(int value) {
        this.value = value;
    }
    public static FutureDates valueOf(int i) {
        return map.get(i, None);
    }
    public int toInt() {
        return this.value;
    }
    public String getText(Resources resources) {
        switch (value) {
            case 7:
                return resources.getString(R.string.future_dates_7);
            case 14:
                return resources.getString(R.string.future_dates_14);
            case 30:
                return resources.getString(R.string.future_dates_30);
            case 60:
                return resources.getString(R.string.future_dates_60);
            case 90:
                return resources.getString(R.string.future_dates_90);
            case 180:
                return resources.getString(R.string.future_dates_180);
            case 365:
                return resources.getString(R.string.future_dates_365);
            case -1:
                return resources.getString(R.string.future_dates_all);
            default:
                return resources.getString(R.string.future_dates_none);
        }
    }
}
