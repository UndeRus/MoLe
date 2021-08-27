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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.dao.BaseDAO;
import net.ktnx.mobileledger.dao.TemplateAccountDAO;
import net.ktnx.mobileledger.dao.TemplateHeaderDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TemplateWithAccounts;
import net.ktnx.mobileledger.model.TemplateDetailsItem;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TemplateDetailsViewModel extends ViewModel {
    static final String TAG = "template-details-model";
    static final String DB_TAG = TAG + "-db";
    private final MutableLiveData<List<TemplateDetailsItem>> items =
            new MutableLiveData<>(Collections.emptyList());
    private final AtomicInteger syntheticItemId = new AtomicInteger(0);
    private Long mPatternId;
    private String mDefaultTemplateName;
    private boolean itemsLoaded = false;
    public String getDefaultTemplateName() {
        return mDefaultTemplateName;
    }
    public void setDefaultTemplateName(String name) {
        mDefaultTemplateName = name;
    }

    public void resetItems() {
        applyList(new ArrayList<>());
    }
    public void applyList(List<TemplateDetailsItem> srcList) {
        applyList(srcList, false);
    }
    public void applyList(List<TemplateDetailsItem> srcList, boolean async) {
        boolean changes;
        if (srcList == null) {
            srcList = new ArrayList<>(items.getValue());
            changes = false;
        }
        else
            changes = true;

        srcList = Collections.unmodifiableList(srcList);

        if (BuildConfig.DEBUG) {
            Logger.debug(TAG, "Considering old list");
            for (TemplateDetailsItem item : srcList)
                Logger.debug(TAG, String.format(Locale.US, " id %d pos %d", item.getId(),
                        item.getPosition()));
        }

        ArrayList<TemplateDetailsItem> newList = new ArrayList<>();

        boolean hasEmptyItem = false;

        if (srcList.size() < 1) {
            final TemplateDetailsItem.Header header = TemplateDetailsItem.createHeader();
            header.setId(0);
            newList.add(header);
            changes = true;
        }
        else {
            newList.add(srcList.get(0));
        }

        for (int i = 1; i < srcList.size(); i++) {
            final TemplateDetailsItem.AccountRow accRow = srcList.get(i)
                                                                 .asAccountRowItem();
            if (accRow.isEmpty()) {
                // it is normal to have two empty rows if they are at the
                // top (position 1 and 2)
                if (!hasEmptyItem || i < 3) {
                    accRow.setPosition(newList.size());
                    newList.add(accRow);
                }
                else
                    changes = true; // row skipped

                hasEmptyItem = true;
            }
            else {
                accRow.setPosition(newList.size());
                newList.add(accRow);
            }
        }

        while (newList.size() < 3) {
            final TemplateDetailsItem.AccountRow accountRow =
                    TemplateDetailsItem.createAccountRow();
            accountRow.setId(genItemId());
            accountRow.setPosition(newList.size());
            newList.add(accountRow);
            changes = true;
            hasEmptyItem = true;
        }

        if (!hasEmptyItem) {
            final TemplateDetailsItem.AccountRow accountRow =
                    TemplateDetailsItem.createAccountRow();
            accountRow.setId(genItemId());
            accountRow.setPosition(newList.size());
            newList.add(accountRow);
            changes = true;
        }

        if (changes) {
            Logger.debug(TAG, "Changes detected, applying new list");

            if (async)
                items.postValue(newList);
            else
                items.setValue(newList);
        }
        else
            Logger.debug(TAG, "No changes, ignoring new list");
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
                Logger.debug(DB_TAG, "Got header template item with id of " + header.getId());
                l.add(header);
                Collections.sort(src.accounts,
                        (o1, o2) -> Long.compare(o1.getPosition(), o2.getPosition()));
                for (TemplateAccount acc : src.accounts) {
                    l.add(TemplateDetailsItem.fromRoomObject(acc));
                }

                for (TemplateDetailsItem i : l) {
                    Logger.debug(DB_TAG, "Loaded pattern item " + i);
                }
                applyList(l, true);
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

        BaseDAO.runAsync(() -> {
            boolean newPattern = mPatternId == null || mPatternId <= 0;

            TemplateDetailsItem.Header modelHeader = list.get(0)
                                                         .asHeaderItem();

            modelHeader.setName(Misc.trim(modelHeader.getName()));
            if (modelHeader.getName()
                           .isEmpty())
                modelHeader.setName(getDefaultTemplateName());

            TemplateHeaderDAO headerDAO = DB.get()
                                            .getTemplateDAO();
            TemplateHeader dbHeader = modelHeader.toDBO();
            if (newPattern) {
                dbHeader.setId(0L);
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
                    dbAccount.setId(0);
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
    private ArrayList<TemplateDetailsItem> copyItems() {
        List<TemplateDetailsItem> oldList = items.getValue();

        if (oldList == null)
            return new ArrayList<>();

        ArrayList<TemplateDetailsItem> result = new ArrayList<>(oldList.size());

        for (TemplateDetailsItem item : oldList) {
            if (item instanceof TemplateDetailsItem.Header)
                result.add(new TemplateDetailsItem.Header(item.asHeaderItem()));
            else if (item instanceof TemplateDetailsItem.AccountRow)
                result.add(new TemplateDetailsItem.AccountRow(item.asAccountRowItem()));
            else
                throw new RuntimeException("Unexpected item " + item);
        }

        return result;
    }
    public void moveItem(int sourcePos, int targetPos) {
        final List<TemplateDetailsItem> newList = copyItems();

        if (BuildConfig.DEBUG) {
            Logger.debug("drag", "Before move:");
            for (int i = 1; i < newList.size(); i++) {
                final TemplateDetailsItem item = newList.get(i);
                Logger.debug("drag",
                        String.format(Locale.US, "  %d: id %d, pos %d", i, item.getId(),
                                item.getPosition()));
            }
        }

        {
            TemplateDetailsItem item = newList.remove(sourcePos);
            newList.add(targetPos, item);
        }

        // adjust affected items' positions
        {
            int startPos, endPos;
            if (sourcePos < targetPos) {
                // moved down
                startPos = sourcePos;
                endPos = targetPos;
            }
            else {
                // moved up
                startPos = targetPos;
                endPos = sourcePos;
            }

            for (int i = startPos; i <= endPos; i++) {
                newList.get(i)
                       .setPosition(i);
            }
        }

        if (BuildConfig.DEBUG) {
            Logger.debug("drag", "After move:");
            for (int i = 1; i < newList.size(); i++) {
                final TemplateDetailsItem item = newList.get(i);
                Logger.debug("drag",
                        String.format(Locale.US, "  %d: id %d, pos %d", i, item.getId(),
                                item.getPosition()));
            }
        }

        items.setValue(newList);
    }
    public void removeItem(int position) {
        Logger.debug(TAG, "Removing item at position " + position);
        ArrayList<TemplateDetailsItem> newList = copyItems();
        newList.remove(position);
        for (int i = position; i < newList.size(); i++)
            newList.get(i)
                   .setPosition(i);
        applyList(newList);
    }
}