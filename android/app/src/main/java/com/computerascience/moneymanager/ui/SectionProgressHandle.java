package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public final class SectionProgressHandle extends FrameLayout {
    private static final int PANEL = Color.WHITE;
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int ACCENT = Color.rgb(18, 107, 95);

    private final Activity activity;
    private final View thumb;
    private final View.OnClickListener clickListener;
    private final ProgressDragListener dragListener;
    private float downY;
    private boolean dragging;

    public SectionProgressHandle(
            Activity activity,
            View.OnClickListener clickListener,
            ProgressDragListener dragListener
    ) {
        super(activity);
        this.activity = activity;
        this.clickListener = clickListener;
        this.dragListener = dragListener;
        setBackground(buttonBackground(PANEL, ROW_SURFACE, PANEL_BORDER));
        setElevation(dp(8));
        setClickable(true);
        setFocusable(true);
        setContentDescription("拖动滚动页面，点击打开本页目录");

        View rail = new View(activity);
        rail.setBackground(roundedBackground(PANEL_BORDER, Color.TRANSPARENT, 4));
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(dp(4), -1, Gravity.CENTER);
        railParams.topMargin = dp(10);
        railParams.bottomMargin = dp(10);
        addView(rail, railParams);

        thumb = new View(activity);
        thumb.setBackground(roundedBackground(ACCENT, Color.TRANSPARENT, 4));
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp(6), dp(32), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        thumbParams.topMargin = dp(10);
        addView(thumb, thumbParams);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                dragging = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getY() - downY) > dp(4)) {
                    dragging = true;
                }
                if (dragging) {
                    dispatchDrag(event.getY());
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (dragging) {
                    dispatchDrag(event.getY());
                    dragging = false;
                    return true;
                }
                return performClick();
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        if (clickListener != null) {
            clickListener.onClick(this);
        }
        return true;
    }

    public void setProgress(float value) {
        if (getHeight() == 0) {
            post(() -> setProgress(value));
            return;
        }
        float progress = Math.max(0f, Math.min(1f, value));
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        int topPadding = dp(10);
        int available = Math.max(0, getHeight() - topPadding * 2 - params.height);
        params.topMargin = topPadding + Math.round(available * progress);
        thumb.setLayoutParams(params);
    }

    private void dispatchDrag(float y) {
        if (dragListener == null || getHeight() <= 0) {
            return;
        }
        int topPadding = dp(10);
        int bottomPadding = dp(10);
        int available = Math.max(1, getHeight() - topPadding - bottomPadding);
        float progress = (y - topPadding) / available;
        dragListener.onProgress(Math.max(0f, Math.min(1f, progress)));
    }

    private StateListDrawable buttonBackground(int fill, int pressedFill, int border) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, roundedBackground(pressedFill, ACCENT, 12));
        states.addState(new int[]{android.R.attr.state_focused}, roundedBackground(pressedFill, ACCENT, 12));
        states.addState(new int[]{}, roundedBackground(fill, border, 12));
        return states;
    }

    private GradientDrawable roundedBackground(int fill, int border, int radius) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radius));
        bg.setStroke(dp(1), border);
        return bg;
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    public interface ProgressDragListener {
        void onProgress(float progress);
    }
}
