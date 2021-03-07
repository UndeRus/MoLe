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

import android.app.Activity;
import android.content.res.Configuration;
import android.text.Editable;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.jetbrains.annotations.Contract;

public class Misc {
    public static boolean isZero(float f) {
        return (f < 0.005) && (f > -0.005);
    }
    public static boolean equalFloats(float a, float b) { return isZero(a - b); }
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
        return str != null && str.isEmpty() ? null : str;
    }
    public static String nullIsEmpty(String str) {
        return (str == null) ? "" : str;
    }
    public static String nullIsEmpty(Editable e) {
        if (e == null)
            return "";
        return e.toString();
    }
    public static boolean equalStrings(String u, CharSequence text) {
        return nullIsEmpty(u).equals(text.toString());
    }
    public static boolean equalStrings(String a, String b) {
        return nullIsEmpty(a).equals(nullIsEmpty(b));
    }
    public static String trim(@Nullable String string) {
        if (string == null)
            return null;

        return string.trim();
    }
    @Contract(value = "null, null -> true; null, !null -> false; !null, null -> false", pure = true)
    public static boolean equalIntegers(Integer a, Integer b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;

        return a.equals(b);
    }
    @Contract(value = "null, null -> true; null, !null -> false; !null, null -> false", pure = true)
    public static boolean equalLongs(Long a, Long b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;

        return a.equals(b);
    }
}
