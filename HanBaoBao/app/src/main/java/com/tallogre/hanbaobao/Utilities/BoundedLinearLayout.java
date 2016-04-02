package com.tallogre.hanbaobao.Utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import com.tallogre.hanbaobao.R;

public class BoundedLinearLayout extends LinearLayout {
    private static final int WITHOUT_MAX_HEIGHT_VALUE = -1;
    private int maxHeight = WITHOUT_MAX_HEIGHT_VALUE;

    public BoundedLinearLayout(Context context) {
        super(context);
    }


    public BoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyAttrs(context, attrs);
    }

    private void applyAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BoundedHeightLayout, 0, 0);

        try {
            setMaxHeight(a.getDimensionPixelSize(R.styleable.BoundedHeightLayout_boundedMaxHeight, 0));
        } finally {
            a.recycle();
        }
    }

    public BoundedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyAttrs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BoundedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        applyAttrs(context, attrs);
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        requestLayout();
    }

    public int getMaxHeight() {
        return this.maxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            if (maxHeight > 0 && heightSize > maxHeight) {
                heightSize = maxHeight;
                //noinspection Range
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);
            }
        } catch (Exception e) {
            Log.e("BoundedRelativeLayout", "Error forcing height", e);
        } finally {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
