package com.tallogre.hanbaobao;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.tallogre.hanbaobao.Utilities.Globals;
import com.tallogre.hanbaobao.Utilities.UserPreferences;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TutorialActivity extends AppCompatActivity {
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 100;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    private UserPreferences userPreferences;
    private BackgroundService backgroundService;
    private ServiceConnection serviceConnection;
    private Boolean isServiceBound = false;
    @Bind(R.id.permissionRequiredCard)
    public View permissionRequiredCard;

    @OnClick(R.id.done)
    public void onClickDone() {
        userPreferences.setShouldShowTutorial(false);
        if (tryShowWidget()) {
            finish();
        }
    }

    @OnClick(R.id.acknowledgements)
    public void onClickAcknowledgements() {
        startActivity(new Intent(this, AcknowledgementsActivity.class));
    }

    @OnClick(R.id.enableDrawing)
    public void onClickEnableDrawing() {
        // Request the new overlay permission. Only applicable on Android M and above.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    @OnClick(R.id.enableAccessibility)
    public void onClickAccessibility() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        userPreferences = Globals.getUserPreferences();
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

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.ACTION_STOP);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        if (isServiceBound) {
            getApplicationContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tryShowWidget();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            tryShowWidget();
        }
    }

    private boolean tryShowWidget() {
        if (!showWidget()) {
            Toast toast = Toast.makeText(this, "Please permit HànBǎoBāo to draw over other apps.", Toast.LENGTH_LONG);
            toast.show();
            permissionRequiredCard.setVisibility(View.VISIBLE);
            userPreferences.setShouldShowTutorial(true);
            return false;
        } else {
            permissionRequiredCard.setVisibility(View.GONE);
            userPreferences.setShouldShowTutorial(false);
            return true;
        }
    }

    protected boolean showWidget() {
        if (FloatingWidget.hasOverlayPermissions(this)) {
            if (backgroundService == null) {
                Intent serviceIntent = new Intent(this, BackgroundService.class);
                startService(serviceIntent);
                getApplicationContext().bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
                isServiceBound = true;
            }

            if (backgroundService != null) {
                backgroundService.show();
            }

            return true;
        }

        return false;
    }
}
