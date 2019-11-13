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

import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.ktnx.mobileledger.utils.Logger.debug;
import static net.ktnx.mobileledger.utils.Misc.isZero;

public class NewTransactionModel extends ViewModel {
    static final Pattern reYMD = Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reMD = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reD = Pattern.compile("\\s*(\\d+)\\s*$");
    private final Item header = new Item(this, null, "");
    private final Item trailer = new Item(this);
    private final ArrayList<Item> items = new ArrayList<>();
    private final MutableLiveData<Boolean> isSubmittable = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> focusedItem = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> accountCount = new MutableLiveData<>(0);
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
    // rules:
    // 1) at least two account names
    // 2) each amount must have account name
    // 3) amounts must balance to 0, or
    // 3a) there must be exactly one empty amount
    // 4) empty accounts with empty amounts are ignored
    // 5) a row with an empty account name or empty amount is guaranteed to exist
    @SuppressLint("DefaultLocale")
    public void checkTransactionSubmittable(NewTransactionItemsAdapter adapter) {
        int accounts = 0;
        int accounts_with_values = 0;
        int amounts = 0;
        int amounts_with_accounts = 0;
        int empty_rows = 0;
        Item empty_amount = null;
        boolean single_empty_amount = false;
        boolean single_empty_amount_has_account = false;
        float running_total = 0f;
        final String descriptionText = getDescription();
        final boolean have_description = ((descriptionText != null) && !descriptionText.isEmpty());

        try {
            for (int i = 0; i < this.items.size(); i++) {
                Item item = this.items.get(i);

                LedgerTransactionAccount acc = item.getAccount();
                String acc_name = acc.getAccountName()
                                     .trim();
                if (acc_name.isEmpty()) {
                    empty_rows++;
                }
                else {
                    accounts++;

                    if (acc.isAmountSet()) {
                        accounts_with_values++;
                    }
                }

                if (acc.isAmountSet()) {
                    amounts++;
                    if (!acc_name.isEmpty())
                        amounts_with_accounts++;
                    running_total += acc.getAmount();
                }
                else {
                    if (empty_amount == null) {
                        empty_amount = item;
                        single_empty_amount = true;
                        single_empty_amount_has_account = !acc_name.isEmpty();
                    }
                    else if (!acc_name.isEmpty())
                        single_empty_amount = false;
                }
            }

            if ((empty_rows == 0) &&
                ((this.items.size() == accounts) || (this.items.size() == amounts)))
            {
                adapter.addRow();
            }

            for (NewTransactionModel.Item item : items) {

                final LedgerTransactionAccount acc = item.getAccount();
                if (acc.isAmountSet())
                    continue;

                if (single_empty_amount) {
                    if (item.equals(empty_amount)) {
                        empty_amount.setAmountHint(Misc.isZero(running_total) ? null
                                                                              : String.format(
                                                                                      "%1.2f",
                                                                                      -running_total));
                        continue;
                    }
                }
                else {
                    // no single empty account and this account's amount is not set
                    // => hint should be '0.00'
                    item.setAmountHint(null);
                }

            }

            debug("submittable", String.format(Locale.US,
                    "%s, accounts=%d, accounts_with_values=%s, " +
                    "amounts_with_accounts=%d, amounts=%d, running_total=%1.2f, " +
                    "single_empty_with_acc=%s", have_description ? "description" : "NO description",
                    accounts, accounts_with_values, amounts_with_accounts, amounts, running_total,
                    (single_empty_amount && single_empty_amount_has_account) ? "true" : "false"));

            if (have_description && (accounts >= 2) && (accounts_with_values >= (accounts - 1)) &&
                (amounts_with_accounts == amounts) &&
                (single_empty_amount && single_empty_amount_has_account || isZero(running_total)))
            {
                debug("submittable", "YES");
                isSubmittable.setValue(true);
            }
            else {
                debug("submittable", "NO");
                isSubmittable.setValue(false);
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
    enum ItemType {generalData, transactionRow, bottomFiller}

    class Item extends Object {
        private ItemType type;
        private MutableLiveData<Date> date = new MutableLiveData<>();
        private MutableLiveData<String> description = new MutableLiveData<>();
        private LedgerTransactionAccount account;
        private MutableLiveData<String> amountHint = new MutableLiveData<>();
        private NewTransactionModel model;
        private MutableLiveData<Boolean> editable = new MutableLiveData<>(true);
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
        public NewTransactionModel getModel() {
            return model;
        }
        public boolean isEditable() {
            ensureType(ItemType.transactionRow);
            return this.editable.getValue();
        }
        public void setEditable(boolean editable) {
            ensureType(ItemType.transactionRow);
            this.editable.setValue(editable);
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
                return String.format(Locale.US, "%d/%02d/%02d", myYear, myMonth, myDay);
            }

            if (today.get(Calendar.MONTH) != myMonth) {
                return String.format(Locale.US, "%d/%02d", myMonth, myDay);
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
    }
}
