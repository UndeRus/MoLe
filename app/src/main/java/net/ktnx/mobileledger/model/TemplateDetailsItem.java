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

package net.ktnx.mobileledger.model;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateBase;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

abstract public class TemplateDetailsItem {
    private final Type type;
    protected Long id;
    protected long position;

    protected TemplateDetailsItem(Type type) {
        this.type = type;
    }
    @Contract(" -> new")
    public static @NotNull TemplateDetailsItem.Header createHeader() {
        return new Header();
    }
    public static @NotNull TemplateDetailsItem.Header createHeader(Header origin) {
        return new Header(origin);
    }
    @Contract("-> new")
    public static @NotNull TemplateDetailsItem.AccountRow createAccountRow() {
        return new AccountRow();
    }
    public static TemplateDetailsItem fromRoomObject(TemplateBase p) {
        if (p instanceof TemplateHeader) {
            TemplateHeader ph = (TemplateHeader) p;
            Header header = createHeader();
            header.setId(ph.getId());
            header.setName(ph.getName());
            header.setPattern(ph.getRegularExpression());
            header.setTestText(ph.getTestText());

            if (ph.getTransactionDescriptionMatchGroup() == null)
                header.setTransactionDescription(ph.getTransactionDescription());
            else
                header.setTransactionDescriptionMatchGroup(
                        ph.getTransactionDescriptionMatchGroup());

            if (ph.getTransactionCommentMatchGroup() == null)
                header.setTransactionComment(ph.getTransactionComment());
            else
                header.setTransactionCommentMatchGroup(ph.getTransactionCommentMatchGroup());

            if (ph.getDateDayMatchGroup() == null)
                header.setDateDay(ph.getDateDay());
            else
                header.setDateDayMatchGroup(ph.getDateDayMatchGroup());

            if (ph.getDateMonthMatchGroup() == null)
                header.setDateMonth(ph.getDateMonth());
            else
                header.setDateMonthMatchGroup(ph.getDateMonthMatchGroup());

            if (ph.getDateYearMatchGroup() == null)
                header.setDateYear(ph.getDateYear());
            else
                header.setDateYearMatchGroup(ph.getDateYearMatchGroup());

            header.setFallback(ph.isFallback());

            return header;
        }
        else if (p instanceof TemplateAccount) {
            TemplateAccount pa = (TemplateAccount) p;
            AccountRow acc = createAccountRow();
            acc.setId(pa.getId());
            acc.setPosition(pa.getPosition());

            if (pa.getAccountNameMatchGroup() == null)
                acc.setAccountName(Misc.nullIsEmpty(pa.getAccountName()));
            else
                acc.setAccountNameMatchGroup(pa.getAccountNameMatchGroup());

            if (pa.getAccountCommentMatchGroup() == null)
                acc.setAccountComment(Misc.nullIsEmpty(pa.getAccountComment()));
            else
                acc.setAccountCommentMatchGroup(pa.getAccountCommentMatchGroup());

            if (pa.getCurrencyMatchGroup() == null) {
                final Integer currencyId = pa.getCurrency();
                if (currencyId != null && currencyId > 0)
                    acc.setCurrency(Currency.loadById(currencyId));
            }
            else
                acc.setCurrencyMatchGroup(pa.getCurrencyMatchGroup());

            final Integer amountMatchGroup = pa.getAmountMatchGroup();
            if (amountMatchGroup != null && amountMatchGroup > 0) {
                acc.setAmountMatchGroup(amountMatchGroup);
                final Boolean negateAmount = pa.getNegateAmount();
                acc.setNegateAmount(negateAmount != null && negateAmount);
            }
            else
                acc.setAmount(pa.getAmount());

            return acc;
        }
        else {
            throw new IllegalStateException("Unexpected item class " + p.getClass());
        }
    }
    public Header asHeaderItem() {
        ensureType(Type.HEADER);
        return (Header) this;
    }
    public AccountRow asAccountRowItem() {
        ensureType(Type.ACCOUNT_ITEM);
        return (AccountRow) this;
    }
    private void ensureType(Type type) {
        if (this.type != type)
            throw new IllegalStateException(
                    String.format("Type is %s, but %s is required", this.type.toString(),
                            type.toString()));
    }
    void ensureTrue(boolean flag) {
        if (!flag)
            throw new IllegalStateException(
                    "Literal value requested, but it is matched via a pattern group");
    }
    void ensureFalse(boolean flag) {
        if (flag)
            throw new IllegalStateException("Matching group requested, but the value is a literal");
    }
    public long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public void setId(int id) {
        this.id = (long) id;
    }
    public long getPosition() {
        return position;
    }
    public void setPosition(long position) {
        this.position = position;
    }
    abstract public String getProblem(@NonNull Resources r, int patternGroupCount);
    public Type getType() {
        return type;
    }
    public enum Type {
        HEADER(TYPE.header), ACCOUNT_ITEM(TYPE.accountItem);
        final int index;
        Type(int i) {
            index = i;
        }
        public int toInt() {
            return index;
        }
    }

    static class PossiblyMatchedValue<T> {
        private boolean literalValue;
        private T value;
        private int matchGroup;
        public PossiblyMatchedValue() {
            literalValue = true;
            value = null;
        }
        public PossiblyMatchedValue(@NonNull PossiblyMatchedValue<T> origin) {
            literalValue = origin.literalValue;
            value = origin.value;
            matchGroup = origin.matchGroup;
        }
        @NonNull
        public static PossiblyMatchedValue<Integer> withLiteralInt(Integer initialValue) {
            PossiblyMatchedValue<Integer> result = new PossiblyMatchedValue<>();
            result.setValue(initialValue);
            return result;
        }
        @NonNull
        public static PossiblyMatchedValue<Float> withLiteralFloat(Float initialValue) {
            PossiblyMatchedValue<Float> result = new PossiblyMatchedValue<>();
            result.setValue(initialValue);
            return result;
        }
        public static PossiblyMatchedValue<Short> withLiteralShort(Short initialValue) {
            PossiblyMatchedValue<Short> result = new PossiblyMatchedValue<>();
            result.setValue(initialValue);
            return result;
        }
        @NonNull
        public static PossiblyMatchedValue<String> withLiteralString(String initialValue) {
            PossiblyMatchedValue<String> result = new PossiblyMatchedValue<>();
            result.setValue(initialValue);
            return result;
        }
        public void copyFrom(@NonNull PossiblyMatchedValue<T> origin) {
            literalValue = origin.literalValue;
            value = origin.value;
            matchGroup = origin.matchGroup;
        }
        public T getValue() {
            if (!literalValue)
                throw new IllegalStateException("Value is not literal");
            return value;
        }
        public void setValue(T newValue) {
            value = newValue;
            literalValue = true;
        }
        public boolean hasLiteralValue() {
            return literalValue;
        }
        public int getMatchGroup() {
            if (literalValue)
                throw new IllegalStateException("Value is literal");
            return matchGroup;
        }
        public void setMatchGroup(int group) {
            this.matchGroup = group;
            literalValue = false;
        }
        public boolean equals(PossiblyMatchedValue<T> other) {
            if (!other.literalValue == literalValue)
                return false;
            if (literalValue) {
                if (value == null)
                    return other.value == null;
                return value.equals(other.value);
            }
            else
                return matchGroup == other.matchGroup;
        }
        public void switchToLiteral() {
            literalValue = true;
        }
        public String toString() {
            if (literalValue)
                if (value == null)
                    return "<null>";
                else
                    return value.toString();
            if (matchGroup > 0)
                return "grp:" + matchGroup;
            return "<null>";
        }
        public boolean isEmpty() {
            if (literalValue)
                return value == null || Misc.emptyIsNull(value.toString()) == null;

            return matchGroup > 0;
        }
    }

    public static class TYPE {
        public static final int header = 0;
        public static final int accountItem = 1;
    }

    public static class AccountRow extends TemplateDetailsItem {
        private final PossiblyMatchedValue<String> accountName =
                PossiblyMatchedValue.withLiteralString("");
        private final PossiblyMatchedValue<String> accountComment =
                PossiblyMatchedValue.withLiteralString("");
        private final PossiblyMatchedValue<Float> amount =
                PossiblyMatchedValue.withLiteralFloat(null);
        private final PossiblyMatchedValue<Currency> currency = new PossiblyMatchedValue<>();
        private boolean negateAmount;
        public AccountRow() {
            super(Type.ACCOUNT_ITEM);
        }
        public AccountRow(AccountRow origin) {
            super(Type.ACCOUNT_ITEM);
            id = origin.id;
            position = origin.position;
            accountName.copyFrom(origin.accountName);
            accountComment.copyFrom(origin.accountComment);
            amount.copyFrom(origin.amount);
            currency.copyFrom(origin.currency);
            negateAmount = origin.negateAmount;
        }
        public boolean isNegateAmount() {
            return negateAmount;
        }
        public void setNegateAmount(boolean negateAmount) {
            this.negateAmount = negateAmount;
        }
        public int getAccountCommentMatchGroup() {
            return accountComment.getMatchGroup();
        }
        public void setAccountCommentMatchGroup(int group) {
            accountComment.setMatchGroup(group);
        }
        public String getAccountComment() {
            return accountComment.getValue();
        }
        public void setAccountComment(String comment) {
            this.accountComment.setValue(comment);
        }
        public int getCurrencyMatchGroup() {
            return currency.getMatchGroup();
        }
        public void setCurrencyMatchGroup(int group) {
            currency.setMatchGroup(group);
        }
        public Currency getCurrency() {
            return currency.getValue();
        }
        public void setCurrency(Currency currency) {
            this.currency.setValue(currency);
        }
        public int getAccountNameMatchGroup() {
            return accountName.getMatchGroup();
        }
        public void setAccountNameMatchGroup(int group) {
            accountName.setMatchGroup(group);
        }
        public String getAccountName() {
            return accountName.getValue();
        }
        public void setAccountName(String accountName) {
            this.accountName.setValue(accountName);
        }
        public boolean hasLiteralAccountName() { return accountName.hasLiteralValue(); }
        public boolean hasLiteralAmount() {
            return amount.hasLiteralValue();
        }
        public int getAmountMatchGroup() {
            return amount.getMatchGroup();
        }
        public void setAmountMatchGroup(int group) {
            amount.setMatchGroup(group);
        }
        public Float getAmount() {
            return amount.getValue();
        }
        public void setAmount(Float amount) {
            this.amount.setValue(amount);
        }
        public String getProblem(@NonNull Resources r, int patternGroupCount) {
            if (Misc.emptyIsNull(accountName.getValue()) == null)
                return r.getString(R.string.account_name_is_empty);
            if (!amount.hasLiteralValue() &&
                (amount.getMatchGroup() < 1 || amount.getMatchGroup() > patternGroupCount))
                return r.getString(R.string.invalid_matching_group_number);

            return null;
        }
        public boolean hasLiteralAccountComment() {
            return accountComment.hasLiteralValue();
        }
        public boolean equalContents(AccountRow o) {
            if (position != o.position) {
                Logger.debug("cmpAcc",
                        String.format(Locale.US, "[%d] != [%d]: pos %d != pos %d", getId(),
                                o.getId(), position, o.position));
                return false;
            }
            return amount.equals(o.amount) && accountName.equals(o.accountName) &&
                   position == o.position && accountComment.equals(o.accountComment) &&
                   negateAmount == o.negateAmount;
        }
        public void switchToLiteralAmount() {
            amount.switchToLiteral();
        }
        public void switchToLiteralAccountName() {
            accountName.switchToLiteral();
        }
        public void switchToLiteralAccountComment() {
            accountComment.switchToLiteral();
        }
        public TemplateAccount toDBO(@NonNull Long patternId) {
            TemplateAccount result = new TemplateAccount(id, patternId, position);

            if (accountName.hasLiteralValue())
                result.setAccountName(accountName.getValue());
            else
                result.setAccountNameMatchGroup(accountName.getMatchGroup());

            if (accountComment.hasLiteralValue())
                result.setAccountComment(accountComment.getValue());
            else
                result.setAccountCommentMatchGroup(accountComment.getMatchGroup());

            if (amount.hasLiteralValue()) {
                result.setAmount(amount.getValue());
                result.setNegateAmount(null);
            }
            else {
                result.setAmountMatchGroup(amount.getMatchGroup());
                result.setNegateAmount(negateAmount ? true : null);
            }

            return result;
        }
        public boolean isEmpty() {
            return accountName.isEmpty() && accountComment.isEmpty() && amount.isEmpty();
        }
    }

    public static class Header extends TemplateDetailsItem {
        private String pattern = "";
        private String testText = "";
        private String name = "";
        private Pattern compiledPattern;
        private String patternError;
        private PossiblyMatchedValue<String> transactionDescription =
                PossiblyMatchedValue.withLiteralString("");
        private PossiblyMatchedValue<String> transactionComment =
                PossiblyMatchedValue.withLiteralString("");
        private PossiblyMatchedValue<Integer> dateYear = PossiblyMatchedValue.withLiteralInt(null);
        private PossiblyMatchedValue<Integer> dateMonth = PossiblyMatchedValue.withLiteralInt(null);
        private PossiblyMatchedValue<Integer> dateDay = PossiblyMatchedValue.withLiteralInt(null);
        private SpannableString testMatch;
        private boolean isFallback;
        private Header() {
            super(Type.HEADER);
        }
        public Header(Header origin) {
            this();
            id = origin.id;
            name = origin.name;
            testText = origin.testText;
            testMatch = origin.testMatch;
            setPattern(origin.pattern);

            transactionDescription = new PossiblyMatchedValue<>(origin.transactionDescription);
            transactionComment = new PossiblyMatchedValue<>(origin.transactionComment);

            dateYear = new PossiblyMatchedValue<>(origin.dateYear);
            dateMonth = new PossiblyMatchedValue<>(origin.dateMonth);
            dateDay = new PossiblyMatchedValue<>(origin.dateDay);

            isFallback = origin.isFallback;
        }
        private static StyleSpan capturedSpan() { return new StyleSpan(Typeface.BOLD); }
        private static UnderlineSpan matchedSpan() { return new UnderlineSpan(); }
        private static ForegroundColorSpan notMatchedSpan() {
            return new ForegroundColorSpan(Color.GRAY);
        }
        public boolean isFallback() {
            return isFallback;
        }
        public void setFallback(boolean fallback) {
            this.isFallback = fallback;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getPattern() {
            return pattern;
        }
        public void setPattern(String pattern) {
            this.pattern = pattern;
            try {
                this.compiledPattern = Pattern.compile(pattern);
                checkPatternMatch();
            }
            catch (PatternSyntaxException ex) {
                patternError = ex.getDescription();
                compiledPattern = null;

                testMatch = new SpannableString(testText);
                testMatch.setSpan(notMatchedSpan(), 0, testText.length() - 1,
                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
        }
        @NonNull
        @Override
        public String toString() {
            return super.toString() +
                   String.format(" name[%s] pat[%s] test[%s] tran[%s] com[%s]", name, pattern,
                           testText, transactionDescription, transactionComment);
        }
        public String getTestText() {
            return testText;
        }
        public void setTestText(String testText) {
            this.testText = testText;

            checkPatternMatch();
        }
        public String getTransactionDescription() {
            return transactionDescription.getValue();
        }
        public void setTransactionDescription(String transactionDescription) {
            this.transactionDescription.setValue(transactionDescription);
        }
        public String getTransactionComment() {
            return transactionComment.getValue();
        }
        public void setTransactionComment(String transactionComment) {
            this.transactionComment.setValue(transactionComment);
        }
        public Integer getDateYear() {
            return dateYear.getValue();
        }
        public void setDateYear(Integer dateYear) {
            this.dateYear.setValue(dateYear);
        }
        public Integer getDateMonth() {
            return dateMonth.getValue();
        }
        public void setDateMonth(Integer dateMonth) {
            this.dateMonth.setValue(dateMonth);
        }
        public Integer getDateDay() {
            return dateDay.getValue();
        }
        public void setDateDay(Integer dateDay) {
            this.dateDay.setValue(dateDay);
        }
        public int getDateYearMatchGroup() {
            return dateYear.getMatchGroup();
        }
        public void setDateYearMatchGroup(int dateYearMatchGroup) {
            this.dateYear.setMatchGroup(dateYearMatchGroup);
        }
        public int getDateMonthMatchGroup() {
            return dateMonth.getMatchGroup();
        }
        public void setDateMonthMatchGroup(int dateMonthMatchGroup) {
            this.dateMonth.setMatchGroup(dateMonthMatchGroup);
        }
        public int getDateDayMatchGroup() {
            return dateDay.getMatchGroup();
        }
        public void setDateDayMatchGroup(int dateDayMatchGroup) {
            this.dateDay.setMatchGroup(dateDayMatchGroup);
        }
        public boolean hasLiteralDateYear() {
            return dateYear.hasLiteralValue();
        }
        public boolean hasLiteralDateMonth() {
            return dateMonth.hasLiteralValue();
        }
        public boolean hasLiteralDateDay() {
            return dateDay.hasLiteralValue();
        }
        public boolean hasLiteralTransactionDescription() { return transactionDescription.hasLiteralValue(); }
        public boolean hasLiteralTransactionComment() { return transactionComment.hasLiteralValue(); }
        public String getProblem(@NonNull Resources r, int patternGroupCount) {
            if (patternError != null)
                return r.getString(R.string.pattern_has_errors) + ": " + patternError;
            if (Misc.emptyIsNull(pattern) == null)
                return r.getString(R.string.pattern_is_empty);

            if (!dateYear.hasLiteralValue() && compiledPattern != null &&
                (dateDay.getMatchGroup() < 1 || dateDay.getMatchGroup() > patternGroupCount))
                return r.getString(R.string.invalid_matching_group_number);

            if (!dateMonth.hasLiteralValue() && compiledPattern != null &&
                (dateMonth.getMatchGroup() < 1 || dateMonth.getMatchGroup() > patternGroupCount))
                return r.getString(R.string.invalid_matching_group_number);

            if (!dateDay.hasLiteralValue() && compiledPattern != null &&
                (dateDay.getMatchGroup() < 1 || dateDay.getMatchGroup() > patternGroupCount))
                return r.getString(R.string.invalid_matching_group_number);

            return null;
        }

        public boolean equalContents(Header o) {
            if (!dateDay.equals(o.dateDay))
                return false;
            if (!dateMonth.equals(o.dateMonth))
                return false;
            if (!dateYear.equals(o.dateYear))
                return false;
            if (!transactionDescription.equals(o.transactionDescription))
                return false;
            if (!transactionComment.equals(o.transactionComment))
                return true;

            return Misc.equalStrings(name, o.name) && Misc.equalStrings(pattern, o.pattern) &&
                   Misc.equalStrings(testText, o.testText) &&
                   Misc.equalStrings(patternError, o.patternError) &&
                   Objects.equals(testMatch, o.testMatch) && isFallback == o.isFallback;
        }
        public String getMatchGroupText(int group) {
            if (compiledPattern != null && testText != null) {
                Matcher m = compiledPattern.matcher(testText);
                if (m.matches())
                    return m.group(group);
            }

            return "ø";
        }
        public Pattern getCompiledPattern() {
            return compiledPattern;
        }
        public void switchToLiteralTransactionDescription() {
            transactionDescription.switchToLiteral();
        }
        public void switchToLiteralTransactionComment() {
            transactionComment.switchToLiteral();
        }
        public int getTransactionDescriptionMatchGroup() {
            return transactionDescription.getMatchGroup();
        }
        public void setTransactionDescriptionMatchGroup(int group) {
            transactionDescription.setMatchGroup(group);
        }
        public int getTransactionCommentMatchGroup() {
            return transactionComment.getMatchGroup();
        }
        public void setTransactionCommentMatchGroup(int group) {
            transactionComment.setMatchGroup(group);
        }
        public void switchToLiteralDateYear() {
            dateYear.switchToLiteral();
        }
        public void switchToLiteralDateMonth() {
            dateMonth.switchToLiteral();
        }
        public void switchToLiteralDateDay() { dateDay.switchToLiteral(); }
        public TemplateHeader toDBO() {
            TemplateHeader result = new TemplateHeader(id, name, pattern);

            if (Misc.emptyIsNull(testText) != null)
                result.setTestText(testText);

            if (transactionDescription.hasLiteralValue())
                result.setTransactionDescription(transactionDescription.getValue());
            else
                result.setTransactionDescriptionMatchGroup(transactionDescription.getMatchGroup());

            if (transactionComment.hasLiteralValue())
                result.setTransactionComment(transactionComment.getValue());
            else
                result.setTransactionCommentMatchGroup(transactionComment.getMatchGroup());

            if (dateYear.hasLiteralValue())
                result.setDateYear(dateYear.getValue());
            else
                result.setDateYearMatchGroup(dateYear.getMatchGroup());

            if (dateMonth.hasLiteralValue())
                result.setDateMonth(dateMonth.getValue());
            else
                result.setDateMonthMatchGroup(dateMonth.getMatchGroup());

            if (dateDay.hasLiteralValue())
                result.setDateDay(dateDay.getValue());
            else
                result.setDateDayMatchGroup(dateDay.getMatchGroup());

            result.setFallback(isFallback);

            return result;
        }
        public SpannableString getTestMatch() {
            return testMatch;
        }
        public void checkPatternMatch() {
            patternError = null;
            testMatch = null;

            if (pattern != null) {
                try {
                    if (Misc.emptyIsNull(testText) != null) {
                        SpannableString ss = new SpannableString(testText);
                        Matcher m = compiledPattern.matcher(testText);
                        if (m.find()) {
                            if (m.start() > 0)
                                ss.setSpan(notMatchedSpan(), 0, m.start(),
                                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            if (m.end() < testText.length() - 1)
                                ss.setSpan(notMatchedSpan(), m.end(), testText.length(),
                                        Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                            ss.setSpan(matchedSpan(), m.start(0), m.end(0),
                                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                            if (m.groupCount() > 0) {
                                for (int g = 1; g <= m.groupCount(); g++) {
                                    ss.setSpan(capturedSpan(), m.start(g), m.end(g),
                                            Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                }
                            }
                        }
                        else {
                            patternError = "Pattern does not match";
                            ss.setSpan(new ForegroundColorSpan(Color.GRAY), 0,
                                    testText.length() - 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                        }

                        testMatch = ss;
                    }
                }
                catch (PatternSyntaxException e) {
                    this.compiledPattern = null;
                    this.patternError = e.getMessage();
                }
            }
            else {
                patternError = "Missing pattern";
            }
        }
        public String getPatternError() {
            return patternError;
        }
        public SpannableString testMatch() {
            return testMatch;
        }
    }
}
