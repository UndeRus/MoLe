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

package net.ktnx.mobileledger;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

class UrlEncodedFormData {
    private List<AbstractMap.SimpleEntry<String,String>> pairs;

    UrlEncodedFormData() {
        pairs = new ArrayList<>();
    }

    void add_pair(String name, String value) {
        pairs.add(new AbstractMap.SimpleEntry<String,String>(name, value));
    }

    @NonNull
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry<String,String> pair : pairs) {
            if (first) {
                first = false;
            }
            else {
                result.append('&');
            }

            try {
                result.append(URLEncoder.encode(pair.getKey(), "UTF-8"))
                      .append('=')
                      .append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }
}
