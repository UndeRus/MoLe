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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.widget.TextView;

import androidx.annotation.ArrayRes;
import androidx.annotation.StringRes;

import net.ktnx.mobileledger.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelpDialog {
    private final static Pattern MARKDOWN_LINK_PATTERN =
            Pattern.compile("\\[([^\\[]+)]\\(([^)]*)\\)");
    public static void show(Context context, @StringRes int title, @ArrayRes int content) {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(title);
        String message = TextUtils.join("\n\n", context.getResources()
                                                       .getStringArray(content));

        SpannableStringBuilder richTextMessage = new SpannableStringBuilder();
        while (true) {
            Matcher m = MARKDOWN_LINK_PATTERN.matcher(message);
            if (m.find()) {
                richTextMessage.append(message.substring(0, m.start()));
                String linkText = m.group(1);
                assert linkText != null;
                String linkURL = m.group(2);
                assert linkURL != null;

                if (linkText.isEmpty())
                    linkText = linkURL;

                int spanStart = richTextMessage.length();
                richTextMessage.append(linkText);
                richTextMessage.setSpan(new URLSpan(linkURL), spanStart,
                        spanStart + linkText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                URLSpan linkSpan = new URLSpan(linkText);

                message = message.substring(m.end());
            }
            else {
                richTextMessage.append(message);
                break;
            }
        }
        adb.setMessage(richTextMessage);
        adb.setPositiveButton(R.string.close_button, (dialog, buttonId) -> dialog.dismiss());
        final AlertDialog dialog = adb.create();
        dialog.show();
        ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(
                LinkMovementMethod.getInstance());
    }
}
