package com.tallogre.hanbaobao;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.tallogre.hanbaobao.Adapters.TabViewPagerAdapter;
import com.tallogre.hanbaobao.Utilities.Globals;
import com.tallogre.hanbaobao.Utilities.ViewUtil;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OverAppActivity extends AppCompatActivity {
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    private BackgroundService backgroundService;
    private ServiceConnection serviceConnection;

    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.tabs)
    TabLayout tabLayout;
    private SoftKeyboardTouchEventDispatcher touchEventDispatcher;
    private TabViewPagerAdapter tabViewPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) savedInstanceState = Globals.getSavedInstanceState();
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.left_slide_in, R.anim.left_slide_out);
        setContentView(R.layout.activity_overapp);
        touchEventDispatcher = new SoftKeyboardTouchEventDispatcher();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                backgroundService = ((BackgroundService.ServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                backgroundService = null;
            }
        };
        ButterKnife.bind(this);

        // Register for app close events.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.ACTION_STOP);
        registerReceiver(receiver, intentFilter);

        configureTabs();

        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) savedInstanceState = Globals.getSavedInstanceState();
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            Parcelable tabState = savedInstanceState.getParcelable("tabs");
            if (tabState != null) tabViewPagerAdapter.restoreState(tabState, getClassLoader());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("tabs", tabViewPagerAdapter.saveState());
        Globals.setSavedInstanceState(outState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return touchEventDispatcher.dispatchTouchEvent(ev);
    }

    @OnClick(R.id.exitButton)
    public void onClickExit() {
        if (backgroundService != null) {
            startService(backgroundService.getExitIntent());
        }
    }

    private void configureTabs() {
        tabViewPagerAdapter = new TabViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabViewPagerAdapter);

        TabLayout.Tab dictionaryTab = tabLayout.newTab();
        TabLayout.Tab settingsTab = tabLayout.newTab();
        //TabLayout.Tab historyTab = tabLayout.newTab();
        //TabLayout.Tab wordListTab = tabLayout.newTab();

        dictionaryTab/*.setText("Find")*/.setIcon(R.drawable.ic_magnifying_glass_light);
        settingsTab/*.setText("History")*/.setIcon(R.drawable.ic_settings_light);
        //historyTab/*.setText("History")*/.setIcon(R.drawable.ic_history);
        //wordListTab/*.setText("Stars")*/.setIcon(R.drawable.ic_star);

        tabLayout.addTab(dictionaryTab, 0);
        tabLayout.addTab(settingsTab, 1);
        //tabLayout.addTab(historyTab, 1);
        //tabLayout.addTab(wordListTab, 2);

        tabLayout.setTabTextColors(ContextCompat.getColorStateList(this, R.color.tab_selector));
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.tabIndicator));

        // Listen for both clicks and swipes, updating the tab as necessary.
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }

    @Override
    protected void onDestroy() {
        getApplicationContext().unbindService(serviceConnection);
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void finish() {
        onSaveInstanceState(new Bundle());
        super.finish();
        overridePendingTransition(R.anim.left_slide_in, R.anim.left_slide_out);
    }

    @Override
    protected void onPause() {
        Bundle bundle = new Bundle();
        onSaveInstanceState(bundle);
        Globals.setSavedInstanceState(bundle);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLayoutParams();
        if (backgroundService == null) {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            startService(serviceIntent);
            getApplicationContext().bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        }
    }

    private void initLayoutParams() {
        WindowManager.LayoutParams originalParams = getWindow().getAttributes();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                originalParams.width,
                originalParams.height,
                originalParams.type,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Get the screen size.
        Rect screenRect = new Rect();
        Display display = windowManager.getDefaultDisplay();
        display.getRectSize(screenRect);
        params.gravity = Gravity.LEFT;
        params.width = (int) (screenRect.width() * 4.0 / 5.0);
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
        getWindow().setAttributes(params);
    }

    private class SoftKeyboardTouchEventDispatcher {
        private InputMethodManager inputMethodManager;
        private final ViewUtil viewUtil = new ViewUtil();

        private SoftKeyboardTouchEventDispatcher() {
            inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }

        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_UP) {
                View currentFocus = getCurrentFocus();
                if (currentFocus != null && currentFocus instanceof EditText && !viewUtil.eventIsInView(currentFocus, ev)) {
                    inputMethodManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                    currentFocus.clearFocus();
                }
            }

            return OverAppActivity.super.dispatchTouchEvent(ev);
        }
    }
}