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

package net.ktnx.mobileledger.ui.activity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import net.ktnx.mobileledger.ui.CrashReportDialogFragment;

import java.io.PrintWriter;
import java.io.StringWriter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class CrashReportingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);

                pw.format("OS version: %s; API level %d\n\n", Build.VERSION.RELEASE,
                        Build.VERSION.SDK_INT);
                e.printStackTrace(pw);

                Log.e(null, sw.toString());

                CrashReportDialogFragment df = new CrashReportDialogFragment();
                df.setCrashReportText(sw.toString());
                df.show(getSupportFragmentManager(), "crash_report");
            }
        });
        Log.d("crash", "Uncaught exception handler set");
    }
}
