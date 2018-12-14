/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui;

import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.widget.DatePicker;
import android.widget.TextView;

import net.ktnx.mobileledger.R;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatePickerFragment extends AppCompatDialogFragment
implements DatePickerDialog.OnDateSetListener, DatePicker.OnDateChangedListener
{
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = GregorianCalendar.getInstance();
        int year = c.get(GregorianCalendar.YEAR);
        int month = c.get(GregorianCalendar.MONTH);
        int day = c.get(GregorianCalendar.DAY_OF_MONTH);
        TextView date = Objects.requireNonNull(getActivity()).findViewById(R.id.new_transaction_date);

        CharSequence present = date.getText();

        Pattern re_mon_day = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
        Matcher m_mon_day = re_mon_day.matcher(present);

        if (m_mon_day.matches()) {
            month = Integer.parseInt(m_mon_day.group(1))-1;
            day = Integer.parseInt(m_mon_day.group(2));
        }
        else {
            Pattern re_day = Pattern.compile("^\\s*(\\d{1,2})\\s*$");
            Matcher m_day = re_day.matcher(present);
            if (m_day.matches()) {
                day = Integer.parseInt(m_day.group(1));
            }
        }

        DatePickerDialog dpd =  new DatePickerDialog(Objects.requireNonNull(getActivity()), this, year, month, day);
        // quicker date selection available in API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DatePicker dp = dpd.getDatePicker();
            dp.setOnDateChangedListener(this);
        }

        return dpd;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public void onDateSet(DatePicker view, int year, int month, int day) {
        TextView date = Objects.requireNonNull(getActivity()).findViewById(R.id.new_transaction_date);

        final Calendar c = GregorianCalendar.getInstance();
        if ( c.get(GregorianCalendar.YEAR) == year && c.get(GregorianCalendar.MONTH) == month) {
            date.setText(String.format(Locale.US, "%d", day));
        }
        else {
            date.setText(String.format(Locale.US, "%d/%d", month+1, day));
        }

        TextView description = Objects.requireNonNull(getActivity())
                .findViewById(R.id.new_transaction_description);
        description.requestFocus();
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        TextView date = Objects.requireNonNull(getActivity()).findViewById(R.id.new_transaction_date);

        final Calendar c = GregorianCalendar.getInstance();
        if ( c.get(GregorianCalendar.YEAR) == year && c.get(GregorianCalendar.MONTH) == monthOfYear) {
            date.setText(String.format(Locale.US, "%d", dayOfMonth));
        }
        else {
            date.setText(String.format(Locale.US, "%d/%d", monthOfYear+1, dayOfMonth));
        }

        TextView description = Objects.requireNonNull(getActivity())
                .findViewById(R.id.new_transaction_description);
        description.requestFocus();

        this.dismiss();
    }
}
