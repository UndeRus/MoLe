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

package net.ktnx.mobileledger.ui.templates;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.databinding.TemplateDetailsAccountBinding;
import net.ktnx.mobileledger.databinding.TemplateDetailsHeaderBinding;
import net.ktnx.mobileledger.db.AccountAutocompleteAdapter;
import net.ktnx.mobileledger.db.TemplateBase;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.model.TemplateDetailsItem;
import net.ktnx.mobileledger.ui.QRScanCapableFragment;
import net.ktnx.mobileledger.ui.TemplateDetailSourceSelectorFragment;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateDetailsAdapter extends RecyclerView.Adapter<TemplateDetailsAdapter.ViewHolder> {
    private static final String D_TEMPLATE_UI = "template-ui";
    private final AsyncListDiffer<TemplateDetailsItem> differ;
    private final TemplateDetailsViewModel mModel;
    private final ItemTouchHelper itemTouchHelper;
    public TemplateDetailsAdapter(TemplateDetailsViewModel model) {
        super();
        mModel = model;
        setHasStableIds(true);
        differ = new AsyncListDiffer<>(this, new DiffUtil.ItemCallback<TemplateDetailsItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull TemplateDetailsItem oldItem,
                                           @NonNull TemplateDetailsItem newItem) {
                if (oldItem.getType() != newItem.getType())
                    return false;
                if (oldItem.getType()
                           .equals(TemplateDetailsItem.Type.HEADER))
                    return true;    // only one header item, ever
                // the rest is comparing two account row items
                return oldItem.asAccountRowItem()
                              .getId() == newItem.asAccountRowItem()
                                                 .getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull TemplateDetailsItem oldItem,
                                              @NonNull TemplateDetailsItem newItem) {
                if (oldItem.getType()
                           .equals(TemplateDetailsItem.Type.HEADER))
                {
                    TemplateDetailsItem.Header oldHeader = oldItem.asHeaderItem();
                    TemplateDetailsItem.Header newHeader = newItem.asHeaderItem();

                    return oldHeader.equalContents(newHeader);
                }
                else {
                    TemplateDetailsItem.AccountRow oldAcc = oldItem.asAccountRowItem();
                    TemplateDetailsItem.AccountRow newAcc = newItem.asAccountRowItem();

                    return oldAcc.equalContents(newAcc);
                }
            }
        });
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public float getMoveThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.1f;
            }
            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }
            @Override
            public RecyclerView.ViewHolder chooseDropTarget(
                    @NonNull RecyclerView.ViewHolder selected,
                    @NonNull List<RecyclerView.ViewHolder> dropTargets, int curX, int curY) {
                RecyclerView.ViewHolder best = null;
                int bestDistance = 0;
                for (RecyclerView.ViewHolder v : dropTargets) {
                    if (v == selected)
                        continue;

                    final int viewTop = v.itemView.getTop();
                    int distance = Math.abs(viewTop - curY);
                    if (best == null) {
                        best = v;
                        bestDistance = distance;
                    }
                    else {
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            best = v;
                        }
                    }
                }

                Logger.debug("dnd", "Best target is " + best);
                return best;
            }
            @Override
            public boolean canDropOver(@NonNull RecyclerView recyclerView,
                                       @NonNull RecyclerView.ViewHolder current,
                                       @NonNull RecyclerView.ViewHolder target) {
                final int adapterPosition = target.getAdapterPosition();

                // first item is immovable
                if (adapterPosition == 0)
                    return false;

                return super.canDropOver(recyclerView, current, target);
            }
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                int flags = 0;
                // the top item (transaction params) is always there
                final int adapterPosition = viewHolder.getAdapterPosition();
                if (adapterPosition > 0)
                    flags |= makeFlag(ItemTouchHelper.ACTION_STATE_DRAG,
                            ItemTouchHelper.UP | ItemTouchHelper.DOWN) |
                             makeFlag(ItemTouchHelper.ACTION_STATE_SWIPE,
                                     ItemTouchHelper.START | ItemTouchHelper.END);

                return flags;
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {

                final int fromPosition = viewHolder.getAdapterPosition();
                final int toPosition = target.getAdapterPosition();
                mModel.moveItem(fromPosition, toPosition);

                return true;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                mModel.removeItem(pos);
            }
        });
    }
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        itemTouchHelper.attachToRecyclerView(null);
    }
    @Override
    public long getItemId(int position) {
        // header item is always first and IDs id may duplicate some of the account IDs
        if (position == 0)
            return -1;
        TemplateDetailsItem.AccountRow accRow = differ.getCurrentList()
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
    public TemplateDetailsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TemplateDetailsItem.TYPE.header:
                return new Header(TemplateDetailsHeaderBinding.inflate(inflater, parent, false));
            case TemplateDetailsItem.TYPE.accountItem:
                return new AccountRow(
                        TemplateDetailsAccountBinding.inflate(inflater, parent, false));
            default:
                throw new IllegalStateException("Unsupported view type " + viewType);
        }
    }
    @Override
    public void onBindViewHolder(@NonNull TemplateDetailsAdapter.ViewHolder holder, int position) {
        TemplateDetailsItem item = differ.getCurrentList()
                                         .get(position);
        holder.bind(item);
    }
    @Override
    public int getItemCount() {
        return differ.getCurrentList()
                     .size();
    }
    public void setTemplateItems(List<TemplateBase> items) {
        ArrayList<TemplateDetailsItem> list = new ArrayList<>();
        for (TemplateBase p : items) {
            TemplateDetailsItem item = TemplateDetailsItem.fromRoomObject(p);
            list.add(item);
        }
        setItems(list);
    }
    public void setItems(List<TemplateDetailsItem> items) {
        differ.submitList(items);
    }
    public String getMatchGroupText(int groupNumber) {
        TemplateDetailsItem.Header header = getHeader();
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
    protected TemplateDetailsItem.Header getHeader() {
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
        abstract void bind(TemplateDetailsItem item);
    }

    public class Header extends ViewHolder {
        private final TemplateDetailsHeaderBinding b;
        public Header(@NonNull TemplateDetailsHeaderBinding binding) {
            super(binding.getRoot());
            b = binding;

            TextWatcher templateNameWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    final TemplateDetailsItem.Header header = getItem();
                    Logger.debug(D_TEMPLATE_UI,
                            "Storing changed template name " + s + "; header=" + header);
                    header.setName(String.valueOf(s));
                }
            };
            b.templateName.addTextChangedListener(templateNameWatcher);

            TextWatcher patternWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    final TemplateDetailsItem.Header header = getItem();
                    Logger.debug(D_TEMPLATE_UI,
                            "Storing changed pattern " + s + "; header=" + header);
                    header.setPattern(String.valueOf(s));

                    checkPatternError(header);
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
                    final TemplateDetailsItem.Header header = getItem();
                    Logger.debug(D_TEMPLATE_UI,
                            "Storing changed test text " + s + "; header=" + header);
                    header.setTestText(String.valueOf(s));

                    checkPatternError(header);
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
                    final TemplateDetailsItem.Header header = getItem();
                    Logger.debug(D_TEMPLATE_UI,
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
                    final TemplateDetailsItem.Header header = getItem();
                    Logger.debug(D_TEMPLATE_UI,
                            "Storing changed transaction description " + s + "; header=" + header);
                    header.setTransactionComment(String.valueOf(s));
                }
            };
            b.transactionComment.addTextChangedListener(transactionCommentWatcher);
        }
        @NotNull
        private TemplateDetailsItem.Header getItem() {
            int pos = getAdapterPosition();
            return differ.getCurrentList()
                         .get(pos)
                         .asHeaderItem();
        }
        private void selectHeaderDetailSource(View v, HeaderDetail detail) {
            TemplateDetailsItem.Header header = getItem();
            Logger.debug(D_TEMPLATE_UI, "header is " + header);
            TemplateDetailSourceSelectorFragment sel =
                    TemplateDetailSourceSelectorFragment.newInstance(1, header.getPattern(),
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
            sel.show(activity.getSupportFragmentManager(), "template-details-source-selector");
        }
        @Override
        void bind(TemplateDetailsItem item) {
            TemplateDetailsItem.Header header = item.asHeaderItem();
            Logger.debug(D_TEMPLATE_UI, "Binding to header " + header);

            String groupNoText = b.getRoot()
                                  .getResources()
                                  .getString(R.string.template_item_match_group_source);

            b.templateName.setText(header.getName());
            b.pattern.setText(header.getPattern());
            b.testText.setText(header.getTestText());

            if (header.hasLiteralDateYear()) {
                b.templateDetailsYearSource.setText(R.string.template_details_source_literal);
                final Integer dateYear = header.getDateYear();
                b.templateDetailsDateYear.setText(
                        (dateYear == null) ? null : String.valueOf(dateYear));
                b.templateDetailsDateYearLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.templateDetailsDateYearLayout.setVisibility(View.GONE);
                b.templateDetailsYearSource.setText(
                        String.format(Locale.US, groupNoText, header.getDateYearMatchGroup(),
                                getMatchGroupText(header.getDateYearMatchGroup())));
            }
            b.templateDetailsYearSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_YEAR));
            b.templateDetailsYearSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_YEAR));

            if (header.hasLiteralDateMonth()) {
                b.templateDetailsMonthSource.setText(R.string.template_details_source_literal);
                final Integer dateMonth = header.getDateMonth();
                b.templateDetailsDateMonth.setText(
                        (dateMonth == null) ? null : String.valueOf(dateMonth));
                b.templateDetailsDateMonthLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.templateDetailsDateMonthLayout.setVisibility(View.GONE);
                b.templateDetailsMonthSource.setText(
                        String.format(Locale.US, groupNoText, header.getDateMonthMatchGroup(),
                                getMatchGroupText(header.getDateMonthMatchGroup())));
            }
            b.templateDetailsMonthSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_MONTH));
            b.templateDetailsMonthSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_MONTH));

            if (header.hasLiteralDateDay()) {
                b.templateDetailsDaySource.setText(R.string.template_details_source_literal);
                final Integer dateDay = header.getDateDay();
                b.templateDetailsDateDay.setText(
                        (dateDay == null) ? null : String.valueOf(dateDay));
                b.templateDetailsDateDayLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.templateDetailsDateDayLayout.setVisibility(View.GONE);
                b.templateDetailsDaySource.setText(
                        String.format(Locale.US, groupNoText, header.getDateDayMatchGroup(),
                                getMatchGroupText(header.getDateDayMatchGroup())));
            }
            b.templateDetailsDaySourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_DAY));
            b.templateDetailsDaySource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DATE_DAY));

            if (header.hasLiteralTransactionDescription()) {
                b.templateTransactionDescriptionSource.setText(
                        R.string.template_details_source_literal);
                b.transactionDescription.setText(header.getTransactionDescription());
                b.transactionDescriptionLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.transactionDescriptionLayout.setVisibility(View.GONE);
                b.templateTransactionDescriptionSource.setText(String.format(Locale.US, groupNoText,
                        header.getTransactionDescriptionMatchGroup(),
                        getMatchGroupText(header.getTransactionDescriptionMatchGroup())));

            }
            b.templateTransactionDescriptionSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DESCRIPTION));
            b.templateTransactionDescriptionSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.DESCRIPTION));

            if (header.hasLiteralTransactionComment()) {
                b.templateTransactionCommentSource.setText(
                        R.string.template_details_source_literal);
                b.transactionComment.setText(header.getTransactionComment());
                b.transactionCommentLayout.setVisibility(View.VISIBLE);
            }
            else {
                b.transactionCommentLayout.setVisibility(View.GONE);
                b.templateTransactionCommentSource.setText(String.format(Locale.US, groupNoText,
                        header.getTransactionCommentMatchGroup(),
                        getMatchGroupText(header.getTransactionCommentMatchGroup())));

            }
            b.templateTransactionCommentSourceLabel.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.COMMENT));
            b.templateTransactionCommentSource.setOnClickListener(
                    v -> selectHeaderDetailSource(v, HeaderDetail.COMMENT));

            b.templateDetailsHeadScanQrButton.setOnClickListener(this::scanTestQR);

            checkPatternError(header);
        }
        private void checkPatternError(TemplateDetailsItem.Header item) {
            if (item.getPatternError() != null) {
                b.patternLayout.setError(item.getPatternError());
                b.patternHintTitle.setVisibility(View.GONE);
                b.patternHintText.setVisibility(View.GONE);
            }
            else {
                b.patternLayout.setError(null);
                if (item.testMatch() != null) {
                    b.patternHintText.setText(item.testMatch());
                    b.patternHintTitle.setVisibility(View.VISIBLE);
                    b.patternHintText.setVisibility(View.VISIBLE);
                }
                else {
                    b.patternLayout.setError(null);
                    b.patternHintTitle.setVisibility(View.GONE);
                    b.patternHintText.setVisibility(View.GONE);
                }
            }

        }
        private void scanTestQR(View view) {
            QRScanCapableFragment.triggerQRScan();
        }
    }

    public class AccountRow extends ViewHolder {
        private final TemplateDetailsAccountBinding b;
        public AccountRow(@NonNull TemplateDetailsAccountBinding binding) {
            super(binding.getRoot());
            b = binding;

            TextWatcher accountNameWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    TemplateDetailsItem.AccountRow accRow = getItem();
                    Logger.debug(D_TEMPLATE_UI,
                            "Storing changed account name " + s + "; accRow=" + accRow);
                    accRow.setAccountName(String.valueOf(s));
                }
            };
            b.templateDetailsAccountName.addTextChangedListener(accountNameWatcher);
            b.templateDetailsAccountName.setAdapter(new AccountAutocompleteAdapter(b.getRoot()
                                                                                    .getContext()));
            b.templateDetailsAccountName.setOnItemClickListener(
                    (parent, view, position, id) -> b.templateDetailsAccountName.setText(
                            ((TextView) view).getText()));
            TextWatcher accountCommentWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    TemplateDetailsItem.AccountRow accRow = getItem();
                    Logger.debug(D_TEMPLATE_UI,
                            "Storing changed account comment " + s + "; accRow=" + accRow);
                    accRow.setAccountComment(String.valueOf(s));
                }
            };
            b.templateDetailsAccountComment.addTextChangedListener(accountCommentWatcher);

            b.templateDetailsAccountAmount.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }
                @Override
                public void afterTextChanged(Editable s) {
                    TemplateDetailsItem.AccountRow accRow = getItem();

                    String str = String.valueOf(s);
                    if (Misc.emptyIsNull(str) == null) {
                        accRow.setAmount(null);
                    }
                    else {
                        try {
                            final float amount = Data.parseNumber(str);
                            accRow.setAmount(amount);
                            b.templateDetailsAccountAmountLayout.setError(null);

                            Logger.debug(D_TEMPLATE_UI, String.format(Locale.US,
                                    "Storing changed account amount %s [%4.2f]; accRow=%s", s,
                                    amount, accRow));
                        }
                        catch (NumberFormatException | ParseException e) {
                            b.templateDetailsAccountAmountLayout.setError("!");
                        }
                    }
                }
            });
            b.templateDetailsAccountAmount.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus)
                    return;

                TemplateDetailsItem.AccountRow accRow = getItem();
                if (!accRow.hasLiteralAmount())
                    return;
                Float amt = accRow.getAmount();
                if (amt == null)
                    return;

                b.templateDetailsAccountAmount.setText(Data.formatNumber(amt));
            });

            b.negateAmountSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                getItem().setNegateAmount(isChecked);
                b.templateDetailsNegateAmountText.setText(
                        isChecked ? R.string.template_account_change_amount_sign
                                  : R.string.template_account_keep_amount_sign);
            });
            final View.OnClickListener negLabelClickListener =
                    (view) -> b.negateAmountSwitch.toggle();
            b.templateDetailsNegateAmountLabel.setOnClickListener(negLabelClickListener);
            b.templateDetailsNegateAmountText.setOnClickListener(negLabelClickListener);
            manageAccountLabelDrag();
        }
        @SuppressLint("ClickableViewAccessibility")
        public void manageAccountLabelDrag() {
            b.patternAccountLabel.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this);
                }
                return false;
            });
        }
        @Override
        void bind(TemplateDetailsItem item) {
            final Resources resources = b.getRoot()
                                         .getResources();
                String groupNoText = resources.getString(R.string.template_item_match_group_source);

            TemplateDetailsItem.AccountRow accRow = item.asAccountRowItem();
            b.patternAccountLabel.setText(String.format(Locale.US,
                    resources.getString(R.string.template_details_account_row_label),
                    accRow.getPosition()));
            if (accRow.hasLiteralAccountName()) {
                b.templateDetailsAccountNameLayout.setVisibility(View.VISIBLE);
                b.templateDetailsAccountName.setText(accRow.getAccountName());
                b.templateDetailsAccountNameSource.setText(
                        R.string.template_details_source_literal);
            }
            else {
                b.templateDetailsAccountNameLayout.setVisibility(View.GONE);
                b.templateDetailsAccountNameSource.setText(
                        String.format(Locale.US, groupNoText, accRow.getAccountNameMatchGroup(),
                                getMatchGroupText(accRow.getAccountNameMatchGroup())));
            }

            if (accRow.hasLiteralAccountComment()) {
                b.templateDetailsAccountCommentLayout.setVisibility(View.VISIBLE);
                b.templateDetailsAccountComment.setText(accRow.getAccountComment());
                b.templateDetailsAccountCommentSource.setText(
                        R.string.template_details_source_literal);
            }
            else {
                b.templateDetailsAccountCommentLayout.setVisibility(View.GONE);
                b.templateDetailsAccountCommentSource.setText(
                        String.format(Locale.US, groupNoText, accRow.getAccountCommentMatchGroup(),
                                getMatchGroupText(accRow.getAccountCommentMatchGroup())));
            }

            if (accRow.hasLiteralAmount()) {
                b.templateDetailsAccountAmountSource.setText(
                        R.string.template_details_source_literal);
                b.templateDetailsAccountAmount.setVisibility(View.VISIBLE);
                Float amt = accRow.getAmount();
                b.templateDetailsAccountAmount.setText((amt == null) ? null : String.format(
                        Data.locale.getValue(), "%,4.2f", (accRow.getAmount())));
                b.negateAmountSwitch.setVisibility(View.GONE);
                b.templateDetailsNegateAmountLabel.setVisibility(View.GONE);
                b.templateDetailsNegateAmountText.setVisibility(View.GONE);
            }
            else {
                b.templateDetailsAccountAmountSource.setText(
                        String.format(Locale.US, groupNoText, accRow.getAmountMatchGroup(),
                                getMatchGroupText(accRow.getAmountMatchGroup())));
                b.templateDetailsAccountAmountLayout.setVisibility(View.GONE);
                b.negateAmountSwitch.setVisibility(View.VISIBLE);
                b.negateAmountSwitch.setChecked(accRow.isNegateAmount());
                b.templateDetailsNegateAmountText.setText(
                        accRow.isNegateAmount() ? R.string.template_account_change_amount_sign
                                                : R.string.template_account_keep_amount_sign);
                b.templateDetailsNegateAmountLabel.setVisibility(View.VISIBLE);
                b.templateDetailsNegateAmountText.setVisibility(View.VISIBLE);
            }

            b.templateAccountNameSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.ACCOUNT));
            b.templateDetailsAccountNameSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.ACCOUNT));
            b.templateAccountCommentSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.COMMENT));
            b.templateDetailsAccountCommentSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.COMMENT));
            b.templateAccountAmountSourceLabel.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.AMOUNT));
            b.templateDetailsAccountAmountSource.setOnClickListener(
                    v -> selectAccountRowDetailSource(v, AccDetail.AMOUNT));
        }
        private @NotNull TemplateDetailsItem.AccountRow getItem() {
            return differ.getCurrentList()
                         .get(getAdapterPosition())
                         .asAccountRowItem();
        }
        private void selectAccountRowDetailSource(View v, AccDetail detail) {
            TemplateDetailsItem.AccountRow accRow = getItem();
            final TemplateDetailsItem.Header header = getHeader();
            Logger.debug(D_TEMPLATE_UI, "header is " + header);
            TemplateDetailSourceSelectorFragment sel =
                    TemplateDetailSourceSelectorFragment.newInstance(1, header.getPattern(),
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
            sel.show(activity.getSupportFragmentManager(), "template-details-source-selector");
        }
    }
}
