package com.tallogre.hanbaobao;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ProcessTextActivity extends Activity {
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        CharSequence text = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.putExtra(MainActivity.TEXT_EXTRA, text);
        startActivity(mainActivityIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
