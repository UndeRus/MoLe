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
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import org.jetbrains.annotations.NotNull;

/*
create table transactions(profile varchar not null, id integer not null, data_hash varchar not
null, year integer not null, month integer not null, day integer not null, description varchar
collate NOCASE not null, comment varchar, generation integer default 0, primary key(profile,id));
create unique index un_transactions_data_hash on transactions(profile,data_hash);
create index idx_transaction_description on transactions(description);
 */
@Entity(tableName = "transactions", foreignKeys = {
        @ForeignKey(entity = Profile.class, parentColumns = "id", childColumns = "profile_id",
                    onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT)
}, indices = {@Index(name = "un_transactions_ledger_id", unique = true,
                     value = {"profile_id", "ledger_id"}),
              @Index(name = "idx_transaction_description", value = "description"),
              @Index(name = "fk_transaction_profile", value = "profile_id")
})
public class Transaction {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    long id;
    @ColumnInfo(name = "ledger_id")
    long ledgerId;
    @ColumnInfo(name = "profile_id")
    private long profileId;
    @ColumnInfo(name = "data_hash")
    @NonNull
    private String dataHash;
    @ColumnInfo
    private int year;
    @ColumnInfo
    private int month;
    @ColumnInfo
    private int day;
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    @NonNull
    private String description;
    @ColumnInfo
    private String comment;
    @ColumnInfo
    private long generation = 0;
    public long getLedgerId() {
        return ledgerId;
    }
    public void setLedgerId(long ledgerId) {
        this.ledgerId = ledgerId;
    }
    public long getProfileId() {
        return profileId;
    }
    public void setProfileId(long profileId) {
        this.profileId = profileId;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getDataHash() {
        return dataHash;
    }
    public void setDataHash(@NotNull String dataHash) {
        this.dataHash = dataHash;
    }
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public int getMonth() {
        return month;
    }
    public void setMonth(int month) {
        this.month = month;
    }
    public int getDay() {
        return day;
    }
    public void setDay(int day) {
        this.day = day;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public long getGeneration() {
        return generation;
    }
    public void setGeneration(long generation) {
        this.generation = generation;
    }

}
