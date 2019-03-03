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

package net.ktnx.mobileledger.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import net.ktnx.mobileledger.utils.Colors;

import androidx.annotation.Nullable;

public class HueRing extends View {
    private final int markerWidthDegrees = 10;
    private Paint ringPaint, initialPaint, currentPaint;
    private int centerX, centerY;
    private int diameter;
    private int padding;
    private int initialHueDegrees;
    private int color, hueDegrees;
    private float radius;
    private float bandWidth;
    private float ringR;
    private float innerDiameter;
    private float centerR;
    private RectF centerRect;
    private RectF ringRect;
    private int markerOverflow;
    public HueRing(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(Colors.DEFAULT_HUE_DEG);
    }
    public HueRing(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(Colors.DEFAULT_HUE_DEG);
    }
    public HueRing(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                   int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(Colors.DEFAULT_HUE_DEG);
    }
    public HueRing(Context context) {
        super(context);
        init(Colors.DEFAULT_HUE_DEG);
    }
    public HueRing(Context context, int initialHueDegrees) {
        super(context);
        init(initialHueDegrees);
    }
    private void init(int initialHueDegrees) {
        final int[] steps = {0xFF000000 | Colors.getPrimaryColorForHue(0),      // red
                             0xFF000000 | Colors.getPrimaryColorForHue(60),     // yellow
                             0xFF000000 | Colors.getPrimaryColorForHue(120),    // green
                             0xFF000000 | Colors.getPrimaryColorForHue(180),    // cyan
                             0xFF000000 | Colors.getPrimaryColorForHue(240),    // blue
                             0xFF000000 | Colors.getPrimaryColorForHue(300),    // magenta
                             0xFF000000 | Colors.getPrimaryColorForHue(360),    // red, again
        };
        Shader rainbow = new SweepGradient(0, 0, steps, null);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setShader(rainbow);
        ringPaint.setStyle(Paint.Style.STROKE);

        initialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        initialPaint.setStyle(Paint.Style.FILL);

        currentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        currentPaint.setStyle(Paint.Style.FILL);

        setInitialHue(initialHueDegrees);
        setHue(initialHueDegrees);
    }
    public int getColor() {
        return color;
    }
    public int getHueDegrees() {
        return hueDegrees;
    }
    public void setHue(int hueDegrees) {
        if (hueDegrees != Colors.DEFAULT_HUE_DEG) {
            // round to 15 degrees
            int rem = hueDegrees % 15;
            if (rem < 8) hueDegrees -= rem;
            else hueDegrees += 15 - rem;
        }

        this.hueDegrees = hueDegrees;
        this.color = Colors.getPrimaryColorForHue(hueDegrees);
        currentPaint.setColor(this.color);
        invalidate();
    }
    private void setHue(float hue) {
        setHue((int) (360f * hue));
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        ringPaint.setStrokeWidth((int) bandWidth);

        canvas.translate(centerX, centerY);
        canvas.drawOval(ringRect, ringPaint);

        canvas.drawArc(centerRect, 180, 180, true, initialPaint);
        canvas.drawArc(centerRect, 0, 180, true, currentPaint);

        drawMarker(canvas);
    }
    private void drawMarker(Canvas canvas) {
        // TODO
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        if (((widthMode == MeasureSpec.AT_MOST) && (heightMode == MeasureSpec.AT_MOST)) ||
            ((widthMode == MeasureSpec.EXACTLY) && (heightMode == MeasureSpec.EXACTLY)))
        {
            diameter = Math.min(widthSize, heightSize);
        }

        setMeasuredDimension(diameter, diameter);

//        padding = DimensionUtils.dp2px(getContext(),
//                getContext().getResources().getDimension(R.dimen.activity_horizontal_margin)) / 2;
        padding = 0;
        diameter -= 2 * padding;
        radius = diameter / 2f;
        centerX = padding + (int) radius;
        centerY = centerX;

        bandWidth = diameter / 3.5f;
        ringR = radius - bandWidth / 2f;

        ringRect = new RectF(-ringR, -ringR, ringR, ringR);

        innerDiameter = diameter - 2 * bandWidth;
        centerR = innerDiameter * 0.5f;
        centerRect = new RectF(-centerR, -centerR, centerR, centerR);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = event.getX() - centerX;
                float y = event.getY() - centerY;

                float dist = (float) Math.hypot(x, y);

                if (dist < centerR) {
                    if (y < 0) {
                        setHue(initialHueDegrees);
                    }

                    break;
                }
                float angleRad = (float) Math.atan2(y, x);
                // angleRad is [-𝜋; +𝜋]
                float hue = (float) (angleRad / (2 * Math.PI));
                if (hue < 0) hue += 1;
                Log.d("TMP",
                        String.format("x=%1.3f, y=%1.3f, angle=%1.3frad, hueDegrees=%1.3f", x, y,
                                angleRad, hue));
                setHue(hue);
                break;
        }

        return true;
    }
    public void setInitialHue(int initialHue) {
        if (initialHue == -1) initialHue = Colors.DEFAULT_HUE_DEG;
        this.initialHueDegrees = initialHue;
        this.initialPaint.setColor(Colors.getPrimaryColorForHue(initialHue));
        invalidate();
    }
}
