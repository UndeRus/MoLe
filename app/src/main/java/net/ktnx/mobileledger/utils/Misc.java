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

import android.app.Activity;
import android.content.res.Configuration;
import android.view.WindowManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class Misc {
    public static boolean isZero(float f) {
        return (f < 0.005) && (f > -0.005);
    }
    public static void showSoftKeyboard(Activity activity) {
        // make the keyboard appear
        Configuration cf = activity.getResources()
                                   .getConfiguration();
        if (cf.keyboard == Configuration.KEYBOARD_NOKEYS ||
            cf.keyboardHidden == Configuration.KEYBOARDHIDDEN_YES)
            activity.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    public static void showSoftKeyboard(Fragment fragment) {
        final FragmentActivity activity = fragment.getActivity();
        if (activity != null)
            showSoftKeyboard(activity);
    }
    public static void hideSoftKeyboard(Fragment fragment) {
        final FragmentActivity activity = fragment.getActivity();
        if (activity != null)
            hideSoftKeyboard(activity);
    }
    public static void hideSoftKeyboard(Activity activity) {
        Configuration cf = activity.getResources()
                                   .getConfiguration();
        if (cf.keyboard == Configuration.KEYBOARD_NOKEYS ||
            cf.keyboardHidden == Configuration.KEYBOARDHIDDEN_NO)
            activity.getWindow()
                    .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }
    public static String emptyIsNull(String str) {
        return "".equals(str) ? null : str;
    }
    public static String nullIsEmpty(String str) {
        return (str == null) ? "" : str;
    }
    public static boolean isEmptyOrNull (String str) {
        if (str == null) return true;
        return str.isEmpty();
    }
    public static boolean equalStrings(String u, CharSequence text) {
        return nullIsEmpty(u).equals(text.toString());
    }
    public static boolean equalStrings(String a, String b) {
        return nullIsEmpty(a).equals(nullIsEmpty(b));
    }
    public static boolean isEmptyOrNull(CharSequence text) {
        if (text == null)
            return true;
        return text.length() == 0;
    }
}
