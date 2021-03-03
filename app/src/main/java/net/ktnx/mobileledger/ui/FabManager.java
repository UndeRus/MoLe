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

package net.ktnx.mobileledger.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.ktnx.mobileledger.utils.DimensionUtils;
import net.ktnx.mobileledger.utils.Logger;

public class FabManager {
    private static final boolean FAB_SHOWN = true;
    private static final boolean FAB_HIDDEN = false;
    private final FloatingActionButton fab;
    private boolean wantedFabState = FAB_SHOWN;
    private ViewPropertyAnimator fabSlideAnimator;
    private int fabVerticalOffset;
    public FabManager(FloatingActionButton fab) {
        this.fab = fab;
    }
    @SuppressLint("ClickableViewAccessibility")
    public static void handle(FabHandler activity, RecyclerView recyclerView) {
        final float triggerAbsolutePixels = DimensionUtils.dp2px(activity.getContext(), 20f);
        final float triggerRelativePixels = triggerAbsolutePixels / 4f;
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                Logger.debug("touch", "Scrolled " + dy);
                if (dy <= 0)
                    activity.showManagedFab();
                else
                    activity.hideManagedFab();

                super.onScrolled(recyclerView, dx, dy);
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            private float absoluteAnchor = -1;
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        absoluteAnchor = e.getRawY();
//                        Logger.debug("touch",
//                                String.format(Locale.US, "Touch down at %4.2f", absoluteAnchor));
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (absoluteAnchor < 0)
                            break;

                        final float absoluteY = e.getRawY();
//                        Logger.debug("touch", String.format(Locale.US, "Move to %4.2f", absoluteY));

                        if (absoluteY > absoluteAnchor + triggerAbsolutePixels) {
                            // swipe down
//                            Logger.debug("touch", "SHOW");
                            activity.showManagedFab();
                            absoluteAnchor = absoluteY;
                        }
                        else if (absoluteY < absoluteAnchor - triggerAbsolutePixels) {
                            // swipe up
//                            Logger.debug("touch", "HIDE");
                            activity.hideManagedFab();
                            absoluteAnchor = absoluteY;
                        }

                        break;
                }
                return false;
            }
        });
    }
    private void slideFabTo(int target, long duration, TimeInterpolator interpolator) {
        fabSlideAnimator = fab.animate()
                              .translationY((float) target)
                              .setInterpolator(interpolator)
                              .setDuration(duration)
                              .setListener(new AnimatorListenerAdapter() {
                                  public void onAnimationEnd(Animator animation) {
                                      fabSlideAnimator = null;
                                  }
                              });
    }
    public void showFab() {
        if (wantedFabState == FAB_SHOWN)
            return;

//        b.btnAddTransaction.show();
        if (this.fabSlideAnimator != null) {
            this.fabSlideAnimator.cancel();
            fab.clearAnimation();
        }

        wantedFabState = FAB_SHOWN;
        slideFabTo(0, 200L,
                com.google.android.material.animation.AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR);
    }
    public void hideFab() {
        if (wantedFabState == FAB_HIDDEN)
            return;

        calcVerticalFabOffset();

//        b.btnAddTransaction.hide();
        if (this.fabSlideAnimator != null) {
            this.fabSlideAnimator.cancel();
            fab.clearAnimation();
        }

        wantedFabState = FAB_HIDDEN;
        slideFabTo(fabVerticalOffset, 150L,
                com.google.android.material.animation.AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR);
    }
    private void calcVerticalFabOffset() {
        if (fabVerticalOffset > 0)
            return;// already calculated
        int top = fab.getTop();
        ViewParent parent = fab.getParent();
        while (parent != null && !(parent instanceof View))
            parent = parent.getParent();

        if (parent instanceof View) {
            View parentView = (View) parent;
            int parentHeight = parentView.getHeight();
            fabVerticalOffset = parentHeight - top;
        }
    }
    public interface FabHandler {
        Context getContext();

        void showManagedFab();

        void hideManagedFab();
    }
}
