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
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.MobileLedgerProfile;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatePickerFragment extends AppCompatDialogFragment
        implements CalendarView.OnDateChangeListener {
    static final Pattern reYMD = Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reMD = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reD = Pattern.compile("\\s*(\\d+)\\s*$");
    private DatePickedListener onDatePickedListener;
    private MobileLedgerProfile.FutureDates futureDates = MobileLedgerProfile.FutureDates.None;
    public MobileLedgerProfile.FutureDates getFutureDates() {
        return futureDates;
    }
    public void setFutureDates(MobileLedgerProfile.FutureDates futureDates) {
        this.futureDates = futureDates;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = GregorianCalendar.getInstance();
        int year = c.get(GregorianCalendar.YEAR);
        int month = c.get(GregorianCalendar.MONTH);
        int day = c.get(GregorianCalendar.DAY_OF_MONTH);
        TextView date = Objects.requireNonNull(getActivity())
                               .findViewById(R.id.new_transaction_date);

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

        c.set(year, month, day);

        Dialog dpd = new Dialog(Objects.requireNonNull(getActivity()));
        dpd.setContentView(R.layout.date_picker_view);
        dpd.setTitle(null);
        CalendarView cv = dpd.findViewById(R.id.calendarView);
        cv.setDate(c.getTime()
                    .getTime());

        if (futureDates == MobileLedgerProfile.FutureDates.All) {
            cv.setMaxDate(Long.MAX_VALUE);
        }
        else {
            final Calendar dateLimit = GregorianCalendar.getInstance();
            switch (futureDates) {
                case None:
                    // already there
                    break;
                case OneMonth:
                    dateLimit.add(Calendar.MONTH, 1);
                    break;
                case TwoMonths:
                    dateLimit.add(Calendar.MONTH, 2);
                    break;
                case ThreeMonths:
                    dateLimit.add(Calendar.MONTH, 3);
                    break;
                case SixMonths:
                    dateLimit.add(Calendar.MONTH, 6);
                    break;
                case OneYear:
                    dateLimit.add(Calendar.YEAR, 1);
                    break;
            }
            cv.setMaxDate(dateLimit.getTime()
                                   .getTime());
        }

        cv.setOnDateChangeListener(this);

        return dpd;
    }
    @Override
    public void onSelectedDayChange(@NonNull CalendarView view, int year, int month,
                                    int dayOfMonth) {
        this.dismiss();
        if (onDatePickedListener != null)
            onDatePickedListener.onDatePicked(year, month, dayOfMonth);
    }
    public void setOnDatePickedListener(DatePickedListener listener) {
        onDatePickedListener = listener;
    }
    public interface DatePickedListener {
        void onDatePicked(int year, int month, int day);
    }
}
