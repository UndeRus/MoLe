/*
 * Copyright © 2021 Damyan Ivanov.
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

//
// Substantial portions taken from DividerItemDecoration subject to the following license terms:
//
// Copyright 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package net.ktnx.mobileledger.ui.templates;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;

class TemplateListDivider extends DividerItemDecoration {
    private final Rect mBounds = new Rect();
    private int mOrientation;
    public TemplateListDivider(Context context, int orientation) {
        super(context, orientation);
        mOrientation = orientation;
    }
    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        mOrientation = orientation;
    }
    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (parent.getLayoutManager() == null || getDrawable() == null) {
            return;
        }
        if (mOrientation == VERTICAL) {
            drawVertical(c, parent);
        }
        else {
            drawHorizontal(c, parent);
        }
    }

    private void drawVertical(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int left;
        final int right;
        //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
        if (parent.getClipToPadding()) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
            canvas.clipRect(left, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        }
        else {
            left = 0;
            right = parent.getWidth();
        }

        final Drawable divider = Objects.requireNonNull(getDrawable());
        final int childCount = parent.getChildCount();
        final TemplatesRecyclerViewAdapter adapter =
                (TemplatesRecyclerViewAdapter) Objects.requireNonNull(parent.getAdapter());
        final int itemCount = adapter.getItemCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final int childAdapterPosition = parent.getChildAdapterPosition(child);
            if (adapter.getItemViewType(childAdapterPosition) ==
                TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER ||
                childAdapterPosition + 1 < itemCount &&
                adapter.getItemViewType(childAdapterPosition + 1) ==
                TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER)
                continue;
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            final int bottom = mBounds.bottom + Math.round(child.getTranslationY());
            final int top = bottom - divider.getIntrinsicHeight();
            divider.setBounds(left, top, right, bottom);
            divider.draw(canvas);
        }
        canvas.restore();
    }

    private void drawHorizontal(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int top;
        final int bottom;
        //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
        if (parent.getClipToPadding()) {
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
            canvas.clipRect(parent.getPaddingLeft(), top,
                    parent.getWidth() - parent.getPaddingRight(), bottom);
        }
        else {
            top = 0;
            bottom = parent.getHeight();
        }

        final Drawable divider = Objects.requireNonNull(getDrawable());
        final int childCount = parent.getChildCount();
        final TemplatesRecyclerViewAdapter adapter =
                (TemplatesRecyclerViewAdapter) Objects.requireNonNull(parent.getAdapter());
        final int itemCount = adapter.getItemCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final int childAdapterPosition = parent.getChildAdapterPosition(child);
            if (adapter.getItemViewType(childAdapterPosition) ==
                TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER ||
                childAdapterPosition + 1 < itemCount &&
                adapter.getItemViewType(childAdapterPosition + 1) ==
                TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER)
                continue;
            parent.getLayoutManager()
                  .getDecoratedBoundsWithMargins(child, mBounds);
            final int right = mBounds.right + Math.round(child.getTranslationX());
            final int left = right - divider.getIntrinsicWidth();
            divider.setBounds(left, top, right, bottom);
            divider.draw(canvas);
        }
        canvas.restore();
    }
    @Override
    public void getItemOffsets(Rect outRect, View child, RecyclerView parent,
                               RecyclerView.State state) {
        final int childAdapterPosition = parent.getChildAdapterPosition(child);
        final TemplatesRecyclerViewAdapter adapter =
                (TemplatesRecyclerViewAdapter) Objects.requireNonNull(parent.getAdapter());
        final int itemCount = adapter.getItemCount();

        if (adapter.getItemViewType(childAdapterPosition) ==
            TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER ||
            childAdapterPosition + 1 < itemCount &&
            adapter.getItemViewType(childAdapterPosition + 1) ==
            TemplatesRecyclerViewAdapter.ITEM_TYPE_DIVIDER)
            return;

        super.getItemOffsets(outRect, child, parent, state);
    }
}
