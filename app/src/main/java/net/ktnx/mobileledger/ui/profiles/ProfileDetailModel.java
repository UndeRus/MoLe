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

import net.ktnx.mobileledger.async.SendTransactionTask;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.Misc;

public class ProfileDetailModel extends ViewModel {
    private static final String HTTPS_URL_START = "https://";
    private final MutableLiveData<String> profileName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> postingPermitted = new MutableLiveData<>(true);
    private final MutableLiveData<Currency> defaultCommodity = new MutableLiveData<>(null);
    private final MutableLiveData<MobileLedgerProfile.FutureDates> futureDates =
            new MutableLiveData<>(MobileLedgerProfile.FutureDates.None);
    private final MutableLiveData<Boolean> showCommodityByDefault = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showCommentsByDefault = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> useAuthentication = new MutableLiveData<>(false);
    private final MutableLiveData<SendTransactionTask.API> apiVersion =
            new MutableLiveData<>(SendTransactionTask.API.auto);
    private final MutableLiveData<String> url = new MutableLiveData<>(null);
    private final MutableLiveData<String> authUserName = new MutableLiveData<>(null);
    private final MutableLiveData<String> authPassword = new MutableLiveData<>(null);
    private final MutableLiveData<String> preferredAccountsFilter = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> themeId = new MutableLiveData<>(-1);
    public int initialThemeHue = Colors.DEFAULT_HUE_DEG;
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
    MobileLedgerProfile.FutureDates getFutureDates() {
        return futureDates.getValue();
    }
    void setFutureDates(MobileLedgerProfile.FutureDates newValue) {
        if (newValue != futureDates.getValue())
            futureDates.setValue(newValue);
    }
    void observeFutureDates(LifecycleOwner lfo, Observer<MobileLedgerProfile.FutureDates> o) {
        futureDates.observe(lfo, o);
    }
    Currency getDefaultCommodity() {
        return defaultCommodity.getValue();
    }
    void setDefaultCommodity(Currency newValue) {
        if (newValue != defaultCommodity.getValue())
            defaultCommodity.setValue(newValue);
    }
    void observeDefaultCommodity(LifecycleOwner lfo, Observer<Currency> o) {
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
    Boolean getUseAuthentication() {
        return useAuthentication.getValue();
    }
    void setUseAuthentication(boolean newValue) {
        if (newValue != useAuthentication.getValue())
            useAuthentication.setValue(newValue);
    }
    void observeUseAuthentication(LifecycleOwner lfo, Observer<Boolean> o) {
        useAuthentication.observe(lfo, o);
    }
    SendTransactionTask.API getApiVersion() {
        return apiVersion.getValue();
    }
    void setApiVersion(SendTransactionTask.API newValue) {
        if (newValue != apiVersion.getValue())
            apiVersion.setValue(newValue);
    }
    void observeApiVersion(LifecycleOwner lfo, Observer<SendTransactionTask.API> o) {
        apiVersion.observe(lfo, o);
    }
    String getUrl() {
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
    String getAuthUserName() {
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
    String getAuthPassword() {
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
    void setValuesFromProfile(MobileLedgerProfile mProfile, int newProfileHue) {
        final int profileThemeId;
        if (mProfile != null) {
            profileName.setValue(mProfile.getName());
            postingPermitted.setValue(mProfile.isPostingPermitted());
            showCommentsByDefault.setValue(mProfile.getShowCommentsByDefault());
            showCommodityByDefault.setValue(mProfile.getShowCommodityByDefault());
            {
                String comm = mProfile.getDefaultCommodity();
                if (TextUtils.isEmpty(comm))
                    setDefaultCommodity(null);
                else
                    setDefaultCommodity(new Currency(-1, comm));
            }
            futureDates.setValue(mProfile.getFutureDates());
            apiVersion.setValue(mProfile.getApiVersion());
            url.setValue(mProfile.getUrl());
            useAuthentication.setValue(mProfile.isAuthEnabled());
            authUserName.setValue(mProfile.isAuthEnabled() ? mProfile.getAuthUserName() : "");
            authPassword.setValue(mProfile.isAuthEnabled() ? mProfile.getAuthPassword() : "");
            preferredAccountsFilter.setValue(mProfile.getPreferredAccountsFilter());
            themeId.setValue(mProfile.getThemeHue());
        }
        else {
            profileName.setValue(null);
            url.setValue(HTTPS_URL_START);
            postingPermitted.setValue(true);
            showCommentsByDefault.setValue(true);
            showCommodityByDefault.setValue(false);
            setFutureDates(MobileLedgerProfile.FutureDates.None);
            setApiVersion(SendTransactionTask.API.auto);
            useAuthentication.setValue(false);
            authUserName.setValue("");
            authPassword.setValue("");
            preferredAccountsFilter.setValue(null);
            themeId.setValue(newProfileHue);
        }


    }
    void updateProfile(MobileLedgerProfile mProfile) {
        mProfile.setName(profileName.getValue());
        mProfile.setUrl(url.getValue());
        mProfile.setPostingPermitted(postingPermitted.getValue());
        mProfile.setShowCommentsByDefault(showCommentsByDefault.getValue());
        Currency c = defaultCommodity.getValue();
        mProfile.setDefaultCommodity((c == null) ? null : c.getName());
        mProfile.setShowCommodityByDefault(showCommodityByDefault.getValue());
        mProfile.setPreferredAccountsFilter(preferredAccountsFilter.getValue());
        mProfile.setAuthEnabled(useAuthentication.getValue());
        mProfile.setAuthUserName(authUserName.getValue());
        mProfile.setAuthPassword(authPassword.getValue());
        mProfile.setThemeHue(themeId.getValue());
        mProfile.setFutureDates(futureDates.getValue());
        mProfile.setApiVersion(apiVersion.getValue());
    }
}
