/*
 * Copyright © 2019 Damyan Ivanov.
 *  This file is part of MoLe.
 *  MoLe is free software: you can distribute it and/or modify it
 *  under the term of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your opinion), any later version.
 *
 *  MoLe is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License terms for details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.profiles;

import android.content.Context;
import android.util.AttributeSet;

import net.ktnx.mobileledger.utils.Colors;

import androidx.appcompat.widget.AppCompatTextView;

public class MoLeColoredTextView extends AppCompatTextView {
    public MoLeColoredTextView(Context context) {
        super(context);
    }
    public MoLeColoredTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MoLeColoredTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        int deg = (text == null || text.equals("")) ? -1 : Integer.valueOf(String.valueOf(text));
        if (deg == -1) deg = Colors.DEFAULT_HUE_DEG;
        setBackgroundColor(Colors.getPrimaryColorForHue(deg));
    }
}
