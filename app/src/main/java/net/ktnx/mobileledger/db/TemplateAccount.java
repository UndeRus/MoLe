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

@Entity(tableName = "template_accounts",
        indices = {@Index(name = "fk_template_accounts_template", value = "template_id"),
                   @Index(name = "fk_template_accounts_currency", value = "currency")
        }, foreignKeys = {@ForeignKey(childColumns = "template_id", parentColumns = "id",
                                      entity = TemplateHeader.class, onDelete = ForeignKey.CASCADE,
                                      onUpdate = ForeignKey.RESTRICT),
                          @ForeignKey(childColumns = "currency", parentColumns = "id",
                                      entity = Currency.class, onDelete = ForeignKey.RESTRICT,
                                      onUpdate = ForeignKey.RESTRICT)
})
public class TemplateAccount extends TemplateBase {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "template_id")
    private long templateId;
    @ColumnInfo(name = "acc")
    private String accountName;
    @ColumnInfo(name = "position")
    @NonNull
    private Long position;
    @ColumnInfo(name = "acc_match_group")
    private Integer accountNameMatchGroup;
    @ColumnInfo
    private Long currency;
    @ColumnInfo(name = "currency_match_group")
    private Integer currencyMatchGroup;
    @ColumnInfo(name = "amount")
    private Float amount;
    @ColumnInfo(name = "amount_match_group")
    private Integer amountMatchGroup;
    @ColumnInfo(name = "comment")
    private String accountComment;
    @ColumnInfo(name = "comment_match_group")
    private Integer accountCommentMatchGroup;
    @ColumnInfo(name = "negate_amount")
    private Boolean negateAmount;
    public TemplateAccount(@NotNull Long id, @NonNull Long templateId, @NonNull Long position) {
        this.id = id;
        this.templateId = templateId;
        this.position = position;
    }
    public TemplateAccount(TemplateAccount o) {
        id = o.id;
        templateId = o.templateId;
        accountName = o.accountName;
        position = o.position;
        accountNameMatchGroup = o.accountNameMatchGroup;
        currency = o.currency;
        currencyMatchGroup = o.currencyMatchGroup;
        amount = o.amount;
        amountMatchGroup = o.amountMatchGroup;
        accountComment = o.accountComment;
        accountCommentMatchGroup = o.accountCommentMatchGroup;
        negateAmount = o.negateAmount;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public Boolean getNegateAmount() {
        return negateAmount;
    }
    public void setNegateAmount(Boolean negateAmount) {
        this.negateAmount = negateAmount;
    }
    public long getTemplateId() {
        return templateId;
    }
    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }
    @NonNull
    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(@NonNull String accountName) {
        this.accountName = accountName;
    }
    @NonNull
    public Long getPosition() {
        return position;
    }
    public void setPosition(@NonNull Long position) {
        this.position = position;
    }
    public void setPosition(int position) {
        this.position = (long) position;
    }
    public Integer getAccountNameMatchGroup() {
        return accountNameMatchGroup;
    }
    public void setAccountNameMatchGroup(Integer accountNameMatchGroup) {
        this.accountNameMatchGroup = accountNameMatchGroup;
    }
    public Long getCurrency() {
        return currency;
    }
    public void setCurrency(Long currency) {
        this.currency = currency;
    }
    public Currency getCurrencyObject() {
        if (currency == null || currency <= 0)
            return null;
        return DB.get()
                 .getCurrencyDAO()
                 .getByIdSync(currency);
    }
    public Integer getCurrencyMatchGroup() {
        return currencyMatchGroup;
    }
    public void setCurrencyMatchGroup(Integer currencyMatchGroup) {
        this.currencyMatchGroup = currencyMatchGroup;
    }
    public Float getAmount() {
        return amount;
    }
    public void setAmount(Float amount) {
        this.amount = amount;
    }
    public Integer getAmountMatchGroup() {
        return amountMatchGroup;
    }
    public void setAmountMatchGroup(Integer amountMatchGroup) {
        this.amountMatchGroup = amountMatchGroup;
    }
    public String getAccountComment() {
        return accountComment;
    }
    public void setAccountComment(String accountComment) {
        this.accountComment = accountComment;
    }
    public Integer getAccountCommentMatchGroup() {
        return accountCommentMatchGroup;
    }
    public void setAccountCommentMatchGroup(Integer accountCommentMatchGroup) {
        this.accountCommentMatchGroup = accountCommentMatchGroup;
    }
    public TemplateAccount createDuplicate(TemplateHeader header) {
        TemplateAccount dup = new TemplateAccount(this);
        dup.id = 0;
        dup.templateId = header.getId();

        return dup;
    }
}
