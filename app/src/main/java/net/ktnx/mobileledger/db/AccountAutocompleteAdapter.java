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

import net.ktnx.mobileledger.dao.AccountDAO;

import java.util.ArrayList;
import java.util.List;

public class AccountAutocompleteAdapter extends ArrayAdapter<Account> {
    private final AccountFilter filter = new AccountFilter();
    private final AccountDAO dao = DB.get()
                                     .getAccountDAO();
    private String profileUUID;
    public AccountAutocompleteAdapter(Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
    }
    public void setProfileUUID(String profileUUID) {
        this.profileUUID = profileUUID;
    }
    @NonNull
    @Override
    public Filter getFilter() {
        return filter;
    }
    //    @NonNull
//    @Override
//    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//        View view = convertView;
//        if (view == null) {
//            view = LayoutInflater.from(parent.getContext())
//                                 .inflate(android.R.layout.simple_dropdown_item_1line, parent,
//                                         false);
//        }
//        Account item = getItem(position);
//        ((TextView) view.findViewById(android.R.id.text1)).setText(item.getName());
//        return view;
//    }
    class AccountFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null) {
                results.count = 0;
                return results;
            }

            final List<Account> matches =
                    (profileUUID == null) ? dao.lookupByNameSync(String.valueOf(constraint))
                                          : dao.lookupInProfileByNameSync(profileUUID,
                                                  String.valueOf(constraint));
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
                addAll((List<Account>) results.values);
                notifyDataSetChanged();
            }
        }
    }
}
