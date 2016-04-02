package com.tallogre.hanbaobao;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;


import com.github.promeg.pinyinhelper.Pinyin;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

public class TranslatorService extends Service implements FloatingViewListener, ClipboardManager.OnPrimaryClipChangedListener {
    private static final String TAG = "TranslatorService";
    private static final int NOTIFICATION_ID = 38389001;
    private FloatingViewManager floatingViewManager;

    private IBinder serviceBinder;
    private ClipboardManager clipboardManager;
    public TranslatorWidget widget;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (floatingViewManager != null) {
            return START_STICKY;
        }

        final DisplayMetrics metrics = new DisplayMetrics();
        final WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        serviceBinder = new TranslatorServiceBinder(this);
        final LayoutInflater inflater = LayoutInflater.from(this);
        widget = new TranslatorWidget(inflater.inflate(R.layout.widget_translator, null, false));

        widget.rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "bsadasdsaads");
            }
        });

        listenForClipboardChanges();

        floatingViewManager = new FloatingViewManager(this, this);
        floatingViewManager.setFixedTrashIconImage(R.drawable.ic_trash_fixed);
        floatingViewManager.setActionTrashIconImage(R.drawable.ic_trash_action);
        final FloatingViewManager.Options options = new FloatingViewManager.Options();
        options.shape = FloatingViewManager.SHAPE_CIRCLE;
        options.overMargin = (int) (16 * metrics.density);
        floatingViewManager.addViewToWindow(widget.rootView, options);

        startForeground(NOTIFICATION_ID, createNotification());

        return START_REDELIVER_INTENT;
    }

    private void listenForClipboardChanges() {
        clipboardManager = (ClipboardManager)this.getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        destroy();
        super.onDestroy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onFinishFloatingView() {
        stopSelf();
    }

    private void destroy() {
        if (floatingViewManager != null) {
            floatingViewManager.removeAllViewToWindow();
            floatingViewManager = null;
        }
    }

    private Notification createNotification() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("CONTENT TITLE LALALALD");
        builder.setContentText("CONTENT TEXT!!!!");
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        final Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyPendingIntent);

        return builder.build();
    }

    @Override
    public void onPrimaryClipChanged() {
        if (!clipboardManager.hasPrimaryClip()) {
            Log.i(TAG, "Clipboard cleared");
            return;
        }

        ClipData clip = clipboardManager.getPrimaryClip();
        int numClipboardItems = clip.getItemCount();
        for(int i = 0; i < numClipboardItems; i++) {
            ClipData.Item item = clip.getItemAt(i);
            CharSequence chars = item.coerceToText(this);
            convertToPinyin(chars);
        }
    }

    private void convertToPinyin(CharSequence text) {

        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITH_TONE_MARK);
        Log.i(TAG, "Clipboard changed:" + text);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++){
            char c = text.charAt(i);
            if (Pinyin.isChinese(c)) {
                String[] pinyin = new String[0];
                try {
                    pinyin = PinyinHelper.toHanyuPinyinStringArray(c, format);
                } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                    badHanyuPinyinOutputFormatCombination.printStackTrace();
                }
                if (pinyin.length > 0) {
                    builder.append(pinyin[0]);
                } else {
                    builder.append(c);
                }

                if (i + 1 < text.length()) {
                    builder.append(' ');
                }
            }
            else builder.append(c);
        }



        String result = builder.toString();
        Log.i(TAG, "Transliterated \"" + text + "\" to \"" + result +"\"");

        widget.output.setText(result);
    }

    public static class TranslatorServiceBinder extends Binder {
        private final WeakReference<TranslatorService> service;

        public TranslatorServiceBinder(TranslatorService service) {
            this.service = new WeakReference<>(service);
        }

        public TranslatorService getService() {
            return this.service.get();
        }
    }
}
