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
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import net.ktnx.mobileledger.dao.TransactionDAO;
import net.ktnx.mobileledger.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class TransactionDescriptionAutocompleteAdapter extends ArrayAdapter<String> {
    private final TransactionFilter filter = new TransactionFilter();
    private final TransactionDAO dao = DB.get()
                                         .getTransactionDAO();
    public TransactionDescriptionAutocompleteAdapter(Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
    }
    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }
    class TransactionFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null) {
                results.count = 0;
                return results;
            }

            Logger.debug("acc", String.format("Looking for description '%s'", constraint));
            final List<String> matches = TransactionDAO.unbox(dao.lookupDescriptionSync(
                    String.valueOf(constraint)
                          .toUpperCase()));
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
                addAll((List<String>) results.values);
                notifyDataSetChanged();
            }
        }
    }
}
