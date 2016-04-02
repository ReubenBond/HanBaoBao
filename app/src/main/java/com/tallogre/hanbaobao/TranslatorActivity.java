package com.tallogre.hanbaobao;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class TranslatorActivity extends AppCompatActivity implements ServiceConnection {

    private static final String FRAGMENT_TAG_TRANSLATOR = "translator";
    private TranslatorService translatorService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translator);

        if (savedInstanceState == null) {
            final FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.add(R.id.container, TranslatorFragment.newInstance(), FRAGMENT_TAG_TRANSLATOR);
            ft.commit();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        translatorService = ((TranslatorService.TranslatorServiceBinder) service).getService();
        if (translatorService != null) {
            unbindService(this);
            translatorService.stopSelf();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        translatorService = null;
    }
}
