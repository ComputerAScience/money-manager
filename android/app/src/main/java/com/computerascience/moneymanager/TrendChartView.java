package com.computerascience.moneymanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

final class TrendChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private List<AssetSnapshot> snapshots = new ArrayList<>();

    TrendChartView(Context context) {
        super(context);
        setMinimumHeight(dp(180));
    }

    void setSnapshots(List<AssetSnapshot> snapshots) {
        this.snapshots = snapshots == null ? new ArrayList<>() : new ArrayList<>(snapshots);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(14);
        float right = getWidth() - dp(14);
        float top = dp(18);
        float bottom = getHeight() - dp(28);
        if (right <= left || bottom <= top) {
            return;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1));
        paint.setColor(0xFFD9DDD5);
        for (int i = 0; i < 4; i += 1) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        if (snapshots.size() < 2) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF667068);
            paint.setTextSize(dp(14));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("记录几次快照后显示一年趋势", getWidth() / 2f, (top + bottom) / 2f, paint);
            return;
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (AssetSnapshot snapshot : snapshots) {
            min = Math.min(min, snapshot.netWorth);
            max = Math.max(max, snapshot.netWorth);
        }
        if (Math.abs(max - min) < 0.0001) {
            max += 1;
            min -= 1;
        }

        path.reset();
        for (int index = 0; index < snapshots.size(); index += 1) {
            AssetSnapshot snapshot = snapshots.get(index);
            float x = left + (right - left) * index / Math.max(1f, snapshots.size() - 1f);
            float y = (float) (bottom - ((snapshot.netWorth - min) / (max - min)) * (bottom - top));
            if (index == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(dp(3));
        paint.setColor(0xFF126B5F);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        AssetSnapshot last = snapshots.get(snapshots.size() - 1);
        float lastX = right;
        float lastY = (float) (bottom - ((last.netWorth - min) / (max - min)) * (bottom - top));
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(lastX, lastY, dp(6), paint);
        paint.setColor(0xFF126B5F);
        canvas.drawCircle(lastX, lastY, dp(4), paint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
