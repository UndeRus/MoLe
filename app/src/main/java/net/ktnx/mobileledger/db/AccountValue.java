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

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/*
create table account_values(profile varchar not null, account varchar not null, currency varchar
not null default '', value decimal not null, generation integer default 0 );
create unique index un_account_values on account_values(profile,account,currency);
 */
@Entity(tableName = "account_values", primaryKeys = {"profile", "account", "currency"})
public class AccountValue {
    @ColumnInfo
    @NonNull
    private String profile;
    @ColumnInfo
    @NonNull
    private String account;
    @NonNull
    @ColumnInfo(defaultValue = "")
    private String currency = "";
    @ColumnInfo
    private float value;
    @ColumnInfo(defaultValue = "0")
    private int generation = 0;
    @NonNull
    public String getProfile() {
        return profile;
    }
    public void setProfile(@NonNull String profile) {
        this.profile = profile;
    }
    @NonNull
    public String getAccount() {
        return account;
    }
    public void setAccount(@NonNull String account) {
        this.account = account;
    }
    @NonNull
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(@NonNull String currency) {
        this.currency = currency;
    }
    public float getValue() {
        return value;
    }
    public void setValue(float value) {
        this.value = value;
    }
    public int getGeneration() {
        return generation;
    }
    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
