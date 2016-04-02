package com.tallogre.hanbaobao.Utilities;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TextView;

public class SingleLineTextView extends TextView {
    public SingleLineTextView(Context context) {
        super(context);
    }

    public SingleLineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SingleLineTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SingleLineTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float width = getPaint().measureText(getText(), 0, getText().length());
        super.onMeasure(MeasureSpec.makeMeasureSpec((int)Math.ceil(width), MeasureSpec.EXACTLY), heightMeasureSpec);
    }
}
