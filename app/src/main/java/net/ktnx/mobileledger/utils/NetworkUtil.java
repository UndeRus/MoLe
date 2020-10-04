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

package net.ktnx.mobileledger.utils;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.model.MobileLedgerProfile;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.ktnx.mobileledger.utils.Logger.debug;

public final class NetworkUtil {
    private static final int thirtySeconds = 30000;
    @NotNull
    public static HttpURLConnection prepareConnection(@NonNull MobileLedgerProfile profile,
                                                      @NonNull String path) throws IOException {
        return prepareConnection(profile.getUrl(), path, profile.isAuthEnabled());
    }
    public static HttpURLConnection prepareConnection(@NonNull String url, @NonNull String path,
                                                      boolean authEnabled) throws IOException {
        String connectURL = url;
        if (!connectURL.endsWith("/"))
            connectURL += "/";
        connectURL += path;
        debug("network", "Connecting to " + connectURL);
        HttpURLConnection http = (HttpURLConnection) new URL(connectURL).openConnection();
        http.setAllowUserInteraction(true);
        http.setRequestProperty("Accept-Charset", "UTF-8");
        http.setInstanceFollowRedirects(false);
        http.setUseCaches(false);
        http.setReadTimeout(thirtySeconds);
        http.setConnectTimeout(thirtySeconds);

        return http;
    }
}
