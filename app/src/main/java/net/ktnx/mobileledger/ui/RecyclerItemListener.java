/*
 * Copyright © 2018 Damyan Ivanov.
 * This file is part of Mobile-Ledger.
 * Mobile-Ledger is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * Mobile-Ledger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mobile-Ledger. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class RecyclerItemListener implements OnItemTouchListener {
    private RecyclerTouchListener listener;
    private GestureDetector gd;

    public interface RecyclerTouchListener {
        void onClickItem(View v, int position);
        void onLongClickItem(View v, int position);
    }

    public RecyclerItemListener(Context ctx, RecyclerView rv, RecyclerTouchListener listener) {
        this.listener = listener;
        this.gd = new GestureDetector(
                ctx, new GestureDetector.SimpleOnGestureListener() {
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
        }
        );
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
}
