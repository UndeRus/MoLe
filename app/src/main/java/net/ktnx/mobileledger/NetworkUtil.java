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

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

final class NetworkUtil {
    static HttpURLConnection prepare_connection(SharedPreferences pref, String path) throws IOException {
        final String backend_url = pref.getString("backend_url", "");
        final boolean use_auth = pref.getBoolean("backend_use_http_auth", false);
        Log.d("network", "Connecting to "+backend_url + "/" + path);
        HttpURLConnection http = (HttpURLConnection) new URL(backend_url + "/" + path).openConnection();
        if (use_auth) {
            final String auth_user = pref.getString("backend_auth_user", "");
            final String auth_password = pref.getString("backend_auth_password", "");
            final byte[] bytes = (String.format("%s:%s", auth_user, auth_password)).getBytes("UTF-8");
            final String value = Base64.encodeToString(bytes, Base64.DEFAULT);
            http.setRequestProperty("Authorization", "Basic " + value);
        }
        http.setAllowUserInteraction(false);
        http.setRequestProperty("Accept-Charset", "UTF-8");
        http.setInstanceFollowRedirects(false);
        http.setUseCaches(false);

        return http;
    }
}
