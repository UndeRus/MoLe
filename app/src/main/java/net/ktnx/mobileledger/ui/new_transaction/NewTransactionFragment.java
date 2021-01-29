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

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.renderscript.RSInvalidStateException;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.QRScanAbleFragment;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnNewTransactionFragmentInteractionListener} interface
 * to handle interaction events.
 */

// TODO: offer to undo account remove-on-swipe

public class NewTransactionFragment extends QRScanAbleFragment {
    private NewTransactionItemsAdapter listAdapter;
    private NewTransactionModel viewModel;
    private FloatingActionButton fab;
    private OnNewTransactionFragmentInteractionListener mListener;
    private MobileLedgerProfile mProfile;
    public NewTransactionFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }
    protected void onQrScanned(String text) {
        Logger.debug("qr", String.format("Got QR scan result [%s]", text));
        Pattern p =
                Pattern.compile("^(\\d+)\\*(\\d+)\\*(\\d+)-(\\d+)-(\\d+)\\*([:\\d]+)\\*([\\d.]+)$");
        Matcher m = p.matcher(text);
        if (m.matches()) {
            float amount = Float.parseFloat(m.group(7));
            viewModel.setDate(
                    new SimpleDate(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)),
                            Integer.parseInt(m.group(5))));

            if (viewModel.accountsInInitialState()) {
                {
                    NewTransactionModel.Item firstItem = viewModel.getItem(1);
                    if (firstItem == null) {
                        viewModel.addAccount(new LedgerTransactionAccount("разход:пазар"));
                        listAdapter.notifyItemInserted(viewModel.items.size() - 1);
                    }
                    else {
                        firstItem.setAccountName("разход:пазар");
                        firstItem.getAccount()
                                 .resetAmount();
                        listAdapter.notifyItemChanged(1);
                    }
                }
                {
                    NewTransactionModel.Item secondItem = viewModel.getItem(2);
                    if (secondItem == null) {
                        viewModel.addAccount(
                                new LedgerTransactionAccount("актив:кеш:дам", -amount, null, null));
                        listAdapter.notifyItemInserted(viewModel.items.size() - 1);
                    }
                    else {
                        secondItem.setAccountName("актив:кеш:дам");
                        secondItem.getAccount()
                                  .setAmount(-amount);
                        listAdapter.notifyItemChanged(2);
                    }
                }
            }
            else {
                viewModel.addAccount(new LedgerTransactionAccount("разход:пазар"));
                viewModel.addAccount(
                        new LedgerTransactionAccount("актив:кеш:дам", -amount, null, null));
                listAdapter.notifyItemRangeInserted(viewModel.items.size() - 1, 2);
            }

            listAdapter.checkTransactionSubmittable();
        }
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final FragmentActivity activity = getActivity();

        inflater.inflate(R.menu.new_transaction_fragment, menu);

        menu.findItem(R.id.scan_qr)
            .setOnMenuItemClickListener(this::onScanQrAction);

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
        if (activity != null)
            viewModel.showCurrency.observe(activity, toggleCurrencyItem::setChecked);

        final MenuItem toggleCommentsItem = menu.findItem(R.id.toggle_comments);
        toggleCommentsItem.setOnMenuItemClickListener(item -> {
            viewModel.toggleShowComments();
            return true;
        });
        if (activity != null)
            viewModel.showComments.observe(activity, toggleCommentsItem::setChecked);
    }
    private boolean onScanQrAction(MenuItem item) {
        try {
            scanQrLauncher.launch(null);
        }
        catch (Exception e) {
            Logger.debug("qr", "Error launching QR scanner", e);
        }

        return true;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_new_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity == null)
            throw new RSInvalidStateException(
                    "getActivity() returned null within onActivityCreated()");

        viewModel = new ViewModelProvider(activity).get(NewTransactionModel.class);
        viewModel.observeDataProfile(this);
        mProfile = Data.getProfile();
        listAdapter = new NewTransactionItemsAdapter(viewModel, mProfile);

        RecyclerView list = activity.findViewById(R.id.new_transaction_accounts);
        list.setAdapter(listAdapter);
        list.setLayoutManager(new LinearLayoutManager(activity));

        Data.observeProfile(getViewLifecycleOwner(), profile -> {
            mProfile = profile;
            listAdapter.setProfile(profile);
        });
        listAdapter.notifyDataSetChanged();
        viewModel.isSubmittable()
                 .observe(getViewLifecycleOwner(), isSubmittable -> {
                     if (isSubmittable) {
                         if (fab != null) {
                             fab.show();
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

                Context context = getContext();
                if (context != null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    final Resources resources = context.getResources();
                    final StringBuilder message = new StringBuilder();
                    message.append(resources.getString(R.string.err_json_send_error_head));
                    message.append("\n\n");
                    message.append(error);
                    if (mProfile.getApiVersion()
                                .equals(API.auto))
                        message.append(
                                resources.getString(R.string.err_json_send_error_unsupported));
                    else {
                        message.append(resources.getString(R.string.err_json_send_error_tail));
                        builder.setPositiveButton(R.string.btn_profile_options, (dialog, which) -> {
                            Logger.debug("error", "will start profile editor");
                            MobileLedgerProfile.startEditProfileActivity(context, mProfile);
                        });
                    }
                    builder.setMessage(message);
                    builder.create()
                           .show();
                }
                else {
                    Snackbar.make(list, error, Snackbar.LENGTH_INDEFINITE)
                            .show();
                }
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

        ProgressBar p = activity.findViewById(R.id.progressBar);
        viewModel.observeBusyFlag(getViewLifecycleOwner(), isBusy -> {
            if (isBusy) {
//                Handler h = new Handler();
//                h.postDelayed(() -> {
//                    if (viewModel.getBusyFlag())
//                        p.setVisibility(View.VISIBLE);
//
//                }, 10);
                p.setVisibility(View.VISIBLE);
            }
            else
                p.setVisibility(View.INVISIBLE);
        });
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("keep", true);
        final int focusedItem = viewModel.getFocusedItem();
        outState.putInt("focused", focusedItem);
    }
    private void onFabPressed() {
        fab.hide();
        Misc.hideSoftKeyboard(this);
        if (mListener != null) {
            SimpleDate date = viewModel.getDate();
            LedgerTransaction tr =
                    new LedgerTransaction(null, date, viewModel.getDescription(), mProfile);

            tr.setComment(viewModel.getComment());
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
