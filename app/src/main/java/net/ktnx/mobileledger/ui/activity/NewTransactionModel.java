/*
 * Copyright © 2019 Damyan Ivanov.
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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewTransactionModel extends ViewModel {
    private static final Pattern reYMD =
            Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    private static final Pattern reMD = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    private static final Pattern reD = Pattern.compile("\\s*(\\d+)\\s*$");
    final MutableLiveData<Boolean> showCurrency = new MutableLiveData<>(false);
    final ArrayList<Item> items = new ArrayList<>();
    final MutableLiveData<Boolean> isSubmittable = new MutableLiveData<>(false);
    private final Item header = new Item(this, null, "");
    private final Item trailer = new Item(this);
    private final MutableLiveData<Integer> focusedItem = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> accountCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> simulateSave = new MutableLiveData<>(false);
    private boolean observingDataProfile;
    private Observer<MobileLedgerProfile> profileObserver =
            profile -> showCurrency.postValue(profile.getShowCommodityByDefault());
    private final AtomicInteger busyCounter = new AtomicInteger(0);
    private final MutableLiveData<Boolean> busyFlag = new MutableLiveData<>(false);
    void observeBusyFlag(LifecycleOwner owner, Observer<? super Boolean> observer) {
        busyFlag.observe(owner, observer);
    }
    void observeDataProfile(LifecycleOwner activity) {
        if (!observingDataProfile)
            Data.profile.observe(activity, profileObserver);
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
    public Date getDate() {
        return header.date.getValue();
    }
    public String getDescription() {
        return header.description.getValue();
    }
    LiveData<Boolean> isSubmittable() {
        return this.isSubmittable;
    }
    void reset() {
        header.date.setValue(null);
        header.description.setValue(null);
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
        if (newValue == 1) busyFlag.postValue(true);
    }
    void decrementBusyCounter() {
        int newValue = busyCounter.decrementAndGet();
        if (newValue == 0) busyFlag.postValue(false);
    }
    enum ItemType {generalData, transactionRow, bottomFiller}

    enum FocusedElement {Account, Comment, Amount}


    //==========================================================================================


    static class Item {
        private ItemType type;
        private MutableLiveData<Date> date = new MutableLiveData<>();
        private MutableLiveData<String> description = new MutableLiveData<>();
        private LedgerTransactionAccount account;
        private MutableLiveData<String> amountHint = new MutableLiveData<>(null);
        private NewTransactionModel model;
        private MutableLiveData<Boolean> editable = new MutableLiveData<>(true);
        private FocusedElement focusedElement = FocusedElement.Account;
        private MutableLiveData<String> comment = new MutableLiveData<>(null);
        private MutableLiveData<Currency> currency = new MutableLiveData<>(null);
        private boolean amountHintIsSet = false;
        Item(NewTransactionModel model) {
            this.model = model;
            type = ItemType.bottomFiller;
            editable.setValue(false);
        }
        Item(NewTransactionModel model, Date date, String description) {
            this.model = model;
            this.type = ItemType.generalData;
            this.date.setValue(date);
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
            ensureType(ItemType.generalData, ItemType.transactionRow);
            this.editable.setValue(editable);
        }
        private void ensureType(ItemType type1, ItemType type2) {
            if ((type != type1) && (type != type2)) {
                throw new RuntimeException(
                        String.format("Actual type (%s) differs from wanted (%s or %s)", type,
                                type1, type2));
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
        public Date getDate() {
            ensureType(ItemType.generalData);
            return date.getValue();
        }
        public void setDate(Date date) {
            ensureType(ItemType.generalData);
            this.date.setValue(date);
        }
        public void setDate(String text) {
            if ((text == null) || text.trim()
                                      .isEmpty())
            {
                setDate((Date) null);
                return;
            }

            int year, month, day;
            final Calendar c = GregorianCalendar.getInstance();
            Matcher m = reYMD.matcher(text);
            if (m.matches()) {
                year = Integer.parseInt(m.group(1));
                month = Integer.parseInt(m.group(2)) - 1;   // month is 0-based
                day = Integer.parseInt(m.group(3));
            }
            else {
                year = c.get(Calendar.YEAR);
                m = reMD.matcher(text);
                if (m.matches()) {
                    month = Integer.parseInt(m.group(1)) - 1;
                    day = Integer.parseInt(m.group(2));
                }
                else {
                    month = c.get(Calendar.MONTH);
                    m = reD.matcher(text);
                    if (m.matches()) {
                        day = Integer.parseInt(m.group(1));
                    }
                    else {
                        day = c.get(Calendar.DAY_OF_MONTH);
                    }
                }
            }

            c.set(year, month, day);

            this.setDate(c.getTime());
        }
        void observeDate(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                         @NonNull androidx.lifecycle.Observer<? super Date> observer) {
            this.date.observe(owner, observer);
        }
        void stopObservingDate(@NonNull androidx.lifecycle.Observer<? super Date> observer) {
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
            Date time = date.getValue();
            if (time == null)
                return null;

            Calendar c = GregorianCalendar.getInstance();
            c.setTime(time);
            Calendar today = GregorianCalendar.getInstance();

            final int myYear = c.get(Calendar.YEAR);
            final int myMonth = c.get(Calendar.MONTH);
            final int myDay = c.get(Calendar.DAY_OF_MONTH);

            if (today.get(Calendar.YEAR) != myYear) {
                return String.format(Locale.US, "%d/%02d/%02d", myYear, myMonth + 1, myDay);
            }

            if (today.get(Calendar.MONTH) != myMonth) {
                return String.format(Locale.US, "%d/%02d", myMonth + 1, myDay);
            }

            return String.valueOf(myDay);
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
        boolean isOfType(ItemType type) {
            return this.type == type;
        }
        boolean isAmountHintSet() {
            return amountHintIsSet;
        }
    }
}
