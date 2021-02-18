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
import java.util.concurrent.atomic.AtomicInteger;

public class TemplateDetailsViewModel extends ViewModel {
    private final MutableLiveData<List<TemplateDetailsItem>> items =
            new MutableLiveData<>(Collections.emptyList());
    private Long mPatternId;
    private String mDefaultPatternName;
    private boolean itemsLoaded = false;
    private final AtomicInteger syntheticItemId = new AtomicInteger(0);

    public String getDefaultPatternName() {
        return mDefaultPatternName;
    }
    public void setDefaultPatternName(String name) {
        mDefaultPatternName = name;
    }

    public void resetItems() {
        checkItemConsistency(new ArrayList<>());
    }
    public void checkItemConsistency(List<TemplateDetailsItem> list) {
        if (list == null)
            list = new ArrayList<>(items.getValue());

        boolean changes = false;
        if (list.size() < 1) {
            final TemplateDetailsItem.Header header = TemplateDetailsItem.createHeader();
            header.setName(mDefaultPatternName);
            header.setId(0);
            list.add(header);
            changes = true;
        }

        while (list.size() < 3) {
            final TemplateDetailsItem.AccountRow accountRow =
                    TemplateDetailsItem.createAccountRow();
            accountRow.setId(genItemId());
            list.add(accountRow);
            changes = true;
        }

        if (changes)
            items.setValue(list);
    }
    public int genItemId() {
        return syntheticItemId.decrementAndGet();
    }
    public LiveData<List<TemplateDetailsItem>> getItems(Long patternId) {
        if (itemsLoaded && Objects.equals(patternId, this.mPatternId))
            return items;

        if (patternId != null && patternId <= 0)
            throw new IllegalArgumentException("Pattern ID " + patternId + " is invalid");

        mPatternId = patternId;

        if (mPatternId == null) {
            resetItems();
            itemsLoaded = true;
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
                itemsLoaded = true;

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
                dbHeader.setId(mPatternId = headerDAO.insertSync(dbHeader));
            }
            else
                headerDAO.updateSync(dbHeader);

            Logger.debug("pattern-db",
                    String.format(Locale.US, "Stored pattern header %d, item=%s", dbHeader.getId(),
                            modelHeader));


            TemplateAccountDAO taDAO = DB.get()
                                         .getTemplateAccountDAO();
            taDAO.prepareForSave(mPatternId);
            for (int i = 1; i < list.size(); i++) {
                final TemplateDetailsItem.AccountRow accRowItem = list.get(i)
                                                                      .asAccountRowItem();
                TemplateAccount dbAccount = accRowItem.toDBO(dbHeader.getId());
                dbAccount.setTemplateId(mPatternId);
                dbAccount.setPosition(i);
                if (dbAccount.getId() < 0) {
                    dbAccount.setId(null);
                    dbAccount.setId(taDAO.insertSync(dbAccount));
                }
                else
                    taDAO.updateSync(dbAccount);

                Logger.debug("pattern-db", String.format(Locale.US,
                        "Stored pattern account %d, account=%s, comment=%s, neg=%s, item=%s",
                        dbAccount.getId(), dbAccount.getAccountName(),
                        dbAccount.getAccountComment(), dbAccount.getNegateAmount(), accRowItem));
            }
            taDAO.finishSave(mPatternId);
        });
    }
    public void moveItem(int sourcePos, int targetPos) {
        ArrayList<TemplateDetailsItem> newList = new ArrayList<>(items.getValue());
        TemplateDetailsItem item = newList.remove(sourcePos);
        newList.add(targetPos, item);
        items.setValue(newList);
    }
    public void removeItem(int position) {
        ArrayList<TemplateDetailsItem> newList = new ArrayList<>(items.getValue());
        newList.remove(position);
        checkItemConsistency(newList);
        items.setValue(newList);
    }
}