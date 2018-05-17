package com.tallogre.hanbaobao;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AcknowledgementsActivity extends AppCompatActivity {
    @OnClick(R.id.email)
    public void onClickEmail() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("mailto:reuben.bond@gmail.com?subject=HanBaoBao feedback"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @BindView(R.id.acknowledgements)
    TextView acknowledgements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acknowledgements);
        ButterKnife.bind(this);
        StringBuilder res = getLicenseText();
        acknowledgements.setText(res.toString());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackgroundService.ACTION_STOP);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        }, intentFilter);
    }

    @NonNull
    private StringBuilder getLicenseText() {
        StringBuilder res = new StringBuilder();
        try {
            InputStream is = getResources().openRawResource(R.raw.license);
            BufferedReader bis = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = bis.readLine()) != null) res.append(line).append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
}
