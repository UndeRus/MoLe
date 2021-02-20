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

@Entity(tableName = "transaction_accounts", primaryKeys = {"profile", "transaction_id", "order_no"},
        foreignKeys = {@ForeignKey(entity = Transaction.class, parentColumns = {"profile", "id"},
                                   childColumns = {"profile", "transaction_id"},
                                   onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT),
                       @ForeignKey(entity = Account.class, parentColumns = {"profile", "name"},
                                   childColumns = {"profile", "account_name"},
                                   onDelete = ForeignKey.CASCADE, onUpdate = ForeignKey.RESTRICT)
        }, indices = {@Index(name = "fk_tran_acc_prof_acc", value = {"profile", "account_name"})})
public class TransactionAccount {
    @ColumnInfo
    @NonNull
    private String profile;
    @ColumnInfo(name = "transaction_id")
    private int transactionId;
    @ColumnInfo(name = "order_no")
    private int orderNo;
    @ColumnInfo(name = "account_name")
    @NonNull
    private String accountName;
    @ColumnInfo(defaultValue = "")
    @NonNull
    private String currency = "";
    @ColumnInfo
    private float amount;
    @ColumnInfo
    private String comment;
    @ColumnInfo(defaultValue = "0")
    private int generation = 0;
    @NonNull
    public String getProfile() {
        return profile;
    }
    public void setProfile(@NonNull String profile) {
        this.profile = profile;
    }
    public int getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
    public int getOrderNo() {
        return orderNo;
    }
    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }
    @NonNull
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(@NonNull String accountName) {
        this.accountName = accountName;
    }
    @NonNull
    public String getCurrency() {
        return currency;
    }
    public void setCurrency(@NonNull String currency) {
        this.currency = currency;
    }
    public float getAmount() {
        return amount;
    }
    public void setAmount(float amount) {
        this.amount = amount;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public int getGeneration() {
        return generation;
    }
    public void setGeneration(int generation) {
        this.generation = generation;
    }
}
