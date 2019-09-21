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

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import net.ktnx.mobileledger.R;

public final class AutoCompleteTextViewWithClear extends AppCompatAutoCompleteTextView {
    private boolean hadText = false;

    public AutoCompleteTextViewWithClear(Context context) {
        super(context);
    }
    public AutoCompleteTextViewWithClear(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public AutoCompleteTextViewWithClear(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            if (getText().length() > 0) {
                showClearDrawable();
            }
        }
        else {
            hideClearDrawable();
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }
    private void hideClearDrawable() {
        setCompoundDrawablesRelative(null, null, null, null);
    }
    private void showClearDrawable() {
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_clear_black_24dp, 0);
    }
    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        final boolean hasText = text.length() > 0;

        if (hasFocus()) {
            if (hadText && !hasText) hideClearDrawable();
            if (!hadText && hasText) showClearDrawable();
        }

        hadText = hasText;

        super.onTextChanged(text, start, lengthBefore, lengthAfter);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) if (getText().length() > 0) {
            boolean clearClicked = false;
            final float x = event.getX();
            final int vw = getWidth();
            // start, top, end, bottom (end == 2)
            Drawable dwb = getCompoundDrawablesRelative()[2];
            if (dwb != null) {
                final int dw = dwb.getBounds().width();
                if (getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    if ((x > vw - dw)) clearClicked = true;
                }
                else {
                    if (x < vw - dw) clearClicked = true;
                }
                if (clearClicked) {
                    setText("");
                    requestFocus();
                    performClick();
                    return true;
                }
            }
        }

        return super.onTouchEvent(event);
    }
    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
