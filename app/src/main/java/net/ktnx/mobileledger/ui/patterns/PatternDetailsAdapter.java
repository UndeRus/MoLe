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

import android.annotation.SuppressLint;
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
import net.ktnx.mobileledger.ui.QRScanAbleFragment;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
                if (oldItem.getType() == PatternDetailsItem.Type.HEADER)
                    return true;    // only one header item, ever
                // the rest is comparing two account row items
                return oldItem.asAccountRowItem()
                              .getId() == newItem.asAccountRowItem()
                                                 .getId();
            }
            @SuppressLint("DiffUtilEquals")
            @Override
            public boolean areContentsTheSame(@NonNull PatternDetailsItem oldItem,
                                              @NonNull PatternDetailsItem newItem) {
                if (oldItem.getType() == PatternDetailsItem.Type.HEADER) {
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
        if (p == null) return null;

        Matcher m = p.matcher(header.getTestText());
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
        protected int updateInProgress = 0;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
        protected void startUpdate() {
            updateInProgress++;
        }
        protected void finishUpdate() {
            if (updateInProgress <= 0)
                throw new IllegalStateException(
                        "Unexpected updateInProgress value " + updateInProgress);

            updateInProgress--;
        }
        abstract void bind(PatternDetailsItem item);
    }

    public class Header extends ViewHolder {
        private final PatternDetailsHeaderBinding b;
        private final TextWatcher patternNameWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Object tag = b.patternDetailsItemHead.getTag();
                if (tag != null) {
                    final PatternDetailsItem.Header header =
                            ((PatternDetailsItem) tag).asHeaderItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed pattern name " + s + "; header=" + header);
                    header.setName(String.valueOf(s));
                }
            }
        };
        private final TextWatcher patternWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Object tag = b.patternDetailsItemHead.getTag();
                if (tag != null) {
                    final PatternDetailsItem.Header header =
                            ((PatternDetailsItem) tag).asHeaderItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed pattern " + s + "; header=" + header);
                    header.setPattern(String.valueOf(s));
                }
            }
        };
        private final TextWatcher testTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                Object tag = b.patternDetailsItemHead.getTag();
                if (tag != null) {
                    final PatternDetailsItem.Header header =
                            ((PatternDetailsItem) tag).asHeaderItem();
                    Logger.debug(D_PATTERN_UI,
                            "Storing changed test text " + s + "; header=" + header);
                    header.setTestText(String.valueOf(s));
                }
            }
        };
        private final TextWatcher transactionDescriptionWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                PatternDetailsItem.Header header = ((PatternDetailsItem) Objects.requireNonNull(
                        b.patternDetailsItemHead.getTag())).asHeaderItem();
                Logger.debug(D_PATTERN_UI,
                        "Storing changed transaction description " + s + "; header=" + header);
                header.setTransactionDescription(String.valueOf(s));
            }
        };
        private final TextWatcher transactionCommentWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                PatternDetailsItem.Header header = ((PatternDetailsItem) Objects.requireNonNull(
                        b.patternDetailsItemHead.getTag())).asHeaderItem();
                Logger.debug(D_PATTERN_UI,
                        "Storing changed transaction description " + s + "; header=" + header);
                header.setTransactionComment(String.valueOf(s));
            }
        };
        public Header(@NonNull PatternDetailsHeaderBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
        Header(@NonNull View itemView) {
            super(itemView);
            throw new IllegalStateException("Should not be used");
        }
        private void selectHeaderDetailSource(View v, PatternDetailsItem.Header header,
                                              HeaderDetail detail) {
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
            startUpdate();
            try {
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
                    b.patternDetailsYearSource.setText(String.format(Locale.US, "Group %d (%s)",
                            header.getDateYearMatchGroup(), getMatchGroupText(
                                    header.getDateYearMatchGroup())));
                }
                b.patternDetailsYearSourceLabel.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DATE_YEAR));
                b.patternDetailsYearSource.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DATE_YEAR));

                if (header.hasLiteralDateMonth()) {
                    b.patternDetailsMonthSource.setText(R.string.pattern_details_source_literal);
                    b.patternDetailsDateMonth.setText(String.valueOf(header.getDateMonth()));
                    b.patternDetailsDateMonthLayout.setVisibility(View.VISIBLE);
                }
                else {
                    b.patternDetailsDateMonthLayout.setVisibility(View.GONE);
                    b.patternDetailsMonthSource.setText(String.format(Locale.US, "Group %d (%s)",
                            header.getDateMonthMatchGroup(), getMatchGroupText(
                                    header.getDateMonthMatchGroup())));
                }
                b.patternDetailsMonthSourceLabel.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DATE_MONTH));
                b.patternDetailsMonthSource.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DATE_MONTH));

                if (header.hasLiteralDateDay()) {
                    b.patternDetailsDaySource.setText(R.string.pattern_details_source_literal);
                    b.patternDetailsDateDay.setText(String.valueOf(header.getDateDay()));
                    b.patternDetailsDateDayLayout.setVisibility(View.VISIBLE);
                }
                else {
                    b.patternDetailsDateDayLayout.setVisibility(View.GONE);
                    b.patternDetailsDaySource.setText(String.format(Locale.US, "Group %d (%s)",
                            header.getDateDayMatchGroup(), getMatchGroupText(
                                    header.getDateDayMatchGroup())));
                }
                b.patternDetailsDaySourceLabel.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DATE_DAY));
                b.patternDetailsDaySource.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DATE_DAY));

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
                                    header.getTransactionDescriptionMatchGroup(), getMatchGroupText(
                                            header.getTransactionDescriptionMatchGroup())));

                }
                b.patternTransactionDescriptionSourceLabel.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DESCRIPTION));
                b.patternTransactionDescriptionSource.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.DESCRIPTION));

                if (header.hasLiteralTransactionComment()) {
                    b.patternTransactionCommentSource.setText(
                            R.string.pattern_details_source_literal);
                    b.transactionComment.setText(header.getTransactionComment());
                    b.transactionCommentLayout.setVisibility(View.VISIBLE);
                }
                else {
                    b.transactionCommentLayout.setVisibility(View.GONE);
                    b.patternTransactionCommentSource.setText(
                            String.format(Locale.US, "Group %d (%s)",
                                    header.getTransactionCommentMatchGroup(),
                                    getMatchGroupText(header.getTransactionCommentMatchGroup())));

                }
                b.patternTransactionCommentSourceLabel.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.COMMENT));
                b.patternTransactionCommentSource.setOnClickListener(
                        v -> selectHeaderDetailSource(v, header, HeaderDetail.COMMENT));

                b.patternDetailsHeadScanQrButton.setOnClickListener(this::scanTestQR);

                final Object prevTag = b.patternDetailsItemHead.getTag();
                if (!(prevTag instanceof PatternDetailsItem)) {
                    Logger.debug(D_PATTERN_UI, "Hooked text change listeners");

                    b.patternName.addTextChangedListener(patternNameWatcher);
                    b.pattern.addTextChangedListener(patternWatcher);
                    b.testText.addTextChangedListener(testTextWatcher);
                    b.transactionDescription.addTextChangedListener(transactionDescriptionWatcher);
                    b.transactionComment.addTextChangedListener(transactionCommentWatcher);
                }

                b.patternDetailsItemHead.setTag(item);
            }
            finally {
                finishUpdate();
            }
        }
        private void scanTestQR(View view) {
            QRScanAbleFragment.triggerQRScan();
        }
    }

    public class AccountRow extends ViewHolder {
        private final PatternDetailsAccountBinding b;
        public AccountRow(@NonNull PatternDetailsAccountBinding binding) {
            super(binding.getRoot());
            b = binding;
        }
        AccountRow(@NonNull View itemView) {
            super(itemView);
            throw new IllegalStateException("Should not be used");
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
                b.patternDetailsAccountAmount.setText(Data.formatNumber(accRow.getAmount()));
            }
            else {
                b.patternDetailsAccountAmountSource.setText(
                        String.format(Locale.US, "Group %d (%s)", accRow.getAmountMatchGroup(),
                                getMatchGroupText(accRow.getAmountMatchGroup())));
                b.patternDetailsAccountAmountLayout.setVisibility(View.GONE);
            }

            b.patternAccountNameSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, accRow, AccDetail.ACCOUNT));
            b.patternDetailsAccountNameSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, accRow, AccDetail.ACCOUNT));
            b.patternAccountCommentSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, accRow, AccDetail.COMMENT));
            b.patternDetailsAccountCommentSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, accRow, AccDetail.COMMENT));
            b.patternAccountAmountSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, accRow, AccDetail.AMOUNT));
            b.patternDetailsAccountAmountSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, accRow, AccDetail.AMOUNT));
        }
        private void selectAccountRowDetailSource(View v, PatternDetailsItem.AccountRow accRow,
                                                  AccDetail detail) {
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
