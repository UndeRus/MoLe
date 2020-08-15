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

package net.ktnx.mobileledger.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.App;
import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnCurrencySelectedListener}
 * interface.
 */
public class CurrencySelectorFragment extends AppCompatDialogFragment
        implements OnCurrencySelectedListener, OnCurrencyLongClickListener {

    public static final int DEFAULT_COLUMN_COUNT = 2;
    public static final String ARG_COLUMN_COUNT = "column-count";
    public static final String ARG_SHOW_PARAMS = "show-params";
    public static final boolean DEFAULT_SHOW_PARAMS = true;
    private int mColumnCount = DEFAULT_COLUMN_COUNT;
    private CurrencySelectorModel model;
    private boolean deferredShowPositionAndPadding;
    private OnCurrencySelectedListener onCurrencySelectedListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CurrencySelectorFragment() {
    }
    @SuppressWarnings("unused")
    public static CurrencySelectorFragment newInstance() {
        return newInstance(DEFAULT_COLUMN_COUNT, DEFAULT_SHOW_PARAMS);
    }
    public static CurrencySelectorFragment newInstance(int columnCount, boolean showParams) {
        CurrencySelectorFragment fragment = new CurrencySelectorFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putBoolean(ARG_SHOW_PARAMS, showParams);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT, DEFAULT_COLUMN_COUNT);
        }
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context context = requireContext();
        Dialog csd = new Dialog(context);
        csd.setContentView(R.layout.fragment_currency_selector_list);
        csd.setTitle(R.string.choose_currency_label);

        RecyclerView recyclerView = csd.findViewById(R.id.list);

        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        }
        else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        model = new ViewModelProvider(this).get(CurrencySelectorModel.class);
        if (onCurrencySelectedListener != null)
            model.setOnCurrencySelectedListener(onCurrencySelectedListener);
        MobileLedgerProfile profile = Objects.requireNonNull(Data.getProfile());

        model.currencies.setValue(new CopyOnWriteArrayList<>(profile.getCurrencies()));
        CurrencySelectorRecyclerViewAdapter adapter = new CurrencySelectorRecyclerViewAdapter();
        model.currencies.observe(this, adapter::submitList);

        recyclerView.setAdapter(adapter);
        adapter.setCurrencySelectedListener(this);
        adapter.setCurrencyLongClickListener(this);

        final TextView tvNewCurrName = csd.findViewById(R.id.new_currency_name);
        final TextView tvNoCurrBtn = csd.findViewById(R.id.btn_no_currency);
        final TextView tvAddCurrOkBtn = csd.findViewById(R.id.btn_add_currency);
        final TextView tvAddCurrBtn = csd.findViewById(R.id.btn_add_new);

        tvNewCurrName.setVisibility(View.GONE);
        tvAddCurrOkBtn.setVisibility(View.GONE);
        tvNoCurrBtn.setVisibility(View.VISIBLE);
        tvAddCurrBtn.setVisibility(View.VISIBLE);

        tvAddCurrBtn.setOnClickListener(v -> {
            tvNewCurrName.setVisibility(View.VISIBLE);
            tvAddCurrOkBtn.setVisibility(View.VISIBLE);

            tvNoCurrBtn.setVisibility(View.GONE);
            tvAddCurrBtn.setVisibility(View.GONE);

            tvNewCurrName.setText(null);
            tvNewCurrName.requestFocus();
            net.ktnx.mobileledger.utils.Misc.showSoftKeyboard(this);
        });

        tvAddCurrOkBtn.setOnClickListener(v -> {


            String currName = String.valueOf(tvNewCurrName.getText());
            if (!currName.isEmpty()) {
                List<Currency> list = new ArrayList<>(model.currencies.getValue());
                // FIXME hardcoded position and gap setting
                list.add(new Currency(profile, String.valueOf(tvNewCurrName.getText()),
                        Currency.Position.after, false));
                model.currencies.setValue(list);
            }

            tvNewCurrName.setVisibility(View.GONE);
            tvAddCurrOkBtn.setVisibility(View.GONE);

            tvNoCurrBtn.setVisibility(View.VISIBLE);
            tvAddCurrBtn.setVisibility(View.VISIBLE);
        });

        tvNoCurrBtn.setOnClickListener(v -> {
            adapter.notifyCurrencySelected(null);
            dismiss();
        });

        RadioButton rbPositionLeft = csd.findViewById(R.id.currency_position_left);
        RadioButton rbPositionRight = csd.findViewById(R.id.currency_position_right);

        if (Data.currencySymbolPosition.getValue() == Currency.Position.before)
            rbPositionLeft.toggle();
        else
            rbPositionRight.toggle();

        RadioGroup rgPosition = csd.findViewById(R.id.position_radio_group);
        rgPosition.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.currency_position_left)
                Data.currencySymbolPosition.setValue(Currency.Position.before);
            else
                Data.currencySymbolPosition.setValue(Currency.Position.after);
        });

        Switch gap = csd.findViewById(R.id.currency_gap);

        gap.setChecked(Data.currencyGap.getValue());

        gap.setOnCheckedChangeListener((v, checked) -> Data.currencyGap.setValue(checked));

        model.observePositionAndPaddingVisible(this, visible -> csd.findViewById(R.id.params_panel)
                                                                   .setVisibility(
                                                                           visible ? View.VISIBLE
                                                                                   : View.GONE));

        if ((savedInstanceState != null) ? savedInstanceState.getBoolean(ARG_SHOW_PARAMS,
                DEFAULT_SHOW_PARAMS) : DEFAULT_SHOW_PARAMS)
            model.showPositionAndPadding();
        else
            model.hidePositionAndPadding();

        return csd;
    }
    public void setOnCurrencySelectedListener(OnCurrencySelectedListener listener) {
        onCurrencySelectedListener = listener;

        if (model != null)
            model.setOnCurrencySelectedListener(listener);
    }
    public void resetOnCurrencySelectedListener() {
        model.resetOnCurrencySelectedListener();
    }
    @Override
    public void onCurrencySelected(Currency item) {
        model.triggerOnCurrencySelectedListener(item);

        dismiss();
    }

    @Override
    public void onCurrencyLongClick(Currency item) {
        ArrayList<Currency> list = new ArrayList<>(model.currencies.getValue());
        App.getDatabase()
           .execSQL("delete from currencies where id=?", new Object[]{item.getId()});
        list.remove(item);
        model.currencies.setValue(list);
    }
    public void showPositionAndPadding() {
        deferredShowPositionAndPadding = true;
    }
    public void hidePositionAndPadding() {
        deferredShowPositionAndPadding = false;
    }
}
