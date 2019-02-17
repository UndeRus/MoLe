package net.ktnx.mobileledger.ui.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import net.ktnx.mobileledger.ui.CrashReportDialogFragment;
import net.ktnx.mobileledger.utils.Colors;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class CrashReportingActivity extends AppCompatActivity {
    protected void setupProfileColors() {
        Colors.setupTheme(this);
    }
    @Override
    protected void onStart() {
        super.onStart();
        Colors.refreshColors(getTheme());
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Colors.setupTheme(this);

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
