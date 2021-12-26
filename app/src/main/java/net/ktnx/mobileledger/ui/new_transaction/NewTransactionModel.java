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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.db.Currency;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.db.TemplateAccount;
import net.ktnx.mobileledger.db.TemplateHeader;
import net.ktnx.mobileledger.db.TransactionWithAccounts;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.InertMutableLiveData;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MatchedTemplate;
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
    private final Observer<Profile> profileObserver = profile -> {
        if (profile != null) {
            showCurrency.postValue(profile.getShowCommodityByDefault());
            showComments.postValue(profile.getShowCommentsByDefault());
        }
    };
    private final MutableLiveData<FocusInfo> focusInfo = new MutableLiveData<>();
    private boolean observingDataProfile;
    public NewTransactionModel() {
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
    private void replaceItems(@NonNull List<Item> newList) {
        renumberItems();

        setItems(newList);
    }
    /**
     * make old items replaceable in-place. makes the new values visually blend in
     */
    private void renumberItems() {
        renumberItems(items.getValue());
    }
    private void renumberItems(List<Item> list) {
        if (list == null) {
            return;
        }

        int id = 0;
        for (Item item : list)
            item.id = id++;
    }
    private void setItemsWithoutSubmittableChecks(@NonNull List<Item> list) {
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

        if (BuildConfig.DEBUG)
            dumpItemList("Before setValue()", list);
        items.setValue(list);
    }
    private List<Item> copyList() {
        List<Item> copy = new ArrayList<>();
        List<Item> oldList = items.getValue();

        if (oldList != null)
            for (Item item : oldList) {
                copy.add(Item.from(item));
            }

        return copy;
    }
    private List<Item> copyListWithoutItem(int position) {
        List<Item> copy = new ArrayList<>();
        List<Item> oldList = items.getValue();

        if (oldList != null) {
            int i = 0;
            for (Item item : oldList) {
                if (i++ == position)
                    continue;
                copy.add(Item.from(item));
            }
        }

        return copy;
    }
    private List<Item> shallowCopyList() {
        return new ArrayList<>(Objects.requireNonNull(items.getValue()));
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
        Item.resetIdDispenser();
        list.add(new TransactionHead(""));
        final String defaultCurrency = Objects.requireNonNull(Data.getProfile())
                                              .getDefaultCommodity();
        list.add(new TransactionAccount("", defaultCurrency));
        list.add(new TransactionAccount("", defaultCurrency));
        noteFocusChanged(0, FocusedElement.Description);
        renumberItems();
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

                  TransactionAccount accRow = new TransactionAccount(accountName);
                  accRow.setComment(accountComment);
                  if (amount != null)
                      accRow.setAmount(amount);
                  accRow.setCurrency(
                          extractCurrencyFromMatches(matchResult, acc.getCurrencyMatchGroup(),
                                  acc.getCurrencyObject()));

                  newItems.add(accRow);
              }

              renumberItems(newItems);
              Misc.onMainThread(() -> replaceItems(newItems));
          });
    }
    @NonNull
    private String extractCurrencyFromMatches(MatchResult m, Integer group, Currency literal) {
        return Misc.nullIsEmpty(
                extractStringFromMatches(m, group, (literal == null) ? "" : literal.getName()));
    }
    private int extractIntFromMatches(MatchResult m, Integer group, Integer literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 && grp <= m.groupCount())
                try {
                    return Integer.parseInt(m.group(grp));
                }
                catch (NumberFormatException e) {
                    Logger.debug("new-trans", "Error extracting matched number", e);
                }
        }

        return 0;
    }
    @Nullable
    private String extractStringFromMatches(MatchResult m, Integer group, String literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 && grp <= m.groupCount())
                return m.group(grp);
        }

        return null;
    }
    private Float extractFloatFromMatches(MatchResult m, Integer group, Float literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 && grp <= m.groupCount())
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
        Logger.debug("new-trans", String.format(Locale.US, "Removing item at position %d", pos));
        List<Item> newList = copyListWithoutItem(pos);
        final FocusInfo fi = focusInfo.getValue();
        if ((fi != null) && (pos < fi.position))
            noteFocusChanged(fi.position - 1, fi.element);
        setItems(newList);
    }
    void noteFocusChanged(int position, @Nullable FocusedElement element) {
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

        FocusInfo fi = focusInfo.getValue();
        if (fi != null && fi.position == fromIndex)
            noteFocusChanged(toIndex, fi.element);

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
        final boolean newValue = !Objects.requireNonNull(showCurrency.getValue());

        // remove currency from all items, or reset currency to the default
        // no need to clone the list, because the removal of the currency won't lead to
        // visual changes -- the currency fields will be hidden or reset to default anyway
        // still, there may be changes in the submittable state
        final List<Item> list = Objects.requireNonNull(this.items.getValue());
        final Profile profile = Objects.requireNonNull(Data.getProfile());
        for (int i = 1; i < list.size(); i++) {
            ((TransactionAccount) list.get(i)).setCurrency(
                    newValue ? profile.getDefaultCommodity() : "");
        }
        checkTransactionSubmittable(null);
        showCurrency.setValue(newValue);
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
        LedgerTransaction tr = head.asLedgerTransaction();

        tr.setComment(head.getComment());
        List<LedgerTransactionAccount> emptyAmountAccounts = new ArrayList<>();
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
                emptyAmountAccounts.add(acc);
            }

            tr.addAccount(acc);
        }

        if (emptyAmountAccounts.size() > 0) {
            if (emptyAmountAccounts.size() > 1 && !Misc.isZero(emptyAmountAccountBalance))
                throw new RuntimeException(String.format(Locale.US,
                        "Should not happen. %d accounts with non zero amount to distribute (%5" +
                        ".3f)"));
            for (LedgerTransactionAccount a : emptyAmountAccounts)
                a.setAmount(-emptyAmountAccountBalance);
        }

        return tr;
    }
    void loadTransactionIntoModel(@NonNull TransactionWithAccounts tr) {
        List<Item> newList = new ArrayList<>();
        Item.resetIdDispenser();

        Item currentHead = Objects.requireNonNull(items.getValue())
                                  .get(0);
        TransactionHead head = new TransactionHead(tr.transaction.getDescription());
        head.setComment(tr.transaction.getComment());
        if (currentHead instanceof TransactionHead)
            head.setDate(((TransactionHead) currentHead).date);

        newList.add(head);

        List<LedgerTransactionAccount> accounts = new ArrayList<>();
        for (net.ktnx.mobileledger.db.TransactionAccount acc : tr.accounts) {
            accounts.add(new LedgerTransactionAccount(acc));
        }

        TransactionAccount firstNegative = null;
        TransactionAccount firstPositive = null;
        int singleNegativeIndex = -1;
        int singlePositiveIndex = -1;
        int negativeCount = 0;
        for (int i = 0; i < accounts.size(); i++) {
            LedgerTransactionAccount acc = accounts.get(i);
            TransactionAccount item = new TransactionAccount(acc.getAccountName(),
                    Misc.nullIsEmpty(acc.getCurrency()));
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

        Misc.onMainThread(() -> {
            setItems(newList);
            noteFocusChanged(1, FocusedElement.Amount);
        });
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
        if (list == null) {
            list = copyList();
            workingWithLiveList = true;
        }

        if (BuildConfig.DEBUG)
            dumpItemList(String.format("Before submittable checks (%s)",
                    workingWithLiveList ? "LIVE LIST" : "custom list"), list);

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

            boolean hasInvalidAmount = false;

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

                if (item.isAmountSet() && item.isAmountValid()) {
                    itemsWithAmountForCurrency.add(currName, item);
                    balance.add(currName, item.getAmount());
                }
                else {
                    if (!item.isAmountValid()) {
                        Logger.debug("submittable",
                                String.format("Not submittable: row %d has an invalid amount", i));
                        submittable = false;
                        hasInvalidAmount = true;
                    }

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
                                        String.format(Locale.US, "Resetting hint of %d:'%s' [%s]",
                                                i, Misc.nullIsEmpty(acc.getAccountName()),
                                                balCurrency));
                            // skip if the amount is set, in which case the hint is not
                            // important/visible
                            if (!acc.isAmountSet() && acc.amountHintIsSet &&
                                !TextUtils.isEmpty(acc.getAmountHint()))
                            {
                                acc.setAmountHint(null);
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
                                        "Transaction not submittable [curr:%s]: non-zero balance " +
                                        "with no empty amounts with accounts", balCurrency));
                            else
                                Logger.debug("submittable", String.format(
                                        "Transaction not submittable [curr:%s]: non-zero balance " +
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
                            final String hint = Data.formatNumber(-currencyBalance);
                            if (!acc.isAmountHintSet() ||
                                !Misc.equalStrings(acc.getAmountHint(), hint))
                            {
                                Logger.debug("submittable",
                                        String.format("Setting amount hint of {%s} to %s [%s]",
                                                acc.toString(), hint, balCurrency));
                                acc.setAmountHint(hint);
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
                                acc.setAmountHint(null);
                                listChanged = true;
                            }
                        }
                    }
                }
            }

            // 5) a row with an empty account name or empty amount is guaranteed to exist for
            // each commodity
            if (!hasInvalidAmount) {
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
//                                        Data.formatNumber(-balance.get(balCurrency)));
//                                foundIt = true;
//                                break;
//                            }
//                        }
//
//                        if (!foundIt)
                        final TransactionAccount newAcc = new TransactionAccount("", balCurrency);
                        final float bal = balance.get(balCurrency);
                        if (!Misc.isZero(bal) && currAmounts == currRows)
                            newAcc.setAmountHint(Data.formatNumber(-bal));
                        Logger.debug("submittable",
                                String.format("Adding new item with %s for currency %s",
                                        newAcc.getAmountHint(), balCurrency));
                        list.add(newAcc);
                        listChanged = true;
                    }
                }
            }

            // drop extra empty rows, not needed
            for (String currName : emptyRowsForCurrency.currencies()) {
                List<Item> emptyItems = emptyRowsForCurrency.getList(currName);
                while ((list.size() > MIN_ITEMS) && (emptyItems.size() > 1)) {
                    // the list is a copy, so the empty item is no longer present
                    Item itemToRemove = emptyItems.remove(1);
                    removeItemById(list, itemToRemove.id);
                    listChanged = true;
                }

                // unused currency, remove last item (which is also an empty one)
                if ((list.size() > MIN_ITEMS) && (emptyItems.size() == 1)) {
                    List<Item> currItems = itemsForCurrency.getList(currName);

                    if (currItems.size() == 1) {
                        // the list is a copy, so the empty item is no longer present
                        removeItemById(list, emptyItems.get(0).id);
                        listChanged = true;
                    }
                }
            }

            // 6) at least two rows need to be present in the ledger
            //    (the list also contains header and trailer)
            while (list.size() < MIN_ITEMS) {
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
        public FocusInfo(int position, @Nullable FocusedElement element) {
            this.position = position;
            this.element = element;
        }
    }

    static abstract class Item {
        private static int idDispenser = 0;
        protected int id;
        private Item() {
            if (this instanceof TransactionHead)
                id = 0;
            else
                synchronized (Item.class) {
                    id = ++idDispenser;
                }
        }
        public Item(int id) {
            this.id = id;
        }
        public static Item from(Item origin) {
            if (origin instanceof TransactionHead)
                return new TransactionHead((TransactionHead) origin);
            if (origin instanceof TransactionAccount)
                return new TransactionAccount((TransactionAccount) origin);
            throw new RuntimeException("Don't know how to handle " + origin);
        }
        private static void resetIdDispenser() {
            idDispenser = 0;
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
            super(origin.id);
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
                b.append(String.format(" '%s'", description));

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
            return new LedgerTransaction(0, (date == null) ? SimpleDate.today() : date, description,
                    Objects.requireNonNull(Data.getProfile()));
        }
        public boolean equalContents(TransactionHead other) {
            if (other == null)
                return false;

            return Objects.equals(date, other.date) &&
                   Misc.equalStrings(description, other.description) &&
                   Misc.equalStrings(comment, other.comment);
        }
    }

    public static class TransactionAccount extends Item {
        private String accountName;
        private String amountHint;
        private String comment;
        @NotNull
        private String currency = "";
        private float amount;
        private boolean amountSet;
        private boolean amountValid = true;
        @NotNull
        private String amountText = "";
        private FocusedElement focusedElement = FocusedElement.Account;
        private boolean amountHintIsSet = false;
        private boolean isLast = false;
        private int accountNameCursorPosition;
        public TransactionAccount(TransactionAccount origin) {
            super(origin.id);
            accountName = origin.accountName;
            amount = origin.amount;
            amountSet = origin.amountSet;
            amountHint = origin.amountHint;
            amountHintIsSet = origin.amountHintIsSet;
            amountText = origin.amountText;
            comment = origin.comment;
            currency = origin.currency;
            amountValid = origin.amountValid;
            focusedElement = origin.focusedElement;
            isLast = origin.isLast;
            accountNameCursorPosition = origin.accountNameCursorPosition;
        }
        public TransactionAccount(String accountName) {
            super();
            this.accountName = accountName;
        }
        public TransactionAccount(String accountName, @NotNull String currency) {
            super();
            this.accountName = accountName;
            this.currency = currency;
        }
        public @NotNull String getAmountText() {
            return amountText;
        }
        public void setAmountText(@NotNull String amountText) {
            this.amountText = amountText;
        }
        public boolean setAndCheckAmountText(@NotNull String amountText) {
            String amtText = amountText.trim();
            this.amountText = amtText;

            boolean significantChange = false;

            if (amtText.isEmpty()) {
                if (amountSet) {
                    significantChange = true;
                }
                resetAmount();
            }
            else {
                try {
                    amtText = amtText.replace(Data.getDecimalSeparator(), Data.decimalDot);
                    final float parsedAmount = Float.parseFloat(amtText);
                    if (!amountSet || !amountValid || !Misc.equalFloats(parsedAmount, amount))
                        significantChange = true;
                    amount = parsedAmount;
                    amountSet = true;
                    amountValid = true;
                }
                catch (NumberFormatException e) {
                    Logger.debug("new-trans", String.format(
                            "assuming amount is not set due to number format exception. " +
                            "input was '%s'", amtText));
                    if (amountValid) // it was valid and now it's not
                        significantChange = true;
                    amountValid = false;
                }
            }

            return significantChange;
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
            amountValid = true;
            amountText = Data.formatNumber(amount);
        }
        public void resetAmount() {
            amountSet = false;
            amountValid = true;
            amountText = "";
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
        @NotNull
        public String getCurrency() {
            return currency;
        }
        public void setCurrency(@org.jetbrains.annotations.Nullable String currency) {
            this.currency = Misc.nullIsEmpty(currency);
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
        @NotNull
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(String.format("id:%d/%s", id, Integer.toHexString(hashCode())));
            if (!TextUtils.isEmpty(accountName))
                b.append(String.format(" acc'%s'", accountName));

            if (amountSet)
                b.append(amountText)
                 .append(" [")
                 .append(amountValid ? "valid" : "invalid")
                 .append("] ")
                 .append(String.format(Locale.ROOT, " {raw %4.2f}", amount));
            else if (amountHintIsSet)
                b.append(String.format(" (hint %s)", amountHint));

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

            boolean equal = Misc.equalStrings(accountName, other.accountName);
            equal = equal && Misc.equalStrings(comment, other.comment) &&
                    (amountSet ? other.amountSet && amountValid == other.amountValid &&
                                 Misc.equalStrings(amountText, other.amountText)
                               : !other.amountSet);

            // compare amount hint only if there is no amount
            if (!amountSet)
                equal = equal && (amountHintIsSet ? other.amountHintIsSet &&
                                                    Misc.equalStrings(amountHint, other.amountHint)
                                                  : !other.amountHintIsSet);
            equal = equal && Misc.equalStrings(currency, other.currency) && isLast == other.isLast;

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
        private final HashMap<@NotNull String, List<Item>> hashMap = new HashMap<>();
        @NonNull
        List<NewTransactionModel.Item> getList(@NotNull String currencyName) {
            List<NewTransactionModel.Item> list = hashMap.get(currencyName);
            if (list == null) {
                list = new ArrayList<>();
                hashMap.put(currencyName, list);
            }
            return list;
        }
        void add(@NotNull String currencyName, @NonNull NewTransactionModel.Item item) {
            getList(Objects.requireNonNull(currencyName)).add(item);
        }
        int size(@NotNull String currencyName) {
            return this.getList(Objects.requireNonNull(currencyName))
                       .size();
        }
        Set<String> currencies() {
            return hashMap.keySet();
        }
    }
}
