package net.ktnx.mobileledger.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "patterns",
        indices = {@Index(name = "un_patterns_id", value = "id", unique = true)})
public class PatternHeader extends PatternBase {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private Long id;
    @ColumnInfo(name = "name")
    @NonNull
    private String name;
    @NonNull
    @ColumnInfo(name = "regular_expression")
    private String regularExpression;
    @ColumnInfo(name = "test_text")
    private String testText;
    @ColumnInfo(name = "transaction_description")
    private String transactionDescription;
    @ColumnInfo(name = "transaction_description_match_group")
    private Integer transactionDescriptionMatchGroup;
    @ColumnInfo(name = "transaction_comment")
    private String transactionComment;
    @ColumnInfo(name = "transaction_comment_match_group")
    private Integer transactionCommentMatchGroup;
    @ColumnInfo(name = "date_year")
    private Integer dateYear;
    @ColumnInfo(name = "date_year_match_group")
    private Integer dateYearMatchGroup;
    @ColumnInfo(name = "date_month")
    private Integer dateMonth;
    @ColumnInfo(name = "date_month_match_group")
    private Integer dateMonthMatchGroup;
    @ColumnInfo(name = "date_day")
    private Integer dateDay;
    @ColumnInfo(name = "date_day_match_group")
    private Integer dateDayMatchGroup;
    public PatternHeader(@NotNull Long id, @NonNull String name,
                         @NonNull String regularExpression) {
        this.id = id;
        this.name = name;
        this.regularExpression = regularExpression;
    }
    public String getTestText() {
        return testText;
    }
    public void setTestText(String testText) {
        this.testText = testText;
    }
    public Integer getTransactionDescriptionMatchGroup() {
        return transactionDescriptionMatchGroup;
    }
    public void setTransactionDescriptionMatchGroup(Integer transactionDescriptionMatchGroup) {
        this.transactionDescriptionMatchGroup = transactionDescriptionMatchGroup;
    }
    public Integer getTransactionCommentMatchGroup() {
        return transactionCommentMatchGroup;
    }
    public void setTransactionCommentMatchGroup(Integer transactionCommentMatchGroup) {
        this.transactionCommentMatchGroup = transactionCommentMatchGroup;
    }
    public Integer getDateYear() {
        return dateYear;
    }
    public void setDateYear(Integer dateYear) {
        this.dateYear = dateYear;
    }
    public Integer getDateMonth() {
        return dateMonth;
    }
    public void setDateMonth(Integer dateMonth) {
        this.dateMonth = dateMonth;
    }
    public Integer getDateDay() {
        return dateDay;
    }
    public void setDateDay(Integer dateDay) {
        this.dateDay = dateDay;
    }
    @NonNull
    public Long getId() {
        return id;
    }
    public void setId(@NonNull Long id) {
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
    public String getRegularExpression() {
        return regularExpression;
    }
    public void setRegularExpression(@NonNull String regularExpression) {
        this.regularExpression = regularExpression;
    }
    public String getTransactionDescription() {
        return transactionDescription;
    }
    public void setTransactionDescription(String transactionDescription) {
        this.transactionDescription = transactionDescription;
    }
    public String getTransactionComment() {
        return transactionComment;
    }
    public void setTransactionComment(String transactionComment) {
        this.transactionComment = transactionComment;
    }
    public Integer getDateYearMatchGroup() {
        return dateYearMatchGroup;
    }
    public void setDateYearMatchGroup(Integer dateYearMatchGroup) {
        this.dateYearMatchGroup = dateYearMatchGroup;
    }
    public Integer getDateMonthMatchGroup() {
        return dateMonthMatchGroup;
    }
    public void setDateMonthMatchGroup(Integer dateMonthMatchGroup) {
        this.dateMonthMatchGroup = dateMonthMatchGroup;
    }
    public Integer getDateDayMatchGroup() {
        return dateDayMatchGroup;
    }
    public void setDateDayMatchGroup(Integer dateDayMatchGroup) {
        this.dateDayMatchGroup = dateDayMatchGroup;
    }
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof PatternHeader))
            return false;

        PatternHeader o = (PatternHeader) obj;

        return Misc.equalLongs(id, o.id) && Misc.equalStrings(name, o.name) &&
               Misc.equalStrings(regularExpression, o.regularExpression) &&
               Misc.equalStrings(transactionDescription, o.transactionDescription) &&
               Misc.equalStrings(transactionComment, o.transactionComment) &&
               Misc.equalIntegers(transactionDescriptionMatchGroup,
                       o.transactionDescriptionMatchGroup) &&
               Misc.equalIntegers(transactionCommentMatchGroup, o.transactionCommentMatchGroup) &&
               Misc.equalIntegers(dateDay, o.dateDay) &&
               Misc.equalIntegers(dateDayMatchGroup, o.dateDayMatchGroup) &&
               Misc.equalIntegers(dateMonth, o.dateMonth) &&
               Misc.equalIntegers(dateMonthMatchGroup, o.dateMonthMatchGroup) &&
               Misc.equalIntegers(dateYear, o.dateYear) &&
               Misc.equalIntegers(dateYearMatchGroup, o.dateYearMatchGroup);
    }
}
