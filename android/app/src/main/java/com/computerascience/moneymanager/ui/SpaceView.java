package com.computerascience.moneymanager.ui;

import android.app.Activity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public final class SpaceView extends FrameLayout {
    public SpaceView(Activity activity, int width, int height) {
        super(activity);
        setLayoutParams(new LinearLayout.LayoutParams(width, height));
    }
}
