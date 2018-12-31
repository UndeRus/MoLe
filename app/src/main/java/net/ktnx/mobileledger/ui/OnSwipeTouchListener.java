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
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public abstract class OnSwipeTouchListener implements View.OnTouchListener {
    public final GestureDetector gestureDetector;

    protected OnSwipeTouchListener(Context ctx) {
        gestureDetector = new GestureDetector(ctx, new GestureListener() );
    }

    private final class GestureListener extends SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d("sw-l", "onDown");
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;

            Log.d("sw-l", "onFling");

            try {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            Log.d("sw-l", "calling onSwipeRight");
                            onSwipeRight();
                        }
                        else {
                            Log.d("sw-l", "calling onSwipeLeft");
                            onSwipeLeft();
                        }
                    }
                    result = true;
                }
                else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeDown();
                    }
                    else {
                        onSwipeUp();
                    }
                    result = true;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    public void onSwipeRight() {}
    public void onSwipeLeft() {
        Log.d("sw-l", "LEFT");
    }
    public void onSwipeUp() {}
    public void onSwipeDown() {}
}
