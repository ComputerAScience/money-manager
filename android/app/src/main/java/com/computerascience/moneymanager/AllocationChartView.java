package com.computerascience.moneymanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

final class AllocationChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private List<CategoryBreakdown> categories = new ArrayList<>();

    AllocationChartView(Context context) {
        super(context);
        setMinimumHeight(dp(144));
    }

    void setCategories(List<CategoryBreakdown> categories) {
        this.categories = categories == null ? new ArrayList<>() : new ArrayList<>(categories);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight()) - dp(12);
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        oval.set(left, top, left + size, top + size);

        double total = 0;
        for (CategoryBreakdown category : categories) {
            total += category.value;
        }

        paint.setStyle(Paint.Style.FILL);
        if (total <= 0) {
            paint.setColor(0xFFD9DDD5);
            canvas.drawOval(oval, paint);
        } else {
            float start = -90f;
            for (CategoryBreakdown category : categories) {
                paint.setColor(category.color);
                float sweep = (float) (category.value / total * 360f);
                canvas.drawArc(oval, start, sweep, true, paint);
                start += sweep;
            }
        }

        paint.setColor(Color.WHITE);
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size * 0.28f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xFFD9DDD5);
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size * 0.28f, paint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
