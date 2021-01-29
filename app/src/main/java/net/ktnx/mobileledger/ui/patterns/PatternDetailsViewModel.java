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

package net.ktnx.mobileledger.ui.patterns;

import android.database.Cursor;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.dao.PatternAccountDAO;
import net.ktnx.mobileledger.dao.PatternHeaderDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.PatternAccount;
import net.ktnx.mobileledger.db.PatternHeader;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.PatternDetailsItem;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PatternDetailsViewModel extends ViewModel {
    static final int NEW_PATTERN = -1;
    private final MutableLiveData<List<PatternDetailsItem>> items = new MutableLiveData<>();
    private long mPatternId;
    private String mDefaultPatternName;
    public String getDefaultPatternName() {
        return mDefaultPatternName;
    }
    public void setDefaultPatternName(String name) {
        mDefaultPatternName = name;
    }
    public LiveData<List<PatternDetailsItem>> getItems() {
        return items;
    }

    public void resetItems() {
        items.setValue(Collections.emptyList());
        checkItemConsistency();
    }
    private void checkItemConsistency() {
        ArrayList<PatternDetailsItem> newList = new ArrayList<>(items.getValue());
        boolean changes = false;
        if (newList.size() < 1) {
            final PatternDetailsItem.Header header = PatternDetailsItem.createHeader();
            header.setName(mDefaultPatternName);
            newList.add(header);
            changes = true;
        }

        while (newList.size() < 3) {
            newList.add(PatternDetailsItem.createAccountRow(newList.size() - 1));
            changes = true;
        }

        if (changes)
            items.setValue(newList);
    }
    public void loadItems(long patternId) {
        DB db = App.getRoomDB();
        LiveData<PatternHeader> ph = db.getPatternDAO()
                                       .getPattern(patternId);
        ArrayList<PatternDetailsItem> list = new ArrayList<>();

        MLDB.queryInBackground(
                "SELECT name, regular_expression, transaction_description, transaction_comment, " +
                "date_year_match_group, date_month_match_group, date_day_match_group FROM " +
                "patterns WHERE id=?", new String[]{String.valueOf(patternId)},
                new MLDB.CallbackHelper() {
                    @Override
                    public void onDone() {
                        super.onDone();

                        MLDB.queryInBackground(
                                "SELECT id, position, acc, acc_match_group, currency, " +
                                "currency_match_group, amount, amount_match_group," +
                                " comment, comment_match_group FROM " +
                                "pattern_accounts WHERE pattern_id=? ORDER BY " + "position ASC",
                                new String[]{String.valueOf(patternId)}, new MLDB.CallbackHelper() {
                                    @Override
                                    public void onDone() {
                                        super.onDone();
                                        items.postValue(list);
                                    }
                                    @Override
                                    public boolean onRow(@NonNull Cursor cursor) {
                                        PatternDetailsItem.AccountRow item =
                                                PatternDetailsItem.createAccountRow(
                                                        cursor.getInt(1));
                                        list.add(item);

                                        item.setId(cursor.getInt(0));

                                        if (cursor.isNull(3)) {
                                            item.setAccountName(cursor.getString(2));
                                        }
                                        else {
                                            item.setAccountNameMatchGroup(cursor.getShort(3));
                                        }

                                        if (cursor.isNull(5)) {
                                            final int currId = cursor.getInt(4);
                                            if (currId > 0)
                                                item.setCurrency(Currency.loadById(currId));
                                        }
                                        else {
                                            item.setCurrencyMatchGroup(cursor.getShort(5));
                                        }

                                        if (cursor.isNull(7)) {
                                            item.setAmount(cursor.getFloat(6));
                                        }
                                        else {
                                            item.setAmountMatchGroup(cursor.getShort(7));
                                        }

                                        if (cursor.isNull(9)) {
                                            item.setAccountComment(cursor.getString(8));
                                        }
                                        else {
                                            item.setAccountCommentMatchGroup(cursor.getShort(9));
                                        }

                                        return true;
                                    }
                                });
                    }
                    @Override
                    public boolean onRow(@NonNull Cursor cursor) {
                        PatternDetailsItem.Header header = PatternDetailsItem.createHeader();
                        header.setName(cursor.getString(0));
                        header.setPattern(cursor.getString(1));
                        header.setTransactionDescription(cursor.getString(2));
                        header.setTransactionComment(cursor.getString(3));
                        header.setDateYearMatchGroup(cursor.getShort(4));
                        header.setDateMonthMatchGroup(cursor.getShort(5));
                        header.setDateDayMatchGroup(cursor.getShort(6));

                        list.add(header);

                        return false;
                    }
                });
    }
    public void setTestText(String text) {
        List<PatternDetailsItem> list = new ArrayList<>(items.getValue());
        PatternDetailsItem.Header header = new PatternDetailsItem.Header(list.get(0)
                                                                             .asHeaderItem());
        header.setTestText(text);
        list.set(0, header);

        items.setValue(list);
    }
    public void setPatternId(int patternId) {
        if (mPatternId != patternId) {
            if (patternId == NEW_PATTERN) {
                resetItems();
            }
            else {
                loadItems(patternId);
            }
            mPatternId = patternId;
        }

    }
    public void onSavePattern() {
        Logger.debug("flow", "PatternDetailsViewModel.onSavePattern(); model=" + this);
        final List<PatternDetailsItem> list = Objects.requireNonNull(items.getValue());

        AsyncTask.execute(() -> {
            PatternDetailsItem.Header modelHeader = list.get(0)
                                                        .asHeaderItem();
            PatternHeaderDAO headerDAO = App.getRoomDB()
                                            .getPatternDAO();
            PatternHeader dbHeader = modelHeader.toDBO();
            if (mPatternId <= 0) {
                dbHeader.setId(mPatternId = headerDAO.insert(dbHeader));
            }
            else
                headerDAO.update(dbHeader);


            PatternAccountDAO paDAO = App.getRoomDB()
                                         .getPatternAccountDAO();
            for (int i = 1; i < list.size(); i++) {
                PatternAccount dbAccount = list.get(i)
                                               .asAccountRowItem()
                                               .toDBO(dbHeader.getId());
                dbAccount.setPatternId(mPatternId);
                if (dbAccount.getId() == null || dbAccount.getId() <= 0)
                    dbAccount.setId(paDAO.insert(dbAccount));
                else
                    paDAO.update(dbAccount);
            }
        });
    }
}