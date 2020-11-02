package com.tallogre.hanbaobao;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.tallogre.hanbaobao.Utilities.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FloatingMenu {
    @BindView(R.id.floatingMenu)
    public ViewGroup rootView;
    private boolean overlayVisible;
    private ViewPropertyAnimator entranceAnimation;

    private final LayoutInflater inflater;
    private final WindowManager windowManager;
    private final List<Item> items = new ArrayList<>();
    private final WindowManager.LayoutParams layoutParams;
    private boolean isFullyVisible;

    FloatingMenu(LayoutInflater inflater, WindowManager windowManager) {
        this.inflater = inflater;
        this.windowManager = windowManager;
        ButterKnife.bind(this, inflater.inflate(R.layout.view_floating_menu, null, false));
        Rect screenRect = new Rect();
        windowManager.getDefaultDisplay().getRectSize(screenRect);
        layoutParams = new WindowManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.FILL;

        rootView.setAlpha(0.0f);
    }

    public Item getNearestItemWithinRange(MotionEvent event, double range) {
        Item nearestItem = null;
        double nearestItemDistance = 999999;
        if (!isFullyVisible()) return null;
        for (Item item : items) {

            if (item == null || item.getView() == null) continue;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (!item.getView().isLaidOut()) {
                    continue;
                }
            }

            double currentItemDistance = getDistance(item.getView(), event);
            if (currentItemDistance < nearestItemDistance && currentItemDistance < range) {
                nearestItem = item;
                nearestItemDistance = currentItemDistance;
            }
        }

        return nearestItem;
    }

    private double getDistance(View view, MotionEvent event) {
        int[] center = new int[2];
        ViewUtil.getViewCenter(view, center);
        if (center[0] == 0 && center[1] == 0) return 9001;

        double x = event.getRawX();
        double y = event.getRawY();
        double distanceX2 = (x - center[0]) * (x - center[0]);
        double distanceY2 = (y - center[1]) * (y - center[1]);

        return Math.sqrt(distanceX2 + distanceY2);
    }


    public boolean isFullyVisible() {
        return isFullyVisible && overlayVisible;
    }

    public void show() {
        if (overlayVisible) return;
        rootView.setAlpha(0.0f);
        entranceAnimation = rootView.animate().alpha(1f);
        entranceAnimation.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isFullyVisible = overlayVisible;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isFullyVisible = overlayVisible;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        entranceAnimation.setDuration(400);
        entranceAnimation.start();

        windowManager.addView(rootView, layoutParams);
        overlayVisible = true;
    }

    public void hide() {
        if (rootView == null || !overlayVisible) return;
        if (entranceAnimation != null) {
            entranceAnimation.cancel();
            entranceAnimation = null;
        }
        hideImmediate();
    }

    private void hideImmediate() {
        if (rootView == null || !overlayVisible) return;

        windowManager.removeViewImmediate(rootView);
        isFullyVisible = overlayVisible = false;
    }

    public void addItem(Item item) {
        items.add(item);
        item.setView(inflater.inflate(R.layout.view_floating_menu_item, rootView, false), inflater.inflate(R.layout.view_floating_menu_spacer, rootView, false));
        if (items.size() == 1) {
            // If this is the first view being added, add a spacer view above it.
            rootView.addView(inflater.inflate(R.layout.view_floating_menu_spacer, rootView, false));
        }

        rootView.addView(item.getView());

        // Add a spacer view below the view.
        rootView.addView(item.getSpacerView());
    }

    public static class Item {
        public final int iconResourceId;
        private View spacerView;
        private ScaleAnimation iconPressedAnimation;
        private static final float SWALLOW_ICON_SCALE = 1.35f;
        private static final int SWALLOW_DURATION = 150;
        private Vibrator vibrator;
        private ImageView iconView;

        public Item(int iconResourceId) {
            this.iconResourceId = iconResourceId;
        }

        private void vibrate() {
            vibrator.vibrate(20);
        }

        public void onHoverEnter() {
            vibrate();
            startDepressAnimation();
            if (iconView != null) iconView.setVisibility(View.INVISIBLE);
        }

        public void onHoverExit() {
            if (iconView != null) iconView.setVisibility(View.VISIBLE);
            reverseDepressAnimation();
        }

        public void onSelected() {
        }

        public void onBindView() {
            Picasso.get().load(iconResourceId).into(iconView);
        }

        private View view;

        public View getView() {
            return view;
        }

        protected View getSpacerView() {
            return spacerView;
        }

        public void setVisibility(int visibility) {
            view.setVisibility(visibility);
            spacerView.setVisibility(visibility);
        }

        private void setView(View view, View spacerView) {
            this.view = view;
            this.spacerView = spacerView;
            iconView = (ImageView) view.findViewById(R.id.icon);
            vibrator = (Vibrator) view.getContext().getSystemService(Context.VIBRATOR_SERVICE);
            onBindView();
        }

        private void cancelDepressAnimation() {
            if (iconPressedAnimation != null) {
                iconPressedAnimation.cancel();
                iconPressedAnimation = null;
            }
        }

        private void startDepressAnimation() {
            float initialScale = getLogoButtonAnimationScale();
            cancelDepressAnimation();
            // When the icon is pressed down, it gets pushed into the screen.
            // Setup the item press animation so we can start and reverse it when needed.
            iconPressedAnimation = new ScaleAnimation(initialScale, SWALLOW_ICON_SCALE, initialScale,
                    SWALLOW_ICON_SCALE, ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            iconPressedAnimation.setDuration((long) (SWALLOW_DURATION * initialScale));
            iconPressedAnimation.setFillAfter(true);
            iconPressedAnimation.setInterpolator(new OvershootInterpolator());
            getView().startAnimation(iconPressedAnimation);
        }

        private void reverseDepressAnimation() {
            float initialScale = getLogoButtonAnimationScale();
            cancelDepressAnimation();
            // Start the de-scaling animation.
            iconPressedAnimation = new ScaleAnimation(initialScale, 1f, initialScale, 1f, ScaleAnimation.RELATIVE_TO_SELF,
                    0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            iconPressedAnimation.setDuration(
                    (long) (SWALLOW_DURATION * ((1f - initialScale) / (1f - SWALLOW_ICON_SCALE))));
            iconPressedAnimation.setFillAfter(true);
            iconPressedAnimation.setInterpolator(new OvershootInterpolator());
            getView().startAnimation(iconPressedAnimation);
        }

        private float getLogoButtonAnimationScale() {
            float initialScale; // First get the current animation scale of the view.
            long currentTime = AnimationUtils.currentAnimationTimeMillis();
            android.view.animation.Transformation transformation = new android.view.animation.Transformation();
            if (iconPressedAnimation != null) {
                iconPressedAnimation.getTransformation(currentTime, transformation);
                float[] matrixValues = new float[9];
                transformation.getMatrix().getValues(matrixValues);
                initialScale = matrixValues[Matrix.MSCALE_X];
            } else {
                initialScale = 1f;
            }
            return initialScale;
        }
    }
}
