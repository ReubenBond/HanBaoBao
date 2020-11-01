package com.tallogre.hanbaobao;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.tallogre.hanbaobao.Utilities.Globals;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    private BackgroundService backgroundService;
    private ServiceConnection serviceConnection;
    private boolean shouldUnbindServiceOnDestroy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getApplicationContext().setTheme(R.style.AppTheme);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                shouldUnbindServiceOnDestroy = true;
                backgroundService = ((BackgroundService.ServiceBinder) service).getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                backgroundService = null;
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.ACTION_STOP);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        if (shouldUnbindServiceOnDestroy) getApplicationContext().unbindService(serviceConnection);
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!showWidget() || Globals.getUserPreferences().getShouldShowTutorial()) {
            startActivity(new Intent(this, TutorialActivity.class));
        }

        finish();
    }

    protected boolean showWidget() {
        try {
            if (FloatingWidget.hasOverlayPermissions(this)) {
                if (backgroundService == null) {
                    Intent serviceIntent = new Intent(this, BackgroundService.class);
                    startService(serviceIntent);
                    getApplicationContext().bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
                }

                if (backgroundService != null) {
                    backgroundService.show();
                }

                return true;
            }

            return false;
        } catch (Throwable e) {
            Log.e("MainActivity", "Exception in showWidget", e);
            return false;
        }
    }
}
