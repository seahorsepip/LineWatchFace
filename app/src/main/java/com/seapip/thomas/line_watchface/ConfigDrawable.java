package com.seapip.thomas.line_watchface;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ConfigDrawable extends Drawable {
    private float radius;
    private Paint colorPaint;
    private Drawable icon;

    public ConfigDrawable(float radius) {
        this.radius = radius;
    }

    public ConfigDrawable(float radius, int color) {
        this(radius);
        colorPaint = new Paint();
        colorPaint.setAntiAlias(true);
        colorPaint.setColor(color);
    }

    public ConfigDrawable(float radius, Drawable icon) {
        this(radius);
        this.icon = icon;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setColor(Color.argb(51, 255, 255, 255));
        canvas.drawCircle(radius, radius, radius, backgroundPaint);
        if (colorPaint != null) {
            canvas.drawCircle(radius, radius, radius / 2, colorPaint);
        } else if (icon != null) {
            icon.setTint(Color.WHITE);
            icon.setBounds((int) (radius / 2.5),
                    (int) (radius / 2.5),
                    (int) (radius * 2 - radius / 2.5),
                    (int) (radius * 2 - radius / 2.5));
            icon.draw(canvas);
        }
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}
