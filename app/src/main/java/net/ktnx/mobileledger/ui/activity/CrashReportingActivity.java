package net.ktnx.mobileledger.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.ktnx.mobileledger.ui.CrashReportDialogFragment;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashReportingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
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
