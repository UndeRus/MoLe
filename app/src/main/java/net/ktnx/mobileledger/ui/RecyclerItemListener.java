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

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;

public class RecyclerItemListener implements OnItemTouchListener {
    private final GestureDetector gd;

    public RecyclerItemListener(Context ctx, RecyclerView rv, RecyclerTouchListener listener) {
        this.gd = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                View v = rv.findChildViewUnder(e.getX(), e.getY());
                listener.onLongClickItem(v, rv.getChildAdapterPosition(v));
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View v = rv.findChildViewUnder(e.getX(), e.getY());
                listener.onClickItem(v, rv.getChildAdapterPosition(v));
                return true;
            }
        });
    }
    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView,
                                         @NonNull MotionEvent motionEvent) {
        View v = recyclerView.findChildViewUnder(motionEvent.getX(), motionEvent.getY());
        return (v != null) && gd.onTouchEvent(motionEvent);
    }
    @Override
    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent motionEvent) {

    }
    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean b) {

    }

    public interface RecyclerTouchListener {
        void onClickItem(View v, int position);

        void onLongClickItem(View v, int position);
    }
}
