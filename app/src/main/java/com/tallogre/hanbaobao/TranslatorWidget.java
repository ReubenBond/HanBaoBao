package com.tallogre.hanbaobao;
import android.view.View;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class TranslatorWidget {
    public View rootView;
    public TranslatorWidget(View widget) {
        this.rootView = widget;
        ButterKnife.bind(this, this.rootView);
    }

    @Bind(R.id.outputText)
    public TextView output;
}