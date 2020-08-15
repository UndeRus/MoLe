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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import net.ktnx.mobileledger.utils.Colors;
import net.ktnx.mobileledger.utils.DimensionUtils;

import java.util.Locale;

import static net.ktnx.mobileledger.utils.Logger.debug;

public class HueRing extends View {
    public static final int hueStepDegrees = 5;
    private Paint ringPaint, initialPaint, currentPaint, markerPaint;
    private int center;
    private int padding;
    private int initialHueDegrees;
    private int color, hueDegrees;
    private float outerR;
    private float innerR;
    private float bandWidth;
    private float centerR;
    private RectF centerRect = new RectF();
    private RectF ringRect = new RectF();
    private int markerOverflow;
    private int markerStrokeWidth;
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
        final int[] steps = {Colors.getPrimaryColorForHue(0),      // red
                             Colors.getPrimaryColorForHue(60),     // yellow
                             Colors.getPrimaryColorForHue(120),    // green
                             Colors.getPrimaryColorForHue(180),    // cyan
                             Colors.getPrimaryColorForHue(240),    // blue
                             Colors.getPrimaryColorForHue(300),    // magenta
                             Colors.getPrimaryColorForHue(360),    // red, again
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

        markerStrokeWidth = DimensionUtils.dp2px(getContext(), 4);

        padding = markerStrokeWidth * 2 + 2;

        markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setColor(0xa0000000);
        markerPaint.setStrokeWidth(markerStrokeWidth);
    }
    public int getColor() {
        return color;
    }
    public int getHueDegrees() {
        return hueDegrees;
    }
    public void setHue(int hueDegrees) {
        if (hueDegrees == -1)
            hueDegrees = Colors.DEFAULT_HUE_DEG;

        if (hueDegrees != Colors.DEFAULT_HUE_DEG) {
            // round to 15 degrees
            int rem = hueDegrees % hueStepDegrees;
            if (rem < (hueStepDegrees / 2))
                hueDegrees -= rem;
            else
                hueDegrees += hueStepDegrees - rem;
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

        float center = getWidth() / 2f;
        ringPaint.setStrokeWidth((int) bandWidth);

        canvas.save();
        canvas.translate(center, center);
        canvas.drawOval(ringRect, ringPaint);

        canvas.drawArc(centerRect, 180, 180, true, initialPaint);
        canvas.drawArc(centerRect, 0, 180, true, currentPaint);

        canvas.restore();
        drawMarker(canvas, center);
    }
    private void drawMarker(Canvas canvas, float center) {
        float leftRadians = (float) Math.toRadians(-hueStepDegrees / 2f);
        float rightRadians = (float) Math.toRadians(hueStepDegrees / 2f);
        float sl = (float) Math.sin(leftRadians);
        float sr = (float) Math.sin(rightRadians);
        float cl = (float) Math.cos(leftRadians);
        float cr = (float) Math.cos(rightRadians);
        float innerEdge = innerR - 1.5f * markerStrokeWidth;
        float outerEdge = outerR + 1.5f + markerStrokeWidth;
        Path p = new Path();
//        p.arcTo(-innerEdge, -innerEdge, innerEdge, innerEdge, -hueStepDegrees / 2f,
//                hueStepDegrees, true);
//        p.lineTo(outerEdge * cr, outerEdge * sr);
        p.arcTo(-outerEdge, -outerEdge, outerEdge, outerEdge, hueStepDegrees / 2f, -hueStepDegrees,
                false);
//        p.close();
        canvas.save();
        canvas.translate(center, center);
        canvas.rotate(hueDegrees, 0, 0);
        canvas.drawPath(p, markerPaint);
        canvas.restore();
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);

        int diameter;
        if ((widthMode == MeasureSpec.AT_MOST) && (heightMode == MeasureSpec.AT_MOST)) {
            diameter = Math.min(widthSize, heightSize);
        }
        else {
            setMeasuredDimension(MEASURED_STATE_TOO_SMALL, MEASURED_STATE_TOO_SMALL);
            return;
        }

        setMeasuredDimension(diameter, diameter);

//        padding = DimensionUtils.dp2px(getContext(),
//                getContext().getResources().getDimension(R.dimen.activity_horizontal_margin)) / 2;
        diameter -= 2 * padding;
        outerR = diameter / 2f;
        center = padding + (int) outerR;

        bandWidth = diameter / 3.5f;
        float ringR = outerR - bandWidth / 2f;
        innerR = outerR - bandWidth;

        ringRect.set(-ringR, -ringR, ringR, ringR);

        float innerDiameter = diameter - 2 * bandWidth;
        centerR = innerDiameter * 0.5f;
        centerRect.set(-centerR, -centerR, centerR, centerR);
    }
    @Override
    public boolean performClick() {
        return super.performClick();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = event.getX() - center;
                float y = event.getY() - center;

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
                if (hue < 0)
                    hue += 1;
                debug("TMP", String.format(Locale.US,
                        "x=%1.3f, y=%1.3f, angle=%1.3f rad, hueDegrees=%1.3f", x, y, angleRad,
                        hue));
                setHue(hue);
                break;
            case MotionEvent.ACTION_UP:
                performClick();
                break;
        }
        return true;
    }
    public void setInitialHue(int initialHue) {
        if (initialHue == -1)
            initialHue = Colors.DEFAULT_HUE_DEG;
        this.initialHueDegrees = initialHue;
        this.initialPaint.setColor(Colors.getPrimaryColorForHue(initialHue));
        invalidate();
    }
}
