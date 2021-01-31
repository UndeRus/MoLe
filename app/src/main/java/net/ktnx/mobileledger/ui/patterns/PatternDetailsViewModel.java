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

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.dao.PatternAccountDAO;
import net.ktnx.mobileledger.dao.PatternHeaderDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.PatternAccount;
import net.ktnx.mobileledger.db.PatternHeader;
import net.ktnx.mobileledger.db.PatternWithAccounts;
import net.ktnx.mobileledger.model.PatternDetailsItem;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PatternDetailsViewModel extends ViewModel {
    private final MutableLiveData<List<PatternDetailsItem>> items =
            new MutableLiveData<>(Collections.emptyList());
    private Long mPatternId;
    private String mDefaultPatternName;
    public String getDefaultPatternName() {
        return mDefaultPatternName;
    }
    public void setDefaultPatternName(String name) {
        mDefaultPatternName = name;
    }

    public void resetItems() {
        ArrayList<PatternDetailsItem> newList = new ArrayList<>();
        final PatternDetailsItem.Header header = PatternDetailsItem.createHeader();
        header.setName(mDefaultPatternName);
        header.setId(0);
        newList.add(header);

        while (newList.size() < 3) {
            final PatternDetailsItem.AccountRow aRow = PatternDetailsItem.createAccountRow();
            aRow.setId(newList.size() + 1);
            newList.add(aRow);
        }

        items.setValue(newList);
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
            newList.add(PatternDetailsItem.createAccountRow());
            changes = true;
        }

        if (changes)
            items.setValue(newList);
    }
    public LiveData<List<PatternDetailsItem>> getItems(Long patternId) {
        if (patternId != null && patternId <= 0)
            throw new IllegalArgumentException("Pattern ID " + patternId + " is invalid");

        mPatternId = patternId;

        if (mPatternId == null) {
            resetItems();
            return items;
        }

        DB db = DB.get();
        LiveData<PatternWithAccounts> dbList = db.getPatternDAO()
                                                 .getPatternWithAccounts(mPatternId);
        Observer<PatternWithAccounts> observer = new Observer<PatternWithAccounts>() {
            @Override
            public void onChanged(PatternWithAccounts src) {
                ArrayList<PatternDetailsItem> l = new ArrayList<>();

                PatternDetailsItem header = PatternDetailsItem.fromRoomObject(src.header);
                l.add(header);
                for (PatternAccount acc : src.accounts) {
                    l.add(PatternDetailsItem.fromRoomObject(acc));
                }

                for (PatternDetailsItem i : l) {
                    Logger.debug("patterns-db", "Loaded pattern item " + i);
                }
                items.postValue(l);

                dbList.removeObserver(this);
            }
        };
        dbList.observeForever(observer);

        return items;
    }
    public void setTestText(String text) {
        List<PatternDetailsItem> list = new ArrayList<>(items.getValue());
        PatternDetailsItem.Header header = new PatternDetailsItem.Header(list.get(0)
                                                                             .asHeaderItem());
        header.setTestText(text);
        list.set(0, header);

        items.setValue(list);
    }
    public void onSavePattern() {
        Logger.debug("flow", "PatternDetailsViewModel.onSavePattern(); model=" + this);
        final List<PatternDetailsItem> list = Objects.requireNonNull(items.getValue());

        AsyncTask.execute(() -> {
            boolean newPattern = mPatternId == null || mPatternId <= 0;

            PatternDetailsItem.Header modelHeader = list.get(0)
                                                        .asHeaderItem();
            PatternHeaderDAO headerDAO = DB.get()
                                           .getPatternDAO();
            PatternHeader dbHeader = modelHeader.toDBO();
            if (newPattern) {
                dbHeader.setId(null);
                dbHeader.setId(mPatternId = headerDAO.insert(dbHeader));
            }
            else
                headerDAO.update(dbHeader);

            Logger.debug("pattern-db",
                    String.format(Locale.US, "Stored pattern header %d, item=%s", dbHeader.getId(),
                            modelHeader));


            PatternAccountDAO paDAO = DB.get()
                                        .getPatternAccountDAO();
            for (int i = 1; i < list.size(); i++) {
                final PatternDetailsItem.AccountRow accRowItem = list.get(i)
                                                                     .asAccountRowItem();
                PatternAccount dbAccount = accRowItem.toDBO(dbHeader.getId());
                dbAccount.setPatternId(mPatternId);
                dbAccount.setPosition(i);
                if (newPattern) {
                    dbAccount.setId(null);
                    dbAccount.setId(paDAO.insert(dbAccount));
                }
                else
                    paDAO.update(dbAccount);

                Logger.debug("pattern-db", String.format(Locale.US,
                        "Stored pattern account %d, account=%s, comment=%s, item=%s",
                        dbAccount.getId(), dbAccount.getAccountName(),
                        dbAccount.getAccountComment(), accRowItem));
            }
        });
    }
}