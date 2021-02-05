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

package net.ktnx.mobileledger.ui.templates;

import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.dao.TemplateAccountDAO;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.model.TemplateDetailsItem;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TemplateDetailsViewModel extends ViewModel {
    private final MutableLiveData<List<TemplateDetailsItem>> items =
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
        ArrayList<TemplateDetailsItem> newList = new ArrayList<>();
        final TemplateDetailsItem.Header header = TemplateDetailsItem.createHeader();
        header.setName(mDefaultPatternName);
        header.setId(0);
        newList.add(header);

        while (newList.size() < 3) {
            final TemplateDetailsItem.AccountRow aRow = TemplateDetailsItem.createAccountRow();
            aRow.setId(newList.size() + 1);
            newList.add(aRow);
        }

        items.setValue(newList);
    }
    private void checkItemConsistency() {
        ArrayList<TemplateDetailsItem> newList = new ArrayList<>(items.getValue());
        boolean changes = false;
        if (newList.size() < 1) {
            final TemplateDetailsItem.Header header = TemplateDetailsItem.createHeader();
            header.setName(mDefaultPatternName);
            newList.add(header);
            changes = true;
        }

        while (newList.size() < 3) {
            newList.add(TemplateDetailsItem.createAccountRow());
            changes = true;
        }

        if (changes)
            items.setValue(newList);
    }
    public LiveData<List<TemplateDetailsItem>> getItems(Long patternId) {
        if (patternId != null && patternId <= 0)
            throw new IllegalArgumentException("Pattern ID " + patternId + " is invalid");

        mPatternId = patternId;

        if (mPatternId == null) {
            resetItems();
            return items;
        }

        DB db = DB.get();
        LiveData<TemplateWithAccounts> dbList = db.getTemplateDAO()
                                                  .getTemplateWithAccounts(mPatternId);
        Observer<TemplateWithAccounts> observer = new Observer<TemplateWithAccounts>() {
            @Override
            public void onChanged(TemplateWithAccounts src) {
                ArrayList<TemplateDetailsItem> l = new ArrayList<>();

                TemplateDetailsItem header = TemplateDetailsItem.fromRoomObject(src.header);
                l.add(header);
                for (TemplateAccount acc : src.accounts) {
                    l.add(TemplateDetailsItem.fromRoomObject(acc));
                }

                for (TemplateDetailsItem i : l) {
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
        List<TemplateDetailsItem> list = new ArrayList<>(items.getValue());
        TemplateDetailsItem.Header header = new TemplateDetailsItem.Header(list.get(0)
                                                                               .asHeaderItem());
        header.setTestText(text);
        list.set(0, header);

        items.setValue(list);
    }
    public void onSaveTemplate() {
        Logger.debug("flow", "PatternDetailsViewModel.onSavePattern(); model=" + this);
        final List<TemplateDetailsItem> list = Objects.requireNonNull(items.getValue());

        AsyncTask.execute(() -> {
            boolean newPattern = mPatternId == null || mPatternId <= 0;

            TemplateDetailsItem.Header modelHeader = list.get(0)
                                                         .asHeaderItem();
            TemplateHeaderDAO headerDAO = DB.get()
                                            .getTemplateDAO();
            TemplateHeader dbHeader = modelHeader.toDBO();
            if (newPattern) {
                dbHeader.setId(null);
                dbHeader.setId(mPatternId = headerDAO.insert(dbHeader));
            }
            else
                headerDAO.update(dbHeader);

            Logger.debug("pattern-db",
                    String.format(Locale.US, "Stored pattern header %d, item=%s", dbHeader.getId(),
                            modelHeader));


            TemplateAccountDAO taDAO = DB.get()
                                         .getTemplateAccountDAO();
            for (int i = 1; i < list.size(); i++) {
                final TemplateDetailsItem.AccountRow accRowItem = list.get(i)
                                                                      .asAccountRowItem();
                TemplateAccount dbAccount = accRowItem.toDBO(dbHeader.getId());
                dbAccount.setTemplateId(mPatternId);
                dbAccount.setPosition(i);
                if (newPattern) {
                    dbAccount.setId(null);
                    dbAccount.setId(taDAO.insert(dbAccount));
                }
                else
                    taDAO.update(dbAccount);

                Logger.debug("pattern-db", String.format(Locale.US,
                        "Stored pattern account %d, account=%s, comment=%s, neg=%s, item=%s",
                        dbAccount.getId(), dbAccount.getAccountName(),
                        dbAccount.getAccountComment(), dbAccount.getNegateAmount(), accRowItem));
            }
        });
    }
}