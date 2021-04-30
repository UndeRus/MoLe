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

package net.ktnx.mobileledger.ui.account_summary;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.ktnx.mobileledger.databinding.AccountSummaryFragmentBinding;
import net.ktnx.mobileledger.db.AccountWithAmounts;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.AccountListItem;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerAccount;
import net.ktnx.mobileledger.ui.FabManager;
import net.ktnx.mobileledger.ui.MainModel;
import net.ktnx.mobileledger.ui.MobileLedgerListFragment;
import net.ktnx.mobileledger.ui.activity.MainActivity;
import net.ktnx.mobileledger.utils.Colors;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class AccountSummaryFragment extends MobileLedgerListFragment {
    public AccountSummaryAdapter modelAdapter;
    private AccountSummaryFragmentBinding b;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        debug("flow", "AccountSummaryFragment.onCreate()");
        setHasOptionsMenu(true);
    }
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        debug("flow", "AccountSummaryFragment.onAttach()");
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        debug("flow", "AccountSummaryFragment.onCreateView()");
        b = AccountSummaryFragmentBinding.inflate(inflater, container, false);
        return b.getRoot();
    }
    @Override
    public SwipeRefreshLayout getRefreshLayout() {
        return b.accountSwipeRefreshLayout;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        debug("flow", "AccountSummaryFragment.onActivityCreated()");
        super.onViewCreated(view, savedInstanceState);

        MainModel model = new ViewModelProvider(requireActivity()).get(MainModel.class);

        Data.backgroundTasksRunning.observe(this.getViewLifecycleOwner(),
                this::onBackgroundTaskRunningChanged);

        modelAdapter = new AccountSummaryAdapter();
        MainActivity mainActivity = getMainActivity();

        LinearLayoutManager llm = new LinearLayoutManager(mainActivity);
        llm.setOrientation(RecyclerView.VERTICAL);
        b.accountRoot.setLayoutManager(llm);
        b.accountRoot.setAdapter(modelAdapter);
        DividerItemDecoration did =
                new DividerItemDecoration(mainActivity, DividerItemDecoration.VERTICAL);
        b.accountRoot.addItemDecoration(did);

        mainActivity.fabShouldShow();

        if (mainActivity instanceof FabManager.FabHandler)
            FabManager.handle(mainActivity, b.accountRoot);

        Colors.themeWatch.observe(getViewLifecycleOwner(), this::themeChanged);
        b.accountSwipeRefreshLayout.setOnRefreshListener(() -> {
            debug("ui", "refreshing accounts via swipe");
            model.scheduleTransactionListRetrieval();
        });

        Data.observeProfile(this, this::onProfileChanged);
    }
    private void onProfileChanged(Profile profile) {
        if (profile == null)
            return;

        DB.get()
          .getAccountDAO()
          .getAllWithAmounts(profile.getId())
          .observe(getViewLifecycleOwner(), list -> AsyncTask.execute(() -> {
              List<AccountListItem> adapterList = new ArrayList<>();
              adapterList.add(new AccountListItem.Header(Data.lastAccountsUpdateText));
              HashMap<String, LedgerAccount> accMap = new HashMap<>();
              for (AccountWithAmounts dbAcc : list) {
                  LedgerAccount parent = null;
                  String parentName = dbAcc.account.getParentName();
                  if (parentName != null)
                      parent = accMap.get(parentName);
                  if (parent != null)
                      parent.setHasSubAccounts(true);
                  final LedgerAccount account = LedgerAccount.fromDBO(dbAcc, parent);
                  if (account.isVisible())
                      adapterList.add(new AccountListItem.Account(account));
                  accMap.put(dbAcc.account.getName(), account);
              }
              modelAdapter.setAccounts(adapterList);
              Data.lastUpdateAccountCount.postValue(adapterList.size() - 1);
          }));
    }
}
