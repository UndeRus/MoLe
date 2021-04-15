/*
 * Copyright © 2020 Damyan Ivanov.
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.model.FutureDates;
import net.ktnx.mobileledger.utils.SimpleDate;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatePickerFragment extends AppCompatDialogFragment
        implements CalendarView.OnDateChangeListener {
    static final Pattern reYMD = Pattern.compile("^\\s*(\\d+)\\d*/\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reMD = Pattern.compile("^\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    static final Pattern reD = Pattern.compile("\\s*(\\d+)\\s*$");
    private final Calendar presentDate = GregorianCalendar.getInstance();
    private DatePickedListener onDatePickedListener;
    private long minDate = 0;
    private long maxDate = Long.MAX_VALUE;
    public void setDateRange(@Nullable SimpleDate minDate, @Nullable SimpleDate maxDate) {
        if (minDate == null)
            this.minDate = 0;
        else
            this.minDate = minDate.toDate().getTime();

        if (maxDate == null)
            this.maxDate = Long.MAX_VALUE;
        else
            this.maxDate = maxDate.toDate().getTime();
    }
    public void setFutureDates(FutureDates futureDates) {
        if (futureDates == FutureDates.All) {
            maxDate = Long.MAX_VALUE;
        }
        else {
            final Calendar dateLimit = GregorianCalendar.getInstance();
            switch (futureDates) {
                case None:
                    // already there
                    break;
                case OneWeek:
                    dateLimit.add(Calendar.DAY_OF_MONTH, 7);
                    break;
                case TwoWeeks:
                    dateLimit.add(Calendar.DAY_OF_MONTH, 14);
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
            maxDate = dateLimit.getTime()
                               .getTime();
        }
    }
    public void setCurrentDateFromText(CharSequence present) {
        final Calendar now = GregorianCalendar.getInstance();
        int year = now.get(GregorianCalendar.YEAR);
        int month = now.get(GregorianCalendar.MONTH);
        int day = now.get(GregorianCalendar.DAY_OF_MONTH);

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

        presentDate.set(year, month, day);
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dpd = new Dialog(requireActivity());
        dpd.setContentView(R.layout.date_picker_view);
        dpd.setTitle(null);
        CalendarView cv = dpd.findViewById(R.id.calendarView);
        cv.setDate(presentDate.getTime()
                              .getTime());

        cv.setMinDate(minDate);
        cv.setMaxDate(maxDate);

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
