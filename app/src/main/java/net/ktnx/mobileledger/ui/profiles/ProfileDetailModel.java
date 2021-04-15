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

package net.ktnx.mobileledger.ui.profiles;

import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.FutureDates;
import net.ktnx.mobileledger.model.HledgerVersion;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.NetworkUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileDetailModel extends ViewModel {
    private static final String HTTPS_URL_START = "https://";
    private final MutableLiveData<String> profileName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> postingPermitted = new MutableLiveData<>(true);
    private final MutableLiveData<String> defaultCommodity = new MutableLiveData<>(null);
    private final MutableLiveData<FutureDates> futureDates =
            new MutableLiveData<>(FutureDates.None);
    private final MutableLiveData<Boolean> showCommodityByDefault = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showCommentsByDefault = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> useAuthentication = new MutableLiveData<>(false);
    private final MutableLiveData<API> apiVersion = new MutableLiveData<>(API.auto);
    private final MutableLiveData<String> url = new MutableLiveData<>(null);
    private final MutableLiveData<String> authUserName = new MutableLiveData<>(null);
    private final MutableLiveData<String> authPassword = new MutableLiveData<>(null);
    private final MutableLiveData<String> preferredAccountsFilter = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> themeId = new MutableLiveData<>(-1);
    private final MutableLiveData<HledgerVersion> detectedVersion = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> detectingHledgerVersion = new MutableLiveData<>(false);
    public int initialThemeHue = Colors.DEFAULT_HUE_DEG;
    private VersionDetectionThread versionDetectionThread;
    public ProfileDetailModel() {
    }
    String getProfileName() {
        return profileName.getValue();
    }
    void setProfileName(String newValue) {
        if (!Misc.nullIsEmpty(newValue)
                 .equals(Misc.nullIsEmpty(profileName.getValue())))
            profileName.setValue(newValue);
    }
    void setProfileName(CharSequence newValue) {
        setProfileName(String.valueOf(newValue));
    }
    void observeProfileName(LifecycleOwner lfo, Observer<String> o) {
        profileName.observe(lfo, o);
    }
    Boolean getPostingPermitted() {
        return postingPermitted.getValue();
    }
    void setPostingPermitted(boolean newValue) {
        if (newValue != postingPermitted.getValue())
            postingPermitted.setValue(newValue);
    }
    void observePostingPermitted(LifecycleOwner lfo, Observer<Boolean> o) {
        postingPermitted.observe(lfo, o);
    }
    public void setShowCommentsByDefault(boolean newValue) {
        if (newValue != showCommentsByDefault.getValue())
            showCommentsByDefault.setValue(newValue);
    }
    void observeShowCommentsByDefault(LifecycleOwner lfo, Observer<Boolean> o) {
        showCommentsByDefault.observe(lfo, o);
    }
    FutureDates getFutureDates() {
        return futureDates.getValue();
    }
    void setFutureDates(FutureDates newValue) {
        if (newValue != futureDates.getValue())
            futureDates.setValue(newValue);
    }
    void observeFutureDates(LifecycleOwner lfo, Observer<FutureDates> o) {
        futureDates.observe(lfo, o);
    }
    String getDefaultCommodity() {
        return defaultCommodity.getValue();
    }
    void setDefaultCommodity(String newValue) {
        if (!Misc.equalStrings(newValue, defaultCommodity.getValue()))
            defaultCommodity.setValue(newValue);
    }
    void observeDefaultCommodity(LifecycleOwner lfo, Observer<String> o) {
        defaultCommodity.observe(lfo, o);
    }
    Boolean getShowCommodityByDefault() {
        return showCommodityByDefault.getValue();
    }
    void setShowCommodityByDefault(boolean newValue) {
        if (newValue != showCommodityByDefault.getValue())
            showCommodityByDefault.setValue(newValue);
    }
    void observeShowCommodityByDefault(LifecycleOwner lfo, Observer<Boolean> o) {
        showCommodityByDefault.observe(lfo, o);
    }
    public Boolean getUseAuthentication() {
        return useAuthentication.getValue();
    }
    void setUseAuthentication(boolean newValue) {
        if (newValue != useAuthentication.getValue())
            useAuthentication.setValue(newValue);
    }
    void observeUseAuthentication(LifecycleOwner lfo, Observer<Boolean> o) {
        useAuthentication.observe(lfo, o);
    }
    API getApiVersion() {
        return apiVersion.getValue();
    }
    void setApiVersion(API newValue) {
        if (newValue != apiVersion.getValue())
            apiVersion.setValue(newValue);
    }
    void observeApiVersion(LifecycleOwner lfo, Observer<API> o) {
        apiVersion.observe(lfo, o);
    }
    HledgerVersion getDetectedVersion() { return detectedVersion.getValue(); }
    void setDetectedVersion(HledgerVersion newValue) {
        if (!Objects.equals(detectedVersion.getValue(), newValue))
            detectedVersion.setValue(newValue);
    }
    void observeDetectedVersion(LifecycleOwner lfo, Observer<HledgerVersion> o) {
        detectedVersion.observe(lfo, o);
    }
    public String getUrl() {
        return url.getValue();
    }
    void setUrl(String newValue) {
        if (!Misc.nullIsEmpty(newValue)
                 .equals(Misc.nullIsEmpty(url.getValue())))
            url.setValue(newValue);
    }
    void setUrl(CharSequence newValue) {
        setUrl(String.valueOf(newValue));
    }
    void observeUrl(LifecycleOwner lfo, Observer<String> o) {
        url.observe(lfo, o);
    }
    public String getAuthUserName() {
        return authUserName.getValue();
    }
    void setAuthUserName(String newValue) {
        if (!Misc.nullIsEmpty(newValue)
                 .equals(Misc.nullIsEmpty(authUserName.getValue())))
            authUserName.setValue(newValue);
    }
    void setAuthUserName(CharSequence newValue) {
        setAuthUserName(String.valueOf(newValue));
    }
    void observeUserName(LifecycleOwner lfo, Observer<String> o) {
        authUserName.observe(lfo, o);
    }
    public String getAuthPassword() {
        return authPassword.getValue();
    }
    void setAuthPassword(String newValue) {
        if (!Misc.nullIsEmpty(newValue)
                 .equals(Misc.nullIsEmpty(authPassword.getValue())))
            authPassword.setValue(newValue);
    }
    void setAuthPassword(CharSequence newValue) {
        setAuthPassword(String.valueOf(newValue));
    }
    void observePassword(LifecycleOwner lfo, Observer<String> o) {
        authPassword.observe(lfo, o);
    }
    String getPreferredAccountsFilter() {
        return preferredAccountsFilter.getValue();
    }
    void setPreferredAccountsFilter(String newValue) {
        if (!Misc.nullIsEmpty(newValue)
                 .equals(Misc.nullIsEmpty(preferredAccountsFilter.getValue())))
            preferredAccountsFilter.setValue(newValue);
    }
    void setPreferredAccountsFilter(CharSequence newValue) {
        setPreferredAccountsFilter(String.valueOf(newValue));
    }
    void observePreferredAccountsFilter(LifecycleOwner lfo, Observer<String> o) {
        preferredAccountsFilter.observe(lfo, o);
    }
    int getThemeId() {
        return themeId.getValue();
    }
    void setThemeId(int newValue) {
        themeId.setValue(newValue);
    }
    void observeThemeId(LifecycleOwner lfo, Observer<Integer> o) {
        themeId.observe(lfo, o);
    }
    void observeDetectingHledgerVersion(LifecycleOwner lfo, Observer<Boolean> o) {
        detectingHledgerVersion.observe(lfo, o);
    }
    void setValuesFromProfile(Profile mProfile) {
        if (mProfile != null) {
            profileName.setValue(mProfile.getName());
            postingPermitted.setValue(mProfile.permitPosting());
            showCommentsByDefault.setValue(mProfile.getShowCommentsByDefault());
            showCommodityByDefault.setValue(mProfile.getShowCommodityByDefault());
            {
                String comm = mProfile.getDefaultCommodity();
                if (TextUtils.isEmpty(comm))
                    setDefaultCommodity(null);
                else
                    setDefaultCommodity(comm);
            }
            futureDates.setValue(
                    FutureDates.valueOf(mProfile.getFutureDates()));
            apiVersion.setValue(API.valueOf(mProfile.getApiVersion()));
            url.setValue(mProfile.getUrl());
            useAuthentication.setValue(mProfile.useAuthentication());
            authUserName.setValue(mProfile.useAuthentication() ? mProfile.getAuthUser() : "");
            authPassword.setValue(mProfile.useAuthentication() ? mProfile.getAuthPassword() : "");
            preferredAccountsFilter.setValue(mProfile.getPreferredAccountsFilter());
            themeId.setValue(mProfile.getTheme());
            detectedVersion.setValue(mProfile.detectedVersionPre_1_19() ? new HledgerVersion(true)
                                                                        : new HledgerVersion(
                                                                                mProfile.getDetectedVersionMajor(),
                                                                                mProfile.getDetectedVersionMinor()));
        }
        else {
            profileName.setValue(null);
            url.setValue(HTTPS_URL_START);
            postingPermitted.setValue(true);
            showCommentsByDefault.setValue(true);
            showCommodityByDefault.setValue(false);
            setFutureDates(FutureDates.None);
            setApiVersion(API.auto);
            useAuthentication.setValue(false);
            authUserName.setValue("");
            authPassword.setValue("");
            preferredAccountsFilter.setValue(null);
            detectedVersion.setValue(null);
        }
    }
    void updateProfile(Profile mProfile) {
        mProfile.setName(profileName.getValue());
        mProfile.setUrl(url.getValue());
        mProfile.setPermitPosting(postingPermitted.getValue());
        mProfile.setShowCommentsByDefault(showCommentsByDefault.getValue());
        mProfile.setDefaultCommodity(defaultCommodity.getValue());
        mProfile.setShowCommodityByDefault(showCommodityByDefault.getValue());
        mProfile.setPreferredAccountsFilter(preferredAccountsFilter.getValue());
        mProfile.setUseAuthentication(useAuthentication.getValue());
        mProfile.setAuthUser(authUserName.getValue());
        mProfile.setAuthPassword(authPassword.getValue());
        mProfile.setTheme(themeId.getValue());
        mProfile.setFutureDates(futureDates.getValue()
                                           .toInt());
        mProfile.setApiVersion(apiVersion.getValue()
                                         .toInt());
        HledgerVersion version = detectedVersion.getValue();
        mProfile.setDetectedVersionPre_1_19(version.isPre_1_20_1());
        mProfile.setDetectedVersionMajor(version.getMajor());
        mProfile.setDetectedVersionMinor(version.getMinor());
    }
    synchronized public void triggerVersionDetection() {
        if (versionDetectionThread != null)
            versionDetectionThread.interrupt();

        versionDetectionThread = new VersionDetectionThread(this);
        versionDetectionThread.start();
    }
    static class VersionDetectionThread extends Thread {
        static final int TARGET_PROCESS_DURATION = 1000;
        private final Pattern versionPattern =
                Pattern.compile("^\"(\\d+)\\.(\\d+)(?:\\.(\\d+))?\"$");
        private final ProfileDetailModel model;
        public VersionDetectionThread(ProfileDetailModel model) {
            this.model = model;
        }
        private HledgerVersion detectVersion() {
            App.setAuthenticationDataFromProfileModel(model);
            HttpURLConnection http;
            try {
                http = NetworkUtil.prepareConnection(model.getUrl(), "version",
                        model.getUseAuthentication());
                switch (http.getResponseCode()) {
                    case 200:
                        break;
                    case 404:
                        return new HledgerVersion(true);
                    default:
                        Logger.debug("profile", String.format(Locale.US,
                                "HTTP error detecting hledger-web version: [%d] %s",
                                http.getResponseCode(), http.getResponseMessage()));
                        return null;
                }
                InputStream stream = http.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String version = reader.readLine();
                Matcher m = versionPattern.matcher(version);
                if (m.matches()) {
                    int major = Integer.parseInt(Objects.requireNonNull(m.group(1)));
                    int minor = Integer.parseInt(Objects.requireNonNull(m.group(2)));
                    final boolean hasPatch = m.groupCount() >= 3;
                    int patch = hasPatch ? Integer.parseInt(Objects.requireNonNull(m.group(3))) : 0;

                    return hasPatch ? new HledgerVersion(major, minor, patch)
                                    : new HledgerVersion(major, minor);
                }
                else {
                    Logger.debug("profile",
                            String.format("Unrecognised version string '%s'", version));
                    return null;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            finally {
                App.resetAuthenticationData();
            }
        }
        @Override
        public void run() {
            model.detectingHledgerVersion.postValue(true);
            try {
                long startTime = System.currentTimeMillis();

                final HledgerVersion version = detectVersion();

                long elapsed = System.currentTimeMillis() - startTime;
                Logger.debug("profile", "Detection duration " + elapsed);
                if (elapsed < TARGET_PROCESS_DURATION) {
                    try {
                        Thread.sleep(TARGET_PROCESS_DURATION - elapsed);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                model.detectedVersion.postValue(version);
            }
            finally {
                model.detectingHledgerVersion.postValue(false);
            }
        }
    }
}
