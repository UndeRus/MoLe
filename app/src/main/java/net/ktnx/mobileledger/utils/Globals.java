/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.utils;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.ColorInt;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public final class Globals {
    @ColorInt
    public static int table_row_even_bg;
    @ColorInt
    public static int table_row_odd_bg;
    @ColorInt
    public static int primaryDark, defaultTextColor;
    public static void hideSoftKeyboard(Activity act) {
        // hide the keyboard
        View v = act.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm =
                    (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

}