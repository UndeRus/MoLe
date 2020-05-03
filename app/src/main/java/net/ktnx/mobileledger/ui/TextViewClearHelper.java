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

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import net.ktnx.mobileledger.R;

public class TextViewClearHelper {
    private boolean hadText = false;
    private boolean hasFocus = false;
    private EditText view;
    private TextWatcher textWatcher;
    private View.OnFocusChangeListener prevOnFocusChangeListener;
    public void detachFromView() {
        if (view == null)
            return;
        view.removeTextChangedListener(textWatcher);
        prevOnFocusChangeListener = null;
        textWatcher = null;
        hasFocus = false;
        hadText = false;
        view = null;
    }
    @SuppressLint("ClickableViewAccessibility")
    public void attachToTextView(EditText view) {
        if (this.view != null)
            detachFromView();
        this.view = view;
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
            @Override
            public void afterTextChanged(Editable s) {
                boolean hasText = s.length() > 0;

                if (hasFocus) {
                    if (hadText && !hasText)
                        hideClearDrawable();
                    if (!hadText && hasText)
                        showClearDrawable();
                }

                hadText = hasText;

            }
        };
        view.addTextChangedListener(textWatcher);
        prevOnFocusChangeListener = view.getOnFocusChangeListener();
        view.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (view.getText()
                        .length() > 0)
                {
                    showClearDrawable();
                }
            }
            else {
                hideClearDrawable();
            }

            this.hasFocus = hasFocus;

            if (prevOnFocusChangeListener != null)
                prevOnFocusChangeListener.onFocusChange(v, hasFocus);
        });

        view.setOnTouchListener((v, event) -> this.onTouchEvent(view, event));
    }
    private void hideClearDrawable() {
        view.setCompoundDrawablesRelative(null, null, null, null);
    }
    private void showClearDrawable() {
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_clear_accent_24dp,
                0);
    }
    public boolean onTouchEvent(EditText view, MotionEvent event) {
        if ((event.getAction() == MotionEvent.ACTION_UP) && (view.getText()
                                                                 .length() > 0))
        {
            boolean clearClicked = false;
            final float x = event.getX();
            final int vw = view.getWidth();
            // start, top, end, bottom (end == 2)
            Drawable dwb = view.getCompoundDrawablesRelative()[2];
            if (dwb != null) {
                final int dw = dwb.getBounds()
                                  .width();
                if (view.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    if ((x > vw - dw))
                        clearClicked = true;
                }
                else {
                    if (x < vw - dw)
                        clearClicked = true;
                }
                if (clearClicked) {
                    view.setText("");
                    view.requestFocus();
                    return true;
                }
            }
        }

        return false;
    }

}
