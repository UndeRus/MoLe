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
        implements DatePickerDialog.OnDateSetListener, DatePicker.OnDateChangedListener {
    static final Pattern reYMD = Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reMD = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reD = Pattern.compile("\\s*(\\d+)\\s*$");
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = GregorianCalendar.getInstance();
        int year = c.get(GregorianCalendar.YEAR);
        int month = c.get(GregorianCalendar.MONTH);
        int day = c.get(GregorianCalendar.DAY_OF_MONTH);
        TextView date =
                Objects.requireNonNull(getActivity()).findViewById(R.id.new_transaction_date);

        CharSequence present = date.getText();

        Matcher m = reYMD.matcher(present);
        if (m.matches()) {
            year = Integer.parseInt(m.group(1));
            month = Integer.parseInt(m.group(2)) - 1;   // month is 0-based
            day = Integer.parseInt(m.group(3));
        }
        else {
            m = reMD.matcher(present);
            if (m.matches()) {
                month = Integer.parseInt(m.group(1)) - 1;
                day = Integer.parseInt(m.group(2));
            }
            else {
                m = reD.matcher(present);
                if (m.matches()) {
                    day = Integer.parseInt(m.group(1));
                }
            }
        }

        DatePickerDialog dpd =
                new DatePickerDialog(Objects.requireNonNull(getActivity()), this, year, month, day);
        // quicker date selection available in API 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DatePicker dp = dpd.getDatePicker();
            dp.setOnDateChangedListener(this);
        }

        return dpd;
    }
    private void updateDateInput(int year, int month, int day) {
        TextView date =
                Objects.requireNonNull(getActivity()).findViewById(R.id.new_transaction_date);

        final Calendar c = GregorianCalendar.getInstance();
        if (c.get(GregorianCalendar.YEAR) == year) {
            if (c.get(GregorianCalendar.MONTH) == month)
                date.setText(String.format(Locale.US, "%d", day));
            else date.setText(String.format(Locale.US, "%d/%d", month + 1, day));
        }
        else date.setText(String.format(Locale.US, "%d/%d/%d", year, month + 1, day));

        TextView description = Objects.requireNonNull(getActivity())
                .findViewById(R.id.new_transaction_description);
        description.requestFocus();
    }
    @TargetApi(Build.VERSION_CODES.O)
    public void onDateSet(DatePicker view, int year, int month, int day) {
        updateDateInput(year, month, day);
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        updateDateInput(year, monthOfYear, dayOfMonth);

        this.dismiss();
    }
}
