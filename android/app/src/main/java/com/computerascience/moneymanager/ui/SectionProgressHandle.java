package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

public final class SectionProgressHandle extends FrameLayout {
    private static final int PANEL_BORDER = Color.rgb(226, 232, 240);
    private static final int ROW_SURFACE = Color.rgb(248, 250, 252);
    private static final int INK = Color.rgb(31, 41, 55);
    private static final int MUTED = Color.rgb(100, 116, 139);
    private static final int ACCENT = Color.rgb(18, 107, 95);

    private final Activity activity;
    private final View rail;
    private final View thumb;
    private final TextView bubble;
    private final View.OnClickListener clickListener;
    private final ProgressDragListener dragListener;
    private String[] sectionLabels = new String[0];
    private float downY;
    private boolean dragging;
    private boolean active;
    private int activeSectionIndex = -1;

    public SectionProgressHandle(
            Activity activity,
            View.OnClickListener clickListener,
            ProgressDragListener dragListener
    ) {
        super(activity);
        this.activity = activity;
        this.clickListener = clickListener;
        this.dragListener = dragListener;
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(true);
        setContentDescription("沿右侧拖动选择本页目录，点击打开本页目录");

        rail = new View(activity);
        rail.setBackground(roundedBackground(PANEL_BORDER, Color.TRANSPARENT, 4));
        rail.setAlpha(0.28f);
        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(dp(3), -1, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        railParams.topMargin = dp(42);
        railParams.rightMargin = dp(12);
        railParams.bottomMargin = dp(42);
        addView(rail, railParams);

        thumb = new View(activity);
        thumb.setBackground(roundedBackground(ACCENT, Color.TRANSPARENT, 4));
        thumb.setAlpha(0.46f);
        FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(dp(7), dp(42), Gravity.TOP | Gravity.RIGHT);
        thumbParams.topMargin = dp(42);
        thumbParams.rightMargin = dp(10);
        addView(thumb, thumbParams);

        bubble = new TextView(activity);
        bubble.setTextColor(INK);
        bubble.setTextSize(14);
        bubble.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER_VERTICAL);
        bubble.setIncludeFontPadding(false);
        bubble.setPadding(dp(14), 0, dp(14), 0);
        bubble.setSingleLine(true);
        bubble.setBackground(roundedBackground(ROW_SURFACE, PANEL_BORDER, 20));
        bubble.setElevation(dp(10));
        bubble.setVisibility(INVISIBLE);
        bubble.setAlpha(0f);
        FrameLayout.LayoutParams bubbleParams = new FrameLayout.LayoutParams(dp(154), dp(42), Gravity.TOP | Gravity.RIGHT);
        bubbleParams.topMargin = dp(42);
        bubbleParams.rightMargin = dp(32);
        addView(bubble, bubbleParams);
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
                setPressed(true);
                setActive(true);
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && Math.abs(event.getY() - downY) > dp(4)) {
                    dragging = true;
                    if (dragListener != null) {
                        dragListener.onDragStart();
                    }
                }
                if (dragging) {
                    dispatchDrag(event.getY());
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                setPressed(false);
                if (dragging) {
                    float progress = progressForY(event.getY());
                    setProgress(progress);
                    if (dragListener != null) {
                        dragListener.onProgress(progress);
                        dragListener.onDragEnd(progress);
                    }
                    dragging = false;
                    setActive(false);
                    return true;
                }
                setActive(false);
                return performClick();
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                setPressed(false);
                if (dragging && dragListener != null) {
                    dragListener.onDragCancel();
                }
                dragging = false;
                setActive(false);
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
        int topPadding = dp(42);
        int bottomPadding = dp(42);
        int available = Math.max(0, getHeight() - topPadding - bottomPadding - params.height);
        params.topMargin = topPadding + Math.round(available * progress);
        thumb.setLayoutParams(params);
        updateBubblePosition();
    }

    public void setSectionLabels(String[] labels) {
        sectionLabels = labels == null ? new String[0] : labels;
        updateBubbleText();
    }

    public void setActiveSection(int index) {
        activeSectionIndex = index;
        updateBubbleText();
    }

    private void dispatchDrag(float y) {
        if (dragListener == null || getHeight() <= 0) {
            return;
        }
        float progress = progressForY(y);
        setProgress(progress);
        dragListener.onProgress(progress);
    }

    private float progressForY(float y) {
        int topPadding = dp(42);
        int bottomPadding = dp(42);
        int available = Math.max(1, getHeight() - topPadding - bottomPadding);
        float progress = (y - topPadding) / available;
        return Math.max(0f, Math.min(1f, progress));
    }

    private void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
        animateHandle();
    }

    private void animateHandle() {
        rail.animate().alpha(active ? 0.64f : 0.28f).setDuration(120).start();
        thumb.animate()
                .alpha(active ? 1f : 0.46f)
                .scaleX(active ? 1.18f : 1f)
                .scaleY(active ? 1.12f : 1f)
                .setDuration(120)
                .start();
        if (active) {
            bubble.setVisibility(VISIBLE);
            updateBubbleText();
            bubble.animate().alpha(1f).translationX(0f).setDuration(120).start();
        } else {
            bubble.animate().alpha(0f).translationX(dp(8)).setDuration(120).withEndAction(() -> {
                if (!this.active) {
                    bubble.setVisibility(INVISIBLE);
                }
            }).start();
        }
    }

    private void updateBubbleText() {
        if (activeSectionIndex >= 0 && activeSectionIndex < sectionLabels.length) {
            bubble.setText(sectionLabels[activeSectionIndex]);
            bubble.setTextColor(INK);
            return;
        }
        int count = sectionLabels.length;
        String countText = count == 0 ? "本页目录" : String.format(Locale.getDefault(), "%d 个区块", count);
        bubble.setText(countText);
        bubble.setTextColor(MUTED);
    }

    private void updateBubblePosition() {
        if (getHeight() == 0) {
            return;
        }
        FrameLayout.LayoutParams thumbParams = (FrameLayout.LayoutParams) thumb.getLayoutParams();
        FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        int topPadding = dp(42);
        int bottomPadding = dp(42);
        int available = Math.max(0, getHeight() - topPadding - bottomPadding - bubbleParams.height);
        int thumbCenter = thumbParams.topMargin + thumbParams.height / 2;
        bubbleParams.topMargin = Math.max(topPadding, Math.min(topPadding + available, thumbCenter - bubbleParams.height / 2));
        bubble.setLayoutParams(bubbleParams);
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
        void onDragStart();

        void onProgress(float progress);

        void onDragEnd(float progress);

        void onDragCancel();
    }
}
