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

import net.ktnx.mobileledger.utils.Misc;

@Entity(tableName = "transaction_accounts", foreignKeys = {
        @ForeignKey(entity = Transaction.class, parentColumns = {"id"},
                    childColumns = {"transaction_id"}, onDelete = ForeignKey.CASCADE,
                    onUpdate = ForeignKey.RESTRICT)
}, indices = {@Index(name = "fk_trans_acc_trans", value = {"transaction_id"}),
              @Index(name = "un_transaction_accounts", unique = true,
                     value = {"transaction_id", "order_no"})
})
public class TransactionAccount {
    @ColumnInfo
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "transaction_id")
    private long transactionId;
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
    private long generation = 0;
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    @NonNull
    public long getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(long transactionId) {
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
    public long getGeneration() {
        return generation;
    }
    public void setGeneration(long generation) {
        this.generation = generation;
    }

    public void copyDataFrom(TransactionAccount o) {
        // id = o.id
        transactionId = o.transactionId;
        orderNo = o.orderNo;
        accountName = o.accountName;
        currency = Misc.nullIsEmpty(o.currency);
        amount = o.amount;
        comment = o.comment;
        generation = o.generation;
    }
}
