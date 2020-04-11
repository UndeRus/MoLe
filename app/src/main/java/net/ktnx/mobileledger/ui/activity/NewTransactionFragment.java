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

import android.content.Context;
import android.os.Bundle;
import android.renderscript.RSInvalidStateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnNewTransactionFragmentInteractionListener} interface
 * to handle interaction events.
 */

// TODO: offer to undo account remove-on-swipe

public class NewTransactionFragment extends Fragment {
    private NewTransactionItemsAdapter listAdapter;
    private NewTransactionModel viewModel;
    private RecyclerView list;
    private FloatingActionButton fab;
    private OnNewTransactionFragmentInteractionListener mListener;
    private MobileLedgerProfile mProfile;
    public NewTransactionFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.new_transaction_fragment, menu);
        menu.findItem(R.id.action_reset_new_transaction_activity)
            .setOnMenuItemClickListener(item -> {
                listAdapter.reset();
                return true;
            });
        final MenuItem toggleCurrencyItem = menu.findItem(R.id.toggle_currency);
        toggleCurrencyItem.setOnMenuItemClickListener(item -> {
            viewModel.toggleCurrencyVisible();
            return true;
        });
        final FragmentActivity activity = getActivity();
        if (activity != null)
            viewModel.showCurrency.observe(activity, toggleCurrencyItem::setChecked);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_transaction, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity == null)
            throw new RSInvalidStateException(
                    "getActivity() returned null within onActivityCreated()");

        list = activity.findViewById(R.id.new_transaction_accounts);
        viewModel = new ViewModelProvider(activity).get(NewTransactionModel.class);
        viewModel.observeDataProfile(this);
        mProfile = Data.profile.getValue();
        listAdapter = new NewTransactionItemsAdapter(viewModel, mProfile);
        list.setAdapter(listAdapter);
        list.setLayoutManager(new LinearLayoutManager(activity));
        Data.profile.observe(getViewLifecycleOwner(), profile -> {
            mProfile = profile;
            listAdapter.setProfile(profile);
        });
        listAdapter.notifyDataSetChanged();
        viewModel.isSubmittable()
                 .observe(getViewLifecycleOwner(), isSubmittable -> {
                     if (isSubmittable) {
                         if (fab != null) {
                             fab.show();
                             fab.setEnabled(true);
                         }
                     }
                     else {
                         if (fab != null) {
                             fab.hide();
                         }
                     }
                 });
//        viewModel.checkTransactionSubmittable(listAdapter);

        fab = activity.findViewById(R.id.fab);
        fab.setOnClickListener(v -> onFabPressed());

        boolean keep = false;

        Bundle args = getArguments();
        if (args != null) {
            String error = args.getString("error");
            if (error != null) {
                Logger.debug("new-trans-f", String.format("Got error: %s", error));
                Snackbar.make(list, error, Snackbar.LENGTH_LONG)
                        .show();
                keep = true;
            }
        }

        int focused = 0;
        if (savedInstanceState != null) {
            keep |= savedInstanceState.getBoolean("keep", true);
            focused = savedInstanceState.getInt("focused", 0);
        }

        if (!keep)
            viewModel.reset();
        else {
            viewModel.setFocusedItem(focused);
        }
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("keep", true);
        final int focusedItem = viewModel.getFocusedItem();
        outState.putInt("focused", focusedItem);
    }
    private void onFabPressed() {
        fab.setEnabled(false);
        Misc.hideSoftKeyboard(this);
        if (mListener != null) {
            Date date = viewModel.getDate();
            LedgerTransaction tr =
                    new LedgerTransaction(null, date, viewModel.getDescription(), mProfile);

            LedgerTransactionAccount emptyAmountAccount = null;
            float emptyAmountAccountBalance = 0;
            for (int i = 0; i < viewModel.getAccountCount(); i++) {
                LedgerTransactionAccount acc =
                        new LedgerTransactionAccount(viewModel.getAccount(i));
                if (acc.getAccountName()
                       .trim()
                       .isEmpty())
                    continue;

                if (acc.isAmountSet()) {
                    emptyAmountAccountBalance += acc.getAmount();
                }
                else {
                    emptyAmountAccount = acc;
                }

                tr.addAccount(acc);
            }

            if (emptyAmountAccount != null)
                emptyAmountAccount.setAmount(-emptyAmountAccountBalance);

            mListener.onTransactionSave(tr);
        }
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (context instanceof OnNewTransactionFragmentInteractionListener) {
            mListener = (OnNewTransactionFragmentInteractionListener) context;
        }
        else {
            throw new RuntimeException(
                    context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnNewTransactionFragmentInteractionListener {
        void onTransactionSave(LedgerTransaction tr);
    }
}
