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

import net.ktnx.mobileledger.model.MobileLedgerProfile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class NetworkUtil {
    private static final int thirtySeconds = 30000;
    public static HttpURLConnection prepareConnection(MobileLedgerProfile profile, String path)
            throws IOException {
        String url = profile.getUrl();
        final boolean use_auth = profile.isAuthEnabled();
        if (!url.endsWith("/")) url += "/";
        url += path;
        debug("network", "Connecting to " + url);
        HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
        http.setAllowUserInteraction(true);
        http.setRequestProperty("Accept-Charset", "UTF-8");
        http.setInstanceFollowRedirects(false);
        http.setUseCaches(false);
        http.setReadTimeout(thirtySeconds);
        http.setConnectTimeout(thirtySeconds);

        return http;
    }
}
