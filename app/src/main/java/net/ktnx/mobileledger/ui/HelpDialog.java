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

package net.ktnx.mobileledger.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.ArrayRes;
import androidx.annotation.StringRes;

import net.ktnx.mobileledger.R;

public class HelpDialog {
    public static void show(Context context, @StringRes int title, @ArrayRes int content) {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(title);
        adb.setMessage(TextUtils.join("\n\n", context.getResources()
                                                     .getStringArray(content)));
        adb.setPositiveButton(R.string.close_button, (dialog, buttonId) -> dialog.dismiss());
        adb.create()
           .show();
    }
}
