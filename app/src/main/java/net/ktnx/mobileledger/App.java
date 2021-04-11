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

package net.ktnx.mobileledger;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailModel;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MobileLedgerDatabase;

import org.jetbrains.annotations.NotNull;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Locale;

public class App extends Application {
    public static final String PREF_NAME = "MoLe";
    public static final String PREF_THEME_HUE = "theme-hue";
    public static final String PREF_PROFILE_ID = "profile-id";
    public static App instance;
    private static ProfileDetailModel profileModel;
    private MobileLedgerDatabase dbHelper;
    private boolean monthNamesPrepared = false;
    public static SQLiteDatabase getDatabase() {
        if (instance == null)
            throw new RuntimeException("Application not created yet");

        return instance.getDB();
    }
    public static void prepareMonthNames() {
        instance.prepareMonthNames(false);
    }
    public static void setAuthenticationDataFromProfileModel(ProfileDetailModel model) {
        profileModel = model;
    }
    public static void resetAuthenticationData() {
        profileModel = null;
    }
    public static void storeStartupProfileAndTheme(long currentProfileId, int currentTheme) {
        SharedPreferences prefs =
                instance.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_PROFILE_ID,
                currentProfileId);
        editor.putInt(PREF_THEME_HUE, currentTheme);
        editor.apply();
    }
    public static long getStartupProfile() {
        SharedPreferences prefs = instance.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getLong(PREF_PROFILE_ID, -1);
    }
    public static int getStartupTheme() {
        SharedPreferences prefs = instance.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        return prefs.getInt(PREF_THEME_HUE, Colors.DEFAULT_HUE_DEG);
    }
    private String getAuthURL() {
        if (profileModel != null)
            return profileModel.getUrl();
        return Data.getProfile()
                   .getUrl();
    }
    private String getAuthUserName() {
        if (profileModel != null)
            return profileModel.getAuthUserName();
        return Data.getProfile()
                   .getAuthUserName();
    }
    private String getAuthPassword() {
        if (profileModel != null)
            return profileModel.getAuthPassword();
        return Data.getProfile()
                   .getAuthPassword();
    }
    private boolean getAuthEnabled() {
        if (profileModel != null)
            return profileModel.getUseAuthentication();
        return Data.getProfile()
                   .isAuthEnabled();
    }
    @Override
    public void onCreate() {
        Logger.debug("flow", "App onCreate()");
        instance = this;
        super.onCreate();
        Data.refreshCurrencyData(Locale.getDefault());
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getAuthEnabled()) {
                    try {
                        final URL url = new URL(getAuthURL());
                        final String requestingHost = getRequestingHost();
                        final String expectedHost = url.getHost();
                        if (requestingHost.equalsIgnoreCase(expectedHost))
                            return new PasswordAuthentication(getAuthUserName(),
                                    getAuthPassword().toCharArray());
                        else
                            Log.w("http-auth",
                                    String.format("Requesting host [%s] differs from expected [%s]",
                                            requestingHost, expectedHost));
                    }
                    catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }

                return super.getPasswordAuthentication();
            }
        });
    }
    private void prepareMonthNames(boolean force) {
        if (force || monthNamesPrepared)
            return;
        Resources rm = getResources();
        Globals.monthNames = rm.getStringArray(R.array.month_names);
        monthNamesPrepared = true;
    }
    @Override
    public void onTerminate() {
        Logger.debug("flow", "App onTerminate()");
        if (dbHelper != null)
            dbHelper.close();
        super.onTerminate();
    }
    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        prepareMonthNames(true);
        Data.refreshCurrencyData(Locale.getDefault());
        Data.locale.setValue(Locale.getDefault());
    }
    public SQLiteDatabase getDB() {
        if (dbHelper == null)
            initDb();

        return dbHelper.getWritableDatabase();
    }
    private synchronized void initDb() {
        if (dbHelper != null)
            return;

        // Let Room do any possible migrations
        // this method may be removed when all DB access is made via Room
        DB.get()
          .compileStatement("select count(*) from profiles");
        dbHelper = new MobileLedgerDatabase(this);
    }
}
