/*
 * Copyright © 2020 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.activity;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Globals;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class NewTransactionModel extends ViewModel {
    final MutableLiveData<Boolean> showCurrency = new MutableLiveData<>(false);
    final ArrayList<Item> items = new ArrayList<>();
    final MutableLiveData<Boolean> isSubmittable = new MutableLiveData<>(false);
    final MutableLiveData<Boolean> showComments = new MutableLiveData<>(true);
    private final Item header = new Item(this, "");
    private final Item trailer = new Item(this);
    private final MutableLiveData<Integer> focusedItem = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> accountCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> simulateSave = new MutableLiveData<>(false);
    private final AtomicInteger busyCounter = new AtomicInteger(0);
    private final MutableLiveData<Boolean> busyFlag = new MutableLiveData<>(false);
    private final Observer<MobileLedgerProfile> profileObserver = profile -> {
        showCurrency.postValue(profile.getShowCommodityByDefault());
        showComments.postValue(profile.getShowCommentsByDefault());
    };
    private boolean observingDataProfile;
    void observeShowComments(LifecycleOwner owner, Observer<? super Boolean> observer) {
        showComments.observe(owner, observer);
    }
    void observeBusyFlag(@NonNull LifecycleOwner owner, Observer<? super Boolean> observer) {
        busyFlag.observe(owner, observer);
    }
    void observeDataProfile(LifecycleOwner activity) {
        if (!observingDataProfile)
            Data.observeProfile(activity, profileObserver);
        observingDataProfile = true;
    }
    boolean getSimulateSave() {
        return simulateSave.getValue();
    }
    public void setSimulateSave(boolean simulateSave) {
        this.simulateSave.setValue(simulateSave);
    }
    void toggleSimulateSave() {
        simulateSave.setValue(!simulateSave.getValue());
    }
    void observeSimulateSave(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                             @NonNull androidx.lifecycle.Observer<? super Boolean> observer) {
        this.simulateSave.observe(owner, observer);
    }
    int getAccountCount() {
        return items.size();
    }
    public SimpleDate getDate() {
        return header.date.getValue();
    }
    public String getDescription() {
        return header.description.getValue();
    }
    public String getComment() {
        return header.comment.getValue();
    }
    LiveData<Boolean> isSubmittable() {
        return this.isSubmittable;
    }
    void reset() {
        header.date.setValue(null);
        header.description.setValue(null);
        header.comment.setValue(null);
        items.clear();
        items.add(new Item(this, new LedgerTransactionAccount("")));
        items.add(new Item(this, new LedgerTransactionAccount("")));
        focusedItem.setValue(0);
    }
    void observeFocusedItem(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                            @NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.focusedItem.observe(owner, observer);
    }
    void stopObservingFocusedItem(@NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.focusedItem.removeObserver(observer);
    }
    void observeAccountCount(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                             @NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.accountCount.observe(owner, observer);
    }
    void stopObservingAccountCount(@NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.accountCount.removeObserver(observer);
    }
    int getFocusedItem() { return focusedItem.getValue(); }
    void setFocusedItem(int position) {
        focusedItem.setValue(position);
    }
    int addAccount(LedgerTransactionAccount acc) {
        items.add(new Item(this, acc));
        accountCount.setValue(getAccountCount());
        return items.size();
    }
    boolean accountsInInitialState() {
        for (Item item : items) {
            LedgerTransactionAccount acc = item.getAccount();
            if (acc.isAmountSet())
                return false;
            if (!acc.getAccountName()
                    .trim()
                    .isEmpty())
                return false;
        }

        return true;
    }
    LedgerTransactionAccount getAccount(int index) {
        return items.get(index)
                    .getAccount();
    }
    Item getItem(int index) {
        if (index == 0) {
            return header;
        }

        if (index <= items.size())
            return items.get(index - 1);

        return trailer;
    }
    void removeRow(Item item, NewTransactionItemsAdapter adapter) {
        int pos = items.indexOf(item);
        items.remove(pos);
        if (adapter != null) {
            adapter.notifyItemRemoved(pos + 1);
            sendCountNotifications();
        }
    }
    void removeItem(int pos) {
        items.remove(pos);
        accountCount.setValue(getAccountCount());
    }
    void sendCountNotifications() {
        accountCount.setValue(getAccountCount());
    }
    public void sendFocusedNotification() {
        focusedItem.setValue(focusedItem.getValue());
    }
    void updateFocusedItem(int position) {
        focusedItem.setValue(position);
    }
    void noteFocusChanged(int position, FocusedElement element) {
        getItem(position).setFocusedElement(element);
    }
    void swapItems(int one, int two) {
        Collections.swap(items, one - 1, two - 1);
    }
    void moveItemLast(int index) {
        /*   0
             1   <-- index
             2
             3   <-- desired position
         */
        int itemCount = items.size();

        if (index < itemCount - 1) {
            Item acc = items.remove(index);
            items.add(itemCount - 1, acc);
        }
    }
    void toggleCurrencyVisible() {
        showCurrency.setValue(!showCurrency.getValue());
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
    public boolean getBusyFlag() {
        return busyFlag.getValue();
    }
    public void toggleShowComments() {
        showComments.setValue(!showComments.getValue());
    }
    enum ItemType {generalData, transactionRow, bottomFiller}

    enum FocusedElement {Account, Comment, Amount, Description, TransactionComment}


    //==========================================================================================


    static class Item {
        private final ItemType type;
        private final MutableLiveData<SimpleDate> date = new MutableLiveData<>();
        private final MutableLiveData<String> description = new MutableLiveData<>();
        private final MutableLiveData<String> amountHint = new MutableLiveData<>(null);
        private final NewTransactionModel model;
        private final MutableLiveData<Boolean> editable = new MutableLiveData<>(true);
        private final MutableLiveData<String> comment = new MutableLiveData<>(null);
        private final MutableLiveData<Currency> currency = new MutableLiveData<>(null);
        private final MutableLiveData<Boolean> amountValid = new MutableLiveData<>(true);
        private LedgerTransactionAccount account;
        private FocusedElement focusedElement = FocusedElement.Account;
        private boolean amountHintIsSet = false;
        Item(NewTransactionModel model) {
            this.model = model;
            type = ItemType.bottomFiller;
            editable.setValue(false);
        }
        Item(NewTransactionModel model, String description) {
            this.model = model;
            this.type = ItemType.generalData;
            this.description.setValue(description);
            this.editable.setValue(true);
        }
        Item(NewTransactionModel model, LedgerTransactionAccount account) {
            this.model = model;
            this.type = ItemType.transactionRow;
            this.account = account;
            String currName = account.getCurrency();
            Currency curr = null;
            if ((currName != null) && !currName.isEmpty())
                curr = Currency.loadByName(currName);
            this.currency.setValue(curr);
            this.editable.setValue(true);
        }
        FocusedElement getFocusedElement() {
            return focusedElement;
        }
        void setFocusedElement(FocusedElement focusedElement) {
            this.focusedElement = focusedElement;
        }
        public NewTransactionModel getModel() {
            return model;
        }
        void setEditable(boolean editable) {
            ensureTypeIsGeneralDataOrTransactionRow();
            this.editable.setValue(editable);
        }
        private void ensureTypeIsGeneralDataOrTransactionRow() {
            if ((type != ItemType.generalData) && (type != ItemType.transactionRow)) {
                throw new RuntimeException(
                        String.format("Actual type (%s) differs from wanted (%s or %s)", type,
                                ItemType.generalData, ItemType.transactionRow));
            }
        }
        String getAmountHint() {
            ensureType(ItemType.transactionRow);
            return amountHint.getValue();
        }
        void setAmountHint(String amountHint) {
            ensureType(ItemType.transactionRow);

            // avoid unnecessary triggers
            if (amountHint == null) {
                if (this.amountHint.getValue() == null)
                    return;
                amountHintIsSet = false;
            }
            else {
                if (amountHint.equals(this.amountHint.getValue()))
                    return;
                amountHintIsSet = true;
            }

            this.amountHint.setValue(amountHint);
        }
        void observeAmountHint(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                               @NonNull androidx.lifecycle.Observer<? super String> observer) {
            this.amountHint.observe(owner, observer);
        }
        void stopObservingAmountHint(
                @NonNull androidx.lifecycle.Observer<? super String> observer) {
            this.amountHint.removeObserver(observer);
        }
        ItemType getType() {
            return type;
        }
        void ensureType(ItemType wantedType) {
            if (type != wantedType) {
                throw new RuntimeException(
                        String.format("Actual type (%s) differs from wanted (%s)", type,
                                wantedType));
            }
        }
        public SimpleDate getDate() {
            ensureType(ItemType.generalData);
            return date.getValue();
        }
        public void setDate(SimpleDate date) {
            ensureType(ItemType.generalData);
            this.date.setValue(date);
        }
        public void setDate(String text) throws ParseException {
            if ((text == null) || text.trim()
                                      .isEmpty())
            {
                setDate((SimpleDate) null);
                return;
            }

            SimpleDate date = Globals.parseLedgerDate(text);
            this.setDate(date);
        }
        void observeDate(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                         @NonNull androidx.lifecycle.Observer<? super SimpleDate> observer) {
            this.date.observe(owner, observer);
        }
        void stopObservingDate(@NonNull androidx.lifecycle.Observer<? super SimpleDate> observer) {
            this.date.removeObserver(observer);
        }
        public String getDescription() {
            ensureType(ItemType.generalData);
            return description.getValue();
        }
        public void setDescription(String description) {
            ensureType(ItemType.generalData);
            this.description.setValue(description);
        }
        void observeDescription(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                @NonNull androidx.lifecycle.Observer<? super String> observer) {
            this.description.observe(owner, observer);
        }
        void stopObservingDescription(
                @NonNull androidx.lifecycle.Observer<? super String> observer) {
            this.description.removeObserver(observer);
        }
        public String getTransactionComment() {
            ensureType(ItemType.generalData);
            return comment.getValue();
        }
        public void setTransactionComment(String transactionComment) {
            ensureType(ItemType.generalData);
            this.comment.setValue(transactionComment);
        }
        void observeTransactionComment(@NonNull @NotNull LifecycleOwner owner,
                                       @NonNull Observer<? super String> observer) {
            ensureType(ItemType.generalData);
            this.comment.observe(owner, observer);
        }
        void stopObservingTransactionComment(@NonNull Observer<? super String> observer) {
            this.comment.removeObserver(observer);
        }
        public LedgerTransactionAccount getAccount() {
            ensureType(ItemType.transactionRow);
            return account;
        }
        public void setAccountName(String name) {
            account.setAccountName(name);
        }
        /**
         * getFormattedDate()
         *
         * @return nicely formatted, shortest available date representation
         */
        String getFormattedDate() {
            if (date == null)
                return null;
            SimpleDate d = date.getValue();
            if (d == null)
                return null;

            Calendar today = GregorianCalendar.getInstance();

            if (today.get(Calendar.YEAR) != d.year) {
                return String.format(Locale.US, "%d/%02d/%02d", d.year, d.month, d.day);
            }

            if (today.get(Calendar.MONTH) != d.month - 1) {
                return String.format(Locale.US, "%d/%02d", d.month, d.day);
            }

            return String.valueOf(d.day);
        }
        void observeEditableFlag(NewTransactionActivity activity, Observer<Boolean> observer) {
            editable.observe(activity, observer);
        }
        void stopObservingEditableFlag(Observer<Boolean> observer) {
            editable.removeObserver(observer);
        }
        void observeComment(NewTransactionActivity activity, Observer<String> observer) {
            comment.observe(activity, observer);
        }
        void stopObservingComment(Observer<String> observer) {
            comment.removeObserver(observer);
        }
        public void setComment(String comment) {
            getAccount().setComment(comment);
            this.comment.postValue(comment);
        }
        public Currency getCurrency() {
            return this.currency.getValue();
        }
        public void setCurrency(Currency currency) {
            Currency present = this.currency.getValue();
            if ((currency == null) && (present != null) ||
                (currency != null) && !currency.equals(present))
            {
                getAccount().setCurrency((currency != null && !currency.getName()
                                                                       .isEmpty())
                                         ? currency.getName() : null);
                this.currency.setValue(currency);
            }
        }
        void observeCurrency(NewTransactionActivity activity, Observer<Currency> observer) {
            currency.observe(activity, observer);
        }
        void stopObservingCurrency(Observer<Currency> observer) {
            currency.removeObserver(observer);
        }
        boolean isBottomFiller() {
            return this.type == ItemType.bottomFiller;
        }
        boolean isAmountHintSet() {
            return amountHintIsSet;
        }
        void validateAmount() {
            amountValid.setValue(true);
        }
        void invalidateAmount() {
            amountValid.setValue(false);
        }
        void observeAmountValidity(NewTransactionActivity activity, Observer<Boolean> observer) {
            amountValid.observe(activity, observer);
        }
        void stopObservingAmountValidity(Observer<Boolean> observer) {
            amountValid.removeObserver(observer);
        }
    }
}
