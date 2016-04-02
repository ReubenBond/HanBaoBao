package com.tallogre.hanbaobao.Utilities;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by reube on 4/23/2016.
 */
public class ViewUtil {
    public static void getViewCenter(View view, int[] coordinates) {
        view.getLocationOnScreen(coordinates);
        int x = coordinates[0] + view.getPaddingLeft();
        int y = coordinates[1] + view.getPaddingTop();
        int width = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
        int height = view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
        coordinates[0] = x + width / 2;
        coordinates[1] = y + height / 2;
    }

    private Rect touchRect = new Rect();
    public boolean eventIsInView(View view, MotionEvent ev) {

        final int x = (int) ev.getRawX();
        final int y = (int) ev.getRawY();

        getLocationOnScreen(view, touchRect);
        return touchRect.contains(x, y);
    }

    private int[] coordinates = new int[2];

    public void getLocationOnScreen(View view, Rect location) {
        view.getLocationOnScreen(coordinates);
        location.set(
                coordinates[0],
                coordinates[1],
                coordinates[0] + view.getWidth(),
                coordinates[1] + view.getHeight());
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }
}
