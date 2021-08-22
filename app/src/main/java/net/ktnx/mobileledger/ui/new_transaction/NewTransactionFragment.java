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

import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.ui.FabManager;
import net.ktnx.mobileledger.ui.QR;
import net.ktnx.mobileledger.ui.profiles.ProfileDetailActivity;
import net.ktnx.mobileledger.utils.Logger;

import org.jetbrains.annotations.NotNull;

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
    private OnNewTransactionFragmentInteractionListener mListener;
    private Profile mProfile;
    public NewTransactionFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
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
                viewModel.reset();
                return true;
            });

        final MenuItem toggleCurrencyItem = menu.findItem(R.id.toggle_currency);
        toggleCurrencyItem.setOnMenuItemClickListener(item -> {
            viewModel.toggleCurrencyVisible();
            return true;
        });
        if (activity != null)
            viewModel.getShowCurrency()
                     .observe(activity, toggleCurrencyItem::setChecked);

        final MenuItem toggleCommentsItem = menu.findItem(R.id.toggle_comments);
        toggleCommentsItem.setOnMenuItemClickListener(item -> {
            viewModel.toggleShowComments();
            return true;
        });
        if (activity != null)
            viewModel.getShowComments()
                     .observe(activity, toggleCommentsItem::setChecked);
    }
    private boolean onScanQrAction(MenuItem item) {
        try {
            Context ctx = requireContext();
            if (ctx instanceof QR.QRScanTrigger)
                ((QR.QRScanTrigger) ctx).triggerQRScan();
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

        viewModel.getItems()
                 .observe(getViewLifecycleOwner(), newList -> listAdapter.setItems(newList));

        RecyclerView list = activity.findViewById(R.id.new_transaction_accounts);
        list.setAdapter(listAdapter);
        list.setLayoutManager(new LinearLayoutManager(activity));

        Data.observeProfile(getViewLifecycleOwner(), profile -> {
            mProfile = profile;
            listAdapter.setProfile(profile);
        });
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
                    message.append(resources.getString(R.string.err_json_send_error_head))
                           .append("\n\n")
                           .append(error)
                           .append("\n\n");
                    if (API.valueOf(mProfile.getApiVersion())
                           .equals(API.auto))
                        message.append(
                                resources.getString(R.string.err_json_send_error_unsupported));
                    else {
                        message.append(resources.getString(R.string.err_json_send_error_tail));
                        builder.setPositiveButton(R.string.btn_profile_options, (dialog, which) -> {
                            Logger.debug("error", "will start profile editor");
                            ProfileDetailActivity.start(context, mProfile);
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
        FocusedElement element = null;
        if (savedInstanceState != null) {
            keep |= savedInstanceState.getBoolean("keep", true);
            focused = savedInstanceState.getInt("focused-item", 0);
            element = FocusedElement.valueOf(savedInstanceState.getString("focused-element"));
        }

        if (!keep) {
            // we need the DB up and running
            Data.observeProfile(getViewLifecycleOwner(), p -> {
                if (p != null)
                    viewModel.reset();
            });
        }
        else {
            viewModel.noteFocusChanged(focused, element);
        }

        ProgressBar p = activity.findViewById(R.id.progressBar);
        viewModel.getBusyFlag()
                 .observe(getViewLifecycleOwner(), isBusy -> {
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

        if (activity instanceof FabManager.FabHandler)
            FabManager.handle((FabManager.FabHandler) activity, list);
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("keep", true);
        final NewTransactionModel.FocusInfo focusInfo = viewModel.getFocusInfo()
                                                                 .getValue();
        final int focusedItem = focusInfo.position;
        if (focusedItem >= 0)
            outState.putInt("focused-item", focusedItem);
        outState.putString("focused-element", focusInfo.element.toString());
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
