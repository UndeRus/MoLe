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
import android.content.Context;
import android.os.Bundle;

import net.ktnx.mobileledger.R;
import net.ktnx.mobileledger.utils.Colors;

import androidx.annotation.NonNull;

public class HueRingDialog extends Dialog {
    private int initialHue;
    private HueRing hueRing;
    private HueSelectedListener listener;

    public HueRingDialog(@NonNull Context context, int initialHue) {
        super(context);
        this.initialHue = initialHue;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hue_dialog);
        hueRing = findViewById(R.id.ring);
        hueRing.setInitialHue(initialHue);
        hueRing.setHue(initialHue);

        findViewById(R.id.btn_ok).setOnClickListener(v -> {
            if (listener != null) listener.onHueSelected(hueRing.getHueDegrees());

            dismiss();
        });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        findViewById(R.id.btn_default)
                .setOnClickListener(v -> hueRing.setHue(Colors.DEFAULT_HUE_DEG));
    }
    public void setColorSelectedListener(HueSelectedListener listener) {
        this.listener = listener;
    }
    public interface HueSelectedListener {
        void onHueSelected(int hue);
    }
}
