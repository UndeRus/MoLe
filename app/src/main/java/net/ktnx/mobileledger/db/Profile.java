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

package net.ktnx.mobileledger.db;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Transaction;

import net.ktnx.mobileledger.dao.AccountDAO;
import net.ktnx.mobileledger.dao.OptionDAO;
import net.ktnx.mobileledger.dao.TransactionDAO;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity(tableName = "profiles",
        indices = {@Index(name = "profiles_uuid_idx", unique = true, value = "uuid")})
public class Profile {
    public static final long NO_PROFILE_ID = 0;
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    private long id;
    @NonNull
    @ColumnInfo
    private String name = "";
    @NonNull
    @ColumnInfo()
    private String uuid;
    @NonNull
    @ColumnInfo
    private String url = "";
    @ColumnInfo(name = "use_authentication")
    private boolean useAuthentication;
    @ColumnInfo(name = "auth_user")
    private String authUser;
    @ColumnInfo(name = "auth_password")
    private String authPassword;
    @ColumnInfo(name = "order_no")
    private int orderNo;
    @ColumnInfo(name = "permit_posting")
    private boolean permitPosting;
    @ColumnInfo(defaultValue = "-1")
    private int theme = -1;
    @ColumnInfo(name = "preferred_accounts_filter")
    private String preferredAccountsFilter;
    @ColumnInfo(name = "future_dates")
    private int futureDates;
    @ColumnInfo(name = "api_version")
    private int apiVersion;
    @ColumnInfo(name = "show_commodity_by_default")
    private boolean showCommodityByDefault;
    @ColumnInfo(name = "default_commodity")
    private String defaultCommodity;
    @ColumnInfo(name = "show_comments_by_default", defaultValue = "1")
    private boolean showCommentsByDefault = true;
    @ColumnInfo(name = "detected_version_pre_1_19")
    private boolean detectedVersionPre_1_19;
    @ColumnInfo(name = "detected_version_major")
    private int detectedVersionMajor;
    @ColumnInfo(name = "detected_version_minor")
    private int detectedVersionMinor;
    public Profile() {
        uuid = UUID.randomUUID()
                   .toString();
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    @NonNull
    public String getName() {
        return name;
    }
    public void setName(@NonNull String name) {
        this.name = name;
    }
    @NonNull
    public String getUrl() {
        return url;
    }
    public void setUrl(@NonNull String url) {
        this.url = url;
    }
    public boolean useAuthentication() {
        return useAuthentication;
    }
    public void setUseAuthentication(boolean useAuthentication) {
        this.useAuthentication = useAuthentication;
    }
    public String getAuthUser() {
        return authUser;
    }
    public void setAuthUser(String authUser) {
        this.authUser = authUser;
    }
    public String getAuthPassword() {
        return authPassword;
    }
    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }
    public int getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }
    public boolean permitPosting() {
        return permitPosting;
    }
    public void setPermitPosting(boolean permitPosting) {
        this.permitPosting = permitPosting;
    }
    public int getTheme() {
        return theme;
    }
    public void setTheme(int theme) {
        this.theme = theme;
    }
    public String getPreferredAccountsFilter() {
        return preferredAccountsFilter;
    }
    public void setPreferredAccountsFilter(String preferredAccountsFilter) {
        this.preferredAccountsFilter = preferredAccountsFilter;
    }
    public int getFutureDates() {
        return futureDates;
    }
    public void setFutureDates(int futureDates) {
        this.futureDates = futureDates;
    }
    public int getApiVersion() {
        return apiVersion;
    }
    public void setApiVersion(int apiVersion) {
        this.apiVersion = apiVersion;
    }
    public boolean getShowCommodityByDefault() {
        return showCommodityByDefault;
    }
    public void setShowCommodityByDefault(boolean showCommodityByDefault) {
        this.showCommodityByDefault = showCommodityByDefault;
    }
    @NotNull
    public String getDefaultCommodity() {
        return defaultCommodity;
    }
    public void setDefaultCommodity(@org.jetbrains.annotations.Nullable String defaultCommodity) {
        this.defaultCommodity = Misc.nullIsEmpty(defaultCommodity);
    }
    public boolean getShowCommentsByDefault() {
        return showCommentsByDefault;
    }
    public void setShowCommentsByDefault(boolean showCommentsByDefault) {
        this.showCommentsByDefault = showCommentsByDefault;
    }
    public boolean detectedVersionPre_1_19() {
        return detectedVersionPre_1_19;
    }
    public void setDetectedVersionPre_1_19(boolean detectedVersionPre_1_19) {
        this.detectedVersionPre_1_19 = detectedVersionPre_1_19;
    }
    public int getDetectedVersionMajor() {
        return detectedVersionMajor;
    }
    public void setDetectedVersionMajor(int detectedVersionMajor) {
        this.detectedVersionMajor = detectedVersionMajor;
    }
    public int getDetectedVersionMinor() {
        return detectedVersionMinor;
    }
    public void setDetectedVersionMinor(int detectedVersionMinor) {
        this.detectedVersionMinor = detectedVersionMinor;
    }
    @NonNull
    @Override
    public String toString() {
        return getName();
    }
    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof Profile))
            return false;
        Profile p = (Profile) o;
        return id == p.id && Misc.equalStrings(name, p.name) && Misc.equalStrings(uuid, p.uuid) &&
               Misc.equalStrings(url, p.url) && useAuthentication == p.useAuthentication &&
               Misc.equalStrings(authUser, p.authUser) &&
               Misc.equalStrings(authPassword, p.authPassword) && orderNo == p.orderNo &&
               permitPosting == p.permitPosting && theme == p.theme &&
               Misc.equalStrings(preferredAccountsFilter, p.preferredAccountsFilter) &&
               futureDates == p.futureDates && apiVersion == p.apiVersion &&
               showCommentsByDefault == p.showCommentsByDefault &&
               Misc.equalStrings(defaultCommodity, p.defaultCommodity) &&
               showCommentsByDefault == p.showCommentsByDefault &&
               detectedVersionPre_1_19 == p.detectedVersionPre_1_19 &&
               detectedVersionMajor == p.detectedVersionMajor &&
               detectedVersionMinor == p.detectedVersionMinor;
    }
    @Transaction
    public void wipeAllDataSync() {
        OptionDAO optDao = DB.get()
                             .getOptionDAO();
        optDao.deleteSync(optDao.allForProfileSync(id));

        AccountDAO accDao = DB.get()
                              .getAccountDAO();
        accDao.deleteSync(accDao.allForProfileSync(id));

        TransactionDAO trnDao = DB.get()
                                  .getTransactionDAO();
        trnDao.deleteSync(trnDao.getAllForProfileUnorderedSync(id));
    }
    public void wipeAllData() {
        AsyncTask.execute(this::wipeAllDataSync);
    }

}
