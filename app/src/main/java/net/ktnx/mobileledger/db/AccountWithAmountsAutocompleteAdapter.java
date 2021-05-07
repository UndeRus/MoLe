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

package net.ktnx.mobileledger.db;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.dao.AccountDAO;
import net.ktnx.mobileledger.model.Data;
import net.ktnx.mobileledger.utils.Logger;
import net.ktnx.mobileledger.utils.Misc;

import java.util.List;

public class AccountWithAmountsAutocompleteAdapter extends ArrayAdapter<AccountWithAmounts> {
    private final AccountFilter filter = new AccountFilter();
    private final long profileId;
    public AccountWithAmountsAutocompleteAdapter(Context context, @NonNull Profile profile) {
        super(context, R.layout.account_autocomplete_row);
        profileId = profile.getId();
    }
    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(parent.getContext())
                                 .inflate(R.layout.account_autocomplete_row, parent, false);
        }
        AccountWithAmounts item = getItem(position);
        ((TextView) view.findViewById(R.id.account_name)).setText(item.account.getName());
        StringBuilder amountsText = new StringBuilder();
        for (AccountValue amt : item.amounts) {
            if (amountsText.length() != 0)
                amountsText.append('\n');
            String currency = amt.getCurrency();
            if (Misc.emptyIsNull(currency) != null)
                amountsText.append(currency)
                           .append(' ');
            amountsText.append(Data.formatNumber(amt.getValue()));
        }
        ((TextView) view.findViewById(R.id.amounts)).setText(amountsText.toString());

        return view;
    }
    class AccountFilter extends Filter {
        private final AccountDAO dao = DB.get()
                                         .getAccountDAO();
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null) {
                results.count = 0;
                return results;
            }

            Logger.debug("acc", String.format("Looking for account '%s'", constraint));
            final List<AccountWithAmounts> matches =
                    dao.lookupWithAmountsInProfileByNameSync(profileId, String.valueOf(constraint)
                                                                              .toUpperCase());
            results.values = matches;
            results.count = matches.size();

            return results;
        }
        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            if (results.values == null) {
                notifyDataSetInvalidated();
            }
            else {
                setNotifyOnChange(false);
                clear();
                addAll((List<AccountWithAmounts>) results.values);
                notifyDataSetChanged();
            }
        }
    }
}
