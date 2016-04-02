package com.tallogre.hanbaobao;

import android.app.Application;

import com.tallogre.hanbaobao.Utilities.Globals;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        setTheme(R.style.AppTheme);
        Globals.initialize(this);
    }
}
