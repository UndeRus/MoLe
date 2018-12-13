package net.ktnx.mobileledger;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnItemTouchListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

class RecyclerItemListener implements OnItemTouchListener {
    private RecyclerTouchListener listener;
    private GestureDetector gd;

    interface RecyclerTouchListener {
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
