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
import android.content.Intent;
import android.content.res.Resources;
import android.database.AbstractCursor;
import android.os.Bundle;
import android.os.ParcelFormatException;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.PatternAccount;
import net.ktnx.mobileledger.db.PatternHeader;
import net.ktnx.mobileledger.json.API;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.LedgerTransaction;
import net.ktnx.mobileledger.model.LedgerTransactionAccount;
import net.ktnx.mobileledger.model.MobileLedgerProfile;
import net.ktnx.mobileledger.ui.QRScanCapableFragment;
import net.ktnx.mobileledger.ui.patterns.PatternsActivity;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;
import net.ktnx.mobileledger.utils.SimpleDate;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnNewTransactionFragmentInteractionListener} interface
 * to handle interaction events.
 */

// TODO: offer to undo account remove-on-swipe

public class NewTransactionFragment extends QRScanCapableFragment {
    private NewTransactionItemsAdapter listAdapter;
    private NewTransactionModel viewModel;
    private FloatingActionButton fab;
    private OnNewTransactionFragmentInteractionListener mListener;
    private MobileLedgerProfile mProfile;
    public NewTransactionFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }
    private void startNewPatternActivity(String scanned) {
        Intent intent = new Intent(requireContext(), PatternsActivity.class);
        Bundle args = new Bundle();
        args.putString(PatternsActivity.ARG_ADD_PATTERN, scanned);
        requireContext().startActivity(intent, args);
    }
    private void alertNoPatternMatch(String scanned) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setCancelable(true)
               .setMessage(R.string.no_pattern_matches)
               .setPositiveButton(R.string.add_button,
                       (dialog, which) -> startNewPatternActivity(scanned))
               .create()
               .show();
    }
    protected void onQrScanned(String text) {
        Logger.debug("qr", String.format("Got QR scan result [%s]", text));

        if (Misc.emptyIsNull(text) == null)
            return;

        LiveData<List<PatternHeader>> allPatterns = DB.get()
                                                      .getPatternDAO()
                                                      .getPatterns();
        allPatterns.observe(getViewLifecycleOwner(), patternHeaders -> {
            ArrayList<PatternHeader> matchingPatterns = new ArrayList<>();

            for (PatternHeader ph : patternHeaders) {
                String patternSource = ph.getRegularExpression();
                if (Misc.emptyIsNull(patternSource) == null)
                    continue;
                try {
                    Pattern pattern = Pattern.compile(patternSource);
                    Matcher matcher = pattern.matcher(text);
                    if (!matcher.matches())
                        continue;

                    Logger.debug("pattern",
                            String.format("Pattern '%s' [%s] matches '%s'", ph.getName(),
                                    patternSource, text));
                    matchingPatterns.add(ph);
                }
                catch (ParcelFormatException e) {
                    // ignored
                    Logger.debug("pattern",
                            String.format("Error compiling regular expression '%s'", patternSource),
                            e);
                }
            }

            if (matchingPatterns.isEmpty())
                alertNoPatternMatch(text);
            else if (matchingPatterns.size() == 1)
                applyPattern(matchingPatterns.get(0), text);
            else
                choosePattern(matchingPatterns, text);
        });
    }
    private void choosePattern(ArrayList<PatternHeader> matchingPatterns, String matchedText) {
        final String patternNameColumn = "name";
        AbstractCursor cursor = new AbstractCursor() {
            @Override
            public int getCount() {
                return matchingPatterns.size();
            }
            @Override
            public String[] getColumnNames() {
                return new String[]{"_id", patternNameColumn};
            }
            @Override
            public String getString(int column) {
                if (column == 0)
                    return String.valueOf(getPosition());
                return matchingPatterns.get(getPosition())
                                       .getName();
            }
            @Override
            public short getShort(int column) {
                if (column == 0)
                    return (short) getPosition();
                return -1;
            }
            @Override
            public int getInt(int column) {
                return getShort(column);
            }
            @Override
            public long getLong(int column) {
                return getShort(column);
            }
            @Override
            public float getFloat(int column) {
                return getShort(column);
            }
            @Override
            public double getDouble(int column) {
                return getShort(column);
            }
            @Override
            public boolean isNull(int column) {
                return false;
            }
            @Override
            public int getColumnCount() {
                return 2;
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setCancelable(true)
               .setTitle(R.string.choose_pattern_to_apply)
               .setSingleChoiceItems(cursor, -1, patternNameColumn, (dialog, which) -> {
                   applyPattern(matchingPatterns.get(which), matchedText);
                   dialog.dismiss();
               })
               .create()
               .show();
    }
    private void applyPattern(PatternHeader patternHeader, String text) {
        Pattern pattern = Pattern.compile(patternHeader.getRegularExpression());

        Matcher m = pattern.matcher(text);

        if (!m.matches()) {
            Snackbar.make(requireView(), R.string.pattern_does_not_match,
                    BaseTransientBottomBar.LENGTH_INDEFINITE)
                    .show();
            return;
        }

        SimpleDate transactionDate;
        {
            int day = extractIntFromMatches(m, patternHeader.getDateDayMatchGroup(),
                    patternHeader.getDateDay());
            int month = extractIntFromMatches(m, patternHeader.getDateMonthMatchGroup(),
                    patternHeader.getDateMonth());
            int year = extractIntFromMatches(m, patternHeader.getDateYearMatchGroup(),
                    patternHeader.getDateYear());

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

        NewTransactionModel.Item head = viewModel.getItem(0);
        head.ensureType(NewTransactionModel.ItemType.generalData);
        final String transactionDescription =
                extractStringFromMatches(m, patternHeader.getTransactionDescriptionMatchGroup(),
                        patternHeader.getTransactionDescription());
        head.setDescription(transactionDescription);
        Logger.debug("pattern", "Setting transaction description to " + transactionDescription);
        final String transactionComment =
                extractStringFromMatches(m, patternHeader.getTransactionCommentMatchGroup(),
                        patternHeader.getTransactionComment());
        head.setTransactionComment(transactionComment);
        Logger.debug("pattern", "Setting transaction comment to " + transactionComment);
        head.setDate(transactionDate);
        listAdapter.notifyItemChanged(0);

        DB.get()
          .getPatternDAO()
          .getPatternWithAccounts(patternHeader.getId())
          .observe(getViewLifecycleOwner(), entry -> {
              int rowIndex = 0;
              final boolean accountsInInitialState = viewModel.accountsInInitialState();
              for (PatternAccount acc : entry.accounts) {
                  rowIndex++;

                  String accountName = extractStringFromMatches(m, acc.getAccountNameMatchGroup(),
                          acc.getAccountName());
                  String accountComment =
                          extractStringFromMatches(m, acc.getAccountCommentMatchGroup(),
                                  acc.getAccountComment());
                  Float amount =
                          extractFloatFromMatches(m, acc.getAmountMatchGroup(), acc.getAmount());
                  if (amount != null && acc.getNegateAmount() != null && acc.getNegateAmount())
                      amount = -amount;

                  if (accountsInInitialState) {
                      NewTransactionModel.Item item = viewModel.getItem(rowIndex);
                      if (item == null) {
                          Logger.debug("pattern", String.format(Locale.US,
                                  "Adding new account item [%s][c:%s][a:%s]", accountName,
                                  accountComment, amount));
                          final LedgerTransactionAccount ledgerAccount =
                                  new LedgerTransactionAccount(accountName);
                          ledgerAccount.setComment(accountComment);
                          if (amount != null)
                              ledgerAccount.setAmount(amount);
                          // TODO currency
                          viewModel.addAccount(ledgerAccount);
                          listAdapter.notifyItemInserted(viewModel.items.size() - 1);
                      }
                      else {
                          Logger.debug("pattern", String.format(Locale.US,
                                  "Stamping account item #%d [%s][c:%s][a:%s]", rowIndex,
                                  accountName, accountComment, amount));

                          item.setAccountName(accountName);
                          item.setComment(accountComment);
                          if (amount != null)
                              item.getAccount()
                                  .setAmount(amount);

                          listAdapter.notifyItemChanged(rowIndex);
                      }
                  }
                  else {
                      final LedgerTransactionAccount transactionAccount =
                              new LedgerTransactionAccount(accountName);
                      transactionAccount.setComment(accountComment);
                      if (amount != null)
                          transactionAccount.setAmount(amount);
                      // TODO currency
                      Logger.debug("pattern", String.format(Locale.US,
                              "Adding trailing account item [%s][c:%s][a:%s]", accountName,
                              accountComment, amount));

                      viewModel.addAccount(transactionAccount);
                      listAdapter.notifyItemInserted(viewModel.items.size() - 1);
                  }
              }

              listAdapter.checkTransactionSubmittable();
          });
    }
    private int extractIntFromMatches(Matcher m, Integer group, Integer literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 & grp <= m.groupCount())
                try {
                    return Integer.parseInt(m.group(grp));
                }
                catch (NumberFormatException e) {
                    Snackbar.make(requireView(),
                            "Error extracting transaction date: " + e.getMessage(),
                            BaseTransientBottomBar.LENGTH_INDEFINITE)
                            .show();
                }
        }

        return 0;
    }
    private String extractStringFromMatches(Matcher m, Integer group, String literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 & grp <= m.groupCount())
                return m.group(grp);
        }

        return null;
    }
    private Float extractFloatFromMatches(Matcher m, Integer group, Float literal) {
        if (literal != null)
            return literal;

        if (group != null) {
            int grp = group;
            if (grp > 0 & grp <= m.groupCount())
                try {
                    return Float.valueOf(m.group(grp));
                }
                catch (NumberFormatException e) {
                    Snackbar.make(requireView(),
                            "Error extracting transaction amount: " + e.getMessage(),
                            BaseTransientBottomBar.LENGTH_INDEFINITE)
                            .show();
                }
        }

        return null;
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

        fab = activity.findViewById(R.id.fabAdd);
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
