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

package net.ktnx.mobileledger.ui.patterns;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.PatternDetailsAccountBinding;
import net.ktnx.mobileledger.databinding.PatternDetailsHeaderBinding;
import net.ktnx.mobileledger.db.PatternBase;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.PatternDetailsItem;
import net.ktnx.mobileledger.ui.PatternDetailSourceSelectorFragment;
import net.ktnx.mobileledger.ui.QRScanCapableFragment;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PatternDetailsAdapter extends RecyclerView.Adapter<PatternDetailsAdapter.ViewHolder> {
    private static final String D_PATTERN_UI = "pattern-ui";
    private final AsyncListDiffer<PatternDetailsItem> differ;
    public PatternDetailsAdapter() {
        super();
        setHasStableIds(true);
        differ = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<PatternDetailsItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull PatternDetailsItem oldItem,
                                           @NonNull PatternDetailsItem newItem) {
                if (oldItem.getType() != newItem.getType())
                    return false;
                if (oldItem.getType()
                           .equals(PatternDetailsItem.Type.HEADER))
                    return true;    // only one header item, ever
                // the rest is comparing two account row items
                return oldItem.asAccountRowItem()
                              .getId() == newItem.asAccountRowItem()
                                                 .getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull PatternDetailsItem oldItem,
                                              @NonNull PatternDetailsItem newItem) {
                if (oldItem.getType()
                           .equals(PatternDetailsItem.Type.HEADER))
                {
                    PatternDetailsItem.Header oldHeader = oldItem.asHeaderItem();
                    PatternDetailsItem.Header newHeader = newItem.asHeaderItem();

                    return oldHeader.equalContents(newHeader);
                }
                else {
                    PatternDetailsItem.AccountRow oldAcc = oldItem.asAccountRowItem();
                    PatternDetailsItem.AccountRow newAcc = newItem.asAccountRowItem();

                    return oldAcc.equalContents(newAcc);
                }
            }
        });
    }
    @Override
    public long getItemId(int position) {
        // header item is always first and IDs id may duplicate some of the account IDs
        if (position == 0)
            return -1;
        PatternDetailsItem.AccountRow accRow = differ.getCurrentList()
                                                     .get(position)
                                                     .asAccountRowItem();
        return accRow.getId();
    }
    @Override
    public int getItemViewType(int position) {

        return differ.getCurrentList()
                     .get(position)
                     .getType()
                     .toInt();
    }
    @NonNull
    @Override
    public PatternDetailsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                               int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case PatternDetailsItem.TYPE.header:
                return new Header(PatternDetailsHeaderBinding.inflate(inflater, parent, false));
            case PatternDetailsItem.TYPE.accountItem:
                return new AccountRow(
                        PatternDetailsAccountBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalStateException("Unsupported view type " + viewType);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull PatternDetailsAdapter.ViewHolder holder, int position) {
        PatternDetailsItem item = differ.getCurrentList()
                                        .get(position);
        holder.bind(item);
    }
    @Override
    public int getItemCount() {
        return differ.getCurrentList()
                     .size();
    }
    public void setPatternItems(List<PatternBase> items) {
        ArrayList<PatternDetailsItem> list = new ArrayList<>();
        for (PatternBase p : items) {
            PatternDetailsItem item = PatternDetailsItem.fromRoomObject(p);
            list.add(item);
        }
        setItems(list);
    }
    public void setItems(List<PatternDetailsItem> items) {
        differ.submitList(items);
    }
    public String getMatchGroupText(int groupNumber) {
        PatternDetailsItem.Header header = getHeader();
        Pattern p = header.getCompiledPattern();
        if (p == null)
            return null;

        final String testText = Misc.nullIsEmpty(header.getTestText());
        Matcher m = p.matcher(testText);
        if (m.matches() && m.groupCount() >= groupNumber)
            return m.group(groupNumber);
        else
            return null;
    }
    protected PatternDetailsItem.Header getHeader() {
        return differ.getCurrentList()
                     .get(0)
                     .asHeaderItem();
    }

    private enum HeaderDetail {DESCRIPTION, COMMENT, DATE_YEAR, DATE_MONTH, DATE_DAY}

    private enum AccDetail {ACCOUNT, COMMENT, AMOUNT}

    public abstract static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        abstract void bind(PatternDetailsItem item);
    }

    public class Header extends ViewHolder {
        private final PatternDetailsHeaderBinding b;
        public Header(@NonNull PatternDetailsHeaderBinding binding) {
            super(binding.getRoot());
            b = binding;

            TextWatcher patternNameWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    final PatternDetailsItem.Header header = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed pattern name " + s + "; header=" + header);
                    header.setName(String.valueOf(s));
                }
            };
            b.patternName.addTextChangedListener(patternNameWatcher);
            TextWatcher patternWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    final PatternDetailsItem.Header header = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed pattern " + s + "; header=" + header);
                    header.setPattern(String.valueOf(s));
                }
            };
            b.pattern.addTextChangedListener(patternWatcher);
            TextWatcher testTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    final PatternDetailsItem.Header header = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed test text " + s + "; header=" + header);
                    header.setTestText(String.valueOf(s));
                }
            };
            b.testText.addTextChangedListener(testTextWatcher);
            TextWatcher transactionDescriptionWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }
                @Override
                public void afterTextChanged(Editable s) {
                    final PatternDetailsItem.Header header = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed transaction description " + s + "; header=" + header);
                    header.setTransactionDescription(String.valueOf(s));
                }
            };
            b.transactionDescription.addTextChangedListener(transactionDescriptionWatcher);
            TextWatcher transactionCommentWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }
                @Override
                public void afterTextChanged(Editable s) {
                    final PatternDetailsItem.Header header = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed transaction description " + s + "; header=" + header);
                    header.setTransactionComment(String.valueOf(s));
                }
            };
            b.transactionComment.addTextChangedListener(transactionCommentWatcher);
        }
        @NotNull
        private PatternDetailsItem.Header getItem() {
            int pos = getAdapterPosition();
            return differ.getCurrentList()
                         .get(pos)
                         .asHeaderItem();
        }
        private void selectHeaderDetailSource(View v, HeaderDetail detail) {
            PatternDetailsItem.Header header = getItem();
            Logger.debug(D_PATTERN_UI, "header is " + header);
            PatternDetailSourceSelectorFragment sel =
                    PatternDetailSourceSelectorFragment.newInstance(1, header.getPattern(),
                            header.getTestText());
            sel.setOnSourceSelectedListener((literal, group) -> {
                if (literal) {
                    switch (detail) {
                        case DESCRIPTION:
                            header.switchToLiteralTransactionDescription();
                            break;
                        case COMMENT:
                            header.switchToLiteralTransactionComment();
                            break;
                        case DATE_YEAR:
                            header.switchToLiteralDateYear();
                            break;
                        case DATE_MONTH:
                            header.switchToLiteralDateMonth();
                            break;
                        case DATE_DAY:
                            header.switchToLiteralDateDay();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected detail " + detail);
                    }
                }
                else {
                    switch (detail) {
                        case DESCRIPTION:
                            header.setTransactionDescriptionMatchGroup(group);
                            break;
                        case COMMENT:
                            header.setTransactionCommentMatchGroup(group);
                            break;
                        case DATE_YEAR:
                            header.setDateYearMatchGroup(group);
                            break;
                        case DATE_MONTH:
                            header.setDateMonthMatchGroup(group);
                            break;
                        case DATE_DAY:
                            header.setDateDayMatchGroup(group);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected detail " + detail);
                    }
                }

                notifyItemChanged(getAdapterPosition());
            });
            final AppCompatActivity activity = (AppCompatActivity) v.getContext();
            sel.show(activity.getSupportFragmentManager(), "pattern-details-source-selector");
        }
        @Override
        void bind(PatternDetailsItem item) {
            PatternDetailsItem.Header header = item.asHeaderItem();
            Logger.debug(D_PATTERN_UI, "Binding to header " + header);

            b.patternName.setText(header.getName());
            b.pattern.setText(header.getPattern());
            b.testText.setText(header.getTestText());

            if (header.hasLiteralDateYear()) {
                b.patternDetailsYearSource.setText(R.string.pattern_details_source_literal);
                b.patternDetailsDateYear.setText(String.valueOf(header.getDateYear()));
                b.patternDetailsDateYearLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.patternDetailsDateYearLayout.setVisibility(View.GONE);
                b.patternDetailsYearSource.setText(
                        String.format(Locale.US, "Group %d (%s)", header.getDateYearMatchGroup(),
                                getMatchGroupText(header.getDateYearMatchGroup())));
            }
            b.patternDetailsYearSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_YEAR));
            b.patternDetailsYearSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_YEAR));

            if (header.hasLiteralDateMonth()) {
                b.patternDetailsMonthSource.setText(R.string.pattern_details_source_literal);
                b.patternDetailsDateMonth.setText(String.valueOf(header.getDateMonth()));
                b.patternDetailsDateMonthLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.patternDetailsDateMonthLayout.setVisibility(View.GONE);
                b.patternDetailsMonthSource.setText(
                        String.format(Locale.US, "Group %d (%s)", header.getDateMonthMatchGroup(),
                                getMatchGroupText(header.getDateMonthMatchGroup())));
            }
            b.patternDetailsMonthSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_MONTH));
            b.patternDetailsMonthSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_MONTH));

            if (header.hasLiteralDateDay()) {
                b.patternDetailsDaySource.setText(R.string.pattern_details_source_literal);
                b.patternDetailsDateDay.setText(String.valueOf(header.getDateDay()));
                b.patternDetailsDateDayLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.patternDetailsDateDayLayout.setVisibility(View.GONE);
                b.patternDetailsDaySource.setText(
                        String.format(Locale.US, "Group %d (%s)", header.getDateDayMatchGroup(),
                                getMatchGroupText(header.getDateDayMatchGroup())));
            }
            b.patternDetailsDaySourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_DAY));
            b.patternDetailsDaySource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_DAY));

            if (header.hasLiteralTransactionDescription()) {
                b.patternTransactionDescriptionSource.setText(
                        R.string.pattern_details_source_literal);
                b.transactionDescription.setText(header.getTransactionDescription());
                b.transactionDescriptionLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.transactionDescriptionLayout.setVisibility(View.GONE);
                b.patternTransactionDescriptionSource.setText(
                        String.format(Locale.US, "Group %d (%s)",
                                header.getTransactionDescriptionMatchGroup(),
                                getMatchGroupText(header.getTransactionDescriptionMatchGroup())));

            }
            b.patternTransactionDescriptionSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DESCRIPTION));
            b.patternTransactionDescriptionSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DESCRIPTION));

            if (header.hasLiteralTransactionComment()) {
                b.patternTransactionCommentSource.setText(R.string.pattern_details_source_literal);
                b.transactionComment.setText(header.getTransactionComment());
                b.transactionCommentLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.transactionCommentLayout.setVisibility(View.GONE);
                b.patternTransactionCommentSource.setText(String.format(Locale.US, "Group %d (%s)",
                        header.getTransactionCommentMatchGroup(),
                        getMatchGroupText(header.getTransactionCommentMatchGroup())));

            }
            b.patternTransactionCommentSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.COMMENT));
            b.patternTransactionCommentSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.COMMENT));

            b.patternDetailsHeadScanQrButton.setOnClickListener(this::scanTestQR);

        }
        private void scanTestQR(View view) {
            QRScanCapableFragment.triggerQRScan();
        }
    }

    public class AccountRow extends ViewHolder {
        private final PatternDetailsAccountBinding b;
        public AccountRow(@NonNull PatternDetailsAccountBinding binding) {
            super(binding.getRoot());
            b = binding;

            TextWatcher accountNameWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    PatternDetailsItem.AccountRow accRow = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed account name " + s + "; accRow=" + accRow);
                    accRow.setAccountName(String.valueOf(s));
                }
            };
            b.patternDetailsAccountName.addTextChangedListener(accountNameWatcher);
            TextWatcher accountCommentWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    PatternDetailsItem.AccountRow accRow = getItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed account comment " + s + "; accRow=" + accRow);
                    accRow.setAccountComment(String.valueOf(s));
                }
            };
            b.patternDetailsAccountComment.addTextChangedListener(accountCommentWatcher);

            b.patternDetailsAccountAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }
                @Override
                public void afterTextChanged(Editable s) {
                    PatternDetailsItem.AccountRow accRow = getItem();

                    String str = String.valueOf(s);
                    if (Misc.emptyIsNull(str) == null) {
                        accRow.setAmount(null);
                    }
                    else {
                        try {
                            final float amount = Data.parseNumber(str);
                            accRow.setAmount(amount);
                            b.patternDetailsAccountAmountLayout.setError(null);

                            Logger.debug(D_PATTERN_UI, String.format(Locale.US,
                                    "Storing changed account amount %s [%4.2f]; accRow=%s", s,
                                    amount, accRow));
                        }
                        catch (NumberFormatException | ParseException e) {
                            b.patternDetailsAccountAmountLayout.setError("!");
                        }
                    }
                }
            });
            b.patternDetailsAccountAmount.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus)
                    return;

                PatternDetailsItem.AccountRow accRow = getItem();
                if (!accRow.hasLiteralAmount())
                    return;
                Float amt = accRow.getAmount();
                if (amt == null)
                    return;

                b.patternDetailsAccountAmount.setText(Data.formatNumber(amt));
            });

            b.negateAmountSwitch.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> getItem().setNegateAmount(isChecked));
        }
        @Override
        void bind(PatternDetailsItem item) {
            PatternDetailsItem.AccountRow accRow = item.asAccountRowItem();
            if (accRow.hasLiteralAccountName()) {
                b.patternDetailsAccountNameLayout.setVisibility(View.VISIBLE);
                b.patternDetailsAccountName.setText(accRow.getAccountName());
                b.patternDetailsAccountNameSource.setText(R.string.pattern_details_source_literal);
            }
            else {
                b.patternDetailsAccountNameLayout.setVisibility(View.GONE);
                b.patternDetailsAccountNameSource.setText(
                        String.format(Locale.US, "Group %d (%s)", accRow.getAccountNameMatchGroup(),
                                getMatchGroupText(accRow.getAccountNameMatchGroup())));
            }

            if (accRow.hasLiteralAccountComment()) {
                b.patternDetailsAccountCommentLayout.setVisibility(View.VISIBLE);
                b.patternDetailsAccountComment.setText(accRow.getAccountComment());
                b.patternDetailsAccountCommentSource.setText(
                        R.string.pattern_details_source_literal);
            }
            else {
                b.patternDetailsAccountCommentLayout.setVisibility(View.GONE);
                b.patternDetailsAccountCommentSource.setText(
                        String.format(Locale.US, "Group %d (%s)",
                                accRow.getAccountCommentMatchGroup(),
                                getMatchGroupText(accRow.getAccountCommentMatchGroup())));
            }

            if (accRow.hasLiteralAmount()) {
                b.patternDetailsAccountAmountSource.setText(
                        R.string.pattern_details_source_literal);
                b.patternDetailsAccountAmount.setVisibility(View.VISIBLE);
                Float amt = accRow.getAmount();
                b.patternDetailsAccountAmount.setText((amt == null) ? null : String.format(
                        Data.locale.getValue(), "%,4.2f", (accRow.getAmount())));
                b.negateAmountSwitch.setVisibility(View.GONE);
            }
            else {
                b.patternDetailsAccountAmountSource.setText(
                        String.format(Locale.US, "Group %d (%s)", accRow.getAmountMatchGroup(),
                                getMatchGroupText(accRow.getAmountMatchGroup())));
                b.patternDetailsAccountAmountLayout.setVisibility(View.GONE);
                b.negateAmountSwitch.setVisibility(View.VISIBLE);
                b.negateAmountSwitch.setChecked(accRow.isNegateAmount());
            }

            b.patternAccountNameSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.ACCOUNT));
            b.patternDetailsAccountNameSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.ACCOUNT));
            b.patternAccountCommentSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.COMMENT));
            b.patternDetailsAccountCommentSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.COMMENT));
            b.patternAccountAmountSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.AMOUNT));
            b.patternDetailsAccountAmountSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.AMOUNT));
        }
        private @NotNull PatternDetailsItem.AccountRow getItem() {
            return differ.getCurrentList()
                         .get(getAdapterPosition())
                         .asAccountRowItem();
        }
        private void selectAccountRowDetailSource(View v, AccDetail detail) {
            PatternDetailsItem.AccountRow accRow = getItem();
            final PatternDetailsItem.Header header = getHeader();
            Logger.debug(D_PATTERN_UI, "header is " + header);
            PatternDetailSourceSelectorFragment sel =
                    PatternDetailSourceSelectorFragment.newInstance(1, header.getPattern(),
                            header.getTestText());
            sel.setOnSourceSelectedListener((literal, group) -> {
                if (literal) {
                    switch (detail) {
                        case ACCOUNT:
                            accRow.switchToLiteralAccountName();
                            break;
                        case COMMENT:
                            accRow.switchToLiteralAccountComment();
                            break;
                        case AMOUNT:
                            accRow.switchToLiteralAmount();
                            break;
                        default:
                            throw new IllegalStateException("Unexpected detail " + detail);
                    }
                }
                else {
                    switch (detail) {
                        case ACCOUNT:
                            accRow.setAccountNameMatchGroup(group);
                            break;
                        case COMMENT:
                            accRow.setAccountCommentMatchGroup(group);
                            break;
                        case AMOUNT:
                            accRow.setAmountMatchGroup(group);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected detail " + detail);
                    }
                }

                notifyItemChanged(getAdapterPosition());
            });
            final AppCompatActivity activity = (AppCompatActivity) v.getContext();
            sel.show(activity.getSupportFragmentManager(), "pattern-details-source-selector");
        }
    }
}
