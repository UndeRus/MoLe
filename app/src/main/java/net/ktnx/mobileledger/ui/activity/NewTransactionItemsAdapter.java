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
import android.app.Activity;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.BuildConfig;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.async.DescriptionSelectedCallback;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.MLDB;
import net.ktnx.mobileledger.utils.Misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static net.ktnx.mobileledger.utils.Logger.debug;

class NewTransactionItemsAdapter extends RecyclerView.Adapter<NewTransactionItemHolder>
        implements DescriptionSelectedCallback {
    private NewTransactionModel model;
    private MobileLedgerProfile mProfile;
    private ItemTouchHelper touchHelper;
    private RecyclerView recyclerView;
    private int checkHoldCounter = 0;
    NewTransactionItemsAdapter(NewTransactionModel viewModel, MobileLedgerProfile profile) {
        super();
        model = viewModel;
        mProfile = profile;
        int size = model.getAccountCount();
        while (size < 2) {
            Logger.debug("new-transaction",
                    String.format(Locale.US, "%d accounts is too little, Calling addRow()", size));
            size = addRow();
        }

        NewTransactionItemsAdapter adapter = this;

        touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
            @Override
            public boolean canDropOver(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.ViewHolder current,
                                       @NonNull RecyclerView.ViewHolder target) {
                final int adapterPosition = target.getAdapterPosition();

                // first and last items are immovable
                if (adapterPosition == 0)
                    return false;
                if (adapterPosition == adapter.getItemCount() - 1)
                    return false;

                return super.canDropOver(recyclerView, current, target);
            }
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int flags = makeFlag(ItemTouchHelper.ACTION_STATE_IDLE, ItemTouchHelper.END);
                // the top (date and description) and the bottom (padding) items are always there
                final int adapterPosition = viewHolder.getAdapterPosition();
                if ((adapterPosition > 0) && (adapterPosition < adapter.getItemCount() - 1)) {
                    flags |= makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN) |
                             makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE,
                                     ItemTouchHelper.START | ItemTouchHelper.END);
                }

                return flags;
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                model.swapItems(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                viewModel.removeItem(pos - 1);
                notifyItemRemoved(pos);
                viewModel.sendCountNotifications(); // needed after items re-arrangement
                checkTransactionSubmittable();
            }
        });
    }
    public void setProfile(MobileLedgerProfile profile) {
        mProfile = profile;
    }
    private int addRow() {
        return addRow(null);
    }
    private int addRow(String commodity) {
        final int newAccountCount = model.addAccount(new LedgerTransactionAccount("", commodity));
        Logger.debug("new-transaction",
                String.format(Locale.US, "invoking notifyItemInserted(%d)", newAccountCount));
        // the header is at position 0
        notifyItemInserted(newAccountCount);
        model.sendCountNotifications(); // needed after holders' positions have changed
        return newAccountCount;
    }
    @NonNull
    @Override
    public NewTransactionItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout row = (LinearLayout) LayoutInflater.from(parent.getContext())
                                                        .inflate(R.layout.new_transaction_row,
                                                                parent, false);

        return new NewTransactionItemHolder(row, this);
    }
    @Override
    public void onBindViewHolder(@NonNull NewTransactionItemHolder holder, int position) {
        Logger.debug("bind", String.format(Locale.US, "Binding item at position %d", position));
        NewTransactionModel.Item item = model.getItem(position);
        holder.setData(item);
        Logger.debug("bind", String.format(Locale.US, "Bound %s item at position %d", item.getType()
                                                                                          .toString(),
                position));
    }
    @Override
    public int getItemCount() {
        return model.getAccountCount() + 2;
    }
    private boolean accountListIsEmpty() {
        for (int i = 0; i < model.getAccountCount(); i++) {
            LedgerTransactionAccount acc = model.getAccount(i);
            if (!acc.getAccountName()
                    .isEmpty())
                return false;
            if (acc.isAmountSet())
                return false;
        }

        return true;
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        touchHelper.attachToRecyclerView(recyclerView);
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        touchHelper.attachToRecyclerView(null);
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }
    public void descriptionSelected(String description) {
        debug("descr selected", description);
        if (!accountListIsEmpty())
            return;

        String accFilter = mProfile.getPreferredAccountsFilter();

        ArrayList<String> params = new ArrayList<>();
        StringBuilder sb = new StringBuilder("select t.profile, t.id from transactions t");

        if (!Misc.isEmptyOrNull(accFilter)) {
            sb.append(" JOIN transaction_accounts ta")
              .append(" ON ta.profile = t.profile")
              .append(" AND ta.transaction_id = t.id");
        }

        sb.append(" WHERE t.description=?");
        params.add(description);

        if (!Misc.isEmptyOrNull(accFilter)) {
            sb.append(" AND ta.account_name LIKE '%'||?||'%'");
            params.add(accFilter);
        }

        sb.append(" ORDER BY t.date DESC LIMIT 1");

        final String sql = sb.toString();
        debug("descr", sql);
        debug("descr", params.toString());

        Activity activity = (Activity) recyclerView.getContext();
        // FIXME: handle exceptions?
        MLDB.queryInBackground(sql, params.toArray(new String[]{}), new MLDB.CallbackHelper() {
            @Override
            public void onStart() {
                model.incrementBusyCounter();
            }
            @Override
            public void onDone() {
                model.decrementBusyCounter();
            }
            @Override
            public boolean onRow(@NonNull Cursor cursor) {
                final String profileUUID = cursor.getString(0);
                final int transactionId = cursor.getInt(1);
                activity.runOnUiThread(() -> loadTransactionIntoModel(profileUUID, transactionId));
                return false; // limit 1, by the way
            }
            @Override
            public void onNoRows() {
                if (Misc.isEmptyOrNull(accFilter))
                    return;

                debug("descr", "Trying transaction search without preferred account filter");

                final String broaderSql =
                        "select t.profile, t.id from transactions t where t.description=?" +
                        " ORDER BY date desc LIMIT 1";
                params.remove(1);
                debug("descr", broaderSql);
                debug("descr", description);

                MLDB.queryInBackground(broaderSql, new String[]{description},
                        new MLDB.CallbackHelper() {
                            @Override
                            public void onStart() {
                                model.incrementBusyCounter();
                            }
                            @Override
                            public boolean onRow(@NonNull Cursor cursor) {
                                final String profileUUID = cursor.getString(0);
                                final int transactionId = cursor.getInt(1);
                                activity.runOnUiThread(
                                        () -> loadTransactionIntoModel(profileUUID, transactionId));
                                return false;
                            }
                            @Override
                            public void onDone() {
                                model.decrementBusyCounter();
                            }
                        });
            }
        });
    }
    private void loadTransactionIntoModel(String profileUUID, int transactionId) {
        LedgerTransaction tr;
        MobileLedgerProfile profile = Data.getProfile(profileUUID);
        if (profile == null)
            throw new RuntimeException(String.format(
                    "Unable to find profile %s, which is supposed to contain transaction %d",
                    profileUUID, transactionId));

        tr = profile.loadTransaction(transactionId);
        ArrayList<LedgerTransactionAccount> accounts = tr.getAccounts();
        NewTransactionModel.Item firstNegative = null;
        NewTransactionModel.Item firstPositive = null;
        int singleNegativeIndex = -1;
        int singlePositiveIndex = -1;
        int negativeCount = 0;
        for (int i = 0; i < accounts.size(); i++) {
            LedgerTransactionAccount acc = accounts.get(i);
            NewTransactionModel.Item item;
            if (model.getAccountCount() < i + 1) {
                model.addAccount(acc);
                notifyItemInserted(i + 1);
            }
            item = model.getItem(i + 1);

            item.getAccount()
                .setAccountName(acc.getAccountName());
            item.setComment(acc.getComment());
            if (acc.isAmountSet()) {
                item.getAccount()
                    .setAmount(acc.getAmount());
                if (acc.getAmount() < 0) {
                    if (firstNegative == null) {
                        firstNegative = item;
                        singleNegativeIndex = i;
                    }
                    else
                        singleNegativeIndex = -1;
                }
                else {
                    if (firstPositive == null) {
                        firstPositive = item;
                        singlePositiveIndex = i;
                    }
                    else
                        singlePositiveIndex = -1;
                }
            }
            else
                item.getAccount()
                    .resetAmount();
            notifyItemChanged(i + 1);
        }

        if (singleNegativeIndex != -1) {
            firstNegative.getAccount()
                         .resetAmount();
            model.moveItemLast(singleNegativeIndex);
        }
        else if (singlePositiveIndex != -1) {
            firstPositive.getAccount()
                         .resetAmount();
            model.moveItemLast(singlePositiveIndex);
        }

        checkTransactionSubmittable();
        model.setFocusedItem(1);
    }
    public void toggleAllEditing(boolean editable) {
        // item 0 is the header
        for (int i = 0; i <= model.getAccountCount(); i++) {
            model.getItem(i)
                 .setEditable(editable);
            notifyItemChanged(i);
            // TODO perhaps do only one notification about the whole range (notifyDatasetChanged)?
        }
    }
    void reset() {
        int presentItemCount = model.getAccountCount();
        model.reset();
        notifyItemChanged(0);       // header changed
        notifyItemRangeChanged(1, 2);    // the two empty rows
        if (presentItemCount > 2)
            notifyItemRangeRemoved(3, presentItemCount - 2); // all the rest are gone
    }
    void updateFocusedItem(int position) {
        model.updateFocusedItem(position);
    }
    void noteFocusIsOnAccount(int position) {
        model.noteFocusChanged(position, NewTransactionModel.FocusedElement.Account);
    }
    void noteFocusIsOnAmount(int position) {
        model.noteFocusChanged(position, NewTransactionModel.FocusedElement.Amount);
    }
    void noteFocusIsOnComment(int position) {
        model.noteFocusChanged(position, NewTransactionModel.FocusedElement.Comment);
    }
    private void holdSubmittableChecks() {
        checkHoldCounter++;
    }
    private void releaseSubmittableChecks() {
        if (checkHoldCounter == 0)
            throw new RuntimeException("Asymmetrical call to releaseSubmittableChecks");
        checkHoldCounter--;
    }
    void setItemCurrency(NewTransactionModel.Item item, Currency newCurrency) {
        Currency oldCurrency = item.getCurrency();
        if (!Currency.equal(newCurrency, oldCurrency)) {
            holdSubmittableChecks();
            try {
                item.setCurrency(newCurrency);
//                for (Item i : items) {
//                    if (Currency.equal(i.getCurrency(), oldCurrency))
//                        i.setCurrency(newCurrency);
//                }
            }
            finally {
                releaseSubmittableChecks();
            }

            checkTransactionSubmittable();
        }
    }
    /*
         A transaction is submittable if:
         0) has description
         1) has at least two account names
         2) each row with amount has account name
         3) for each commodity:
         3a) amounts must balance to 0, or
         3b) there must be exactly one empty amount (with account)
         4) empty accounts with empty amounts are ignored
         Side effects:
         5) a row with an empty account name or empty amount is guaranteed to exist for each
         commodity
         6) at least two rows need to be present in the ledger

        */
    @SuppressLint("DefaultLocale")
    void checkTransactionSubmittable() {
        if (checkHoldCounter > 0)
            return;

        int accounts = 0;
        final BalanceForCurrency balance = new BalanceForCurrency();
        final String descriptionText = model.getDescription();
        boolean submittable = true;
        final ItemsForCurrency itemsForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithEmptyAmountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithAccountAndEmptyAmountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithEmptyAccountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithAmountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency itemsWithAccountForCurrency = new ItemsForCurrency();
        final ItemsForCurrency emptyRowsForCurrency = new ItemsForCurrency();
        final List<NewTransactionModel.Item> emptyRows = new ArrayList<>();

        try {
            if ((descriptionText == null) || descriptionText.trim()
                                                            .isEmpty())
            {
                Logger.debug("submittable", "Transaction not submittable: missing description");
                submittable = false;
            }

            for (int i = 0; i < model.items.size(); i++) {
                NewTransactionModel.Item item = model.items.get(i);

                LedgerTransactionAccount acc = item.getAccount();
                String acc_name = acc.getAccountName()
                                     .trim();
                String currName = acc.getCurrency();

                itemsForCurrency.add(currName, item);

                if (acc_name.isEmpty()) {
                    itemsWithEmptyAccountForCurrency.add(currName, item);

                    if (acc.isAmountSet()) {
                        // 2) each amount has account name
                        Logger.debug("submittable", String.format(
                                "Transaction not submittable: row %d has no account name, but" +
                                " has" + " amount %1.2f", i + 1, acc.getAmount()));
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

                if (acc.isAmountSet()) {
                    itemsWithAmountForCurrency.add(currName, item);
                    balance.add(currName, acc.getAmount());
                }
                else {
                    itemsWithEmptyAmountForCurrency.add(currName, item);

                    if (!acc_name.isEmpty())
                        itemsWithAccountAndEmptyAmountForCurrency.add(currName, item);
                }
            }

            // 1) has at least two account names
            if (accounts < 2) {
                if (accounts == 0)
                    Logger.debug("submittable",
                            "Transaction not submittable: no account " + "names");
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
                    for (NewTransactionModel.Item item : model.items) {
                        if (Currency.equal(item.getCurrency(), balCurrency))
                            item.setAmountHint(null);
                    }
                }
                else {
                    List<NewTransactionModel.Item> list =
                            itemsWithAccountAndEmptyAmountForCurrency.getList(balCurrency);
                    int balanceReceiversCount = list.size();
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

                    List<NewTransactionModel.Item> emptyAmountList =
                            itemsWithEmptyAmountForCurrency.getList(balCurrency);

                    // suggest off-balance amount to a row and remove hints on other rows
                    NewTransactionModel.Item receiver = null;
                    if (!list.isEmpty())
                        receiver = list.get(0);
                    else if (!emptyAmountList.isEmpty())
                        receiver = emptyAmountList.get(0);

                    for (NewTransactionModel.Item item : model.items) {
                        if (!Currency.equal(item.getCurrency(), balCurrency))
                            continue;

                        if (item.equals(receiver)) {
                            if (BuildConfig.DEBUG)
                                Logger.debug("submittable",
                                        String.format("Setting amount hint to %1.2f [%s]",
                                                -currencyBalance, balCurrency));
                            item.setAmountHint(String.format("%1.2f", -currencyBalance));
                        }
                        else {
                            if (BuildConfig.DEBUG)
                                Logger.debug("submittable",
                                        String.format("Resetting hint of '%s' [%s]",
                                                (item.getAccount() == null) ? "" : item.getAccount()
                                                                                       .getAccountName(),
                                                balCurrency));
                            item.setAmountHint(null);
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
                    addRow(balCurrency);
                }
            }

            // drop extra empty rows, not needed
            for (String currName : emptyRowsForCurrency.currencies()) {
                List<NewTransactionModel.Item> emptyItems = emptyRowsForCurrency.getList(currName);
                while ((model.items.size() > 2) && (emptyItems.size() > 1)) {
                    NewTransactionModel.Item item = emptyItems.get(1);
                    emptyItems.remove(1);
                    model.removeRow(item, this);
                }

                // unused currency, remove last item (which is also an empty one)
                if ((model.items.size() > 2) && (emptyItems.size() == 1)) {
                    List<NewTransactionModel.Item> currItems = itemsForCurrency.getList(currName);

                    if (currItems.size() == 1) {
                        NewTransactionModel.Item item = emptyItems.get(0);
                        model.removeRow(item, this);
                    }
                }
            }

            // 6) at least two rows need to be present in the ledger
            while (model.items.size() < 2)
                addRow();


            debug("submittable", submittable ? "YES" : "NO");
            model.isSubmittable.setValue(submittable);

            if (BuildConfig.DEBUG) {
                debug("submittable", "== Dump of all items");
                for (int i = 0; i < model.items.size(); i++) {
                    NewTransactionModel.Item item = model.items.get(i);
                    LedgerTransactionAccount acc = item.getAccount();
                    debug("submittable", String.format("Item %2d: [%4.2f(%s) %s] %s ; %s", i,
                            acc.isAmountSet() ? acc.getAmount() : 0,
                            item.isAmountHintSet() ? item.getAmountHint() : "ø", acc.getCurrency(),
                            acc.getAccountName(), acc.getComment()));
                }
            }
        }
        catch (NumberFormatException e) {
            debug("submittable", "NO (because of NumberFormatException)");
            model.isSubmittable.setValue(false);
        }
        catch (Exception e) {
            e.printStackTrace();
            debug("submittable", "NO (because of an Exception)");
            model.isSubmittable.setValue(false);
        }
    }

    private static class BalanceForCurrency {
        private HashMap<String, Float> hashMap = new HashMap<>();
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
        private HashMap<String, List<NewTransactionModel.Item>> hashMap = new HashMap<>();
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
