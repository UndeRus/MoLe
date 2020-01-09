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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class NewTransactionModel extends ViewModel {
    static final Pattern reYMD = Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reMD = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reD = Pattern.compile("\\s*(\\d+)\\s*$");
    private final Item header = new Item(this, null, "");
    private final Item trailer = new Item(this);
    private final ArrayList<Item> items = new ArrayList<>();
    private final MutableLiveData<Boolean> isSubmittable = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> focusedItem = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> accountCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> simulateSave = new MutableLiveData<>(false);
    public boolean getSimulateSave() {
        return simulateSave.getValue();
    }
    public void setSimulateSave(boolean simulateSave) {
        this.simulateSave.setValue(simulateSave);
    }
    public void toggleSimulateSave() {
        simulateSave.setValue(!simulateSave.getValue());
    }
    public void observeSimulateSave(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                    @NonNull
                                            androidx.lifecycle.Observer<? super Boolean> observer) {
        this.simulateSave.observe(owner, observer);
    }
    public int getAccountCount() {
        return items.size();
    }
    public Date getDate() {
        return header.date.getValue();
    }
    public String getDescription() {
        return header.description.getValue();
    }
    public LiveData<Boolean> isSubmittable() {
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
    public void observeFocusedItem(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                   @NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.focusedItem.observe(owner, observer);
    }
    public void stopObservingFocusedItem(
            @NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.focusedItem.removeObserver(observer);
    }
    public void observeAccountCount(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                    @NonNull
                                            androidx.lifecycle.Observer<? super Integer> observer) {
        this.accountCount.observe(owner, observer);
    }
    public void stopObservingAccountCount(
            @NonNull androidx.lifecycle.Observer<? super Integer> observer) {
        this.accountCount.removeObserver(observer);
    }
    public int getFocusedItem() { return focusedItem.getValue(); }
    public void setFocusedItem(int position) {
        focusedItem.setValue(position);
    }
    public int addAccount(LedgerTransactionAccount acc) {
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
    public Item getItem(int index) {
        if (index == 0) {
            return header;
        }

        if (index <= items.size())
            return items.get(index - 1);

        return trailer;
    }
    /*
     A transaction is submittable if:
     0) has description
     1) has at least two account names
     2) each amount has account name
     3) amounts must balance to 0, or
     3a) there must be exactly one empty amount (with account)
     4) empty accounts with empty amounts are ignored
     5) a row with an empty account name or empty amount is guaranteed to exist
    */
    @SuppressLint("DefaultLocale")
    public void checkTransactionSubmittable(NewTransactionItemsAdapter adapter) {
        int accounts = 0;
        int amounts = 0;
        int empty_rows = 0;
        float balance = 0f;
        final String descriptionText = getDescription();
        boolean submittable = true;
        List<Item> itemsWithEmptyAmount = new ArrayList<>();
        List<Item> itemsWithAccountAndEmptyAmount = new ArrayList<>();

        try {
            if ((descriptionText == null) || descriptionText.trim()
                                                            .isEmpty())
            {
                Logger.debug("submittable", "Transaction not submittable: missing description");
                submittable = false;
            }

            for (int i = 0; i < this.items.size(); i++) {
                Item item = this.items.get(i);

                LedgerTransactionAccount acc = item.getAccount();
                String acc_name = acc.getAccountName()
                                     .trim();
                if (acc_name.isEmpty()) {
                    empty_rows++;

                    if (acc.isAmountSet()) {
                        // 2) each amount has account name
                        Logger.debug("submittable", String.format(
                                "Transaction not submittable: row %d has no account name, but has" +
                                " amount %1.2f", i + 1, acc.getAmount()));
                        submittable = false;
                    }
                }
                else {
                    accounts++;
                }

                if (acc.isAmountSet()) {
                    amounts++;
                    balance += acc.getAmount();
                }
                else {
                    itemsWithEmptyAmount.add(item);

                    if (!acc_name.isEmpty()) {
                        itemsWithAccountAndEmptyAmount.add(item);
                    }
                }
            }

            // 1) has at least two account names
            if (accounts < 2) {
                Logger.debug("submittable",
                        String.format("Transaction not submittable: only %d account names",
                                accounts));
                submittable = false;
            }

            // 3) amount must balance to 0, or
            // 3a) there must be exactly one empty amount (with account)
            if (Misc.isZero(balance)) {
                for (Item item : items) {
                    item.setAmountHint(null);
                }
            }
            else {
                int balanceReceiversCount = itemsWithAccountAndEmptyAmount.size();
                if (balanceReceiversCount != 1) {
                    Logger.debug("submittable", (balanceReceiversCount == 0) ?
                                                "Transaction not submittable: non-zero balance " +
                                                "with no empty amounts with accounts" :
                                                "Transaction not submittable: non-zero balance " +
                                                "with multiple empty amounts with accounts");
                    submittable = false;
                }

                // suggest off-balance amount to a row and remove hints on other rows
                Item receiver = null;
                if (!itemsWithAccountAndEmptyAmount.isEmpty())
                    receiver = itemsWithAccountAndEmptyAmount.get(0);
                else if (!itemsWithEmptyAmount.isEmpty())
                    receiver = itemsWithEmptyAmount.get(0);

                for (Item item : items) {
                    if (item.equals(receiver)) {
                        Logger.debug("submittable",
                                String.format("Setting amount hint to %1.2f", -balance));
                        item.setAmountHint(String.format("%1.2f", -balance));
                    }
                    else
                        item.setAmountHint(null);
                }
            }

            // 5) a row with an empty account name or empty amount is guaranteed to exist
            if ((empty_rows == 0) &&
                ((this.items.size() == accounts) || (this.items.size() == amounts)))
            {
                adapter.addRow();
            }


            debug("submittable", submittable ? "YES" : "NO");
            isSubmittable.setValue(submittable);

            if (BuildConfig.DEBUG) {
                debug("submittable", "== Dump of all items");
                for (int i = 0; i < items.size(); i++) {
                    Item item = items.get(i);
                    LedgerTransactionAccount acc = item.getAccount();
                    debug("submittable", String.format("Item %2d: [%4.2f] %s (%s)", i,
                            acc.isAmountSet() ? acc.getAmount() : 0, acc.getAccountName(),
                            acc.getComment()));
                }
            }
        }
        catch (NumberFormatException e) {
            debug("submittable", "NO (because of NumberFormatException)");
            isSubmittable.setValue(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            debug("submittable", "NO (because of an Exception)");
            isSubmittable.setValue(false);
        }
    }
    public void removeItem(int pos) {
        items.remove(pos);
        accountCount.setValue(getAccountCount());
    }
    public void sendCountNotifications() {
        accountCount.setValue(getAccountCount());
    }
    public void sendFocusedNotification() {
        focusedItem.setValue(focusedItem.getValue());
    }
    public void updateFocusedItem(int position) {
        focusedItem.setValue(position);
    }
    public void noteFocusChanged(int position, FocusedElement element) {
        getItem(position).setFocusedElement(element);
    }
    public void swapItems(int one, int two) {
        Collections.swap(items, one - 1, two - 1);
    }
    public void toggleComment(int position) {
        final MutableLiveData<Boolean> commentVisible = getItem(position).commentVisible;
        commentVisible.postValue(!commentVisible.getValue());
    }
    enum ItemType {generalData, transactionRow, bottomFiller}

    //==========================================================================================

    enum FocusedElement {Account, Comment, Amount}

    class Item {
        private ItemType type;
        private MutableLiveData<Date> date = new MutableLiveData<>();
        private MutableLiveData<String> description = new MutableLiveData<>();
        private LedgerTransactionAccount account;
        private MutableLiveData<String> amountHint = new MutableLiveData<>(null);
        private NewTransactionModel model;
        private MutableLiveData<Boolean> editable = new MutableLiveData<>(true);
        private FocusedElement focusedElement = FocusedElement.Account;
        private MutableLiveData<String> comment = new MutableLiveData<>(null);
        private MutableLiveData<Boolean> commentVisible = new MutableLiveData<>(false);
        public Item(NewTransactionModel model) {
            this.model = model;
            type = ItemType.bottomFiller;
            editable.setValue(false);
        }
        public Item(NewTransactionModel model, Date date, String description) {
            this.model = model;
            this.type = ItemType.generalData;
            this.date.setValue(date);
            this.description.setValue(description);
            this.editable.setValue(true);
        }
        public Item(NewTransactionModel model, LedgerTransactionAccount account) {
            this.model = model;
            this.type = ItemType.transactionRow;
            this.account = account;
            this.editable.setValue(true);
        }
        public FocusedElement getFocusedElement() {
            return focusedElement;
        }
        public void setFocusedElement(FocusedElement focusedElement) {
            this.focusedElement = focusedElement;
        }
        public NewTransactionModel getModel() {
            return model;
        }
        public void setEditable(boolean editable) {
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
        public String getAmountHint() {
            ensureType(ItemType.transactionRow);
            return amountHint.getValue();
        }
        public void setAmountHint(String amountHint) {
            ensureType(ItemType.transactionRow);

            // avoid unnecessary triggers
            if (amountHint == null) {
                if (this.amountHint.getValue() == null)
                    return;
            }
            else {
                if (amountHint.equals(this.amountHint.getValue()))
                    return;
            }

            this.amountHint.setValue(amountHint);
        }
        public void observeAmountHint(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                      @NonNull
                                              androidx.lifecycle.Observer<? super String> observer) {
            this.amountHint.observe(owner, observer);
        }
        public void stopObservingAmountHint(
                @NonNull androidx.lifecycle.Observer<? super String> observer) {
            this.amountHint.removeObserver(observer);
        }
        public ItemType getType() {
            return type;
        }
        public void ensureType(ItemType wantedType) {
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
        public void observeDate(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                @NonNull androidx.lifecycle.Observer<? super Date> observer) {
            this.date.observe(owner, observer);
        }
        public void stopObservingDate(@NonNull androidx.lifecycle.Observer<? super Date> observer) {
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
        public void observeDescription(@NonNull @NotNull androidx.lifecycle.LifecycleOwner owner,
                                       @NonNull
                                               androidx.lifecycle.Observer<? super String> observer) {
            this.description.observe(owner, observer);
        }
        public void stopObservingDescription(
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
        public String getFormattedDate() {
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
        public void observeEditableFlag(NewTransactionActivity activity,
                                        Observer<Boolean> observer) {
            editable.observe(activity, observer);
        }
        public void stopObservingEditableFlag(Observer<Boolean> observer) {
            editable.removeObserver(observer);
        }
        public void observeCommentVisible(NewTransactionActivity activity,
                                          Observer<Boolean> observer) {
            commentVisible.observe(activity, observer);
        }
        public void stopObservingCommentVisible(Observer<Boolean> observer) {
            commentVisible.removeObserver(observer);
        }
        public void observeComment(NewTransactionActivity activity,
                                          Observer<String> observer) {
            comment.observe(activity, observer);
        }
        public void stopObservingComment(Observer<String> observer) {
            comment.removeObserver(observer);
        }
        public void setComment(String comment) {
            getAccount().setComment(comment);
            this.comment.postValue(comment);
        }
    }
}
