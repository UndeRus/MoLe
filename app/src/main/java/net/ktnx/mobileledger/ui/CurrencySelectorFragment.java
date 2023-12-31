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

package net.ktnx.mobileledger.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.dao.CurrencyDAO;
import net.ktnx.mobileledger.db.DB;
import net.ktnx.mobileledger.db.Profile;
import net.ktnx.mobileledger.model.Currency;
import net.ktnx.mobileledger.model.Data;

import java.util.ArrayList;
import java.util.List;

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
        Profile profile = Data.getProfile();

        CurrencySelectorRecyclerViewAdapter adapter = new CurrencySelectorRecyclerViewAdapter();
        DB.get()
          .getCurrencyDAO()
          .getAll()
          .observe(this, list -> {
              List<String> strings = new ArrayList<>();
              for (net.ktnx.mobileledger.db.Currency c : list) {
                  strings.add(c.getName());
              }
              adapter.submitList(strings);
          });

        recyclerView.setAdapter(adapter);
        adapter.setCurrencySelectedListener(this);
        adapter.setCurrencyLongClickListener(this);

        final TextView tvNewCurrName = csd.findViewById(R.id.new_currency_name);
        final TextView tvNoCurrBtn = csd.findViewById(R.id.btn_no_currency);
        final TextView tvAddCurrOkBtn = csd.findViewById(R.id.btn_add_currency);
        final TextView tvAddCurrBtn = csd.findViewById(R.id.btn_add_new);
        final SwitchMaterial gap = csd.findViewById(R.id.currency_gap);
        final RadioGroup rgPosition = csd.findViewById(R.id.position_radio_group);

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
                DB.get()
                  .getCurrencyDAO()
                  .insert(new net.ktnx.mobileledger.db.Currency(0,
                          String.valueOf(tvNewCurrName.getText()),
                          (rgPosition.getCheckedRadioButtonId() == R.id.currency_position_left)
                          ? "before" : "after", gap.isChecked()));
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

        rgPosition.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.currency_position_left)
                Data.currencySymbolPosition.setValue(Currency.Position.before);
            else
                Data.currencySymbolPosition.setValue(Currency.Position.after);
        });

        gap.setChecked(Data.currencyGap.getValue());

        gap.setOnCheckedChangeListener((v, checked) -> Data.currencyGap.setValue(checked));

        model.observePositionAndPaddingVisible(this, visible -> csd.findViewById(R.id.params_panel)
                                                                   .setVisibility(
                                                                           visible ? View.VISIBLE
                                                                                   : View.GONE));

        final boolean showParams;
        if (getArguments() == null)
            showParams = DEFAULT_SHOW_PARAMS;
        else
            showParams = getArguments().getBoolean(ARG_SHOW_PARAMS, DEFAULT_SHOW_PARAMS);

        if (showParams)
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
    public void onCurrencySelected(String item) {
        model.triggerOnCurrencySelectedListener(item);

        dismiss();
    }

    @Override
    public void onCurrencyLongClick(String item) {
        CurrencyDAO dao = DB.get()
                            .getCurrencyDAO();
        dao.getByName(item)
           .observe(this, dao::deleteSync);
    }
    public void showPositionAndPadding() {
        deferredShowPositionAndPadding = true;
    }
    public void hidePositionAndPadding() {
        deferredShowPositionAndPadding = false;
    }
}
