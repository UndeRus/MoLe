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

package net.ktnx.mobileledger.ui.new_transaction;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.InertMutableLiveData;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MatchedTemplate;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.MatchResult;

enum ItemType {generalData, transactionRow}

enum FocusedElement {Account, Comment, Amount, Description, TransactionComment}


public class NewTransactionModel extends ViewModel {
    private static final int MIN_ITEMS = 3;
    private final MutableLiveData<Boolean> showCurrency = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isSubmittable = new InertMutableLiveData<>(false);
    private final MutableLiveData<Boolean> showComments = new MutableLiveData<>(true);
    private final MutableLiveData<List<Item>> items = new MutableLiveData<>();
    private final MutableLiveData<Boolean> simulateSave = new InertMutableLiveData<>(false);
    private final AtomicInteger busyCounter = new AtomicInteger(0);
    private final MutableLiveData<Boolean> busyFlag = new InertMutableLiveData<>(false);
    private final Observer<MobileLedgerProfile> profileObserver = profile -> {
        showCurrency.postValue(profile.getShowCommodityByDefault());
        showComments.postValue(profile.getShowCommentsByDefault());
    };
    private final MutableLiveData<FocusInfo> focusInfo = new MutableLiveData<>();
    private boolean observingDataProfile;
    public NewTransactionModel() {
        reset();
    }
    public LiveData<Boolean> getShowCurrency() {
        return showCurrency;
    }
    public LiveData<List<Item>> getItems() {
        return items;
    }
    private void setItems(@NonNull List<Item> newList) {
        checkTransactionSubmittable(newList);
        setItemsWithoutSubmittableChecks(newList);
    }
    private void setItemsWithoutSubmittableChecks(@NonNull List<Item> list) {
        Logger.debug("new-trans", "model: Setting new item list");
        final int cnt = list.size();
        for (int i = 1; i < cnt - 1; i++) {
            final TransactionAccount item = list.get(i)
                                                .toTransactionAccount();
            if (item.isLast) {
                TransactionAccount replacement = new TransactionAccount(item);
                replacement.isLast = false;
                list.set(i, replacement);
            }
        }
        final TransactionAccount last = list.get(cnt - 1)
                                            .toTransactionAccount();
        if (!last.isLast) {
            TransactionAccount replacement = new TransactionAccount(last);
            replacement.isLast = true;
            list.set(cnt - 1, replacement);
        }

        items.setValue(list);
    }
    private List<Item> copyList() {
        return copyList(null);
    }
    private List<Item> copyList(@Nullable List<Item> source) {
        List<Item> copy = new ArrayList<>();
        List<Item> oldList = (source == null) ? items.getValue() : source;

        if (oldList != null)
            for (Item item : oldList) {
                copy.add(Item.from(item));
            }

        return copy;
    }
    private List<Item> shallowCopyListWithoutItem(int position) {
        List<Item> copy = new ArrayList<>();
        List<Item> oldList = items.getValue();

        if (oldList != null) {
            int i = 0;
            for (Item item : oldList) {
                if (i++ == position)
                    continue;
                copy.add(item);
            }
        }

        return copy;
    }
    private List<Item> shallowCopyList() {
        return new ArrayList<>(items.getValue());
    }
    LiveData<Boolean> getShowComments() {
        return showComments;
    }
    void observeDataProfile(LifecycleOwner activity) {
        if (!observingDataProfile)
            Data.observeProfile(activity, profileObserver);
        observingDataProfile = true;
    }
    boolean getSimulateSaveFlag() {
        Boolean value = simulateSave.getValue();
        if (value == null)
            return false;
        return value;
    }
    LiveData<Boolean> getSimulateSave() {
        return simulateSave;
    }
    void toggleSimulateSave() {
        simulateSave.setValue(!getSimulateSaveFlag());
    }
    LiveData<Boolean> isSubmittable() {
        return this.isSubmittable;
    }
    void reset() {
        Logger.debug("new-trans", "Resetting model");
        List<Item> list = new ArrayList<>();
        list.add(new TransactionHead(""));
        list.add(new TransactionAccount(""));
        list.add(new TransactionAccount(""));
        noteFocusChanged(0, FocusedElement.Description);
        isSubmittable.setValue(false);
        setItemsWithoutSubmittableChecks(list);
    }
    boolean accountsInInitialState() {
        final List<Item> list = items.getValue();

        if (list == null)
            return true;

        for (Item item : list) {
            if (!(item instanceof TransactionAccount))
                continue;

            TransactionAccount accRow = (TransactionAccount) item;
            if (!accRow.isEmpty())
                return false;
        }

        return true;
    }
    void applyTemplate(MatchedTemplate matchedTemplate, String text) {
        SimpleDate transactionDate = null;
        final MatchResult matchResult = matchedTemplate.matchResult;
        final TemplateHeader templateHead = matchedTemplate.templateHead;
        {
            int day = extractIntFromMatches(matchResult, templateHead.getDateDayMatchGroup(),
                    templateHead.getDateDay());
            int month = extractIntFromMatches(matchResult, templateHead.getDateMonthMatchGroup(),
                    templateHead.getDateMonth());
            int year = extractIntFromMatches(matchResult, templateHead.getDateYearMatchGroup(),
                    templateHead.getDateYear());

            if (year > 0 || month > 0 || day > 0) {
                SimpleDate today = SimpleDate.today();
                if (year <= 0)
                    year = today.year;
                if (month <= 0)
                    month = today.month;
                if (day <= 0)
                    day = today.day;

                transactionDate = new SimpleDate(year, month, day);

                Logger.debug("pattern", "setting transaction date to " + transactionDate);
            }
        }

        List<Item> present = copyList();

        TransactionHead head = new TransactionHead(present.get(0)
                                                          .toTransactionHead());
        if (transactionDate != null)
            head.setDate(transactionDate);

        final String transactionDescription = extractStringFromMatches(matchResult,
                templateHead.getTransactionDescriptionMatchGroup(),
                templateHead.getTransactionDescription());
        if (Misc.emptyIsNull(transactionDescription) != null)
            head.setDescription(transactionDescription);

        final String transactionComment = extractStringFromMatches(matchResult,
                templateHead.getTransactionCommentMatchGroup(),
                templateHead.getTransactionComment());
        if (Misc.emptyIsNull(transactionComment) != null)
            head.setComment(transactionComment);

        List<Item> newItems = new ArrayList<>();

        newItems.add(head);

        for (int i = 1; i < present.size(); i++) {
            final TransactionAccount row = present.get(i)
                                                  .toTransactionAccount();
            if (!row.isEmpty())
                newItems.add(new TransactionAccount(row));
        }

        DB.get()
          .getTemplateDAO()
          .getTemplateWithAccountsAsync(templateHead.getId(), entry -> {
              int rowIndex = 0;
              final boolean accountsInInitialState = accountsInInitialState();
              for (TemplateAccount acc : entry.accounts) {
                  rowIndex++;

                  String accountName =
                          extractStringFromMatches(matchResult, acc.getAccountNameMatchGroup(),
                                  acc.getAccountName());
                  String accountComment =
                          extractStringFromMatches(matchResult, acc.getAccountCommentMatchGroup(),
                                  acc.getAccountComment());
                  Float amount = extractFloatFromMatches(matchResult, acc.getAmountMatchGroup(),
                          acc.getAmount());
                  if (amount != null && acc.getNegateAmount() != null && acc.getNegateAmount())
                      amount = -amount;

                  // TODO currency
                  TransactionAccount accRow = new TransactionAccount(accountName);
                  accRow.setComment(accountComment);
                  if (amount != null)
                      accRow.setAmount(amount);

                  newItems.add(accRow);
              }

              new Handler(Looper.getMainLooper()).post(() -> setItems(newItems));
          });
    }
    private int extractIntFromMatches(MatchResult m, Integer group, Integer literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 & grp <= m.groupCount())
                try {
                    return Integer.parseInt(m.group(grp));
                }
                catch (NumberFormatException e) {
                    Logger.debug("new-trans", "Error extracting matched number", e);
                }
        }

        return 0;
    }
    private String extractStringFromMatches(MatchResult m, Integer group, String literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 & grp <= m.groupCount())
                return m.group(grp);
        }

        return null;
    }
    private Float extractFloatFromMatches(MatchResult m, Integer group, Float literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 & grp <= m.groupCount())
                try {
                    return Float.valueOf(m.group(grp));
                }
                catch (NumberFormatException e) {
                    Logger.debug("new-trans", "Error extracting matched number", e);
                }
        }

        return null;
    }
    void removeItem(int pos) {
        List<Item> newList = shallowCopyListWithoutItem(pos);
        setItems(newList);
    }
    void noteFocusChanged(int position, FocusedElement element) {
        FocusInfo present = focusInfo.getValue();
        if (present == null || present.position != position || present.element != element)
            focusInfo.setValue(new FocusInfo(position, element));
    }
    public LiveData<FocusInfo> getFocusInfo() {
        return focusInfo;
    }
    void moveItem(int fromIndex, int toIndex) {
        List<Item> newList = shallowCopyList();
        Item item = newList.remove(fromIndex);
        newList.add(toIndex, item);
        items.setValue(newList); // same count, same submittable state
    }
    void moveItemLast(List<Item> list, int index) {
        /*   0
             1   <-- index
             2
             3   <-- desired position
                 (no bottom filler)
         */
        int itemCount = list.size();

        if (index < itemCount - 1)
            list.add(list.remove(index));
    }
    void toggleCurrencyVisible() {
        showCurrency.setValue(!Objects.requireNonNull(showCurrency.getValue()));
    }
    void stopObservingBusyFlag(Observer<Boolean> observer) {
        busyFlag.removeObserver(observer);
    }
    void incrementBusyCounter() {
        int newValue = busyCounter.incrementAndGet();
        if (newValue == 1)
            busyFlag.postValue(true);
    }
    void decrementBusyCounter() {
        int newValue = busyCounter.decrementAndGet();
        if (newValue == 0)
            busyFlag.postValue(false);
    }
    public LiveData<Boolean> getBusyFlag() {
        return busyFlag;
    }
    public void toggleShowComments() {
        showComments.setValue(!Objects.requireNonNull(showComments.getValue()));
    }
    public LedgerTransaction constructLedgerTransaction() {
        List<Item> list = Objects.requireNonNull(items.getValue());
        TransactionHead head = list.get(0)
                                   .toTransactionHead();
        SimpleDate date = head.getDate();
        LedgerTransaction tr = head.asLedgerTransaction();

        tr.setComment(head.getComment());
        LedgerTransactionAccount emptyAmountAccount = null;
        float emptyAmountAccountBalance = 0;
        for (int i = 1; i < list.size(); i++) {
            TransactionAccount item = list.get(i)
                                          .toTransactionAccount();
            LedgerTransactionAccount acc = new LedgerTransactionAccount(item.getAccountName()
                                                                            .trim(),
                    item.getCurrency());
            if (acc.getAccountName()
                   .isEmpty())
                continue;

            acc.setComment(item.getComment());

            if (item.isAmountSet()) {
                acc.setAmount(item.getAmount());
                emptyAmountAccountBalance += item.getAmount();
            }
            else {
                emptyAmountAccount = acc;
            }

            tr.addAccount(acc);
        }

        if (emptyAmountAccount != null)
            emptyAmountAccount.setAmount(-emptyAmountAccountBalance);

        return tr;
    }
    void loadTransactionIntoModel(String profileUUID, int transactionId) {
        List<Item> newList = new ArrayList<>();
        LedgerTransaction tr;
        MobileLedgerProfile profile = Data.getProfile(profileUUID);
        if (profile == null)
            throw new RuntimeException(String.format(
                    "Unable to find profile %s, which is supposed to contain transaction %d",
                    profileUUID, transactionId));

        tr = profile.loadTransaction(transactionId);
        TransactionHead head = new TransactionHead(tr.getDescription());
        head.setComment(tr.getComment());

        newList.add(head);

        List<LedgerTransactionAccount> accounts = tr.getAccounts();

        TransactionAccount firstNegative = null;
        TransactionAccount firstPositive = null;
        int singleNegativeIndex = -1;
        int singlePositiveIndex = -1;
        int negativeCount = 0;
        for (int i = 0; i < accounts.size(); i++) {
            LedgerTransactionAccount acc = accounts.get(i);
            TransactionAccount item =
                    new TransactionAccount(acc.getAccountName(), acc.getCurrency());
            newList.add(item);

            item.setAccountName(acc.getAccountName());
            item.setComment(acc.getComment());
            if (acc.isAmountSet()) {
                item.setAmount(acc.getAmount());
                if (acc.getAmount() < 0) {
                    if (firstNegative == null) {
                        firstNegative = item;
                        singleNegativeIndex = i + 1;
                    }
                    else
                        singleNegativeIndex = -1;
                }
                else {
                    if (firstPositive == null) {
                        firstPositive = item;
                        singlePositiveIndex = i + 1;
                    }
                    else
                        singlePositiveIndex = -1;
                }
            }
            else
                item.resetAmount();
        }
        if (BuildConfig.DEBUG)
            dumpItemList("Loaded previous transaction", newList);

        if (singleNegativeIndex != -1) {
            firstNegative.resetAmount();
            moveItemLast(newList, singleNegativeIndex);
        }
        else if (singlePositiveIndex != -1) {
            firstPositive.resetAmount();
            moveItemLast(newList, singlePositiveIndex);
        }

        setItems(newList);

        noteFocusChanged(1, FocusedElement.Amount);
    }
    /**
     * A transaction is submittable if:
     * 0) has description
     * 1) has at least two account names
     * 2) each row with amount has account name
     * 3) for each commodity:
     * 3a) amounts must balance to 0, or
     * 3b) there must be exactly one empty amount (with account)
     * 4) empty accounts with empty amounts are ignored
     * Side effects:
     * 5) a row with an empty account name or empty amount is guaranteed to exist for each
     * commodity
     * 6) at least two rows need to be present in the ledger
     *
     * @param list - the item list to check. Can be the displayed list or a list that will be
     *             displayed soon
     */
    @SuppressLint("DefaultLocale")
    void checkTransactionSubmittable(@Nullable List<Item> list) {
        boolean workingWithLiveList = false;
        boolean liveListCopied = false;
        if (list == null) {
            list = Objects.requireNonNull(items.getValue());
            workingWithLiveList = true;
        }

        if (BuildConfig.DEBUG)
            dumpItemList("Before submittable checks", list);

        int accounts = 0;
        final BalanceForCurrency balance = new BalanceForCurrency();
        final String descriptionText = list.get(0)
                                           .toTransactionHead()
                                           .getDescription();
        boolean submittable = true;
        boolean listChanged = false;
        final ItemsForCurrency itemsForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithEmptyAmountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithAccountAndEmptyAmountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithEmptyAccountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithAmountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithAccountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency emptyRowsForCurrency = new ItemsForCurrency();
        final List<Item> emptyRows = new ArrayList<>();

        try {
            if ((descriptionText == null) || descriptionText.trim()
                                                            .isEmpty())
            {
                Logger.debug("submittable", "Transaction not submittable: missing description");
                submittable = false;
            }

            for (int i = 1; i < list.size(); i++) {
                TransactionAccount item = list.get(i)
                                              .toTransactionAccount();

                String accName = item.getAccountName()
                                     .trim();
                String currName = item.getCurrency();

                itemsForCurrency.add(currName, item);

                if (accName.isEmpty()) {
                    itemsWithEmptyAccountForCurrency.add(currName, item);

                    if (item.isAmountSet()) {
                        // 2) each amount has account name
                        Logger.debug("submittable", String.format(
                                "Transaction not submittable: row %d has no account name, but" +
                                " has" + " amount %1.2f", i + 1, item.getAmount()));
                        submittable = false;
                    }
                    else {
                        emptyRowsForCurrency.add(currName, item);
                    }
                }
                else {
                    accounts++;
                    itemsWithAccountForCurrency.add(currName, item);
                }

                if (!item.isAmountValid()) {
                    Logger.debug("submittable",
                            String.format("Not submittable: row %d has an invalid amount", i + 1));
                    submittable = false;
                }
                else if (item.isAmountSet()) {
                    itemsWithAmountForCurrency.add(currName, item);
                    balance.add(currName, item.getAmount());
                }
                else {
                    itemsWithEmptyAmountForCurrency.add(currName, item);

                    if (!accName.isEmpty())
                        itemsWithAccountAndEmptyAmountForCurrency.add(currName, item);
                }
            }

            // 1) has at least two account names
            if (accounts < 2) {
                if (accounts == 0)
                    Logger.debug("submittable", "Transaction not submittable: no account names");
                else if (accounts == 1)
                    Logger.debug("submittable",
                            "Transaction not submittable: only one account name");
                else
                    Logger.debug("submittable",
                            String.format("Transaction not submittable: only %d account names",
                                    accounts));
                submittable = false;
            }

            // 3) for each commodity:
            // 3a) amount must balance to 0, or
            // 3b) there must be exactly one empty amount (with account)
            for (String balCurrency : itemsForCurrency.currencies()) {
                float currencyBalance = balance.get(balCurrency);
                if (Misc.isZero(currencyBalance)) {
                    // remove hints from all amount inputs in that currency
                    for (int i = 1; i < list.size(); i++) {
                        TransactionAccount acc = list.get(i)
                                                     .toTransactionAccount();
                        if (Misc.equalStrings(acc.getCurrency(), balCurrency)) {
                            if (BuildConfig.DEBUG)
                                Logger.debug("submittable",
                                        String.format("Resetting hint of '%s' [%s]",
                                                Misc.nullIsEmpty(acc.getAccountName()),
                                                balCurrency));
                            // skip if the amount is set, in which case the hint is not
                            // important/visible
                            if (!acc.isAmountSet() && acc.amountHintIsSet &&
                                !TextUtils.isEmpty(acc.getAmountHint()))
                            {
                                if (workingWithLiveList && !liveListCopied) {
                                    list = copyList(list);
                                    liveListCopied = true;
                                }
                                final TransactionAccount newAcc = new TransactionAccount(acc);
                                newAcc.setAmountHint(null);
                                if (!liveListCopied) {
                                    list = copyList(list);
                                    liveListCopied = true;
                                }
                                list.set(i, newAcc);
                                listChanged = true;
                            }
                        }
                    }
                }
                else {
                    List<Item> tmpList =
                            itemsWithAccountAndEmptyAmountForCurrency.getList(balCurrency);
                    int balanceReceiversCount = tmpList.size();
                    if (balanceReceiversCount != 1) {
                        if (BuildConfig.DEBUG) {
                            if (balanceReceiversCount == 0)
                                Logger.debug("submittable", String.format(
                                        "Transaction not submittable [%s]: non-zero balance " +
                                        "with no empty amounts with accounts", balCurrency));
                            else
                                Logger.debug("submittable", String.format(
                                        "Transaction not submittable [%s]: non-zero balance " +
                                        "with multiple empty amounts with accounts", balCurrency));
                        }
                        submittable = false;
                    }

                    List<Item> emptyAmountList =
                            itemsWithEmptyAmountForCurrency.getList(balCurrency);

                    // suggest off-balance amount to a row and remove hints on other rows
                    Item receiver = null;
                    if (!tmpList.isEmpty())
                        receiver = tmpList.get(0);
                    else if (!emptyAmountList.isEmpty())
                        receiver = emptyAmountList.get(0);

                    for (int i = 0; i < list.size(); i++) {
                        Item item = list.get(i);
                        if (!(item instanceof TransactionAccount))
                            continue;

                        TransactionAccount acc = item.toTransactionAccount();
                        if (!Misc.equalStrings(acc.getCurrency(), balCurrency))
                            continue;

                        if (item == receiver) {
                            final String hint = String.format("%1.2f", -currencyBalance);
                            if (!acc.isAmountHintSet() ||
                                !TextUtils.equals(acc.getAmountHint(), hint))
                            {
                                Logger.debug("submittable",
                                        String.format("Setting amount hint of {%s} to %s [%s]",
                                                acc.toString(), hint, balCurrency));
                                if (workingWithLiveList & !liveListCopied) {
                                    list = copyList(list);
                                    liveListCopied = true;
                                }
                                final TransactionAccount newAcc = new TransactionAccount(acc);
                                newAcc.setAmountHint(hint);
                                list.set(i, newAcc);
                                listChanged = true;
                            }
                        }
                        else {
                            if (BuildConfig.DEBUG)
                                Logger.debug("submittable",
                                        String.format("Resetting hint of '%s' [%s]",
                                                Misc.nullIsEmpty(acc.getAccountName()),
                                                balCurrency));
                            if (acc.amountHintIsSet && !TextUtils.isEmpty(acc.getAmountHint())) {
                                if (workingWithLiveList && !liveListCopied) {
                                    list = copyList(list);
                                    liveListCopied = true;
                                }
                                final TransactionAccount newAcc = new TransactionAccount(acc);
                                newAcc.setAmountHint(null);
                                list.set(i, newAcc);
                                listChanged = true;
                            }
                        }
                    }
                }
            }

            // 5) a row with an empty account name or empty amount is guaranteed to exist for
            // each commodity
            for (String balCurrency : balance.currencies()) {
                int currEmptyRows = itemsWithEmptyAccountForCurrency.size(balCurrency);
                int currRows = itemsForCurrency.size(balCurrency);
                int currAccounts = itemsWithAccountForCurrency.size(balCurrency);
                int currAmounts = itemsWithAmountForCurrency.size(balCurrency);
                if ((currEmptyRows == 0) &&
                    ((currRows == currAccounts) || (currRows == currAmounts)))
                {
                    // perhaps there already is an unused empty row for another currency that
                    // is not used?
//                        boolean foundIt = false;
//                        for (Item item : emptyRows) {
//                            Currency itemCurrency = item.getCurrency();
//                            String itemCurrencyName =
//                                    (itemCurrency == null) ? "" : itemCurrency.getName();
//                            if (Misc.isZero(balance.get(itemCurrencyName))) {
//                                item.setCurrency(Currency.loadByName(balCurrency));
//                                item.setAmountHint(
//                                        String.format("%1.2f", -balance.get(balCurrency)));
//                                foundIt = true;
//                                break;
//                            }
//                        }
//
//                        if (!foundIt)
                    if (workingWithLiveList && !liveListCopied) {
                        list = copyList(list);
                        liveListCopied = true;
                    }
                    final TransactionAccount newAcc = new TransactionAccount("", balCurrency);
                    final float bal = balance.get(balCurrency);
                    if (!Misc.isZero(bal) && currAmounts == currRows)
                        newAcc.setAmountHint(String.format("%4.2f", -bal));
                    Logger.debug("submittable",
                            String.format("Adding new item with %s for currency %s",
                                    newAcc.getAmountHint(), balCurrency));
                    list.add(newAcc);
                    listChanged = true;
                }
            }

            // drop extra empty rows, not needed
            for (String currName : emptyRowsForCurrency.currencies()) {
                List<Item> emptyItems = emptyRowsForCurrency.getList(currName);
                while ((list.size() > MIN_ITEMS) && (emptyItems.size() > 1)) {
                    if (workingWithLiveList && !liveListCopied) {
                        list = copyList(list);
                        liveListCopied = true;
                    }
                    // the list is a copy, so the empty item is no longer present
                    Item itemToRemove = emptyItems.remove(1);
                    removeItemById(list, itemToRemove.id);
                    listChanged = true;
                }

                // unused currency, remove last item (which is also an empty one)
                if ((list.size() > MIN_ITEMS) && (emptyItems.size() == 1)) {
                    List<Item> currItems = itemsForCurrency.getList(currName);

                    if (currItems.size() == 1) {
                        if (workingWithLiveList && !liveListCopied) {
                            list = copyList(list);
                            liveListCopied = true;
                        }
                        // the list is a copy, so the empty item is no longer present
                        removeItemById(list, emptyItems.get(0).id);
                        listChanged = true;
                    }
                }
            }

            // 6) at least two rows need to be present in the ledger
            //    (the list also contains header and trailer)
            while (list.size() < MIN_ITEMS) {
                if (workingWithLiveList && !liveListCopied) {
                    list = copyList(list);
                    liveListCopied = true;
                }
                list.add(new TransactionAccount(""));
                listChanged = true;
            }


            Logger.debug("submittable", submittable ? "YES" : "NO");
            isSubmittable.setValue(submittable);

            if (BuildConfig.DEBUG)
                dumpItemList("After submittable checks", list);
        }
        catch (NumberFormatException e) {
            Logger.debug("submittable", "NO (because of NumberFormatException)");
            isSubmittable.setValue(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            Logger.debug("submittable", "NO (because of an Exception)");
            isSubmittable.setValue(false);
        }

        if (listChanged && workingWithLiveList) {
            setItemsWithoutSubmittableChecks(list);
        }
    }
    private void removeItemById(@NotNull List<Item> list, int id) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            list.removeIf(item -> item.id == id);
        }
        else {
            for (Item item : list) {
                if (item.id == id) {
                    list.remove(item);
                    break;
                }
            }
        }
    }
    @SuppressLint("DefaultLocale")
    private void dumpItemList(@NotNull String msg, @NotNull List<Item> list) {
        Logger.debug("submittable", "== Dump of all items " + msg);
        for (int i = 1; i < list.size(); i++) {
            TransactionAccount item = list.get(i)
                                          .toTransactionAccount();
            Logger.debug("submittable", String.format("%d:%s", i, item.toString()));
        }
    }
    public void setItemCurrency(int position, String newCurrency) {
        TransactionAccount item = Objects.requireNonNull(items.getValue())
                                         .get(position)
                                         .toTransactionAccount();
        final String oldCurrency = item.getCurrency();

        if (Misc.equalStrings(oldCurrency, newCurrency))
            return;

        List<Item> newList = copyList();
        newList.get(position)
               .toTransactionAccount()
               .setCurrency(newCurrency);

        setItems(newList);
    }
    public boolean accountListIsEmpty() {
        List<Item> items = Objects.requireNonNull(this.items.getValue());

        for (Item item : items) {
            if (!(item instanceof TransactionAccount))
                continue;

            if (!((TransactionAccount) item).isEmpty())
                return false;
        }

        return true;
    }

    public static class FocusInfo {
        int position;
        FocusedElement element;
        public FocusInfo(int position, FocusedElement element) {
            this.position = position;
            this.element = element;
        }
    }

    static abstract class Item {
        private static int idDispenser = 0;
        protected int id;
        private Item() {
            synchronized (Item.class) {
                id = ++idDispenser;
            }
        }
        public static Item from(Item origin) {
            if (origin instanceof TransactionHead)
                return new TransactionHead((TransactionHead) origin);
            if (origin instanceof TransactionAccount)
                return new TransactionAccount((TransactionAccount) origin);
            throw new RuntimeException("Don't know how to handle " + origin);
        }
        public int getId() {
            return id;
        }
        public abstract ItemType getType();
        public TransactionHead toTransactionHead() {
            if (this instanceof TransactionHead)
                return (TransactionHead) this;

            throw new IllegalStateException("Wrong item type " + this);
        }
        public TransactionAccount toTransactionAccount() {
            if (this instanceof TransactionAccount)
                return (TransactionAccount) this;

            throw new IllegalStateException("Wrong item type " + this);
        }
        public boolean equalContents(@Nullable Object item) {
            if (item == null)
                return false;

            if (!getClass().equals(item.getClass()))
                return false;

            // shortcut - comparing same instance
            if (item == this)
                return true;

            if (this instanceof TransactionHead)
                return ((TransactionHead) item).equalContents((TransactionHead) this);
            if (this instanceof TransactionAccount)
                return ((TransactionAccount) item).equalContents((TransactionAccount) this);

            throw new RuntimeException("Don't know how to handle " + this);
        }
    }


//==========================================================================================

    public static class TransactionHead extends Item {
        private SimpleDate date;
        private String description;
        private String comment;
        TransactionHead(String description) {
            super();
            this.description = description;
        }
        public TransactionHead(TransactionHead origin) {
            id = origin.id;
            date = origin.date;
            description = origin.description;
            comment = origin.comment;
        }
        public SimpleDate getDate() {
            return date;
        }
        public void setDate(SimpleDate date) {
            this.date = date;
        }
        public void setDate(String text) throws ParseException {
            if (Misc.emptyIsNull(text) == null) {
                date = null;
                return;
            }

            date = Globals.parseLedgerDate(text);
        }
        /**
         * getFormattedDate()
         *
         * @return nicely formatted, shortest available date representation
         */
        String getFormattedDate() {
            if (date == null)
                return null;

            Calendar today = GregorianCalendar.getInstance();

            if (today.get(Calendar.YEAR) != date.year) {
                return String.format(Locale.US, "%d/%02d/%02d", date.year, date.month, date.day);
            }

            if (today.get(Calendar.MONTH) + 1 != date.month) {
                return String.format(Locale.US, "%d/%02d", date.month, date.day);
            }

            return String.valueOf(date.day);
        }
        @NonNull
        @Override
        public String toString() {
            @SuppressLint("DefaultLocale") StringBuilder b = new StringBuilder(
                    String.format("id:%d/%s", id, Integer.toHexString(hashCode())));

            if (TextUtils.isEmpty(description))
                b.append(" «no description»");
            else
                b.append(String.format(" descr'%s'", description));

            if (date != null)
                b.append(String.format("@%s", date.toString()));

            if (!TextUtils.isEmpty(comment))
                b.append(String.format(" /%s/", comment));

            return b.toString();
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(String comment) {
            this.comment = comment;
        }
        @Override
        public ItemType getType() {
            return ItemType.generalData;
        }
        public LedgerTransaction asLedgerTransaction() {
            return new LedgerTransaction(null, date, description, Data.getProfile());
        }
        public boolean equalContents(TransactionHead other) {
            if (other == null)
                return false;

            return Objects.equals(date, other.date) &&
                   TextUtils.equals(description, other.description) &&
                   TextUtils.equals(comment, other.comment);
        }
    }

    public static class TransactionAccount extends Item {
        private String accountName;
        private String amountHint;
        private String comment;
        private String currency;
        private float amount;
        private boolean amountSet;
        private boolean amountValid = true;
        private FocusedElement focusedElement = FocusedElement.Account;
        private boolean amountHintIsSet = true;
        private boolean isLast = false;
        private int accountNameCursorPosition;
        public TransactionAccount(TransactionAccount origin) {
            id = origin.id;
            accountName = origin.accountName;
            amount = origin.amount;
            amountSet = origin.amountSet;
            amountHint = origin.amountHint;
            amountHintIsSet = origin.amountHintIsSet;
            comment = origin.comment;
            currency = origin.currency;
            amountValid = origin.amountValid;
            focusedElement = origin.focusedElement;
            isLast = origin.isLast;
            accountNameCursorPosition = origin.accountNameCursorPosition;
        }
        public TransactionAccount(LedgerTransactionAccount account) {
            super();
            currency = account.getCurrency();
            amount = account.getAmount();
        }
        public TransactionAccount(String accountName) {
            super();
            this.accountName = accountName;
        }
        public TransactionAccount(String accountName, String currency) {
            super();
            this.accountName = accountName;
            this.currency = currency;
        }
        public boolean isLast() {
            return isLast;
        }
        public boolean isAmountSet() {
            return amountSet;
        }
        public String getAccountName() {
            return accountName;
        }
        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }
        public float getAmount() {
            if (!amountSet)
                throw new IllegalStateException("Amount is not set");
            return amount;
        }
        public void setAmount(float amount) {
            this.amount = amount;
            amountSet = true;
        }
        public void resetAmount() {
            amountSet = false;
        }
        @Override
        public ItemType getType() {
            return ItemType.transactionRow;
        }
        public String getAmountHint() {
            return amountHint;
        }
        public void setAmountHint(String amountHint) {
            this.amountHint = amountHint;
            amountHintIsSet = !TextUtils.isEmpty(amountHint);
        }
        public String getComment() {
            return comment;
        }
        public void setComment(String comment) {
            this.comment = comment;
        }
        public String getCurrency() {
            return currency;
        }
        public void setCurrency(String currency) {
            this.currency = currency;
        }
        public boolean isAmountValid() {
            return amountValid;
        }
        public void setAmountValid(boolean amountValid) {
            this.amountValid = amountValid;
        }
        public FocusedElement getFocusedElement() {
            return focusedElement;
        }
        public void setFocusedElement(FocusedElement focusedElement) {
            this.focusedElement = focusedElement;
        }
        public boolean isAmountHintSet() {
            return amountHintIsSet;
        }
        public void setAmountHintIsSet(boolean amountHintIsSet) {
            this.amountHintIsSet = amountHintIsSet;
        }
        public boolean isEmpty() {
            return !amountSet && Misc.emptyIsNull(accountName) == null &&
                   Misc.emptyIsNull(comment) == null;
        }
        @SuppressLint("DefaultLocale")
        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(String.format("id:%d/%s", id, Integer.toHexString(hashCode())));
            if (!TextUtils.isEmpty(accountName))
                b.append(String.format(" acc'%s'", accountName));

            if (amountSet)
                b.append(String.format(" %4.2f", amount));
            else if (amountHintIsSet)
                b.append(String.format(" (%s)", amountHint));

            if (!TextUtils.isEmpty(currency))
                b.append(" ")
                 .append(currency);

            if (!TextUtils.isEmpty(comment))
                b.append(String.format(" /%s/", comment));

            if (isLast)
                b.append(" last");

            return b.toString();
        }
        public boolean equalContents(TransactionAccount other) {
            if (other == null)
                return false;

            boolean equal = TextUtils.equals(accountName, other.accountName);
            equal = equal && TextUtils.equals(comment, other.comment) &&
                    (amountSet ? other.amountSet && amount == other.amount : !other.amountSet);

            // compare amount hint only if there is no amount
            if (!amountSet)
                equal = equal && (amountHintIsSet ? other.amountHintIsSet &&
                                                    TextUtils.equals(amountHint, other.amountHint)
                                                  : !other.amountHintIsSet);
            equal = equal && TextUtils.equals(currency, other.currency) && isLast == other.isLast;

            Logger.debug("new-trans",
                    String.format("Comparing {%s} and {%s}: %s", this.toString(), other.toString(),
                            equal));
            return equal;
        }
        public int getAccountNameCursorPosition() {
            return accountNameCursorPosition;
        }
        public void setAccountNameCursorPosition(int position) {
            this.accountNameCursorPosition = position;
        }
    }

    private static class BalanceForCurrency {
        private final HashMap<String, Float> hashMap = new HashMap<>();
        float get(String currencyName) {
            Float f = hashMap.get(currencyName);
            if (f == null) {
                f = 0f;
                hashMap.put(currencyName, f);
            }
            return f;
        }
        void add(String currencyName, float amount) {
            hashMap.put(currencyName, get(currencyName) + amount);
        }
        Set<String> currencies() {
            return hashMap.keySet();
        }
        boolean containsCurrency(String currencyName) {
            return hashMap.containsKey(currencyName);
        }
    }

    private static class ItemsForCurrency {
        private final HashMap<String, List<Item>> hashMap = new HashMap<>();
        @NonNull
        List<NewTransactionModel.Item> getList(@Nullable String currencyName) {
            List<NewTransactionModel.Item> list = hashMap.get(currencyName);
            if (list == null) {
                list = new ArrayList<>();
                hashMap.put(currencyName, list);
            }
            return list;
        }
        void add(@Nullable String currencyName, @NonNull NewTransactionModel.Item item) {
            getList(currencyName).add(item);
        }
        int size(@Nullable String currencyName) {
            return this.getList(currencyName)
                       .size();
        }
        Set<String> currencies() {
            return hashMap.keySet();
        }
    }
}
