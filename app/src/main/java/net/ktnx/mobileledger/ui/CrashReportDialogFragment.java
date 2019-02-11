package net.ktnx.mobileledger.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.utils.Globals;

public class CrashReportDialogFragment extends DialogFragment {
    private String mCrashReportText;
    private ScrollView repScroll = null;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        if (savedInstanceState != null)
            mCrashReportText = savedInstanceState.getString("crash_text");

        View view = inflater.inflate(R.layout.crash_dialog, null);
        ((TextView) view.findViewById(R.id.textCrashReport)).setText(mCrashReportText);
        repScroll = view.findViewById(R.id.scrollText);
        builder.setTitle(R.string.crash_dialog_title).setView(view)
                .setPositiveButton(R.string.btn_send_crash_report,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // still nothing
                                Intent email = new Intent(Intent.ACTION_SEND);
                                email.putExtra(Intent.EXTRA_EMAIL,
                                        new String[]{Globals.developerEmail});
                                email.putExtra(Intent.EXTRA_SUBJECT, "MoLe crash report");
                                email.putExtra(Intent.EXTRA_TEXT, mCrashReportText);
                                email.setType("message/rfc822");
                                startActivity(Intent.createChooser(email,
                                        getResources().getString(R.string.send_crash_via)));
                            }
                        })
                .setNegativeButton(R.string.btn_not_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CrashReportDialogFragment.this.getDialog().cancel();
                    }
                })
                .setNeutralButton(R.string.btn_show_report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogIinterface) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (repScroll != null) {
                                    repScroll.setVisibility(View.VISIBLE);
                                    v.setVisibility(View.GONE);
                                }
                            }
                        });
            }
        });
        return dialog;
    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("crash_text", mCrashReportText);
    }
    public void setCrashReportText(String text) {
        mCrashReportText = text;
    }
}
